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
package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;
import org.uimafit.util.JCasUtil;

public class NearbyVerbTenseRelationExtractor implements RelationFeaturesExtractor{

	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<Feature>();

		//find event
		EventMention event = null;
		if(arg1 instanceof EventMention){
			event = (EventMention) arg1;
		}else if(arg1 instanceof EventMention){
			event = (EventMention) arg2;
		}else{
			return feats;
		}

		//1 get covering sentence:
		Map<EventMention, Collection<Sentence>> coveringMap =
				JCasUtil.indexCovering(jcas, EventMention.class, Sentence.class);
		Collection<Sentence> sentList = coveringMap.get(event);

		//2 get Verb Tense
		if (sentList != null && !sentList.isEmpty()){
			for(Sentence sent : sentList) {
				String verbTP ="";
				for ( WordToken wt : JCasUtil.selectCovered(jcas, WordToken.class, sent)) {
					if (wt != null){
						String pos = wt.getPartOfSpeech();
						if (pos.startsWith("VB")){
							verbTP = verbTP + "_" + pos;
						}
					}
				}
				Feature feature = new Feature("VerbTenseFeature", verbTP);
				feats.add(feature);
				//logger.info("found nearby verb's pos tag: "+ verbTP);
			}

		}
		return feats;
	}


}
