package org.apache.ctakes.dictionary.creator.gui.ctakes;


import org.apache.ctakes.dictionary.creator.gui.umls.*;
import org.apache.ctakes.dictionary.creator.util.HsqlUtil;
import org.apache.ctakes.dictionary.creator.util.RareWordDbWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/11/2015
 */
final public class DictionaryBuilder {

   static private final Logger LOGGER = LogManager.getLogger( "DictionaryBuilder" );

   // TODO  static private final String DEFAULT_DATA_DIR = "resources/org/apache/ctakes/dictionary-gui/data/default";
   static private final String DEFAULT_DATA_DIR = "resources/org/apache/ctakes/dictionary-gui/data/tiny";
   static private final String CTAKES_APP_DB_PATH = "resources/org/apache/ctakes/dictionary/lookup/fast";
   static private final String CTAKES_RES_MODULE = "ctakes-dictionary-lookup-fast-res";
   static private final String CTAKES_RES_DB_PATH = CTAKES_RES_MODULE + "/src/main/" + CTAKES_APP_DB_PATH;
   static private final String RXNORM = "RXNORM";
   static private final int MIN_CHAR_LENGTH = 2;
   static private final int MAX_CHAR_LENGTH = 50;
   static private final int MAX_WORD_COUNT = 12;
   static private final int MAX_SYM_COUNT = 7;

   private DictionaryBuilder() {}

   static public boolean buildDictionary( final String umlsDirPath,
                                          final String ctakesDirPath,
                                          final String dictionaryName,
                                          final Collection<String> wantedLanguages,
                                          final Collection<String> wantedSources,
                                          final Collection<String> wantedTargets,
                                          final Collection<Tui> wantedTuis ) {
      // Set up the term utility
      final UmlsTermUtil umlsTermUtil = new UmlsTermUtil( DEFAULT_DATA_DIR );
      final Map<Long,Concept> conceptMap = parseAll( umlsTermUtil, umlsDirPath, wantedLanguages, wantedSources, wantedTargets, wantedTuis );

      // special case for nitric oxide "no"
      final Concept nitricOxide = conceptMap.get( 28128l );
      if ( nitricOxide != null ) {
         nitricOxide.removeTexts( Collections.singletonList( "no" ) );
      }
      // special case for nitric oxide synthase "nos"
      final Concept nitricOxides = conceptMap.get( 132555l );
      if ( nitricOxides != null ) {
         nitricOxides.removeTexts( Arrays.asList( "nos", "synthase" ) );
      }

      return writeDatabase( ctakesDirPath, dictionaryName, conceptMap );
   }




   static private Map<Long,Concept> parseAll( final UmlsTermUtil umlsTermUtil,
                                              final String umlsDirPath,
                                              final Collection<String> wantedLanguages,
                                              final Collection<String> wantedSources,
                                              final Collection<String> wantedTargets,
                                              final Collection<Tui> wantedTuis ) {
      LOGGER.info( "Parsing Concepts" );
      // Create a map of Cuis to empty Concepts for all wanted Tuis and source vocabularies
      final Map<Long,Concept> conceptMap
            = ConceptMapFactory.createInitialConceptMap( umlsDirPath, wantedSources, wantedTuis );
      // Fill in information for all valid concepts
      MrconsoParser.parseAllConcepts( umlsDirPath, conceptMap, wantedTargets, umlsTermUtil,
            wantedLanguages, true, MIN_CHAR_LENGTH, MAX_CHAR_LENGTH, MAX_WORD_COUNT, MAX_SYM_COUNT );
      removeUnwantedConcepts( conceptMap );
      removeUnwantedDrugs( conceptMap, wantedTuis );
      // Cull non-ANAT texts by ANAT texts as determined by ANAT tuis
      removeAnatTexts( conceptMap.values(), wantedTuis );
      conceptMap.values().forEach( Concept::minimizeTexts );
      LOGGER.info( "Done Parsing Concepts" );
      return conceptMap;
   }

   /**
    * Remove any concepts that are unwanted - don't have any text from a desired vocabulary
    * @param conceptMap -
    */
   static private void removeUnwantedConcepts( final Map<Long, Concept> conceptMap ) {
      final Collection<Long> empties = conceptMap.entrySet().stream()
            .filter( e -> e.getValue().isUnwanted() )
            .map( Map.Entry::getKey )
            .collect( Collectors.toSet() );
      conceptMap.keySet().removeAll( empties );
   }

   static private Collection<String> getAnatTexts( final Collection<Concept> concepts, final Collection<Tui> wantedTuis ) {
      final Collection<Tui> wantedAnatTuis = new ArrayList<>( wantedTuis );
      wantedAnatTuis.retainAll( Arrays.asList( TuiTableModel.CTAKES_ANAT ) );
      return concepts.stream()
            .filter( c -> c.hasTui( wantedAnatTuis ) )
            .map( Concept::getTexts )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }

   static private void removeAnatTexts( final Collection<Concept> concepts,
                                        final Collection<Tui> wantedTuis,
                                        final Collection<String> anatTexts ) {
      final Collection<Tui> nonAnatTuis = new ArrayList<>( wantedTuis );
      nonAnatTuis.removeAll( Arrays.asList( TuiTableModel.CTAKES_ANAT ) );
      concepts.stream()
            .filter( c -> c.hasTui( nonAnatTuis ) )
            .forEach( c -> c.removeTexts( anatTexts ) );
   }

   static private void removeAnatTexts( final Collection<Concept> concepts,
                                        final Collection<Tui> wantedTuis ) {
      final Collection<String> anatTexts = getAnatTexts( concepts, wantedTuis );
      removeAnatTexts( concepts, wantedTuis, anatTexts );
   }


   static private void removeUnwantedDrugs( final Map<Long,Concept> conceptMap, Collection<Tui> wantedTuis ) {
      // remove concepts that have only drug tuis but are not in rxnorm
      final Collection<Tui> drugTuis = new ArrayList<>( wantedTuis );
      drugTuis.retainAll( Arrays.asList( TuiTableModel.CTAKES_DRUG ) );
      // remove concepts that are in rxnorm but have non-drug tuis
      final Collection<Tui> nonDrugTuis = new ArrayList<>( wantedTuis );
      nonDrugTuis.removeAll( Arrays.asList( TuiTableModel.CTAKES_DRUG ) );
      // if concept has drug tuis but is not in rxnorm || concept is in rxnorm but does not have drug tuis
      final Predicate<Map.Entry<Long,Concept>> unwantedDrug
            = e -> ( drugTuis.containsAll( e.getValue().getTuis() )
            && !e.getValue().getVocabularies().contains( RXNORM ) )
            || ( e.getValue().getVocabularies().contains( RXNORM )
            && nonDrugTuis.containsAll( e.getValue().getTuis() ) );

      final Collection<Long> removalCuis = conceptMap.entrySet().stream()
            .filter( unwantedDrug )
            .map( Map.Entry::getKey )
            .collect( Collectors.toSet() );
      conceptMap.keySet().removeAll( removalCuis );
   }


   static private boolean writeDatabase( final String ctakesDirPath,
                                         final String dictionaryName,
                                         final Map<Long,Concept> conceptMap ) {
      final File ctakesRoot = new File( ctakesDirPath );
      String databaseDirPath = ctakesDirPath + "/" + CTAKES_APP_DB_PATH;
      if ( Arrays.asList( ctakesRoot.list() ).contains( CTAKES_RES_MODULE ) ) {
         databaseDirPath = ctakesDirPath + "/" + CTAKES_RES_DB_PATH;
      }
      if ( !HsqlUtil.createDatabase( databaseDirPath, dictionaryName ) ) {
         return false;
      }
      if ( !DictionaryXmlWriter.writeXmlFile( databaseDirPath, dictionaryName ) ) {
         return false;
      }
      final String url = HsqlUtil.URL_PREFIX + databaseDirPath.replace( '\\', '/' ) + "/" + dictionaryName + "/" + dictionaryName;
      return RareWordDbWriter.writeConcepts( conceptMap, url, "sa", "" );
   }


}
