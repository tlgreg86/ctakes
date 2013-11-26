package org.mitre.medfacts.uima;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URISyntaxException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;

public class CreateAssertionZonerDescriptors
{
  
  //public static final Class<? extends DataWriterFactory<String>> dataWriterFactoryClass = DefaultMaxentDataWriterFactory.class;

  /**
   * @param args
   * @throws URISyntaxException 
   * @throws FileNotFoundException 
   * @throws ResourceInitializationException 
   */
  public static void main(String[] args) throws Exception
  {
    CreateAssertionZonerDescriptors creator = new CreateAssertionZonerDescriptors();
    
    creator.execute();

  }
  
  public void execute() throws Exception
  {
    createZonerAggregateDescriptor();
    createZonerNormalDescriptor();
    createZonerMayoDescriptor();
  }
  
  public void createZonerAggregateDescriptor() throws Exception
  {
    AggregateBuilder builder = new AggregateBuilder();

////
    String generalSectionRegexFileUri =
      "org/mitre/medfacts/zoner/section_regex.xml";
    AnalysisEngineDescription zonerAnnotator =
        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
            generalSectionRegexFileUri
            );
    builder.add(zonerAnnotator);

    String mayoSectionRegexFileUri =
      "org/mitre/medfacts/uima/mayo_sections.xml";
    AnalysisEngineDescription mayoZonerAnnotator =
        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
            mayoSectionRegexFileUri
            );
    builder.add(mayoZonerAnnotator);
    
////
    
    File outputFile = new File("desc/analysis_engine/assertion_zoner__both_regular_and_mayo.xml");
    FileOutputStream outputStream = new FileOutputStream(outputFile);
    String outputFilePath = outputFile.getAbsolutePath();
    System.out.println("output descriptor file: " + outputFilePath);
    
    AnalysisEngineDescription description = builder.createAggregateDescription();
    
    description.toXML(outputStream);
  }
  
  public void createZonerNormalDescriptor() throws Exception
  {
    AggregateBuilder builder = new AggregateBuilder();

    String generalSectionRegexFileUri =
      "org/mitre/medfacts/zoner/section_regex.xml";
    AnalysisEngineDescription zonerAnnotator =
        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
            generalSectionRegexFileUri
            );
    builder.add(zonerAnnotator);

    File outputFile = new File("desc/analysis_engine/assertion_zoner__normal.xml");
    FileOutputStream outputStream = new FileOutputStream(outputFile);
    String outputFilePath = outputFile.getAbsolutePath();
    System.out.println("output descriptor file: " + outputFilePath);
    
    zonerAnnotator.toXML(outputStream);
  }
  
  public void createZonerMayoDescriptor() throws Exception
  {
    AggregateBuilder builder = new AggregateBuilder();

    String generalSectionRegexFileUri =
      "org/mitre/medfacts/uima/mayo_sections.xml";
    AnalysisEngineDescription zonerAnnotator =
        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
            generalSectionRegexFileUri
            );
    builder.add(zonerAnnotator);

    File outputFile = new File("desc/analysis_engine/assertion_zoner__mayo.xml");
    FileOutputStream outputStream = new FileOutputStream(outputFile);
    String outputFilePath = outputFile.getAbsolutePath();
    System.out.println("output descriptor file: " + outputFilePath);
    
    zonerAnnotator.toXML(outputStream);
  }
  
  
}
