package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cleartk.classifier.Feature;
import org.cleartk.classifier.feature.function.FeatureFunction;

public class FedaFeatureFunction implements FeatureFunction {

	  public static final String DOMAIN_ADAPTATION_ALGORITHM = "FEDA";
	  List<String> domainIds;
	  String currentDomain;
	  
	  public FedaFeatureFunction ( List<String> domains ) {
		  domainIds = domains;
	  }
	  
	  /**
	   * @return replicate the feature for the current domain, the original is a "general" domain
	   */
	  @Override
	  public List<Feature> apply(Feature feature) {
	    Object featureValue = feature.getValue();
	    
	    List<Feature> fedaFeatures = new ArrayList<Feature>();  
	    fedaFeatures.add(feature);
	    if (null==currentDomain) { return fedaFeatures; }
	    
//	    for (String domain : domainIds) {
//		    String featureName = Feature.createName(domain, DOMAIN_ADAPTATION_ALGORITHM, feature.getName());
	    String featureName = Feature.createName(currentDomain, DOMAIN_ADAPTATION_ALGORITHM, feature.getName());
	    
	    fedaFeatures.add(
	    		new Feature(
	    				featureName,
	    				featureValue.toString() )
	    		);
//	    }
	    return fedaFeatures;
	  }

	  public void setDomain(String domain) {
		  currentDomain = domain;
	  }

}
