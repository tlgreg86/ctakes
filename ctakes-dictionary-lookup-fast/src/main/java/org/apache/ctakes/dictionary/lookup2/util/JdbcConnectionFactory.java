package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

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

   static private final String CTAKES_HOME = "CTAKES_HOME";

   static final private Logger LOGGER = Logger.getLogger( "JdbcConnectionFactory" );
   static final private Logger DOT_LOGGER = Logger.getLogger( "ProgressAppender" );
   static final private Logger EOL_LOGGER = Logger.getLogger( "ProgressDone" );

   static private final String HSQL_FILE_PREFIX = "jdbc:hsqldb:file:";
   static private final String HSQL_DB_EXT = ".script";
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
      String trueJdbcUrl = jdbcUrl;
      if ( jdbcUrl.startsWith( HSQL_FILE_PREFIX ) ) {
         // Hack for hsqldb file needing to be absolute or relative to current working directory
         trueJdbcUrl = HSQL_FILE_PREFIX + getConnectionUrl( jdbcUrl );
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
      LOGGER.info( "Connecting to " + jdbcUrl + ":" );
      final Timer timer = new Timer();
      timer.scheduleAtFixedRate( new DotPlotter(), 333, 333 );
      try {
         // DO NOT use try with resources here.
         // Try with resources uses a closable and closes it when exiting the try block
         // We need the Connection later, and if it is closed then it is useless
         connection = DriverManager.getConnection( trueJdbcUrl, jdbcUser, jdbcPass );
      } catch ( SQLException sqlE ) {
         timer.cancel();
         EOL_LOGGER.error( "" );
         LOGGER.error( "  Could not create Connection with " + trueJdbcUrl + " as " + jdbcUser, sqlE );
         throw sqlE;
      }
      timer.cancel();
      EOL_LOGGER.info( "" );
      LOGGER.info( " Database connected" );
      CONNECTIONS.put( jdbcUrl, connection );
      return connection;
   }

   static private String getConnectionUrl( final String jdbcUrl ) throws SQLException {
      final String urlDbPath = jdbcUrl.substring( HSQL_FILE_PREFIX.length() );
      final String urlFilePath = urlDbPath + HSQL_DB_EXT;
      File file = new File( urlFilePath );
      LOGGER.debug( "absolute url: " + file.getPath() + " , use " + urlDbPath );
      if ( file.exists() ) {
         return urlDbPath;
      }
      // file url is not absolute, check for relative directly under current working directory
      final String cwd = System.getProperty( "user.dir" );
      file = new File( cwd, urlFilePath );
      LOGGER.debug( "cwd relative url: " + file.getPath() + " , use " + urlDbPath );
      if ( file.exists() ) {
         return urlDbPath;
      }
      // Users running projects out of an ide may have the module directory as cwd
      String upOne = "../";
      File cwdDerived = new File( cwd );
      while ( cwdDerived.getParentFile() != null ) {
         cwdDerived = cwdDerived.getParentFile();
         file = new File( cwdDerived, urlFilePath );
         LOGGER.debug( "cwd parent relative url: " + file.getPath() + " , use " + upOne + urlDbPath );
         if ( file.exists() ) {
            return upOne+urlDbPath;
         }
         file = new File( cwdDerived, "ctakes/" + urlFilePath );
         LOGGER.debug(
               "cwd parent relative ctakes url: " + file.getPath() + " , use " + upOne + "ctakes/" + urlDbPath );
         if ( file.exists() ) {
            return upOne + "ctakes/" + urlDbPath;
         }
         upOne += "../";
      }
      final String cTakesHome = System.getenv( CTAKES_HOME );
      if ( cTakesHome != null && !cTakesHome.isEmpty() ) {
         file = new File( cTakesHome, urlFilePath );
         LOGGER.debug( "$CTAKES_HOME absolute url: " + file.getPath() + " , use " + cTakesHome + "/" + urlDbPath );
         if ( file.exists() ) {
            return cTakesHome + "/" + urlDbPath;
         }
      }
      LOGGER.error( "Could not find " + urlFilePath + " as absolute or in \n" + cwd
              + " or in any parent thereof or in $CTAKES_HOME \n" + cTakesHome );
      throw new SQLException( "No Hsql DB exists at Url" );
   }

   static private class DotPlotter extends TimerTask {
      private int _count = 0;

      @Override
      public void run() {
         DOT_LOGGER.info( "." );
         _count++;
         if ( _count % 50 == 0 ) {
            EOL_LOGGER.info( " " + _count );
         }
      }
   }

}
