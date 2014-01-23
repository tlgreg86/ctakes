package org.apache.ctakes.temporal.ae;

import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.timeml.type.Time;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.util.JCasUtil;

public class TimexToClearTKTimexAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    for(TimeMention timex : JCasUtil.select(jCas, TimeMention.class)){
      Time time = new Time(jCas);
      time.setBegin(timex.getBegin());
      time.setEnd(timex.getEnd());
      time.addToIndexes();
    }
  }
  
  public static AnalysisEngineDescription getAnnotatorDescription()
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createPrimitiveDescription(TimexToClearTKTimexAnnotator.class);
  }
}
