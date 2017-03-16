package org.apache.ctakes.dictionary.creator.gui.umls;

import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/5/2016
 */
public class DoseUtilTester {

   static private final Logger LOGGER = Logger.getLogger( "DoseUtilTester" );

   @Test
   public void testHasUnit() {
      Assert.assertTrue( "No ml detected!",
            DoseUtil.hasUnit( "alcohol . 31 ml in 1 ml topical cloth [ alcohol wipes ]" ) );
      Assert.assertTrue( "No mpa detected!",
            DoseUtil.hasUnit( "polyquaternium - 32 ( 30000 mpa . s at 2 % )" ) );
      Assert.assertTrue( "No mg detected!",
            DoseUtil.hasUnit( "myasthenia gravis ( mg )" ) );
      Assert.assertTrue( "No % detected!",
            DoseUtil.hasUnit( "imiquimod 2 . 5 % top cream" ) );

   }

}
