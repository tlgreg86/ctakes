package org.apache.ctakes.temporal.ae.feature.treekernel;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.constituency.parser.treekernel.TreeExtractor;
import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.TreeFeature;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
/**
 * Given a focused annotation, get the whole sentence-level parse tree that cover this annotation.
 * @author CH151862
 *
 */
public class SyntaticSingleTreeExtractor implements SimpleFeatureExtractor {

	public static final String FEAT_NAME = "TK_SingleT";

	@Override
	public List<Feature> extract(JCas view, Annotation focusAnnotation)
			throws CleartkExtractorException {
		List<Feature> features = new ArrayList<Feature>();
		// first get the root and print it out...
		TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(view, AnnotationTreeUtils.getAnnotationTree(view, focusAnnotation));

		if(root == null){
			SimpleTree fakeTree = new SimpleTree("(S (NN null))");
			features.add(new TreeFeature(FEAT_NAME, fakeTree.toString()));
			return features;
		}


		String etype="";
		String eventModality="";

		if(focusAnnotation instanceof EventMention){
			eventModality = ((EventMention)focusAnnotation).getEvent().getProperties().getContextualModality();
			etype = "EVENT-"+eventModality;
			AnnotationTreeUtils.insertAnnotationNode(view, root, focusAnnotation, etype);
		}
	
		SimpleTree tree = null;
		tree = TreeExtractor.getSimpleClone(root);

		TemporalPETExtractor.moveTimexDownToNP(tree);

		features.add(new TreeFeature(FEAT_NAME, tree.toString()));
		return features;
	}

}
