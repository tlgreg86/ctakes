package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.Arrays;
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

public class CheckSpecialWordRelationExtractor implements RelationFeaturesExtractor{
	
	final static List<String> specialWd = Arrays.asList("before","prior","previous","previously","ago","soon","earlier","early","after","later","subsequent","follow","following","followed","post","since","back","start","started","by","past","starting");

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
				for ( WordToken wt : JCasUtil.selectCovered(jcas, WordToken.class, sent)) {
					if (wt != null){
						String wdtext = wt.getCoveredText().toLowerCase();
						if (specialWd.contains(wdtext)){
							Feature feature = new Feature("SpecialWd", wdtext);
							feats.add(feature);
						}
					}
				}
				
				//logger.info("found nearby verb's pos tag: "+ verbTP);
			}

		}
		return feats;
	}


}
