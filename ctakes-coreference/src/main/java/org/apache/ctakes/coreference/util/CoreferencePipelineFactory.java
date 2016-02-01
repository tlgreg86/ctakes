package org.apache.ctakes.coreference.util;

import org.apache.ctakes.coreference.ae.MipacqMarkableCreator;
import org.apache.ctakes.coreference.ae.MipacqMarkableExpander;
import org.apache.ctakes.coreference.ae.MipacqMarkablePairGenerator;
import org.apache.ctakes.coreference.ae.MipacqSvmChainCreator;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

public class CoreferencePipelineFactory {
  
  public static AnalysisEngineDescription getCoreferencePipeline() throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    
    builder.add(AnalysisEngineFactory.createEngineDescription(MipacqMarkableCreator.class));
    builder.add(AnalysisEngineFactory.createEngineDescription(MipacqMarkableExpander.class));
    builder.add(AnalysisEngineFactory.createEngineDescription(MipacqMarkablePairGenerator.class));
    builder.add(AnalysisEngineFactory.createEngineDescription(MipacqSvmChainCreator.class));

    return builder.createAggregateDescription();
  }
}
