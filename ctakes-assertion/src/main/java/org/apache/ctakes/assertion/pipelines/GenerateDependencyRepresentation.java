package org.apache.ctakes.assertion.pipelines;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ctakes.assertion.eval.XMIReader;
import org.apache.ctakes.assertion.pipelines.GenerateTreeRepresentation.ATTRIBUTE;
import org.apache.ctakes.assertion.util.AssertionDepUtils;
import org.apache.ctakes.assertion.util.AssertionTreeUtils;
import org.apache.ctakes.assertion.util.SemanticClasses;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.util.JCasUtil;

public class GenerateDependencyRepresentation {
  public static class Options {

    @Option(
        name = "--train-dir",
        usage = "specify the directory containing the XMI training files (for example, /NLP/Corpus/Relations/mipacq/xmi/train)",
        required = true)
    public File trainDirectory;
    
    @Option(
        name = "--output",
        usage = "The file to which the data points be written.",
        required = true)
    public File outFile;

    @Option(name = "--attribute", required=false)
    public ATTRIBUTE attributeType = ATTRIBUTE.NEG;
  }

  protected static Options options = new Options();
  private static SemanticClasses sems = null; 
  private static PrintStream out = null;
  private static Logger log = Logger.getLogger(GenerateDependencyRepresentation.class);

  /**
   * @param args
   * @throws CmdLineException 
   */
  public static void main(String[] args) throws UIMAException, IOException, CmdLineException {
    CmdLineParser optionParser = new CmdLineParser(options);
    optionParser.parseArgument(args);
    
    out = new PrintStream(options.outFile);
    List<File> trainFiles = Arrays.asList(options.trainDirectory.listFiles());
    if(sems == null){
      sems = new SemanticClasses(FileLocator.getAsStream("org/apache/ctakes/assertion/all_cues.txt"));
    }

    String[] paths = new String[trainFiles.size()];
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = trainFiles.get(i).getPath();
    }
    CollectionReader reader = CollectionReaderFactory.createCollectionReader(
            XMIReader.class,
            XMIReader.PARAM_FILES,
            paths);
       
    JCasIterable casIter = new JCasIterable(reader);
    while(casIter.hasNext()){
      JCas jcas = casIter.next();
//      String docId = DocumentIDAnnotationUtil.getDocumentID(jcas);
//      out.println("## Document id: " + docId);
      processDocument(jcas);
    }
    out.close();

  }
  
  public static void processDocument(JCas jcas) {
    log.info("Processing document: " + DocumentIDAnnotationUtil.getDocumentID(jcas));
    Collection<Sentence> sents = JCasUtil.select(jcas, Sentence.class);
    for(Sentence sent : sents){
      List<ConllDependencyNode> nodes = JCasUtil.selectCovered(jcas, ConllDependencyNode.class, sent);

      // now that we've bult the tree, let's get the sub-trees for each concept:
      List<IdentifiedAnnotation> mentions = new ArrayList<IdentifiedAnnotation>(JCasUtil.selectCovered(EventMention.class, sent));
      mentions.addAll(JCasUtil.selectCovered(EntityMention.class, sent));
      
      for(IdentifiedAnnotation mention : mentions){

        SimpleTree tree = AssertionDepUtils.getTokenTreeString(jcas, nodes, mention);
        
//        String treeStr = AnnotationDepUtils.getTokenRelTreeString(jcas, nodes, new Annotation[]{mention}, new String[]{"CONCEPT"}, true);
        
        if(tree == null) continue;
        AssertionTreeUtils.replaceDependencyWordsWithSemanticClasses(tree, sems);
        String label = "-1";
        
        
        if(options.attributeType == ATTRIBUTE.NEG && mention.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT || 
           options.attributeType == ATTRIBUTE.UNC && mention.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT){
          label = "+1";
        }
        
        out.print(label);
        out.print(" |BT| ");
        out.print(tree.toString()); //tree.toString());
        out.println(" |ET|");
        out.flush();
//        // restore cat name:
//        node2tree.get(headNode).cat = realCat;

      }
      
//      out.println(node2tree.get(rootNode).toString());
    }
  }
}
