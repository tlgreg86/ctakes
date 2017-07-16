package org.apache.ctakes.core.cc.pretty.html;


import org.apache.ctakes.core.cc.AbstractOutputFileWriter;
import org.apache.ctakes.core.cc.pretty.SemanticGroup;
import org.apache.ctakes.core.cc.pretty.textspan.DefaultTextSpan;
import org.apache.ctakes.core.cc.pretty.textspan.TextSpan;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

   static final String TOOL_TIP = "TIP";

   static final String UNCERTAIN_NEGATED = "UNN";
   static final String NEGATED = "NEG";
   static final String UNCERTAIN = "UNC";
   static final String AFFIRMED = "AFF";

   static private final Logger LOGGER = Logger.getLogger( "HtmlTextWriter" );


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
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( htmlFile ) ) ) {
         final String title = DocumentIDAnnotationUtil.getDocumentID( jCas );
         writer.write( getHeader( title ) );
         writer.write( getCssLink( CSS_FILENAME ) );
         writeTitle( title, writer );
         final Map<Segment, Collection<Sentence>> sectionSentences
               = JCasUtil.indexCovered( jCas, Segment.class, Sentence.class );
         final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations
               = JCasUtil.indexCovered( jCas, Sentence.class, IdentifiedAnnotation.class );
         final Map<Sentence, Collection<BaseToken>> sentenceTokens
               = JCasUtil.indexCovered( jCas, Sentence.class, BaseToken.class );
         writeSections( sectionSentences, sentenceAnnotations, sentenceTokens, writer );
         writeInfoPane( writer );
         writer.write( startJavascript() );
         writer.write( getSwapInfoScript() );
         writer.write( endJavascript() );
         writer.write( getFooter() );
      }
   }

   /**
    *
    * @param title normally the document title
    * @return html to set the header
    */
   static private String getHeader( final String title ) {
      return "<!DOCTYPE html>\n<html>\n<head>\n  <title>" + title + " Output</title>\n</head>\n<body>\n";
   }

   /**
    * @param filePath path to the css file
    * @return html to link to css
    */
   static private String getCssLink( final String filePath ) {
      return "<link rel=\"stylesheet\" href=\"" + filePath + "\" type=\"text/css\" media=\"screen\">";
   }

   /**
    * Write html for document title
    * @param title normally document title, such as filename
    * @param writer    writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeTitle( final String title, final BufferedWriter writer ) throws IOException {
      if ( !title.isEmpty() ) {
         writer.write( "\n<h2>" + title + "</h2>\n" );
      }
   }

   /**
    * write html for all sections (all text) in the document
    *
    * @param sectionSentences    map of sections and their contained sentences
    * @param sentenceAnnotations map of sentences and their contained annotations
    * @param sentenceTokens      map of sentences and their contained base tokens
    * @param writer              writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSections( final Map<Segment, Collection<Sentence>> sectionSentences,
                                      final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations,
                                      final Map<Sentence, Collection<BaseToken>> sentenceTokens,
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
            writeSentence( sentence, annotations, tokens, writer );
         }
         writer.write( "\n</p>\n" );
      }
      writer.write( "\n</div>\n" );
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
      final String sectionTag = section.getTagText();
      if ( sectionTag != null && !sectionTag.trim().isEmpty() ) {
         sb.append( " onClick=\"iaf(\'" ).append( sectionTag.trim() ).append( "')\"" );
      }
      sb.append( ">" ).append( sectionId );
      final String sectionName = section.getPreferredText();
      if ( sectionName != null && !sectionName.trim().isEmpty() && !sectionName.trim().equals( sectionId ) ) {
         sb.append( " : " ).append( sectionName );
      }
      sb.append( "</h3>\n" );
      writer.write( sb.toString() );
   }


   /**
    * Write html for a sentence from the document text
    *
    * @param sentence sentence of interest
    * @param annotations identified annotations in the section
    * @param baseTokens baseTokens in the section
    * @param writer    writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeSentence( final Sentence sentence,
                                      final Collection<IdentifiedAnnotation> annotations,
                                      final Collection<BaseToken> baseTokens,
                                      final BufferedWriter writer ) throws IOException {
      if ( baseTokens.isEmpty() ) {
         return;
      }
      // Because of character substitutions, baseTokens and IdentifiedAnnotations have to be tied by text span
      final Map<TextSpan, String> baseTokenMap = createBaseTokenMap( sentence, baseTokens );
      if ( baseTokenMap.isEmpty() ) {
         return;
      }
      final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap = createAnnotationMap( sentence, annotations );
      final Map<Integer, String> tags = createTags( annotationMap );
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
      writer.write( sb.toString() + "\n<br>\n" );
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
         String text = baseToken.getCoveredText().trim();
         if ( text.isEmpty() ) {
            continue;
         }
         text = text.replaceAll( "'", "&apos;" );
         text = text.replaceAll( "\"", "&quot;" );
         text = text.replaceAll( "@", "&amp;" );
         text = text.replaceAll( "<", "&lt;" );
         text = text.replaceAll( ">", "&gt;" );
         baseItemMap.put( textSpan, text );
      }
      return baseItemMap;
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
         final Collection<String> semanticNames = getSemanticNames( annotation );
         if ( !semanticNames.isEmpty() || annotation instanceof TimeMention || annotation instanceof EventMention ) {
            annotationMap.putIfAbsent( textSpan, new ArrayList<>() );
            annotationMap.get( textSpan ).add( annotation );
         }
      }
      return annotationMap;
   }

   /**
    *
    * @param identifiedAnnotation -
    * @return all applicable semantic names for the annotation
    */
   static private Collection<String> getSemanticNames( final IdentifiedAnnotation identifiedAnnotation ) {
      final Collection<UmlsConcept> umlsConcepts = OntologyConceptUtil.getUmlsConcepts( identifiedAnnotation );
      if ( umlsConcepts == null || umlsConcepts.isEmpty() ) {
         return Collections.emptyList();
      }
      final Collection<String> semanticNames = new HashSet<>();
      for ( UmlsConcept umlsConcept : umlsConcepts ) {
         final String tui = umlsConcept.getTui();
         String semanticName = SemanticGroup.getSemanticName( tui );
         if ( semanticName.equals( "Unknown" ) ) {
            semanticName = identifiedAnnotation.getClass().getSimpleName();
         }
         semanticNames.add( semanticName );
      }
      final List<String> semanticList = new ArrayList<>( semanticNames );
      Collections.sort( semanticList );
      return semanticList;
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
    * @param annotationMap map of all annotations within or overlapping the small span elements
    * @return html for span elements
    */
   static private Map<Integer, String> createTags( final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap ) {
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
         final String clickInfo = createClickInfo( annotations );
         if ( !clickInfo.isEmpty() ) {
            sb.append( " onClick=\"iaf(\'" ).append( clickInfo ).append( "\')\"" );
         }
         final String tip = createTipText( annotations );
         if ( !tip.isEmpty() ) {
            sb.append( " " + TOOL_TIP + "=\"" ).append( tip ).append( '\"' );
         }
         sb.append( '>' );

         final Integer begin = adjustedSpan.getBegin();
         final String previousTag = indexTags.getOrDefault( begin, "" );
         indexTags.put( begin, previousTag + sb.toString() );
         indexTags.put( adjustedSpan.getEnd(), "</span>" );
      }
      return indexTags;
   }

   /**
    * @param annotations -
    * @return html with annotation information: polarity, semantic, cui, text, pref text
    */
   static private String createClickInfo( final Collection<IdentifiedAnnotation> annotations ) {
      final Map<String, Map<String, Collection<String>>> polarInfoMap = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final String polarity = createPolarity( annotation );
         polarInfoMap.putIfAbsent( polarity, new HashMap<>() );
         final Map<String, Collection<String>> infoMap = createInfoMap( annotation );
         for ( Map.Entry<String, Collection<String>> infoEntry : infoMap.entrySet() ) {
            polarInfoMap.get( polarity ).putIfAbsent( infoEntry.getKey(), new HashSet<>() );
            polarInfoMap.get( polarity ).get( infoEntry.getKey() ).addAll( infoEntry.getValue() );
         }
      }
      final List<String> polarities = new ArrayList<>( polarInfoMap.keySet() );
      Collections.sort( polarities );
      final StringBuilder sb = new StringBuilder();
      for ( String polarity : polarities ) {
         sb.append( polarity ).append( "<br>" );
         final Map<String, Collection<String>> infoMap = polarInfoMap.get( polarity );
         final List<String> semantics = new ArrayList<>( infoMap.keySet() );
         Collections.sort( semantics );
         for ( String semantic : semantics ) {
            sb.append( semantic ).append( "<br>" );
            final List<String> texts = new ArrayList<>( infoMap.get( semantic ) );
            Collections.sort( texts );
            for ( String text : texts ) {
               sb.append( "&nbsp;&nbsp;&nbsp;&nbsp;" ).append( text ).append( "<br>" );
            }
         }
      }
      return sb.toString();
   }

   /**
    * @param annotation -
    * @return map of semantic to pref text
    */
   static private Map<String, Collection<String>> createInfoMap( final IdentifiedAnnotation annotation ) {
      final Collection<UmlsConcept> concepts = OntologyConceptUtil.getUmlsConcepts( annotation );
      final Map<String, Collection<String>> semanticMap = new HashMap<>();
      for ( UmlsConcept concept : concepts ) {
         final String semanticName = getSemanticCode( annotation, concept );
         semanticMap.putIfAbsent( semanticName, new HashSet<>() );
         semanticMap.get( semanticName ).add( getPreferredText( annotation, concept ) );
      }
      return semanticMap;
   }

   /**
    * @param annotation -
    * @param concept    -
    * @return semantic name
    */
   static private String getSemanticName( final IdentifiedAnnotation annotation, final UmlsConcept concept ) {
      final String tui = concept.getTui();
      final String semanticName = SemanticGroup.getSemanticName( tui );
      if ( semanticName != null && !semanticName.equals( "Unknown" ) ) {
         return semanticName;
      }
      return annotation.getClass().getSimpleName();
   }

   /**
    * @param annotation -
    * @param concept    -
    * @return semantic code
    */
   static private String getSemanticCode( final IdentifiedAnnotation annotation, final UmlsConcept concept ) {
      final String tui = concept.getTui();
      final String semanticCode = SemanticGroup.getSemanticCode( tui );
      if ( semanticCode != null && !semanticCode.equals( SemanticGroup.UNKNOWN_SEMANTIC_CODE ) ) {
         return semanticCode;
      }
      return annotation.getClass().getSimpleName();
   }

   /**
    * @param annotation -
    * @param concept    -
    * @return cui and pref text
    */
   static private String getPreferredText( final IdentifiedAnnotation annotation, final UmlsConcept concept ) {
      final String cui = concept.getCui();
      final String coveredText = annotation.getCoveredText();
      final String preferredText = concept.getPreferredText();
      if ( preferredText != null && !preferredText.isEmpty()
            && !preferredText.equalsIgnoreCase( coveredText )
            && !coveredText.equalsIgnoreCase( preferredText + 's' ) ) {
         return cui + " : " + coveredText + " [" + preferredText + "]";
      }
      return cui + " : " + coveredText;
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
    *
    * @param annotation -
    * @return polarity for a single annotation
    */
   static private String createPolarity( final IdentifiedAnnotation annotation ) {
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
    *
    * @param annotations -
    * @return semantic names for given annotations
    */
   static private String createTipText( final Collection<IdentifiedAnnotation> annotations ) {
      final Map<String, Integer> semanticCounts = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final Collection<UmlsConcept> concepts = OntologyConceptUtil.getUmlsConcepts( annotation );
         for ( UmlsConcept concept : concepts ) {
            final String semanticName = getSemanticName( annotation, concept );
            semanticCounts.putIfAbsent( semanticName, 0 );
            final int count = semanticCounts.get( semanticName );
            semanticCounts.put( semanticName, count + 1 );
         }
      }
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

   /**
    * writes html for right-hand annotation information panel
    * @param writer    writer to which pretty html for the section should be written
    * @throws IOException if the writer has issues
    */
   static private void writeInfoPane( final BufferedWriter writer ) throws IOException {
      writer.write( "\n<div id=\"ia\"> Annotation Information </div>\n" );
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
            "    var unk=drg.replace( /" + SemanticGroup.UNKNOWN_SEMANTIC_CODE + "/g,\"<b>Unknown</b>\" );\n" +
            "    var prf1=unk.replace( /\\[/g,\"&nbsp;&nbsp;&nbsp;<i>\" );\n" +
            "    var prf2=prf1.replace( /\\]/g,\"</i>\" );\n" +
            "    document.getElementById(\"ia\").innerHTML = prf2;\n" +
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
