/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.temporal.pipelines;

import java.io.File;

import org.apache.ctakes.core.cr.FilesInDirectoryCollectionReader;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.ctakes.temporal.ae.EventEventRelationAnnotator;
import org.apache.ctakes.temporal.ae.EventTimeRelationAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

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
    
    CollectionReader collectionReader = CollectionReaderFactory.createReaderFromPath(
        "../ctakes-core/desc/collection_reader/FilesInDirectoryCollectionReader.xml",
        FilesInDirectoryCollectionReader.PARAM_INPUTDIR,
        options.getInputDirectory());

    AggregateBuilder aggregateBuilder = getLightweightPreprocessorAggregateBuilder();
    aggregateBuilder.add(EventAnnotator.createAnnotatorDescription(new File(options.getEventModelDirectory())));
    aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription(options.getTimeModelDirectory() + File.pathSeparator + "model.jar"));
    aggregateBuilder.add(EventTimeRelationAnnotator.createAnnotatorDescription(options.getEventTimeRelationModelDirectory() + File.separator + "model.jar"));
    if(options.getEventEventRelationModelDirectory()!=null){
      aggregateBuilder.add(EventEventRelationAnnotator.createAnnotatorDescription(options.getEventEventRelationModelDirectory() + File.separator + "model.jar"));
    }
    
    //aggregateBuilder.createEngineDescription().toXML(new FileWriter("desc/analysis_engine/TemporalAggregateUMLSPipeline.xml"));
    AnalysisEngine xWriter = getXMIWriter(options.getOutputDirectory());
    
    SimplePipeline.runPipeline(
        collectionReader,
        aggregateBuilder.createAggregate(),
        xWriter);
  }

}
