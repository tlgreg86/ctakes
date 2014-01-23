package org.apache.ctakes.temporal.ae;

import java.util.HashSet;

import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.timeml.type.Anchor;
import org.cleartk.timeml.type.TemporalLink;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.util.JCasUtil;

public class ClearTKLinkToTHYMELinkAnnotator extends JCasAnnotator_ImplBase {

  static HashSet<String> ctkRels = new HashSet<String>();
  
  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    for(TemporalLink link : JCasUtil.select(jCas, TemporalLink.class)){
      BinaryTextRelation rel = new BinaryTextRelation(jCas);
      RelationArgument arg1 = new RelationArgument(jCas);
      Anchor source = link.getSource();
      arg1.setArgument(new Annotation(jCas, source.getBegin(), source.getEnd()));
      arg1.addToIndexes();
      
      RelationArgument arg2 = new RelationArgument(jCas);
      Anchor target = link.getTarget();
      arg2.setArgument(new Annotation(jCas, target.getBegin(), target.getEnd()));
      arg2.addToIndexes();
      
      String cat = getMappedCategory(link.getRelationType());
      if(cat.endsWith("-1")){
        rel.setArg1(arg2);
        rel.setArg2(arg1);
        rel.setCategory(cat.substring(0, cat.length()-2));
      }else{
        rel.setCategory(getMappedCategory(link.getRelationType()));
        rel.setArg1(arg1);
        rel.setArg2(arg2);
      }
      rel.addToIndexes();
    }
  }

  public static AnalysisEngineDescription getAnnotatorDescription() throws ResourceInitializationException {
    return AnalysisEngineFactory.createPrimitiveDescription(ClearTKLinkToTHYMELinkAnnotator.class);
  }

  private static String getMappedCategory(String cleartkCat){
    if(!ctkRels.contains(cleartkCat)){
      System.err.println("New relation: " + cleartkCat);
      ctkRels.add(cleartkCat);
    }
    
    if(cleartkCat.equals("AFTER")){
      return "BEFORE-1";
    }else if(cleartkCat.equals("INCLUDES")){
      return "CONTAINS";
    }else if(cleartkCat.equals("IS_INCLUDED")){
      return "CONTAINS-1";
    }else{
      return cleartkCat;
    }
  }
}
