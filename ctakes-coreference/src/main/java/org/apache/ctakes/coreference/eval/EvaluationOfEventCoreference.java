package org.apache.ctakes.coreference.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.assertion.medfacts.cleartk.GenericCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.HistoryCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.SubjectCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.UncertaintyCleartkAnalysisEngine;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.ctakes.core.patient.PatientNoteCollector;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.coreference.ae.*;
import org.apache.ctakes.coreference.factory.CoreferenceAnnotatorFactory;
import org.apache.ctakes.coreference.flow.CoreferenceFlowController;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.DocTimeRelAnnotator;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.ctakes.temporal.ae.ThymePatientViewAnnotator;
import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.temporal.eval.EvaluationOfTemporalRelations_ImplBase;
import org.apache.ctakes.temporal.utils.PatientViewsUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.structured.DocumentIdPrefix;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.FileUtils;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.cleartk.ml.svmlight.rank.SvmLightRankDataWriter;
import org.cleartk.ml.tksvmlight.model.CompositeKernel.ComboOperator;
import org.cleartk.util.ViewUriUtil;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import de.bwaldvogel.liblinear.FeatureNode;

public class EvaluationOfEventCoreference extends EvaluationOfTemporalRelations_ImplBase {
 

  static interface CoreferenceOptions extends TempRelOptions{
    @Option
    public String getOutputDirectory();
    
    @Option
    public boolean getUseTmp();
    
    @Option
    public boolean getTestOnTrain();
    
    @Option(longName="external")
    public boolean getUseExternalScorer();
    
    @Option(shortName="t", defaultValue={"MENTION_CLUSTER"})
    public EVAL_SYSTEM getEvalSystem();
    
    @Option(shortName="c", defaultValue="default")
    public String getConfig();
    
    @Option(shortName="s")
    public String getScorerPath();
    
    @Option
    public boolean getGoldMarkables();
    
    @Option
    public boolean getSkipTest();
  }
  
  private static Logger logger = Logger.getLogger(EvaluationOfEventCoreference.class);
  public static float COREF_PAIRS_DOWNSAMPLE = 0.5f;
  public static float COREF_CLUSTER_DOWNSAMPLE=0.5f;
  private static final int NUM_SAMPLES = 0;
  private static final double DROPOUT_RATE = 0.1;
  
  protected static ParameterSettings pairwiseParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, COREF_PAIRS_DOWNSAMPLE, "linear",
      1.0, 1.0, "linear", ComboOperator.SUM, 0.1, 0.5);
  protected static ParameterSettings clusterParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, COREF_CLUSTER_DOWNSAMPLE, "linear",
      1.0, 1.0, "linear", ComboOperator.SUM, 0.1, 0.5);
  
  private static String goldOut = "";
  private static String systemOut = "";
  
  public static void main(String[] args) throws Exception {
    CoreferenceOptions options = CliFactory.parseArguments(CoreferenceOptions.class, args);

    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = getTrainItems(options);
    List<Integer> testItems = options.getTestOnTrain() ? getTrainItems(options) : getTestItems(options);

    ParameterSettings params = options.getEvalSystem() == EVAL_SYSTEM.MENTION_PAIR ? pairwiseParams : clusterParams;
    
    File workingDir = new File("target/eval/temporal-relations/coreference/" + options.getEvalSystem() + File.separator +  options.getConfig());
    if(!workingDir.exists()) workingDir.mkdirs();
    if(options.getUseTmp()){
      File tempModelDir = File.createTempFile("temporal", null, workingDir);
      tempModelDir.delete();
      tempModelDir.mkdir();
      workingDir = tempModelDir;
    }
    EvaluationOfEventCoreference eval = new EvaluationOfEventCoreference(
        workingDir,
        options.getRawTextDirectory(),
        options.getXMLDirectory(),
        options.getXMLFormat(),
        options.getSubcorpus(),
        options.getXMIDirectory(),
        options.getTreebankDirectory(),
        options.getPrintErrors(),
        options.getPrintFormattedRelations(),
        params,
        options.getKernelParams(),
        options.getOutputDirectory());

    eval.skipTrain = options.getSkipTrain();
    eval.skipWrite = options.getSkipDataWriting();
    eval.skipTest = options.getSkipTest();
    eval.goldMarkables = options.getGoldMarkables();
    eval.evalType = options.getEvalSystem();
    eval.config = options.getConfig();
    goldOut = "gold." + eval.config + ".conll";
    systemOut = "system." + eval.config + ".conll";
    
    eval.prepareXMIsFor(patientSets);
    
    params.stats = eval.trainAndTest(trainItems, testItems);//training);//

    if(options.getUseTmp()){
      FileUtils.deleteRecursive(workingDir);
    }
    
    if(options.getUseExternalScorer() && !options.getSkipTest()){
      Pattern patt = Pattern.compile("(?:Coreference|BLANC): Recall: \\([^\\)]*\\) (\\S+)%.*Precision: \\([^\\)]*\\) (\\S+)%.*F1: (\\S+)%");
      Runtime runtime = Runtime.getRuntime();
      String cmd = String.format("perl %s all %s %s none", 
          options.getScorerPath(), 
          options.getOutputDirectory() + File.separator + goldOut, 
          options.getOutputDirectory() + File.separator + systemOut);
      System.out.println("Running official scoring tool with command: " + cmd);
      Process p = runtime.exec(cmd.split(" "));
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line, metric=null;
      System.out.println(String.format("%10s%7s%7s%7s", "Metric", "Rec", "Prec", "F1"));
      Map<String,Double> scores = new HashMap<>();
      while((line = reader.readLine()) != null){
        line = line.trim();
        if(line.startsWith("METRIC")){
          metric = line.substring(7);  // everything after "METRIC"
          metric = metric.substring(0, metric.length()-1);  // remove colon from the end
        }else if(line.startsWith("Coreference")){
          Matcher m = patt.matcher(line);
          if(m.matches()){
            System.out.println(String.format("%10s%7.2f%7.2f%7.2f", metric, Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))));
            scores.put(metric, Double.parseDouble(m.group(3)));
          }
        }
      }
      
      if(scores.containsKey("muc") && scores.containsKey("bcub") && scores.containsKey("ceafe")){
        double conll = (scores.get("muc") + scores.get("bcub") + scores.get("ceafe")) / 3.0;
        System.out.println(String.format("%10s              %7.2f", "Conll", conll));
      }
    }
  }
  
  boolean skipTrain=false; 
  boolean skipWrite=false;
  boolean skipTest=false;
  boolean goldMarkables=false;
  public enum EVAL_SYSTEM { BASELINE, MENTION_PAIR, MENTION_CLUSTER, CLUSTER_RANK, PERSON_ONLY };
  EVAL_SYSTEM evalType;
  String config=null;
  
  private String outputDirectory;
  
  public EvaluationOfEventCoreference(File baseDirectory,
      File rawTextDirectory, File xmlDirectory,
      org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat xmlFormat, Subcorpus subcorpus,
      File xmiDirectory, File treebankDirectory, boolean printErrors,
      boolean printRelations, ParameterSettings params, String cmdParams, String outputDirectory) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory,
        treebankDirectory, printErrors, printRelations, params);
    this.outputDirectory = outputDirectory;
    this.kernelParams = cmdParams == null ? null : cmdParams.replace("\"", "").split(" ");
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory)
      throws Exception {
    if(skipTrain) return;
    if(this.evalType == EVAL_SYSTEM.BASELINE || this.evalType == EVAL_SYSTEM.PERSON_ONLY) return;
    if(!skipWrite){
//      ExternalResourceDescription depParserExtDesc = ExternalResourceFactory.createExternalResourceDescription(DependencySharedModel.class,
//          DependencySharedModel.DEFAULT_MODEL_FILE_NAME);

      // need this mapping for the document-aware coref module to map all gold views to system views during training.
      AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DocumentIDPrinter.class));
      aggregateBuilder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
      aggregateBuilder.add(UncertaintyCleartkAnalysisEngine.createAnnotatorDescription());
      aggregateBuilder.add(GenericCleartkAnalysisEngine.createAnnotatorDescription());
      aggregateBuilder.add(HistoryCleartkAnalysisEngine.createAnnotatorDescription());
      aggregateBuilder.add(SubjectCleartkAnalysisEngine.createAnnotatorDescription());

      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ViewCreatorAnnotator.class, ViewCreatorAnnotator.PARAM_VIEW_NAME, "Baseline"));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ParagraphAnnotator.class));
      //      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ParagraphVectorAnnotator.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RelationPropagator.class));
      aggregateBuilder.add(EventAnnotator.createAnnotatorDescription());
      aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar"));
      aggregateBuilder.add(DocTimeRelAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar"));
      if(this.goldMarkables){
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyGoldMarkablesInChains.class));
      }else{
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DeterministicMarkableAnnotator.class));
        //    aggregateBuilder.add(CopyFromGold.getDescription(/*Markable.class,*/ CoreferenceRelation.class, CollectionTextRelation.class));
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemovePersonMarkables.class));
      }
      // MarkableHeadTreeCreator creates a cache of mappings from Markables to dependency heads since so many feature extractors use that information
      // major speedup
//      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(MarkableHeadTreeCreator.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyCoreferenceRelations.class), CopyCoreferenceRelations.PARAM_GOLD_VIEW, GOLD_VIEW_NAME);
      // the coreference module uses segments to index markables, but we don't have them in the gold standard.
      aggregateBuilder.add(CopyFromSystem.getDescription(Segment.class), GOLD_VIEW_NAME, GOLD_VIEW_NAME);

      aggregateBuilder.add(MarkableSalienceAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/salience/model.jar"));
      if(this.evalType == EVAL_SYSTEM.MENTION_PAIR){
        aggregateBuilder.add(EventCoreferenceAnnotator.createDataWriterDescription(
            //        TKSVMlightStringOutcomeDataWriter.class,
            FlushingDataWriter.class,
            //            LibSvmStringOutcomeDataWriter.class,
            //            TkLibSvmStringOutcomeDataWriter.class,
            directory,
            params.probabilityOfKeepingANegativeExample
            ));
        Logger.getLogger(EventCoreferenceAnnotator.class).setLevel(Level.WARN);
      }else if(this.evalType == EVAL_SYSTEM.MENTION_CLUSTER){
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientNoteCollector.class));
        aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
                PatientMentionClusterCoreferencer.class,
                CleartkAnnotator.PARAM_IS_TRAINING,
                true,
                MentionClusterCoreferenceAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
                params.probabilityOfKeepingANegativeExample,
                DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
                FlushingDataWriter.class,
                DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
                directory,
                MentionClusterCoreferenceAnnotator.PARAM_SINGLE_DOCUMENT,
                false));
      }else if(this.evalType == EVAL_SYSTEM.CLUSTER_RANK){
        aggregateBuilder.add(MentionClusterRankingCoreferenceAnnotator.createDataWriterDescription(
            SvmLightRankDataWriter.class,
            directory,
            params.probabilityOfKeepingANegativeExample));
      }else{
        logger.warn("Encountered a training configuration that does not add an annotator: " + this.evalType);
      }

      // If we are using mention-cluster algorithm, it is aware of multiple documents so we only have to call it once.
      //      FlowControllerDescription corefFlowControl = FlowControllerFactory.createFlowControllerDescription(CoreferenceFlowController.class);
      //      aggregateBuilder.setFlowControllerDescription(corefFlowControl);
      AnalysisEngineDescription aed = aggregateBuilder.createAggregateDescription();
      SimplePipeline.runPipeline(collectionReader, AnalysisEngineFactory.createEngine(aed));
    }
    String[] optArray;

    if(this.kernelParams == null){
      ArrayList<String> svmOptions = new ArrayList<>();
      svmOptions.add("-c"); svmOptions.add(""+params.svmCost);        // svm cost
      svmOptions.add("-t"); svmOptions.add(""+params.svmKernelIndex); // kernel index 
      svmOptions.add("-d"); svmOptions.add("3");                      // degree parameter for polynomial
      svmOptions.add("-g"); svmOptions.add(""+params.svmGamma);
      if(params.svmKernelIndex==ParameterSettings.SVM_KERNELS.indexOf("tk")){
        svmOptions.add("-S"); svmOptions.add(""+params.secondKernelIndex);   // second kernel index (similar to -t) for composite kernel
        String comboFlag = (params.comboOperator == ComboOperator.SUM ? "+" : params.comboOperator == ComboOperator.PRODUCT ? "*" : params.comboOperator == ComboOperator.TREE_ONLY ? "T" : "V");
        svmOptions.add("-C"); svmOptions.add(comboFlag);
        svmOptions.add("-L"); svmOptions.add(""+params.lambda);
        svmOptions.add("-T"); svmOptions.add(""+params.tkWeight);
        svmOptions.add("-N"); svmOptions.add("3");   // normalize trees and features
      }
      optArray = svmOptions.toArray(new String[]{});
    }else{
      optArray = this.kernelParams;
    }
    JarClassifierBuilder.trainAndPackage(directory, optArray);
  }

  @Override
  protected AnnotationStatistics<String> test(
      CollectionReader collectionReader, File directory) throws Exception {
    AnnotationStatistics<String> corefStats = new AnnotationStatistics<>();
    AnnotationStatistics<String> mentionStats = new AnnotationStatistics<>();
    
    if(this.skipTest){
      logger.info("Skipping test");
      return corefStats;
    }
    AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DocumentIdFromURI.class));
    aggregateBuilder.add("Patient id printer", AnalysisEngineFactory.createEngineDescription(DocumentIDPrinter.class));
//      AggregateBuilder singleNoteBuilder = new AggregateBuilder();
    aggregateBuilder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(UncertaintyCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(GenericCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(HistoryCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(SubjectCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ParagraphAnnotator.class));
    //    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ParagraphVectorAnnotator.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RelationPropagator.class));
    aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar"));
    aggregateBuilder.add(EventAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(DocTimeRelAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar"));
//      singleNoteBuilder.add(AnalysisEngineFactory.createEngineDescription(CoreferenceChainScoringOutput.class,
//          ConfigParameterConstants.PARAM_OUTPUTDIR,
//          this.outputDirectory + File.separator + goldOut,
//          CoreferenceChainScoringOutput.PARAM_GOLD_VIEW_NAME,
//          goldViewName),
//          CAS.NAME_DEFAULT_SOFA,
//          viewName);
    if(this.goldMarkables){
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CopyGoldMarkablesInChains.class)); //CopyFromGold.getDescription(Markable.class));
    }else{
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DeterministicMarkableAnnotator.class));
      //    aggregateBuilder.add(CopyFromGold.getDescription(/*Markable.class,*/ CoreferenceRelation.class, CollectionTextRelation.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemovePersonMarkables.class));
    }
//    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(MarkableHeadTreeCreator.class));
    aggregateBuilder.add(MarkableSalienceAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/salience/model.jar"));
    if(this.evalType == EVAL_SYSTEM.MENTION_CLUSTER) {
      // Do nothing but we still need this here so the else clause works right
//        singleNoteBuilder.add(MentionClusterCoreferenceAnnotator.createAnnotatorDescription(directory.getAbsolutePath() + File.separator + "model.jar"), CAS.NAME_DEFAULT_SOFA, viewName);
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientNoteCollector.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientNoteCollector.class), CAS.NAME_DEFAULT_SOFA, GOLD_VIEW_NAME);
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientMentionClusterCoreferencer.class,
              CleartkAnnotator.PARAM_IS_TRAINING,
              false,
              GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
              directory.getAbsolutePath() + File.separator + "model.jar",
              MentionClusterCoreferenceAnnotator.PARAM_SINGLE_DOCUMENT,
              false));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientPersonChainAnnotator.class));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientScoringWriter.class,
              ConfigParameterConstants.PARAM_OUTPUTDIR,
              this.outputDirectory + File.separator + goldOut,
              CoreferenceChainScoringOutput.PARAM_GOLD_VIEW_NAME,
              GOLD_VIEW_NAME));
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PatientScoringWriter.class,
              ConfigParameterConstants.PARAM_OUTPUTDIR,
              this.outputDirectory + File.separator + systemOut));
    }else{
      if(!this.goldMarkables){
        aggregateBuilder.add(PersonChainAnnotator.createAnnotatorDescription());
      }
      if(this.evalType == EVAL_SYSTEM.MENTION_PAIR){
        aggregateBuilder.add(EventCoreferenceAnnotator.createAnnotatorDescription(directory.getAbsolutePath() + File.separator + "model.jar"));
      }else if(this.evalType == EVAL_SYSTEM.CLUSTER_RANK){
        aggregateBuilder.add(MentionClusterRankingCoreferenceAnnotator.createAnnotatorDescription(directory.getAbsolutePath() + File.separator + "model.jar"));
      }else if(this.evalType == EVAL_SYSTEM.BASELINE){
        aggregateBuilder.add(CoreferenceAnnotatorFactory.getLegacyCoreferencePipeline());
      }else{
        logger.info("Running an evaluation that does not add an annotator: " + this.evalType);
      }
    }
    //    aggregateBuilder.add(CoreferenceChainAnnotator.createAnnotatorDescription());

//    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CoreferenceChainScoringOutput.class,
//        ConfigParameterConstants.PARAM_OUTPUTDIR,
//        this.outputDirectory + File.separator + goldOut,
//            CoreferenceChainScoringOutput.PARAM_GOLD_VIEW_NAME,
//            GOLD_VIEW_NAME));
//    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CoreferenceChainScoringOutput.class,
//        ConfigParameterConstants.PARAM_OUTPUTDIR,
//        this.outputDirectory + File.separator + systemOut));

//    FlowControllerDescription corefFlowControl = FlowControllerFactory.createFlowControllerDescription(CoreferenceFlowController.class);
//    aggregateBuilder.setFlowControllerDescription(corefFlowControl);
//    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(XMIWriter.class));
    Function<CoreferenceRelation, ?> getSpan = new Function<CoreferenceRelation, HashableArguments>() {
      public HashableArguments apply(CoreferenceRelation relation) {
        return new HashableArguments(relation);
      }
    };
    Function<CoreferenceRelation, String> getOutcome = new Function<CoreferenceRelation,String>() {
      public String apply(CoreferenceRelation relation){
        return "Coreference";
      }
    };
     
    for(Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();){
      JCas jCas = casIter.next();
      JCas goldView = jCas.getView(GOLD_VIEW_NAME);

      Collection<CoreferenceRelation> goldRelations = JCasUtil.select(
          goldView,
          CoreferenceRelation.class);
      Collection<CoreferenceRelation> systemRelations = JCasUtil.select(
          jCas,
          CoreferenceRelation.class);
      corefStats.add(goldRelations, systemRelations, getSpan, getOutcome);
      mentionStats.add(JCasUtil.select(goldView,  Markable.class), JCasUtil.select(jCas, Markable.class));

      if(this.printErrors){
        Map<HashableArguments, BinaryTextRelation> goldMap = Maps.newHashMap();
        for (BinaryTextRelation relation : goldRelations) {
          goldMap.put(new HashableArguments(relation), relation);
        }
        Map<HashableArguments, BinaryTextRelation> systemMap = Maps.newHashMap();
        for (BinaryTextRelation relation : systemRelations) {
          systemMap.put(new HashableArguments(relation), relation);
        }
        Set<HashableArguments> all = Sets.union(goldMap.keySet(), systemMap.keySet());
        List<HashableArguments> sorted = Lists.newArrayList(all);
        Collections.sort(sorted);
        for (HashableArguments key : sorted) {
          BinaryTextRelation goldRelation = goldMap.get(key);
          BinaryTextRelation systemRelation = systemMap.get(key);
          if (goldRelation == null) {
            System.out.println("System added: " + formatRelation(systemRelation));
          } else if (systemRelation == null) {
            System.out.println("System dropped: " + formatRelation(goldRelation));
          } else if (!systemRelation.getCategory().equals(goldRelation.getCategory())) {
            String label = systemRelation.getCategory();
            System.out.printf("System labeled %s for %s\n", label, formatRelation(goldRelation));
          } else{
            System.out.println("Nailed it! " + formatRelation(systemRelation));
          }
        }
      }
    }
    System.out.println(String.format("P=%f, R=%f, F=%f", mentionStats.precision(), mentionStats.recall(), mentionStats.f1()));
    return corefStats;
  }
  
  protected AggregateBuilder getXMIWritingPreprocessorAggregateBuilder()
      throws Exception {
    
    AggregateBuilder preprocess = new AggregateBuilder();
    
    // create URI views for each note:
//    preprocess.add(AnalysisEngineFactory.createEngineDescription( ThymePatientViewAnnotator.class));
    
    // Then run the preprocessing engine on all views
    preprocess.add(AnalysisEngineFactory.createEngineDescription( UriToDocumentTextAnnotatorCtakes.class ));
    preprocess.add(AnalysisEngineFactory.createEngineDescription(DocumentIdFromURI.class));

    preprocess.add(getLinguisticProcessingDescription());
    // Mapping explanation: Grab the text from the specific document URI and write to the gold view for this document
    preprocess.add(getGoldWritingAggregate(GOLD_VIEW_NAME));

    // write out the CAS after all the above annotations
    preprocess.add( AnalysisEngineFactory.createEngineDescription(
        XMIWriter.class,
        XMIWriter.PARAM_XMI_DIRECTORY,
        this.xmiDirectory ) );

//    preprocess.setFlowControllerDescription(FlowControllerFactory.createFlowControllerDescription(CoreferenceFlowController.class));
    return preprocess;
  }
  
  protected AggregateBuilder getXMIReadingPreprocessorAggregateBuilder() throws UIMAException {
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        XMIReader.class,
        XMIReader.PARAM_XMI_DIRECTORY,
        this.xmiDirectory ) );
    return aggregateBuilder;
  }

  public static class NewDocSentinelAnnotator extends NoOpAnnotator {
    // Purposefully empty; doesn't do anything
  }
  
  public static class EndDocsSentinelAnnotator extends NoOpAnnotator {
    // Purposefully empty; doesn't do anything
  }

  public static class AnnotationComparator implements Comparator<Annotation> {

    @Override
    public int compare(Annotation o1, Annotation o2) {
      if(o1.getBegin() < o2.getBegin()){
        return -1;
      }else if(o1.getBegin() == o2.getBegin() && o1.getEnd() < o2.getEnd()){
        return -1;
      }else if(o1.getBegin() == o2.getBegin() && o1.getEnd() > o2.getEnd()){
        return 1;
      }else if(o2.getBegin() < o1.getBegin()){
        return 1;
      }else{
        return 0;
      }
    }
  }
  public static class DocumentIDPrinter extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    static Logger logger = Logger.getLogger(DocumentIDPrinter.class);
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      String docId = DocumentIDAnnotationUtil.getDocumentID(jCas);
      if(docId.startsWith(DocumentIDAnnotationUtil.NO_DOCUMENT_ID)){
        docId = new File(ViewUriUtil.getURI(jCas)).getName();
      }
      logger.info(String.format("Processing %s\n", docId));
    }
    
  }

  @PipeBitInfo(
        name = "Gold Markables Copier",
        description = "Copies Markables from the Gold view to the System view.",
        role = PipeBitInfo.Role.SPECIAL,
        dependencies = { PipeBitInfo.TypeProduct.MARKABLE }
  )
  public static class CopyGoldMarkablesInChains extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      JCas goldView, systemView;
      try {
        goldView = jCas.getView( GOLD_VIEW_NAME );
        systemView = jCas.getView( CAS.NAME_DEFAULT_SOFA );
      } catch ( CASException e ) {
        throw new AnalysisEngineProcessException( e );
      }
      // first remove any system markables that snuck in
      for ( Markable annotation : Lists.newArrayList( JCasUtil.select( systemView, Markable.class ) ) ) {
        annotation.removeFromIndexes();
      }

      CasCopier copier = new CasCopier( goldView.getCas(), systemView.getCas() );
      Feature sofaFeature = jCas.getTypeSystem().getFeatureByFullName( CAS.FEATURE_FULL_NAME_SOFA );
      HashSet<String> existingSpans = new HashSet<>();
      for ( CollectionTextRelation chain : JCasUtil.select(goldView, CollectionTextRelation.class)){
        for ( Markable markable : JCasUtil.select(chain.getMembers(), Markable.class)){
          // some spans are annotated twice erroneously in gold -- if we can't fix make sure we don't add twice
          // or else the evaluation script will explode.
          String key = markable.getBegin() + "-" + (markable.getEnd() - markable.getBegin());
          if(existingSpans.contains(key)) continue;
          
          Markable copy = (Markable)copier.copyFs( markable );
          copy.setFeatureValue( sofaFeature, systemView.getSofa() );
          copy.addToIndexes( systemView );
          existingSpans.add(key);
        }
      }
    }
      
    
  }
  /*
   * The Relation extractors all create relation objects but don't populate the objects inside of them
   * with pointers to the relation.
   */
  public static class RelationPropagator extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      for(LocationOfTextRelation locRel : JCasUtil.select(jcas, LocationOfTextRelation.class)){
        IdentifiedAnnotation arg1 = (IdentifiedAnnotation) locRel.getArg1().getArgument();
        IdentifiedAnnotation arg2 = (IdentifiedAnnotation) locRel.getArg2().getArgument();
        // have to do this 3 different times because there is no intermediate class between EventMention and
        // the three types that can have locations that has that location attribute.
        // for the case where there are 2 locations, we take the one whose anatomical site argument
        // has the the longer span assuming it is more specific
        if(arg1 instanceof ProcedureMention){
          ProcedureMention p = ((ProcedureMention)arg1);
          if(p.getBodyLocation() == null){
            p.setBodyLocation(locRel);
          }else{
            Annotation a = p.getBodyLocation().getArg2().getArgument();
            int oldSize = a.getEnd() - a.getBegin();
            int newSize = arg2.getEnd() - arg2.getEnd();
            if(newSize > oldSize){
              p.setBodyLocation(locRel);
            }
          }
        }else if(arg1 instanceof DiseaseDisorderMention){
          DiseaseDisorderMention d = (DiseaseDisorderMention)arg1;
          if(d.getBodyLocation() == null){
            d.setBodyLocation(locRel);
          }else{
            Annotation a = d.getBodyLocation().getArg2().getArgument();
            int oldSize = a.getEnd() - a.getBegin();
            int newSize = arg2.getEnd() - arg2.getEnd();
            if(newSize > oldSize){
              d.setBodyLocation(locRel);
            }
          }
        }else if(arg1 instanceof SignSymptomMention){
          SignSymptomMention s = (SignSymptomMention)arg1;
          if(s.getBodyLocation() == null){
            s.setBodyLocation(locRel);
          }else{
            Annotation a = s.getBodyLocation().getArg2().getArgument();
            int oldSize = a.getEnd() - a.getBegin();
            int newSize = arg2.getEnd() - arg2.getEnd();
            if(newSize > oldSize){
              s.setBodyLocation(locRel);
            }
          }          
        }
      }
    }
    
  }
  
  public static class ParagraphAnnotator extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      List<BaseToken> tokens = new ArrayList<>(JCasUtil.select(jcas, BaseToken.class));
      BaseToken lastToken = null;
      int parStart = 0;
      
      for(int i = 0; i < tokens.size(); i++){
        BaseToken token = tokens.get(i);
        if(parStart == i && token instanceof NewlineToken){
          // we've just created a pargraph ending but there were multiple newlines -- don't want to start the
          // new paragraph until we are past the newlines -- increment the parStart index and move forward
          parStart++;
        }else if(lastToken != null && token instanceof NewlineToken){
          Paragraph par = new Paragraph(jcas, tokens.get(parStart).getBegin(), lastToken.getEnd());
          par.addToIndexes();
          parStart = i+1;
        }
        lastToken = token;
      }
      
    }
    
  }
  
  
  public static class ParagraphVectorAnnotator extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    WordEmbeddings words = null;

    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException{
      try {
        words = WordVectorReader.getEmbeddings(FileLocator.getAsStream("org/apache/ctakes/coreference/distsem/mimic_vectors.txt"));
      } catch (IOException e) {
        e.printStackTrace();
        throw new ResourceInitializationException(e);
      }
    }
    
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      List<Paragraph> pars = new ArrayList<>(JCasUtil.select(jcas, Paragraph.class));
      FSArray parVecs = new FSArray(jcas, pars.size());
      for(int parNum = 0; parNum < pars.size(); parNum++){
        Paragraph par = pars.get(parNum);
        float[] parVec = new float[words.getDimensionality()];

        List<BaseToken> tokens = JCasUtil.selectCovered(BaseToken.class, par);
        for(int i = 0; i < tokens.size(); i++){
          BaseToken token = tokens.get(i);
          if(token instanceof WordToken){
            String word = token.getCoveredText().toLowerCase();
            if(words.containsKey(word)){
              WordVector wv = words.getVector(word);
              for(int j = 0; j < parVec.length; j++){
                parVec[j] += wv.getValue(j);
              }
            }          
          }
        }
        normalize(parVec);
        FloatArray vec = new FloatArray(jcas, words.getDimensionality());
        vec.copyFromArray(parVec, 0, 0, parVec.length);
        vec.addToIndexes();
        parVecs.set(parNum, vec);
      }
      parVecs.addToIndexes();
    }

    private static final void normalize(float[] vec) {
      double sum = 0.0;
      for(int i = 0; i < vec.length; i++){
        sum += (vec[i]*vec[i]);
      }
      sum = Math.sqrt(sum);
      for(int i = 0; i < vec.length; i++){
        vec[i] /= sum;
      }
    }
  }

  @PipeBitInfo(
        name = "Coreference Copier",
        description = "Sets Modality based upon context.",
        role = PipeBitInfo.Role.SPECIAL,
        dependencies = { PipeBitInfo.TypeProduct.MARKABLE, PipeBitInfo.TypeProduct.COREFERENCE_RELATION }
  )
  public static class CopyCoreferenceRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    // TODO - make document aware for mention-cluster coreference? Not as easy as relation remover because this should work for
    // non-document-aware annotators.
    public static final String PARAM_GOLD_VIEW = "GoldViewName";
    @ConfigurationParameter(name=PARAM_GOLD_VIEW, mandatory=false, description="View containing gold standard annotations")
    private String goldViewName=GOLD_VIEW_NAME;
    
    public static final String PARAM_DROP_ELEMENTS = "Dropout";
    @ConfigurationParameter(name = PARAM_DROP_ELEMENTS, mandatory=false)
    private boolean dropout = false;

    @SuppressWarnings("synthetic-access")
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      JCas goldView = null;
      try {
        goldView = jcas.getView(goldViewName);
      } catch (CASException e) {
        e.printStackTrace();
        throw new AnalysisEngineProcessException(e);
      }
      
      HashMap<Markable,Markable> gold2sys = new HashMap<>();
      Map<ConllDependencyNode,Collection<Markable>> depIndex = JCasUtil.indexCovering(jcas, ConllDependencyNode.class, Markable.class);
      // remove those with removed markables (person mentions)
      List<CollectionTextRelation> toRemove = new ArrayList<>();
      
      for(CollectionTextRelation goldChain : JCasUtil.select(goldView, CollectionTextRelation.class)){
        FSList head = goldChain.getMembers();
//        NonEmptyFSList sysList = new NonEmptyFSList(jcas);
//        NonEmptyFSList listEnd = sysList;
        List<List<Markable>> systemLists = new ArrayList<>(); // the gold list can be split up into many lists if we allow dropout.
        boolean removeChain = false;
        List<Markable> prevList = null;
        
        // first one is guaranteed to be nonempty otherwise it would not be in cas
        do{
          NonEmptyFSList element = (NonEmptyFSList) head;
          Markable goldMarkable = (Markable) element.getHead();
          if(goldMarkable == null){
            logger.error(String.format("Found an unexpected null gold markable"));
          }
          boolean mapped = mapGoldMarkable(jcas, goldMarkable, gold2sys, depIndex);
          
          // if we can't align the gold markable with one in the system cas then don't add it:
          if(!mapped){
            String text = "<Out of bounds>";
            if(!(goldMarkable.getBegin() < 0 || goldMarkable.getEnd() >= jcas.getDocumentText().length())){
              text = goldMarkable.getCoveredText();
            }
            logger.warn(String.format("There is a gold markable %s [%d, %d] which could not map to a system markable.", 
                text, goldMarkable.getBegin(), goldMarkable.getEnd()));
            removeChain = true;
            break;
          }
          
          Markable sysMarkable = gold2sys.get(goldMarkable);
          if(!dropout || systemLists.size() == 0){
            if(systemLists.size() == 0) systemLists.add(new ArrayList<>());
            systemLists.get(0).add(sysMarkable);
//            prevList = systemLists.get(0);
//            // if this is not first time through move listEnd to end.
//            if(listEnd.getHead() != null){
//              listEnd.setTail(new NonEmptyFSList(jcas));
//              listEnd.addToIndexes();
//              listEnd = (NonEmptyFSList) listEnd.getTail();
//            }
//
//            // add markable to end of list:
//            listEnd.setHead(gold2sys.get(goldMarkable));
          }else{
            // 3 options: Do correctly (append to same list as last element), ii) Start its own list, iii) Randomly join another list
            if(Math.random() > DROPOUT_RATE){
              // most of the time do the right thing:
              systemLists.get(0).add(sysMarkable);
            }else{
              int listIndex = (int) Math.ceil(Math.random() * systemLists.size());
              if(listIndex == systemLists.size()){
                systemLists.add(new ArrayList<>());
              }
              systemLists.get(listIndex).add(sysMarkable);
            }
          }
          head = element.getTail();
        }while(head instanceof NonEmptyFSList);
        
        // don't bother copying over -- the gold chain was of person mentions
        if(!removeChain){
//          listEnd.setTail(new EmptyFSList(jcas));
//          listEnd.addToIndexes();
//          listEnd.getTail().addToIndexes();
//          sysList.addToIndexes();
          for(List<Markable> chain : systemLists){
            if(chain.size() > 1){
              CollectionTextRelation sysRel = new CollectionTextRelation(jcas);
              sysRel.setMembers(ListFactory.buildList(jcas, chain));
              sysRel.addToIndexes();
            }
          }
        }
      }
      
      for(CoreferenceRelation goldRel : JCasUtil.select(goldView, CoreferenceRelation.class)){
        if((gold2sys.containsKey(goldRel.getArg1().getArgument()) && gold2sys.containsKey(goldRel.getArg2().getArgument()))){
          CoreferenceRelation sysRel = new CoreferenceRelation(jcas);
          sysRel.setCategory(goldRel.getCategory());
          sysRel.setDiscoveryTechnique(CONST.REL_DISCOVERY_TECH_GOLD_ANNOTATION);

          RelationArgument arg1 = new RelationArgument(jcas);
          arg1.setArgument(gold2sys.get(goldRel.getArg1().getArgument()));
          sysRel.setArg1(arg1);
          arg1.addToIndexes();

          RelationArgument arg2 = new RelationArgument(jcas);
          arg2.setArgument(gold2sys.get(goldRel.getArg2().getArgument()));
          sysRel.setArg2(arg2);
          arg2.addToIndexes();         
          
          sysRel.addToIndexes();        
        }
      }
    }
    
    private static boolean mapGoldMarkable(JCas jcas, Markable goldMarkable, Map<Markable,Markable> gold2sys, Map<ConllDependencyNode, Collection<Markable>> depIndex){
      if(!(goldMarkable.getBegin() < 0 || goldMarkable.getEnd() >= jcas.getDocumentText().length())){
        
        
        ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, goldMarkable);

        for(Markable sysMarkable : depIndex.get(headNode)){
          ConllDependencyNode markNode = DependencyUtility.getNominalHeadNode(jcas, sysMarkable);
          if(markNode == headNode){
            gold2sys.put(goldMarkable, sysMarkable);
            return true;
          }
        }
      }else{
        // Have seen some instances where anafora writes a span that is not possible, log them
        // so they can be found and fixed:
        logger.warn(String.format("There is a markable with span [%d, %d] in a document with length %d\n", 
            goldMarkable.getBegin(), goldMarkable.getEnd(), jcas.getDocumentText().length()));
        return false;
      }
      return false;
    }
  }
  
  public static class RemoveAllCoreferenceAnnotations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    // TODO - make document aware so it can run with mention-cluster as intended
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      List<CollectionTextRelation> chains = new ArrayList<>(JCasUtil.select(jcas, CollectionTextRelation.class));
      for(CollectionTextRelation chain : chains){
        NonEmptyFSList head = null;
        FSList nextHead = chain.getMembers();
        do{
          head = (NonEmptyFSList) nextHead;
          head.removeFromIndexes();
          nextHead = head.getTail();
        }while(nextHead instanceof NonEmptyFSList);
        chain.removeFromIndexes();
      }
      List<CoreferenceRelation> rels = new ArrayList<>(JCasUtil.select(jcas, CoreferenceRelation.class));
      for(CoreferenceRelation rel : rels){
        rel.getArg1().removeFromIndexes();
        rel.getArg2().removeFromIndexes();
        rel.removeFromIndexes();
      }
    }    
  }
  
  public static class RemovePersonMarkables extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
//      JCas systemView=null, goldView=null;
//      try{
//        systemView = jcas.getView(CAS.NAME_DEFAULT_SOFA);
//        goldView = jcas.getView(GOLD_VIEW_NAME);
//      }catch(Exception e){
//        throw new AnalysisEngineProcessException(e);
//      }
      List<Markable> toRemove = new ArrayList<>();
      for(Markable markable : JCasUtil.select(jcas, Markable.class)){
        if(markable.getCoveredText().equals("I")){
          System.err.println("Unauthorized markable 'I'");
        }
        List<BaseToken> coveredTokens = JCasUtil.selectCovered(jcas, BaseToken.class, markable);
        if(coveredTokens.size() == 1 && coveredTokens.get(0).getPartOfSpeech() != null &&
            coveredTokens.get(0).getPartOfSpeech().startsWith("PRP") &&
            !markable.getCoveredText().toLowerCase().equals("it")){
          toRemove.add(markable);
        }else if(coveredTokens.size() > 0 && (coveredTokens.get(0).getCoveredText().startsWith("Mr.") || coveredTokens.get(0).getCoveredText().startsWith("Dr.") ||
                coveredTokens.get(0).getCoveredText().startsWith("Mrs.") || coveredTokens.get(0).getCoveredText().startsWith("Ms."))){
          toRemove.add(markable);
        }else if(markable.getCoveredText().toLowerCase().endsWith("patient") || markable.getCoveredText().toLowerCase().equals("pt")){
          toRemove.add(markable);
        }
      }
      
      for(Markable markable : toRemove){
        markable.removeFromIndexes();
      }
    } 
  }

  public static class DocumentIdFromURI extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    @Override
    public void process(JCas docCas) throws AnalysisEngineProcessException {
      try {
        for (Iterator<JCas> it = docCas.getViewIterator(); it.hasNext(); ) {

          JCas jCas = it.next();
          String uri = new File(ViewUriUtil.getURI(jCas)).getName();
          DocumentID docId = new DocumentID(jCas);
          if(jCas.getViewName().equals(GOLD_VIEW_NAME)){
            docId.setDocumentID(GOLD_VIEW_NAME + "_" + uri);
          }else if(jCas.getViewName().equals(CAS.NAME_DEFAULT_SOFA)){
            docId.setDocumentID(uri);
          }else{
            docId.setDocumentID(jCas.getViewName() + "_" + uri);
          }
          docId.addToIndexes();

          DocumentIdPrefix docPrefix = new DocumentIdPrefix(jCas);
          docPrefix.setDocumentIdPrefix(uri.split("_")[0]);
          docPrefix.addToIndexes();
        }
      }catch(CASException e){
        throw new AnalysisEngineProcessException(e);
      }
    }
  }

  public static class PatientScoringWriter extends AbstractPatientConsumer {

    private CoreferenceChainScoringOutput scorer = null;
    private PatientNoteStore notes = PatientNoteStore.INSTANCE;

    public PatientScoringWriter(){
      super("PatientScoringWriter", "Writes conll output that can be used in standard scoring scripts.");
      scorer = new CoreferenceChainScoringOutput();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      scorer.collectionProcessComplete();
    }

    @Override
    protected void processPatientCas(JCas patientJcas) throws AnalysisEngineProcessException {
//      scorer.process(patientJcas);
      for(JCas docView : PatientViewUtil.getDocumentViews(patientJcas)){
        scorer.process(docView);
      }
    }

    /**
     * Call initialize() on super and the delegate
     * {@inheritDoc}
     */
    @Override
    public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      scorer.initialize( context );
    }

    /**
     * Call destroy on super and the delegate
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
      super.destroy();
      scorer.destroy();
    }

    /**
     * Set the resultSpecification in this and the delegate to the same object
     * {@inheritDoc}
     */
    @Override
    public void setResultSpecification( final ResultSpecification resultSpecification ) {
      super.setResultSpecification( resultSpecification );
      scorer.setResultSpecification( resultSpecification );
    }
  }

  public static class PatientPersonChainAnnotator extends AbstractPatientConsumer {
    private PatientNoteStore notes = PatientNoteStore.INSTANCE;
    private PersonChainAnnotator delegate = new PersonChainAnnotator();

    public PatientPersonChainAnnotator(){
      super("PatientPersonAnnotator", "Finds links between person mentions in a patient-based CAS.");
    }

    @Override
    protected void processPatientCas(JCas patientJcas) throws AnalysisEngineProcessException {
      for(JCas docView : PatientViewUtil.getDocumentViews(patientJcas)){
        delegate.process(docView);
      }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      delegate.collectionProcessComplete();
    }

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
      super.initialize(context);
      delegate.initialize(context);
    }

    @Override
    public void destroy() {
      super.destroy();
      delegate.destroy();
    }
  }

  public static class FlushingDataWriter extends LibLinearStringOutcomeDataWriter {

    int numChains = 0;

    public FlushingDataWriter(File outputDirectory)
        throws FileNotFoundException {
      super(outputDirectory);
    }
    
    @Override
    protected void writeEncoded(FeatureNode[] features, Integer outcome)
        throws CleartkProcessingException {
      //        this.trainingDataWriter.println("# Writing instance:");
      super.writeEncoded(features, outcome);
      //        this.trainingDataWriter.println("# Instance written");
      this.trainingDataWriter.flush();
    }
    
    @Override
    public void write(Instance<String> instance)
        throws CleartkProcessingException {
      if(instance.getOutcome().startsWith("#DEBUG")){
        this.trainingDataWriter.println(instance.getOutcome());
        this.trainingDataWriter.flush();
      }else{
        super.write(instance);
      }
    }
  }
}
