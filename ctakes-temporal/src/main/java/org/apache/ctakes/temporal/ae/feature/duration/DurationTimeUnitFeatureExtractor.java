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

import info.bethard.timenorm.Period;
import info.bethard.timenorm.PeriodSet;
import info.bethard.timenorm.Temporal;
import info.bethard.timenorm.TemporalExpressionParser;
import info.bethard.timenorm.TimeSpan;
import info.bethard.timenorm.TimeSpanSet;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalUnit;

import scala.collection.immutable.Set;
import scala.util.Try;

public class DurationTimeUnitFeatureExtractor implements RelationFeaturesExtractor {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2)
      throws AnalysisEngineProcessException {

    List<Feature> features = new ArrayList<Feature>();
    String timeText = arg2.getCoveredText().toLowerCase();  // arg2 is a time mention

    Set<TemporalUnit> units = normalize(timeText);
    if(units == null) {
      features.add(new Feature("time_unit", "not_available"));
    } else {
      scala.collection.Iterator<TemporalUnit> iterator = units.iterator();
      while(iterator.hasNext()) {
        TemporalUnit unit = iterator.next();
        Map<String, Float> distribution = convertToDistribution(unit.getName());
        float expectedDuration = DurationExpectationFeatureExtractor.expectedDuration(distribution);
        features.add(new Feature("expected_duration", expectedDuration));
      }
    } 

    return features; 
  }
  
  public static Set<TemporalUnit> normalize(String timex) {

    URL grammarURL = DurationTimeUnitFeatureExtractor.class.getResource("/info/bethard/timenorm/en.grammar");
    TemporalExpressionParser parser = new TemporalExpressionParser(grammarURL);
    TimeSpan anchor = TimeSpan.of(2013, 12, 16);
    Try<Temporal> result = parser.parse(timex, anchor);

    Set<TemporalUnit> units = null;
    if (result.isSuccess()) {
      Temporal temporal = result.get();

      if (temporal instanceof Period) {
        units = ((Period) temporal).unitAmounts().keySet();
      } else if (temporal instanceof PeriodSet) {
        units = ((PeriodSet) temporal).period().unitAmounts().keySet();
      } else if (temporal instanceof TimeSpan) {
        units = ((TimeSpan) temporal).period().unitAmounts().keySet();
      } else if (temporal instanceof TimeSpanSet) {
        Set<TemporalField> fields = ((TimeSpanSet) temporal).fields().keySet();
        units = null; // fill units by calling .getBaseUnit() on each field
      }
    }
    
    return units;
  }
  
  /**
   * Take a time unit and return a probability distribution
   * in which p(this time unit) = 1 and all others are zero.
   */
  public static Map<String, Float> convertToDistribution(String timeUnit) {
    
    String[] bins = {"second", "minute", "hour", "day", "week", "month", "year"};
    Map<String, Float> distribution = new HashMap<String, Float>();
    
    for(String bin: bins) {
      // convert things like "Hours" to "hour"
      String normalized = timeUnit.substring(0, timeUnit.length() - 1).toLowerCase(); 
      if(bin.equals(normalized)) {
        distribution.put(bin, 1.0f);
      } else {
        distribution.put(bin, 0.0f);
      }
    }
    
    return distribution;
  }
}
