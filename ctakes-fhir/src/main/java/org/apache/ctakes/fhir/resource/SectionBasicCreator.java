package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

import java.util.Date;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class SectionBasicCreator implements FhirResourceCreator<Segment, Basic> {

   static private final Logger LOGGER = Logger.getLogger( "SectionBasicCreator" );

   static public final String CODING_SECTION_NAME = "section-name";
   static public final String CODING_SECTION_ID = "section-id";

   static public final String SECTION_EXT = "document-section";
   static public final String ID_NAME_SECTION = "DocumentSection";

   /**
    * {@inheritDoc}
    */
   @Override
   public Basic createResource( final JCas jCas, final Segment section, final FhirPractitioner practitioner,
                                final FhirNoteSpecs noteSpecs ) {
      final Basic basic = new Basic();
      // The 'id' is name of the Resource type (class).  e.g. DiseaseDisorderMention
      basic.setId( FhirElementFactory.createId( jCas, ID_NAME_SECTION, section.hashCode() ) );
      // The 'code' is the normalized section name PLUS section tag text.
      final CodeableConcept codeableConcept = FhirElementFactory.createSimpleCode( section );
      codeableConcept.addCoding( new Coding( CODING_SECTION_NAME, section.getPreferredText(), null ) );
      if ( !section.getPreferredText()
            .equals( section.getId() ) ) {
         codeableConcept.addCoding( new Coding( CODING_SECTION_ID, section.getId(), null ) );
      }
      codeableConcept.setText( section.getTagText() );
      basic.setCode( codeableConcept );
      // Add Creation Date as now.
      basic.setCreated( new Date() );
      // Add Author (ctakes).
      basic.setAuthor( practitioner.getPractitionerReference() );
      // Add text span as an extension.
      basic.addExtension( FhirElementFactory.createSpanBegin( section ) );
      basic.addExtension( FhirElementFactory.createSpanEnd( section ) );
      return basic;
   }

}
