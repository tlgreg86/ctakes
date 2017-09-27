package org.apache.ctakes.core.patient;


import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;

import java.util.*;

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
   private String _currentPatientName;
   private String _previousPatientName;

   PatientNoteStore() {
      _patientMap = new HashMap<>();
   }

   /**
    * @return all patient names in the cache
    */
   synchronized public Collection<String> getPatientNames() {
      return Collections.unmodifiableList( new ArrayList<>( _patientMap.keySet() ) );
   }

   /**
    * @return the name of the most recently stored patient
    */
   synchronized public String getCurrentPatientName() {
      return _currentPatientName;
   }

   /**
    * @return the name of the patient stored before the most recently stored patient
    */
   synchronized public String getPreviousPatientName() {
      return _previousPatientName;
   }

   /**
    * @return the default name for a view of the document.  {@link DocumentIDAnnotationUtil#getDocumentID(JCas)}
    */
   public String getDefaultDocumentName( final JCas viewCas ) {
      return DocumentIDAnnotationUtil.getDocumentID( viewCas );
   }

   /**
    * @return the default name for a view of the document's patient.  {@link DocumentIDAnnotationUtil#getDocumentIdPrefix(JCas)}
    */
   public String getDefaultPatientName( final JCas viewCas ) {
      return DocumentIDAnnotationUtil.getDocumentIdPrefix( viewCas );
   }

   /**
    * @param goldCas ye olde containing the gold in the default view
    */
   synchronized public void addGoldView( final JCas goldCas ) {
      final int goldCount = getGoldViewNames( getDefaultPatientName( goldCas ) ).size();
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
      addDocument( getDefaultDocumentName( documentCas ), documentCas );
   }

   /**
    * @param viewName    name to use for the cached document view
    * @param documentCas ye olde containing the document in the default view
    */
   synchronized public void addDocument( final String viewName, final JCas documentCas ) {
      addDocument( getDefaultPatientName( documentCas ), viewName, documentCas );
   }

   /**
    * @param patientName name to use for the cached patient cas
    * @param viewName    name to use for the cached document view
    * @param documentCas ye olde containing the document in the default view
    */
   synchronized public void addDocument( final String patientName, final String viewName, final JCas documentCas ) {
      if ( !patientName.equals( _currentPatientName ) ) {
         _previousPatientName = _currentPatientName;
         _currentPatientName = patientName;
      }
      // don't use putIfAbsent or computeIfAbsent to better handle exceptions and lazy instantiation
      JCas patientCas = _patientMap.get( patientName );
      if ( patientCas == null ) {
         try {
            patientCas = JCasFactory.createJCas();
            _patientMap.put( patientName, patientCas );
         } catch ( UIMAException uE ) {
            LOGGER.error( uE.getMessage() );
            return;
         }
      }
      LOGGER.info( "Caching " + viewName + " for patient " + patientName + " ..." );
      try {
         final JCas mainView = documentCas.getView( PatientViewUtil.DEFAULT_VIEW );
         final CasCopier copier = new CasCopier( documentCas.getCas(), patientCas.getCas() );
         copier.copyCasView( mainView.getCas(), viewName, true );
      } catch ( CASException | CASRuntimeException casE ) {
         LOGGER.error( casE.getMessage() );
      }
   }

   /**
    * @param patientName name of patient
    * @return cached cas representing patient with documents and gold as views, or null if none
    */
   synchronized public JCas getPatientCas( final String patientName ) {
      return _patientMap.get( patientName );
   }

   /**
    * @param patientName name of patient to remove from cache
    */
   synchronized public void removePatientCas( final String patientName ) {
      _patientMap.remove( patientName );
   }

   /**
    * @param patientName name of patient
    * @return All views, including gold and default
    */
   synchronized public Collection<JCas> getAllViews( final String patientName ) {
      final JCas patientCas = getPatientCas( patientName );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getAllViews( patientCas );
   }

   /**
    * @param patientName name of patient
    * @return All document views, which are views that are not the default and not gold
    */
   synchronized public Collection<JCas> getDocumentViews( final String patientName ) {
      final JCas patientCas = getPatientCas( patientName );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getDocumentViews( patientCas );
   }

   /**
    * @param patientName name of patient
    * @return All gold views, which are views with the prefix {@link PatientViewUtil#GOLD_PREFIX}
    */
   synchronized public Collection<JCas> getGoldViews( final String patientName ) {
      final JCas patientCas = getPatientCas( patientName );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getGoldViews( patientCas );
   }

   /**
    * @param patientName name of patient
    * @return Names of all views, including gold and default
    */
   synchronized public Collection<String> getAllViewNames( final String patientName ) {
      final JCas patientCas = getPatientCas( patientName );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getAllViewNames( patientCas );
   }

   /**
    * @param patientName name of patient
    * @return Names of all document views, which are views that are not the default and not gold
    */
   synchronized public Collection<String> getDocumentViewNames( final String patientName ) {
      final JCas patientCas = getPatientCas( patientName );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getDocumentViewNames( patientCas );
   }

   /**
    * @param patientName name of patient
    * @return Names of all gold views, which are views with the prefix {@link PatientViewUtil#GOLD_PREFIX}
    */
   synchronized public Collection<String> getGoldViewNames( final String patientName ) {
      final JCas patientCas = getPatientCas( patientName );
      if ( patientCas == null ) {
         return Collections.emptyList();
      }
      return PatientViewUtil.getGoldViewNames( patientCas );
   }

}
