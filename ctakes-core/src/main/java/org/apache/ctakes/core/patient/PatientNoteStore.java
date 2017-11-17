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

import javax.annotation.concurrent.Immutable;
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
   private final Map<String, Collection<ViewInfo>> _patientViewInfos;
   private final Map<String, Integer> _wantedDocCounts;

   /**
    * private
    */
   PatientNoteStore() {
      _patientMap = new HashMap<>();
      _patientViewInfos = new HashMap<>();
      _wantedDocCounts = new HashMap<>();
   }

   /////////////////    Get available patient, document, view names   ///////////////

   /**
    * @return identifiers for all stored patients
    */
   synchronized public Collection<String> getStoredPatientIds() {
      return _patientMap.keySet().stream()
            .sorted()
            .collect( Collectors.toList() );
   }

   /**
    * @param patientId -
    * @return identifiers for all stored documents for the given patient
    */
   synchronized public Collection<String> getStoredDocIds( final String patientId ) {
      return getViewInfos( patientId ).stream()
            .map( ViewInfo::getDocId )
            .sorted()
            .distinct()
            .collect( Collectors.toList() );
   }

   /**
    * @param patientId -
    * @param docId     -
    * @return names for all stored views for the given patient and document
    */
   synchronized public Collection<String> getStoredViewNames( final String patientId, final String docId ) {
      return getViewInfos( patientId ).stream()
            .filter( vi -> vi.getDocId().equals( docId ) )
            .map( ViewInfo::getViewName )
            .sorted()
            .collect( Collectors.toList() );
   }

   /////////////////    Completion Information    ///////////////

   /**
    * @return all completed patient identifiers in the cache
    */
   synchronized public Collection<String> getCompletedPatientIds() {
      return getStoredPatientIds().stream()
            .filter( pid -> getWantedDocCount( pid ) == getStoredDocCount( pid ) )
            .sorted()
            .collect( Collectors.toList() );
   }

   /**
    * @param patientId -
    * @return number of documents for the patient that have been completed and stored in the cache
    */
   synchronized public int getStoredDocCount( final String patientId ) {
      return getStoredDocIds( patientId ).size();
   }

   /**
    *
    * @param patientId -
    * @return number of documents that exist for the patient or -1 if unknown
    */
   synchronized public int getWantedDocCount( final String patientId ) {
      return _wantedDocCounts.getOrDefault( patientId, -1 );
   }

   /**
    *
    * @param patientId -
    * @param count number of documents that exist for the patient
    */
   synchronized public void setWantedDocCount( final String patientId, final int count ) {
      _wantedDocCounts.put( patientId, count );
   }

   /////////////////    Get default patient, document names   ///////////////

   /**
    * @return the default identifier for a view of the document's patient.
    * If it has been set in the document metadata then that is used,
    * otherwise it will come from the document's parent directory.
    * @see SourceMetadataUtil#getPatientIdentifier(JCas)
    * @see DocumentIDAnnotationUtil#getDocumentIdPrefix(JCas)
    */
   public String getDefaultPatientId( final JCas viewCas ) {
      final String patientIdentifier = SourceMetadataUtil.getPatientIdentifier( viewCas );
      if ( patientIdentifier != null && !patientIdentifier.isEmpty() && !patientIdentifier.equals( SourceMetadataUtil.UNKNOWN_PATIENT ) ) {
         return patientIdentifier;
      }
      return DocumentIDAnnotationUtil.getDocumentIdPrefix( viewCas );
   }

   /**
    * @return the default identifier for a view of the document.
    * @see DocumentIDAnnotationUtil#getDocumentID(JCas)
    */
   public String getDefaultDocumentId( final JCas viewCas ) {
      return DocumentIDAnnotationUtil.getDocumentID( viewCas );
   }

   /////////////////    Store Views   ///////////////

   /**
    * Store all views in the source cas.  Patient Id and Document Id will be determined from the source cas.
    * @param sourceCas source (document) cas
    */
   synchronized public void storeAllViews( final JCas sourceCas ) {
      PatientViewUtil.getAllViewNames( sourceCas ).forEach( n -> storeView( n, sourceCas ) );
   }

   /**
    * Store all views in the source cas.
    * @param patientId -
    * @param docId -
    * @param sourceCas source (document) cas
    */
   synchronized public void storeAllViews( final String patientId, final String docId, final JCas sourceCas ) {
      PatientViewUtil.getAllViewNames( sourceCas ).forEach( n -> storeView( patientId, docId, n, sourceCas ) );
   }

   /**
    * Store the primary view under some different name.  Patient Id and Document Id will be determined from the source cas.
    * @param storeViewName the name to use to store the primary view
    * @param sourceCas source (document) cas
    */
   synchronized public void storePrimaryAsView( final String storeViewName, final JCas sourceCas ) {
      storePrimaryAsView( getDefaultPatientId( sourceCas ), getDefaultDocumentId( sourceCas ),
            storeViewName, sourceCas );
   }

   /**
    * Store the primary view under some different name.
    * @param patientId -
    * @param docId -
    * @param storeViewName the name to use to store the primary view
    * @param sourceCas source (document) cas
    */
   synchronized public void storePrimaryAsView( final String patientId, final String docId, final String storeViewName,
                                                final JCas sourceCas ) {
      storeView( patientId, docId, storeViewName, PatientViewUtil.DEFAULT_VIEW, sourceCas );
   }

   /**
    * Store some single view with its own name.  Patient Id and Document Id will be determined from the source cas.
    *
    * @param sourceViewName the name of the view in the source cas
    * @param sourceCas      source (document) cas
    */
   synchronized public void storeView( final String sourceViewName, final JCas sourceCas ) {
      storeView( getDefaultPatientId( sourceCas ), getDefaultDocumentId( sourceCas ),
            sourceViewName, sourceViewName, sourceCas );
   }

   /**
    * Explicitly store some single view with its own name.
    *
    * @param patientId      -
    * @param docId          -
    * @param sourceViewName the name of the view in the source cas
    * @param sourceCas      source (document) cas
    */
   synchronized public void storeView( final String patientId, final String docId, final String sourceViewName, final JCas sourceCas ) {
      storeView( patientId, docId, sourceViewName, sourceViewName, sourceCas );
   }

   /**
    * Explicitly store some single view under a different name.
    *
    * @param patientId      -
    * @param docId          -
    * @param storeViewName  the name to use to store the view
    * @param sourceViewName the name of the view in the source cas
    * @param sourceCas      source (document) cas
    */
   synchronized public void storeView( final String patientId, final String docId, final String storeViewName,
                                       final String sourceViewName, final JCas sourceCas ) {
      if ( getStoredViewNames( patientId, docId ).contains( storeViewName ) ) {
         LOGGER.warn( "View already stored as " + patientId + " " + docId + " " + storeViewName );
         LOGGER.warn( "Previously stored view will be replaced." );
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
      // Cache view into patient using encoded view name
      LOGGER.info( "Caching view for" + patientId + " " + docId + " " + sourceViewName
            + (sourceViewName.equals( storeViewName ) ? "" : " as " + storeViewName) + " ..." );
      final ViewInfo viewInfo = new ViewInfo( patientId, docId, storeViewName );
      try {
         final JCas sourceView = sourceCas.getView( sourceViewName );
         final CasCopier copier = new CasCopier( sourceCas.getCas(), patientCas.getCas() );
         copier.copyCasView( sourceView.getCas(), viewInfo.getViewCode(), true );
         _patientViewInfos.putIfAbsent( patientId, new ArrayList<>() );
         _patientViewInfos.get( patientId ).add( viewInfo );
      } catch ( CASException | CASRuntimeException casE ) {
         LOGGER.error( casE.getMessage() );
      }
   }

   /////////////////    view fetchers   ///////////////

   /**
    * @param patientId -
    * @param docId -
    * @param viewName -
    * @return Stored view for the parameters
    */
   synchronized public JCas getStoredView( final String patientId, final String docId, final String viewName ) {
      final JCas patientCas = _patientMap.get( patientId );
      if ( patientCas == null ) {
         LOGGER.warn( "No patient with id " + patientId );
         return null;
      }
      final ViewInfo viewInfo = new ViewInfo( patientId, docId, viewName );
      try {
         return patientCas.getView( viewInfo.getViewCode() );
      } catch ( CASException casE ) {
         LOGGER.error( casE.getMessage() );
      }
      return null;
   }

   /**
    * @param patientId -
    * @param docId     -
    * @return Map of ViewNames to Views
    */
   synchronized public Map<String, JCas> getStoredViews( final String patientId, final String docId ) {
      final JCas patientCas = _patientMap.get( patientId );
      if ( patientCas == null ) {
         LOGGER.warn( "No patient with id " + patientId );
         return null;
      }
      final Collection<String> viewNames = getStoredViewNames( patientId, docId );
      final Map<String, JCas> viewMap = new HashMap<>();
      try {
         for ( String viewName : viewNames ) {
            final ViewInfo viewInfo = new ViewInfo( viewName );
            viewMap.put( viewInfo.getViewName(), patientCas.getView( viewName ) );
         }
      } catch ( CASException casE ) {
         LOGGER.error( casE.getMessage() );
      }
      return viewMap;
   }

   /**
    * @param patientId -
    * @return Map of docIds to Map of ViewNames to Views
    */
   synchronized public Map<String, Map<String, JCas>> getStoredViews( final String patientId ) {
      final Map<String, Map<String, JCas>> viewMap = new HashMap<>();
      final Collection<String> docIds = getStoredDocIds( patientId );
      for ( String docId : docIds ) {
         viewMap.put( docId, getStoredViews( patientId, docId ) );
      }
      return viewMap;
   }

   /**
    * @param patientId - Stored patient ID
    * @param docId - Stored document ID
    * @param viewName - View name in original document CAS
    * @return String representing view name in patient CAS
    */
   synchronized public String getInternalViewname( final String patientId, final String docId, final String viewName){
      ViewInfo viewInfo = new ViewInfo(patientId, docId, viewName);
      return viewInfo.getViewCode();
   }

   /////////////////    patient cleanup - careful !   ///////////////

   /**
    * @param patientId -
    */
   synchronized public JCas getFullPatientCas( final String patientId ) {
      return _patientMap.get( patientId );
   }

   /**
    * @param patientId identifier of patient to remove from cache
    */
   synchronized public void removePatient( final String patientId ) {
      _patientMap.remove( patientId );
      _patientViewInfos.remove( patientId );
      _wantedDocCounts.remove( patientId );
   }

   /////////////////    Encoding for cached patient view names   ///////////////

   /**
    * @param patientId -
    * @return all encoded
    */
   synchronized private Collection<ViewInfo> getViewInfos( final String patientId ) {
      final Collection<ViewInfo> viewInfos = _patientViewInfos.get( patientId );
      if ( viewInfos == null ) {
         LOGGER.debug( "No patient with id " + patientId );
         return Collections.emptyList();
      }
      return viewInfos;
   }

   /**
    * Used to map pid, docId, view names to views for each patient.
    */
   @Immutable
   static private final class ViewInfo {
      private final String _pid;
      private final String _docId;
      private final String _viewName;

      private ViewInfo( final String viewCode ) {
         this( getId( viewCode, "pid" ),
               getId( viewCode, "docId" ),
               getId( viewCode, "viewName" ) );
      }

      private ViewInfo( final String pid, final String docId, final String viewName ) {
         _pid = pid;
         _docId = docId;
         _viewName = viewName;
      }

      public String getPid() {
         return _pid;
      }

      public String getDocId() {
         return _docId;
      }

      public String getViewName() {
         return _viewName;
      }

      public String getViewCode() {
         return "<pid>" + _pid + "</pid><docId>" + _docId + "</docId><viewName>" + _viewName + "</viewName>";
      }

      @Override
      public boolean equals( final Object object ) {
         return object instanceof ViewInfo && ((ViewInfo) object).getViewCode().equals( getViewCode() );
      }

      @Override
      public int hashCode() {
         return getViewCode().hashCode();
      }

      static private String getId( final String viewCode, final String tag ) {
         final int i1 = viewCode.indexOf( "<" + tag + ">" );
         if ( i1 < 0 ) {
            return viewCode;
         }
         final int i2 = viewCode.indexOf( "</" + tag + ">" );
         if ( i2 < 0 ) {
            return viewCode;
         }
         return viewCode.substring( i1 + 2 + tag.length(), i2 );
      }
   }


}
