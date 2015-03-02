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
package org.apache.ctakes.relationextractor.data.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.cr.XMIReader;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Various useful classes and methods.
 */
public class Utils {
  
  /**
   * Instantiate an XMI collection reader.
   */
  public static CollectionReader getCollectionReader(File inputDirectory) throws Exception {

    List<String> fileNames = new ArrayList<>();
    for(File file : inputDirectory.listFiles()) {
      if(! (file.isHidden())) {
        fileNames.add(file.getPath());
      }
    }

    String[] paths = new String[fileNames.size()];
    fileNames.toArray(paths);

    return CollectionReaderFactory.createReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
  
  /**
   * Given an annotation, retrieve its last word.
   */
  public static String getLastWord(JCas systemView, Annotation annotation) {
    
    List<WordToken> tokens = JCasUtil.selectCovered(systemView, WordToken.class, annotation);
    if(tokens.size() == 0) {
          return annotation.getCoveredText();
    }
    
    WordToken lastToken = tokens.get(tokens.size() - 1);
    return lastToken.getCoveredText();
  }
}
