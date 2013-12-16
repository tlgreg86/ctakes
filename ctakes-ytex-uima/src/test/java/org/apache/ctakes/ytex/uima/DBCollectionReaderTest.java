package org.apache.ctakes.ytex.uima;

import java.io.IOException;

import org.apache.ctakes.ytex.dao.DBUtil;
import org.apache.ctakes.ytex.uima.types.DocKey;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.Test;
import org.uimafit.factory.JCasFactory;

public class DBCollectionReaderTest {

	@Test
	public void testGetNext() throws UIMAException, IOException {

		JCas jCas = JCasFactory
				.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
		DBCollectionReader reader = new DBCollectionReader();
		reader.initDB(null, null);
		reader.queryGetDocument = String
				.format("select note_text from %sfracture_demo where note_id = :instance_id",
						DBUtil.getYTEXTablePrefix());
		reader.queryGetDocumentKeys = String.format(
				"select note_id instance_id from %sfracture_demo",
				DBUtil.getYTEXTablePrefix());
		reader.loadDocumentIds();
		reader.getNext(jCas);
		Assert.assertNotNull(jCas.getDocumentText());
		Assert.assertTrue(jCas.getAnnotationIndex(DocKey.type).iterator()
				.hasNext());

	}

}
