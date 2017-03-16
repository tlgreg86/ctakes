package org.apache.ctakes.dictionary.creator.gui.umls;


import org.apache.ctakes.dictionary.creator.util.FileUtil;
import org.apache.ctakes.dictionary.creator.util.TextTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static org.apache.ctakes.dictionary.creator.gui.umls.MrconsoIndex.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/17/14
 */
final public class MrconsoParser {

   static private final Logger LOGGER = LogManager.getLogger( "MrConsoParser" );

   static private final String MR_CONSO_SUB_PATH = "/META/MRCONSO.RRF";

   // TODO - put all exclusions in a data file, display for user, allow changes and save, etc.

   //  https://www.nlm.nih.gov/research/umls/sourcereleasedocs
   //  https://www.nlm.nih.gov/research/umls/sourcereleasedocs/current/SNOMEDCT_US/stats.html
   //  https://www.nlm.nih.gov/research/umls/sourcereleasedocs/current/RXNORM/stats.html
   static private final String[] DEFAULT_EXCLUSIONS = { "FN", "CCS", "CA2", "CA3", "PSN", "TMSY",
                                                     "SBD", "SBDC", "SBDF", "SBDG",
                                                     "SCD", "SCDC", "SCDF", "SCDG", "BPCK", "GPCK", "XM" };

   static private final String[] SNOMED_OBSOLETES = { "OF", "MTH_OF", "OAP", "MTH_OAP", "OAF", "MTH_OAF",
                                                     "IS", "MTH_IS", "OAS", "MTH_OAS",
                                                     "OP", "MTH_OP" };
   // Snomed OF  = Obsolete Fully Specified Name      MTH_OF
   // Snomed OAP = Obsolete Active Preferred Term     MTH_OAP
   // Snomed OAF = Obsolete Active Full Name          MTH_OAF
   // Snomed IS  = Obsolete Synonym                   MTH_IS
   // Snomed OAS = Obsolete Active Synonym            MTH_OAS
   // Snomed OP  = Obsolete Preferred Name            MTH_OP
   // Snomed PT  = Preferred Term , but we don't need that for valid cuis ...  or do we want only those with preferred terms?
   // Snomed PTGB = British Preferred Term

   // GO has same snomed obsoletes +
   // GO EOT = Obsolete Entry Term
   // HPO has same snomed obsoletes

   // MTHSPL - DP is Drug Product  as is MTH_RXN_DP      MTHSPL SU is active substance
   // VANDF AB  is abbreviation for drug  VANDF CD is Clinical Drug.  Both are dosed.
   //  NDFRT AB?  Looks like ingredient.  NDFRT PT can be dosed

   static private final String[] GO_OBSOLETES = { "EOT" };

   static private final String[] LOINC_OBSOLETES = { "LO", "OLC", "MTH_LO", "OOSN" };

   static private final String[] MEDRA_OBSOLETES = { "OL", "MTH_OL" };

   static private final String[] MESH_EXCLUSIONS = { "N1", "EN", "PEN" };

   static private final String[] RXNORM_EXCLUSIONS = { "SY" };   // What is IN ?  Ingredient?

   static private final String[] NCI_EXCLUSIONS = { "CSN" };

   // Related to, but not synonymous
   static private final String[] UMDNS_EXCLUSIONS = { "RT" };

   private MrconsoParser() {
   }

   static public String[] getDefaultExclusions() {
      return DEFAULT_EXCLUSIONS;
   }

   static public String[] getSnomedExclusions() {
      final String[] defaults = getDefaultExclusions();
      final String[] exclusionTypes = Arrays.copyOf( defaults,
            defaults.length + SNOMED_OBSOLETES.length );
      System.arraycopy( SNOMED_OBSOLETES, 0, exclusionTypes, defaults.length, SNOMED_OBSOLETES.length );
      return exclusionTypes;
   }

   static public String[] getNonRxnormExclusions() {
      final String[] snomeds = getSnomedExclusions();
      final String[] exclusionTypes = Arrays.copyOf( snomeds,
            snomeds.length
            + GO_OBSOLETES.length
            + LOINC_OBSOLETES.length
            + MEDRA_OBSOLETES.length
            + MESH_EXCLUSIONS.length
            + NCI_EXCLUSIONS.length
            + UMDNS_EXCLUSIONS.length );
      int start = snomeds.length;
      System.arraycopy( GO_OBSOLETES, 0, exclusionTypes, start, GO_OBSOLETES.length );
      start += GO_OBSOLETES.length;
      System.arraycopy( LOINC_OBSOLETES, 0, exclusionTypes, start, LOINC_OBSOLETES.length );
      start += LOINC_OBSOLETES.length;
      System.arraycopy( MEDRA_OBSOLETES, 0, exclusionTypes, start, MEDRA_OBSOLETES.length );
      start += MEDRA_OBSOLETES.length;
      System.arraycopy( MESH_EXCLUSIONS, 0, exclusionTypes, start, MESH_EXCLUSIONS.length );
      start += MESH_EXCLUSIONS.length;
      System.arraycopy( NCI_EXCLUSIONS, 0, exclusionTypes, start, NCI_EXCLUSIONS.length );
      start += NCI_EXCLUSIONS.length;
      System.arraycopy( UMDNS_EXCLUSIONS, 0, exclusionTypes, start, UMDNS_EXCLUSIONS.length );
      return exclusionTypes;
   }



   static public Map<Long, Concept> parseAllConcepts( final String umlsDirPath,
                                                   final Map<Long, Concept> concepts,
                                                   final Collection<String> wantedTargets,
                                                   final UmlsTermUtil umlsTermUtil,
                                                   final Collection<String> languages,
                                                   final boolean extractAbbreviations,
                                                   final int minCharLength,
                                                      final int maxCharLength,
                                                   final int maxWordCount,
                                                      final int maxSymCount ) {
      final String mrconsoPath = umlsDirPath + MR_CONSO_SUB_PATH;
      final Collection<String> invalidTypeSet = new HashSet<>( Arrays.asList( getNonRxnormExclusions() ) );
      LOGGER.info( "Compiling map of Concepts from " + mrconsoPath );
      long lineCount = 0;
      long textCount = 0;
      try ( final BufferedReader reader = FileUtil.createReader( mrconsoPath ) ) {
         List<String> tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
         while ( tokens != null ) {
            lineCount++;
            if ( lineCount % 100000 == 0 ) {
               LOGGER.info( "File Line " + lineCount + "   Texts " + textCount );
            }
            if ( !isRowOk( tokens, languages, invalidTypeSet ) ) {
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            final Long cuiCode = CuiCodeUtil.getInstance().getCuiCode( getToken( tokens, CUI ) );
            final Concept concept = concepts.get( cuiCode );
            if ( concept == null ) {
               // cui for current row is unwanted
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            final String text = getToken( tokens, TEXT );
            if ( isPreferredTerm( tokens ) ) {
               concept.setPreferredText( text );
            }
            final String source = getToken( tokens, SOURCE );
            if ( wantedTargets.contains( source ) ) {
               final String code = getToken( tokens, SOURCE_CODE );
               if ( !code.equals( "NOCODE" ) ) {
                  Vocabulary.getInstance().addVocabulary( source, code );
                  concept.addCode( source, code );
               }
            }
            final String tokenizedText = TextTokenizer.getTokenizedText( text );
            if ( tokenizedText == null || tokenizedText.isEmpty()
                 || !umlsTermUtil.isTextValid( tokenizedText ) ) {
               // no tokenizable text or tokenized text is invalid for some reason
//               LOGGER.warn( tokenizedText + " not valid" );
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            final String strippedText = umlsTermUtil.getStrippedText( tokenizedText );
            if ( strippedText == null || strippedText.isEmpty()
                 || UmlsTermUtil.isTextTooShort( strippedText, minCharLength )
                 || UmlsTermUtil.isTextTooLong( strippedText, maxCharLength, maxWordCount, maxSymCount ) ) {
               // after stripping unwanted prefixes and suffixes there is no valid text
//               LOGGER.warn( tokenizedText + " stripped invalid" );
               tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
               continue;
            }
            final Collection<String> formattedTexts
                  = umlsTermUtil.getFormattedTexts( strippedText, extractAbbreviations, minCharLength, maxCharLength, maxWordCount, maxSymCount );
            if ( formattedTexts != null && !formattedTexts.isEmpty() ) {
               if ( DoseUtil.hasUnit( tokenizedText ) ) {
                  concept.setHasDose();
//                  LOGGER.warn( tokenizedText + " has dose" );
                  tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
                  continue;
               }
               if ( concept.addTexts( formattedTexts ) ) {
                  textCount += formattedTexts.size();
               }
            }
            tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      LOGGER.info( "File Lines " + lineCount + "   Texts " + textCount );
      return concepts;
   }


   static private boolean isRowOk( final List<String> tokens,
                                   final Collection<String> languages,
                                   final Collection<String> invalidTypeSet ) {
      if ( tokens.size() <= TEXT._index || !languages.contains( getToken( tokens, LANGUAGE ) ) ) {
         return false;
      }
      final String type = getToken( tokens, TERM_TYPE );
      if ( invalidTypeSet.contains( type ) ) {
         return false;
      }
      // "Synonyms" are actually undesirable in the rxnorm vocabulary
      final String source = getToken( tokens, SOURCE );
      return !( source.equals( "RXNORM" ) && type.equals( "SY" ) );
   }


   static private boolean isPreferredTerm( final List<String> tokens ) {
      return getToken( tokens, STATUS ).equals( "P" ) && getToken( tokens, FORM ).equals( "PF" );
   }


   /**
    * Can cull the given collection of cuis
    *
    * @param umlsDirPath     path to the UMLS_ROOT Meta/MRCONSO.RRF file
    * @param sourceVocabularies desired source type names as appear in rrf: RXNORM, SNOMEDCT, MSH, etc.
    * @return Subset of cuis that exist in in the given sources
    */
   static public Collection<Long> getValidVocabularyCuis( final String umlsDirPath,
                                                          final Collection<String> sourceVocabularies ) {
//      return getValidVocabularyCuis( umlsDirPath, sourceVocabularies, getDefaultExclusions() );
      return getValidVocabularyCuis( umlsDirPath, sourceVocabularies, getNonRxnormExclusions() );
   }

//   /**
//    * Can cull the given collection of cuis
//    *
//    * @param umlsDirPath     path to the UMLS_ROOT Meta/MRCONSO.RRF file
//    * @return Subset of cuis that exist in in the given sources
//    */
//   static public Collection<Long> getValidRxNormCuis( final String umlsDirPath ) {
//      return getValidVocabularyCuis( umlsDirPath, Collections.singletonList( "RXNORM" ), getRxnormExclusions() );
//   }

   /**
    * Can cull the given collection of cuis
    *
    * @param umlsDirPath     path to the UMLS_ROOT Meta/MRCONSO.RRF file
    * @param sourceVocabularies desired source type names as appear in rrf: RXNORM, SNOMEDCT, MSH, etc.
    * @param invalidTypes term type names as appear in rrf: FN, CCS, etc. that are not valid
    * @return Subset of cuis that exist in in the given sources
    */
   static private Collection<Long> getValidVocabularyCuis( final String umlsDirPath,
                                                           final Collection<String> sourceVocabularies,
                                                           final String... invalidTypes ) {
      final String mrconsoPath = umlsDirPath + MR_CONSO_SUB_PATH;
      LOGGER.info( "Compiling list of Cuis with wanted Vocabularies using " + mrconsoPath );
      final Collection<Long> validCuis = new HashSet<>();
      long lineCount = 0;
      try ( final BufferedReader reader = FileUtil.createReader( mrconsoPath ) ) {
         List<String> tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
         while ( tokens != null ) {
            lineCount++;
            if ( lineCount % 100000 == 0 ) {
               LOGGER.info( "File Line " + lineCount + "\t Valid Cuis " + validCuis.size() );
            }
            if ( tokens.size() > SOURCE._index
                 && sourceVocabularies.stream().anyMatch( getToken( tokens, SOURCE )::equals )
                 && Arrays.stream( invalidTypes ).noneMatch( getToken( tokens, TERM_TYPE )::equals ) ) {
               final Long cuiCode = CuiCodeUtil.getInstance().getCuiCode( getToken( tokens, CUI ) );
               validCuis.add( cuiCode );
            }
            tokens = FileUtil.readBsvTokens( reader, mrconsoPath );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      LOGGER.info( "File Lines " + lineCount + "\t Valid Cuis " + validCuis.size() + "\t for wanted Vocabularies" );
      return validCuis;
   }


   static private String getToken( final List<String> tokens, final MrconsoIndex mrconsoIndex ) {
      return tokens.get( mrconsoIndex._index );
   }


}
