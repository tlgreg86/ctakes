package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.dictionary.lookup2.util.TuiCodeUtil;
import org.apache.ctakes.dictionary.lookup2.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.util.collection.EnumSetMap;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import static org.apache.ctakes.dictionary.lookup2.concept.ConceptCode.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
public class JdbcConceptFactory extends AbstractConceptFactory {


   // LOG4J logger based on class name
   final private Logger _logger = Logger.getLogger( getClass().getName() );


   // TODO move to Constants class
   static private final String JDBC_DRIVER = "jdbcDriver";
   static private final String JDBC_URL = "jdbcUrl";
   static private final String JDBC_USER = "jdbcUser";
   static private final String JDBC_PASS = "jdbcPass";
   static private final String TUI_TABLE = "tuiTable";
   static private final String PREF_TERM_TABLE = "prefTermTable";
   static private final String SNOMED_TABLE = "snomedTable";
   static private final String RXNORM_TABLE = "rxnormTable";
   static private final String ICD9_TABLE = "icd9Table";
   static private final String ICD10_TABLE = "icd10Table";


   final private Connection _connection;
   private PreparedStatement _selectTuiCall;
   private PreparedStatement _selectPrefTermCall;
   private PreparedStatement _selectSnomedCall;
   private PreparedStatement _selectRxNormCall;
   private PreparedStatement _selectIcd9Call;
   private PreparedStatement _selectIcd10Call;


   public JdbcConceptFactory( final String name, final UimaContext uimaContext, final Properties properties )
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      this( name,
            properties.getProperty( JDBC_DRIVER ), properties.getProperty( JDBC_URL ),
            properties.getProperty( JDBC_USER ), properties.getProperty( JDBC_PASS ),
            properties.getProperty( TUI_TABLE ), properties.getProperty( PREF_TERM_TABLE ),
            properties.getProperty( SNOMED_TABLE ), properties.getProperty( RXNORM_TABLE ),
            properties.getProperty( ICD9_TABLE ), properties.getProperty( ICD10_TABLE ) );
   }

   public JdbcConceptFactory( final String name,
                              final String jdbcDriver, final String jdbcUrl,
                              final String jdbcUser, final String jdbcPass,
                              final String tuiName, final String prefTermName,
                              final String snomedName, final String rxnormName,
                              final String icd9Name, final String icd10Name )
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
         _selectTuiCall = createSelectCall( tuiName );
         _selectPrefTermCall = createSelectCall( prefTermName );
         _selectSnomedCall = createSelectCall( snomedName );
         _selectRxNormCall = createSelectCall( rxnormName );
         _selectIcd9Call = createSelectCall( icd9Name );
         _selectIcd10Call = createSelectCall( icd10Name );
      } catch ( SQLException sqlE ) {
         _logger.error( "Could not create Concept Data Selection Call", sqlE );
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Concept createConcept( final Long cuiCode ) {
      final String prefTerm = (_selectPrefTermCall == null) ? null : getPreferredTerm( cuiCode );
      final CollectionMap<ConceptCode, String, ? extends Collection<String>> codes
            = new EnumSetMap<>( ConceptCode.class );
      if ( _selectTuiCall != null ) {
         codes.addAllValues( TUI, getTuis( cuiCode ) );
      }
      if ( _selectSnomedCall != null ) {
         codes.addAllValues( SNOMEDCT, getLongCodes( _selectSnomedCall, cuiCode ) );
      }
      if ( _selectRxNormCall != null ) {
         codes.addAllValues( RXNORM, getLongCodes( _selectRxNormCall, cuiCode ) );
      }
      if ( _selectIcd9Call != null ) {
         codes.addAllValues( ICD9CM, getStringCodes( _selectIcd9Call, cuiCode ) );
      }
      if ( _selectIcd10Call != null ) {
         codes.addAllValues( ICD10PCS, getStringCodes( _selectIcd10Call, cuiCode ) );
      }
      return new Concept( CuiCodeUtil.getAsCui( cuiCode ), prefTerm, codes );
   }


   private Collection<String> getTuis( final Long cuiCode ) {
      final Collection<String> tuis = new HashSet<>();
      try {
         fillSelectCall( _selectTuiCall, cuiCode );
         final ResultSet resultSet = _selectTuiCall.executeQuery();
         while ( resultSet.next() ) {
            tuis.add( TuiCodeUtil.getAsTui( resultSet.getInt( 2 ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         _logger.error( e.getMessage() );
      }
      return tuis;
   }

   private String getPreferredTerm( final Long cuiCode ) {
      String preferredName = "";
      try {
         fillSelectCall( _selectPrefTermCall, cuiCode );
         final ResultSet resultSet = _selectPrefTermCall.executeQuery();
         if ( resultSet.next() ) {
            preferredName = resultSet.getString( 2 );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         _logger.error( e.getMessage() );
      }
      return preferredName;
   }


   private Collection<String> getLongCodes( PreparedStatement selectCall, final Long cuiCode ) {
      final Collection<String> codes = new HashSet<>();
      try {
         fillSelectCall( selectCall, cuiCode );
         final ResultSet resultSet = selectCall.executeQuery();
         while ( resultSet.next() ) {
            codes.add( Long.toString( resultSet.getLong( 2 ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         _logger.error( e.getMessage() );
      }
      return codes;
   }


   private Collection<String> getStringCodes( PreparedStatement selectCall, final Long cuiCode ) {
      final Collection<String> codes = new HashSet<>();
      try {
         fillSelectCall( selectCall, cuiCode );
         final ResultSet resultSet = selectCall.executeQuery();
         while ( resultSet.next() ) {
            codes.add( resultSet.getString( 2 ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         _logger.error( e.getMessage() );
      }
      return codes;
   }

   /**
    * @param tableName -
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   private PreparedStatement createSelectCall( final String tableName ) throws SQLException {
      if ( tableName == null || tableName.isEmpty() || tableName.equalsIgnoreCase( "null" ) ) {
         return null;
      }
      final String lookupSql = "SELECT * FROM " + tableName + " WHERE CUI = ?";
      return _connection.prepareStatement( lookupSql );
   }


   /**
    * @param cuiCode -
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   static private void fillSelectCall( final PreparedStatement selectCall, final Long cuiCode ) throws SQLException {
      selectCall.clearParameters();
      selectCall.setLong( 1, cuiCode );
   }


}
