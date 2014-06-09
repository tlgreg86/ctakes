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

import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.LabMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/9/14
 */
final public class DefaultTermConsumer extends AbstractTermConsumer {


   public DefaultTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      super( uimaContext, properties );
   }

   /**
    *
    * @param jcas -
    * @param codingScheme -
    * @param typeId  cTakes IdentifiedAnnotation only accepts an integer as a typeId
    * @param lookupHitMap map of spans to terms for those spans
    * @throws org.apache.uima.analysis_engine.AnalysisEngineProcessException
    */
   protected void consumeTypeIdHits( final JCas jcas, final String codingScheme, final int typeId,
                                     final Map<TextSpan, Collection<RareWordTerm>> lookupHitMap )
         throws AnalysisEngineProcessException {
      // Set of Cuis to avoid duplicates at this offset
      final Set<String> cuiSet = new HashSet<String>();
      // Collection of UmlsConcept objects
      final Collection<UmlsConcept> conceptList = new ArrayList<UmlsConcept>();
      try {
         for ( Map.Entry<TextSpan, Collection<RareWordTerm>> entry : lookupHitMap.entrySet() ) {
            cuiSet.clear();
            conceptList.clear();
            for ( RareWordTerm lookupHit : entry.getValue() ) {
               final String cui = lookupHit.getCui() ;
               if ( cuiSet.add( cui ) ) {
                  final UmlsConcept concept = new UmlsConcept( jcas );
                  concept.setCodingScheme( codingScheme );
                  concept.setCui( cui );
                  concept.setTui( lookupHit.getTui() );
                  conceptList.add( concept );
               }
            }
            // Skip updating CAS if all Concepts for this type were filtered out for this span.
            if ( conceptList.isEmpty() ) {
               continue;
            }
            // code is only valid if the covered text is also present in the filter
            final int neBegin = entry.getKey().getStart();
            final int neEnd = entry.getKey().getEnd();
            final FSArray conceptArr = new FSArray( jcas, conceptList.size() );
            int arrIdx = 0;
            for ( UmlsConcept umlsConcept : conceptList ) {
               conceptArr.set( arrIdx, umlsConcept );
               arrIdx++;
            }
            IdentifiedAnnotation annotation;
            if ( typeId == CONST.NE_TYPE_ID_DRUG ) {
               annotation = new MedicationMention( jcas );
            } else if ( typeId == CONST.NE_TYPE_ID_ANATOMICAL_SITE ) {
               annotation = new AnatomicalSiteMention( jcas );
            } else if ( typeId == CONST.NE_TYPE_ID_DISORDER ) {
               annotation = new DiseaseDisorderMention( jcas );
            } else if ( typeId == CONST.NE_TYPE_ID_FINDING ) {
               annotation = new SignSymptomMention( jcas );
            } else if ( typeId == CONST.NE_TYPE_ID_LAB ) {
               annotation = new LabMention( jcas );
            } else if ( typeId == CONST.NE_TYPE_ID_PROCEDURE ) {
               annotation = new ProcedureMention( jcas );
            } else {
               annotation = new EntityMention( jcas );
            }
            annotation.setTypeID( typeId );
            annotation.setBegin( neBegin );
            annotation.setEnd( neEnd );
            annotation.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );
            annotation.setOntologyConceptArr( conceptArr );
            annotation.addToIndexes();
         }
      } catch ( Exception e ) {
         // TODO Poor form - refactor
         // What is really thrown?  The jcas "throwFeatMissing" is not a great help
         throw new AnalysisEngineProcessException( e );
      }
   }


}
