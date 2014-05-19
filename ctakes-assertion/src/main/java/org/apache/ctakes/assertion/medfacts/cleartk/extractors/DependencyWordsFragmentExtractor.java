package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.assertion.pipelines.GenerateDependencyRepresentation;
import org.apache.ctakes.assertion.util.AssertionDepUtils;
import org.apache.ctakes.assertion.util.AssertionTreeUtils;
import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.classifier.Feature;
import org.cleartk.util.CleartkInitializationException;
import org.uimafit.util.JCasUtil;

public class DependencyWordsFragmentExtractor extends TreeFragmentFeatureExtractor {

  public DependencyWordsFragmentExtractor(String prefix, String fragsPath) throws CleartkInitializationException {
    super(prefix, fragsPath);
  }

  @Override
  public List<Feature> extract(JCas jCas, Annotation mention) {
    List<Feature> features = new ArrayList<Feature>();

    List<Sentence> sents = JCasUtil.selectCovering(jCas, Sentence.class, mention.getBegin(), mention.getEnd());
    if(sents != null && sents.size() > 0){

      Sentence sent = sents.get(0);
      List<ConllDependencyNode> nodes = JCasUtil.selectCovered(ConllDependencyNode.class, sent);

      SimpleTree tree = AssertionDepUtils.getTokenTreeString(jCas, nodes, mention, GenerateDependencyRepresentation.UP_NODES);
      if(tree == null){
        System.err.println("Tree is null!");
      }else{
        AssertionTreeUtils.replaceDependencyWordsWithSemanticClasses(tree, sems);
        for(SimpleTree frag : frags){
          if(TreeUtils.containsDepFragIgnoreCase(tree, frag)){
            features.add(new Feature("TreeFrag_" + prefix, frag.toString()));
          }
        }
      }

    }
    return features;
  }
}