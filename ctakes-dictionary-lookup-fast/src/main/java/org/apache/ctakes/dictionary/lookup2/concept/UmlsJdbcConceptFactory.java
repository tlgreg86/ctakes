package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.dictionary.lookup2.util.UmlsUserApprover;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2014
 */
final public class UmlsJdbcConceptFactory implements ConceptFactory {

   static private final Logger LOGGER = Logger.getLogger( "UmlsJdbcConceptFactory" );

   private final static String URL_PARAM = "umlsUrl";
   private final static String VENDOR_PARAM = "umlsVendor";
   private final static String USER_PARAM = "umlsUser";
   private final static String PASS_PARAM = "umlsPass";


   final private ConceptFactory _delegateConceptFactory;


   public UmlsJdbcConceptFactory( final String name, final UimaContext uimaContext, final Properties properties )
         throws SQLException {
      final String umlsUrl = properties.getProperty( URL_PARAM );
      final String vendor = properties.getProperty( VENDOR_PARAM );
      final String user = properties.getProperty( USER_PARAM );
      final String pass = properties.getProperty( PASS_PARAM );
      final boolean isValidUser = UmlsUserApprover.isValidUMLSUser( umlsUrl, vendor, user, pass );
      if ( !isValidUser ) {
         LOGGER.error( "UMLS Account at " + umlsUrl + " is not valid for user " + user + " with " + pass );
         throw new SQLException( "Invalid User for UMLS Concept Factory " + name );
      }
      LOGGER.info( "UMLS Account at " + umlsUrl + " for user " + user + " has been validated" );
      _delegateConceptFactory = new JdbcConceptFactory( name, uimaContext, properties );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateConceptFactory.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Concept createConcept( final Long cuiCode ) {
      return _delegateConceptFactory.createConcept( cuiCode );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<Long, Concept> createConcepts( final Collection<Long> cuiCodes ) {
      return _delegateConceptFactory.createConcepts( cuiCodes );
   }

}
