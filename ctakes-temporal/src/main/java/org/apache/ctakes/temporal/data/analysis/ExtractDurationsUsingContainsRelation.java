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
package org.apache.ctakes.temporal.data.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.temporal.ae.feature.duration.Utils;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.threeten.bp.temporal.TemporalUnit;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.util.JCasUtil;

import scala.collection.immutable.Set;

import com.google.common.collect.Lists;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * @author dmitriy dligach
 */
public class ExtractDurationsUsingContainsRelation {

  static interface Options {

    @Option(
        description = "specify the path to the directory containing the xmi files")
    public File getInputDirectory();

    @Option(
        description = "specify the path to the output file")
    public File getEventOutputFile();
  }

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);

    List<File> files = Arrays.asList(options.getInputDirectory().listFiles());
    CollectionReader collectionReader = getCollectionReader(files);

    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createPrimitive(
        ProcessRelations.class,
        "EventOutputFile",
        options.getEventOutputFile());

    SimplePipeline.runPipeline(collectionReader, annotationConsumer);
  }

  /**
   *
   */
  public static class ProcessRelations extends JCasAnnotator_ImplBase {

    @ConfigurationParameter(
        name = "EventOutputFile",
        mandatory = true,
        description = "path to the output file that will store the events")
    private String eventOutputFile;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      JCas goldView;
      try {
        goldView = jCas.getView("GoldView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      JCas systemView;
      try {
        systemView = jCas.getView("_InitialView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) { 
        if(! relation.getCategory().equals("CONTAINS")) {
          continue;
        }
        
        RelationArgument arg1 = relation.getArg1();                                                                             
        RelationArgument arg2 = relation.getArg2(); 

        String eventText;
        String timeText;
        if(arg1.getArgument() instanceof TimeMention && arg2.getArgument() instanceof EventMention) {
          timeText = arg1.getArgument().getCoveredText().toLowerCase(); 
          eventText = arg2.getArgument().getCoveredText().toLowerCase();  
        } else if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof TimeMention) {
          eventText = arg1.getArgument().getCoveredText().toLowerCase(); 
          timeText = arg2.getArgument().getCoveredText().toLowerCase();  
        } else {
          // this is not a event-time relation
          continue;
        }    

        Set<TemporalUnit> units = Utils.normalize(timeText);
        
        System.out.println(relation.getCategory() + " / " + timeText + " / " + eventText);
      }
    }
  }

  public static CollectionReader getCollectionReader(List<File> inputFiles) throws Exception {

    List<String> fileNames = new ArrayList<String>();
    for(File file : inputFiles) {
      if(! (file.isHidden())) {
        fileNames.add(file.getPath());
      }
    }

    String[] paths = new String[fileNames.size()];
    fileNames.toArray(paths);

    return CollectionReaderFactory.createCollectionReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
}

