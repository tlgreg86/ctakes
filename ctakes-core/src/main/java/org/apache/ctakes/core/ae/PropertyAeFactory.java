package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * There may be some way to get values directly into the root UimaContext.
 * This factory can load plain old java properties files and pass the specified properties as parameters for AE creation
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/8/2016
 */
public enum PropertyAeFactory {
   INSTANCE;

   static public PropertyAeFactory getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "PropertyAeFactory" );


   final private Map<String, String> _properties = new HashMap<>();

   synchronized public void loadPropertyFile( final String filePath ) {
      try ( InputStream stream = FileLocator.getAsStream( filePath ) ) {
         final Properties properties = new Properties();
         properties.load( stream );
         for ( String name : properties.stringPropertyNames() ) {
            final String value = properties.getProperty( name );
            if ( value == null ) {
               LOGGER.warn( "Property has no value: " + name );
            } else {
               _properties.put( name, value );
            }
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Property File not found: " + filePath );
      }
   }

   static private Object[] createParameters( final Map<String, String> parameterMap ) {
      final Object[] parameters = new Object[ parameterMap.size() * 2 ];
      int i = 0;
      for ( Map.Entry<String, String> entry : parameterMap.entrySet() ) {
         parameters[ i ] = entry.getKey();
         parameters[ i + 1 ] = entry.getValue();
         i += 2;
      }
      return parameters;
   }

   /**
    * @param parameters parameters possibly not loaded by this factory
    * @return new parameter arrays containing parameters loaded by this factory and followed by specified parameters
    */
   synchronized private Object[] getAllParameters( final Object... parameters ) {
      if ( _properties.isEmpty() ) {
         return parameters;
      }
      if ( parameters == null || parameters.length == 0 ) {
         return createParameters( _properties );
      }
      final Map<String, String> parameterMap = new HashMap<>( _properties );
      for ( int i = 0; i < parameters.length; i += 2 ) {
         parameterMap.put( parameters[ i ].toString(), parameters[ i + 1 ].toString() );
      }
      return createParameters( parameterMap );
   }

   /**
    * This method should be avoided.  See the bottom of https://uima.apache.org/d/uimafit-current/api/index.html
    *
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Engine with specified parameters plus those loaded from properties
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngine createEngine( final Class<? extends AnalysisComponent> classType,
                                       final Object... parameters )
         throws ResourceInitializationException {
      final AnalysisEngineDescription description = createDescription( classType, parameters );
      final Object allParameters = getAllParameters( parameters );
      return AnalysisEngineFactory.createEngine( description, allParameters );
   }

   /**
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Description with specified parameters plus those loaded from properties
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngineDescription createDescription( final Class<? extends AnalysisComponent> classType,
                                                       final Object... parameters )
         throws ResourceInitializationException {
      final Object allParameters = getAllParameters( parameters );
      return AnalysisEngineFactory.createEngineDescription( classType, allParameters );
   }

   /**
    * This method should be avoided.  See the bottom of https://uima.apache.org/d/uimafit-current/api/index.html
    *
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Engine with specified parameters plus those loaded from properties that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngine createLoggedEngine( final Class<? extends AnalysisComponent> classType,
                                             final Object... parameters )
         throws ResourceInitializationException {
      final Object allParameters = getAllParameters( parameters );
      return StartFinishLogger.createLoggedEngine( classType, allParameters );
   }

   /**
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Description with specified parameters plus those loaded from properties that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngineDescription createLoggedDescription( final Class<? extends AnalysisComponent> classType,
                                                             final Object... parameters )
         throws ResourceInitializationException {
      final Object allParameters = getAllParameters( parameters );
      return StartFinishLogger.createLoggedDescription( classType, allParameters );
   }

   /**
    * @param mainDescription main component description
    * @return Description with specified parameters plus those loaded from properties that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngineDescription createLoggedDescription( final AnalysisEngineDescription mainDescription )
         throws ResourceInitializationException {
      return StartFinishLogger.createLoggedDescription( mainDescription );
   }


}
