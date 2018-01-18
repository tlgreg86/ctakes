package org.apache.ctakes.fhir.cc;


import org.apache.ctakes.core.util.EssentialAnnotationUtil;
import org.apache.ctakes.fhir.resource.*;
import org.apache.ctakes.fhir.util.NoteSpecs;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/15/2017
 */
final public class FhirDocComposer {

   static private final Logger LOGGER = Logger.getLogger( "FhirDocComposer" );

   static private final String SIMPLE_SECTION = "SIMPLE_SEGMENT";

   private FhirDocComposer() {
   }


   static public Bundle composeDocFhir( final JCas jCas ) {
      final NoteSpecs noteSpecs = new NoteSpecs( jCas );
      // creators
      final FhirResourceCreator<IdentifiedAnnotation, Basic> iaCreator = new IdentifiedAnnotationBasicCreator();
      final FhirResourceCreator<Annotation, Basic> aCreator = new AnnotationBasicCreator();
      final FhirResourceCreator<Segment, Basic> sectionCreator = new SectionBasicCreator();
      // essential types
      final Map<Segment, Collection<IdentifiedAnnotation>> sectionAnnotationMap
            = JCasUtil.indexCovered( jCas, Segment.class, IdentifiedAnnotation.class );
      final Collection<Basic> sections = new ArrayList<>( sectionAnnotationMap.size() );
      final Map<IdentifiedAnnotation, Collection<Integer>> markableCorefs = EssentialAnnotationUtil.createMarkableCorefs( jCas );
      final Collection<IdentifiedAnnotation> requiredAnnotations = EssentialAnnotationUtil.getRequiredAnnotations( jCas, markableCorefs );
      // Create map of annotations to Fhir Basics.
      final Map<IdentifiedAnnotation, Basic> annotationBasics = new HashMap<>();
      for ( Map.Entry<Segment, Collection<IdentifiedAnnotation>> sectionAnnotations : sectionAnnotationMap.entrySet() ) {
         final Segment segment = sectionAnnotations.getKey();
         final String segmentId = segment.getId();
         final Basic section = sectionCreator.createResource( jCas, sectionAnnotations.getKey(), noteSpecs );
         if ( !segmentId.isEmpty() && !segmentId.equals( SIMPLE_SECTION ) ) {
            sections.add( section );
         }
         final Reference sectionRef = new Reference( section );
         for ( IdentifiedAnnotation annotation : sectionAnnotations.getValue() ) {
            if ( !requiredAnnotations.contains( annotation ) ) {
               continue;
            }
            final Basic basic = iaCreator.createResource( jCas, annotation, noteSpecs );
            if ( !segmentId.isEmpty() && !segmentId.equals( SIMPLE_SECTION ) ) {
               basic.addExtension( FhirElementFactory.createSectionExtension( sectionRef ) );
            }
            final Collection<Integer> corefs = markableCorefs.get( annotation );
            if ( corefs != null ) {
               corefs.stream()
                     .map( FhirElementFactory::createCorefIndex )
                     .forEach( basic::addExtension );
            }
            annotationBasics.put( annotation, basic );
         }
      }
      // Add relations as reference extensions.
      final Map<org.apache.uima.jcas.tcas.Annotation, Basic> simpleAnnotationBasics = new HashMap<>();
      addRelations( jCas, noteSpecs, aCreator, annotationBasics, simpleAnnotationBasics );
      // Create a Bundle
      final Bundle bundle = new BundleCreator().createResource( jCas, null, noteSpecs );
      bundle.addEntry( new Bundle.BundleEntryComponent().setResource( PractitionerCtakes.getPractitioner() ) );
      addBundleResources( bundle, noteSpecs.getSubjects() );
      addBundleResources( bundle, sections );
      addBundleResources( bundle, annotationBasics.values() );
      addBundleResources( bundle, simpleAnnotationBasics.values() );
      return bundle;
   }

   static private void addBundleResources( final Bundle bundle, final Collection<? extends Resource> resources ) {
      resources.stream()
            .map( r -> new Bundle.BundleEntryComponent().setResource( r ) )
            .forEach( bundle::addEntry );
   }

   static private void addRelations( final JCas jCas,
                                     final NoteSpecs noteSpecs,
                                     final FhirResourceCreator<Annotation, Basic> aCreator,
                                     final Map<IdentifiedAnnotation, Basic> annotationBasics,
                                     final Map<org.apache.uima.jcas.tcas.Annotation, Basic> simpleBasics ) {
      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      for ( BinaryTextRelation relation : relations ) {
         final RelationArgument arg1 = relation.getArg1();
         final org.apache.uima.jcas.tcas.Annotation source = arg1.getArgument();
         Basic basicSource;
         if ( source instanceof IdentifiedAnnotation ) {
            basicSource = annotationBasics.get( (IdentifiedAnnotation) source );
         } else {
            basicSource = getSimpleBasic( jCas, source, noteSpecs, aCreator, simpleBasics );
         }
         final RelationArgument arg2 = relation.getArg2();
         final org.apache.uima.jcas.tcas.Annotation target = arg2.getArgument();
         Basic basicTarget;
         if ( target instanceof IdentifiedAnnotation ) {
            basicTarget = annotationBasics.get( (IdentifiedAnnotation) target );
         } else {
            basicTarget = getSimpleBasic( jCas, target, noteSpecs, aCreator, simpleBasics );
         }
         final String type = relation.getCategory();
         basicSource.addExtension( FhirElementFactory.createRelation( type, basicTarget ) );
      }
   }


   static private Basic getSimpleBasic( final JCas jCas,
                                        final org.apache.uima.jcas.tcas.Annotation annotation,
                                        final NoteSpecs noteSpecs,
                                        final FhirResourceCreator<Annotation, Basic> aCreator,
                                        final Map<org.apache.uima.jcas.tcas.Annotation, Basic> simpleBasics ) {
      final Basic basic = simpleBasics.get( annotation );
      if ( basic != null ) {
         return basic;
      }
      final Basic newBasic = aCreator.createResource( jCas, annotation, noteSpecs );
      simpleBasics.put( annotation, newBasic );
      return newBasic;
   }


}
