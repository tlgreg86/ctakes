package org.apache.ctakes.core.cc.pretty;

import java.util.Arrays;
import java.util.Collection;

/**
 * enumeration of ctakes semantic types:
 * anatomical site, disease/disorder, finding (sign/symptom), test/procedure, and medication
 */
public enum SemanticGroup {
   ////////////////////////////  Similar is in org.apache.ctakes.dictionary.lookup2.util.SemanticUtil
   ////////////////////////////  and should be moved to core if this new class is taken up
   // cTakes types
   ANATOMICAL_SITE( "Anatomy", "ANT", "T021", "T022", "T023", "T024", "T025", "T026", "T029", "T030" ),
   DISORDER( "Disorder", "DIS", "T019", "T020", "T037", "T047", "T048", "T049", "T050", "T190", "T191" ),
   FINDING( "Finding", "FND", "T033", "T034", "T040", "T041", "T042", "T043", "T044", "T045", "T046",
         "T056", "T057", "T184" ),
   PROCEDURE( "Procedure", "PRC", "T059", "T060", "T061" ),
   MEDICATION( "Drug", "DRG", "T109", "T110", "T114", "T115", "T116", "T118", "T119",
         "T121", "T122", "T123", "T124", "T125", "T126", "T127",
         "T129", "T130", "T131", "T195", "T196", "T197", "T200", "T203" );

   static public final String UNKNOWN_SEMANTIC = "Unknown";
   static public final String UNKNOWN_SEMANTIC_CODE = "UNK";
   final private String _name;
   final private String _code;
   final private Collection<String> _tuis;

   /**
    * ctakes semantic type defined by tuis
    *
    * @param name short name of the type: anatomy, disorder, finding, procedure, drug
    * @param code short code of the type: ANT, DIS, FND, PRC, DRG
    * @param tuis tuis that define the semantic type
    */
   SemanticGroup( final String name, final String code, final String... tuis ) {
      _name = name;
      _code = code;
      _tuis = Arrays.asList( tuis );
   }

   /**
    * @return name of this semantic type
    */
   public String getName() {
      return _name;
   }

   /**
    * @return code of this semantic type
    */
   public String getCode() {
      return _code;
   }

   /**
    * @param tui a tui of interest
    * @return the name of a Semantic type associated with the tui
    */
   static public String getSemanticName( final String tui ) {
      if ( tui == null || tui.isEmpty() ) {
         return UNKNOWN_SEMANTIC;
      }
      for ( SemanticGroup semanticGroup : SemanticGroup.values() ) {
         if ( semanticGroup._tuis.contains( tui ) ) {
            return semanticGroup._name;
         }
      }
      return UNKNOWN_SEMANTIC;
   }

   /**
    * @param tui a tui of interest
    * @return the code of a Semantic type associated with the tui
    */
   static public String getSemanticCode( final String tui ) {
      if ( tui == null || tui.isEmpty() ) {
         return UNKNOWN_SEMANTIC_CODE;
      }
      for ( SemanticGroup semanticGroup : SemanticGroup.values() ) {
         if ( semanticGroup._tuis.contains( tui ) ) {
            return semanticGroup._code;
         }
      }
      return UNKNOWN_SEMANTIC_CODE;
   }

   /**
    * @param code the code of a Semantic type
    * @return the name of a Semantic type associated with the given code
    */
   static public String getNameForCode( final String code ) {
      for ( SemanticGroup semanticGroup : SemanticGroup.values() ) {
         if ( semanticGroup._code.equals( code ) ) {
            return semanticGroup._name;
         }
      }
      return UNKNOWN_SEMANTIC;
   }


}
