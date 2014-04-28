package org.apache.ctakes.ytex.uima.annotators;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.ytex.uima.model.NamedEntityRegex;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Assert;
import org.junit.Test;
import org.uimafit.factory.JCasFactory;

/**
 * TODO get rid of hard-coded path to Types.xml - load from classpath
 * @author vgarla
 *
 */
public class NamedEntityRegexTest {

	/**
	 * Verify that date parsing with a manually created date works
	 * @throws UIMAException
	 * @throws Exception 
	 */
	@Test
	public void testRegex() throws UIMAException, IOException {
		NamedEntityRegexAnnotator ner = new NamedEntityRegexAnnotator();
		NamedEntityRegex nerex = new NamedEntityRegex();
		nerex.setCode("C00TEST");
		nerex.setContext("DEFAULT");
		nerex.setRegex("(?i)COUGH");
		ner.initRegexMap(Arrays.asList(nerex));
		
	    JCas jCas = JCasFactory.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
	    String text = "Dr. Doolitle asked patient\nto take a deep breath\nand exhale slowly.  Patient coughed.";
	    jCas.setDocumentText(text);
	    Segment s = new Segment(jCas);
	    s.setBegin(0);
	    s.setEnd(text.length());
	    s.setId("DEFAULT");
	    s.addToIndexes();
	    ner.process(jCas);
	    // run the analysis engine
	    AnnotationIndex<Annotation> mentions = jCas.getAnnotationIndex(EntityMention.type);
		Iterator<Annotation> iter =mentions.iterator();
		int emCount = 0;
		while(iter.hasNext()) {
			EntityMention em = (EntityMention)iter.next();
			System.out.println("[" + em.getCoveredText() + "]");
			emCount ++;
		}
		Assert.assertTrue(emCount == 1);
	}
}
