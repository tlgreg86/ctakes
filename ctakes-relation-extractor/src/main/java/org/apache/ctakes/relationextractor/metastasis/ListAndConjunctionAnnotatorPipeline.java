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
package org.apache.ctakes.relationextractor.metastasis;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.relationextractor.data.analysis.Utils;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * 
 * @author dmitriy dligach
 */
public class ListAndConjunctionAnnotatorPipeline {
  
  static interface Options {

    @Option(
        longName = "xmi-dir",
        description = "path to xmi files containing gold annotations")
    public File getInputDirectory();
  }
  
	public static void main(String[] args) throws Exception {
		  
	  System.out.println("beginning...");
		Options options = CliFactory.parseArguments(Options.class, args);
    CollectionReader collectionReader = Utils.getCollectionReader(options.getInputDirectory());
    AnalysisEngine listAndConjunctionAnnotator = AnalysisEngineFactory.createEngine(ListAndConjunctionAe.class);
		SimplePipeline.runPipeline(collectionReader, listAndConjunctionAnnotator);
	}

  /**
   * Detect simple lists and conjunctions.
   * E.g. CT chest, abdomen and pelvis.
   *  
   * @author dmitriy dligach
   */
  public static class ListAndConjunctionAe extends JCasAnnotator_ImplBase {
    
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      
      JCas systemView;
      try {
        systemView = jCas.getView("_InitialView");
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }
      
      for(Sentence sentence : JCasUtil.select(systemView, Sentence.class)) {
        
        String state = "start";
        String list = "";
        for(BaseToken token : JCasUtil.selectCovered(systemView, BaseToken.class, sentence)) {
          state = getNextState(systemView, state, token);   
          if(state == "a/s" || state == "punct" || state == "done") {
            list = list + " " + token.getCoveredText();
          }
        } 
        if(list != "") {
          System.out.println(sentence.getCoveredText() + "/" + list);
        }
      }
    }
    
    public String getNextState(JCas systemView, String currentState, BaseToken nextToken) {
      
      Set<String> listConnectors = new HashSet();
      listConnectors.add("and");
      listConnectors.add(",");
      
      String nextState = "";
      int tokenSemType = getSemanticType(systemView, nextToken);
      if(currentState == "start" && tokenSemType == 6) {
        nextState = "a/s";
      } else if(currentState == "start" && tokenSemType != 6)  {
        nextState = "start";
      } else if(currentState == "a/s" && listConnectors.contains(nextToken.getCoveredText().toLowerCase())) {
        nextState = "punct";
      } else if(currentState == "a/s" && ! listConnectors.contains(nextToken.getCoveredText().toLowerCase())) {
        nextState = "done";
      } else if(currentState == "punct" && tokenSemType == 6) {
        nextState = "a/s";
      } else if(currentState == "punct" && tokenSemType != 6) {
        nextState = "reject";
      }

      return nextState;
    }
    
    public int getSemanticType(JCas systemView, BaseToken baseToken) {
      
      List<IdentifiedAnnotation> coveredIdentifiedAnnotations = 
          JCasUtil.selectCovered(systemView, IdentifiedAnnotation.class, baseToken.getBegin(), baseToken.getEnd());
      
      if(coveredIdentifiedAnnotations.size() < 1) {
        return 0; // no type
      } 
      
      return coveredIdentifiedAnnotations.get(0).getTypeID();
    }
  }
}
