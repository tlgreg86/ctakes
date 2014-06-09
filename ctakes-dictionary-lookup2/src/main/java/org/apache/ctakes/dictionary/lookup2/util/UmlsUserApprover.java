/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.ctakes.utils.env.EnvironmentVariable;


/**
 * Used to validate UMLS license / user.
 *
 * TODO  Authentication before download would be nice, or perhaps an encrypted download
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 2/19/14
 */
final public class UmlsUserApprover {


   private final static String UMLSADDR_PARAM = "ctakes.umlsaddr";
   private final static String UMLSVENDOR_PARAM = "ctakes.umlsvendor";
   private final static String UMLSUSER_PARAM = "ctakes.umlsuser";
   private final static String UMLSPW_PARAM = "ctakes.umlspw";

   static final private Logger LOGGER = Logger.getLogger( "UmlsUserApprover" );

   private UmlsUserApprover() {}

   /**
    * Silently validate the UMLS license / user
    * @param aContext contains information about the UMLS license / user
    * @throws ResourceInitializationException if the validation does not pass
    */
   static public void validateUMLSUser( final UimaContext aContext ) throws ResourceInitializationException {
      final String umlsAddress = EnvironmentVariable.getEnv( UMLSADDR_PARAM, aContext );
      final String umlsVendor = EnvironmentVariable.getEnv( UMLSVENDOR_PARAM, aContext );
      final String umlsUser = EnvironmentVariable.getEnv( UMLSUSER_PARAM, aContext );
      final String umlsPassword = EnvironmentVariable.getEnv( UMLSPW_PARAM, aContext );
      LOGGER.info( "Using " + UMLSADDR_PARAM + ": " + umlsAddress + ": " + umlsUser );
      if ( !isValidUMLSUser( umlsAddress, umlsVendor, umlsUser, umlsPassword ) ) {
         LOGGER.error( "Error: Invalid UMLS License.  " +
                        "A UMLS License is required to use the UMLS dictionary lookup. \n" +
                        "Error: You may request one at: https://uts.nlm.nih.gov/license.html \n" +
                        "Please verify your UMLS license settings in the " +
                        "DictionaryLookupAnnotatorUMLS.xml configuration." );
         throw new ResourceInitializationException( new Exception( "Failed to initilize.  Invalid UMLS License" ) );
      }
   }

   /**
    * @param umlsaddr -
    * @param vendor   -
    * @param username -
    * @param password -
    * @return true if the server at umlsaddr approves of the vendor, user, password combination
    */
   public static boolean isValidUMLSUser( final String umlsaddr, final String vendor,
                                          final String username, final String password ) {
      String data;
      try {
         data = URLEncoder.encode( "licenseCode", "UTF-8" ) + "=" + URLEncoder.encode( vendor, "UTF-8" );
         data += "&" + URLEncoder.encode( "user", "UTF-8" ) + "=" + URLEncoder.encode( username, "UTF-8" );
         data += "&" + URLEncoder.encode( "password", "UTF-8" ) + "=" + URLEncoder.encode( password, "UTF-8" );
      } catch ( UnsupportedEncodingException unseE ) {
         LOGGER.error( "Could not encode URL for " + username + " with vendor license " + vendor );
         return false;
      }
      try {
         final URL url = new URL( umlsaddr );
         final URLConnection connection = url.openConnection();
         connection.setDoOutput( true );
         final OutputStreamWriter writer = new OutputStreamWriter( connection.getOutputStream() );
         writer.write( data );
         writer.flush();
         boolean result = false;
         final BufferedReader reader = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
         String line;
         while ( (line = reader.readLine()) != null ) {
            final String trimline = line.trim();
            if ( trimline.isEmpty() ) {
               break;
            }
            result = trimline.equalsIgnoreCase( "<Result>true</Result>" );
         }
         writer.close();
         reader.close();
         return result;
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
   }


}
