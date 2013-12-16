package org.apache.ctakes.ytex.uima.annotators;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.Assert;
import org.junit.Test;
import org.uimafit.factory.AggregateBuilder;

/**
 * TODO get rid of hard-coded path to Types.xml - load from classpath
 * @author vgarla
 *
 */
public class SentenceDetectorTest {

	/**
	 * Verify that date parsing with a manually created date works
	 * @throws UIMAException
	 * @throws Exception 
	 */
	@Test
	public void testSentenceDetection() throws UIMAException, IOException {
//	    JCas jCas = JCasFactory.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
	    String text = "Dr. Doolitle asked patient\nto take a deep breath\nand exhale slowly.  Patient coughed.";
	    AggregateBuilder builder = new AggregateBuilder();
	    File directoryCtakes = new File("../ctakes-core/desc/analysis_engine");
	    File fileCtakes = new File(directoryCtakes, "TokenizerAnnotator.xml");
	    XMLParser parser = UIMAFramework.getXMLParser();
	    XMLInputSource source = new XMLInputSource(fileCtakes);
	    builder.add(parser.parseAnalysisEngineDescription(source));
	    File directory = new File("desc/analysis_engine");
	    File file = new File(directory, "SentenceDetectorAnnotator.xml");
	    source = new XMLInputSource(file);
	    builder.add(parser.parseAnalysisEngineDescription(source));
	    AnalysisEngine engine = builder.createAggregate();
	    JCas jCas = engine.newJCas();
	    jCas.setDocumentText(text);
	    Segment s = new Segment(jCas);
	    s.setBegin(0);
	    s.setEnd(text.length());
	    s.setId("DEFAULT");
	    s.addToIndexes();
	    // run the analysis engine
	    engine.process(jCas);
	    AnnotationIndex<Annotation> sentences = jCas.getAnnotationIndex(Sentence.type);
		Iterator<Annotation> iter =sentences.iterator();
		int sentCount = 0;
		while(iter.hasNext()) {
			Sentence sent = (Sentence)iter.next();
			System.out.println("[" + sent.getCoveredText() + "]");
			sentCount ++;
		}
		Assert.assertTrue(sentCount == 2);
	}
}
