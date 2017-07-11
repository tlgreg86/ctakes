package org.apache.ctakes.dependency.parser.ae.shared;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.reader.AbstractReader;

public abstract class SRLSharedModel implements SharedResourceObject {

  protected AbstractComponent component;
  protected String language = AbstractReader.LANG_EN;
  
  @Override
  public void load(DataResource aData) throws ResourceInitializationException {
    URI modelUri = aData.getUri();
    try{
      InputStream modelInputStream = (modelUri == null)
          ? FileLocator.getAsStream( this.getDefaultModel() )
              : FileLocator.getAsStream( modelUri.toString() );
          this.component = EngineGetter.getComponent( modelInputStream, this.language, this.getMode() );
    }catch(IOException e){
      throw new ResourceInitializationException(e);
    }
  }

  public AbstractComponent getComponent(){
    return this.component;
  }
  
  protected abstract String getMode();

  protected abstract String getDefaultModel();
}
