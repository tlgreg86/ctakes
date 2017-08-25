package org.apache.ctakes.temporal.utils;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

public class PatientViewsUtil {

  public static final String GOLD_PREFIX = "GoldView";
  public static final String VIEW_PREFIX = "DocView";
  public static final String URI_PREFIX = "UriView";
  public static final String NUM_DOCS_NAME = "NumDocsView";
  
  public static String getViewName(int i){
    return String.join("_", VIEW_PREFIX, String.valueOf(i));
  }

  public static String getGoldViewName(int i) {
    return String.join("_", GOLD_PREFIX, String.valueOf(i));
  }
  
  public static String getUriViewName(int i){
    return String.join("_", URI_PREFIX, String.valueOf(i));
  }
  
  public static String getNumDocsViewName() {
    return NUM_DOCS_NAME;
  }

  public static boolean isGoldView(JCas jcas){
    return jcas.getViewName().startsWith(GOLD_PREFIX);
  }
  
  public static String getAnnotatorName(String rawName, int viewNum){
    return String.join("_", rawName, "DocumentIndex", String.valueOf(viewNum));
  }
  
  public AnalysisEngineDescription getRenamedPipelineForDoc(AggregateBuilder builder, int i) throws ResourceInitializationException{
    AnalysisEngineDescription aed = builder.createAggregateDescription();
    
    return aed;
  }
}
