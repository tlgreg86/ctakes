package org.apache.ctakes.core.cc.pretty.plaintext;

//import org.apache.log4j.Logger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.CasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * Writes Document text, pos, semantic types and cuis.  Each Sentence starts a new series of pretty text lines.
 * This version can be used in the UimaFit style with {@link org.apache.uima.fit.descriptor.ConfigurationParameter}
 * It cannot be used in the old Uima CPE style (e.g. the Uima CPE Gui) as the Uima CPE has problems with Fit Consumers.
 * There is a version that can be used with the CPE GUI:
 * {@link org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriterUima}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @see org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriter
 * @since 7/8/2015
 */
public class PrettyTextWriterFit extends CasConsumer_ImplBase {

   // UimaFit magically sets the value of the first instance variable to the parameter value - desired or otherwise
   static private final String PARAM_OUTPUTDIR = PrettyTextWriter.PARAM_OUTPUTDIR;
   @ConfigurationParameter(
         name = PARAM_OUTPUTDIR,
         mandatory = false,
         description = "Directory to which files should be saved",
         defaultValue = ""
   )
   private String fitOutputDirectoryPath;

//   static private final Logger LOGGER = Logger.getLogger( "PrettyTextWriterFit" );

   // delegate
   final private PrettyTextWriter _prettyTextWriter;

   public PrettyTextWriterFit() {
      super();
      _prettyTextWriter = new PrettyTextWriter();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      try {
         if ( fitOutputDirectoryPath != null ) {
            _prettyTextWriter.setOutputDirectory( fitOutputDirectoryPath );
         } else {
            _prettyTextWriter.setOutputDirectory( (String)uimaContext.getConfigParameterValue( PARAM_OUTPUTDIR ) );
         }
      } catch ( IllegalArgumentException | SecurityException multE ) {
         // thrown if the path specifies a File (not Dir) or by file system access methods
         throw new ResourceInitializationException( multE );
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final CAS aCAS ) throws AnalysisEngineProcessException {
      JCas jcas;
      try {
         jcas = aCAS.getJCas();
      } catch ( CASException casE ) {
         throw new AnalysisEngineProcessException( casE );
      }
      _prettyTextWriter.process( jcas );
   }

   /**
    * @return This Cas Consumer as an Analysis Engine
    * @throws org.apache.uima.resource.ResourceInitializationException if anything went wrong
    */
   static public AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return createAnnotatorDescription( "" );
   }

   /**
    * @param outputDirectoryPath may be empty or null, in which case the current working directory is used
    * @return This Cas Consumer as an Analysis Engine
    * @throws org.apache.uima.resource.ResourceInitializationException if anything went wrong
    */
   static public AnalysisEngineDescription createAnnotatorDescription( final String outputDirectoryPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( PrettyTextWriterFit.class,
            PrettyTextWriterFit.PARAM_OUTPUTDIR, outputDirectoryPath );
   }

}
