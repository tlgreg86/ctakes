package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;
import org.uimafit.util.JCasUtil;

public class SectionHeaderRelationExtractor implements RelationFeaturesExtractor{

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

		//get covering segment set:
		Map<EventMention, Collection<Segment>> coveringMap =
				JCasUtil.indexCovering(jcas, EventMention.class, Segment.class);
		Collection<Segment> segList = coveringMap.get(event);

		//get segment id
		if (segList != null && !segList.isEmpty()){
			for(Segment seg : segList) {
				String segname = seg.getId();
				Feature feature = new Feature("SegmentID", segname);
				feats.add(feature);
			}

		}
		return feats;
	}


}
