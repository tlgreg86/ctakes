package org.apache.ctakes.core.cc;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.Collection;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/14/2018
 */
@PipeBitInfo(
      name = "CUI List Writer",
      description = "Writes a list of CUIs, covered text and preferred text to files.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, SENTENCE, BASE_TOKEN },
      usables = { DOCUMENT_ID_PREFIX, IDENTIFIED_ANNOTATION, EVENT, TIMEX, TEMPORAL_RELATION }
)
public class CuiListFileWriter extends AbstractJCasFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "CuiListFileWriter" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final File file = new File( outputDir, fileName + "_cuis.txt" );
      final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( jCas, IdentifiedAnnotation.class );
      LOGGER.info( "Writing CUI list to " + file.getPath() + " ..." );
      final StringBuilder sb = new StringBuilder();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final String coveredText = annotation.getCoveredText();
         OntologyConceptUtil.getUmlsConceptStream( annotation )
                            .map( c -> c.getCui() + " , " + coveredText
                                       + (c.getPreferredText() != null ? " , " + c.getPreferredText() : "")
                                       + "\r\n" )
                            .forEach( sb::append );
      }
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( sb.toString() );
      }
      LOGGER.info( "Finished Writing" );
   }

}
