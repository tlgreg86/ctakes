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
package org.apache.ctakes.dictionary.lookup2.term;

import javax.annotation.concurrent.Immutable;

/**
 * Container class for terms in a {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary}
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/18/13
 */
@Immutable
final public class RareWordTerm {

   final private String _text;
   final private String _cui;
   final private String _tui;
   final private String _rareWord;
   final private int _rareWordIndex;
   final private int _tokenCount;
   final private int _hashCode;

   /**
    *
    * @param text full text of term
    * @param cui  umls cui for the term
    * @param tui  semantic type tui for the term
    * @param rareWord rare word in the term that is used for lookup
    * @param rareWordIndex index of the rare word within the term
    * @param tokenCount number of tokens within the term
    */
   public RareWordTerm( final String text, final String cui, final String tui,
                        final String rareWord, final int rareWordIndex,
                        final int tokenCount ) {
      _text = text;
      _cui = cui;
      _tui = tui;
      _rareWord = rareWord;
      _rareWordIndex = rareWordIndex;
      _tokenCount = tokenCount;
      _hashCode = (_cui+_tui+ _text).hashCode();
   }

   /**
    *
    * @return full text of term
    */
   public String getText() {
      return _text;
   }

   /**
    *
    * @return umls cui for the term
    */
   public String getCui() {
      return _cui;
   }

   /**
    *
    * @return semantic type tui for the term
    */
   public String getTui() {
      return _tui;
   }

   /**
    *
    * @return rare word in the term that is used for lookup
    */
   public String getRareWord() {
      return _rareWord;
   }

   /**
    *
    * @return index of the rare word within the term
    */
   public int getRareWordIndex() {
      return _rareWordIndex;
   }

   /**
    *
    * @return number of tokens within the term
    */
   public int getTokenCount() {
      return _tokenCount;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      if ( !( value instanceof RareWordTerm) ) {
         return false;
      }
      final RareWordTerm other = (RareWordTerm)value;
      return other.getCui().equals( _cui ) && other.getText().equals( _text ) && other.getTui().equals( _tui );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _hashCode;
   }

}
