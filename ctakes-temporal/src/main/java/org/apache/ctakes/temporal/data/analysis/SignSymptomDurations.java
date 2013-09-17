package org.apache.ctakes.temporal.data.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.Option;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

/**
 * Extract durations of signs/symptoms.
 * 
 * TODO: check drinking.txt; fewer day durations are captured than exist in data.
 * 
 * @author dmitriy dligach
 */
public class SignSymptomDurations {

  // regular expression to match temporal durations
  public final static String REGEX = "(sec|min|hour|hr|day|week|wk|mo|year|yr)";
  
  // mapping between temporal durations and their normal forms
  public final static Map<String, String> MAPPING = ImmutableMap.<String, String>builder()
      .put("sec", "second")
      .put("min", "minute")
      .put("hour", "hour")
      .put("hr", "hour")
      .put("day", "day")
      .put("week", "week")
      .put("wk", "week")
      .put("mo", "month")
      .put("year", "year")
      .put("yr", "year")
      .build(); 

  public static class Options extends Options_ImplBase {

    @Option(
        name = "--input-dir",
        usage = "specify the path to the directory containing the xmi files",
        required = true)
    public File inputDirectory;
  }
  
	public static void main(String[] args) throws Exception {
		
		Options options = new Options();
		options.parseOptions(args);

		List<File> trainFiles = Arrays.asList(options.inputDirectory.listFiles());
    CollectionReader collectionReader = getCollectionReader(trainFiles);
		
    AnalysisEngine durationPrinter = AnalysisEngineFactory.createPrimitive(
    		DurationPrinter.class);
    		
		SimplePipeline.runPipeline(collectionReader, durationPrinter);
	}
  
  public static class DurationPrinter extends JCasAnnotator_ImplBase {

    // max distance between a time and an evenet
    final int MAXDISTANCE = 2;

    // regex to match different time granularities
    Pattern pattern = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
    
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      Collection<DocumentID> ids = JCasUtil.select(jCas, DocumentID.class);
      String fileName = ids.iterator().next().getDocumentID();
      String signSymptomText = fileName.split("\\.")[0]; // e.g. "smoker.txt"

      // counts of different time granularities for this sign/symptom
      Multiset<String> durationDistribution = HashMultiset.create();
      
      for(SignSymptomMention signSymptomMention : JCasUtil.select(jCas, SignSymptomMention.class)) {

        if(signSymptomMention.getCoveredText().equals(signSymptomText)) {
          TimeMention nearestTimeMention = getNearestTimeMention(jCas, signSymptomMention);

          if(nearestTimeMention != null) {
            Matcher matcher = pattern.matcher(nearestTimeMention.getCoveredText());

            while(matcher.find()) {
              String matchedDuration = matcher.group(); // e.g. "wks"
              String normalizedDuration = MAPPING.get(matchedDuration);
              durationDistribution.add(normalizedDuration);
            }
          }
        }
      }

      if(durationDistribution.size() > 0) { 
        System.out.println(signSymptomText + "," + convertToString(durationDistribution));
        // System.out.println(signSymptomText + ": " + durationDistribution);
      }
    }
    
    /**
     * Find nearest time mention. Return null if none found.
     */
    private static TimeMention getNearestTimeMention(JCas jCas, SignSymptomMention signSymptomMention) {
      
      // max distance between a time and an evenet
      final int MAXDISTANCE = 2;
      
      // distances to time expressions from this sign/symptom
      Map<TimeMention, Integer> distances = new HashMap<TimeMention, Integer>();

      for(TimeMention timeMention : JCasUtil.selectFollowing(jCas, TimeMention.class, signSymptomMention, 1)) {
        int distance = JCasUtil.selectBetween(jCas, BaseToken.class, signSymptomMention, timeMention).size();
        distances.put(timeMention, distance);
      }

      if(distances.size() < 1) {
        return null;
      }
      
      // sort time mentions by distance to sign/symptom
      List<TimeMention> sortedTimeMentions = new ArrayList<TimeMention>(distances.keySet());
      Function<TimeMention, Integer> getValue = Functions.forMap(distances);
      Collections.sort(sortedTimeMentions, Ordering.natural().onResultOf(getValue));

      // if the closest one too far away, return null
      if(distances.get(sortedTimeMentions.get(0)) > MAXDISTANCE) {
        return null;
      }
      
      return sortedTimeMentions.get(0);
    }
    
    private static String getAnnotationContext(Annotation annotation, int maxContextWindowSize) {
      
      String text = annotation.getCAS().getDocumentText();
      int begin = Math.max(0, annotation.getBegin() - maxContextWindowSize);
      int end = Math.min(text.length(), annotation.getEnd() + maxContextWindowSize);
      
      return text.substring(begin, end).replaceAll("[\r\n]", " ");
    }
    
    private static String convertToString(Multiset<String> durationDistribution) {
      
      List<String> durationBins = Arrays.asList("second", "minute", "hour", "day", "week", "month", "year");
      List<Integer> durationValues = new LinkedList<Integer>();
      
      for(String durationBin : durationBins) {
        durationValues.add(durationDistribution.count(durationBin));
      }

      Joiner joiner = Joiner.on(',');
      return joiner.join(durationValues);
    }
  }
  
  private static CollectionReader getCollectionReader(List<File> items) throws Exception {

    String[] paths = new String[items.size()];
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = items.get(i).getPath();
    }
    
    return CollectionReaderFactory.createCollectionReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
}
