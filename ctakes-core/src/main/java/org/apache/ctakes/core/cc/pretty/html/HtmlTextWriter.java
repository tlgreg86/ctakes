package org.apache.ctakes.core.cc.pretty.html;


import org.apache.ctakes.core.cc.AbstractOutputFileWriter;
import org.apache.ctakes.core.cc.pretty.SemanticGroup;
import org.apache.ctakes.core.cc.pretty.textspan.DefaultTextSpan;
import org.apache.ctakes.core.cc.pretty.textspan.TextSpan;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.ListEntry;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.cc.pretty.SemanticGroup.UNKNOWN_SEMANTIC_CODE;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/8/2016
 */
@PipeBitInfo(
      name = "HTML Writer",
      description = "Writes html files with document text and simple markups (Semantic Group, CUI, Negation).",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, SENTENCE, BASE_TOKEN },
      usables = { DOCUMENT_ID_PREFIX, IDENTIFIED_ANNOTATION, EVENT, TIMEX, TEMPORAL_RELATION }
)
final public class HtmlTextWriter extends AbstractOutputFileWriter {

   // TODO https://www.w3schools.com/howto/howto_css_switch.asp
   // TODO https://www.w3schools.com/html/tryit.asp?filename=tryhtml_layout_flexbox
   // TODO https://www.w3schools.com/html/html5_new_elements.asp



   static final String TOOL_TIP = "TIP";

   static final String UNCERTAIN_NEGATED = "UNN_";
   static final String NEGATED = "NEG_";
   static final String UNCERTAIN = "UNC_";
   static final String AFFIRMED = "AFF_";
   static final String GENERIC = "GNR_";
   static private final String SPACER = "SPC_";
   static private final String NEWLINE = "NL_";

   static private final Logger LOGGER = Logger.getLogger( "HtmlTextWriter" );

   static private final String PREFERRED_TERM_UNKNOWN = "Unknown Preferred Term";


   static private final String FILE_EXTENSION = ".pretty.html";
   static private final String CSS_FILENAME = "ctakes.pretty.css";

   private final Collection<String> _usedDirectories = new HashSet<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      if ( _usedDirectories.add( outputDir ) ) {
         final String cssPath = outputDir + '/' + CSS_FILENAME;
         CssWriter.writeCssFile( cssPath );
      }

      final File htmlFile = new File( outputDir, fileName + FILE_EXTENSION );
      LOGGER.info( "Writing HTML to " + htmlFile.getPath() + " ..." );
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( htmlFile ) ) ) {
         final String title = DocumentIDAnnotationUtil.getDocumentID( jCas );
         writer.write( getHeader( title ) );
         writer.write( getCssLink( CSS_FILENAME ) );
         writeTitle( title, writer );
         final Collection<Segment> sections = JCasUtil.select( jCas, Segment.class );
         final Map<Segment, Collection<org.apache.ctakes.typesystem.type.textspan.List>> lists
               = JCasUtil.indexCovered( jCas, Segment.class, org.apache.ctakes.typesystem.type.textspan.List.class );
         final Map<org.apache.ctakes.typesystem.type.textspan.List, Collection<ListEntry>> listEntries
               = JCasUtil.indexCovered( jCas, org.apache.ctakes.typesystem.type.textspan.List.class, ListEntry.class );
         final Map<Segment, Collection<Sentence>> sectionSentences
               = JCasUtil.indexCovered( jCas, Segment.class, Sentence.class );
         final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations
               = JCasUtil.indexCovered( jCas, Sentence.class, IdentifiedAnnotation.class );
         final Map<Sentence, Collection<BaseToken>> sentenceTokens
               = JCasUtil.indexCovered( jCas, Sentence.class, BaseToken.class );
         final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
         final Collection<CollectionTextRelation> corefRelations = JCasUtil.select( jCas, CollectionTextRelation.class );

         final Map<Integer, Collection<Integer>> corefEnds = createCorefEnds( corefRelations );

         writeSections( sections, lists, listEntries, sectionSentences, sentenceAnnotations, sentenceTokens, relations, corefEnds, writer );
         writeInfoPane( writer );
         writer.write( startJavascript() );
         writer.write( getSwapInfoScript() );
         if ( !corefRelations.isEmpty() ) {
            writeCorefInfos( corefRelations, writer );
         }
         writer.write( endJavascript() );
         writer.write( getFooter() );
      }
      LOGGER.info( "Finished Writing" );
   }

   /**
    * @param corefRelations coreference chains
    * @return a map of markable text span ends to chain numbers
    */
   static private Map<Integer, Collection<Integer>> createCorefEnds( final Collection<CollectionTextRelation> corefRelations ) {
      if ( corefRelations == null || corefRelations.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<Integer, Collection<Integer>> corefEnds = new HashMap<>();
      int index = 1;
      for ( CollectionTextRelation corefRelation : corefRelations ) {
         final FSList chainHead = corefRelation.getMembers();
         final Collection<IdentifiedAnnotation> markables
               = FSCollectionFactory.create( chainHead, IdentifiedAnnotation.class );
         for ( IdentifiedAnnotation markable : markables ) {
            corefEnds.putIfAbsent( markable.getEnd(), new ArrayList<>() );
            corefEnds.get( markable.getEnd() ).add( index );
         }
         index++;
      }
      return corefEnds;
   }

   /**
    * @param title normally the document title
    * @return html to set the header
    */
   static private String getHeader( final String title ) {
      return "<!DOCTYPE html>\n<html>\n<head>\n  <title>" + getSafeText( title ) + " Output</title>\n</head>\n<body>\n";
   }

   /**
    * @param filePath path to the css file
    * @return html to link to css
    */
   static private String getCssLink( final String filePath ) {
      return "<link rel=\"stylesheet\" href=\"" + filePath + "\" type=\"text/css\" media=\"screen\">";
   }

   /**
    * Write html for document title and job completion time for document
    *
    * @param title  normally document title, such as filename
    * @param writer writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeTitle( final String title, final BufferedWriter writer ) throws IOException {
      if ( !title.isEmpty() ) {
         writer.write( "\n<h2>" + getSafeText( title ) + "</h2>\n " );
      }
      final LocalDateTime time = LocalDateTime.now();
      final DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "L dd yyyy, HH:mm:ss" );
      writer.write( "<i>Text processing finished on: " + formatter.format( time ) + "</i>\n<hr>\n" );
   }

   /**
    * write html for all sections (all text) in the document
    *
    * @param sectionSentences    map of sections and their contained sentences
    * @param sentenceAnnotations map of sentences and their contained annotations
    * @param sentenceTokens      map of sentences and their contained base tokens
    * @param relations           all relations
    * @param corefEnds           map of text span ends to coreference chain indices
    * @param writer              writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSections( final Map<Segment, Collection<Sentence>> sectionSentences,
                                      final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations,
                                      final Map<Sentence, Collection<BaseToken>> sentenceTokens,
                                      final Collection<BinaryTextRelation> relations,
                                      final Map<Integer, Collection<Integer>> corefEnds,
                                      final BufferedWriter writer ) throws IOException {
      writer.write( "\n<div id=\"content\">\n" );
      final List<Segment> sections = new ArrayList<>( sectionSentences.keySet() );
      sections.sort( Comparator.comparingInt( Segment::getBegin ) );
      for ( Segment section : sections ) {
         writeSectionHeader( section, writer );
         writer.write( "\n<p>\n" );
         final List<Sentence> sentences = new ArrayList<>( sectionSentences.get( section ) );
         sentences.sort( Comparator.comparingInt( Sentence::getBegin ) );
         for ( Sentence sentence : sentences ) {
            final Collection<IdentifiedAnnotation> annotations = sentenceAnnotations.get( sentence );
            final Collection<BaseToken> tokens = sentenceTokens.get( sentence );
            writeSentence( sentence, annotations, tokens, relations, corefEnds, writer );
         }
         writer.write( "\n</p>\n" );
      }
      writer.write( "\n</div>\n" );
   }

   /**
    * write html for all sections (all text) in the document
    *
    * @param sectionSentences    map of sections and their contained sentences
    * @param sentenceAnnotations map of sentences and their contained annotations
    * @param sentenceTokens      map of sentences and their contained base tokens
    * @param relations           all relations
    * @param corefEnds           map of text span ends to coreference chain indices
    * @param writer              writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSections( final Collection<Segment> sectionSet,
                                      final Map<Segment, Collection<org.apache.ctakes.typesystem.type.textspan.List>> lists,
                                      final Map<org.apache.ctakes.typesystem.type.textspan.List, Collection<ListEntry>> listEntries,
                                      final Map<Segment, Collection<Sentence>> sectionSentences,
                                      final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations,
                                      final Map<Sentence, Collection<BaseToken>> sentenceTokens,
                                      final Collection<BinaryTextRelation> relations,
                                      final Map<Integer, Collection<Integer>> corefEnds,
                                      final BufferedWriter writer ) throws IOException {
      if ( lists.isEmpty() ) {
         writeSections( sectionSentences, sentenceAnnotations, sentenceTokens, relations, corefEnds, writer );
         return;
      }
      writer.write( "\n<div id=\"content\">\n" );
      final List<Segment> sections = new ArrayList<>( sectionSet );
      sections.sort( Comparator.comparingInt( Segment::getBegin ) );
      final Map<Integer, Integer> enclosers = new HashMap<>();
      for ( Map.Entry<org.apache.ctakes.typesystem.type.textspan.List, Collection<ListEntry>> entry : listEntries.entrySet() ) {
         final int listEnd = entry.getKey().getEnd();
         entry.getValue().forEach( e -> enclosers.put( e.getBegin(), listEnd ) );
      }
      for ( Segment section : sections ) {
         writeSectionHeader( section, writer );
         final Collection<Sentence> sentenceSet = sectionSentences.get( section );
         if ( sentenceSet == null ) {
            continue;
         }
         writer.write( "\n<p>\n" );
         final List<Sentence> sentences = new ArrayList<>( sentenceSet );
         sentences.sort( Comparator.comparingInt( Sentence::getBegin ) );
         int currentEnd = -1;
         boolean freshEntry = false;
         for ( Sentence sentence : sentences ) {
            final Collection<IdentifiedAnnotation> annotations = sentenceAnnotations.get( sentence );
            final Collection<BaseToken> tokens = sentenceTokens.get( sentence );
            final Integer end = enclosers.get( sentence.getBegin() );
            if ( end != null ) {
               freshEntry = true;
               if ( currentEnd < 0 ) {
                  startList( sentence, annotations, tokens, relations, corefEnds, writer );
                  currentEnd = end;
               } else {
                  writeListEntry( sentence, annotations, tokens, relations, corefEnds, writer );
               }
            } else {
               if ( currentEnd >= 0 && sentence.getBegin() > currentEnd ) {
                  endList( sentence, annotations, tokens, relations, corefEnds, writer );
                  currentEnd = -1;
                  freshEntry = false;
                  continue;
               }
               if ( freshEntry ) {
                  freshEntry = false;
                  writer.write( "\n<br>\n" );
               }
               writeSentence( sentence, annotations, tokens, relations, corefEnds, writer );
            }
         }
         if ( currentEnd >= 0 ) {
            endList( writer );
         }
         writer.write( "\n</p>\n" );
      }
      writer.write( "\n</div>\n" );
   }

   /**
    *
    * @param sentence    sentence of interest
    * @param annotations identified annotations in the section
    * @param baseTokenMap  baseTokens in the section
    * @param relations   all relations
    * @param corefEnds           map of text span ends to coreference chain indices
    * @return marked up text
    */
   static private String createLineText( final Sentence sentence,
                                         final Collection<IdentifiedAnnotation> annotations,
                                         final Map<TextSpan, String> baseTokenMap,
                                         final Collection<BinaryTextRelation> relations,
                                         final Map<Integer, Collection<Integer>> corefEnds ) {
      final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap = createAnnotationMap( sentence, annotations );
      final Map<Integer, String> tags = createTags( sentence.getBegin(), annotationMap, relations, corefEnds );
      final StringBuilder sb = new StringBuilder();
      int previousIndex = -1;
      for ( Map.Entry<TextSpan, String> entry : baseTokenMap.entrySet() ) {
         final String text = entry.getValue();
         final int begin = entry.getKey().getBegin();
         if ( begin != previousIndex ) {
            final String beginTag = tags.get( begin );
            if ( beginTag != null ) {
               sb.append( beginTag );
            }
         }
         sb.append( text );
         final int end = entry.getKey().getEnd();
         final String endTag = tags.get( end );
         if ( endTag != null ) {
            sb.append( endTag );
         }
         sb.append( " " );
         previousIndex = end;
      }
      return sb.toString();
   }

   static private void startList( final Sentence sentence,
                                  final Collection<IdentifiedAnnotation> annotations,
                                  final Collection<BaseToken> baseTokens,
                                  final Collection<BinaryTextRelation> relations,
                                  final Map<Integer, Collection<Integer>> corefEnds,
                                  final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      writer.write( "\n<ul>\n<li>" );
      final String lineText = createLineText( sentence, annotations, baseTokenMap, relations, corefEnds );
      writer.write( lineText );
   }

   /**
    * Write html for a sentence from the document text
    *
    * @param sentence    sentence of interest
    * @param annotations identified annotations in the section
    * @param baseTokens  baseTokens in the section
    * @param relations   all relations
    * @param writer      writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeListEntry( final Sentence sentence,
                                       final Collection<IdentifiedAnnotation> annotations,
                                       final Collection<BaseToken> baseTokens,
                                       final Collection<BinaryTextRelation> relations,
                                       final Map<Integer, Collection<Integer>> corefEnds,
                                       final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      writer.write( "</li>\n<li>" );
      final String lineText = createLineText( sentence, annotations, baseTokenMap, relations, corefEnds );
      writer.write( lineText );
   }

   static private void endList( final Sentence sentence,
                                final Collection<IdentifiedAnnotation> annotations,
                                final Collection<BaseToken> baseTokens,
                                final Collection<BinaryTextRelation> relations,
                                final Map<Integer, Collection<Integer>> corefEnds,
                                final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      final String lineText = createLineText( sentence, annotations, baseTokenMap, relations, corefEnds );
      writer.write( lineText + "</li>\n</ul>\n" );
   }

   static private void endList( final BufferedWriter writer ) throws IOException {
      writer.write( "</li>\n</ul>\n" );
   }

   /**
    * write html for section header
    *
    * @param section -
    * @param writer  writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSectionHeader( final Segment section, final BufferedWriter writer ) throws IOException {
      String sectionId = section.getId();
      if ( sectionId.equals( "SIMPLE_SEGMENT" ) ) {
         return;
      }
      final StringBuilder sb = new StringBuilder();
      sb.append( "\n<h3" );
      final String sectionTag = getSafeText( section.getTagText() );
      if ( sectionTag != null && !sectionTag.trim().isEmpty() ) {
         sb.append( " onClick=\"iaf(\'" ).append( sectionTag.trim() ).append( "')\"" );
      }
      sb.append( ">" ).append( getSafeText( sectionId ) );
      final String sectionName = section.getPreferredText();
      if ( sectionName != null && !sectionName.trim().isEmpty() && !sectionName.trim().equals( sectionId ) ) {
         sb.append( " : " ).append( getSafeText( sectionName ) );
      }
      sb.append( "</h3>\n" );
      writer.write( sb.toString() );
   }


   /**
    * Write html for a sentence from the document text
    *
    * @param sentence    sentence of interest
    * @param annotations identified annotations in the section
    * @param baseTokens  baseTokens in the section
    * @param relations all relations
    * @param corefEnds           map of text span ends to coreference chain indices
    * @param writer      writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSentence( final Sentence sentence,
                                      final Collection<IdentifiedAnnotation> annotations,
                                      final Collection<BaseToken> baseTokens,
                                      final Collection<BinaryTextRelation> relations,
                                      final Map<Integer, Collection<Integer>> corefEnds,
                                      final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      final String lineText = createLineText( sentence, annotations, baseTokenMap, relations, corefEnds );
      writer.write( lineText + "\n<br>\n" );
   }

   /**
    * removes empty spans and replaces non-html compatible characters with their html ok equivalents
    *
    * @param sentence   -
    * @param baseTokens in the sentence
    * @return a map of text spans and their contained text
    */
   static private Map<TextSpan, String> createBaseTokenMap( final Sentence sentence,
                                                            final Collection<BaseToken> baseTokens ) {
      final int sentenceBegin = sentence.getBegin();
      final Map<TextSpan, String> baseItemMap = new LinkedHashMap<>();
      for ( BaseToken baseToken : baseTokens ) {
         final TextSpan textSpan = new DefaultTextSpan( baseToken, sentenceBegin );
         if ( textSpan.getWidth() == 0 ) {
            continue;
         }
         String text = getSafeText( baseToken );
         if ( text.isEmpty() ) {
            continue;
         }
         baseItemMap.put( textSpan, text );
      }
      return baseItemMap;
   }

   static private String getSafeText( final Annotation annotation ) {
      if ( annotation == null ) {
         return "";
      }
      return getSafeText( annotation.getCoveredText().trim() );
   }

   static private String getSafeText( final String text ) {
      if ( text.isEmpty() ) {
         return "";
      }
      String safeText = text.replaceAll( "'", "&apos;" );
      safeText = safeText.replaceAll( "\"", "&quot;" );
      safeText = safeText.replaceAll( "@", "&amp;" );
      safeText = safeText.replaceAll( "<", "&lt;" );
      safeText = safeText.replaceAll( ">", "&gt;" );
      return safeText;
   }

   /**
    * @param sentence    -
    * @param annotations annotations within the sentence
    * @return map of text spans and all annotations within those spans.  Accounts for overlap, etc.
    */
   static private Map<TextSpan, Collection<IdentifiedAnnotation>> createAnnotationMap( final Sentence sentence,
                                                                                       final Collection<IdentifiedAnnotation> annotations ) {
      final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap = new HashMap<>();
      final int sentenceBegin = sentence.getBegin();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final TextSpan textSpan = new DefaultTextSpan( annotation, sentenceBegin );
         if ( textSpan.getWidth() == 0 ) {
            continue;
         }
         final Collection<String> semanticNames = SemanticGroup.getSemanticNames( annotation );
         if ( !semanticNames.isEmpty() ) {
            annotationMap.putIfAbsent( textSpan, new ArrayList<>() );
            annotationMap.get( textSpan ).add( annotation );
         }
      }
      return annotationMap;
   }

   /**
    * sorts by begins, then by ends if begins are equal
    */
   static private class TextSpanComparator implements Comparator<TextSpan> {
      public int compare( final TextSpan t1, final TextSpan t2 ) {
         int r = t1.getBegin() - t2.getBegin();
         if ( r != 0 ) {
            return r;
         }
         return t1.getEnd() - t2.getEnd();
      }
   }

   static private final Comparator<TextSpan> TEXT_SPAN_COMPARATOR = new TextSpanComparator();

   /**
    * Creates map of text span indices and whether each span represents the beginning of one or more annotations,
    * the inside of two or more overlapping annotations, or the end of two or more overlapping annotations
    *
    * @param textSpans -
    * @return B I E map
    */
   static private Map<Integer, Character> createIndexMap( final Collection<TextSpan> textSpans ) {
      if ( textSpans.isEmpty() ) {
         return Collections.emptyMap();
      }
      final List<TextSpan> spanList = new ArrayList<>( textSpans );
      spanList.sort( TEXT_SPAN_COMPARATOR );
      final int spanCount = spanList.size();
      final int spanCountMinus = spanCount - 1;
      final Map<Integer, Character> indexMap = new HashMap<>();
      for ( int i = 0; i < spanCountMinus; i++ ) {
         final TextSpan textSpan = spanList.get( i );
         final int begin = textSpan.getBegin();
         indexMap.putIfAbsent( begin, 'B' );
         final int end = textSpan.getEnd();
         indexMap.putIfAbsent( end, 'E' );
         for ( int j = i + 1; j < spanCount; j++ ) {
            TextSpan nextSpan = spanList.get( j );
            if ( nextSpan.getBegin() > end ) {
               break;
            }
            if ( nextSpan.getBegin() > begin ) {
               indexMap.put( nextSpan.getBegin(), 'I' );
            }
            if ( nextSpan.getEnd() < end ) {
               indexMap.put( nextSpan.getEnd(), 'I' );
            } else if ( nextSpan.getEnd() > end ) {
               indexMap.put( end, 'I' );
            }
         }
      }
      final TextSpan lastSpan = spanList.get( spanCountMinus );
      indexMap.putIfAbsent( lastSpan.getBegin(), 'B' );
      indexMap.putIfAbsent( lastSpan.getEnd(), 'E' );
      return indexMap;
   }

   /**
    * @param indexMap map of text span indices and the B I E status of the spans
    * @return new spans representing the smallest required unique span elements of overlapping spans
    */
   static private Collection<TextSpan> createAdjustedSpans( final Map<Integer, Character> indexMap ) {
      if ( indexMap.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Integer> indexList = new ArrayList<>( indexMap.keySet() );
      Collections.sort( indexList );
      final int indexCount = indexList.size();
      final Collection<TextSpan> newSpans = new ArrayList<>();
      Integer index1 = indexList.get( 0 );
      Character c1 = indexMap.get( index1 );
      for ( int i = 1; i < indexCount; i++ ) {
         final Integer index2 = indexList.get( i );
         final Character c2 = indexMap.get( index2 );
         if ( c1.equals( 'B' ) || c1.equals( 'I' ) ) {
            newSpans.add( new DefaultTextSpan( index1, index2 ) );
         }
         index1 = index2;
         c1 = c2;
      }
      return newSpans;
   }

   /**
    * @param adjustedList  spans representing the smallest required unique span elements of overlapping spans
    * @param annotationMap map of larger overlapping text spans and their annotations
    * @return map of all annotations within or overlapping the small span elements
    */
   static private Map<TextSpan, Collection<IdentifiedAnnotation>> createAdjustedAnnotations(
         final List<TextSpan> adjustedList, final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap ) {
      final List<TextSpan> spanList = new ArrayList<>( annotationMap.keySet() );
      spanList.sort( TEXT_SPAN_COMPARATOR );
      final Map<TextSpan, Collection<IdentifiedAnnotation>> spanAnnotations = new HashMap<>( adjustedList.size() );
      final int spanCount = spanList.size();
      int previousMatchIndex = 0;
      for ( TextSpan adjusted : adjustedList ) {
         boolean matched = false;
         for ( int i = previousMatchIndex; i < spanCount; i++ ) {
            final TextSpan annotationsSpan = spanList.get( i );
            if ( annotationsSpan.overlaps( adjusted ) ) {
               if ( !matched ) {
                  previousMatchIndex = i;
                  matched = true;
               }
               spanAnnotations.putIfAbsent( adjusted, new HashSet<>() );
               spanAnnotations.get( adjusted ).addAll( annotationMap.get( annotationsSpan ) );
            }
         }
      }
      return spanAnnotations;
   }

   /**
    * @param sentenceBegin begin offset of sentence
    * @param annotationMap map of all annotations within or overlapping the small span elements
    * @param relations all relations
    * @param corefEnds           map of text span ends to coreference chain indices
    * @return html for span elements
    */
   static private Map<Integer, String> createTags( final int sentenceBegin,
                                                   final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap,
                                                   final Collection<BinaryTextRelation> relations,
                                                   final Map<Integer, Collection<Integer>> corefEnds ) {
      if ( annotationMap.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<Integer, Character> indexMap = createIndexMap( annotationMap.keySet() );
      final Collection<TextSpan> adjustedSpans = createAdjustedSpans( indexMap );
      final List<TextSpan> adjustedList = new ArrayList<>( adjustedSpans );
      adjustedList.sort( TEXT_SPAN_COMPARATOR );
      final Map<TextSpan, Collection<IdentifiedAnnotation>> adjustedAnnotations
            = createAdjustedAnnotations( adjustedList, annotationMap );

      final Map<Integer, String> indexTags = new HashMap<>();
      for ( TextSpan adjustedSpan : adjustedList ) {
         final StringBuilder sb = new StringBuilder( "<span" );
         final Collection<IdentifiedAnnotation> annotations = adjustedAnnotations.get( adjustedSpan );
         if ( annotations.isEmpty() ) {
            continue;
         }
         final String polarityClasses = createPolaritiesText( annotations );
         if ( !polarityClasses.isEmpty() ) {
            sb.append( " class=\"" ).append( polarityClasses ).append( '\"' );
         }
         final String clickInfo = createClickInfo( annotations, relations );
         if ( !clickInfo.isEmpty() ) {
            sb.append( " onClick=\"iaf(\'" ).append( clickInfo ).append( "\')\"" );
         }
         final String tip = createTipText( annotations );
         if ( !tip.isEmpty() ) {
            sb.append( " " + TOOL_TIP + "=\"" ).append( tip ).append( '\"' );
         }
         sb.append( '>' );

         // coref chain
         final StringBuilder sb2 = new StringBuilder();
         final Collection<IdentifiedAnnotation> endAnnotations = annotations.stream()
               .filter( a -> a.getEnd() == (sentenceBegin + adjustedSpan.getEnd()) )
               .collect( Collectors.toSet() );
         final Collection<String> semanticCodes = SemanticGroup.getSemanticCodes( endAnnotations );
         final Collection<Integer> chains = corefEnds.get( sentenceBegin + adjustedSpan.getEnd() );
         if ( chains != null && !chains.isEmpty() ) {
            String semantic = semanticCodes.stream().findAny().orElse( UNKNOWN_SEMANTIC_CODE );
            if ( endAnnotations.size() != annotations.size() ) {
               semantic += " " + polarityClasses;
            }
            for ( Integer chain : chains ) {
               sb2.append( "<span class=\"" ).append( semantic ).append( "\"" );
               sb2.append( " onClick=\"crf" ).append( chain ).append( "()\">" );
               sb2.append( "<sup>" ).append( chain ).append( "</sup></span>" );
            }
         } else {
            for ( String semantic : semanticCodes ) {
               sb2.append( "<span class=\"" ).append( semantic );
               if ( endAnnotations.size() != annotations.size() ) {
                  sb2.append( " " ).append( polarityClasses );
               }
               sb2.append( "\"><sup>" ).append( "&bull;" ).append( "</sup></span>" );
            }
         }

         final Integer begin = adjustedSpan.getBegin();
         final String previousTag = indexTags.getOrDefault( begin, "" );
         indexTags.put( begin, previousTag + sb.toString() );
         indexTags.put( adjustedSpan.getEnd(), "</span>" + sb2.toString() );
      }
      return indexTags;
   }

   /**
    * @param annotations -
    * @return html with annotation information: polarity, semantic, cui, text, pref text
    */
   static private String createClickInfo( final Collection<IdentifiedAnnotation> annotations,
                                          final Collection<BinaryTextRelation> relations ) {
      final Map<String, Map<String, Collection<String>>> polarInfoMap = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final String polarity = createPolarity( annotation );
         polarInfoMap.putIfAbsent( polarity, new HashMap<>() );
         final Map<String, Collection<String>> infoMap = createInfoMap( annotation, relations );
         for ( Map.Entry<String, Collection<String>> infoEntry : infoMap.entrySet() ) {
            polarInfoMap.get( polarity ).putIfAbsent( infoEntry.getKey(), new HashSet<>() );
            polarInfoMap.get( polarity ).get( infoEntry.getKey() ).addAll( infoEntry.getValue() );
         }
      }
      final List<String> polarities = new ArrayList<>( polarInfoMap.keySet() );
      Collections.sort( polarities );
      final StringBuilder sb = new StringBuilder();
      for ( String polarity : polarities ) {
         sb.append( polarity ).append( NEWLINE );
         final Map<String, Collection<String>> infoMap = polarInfoMap.get( polarity );
         final List<String> semantics = new ArrayList<>( infoMap.keySet() );
         Collections.sort( semantics );
         for ( String semantic : semantics ) {
            sb.append( semantic ).append( NEWLINE );
            final List<String> texts = new ArrayList<>( infoMap.get( semantic ) );
            Collections.sort( texts );
            for ( String text : texts ) {
               sb.append( text ).append( NEWLINE );
            }
         }
      }
      return sb.toString();
   }

   /**
    * @param annotation -
    * @return map of semantic to text for annotations
    */
   static private Map<String, Collection<String>> createInfoMap( final IdentifiedAnnotation annotation,
                                                                 final Collection<BinaryTextRelation> relations ) {
      final Collection<UmlsConcept> concepts = OntologyConceptUtil.getUmlsConcepts( annotation );
      final Map<String, Collection<String>> semanticMap = new HashMap<>();
      final String coveredText = getCoveredText( annotation );
      final String safeText = getSafeText( coveredText );
      final String relationText = getRelationText( annotation, relations );
      for ( UmlsConcept concept : concepts ) {
         final String semanticCode = SemanticGroup.getSemanticCode( concept );
         semanticMap.putIfAbsent( semanticCode, new HashSet<>() );
         String text = safeText + NEWLINE + getCodes( concept ) + getPreferredText( coveredText, concept ) + relationText;
         if ( annotation instanceof EventMention ) {
            text += getDocTimeRel( (EventMention) annotation );
         }
         semanticMap.get( semanticCode ).add( text );
      }
      if ( concepts.isEmpty() ) {
         String semanticCode = "";
         String postText = "";
         if ( annotation instanceof EventMention ) {
            semanticCode = SemanticGroup.EVENT_CODE;
            postText = getDocTimeRel( (EventMention) annotation );
         } else if ( annotation instanceof TimeMention ) {
            semanticCode = SemanticGroup.TIMEX_CODE;
         }
         if ( !semanticCode.isEmpty() ) {
            semanticMap.putIfAbsent( semanticCode, new HashSet<>() );
            semanticMap.get( semanticCode ).add( safeText + NEWLINE + postText + relationText );
         }
      }
      return semanticMap;
   }

   /**
    * @param concept -
    * @return cui if it exists and any codes if they exist
    */
   static private String getCodes( final UmlsConcept concept ) {
      String codes = "";
      final String cui = concept.getCui();
      if ( cui != null && !cui.isEmpty() ) {
         codes += SPACER + cui + NEWLINE;
      }
      final String code = concept.getCode();
      if ( code != null && !code.isEmpty() ) {
         codes += SPACER + code + NEWLINE;
      }
      return codes;
   }

   /**
    * @param annotation -
    * @return the covered text
    */
   static private String getCoveredText( final IdentifiedAnnotation annotation ) {
      return annotation.getCoveredText().replace( '\r', ' ' ).replace( '\n', ' ' );
   }

   /**
    * @param coveredText -
    * @param concept     -
    * @return the covered text plus preferred text if it exists and is not equal to the covered text
    */
   static private String getPreferredText( final String coveredText, final UmlsConcept concept ) {
      final String preferredText = concept.getPreferredText();
      if ( preferredText != null && !preferredText.isEmpty()
            && !preferredText.equals( PREFERRED_TERM_UNKNOWN )
            && !preferredText.equalsIgnoreCase( coveredText )
            && !preferredText.equalsIgnoreCase( coveredText + 's' )
            && !coveredText.equalsIgnoreCase( preferredText + 's' ) ) {
         return SPACER + "[" + getSafeText( preferredText ) + "]" + NEWLINE;
      }
      return "";
   }


   /**
    * @param eventMention -
    * @return a line of text with doctimerel if available
    */
   static private String getDocTimeRel( final EventMention eventMention ) {
      final Event event = eventMention.getEvent();
      if ( event == null ) {
         return "";
      }
      final EventProperties eventProperties = event.getProperties();
      if ( eventProperties == null ) {
         return "";
      }
      final String dtr = eventProperties.getDocTimeRel();
      if ( dtr == null || dtr.isEmpty() ) {
         return "";
      }
      return SPACER + "[" + dtr.toLowerCase() + "] doc time" + NEWLINE;
   }

   /**
    * @param annotations -
    * @return polarity representation for all provided annotations
    */
   static private String createPolaritiesText( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
            .map( HtmlTextWriter::createPolarity )
            .distinct()
            .sorted()
            .collect( Collectors.joining( " " ) );
   }

   /**
    * @param annotation -
    * @return polarity for a single annotation
    */
   static private String createPolarity( final IdentifiedAnnotation annotation ) {
      if ( annotation instanceof TimeMention ) {
         return GENERIC;
      }
      if ( annotation.getPolarity() < 0 ) {
         if ( annotation.getUncertainty() > 0 ) {
            return UNCERTAIN_NEGATED;
         } else {
            return NEGATED;
         }
      } else if ( annotation.getUncertainty() > 0 ) {
         return UNCERTAIN;
      } else {
         return AFFIRMED;
      }
   }

   /**
    * @param annotations -
    * @return tooltip text with semantic names for given annotations
    */
   static private String createTipText( final Collection<IdentifiedAnnotation> annotations ) {
      final Map<String, Integer> semanticCounts = getSemanticCounts( annotations );
      final List<String> semantics = new ArrayList<>( semanticCounts.keySet() );
      Collections.sort( semantics );
      final StringBuilder sb = new StringBuilder();
      for ( String semanticName : semantics ) {
         sb.append( semanticName );
         final int count = semanticCounts.get( semanticName );
         if ( count > 1 ) {
            sb.append( '(' ).append( count ).append( ')' );
         }
         sb.append( ' ' );
      }
      return sb.toString();
   }

   static private String getRelationText( final IdentifiedAnnotation annotation, final Collection<BinaryTextRelation> relations ) {
      return relations.stream()
            .map( r -> getRelationText( annotation, r ) )
            .collect( Collectors.joining() );
   }

   static private String getRelationText( final IdentifiedAnnotation annotation, final BinaryTextRelation relation ) {
      if ( relation.getArg1().getArgument().equals( annotation ) ) {
         return SPACER + "[" + relation.getCategory() + "] " + getSafeText( relation.getArg2().getArgument() ) + NEWLINE;
      } else if ( relation.getArg2().getArgument().equals( annotation ) ) {
         return SPACER + getSafeText( relation.getArg1().getArgument() ) + " [" + relation.getCategory() + "]" + NEWLINE;
      }
      return "";
   }

   /**
    * @param annotations -
    * @return counts of semantic types for annotations
    */
   static private Map<String, Integer> getSemanticCounts( final Collection<IdentifiedAnnotation> annotations ) {
      // Check concepts with the same cui can have multiple tuis.  This can make it look like there are extra counts.
      final Collection<String> usedCuis = new HashSet<>();
      final Map<String, Integer> semanticCounts = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final Collection<UmlsConcept> concepts = OntologyConceptUtil.getUmlsConcepts( annotation );
         for ( UmlsConcept concept : concepts ) {
            if ( !usedCuis.add( concept.getCui() ) ) {
               continue;
            }
            final String semanticName = SemanticGroup.getSemanticName( annotation, concept );
            semanticCounts.putIfAbsent( semanticName, 0 );
            final int count = semanticCounts.get( semanticName );
            semanticCounts.put( semanticName, count + 1 );
         }
         usedCuis.clear();
         if ( concepts.isEmpty() ) {
            String semanticName = "";
            if ( annotation instanceof EventMention ) {
               semanticName = SemanticGroup.EVENT_SEMANTIC;
            } else if ( annotation instanceof TimeMention ) {
               semanticName = SemanticGroup.TIMEX_SEMANTIC;
            }
            if ( !semanticName.isEmpty() ) {
               semanticCounts.putIfAbsent( semanticName, 0 );
               final int count = semanticCounts.get( semanticName );
               semanticCounts.put( semanticName, count + 1 );
            }
         }
      }
      return semanticCounts;
   }

   /**
    * writes html for right-hand annotation information panel
    * @param writer    writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeInfoPane( final BufferedWriter writer ) throws IOException {
      writer.write( "\n<div id=\"ia\"> Annotation Information </div>\n" );
   }

   /**
    * @param corefRelations -
    * @param writer    writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeCorefInfos( final Collection<CollectionTextRelation> corefRelations, final BufferedWriter writer ) throws IOException {
      if ( corefRelations == null || corefRelations.isEmpty() ) {
         return;
      }
      int index = 1;
      for ( CollectionTextRelation corefRelation : corefRelations ) {
         final FSList chainHead = corefRelation.getMembers();
         final Collection<IdentifiedAnnotation> markables
               = FSCollectionFactory.create( chainHead, IdentifiedAnnotation.class );
         final String text = markables.stream()
               .sorted( Comparator.comparingInt( Annotation::getBegin ) )
               .map( HtmlTextWriter::getSafeText )
               .collect( Collectors.joining( "<br>" ) );
         writer.write( "  function crf" + index + "() {\n" );
         writer.write( "    document.getElementById(\"ia\").innerHTML = \"<br><h3>Coreference Chain</h3>" + text + "\";\n" );
         writer.write( "  }\n" );
         index++;
      }
   }

   /**
    * A javascript function is used to expand annotation tooltips into formatted html
    * @return javascript
    */
   static private String getSwapInfoScript() {
      return "  function iaf(txt) {\n" +
            "    var aff=txt.replace( /" + AFFIRMED + "/g,\"<br><h3>Affirmed</h3>\" );\n" +
            "    var neg=aff.replace( /" + NEGATED + "/g,\"<br><h3>Negated</h3>\" );\n" +
            "    var unc=neg.replace( /" + UNCERTAIN + "/g,\"<br><h3>Uncertain</h3>\" );\n" +
            "    var unn=unc.replace( /" + UNCERTAIN_NEGATED + "/g,\"<br><h3>Uncertain, Negated</h3>\" );\n" +
            "    var ant=unn.replace( /" + SemanticGroup.ANATOMICAL_SITE.getCode() + "/g,\"<b>Anatomical Site</b>\" );\n" +
            "    var dis=ant.replace( /" + SemanticGroup.DISORDER.getCode() + "/g,\"<b>Disease/ Disorder</b>\" );\n" +
            "    var fnd=dis.replace( /" + SemanticGroup.FINDING.getCode() + "/g,\"<b>Sign/ Symptom</b>\" );\n" +
            "    var prc=fnd.replace( /" + SemanticGroup.PROCEDURE.getCode() + "/g,\"<b>Procedure</b>\" );\n" +
            "    var drg=prc.replace( /" + SemanticGroup.MEDICATION.getCode() + "/g,\"<b>Medication</b>\" );\n" +
            "    var evt=drg.replace( /" + SemanticGroup.EVENT_CODE + "/g,\"<b>Event</b>\" );\n" +
            "    var tmx=evt.replace( /" + SemanticGroup.TIMEX_CODE + "/g,\"<b>Time</b>\" );\n" +
            "    var unk=tmx.replace( /" + SemanticGroup.UNKNOWN_SEMANTIC_CODE + "/g,\"<b>Unknown</b>\" );\n" +
            "    var spc=unk.replace( /" + SPACER + "/g,\"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\" );\n" +
            "    var prf1=spc.replace( /\\[/g,\"<i>\" );\n" +
            "    var prf2=prf1.replace( /\\]/g,\"</i>\" );\n" +
            "    var nl=prf2.replace( /" + NEWLINE + "/g,\"<br>\" );\n" +
            "    document.getElementById(\"ia\").innerHTML = nl;\n" +
            "  }\n";
   }

   /**
    *
    * @return html to write footer
    */
   static private String getFooter() {
      return "</body>\n" +
             "</html>\n";
   }

   /**
    *
    * @return html to start javascript section
    */
   static private String startJavascript() {
      return "<script type=\"text/javascript\">\n";
   }

   /**
    *
    * @return html to end javascript section
    */
   static private String endJavascript() {
      return "</script>";
   }

}
