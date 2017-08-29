package org.apache.ctakes.temporal.ae;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ctakes.temporal.eval.Evaluation_ImplBase;
import org.apache.ctakes.temporal.utils.PatientViewsUtil;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ViewUriUtil;

/*
 * ThymePatientToDocumentUriAnnotator
 * If we are reading from the coreference version of the corpus, we will have
 * our document URI be a pointer to the directory containing multiple notes for
 * a patient. In that case, this annotator, creates multiple views for the URIs
 * of each of the files in the directory and populates the view with the URI
 * for each document.
 */
public class ThymePatientViewAnnotator extends JCasAnnotator_ImplBase{

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    URI uri = ViewUriUtil.getURI( jCas );
    File uriFile = new File(uri);
    if(uriFile.exists() && uriFile.isDirectory()){
      // Create views for each document: One that has the document URI, one for system-created annotations, and 
      // a third for gold-standard annotations.
      List<File> subdirs = Arrays.asList(uriFile.listFiles());
      Collections.sort(subdirs);
      for(int i = 0; i < subdirs.size(); i++){          
        String uriViewName = PatientViewsUtil.getUriViewName(i);
        ViewCreatorAnnotator.createViewSafely(jCas, uriViewName);
        String docViewName = PatientViewsUtil.getViewName(i);
        ViewCreatorAnnotator.createViewSafely(jCas, docViewName);
        String goldViewName = PatientViewsUtil.getGoldViewName(i);
        ViewCreatorAnnotator.createViewSafely(jCas, goldViewName);
        JCas uriView;
        try {
          uriView = jCas.getView(uriViewName);
          File txtFile = new File(subdirs.get(i), subdirs.get(i).getName());
          uriView.setSofaDataURI(txtFile.toURI().toString(), null);
        } catch (CASException e) {
          throw new AnalysisEngineProcessException(e);
        }
      }
      
      // Create a view that will contain the number of documents that this patient has:
      String numDocsViewName = PatientViewsUtil.getNumDocsViewName();
      ViewCreatorAnnotator.createViewSafely(jCas, numDocsViewName);
      JCas numDocsView;
      try {
        numDocsView = jCas.getView(numDocsViewName);
        numDocsView.setDocumentText(String.valueOf(subdirs.size()));
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }
    }
  }
}
