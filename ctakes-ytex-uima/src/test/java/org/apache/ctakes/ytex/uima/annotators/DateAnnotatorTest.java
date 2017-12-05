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
package org.apache.ctakes.ytex.uima.annotators;

import com.ibm.icu.text.SimpleDateFormat;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.ytex.uima.types.Date;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Assert;
import org.junit.Test;
import org.apache.uima.fit.factory.JCasFactory;
import sun.java2d.pipe.SpanShapeRenderer;

import java.net.URL;
import java.text.ParseException;

import static org.junit.Assert.*;

/**
 * TODO get rid of hard-coded path to Types.xml - load from classpath
 * @author vgarla
 *
 */
public class DateAnnotatorTest {

	private final static Logger LOGGER = Logger.getLogger(DateAnnotatorTest.class);

	/**
	 * Verify that date parsing with a manually created date works
	 * @throws UIMAException
	 */
	@Test
	public void testParseDate() throws UIMAException {
		URL urlTypeSystem = getClass().getClassLoader().getResource("org/apache/ctakes/ytex/types/TypeSystem.xml");
	    JCas jCas = JCasFactory.createJCasFromPath(urlTypeSystem.getPath());

	    java.util.Date  dtExpected = new java.util.Date();
	    String sExpected = dtExpected.toString();

	    LOGGER.info(String.format("date to be annotated: %s", sExpected));
	    jCas.setDocumentText(sExpected);

	    // create the annotation
	    DateAnnotation ctakesDate = new DateAnnotation(jCas);
	    ctakesDate.setBegin(0);
	    ctakesDate.setEnd(sExpected.length());
	    ctakesDate.addToIndexes();
	    DateAnnotator dateAnnotator = new DateAnnotator();
	    dateAnnotator.dateType = Date.class.getName();
	    dateAnnotator.process(jCas);
	    AnnotationIndex<Annotation> ytexDates = jCas.getAnnotationIndex(Date.type);
	    assertTrue(ytexDates.iterator().hasNext());

	    // return the parsed
	    String sParsed = ((Date)ytexDates.iterator().next()).getDate();
		assertNotNull("Expected a parsed Date string", sParsed);
		LOGGER.info(String.format("date from annotation: %s", sParsed));
		java.util.Date dtParsed = null;
	    try {
		    dtParsed = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(sParsed);
	    } catch (ParseException e) {
	    	assertFalse("Expected a real java.util.Date object", true);
	    }
	    // java.util.Date.equals is not advised. Comparing Date.getTime ignores the miliseconds.
		assertNotNull("Expected a not NULL java.util.Date object", dtParsed);
	    assertTrue("Expected what we converted .toString to be parsed",
			       (dtExpected.getTime() - dtParsed.getTime()) < 1000 /*1 second*/);
	}

}
