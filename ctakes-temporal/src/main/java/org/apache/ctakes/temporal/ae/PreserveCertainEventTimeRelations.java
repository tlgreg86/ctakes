package org.apache.ctakes.temporal.ae;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.ctakes.temporal.ae.feature.duration.DurationDistributionFeatureExtractor.Callback;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class PreserveCertainEventTimeRelations extends JCasAnnotator_ImplBase {                                               
  
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
                                                                                                                             
    for (BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) {            
      RelationArgument arg1 = relation.getArg1();                                                                            
      RelationArgument arg2 = relation.getArg2();                                                                            
      String arg1text = arg1.getArgument().getCoveredText().toLowerCase();                                                   
      String arg2text = arg2.getArgument().getCoveredText().toLowerCase();                                                   
      if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof EventMention){                          
        if(textToDistribution.containsKey(arg1text) || textToDistribution.containsKey(arg2text)) {                           
          // keep relations where on of the arguments has duration data                                                      
          continue;                                                                                                          
        }                                                                                                                    
                                                                                                                             
        arg1.removeFromIndexes();                                                                                            
        arg2.removeFromIndexes();                                                                                            
        relation.removeFromIndexes();                                                                                        
      }                                                                                                                      
    }                                                                                                                        
  }                                                                                                                          
}                                                                                                                            