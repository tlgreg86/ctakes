package org.apache.ctakes.gui.dictionary.util;


import org.apache.ctakes.gui.dictionary.umls.VocabularyStore;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/12/2015
 */
final public class HsqlUtil {

   static private final Logger LOGGER = Logger.getLogger( "HsqlUtil" );

   static public final String URL_PREFIX = "jdbc:hsqldb:file:";

   private HsqlUtil() {
   }


   static public boolean createDatabase( final String databasePath, final String databaseName ) {
      final File databaseDir = new File( databasePath, databaseName );
      if ( databaseDir.isFile() ) {
         LOGGER.error( databaseDir.getPath() + " exists as a file.  Hsqldb requires that path to be a directory" );
         return false;
      }
      databaseDir.mkdirs();
      return writePropertiesFile( databaseDir, databaseName )
             && writeScriptFile( databaseDir, databaseName )
             && writeRcFile( databaseDir, databaseName );
   }

   static private boolean writePropertiesFile( final File databaseDir, final String databaseName ) {
      final File propertiesFile = new File( databaseDir, databaseName + ".properties" );
      try ( final Writer writer = new BufferedWriter( new FileWriter( propertiesFile ) ) ) {
         writer.write( "#HSQL Database Engine 1.8.0.10\n" );
         writer.write( "#Thu Sep 04 09:49:09 EDT 2014\n" );
         writer.write( "hsqldb.script_format=0\n" );
         writer.write( "runtime.gc_interval=0\n" );
         writer.write( "sql.enforce_strict_size=false\n" );
         writer.write( "hsqldb.cache_size_scale=8\n" );
         writer.write( "readonly=false\n" );
         writer.write( "hsqldb.nio_data_file=true\n" );
         writer.write( "hsqldb.cache_scale=14\n" );
         writer.write( "version=1.8.0\n" );
         writer.write( "hsqldb.default_table_type=memory\n" );
         writer.write( "hsqldb.cache_file_scale=1\n" );
         writer.write( "hsqldb.log_size=200\n" );
         writer.write( "modified=no\n" );
         writer.write( "hsqldb.cache_version=1.7.0\n" );
         writer.write( "hsqldb.original_version=1.8.0\n" );
         writer.write( "hsqldb.compatible_version=1.8.0\n\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }

   static private boolean writeScriptFile( final File databaseDir, final String databaseName ) {
      final File scriptFile = new File( databaseDir, databaseName + ".script" );
      try ( final Writer writer = new BufferedWriter( new FileWriter( scriptFile ) ) ) {
         writer.write( "CREATE SCHEMA PUBLIC AUTHORIZATION DBA\n" );
         // main table
         writer.write( "CREATE MEMORY TABLE CUI_TERMS(CUI BIGINT,RINDEX INTEGER,TCOUNT INTEGER,TEXT VARCHAR(255),RWORD VARCHAR(48))\n" );
         writer.write( "CREATE INDEX IDX_CUI_TERMS ON CUI_TERMS(RWORD)\n" );
         // tui table
         writer.write( "CREATE MEMORY TABLE TUI(CUI BIGINT,TUI INTEGER)\n" );
         writer.write( "CREATE INDEX IDX_TUI ON TUI(CUI)\n" );
         // preferred term table
         writer.write( "CREATE MEMORY TABLE PREFTERM(CUI BIGINT,PREFTERM VARCHAR(255))\n" );
         writer.write( "CREATE INDEX IDX_PREFTERM ON PREFTERM(CUI)\n" );
         // vocabulary tables
         for ( String vocabulary : VocabularyStore.getInstance().getAllVocabularies() ) {
            final String jdbcClass = VocabularyStore.getInstance().getJdbcClass( vocabulary );
            final String tableName = vocabulary.replace( '.', '_' ).replace( '-', '_' );
            writer.write( "CREATE MEMORY TABLE " + tableName + "(CUI BIGINT," + tableName + " " + jdbcClass + ")\n" );
            writer.write( "CREATE INDEX IDX_" + tableName + " ON " + tableName + "(CUI)\n" );
         }
         writer.write( "CREATE USER SA PASSWORD \"\"\n" );
         writer.write( "GRANT DBA TO SA\n" );
         writer.write( "SET WRITE_DELAY 10\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }

   static private boolean writeRcFile( final File databaseDir, final String databaseName ) {
      final File scriptFile = new File( databaseDir, databaseName + ".rc" );
      final String url = HsqlUtil.URL_PREFIX + databaseDir.getPath().replace( '\\', '/' )
                         + "/" + databaseName;
      try ( final Writer writer = new BufferedWriter( new FileWriter( scriptFile ) ) ) {
         writer.write( "urlid " + databaseName + "\n" );
         writer.write( "url " + url + ";shutdown=true\n" );
         writer.write( "username sa\n" );
         writer.write( "password\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }


}
