package org.apache.ctakes.temporal.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.CRFTimeAnnotator;
import org.apache.ctakes.temporal.ae.ConstituencyBasedTimeAnnotator;
import org.apache.ctakes.temporal.ae.MetaTimeAnnotator;
import org.apache.ctakes.temporal.ae.TimeAnnotator;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.crfsuite.CRFSuiteStringOutcomeDataWriter;
import org.cleartk.classifier.jar.JarClassifierBuilder;
import org.cleartk.eval.AnnotationStatistics;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.pipeline.SimplePipeline;

import com.google.common.collect.Maps;
import com.lexicalscope.jewel.cli.CliFactory;

public class EvaluationOfMetaTimeExpressionExtractor extends EvaluationOfAnnotationSpans_ImplBase {
  public static int nFolds = 2;
  private List<Integer> allTrain = null;
  
  public EvaluationOfMetaTimeExpressionExtractor(File baseDirectory,
      File rawTextDirectory, File xmlDirectory,
      org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat xmlFormat,
      File xmiDirectory, File treebankDirectory,
      List<Integer> allTrain, Class<? extends Annotation> annotationClass) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, xmiDirectory,
        treebankDirectory, annotationClass);
    this.allTrain = allTrain;
  }

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getTrainPatientSets(patientSets);
    List<Integer> devItems = THYMEData.getDevPatientSets(patientSets);
    List<Integer> testItems = THYMEData.getTestPatientSets(patientSets);
    List<Integer> allTrain = new ArrayList<>(trainItems);
    List<Integer> allTest = null;
    
    if(options.getTest()){
      allTrain.addAll(devItems);
      allTest = new ArrayList<>(testItems);
    }else{
      allTest = new ArrayList<>(devItems);
    }

    EvaluationOfMetaTimeExpressionExtractor eval = new
        EvaluationOfMetaTimeExpressionExtractor(
            new File("target/eval/time-spans"), 
            options.getRawTextDirectory(), 
            options.getXMLDirectory(), 
            options.getXMLFormat(),
            options.getXMIDirectory(), 
            options.getTreebankDirectory(),
            allTrain,
            TimeMention.class);
    AnnotationStatistics<String> stats = eval.trainAndTest(allTrain, allTest);
    System.out.println(stats.toString());
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory)
      throws Exception {
    
    Class<? extends JCasAnnotator_ImplBase>[] annotatorClasses = MetaTimeAnnotator.getComponents();
    
    // add more annotator types?
    Map<Class<? extends JCasAnnotator_ImplBase>, String[]> annotatorTrainingArguments = Maps.newHashMap();
    annotatorTrainingArguments.put(BackwardsTimeAnnotator.class, new String[]{"-c", "0.3"});
    annotatorTrainingArguments.put(TimeAnnotator.class, new String[]{"-c", "0.1"});
    annotatorTrainingArguments.put(ConstituencyBasedTimeAnnotator.class, new String[]{"-c", "0.3"});
    annotatorTrainingArguments.put(CRFTimeAnnotator.class, new String[]{"-p", "c2=0.03"});
    
    JCasIterable[] casIters = new JCasIterable[nFolds];
    for (int fold = 0; fold < nFolds; ++fold) {
      List<Integer> xfoldTrain = selectTrainItems(allTrain, nFolds, fold);
      List<Integer> xfoldTest = selectTestItems(allTrain, nFolds, fold);
      AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
      File modelDirectory = getModelDirectory(new File("target/eval/time-spans/fold_"+fold));
      for (Class<? extends JCasAnnotator_ImplBase> annotatorClass : annotatorClasses) {
        EvaluationOfTimeSpans evaluation = new EvaluationOfTimeSpans(
            new File("target/eval/time-spans/" ),
            this.rawTextDirectory,
            this.xmlDirectory,
            this.xmlFormat,
            this.xmiDirectory,
            this.treebankDirectory,
            1,
            0,
            annotatorClass,
            false,
            annotatorTrainingArguments.get(annotatorClass));
        evaluation.prepareXMIsFor(allTrain);
        String name = String.format("%s.errors", annotatorClass.getSimpleName());
        evaluation.setLogging(Level.FINE, new File("target/eval", name));

        // train on 4 of the folds of the training data:
        evaluation.train(evaluation.getCollectionReader(xfoldTrain), modelDirectory);
        if(fold == 0){
          // train the main model as well:
          evaluation.train(evaluation.getCollectionReader(allTrain), directory);
        }
        
      }
      casIters[fold] = new JCasIterable(getCollectionReader(xfoldTest), aggregateBuilder.createAggregate());
    }
    // run meta data-writer for this fold:
    AggregateBuilder writerBuilder = new AggregateBuilder();
    writerBuilder.add(CopyFromGold.getDescription(TimeMention.class));
    writerBuilder.add(this.getDataWriterDescription(directory));
    AnalysisEngine writer = writerBuilder.createAggregate();
    for(JCasIterable casIter : casIters){
      for(JCas jcas : casIter){
        SimplePipeline.runPipeline(jcas, writer);
      }
    }
    writer.collectionProcessComplete();
    JarClassifierBuilder.trainAndPackage(getModelDirectory(directory), new String[]{"-p", "c2=0.3"});
  }
  
  private static List<Integer> selectTrainItems(List<Integer> items, int numFolds, int fold) {
    List<Integer> trainItems = new ArrayList<>();
    for (int i = 0; i < items.size(); ++i) {
      if (i % numFolds != fold) {
        trainItems.add(items.get(i));
      }
    }
    return trainItems;
  }
  
  private static List<Integer> selectTestItems(List<Integer> items, int numFolds, int fold) {
    List<Integer> trainItems = new ArrayList<>();
    for (int i = 0; i < items.size(); ++i) {
      if (i % numFolds == fold) {
        trainItems.add(items.get(i));
      }
    }
    return trainItems;
  }
  

  @Override
  protected AnalysisEngineDescription getDataWriterDescription(File directory)
      throws ResourceInitializationException {
    return MetaTimeAnnotator.getDataWriterDescription(CRFSuiteStringOutcomeDataWriter.class, directory);          
  }

  @Override
  protected void trainAndPackage(File directory) throws Exception {
    JarClassifierBuilder.trainAndPackage(getModelDirectory(directory), "-p", "c2=0.3");
  }

  @Override
  protected AnalysisEngineDescription getAnnotatorDescription(File directory)
      throws ResourceInitializationException {
    return MetaTimeAnnotator.getAnnotatorDescription(directory);
  }

  @Override
  protected Collection<? extends Annotation> getGoldAnnotations(JCas jCas,
      Segment segment) {
    return selectExact(jCas, TimeMention.class, segment);
  }

  @Override
  protected Collection<? extends Annotation> getSystemAnnotations(JCas jCas,
      Segment segment) {
    return selectExact(jCas, TimeMention.class, segment);
  }

  private static File getModelDirectory(File directory) {
    return new File(directory, "MetaTimeAnnotator");
  }

}
