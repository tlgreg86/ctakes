package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import java.util.ArrayList;
import java.util.List;

import org.cleartk.classifier.feature.extractor.CleartkExtractor;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.cleartk.classifier.feature.function.FeatureFunction;
import org.cleartk.classifier.feature.function.FeatureFunctionExtractor;

public class ExtractorListFeatureFunctionConverter {
	public static List<FeatureFunctionExtractor> convert( List<? extends SimpleFeatureExtractor> extractors, FeatureFunction ff ) {

		List<FeatureFunctionExtractor> featureFunctionExtractors = new ArrayList<FeatureFunctionExtractor>();
		if (null!=extractors) {
			for (SimpleFeatureExtractor extractor : extractors) {
				featureFunctionExtractors.add(
						new FeatureFunctionExtractor(extractor,ff)
						);
			}
		}
		
		return featureFunctionExtractors;
	}

}
