package org.apache.ctakes.temporal.ae;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.ctakes.temporal.ae.feature.duration.Utils;
import org.apache.ctakes.temporal.ae.feature.duration.Utils.Callback;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.threeten.bp.temporal.TemporalUnit;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;

import scala.collection.immutable.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Preserve only those event-time relations whose event argument has duration data
 * and whose time argument can be normalized using Steve's timex normalizer.
 */
public class PreserveCertainEventTimeRelationsInGold extends JCasAnnotator_ImplBase {                                               
  
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
    
    // remove relations where one or both arguments have no duration data
    for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) {            
      RelationArgument arg1 = relation.getArg1();                                                                             
      RelationArgument arg2 = relation.getArg2(); 
      
      String eventText;
      String timeText;
      if(arg1.getArgument() instanceof TimeMention && arg2.getArgument() instanceof EventMention) {
        timeText = arg1.getArgument().getCoveredText().toLowerCase(); 
        eventText = arg2.getArgument().getCoveredText().toLowerCase();  
      } else if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof TimeMention) {
        eventText = arg1.getArgument().getCoveredText().toLowerCase(); 
        timeText = arg2.getArgument().getCoveredText().toLowerCase();  
      } else {
        // this is not a event-time relation
        continue;
      }    

      Set<TemporalUnit> units = Utils.normalize(timeText);
      if(textToDistribution.containsKey(eventText) && units != null) {
        // there is duration information and we are able to get time units, so keep this
        continue;
      }

      arg1.removeFromIndexes();                                                                                            
      arg2.removeFromIndexes();                                                                                            
      relation.removeFromIndexes();
    }
    
    // remove events (that didn't participate in relations) that have no data
    for(EventMention mention : Lists.newArrayList(JCasUtil.select(goldView, EventMention.class))) {
      if(textToDistribution.containsKey(mention.getCoveredText().toLowerCase())) {
        // these are the kind we keep
        continue;
      } 
      mention.removeFromIndexes();
    }
    
    // finally remove time expressions (that didn't participate in relations) that have no data
    for(TimeMention mention : Lists.newArrayList(JCasUtil.select(goldView, TimeMention.class))) {
      String timeText = mention.getCoveredText().toLowerCase();
      Set<TemporalUnit> units = Utils.normalize(timeText);
      if(units != null) {
        // these are the kind we keep
        continue;
      }
      mention.removeFromIndexes();
    }
  }                                                                                                                          
}                                                                                                                            