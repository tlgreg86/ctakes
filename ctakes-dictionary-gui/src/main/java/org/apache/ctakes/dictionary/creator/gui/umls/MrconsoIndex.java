package org.apache.ctakes.dictionary.creator.gui.umls;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/23/14
 */
public enum MrconsoIndex {
   CUI( 0 ), LANGUAGE( 1 ), STATUS( 2 ), FORM( 4 ), SOURCE( 11 ), TERM_TYPE( 12 ), SOURCE_CODE( 13 ), TEXT( 14 );
   final public int _index;

   private MrconsoIndex( final int index ) {
      _index = index;
   }

}
