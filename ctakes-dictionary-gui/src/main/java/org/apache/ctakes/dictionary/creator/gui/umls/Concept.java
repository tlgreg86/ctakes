package org.apache.ctakes.dictionary.creator.gui.umls;

import org.apache.ctakes.dictionary.creator.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.creator.util.collection.HashSetMap;

import java.util.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
final public class Concept {

   static public String PREFERRED_TERM_UNKNOWN = "Unknown Preferred Term";

   private String _preferredText = null;
   private boolean _hasDose = false;

   final private Collection<String> _texts;
   final private CollectionMap<String, String, ? extends Collection<String>> _codes;
   final private Collection<Tui> _tuis;



   public Concept() {
      _codes = new HashSetMap<>( 0 );
      _texts = new HashSet<>( 1 );
      _tuis = EnumSet.noneOf( Tui.class );
   }

   public boolean addTexts( final Collection<String> texts ) {
      return _texts.addAll( texts );
   }

   public void removeTexts( final Collection<String> texts ) {
      _texts.removeAll( texts );
   }

   public Collection<String> getTexts() {
      return _texts;
   }

   public void minimizeTexts() {
      if ( _texts.size() < 2 ) {
         return;
      }
      final List<String> textList = new ArrayList<>( _texts );
      final Collection<String> extensionTexts = new HashSet<>();
      for ( int i=0; i<textList.size()-1; i++ ) {
         final String iText = textList.get( i );
         for ( int j=i+1; j<textList.size(); j++ ) {
            final String jText = textList.get( j );
            if ( textContained( jText, iText ) ) {
               extensionTexts.add( jText );
            } else if ( textContained( iText, jText ) ) {
               extensionTexts.add( iText );
            }
         }
      }
      _texts.removeAll( extensionTexts );
   }

   static private boolean textContained( final String containerText, final String containedText ) {
      final int index = containerText.indexOf( containedText );
      return index >= 0
             && ( index == 0 || containerText.charAt( index-1 ) == ' ' )
           && ( index+containedText.length() == containerText.length() || containerText.charAt( index + containedText.length() ) == ' ' );
   }

   public void setPreferredText( final String text ) {
      _preferredText = text;
   }

   public String getPreferredText() {
      if ( _preferredText != null ) {
         return _preferredText;
      }
      return PREFERRED_TERM_UNKNOWN;
   }

   public void addCode( final String source, final String code ) {
      _codes.placeValue( source, code );
   }

   public Collection<String> getVocabularies() {
      return _codes.keySet();
   }

   public Collection<String> getCodes( final String source ) {
      final Collection<String> codes = _codes.getCollection( source );
      if ( codes == null ) {
         return Collections.emptyList();
      }
      return codes;
   }

   public void addTui( final Tui tui ) {
      _tuis.add( tui );
   }

   public Collection<Tui> getTuis() {
      return _tuis;
   }

   public boolean hasTui( final Collection<Tui> tuis ) {
      return _tuis.stream().anyMatch( tuis::contains );
   }

   public boolean isEmpty() {
//      return _texts.isEmpty() || _codes.isEmpty();
      return _texts.isEmpty();
   }

   public void setHasDose() {
      _hasDose = true;
   }

   public boolean hasDose() {
      return _hasDose;
   }

   public boolean isUnwanted() {
      return hasDose() || isEmpty();
   }

}
