package org.apache.ctakes.temporal.nn.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

public class ArgContextProvider {
  /**
   * Return tokens between arg1 and arg2 as string 
   * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
   */
  public static String getRegions(JCas jCas, Sentence sent, Annotation left, Annotation right, int contextSize) {

    
    // tokens to the left from the left argument
    List<String> leftTokens = new ArrayList<>();
    for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
      if(sent.getBegin() <= baseToken.getBegin()) {
        leftTokens.add(baseToken.getCoveredText()); 
      }
    }
    String leftAsString = String.join(" ", leftTokens).replaceAll("[\r\n]", " ");
    
    // left arg tokens
    List<String> arg1Tokens = new ArrayList<>(); 
    for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, left)) {
      arg1Tokens.add(baseToken.getCoveredText());
    }
    String arg1AsString = String.join(" ", arg1Tokens).replaceAll("[\r\n]", " ");
    
    // tokens between the arguments
    List<String> betweenTokens = new ArrayList<>();
    for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
      betweenTokens.add(baseToken.getCoveredText());
    }
    String betweenAsString = String.join(" ", betweenTokens).replaceAll("[\r\n]", " ");
    
    // right arg tokens
    List<String> arg2Tokens = new ArrayList<>(); 
    for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, right)) {
      arg2Tokens.add(baseToken.getCoveredText());
    }
    String arg2AsString = String.join(" ", arg2Tokens).replaceAll("[\r\n]", " ");
    
    // tokens to the right from the right argument
    List<String> rightTokens = new ArrayList<>();
    for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
      if(baseToken.getEnd() <= sent.getEnd()) {
        rightTokens.add(baseToken.getCoveredText());
      }
    }
    String rightAsString = String.join(" ", rightTokens).replaceAll("[\r\n]", " ");
    
    return leftAsString + "|" + arg1AsString + "|" + betweenAsString + "|" + arg2AsString + "|" + rightAsString;
  }
  
  /**
   * Print words from left to right.
   * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
   */
  public static String getTokenContext(
      JCas jCas, 
      Sentence sent, 
      Annotation left,
      String leftType,
      Annotation right,
      String rightType,
      int contextSize) {

    List<String> tokens = new ArrayList<>();
    for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
      if(sent.getBegin() <= baseToken.getBegin()) {
        tokens.add(baseToken.getCoveredText()); 
      }
    }
    tokens.add("<" + leftType + ">");
    tokens.add(left.getCoveredText());
    tokens.add("</" + leftType + ">");
    for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
      tokens.add(baseToken.getCoveredText());
    }
    tokens.add("<" + rightType + ">");
    tokens.add(right.getCoveredText());
    tokens.add("</" + rightType + ">");
    for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
      if(baseToken.getEnd() <= sent.getEnd()) {
        tokens.add(baseToken.getCoveredText());
      }
    }

    return String.join(" ", tokens).replaceAll("[\r\n]", " ");
  }

  /**
   * Print POS tags from left to right.
   * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
   */
  public static String getPosContext(
      JCas jCas, 
      Sentence sent, 
      Annotation left,
      String leftType,
      Annotation right,
      String rightType,
      int contextSize) {

    List<String> tokens = new ArrayList<>();
    for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
      if(sent.getBegin() <= baseToken.getBegin()) {
        tokens.add(baseToken.getPartOfSpeech()); 
      }
    }
    tokens.add("<" + leftType + ">");
    for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, left)) {
      tokens.add(baseToken.getPartOfSpeech());
    }
    tokens.add("</" + leftType + ">");
    for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
      tokens.add(baseToken.getPartOfSpeech());
    }
    tokens.add("<" + rightType + ">");
    for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, right)) {
      tokens.add(baseToken.getPartOfSpeech());
    }
    tokens.add("</" + rightType + ">");
    for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
      if(baseToken.getEnd() <= sent.getEnd()) {
        tokens.add(baseToken.getPartOfSpeech());
      }
    }

    return String.join(" ", tokens).replaceAll("[\r\n]", " ");
  }
}
