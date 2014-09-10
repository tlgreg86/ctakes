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
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

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
      CUI( 1 ), RINDEX( 2 ), TCOUNT( 3 ), TEXT( 4 ), RWORD( 5 );
      final private int __index;

      private FIELD_INDEX( final int index ) {
         __index = index;
      }
   }

   // LOG4J logger based on class name
   final private Logger _logger = Logger.getLogger( getClass().getName() );


   // TODO move to Constants class
   static private final String JDBC_DRIVER = "jdbcDriver";
   static private final String JDBC_URL = "jdbcUrl";
   static private final String JDBC_USER = "jdbcUser";
   static private final String JDBC_PASS = "jdbcPass";
   static private final String RARE_WORD_TABLE = "rareWordTable";


   final private Connection _connection;
   private PreparedStatement _selectTermCall;


   public JdbcRareWordDictionary( final String name, final UimaContext uimaContext, final Properties properties )
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      this( name,
            properties.getProperty( JDBC_DRIVER ), properties.getProperty( JDBC_URL ),
            properties.getProperty( JDBC_USER ), properties.getProperty( JDBC_PASS ),
            properties.getProperty( RARE_WORD_TABLE ) );
   }


   public JdbcRareWordDictionary( final String name,
                                  final String jdbcDriver,
                                  final String jdbcUrl,
                                  final String jdbcUser,
                                  final String jdbcPass,
                                  final String tableName )
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      super( name );
      try {
         final Driver driver = (Driver)Class.forName( jdbcDriver ).newInstance();
         DriverManager.registerDriver( driver );
      } catch ( SQLException sqlE ) {
         _logger.error( "Could not register Driver " + jdbcDriver, sqlE );
         throw new InstantiationException( "Could not register Driver " + jdbcDriver );
      } catch ( ClassNotFoundException | InstantiationException | IllegalAccessException multE ) {
         _logger.error( "Could not create Driver " + jdbcDriver, multE );
         throw multE;
      }
      Connection connection = null;
      try {
         connection = DriverManager.getConnection( jdbcUrl, jdbcUser, jdbcPass );
      } catch ( SQLException sqlE ) {
         _logger.error( "Could not create Connection with " + jdbcUrl + " as " + jdbcUser, sqlE );
         throw new InstantiationException( "Could not create Connection with " + jdbcUrl + " as " + jdbcUser );
      }
      _connection = connection;
      try {
         _selectTermCall = createSelectCall( tableName );
      } catch ( SQLException sqlE ) {
         _logger.error( "Could not create Term Data Selection Call", sqlE );
      }
   }


   /**
    * @param connection database connection
    * @param tableName  name of the database table to use for lookup.  Used as the simple name for the dictionary
    */
   public JdbcRareWordDictionary( final String name,
                                  final Connection connection,
                                  final String tableName ) {
      super( name );
      _connection = connection;
      try {
         _selectTermCall = createSelectCall( tableName );
      } catch ( SQLException sqlE ) {
         _logger.error( "Could not create Term Data Selection Call", sqlE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
      final List<RareWordTerm> rareWordTerms = new ArrayList<RareWordTerm>();
      try {
         fillSelectCall( rareWordText );
         final ResultSet resultSet = _selectTermCall.executeQuery();
         while ( resultSet.next() ) {
            final RareWordTerm rareWordTerm = new RareWordTerm( resultSet.getString( FIELD_INDEX.TEXT.__index ),
                  resultSet.getLong( FIELD_INDEX.CUI.__index ),
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
    * @return an sql call to use for term lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   private PreparedStatement createSelectCall( final String tableName ) throws SQLException {
      final String lookupSql = "SELECT * FROM " + tableName + " WHERE RWORD = ?";
      return _connection.prepareStatement( lookupSql );
   }

   /**
    * @param rareWordText text of the rare word to use for term lookup
    * @return an sql call to use for term lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   private PreparedStatement fillSelectCall( final String rareWordText ) throws SQLException {
      _selectTermCall.clearParameters();
      _selectTermCall.setString( 1, rareWordText );
      return _selectTermCall;
   }


}
