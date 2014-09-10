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

import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.core.resource.JdbcConnectionResource;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.jdom.Element;

import java.io.File;
import java.sql.Connection;

/**
 * TODO
 * This factory can create a RareWordDictionary by wrapping the older Jdbc, Lucene, StringTable (CSV) descriptors.
 * However, to prevent the dependency upon the current Dictionary-Lookup module and its "Dictionary" interface,
 * all methods have been commented out.  Uncommenting, linking, and rebuilding is possible if use of an older dictionary
 * resource is required.
 * TODO
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 2/20/14
 */
final public class DictionaryFactory {

   private DictionaryFactory() {
   }


   /**
    * A JDBC accessible RareWordDictionary is the preferred fast lookup dictionary
    *
    * @param implementationElement contains properties for the implementation
    * @param externalResource      jdbc accessible db
    * @param entityTypeId          type of entity that the dictionary contains, specified by {@link this.TYPE_ID}
    * @return a dictionary that uses the specified db for rare word lookup
    * @throws org.apache.uima.analysis_engine.annotator.AnnotatorContextException if the {@code externalResource} is not a db
    */
   static public RareWordDictionary createRareWordJdbc( final Element implementationElement,
                                                        final Object externalResource, final String entityTypeId )
         throws AnnotatorContextException {
      checkResourceType( JdbcConnectionResource.class, externalResource );
      final String tableName = implementationElement.getAttributeValue( "tableName" );
      final Connection connection = ((JdbcConnectionResource)externalResource).getConnection();
      return new JdbcRareWordDictionary( entityTypeId, connection, tableName );
   }

   /**
    * A RareWordDictionary for a simple user created bar-separated value (bsv) file
    *
    * @param externalResourceKey contains the name of the implementation
    * @param externalResource    bar-separated value (bsv) file
    * @param entityTypeId        type of entity that the dictionary contains, specified by {@link this.TYPE_ID}
    * @return a dictionary that uses the specified file for rare word lookup
    * @throws AnnotatorContextException if the {@code externalResource} is not a file
    */
   static public RareWordDictionary createRareWordBsv( final String externalResourceKey, final Object externalResource,
                                                       final String entityTypeId )
         throws AnnotatorContextException {
      checkResourceType( FileResource.class, externalResource );
      final File bsvFile = ((FileResource)externalResource).getFile();
      return new BsvRareWordDictionary( externalResourceKey, bsvFile );
   }

//   /**
//    * A RareWordDictionary for an older "first word lookup" lucene table/index.
//    * The old uber-configurable dictionary paradigm should be abandoned in favor of something stricter yet simpler
//    *
//    * @param rootElement         contains information about fields in the lucene table
//    * @param externalResourceKey contains the name of the implementation
//    * @param externalResource    lucene table
//    * @param entityTypeId        type of entity that the dictionary contains, specified by {@link this.TYPE_ID}
//    * @return a dictionary that uses the specified lucene table for rare word lookup
//    * @throws AnnotatorContextException if the {@code externalResource} is not lucene
//    * @deprecated Fixed index/naming schemes in the data are so much easier than flex in the data and fixed in the desc
//    */
//   @Deprecated
//   static public RareWordDictionary createWrappedLucene( final Element rootElement, final String externalResourceKey,
//                                                         final Object externalResource, final String entityTypeId )
//         throws AnnotatorContextException {
//      checkResourceType( LuceneIndexReaderResource.class, externalResource );
//      final IndexReader indexReader = ((LuceneIndexReaderResource) externalResource).getIndexReader();
//      final IndexSearcher indexSearcher = new IndexSearcher( indexReader );
//      // Added 'MaxListSize' ohnlp-Bugs-3296301
//      final Element lookupFieldElement = rootElement.getChild( "lookupField" );
//      final String lookupFieldName = lookupFieldElement.getAttributeValue( "fieldName" );
//      return createWrappedDictionary( externalResourceKey, entityTypeId,
//                                      new LuceneDictionaryImpl( indexSearcher, lookupFieldName, MAX_LIST_SIZE ) );
//   }
//
//   /**
//    * A RareWordDictionary for an older "first word lookup" jdbc accessible db.
//    * The old uber-configurable dictionary paradigm should be abandoned in favor of something stricter yet simpler
//    *
//    * @param rootElement           contains information about fields in the jdbc accessible db
//    * @param implementationElement contains properties for the implementation
//    * @param externalResourceKey   contains the name of the implementation
//    * @param externalResource      jdbc accessible db
//    * @param entityTypeId          type of entity that the dictionary contains, specified by {@link this.TYPE_ID}
//    * @return a dictionary that uses the specified db for rare word lookup
//    * @throws AnnotatorContextException if the {@code externalResource} is not a db
//    * @deprecated Fixed index/naming schemes in the data are so much easier than flex in the data and fixed in the desc
//    */
//   @Deprecated
//   static public RareWordDictionary createWrappedJdbc( final Element rootElement, final Element implementationElement,
//                                                       final String externalResourceKey,
//                                                       final Object externalResource, final String entityTypeId )
//         throws AnnotatorContextException {
//      checkResourceType( JdbcConnectionResource.class, externalResource );
//      final String tableName = implementationElement.getAttributeValue( "tableName" );
//      final Element lookupFieldElement = rootElement.getChild( "lookupField" );
//      final String lookupFieldName = lookupFieldElement.getAttributeValue( "fieldName" );
//      final Connection connection = ((JdbcConnectionResource) externalResource).getConnection();
//      return createWrappedDictionary( externalResourceKey, entityTypeId,
//                                      new MemReleaseJdbcDictionaryImpl( connection, tableName, lookupFieldName ) );
//   }
//
//   /**
//    * A RareWordDictionary for a simple user created comma-separated value (csv) file.
//    * The old uber-configurable dictionary paradigm should be abandoned in favor of something stricter yet simpler
//    *
//    * @param rootElement           contains information about fields in the csv file
//    * @param implementationElement contains properties for the implementation
//    * @param externalResourceKey   contains the name of the implementation
//    * @param externalResource      comma-separated value (csv) file
//    * @param entityTypeId          type of entity that the dictionary contains, specified by {@link this.TYPE_ID}
//    * @return a dictionary that uses the specified file for rare word lookup
//    * @throws AnnotatorContextException if the {@code externalResource} is not a file
//    * @deprecated Fixed index/naming schemes in the data are so much easier than flex in the data and fixed in the desc
//    */
//   @Deprecated
//   static public RareWordDictionary createWrappedCsv( final Element rootElement, final Element implementationElement,
//                                                      final String externalResourceKey,
//                                                      final Object externalResource, final String entityTypeId )
//         throws AnnotatorContextException {
//      checkResourceType( FileResource.class, externalResource );
//      final String fieldDelimiter = implementationElement.getAttributeValue( "delimiter" );
//      final String indexFieldNames = implementationElement.getAttributeValue( "indexedFieldNames" );
//      final String[] fieldNames = indexFieldNames.split( "," );
//      for ( int i = 0; i < fieldNames.length; i++ ) {
//         fieldNames[i] = fieldNames[i].trim();
//      }
//      final File csvFile = ((FileResource) externalResource).getFile();
//      try {
//         final StringTable stringTable = StringTableFactory.build( new FileReader( csvFile ),
//                                                                   fieldDelimiter, fieldNames, true );
//         final Element lookupFieldElement = rootElement.getChild( "lookupField" );
//         final String lookupFieldName = lookupFieldElement.getAttributeValue( "fieldName" );
//         return createWrappedDictionary( externalResourceKey, entityTypeId,
//                                         new StringTableDictionaryImpl( stringTable, lookupFieldName ) );
//      } catch ( IOException ioE ) {
//         throw new AnnotatorContextException( "Could not build StringTable from " + csvFile.getPath(),
//                                              new Object[0], ioE );
//      }
//   }
//
//   /**
//    * Wraps an {@link Dictionary} using {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionaryWrapper}
//    *
//    * @param externalResourceKey contains the name of the implementation
//    * @param entityTypeId        type of entity that the dictionary contains, specified by {@link this.TYPE_ID}
//    * @param dictionary          implementation of older Dictionary interface
//    * @return a class that implements {@link RareWordDictionary} but <i>does not</i> necessarily
//    *         perform lookup by rare word
//    * @throws AnnotatorContextException if {@code dictionary} is null
//    */
//   static public RareWordDictionary createWrappedDictionary( final String externalResourceKey,
//                                                             final String entityTypeId, final Dictionary dictionary )
//         throws AnnotatorContextException {
//      if ( dictionary != null ) {
//         return new RareWordDictionaryWrapper( externalResourceKey, entityTypeId, dictionary );
//      }
//      throw new AnnotatorContextException( "Could not wrap a null Dictionary for " + externalResourceKey,
//                                           new Object[0] );
//   }


   /**
    * Convenience method that throws an {@link AnnotatorContextException} when an external resource is
    * not of the correct type
    *
    * @param expectedClassType expected resource class
    * @param typeValue         Object that should be an implementation of the {@code expectedClassType}
    * @throws AnnotatorContextException if {@code typeValue} is an incorrect class type
    */
   static private void checkResourceType( final Class expectedClassType, final Object typeValue )
         throws AnnotatorContextException {
      if ( expectedClassType.isInstance( typeValue ) ) {
         return;
      }
      throw new AnnotatorContextException( "Expected external resource to be " + expectedClassType.getName()
                                           + " not " + typeValue.getClass().getName(), new Object[0] );
   }

}
