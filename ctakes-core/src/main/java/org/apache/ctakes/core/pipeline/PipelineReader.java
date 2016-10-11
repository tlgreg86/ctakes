package org.apache.ctakes.core.pipeline;


import org.apache.ctakes.core.cc.XmiWriterCasConsumerCtakes;
import org.apache.ctakes.core.cr.FilesInDirectoryCollectionReader;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Creates a pipeline (PipelineBuilder) from specifications in a flat plaintext file.
 * <p>
 * <p>There are several basic commands:
 * addPackage <i>user_package_name</i>
 * loadParameters <i>path_to_properties_file_with_ae_parameters</i>
 * addParameters <i>ae_parameter_name</i>|<i>ae_parameter_value</i>| ...
 * reader <i>collection_reader_class_name</i>
 * readFiles <i>input_directory</i>
 * <i>input_directory</i> can be empty if {@link FilesInDirectoryCollectionReader#PARAM_INPUTDIR} was specified
 * add <i>ae_or_cc_class_name</i>
 * addLogged <i>ae_or_cc_class_name</i>
 * collectCuis
 * collectEntities
 * writeXmis <i>output_directory</i>
 * <i>output_directory</i> can be empty if {@link XmiWriterCasConsumerCtakes#PARAM_OUTPUTDIR} was specified
 * <p>
 * # and // may be used to mark line comments
 * </p>
 * <p>
 * class names must be fully-specified with package unless they are in standard ctakes cr ae or cc packages,
 * or in a package specified by an earlier addPackage command.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2016
 */
final public class PipelineReader {

   static private final Logger LOGGER = Logger.getLogger( "PipelineReader" );

   static private final String[] CTAKES_PACKAGES
         = { "core",
             "assertion",
             "chunker",
             "clinicalpipeline",
             "constituency.parser",
             "contexttokenizer",
             "coreference",
             "dependency.parser",
             "dictionary.lookup2",
             "dictionary.lookup",
             "temporal",
             "drug-ner",
             "lvg",
             "necontexts",
             "postagger",
             "prepropessor",
             "relationextractor",
             "sideeffect",
             "smokingstatus",
             "template.filler" };

   static private final Object[] EMPTY_OBJECT_ARRAY = new Object[ 0 ];

   static private final Pattern SPLIT_PATTERN = Pattern.compile( "\\|" );

   private PipelineBuilder _builder;

   private final Collection<String> _userPackages;


   /**
    * Create and empty PipelineReader
    */
   public PipelineReader() {
      _builder = new PipelineBuilder();
      _userPackages = new ArrayList<>();
   }

   /**
    * Create a PipelineReader and load a file with command parameter pairs for building a pipeline
    *
    * @param filePath path to the pipeline command file
    * @throws UIMAException if the pipeline cannot be loaded
    */
   public PipelineReader( final String filePath ) throws UIMAException {
      _builder = new PipelineBuilder();
      _userPackages = new ArrayList<>();
      loadPipelineFile( filePath );
   }

   /**
    * Load a file with command parameter pairs for building a pipeline
    *
    * @param filePath path to the pipeline command file
    */
   public void loadPipelineFile( final String filePath ) throws UIMAException {
      try ( final BufferedReader reader
                  = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( filePath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            line = line.trim();
            if ( line.isEmpty() || line.startsWith( "//" ) || line.startsWith( "#" ) ) {
               line = reader.readLine();
               continue;
            }
            final int spaceIndex = line.indexOf( ' ' );
            if ( spaceIndex < 3 ) {
               addToPipeline( line, "" );
            } else {
               addToPipeline( line.substring( 0, spaceIndex ), line.substring( spaceIndex + 1 ).trim() );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Property File not found: " + filePath );
      }
   }

   /**
    * @return the PipelineBuilder with its current state set by this PipelineReader
    */
   public PipelineBuilder getBuilder() {
      return _builder;
   }

   /**
    * @param command   specified by first word in the file line
    * @param parameter specified by second word in the file line
    * @throws UIMAException if the command could not be executed
    */
   private void addToPipeline( final String command, final String parameter ) throws UIMAException {
      switch ( command ) {
         case "addPackage":
            _userPackages.add( parameter );
            break;
         case "loadParameters":
            _builder.loadParameters( parameter );
            break;
         case "addParameters":
            _builder.addParameters( getStrings( parameter ) );
            break;
         case "reader":
            _builder.reader( createReader( parameter ) );
            break;
         case "readFiles":
            if ( parameter.isEmpty() ) {
               _builder.readFiles();
            } else {
               _builder.readFiles( parameter );
            }
            break;
         case "add":
            _builder.add( getComponentClass( parameter ) );
            break;
         case "addLogged":
            _builder.addLogged( getComponentClass( parameter ) );
            break;
         case "collectCuis":
            _builder.collectCuis();
            break;
         case "collectEntites":
            _builder.collectEntities();
            break;
         case "writeXmis":
            if ( parameter.isEmpty() ) {
               _builder.writeXMIs();
            } else {
               _builder.writeXMIs( parameter );
            }
            break;
         default:
            LOGGER.error( "Unknown Command: " + command );
      }
   }

   /**
    * @param className fully-specified or simple name of an ae or cc component class
    * @return discovered class for ae or cc
    * @throws ResourceInitializationException if the class could not be found
    */
   private Class<? extends AnalysisComponent> getComponentClass( final String className ) throws
                                                                                          ResourceInitializationException {
      Class componentClass;
      try {
         componentClass = Class.forName( className );
      } catch ( ClassNotFoundException cnfE ) {
         componentClass = getPackagedComponent( className );
      }
      if ( componentClass == null ) {
         throw new ResourceInitializationException(
               "No Analysis Component found for " + className, EMPTY_OBJECT_ARRAY );
      }
      assertClassType( componentClass, AnalysisComponent.class );
      return componentClass;
   }

   /**
    * @param className fully-specified or simple name of an ae or cc component class
    * @return discovered class for ae or cc
    */
   private Class<? extends AnalysisComponent> getPackagedComponent( final String className ) {
      Class componentClass;
      for ( String packageName : _userPackages ) {
         componentClass = getPackagedClass( packageName, className, AnalysisComponent.class );
         if ( componentClass != null ) {
            return componentClass;
         }
      }
      for ( String packageName : CTAKES_PACKAGES ) {
         componentClass = getPackagedClass(
               "org.apache.ctakes." + packageName + ".ae", className, AnalysisComponent.class );
         if ( componentClass != null ) {
            return componentClass;
         }
         componentClass = getPackagedClass(
               "org.apache.ctakes." + packageName + ".cc", className, AnalysisComponent.class );
         if ( componentClass != null ) {
            return componentClass;
         }
      }
      return null;
   }

   /**
    * @param className fully-specified or simple name of a cr Collection Reader class
    * @return instantiated collection reader
    * @throws ResourceInitializationException if the class could not be found or instantiated
    */
   private CollectionReader createReader( final String className ) throws ResourceInitializationException {
      Class<?> readerClass;
      try {
         readerClass = Class.forName( className );
      } catch ( ClassNotFoundException cnfE ) {
         readerClass = getPackagedReader( className );
      }
      if ( readerClass == null ) {
         throw new ResourceInitializationException( "No Collection Reader found for " + className, EMPTY_OBJECT_ARRAY );
      }
      assertClassType( readerClass, CollectionReader.class );
      final Constructor<?>[] constructors = readerClass.getConstructors();
      for ( Constructor<?> constructor : constructors ) {
         try {
            if ( constructor.getParameterTypes().length == 0 ) {
               return (CollectionReader)constructor.newInstance();
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException iniaitE ) {
            throw new ResourceInitializationException(
                  "Could not construct " + className, EMPTY_OBJECT_ARRAY, iniaitE );
         }
      }
      throw new ResourceInitializationException( "No Constructor for " + className, EMPTY_OBJECT_ARRAY );
   }

   /**
    * @param className simple name of a cr Collection Reader class
    * @return discovered class for a cr
    */
   private Class<? extends CollectionReader> getPackagedReader( final String className ) {
      Class readerClass;
      for ( String packageName : _userPackages ) {
         readerClass = getPackagedClass( packageName, className, CollectionReader.class );
         if ( readerClass != null ) {
            return readerClass;
         }
      }
      for ( String packageName : CTAKES_PACKAGES ) {
         readerClass = getPackagedClass(
               "org.apache.ctakes." + packageName + ".cr", className, CollectionReader.class );
         if ( readerClass != null ) {
            return readerClass;
         }
      }
      return null;
   }

   /**
    * @param packageName     possible package for class
    * @param className       simple name for class
    * @param wantedClassType desired superclass type
    * @return discovered class or null if no proper class was discovered
    */
   static private Class<?> getPackagedClass( final String packageName, final String className,
                                             final Class<?> wantedClassType ) {
      try {
         Class<?> classType = Class.forName( packageName + "." + className );
         if ( isClassType( classType, wantedClassType ) ) {
            return classType;
         }
      } catch ( ClassNotFoundException cnfE ) {
         // do nothing
      }
      return null;
   }

   /**
    * @param classType       class type to test
    * @param wantedClassType wanted class type
    * @throws ResourceInitializationException if the class type does not extend the wanted class type
    */
   static private void assertClassType( final Class<?> classType, final Class<?> wantedClassType )
         throws ResourceInitializationException {
      if ( !isClassType( classType, wantedClassType ) ) {
         throw new ResourceInitializationException(
               "Not " + wantedClassType.getSimpleName() + " " + classType.getName(), EMPTY_OBJECT_ARRAY );
      }
   }

   /**
    * @param classType       class type to test
    * @param wantedClassType wanted class type
    * @return true if the class type extends the wanted class type
    */
   static private boolean isClassType( final Class<?> classType, final Class<?> wantedClassType ) {
      return wantedClassType.isAssignableFrom( classType );
   }

   /**
    * @param parameter text
    * @return array created by splitting text at '|' characters
    */
   static private String[] getStrings( final String parameter ) {
      return SPLIT_PATTERN.split( parameter );
   }


}
