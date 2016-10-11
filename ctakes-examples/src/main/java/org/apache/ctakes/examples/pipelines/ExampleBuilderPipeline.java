package org.apache.ctakes.examples.pipelines;


import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.core.pipeline.EntityCollector;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.examples.ae.ExampleHelloWorldAnnotator;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.IOException;

/**
 * Build and run a pipeline using a {@link PipelineBuilder}.
 * <p>
 * Example of a running a pipeline programatically w/o uima xml descriptor xml files
 * Adds the default Tokenization pipeline and adding the Example HelloWorld Annotator
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2016
 */
final public class ExampleBuilderPipeline {

   static private final Logger LOGGER = Logger.getLogger( "ExampleBuilderPipeline" );

   private ExampleBuilderPipeline() {
   }

   /**
    * @param args an output directory for xmi files or none if xmi files are not wanted
    */
   public static void main( final String... args ) {
      final String text = "Hello World!";
      try {
         PipelineBuilder builder = new PipelineBuilder();
         builder
               // Add a simple pre-defined existing pipeline for Tokenization
               // Could also add engines individually
               .addDescription( ClinicalPipelineFactory.getTokenProcessingPipeline() )
               // Add the new HelloWorld Example
               .add( ExampleHelloWorldAnnotator.class )
               // Collect the Entities
               .collectEntities();
         if ( args.length > 0 ) {
            //Example to save the Aggregate descriptor to an xml file for external
            //use such as the UIMA CVD/CPE
            builder.writeXMIs( args[ 0 ] );
         }
         // Run the pipeline with specified text
         builder.run( text );
      } catch ( IOException | UIMAException multE ) {
         LOGGER.error( multE.getMessage() );
      }
      //Print out the IdentifiedAnnotation objects
      LOGGER.info( "\n" + EntityCollector.getInstance().toString() );
   }


}
