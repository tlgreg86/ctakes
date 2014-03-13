package org.apache.ctakes.temporal.ae.feature.treekernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.dependency.parser.util.AnnotationDepUtils;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.TreeFeature;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.uimafit.util.JCasUtil;
/**
 * Given a focused annotation, get the whole sentence-level dependency tree that cover this annotation.
 * @author CH151862
 *
 */
public class DependencySingleTreeExtractor implements SimpleFeatureExtractor {

	public static final String FEAT_NAME = "TK_DepSingleT";

	@Override
//	public List<Feature> extract(JCas view, Annotation focusAnnotation)
//			throws CleartkExtractorException {
//		List<Feature> features = new ArrayList<Feature>();
//		//1 generate event annotation array and label array
//		Annotation[] annotations = {focusAnnotation};
//		String[] labels ={"EVENT"};
//		
//		//2 get covering sentence:
//		Map<EventMention, Collection<Sentence>> coveringMap =
//				JCasUtil.indexCovering(view, EventMention.class, Sentence.class);
//		EventMention targetTokenAnnotation = (EventMention)focusAnnotation;
//		Collection<Sentence> sentList = coveringMap.get(targetTokenAnnotation);
//		
//		//3 extract trees:
//		String dtreeStr ="(TOP (EVENT " + focusAnnotation.getCoveredText().trim() + "))";
//		if (sentList != null && !sentList.isEmpty()){
//			for(Sentence sent : sentList) {
//				List<ConllDependencyNode> nodes = JCasUtil.selectCovered(view, ConllDependencyNode.class, sent);
//				
//				if(nodes!=null && !nodes.isEmpty()){
//					String treeStr = AnnotationDepUtils.getTokenTreeString(view, nodes, annotations, labels, true);
//					if(treeStr != null){
//						dtreeStr = treeStr;
//						break;
//					}
//				}
//			}
//		}
//		
//		features.add(new TreeFeature(FEAT_NAME, dtreeStr));
//		
//		return features;
//	}

	public List<Feature> extract(JCas view, Annotation focusAnnotation)
			throws CleartkExtractorException {
		List<Feature> features = new ArrayList<Feature>();
		String dtreeStr ="(TOP (EVENT " + focusAnnotation.getCoveredText().trim() + "))";
		//find the colldepnode covered by focusAnnotation:
		for(ConllDependencyNode node : JCasUtil.selectCovered(view, ConllDependencyNode.class, focusAnnotation)){
			//find if it has head:
			ConllDependencyNode head = node.getHead();
			SimpleTree curTree = null;
			SimpleTree headTree = null;
			
			if(head == null) { //if the current node is the root, then not right
				continue;
			}

//			curTree = SimpleTree.fromString(String.format("(%s %s)", node.getDeprel(), SimpleTree.escapeCat(node.getCoveredText().trim())));
			curTree = SimpleTree.fromString(String.format("(%s %s)", node.getDeprel(), node.getPostag()));



			while(head.getHead() != null){ //while head node is not the root
//              String token = node.getHead().getHead() == null ? "TOP" : node.getHead().getCoveredText();
//				headTree = SimpleTree.fromString(String.format("(%s %s)", head.getDeprel(), SimpleTree.escapeCat(head.getCoveredText().trim())));
				headTree = SimpleTree.fromString(String.format("(%s %s)", head.getDeprel(), head.getPostag()));
				curTree.parent = headTree.children.get(0);
				headTree.children.get(0).addChild(curTree);
				curTree = headTree;
				head = head.getHead();
			} 
			if(headTree==null){
				curTree = SimpleTree.fromString(String.format("(%s (%s %s))",node.getDeprel(), node.getPostag(),"null"));
				dtreeStr = curTree.toString();
			}else{
				dtreeStr = headTree.toString();
			}
			break;
		}
		
		features.add(new TreeFeature(FEAT_NAME, dtreeStr));
		return features;
	}
}
