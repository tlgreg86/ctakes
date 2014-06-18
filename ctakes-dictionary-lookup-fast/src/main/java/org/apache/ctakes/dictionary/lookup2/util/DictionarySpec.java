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
package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;

/**
 * Simple Container class that holds a {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary}
 * collection and a {@link org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer}
 */
@Immutable
final public class DictionarySpec {
   final private Collection<RareWordDictionary> _dictionaries;
   final private TermConsumer _termConsumer;
   public DictionarySpec( final Collection<RareWordDictionary> dictionaries,
                          final TermConsumer termConsumer ) {
      _dictionaries = dictionaries;
      _termConsumer = termConsumer;
   }

   /**
    * @return all dictionaries to use for term lookup
    */
   public Collection<RareWordDictionary> getDictionaries() {
      return _dictionaries;
   }

   /**
    * @return the consumer to add terms to the Cas
    */
   public TermConsumer getConsumer() {
      return _termConsumer;
   }
}
