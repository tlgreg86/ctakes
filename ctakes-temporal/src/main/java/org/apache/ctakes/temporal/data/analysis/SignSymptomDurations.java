package org.apache.ctakes.temporal.data.analysis;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;

/**
 * Extract durations of signs/symptoms.
 * 
 * @author dmitriy dligach
 */
public class SignSymptomDurations {

  public static class Options  {

    @Option(
        name = "--input-dir",
        usage = "specify the path to the directory containing the xmi files",
        required = true)
    public File inputDirectory;
  }
  
	public static void main(String[] args) throws Exception {
		
	  Options options = new Options();
	  CmdLineParser parser = new CmdLineParser(options);
	  parser.parseArgument(args);
	  
	  
		List<File> trainFiles = Arrays.asList(options.inputDirectory.listFiles());
    CollectionReader collectionReader = getCollectionReader(trainFiles);
		
    AnalysisEngine temporalDurationExtractor = AnalysisEngineFactory.createPrimitive(
    		TemporalDurationExtractor.class);
    		
		SimplePipeline.runPipeline(collectionReader, temporalDurationExtractor);
	}
  
  public static class TemporalDurationExtractor extends JCasAnnotator_ImplBase {
    
    // regular expression to match temporal durations in time mention annotations
    private final static String REGEX = "(sec|min|hour|hrs|day|week|wk|month|year|yr|decade)";
    
    // mapping between temporal durations and their normalized forms
    private final static Map<String, String> MAPPING = ImmutableMap.<String, String>builder()
        .put("sec", "second")
        .put("min", "minute")
        .put("hour", "hour")
        .put("hrs", "hour")
        .put("day", "day")
        .put("week", "week")
        .put("wk", "week")
        .put("month", "month")
        .put("year", "year")
        .put("yr", "year")
        .put("decade", "decade")
        .build(); 
    
    // unique temporal bins; all time mentions will be classified into one of them
    private final static List<String> BINS = Arrays.asList(
        "second",
        "minute",
        "hour",
        "day",
        "week",
        "month",
        "year",
        "decade");
    
    // max distance between an event and the time mention that defines the event's duration
    private final static int MAXDISTANCE = 2;

    // regex to match different time granularities (e.g. 'day', 'month')
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
          if(isNegated(jCas, signSymptomMention) || isMedicationPattern(jCas, signSymptomMention)) {
            continue;
          }
          
          TimeMention nearestTimeMention = getNearestTimeMention(jCas, signSymptomMention);
          if(nearestTimeMention != null) {
            Matcher matcher = pattern.matcher(nearestTimeMention.getCoveredText());

            // need the loop to handle things like 'several days/weeks'
            while(matcher.find()) {
              String matchedDuration = matcher.group(); // e.g. "wks"
              String normalizedDuration = MAPPING.get(matchedDuration);
              durationDistribution.add(normalizedDuration);
            }
          }
        }
      }

      if(durationDistribution.size() > 0) { 
        System.out.println(formatDistribution(signSymptomText, durationDistribution, ", ", true) + "[" + durationDistribution.size() + " instances]");
      }else{
        System.out.println(signSymptomText + ": No duration information found.");
      }
    }
    
    /**
     * Return true if sign/symptom is negated.
     * TODO: using rules for now; switch to using a negation module
     */
    private static boolean isNegated(JCas jCas, SignSymptomMention signSymptomMention) {
      
      for(BaseToken token : JCasUtil.selectPreceding(jCas, BaseToken.class, signSymptomMention, 3)) {
        if(token.getCoveredText().equals("no")) {
          return true;
        }
      }
      
      return false;
    }

    /**
     * Return true of this is a medication pattern. 
     * E.g. five (5) ml po qid  (4 times a day) as needed for heartburn for 2 weeks.
     */
    private static boolean isMedicationPattern(JCas jCas, SignSymptomMention signSymptomMention) {
      
      for(BaseToken token : JCasUtil.selectPreceding(jCas, BaseToken.class, signSymptomMention, 1)) {
        if(token.getCoveredText().equals("for")) {
          return true;
        }
      }
           
      return false;
    }
    
    /**
     * Find nearest time mention that is within allowable distance. 
     * Return null if none found.
     */
    private static TimeMention getNearestTimeMention(JCas jCas, SignSymptomMention signSymptomMention) {

      List<TimeMention> timeMentions = JCasUtil.selectFollowing(jCas, TimeMention.class, signSymptomMention, 1);
      if(timeMentions.size() < 1) {
        return null;
      }
      
      assert timeMentions.size() == 1;
      
      TimeMention nearestTimeMention = timeMentions.get(0);
      int distance = JCasUtil.selectBetween(jCas, BaseToken.class, signSymptomMention, nearestTimeMention).size();
      if(distance > MAXDISTANCE) {
        return null;
      }
      
      return nearestTimeMention;
    }
    
    @SuppressWarnings("unused")
    private static String getAnnotationContext(Annotation annotation, int maxContextWindowSize) {
      
      String text = annotation.getCAS().getDocumentText();
      int begin = Math.max(0, annotation.getBegin() - maxContextWindowSize);
      int end = Math.min(text.length(), annotation.getEnd() + maxContextWindowSize);
      
      return text.substring(begin, end).replaceAll("[\r\n]", " ");
    }
    
    @SuppressWarnings("unused")
    private static String formatDistribution(Multiset<String> durationDistribution) {
      
      List<String> durationBins = Arrays.asList("second", "minute", "hour", "day", "week", "month", "year", "decade");
      List<Integer> durationValues = new LinkedList<Integer>();
      
      for(String durationBin : durationBins) {
        durationValues.add(durationDistribution.count(durationBin));
      }

      Joiner joiner = Joiner.on(',');
      return joiner.join(durationValues);
    }
    
    /**
     * Convert duration distribution multiset to a format that's easy to parse automatically.
     * Format: <sign/symptom>,<time bin>:<count>, ...
     * Example: apnea, second:5, minute:1, hour:5, day:10, week:1, month:0, year:0
     */
    private static String formatDistribution(
        String signSymptomText, 
        Multiset<String> durationDistribution, 
        String separator,
        boolean normalize) {
      
      List<String> distribution = new LinkedList<String>();
      distribution.add(signSymptomText);

      double total = 0;
      if(normalize) {
        for(String bin : BINS) {
          total += durationDistribution.count(bin);
        }
      }
      
      for(String bin : BINS) {
        if(normalize) {
          distribution.add(String.format("%s:%.3f", bin, durationDistribution.count(bin) / total));  
        } else {
          distribution.add(String.format("%s:%d", bin, durationDistribution.count(bin)));
        }
        
      }
      
      Joiner joiner = Joiner.on(separator);
      return joiner.join(distribution);
    }
  }
  
  private static CollectionReader getCollectionReader(List<File> items) throws Exception {

    String[] paths = new String[items.size()];
    Collections.sort(items, new FileSizeComparator());
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = items.get(i).getPath();
    }
    
    return CollectionReaderFactory.createCollectionReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
  
  public static class FileSizeComparator implements Comparator<File> {

    @Override
    public int compare(File o1, File o2) {
      if(o1.length() > o2.length()){
        return 1;
      }else if(o1.length() < o2.length()){
        return -1;
      }else{
        return 0;
      }
    } 
  }
}
