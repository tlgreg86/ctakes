package org.apache.ctakes.temporal.ae;

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;

import com.google.common.collect.Lists;

public class PreserveCertainEventTimeRelationsInGold extends JCasAnnotator_ImplBase {                                               
  
  public static final String GOLD_VIEW_NAME = "GoldView";

  @Override                                                                                                                  
  public void process(JCas jCas) throws AnalysisEngineProcessException {                                                     

    JCas goldView;                                                                                                           
    try {                                                                                                                    
      goldView = jCas.getView(GOLD_VIEW_NAME);                                                                               
    } catch (CASException e) {                                                                                               
      throw new AnalysisEngineProcessException(e);                                                                           
    }                                                                                                                                                                                                                                         
                                                                                                                             
    for (BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(goldView, BinaryTextRelation.class))) {            
      RelationArgument arg1 = relation.getArg1(); // arg1 is an event                                                                            
      RelationArgument arg2 = relation.getArg2(); // arg2 is a time expression
      
      if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof TimeMention){
        arg1.removeFromIndexes();                                                                                            
        arg2.removeFromIndexes();                                                                                            
        relation.removeFromIndexes();
        System.out.println("removing: " + relation.getCategory());
      }                                                                                                                      
    }                                                                                                                        
  }                                                                                                                          
}                                                                                                                            