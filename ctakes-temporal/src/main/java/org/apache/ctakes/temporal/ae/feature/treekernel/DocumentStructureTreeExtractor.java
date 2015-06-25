package org.apache.ctakes.temporal.ae.feature.treekernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.dependency.parser.util.DependencyPath;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.tksvmlight.TreeFeature;
import org.cleartk.ml.tksvmlight.kernel.DescendingPathKernel;

public class DocumentStructureTreeExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  String cachedDoc = null;
  SimpleTree docTree = null;
  Map<IdentifiedAnnotation,SimpleTree> cand2sentNode = null;
  DescendingPathKernel dpk = new DescendingPathKernel(1.0, true);
  
  public List<Feature> extract(JCas jcas, IdentifiedAnnotation ante,
      IdentifiedAnnotation ana) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    SimpleTree finalTree = null;
    String treeFeatString = null;
    
    if(cachedDoc == null || jcas.getDocumentText() != cachedDoc){
      cachedDoc = jcas.getDocumentText();
      cand2sentNode = new HashMap<>();
      docTree = buildDocTree(jcas, cand2sentNode);
    }

    SimpleTree s1 = cand2sentNode.get(ante);
    SimpleTree s2 = cand2sentNode.get(ana);
    
    if(s1 == null){
      s1 = SimpleTree.fromString("(Couldnotfind markable)");
    }
    if(s2 == null){
      s2 = SimpleTree.fromString("(Couldnotfind markable)");
    }
    
    SimpleTree anteCur = s1;
    SimpleTree anaCur = s2;

    ConllDependencyNode anteHead =  DependencyUtility.getNominalHeadNode(jcas, ante);
    ConllDependencyNode anaHead = DependencyUtility.getNominalHeadNode(jcas, ana);
    
    DependencyPath antePath = null;
    if(anteHead != null){
      antePath = DependencyUtility.getPathToTop(jcas, anteHead).reverse();
    }else{
      antePath = new DependencyPath();
      anteCur.addChild(new SimpleTree(ante.getCoveredText().replace(' ', '_')));
    }
    
    DependencyPath anaPath = null;
    if(anaHead != null){
      anaPath = DependencyUtility.getPathToTop(jcas, anaHead).reverse();
    }else{
      anaPath = new DependencyPath();
      anaCur.addChild(new SimpleTree(ana.getCoveredText().replace(' ', '_')));
    }
    
    for(int i = 1; i < Math.max(antePath.size(), anaPath.size()); i++){
      if(i < antePath.size() && i < anaPath.size() && antePath.get(i) == anaPath.get(i)){
        assert anteCur == anaCur;
        anteCur.addChild(new SimpleTree(antePath.get(i).getForm()));
        anteCur = anteCur.children.get(0);
        anaCur = anteCur;
      }else{
        if(i < antePath.size()){
          SimpleTree newTree = new SimpleTree(antePath.get(i).getForm());
          anteCur.addChild(newTree);
          anteCur = newTree;
        }
        if(i < anaPath.size()){
          SimpleTree newTree = new SimpleTree(anaPath.get(i).getForm());
          anaCur.addChild(newTree);
          anaCur = newTree;
        }
      }
    }
    treeFeatString = docTree.toString();
    s1.children.clear();
    if(s2 != null && s2.children != null && s2.children.size() > 0){
      s2.children.clear();
    }
    
    feats.add(new TreeFeature("TK_path", treeFeatString, dpk));
    

    /*
    SimpleTree antePathTree = getDepPathTree(jcas, ante, "Ante");
    SimpleTree anaPathTree = getDepPathTree(jcas, ana, "Ana");

    Sentence anteSent = getSingleCoveringAnnotation(Sentence.class, ante);
    Sentence anaSent = getSingleCoveringAnnotation(Sentence.class, ana);
    
    if(anteSent == null || anaSent == null){
      finalTree = createTree("Unk", antePathTree, anaPathTree);
    }else if(anteSent == anaSent){
      if(antePathTree.cat.equals(anaPathTree.cat)){
        // normal case -- both terms in the same sentence should have the
        // same head of the sentence
        // simply the dependency path tree
        finalTree = new SimpleTree(antePathTree.cat);
        if(antePathTree.children.size() > 0){
          finalTree.addChild(antePathTree.children.get(0));
        }
        if(anaPathTree.children.size() > 0){
          finalTree.addChild(anaPathTree.children.get(0));
        }
      }else{
        // edge case where a sentence has two "root" relations and each element
        // of the pair goes up to a different root
        finalTree = createTree("Sentence", antePathTree, anaPathTree);
      }
    }else{
      SimpleTree anteSentTree = new SimpleTree("Sentence");
      anteSentTree.addChild(antePathTree);
      SimpleTree anaSentTree = new SimpleTree("Sentence");
      anaSentTree.addChild(anaPathTree);

      Paragraph antePar = getSingleCoveringAnnotation(Paragraph.class, ante);
      Paragraph anaPar = getSingleCoveringAnnotation(Paragraph.class, ana);
      if(antePar == null || anaPar == null){
        finalTree = createTree("Unk", anteSentTree, anaSentTree);
      }else if(antePar == anaPar){
        finalTree = createTree("Paragraph", anteSentTree, anaSentTree);
      }else{
        SimpleTree anteParTree = new SimpleTree("Paragraph");
        anteParTree.addChild(anteSentTree);
        SimpleTree anaParTree = new SimpleTree("Paragraph");
        anaParTree.addChild(anaSentTree);

        Segment anteSeg = getSingleCoveringAnnotation(Segment.class, ante);
        Segment anaSeg = getSingleCoveringAnnotation(Segment.class, ana);
        if(anteSeg == null || anaSeg == null){
          finalTree = createTree("Unk", anteParTree, anaParTree);
        }else if(anteSeg == anaSeg){
          finalTree = createTree("Segment", anteParTree, anaParTree);
        }else{
          SimpleTree anteSegTree = new SimpleTree("Segment");
          anteSegTree.addChild(anteParTree);
          SimpleTree anaSegTree = new SimpleTree("Segment");
          anaSegTree.addChild(anaParTree);
          
          finalTree = createTree("Doc", anteSegTree, anaSegTree);
        }
      }
    }

    
    SimpleTree topTree = new SimpleTree("TOP");
    topTree.addChild(finalTree);
    feats.add(new TreeFeature("TK_DocTree", topTree));
    */
    return feats;
  }

  private static SimpleTree getDepPathTree(JCas jcas, IdentifiedAnnotation a, String cat){
    SimpleTree pathTree = null;
    ConllDependencyNode entNode = DependencyUtility.getNominalHeadNode(jcas, a);
    if(entNode != null){
      DependencyPath depPath = DependencyUtility.getPathToTop(jcas, entNode);
      if(depPath != null){
        depPath = depPath.reverse();
      
        depPath.remove();
        SimpleTree cur = null;
        for(ConllDependencyNode node : depPath){
          if(pathTree == null){
            if(node == depPath.getLast()){
              pathTree = new SimpleTree(cat);
              pathTree.addChild(new SimpleTree(node.getCoveredText()));
            }else{
              pathTree = new SimpleTree(node.getCoveredText());
            }
            cur = pathTree;
          }else{
            if(node == depPath.getLast()){
              cur.addChild(new SimpleTree(cat));
              cur = cur.children.get(0);
            }
            cur.addChild(new SimpleTree(node.getCoveredText()));
            cur = cur.children.get(0);
          }
        }
      }
    }
    if(pathTree == null){
      pathTree = new SimpleTree(cat);
      pathTree.addChild(new SimpleTree(last(a.getCoveredText().split(" "))));
    }
    return pathTree;
  }

  private static <T> T last(T[] array){
    return array[array.length-1];
  }
  
  private static <T extends Annotation> T getSingleCoveringAnnotation(Class<T> coveringClass, Annotation a){
    List<T> list = JCasUtil.selectCovering(coveringClass, a);
    if(list != null && list.size() > 0){
      return list.get(0);
    }
    return null;
  }
  
  private static SimpleTree createTree(String cat, SimpleTree lc, SimpleTree rc){
    SimpleTree t = new SimpleTree(cat);
    t.addChild(lc);
    t.addChild(rc);
    return t;
  }
  
  private static SimpleTree buildDocTree(JCas jcas, Map<IdentifiedAnnotation,SimpleTree> cand2sentNode){
    SimpleTree tree = new SimpleTree("Document");
    
    for(Segment seg : JCasUtil.select(jcas, Segment.class)){
      SimpleTree segChild = new SimpleTree("Segment");
      for(Paragraph par : JCasUtil.selectCovered(jcas, Paragraph.class, seg)){
        SimpleTree parChild = new SimpleTree("Paragraph");
        for(Sentence sent : JCasUtil.selectCovered(jcas, Sentence.class, par)){
          SimpleTree sentChild = new SimpleTree("Sentence");
          for(Markable annot : JCasUtil.selectCovered(jcas, Markable.class, sent)){
            
            cand2sentNode.put(annot, sentChild);
          }
          
//          String tokenTree = AnnotationDepUtils.getTokenTreeString(jcas, DependencyUtility.getDependencyNodes(jcas, sent), null, null, false);
//          sentChild.addChild(SimpleTree.fromString(tokenTree));
          parChild.addChild(sentChild);
        }
        segChild.addChild(parChild);
      }
      tree.addChild(segChild); 
    }
    
    return tree;
  }
}
