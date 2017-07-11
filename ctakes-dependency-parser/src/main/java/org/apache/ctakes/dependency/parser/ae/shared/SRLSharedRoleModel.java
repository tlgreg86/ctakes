package org.apache.ctakes.dependency.parser.ae.shared;

import com.googlecode.clearnlp.nlp.NLPLib;

public class SRLSharedRoleModel extends SRLSharedModel {
  public static final String DEFAULT_ROLE_MODEL_FILE_NAME
  = "org/apache/ctakes/dependency/parser/models/role/mayo-en-role-1.3.0.jar";
  
  @Override
  protected String getMode() {
    return NLPLib.MODE_ROLE;
  }

  @Override
  protected String getDefaultModel() {
    return DEFAULT_ROLE_MODEL_FILE_NAME;
  }
}
