package org.apache.ctakes.clinicalpipeline;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.JCasFactory;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.util.JCasUtil;
import org.xml.sax.SAXException;

public class ClinicalPipelineFactory {

  public static AnalysisEngineDescription getDefaultPipeline() throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(getTokenProcessingPipeline());
    builder.add(AnalysisEngineFactory.createPrimitiveDescription(CopyNPChunksToLookupWindowAnnotations.class));
    builder.add(AnalysisEngineFactory.createPrimitiveDescription(RemoveEnclosedLookupWindows.class));
    builder.add(UmlsDictionaryLookupAnnotator.createAnnotatorDescription());
    builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
    builder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
    return builder.createAggregateDescription();
  }
  
  // TODO
  public static AnalysisEngineDescription getParsingPipeline(){
    AggregateBuilder builder = new AggregateBuilder();
    throw new UnsupportedOperationException("Not yet implemented!");
  }
  
  public static AnalysisEngineDescription getTokenProcessingPipeline() throws ResourceInitializationException {
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
    builder.add(SentenceDetector.createAnnotatorDescription());
    builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
    builder.add(LvgAnnotator.createAnnotatorDescription());
    builder.add(ContextDependentTokenizerAnnotator.createAnnotatorDescription());
    builder.add(POSTagger.createAnnotatorDescription());
    builder.add(Chunker.createAnnotatorDescription());
    builder.add(getStandardChunkAdjusterAnnotator());
    
    return builder.createAggregateDescription();
  }
  
  public static AnalysisEngineDescription getStandardChunkAdjusterAnnotator() throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    // adjust NP in NP NP to span both
    builder.add(ChunkAdjuster.createAnnotatorDescription(new String[] { "NP", "NP" },  1));
    // adjust NP in NP PP NP to span all three
    builder.add(ChunkAdjuster.createAnnotatorDescription(new String[] { "NP", "PP", "NP" }, 2));
    return builder.createAggregateDescription();
  }
  
  public static class CopyNPChunksToLookupWindowAnnotations extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      for (Chunk chunk : JCasUtil.select(jCas, Chunk.class)) {
        if (chunk.getChunkType().equals("NP")) {
          new LookupWindowAnnotation(jCas, chunk.getBegin(), chunk.getEnd()).addToIndexes();
        }
      }
    }
  }
  
  public static class RemoveEnclosedLookupWindows extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      List<LookupWindowAnnotation> lws = new ArrayList<>(JCasUtil.select(jCas, LookupWindowAnnotation.class));
      // we'll navigate backwards so that as we delete things we shorten the list from the back
      for(int i = lws.size()-2; i >= 0; i--){
        LookupWindowAnnotation lw1 = lws.get(i);
        LookupWindowAnnotation lw2 = lws.get(i+1);
        if(lw1.getBegin() <= lw2.getBegin() && lw1.getEnd() >= lw2.getEnd()){
          /// lw1 envelops or encloses lw2
          lws.remove(i+1);
          lw2.removeFromIndexes();
        }
      }
      
    }
    
  }
  
  public static void main(String[] args) throws FileNotFoundException, IOException, UIMAException, SAXException{
    AnalysisEngineDescription aed = getDefaultPipeline();
    String note = "The patient is suffering from extreme pain due to shark bite. Recommend continuing use of aspirin, oxycodone, and coumadin. Continue exercise for obesity and hypertension." +
                  "Patient denies smoking and chest pain. Patient has no cancer. There is no sign of multiple sclerosis.";
    JCas jcas = JCasFactory.createJCas();
    jcas.setDocumentText(note);
    SimplePipeline.runPipeline(jcas, aed);

    for(IdentifiedAnnotation entity : JCasUtil.select(jcas, IdentifiedAnnotation.class)){
      System.out.println("Entity: " + entity.getCoveredText() + " === Polarity: " + entity.getPolarity());
    }
    
    if(args.length > 0)
      aed.toXML(new FileWriter(args[0]));
  }
}
