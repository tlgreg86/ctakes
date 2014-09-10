/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dictionary.lookup2.dictionary;

import org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory;
import org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer;
import org.apache.ctakes.dictionary.lookup2.util.DictionarySpec;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Parses the XML descriptor indicated by the {@code externalResource} for {@code DictionaryDescriptorFile}
 * in the XML descriptor for the Rare Word Term Lookup Annotator
 * {@link org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator}
 * </p>
 * If there is a problem with the descriptor then the whole pipeline goes down, so care must be taken by the User
 * and any messages (logged or otherwise) produced by this class should be as specific as possible.  Devs take notice.
 * <p/>
 * TODO
 * This parser can create a RareWordDictionary by wrapping the older Jdbc, Lucene, StringTable (CSV) descriptors.
 * However, to prevent the dependency upon the current Dictionary-Lookup module and its "Dictionary" interface,
 * all such code has been commented out.  Uncommenting, linking, and rebuilding is possible if use of an older dictionary
 * resource is required.
 * TODO
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
final public class DictionaryDescriptorParser {

   // LOG4J logger based on class name
   static private final Logger LOGGER = Logger.getLogger( "DictionaryDescriptorParser" );

   /**
    * A <B>Utility Class</B> cannot be instantiated
    */
   private DictionaryDescriptorParser() {
   }

   /**
    * XML keys specifying the main sections that define dictionaries, concept factories, and the pairing of the two
    */
   static private final String DICTIONARIES_KEY = "dictionaries";
   static private final String CONCEPT_FACTORIES_KEY = "conceptFactories";
   static private final String PAIRS_KEY = "dictionaryConceptPairs";


   /**
    * Each {@link RareWordDictionary} should have an id that specifies a unique name for that dictionary
    */
   static private final String NAME_ID = "id";
   /**
    * Each {@link RareWordDictionary} must have an external resource specified by the
    * {@code configurableDataResourceSpecifier} in the XML descriptor for the Rare Word Term Lookup Annotator
    * {@link org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator}.
    * The external resource <i>does not</i> need to be unique for each dictionary.
    */
   static private final String EXTERNAL_RESOURCE = "externalResourceKey";
   /**
    * Each {@link RareWordDictionary} can utilize or ignore the case of terms.   In most situations case sensitivity
    * is not beneficial, but it may be for some.  For instance, if it is an acronym dictionary then differentiating
    * between "WHO" (World Health Organization) and "who" is important.
    * The {@link org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator}
    * does ignores case and ignores this setting
    */
   static private final String CASE_SENSITIVE = "caseSensitive";
   /**
    * Each {@link RareWordDictionary} should have a numerical {@code typeId} that indicates the semantic group
    * to which the terms in the dictionary belong.  The standard cTakes type ids are numerical and listed in
    * {*link org.apache.ctakes.typesystem.type.constants.CONST} as
    * <ul>
    * <li>0  Unknown</li>
    * <li>1  Medication / Drug</li>
    * <li>2  Disease / Disorder</li>
    * <li>3  Sign / Symptom (Finding)</li>
    * <li>4  <i>Not Defined</i></li>
    * <li>5  Procedure</li>
    * <li>6  Anatomical Site</li>
    * <li>7  Clinical Attribute</li>
    * <li>8  Device</li>
    * <li>9  Lab</li>
    * <li>10 Phenomena</li>
    * </ul>
    * In truth, any coding scheme (Numerical or otherwise) can be used as long as a {@link org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer}
    * is created to use it. That being said ...
    */
   private static final String TYPE_ID = "typeId";
   /**
    * Each {@link RareWordDictionary} must have a java implementation.
    * It is best if this is a {@link RareWordDictionary},
    * but it can also be an older org apache ctakes dictionary lookup Dictionary, in which case a
    * org apache ctakes dictionary lookup2 dictionary RareWordDictionaryWrapper will be used.
    * <p>The available implementation keys are:</p>
    * <ul>
    * <li>rareWordJdbc</li>
    * <li>rareWordUmls</li>
    * <li>rareWordBsv</li>
    * <li>luceneImpl</li>
    * <li>jdbcImpl</li>
    * <li>csvImpl</li>
    * </ul>
    */
   private static final String IMPLEMENTATION = "implementation";

   /**
    * XML key specifying the section that defines the single
    * {@link org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory} that should be used to create concepts for discovered terms.
    */
   static private final String CONCEPTS_KEY = "conceptFactory";


   /**
    * XML key specifying the section that defines the single {@link org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer} that should be used to
    * consume discovered terms.
    */
   static private final String CONSUMER_KEY = "rareWordConsumer";

   // Added 'maxListSize'.  Size equals max int by default  - used for lucene dictionaries
   private static int MAX_LIST_SIZE = Integer.MAX_VALUE; //ohnlp-Bugs-3296301

   /**
    * Initiates the parsing of the XML descriptor file containing definition of dictionaries and a consumer for the
    * Rare Word Term dictionary paradigm
    *
    * @param descriptorFile XML-formatted file, see the dictionary-lookup resources file {@code RareWordTermsUMLS.xml}
    *                       for an example
    * @param uimaContext    -
    * @return {@link org.apache.ctakes.dictionary.lookup2.util.DictionarySpec} with specification of dictionaries and a consumer as read from the
    * {@code descriptorFile}
    * @throws AnnotatorContextException if the File could not be found/read or the xml could not be parsed
    */
   static public DictionarySpec parseDescriptor( final File descriptorFile, final UimaContext uimaContext )
         throws AnnotatorContextException {
      LOGGER.info( "Parsing dictionary specifications: " + descriptorFile.getPath() );
      final SAXBuilder saxBuilder = new SAXBuilder();
      Document doc;
      try {
         doc = saxBuilder.build( descriptorFile );
      } catch ( JDOMException | IOException jdomioE ) {
         throw new AnnotatorContextException( "Could not parse " + descriptorFile.getPath(), new Object[0], jdomioE );
      }
      final Map<String, RareWordDictionary> dictionaries
            = parseDictionaries( uimaContext, doc.getRootElement().getChild( DICTIONARIES_KEY ) );
      final Map<String, ConceptFactory> conceptFactories
            = parseConceptFactories( uimaContext, doc.getRootElement().getChild( CONCEPT_FACTORIES_KEY ) );
      final Map<String, String> pairDictionaryNames
            = parsePairingNames( doc.getRootElement().getChild( PAIRS_KEY ), "dictionaryName" );
      final Map<String, String> pairConceptFactoryNames
            = parsePairingNames( doc.getRootElement().getChild( PAIRS_KEY ), "conceptFactoryName" );
      final TermConsumer consumer = parseConsumerXml( uimaContext, doc.getRootElement().getChild( CONSUMER_KEY ) );
      return new DictionarySpec( pairDictionaryNames, pairConceptFactoryNames, dictionaries, conceptFactories,
            consumer );
   }

   /**
    * Creates dictionary engines by parsing the section defined by {@link this.DICTIONARIES_KEY}
    *
    * @param uimaContext         -
    * @param dictionariesElement contains definition of all dictionaries
    * @return Mapping of dictionary names to new {@link RareWordDictionary} instances
    * @throws AnnotatorContextException if the resource specified by {@link this.EXTERNAL_RESOURCE} does not match
    *                                   the type specified by {@link this.IMPLEMENTATION} or for some reason could not be used
    */
   static private Map<String, RareWordDictionary> parseDictionaries( final UimaContext uimaContext,
                                                                     final Element dictionariesElement )
         throws AnnotatorContextException {
      final Map<String, RareWordDictionary> dictionaries = new HashMap<>();
      final Collection dictionaryElements = dictionariesElement.getChildren();
      for ( Object dictionaryElement : dictionaryElements ) {
         if ( dictionaryElement instanceof Element ) {
            final RareWordDictionary dictionary = parseDictionary( uimaContext, (Element)dictionaryElement );
            if ( dictionary != null ) {
               dictionaries.put( dictionary.getName(), dictionary );
            }
         }
      }
      return dictionaries;
   }


   /**
    * Creates a dictionary by parsing each child element of {@link this.DICTIONARIES_KEY}
    *
    * @param uimaContext       -
    * @param dictionaryElement contains the definition of a single dictionary
    * @return a dictionary or null if there is a problem
    * @throws AnnotatorContextException if any of a dozen things goes wrong
    */
   private static RareWordDictionary parseDictionary( final UimaContext uimaContext, final Element dictionaryElement )
         throws AnnotatorContextException {
      final Class[] constructionArgs = { String.class, UimaContext.class, Properties.class };

      final String name = getName( "Dictionary Name", dictionaryElement );
      final String className = dictionaryElement.getChildText( "implementationName" );
      final Element propertiesElement = dictionaryElement.getChild( "properties" );
      final Properties properties = parsePropertiesXml( propertiesElement );
      Class dictionaryClass;
      try {
         dictionaryClass = Class.forName( className );
      } catch ( ClassNotFoundException cnfE ) {
         throw new AnnotatorContextException( "Unknown class " + className, new Object[0], cnfE );
      }
      if ( !RareWordDictionary.class.isAssignableFrom( dictionaryClass ) ) {
         throw new AnnotatorContextException( className + " is not a Rare Word Dictionary", new Object[0] );
      }
      final Constructor[] constructors = dictionaryClass.getConstructors();
      for ( Constructor constructor : constructors ) {
         try {
            if ( Arrays.equals( constructionArgs, constructor.getParameterTypes() ) ) {
               final Object[] args = new Object[]{ name, uimaContext, properties };
               return (RareWordDictionary)constructor.newInstance( args );
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException iniaitE ) {
            throw new AnnotatorContextException( "Could not construct " + className, new Object[0], iniaitE );
         }
      }
      throw new AnnotatorContextException( "No Constructor for " + className, new Object[0] );
   }


   /**
    * Creates concept factories by parsing the section defined by {@link this.CONCEPT_FACTORY_KEY
    *
    * @param uimaContext             -
    * @param conceptFactoriesElement contains definition of all concept factories
    * @return Mapping of concept factory names to new {@link ConceptFactory} instances
    * @throws AnnotatorContextException if the resource specified by {@link this.EXTERNAL_RESOURCE} does not match
    *                                   the type specified by {@link this.IMPLEMENTATION} or for some reason could not be used
    */
   static private Map<String, ConceptFactory> parseConceptFactories( final UimaContext uimaContext,
                                                                     final Element conceptFactoriesElement )
         throws AnnotatorContextException {
      final Map<String, ConceptFactory> conceptFactories = new HashMap<>();
      final Collection conceptFactoryElements = conceptFactoriesElement.getChildren();
      for ( Object conceptFactoryElement : conceptFactoryElements ) {
         if ( conceptFactoryElement instanceof Element ) {
            final ConceptFactory conceptFactory = parseConceptFactory( uimaContext, (Element)conceptFactoryElement );
            if ( conceptFactory != null ) {
               conceptFactories.put( conceptFactory.getName(), conceptFactory );
            }
         }
      }
      return conceptFactories;
   }

   /**
    * Creates a dictionary by parsing each child element of {@link this.DICTIONARIES_KEY}
    *
    * @param uimaContext           -
    * @param conceptFactoryElement contains the definition of a single dictionary
    * @return a dictionary or null if there is a problem
    * @throws AnnotatorContextException if any of a dozen things goes wrong
    */
   private static ConceptFactory parseConceptFactory( final UimaContext uimaContext,
                                                      final Element conceptFactoryElement )
         throws AnnotatorContextException {
      final Class[] constructionArgs = { String.class, UimaContext.class, Properties.class };
      final String name = getName( "Concept Factory Name", conceptFactoryElement );
      final String className = conceptFactoryElement.getChildText( "implementationName" );
      final Element propertiesElement = conceptFactoryElement.getChild( "properties" );
      final Properties properties = parsePropertiesXml( propertiesElement );
      Class conceptFactoryClass;
      try {
         conceptFactoryClass = Class.forName( className );
      } catch ( ClassNotFoundException cnfE ) {
         throw new AnnotatorContextException( "Unknown class " + className, new Object[0], cnfE );
      }
      if ( !ConceptFactory.class.isAssignableFrom( conceptFactoryClass ) ) {
         throw new AnnotatorContextException( className + " is not a Concept Factory", new Object[0] );
      }
      final Constructor[] constructors = conceptFactoryClass.getConstructors();
      for ( Constructor constructor : constructors ) {
         try {
            if ( Arrays.equals( constructionArgs, constructor.getParameterTypes() ) ) {
               final Object[] args = new Object[]{ name, uimaContext, properties };
               return (ConceptFactory)constructor.newInstance( args );
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException iniaitE ) {
            throw new AnnotatorContextException( "Could not construct " + className, new Object[0], iniaitE );
         }
      }
      throw new AnnotatorContextException( "No Constructor for " + className, new Object[0] );
   }


   /**
    * @param pairingsElement -
    * @param pairingName     one of "dictionaryName" or "conceptFactoryName"
    * @return -
    * @throws AnnotatorContextException -
    */
   static private Map<String, String> parsePairingNames( final Element pairingsElement, final String pairingName )
         throws AnnotatorContextException {
      final Map<String, String> pairConceptFactoryNames = new HashMap<>();
      final Collection pairingElements = pairingsElement.getChildren();
      for ( Object pairingElement : pairingElements ) {
         if ( pairingElement instanceof Element ) {
            final String pairName = getName( "Dictionary - Concept Factory Pairing", (Element)pairingElement );
            final String conceptFactorName = ((Element)pairingElement).getChildText( pairingName );
            pairConceptFactoryNames.put( pairName, conceptFactorName );
         }
      }
      return pairConceptFactoryNames;
   }

   static private String getName( final String elementName, final Element element ) throws AnnotatorContextException {
      final String name = element.getChildText( "name" );
      if ( name == null || name.isEmpty() ) {
         throw new AnnotatorContextException( "Missing name for " + elementName, new Object[0] );
      }
      return name;
   }


//
//
//
//
//   /**
//    * Creates a dictionary by parsing each child element of {@link this.DICTIONARIES_KEY}
//    *
//    * @param uimaContext       -
//    * @param dictionaryElement contains the definition of a single dictionary
//    * @return a dictionary or null if there is a problem
//    * @throws AnnotatorContextException if any of a dozen things goes wrong
//    */
//   private static RareWordDictionary parseDictionaryXml( final UimaContext uimaContext,
//                                                         final Element dictionaryElement )
//         throws AnnotatorContextException {
//      final String externalResourceKey = dictionaryElement.getAttributeValue( EXTERNAL_RESOURCE );
//      final Boolean keepCase = Boolean.valueOf( dictionaryElement.getAttributeValue( CASE_SENSITIVE ) );
//      final String entityTypeId = dictionaryElement.getAttributeValue( TYPE_ID );
//      Object externalResource;
//      try {
//         externalResource = uimaContext.getResourceObject( externalResourceKey );
//      } catch ( ResourceAccessException raE ) {
//         throw new AnnotatorContextException( "Could not access external resource " + externalResourceKey,
//                                              new Object[0], raE );
//      }
//      if ( externalResource == null ) {
//         throw new AnnotatorContextException( "Could not find external resource " + externalResourceKey,
//                                              new Object[0] );
//      }
//      RareWordDictionary dictionary = null;
//      final Element implementationElement = (Element) dictionaryElement.getChild( IMPLEMENTATION ).getChildren().get( 0 );
//      final String implementationName = implementationElement.getName();
//      if ( implementationName.equals( "rareWordJdbc" ) ) {
//         dictionary = DictionaryFactory.createRareWordJdbc( implementationElement,
//                                                            externalResource,
//                                                            entityTypeId );
//      } else if ( implementationName.equals( "rareWordUmls" ) ) {
//         // TODO move umls info to the dictionary descriptor and parse parameter values here
//         // final String externalResourceKey = dictionaryElement.getAttributeValue( EXTERNAL_RESOURCE );
//         // TODO eventually move the umls dictionary download to a secure server with password protection
//         try {
//            // TODO attempt user etc. fetch from uimaContext.  If empty, attempt fetch from dictionaryElement
//            UmlsUserApprover.validateUMLSUser( uimaContext );
//            dictionary = DictionaryFactory.createRareWordJdbc( implementationElement,
//                                                               externalResource,
//                                                               entityTypeId );
//         } catch ( ResourceInitializationException riE ) {
//            throw new AnnotatorContextException( riE );
//         }
//      } else if ( implementationName.equals( "rareWordBsv" ) ) {
//         dictionary = DictionaryFactory.createRareWordBsv( externalResourceKey, externalResource, entityTypeId );
////      } else if ( implementationName.equals( "luceneImpl" ) ) {
////         dictionary = DictionaryFactory.createWrappedLucene( dictionaryElement,
////                                                                     externalResourceKey,
////                                                                     externalResource,
////                                                                     entityTypeId );
////      } else if ( implementationName.equals( "jdbcImpl" ) ) {
////         dictionary = DictionaryFactory.createWrappedJdbc( dictionaryElement,
////                                                                   implementationElement,
////                                                                   externalResourceKey,
////                                                                   externalResource,
////                                                                   entityTypeId );
////      } else if ( implementationName.equals( "csvImp" ) ) {
////         dictionary = DictionaryFactory.createWrappedCsv( dictionaryElement,
////                                                                  implementationElement,
////                                                                  externalResourceKey,
////                                                                  externalResource,
////                                                                  entityTypeId );
//      } else {
//         throw new AnnotatorContextException( "Unsupported dictionary implementation " + implementationName,
//                                              new Object[0] );
//      }
//      if ( dictionary == null ) {
//         throw new AnnotatorContextException( "No appropriate dictionary defined", new Object[0] );
//      }
//      // Deprecated -
////      if ( dictionary instanceof Dictionary ) {
////         final Collection metaFields = dictionaryElement.getChild( "metaFields" ).getChildren();
////         for ( Object value : metaFields ) {
////            String metaFieldName = ((Element) value).getAttributeValue( "fieldName" );
////            ((Dictionary) dictionary).retainMetaData( metaFieldName );
////         }
////      }
//      return dictionary;
//   }


   /**
    * Creates a term consumer by parsing section defined by {@link this.CONSUMER_KEY}
    *
    * @param uimaContext           -
    * @param lookupConsumerElement contains the definition of the term consumer
    * @return a term consumer
    * @throws AnnotatorContextException if any of a dozen things goes wrong
    */
   private static TermConsumer parseConsumerXml( final UimaContext uimaContext,
                                                 final Element lookupConsumerElement ) throws
         AnnotatorContextException {
      Class[] constrArgsConsum = { UimaContext.class, Properties.class, int.class };//ohnlp-Bugs-3296301
      Class[] constrArgsConsumB = { UimaContext.class, Properties.class };

      String consumerClassName = lookupConsumerElement.getChildText( "implementationName" );
      Element consumerPropertiesElement = lookupConsumerElement.getChild( "properties" );
      Properties consumerProperties = parsePropertiesXml( consumerPropertiesElement );
      Class consumerClass;
      try {
         consumerClass = Class.forName( consumerClassName );
      } catch ( ClassNotFoundException cnfE ) {
         throw new AnnotatorContextException( "Unknown class " + consumerClassName, new Object[0], cnfE );
      }
      if ( !TermConsumer.class.isAssignableFrom( consumerClass ) ) {
         throw new AnnotatorContextException( consumerClassName + " is not a TermConsumer",
               new Object[0] );
      }
      final Constructor[] constructors = consumerClass.getConstructors();
      for ( Constructor constructor : constructors ) {
         try {
            if ( Arrays.equals( constrArgsConsum, constructor.getParameterTypes() ) ) {
               final Object[] args = new Object[]{ uimaContext, consumerProperties,
                                                   MAX_LIST_SIZE }; //ohnlp-Bugs-3296301
               return (TermConsumer)constructor.newInstance( args );
            } else if ( Arrays.equals( constrArgsConsumB, constructor.getParameterTypes() ) ) {
               final Object[] args = new Object[]{ uimaContext, consumerProperties };
               return (TermConsumer)constructor.newInstance( args );
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException multE ) {
            throw new AnnotatorContextException( "Could not construct " + consumerClassName, new Object[0], multE );
         }
      }
      throw new AnnotatorContextException( "No Constructor for " + consumerClassName, new Object[0] );
   }

   /**
    * Builds a collection of key, value properties
    *
    * @param propertiesElement element with key, value pairs
    * @return Properties
    */
   private static Properties parsePropertiesXml( final Element propertiesElement ) {
      final Properties properties = new Properties();
      final Collection propertyElements = propertiesElement.getChildren();
      for ( Object value : propertyElements ) {
         final Element propertyElement = (Element)value;
         final String key = propertyElement.getAttributeValue( "key" );
         final String propertyValue = propertyElement.getAttributeValue( "value" );
         properties.put( key, propertyValue );
      }
      return properties;
   }

}
