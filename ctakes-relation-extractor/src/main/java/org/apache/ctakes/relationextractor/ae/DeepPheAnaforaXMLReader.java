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

import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.relation.AspectualTextRelation;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.cr.UriCollectionReader;
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

  public static final String PARAM_ANAFORA_XML_SUFFIXES = "anaforaSuffixes";

  @ConfigurationParameter(
      name = PARAM_ANAFORA_XML_SUFFIXES,
      mandatory = false,
      description = "list of suffixes that might be added to a file name to identify the Anafora "
          + "XML annotations file; only the first suffix corresponding to a file will be used")
  private String[] anaforaXMLSuffixes = new String[] {
      ".Temporal-Relations.gold.completed.xml",
      ".Temporal-Relation.gold.completed.xml",
          ".Temporal.dave.completed.xml",
      ".Temporal-Relation-Adjudication.gold.completed.xml",
      ".Temporal-Entity-Adjudication.gold.completed.xml",
      ".temporal.Temporal-Adjudication.gold.completed.xml",
      ".temporal.Temporal-Entities.gold.completed.xml",
      ".Temporal-Entity.gold.completed.xml",
      ".Gold_Temporal_Entities.xml",
      ".Gold_Temporal_Relations.xml"};

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

      Map<String, Annotation> idToAnnotation = Maps.newHashMap();

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

        Annotation annotation = null;
        if(type.equals("Disease_Disorder")) {
          EventMention eventMention = new EventMention(jCas, begin, end);
          Event event = new Event(jCas);
          EventProperties eventProperties = new EventProperties(jCas);
          eventProperties.addToIndexes();
          event.addToIndexes();
          eventMention.setEvent(event);
          eventMention.addToIndexes();
          annotation = eventMention;
        } else if(type.equals("Anatomical_site")) {
          AnatomicalSiteMention anatomicalSiteMention = new AnatomicalSiteMention(jCas, begin, end);
          anatomicalSiteMention.addToIndexes();
        } else if (type.equals("Metastasis")) {
          DiseaseDisorderMention diseaseDisorderMention = new DiseaseDisorderMention(jCas, begin, end);
          diseaseDisorderMention.addToIndexes();
        } else {
          continue; // not going to worry about other types for the moment
        }

        // match the annotation to its ID for later use
        idToAnnotation.put(id, annotation);

        // make sure all XML has been consumed
        removeSingleChild(entityElem, "parentsType", id);
        if (!propertiesElem.getChildren().isEmpty() || !entityElem.getChildren().isEmpty()) {
          List<String> children = Lists.newArrayList();
          for (Element child : propertiesElem.getChildren()) {
            children.add(child.getName());
          }
          for (Element child : entityElem.getChildren()) {
            children.add(child.getName());
          }
          error("unprocessed children " + children, id);
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
