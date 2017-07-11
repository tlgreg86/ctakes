package org.apache.ctakes.dependency.parser.ae.shared;

import com.googlecode.clearnlp.nlp.NLPLib;

public class SRLSharedParserModel extends SRLSharedModel {
  public static final String DEFAULT_SRL_MODEL_FILE_NAME = 
      "org/apache/ctakes/dependency/parser/models/srl/mayo-en-srl-1.3.0.jar";

  @Override
  protected String getMode() {
    return NLPLib.MODE_SRL;
  }

  @Override
  protected String getDefaultModel() {
    return DEFAULT_SRL_MODEL_FILE_NAME;
  }
}
