package org.apache.ctakes.fhir.cc;


import org.apache.log4j.Logger;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Practitioner;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/21/2017
 */
final public class FhirConstantsFactory {

   static private final Logger LOGGER = Logger.getLogger( "FhirConstantsFactory" );

   private FhirConstantsFactory() {
   }


   /**
    * https://www.hl7.org/fhir/practitioner.html
    * Even though ctakes is not human, registering it as a Practitioner provides reference to information source and devlist contact
    *
    * @return Practitioner representing ctakes as the creator/extractor of data
    */
   static public Practitioner createCtakesPractitioner() {
      final HumanName name = new HumanName();
      name.setUse( HumanName.NameUse.OFFICIAL );
      name.setText( "Apache cTAKES" );
      name.setFamily( "Apache" );
      name.addGiven( "cTAKES" );
      final ContactPoint devlist = new ContactPoint();
      devlist.setSystem( ContactPoint.ContactPointSystem.EMAIL );
      devlist.setValue( "dev@ctakes.apache.org" );
      devlist.setUse( ContactPoint.ContactPointUse.WORK );
      final Practitioner ctakes = new Practitioner();
      ctakes.setActive( true );
      ctakes.addName( name );
      ctakes.addTelecom( devlist );
      return ctakes;
   }


}
