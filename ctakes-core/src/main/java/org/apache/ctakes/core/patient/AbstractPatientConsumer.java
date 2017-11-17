package org.apache.ctakes.core.patient;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import java.util.Collection;


/**
 * Extend this annotator to consume a patient cas once the current patient name has changed.
 * For instance, when processing patientA doc1, patientA doc2, patientA doc3 this annotator does nothing.
 * After the processing of patientB doc1 this annotator will process a cas containing all documents for patientA.
 * At the end of the pipeline, the last unprocessed patient will be processed.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2017
 */
@PipeBitInfo(
      name = "AbstractPatientConsumer",
      description = "Abstract Engine to take action on a patient level instead of document level.", role = PipeBitInfo.Role.ANNOTATOR
)
abstract public class AbstractPatientConsumer extends JCasAnnotator_ImplBase {

   static public final String REMOVE_PATIENT = "RemovePatient";

   @ConfigurationParameter(
         name = REMOVE_PATIENT,
         description = "The Patient Consumer should remove the patient from the cache when finished.",
         defaultValue = "true"
   )
   private boolean _removePatient;

   private final String _action;
   private final Logger _logger;

   protected AbstractPatientConsumer( final String aeName, final String action ) {
      _action = action;
      _logger = Logger.getLogger( aeName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final Collection<String> completedPatientIds = PatientNoteStore.getInstance().getCompletedPatientIds();
      for ( String id : completedPatientIds ) {
         process( id );
         if ( _removePatient ) {
            PatientNoteStore.getInstance().removePatient( id );
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      final Collection<String> allPatientIds = PatientNoteStore.getInstance().getStoredPatientIds();
      for ( String id : allPatientIds ) {
         process( id );
         if ( _removePatient ) {
            PatientNoteStore.getInstance().removePatient( id );
         }
      }
   }

   /**
    * Logs start and finish and calls {@link #processPatientCas(JCas)}.
    *
    * @param patientName -
    * @throws AnalysisEngineProcessException if subclass has a problem processing.
    */
   private void process( final String patientName ) throws AnalysisEngineProcessException {
      if ( patientName == null ) {
         return;
      }
      _logger.info( _action + " for patient " + patientName + " ..." );

      processPatientCas( PatientNoteStore.getInstance().getFullPatientCas( patientName ) );

      _logger.info( "Finished." );
   }

   /**
    * @param patientJcas JCas containing multiple views for a single patient.
    * @throws AnalysisEngineProcessException if there is some problem.
    */
   abstract protected void processPatientCas( final JCas patientJcas ) throws AnalysisEngineProcessException;

}
