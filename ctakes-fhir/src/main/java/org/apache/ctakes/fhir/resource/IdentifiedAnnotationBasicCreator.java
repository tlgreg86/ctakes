package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.cc.FhirElementFactory;
import org.apache.ctakes.fhir.util.NoteSpecs;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.Extension;

import java.util.Date;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class IdentifiedAnnotationBasicCreator implements FhirResourceCreator<IdentifiedAnnotation, Basic> {

   static private final Logger LOGGER = Logger.getLogger( "IdentifiedAnnotationBasicCreator" );

   static private final String GENERIC_EXT = "generic";
   static private final String UNCERTAIN_EXT = "uncertain";
   static private final String NEGATED_EXT = "negated";
   static private final String HISTORIC_EXT = "historic";

   /**
    * {@inheritDoc}
    */
   @Override
   public Basic createResource( final JCas jCas, final IdentifiedAnnotation annotation, final NoteSpecs noteSpecs ) {
      final Basic basic = new Basic();
      // The 'id' is name of the Resource type (class).  e.g. DiseaseDisorderMention
      basic.setId( FhirElementFactory.createId( jCas, annotation ) );
      // The 'code' is the full ontology concept array: cuis, snomeds, urls, preferred text, PLUS covered text.
      basic.setCode( FhirElementFactory.createPrimaryCode( annotation ) );
      // Add Subject reference.
      basic.setSubject( noteSpecs.getSubjectReference( annotation.getSubject() ) );
      // Add Creation Date as now.
      basic.setCreated( new Date() );
      // Add Author (ctakes).
      basic.setAuthor( PractitionerCtakes.getPractitionerReference() );
      // Add text span as an extension.
      basic.addExtension( FhirElementFactory.createSpanBegin( annotation ) );
      basic.addExtension( FhirElementFactory.createSpanEnd( annotation ) );
      // Add DocTimeRel as an extension.
      if ( annotation instanceof EventMention ) {
         final Extension dtr = FhirElementFactory.createDocTimeRel( (EventMention) annotation );
         if ( dtr != null ) {
            basic.addExtension( dtr );
         }
      }
      // Add generic, uncertainty, negation as modifier extensions.
      if ( annotation.getGeneric() ) {
         basic.addModifierExtension( FhirElementFactory.createTrueExtension( GENERIC_EXT ) );
      }
      if ( annotation.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT ) {
         basic.addModifierExtension( FhirElementFactory.createTrueExtension( UNCERTAIN_EXT ) );
      }
      if ( annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT ) {
         basic.addModifierExtension( FhirElementFactory.createTrueExtension( NEGATED_EXT ) );
      }
      // Add history of as a modifier extension.
      if ( annotation.getHistoryOf() == CONST.NE_HISTORY_OF_PRESENT ) {
         basic.addModifierExtension( FhirElementFactory.createTrueExtension( HISTORIC_EXT ) );
      }
      return basic;
   }

}
