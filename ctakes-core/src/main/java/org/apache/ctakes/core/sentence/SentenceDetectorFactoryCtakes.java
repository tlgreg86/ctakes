package org.apache.ctakes.core.sentence;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.sentdetect.SDContextGenerator;
import opennlp.tools.sentdetect.SentenceDetectorFactory;

public class SentenceDetectorFactoryCtakes extends SentenceDetectorFactory {

  // need empty constructor to allow this to be instantiated through reflection in opennlp classes
  public SentenceDetectorFactoryCtakes(){
    super();
  }
  
  public SentenceDetectorFactoryCtakes(char[] eosChars){
    super("en", true, new Dictionary(), eosChars);
  }
  
  @Override
  public SDContextGenerator getSDContextGenerator() {
    return new SDContextGeneratorCtakes(this.getEOSCharacters());
  }
}
