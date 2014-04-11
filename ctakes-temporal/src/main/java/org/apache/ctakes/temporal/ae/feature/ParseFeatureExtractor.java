package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;

public class ParseFeatureExtractor implements RelationFeaturesExtractor {

  /*
   * (non-Javadoc)
   * @see org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor#extract(org.apache.uima.jcas.JCas, org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation, org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation)
   * This feature extractor finds the lowest dominating phrase category of each
   * argument, then specifies if one dominates t'other.
   */
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = new ArrayList<Feature>();
		
		TreebankNode tree1 = AnnotationTreeUtils.annotationNode(jCas, arg1);
		TreebankNode tree2 = AnnotationTreeUtils.annotationNode(jCas, arg2);
		TreebankNode phrase1 = tree1;
		TreebankNode phrase2 = tree2;
		
		while(phrase1.getParent() != null){
			phrase1 = phrase1.getParent();
			if(phrase1.getNodeType().endsWith("P")) break;
		}
		while(phrase2.getParent() != null){
			phrase2 = phrase2.getParent();
			if(phrase2.getNodeType().endsWith("P")) break;
		}
		
		
		if(phrase1.getBegin() <= phrase2.getBegin() && phrase1.getEnd() >= phrase2.getEnd()){
			features.add(new Feature("Arg1DominatesArg2"));
		}else if(phrase2.getBegin() <= phrase1.getBegin() && phrase2.getEnd() >= phrase1.getEnd()){
			features.add(new Feature("Arg2DominatesArg1"));
		}
		
//		TreebankNode lca = AnnotationTreeUtils.getCommonAncestor(tree1, tree2);
//		features.add(new Feature("LCA", lca));
		
		return features;
	}

}
