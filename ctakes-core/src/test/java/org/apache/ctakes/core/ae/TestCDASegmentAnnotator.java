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
package org.apache.ctakes.core.ae;

import junit.framework.Assert;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.cleartk.util.cr.FilesCollectionReader;
import org.junit.Test;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.util.JCasUtil;

public class TestCDASegmentAnnotator {

	public static String INPUT_FILE = "../ctakes-regression-test/testdata/input/plaintext/doc2_07543210_sample_current.txt";

	@Test
	public void TestCDASegmentPipeLine() throws Exception {
		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription();

		CollectionReader reader = CollectionReaderFactory
				.createCollectionReader(FilesCollectionReader.class,
						typeSystem, FilesCollectionReader.PARAM_ROOT_FILE,
						INPUT_FILE);

		AnalysisEngine sectionAnnotator = AnalysisEngineFactory
				.createPrimitive(CDASegmentAnnotator.class, typeSystem);
		AnalysisEngine dumpOutput = AnalysisEngineFactory.createPrimitive(
				DumpOutputAE.class, typeSystem);
		// SimplePipeline.runPipeline(reader, sectionAnnotator, dumpOutput);
		JCasIterable casIter = new JCasIterable(reader, sectionAnnotator,
				dumpOutput);
		final String expected_hpi_section = "1.3.6.1.4.1.19376.1.5.3.1.3.4";
		final int expected_begin = 220;
		final int expected_end = 1610;
		boolean section_exists = false;
		int section_begin = 0;
		int section_end = 0;

		while (casIter.hasNext()) {
			JCas jCas = casIter.next();
			for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
				if (expected_hpi_section.equalsIgnoreCase(segment.getId())) {
					section_exists = true;
					section_begin = segment.getBegin();
					section_end = segment.getEnd();
					break;
				}
			}
		}
		Assert.assertEquals(section_exists, true);
		Assert.assertEquals(expected_begin, section_begin);
		Assert.assertEquals(expected_end, section_end);
	}

	public static class DumpOutputAE extends JCasAnnotator_ImplBase {
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
				System.out.println("Segment:" + segment.getId() + " Begin:"
						+ segment.getBegin() + " End:" + segment.getEnd());
				// System.out.println("Text" + segment.getCoveredText());
			}
		}
	}
}
