package org.apache.ctakes.core.sentence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.ctakes.utils.struct.CounterMap;

import opennlp.tools.sentdetect.DefaultSDContextGenerator;
import opennlp.tools.util.StringUtil;

public class SDContextGeneratorCtakes extends DefaultSDContextGenerator {

  // TODO -- is this threadsafe?? At the very least its not less thread-safe than existing data structures in parent class
  String ws = null;
//  CounterMap<Integer> lenHist = null;
//  HashMap<Integer,Double> smoothHist = null;
  
  public SDContextGeneratorCtakes(char[] eosCharacters) {
    super(eosCharacters);
  }

  @Override
  public String[] getContext(CharSequence sb, int position){
    return getContext(sb, position, null, null);
  }
  
  public String[] getContext(CharSequence sb, int position, CounterMap<Integer> lenHist, HashMap<Integer, Double> smoothHist) {
    int ind = -1;
    StringBuffer text = new StringBuffer(sb.toString());
    
    if(text.charAt(position)== '>' &&
        text.charAt(position-1) == 'F' &&
        text.charAt(position-2) == 'L' &&
        text.charAt(position-3) == '<'){
      text.replace(position-3, position+1, "\n");
      position -= 3;
    }
    sb = text;
    
    // add features to addlFeats string array:
    int lastIndex = sb.length() - 1;
    int wsEnd = nextNonspaceIndex(sb, position, lastIndex);
    if(wsEnd != -1 && position != lastIndex){
      ws = new StringBuilder(sb.subSequence(position + 1, wsEnd)).toString();
    }

    /*
    int lastBreak = position-1;
    while(lastBreak > 0 && sb.charAt(lastBreak) != '\n'){
      lastBreak--;
    }
    int lineLen = position - lastBreak;
    char eosChar = sb.charAt(position);
    
    // line length-based features (requires document-level information)
    if(lenHist != null && smoothHist != null){
      if(eosChar == '\n'){
        int nextWordLen = 0;
        int nextNonWs = 0;
        while(Character.isWhitespace(sb.charAt(position+nextNonWs))){
          nextNonWs++;
        }
        while(Character.isLetterOrDigit(sb.charAt(position+nextNonWs+nextWordLen))){
          nextWordLen++;
        }
        int potLen = lineLen + nextWordLen;
        if(potLen >= 0){
          this.collectFeats.add("othersOfThisLen=" + (lenHist.get(potLen) > 0));

          boolean upSlope = (smoothHist.get(lineLen) > smoothHist.get(lineLen-1));
          boolean downSlope = (smoothHist.get(potLen) < smoothHist.get(potLen-1));

          this.collectFeats.add("upSlope="+upSlope);
          this.collectFeats.add("downSlope="+downSlope);
        }
      }
    }
    */
    return super.getContext(sb, position);    
  }
  
  private static String escapeChar(Character c) {
    if (c == '\n') {
      return "<LF>";
    }

    if (c == '\r') {
      return "<CR>";
    }

    return new String(new char[]{c});
  }

  @Override
  protected void collectFeatures(String prefix, String suffix, String previous, String next, Character eosChar) {
    super.collectFeatures(prefix, suffix, previous, next, eosChar);

    for(int i = 0; i < collectFeats.size(); i++){
      if(collectFeats.get(i).equals("eos=\n")){
        collectFeats.set(i, "eos=<LF>");
        break;
      }
    }
    if (!next.equals("")) {
      if(isAllUpper(next)) {
        collectFeats.add("nbold");
      }
    }
    buf.append("ws=");
    String featValue  = ws.replace("\n", "<LF>").replace("\t", "<SPACE>").replace(" ", "<SPACE>").replace("\r", "");
    buf.append(featValue);
    //    collectFeats.add(buf.toString());
    buf.setLength(0);

    buf.append("lfs=");
    String lfs = featValue.replace("<SPACE>", "");
    buf.append(lfs);
    collectFeats.add(buf.toString());
    buf.setLength(0);

    buf.append("eolws=");
    buf.append(escapeChar(eosChar));
    buf.append(',');
    buf.append(lfs);
    collectFeats.add(buf.toString());
    buf.setLength(0);

    buf.append("nextshape=");
    buf.append(getShape(next));
    //    collectFeats.add(buf.toString());
    buf.setLength(0);

    String collapsedShape = getCollapsedShape(next); 
    buf.append("collapsedNext=");
    buf.append(collapsedShape);
    collectFeats.add(buf.toString());
    buf.setLength(0);

    buf.append("collapasedPrev=");
    buf.append(getCollapsedShape(previous));
    collectFeats.add(buf.toString());
    buf.setLength(0);

    buf.append("collapsedPrefix=");
    buf.append(getCollapsedShape(prefix));
    collectFeats.add(buf.toString());
    buf.setLength(0);

  }
  
  private static final boolean isAllUpper(String s) {
    for(int i = 0; i < s.length(); i++){
      if(!Character.isUpperCase(s.charAt(i))){
        return false;
      }
    }
    return true;
  }

  private static final String getShape(String s){
    return s.replaceAll("\\p{Upper}", "U").replaceAll("\\p{Lower}", "L").replaceAll("\\p{Digit}", "D").replaceAll("\\p{Punct}","P");
  }

  private static final String getCollapsedShape(String s){
    return getShape(s).replaceAll("(.)\\1+", "$1+").replaceAll("D\\+?", "D+");
  }

  private static final int nextNonspaceIndex(CharSequence sb, int seek, int lastIndex) {
    while(seek < lastIndex){
      char c = sb.charAt(++seek);
      if(!StringUtil.isWhitespace(c)) return seek;
    }
    return lastIndex;
  }
}
