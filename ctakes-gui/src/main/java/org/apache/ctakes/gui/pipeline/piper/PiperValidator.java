//package org.apache.ctakes.gui.pipeline.piper;
//
//
//import org.apache.ctakes.core.pipeline.CliOptionalsHandler;
//import org.apache.ctakes.core.resource.FileLocator;
//import org.apache.log4j.Logger;
//import org.apache.uima.UIMAException;
//import org.apache.uima.analysis_component.AnalysisComponent;
//import org.apache.uima.analysis_engine.AnalysisEngineDescription;
//import org.apache.uima.collection.CollectionReader;
//import org.apache.uima.resource.ResourceInitializationException;
//
//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 3/25/2017
// * TODO extract similar methods with PiperFileReader
// */
//final public class PiperValidator {
//
//   static private final Logger LOGGER = Logger.getLogger( "PiperValidator" );
//
//   static private final String[] CTAKES_PACKAGES
//         = { "core",
//             "assertion",
//             "chunker",
//             "clinicalpipeline",
//             "constituency.parser",
//             "contexttokenizer",
//             "coreference",
//             "dependency.parser",
//             "dictionary.lookup2",
//             "dictionary.lookup",
//             "temporal",
//             "drug-ner",
//             "lvg",
//             "necontexts",
//             "postagger",
//             "prepropessor",
//             "relationextractor",
//             "sideeffect",
//             "smokingstatus",
//             "template.filler" };
//
//   static private final Object[] EMPTY_OBJECT_ARRAY = new Object[ 0 ];
//
//   static private final Pattern KEY_VALUE_PATTERN = Pattern.compile( "=" );
//   static private final Pattern NAME_VALUE_PATTERN = Pattern
//         .compile( "[^\"\\s=]+=(?:(?:[^\"=\\s]+)|(?:\"[^\"=\\r\\n]+\"))" );
//
//   static private final char[] _reservedCli = { 'p', 'i', 'o', 's', 'l' };
//   private final Collection<String> _userPackages = new HashSet<>();
//
//
//
//   /**
//    * @param command   specified by first word in the file line
//    * @param parameter specified by second word in the file line
//    * @throws UIMAException if the command could not be executed
//    */
//   private boolean testParameter( final String command, final String parameter ) throws UIMAException {
//      if ( parameter.trim().isEmpty() ) {
//         return false;
//      }
//      switch ( command ) {
//         case "load":
//            return isValidPiper( parameter );
//         case "package":
//            return isPackageValid( parameter );
//         case "set":
//            return true;
//         case "cli":
//            return isCliValid( parameter );
//         case "reader":
//            if ( !isValidReader( parameter ) ) {
//               return false;
//            }
//            if ( hasParameters( parameter ) ) {
//               return isClassValid( parameter );
//            } else {
//               if ( hasParameters( parameter ) ) {
//                  final String[] component_parameters = splitFromParameters( parameter );
//                  final String component = component_parameters[ 0 ];
//                  final Object[] parameters = splitParameters( component_parameters[ 1 ] );
//                  _builder.reader( getReaderClass( component ), parameters );
//               } else {
//                  _builder.reader( getReaderClass( parameter ) );
//               }
//
//               _builder.reader( getReaderClass( parameter ) );
//            }
//            break;
//         case "readFiles":
//            if ( parameter.isEmpty() ) {
//               return true;
//            } else {
//               isPathValid( parameter );
//            }
//            break;
//         case "add":
//            if ( hasParameters( parameter ) ) {
//               final String[] component_parameters = splitFromParameters( parameter );
//               final String component = component_parameters[ 0 ];
//               final Object[] parameters = splitParameters( component_parameters[ 1 ] );
//               _builder.add( getComponentClass( component ), parameters );
//            } else {
//               return isValidAE( parameter );
//            }
//            break;
//         case "addLogged":
//            if ( hasParameters( parameter ) ) {
//               final String[] component_parameters = splitFromParameters( parameter );
//               final String component = component_parameters[ 0 ];
//               final Object[] parameters = splitParameters( component_parameters[ 1 ] );
//               _builder.addLogged( getComponentClass( component ), parameters );
//            } else {
//               return isValidAE( parameter );
//            }
//            break;
//         case "addDescription":
//            if ( hasParameters( parameter ) ) {
//               final String[] descriptor_parameters = splitFromParameters( parameter );
//               final String component = descriptor_parameters[ 0 ];
//               final Object[] values = splitDescriptorValues( descriptor_parameters[ 1 ] );
//               final AnalysisEngineDescription description = createDescription( component, values );
//               _builder.addDescription( description );
//            } else {
//               final AnalysisEngineDescription description = createDescription( parameter );
//               _builder.addDescription( description );
//            }
//            break;
//         case "addLast":
//            if ( hasParameters( parameter ) ) {
//               final String[] component_parameters = splitFromParameters( parameter );
//               final String component = component_parameters[ 0 ];
//               final Object[] parameters = splitParameters( component_parameters[ 1 ] );
//               _builder.addLast( getComponentClass( component ), parameters );
//            } else {
//               return isValidAE( parameter );
//            }
//            break;
//         case "collectCuis":
//            _builder.collectCuis();
//            break;
//         case "collectEntities":
//            _builder.collectEntities();
//            break;
//         case "writeXmis":
//            if ( parameter.isEmpty() ) {
//               _builder.writeXMIs();
//            } else {
//               _builder.writeXMIs( parameter );
//            }
//            break;
//         default:
//            LOGGER.error( "Unknown Command: " + command );
//      }
//      return false;
//   }
//
//   /**
//    *
//    * @param text -
//    * @return true if there is more than one word in the text
//    */
//   static private boolean hasParameters( final String text ) {
//      return SPACE_PATTERN.split( text ).length > 1;
//   }
//
//
//
//   /**
//    * @param filePath path to the pipeline command file
//    * @return true if the file is found
//    */
//   static private boolean isPathValid( final String filePath ) {
//      if ( filePath.isEmpty() ) {
//         return false;
//      }
//      return !FileLocator.getFullPathQuiet( filePath ).isEmpty();
//   }
//
//   private boolean isPackageValid( final String text ) {
//      if ( text.isEmpty() ) {
//         return false;
//      }
//      if ( hasParameters( text ) ) {
//         return false;
//      }
//      _userPackages.add( text );
//      return true;
//   }
//
//   static private boolean isClassValid( final String text ) {
//      if ( text.isEmpty() ) {
//         return false;
//      }
//      final int spaceIndex = text.indexOf( ' ' );
//      if ( spaceIndex > 0 ) {
//         return isClassValid( text.substring( 0, spaceIndex ) );
//      }
//      return true;
//   }
//
//   static private boolean isCliValid( final String text ) {
//      if ( text.isEmpty() ) {
//         return false;
//      }
//      final Matcher matcher = NAME_VALUE_PATTERN.matcher( text );
//      final List<String> pairList = new ArrayList<>();
//      while ( matcher.find() ) {
//         pairList.add( text.substring( matcher.start(), matcher.end() ) );
//      }
//      final String[] pairs = pairList.toArray( new String[ pairList.size() ] );
//      int i = 0;
//      for ( String pair : pairs ) {
//         final String[] keyAndValue = KEY_VALUE_PATTERN.split( pair );
//         if ( keyAndValue.length != 2 ) {
//            return false;
//         }
//         if ( keyAndValue[0].isEmpty() || keyAndValue[1].length() != 1 ){
//            return false;
//         }
//      }
//      return true;
//   }
//
//   private boolean isValidReader( final String className ) {
//      Class readerClass;
//      try {
//         readerClass = Class.forName( className );
//      } catch ( ClassNotFoundException cnfE ) {
//         readerClass = getPackagedReader( className );
//      }
//      if ( readerClass == null ) {
//         return false;
//      }
//      return isClassType( readerClass, CollectionReader.class );
//   }
//
//   /**
//    * @param className fully-specified or simple name of an ae or cc component class
//    * @return discovered class for ae or cc
//    */
//   private boolean isValidAE( final String className ) {
//      Class componentClass;
//      try {
//         componentClass = Class.forName( className );
//      } catch ( ClassNotFoundException cnfE ) {
//         componentClass = getPackagedComponent( className );
//      }
//      if ( componentClass == null ) {
//         return false;
//      }
//      return isClassType( componentClass, AnalysisComponent.class );
//   }
//
////
////   /**
////    * @param className fully-specified or simple name of an ae or cc component class
////    * @return discovered class for ae or cc
////    */
////   private Class<? extends AnalysisComponent> getComponentClass( final String className )  {
////      Class componentClass;
////      try {
////         componentClass = Class.forName( className );
////      } catch ( ClassNotFoundException cnfE ) {
////         componentClass = getPackagedComponent( className );
////      }
////      if ( componentClass != null&& isClassType( componentClass, AnalysisComponent.class ) ) {
////         return componentClass;
////      }
////      return null;
////   }
//
//   /**
//    * @param className simple name of a cr Collection Reader class
//    * @return discovered class for a cr
//    */
//   private Class<? extends CollectionReader> getPackagedReader( final String className ) {
//      Class readerClass;
//      for ( String packageName : _userPackages ) {
//         readerClass = getPackagedClass( packageName, className, CollectionReader.class );
//         if ( readerClass != null ) {
//            return readerClass;
//         }
//      }
//      for ( String packageName : CTAKES_PACKAGES ) {
//         readerClass = getPackagedClass(
//               "org.apache.ctakes." + packageName + ".cr", className, CollectionReader.class );
//         if ( readerClass != null ) {
//            return readerClass;
//         }
//         readerClass = getPackagedClass(
//               "org.apache.ctakes." + packageName, className, CollectionReader.class );
//         if ( readerClass != null ) {
//            return readerClass;
//         }
//      }
//      return null;
//   }
//
//   /**
//    * @param className fully-specified or simple name of an ae or cc component class
//    * @return discovered class for ae or cc
//    */
//   private Class<? extends AnalysisComponent> getPackagedComponent( final String className ) {
//      Class componentClass;
//      for ( String packageName : _userPackages ) {
//         componentClass = getPackagedClass( packageName, className, AnalysisComponent.class );
//         if ( componentClass != null ) {
//            return componentClass;
//         }
//      }
//      for ( String packageName : CTAKES_PACKAGES ) {
//         componentClass = getPackagedClass(
//               "org.apache.ctakes." + packageName + ".ae", className, AnalysisComponent.class );
//         if ( componentClass != null ) {
//            return componentClass;
//         }
//         componentClass = getPackagedClass(
//               "org.apache.ctakes." + packageName + ".cc", className, AnalysisComponent.class );
//         if ( componentClass != null ) {
//            return componentClass;
//         }
//         componentClass = getPackagedClass(
//               "org.apache.ctakes." + packageName, className, AnalysisComponent.class );
//         if ( componentClass != null ) {
//            return componentClass;
//         }
//      }
//      return null;
//   }
//
//   /**
//    * @param packageName     possible package for class
//    * @param className       simple name for class
//    * @param wantedClassType desired superclass type
//    * @return discovered class or null if no proper class was discovered
//    */
//   static private Class<?> getPackagedClass( final String packageName, final String className,
//                                             final Class<?> wantedClassType ) {
//      try {
//         Class<?> classType = Class.forName( packageName + "." + className );
//         if ( isClassType( classType, wantedClassType ) ) {
//            return classType;
//         }
//      } catch ( ClassNotFoundException cnfE ) {
//         // do nothing
//      }
//      return null;
//   }
//
//   /**
//    * @param filePath fully-specified or simple path of a piper file
//    * @return discovered path for the piper file
//    */
//   private boolean isValidPiper( final String filePath ) {
//      String fullPath = FileLocator.getFullPathQuiet( filePath );
//      if ( fullPath != null && !fullPath.isEmpty() ) {
//         return true;
//      }
//      // Check user packages
//      for ( String packageName : _userPackages ) {
//         fullPath = FileLocator.getFullPathQuiet( packageName.replace( '.', '/' ) + '/' + filePath );
//         if ( fullPath != null && !fullPath.isEmpty() ) {
//            return true;
//         }
//         fullPath = FileLocator.getFullPathQuiet( packageName.replace( '.', '/' ) + "/pipeline/" + filePath );
//         if ( fullPath != null && !fullPath.isEmpty() ) {
//            return true;
//         }
//      }
//      // Check ctakes packages
//      for ( String packageName : CTAKES_PACKAGES ) {
//         fullPath = FileLocator
//               .getFullPathQuiet( "org/apache/ctakes/" + packageName.replace( '.', '/' ) + '/' + filePath );
//         if ( fullPath != null && !fullPath.isEmpty() ) {
//            return true;
//         }
//         fullPath = FileLocator
//               .getFullPathQuiet( "org/apache/ctakes/" + packageName.replace( '.', '/' ) + "/pipeline/" + filePath );
//         if ( fullPath != null && !fullPath.isEmpty() ) {
//            return true;
//         }
//      }
//      return false;
//   }
//
//   /**
//    * @param classType       class type to test
//    * @param wantedClassType wanted class type
//    * @return true if the class type extends the wanted class type
//    */
//   static private boolean isClassType( final Class<?> classType, final Class<?> wantedClassType ) {
//      return wantedClassType.isAssignableFrom( classType );
//   }
//
//}
