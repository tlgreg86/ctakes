package org.apache.ctakes.core.patient;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

/**
 * Sends document cas to the {@link PatientNoteStore} to be cached
 * using the {@link PatientNoteStore#getDefaultPatientName(JCas)}
 * and {@link PatientNoteStore#getDefaultDocumentName(JCas)}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/26/2017
 */
@PipeBitInfo(
      name = "PatientNoteCollector",
      description = "Caches each Document JCas in a Patient JCas as a View.", role = PipeBitInfo.Role.SPECIAL
)
final public class PatientNoteCollector extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PatientNoteCollector" );


   /**
    * Adds the primary view of this cas to a cache of views for patients.
    * See {@link PatientNoteStore}
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Caching Document " + PatientNoteStore.getInstance().getDefaultDocumentName( jCas )
            + " into Patient " + PatientNoteStore.getInstance().getDefaultPatientName( jCas ) + " ..." );

      PatientNoteStore.getInstance().addDocument( jCas );

      LOGGER.info( "Finished." );
   }


}
