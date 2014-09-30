package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Some JDBC Connections can be reused, for instance by a Dictionary and Concept Factory.
 * This Singleton keeps a map of JDBC URLs to open and reusable Connections
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/29/2014
 */
public enum JdbcConnectionFactory {
   INSTANCE;

   static final private Logger LOGGER = Logger.getLogger( "JdbcConnectionFactory" );

   private final Map<String, Connection> CONNECTIONS = Collections.synchronizedMap( new HashMap<String, Connection>() );

   public static JdbcConnectionFactory getInstance() {
      return INSTANCE;
   }

   /**
    * Get an existing Connection or create and store a new one
    *
    * @param jdbcDriver -
    * @param jdbcUrl    -
    * @param jdbcUser   -
    * @param jdbcPass   -
    * @return a previously opened or new Connection
    * @throws SQLException if a JDBC Driver could not be created or registered,
    *                      or if a Connection could not be made to the given <code>jdbcUrl</code>
    */
   public Connection getConnection( final String jdbcDriver,
                                    final String jdbcUrl,
                                    final String jdbcUser,
                                    final String jdbcPass ) throws SQLException {
      Connection connection = CONNECTIONS.get( jdbcUrl );
      if ( connection != null ) {
         return connection;
      }
      try {
         // DO NOT use try with resources here.
         // Try with resources uses a closable and closes it when exiting the try block
         final Driver driver = (Driver)Class.forName( jdbcDriver ).newInstance();
         DriverManager.registerDriver( driver );
      } catch ( SQLException sqlE ) {
         LOGGER.error( "Could not register Driver " + jdbcDriver, sqlE );
         throw sqlE;
      } catch ( ClassNotFoundException | InstantiationException | IllegalAccessException multE ) {
         LOGGER.error( "Could not create Driver " + jdbcDriver, multE );
         throw new SQLException( multE );
      }
      try {
         // DO NOT use try with resources here.
         // Try with resources uses a closable and closes it when exiting the try block
         // We need the Connection later, and if it is closed then it is useless
         connection = DriverManager.getConnection( jdbcUrl, jdbcUser, jdbcPass );
      } catch ( SQLException sqlE ) {
         LOGGER.error( "Could not create Connection with " + jdbcUrl + " as " + jdbcUser, sqlE );
         throw sqlE;
      }
      CONNECTIONS.put( jdbcUrl, connection );
      return connection;
   }

}
