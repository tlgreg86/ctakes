//package org.apache.ctakes.fhir.util;
//
//import org.apache.ctakes.fhir.cc.FhirDatatypeFactory;
//import org.apache.ctakes.typesystem.type.constants.CONST;
//import org.apache.ctakes.typesystem.type.textsem.EventMention;
//import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
//import org.apache.uima.jcas.JCas;
//import org.hl7.fhir.dstu3.model.*;
//
//import java.util.*;
//import java.util.logging.Logger;
//
///**
// * @author SPF , chip-nlp
// * @version %I%
// * @since 12/19/2017
// */
//final public class FhirBasicIaFactory { //implements ca.uhn.fhir.rest.server.IResourceProvider {
//
//   static private final Logger LOGGER = Logger.getLogger( "FhirBasicIaFactory" );
//
//   private FhirBasicIaFactory() {}
//
//   static private final String CTAKES_FHIR_URL = "http://org.apache.ctakes/fhir/";
//   static private final String GENERIC_EXT = "generic";
//   static private final String UNCERTAIN_EXT = "uncertain";
//   static private final String NEGATED_EXT = "negated";
//   static private final String HISTORIC_EXT = "historic";
//
//
//   static public Basic createIaBasic( final JCas jCas,
//                                      final IdentifiedAnnotation annotation,
//                                      final Map<String,BaseResource> subjects,
//                                      final Period period,
//                                      final Reference ctakes ) {
//      final Basic basic = new Basic();
//      // The 'id' is name of the Resource type (class).  e.g. DiseaseDisorderMention
//      final String type = annotation.getType().getShortName();
//      basic.setId( type );
//      // The 'identifier' is unique for the Resource (object).
//      final Identifier identifier = FhirDatatypeFactory.createIdentifier( jCas, annotation, period );
//      basic.addIdentifier( identifier );
//      // The 'code' is the full ontology concept array: cuis, snomeds, urls, preferred text, PLUS covered text.
//      basic.setCode( FhirDatatypeFactory.createPrimaryCode( annotation ) );
//      // Add Subject reference.
//      basic.setSubject( FhirDatatypeFactory.createSubjectReference( annotation.getSubject(), subjects ) );
//      // Add Creation Date as now.
//      basic.setCreated( new Date() );
//      // Add Author (ctakes).
//      basic.setAuthor( ctakes );
//      // Add text span as an extension.
//      basic.addExtension( FhirDatatypeFactory.createTextSpan( annotation ) );
//      // Add DocTimeRel as an extension.
//      if ( annotation instanceof EventMention ) {
//         final Extension dtr = FhirDatatypeFactory.createDocTimeRel( (EventMention)annotation );
//         if ( dtr != null ) {
//            basic.addExtension( dtr );
//         }
//      }
//      // Add generic, uncertainty, negation as modifier extensions.
//      if ( annotation.getGeneric() ) {
//         basic.addModifierExtension( new Extension( CTAKES_FHIR_URL+GENERIC_EXT, new BooleanType( true ) ) );
//      }
//      if ( annotation.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT ) {
//         basic.addModifierExtension( new Extension( CTAKES_FHIR_URL+UNCERTAIN_EXT, new BooleanType( true ) ) );
//      }
//      if ( annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT ) {
//         basic.addModifierExtension( new Extension( CTAKES_FHIR_URL+NEGATED_EXT, new BooleanType( true ) ) );
//      }
//      // Add history of as a modifier extension.
//      if ( annotation.getHistoryOf() == CONST.NE_HISTORY_OF_PRESENT ) {
//         basic.addModifierExtension( new Extension( CTAKES_FHIR_URL+HISTORIC_EXT, new BooleanType( true ) ) );
//      }
//      return basic;
//   }
//
//   static public Basic createAbasic( final JCas jCas,
//                                      final org.apache.uima.jcas.tcas.Annotation annotation,
//                                      final Period period,
//                                      final Reference ctakes ) {
//      final Basic basic = new Basic();
//      // The 'id' is name of the Resource type (class).  e.g. DiseaseDisorderMention
//      final String type = annotation.getType().getShortName();
//      basic.setId( type );
//      // The 'identifier' is unique for the Resource (object).
//      final Identifier identifier = FhirDatatypeFactory.createIdentifier( jCas, annotation, period );
//      basic.addIdentifier( identifier );
//      // The 'code' is the full ontology concept array: cuis, snomeds, urls, preferred text, PLUS covered text.
//      basic.setCode( FhirDatatypeFactory.createSimpleCode( annotation ) );
//      // Add Creation Date as now.
//      basic.setCreated( new Date() );
//      // Add Author (ctakes).
//      basic.setAuthor( ctakes );
//      // Add text span as an extension.
//      basic.addExtension( FhirDatatypeFactory.createTextSpan( annotation ) );
//      return basic;
//   }
//
//}
