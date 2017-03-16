package org.apache.ctakes.dictionary.creator.gui.umls;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 7/14/14
 */
final public class DoseUtil {

   private DoseUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "DoseUtil" );


   // some of these are not strictly units, e.g. "ud" : "ut dictum" or "as directed"
   // but can be properly trimmed as they appear in the same place as would a unit

   static private final String[] UNIT_ARRAY = { "gr", "gm", "gram", "grams", "g",
                                           "mg", "milligram", "milligrams", "kg",
                                           "microgram", "micrograms", "mcg", "ug",
                                           "millicurie", "mic", "oz",
                                            "lf", "ml", "liter", "milliliter", "l",
                                           "milliequivalent", "meq",
                                           "hour", "hours", "hr", //"day", "days", "daily", //"24hr", "8hr", "12hr",
                                                "week", "weeks", "weekly", "biweekly",
                                           "usp", "titradose",
                                           "unit", "units", "unt", "iu", "u", "mmu",
                                           "mm", "cm",
                                           "gauge", "intl","au", "bau", "mci", "ud",
                                           "ww", "vv", "wv",
                                           "%", "percent", "%ww", "%vv", "%wv",
                                           "actuation", "actuat", "vial", "vil", "packet", "pkt" };
   static private final Collection<String> UNITS = Arrays.asList( UNIT_ARRAY );


   static public boolean hasUnit( final String text ) {
      final String[] splits = text.split( "\\s+" );
      if ( splits.length <= 1 ) {
         return false;
      }
      for ( int i=1; i<splits.length; i++ ) {
         for ( String unit : UNITS ) {
            if ( !splits[i].endsWith( unit ) ) {
               continue;
            }
            final int diff = splits[i].length() - unit.length();
            if ( diff == 0 ) {
               if ( i == 1 ) {
                  for ( int j=0; j<splits[0].length(); j++ ) {
                     if ( !Character.isDigit( splits[0].charAt( j ) ) ) {
                        return false;
                     }
                  }
               }
               return true;
            }
            boolean isAmount = true;
            for ( int j=0; j<diff; j++ ) {
               if ( !Character.isDigit( splits[i].charAt( j ) ) ) {
                  isAmount = false;
                  break;
               }
            }
            if ( isAmount ) {
               return true;
            }
         }
      }
      return false;
   }


}
