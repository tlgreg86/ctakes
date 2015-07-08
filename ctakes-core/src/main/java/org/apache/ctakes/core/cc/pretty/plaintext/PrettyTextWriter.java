package org.apache.ctakes.core.cc.pretty.plaintext;

import org.apache.ctakes.core.cc.pretty.SemanticGroup;
import org.apache.ctakes.core.cc.pretty.cell.DefaultBaseItemCell;
import org.apache.ctakes.core.cc.pretty.cell.DefaultUmlsItemCell;
import org.apache.ctakes.core.cc.pretty.cell.ItemCell;
import org.apache.ctakes.core.cc.pretty.row.DefaultItemRow;
import org.apache.ctakes.core.cc.pretty.row.ItemRow;
import org.apache.ctakes.core.cc.pretty.textspan.DefaultTextSpan;
import org.apache.ctakes.core.cc.pretty.textspan.TextSpan;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.IdentifiedAnnotationUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * Writes Document text, pos, semantic types and cuis to file.  Each Sentence starts a new series of pretty text lines.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/24/2015
 */
final public class PrettyTextWriter {

   static final String PARAM_OUTPUTDIR = "OutputDirectory";

   static private final Logger LOGGER = Logger.getLogger( "PrettyTextWriter" );
   static private final String FILE_EXTENSION = ".pretty.txt";

   private String _outputDirPath;

   /**
    * @param outputDirectoryPath may be empty or null, in which case the current working directory is used
    * @throws IllegalArgumentException if the provided path points to a File and not a Directory
    * @throws SecurityException        if the File System has issues
    */
   public void setOutputDirectory( final String outputDirectoryPath ) throws IllegalArgumentException,
                                                                             SecurityException {
      // If no outputDir is specified (null or empty) the current working directory will be used.  Else check path.
      if ( outputDirectoryPath == null || outputDirectoryPath.isEmpty() ) {
         _outputDirPath = "";
         LOGGER.debug( "No Output Directory Path specified, using current working directory "
                       + System.getProperty( "user.dir" ) );
         return;
      }
      final File outputDir = new File( outputDirectoryPath );
      if ( !outputDir.exists() ) {
         outputDir.mkdirs();
      }
      if ( !outputDir.isDirectory() ) {
         throw new IllegalArgumentException( outputDirectoryPath + " is not a valid directory path" );
      }
      _outputDirPath = outputDirectoryPath;
      LOGGER.debug( "Output Directory Path set to " + _outputDirPath );
   }

   /**
    * Process the jcas and write pretty sentences to file.  Filename is based upon the document id stored in the cas
    *
    * @param jcas ye olde ...
    */
   public void process( final JCas jcas ) {
      LOGGER.info( "Starting processing" );
      final String docId = DocumentIDAnnotationUtil.getDocumentIdForFile( jcas );
      File outputFile;
      if ( _outputDirPath == null || _outputDirPath.isEmpty() ) {
         outputFile = new File( docId + FILE_EXTENSION );
      } else {
         outputFile = new File( _outputDirPath, docId + FILE_EXTENSION );
      }
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) ) ) {
         final Collection<Sentence> sentences = JCasUtil.select( jcas, Sentence.class );
         for ( Sentence sentence : sentences ) {
            writeSentence( jcas, sentence, writer );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not not write pretty file " + outputFile.getPath() );
         LOGGER.error( ioE.getMessage() );
      }
      LOGGER.info( "Finished processing" );
   }

   /**
    * Write a sentence from the document text
    *
    * @param jcas     ye olde ...
    * @param sentence annotation containing the sentence
    * @param writer   writer to which pretty text for the sentence should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSentence( final JCas jcas,
                                      final AnnotationFS sentence,
                                      final BufferedWriter writer ) throws IOException {
      // Create the base row
      final Map<TextSpan, ItemCell> baseItemMap = createBaseItemMap( jcas, sentence );
      // Create covering annotations (item cells that cover more than one base cell)
      final Map<Integer, Collection<ItemCell>> coveringItemMap
            = createCoveringItemMap( jcas, sentence, baseItemMap );
      // Create annotation rows with shorter spans on top
      final Collection<ItemRow> itemRows = new ArrayList<>();
      final ItemRow baseItemRow = new DefaultItemRow();
      for ( ItemCell itemCell : baseItemMap.values() ) {
         baseItemRow.addItemCell( itemCell );
      }
      itemRows.add( baseItemRow );
      itemRows.addAll( createItemRows( coveringItemMap ) );
      // Create list of all text span offsets
      final Collection<Integer> offsets = new HashSet<>();
      for ( TextSpan textSpan : baseItemMap.keySet() ) {
         offsets.add( textSpan.getBegin() );
         offsets.add( textSpan.getEnd() );
      }
      // Create map of all text span offsets to adjusted offsets
      final Map<Integer, Integer> offsetAdjustedMap = createOffsetAdjustedMap( offsets, itemRows );
      // print all of the item rows
      printItemRows( offsetAdjustedMap, itemRows, writer );
   }

   /**
    * @param jcas     ye olde ...
    * @param sentence annotation containing the sentence
    * @return map of text spans and item cells that represent those spans
    */
   static private Map<TextSpan, ItemCell> createBaseItemMap( final JCas jcas, final AnnotationFS sentence ) {
      final int sentenceBegin = sentence.getBegin();
      final Collection<BaseToken> baseTokens
            = org.apache.uima.fit.util.JCasUtil.selectCovered( jcas, BaseToken.class, sentence );
      final Map<TextSpan, ItemCell> baseItemMap = new HashMap<>();
      for ( BaseToken baseToken : baseTokens ) {
         final TextSpan textSpan = new DefaultTextSpan( baseToken, sentenceBegin );
         if ( textSpan.getWidth() == 0 ) {
            continue;
         }
         if ( baseToken instanceof NewlineToken ) {
            final ItemCell itemCell = new DefaultBaseItemCell( textSpan, " ", "" );
            baseItemMap.put( textSpan, itemCell );
            continue;
         }
         final String tokenText = baseToken.getCoveredText();
         final String tokenPos = getTokenPos( baseToken );
         final ItemCell itemCell = new DefaultBaseItemCell( textSpan, tokenText, tokenPos );
         baseItemMap.put( textSpan, itemCell );
      }
      return baseItemMap;
   }

   /**
    * @param jcas        ye olde ...
    * @param sentence    annotation containing the sentence
    * @param baseItemMap map of text spans and item cells that represent those spans
    * @return map of covering annotations (item cells that cover more than one base cell)
    */
   static private Map<Integer, Collection<ItemCell>> createCoveringItemMap( final JCas jcas,
                                                                            final AnnotationFS sentence,
                                                                            final Map<TextSpan, ItemCell> baseItemMap ) {
      final int sentenceBegin = sentence.getBegin();
      final Collection<IdentifiedAnnotation> identifiedAnnotations
            = JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, sentence );
      final Map<Integer, Collection<ItemCell>> coveringAnnotationMap = new HashMap<>();
      for ( IdentifiedAnnotation identifiedAnnotation : identifiedAnnotations ) {
         final Map<String, Collection<String>> semanticCuis = getSemanticCuis( identifiedAnnotation );
         if ( semanticCuis.isEmpty() ) {
            continue;
         }
         final TextSpan textSpan = new DefaultTextSpan( identifiedAnnotation, sentenceBegin );
         if ( textSpan.getWidth() == 0 ) {
            continue;
         }
         final ItemCell itemCell = new DefaultUmlsItemCell( textSpan, identifiedAnnotation
               .getPolarity(), semanticCuis );
         final Collection<ItemCell> coveredBaseItems = getCoveredBaseItems( textSpan, baseItemMap );
         Collection<ItemCell> coveringAnnotations
               = coveringAnnotationMap.get( coveredBaseItems.size() );
         if ( coveringAnnotations == null ) {
            coveringAnnotations = new HashSet<>();
            coveringAnnotationMap.put( coveredBaseItems.size(), coveringAnnotations );
         }
         coveringAnnotations.add( itemCell );
      }
      return coveringAnnotationMap;
   }

   /**
    * @param offsets  original document offsets
    * @param itemRows item rows
    * @return map of original document offsets to adjusted printable offsets
    */
   static private Map<Integer, Integer> createOffsetAdjustedMap( final Collection<Integer> offsets,
                                                                 final Iterable<ItemRow> itemRows ) {
      // Create map of all text span offsets to adjusted offsets
      final List<Integer> offsetList = new ArrayList<>( offsets );
      Collections.sort( offsetList );
      final Map<Integer, Integer> offsetAdjustedMap = new HashMap<>( offsetList.size() );
      for ( Integer offset : offsetList ) {
         offsetAdjustedMap.put( offset, offset );
      }
      for ( ItemRow itemRow : itemRows ) {
         final Collection<ItemCell> rowItemCells = itemRow.getItemCells();
         for ( ItemCell itemCell : rowItemCells ) {
            final TextSpan textSpan = itemCell.getTextSpan();
            final int needWidth = itemCell.getWidth();
            final int nowWidth = offsetAdjustedMap.get( textSpan.getEnd() ) -
                                 offsetAdjustedMap.get( textSpan.getBegin() );
            if ( needWidth > nowWidth ) {
               final int delta = needWidth - nowWidth;
               for ( Integer originalOffset : offsetList ) {
                  if ( originalOffset >= textSpan.getEnd() ) {
                     final Integer oldAdjustedOffset = offsetAdjustedMap.get( originalOffset );
                     offsetAdjustedMap.put( originalOffset, oldAdjustedOffset + delta );
                  }
               }
            }
         }
      }
      return offsetAdjustedMap;
   }

   /**
    * @param offsetAdjustedMap map of original document offsets to adjusted printable offsets
    * @param itemRows          item rows
    * @param writer            writer to which pretty text for the sentence should be written
    * @throws IOException if the writer has issues
    */
   static private void printItemRows( final Map<Integer, Integer> offsetAdjustedMap,
                                      final Iterable<ItemRow> itemRows,
                                      final BufferedWriter writer ) throws IOException {
      int rowWidth = 0;
      for ( int adjustedOffset : offsetAdjustedMap.values() ) {
         rowWidth = Math.max( rowWidth, adjustedOffset );
      }
      // Write Sentence Rows
      boolean firstLine = true;
      for ( ItemRow itemRow : itemRows ) {
         final int rowHeight = itemRow.getHeight();
         for ( int i = 0; i < rowHeight; i++ ) {
            final String lineText = itemRow.getTextLine( i, rowWidth, offsetAdjustedMap );
            if ( !lineText.isEmpty() ) {
               if ( firstLine ) {
                  writer.write( "TEXT:  " + lineText );
                  firstLine = false;
               } else {
                  writer.write( "       " + lineText );

               }
               writer.newLine();
            }
         }
      }
      writer.newLine();
   }


   /**
    * @param textSpan    text span of interest
    * @param baseItemMap map of text spans and item cells that represent those spans
    * @return item cells for covered base items
    */
   static private Collection<ItemCell> getCoveredBaseItems( final TextSpan textSpan,
                                                            final Map<TextSpan, ItemCell> baseItemMap ) {
      final Collection<ItemCell> coveredBaseItems = new ArrayList<>();
      for ( Map.Entry<TextSpan, ItemCell> baseItemEntry : baseItemMap.entrySet() ) {
         if ( baseItemEntry.getKey().overlaps( textSpan ) ) {
            coveredBaseItems.add( baseItemEntry.getValue() );
         }
      }
      return coveredBaseItems;
   }


   /**
    * Create annotation rows with shorter spans on top
    *
    * @param coveringItemMap map of all item cells for the sentence,
    *                        key = number of tokens covered, value = item cells
    * @return list of item rows, each containing non-overlapping item cells
    */
   static private Collection<ItemRow> createItemRows( final Map<Integer, Collection<ItemCell>> coveringItemMap ) {
      final List<Integer> sortedCounts = new ArrayList<>( coveringItemMap.keySet() );
      Collections.sort( sortedCounts );
      final Collection<ItemRow> itemRows = new ArrayList<>();
      for ( Integer coveredCount : sortedCounts ) {
         final Collection<ItemCell> itemCells = coveringItemMap.get( coveredCount );
         for ( ItemCell itemCell : itemCells ) {
            boolean added = false;
            for ( ItemRow itemRow : itemRows ) {
               added = itemRow.addItemCell( itemCell );
               if ( added ) {
                  break;
               }
            }
            if ( !added ) {
               final ItemRow itemRow = new DefaultItemRow();
               itemRow.addItemCell( itemCell );
               itemRows.add( itemRow );
            }
         }
      }
      return itemRows;
   }

   /**
    * @param baseToken some token
    * @return a part of speech text representation if the basetoken is a word token, else ""
    */
   static private String getTokenPos( final BaseToken baseToken ) {
      if ( !(baseToken instanceof WordToken) ) {
         return "";
      }
      // We are only interested in tokens that are -words-
      final String tokenPos = baseToken.getPartOfSpeech();
      if ( tokenPos == null ) {
         return "";
      }
      return tokenPos;
   }


   /**
    * @param identifiedAnnotation an annotation of interest
    * @return map of semantic type names and cuis within those types as they apply to the annotation
    */
   static private Map<String, Collection<String>> getSemanticCuis( final IdentifiedAnnotation identifiedAnnotation ) {
      final Collection<UmlsConcept> umlsConcepts = IdentifiedAnnotationUtil.getUmlsConcepts( identifiedAnnotation );
      if ( umlsConcepts == null || umlsConcepts.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<String, Collection<String>> semanticCuis = new HashMap<>();
      for ( UmlsConcept umlsConcept : umlsConcepts ) {
         final String cui = umlsConcept.getCui();
         final String tui = umlsConcept.getTui();
         final String semanticName = SemanticGroup.getSemanticName( tui );
         Collection<String> cuis = semanticCuis.get( semanticName );
         if ( cuis == null ) {
            cuis = new HashSet<>();
            semanticCuis.put( semanticName, cuis );
         }
         cuis.add( cui );
      }
      return semanticCuis;
   }

}
