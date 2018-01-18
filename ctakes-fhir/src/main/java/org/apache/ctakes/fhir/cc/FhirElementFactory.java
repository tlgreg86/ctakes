package org.apache.ctakes.fhir.cc;


import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.fhir.resource.SectionBasicCreator;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.util.Collection;
import java.util.Date;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/22/2017
 */
final public class FhirElementFactory {

   static private final Logger LOGGER = Logger.getLogger( "FhirDatatypeFactory" );

   static public final String CTAKES_FHIR_URL = "http://org.apache.ctakes/fhir/";
   static private final String IDENTIFIER_EXT = "identifier";
   static private final String SPAN_BEGIN_EXT = "span-begin";
   static private final String SPAN_END_EXT = "span-end";
   static private final String DOCTIMEREL_EXT = "doc-time-rel";
   static private final String COREF_INDEX_EXT = "coreference-index";

   static private final String CTAKES_TYPE_SYSTEM = "ctakes-type-system";


   private FhirElementFactory() {
   }

   /**
    * @param annotation -
    * @return all ontology codes and covered text in a codeable concept
    */
   static public CodeableConcept createPrimaryCode( final IdentifiedAnnotation annotation ) {
      final CodeableConcept codeableConcept = createSimpleCode( annotation );
      final Collection<String> cuis = OntologyConceptUtil.getCuis( annotation );
      cuis.forEach( c -> codeableConcept.addCoding( new Coding( "CUI", c, "" ) ) );
      final Collection<String> tuis = OntologyConceptUtil.getTuis( annotation );
      tuis.forEach( t -> codeableConcept.addCoding( new Coding( "TUI", t, "" ) ) );
      for ( OntologyConcept concept : OntologyConceptUtil.getOntologyConcepts( annotation ) ) {
         final String scheme = concept.getCodingScheme();
         final String code = concept.getCode();
         String preferredText = null;
         if ( concept instanceof UmlsConcept ) {
            preferredText = ((UmlsConcept) concept).getPreferredText();
         }
         codeableConcept.addCoding( new Coding( scheme, code, preferredText ) );
      }
      return codeableConcept;
   }

   /**
    * @param annotation -
    * @return all ontology codes and covered text in a codeable concept
    */
   static public CodeableConcept createSimpleCode( final org.apache.uima.jcas.tcas.Annotation annotation ) {
      final CodeableConcept codeableConcept = new CodeableConcept();
      final String type = annotation.getType().getShortName();
      codeableConcept.addCoding( new Coding( CTAKES_TYPE_SYSTEM, type, null ) );
      codeableConcept.setText( annotation.getCoveredText() );
      return codeableConcept;
   }

   static public String createId( final JCas jCas, final org.apache.uima.jcas.tcas.Annotation annotation ) {
      return createId( jCas, annotation.getType().getShortName(), annotation.hashCode() );
   }

   static public String createId( final JCas jCas, final String name ) {
      return createId( jCas, name, name.hashCode() );
   }

   static public String createId( final JCas jCas, final String name, final int code ) {
      return createId( jCas, name, "" + Math.abs( code ) );
   }

   static public String createId( final JCas jCas, final String name, final String code ) {
      return DocumentIDAnnotationUtil.getDocumentID( jCas ) + "_" + name + "_" + code;
   }

//   static public Identifier createIdentifier( final JCas jCas,
//                                              final org.apache.uima.jcas.tcas.Annotation annotation,
//                                              final Period period ) {
//      return createIdentifier( jCas, annotation.getType().getShortName(), annotation.hashCode(), period );
//   }
//
//   static public Identifier createIdentifier( final JCas jCas,
//                                              final String name,
//                                              final Period period ) {
//      return createIdentifier( jCas, name, name.hashCode(), period );
//   }
//
//   static public Identifier createIdentifier( final JCas jCas,
//                                              final String name,
//                                              final int code,
//                                              final Period period ) {
//      return createIdentifier( jCas, name, ""+Math.abs( code ), period );
//   }
//
//   static public Identifier createIdentifier( final JCas jCas,
//                                              final String name,
//                                              final String code,
//                                              final Period period ) {
//      final String id = DocumentIDAnnotationUtil.getDocumentID( jCas )
//            + "_" + name
//            + "_" + code;
//      final Identifier identifier = new Identifier();
//      identifier.setSystem( CTAKES_FHIR_URL + IDENTIFIER_EXT );
//      identifier.setUse( Identifier.IdentifierUse.SECONDARY );
//      identifier.setValue( id );
//      identifier.setPeriod( period );
//      return identifier;
//   }

   /**
    * @param number some integer
    * @return given integer wrapped in a fhir SimpleQuantity object
    */
   static public SimpleQuantity createQuantity( final int number ) {
      final SimpleQuantity quantity = new SimpleQuantity();
      quantity.setValue( number );
      return quantity;
   }

   static public Period createPeriod( final long startMillis, final long endMillis ) {
      final Period period = new Period();
      period.setStart( new Date( startMillis ), TemporalPrecisionEnum.MILLI );
      period.setEnd( new Date( endMillis ), TemporalPrecisionEnum.MILLI );
      return period;
   }

   static public Narrative createNarrative( final String text ) {
      final Narrative narrative = new Narrative();
      narrative.setStatus( Narrative.NarrativeStatus.GENERATED );
      final XhtmlNode htmlNode = new XhtmlNode( NodeType.Element, "div" );
      htmlNode.addText( text );
      narrative.setDiv( htmlNode );
      return narrative;
   }

   static public Extension createSpanBegin( final org.apache.uima.jcas.tcas.Annotation annotation ) {
      final UnsignedIntType begin = new UnsignedIntType( annotation.getBegin() );
      return new Extension( CTAKES_FHIR_URL + SPAN_BEGIN_EXT, begin );
   }

   static public Extension createSpanEnd( final org.apache.uima.jcas.tcas.Annotation annotation ) {
      final UnsignedIntType end = new UnsignedIntType( annotation.getEnd() );
      return new Extension( CTAKES_FHIR_URL + SPAN_END_EXT, end );
   }

   static public Extension createDocTimeRel( final EventMention eventMention ) {
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
      return new Extension( CTAKES_FHIR_URL + DOCTIMEREL_EXT, new StringType( dtr ) );
   }

   static public Extension createRelation( final String name, final Basic target ) {
      return new Extension( CTAKES_FHIR_URL + name, new Reference( target ) );
   }

   static public Extension createCorefIndex( final int index ) {
      return new Extension( CTAKES_FHIR_URL + COREF_INDEX_EXT, new UnsignedIntType( index ) );
   }

   static public Extension createSectionExtension( final Reference sectionRef ) {
      return new Extension( CTAKES_FHIR_URL + SectionBasicCreator.SECTION_TYPE_NAME, sectionRef );
   }

   static public Extension createTrueExtension( final String name ) {
      return new Extension( CTAKES_FHIR_URL + name, new BooleanType( true ) );
   }

}
