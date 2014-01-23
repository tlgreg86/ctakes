package org.apache.ctakes.temporal.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.ClearTKDocTimeRelAnnotator;
import org.apache.ctakes.temporal.ae.ClearTKDocumentCreationTimeAnnotator;
import org.apache.ctakes.temporal.ae.ClearTKLinkToTHYMELinkAnnotator;
import org.apache.ctakes.temporal.ae.EventToClearTKEventAnnotator;
import org.apache.ctakes.temporal.ae.TimexToClearTKTimexAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.RemoveCrossSentenceRelations;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.RemoveEventEventRelations;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.RemoveNonContainsRelations;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.RemoveRelations;
import org.apache.ctakes.temporal.eval.EvaluationOfTemporalRelations_ImplBase.PreserveEventEventRelations;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.syntax.opennlp.ParserAnnotator;
import org.cleartk.syntax.opennlp.PosTaggerAnnotator;
import org.cleartk.syntax.opennlp.SentenceAnnotator;
import org.cleartk.timeml.event.EventAspectAnnotator;
import org.cleartk.timeml.event.EventClassAnnotator;
import org.cleartk.timeml.event.EventModalityAnnotator;
import org.cleartk.timeml.event.EventPolarityAnnotator;
import org.cleartk.timeml.event.EventTenseAnnotator;
import org.cleartk.timeml.time.TimeTypeAnnotator;
import org.cleartk.timeml.tlink.TemporalLinkEventToDocumentCreationTimeAnnotator;
import org.cleartk.timeml.tlink.TemporalLinkEventToSameSentenceTimeAnnotator;
import org.cleartk.timeml.tlink.TemporalLinkEventToSubordinatedEventAnnotator;
import org.cleartk.timeml.type.TemporalLink;
import org.cleartk.token.stem.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Function;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class EvaluationOfClearTKRelations extends
    EvaluationOfTemporalRelations_ImplBase {
 
  /**
   * @param args
   * @throws Exception 
   */
  static interface EvalOptions extends Options{
    @Option(shortName="iee") boolean getIgnoreEventEvent();
    @Option(shortName="iet") boolean getIgnoreEventTime();
  }
  
  private boolean doEventEvent = true;
  private boolean doEventTime = true;

  
  public static void main(String[] args) throws Exception {
    EvalOptions options = CliFactory.parseArguments(EvalOptions.class, args);
    if(options.getIgnoreEventEvent() && options.getIgnoreEventTime()){
      System.err.println("Ignoring all relation types is not a valid configuration.");
      System.exit(-1);
    }
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getTrainPatientSets(patientSets);
    List<Integer> devItems = THYMEData.getDevPatientSets(patientSets);
    List<Integer> testItems = THYMEData.getTestPatientSets(patientSets);
    
    List<Integer> allTraining = new ArrayList<Integer>(trainItems);
    List<Integer> allTest;
    if (options.getTest()) {
      allTraining.addAll(devItems);
      allTest = new ArrayList<Integer>(testItems);
    } else {
      allTest = new ArrayList<Integer>(devItems);
    }
    
    EvaluationOfClearTKRelations evaluation = new EvaluationOfClearTKRelations(
        new File("target/eval/cleartk-event-time-links"),
        options.getRawTextDirectory(),
        options.getXMLDirectory(),
        options.getXMLFormat(),
        options.getXMIDirectory());
    evaluation.setExtractEventEvent(!options.getIgnoreEventEvent());
    evaluation.setExtractEventTime(!options.getIgnoreEventTime());
    
    evaluation.prepareXMIsFor(patientSets);
    AnnotationStatistics<String> stats = evaluation.trainAndTest(allTraining, allTest);
    System.err.println(stats);
  }
  
  
  
  public EvaluationOfClearTKRelations(File baseDirectory, File rawTextDirectory,
      File xmlDirectory,
      org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat xmlFormat,
      File xmiDirectory) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, xmiDirectory, null, false, false, defaultParams);
  }
  
  private void setExtractEventTime(boolean eventTime) {
    this.doEventTime = eventTime;
  }

  private void setExtractEventEvent(boolean eventEvent) {
    this.doEventEvent = eventEvent;
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory)
      throws Exception {
    // not training a model - just using the ClearTK one
  }
  
  @Override
  protected AnnotationStatistics<String> test(
      CollectionReader collectionReader, File directory) throws Exception {
    AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(CopyFromGold.getDescription(EventMention.class, TimeMention.class));
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(
        RemoveCrossSentenceRelations.class,
        RemoveCrossSentenceRelations.PARAM_SENTENCE_VIEW,
        CAS.NAME_DEFAULT_SOFA,
        RemoveCrossSentenceRelations.PARAM_RELATION_VIEW,
        GOLD_VIEW_NAME));
    if(!this.doEventEvent){
      aggregateBuilder.add(
          AnalysisEngineFactory.createPrimitiveDescription(RemoveEventEventRelations.class),
          CAS.NAME_DEFAULT_SOFA,
          GOLD_VIEW_NAME);
    }
    if(!this.doEventTime){
      aggregateBuilder.add(
          AnalysisEngineFactory.createPrimitiveDescription(PreserveEventEventRelations.class),
          CAS.NAME_DEFAULT_SOFA,
          GOLD_VIEW_NAME);
    }
    aggregateBuilder.add(SentenceAnnotator.getDescription());
    aggregateBuilder.add(TokenAnnotator.getDescription());
    aggregateBuilder.add(PosTaggerAnnotator.getDescription());
    aggregateBuilder.add(DefaultSnowballStemmer.getDescription("English"));
    aggregateBuilder.add(ParserAnnotator.getDescription());
    aggregateBuilder.add(EventToClearTKEventAnnotator.getAnnotatorDescription());//for every cTakes eventMention, create a cleartk event
    aggregateBuilder.add(TimexToClearTKTimexAnnotator.getAnnotatorDescription());
//    aggregateBuilder.add(ClearTKDocumentCreationTimeAnnotator.getAnnotatorDescription());//for every jCAS create an empty DCT, and add it to index
    aggregateBuilder.add(EventTenseAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventtenseannotator/model.jar"));
    aggregateBuilder.add(EventAspectAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventaspectannotator/model.jar"));
    aggregateBuilder.add(EventClassAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventclassannotator/model.jar"));
    aggregateBuilder.add(EventPolarityAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventpolarityannotator/model.jar"));
    aggregateBuilder.add(EventModalityAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/event/eventmodalityannotator/model.jar"));
    aggregateBuilder.add(TimeTypeAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/time/timetypeannotator/model.jar"));
//    aggregateBuilder.add(TemporalLinkEventToDocumentCreationTimeAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/tlink/temporallinkeventtodocumentcreationtimeannotator/model.jar"));
//    aggregateBuilder.add(ClearTKDocTimeRelAnnotator.getAnnotatorDescription());// for every tlink, check if it cover and event, add the tlink type to the event's docTimeRel attribute

    if(this.doEventTime){
      aggregateBuilder.add(TemporalLinkEventToSameSentenceTimeAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/tlink/temporallinkeventtosamesentencetimeannotator/model.jar"));
    }
    if(this.doEventEvent){
      aggregateBuilder.add(TemporalLinkEventToSubordinatedEventAnnotator.FACTORY.getAnnotatorDescription("/org/cleartk/timeml/tlink/temporallinkeventtosubordinatedeventannotator/model.jar"));
    }
    
    aggregateBuilder.add(ClearTKLinkToTHYMELinkAnnotator.getAnnotatorDescription());

    Function<BinaryTextRelation, ?> getSpan = new Function<BinaryTextRelation, HashableArguments>() {
      public HashableArguments apply(BinaryTextRelation relation) {
        return new HashableArguments(relation);
      }
    };
    Function<BinaryTextRelation, String> getOutcome = AnnotationStatistics.annotationToFeatureValue("category");
    AnnotationStatistics<String> stats = new AnnotationStatistics<String>();
    for (JCas jCas : new JCasIterable(collectionReader, aggregateBuilder.createAggregate())) {
      JCas goldView = jCas.getView(GOLD_VIEW_NAME);
      JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      Collection<BinaryTextRelation> goldRelations = JCasUtil.select(
          goldView,
          BinaryTextRelation.class);
      Collection<BinaryTextRelation> systemRelations = JCasUtil.select(
          systemView,
          BinaryTextRelation.class);
      stats.add(goldRelations, systemRelations, getSpan, getOutcome);
    }
    
    return stats;
  }


}
