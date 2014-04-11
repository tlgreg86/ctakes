package org.apache.ctakes.temporal.ae.feature;

import java.util.List;

import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;

import com.google.common.collect.Lists;

public class SyntacticParentFeatureExtractor implements RelationFeaturesExtractor {

  /*
   * (non-Javadoc)
   * @see org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor#extract(org.apache.uima.jcas.JCas, org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation, org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation)
   * This feature extractor simply gets the parent syntactic category for each argument.
   */
  public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
    List<Feature> feats = Lists.newArrayList();
    
    
    TreebankNode arg1node = AnnotationTreeUtils.annotationNode(jcas, arg1);
    feats.add(new Feature("Arg1Parent", arg1node.getNodeType()));
    TreebankNode arg2node = AnnotationTreeUtils.annotationNode(jcas, arg2);
    feats.add(new Feature("Arg2Parent", arg2node.getNodeType()));
        
    return feats;
  }

}
