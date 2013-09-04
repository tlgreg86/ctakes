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
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

/**
 * Extract durations of signs/symptoms.
 * 
 * TODO: check drinking.txt; fewer day durations are captured than exist in data.
 * TODO: need to take care of abbreviations (e.g. wk, yr, etc.)
 * 
 * @author dmitriy dligach
 */
public class SignSymptomDurations {

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
    final int maxDistance = 2;

    // regex to match different time granularities
    Pattern pattern = Pattern.compile("(second|minute|hour|day|week|month|year)", Pattern.CASE_INSENSITIVE);
    
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      Collection<DocumentID> ids = JCasUtil.select(jCas, DocumentID.class);
      String fileName = ids.iterator().next().getDocumentID();
      String signSymptomText = fileName.split("\\.")[0]; // e.g. "smoker.txt"

      // counts of different time granularities for this sign/symptom
      Multiset<String> durationDistribution = HashMultiset.create();
      
      for(SignSymptomMention signSymptomMention : JCasUtil.select(jCas, SignSymptomMention.class)) {

        if(signSymptomMention.getCoveredText().equals(signSymptomText)) {
          // distances to time expressions from this sign/symptom
          Map<TimeMention, Integer> distances = new HashMap<TimeMention, Integer>();

          for(TimeMention timeMention : JCasUtil.selectFollowing(jCas, TimeMention.class, signSymptomMention, 1)) {
            int distance = JCasUtil.selectBetween(jCas, BaseToken.class, signSymptomMention, timeMention).size();
            distances.put(timeMention, distance);
          }

          // find closest time to this sign/symptom
          List<TimeMention> sortedTimeMentions = new ArrayList<TimeMention>(distances.keySet());
          Function<TimeMention, Integer> getValue = Functions.forMap(distances);
          Collections.sort(sortedTimeMentions, Ordering.natural().onResultOf(getValue));

          if(sortedTimeMentions.size() > 0 && distances.get(sortedTimeMentions.get(0)) <= maxDistance) {

            String timex = sortedTimeMentions.get(0).getCoveredText();
            Matcher matcher = pattern.matcher(timex);
            while(matcher.find()) {
              durationDistribution.add(matcher.group());
            }
          }
        }
      }

      if(durationDistribution.size() > 0) { 
        String durationDistributionAsString = convertToString(durationDistribution);
        System.out.println(signSymptomText + "," + durationDistributionAsString);
      }
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
