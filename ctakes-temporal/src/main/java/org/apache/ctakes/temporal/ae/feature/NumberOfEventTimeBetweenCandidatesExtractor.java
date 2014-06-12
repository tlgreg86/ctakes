package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;
import org.cleartk.util.ViewURIUtil;
import org.uimafit.util.JCasUtil;

public class NumberOfEventTimeBetweenCandidatesExtractor implements
RelationFeaturesExtractor {

	@SuppressWarnings("null")
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		ArrayList<Feature> feats = new ArrayList<>();
		
		//suppose arg1 is before arg2
		int begin = arg1.getEnd();
		int end   = arg2.getBegin();
		
		//if arg1 is after arg2
		if (begin > end ){
			begin = arg2.getEnd();
			end   = arg1.getBegin();
		}
		
		if(begin > end){
			return feats;
		}
		
		int eventsInBetween = 0;
		int timesInBetween  = 0;
		
		List<EventMention> events = JCasUtil.selectCovered(jCas, EventMention.class, begin, end);
		List<TimeMention> times   = JCasUtil.selectCovered(jCas, TimeMention.class, begin, end);
		eventsInBetween = events==null? 0: events.size();
		timesInBetween  = times==null? 0: times.size();
		feats.add(new Feature("NumOfEvents_InBetween", eventsInBetween));
		feats.add(new Feature("NumOfTimes_InBetween", timesInBetween));
		feats.add(new Feature("NumOfEventsAndTimes_InBetween", timesInBetween+eventsInBetween));
		
//		//print long distances
//		if (eventsInBetween >= 200){
//			System.out.println("++++++++++Long Distance Relation in "+ ViewURIUtil.getURI(jCas).toString() + "+++++++");
//			System.out.println("["+arg1.getCoveredText()+"] "+ jCas.getDocumentText().substring(arg1.getEnd(), arg2.getBegin()) +" ["+arg2.getCoveredText()+"]");
//		}

		return feats;
	}

}
