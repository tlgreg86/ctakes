package org.apache.ctakes.fhir.cr;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import org.apache.ctakes.core.cr.AbstractFileTreeReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.RelationArgumentUtil;
import org.apache.ctakes.fhir.element.FhirElementParser;
import org.apache.ctakes.fhir.resource.AnnotationBasicParser;
import org.apache.ctakes.fhir.resource.IdentifiedAnnotationBasicParser;
import org.apache.ctakes.fhir.resource.SectionBasicParser;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.ctakes.fhir.element.FhirElementFactory.CTAKES_FHIR_URL;
import static org.apache.ctakes.fhir.element.FhirElementFactory.RELATION_EXT_PREFIX;
import static org.apache.ctakes.fhir.resource.AnnotationBasicCreator.ID_NAME_ANNOTATION;
import static org.apache.ctakes.fhir.resource.IdentifiedAnnotationBasicCreator.ID_NAME_IDENTIFIED_ANNOTATION;
import static org.apache.ctakes.fhir.resource.SectionBasicCreator.ID_NAME_SECTION;
import static org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;


/**
 * Unfinished collection reader to create ctakes annotations from fhir files.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
@PipeBitInfo(
      name = "FhirXmlFileReader",
      description = "Reads fhir information from xml.", role = PipeBitInfo.Role.READER
)
public class FhirXmlFileReader extends AbstractFileTreeReader {

   static private final Logger LOGGER = Logger.getLogger( "FhirXmlFileReader" );

   /**
    * {@inheritDoc}
    */
   @Override
   protected void readFile( final JCas jCas, final File file ) throws IOException {
      jCas.reset();

      final Bundle bundle = readBundle( file );

      final SectionBasicParser sectionParser = new SectionBasicParser();
      final AnnotationBasicParser annotationParser = new AnnotationBasicParser();
      final IdentifiedAnnotationBasicParser iaParser = new IdentifiedAnnotationBasicParser();
      // Build map of resources to annotations, sections, etc.
      final List<BundleEntryComponent> entries = bundle.getEntry();
      final Map<IBaseResource, Annotation> resourceAnnotations = new HashMap<>( entries.size() );
      for ( BundleEntryComponent entry : entries ) {
         final IBaseResource resource = entry.getResource();
         final Annotation annotation = parseResource( jCas, resource, sectionParser, annotationParser, iaParser );
         if ( annotation != null ) {
            resourceAnnotations.put( resource, annotation );
         }
      }

      // Go through the (Basic) entries in the map and build relations
      for ( Map.Entry<IBaseResource, Annotation> resourceAnnotation : resourceAnnotations.entrySet() ) {
         if ( !Basic.class.isInstance( resourceAnnotation.getKey() ) ) {
            continue;
         }
         final Basic basic = (Basic) resourceAnnotation.getKey();
         final List<Extension> extensions = basic.getExtension();
         for ( Extension extension : extensions ) {
            final String url = extension.getUrl();
            if ( url.startsWith( CTAKES_FHIR_URL + RELATION_EXT_PREFIX ) ) {
               final Type type = extension.getValue();
               if ( type instanceof Reference ) {
                  final IBaseResource resource = ((Reference) type).getResource();
                  final Annotation target = resourceAnnotations.get( resource );
                  if ( target != null ) {
                     createRelation( jCas, url, resourceAnnotation.getValue(), target );
                  }
               }
            }
         }
      }

      // TODO build Map<Integer,Collection<Annotation>> with coref chain index to annotations that belong from the Basic Extensions
   }


   static private void createRelation( final JCas jCas, final String url,
                                       final Annotation source, final Annotation target ) {
      if ( source instanceof IdentifiedAnnotation && target instanceof IdentifiedAnnotation ) {
         final String category = url.substring( (CTAKES_FHIR_URL + RELATION_EXT_PREFIX).length() );
         final BinaryTextRelation relation
               = RelationArgumentUtil.createRelation( jCas, (IdentifiedAnnotation) source, (IdentifiedAnnotation) target,
               category );
         relation.addToIndexes();
      }
   }


   // TODO
   static private void createCoreference( final JCas jCas, Collection<Annotation> marked ) {

   }


   static private Annotation parseResource( final JCas jCas,
                                            final IBaseResource resource,
                                            final SectionBasicParser sectionParser,
                                            final AnnotationBasicParser annotationParser,
                                            final IdentifiedAnnotationBasicParser iaParser ) {
      if ( resource instanceof Composition ) {
         final Narrative narrative = ((Composition) resource).getText();
         final XhtmlNode html = narrative.getDiv();
         final String docText = html.allText();
         jCas.setDocumentText( docText );
         return null;
      }
      Annotation annotation = null;
      if ( resource instanceof Basic ) {
         final Basic basic = (Basic) resource;
         final String idName = FhirElementParser.getIdName( basic.getId() );
         switch ( idName ) {
            case ID_NAME_SECTION:
               annotation = sectionParser.parseResource( jCas, basic );
               break;
            case ID_NAME_ANNOTATION:
               annotation = annotationParser.parseResource( jCas, basic );
               break;
            case ID_NAME_IDENTIFIED_ANNOTATION:
               annotation = iaParser.parseResource( jCas, basic );
               break;
         }
         if ( annotation != null ) {
            annotation.addToIndexes();
            return annotation;
         }
      }
      return null;
   }


   static private Bundle readBundle( final File file ) throws IOException {
      IBaseResource baseResource;
      final FhirContext fhirContext = FhirContext.forDstu3();
      final IParser xmlParser = fhirContext.newXmlParser();
      try ( Reader reader = new BufferedReader( new FileReader( file ) ) ) {
         baseResource = xmlParser.parseResource( reader );

      } catch ( IOException | ConfigurationException | DataFormatException multE ) {
         LOGGER.error( "Could not read fhir from " + file.getAbsolutePath(), multE );
         throw new IOException( multE );
      }
      if ( baseResource == null ) {
         throw new IOException( "Null Bundle for file " + file.getAbsolutePath() );
      }
      if ( !Bundle.class.isInstance( baseResource ) ) {
         throw new IOException( "Resource is not a Bundle for file " + file.getAbsolutePath() );
      }
      return (Bundle) baseResource;
   }


}
