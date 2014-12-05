package org.apache.ctakes.core.sentence;

import java.util.ArrayList;

import opennlp.tools.sentdetect.DefaultSDContextGenerator;
import opennlp.tools.util.StringUtil;

public class SDContextGeneratorCtakes extends DefaultSDContextGenerator {

  // TODO -- is this threadsafe?? At the very least its not less thread-safe than existing data structures in parent class
  String ws = null;
  
  public SDContextGeneratorCtakes(char[] eosCharacters) {
    super(eosCharacters);
  }

  @Override
  public String[] getContext(CharSequence sb, int position) {
    // add features to addlFeats string array:
    int lastIndex = sb.length() - 1;
    int wsEnd = nextNonspaceIndex(sb, position, lastIndex);
    if(wsEnd != -1 && position != lastIndex){
      ws = new StringBuilder(sb.subSequence(position + 1, wsEnd)).toString();
    }

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
    return getShape(s).replaceAll("(.)\\1+", "$1+");
  }

  private static final int nextNonspaceIndex(CharSequence sb, int seek, int lastIndex) {
    while(seek < lastIndex){
      char c = sb.charAt(++seek);
      if(!StringUtil.isWhitespace(c)) return seek;
    }
    return lastIndex;
  }
}
