package org.apache.ctakes.fhir.resource;


import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * https://www.hl7.org/fhir/practitioner.html
 * Even though ctakes is not human, registering it as a Practitioner provides reference to information source and devlist contact
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
public enum PractitionerCtakes {
   INSTANCE;

   static private final String CTAKES_VERSION = "4_0_1";

   /**
    * @return Practitioner representing ctakes as the creator/extractor of data
    */
   static public Practitioner getPractitioner() {
      return INSTANCE._ctakes;
   }

   /**
    * @return Reference to a Practitioner representing ctakes as the creator/extractor of data
    */
   static public Reference getPractitionerReference() {
      return INSTANCE._ctakesReference;
   }

   final private Practitioner _ctakes;
   final private Reference _ctakesReference;

   PractitionerCtakes() {
      final HumanName name = new HumanName();
      name.setUse( HumanName.NameUse.OFFICIAL );
      name.setFamily( "Apache" );
      name.addGiven( "cTAKES" );
      final ContactPoint devlist = new ContactPoint();
      devlist.setSystem( ContactPoint.ContactPointSystem.EMAIL );
      devlist.setValue( "dev@ctakes.apache.org" );
      devlist.setUse( ContactPoint.ContactPointUse.WORK );
      _ctakes = new Practitioner();
      String hostname = "UnknownHost";
      try {
         hostname = InetAddress.getLocalHost().getHostName();
      } catch ( UnknownHostException uhE ) {
         hostname = "UnknownHost";
      }
      _ctakes.setId( "Apache_cTAKES_" + CTAKES_VERSION + "_" + hostname );
      _ctakes.setActive( true );
      _ctakes.addName( name );
      _ctakes.addTelecom( devlist );
      _ctakesReference = new Reference( _ctakes );
   }

}
