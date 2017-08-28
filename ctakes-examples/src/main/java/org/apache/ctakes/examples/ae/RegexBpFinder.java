package org.apache.ctakes.examples.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/27/2017
 */
@PipeBitInfo(
      name = "RegexBpFinder",
      description = "Detect Blood Pressure values in Vital Signs Section", role = PipeBitInfo.Role.ANNOTATOR
)
final public class RegexBpFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "RegexBpFinder" );

   static private final String BP_SECTION = "Vital Signs";
   static private final String BP_TRIGGER = "BP(?:\\s*:)?\\s+";
   static private final String BP_VALUES = "\\d{2,3} ?\\/ ?\\d{2,3}\\b";

   static private final RegexSpanFinder REGEX_SPAN_FINDER
         = new RegexSpanFinder( BP_TRIGGER + BP_VALUES );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Blood Pressure values in Vital Signs section ..." );

      // Get the sections
      JCasUtil.select( jCas, Segment.class ).stream()
            // filter by sections with the id "Vital Signs"
            .filter( s -> s.getId().equalsIgnoreCase( BP_SECTION ) )
            // find blood pressure values
            .forEach( RegexBpFinder::logBloodPressure );

      LOGGER.info( "Finished." );
   }


   static private void logBloodPressure( final Segment section ) {
      final String text = section.getCoveredText();
      // Find text spans with blood pressure values
      final Collection<String> values = REGEX_SPAN_FINDER.findSpans( text ).stream()
            // switch from spans to text
            .map( p -> text.substring( p.getValue1(), p.getValue2() ) )
            // get rid of the bp trigger word
            .map( t -> t.replaceAll( BP_TRIGGER, "" ) )
            // get rid of whitespace on ends
            .map( String::trim )
            .collect( Collectors.toList() );
      if ( !values.isEmpty() ) {
         LOGGER.info( "Found " + values.size() + " Blood Pressure value(s)" );
         values.forEach( LOGGER::info );
      }
   }

   /**
    * Close the RegexSpanFinder when the run is complete, otherwise a thread will wait forever
    */
   @Override
   public void collectionProcessComplete() throws org.apache.uima.analysis_engine.AnalysisEngineProcessException {
      REGEX_SPAN_FINDER.close();
      super.collectionProcessComplete();
   }


}
