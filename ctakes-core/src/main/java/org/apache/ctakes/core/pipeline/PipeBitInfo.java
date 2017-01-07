package org.apache.ctakes.core.pipeline;


import java.lang.annotation.*;

/**
 * Annotation that should be used for Collection Readers, Annotators, and Cas Consumers (Writers).
 * It may be useful for pipeline builder UIs and other human-pipeline interaction.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/22/2016
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Inherited
public @interface PipeBitInfo {
   enum Role {
      READER, ANNOTATOR, WRITER, SPECIAL
   }

   String NO_INPUT = "No Required Input.";
   String NO_OUTPUT = "No Produced Output.";
   String NO_PARAMETERS = "No Parameters.";
   String NO_DEPENDENCIES = "No Dependencies.";
   String NEW_JCAS = "New JCas.";
   String POPULATED_JCAS = "Populated JCas.";

   /**
    * @return Human-readable name of the Reader, Annotator, or Writer
    */
   String name();

   /**
    * @return Role played within a pipeline
    */
   Role role() default Role.ANNOTATOR;

   /**
    * @return Human-readable description of the purpose of the Reader, Annotator, or Writer
    */
   String description();

   /**
    * @return Human-readable description of the input of the Reader, Annotator, or Writer
    */
   String input() default NO_INPUT;

   /**
    * @return Human-readable description of the output of the Reader, Annotator, or Writer
    */
   String output();

   /**
    * @return Human-readable names of Configuration Parameters
    */
   String[] parameters() default { NO_PARAMETERS };

   /**
    * @return Human-readable array of dependencies of the Reader, Annotator, or Writer
    */
   String[] dependencies() default { NO_DEPENDENCIES };

}
