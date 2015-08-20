package org.apache.ctakes.dictionary.lookup2.ae;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Test;

public class TestDictionaryLoadResources {

	@Test
	public void test() throws Exception {
		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescription();		
		JCas jcas = JCasFactory.createJCas();
		jcas.setDocumentText("The quick red fox jumped over cTAKES.  Allie had a little lamb; little lamb.");
		//Test had to use custom test config otherwise we'll have to save our umls credentials.
		AnalysisEngineDescription aed = DictionaryLookupFactory
				.createCustomDictionaryLookupDescription("org/apache/ctakes/dictionary/lookup/fast/TestcTakesHsql.xml");
		SimplePipeline.runPipeline(jcas,aed);
	}

}
