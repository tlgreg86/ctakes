package org.apache.ctakes.temporal.ae;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.ctakes.temporal.ae.feature.duration.DurationDistributionFeatureExtractor.Callback;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Preserve only those events that have duration data.
 */
public class PreserveCertainEventsInGold extends JCasAnnotator_ImplBase {                                               
  
  public static final String GOLD_VIEW_NAME = "GoldView";

  @Override                                                                                                                  
  public void process(JCas jCas) throws AnalysisEngineProcessException {                                                     

    File durationLookup = new File("/Users/Dima/Boston/Thyme/Duration/Output/Duration/distribution.txt");                      
    Map<String, Map<String, Float>> textToDistribution = null;                                                                 
    try {                                                                                                                      
      textToDistribution = Files.readLines(durationLookup, Charsets.UTF_8, new Callback());                                    
    } catch(IOException e) {                                                                                                   
      e.printStackTrace();                                                                                                     
      return;                                                                                                                  
    }  
    
    JCas goldView;                                                                                                           
    try {                                                                                                                    
      goldView = jCas.getView(GOLD_VIEW_NAME);                                                                               
    } catch (CASException e) {                                                                                               
      throw new AnalysisEngineProcessException(e);                                                                           
    }                                                                                                                                                                                                                                         
    
    for(EventMention mention : Lists.newArrayList(JCasUtil.select(goldView, EventMention.class))) {
      if(textToDistribution.containsKey(mention.getCoveredText().toLowerCase())) {
        // these are the kind we keep
        continue;
      } 
      mention.removeFromIndexes();
    }
  }                                                                                                                          
}                                                                                                                            