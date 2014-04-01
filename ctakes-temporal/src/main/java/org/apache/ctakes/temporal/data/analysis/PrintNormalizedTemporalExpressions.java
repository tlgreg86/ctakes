package org.apache.ctakes.temporal.data.analysis;

import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.threeten.bp.temporal.TemporalUnit;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;

import scala.collection.immutable.Set;

import com.google.common.collect.Lists;

/**
 * Print the time units produced by Steven Bethard's TimeEx normalizer
 * for temporal expressions in the gold standard.
 */
public class PrintNormalizedTemporalExpressions extends JCasAnnotator_ImplBase {                                               
  
  public static final String GOLD_VIEW_NAME = "GoldView";

  @Override                                                                                                                  
  public void process(JCas jCas) throws AnalysisEngineProcessException {                                                     
    
    JCas goldView;                                                                                                           
    try {                                                                                                                    
      goldView = jCas.getView(GOLD_VIEW_NAME);                                                                               
    } catch (CASException e) {                                                                                               
      throw new AnalysisEngineProcessException(e);                                                                           
    }                                                                                                                                                                                                                                         
    
    for(TimeMention mention : Lists.newArrayList(JCasUtil.select(goldView, TimeMention.class))) {
      String timex = mention.getCoveredText().toLowerCase();
      Set<TemporalUnit> units = Utils.runTimexParser(timex);

      if(units == null) {
        System.out.println(timex + "|" + "n/a");
      } else {
        scala.collection.Iterator<TemporalUnit> iterator = units.iterator();
        while(iterator.hasNext()) {
          TemporalUnit unit = iterator.next();
          System.out.println(timex + "|" + unit.getName());
        } 
      }
    }
  }                                                                                                                          
}                                                                                                                            