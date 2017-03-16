package org.apache.ctakes.dictionary.creator.util;


import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/12/2015
 */
final public class LambdaUtil {

   private LambdaUtil() {}

   static public final Function<String, String> asSelf = value -> value;

   static public final Function<String, Integer> zeroInt = value -> 0;
   static public final Function<String, Long> zeroLong = value -> 0l;

   static public final Function<String, Integer> one = value -> 1;

   static public final BinaryOperator<Integer> sumInt = ( count1, count2 ) -> count1 + count2;
   static public final BinaryOperator<Long> sumLong = ( count1, count2 ) -> count1 + count2;



}
