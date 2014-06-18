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

import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Preferred dictionary to use for large collections of terms.
 * Column indices within the database are constant and not configurable: CUI TUI RINDEX TCOUNT TEXT RWORD
 * If a configurable implementation is desired then create an extension.
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 3/26/13
 */
final public class JdbcRareWordDictionary extends AbstractRareWordDictionary {

   /**
    * Column (field) indices in the database.  Notice that these are constant and not configurable.
    * If a configurable implementation is desired then create an extension.
    */
   static private enum FIELD_INDEX {
      CUI( 1 ), TUI( 2 ), RINDEX( 3 ), TCOUNT( 4 ), TEXT( 5 ), RWORD( 6 );
      final private int __index;
      private FIELD_INDEX( final int index ) {
         __index = index;
      }
   }

   // LOG4J logger based on class name
   final private Logger _logger = Logger.getLogger( getClass().getName() );

   final private Connection _connection;
   final private String _tableName;
   private PreparedStatement _metadataStatement;

   /**
    *
    * @param semanticGroup the type of term that exists in the dictionary: Anatomical Site, Disease/Disorder, Drug, etc.
    * @param connection database connection
    * @param tableName name of the database table to use for lookup.  Used as the simple name for the dictionary
    */
   public JdbcRareWordDictionary( final String semanticGroup,
                                  final Connection connection,
                                  final String tableName ) {
      super( tableName, semanticGroup );
      _connection = connection;
      _tableName = tableName;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
      final List<RareWordTerm> rareWordTerms = new ArrayList<RareWordTerm>();
      try {
         initMetaDataStatement( rareWordText );
         final ResultSet resultSet = _metadataStatement.executeQuery();
         while ( resultSet.next() ) {
            final RareWordTerm rareWordTerm = new RareWordTerm( resultSet.getString( FIELD_INDEX.TEXT.__index),
                                                                resultSet.getString( FIELD_INDEX.CUI.__index ),
                                                                resultSet.getString( FIELD_INDEX.TUI.__index ),
                                                                resultSet.getString( FIELD_INDEX.RWORD.__index ),
                                                                resultSet.getInt( FIELD_INDEX.RINDEX.__index ),
                                                                resultSet.getInt( FIELD_INDEX.TCOUNT.__index ) );
            rareWordTerms.add( rareWordTerm );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         _logger.error( e.getMessage() );
      }
      return rareWordTerms;
   }

   /**
    *
    * @param rareWordText text of the rare word to use for term lookup
    * @return an sql call to use for term lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   private PreparedStatement initMetaDataStatement( final String rareWordText ) throws SQLException {
      if ( _metadataStatement == null ) {
         final String lookupSql = "SELECT * FROM " + _tableName + " WHERE RWORD = ?";
         _metadataStatement = _connection.prepareStatement( lookupSql );
      }
      _metadataStatement.clearParameters();
      _metadataStatement.setString( 1, rareWordText );
      return _metadataStatement;
   }

}
