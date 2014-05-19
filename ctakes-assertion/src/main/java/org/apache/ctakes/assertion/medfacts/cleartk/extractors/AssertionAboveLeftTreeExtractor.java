package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import static org.apache.ctakes.assertion.util.AssertionTreeUtils.extractAboveLeftConceptTree;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.assertion.util.SemanticClasses;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.TreeFeature;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.cleartk.util.CleartkInitializationException;

public class AssertionAboveLeftTreeExtractor implements SimpleFeatureExtractor {
  protected SemanticClasses sems = null;

  public AssertionAboveLeftTreeExtractor() throws CleartkInitializationException{
    try{
      sems = new SemanticClasses(FileLocator.getAsStream("org/apache/ctakes/assertion/all_cues.txt"));
    }catch(Exception e){
      throw new CleartkInitializationException(e, "org/apache/ctakes/assertion/semantic_classes", "Could not find semantic classes resource.", new Object[]{});
    }
  }
  
  @Override
  public List<Feature> extract(JCas jcas, Annotation annotation)
      throws CleartkExtractorException {
    List<Feature> features = new ArrayList<Feature>();
    SimpleTree tree = extractAboveLeftConceptTree(jcas, annotation, sems);
    features.add(new TreeFeature("TK_AboveLeftTree", tree.toString()));
    return features;
  }
}
