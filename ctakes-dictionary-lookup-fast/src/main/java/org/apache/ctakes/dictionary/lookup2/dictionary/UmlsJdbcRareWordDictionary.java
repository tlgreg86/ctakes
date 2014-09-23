package org.apache.ctakes.dictionary.lookup2.dictionary;

import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.ctakes.dictionary.lookup2.util.UmlsUserApprover;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.util.Collection;
import java.util.Properties;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2014
 */
final public class UmlsJdbcRareWordDictionary implements RareWordDictionary {

   static private final Logger LOGGER = Logger.getLogger( "UmlsJdbcRareWordDictionary" );

   private final static String URL_PARAM = "umlsUrl";
   private final static String VENDOR_PARAM = "umlsVendor";
   private final static String USER_PARAM = "umlsUser";
   private final static String PASS_PARAM = "umlsPass";


   final private RareWordDictionary _delegateDictionary;


   public UmlsJdbcRareWordDictionary( final String name, final UimaContext uimaContext, final Properties properties )
         throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      final String umlsUrl = properties.getProperty( URL_PARAM );
      final String vendor = properties.getProperty( VENDOR_PARAM );
      final String user = properties.getProperty( USER_PARAM );
      final String pass = properties.getProperty( PASS_PARAM );
      final boolean isValidUser = UmlsUserApprover.isValidUMLSUser( umlsUrl, vendor, user, pass );
      if ( !isValidUser ) {
         LOGGER.error( "UMLS Account at " + umlsUrl + " is not valid for user " + user + " with " + pass );
         throw new InstantiationException();
      }
      _delegateDictionary = new JdbcRareWordDictionary( name, uimaContext, properties );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateDictionary.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final FastLookupToken fastLookupToken ) {
      return _delegateDictionary.getRareWordHits( fastLookupToken );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
      return _delegateDictionary.getRareWordHits( rareWordText );
   }


}
