package org.apache.ctakes.core.ae;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.JCasFactory;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.util.JCasUtil;

public class SentenceDetectorAnnotatorTest {

  public static final String note = "" +
      "Medications:\n" +
      "Hibernol, jamitol, triopenin, sproingo\n\n" +
      "Physical exam:\n" +
      "Patient is doing fine but probably taking too many fictional drugs. Cholesterol is acceptable. Heartrate is elevated. \n" +
      "Instructions:\n" +
      "Patient should quit smoking and taunting sharks.";

  @Test
  public void testSentenceDetectorInitialization() throws UIMAException, IOException{
    
    JCas jcas = JCasFactory.createJCas();
    jcas.setDocumentText(note);
    SimplePipeline.runPipeline(jcas, getSegmentingPipeline());
    
    Collection<Segment> segs = JCasUtil.select(jcas, Segment.class);
    assertEquals(segs.size(), 3);
    
    // test # sentences -- if it skips MEDS and Instructions it should be 3 from the physical exam section only.
    Collection<Sentence> sents = JCasUtil.select(jcas, Sentence.class);
    assertEquals(sents.size(), 3);
    
    jcas = JCasFactory.createJCas();
    jcas.setDocumentText(note);
    SimplePipeline.runPipeline(jcas, getBasicPipeline());
    segs = JCasUtil.select(jcas, Segment.class);
    assertEquals(segs.size(), 1);
    
    // test # sentences -- if it skips MEDS and Instructions it should be 3 from the physical exam section only.
    sents = JCasUtil.select(jcas, Sentence.class);
    assertEquals(sents.size(), 8);
  }
  
  private static AnalysisEngine getSegmentingPipeline() throws ResourceInitializationException{
    AggregateBuilder aggregateBuilder = new AggregateBuilder();

    // identify segments; use simple segment annotator on non-mayo notes
    // aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(SegmentsFromBracketedSectionTagsAnnotator.class));
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(CDASegmentAnnotator.class));

    // identify sentences
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(
        SentenceDetector.class,
        SentenceDetector.SD_MODEL_FILE_PARAM,
        "org/apache/ctakes/core/sentdetect/sd-med-model.zip",
        SentenceDetector.PARAM_SEGMENTS_TO_SKIP,
        new String[]{"2.16.840.1.113883.10.20.22.2.1.1" /*Medications*/, "2.16.840.1.113883.10.20.22.2.45" /*Instructions*/}));

    return aggregateBuilder.createAggregate();
  }
  
  private static AnalysisEngine getBasicPipeline() throws ResourceInitializationException{
    AggregateBuilder aggregateBuilder = new AggregateBuilder();

    // identify segments; use simple segment annotator on non-mayo notes
    // aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(SegmentsFromBracketedSectionTagsAnnotator.class));
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(SimpleSegmentAnnotator.class));

    // identify sentences
    aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(
        SentenceDetector.class,
        SentenceDetector.SD_MODEL_FILE_PARAM,
        "org/apache/ctakes/core/sentdetect/sd-med-model.zip"
        ));

    return aggregateBuilder.createAggregate();
  }
}
