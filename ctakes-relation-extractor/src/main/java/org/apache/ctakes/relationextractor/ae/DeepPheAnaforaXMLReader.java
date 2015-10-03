/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.relationextractor.ae;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DeepPheAnaforaXMLReader extends JCasAnnotator_ImplBase {
  
  private static Logger LOGGER = Logger.getLogger(DeepPheAnaforaXMLReader.class);

  public static final String PARAM_ANAFORA_DIRECTORY = "anaforaDirectory";

  @ConfigurationParameter(
      name = PARAM_ANAFORA_DIRECTORY,
      description = "root directory of the Anafora-annotated files, with one subdirectory for "
          + "each annotated file")
  private File anaforaDirectory;

  public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(DeepPheAnaforaXMLReader.class);
  }

  public static AnalysisEngineDescription getDescription(File anaforaDirectory)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        DeepPheAnaforaXMLReader.class,
        DeepPheAnaforaXMLReader.PARAM_ANAFORA_DIRECTORY,
        anaforaDirectory);
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    
    // determine source text file
    String textFileName = ViewUriUtil.getURI(jCas).getPath();
    String xmlFileName = textFileName + ".UmlsDeepPhe.dave.inprogress.xml";
    LOGGER.info("processing xml file: " + xmlFileName);

    processXmlFile(jCas, new File(xmlFileName));
  }
  
  private static void processXmlFile(JCas jCas, File xmlFile) throws AnalysisEngineProcessException{
    
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

    for (Element annotationsElem : dataElem.getChildren("annotations")) {

      Map<String, IdentifiedAnnotation> idToAnnotation = Maps.newHashMap();
      Map<String, String> diseaseDisorderToBodyLocation = Maps.newHashMap();
      
      for (Element entityElem : annotationsElem.getChildren("entity")) {
        
        String id = removeSingleChildText(entityElem, "id", null);
        Element spanElem = removeSingleChild(entityElem, "span", id);
        String type = removeSingleChildText(entityElem, "type", id);
        Element propertiesElem = removeSingleChild(entityElem, "properties", id);

        // UIMA doesn't support disjoint spans, so take the span enclosing everything
        int begin = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for(String spanString : spanElem.getText().split(";")) {
          String[] beginEndStrings = spanString.split(",");
          if (beginEndStrings.length != 2) {
            error("span not of the format 'number,number'", id);
          }
          int spanBegin = Integer.parseInt(beginEndStrings[0]);
          int spanEnd = Integer.parseInt(beginEndStrings[1]);
          if (spanBegin < begin) {
            begin = spanBegin;
          }
          if (spanEnd > end) {
            end = spanEnd;
          }
        }

        if(type.equals("Disease_Disorder") || type.equals("Metastasis")) {
          DiseaseDisorderMention diseaseDisorderMention = new DiseaseDisorderMention(jCas, begin, end);
          diseaseDisorderMention.addToIndexes();
          idToAnnotation.put(id, diseaseDisorderMention);
          String bodyLocationId = removeSingleChildText(propertiesElem, "body_location", id);
          diseaseDisorderToBodyLocation.put(id, bodyLocationId);
        } else if(type.equals("Anatomical_site")) {
          AnatomicalSiteMention anatomicalSiteMention = new AnatomicalSiteMention(jCas, begin, end);
          anatomicalSiteMention.addToIndexes();
          idToAnnotation.put(id, anatomicalSiteMention);
        } else {
          continue; // not going to worry about other types for the moment
        }
      }
      
      for(String diseaseDisorderId : diseaseDisorderToBodyLocation.keySet()) {
        IdentifiedAnnotation diseaseDisorderMention = idToAnnotation.get(diseaseDisorderId);
        String anatomicalSiteId = diseaseDisorderToBodyLocation.get(diseaseDisorderId);
        IdentifiedAnnotation anatomicalSiteMention = idToAnnotation.get(anatomicalSiteId);
        createLocationOfRelation(jCas, diseaseDisorderMention, anatomicalSiteMention);
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
  
  private static void createLocationOfRelation(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) {
    
    // disease/disorder
    RelationArgument relArg1 = new RelationArgument(jCas);
    relArg1.setArgument(arg1);
    relArg1.setRole("Argument");
    relArg1.addToIndexes();
    
    // anatomical site
    RelationArgument relArg2 = new RelationArgument(jCas);
    relArg2.setArgument(arg2);
    relArg2.setRole("Related_to");
    relArg2.addToIndexes();
    LocationOfTextRelation relation = new LocationOfTextRelation(jCas);
    relation.setArg1(relArg1);
    relation.setArg2(relArg2);
    relation.setCategory("location_of");
    relation.addToIndexes();
  }
}
