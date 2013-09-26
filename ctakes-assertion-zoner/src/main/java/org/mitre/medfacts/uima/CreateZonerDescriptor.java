package org.mitre.medfacts.uima;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.ctakes.core.ae.DocumentIdPrinterAnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.xml.sax.SAXException;

public class CreateZonerDescriptor
{

  /**
   * @param args
   * @throws URISyntaxException 
   * @throws FileNotFoundException 
   * @throws ResourceInitializationException 
   */
  public static void main(String[] args) throws Exception
  {
    CreateZonerDescriptor creator = new CreateZonerDescriptor();
    
    creator.execute();

  }
  
  public void execute() throws Exception
  {
    AggregateBuilder builder = new AggregateBuilder();

//    AnalysisEngineDescription documentIdPrinter =
//        AnalysisEngineFactory.createPrimitiveDescription(DocumentIdPrinterAnalysisEngine.class);
//    builder.add(documentIdPrinter);
  
    URI generalSectionRegexFileUri =
      this.getClass().getClassLoader().getResource("org/mitre/medfacts/uima/section_regex.xml").toURI();
//    ExternalResourceDescription generalSectionRegexDescription = ExternalResourceFactory.createExternalResourceDescription(
//        SectionRegexConfigurationResource.class, new File(generalSectionRegexFileUri));
    AnalysisEngineDescription zonerAnnotator =
        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
            generalSectionRegexFileUri
            );
    builder.add(zonerAnnotator);

    URI mayoSectionRegexFileUri =
        this.getClass().getClassLoader().getResource("org/mitre/medfacts/uima/mayo_sections.xml").toURI();
//      ExternalResourceDescription mayoSectionRegexDescription = ExternalResourceFactory.createExternalResourceDescription(
//          SectionRegexConfigurationResource.class, new File(mayoSectionRegexFileUri));
    AnalysisEngineDescription mayoZonerAnnotator =
        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
            mayoSectionRegexFileUri
            );
    builder.add(mayoZonerAnnotator);
    
    FileOutputStream outputStream = new FileOutputStream("desc/aggregateAssertionZoner.xml");
    
    AnalysisEngineDescription description = builder.createAggregateDescription();
    
    description.toXML(outputStream);
  }

}
