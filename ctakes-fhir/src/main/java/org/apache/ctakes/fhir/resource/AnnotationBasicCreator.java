package org.apache.ctakes.fhir.resource;


import org.apache.ctakes.fhir.cc.FhirElementFactory;
import org.apache.ctakes.fhir.util.NoteSpecs;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.Basic;

import java.util.Date;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class AnnotationBasicCreator implements FhirResourceCreator<Annotation, Basic> {

   static private final Logger LOGGER = Logger.getLogger( "AnnotationBasicCreator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public Basic createResource( final JCas jCas, final Annotation annotation, final NoteSpecs noteSpecs ) {
      final Basic basic = new Basic();
      // The 'id' is name of the Resource type (class).  e.g. DiseaseDisorderMention
      basic.setId( FhirElementFactory.createId( jCas, annotation ) );
      // The 'code' is the full ontology concept array: cuis, snomeds, urls, preferred text, PLUS covered text.
      basic.setCode( FhirElementFactory.createSimpleCode( annotation ) );
      // Add Creation Date as now.
      basic.setCreated( new Date() );
      // Add Author (ctakes).
      basic.setAuthor( PractitionerCtakes.getPractitionerReference() );
      // Add text span as an extension.
      basic.addExtension( FhirElementFactory.createSpanBegin( annotation ) );
      basic.addExtension( FhirElementFactory.createSpanEnd( annotation ) );
      return basic;
   }

}
