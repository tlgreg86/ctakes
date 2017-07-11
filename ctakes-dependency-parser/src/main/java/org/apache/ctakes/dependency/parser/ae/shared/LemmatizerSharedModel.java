package org.apache.ctakes.dependency.parser.ae.shared;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.reader.AbstractReader;

public class LemmatizerSharedModel implements SharedResourceObject {

  public static final String ENG_LEMMATIZER_DATA_FILE = "org/apache/ctakes/dependency/parser/models/lemmatizer/dictionary-1.3.1.jar";

  private AbstractMPAnalyzer lemmatizer = null;
  final String language = AbstractReader.LANG_EN;
  public Logger logger = Logger.getLogger(getClass().getName());

  @Override
  public void load(DataResource aData) throws ResourceInitializationException {
    URI modelUri = aData.getUri();
    try{
      InputStream lemmatizerModel = (modelUri == null)
          ? FileLocator.getAsStream(ENG_LEMMATIZER_DATA_FILE)
              : FileLocator.getAsStream(modelUri.getPath());
          
      this.lemmatizer = EngineGetter.getMPAnalyzer(language, lemmatizerModel);
    }catch(IOException e){
      throw new ResourceInitializationException(e);
    }
  }

  public AbstractMPAnalyzer getLemmatizerModel(){
    return this.lemmatizer;
  }

}
