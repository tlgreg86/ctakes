package org.apache.ctakes.dictionary.creator.gui.umls;


import org.apache.ctakes.dictionary.creator.util.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static org.apache.ctakes.dictionary.creator.gui.umls.MrstyIndex.CUI;
import static org.apache.ctakes.dictionary.creator.gui.umls.MrstyIndex.TUI;


/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/17/14
 */
final public class MrstyParser {

   static private final Logger LOGGER = LogManager.getLogger( "MrStyParser" );

   static private final String MRSTY_SUB_PATH = "/META/MRSTY.RRF";

   private MrstyParser() {
   }

   static public Map<Long, Concept> createConceptsForTuis( final String umlsPath,
                                                           final Collection<Tui> wantedTuis ) {
      final String mrstyPath = umlsPath + MRSTY_SUB_PATH;
      LOGGER.info( "Compiling list of Cuis with wanted Tuis using " + mrstyPath );
      long lineCount = 0;
      final Map<Long,Concept> wantedConcepts = new HashMap<>();
      final Collection<Tui> usedTuis = new HashSet<>( wantedTuis.size() );
      try (final BufferedReader reader = FileUtil.createReader( mrstyPath ) ) {
         List<String> tokens = FileUtil.readBsvTokens( reader, mrstyPath );
         while ( tokens != null ) {
            lineCount++;
            if ( tokens.size() > TUI._index ) {
               final Tui tuiEnum = Tui.valueOf( tokens.get( TUI._index ) );
               if ( !wantedTuis.contains( tuiEnum ) ) {
                  tokens = FileUtil.readBsvTokens( reader, mrstyPath );
                  continue;
               }
               final Long cuiCode = CuiCodeUtil.getInstance().getCuiCode( tokens.get( CUI._index ) );
               Concept concept = wantedConcepts.get( cuiCode );
               if ( concept == null ) {
                  concept = new Concept();
                  wantedConcepts.put( cuiCode, concept );
               }
               concept.addTui( tuiEnum );
               usedTuis.add( tuiEnum );
            }
            if ( lineCount % 100000 == 0 ) {
               LOGGER.info( "File Line " + lineCount + "\t Valid Cuis " + wantedConcepts.size() );
            }
            tokens = FileUtil.readBsvTokens( reader, mrstyPath );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      LOGGER.info( "File Lines " + lineCount + "\t Valid Cuis " + wantedConcepts.size() + "\t for wanted Tuis" );
      if ( usedTuis.size() != wantedTuis.size() ) {
         wantedTuis.removeAll( usedTuis );
         for ( Tui missingTui : wantedTuis ) {
            LOGGER.warn( "Could not find Cuis for Tui " + missingTui + " " + missingTui.getDescription() );
         }
      }
      return wantedConcepts;
   }

}
