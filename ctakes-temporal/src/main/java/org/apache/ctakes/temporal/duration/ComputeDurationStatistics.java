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
package org.apache.ctakes.temporal.duration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.cr.XMIReader;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Analyze duration information for relation arguments.
 * 
 * @author dmitriy dligach
 */
public class ComputeDurationStatistics {

  static interface Options {

    @Option(longName = "xmi-dir")
    public File getInputDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();
    
    @Option(longName = "output-file")
    public File getOutputFile();
  }

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);

    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getTrainPatientSets(patientSets);
    List<File> trainFiles = getFilesFor(trainItems, options.getInputDirectory());
    CollectionReader collectionReader = getCollectionReader(trainFiles);

    AnalysisEngine annotationConsumer = AnalysisEngineFactory.createPrimitive(
        AnalyseRelationArgumentDuration.class,
        "OutputFile",
        options.getOutputFile());

    SimplePipeline.runPipeline(collectionReader, annotationConsumer);
  }

  private static CollectionReader getCollectionReader(List<File> inputFiles) throws Exception {

    List<String> fileNames = new ArrayList<>();
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

  private static List<File> getFilesFor(List<Integer> patientSets, File inputDirectory) {

    List<File> files = new ArrayList<>();

    for (Integer set : patientSets) {
      final int setNum = set;
      for (File file : inputDirectory.listFiles(new FilenameFilter(){
        @Override
        public boolean accept(File dir, String name) {
          return name.contains(String.format("ID%03d", setNum));
        }})) {
        // skip hidden files like .svn
        if (!file.isHidden()) {
          files.add(file);
        } 
      }
    }

    return files;
  }

  /**
   * Preserve only those event-time relations whose event argument has duration data
   * and whose time argument can be normalized using Steve's timex normalizer.
   */
  public static class AnalyseRelationArgumentDuration extends JCasAnnotator_ImplBase {                                               

    @ConfigurationParameter(
        name = "OutputFile",
        mandatory = true,
        description = "path to the file that stores relation data")
    private String outputFile;
    
    public static final String GOLD_VIEW_NAME = "GoldView";

    @Override                                                                                                                  
    public void process(JCas jCas) throws AnalysisEngineProcessException {                                                     

      File durationLookup = new File(Utils.durationDistributionPath);                      
      Map<String, Map<String, Float>> textToDistribution = null;                                                                 
      try {                                                                                                                      
        textToDistribution = Files.readLines(durationLookup, Charsets.UTF_8, new Utils.Callback());                                    
      } catch(IOException e) {                                                                                                   
        e.printStackTrace();                                                                                                     
        return;                                                                                                                  
      }  

      JCas goldView;                                                                                                           
      try {                                                                                                                    
        goldView = jCas.getView(GOLD_VIEW_NAME);                                                                               
      } catch (CASException e) {                                                                                               
        throw new AnalysisEngineProcessException(e);                                                                           
      }                                                                                                                                                                                                                                         

      // remove relations where one or both arguments have no duration data
      for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) {            
        RelationArgument arg1 = relation.getArg1();                                                                             
        RelationArgument arg2 = relation.getArg2(); 

        String eventText;
        String timeText;
        if(arg1.getArgument() instanceof TimeMention && arg2.getArgument() instanceof EventMention) {
          timeText = arg1.getArgument().getCoveredText().toLowerCase(); 
          eventText = Utils.normalizeEventText(jCas, arg2.getArgument());
        } else if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof TimeMention) {
          eventText = Utils.normalizeEventText(jCas, arg1.getArgument());
          timeText = arg2.getArgument().getCoveredText().toLowerCase();  
        } else {
          // this is not a event-time relation
          continue;
        }    

        HashSet<String> timeUnits = Utils.getTimeUnits(timeText);
        if(textToDistribution.containsKey(eventText) && timeUnits.size() > 0) {
          // there is duration information and we are able to get time units
          Map<String, Float> eventDistribution = textToDistribution.get(eventText);
          Map<String, Float> timeDistribution = Utils.convertToDistribution(timeUnits.iterator().next());
          float eventExpectedDuration = Utils.expectedDuration(eventDistribution);
          float timeExpectedDuration = Utils.expectedDuration(timeDistribution);
          String out = timeUnits.iterator().next() + "|" + timeExpectedDuration + "|" + eventText + "|" + eventExpectedDuration;
          try {
            Files.append(out + "\n", new File(outputFile), Charsets.UTF_8);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }
}
