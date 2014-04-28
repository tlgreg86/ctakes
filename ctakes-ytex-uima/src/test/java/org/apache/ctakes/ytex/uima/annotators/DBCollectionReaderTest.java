package org.apache.ctakes.ytex.uima.annotators;

import java.io.IOException;

import org.apache.ctakes.ytex.uima.TestUtils;
import org.apache.ctakes.ytex.uima.types.DocKey;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.Test;
import org.uimafit.factory.JCasFactory;
import org.xml.sax.SAXException;

public class DBCollectionReaderTest {

	@Test
	public void test() throws IOException,
			SAXException, CpeDescriptorException, UIMAException, CASRuntimeException, CASAdminException {
		CollectionReader colReader = TestUtils.getFractureDemoCollectionReader();
		int count = 0;
		JCas jcas = JCasFactory.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
		while(colReader.hasNext()) {
			count++;
			colReader.getNext(jcas.getCas());
			Assert.assertTrue("document should have a dockey", jcas.getAnnotationIndex(DocKey.type).iterator().hasNext());
			jcas.reset();
		}
		Assert.assertTrue("should have read some documents", count > 0);
	}



}
