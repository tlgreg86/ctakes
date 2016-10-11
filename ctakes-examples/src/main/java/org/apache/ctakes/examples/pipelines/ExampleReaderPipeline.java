package org.apache.ctakes.examples.pipelines;


import org.apache.ctakes.core.pipeline.EntityCollector;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PipelineReader;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.IOException;

/**
 * Build and run a pipeline using a {@link PipelineReader} and a {@link PipelineBuilder}.
 * <p>
 * Example of a running a pipeline programatically w/o uima xml descriptor xml files
 * Adds the default Tokenization pipeline and adding the Example HelloWorld Annotator
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2016
 */
final public class ExampleReaderPipeline {

   static private final Logger LOGGER = Logger.getLogger( "ExampleReaderPipeline" );

   static private final String PIPELINE_1_PATH = "org/apache/ctakes/examples/pipelines/ExamplePipeline1.txt";
   static private final String PIPELINE_2_PATH = "org/apache/ctakes/examples/pipelines/ExamplePipeline2.txt";

   private ExampleReaderPipeline() {
   }

   /**
    * @param args an output directory for xmi files or none if xmi files are not wanted
    */
   public static void main( final String... args ) {
      final String text = "Hello World!";
      try {
         // Add a simple pre-defined existing pipeline for Tokenization from file
         final PipelineReader reader = new PipelineReader( PIPELINE_1_PATH );
         // add the POS Tagger manually
         PipelineBuilder builder = reader.getBuilder();
         builder.addDescription( POSTagger.createAnnotatorDescription() );
         // Add the new HelloWorld Example by reading from file
         reader.loadPipelineFile( PIPELINE_2_PATH );
         // Collect the Entities
         builder.collectEntities();
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
