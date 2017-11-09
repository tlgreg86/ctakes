package org.apache.ctakes.core.patient;


import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cache for multi-document patient cas objects
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/26/2017
 */
public enum PatientNoteStore {
   INSTANCE;

   public static PatientNoteStore getInstance() {
      return INSTANCE;
   }


   static private final Logger LOGGER = Logger.getLogger( "PatientNoteStore" );

   private final Map<String, JCas> _patientMap;
   private final Map<String, Integer> _wantedDocCounts;
   private final Map<String, Integer> _storedDocCounts;
   private String _currentPatientName;
   private String _previousPatientName;

   PatientNoteStore() {
      _patientMap = new HashMap<>();
      _wantedDocCounts = new HashMap<>();
      _storedDocCounts = new HashMap<>();
   }

   /**
    * @return all patient identifiers in the cache
    */
   synchronized public Collection<String> getPatientIds() {
      return Collections.unmodifiableList( new ArrayList<>( _patientMap.keySet() ) );
   }

   /**
    * @return all completed patient identifiers in the cache
    */
   synchronized public Collection<String> getCompletedPatientIds() {
      return _wantedDocCounts.entrySet().stream()
            .filter( e -> _storedDocCounts.getOrDefault( e.getKey(), 0 ).equals( e.getValue() ) )
            .map( Map.Entry::getKey )
            .collect( Collectors.toList() );
   }


   /**
    *
    * @param patientId -
    * @return number of documents that exist for the patient or -1 if unknown
    */
   synchronized public int getDocCount( final String patientId ) {
      return _wantedDocCounts.getOrDefault( patientId, -1 );
   }

   /**
    *
    * @param patientId -
    * @param count number of documents that exist for the patient
    */
   synchronized public void setDocCount( final String patientId, final int count ) {
      _wantedDocCounts.put( patientId, count );
   }

   /**
    * @param patientId -
    * @return number of documents for the patient that have been completed and stored in the cache
    */
   synchronized public int getCompletedDocCount( final String patientId ) {
      return _storedDocCounts.getOrDefault( patientId, 0 );
   }

   /**
    * @return the default identifier for a view of the document.  {@link DocumentIDAnnotationUtil#getDocumentID(JCas)}
    */
   public String getDefaultDocumentId( final JCas viewCas ) {
      return DocumentIDAnnotationUtil.getDocumentID( viewCas );
   }

   /**
    * @return the default identifier for a view of the document's patient.  {@link DocumentIDAnnotationUtil#getDocumentIdPrefix(JCas)}
    */
   public String getDefaultPatientId( final JCas viewCas ) {
      final String patientIdentifier = SourceMetadataUtil.getPatientIdentifier( viewCas );
      if ( patientIdentifier != null && !patientIdentifier.isEmpty() && !patientIdentifier.equals( SourceMetadataUtil.UNKNOWN_PATIENT ) ) {
         return patientIdentifier;
      }
      return DocumentIDAnnotationUtil.getDocumentIdPrefix( viewCas );
   }

   /**
    * @param goldCas ye olde containing the gold in the default view
    */
   synchronized public void addGoldView( final JCas goldCas ) {
      final int goldCount = getGoldViewNames( getDefaultPatientId( goldCas ) ).size();
      addGoldView( "" + (goldCount + 1), goldCas );
   }

   /**
    * @param goldName name to use for the cached gold view
    * @param goldCas  ye olde containing the document in the default view
    */
   synchronized public void addGoldView( final String goldName, final JCas goldCas ) {
      addDocument( PatientViewUtil.GOLD_PREFIX + "_" + goldName, goldCas );
   }

   /**
    * @param documentCas ye olde containing the document in the default view
    */
   synchronized public void addDocument( final JCas documentCas ) {
      addDocument( getDefaultDocumentId( documentCas ), documentCas );
   }

   /**
    * @param viewName    name to use for the cached document view
    * @param documentCas ye olde containing the document in the default view
    */
   synchronized public void addDocument( final String viewName, final JCas documentCas ) {
      addDocument( getDefaultPatientId( documentCas ), viewName, documentCas );
   }

   /**
    * @param patientId name to use for the cached patient cas
    * @param viewName    name to use for the cached document view
    * @param documentCas ye olde containing the document in the default view
    */
   synchronized public void addDocument( final String patientId, final String viewName, final JCas documentCas ) {
      if ( !patientId.equals( _currentPatientName ) ) {
         _previousPatientName = _currentPatientName;
         _currentPatientName = patientId;
      }
      // don't use putIfAbsent or computeIfAbsent to better handle exceptions and lazy instantiation
      JCas patientCas = _patientMap.get( patientId );
      if ( patientCas == null ) {
         try {
            patientCas = JCasFactory.createJCas();
            _patientMap.put( patientId, patientCas );
         } catch ( UIMAException uE ) {
            LOGGER.error( uE.getMessage() );
            return;
         }
      }
      LOGGER.info( "Caching " + viewName + " for patient " + patientId + " ..." );
      try {
         final JCas mainView = documentCas.getView( PatientViewUtil.DEFAULT_VIEW );
         final CasCopier copier = new CasCopier( documentCas.getCas(), patientCas.getCas() );
         copier.copyCasView( mainView.getCas(), viewName, true );
         final int stored = _storedDocCounts.getOrDefault( patientId, 0 );
         _storedDocCounts.put( patientId, stored + 1 );
      } catch ( CASException | CASRuntimeException casE ) {
         LOGGER.error( casE.getMessage() );
      }
   }

   /**
    * @param patientId identifier of patient
    * @return cached cas representing patient with documents and gold as views, or null if none
    */
   synchronized public JCas getPatientCas( final String patientId ) {
      return _patientMap.get( patientId );
   }

   /**
    * @param patientId identifier of patient to remove from cache
    */
   synchronized public void removePatient( final String patientId ) {
      _patientMap.remove( patientId );
      _wantedDocCounts.remove( patientId );
   }

   /**
    * @param patientId identifier of patient
    * @return All views, including gold and default
    */
   synchronized public Collection<JCas> getAllViews( final String patientId ) {
      final JCas patientCas = getPatientCas( patientId );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getAllViews( patientCas );
   }

   /**
    * @param patientId identifier of patient
    * @return All document views, which are views that are not the default and not gold
    */
   synchronized public Collection<JCas> getDocumentViews( final String patientId ) {
      final JCas patientCas = getPatientCas( patientId );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getDocumentViews( patientCas );
   }

   /**
    * @param patientId identifier of patient
    * @return All gold views, which are views with the prefix {@link PatientViewUtil#GOLD_PREFIX}
    */
   synchronized public Collection<JCas> getGoldViews( final String patientId ) {
      final JCas patientCas = getPatientCas( patientId );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getGoldViews( patientCas );
   }

   /**
    * @param patientId identifier of patient
    * @return Names of all views, including gold and default
    */
   synchronized public Collection<String> getAllViewNames( final String patientId ) {
      final JCas patientCas = getPatientCas( patientId );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getAllViewNames( patientCas );
   }

   /**
    * @param patientId identifier of patient
    * @return Names of all document views, which are views that are not the default and not gold
    */
   synchronized public Collection<String> getDocumentViewNames( final String patientId ) {
      final JCas patientCas = getPatientCas( patientId );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getDocumentViewNames( patientCas );
   }

   /**
    * @param patientId identifier of patient
    * @return Names of all gold views, which are views with the prefix {@link PatientViewUtil#GOLD_PREFIX}
    */
   synchronized public Collection<String> getGoldViewNames( final String patientId ) {
      final JCas patientCas = getPatientCas( patientId );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getGoldViewNames( patientCas );
   }

}
