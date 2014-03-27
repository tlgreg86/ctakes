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
package org.apache.ctakes.temporal.ae.feature.duration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;
import org.threeten.bp.temporal.TemporalUnit;

import scala.collection.immutable.Set;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Assumes all relations whose argument have no duration data have been deleted.
 */
public class DurationEventTimeFeatureExtractor implements RelationFeaturesExtractor {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2)
      throws AnalysisEngineProcessException {
    
    List<Feature> features = new ArrayList<Feature>();
    
    String eventText = arg1.getCoveredText().toLowerCase(); // arg1 is an event
    String timeText = arg2.getCoveredText().toLowerCase();  // arg2 is a time mention

    File durationLookup = new File(Utils.durationDistributionPath);
    Map<String, Map<String, Float>> textToDistribution = null;
    try {
      textToDistribution = Files.readLines(durationLookup, Charsets.UTF_8, new Utils.Callback());
    } catch(IOException e) {
      e.printStackTrace();
      return features;
    }

    Map<String, Float> eventDistribution = textToDistribution.get(eventText);
    float eventExpectedDuration = Utils.expectedDuration(eventDistribution);

    Set<TemporalUnit> units = Utils.normalize(timeText);
    scala.collection.Iterator<TemporalUnit> iterator = units.iterator();
    while(iterator.hasNext()) {
      TemporalUnit unit = iterator.next();
      Map<String, Float> distribution = Utils.convertToDistribution(unit.getName());
      float timeExpectedDuration = Utils.expectedDuration(distribution);
      features.add(new Feature("expected_duration_difference", timeExpectedDuration - eventExpectedDuration));
      continue; // ignore multiple time units (almost never happens)
    } 

    return features; 
  }
}
