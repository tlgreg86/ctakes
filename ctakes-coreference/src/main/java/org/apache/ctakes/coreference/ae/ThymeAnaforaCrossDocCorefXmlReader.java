package org.apache.ctakes.coreference.ae;

import com.google.common.collect.Maps;
import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.ctakes.temporal.ae.THYMEAnaforaXMLReader;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tmill on 2/22/18.
 */
public class ThymeAnaforaCrossDocCorefXmlReader extends AbstractPatientConsumer {

    public static final String PARAM_XML_DIRECTORY = "XmlDirectory";
    @ConfigurationParameter(
            name = PARAM_XML_DIRECTORY,
            description = "Directory containing cross-document coreference annotations"
    )String xmlDir;

    public static final String PARAM_IS_TRAINING = "IsTraining";
    @ConfigurationParameter(
            name = PARAM_IS_TRAINING,
            description = "Whether this reader is being called at training or test time, and thus whether gold annotations should be put in document or gold CAS"
    )boolean isTraining;

    private static final String NAME = ThymeAnaforaCrossDocCorefXmlReader.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(ThymeAnaforaCrossDocCorefXmlReader.class);

    public ThymeAnaforaCrossDocCorefXmlReader(){
        super(NAME,
                "Reads gold standard cross-document coreference annotations in the format created for the THYME project, using the Anafora tool.");
    }

    public static AnalysisEngineDescription getDescription(String xmlDir, boolean training) throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(ThymeAnaforaCrossDocCorefXmlReader.class,
                ThymeAnaforaCrossDocCorefXmlReader.PARAM_XML_DIRECTORY,
                xmlDir,
                ThymeAnaforaCrossDocCorefXmlReader.PARAM_IS_TRAINING,
                training);
    }

    @Override
    public String getEngineName() {
        return NAME + (isTraining?"_training":"_test");
    }

    @Override
    protected void processPatientCas(JCas patientJcas) throws AnalysisEngineProcessException {
        String patientName = SourceMetadataUtil.getPatientIdentifier( patientJcas );
        String xmlFilename = String.format("%s.Thyme2v1-PostProc.gold.completed.xml", patientName);
        File annotationDir = null;
        for(String subdir : new String[]{"Train", "Dev", "Test"}){
            annotationDir = new File(new File(this.xmlDir, subdir), patientName);
            if(annotationDir.exists()) break;
        }
        if(annotationDir == null){
            System.err.println("Could not find a cross-doc coreference file for patient: " + patientName + " in the specified directory: " + this.xmlDir);
            throw new AnalysisEngineProcessException();
        }
        File annotationFile = new File(annotationDir, xmlFilename);
        if(!annotationFile.exists()){
//            LOGGER.warn("No *PostProc.gold.completed.xml file for this patient... trying Correction...");
//            xmlFilename = String.format("%s.Thyme2v1-Correction.gold.completed.xml", patientName);
//            annotationFile = new File(annotationDir, xmlFilename);
//            if (!annotationFile.exists()) {
            LOGGER.error("No *Correction.gold.completed.xml file exists for this patient either... please remove from dataset");
            throw new AnalysisEngineProcessException();
//            }
        }
        Map<String,String> notes = new HashMap<>();

        for(File file : annotationDir.listFiles()){
            if(file.isDirectory()){
                String fileContents = null;
                File noteFile = new File(file, file.getName());
                try {
                    fileContents = new String(Files.readAllBytes(Paths.get(noteFile.toURI())));
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new AnalysisEngineProcessException(e);
                }
                notes.put(file.getName(), fileContents);
            }
        }
        processXmlfile(patientJcas, annotationFile, notes);
    }

    private void processXmlfile(JCas patientJcas, File xmlFile, Map<String,String> notes) throws AnalysisEngineProcessException {
        // load the XML
        Element dataElem;
        try {
            dataElem = new SAXBuilder().build(xmlFile.toURI().toURL()).getRootElement();
        } catch (MalformedURLException e) {
            throw new AnalysisEngineProcessException(e);
        } catch (JDOMException e) {
            throw new AnalysisEngineProcessException(e);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
        HashMap<String,Integer> docLens = new HashMap<>();
        notes.forEach((k,v) -> docLens.put(k, v.length()));
        HashMap<String,JCas> docCases = new HashMap<>();
        HashMap<String,JCas> goldCases = new HashMap<>();
        for(String docName : notes.keySet()) {
            for (JCas docView : PatientViewUtil.getAllViews(patientJcas)) {
                if (docView.getViewName().contains(docName) && docView.getViewName().contains(CAS.NAME_DEFAULT_SOFA)) {
                    docCases.put(docName, docView);
                    break;
                }
            }
            for(JCas goldView : PatientViewUtil.getAllViews(patientJcas)){
                if(goldView.getViewName().contains(docName) && goldView.getViewName().contains(PatientViewUtil.GOLD_PREFIX)) {
                    goldCases.put(docName, goldView);
                }
            }
        }
        for (Element annotationsElem : dataElem.getChildren("annotations")) {
            // keep track of entity ids as we read entities so that we can find them from the map annotations later:
            Map<String, Annotation> idToAnnotation = Maps.newHashMap();

            for (Element entityElem : annotationsElem.getChildren("entity")) {
                String id = removeSingleChildText(entityElem, "id", null);
                String[] parts = id.split("@");
                String entNum = parts[0];   // note-specific id for this entity
                String entNoteName = parts[2];  // which note is this entity in: e.g., ID001_clinic_001
                String entAnnot = parts[3]; // should be "gold" for gold
                String entNote = notes.get(entNoteName);
                JCas entCas = goldCases.get(entNoteName);
                int docLen = entNote.length();
                Element spanElem = removeSingleChild(entityElem, "span", id);
                String type = removeSingleChildText(entityElem, "type", id);
                Element propertiesElem = removeSingleChild(entityElem, "properties", id);

                // UIMA doesn't support disjoint spans, so take the span enclosing
                // everything
                int begin = Integer.MAX_VALUE;
                int end = Integer.MIN_VALUE;
                for (String spanString : spanElem.getText().split(";")) {
                    String[] beginEndStrings = spanString.split(",");
                    if (beginEndStrings.length != 2) {
                        error("span not of the format 'number,number'", id);
                    }
                    int spanBegin = Integer.parseInt(beginEndStrings[0]);
                    int spanEnd = Integer.parseInt(beginEndStrings[1]);
                    if (spanBegin < begin && spanBegin >= 0) {
                        begin = spanBegin;
                    }
                    if (spanEnd > end && spanEnd <= docLen) {
                        end = spanEnd;
                    }
                }
                if (begin < 0 || end > docLen || end < 0) {
                    error("Illegal begin or end boundary", id);
                    continue;
                }

                Annotation annotation = null;
                if (type.equals("Markable")) {
                    while (end >= begin && (entNote.charAt(end - 1) == '\n' || entNote.charAt(end - 1) == '\r')) {
                        end--;
                    }
                    if(begin < 0 || end < 0){
                        error("Illegal negative span", id);
                    }
                    Markable markable = new Markable(entCas, begin, end);
                    markable.addToIndexes();
                    annotation = markable;

                } else {
                    LOGGER.warn(String.format("Skipping entity type %s because the handler hasn't been written.", type));
                }
                if (annotation != null) idToAnnotation.put(id, annotation);
            }

            for (Element relationElem : annotationsElem.getChildren("relation")) {
                String id = removeSingleChildText(relationElem, "id", null);
                String[] parts = id.split("@");
                String relNum = parts[0];   // note-specific id for this entity
                String relNoteName = parts[2];  // which note is this entity in: e.g., ID001_clinic_001
                String relAnnot = parts[3]; // should be "gold" for gold
                String relNote = notes.get(relNoteName);
                JCas relCas = goldCases.get(relNoteName);
                String type = removeSingleChildText(relationElem, "type", id);
                Element propertiesElem = removeSingleChild(relationElem, "properties", id);

                if (type.equals("Identical")) {
                    // Build list of Markables from FirstInstance and Coreferring_String annotations:
                    String mention = removeSingleChildText(propertiesElem, "FirstInstance", id);
                    List<Markable> markables = new ArrayList<>();
                    Markable antecedent, anaphor;
                    antecedent = (Markable) idToAnnotation.get(mention);
                    if(antecedent != null){
                        markables.add(antecedent);
                    }else{
                        error("Null markable as FirstInstance", id);
                    }
                    List<Element> corefs = propertiesElem.getChildren("Coreferring_String");
                    for(Element coref : corefs){
                        mention = coref.getText();
                        anaphor = (Markable) idToAnnotation.get(mention);
                        if(anaphor != null){
                            markables.add(anaphor);
                        }else{
                            error("Null markable as Coreferring_String", id);
                        }
                    }
                    // Iterate over markable list creating binary coref relations:
                    for(int antInd = 0; antInd < markables.size()-1; antInd++){
                        int anaInd = antInd + 1;
                        // create set of binary relations from chain elements:
                        CoreferenceRelation pair = new CoreferenceRelation(relCas);
                        pair.setCategory("Identity");
                        RelationArgument arg1 = new RelationArgument(relCas);
                        arg1.setArgument(markables.get(antInd));
                        arg1.setRole("antecedent");
                        pair.setArg1(arg1);
                        RelationArgument arg2 = new RelationArgument(relCas);
                        arg2.setArgument(markables.get(anaInd));
                        arg2.setRole("anaphor");
                        pair.setArg2(arg2);
                        pair.addToIndexes();
                    }
                    // Create FSList from markable list and add to collection text relation:
                    if(markables.size() > 1){
                        CollectionTextRelation chain = new CollectionTextRelation(relCas);
                        FSList list = ListFactory.buildList(relCas, markables);
                        list.addToIndexes();
                        chain.setMembers(list);
                        chain.addToIndexes();
                    }else{
                        error("Coreference chain of length <= 1", id);
                    }
                    propertiesElem.removeChildren("Coreferring_String");
                }else{
                    LOGGER.warn(String.format("This script cannot process relations of type %s yet.", type));
                }
            }
        }
    }

    private static Element getSingleChild(Element elem, String elemName, String causeID) {
        List<Element> children = elem.getChildren(elemName);
        if (children.size() != 1) {
            error(String.format("not exactly one '%s' child", elemName), causeID);
        }
        return children.size() > 0 ? children.get(0) : null;
    }

    private static Element removeSingleChild(Element elem, String elemName, String causeID) {
        Element child = getSingleChild(elem, elemName, causeID);
        elem.removeChildren(elemName);
        return child;
    }

    private static String removeSingleChildText(Element elem, String elemName, String causeID) {
        Element child = getSingleChild(elem, elemName, causeID);
        String text = child.getText();
        if (text.isEmpty()) {
            error(String.format("an empty '%s' child", elemName), causeID);
            text = null;
        }
        elem.removeChildren(elemName);
        return text;
    }

    private static void error(String found, String id) {
        LOGGER.error(String.format("found %s in annotation with ID %s", found, id));
    }

}
