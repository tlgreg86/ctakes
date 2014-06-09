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
package org.apache.ctakes.dictionary.lookup2.ae;

import org.apache.ctakes.core.fsm.token.NumberToken;
import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.core.util.JCasUtil;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.dictionary.DictionaryDescriptorParser;
import org.apache.ctakes.dictionary.lookup2.term.SpannedRareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.DictionarySpec;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ContractionToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.syntax.SymbolToken;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs the basic initialization with uima context, including the parse of the dictionary specifications file.
 * Has a
 *
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 12/6/13
 */
abstract public class AbstractJCasTermAnnotator extends JCasAnnotator_ImplBase
      implements JCasTermAnnotator, WindowProcessor {

   // LOG4J logger based on interface name
   final private Logger _logger = Logger.getLogger( "AbstractJCasTermAnnotator" );

   /** specifies the type of window to use for lookup */
   static private final String WINDOW_ANNOT_PRP_KEY = "windowAnnotations";
   /** optional part of speech tags for tokens that should not be used for lookup */
   static private final String EXC_TAGS_PRP_KEY = "exclusionTags";
   /** optional minimum span for tokens that should not be used for lookup */
   static private final String MIN_SPAN_PRP_KEY = "minimumSpan";

   private DictionarySpec _dictionarySpec;

   // type of lookup window to use, typically "LookupWindowAnnotation" or "Sentence"
   private int _lookupWindowType;
   // set of exclusion POS tags (lower cased), may be null
   private final Set<String> _exclusionPartsOfSpeech = new HashSet<String>();
   // minimum span required to use token for lookup
   private int _minimumLookupSpan = 3;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      try {
         final String windowClassName = (String)uimaContext.getConfigParameterValue( WINDOW_ANNOT_PRP_KEY );
         _logger.info( "Using dictionary lookup window type: " + windowClassName );
         _lookupWindowType = JCasUtil.getType( windowClassName );

         // optional exclusion POS tags
         final String exclusionTags = (String)uimaContext.getConfigParameterValue( EXC_TAGS_PRP_KEY );
         if ( exclusionTags != null ) {
            final String[] tagArr = exclusionTags.split( "," );
            for ( String tag : tagArr ) {
               _exclusionPartsOfSpeech.add( tag.toUpperCase() );
            }
            final List<String> posList = new ArrayList<String>( _exclusionPartsOfSpeech );
            Collections.sort( posList );
            final StringBuilder sb = new StringBuilder();
            for ( String pos : posList ) {
               sb.append( pos ).append( " " );
            }
            _logger.info( "Exclusion tagset loaded: " + sb.toString() );
         }

         // optional minimum span, default is 3
         final Object minimumSpan = uimaContext.getConfigParameterValue( MIN_SPAN_PRP_KEY );
         if ( minimumSpan != null ) {
            _minimumLookupSpan = parseInt( minimumSpan, MIN_SPAN_PRP_KEY, _minimumLookupSpan );
         }
         _logger.info( "Using minimum lookup token span: " + _minimumLookupSpan );
         final FileResource fileResource = (FileResource) uimaContext.getResourceObject( DICTIONARY_DESCRIPTOR_KEY );
         final File descriptorFile = fileResource.getFile();
         _dictionarySpec = DictionaryDescriptorParser.parseDescriptor( descriptorFile, uimaContext );
      } catch ( ResourceAccessException raE ) {
         throw new ResourceInitializationException( raE );
      } catch ( AnnotatorContextException acE ) {
         throw new ResourceInitializationException( acE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      _logger.info( "Starting processing" );
      final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
      final AnnotationIndex annotationIndex = indexes.getAnnotationIndex( _lookupWindowType );
      if ( annotationIndex == null ) {  // I don't trust AnnotationIndex.size(), so don't check
         return;
      }
      final Map<RareWordDictionary,Collection<SpannedRareWordTerm>> dictionaryTermsMap
            = new HashMap<RareWordDictionary, Collection<SpannedRareWordTerm>>();
      final Iterator windowIterator = annotationIndex.iterator();
      try {
         while ( windowIterator.hasNext() ) {
            final Annotation window = (Annotation) windowIterator.next();
            if ( isWindowOk( window ) ) {
               processWindow( jcas, window, dictionaryTermsMap );
            }
         }
      } catch ( ArrayIndexOutOfBoundsException iobE ) {
         // JCasHashMap will throw this every once in a while.  Assume the windows are done and move on
         _logger.warn( iobE.getMessage() );
      }
      // Let the consumer handle uniqueness and ordering - some may not care
      for ( Map.Entry<RareWordDictionary, Collection<SpannedRareWordTerm>> entry : dictionaryTermsMap.entrySet() ) {
         _dictionarySpec.getConsumer().consumeHits( jcas, entry.getKey(), entry.getValue() );
      }
      _logger.info( "Finished processing" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordDictionary> getDictionaries() {
      return _dictionarySpec.getDictionaries();
   }

   /**
    * Skip windows that are section headers/footers.  Kludge, but worth doing
    * {@inheritDoc}
    */
   @Override
   public boolean isWindowOk( final Annotation window ) {
      final String coveredText = window.getCoveredText();
      return !coveredText.equals( "section id" )
            && !coveredText.startsWith( "[start section id" )
            && !coveredText.startsWith( "[end section id" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void processWindow( final JCas jcas, final Annotation window,
                              final Map<RareWordDictionary, Collection<SpannedRareWordTerm>> dictionaryTermsMap ) {
      final List<FastLookupToken> allTokens = new ArrayList<FastLookupToken>();
      final List<Integer> lookupTokenIndices = new ArrayList<Integer>();
      getAnnotationsInWindow( jcas, window, allTokens, lookupTokenIndices );
      findTerms( getDictionaries(), allTokens, lookupTokenIndices, dictionaryTermsMap );
   }

   /**
    * Given a set of dictionaries, tokens, and lookup token indices, populate a terms map with discovered terms
    * @param dictionaries -
    * @param allTokens    -
    * @param lookupTokenIndices -
    * @param dictionaryTermsMap -
    */
   private void findTerms( final Collection<RareWordDictionary> dictionaries,
                           final List<FastLookupToken> allTokens, final List<Integer> lookupTokenIndices,
                           final Map<RareWordDictionary, Collection<SpannedRareWordTerm>> dictionaryTermsMap ) {
      Collection<SpannedRareWordTerm> termsFromDictionary;
      for ( RareWordDictionary dictionary : dictionaries ) {
         termsFromDictionary = dictionaryTermsMap.get( dictionary );
         if ( termsFromDictionary == null ) {
            termsFromDictionary = new ArrayList<SpannedRareWordTerm>();
            dictionaryTermsMap.put( dictionary, termsFromDictionary );
         }
         findTerms( dictionary, allTokens, lookupTokenIndices, termsFromDictionary );
      }
   }

   /**
    * Given a dictionary, tokens, and lookup token indices, populate a terms collection with discovered terms
    * @param dictionary -
    * @param allTokens  -
    * @param lookupTokenIndices  -
    * @param termsFromDictionary -
    */
   abstract void findTerms( RareWordDictionary dictionary,
                            List<FastLookupToken> allTokens, List<Integer> lookupTokenIndices,
                            Collection<SpannedRareWordTerm> termsFromDictionary );


   /**
    * For the given lookup window fills two collections with 1) All tokens in the window,
    * and 2) indexes of tokens in the window to be used for lookup
    * @param jcas -
    * @param window annotation lookup window
    * @param allTokens filled with all tokens, including punctuation, etc.
    * @param lookupTokenIndices filled with indices of tokens to use for lookup
    */
   protected void getAnnotationsInWindow( final JCas jcas, final Annotation window,
                                        final List<FastLookupToken> allTokens,
                                        final List<Integer> lookupTokenIndices ) {
      final List<BaseToken> allBaseTokens = org.uimafit.util.JCasUtil.selectCovered( jcas, BaseToken.class, window );
      for ( BaseToken baseToken : allBaseTokens ) {
         if ( baseToken instanceof NewlineToken ) {
            continue;
         }
         final boolean isNonLookup = baseToken instanceof PunctuationToken
               || baseToken instanceof NumberToken
               || baseToken instanceof ContractionToken
               || baseToken instanceof SymbolToken;
         // We are only interested in tokens that are -words-
         // getEnd() and getBegin() are both inclusive, so end - begin is actually text.length()-1
         if ( !isNonLookup && baseToken.getEnd() - baseToken.getBegin() + 1 >= _minimumLookupSpan ) {
           // POS exclusion logic for first word lookup
            final String partOfSpeech = baseToken.getPartOfSpeech();
            if ( partOfSpeech == null || !_exclusionPartsOfSpeech.contains( partOfSpeech ) ) {
               lookupTokenIndices.add( allTokens.size() );
            }
         }
         final FastLookupToken lookupToken = new FastLookupToken( baseToken );
         allTokens.add( lookupToken );
      }
   }



   protected int parseInt( final Object value, final String name, final int defaultValue ) {
      if ( value instanceof Integer ) {
         return (Integer)value;
      } else if ( value instanceof String ) {
         try {
            return Integer.parseInt( (String)value );
         } catch ( NumberFormatException nfE ) {
            _logger.warn( "Could not parse " + name + " " + value + " as an integer" );
         }
      } else {
         _logger.warn( "Could not parse " + name + " " + value + " as an integer" );
      }
      return defaultValue;
   }


}
