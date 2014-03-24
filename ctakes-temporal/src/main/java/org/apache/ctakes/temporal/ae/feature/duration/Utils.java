package org.apache.ctakes.temporal.ae.feature.duration;

import info.bethard.timenorm.Period;
import info.bethard.timenorm.PeriodSet;
import info.bethard.timenorm.Temporal;
import info.bethard.timenorm.TemporalExpressionParser;
import info.bethard.timenorm.TimeSpan;
import info.bethard.timenorm.TimeSpanSet;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalUnit;

import scala.collection.immutable.Set;
import scala.util.Try;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.LineProcessor;

/**
 * Various useful classes and methods for evaluating event duration data.
 */
public class Utils {

  // events and their duration distributions
  public static final String durationDistributionPath = "/Users/dima/Boston/Thyme/Duration/Data/Combined/Distribution/mimic.txt";
  
  // time units over which we define a duration distribution
  public static final String[] bins = {"second", "minute", "hour", "day", "week", "month", "year"};
  
  /**
   * Take the time unit from Steven Bethard's noramlizer
   * and output a coarser time unit: {"second", "minute", "hour", "day", "week", "month", "year"}.
   */
  public static String makeCoarse(String timeUnit) {
    
    HashSet<String> allowableTimeUnits = new HashSet<String>(Arrays.asList(bins));
    
    // map output of Steven Behard's normalizer to coarser time units
    Map<String, String> mapping = ImmutableMap.<String, String>builder()
        .put("afternoon", "hour")
        .put("decade", "year")
        .put("evening", "hour")
        .put("fall", "month")
        .put("winter", "month")
        .put("morning", "hour")
        .put("night", "hour")
        .put("quarteryear", "year")
        .put("spring", "month")
        .put("summer", "month")
        .build(); 
    
    String singularAndLowercased = timeUnit.substring(0, timeUnit.length() - 1).toLowerCase();
    if(allowableTimeUnits.contains(singularAndLowercased)) {
      return singularAndLowercased;
    } 
    if(mapping.get(singularAndLowercased) != null) {
      return mapping.get(singularAndLowercased);
    }
    
    return null;
  }
  
  /**
   * Compute expected duration in seconds. Normalize by number of seconds in a year.
   */
  public static float expectedDuration(Map<String, Float> distribution) {
    
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
  
    return expectation / converter.get("year");
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
  
  /**
   * Read event duration distributions from file.
   */
  public static class Callback implements LineProcessor <Map<String, Map<String, Float>>> {

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
