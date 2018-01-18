package org.apache.ctakes.fhir.util;


import org.apache.ctakes.fhir.cc.FhirElementFactory;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class NoteSpecs {

   static private final Logger LOGGER = Logger.getLogger( "NoteSpecsHolder" );

   static private final String SUBJECT_PATIENT = "patient";

   private final Reference _mainPatientRef;
   private final Map<String, Reference> _subjectRefs;
   private final Period _period;

   public NoteSpecs( final JCas jCas ) {
      final long startMillis = System.currentTimeMillis();
      _period = FhirElementFactory.createPeriod( startMillis, startMillis + 1000 );
      final Patient mainPatient = new Patient();
      mainPatient.setId( FhirElementFactory.createId( jCas, SUBJECT_PATIENT ) );
      _mainPatientRef = new Reference( mainPatient );
      _subjectRefs = createSubjects( jCas );
   }

   public Collection<Resource> getSubjects() {
      final Collection<Resource> subjects = new ArrayList<>();
      subjects.add( (Resource) _mainPatientRef.getResource() );
      _subjectRefs.values().forEach( s -> subjects.add( (Resource) s.getResource() ) );
      return subjects;
   }

   public Reference getSubjectReference( final String name ) {
      if ( name == null || name.isEmpty() || SUBJECT_PATIENT.equalsIgnoreCase( name ) ) {
         return _mainPatientRef;
      }
      return _subjectRefs.getOrDefault( name.toLowerCase(), _mainPatientRef );
   }

   public Period getPeriod() {
      return _period;
   }

   private Reference createSubject( final JCas jCas, final String subject ) {
      final RelatedPerson relation = new RelatedPerson( _mainPatientRef );
      relation.setId( FhirElementFactory.createId( jCas, subject ) );
      return new Reference( relation );
   }

   private Map<String, Reference> createSubjects( final JCas jCas ) {
      return JCasUtil.select( jCas, IdentifiedAnnotation.class ).stream()
            .map( IdentifiedAnnotation::getSubject )
            .filter( Objects::nonNull )
            .map( String::toLowerCase )
            .filter( s -> !SUBJECT_PATIENT.equals( s ) )
            .distinct()
            .collect( Collectors.toMap( Function.identity(), s -> createSubject( jCas, s ) ) );
   }


}
