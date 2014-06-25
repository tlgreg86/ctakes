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
package org.apache.ctakes.temporal.pipelines;

import java.io.File;

import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.DefaultChunkCreator;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.resource.FileResourceImpl;
import org.apache.ctakes.core.resource.JdbcConnectionResourceImpl;
import org.apache.ctakes.core.resource.LuceneIndexReaderResourceImpl;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE;
import org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.lvg.resource.LvgCmdApiResourceImpl;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.CopyNPChunksToLookupWindowAnnotations;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.RemoveEnclosedLookupWindows;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.xwriter.XWriter;
import org.uimafit.component.xwriter.XWriterFileNamer;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.ExternalResourceFactory;
import org.uimafit.factory.TypePrioritiesFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import com.lexicalscope.jewel.cli.Option;

public abstract class TemporalExtractionPipeline_ImplBase {
  static interface Options {

    @Option(
        shortName = "i",
        description = "specify the path to the directory containing the clinical notes to be processed")
    public String getInputDirectory();
    
    @Option(
        shortName = "o",
        description = "specify the path to the directory where the output xmi files are to be saved")
    public String getOutputDirectory();
  }
  
  /**
   * Preprocessing needed for relation extraction.
   */
  protected static AggregateBuilder getPreprocessorAggregateBuilder()
      throws Exception {
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add(ClinicalPipelineFactory.getDefaultPipeline());
    // add semantic role labeler
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(ClearNLPSemanticRoleLabelerAE.class));
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(ConstituencyParser.class));
    return aggregateBuilder;
  }
  
  protected static AggregateBuilder getLightweightPreprocessorAggregateBuilder() throws Exception{
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    
    /** Consider using ClinicalPipelineFactory.getDefaultPipeline()
     * 
     */
    // identify segments; use simple segment annotator on non-mayo notes
    // aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(SegmentsFromBracketedSectionTagsAnnotator.class));
    
    aggregateBuilder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(SentenceDetector.createAnnotatorDescription());
    aggregateBuilder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
    aggregateBuilder.add(ContextDependentTokenizerAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(POSTagger.createAnnotatorDescription());
    aggregateBuilder.add(Chunker.createAnnotatorDescription());
    aggregateBuilder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(ClearNLPSemanticRoleLabelerAE.class));
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(ConstituencyParser.class));

    return aggregateBuilder;
  }
  
  protected static AnalysisEngine getXMIWriter(String outputDirectory) throws ResourceInitializationException{
    return AnalysisEngineFactory.createPrimitive(
        XWriter.class,
        XWriter.PARAM_OUTPUT_DIRECTORY_NAME,
        outputDirectory,
        XWriter.PARAM_FILE_NAMER_CLASS_NAME,
        DocIDFileNamer.class.getName()
        );
  }
  
  public static class DocIDFileNamer implements XWriterFileNamer {
    @Override
    public String nameFile(JCas jCas) {
      return DocumentIDAnnotationUtil.getDocumentID(jCas);
    }
  }
}
