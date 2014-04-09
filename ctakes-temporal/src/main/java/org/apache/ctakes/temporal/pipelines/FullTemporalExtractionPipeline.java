package org.apache.ctakes.temporal.pipelines;

import java.io.File;

import org.apache.ctakes.core.cr.FilesInDirectoryCollectionReader;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.ctakes.temporal.ae.EventEventRelationAnnotator;
import org.apache.ctakes.temporal.ae.EventTimeRelationAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.uimafit.component.xwriter.XWriter;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.pipeline.SimplePipeline;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class FullTemporalExtractionPipeline extends
    TemporalExtractionPipeline_ImplBase {

  static interface FullOptions extends Options {
    @Option(
        shortName = "e",
        description = "specify the path to the directory where the trained event model is located",
        defaultValue="target/eval/event-spans/train_and_test/")
    public String getEventModelDirectory();
    
    @Option(
        shortName = "t",
        description = "specify the path to the directory where the trained event model is located",
        defaultValue="target/eval/time-spans/train_and_test/BackwardsTimeAnnotator/")
    public String getTimeModelDirectory();
    
    @Option(
        shortName = "r",
        description = "Specify the path to the directory where the trained event-time relation model is located",
        defaultValue="target/eval/temporal-relations/event-time/train_and_test/")
    public String getEventTimeRelationModelDirectory();

    @Option(
        shortName = "s",
        description = "Specify the path to the directory where the trained event-event relation model is located",
        defaultToNull=true) // add in default value once we have a satisfying trained model
    public String getEventEventRelationModelDirectory();  
  }

  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    FullOptions options = CliFactory.parseArguments(FullOptions.class, args);
    
    CollectionReader collectionReader = CollectionReaderFactory.createCollectionReaderFromPath(
        "../ctakes-core/desc/collection_reader/FilesInDirectoryCollectionReader.xml",
        FilesInDirectoryCollectionReader.PARAM_INPUTDIR,
        options.getInputDirectory());

    AggregateBuilder aggregateBuilder = getLightweightPreprocessorAggregateBuilder();
    aggregateBuilder.add(EventAnnotator.createAnnotatorDescription(new File(options.getEventModelDirectory())));
    aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription(new File(options.getTimeModelDirectory())));
    aggregateBuilder.add(EventTimeRelationAnnotator.createAnnotatorDescription(new File(options.getEventTimeRelationModelDirectory())));
    if(options.getEventEventRelationModelDirectory()!=null){
      aggregateBuilder.add(EventEventRelationAnnotator.createAnnotatorDescription(new File(options.getEventEventRelationModelDirectory())));
    }
    AnalysisEngine xWriter = getXMIWriter(options.getOutputDirectory());
    
    SimplePipeline.runPipeline(
        collectionReader,
        aggregateBuilder.createAggregate(),
        xWriter);
  }

}
