package org.apache.ctakes.dependency.parser.ae.shared;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;

public class DependencySharedModel implements SharedResourceObject {

  private AbstractComponent parser;
  public static final String DEFAULT_MODEL_FILE_NAME = "org/apache/ctakes/dependency/parser/models/dependency/mayo-en-dep-1.3.0.jar";
  final String language = AbstractReader.LANG_EN;
  public Logger logger = Logger.getLogger(getClass().getName());

  @Override
  public void load(DataResource aData) throws ResourceInitializationException {
    URI uri = aData.getUri();
    try{
      InputStream parserModel = (uri == null)
          ? FileLocator.getAsStream(DEFAULT_MODEL_FILE_NAME)
              : FileLocator.getAsStream(uri.getPath());

          this.parser = EngineGetter.getComponent(parserModel, this.language, NLPLib.MODE_DEP);
    }catch(IOException e){
      throw new ResourceInitializationException(e);
    }
  }

  public AbstractComponent getParser(){
    return parser;
  }
}
