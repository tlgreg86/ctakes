package org.apache.ctakes.lvg.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.JCasFactory;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.util.JCasUtil;

public class TestLvgAnnotator {
  public static final String note = "" +
      "Medications:\n" +
      "Hibernol, jamitol, triopenin, sproingo\n\n" +
      "Physical exam:\n" +
      "Patient is doing fine but probably taking too many fictional drugs. Cholesterol is acceptable. Heartrate is elevated. \n" +
      "Instructions:\n" +
      "Patient should quit smoking and taunting sharks.";

  @Test
  public void testLvgAnnotator() throws UIMAException, IOException, URISyntaxException{
    JCas jcas = JCasFactory.createJCas();
    jcas.setDocumentText(note);
    
    SimplePipeline.runPipeline(jcas, getDefaultPipeline());
    List<WordToken> tokens = new ArrayList<>(JCasUtil.select(jcas, WordToken.class));
    assertEquals("Incorrect canonical form!", "medication", tokens.get(0).getCanonicalForm());
    
    assertTrue(tokens.get(29).getCanonicalForm() == null);
    
  }
  
  public static AnalysisEngineDescription getPrerequisitePipeline() throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
    builder.add(SentenceDetector.createAnnotatorDescription());
    builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
    return builder.createAggregateDescription();
  }
  
  public static AnalysisEngineDescription getDefaultPipeline() throws ResourceInitializationException, URISyntaxException{
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(getPrerequisitePipeline());
    builder.add(LvgAnnotator.createAnnotatorDescription());
    return builder.createAggregateDescription();
  }
  
  
}
