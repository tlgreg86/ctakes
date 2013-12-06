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
package org.apache.ctakes.temporal.ae.feature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class EventDurationFeatureExtractor implements RelationFeaturesExtractor {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2)
      throws AnalysisEngineProcessException {

    List<Feature> features = new ArrayList<Feature>();
    File durationLookup = new File("/Users/dima/Boston/Thyme/Duration/Output/Duration/distribution.txt");
    String text1 = arg1.getCoveredText().toLowerCase();
    String text2 = arg2.getCoveredText().toLowerCase();
    
    try {
      Map<String, Map<String, Float>> textToDistribution = Files.readLines(durationLookup, Charsets.UTF_8, new Callback());
      
      Map<String, Float> distribution1 = textToDistribution.get(text1);
      if(distribution1 == null) {
        features.add(new Feature("arg1_no_duration_info"));
      } else {
        float expectation = expectedDuration(distribution1);
        features.add(new Feature("arg1_expected_duration", expectation));
        System.out.println(text1 + " / " + distribution1 + " / " + expectation / (3600 * 24) + " days");
      }
      
      Map<String, Float> distribution2 = textToDistribution.get(text2);
      if(distribution2 == null) {
        features.add(new Feature("arg2_no_duration_info"));
      } else {
        float expectation = expectedDuration(distribution2);
        features.add(new Feature("arg2_expected_duration", expectation));
        System.out.println(text2 + " / " + distribution2 + " / " + expectation / (3600 * 24) + " days");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return features;
  }

  private static float expectedDuration(Map<String, Float> distribution) {
    
    // unit of time -> duration in seconds
    final Map<String, Integer> converter = ImmutableMap.<String, Integer>builder()
        .put("second", 1)
        .put("minute", 60)
        .put("hour", 60 * 60)
        .put("day", 60 * 60 * 24)
        .put("week", 60 * 60 * 24 * 7)
        .put("month", 60 * 60 * 24 * 30)
        .put("year", 60 * 60 * 24 * 365)
        .build();

    float expectation = 0f;
    for(String unit : distribution.keySet()) {
      expectation = expectation + (converter.get(unit) * distribution.get(unit));
    }
  
    return expectation;
  }
  
  private static class Callback implements LineProcessor <Map<String, Map<String, Float>>> {

    // map event text to its duration distribution
    private Map<String, Map<String, Float>> textToDistribution;
    
    public Callback() {
      textToDistribution = new HashMap<String, Map<String, Float>>();
    }
    
    public boolean processLine(String line) throws IOException {

      String[] elements = line.split(", "); // e.g. pain, second:0.000, minute:0.005, hour:0.099, ...
      Map<String, Float> distribution = new HashMap<String, Float>();
      
      for(int durationBinNumber = 1; durationBinNumber < elements.length; durationBinNumber++) {
        String[] durationAndValue = elements[durationBinNumber].split(":"); // e.g. "day:0.475"
        distribution.put(durationAndValue[0], Float.parseFloat(durationAndValue[1]));
      }
      
      textToDistribution.put(elements[0], distribution);
      return true;
    }

    public Map<String, Map<String, Float>> getResult() {

      return textToDistribution;
    }
  }
}