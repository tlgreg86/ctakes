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
package org.apache.ctakes.temporal.nn.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.ViewUriUtil;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * Print gold standard relations and their context.
 * 
 * @author dmitriy dligach
 */
public class EventTimeRelPrinter {

  static interface Options {

    @Option(longName = "xmi-dir")
    public File getInputDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();

    @Option(longName = "output-train")
    public File getTrainOutputDirectory();

    @Option(longName = "output-test")
    public File getTestOutputDirectory();
  }

  public static void main(String[] args) throws Exception {

    Options options = CliFactory.parseArguments(Options.class, args);

    File trainFile = options.getTrainOutputDirectory();
    if(trainFile.exists()) {
      trainFile.delete();
    }
    trainFile.createNewFile();
    File devFile = options.getTestOutputDirectory();
    if(devFile.exists()) {
      devFile.delete();
    }
    devFile.createNewFile();

    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getPatientSets(patientSets, THYMEData.TRAIN_REMAINDERS);
    List<Integer> devItems = THYMEData.getPatientSets(patientSets, THYMEData.DEV_REMAINDERS);

    List<File> trainFiles = Utils.getFilesFor(trainItems, options.getInputDirectory());
    List<File> devFiles = Utils.getFilesFor(devItems, options.getInputDirectory());

    // sort training files to eliminate platform specific dir listings
    Collections.sort(trainFiles);
    //    Collections.shuffle(trainFiles, new Random(100));  

    // write training data to file
    CollectionReader trainCollectionReader = Utils.getCollectionReader(trainFiles);
    AnalysisEngine trainDataWriter = AnalysisEngineFactory.createEngine(
        RelationSnippetPrinter.class,
        "OutputFile",
        trainFile.getAbsoluteFile());
    SimplePipeline.runPipeline(trainCollectionReader, trainDataWriter);

    // write dev data to file
    CollectionReader devCollectionReader = Utils.getCollectionReader(devFiles);
    AnalysisEngine devDataWriter = AnalysisEngineFactory.createEngine(
        RelationSnippetPrinter.class,
        "OutputFile",
        devFile.getAbsolutePath());
    SimplePipeline.runPipeline(devCollectionReader, devDataWriter);
  }

  /**
   * Print gold standard relations and their context.
   * 
   * @author dmitriy dligach
   */
  public static class RelationSnippetPrinter extends JCasAnnotator_ImplBase {

    @ConfigurationParameter(
        name = "OutputFile",
        mandatory = true,
        description = "path to the output file")
    private String outputFile;

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

      // can't iterate over binary text relations in a sentence, so need
      // a lookup from pair of annotations to binary text relation
      Map<List<Annotation>, BinaryTextRelation> relationLookup = new HashMap<>();
      for(BinaryTextRelation relation : JCasUtil.select(goldView, TemporalTextRelation.class)) {
        Annotation arg1 = relation.getArg1().getArgument();
        Annotation arg2 = relation.getArg2().getArgument();

        if(relationLookup.get(Arrays.asList(arg1, arg2)) != null) {
          // there is already a relation between arg1 and arg2
          // only store if it is 'contains' relation 
          if(relation.getCategory().equals("CONTAINS")) {
            relationLookup.put(Arrays.asList(arg1, arg2), relation);
          } else {
            System.out.println("skipping relation: " + arg1.getCoveredText() + " ... " + arg2.getCoveredText());
          }
        } else {
          relationLookup.put(Arrays.asList(arg1, arg2), relation);
        }
      }

      // go over sentences, extracting event-event relation instances
      for(Sentence sentence : JCasUtil.select(systemView, Sentence.class)) {
        List<String> eventTimeRelationsInSentence = new ArrayList<>();

        // retrieve event-time relations in this sentence
        for(EventMention event : JCasUtil.selectCovered(goldView, EventMention.class, sentence)) {
          for(TimeMention time : JCasUtil.selectCovered(goldView, TimeMention.class, sentence)) {

            BinaryTextRelation timeEventRelation = relationLookup.get(Arrays.asList(time, event));
            BinaryTextRelation eventTimeRelation = relationLookup.get(Arrays.asList(event, time));

            String label = "none";
            if(timeEventRelation != null) {
              if(timeEventRelation.getCategory().equals("CONTAINS")) {
                label = "contains";  // this is contains
              } else {
                // label = "none";      // e.g. before or overlap
              }
            } 
            if(eventTimeRelation != null) {
              if(eventTimeRelation.getCategory().equals("CONTAINS")) {
                label = "contains-1"; // this is contains
              } else {
                // label = "none";       // some other relation type
              }
            } 

            String context;
            if(time.getBegin() < event.getBegin()) {
              // ... time ... event ... scenario
              context = getTokenContext(systemView, sentence, time, "t", event, "e", 2);  
            } else {
              // ... event ... time ... scenario
              context = getTokenContext(systemView, sentence, event, "e", time, "t", 2);
            }

            String text = String.format("%s|%s", label, context);
            eventTimeRelationsInSentence.add(text.toLowerCase());
          }
        }  

        try {
          Files.write(Paths.get(outputFile), eventTimeRelationsInSentence, StandardOpenOption.APPEND);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Print words from left to right.
   * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
   */
  public static String getTokenContext(
      JCas jCas, 
      Sentence sent, 
      Annotation left,
      String leftType,
      Annotation right,
      String rightType,
      int contextSize) {

    List<String> tokens = new ArrayList<>();
    for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
      if(sent.getBegin() <= baseToken.getBegin()) {
        tokens.add(baseToken.getCoveredText()); 
      }
    }
    tokens.add("<" + leftType + ">");
    tokens.add(left.getCoveredText());
    tokens.add("</" + leftType + ">");
    for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
      tokens.add(baseToken.getCoveredText());
    }
    tokens.add("<" + rightType + ">");
    tokens.add(right.getCoveredText());
    tokens.add("</" + rightType + ">");
    for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
      if(baseToken.getEnd() <= sent.getEnd()) {
        tokens.add(baseToken.getCoveredText());
      }
    }

    return String.join(" ", tokens).replaceAll("[\r\n]", " ");
  }

  /**
   * Print POS tags from left to right.
   * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
   */
  public static String getPosContext(
      JCas jCas, 
      Sentence sent, 
      Annotation left,
      String leftType,
      Annotation right,
      String rightType,
      int contextSize) {

    List<String> tokens = new ArrayList<>();
    for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
      if(sent.getBegin() <= baseToken.getBegin()) {
        tokens.add(baseToken.getPartOfSpeech()); 
      }
    }
    tokens.add("<" + leftType + ">");
    for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, left)) {
      tokens.add(baseToken.getPartOfSpeech());
    }
    tokens.add("</" + leftType + ">");
    for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
      tokens.add(baseToken.getPartOfSpeech());
    }
    tokens.add("<" + rightType + ">");
    for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, right)) {
      tokens.add(baseToken.getPartOfSpeech());
    }
    tokens.add("</" + rightType + ">");
    for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
      if(baseToken.getEnd() <= sent.getEnd()) {
        tokens.add(baseToken.getPartOfSpeech());
      }
    }

    return String.join(" ", tokens).replaceAll("[\r\n]", " ");
  }
}
