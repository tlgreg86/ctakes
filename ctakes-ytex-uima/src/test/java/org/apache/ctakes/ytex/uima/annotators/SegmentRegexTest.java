package org.apache.ctakes.ytex.uima.annotators;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.ytex.uima.model.NamedEntityRegex;
import org.apache.ctakes.ytex.uima.model.SegmentRegex;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Assert;
import org.junit.Test;
import org.uimafit.factory.JCasFactory;

/**
 * TODO get rid of hard-coded path to Types.xml - load from classpath
 * 
 * @author vgarla
 * 
 */
public class SegmentRegexTest {

	/**
	 * Verify that date parsing with a manually created date works
	 * 
	 * @throws UIMAException
	 * @throws Exception
	 */
	@Test
	public void testRegex() throws UIMAException, IOException {
		SegmentRegexAnnotator ner = new SegmentRegexAnnotator();
		SegmentRegex segTitle = new SegmentRegex();
		segTitle.setRegex("(?i)title:\\s*(.*)\\n");
		segTitle.setLimitToRegex(true);
		segTitle.setSegmentID("TITLE");
		segTitle.setSegmentRegexID(1);
		SegmentRegex segHistory = new SegmentRegex();
		segHistory.setRegex("(?i)history:");
		segHistory.setLimitToRegex(false);
		segHistory.setSegmentID("HISTORY");
		segHistory.setSegmentRegexID(2);
		ner.initRegexMap(Arrays.asList(segTitle, segHistory));

		JCas jCas = JCasFactory
				.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
		String text = "Title: US Left Upper Quadrant\nDr. Doolitle asked patient\nto take a deep breath\nand exhale slowly.  \nHistory: Patient coughed.";
		jCas.setDocumentText(text);
		ner.process(jCas);
		// run the analysis engine
		AnnotationIndex<Annotation> mentions = jCas
				.getAnnotationIndex(Segment.type);
		Iterator<Annotation> iter = mentions.iterator();
		Set<String> uniqueSegments = new HashSet<String>();
		int emCount = 0;
		while (iter.hasNext()) {
			Segment em = (Segment) iter.next();
			uniqueSegments.add(em.getId());
			System.out.println(em.getId() + " [" + em.getCoveredText() + "]");
			emCount++;
		}
		// should be 4 segments - default/title/default/history
		Assert.assertTrue(emCount == 4);
		// should be 3 unique segments
		Assert.assertTrue(uniqueSegments.size() == 3);
	}
}
