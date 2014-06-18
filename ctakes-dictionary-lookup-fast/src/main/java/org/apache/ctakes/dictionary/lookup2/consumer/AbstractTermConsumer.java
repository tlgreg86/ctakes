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
package org.apache.ctakes.dictionary.lookup2.consumer;

import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.term.SpannedRareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.SemanticUtil;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 2/5/14
 */
abstract public class AbstractTermConsumer implements TermConsumer {

   static private final String CODING_SCHEME_PRP_KEY = "codingScheme";

   final private String _codingScheme;

   public AbstractTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      _codingScheme = properties.getProperty( CODING_SCHEME_PRP_KEY );
   }

   /**
    *
    * @param jcas -
    * @param codingScheme -
    * @param typeId  cTakes IdentifiedAnnotation only accepts an integer as a typeId
    * @param lookupHitMap map of spans to terms for those spans
    * @throws org.apache.uima.analysis_engine.AnalysisEngineProcessException
    */
   abstract protected void consumeTypeIdHits( final JCas jcas, final String codingScheme, final int typeId,
                                              final Map<TextSpan, Collection<RareWordTerm>> lookupHitMap )
         throws AnalysisEngineProcessException;


   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeHits( final JCas jcas, final RareWordDictionary dictionary,
                            final Collection<SpannedRareWordTerm> dictionaryTerms )
         throws AnalysisEngineProcessException {
      final String codingScheme = getCodingScheme();
      final String entityType = dictionary.getSemanticGroup();
      if ( entityType.equals( SemanticUtil.UNKNOWN_SEMANTIC_GROUP )
            || entityType.equals( SemanticUtil.UNKNOWN_SEMANTIC_ZERO ) ) {
         // The dictionary may have more than one type, create a map of types to terms and use them all
         final Map<Integer,Collection<SpannedRareWordTerm>> typeIdLookupHitMap
               = createTypeIdLookupHitMap( dictionaryTerms );
         for ( Map.Entry<Integer,Collection<SpannedRareWordTerm>> typeIdLookupHits : typeIdLookupHitMap.entrySet() ) {
            final Map<TextSpan, Collection<RareWordTerm>> lookupHitMap = createLookupHitMap( typeIdLookupHits.getValue() );
            consumeTypeIdHits( jcas, codingScheme, typeIdLookupHits.getKey(), lookupHitMap );
         }
         return;
      }
      // The dictionary has one type, consume all using that type id
      final int typeId = SemanticUtil.getSemanticGroupId( entityType );
      final Map<TextSpan, Collection<RareWordTerm>> lookupHitMap = createLookupHitMap( dictionaryTerms );
      consumeTypeIdHits( jcas, codingScheme, typeId, lookupHitMap );
   }


   protected String getCodingScheme() {
      return _codingScheme;
   }

   /**
    *
    * @param spannedRareWordTerms discovered terms
    * @return Map of terms for each span
    */
   static protected Map<TextSpan, Collection<RareWordTerm>> createLookupHitMap(
         final Collection<SpannedRareWordTerm> spannedRareWordTerms ) {
      final Map<TextSpan,Collection<RareWordTerm>> lookupHitMap = new HashMap<TextSpan, Collection<RareWordTerm>>();
      for ( SpannedRareWordTerm spannedRareWordTerm : spannedRareWordTerms ) {
         Collection<RareWordTerm> rareWordTerms = lookupHitMap.get( spannedRareWordTerm.getTextSpan() );
         if ( rareWordTerms == null ) {
            rareWordTerms = new HashSet<RareWordTerm>();
            lookupHitMap.put( spannedRareWordTerm.getTextSpan(), rareWordTerms );
         }
         rareWordTerms.add( spannedRareWordTerm.getRareWordTerm() );
      }
      return lookupHitMap;
   }

   /**
    *
    * @param spannedRareWordTerms discovered terms
    * @return Map of type Ids and the discovered terms for each
    */
   static protected Map<Integer,Collection<SpannedRareWordTerm>> createTypeIdLookupHitMap(
         final Collection<SpannedRareWordTerm> spannedRareWordTerms ) {
      final Map<Integer,Collection<SpannedRareWordTerm>> typeIdLookupHitMap
            = new HashMap<Integer, Collection<SpannedRareWordTerm>>( 6 );
      for ( SpannedRareWordTerm spannedTerm : spannedRareWordTerms ) {
         // Attempt to obtain one or more valid type ids from the tuis of the term
         final Collection<Integer> typeIds = SemanticUtil.getSemanticGroupIdFromTui( spannedTerm.getRareWordTerm().getTui() );
         for ( Integer typeId : typeIds ) {
            Collection<SpannedRareWordTerm> typeIdHits = typeIdLookupHitMap.get( typeId );
            if ( typeIdHits == null ) {
               typeIdHits = new ArrayList<SpannedRareWordTerm>();
               typeIdLookupHitMap.put( typeId, typeIdHits );
            }
            typeIdHits.add( spannedTerm );
         }
      }
      return typeIdLookupHitMap;
   }


}
