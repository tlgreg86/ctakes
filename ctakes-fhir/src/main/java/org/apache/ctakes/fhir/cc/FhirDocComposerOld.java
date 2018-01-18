package org.apache.ctakes.fhir.cc;


import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/15/2017
 */
final public class FhirDocComposerOld {

   static private final Logger LOGGER = Logger.getLogger( "FhirDocComposer" );


   static private final String CTAKES_PACKAGE = "org.apache.ctakes";
   static private final String CTAKES_SECTION_URL = "http://ctakes.apache.org/apidocs/4.0.0/org/apache/ctakes/typesystem/type/textspan/Segment";
   static private final String CTAKES_ASSERTION_URL = "https://cwiki.apache.org/confluence/display/CTAKES/cTAKES+4.0+-+Assertion";

   static private final String ASSERTION_GENERIC = "Generic";
   static private final String ASSERTION_UNCERTAIN = "Uncertain";
   static private final String ASSERTION_NEGATED = "Negated";
   static private final String ASSERTION_AFFIRMED = "Affirmed";
   static private final String ASSERTION_UNCERT_NEGATED = "Uncertain_Negated";
   static private final String SPAN_EXT_URL = "http://org.apache.ctakes/fhir/text-span";
   // I feel ok using assertion as a url as it pretty much covers the possible values.  The temporal wiki has more.
   // https://cwiki.apache.org/confluence/display/CTAKES/cTAKES+4.0+-+Temporal+Module#cTAKES4.0-TemporalModule-DocTimeRelannotator
   static private final String DTR_EXT_URL = "http://org.apache.ctakes/fhir/doc-time-rel";
   static private final String SUBJECT_PATIENT = "Patient";
   static private final Collection<String> DIAGNOSIS_SECTIONS
         = Arrays.asList( "Analysis of Problem", "Diagnosis", "Principle Diagnosis", "Post Procedure Diagnosis",
         "Final Diagnosis", "Diagnosis at Death" );
   static private final String SIMPLE_SECTION = "SIMPLE_SEGMENT";

   private FhirDocComposerOld() {
   }


   static public Composition composeDocFhir( final JCas jCas ) {
      final Composition composition = new Composition();

      final Map<Segment, Collection<IdentifiedAnnotation>> sectionAnnotationMap
            = JCasUtil.indexCovered( jCas, Segment.class, IdentifiedAnnotation.class );
      final Map<String, BaseResource> subjects = createSubjects( sectionAnnotationMap.values() );

      final Map<IdentifiedAnnotation, BaseResource> annotationFhirs = new HashMap<>();
      final Map<String, Integer> typeCounts = new HashMap<>();
      for ( Map.Entry<Segment, Collection<IdentifiedAnnotation>> sectionAnnotations : sectionAnnotationMap.entrySet() ) {
         final Segment section = sectionAnnotations.getKey();
         for ( IdentifiedAnnotation annotation : sectionAnnotations.getValue() ) {
            BaseResource fhir;
//         final Identifier id = new Identifier( CTAKES_PACKAGE,  );
            if ( annotation instanceof AnatomicalSiteMention ) {
               fhir = createBodySite( (AnatomicalSiteMention) annotation, section, subjects );
            } else if ( annotation instanceof DiseaseDisorderMention ) {
               fhir = createDisorder( (DiseaseDisorderMention) annotation, section, subjects );
            } else if ( annotation instanceof SignSymptomMention ) {
               fhir = createFinding( (SignSymptomMention) annotation, section, subjects );
            } else if ( annotation instanceof ProcedureMention ) {
               fhir = createProcedure( (ProcedureMention) annotation, section, subjects );
            } else if ( annotation instanceof MedicationMention ) {
               fhir = createMedication( (MedicationMention) annotation, section, subjects );
            } else {
               // How to handle generic events ?  Composition.CompositionEventComponent
               // timex can be ??


               continue;
            }
            final String id = createAnnotationId( annotation, typeCounts );
            fhir.setId( id );
            annotationFhirs.put( annotation, fhir );
         }
      }


      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      // Assign body sites, severity

      // Need to create an extension for

      return composition;
   }


   static private Map<String, BaseResource> createSubjects( final Collection<Collection<IdentifiedAnnotation>> annotations ) {
      final Patient patient = new Patient();
      final Map<String, BaseResource> subjects = new HashMap<>();
      for ( Collection<IdentifiedAnnotation> collection : annotations ) {
         for ( IdentifiedAnnotation annotation : collection ) {
            final String subject = annotation.getSubject();
            if ( subject == null || subjects.containsKey( subject ) ) {
               continue;
            }
            if ( subject.equalsIgnoreCase( "patient" ) ) {
               subjects.put( subject, patient );
            } else {
               final RelatedPerson relation = new RelatedPerson();
               subjects.put( subject, relation );
            }
         }
      }
      subjects.putIfAbsent( "Patient", patient );
      return subjects;
   }


   static private Patient createPatient() {
      final Patient patient = new Patient();


      return patient;
   }

   static private Reference createSubjectReference( final String subject, final Map<String, BaseResource> subjects ) {
      final String searcher = subject == null ? SUBJECT_PATIENT : subject;
      BaseResource resource = subjects.get( searcher );
      if ( resource == null ) {
         resource = subjects.get( SUBJECT_PATIENT );
      }
      return new Reference( resource );
   }


   static private BodySite createBodySite( final AnatomicalSiteMention annotation,
                                           final Segment section,
                                           final Map<String, BaseResource> subjects ) {
      final BodySite bodySite = new BodySite();
      final CodeableConcept code = createPrimaryCode( annotation );
      bodySite.setCode( code );
      // Qualifiers such as laterality and body side are not yet created by ctakes
//      final CodeableConcept qualifier = createBodySiteQualifier( annotation );
      addAssertionExtension( annotation, bodySite );
      bodySite.setPatient( createSubjectReference( annotation.getSubject(), subjects ) );
      return bodySite;
   }

   /**
    * @param annotation -
    * @param section    -
    * @param subjects   -
    * @return as much of a fhir condition as possible.
    */
   static private Condition createDisorder( final DiseaseDisorderMention annotation,
                                            final Segment section,
                                            final Map<String, BaseResource> subjects ) {
      final Condition condition = new Condition();
      final CodeableConcept code = createPrimaryCode( annotation );
      condition.setCode( code );
      final CodeableConcept category = createConditionCategory( annotation, section );
      condition.addCategory( category );
      condition.setSubject( createSubjectReference( annotation.getSubject(), subjects ) );
      addAssertionExtension( annotation, condition );
      addDocTimeRelExtension( annotation, condition );
      return condition;
   }

   static private Observation createFinding( final SignSymptomMention annotation,
                                             final Segment section,
                                             final Map<String, BaseResource> subjects ) {
      final Observation observation = new Observation();
      final CodeableConcept code = createPrimaryCode( annotation );
      observation.setCode( code );
      final CodeableConcept category = createObservationCategory( annotation, section );
      observation.addCategory( category );
      observation.setSubject( createSubjectReference( annotation.getSubject(), subjects ) );
      addAssertionExtension( annotation, observation );
      addDocTimeRelExtension( annotation, observation );
      return observation;
   }

   static private Procedure createProcedure( final ProcedureMention annotation,
                                             final Segment section,
                                             final Map<String, BaseResource> subjects ) {
      final Procedure procedure = new Procedure();
      final CodeableConcept code = createPrimaryCode( annotation );
      procedure.setCode( code );
      final CodeableConcept category = createProcedureCategory( annotation, section );
      procedure.setCategory( category );
      procedure.setSubject( createSubjectReference( annotation.getSubject(), subjects ) );
      addAssertionExtension( annotation, procedure );
      addDocTimeRelExtension( annotation, procedure );
      return procedure;
   }


   static private Medication createMedication( final MedicationMention annotation,
                                               final Segment section,
                                               final Map<String, BaseResource> subjects ) {
      final Medication medication = new Medication();
      final CodeableConcept code = createPrimaryCode( annotation );
      medication.setCode( code );
      addAssertionExtension( annotation, medication );
      addDocTimeRelExtension( annotation, medication );
      return medication;
   }


   /**
    * Conditions do use snomed codes as a general "code".
    *
    * @param annotation -
    * @return all snomed codes in a codeable concept
    */
   static private CodeableConcept createPrimaryCode( final IdentifiedAnnotation annotation ) {
      final CodeableConcept codeableConcept = new CodeableConcept();
      for ( OntologyConcept concept : OntologyConceptUtil.getOntologyConcepts( annotation ) ) {
         final String scheme = concept.getCodingScheme();
         final String code = concept.getCode();
         String preferredText = null;
         if ( concept instanceof UmlsConcept ) {
            preferredText = ((UmlsConcept) concept).getPreferredText();
         }
         codeableConcept.addCoding( new Coding( scheme, code, preferredText ) );
      }
      codeableConcept.setText( annotation.getCoveredText() );
      final Range span = new Range();
      span.setLow( createQuantity( annotation.getBegin() ) );
      span.setHigh( createQuantity( annotation.getEnd() ) );
      final Extension spanExtension = new Extension( SPAN_EXT_URL, span );
      codeableConcept.addExtension( spanExtension );
      return codeableConcept;
   }

   static private SimpleQuantity createQuantity( final int number ) {
      final SimpleQuantity quantity = new SimpleQuantity();
      quantity.setValue( number );
      return quantity;
   }

   static private CodeableConcept createConditionCategory( final IdentifiedAnnotation annotation,
                                                           final Segment section ) {
      Coding coding = new Coding( "http://hl7.org/fhir/condition-category",
            "problem-list-item", "Problem List Item" );
      final String sectionId = section.getId();
      if ( DIAGNOSIS_SECTIONS.contains( sectionId ) ) {
         coding = new Coding( "http://hl7.org/fhir/condition-category",
               "encounter-diagnosis", "Encounter Diagnosis" );
      }
      final CodeableConcept codeableConcept = new CodeableConcept();
      codeableConcept.addCoding( coding );
      if ( !sectionId.equals( SIMPLE_SECTION ) ) {
         // The section can also be used as a category.
         codeableConcept.addCoding( new Coding( CTAKES_SECTION_URL, sectionId, section.getPreferredText() ) );
      }
      return codeableConcept;
   }

   static private CodeableConcept createObservationCategory( final IdentifiedAnnotation annotation,
                                                             final Segment section ) {
      final String sectionId = section.getId();
      // TODO add fhir categories by section id http://hl7.org/fhir/codesystem-observation-category.html
      final CodeableConcept codeableConcept = new CodeableConcept();
      if ( !sectionId.equals( SIMPLE_SECTION ) ) {
         // The section can also be used as a category.
         codeableConcept.addCoding( new Coding( CTAKES_SECTION_URL, sectionId, section.getPreferredText() ) );
      }
      return codeableConcept;
   }

   static private CodeableConcept createProcedureCategory( final IdentifiedAnnotation annotation,
                                                           final Segment section ) {
      final String sectionId = section.getId();
      // TODO add fhir categories by section id https://www.hl7.org/fhir/valueset-procedure-category.html
      final CodeableConcept codeableConcept = new CodeableConcept();
      if ( !sectionId.equals( SIMPLE_SECTION ) ) {
         // The section can also be used as a category.
         codeableConcept.addCoding( new Coding( CTAKES_SECTION_URL, sectionId, section.getPreferredText() ) );
      }
      return codeableConcept;
   }


//   static private CodeableConcept createBodySiteQualifier( final AnatomicalSiteMention annotation ) {
//      // laterality and bodyside are never actually set in ctakes.
//      // The proximity of the location in anatomical terms (distal, proximal, superior, anterior and etc.
//      // This is not an appropriate definition.
//      final BodyLateralityModifier laterality = annotation.getBodyLaterality();
//      //left, right, bilateral
//      final BodySideModifier bodySide = annotation.getBodySide();
//      // use snomed codes such as https://www.hl7.org/fhir/valueset-bodysite-relative-location.html
//   }

   /**
    * @param annotation -
    * @param typeCounts map of type names and counts for each type
    * @return
    */
   static private String createAnnotationId( final IdentifiedAnnotation annotation, final Map<String, Integer> typeCounts ) {
      final String typeName = annotation.getClass().getSimpleName();
      final int count = typeCounts.getOrDefault( typeName, 0 ) + 1;
      typeCounts.put( typeName, count );
      return typeName + '_' + count;
   }

   static private void addAssertionExtension( final IdentifiedAnnotation annotation, final DomainResource fhirEvent ) {
      final Extension assertion = createAssertion( annotation );
      fhirEvent.addExtension( assertion );
   }

   static private Extension createAssertion( final IdentifiedAnnotation annotation ) {
      String assertion = ASSERTION_AFFIRMED;
      if ( annotation.getGeneric() ) {
         assertion = ASSERTION_GENERIC;
      } else if ( annotation.getUncertainty() > 0 ) {
         if ( annotation.getPolarity() < 0 ) {
            assertion = ASSERTION_UNCERT_NEGATED;
         } else {
            assertion = ASSERTION_UNCERTAIN;
         }
      } else if ( annotation.getPolarity() < 0 ) {
         assertion = ASSERTION_NEGATED;
      }
      return new Extension( CTAKES_ASSERTION_URL, new StringType( assertion ) );
   }


   static private void addDocTimeRelExtension( final EventMention eventMention, final DomainResource fhirEvent ) {
      final Extension dtr = createDocTimeRel( eventMention );
      if ( dtr != null ) {
         fhirEvent.addExtension( dtr );
      }
   }

   static private Extension createDocTimeRel( final EventMention eventMention ) {
      final Event event = eventMention.getEvent();
      if ( event == null ) {
         return null;
      }
      final EventProperties eventProperties = event.getProperties();
      if ( eventProperties == null ) {
         return null;
      }
      final String dtr = eventProperties.getDocTimeRel();
      if ( dtr == null || dtr.isEmpty() ) {
         return null;
      }
      return new Extension( DTR_EXT_URL, new StringType( dtr ) );
   }


}
