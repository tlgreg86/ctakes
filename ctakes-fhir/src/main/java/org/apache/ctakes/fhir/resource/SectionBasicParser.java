package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.fhir.element.FhirElementParser;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

import java.util.List;

import static org.apache.ctakes.fhir.resource.SectionBasicCreator.CODING_SECTION_ID;
import static org.apache.ctakes.fhir.resource.SectionBasicCreator.CODING_SECTION_NAME;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
final public class SectionBasicParser implements FhirResourceParser<Segment, Basic> {

   static private final Logger LOGGER = Logger.getLogger( "SectionBasicParser" );

   public Segment parseResource( final JCas jCas, final Basic resource ) {
      final CodeableConcept codeableConcept = resource.getCode();
      final List<Coding> codings = codeableConcept.getCoding();
      String preferredText = "";
      String id = "";
      for ( Coding coding : codings ) {
         final String system = coding.getSystem();
         if ( system.equals( CODING_SECTION_NAME ) ) {
            preferredText = coding.getCode();
         } else if ( system.equals( CODING_SECTION_ID ) ) {
            id = coding.getCode();
         }
      }
      final String tagText = codeableConcept.getText();
      final Pair<Integer> textSpan = FhirElementParser.getTextSpan( resource.getExtension() );
      if ( textSpan == null ) {
         LOGGER.error( "Could not parse text span for section " + preferredText );
         return null;
      }
      final Segment segment = new Segment( jCas );
      segment.setBegin( textSpan.getValue1() );
      segment.setEnd( textSpan.getValue2() );
      segment.setPreferredText( preferredText );
      segment.setTagText( tagText );
      if ( !id.isEmpty() ) {
         segment.setId( id );
      }
      return segment;
   }

}
