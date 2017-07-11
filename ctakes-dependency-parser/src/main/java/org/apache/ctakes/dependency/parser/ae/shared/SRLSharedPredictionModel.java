package org.apache.ctakes.dependency.parser.ae.shared;

import com.googlecode.clearnlp.nlp.NLPLib;

public class SRLSharedPredictionModel extends SRLSharedModel {
  public static final String DEFAULT_PRED_MODEL_FILE_NAME = 
      "org/apache/ctakes/dependency/parser/models/pred/mayo-en-pred-1.3.0.jar";
  
  @Override
  protected String getMode() {
    return NLPLib.MODE_PRED;
  }

  @Override
  protected String getDefaultModel() {
    return DEFAULT_PRED_MODEL_FILE_NAME;
  }

}
