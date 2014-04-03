package org.apache.ctakes.temporal.duration;

import info.bethard.timenorm.Period;
import info.bethard.timenorm.PeriodSet;
import info.bethard.timenorm.Temporal;
import info.bethard.timenorm.TemporalExpressionParser;
import info.bethard.timenorm.TimeSpan;
import info.bethard.timenorm.TimeSpanSet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.temporal.ae.feature.duration.DurationEventTimeFeatureExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalUnit;
import org.uimafit.util.JCasUtil;

import scala.collection.immutable.Set;
import scala.util.Try;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.io.LineProcessor;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.reader.AbstractReader;

/**
 * Various useful classes and methods for evaluating event duration data.
 */
public class Utils {

  // events and their duration distributions
  public static final String durationDistributionPath = "/Users/dima/Boston/Thyme/Duration/Data/Combined/Distribution/all.txt";
  
  // eight bins over which we define a duration distribution
  public static final String[] bins = {"second", "minute", "hour", "day", "week", "month", "year", "decade"};
  
  /**
   * Extract time unit(s) from a temporal expression.
   * Extracted time units should be a subset of the bins above.
   * Return empty set if time units couldnot be extracted.
   * E.g. July 5, 1984 -> day
   */
  public static HashSet<String> getTimeUnits(String timex) {
   
    HashSet<String> timeUnits = new HashSet<>();    
    Set<TemporalUnit> units = runTimexParser(timex.toLowerCase());
    if(units == null) {
      return timeUnits;
    }
    
    scala.collection.Iterator<TemporalUnit> iterator = units.iterator();
    while(iterator.hasNext()) {
      TemporalUnit unit = iterator.next();
      String bin = putInBin(unit.getName());
      if(bin != null) {
        timeUnits.add(bin);    
      }
    }
    
    return timeUnits;
  }
  
  /**
   * Use Bethard normalizer to map a temporal expression to a time unit.
   */
  public static Set<TemporalUnit> runTimexParser(String timex) {

    URL grammarURL = DurationEventTimeFeatureExtractor.class.getResource("/info/bethard/timenorm/en.grammar");
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
   * Take the time unit from Bethard noramlizer
   * and return a coarser time unit, i.e. one of the eight bins.
   * Return null, if this cannot be done. 
   */
  public static String putInBin(String timeUnit) {
    
    HashSet<String> allowableTimeUnits = new HashSet<>(Arrays.asList(bins));
    
    // e.g. Years -> year
    String singularAndLowercased = timeUnit.substring(0, timeUnit.length() - 1).toLowerCase();

    // is this one of the bins already?
    if(allowableTimeUnits.contains(singularAndLowercased)) {
      return singularAndLowercased;
    } 

    // units that Betard normalizer outputs mapped to one of the eight bins
    Map<String, String> mapping = ImmutableMap.<String, String>builder()
        .put("afternoon", "hour")
        .put("evening", "hour")
        .put("morning", "hour")
        .put("night", "hour")
        .put("fall", "month")
        .put("winter", "month")
        .put("spring", "month")
        .put("summer", "month")
        .put("quarteryear", "month")
        .build(); 
    
    // it's not one of the bins; can we map to to a bin?
    if(mapping.get(singularAndLowercased) != null) {
      return mapping.get(singularAndLowercased);
    }

    // we couldn't map it to a bin
    return null;
  }
  
  /**
   * Compute expected duration in seconds. Normalize by number of seconds in a decade.
   */
  public static float expectedDuration(Map<String, Float> distribution) {
    
    // unit of time -> duration in seconds
    final Map<String, Integer> timeUnitInSeconds = ImmutableMap.<String, Integer>builder()
        .put("second", 1)
        .put("minute", 60)
        .put("hour", 60 * 60)
        .put("day", 60 * 60 * 24)
        .put("week", 60 * 60 * 24 * 7)
        .put("month", 60 * 60 * 24 * 30)
        .put("year", 60 * 60 * 24 * 365)
        .put("decade", 60 * 60 * 24 * 365 * 10)
        .build();

    float expectation = 0f;
    for(String unit : distribution.keySet()) {
      expectation = expectation + (timeUnitInSeconds.get(unit) * distribution.get(unit));
    }
  
    return expectation / timeUnitInSeconds.get("decade");
  }
  
  /**
   * Take a time unit and return a probability distribution
   * in which p(this time unit) = 1 and all others are zero.
   * Assume time unit is one of the eight duration bins.
   */
  public static Map<String, Float> convertToDistribution(String timeUnit) {
    
    Map<String, Float> distribution = new HashMap<String, Float>();
    
    for(String bin: bins) {
      if(bin.equals(timeUnit)) {
        distribution.put(bin, 1.0f);
      } else {
        distribution.put(bin, 0.0f);
      }
    }
    
    return distribution;
  }
  
  /**
   * Convert duration distribution multiset to a format that's easy to parse automatically.
   * Format: <sign/symptom>, <time bin>:<count>, ...
   * Example: apnea, second:5, minute:1, hour:5, day:10, week:1, month:0, year:0
   */
  public static String formatDistribution(
      String mentionText, 
      Multiset<String> durationDistribution, 
      String separator,
      boolean normalize) {
    
    List<String> distribution = new LinkedList<String>();
    distribution.add(mentionText);

    double total = 0;
    if(normalize) {
      for(String bin : bins) {
        total += durationDistribution.count(bin);
      }
    }
    
    for(String bin : bins) {
      if(normalize) {
        distribution.add(String.format("%s:%.3f", bin, durationDistribution.count(bin) / total));  
      } else {
        distribution.add(String.format("%s:%d", bin, durationDistribution.count(bin)));
      }
      
    }
    
    Joiner joiner = Joiner.on(separator);
    return joiner.join(distribution);
  }

  /**
   * Lemmatize word using ClearNLP lemmatizer.
   */
  public static String lemmatize(String word, String pos) throws IOException {
    
    final String ENG_LEMMATIZER_DATA_FILE = "org/apache/ctakes/dependency/parser/models/lemmatizer/dictionary-1.3.1.jar";
    AbstractMPAnalyzer lemmatizer;
    InputStream lemmatizerModel = FileLocator.getAsStream(ENG_LEMMATIZER_DATA_FILE);
    lemmatizer = EngineGetter.getMPAnalyzer(AbstractReader.LANG_EN, lemmatizerModel);
    String lemma = lemmatizer.getLemma(word, pos);
    lemmatizerModel.close();

    return lemma;
  }
  
  /**
   * Return system generated POS tag or null if none available.
   */
  public static String getPosTag(JCas systemView, Annotation annotation) {
    
    List<BaseToken> coveringBaseTokens = JCasUtil.selectCovered(
        systemView,
        BaseToken.class,
        annotation.getBegin(),
        annotation.getEnd());
    
    if(coveringBaseTokens.size() < 1) {
      return null;
    }
    
    return coveringBaseTokens.get(0).getPartOfSpeech();
  }
  
  /**
   * Check if the annotation is a UMLS concept. If it is, return as is.
   * Otherwise, lemmatize this annotation if this is a verb. 
   * Return as is if not verb.
   * Lowercase before returning.
   */
  public static String getText(JCas jCas, Annotation annotation) 
      throws AnalysisEngineProcessException {

    JCas systemView;
    try {
      systemView = jCas.getView("_InitialView");
    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    }

    List<EventMention> coveringSystemEventMentions = JCasUtil.selectCovered(
        systemView, 
        EventMention.class, 
        annotation.getBegin(), 
        annotation.getEnd());
    for(EventMention systemEventMention : coveringSystemEventMentions) {
      if(systemEventMention.getTypeID() != 0) {
        return annotation.getCoveredText().toLowerCase();
      }
    } 
    
    String pos = Utils.getPosTag(systemView, annotation);
    if(pos == null) {
      return annotation.getCoveredText().toLowerCase();
    }

    String text;
    if(pos.startsWith("V")) {
      try {
        text = Utils.lemmatize(annotation.getCoveredText().toLowerCase(), pos);
      } catch (IOException e) {
        System.out.println("couldn't lemmatize: " + annotation.getCoveredText());
        e.printStackTrace();
        return annotation.getCoveredText().toLowerCase();
      }
    } else {
      text = annotation.getCoveredText();
    }
    
    return text.toLowerCase();
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
  
  public static void main(String[] args) throws IOException {
    
    HashSet<String> timeUnits = getTimeUnits("three months");
    System.out.println(timeUnits);
  }
}
