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
package org.apache.ctakes.relationextractor.ae.features;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.data.analysis.Utils;
import org.apache.ctakes.relationextractor.data.analysis.Utils.Callback;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * 
 */
public class EmbeddingFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
			
    File word2vec = new File(Utils.embeddingPath);
    Map<String, List<Float>> wordVectors = null;
    try {
      wordVectors = Files.readLines(word2vec, Charsets.UTF_8, new Callback());
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    List<Feature> features = new ArrayList<>();
    
    String arg1LastWord = Utils.getLastWord(jCas, arg1).toLowerCase();
    String arg2LastWord = Utils.getLastWord(jCas, arg2).toLowerCase();
    
    List<Float> arg1Vector = wordVectors.get("oov");
    if(wordVectors.containsKey(arg1LastWord)) {
      arg1Vector = wordVectors.get(arg1LastWord);
    }
    for(int dim = 0; dim < 300; dim++) {
      String featureName = String.format("arg1_dim_%d", dim);
      features.add(new Feature(featureName, arg1Vector.get(dim)));
    }
    
    List<Float> arg2Vector = wordVectors.get("oov");
    if(wordVectors.containsKey(arg2LastWord)) {
      arg2Vector = wordVectors.get(arg2LastWord);
    }
    for(int dim = 0; dim < 300; dim++) {
      String featureName = String.format("arg2_dim_%d", dim);
      features.add(new Feature(featureName, arg2Vector.get(dim)));
    }    
  	
    return features;
  }

}
