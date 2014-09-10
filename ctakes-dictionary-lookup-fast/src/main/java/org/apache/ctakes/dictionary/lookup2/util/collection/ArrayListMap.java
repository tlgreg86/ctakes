package org.apache.ctakes.dictionary.lookup2.util.collection;

import java.util.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 7/23/14
 */
final public class ArrayListMap<K, V> extends HashMap<K, List<V>> implements CollectionMap<K, V> {

   public ArrayListMap() {
      super();
   }

   /**
    * @param size initial size of the ArrayListMap
    */
   public ArrayListMap( final int size ) {
      super( size );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<Map.Entry<K, Collection<V>>> iterator() {
      final Iterator<Map.Entry<K, List<V>>> setIterator = entrySet().iterator();
      return new Iterator<Map.Entry<K, Collection<V>>>() {
         public boolean hasNext() {
            return setIterator.hasNext();
         }

         public Map.Entry<K, Collection<V>> next() {
            final Map.Entry<K, List<V>> next = setIterator.next();
            return new Map.Entry<K, Collection<V>>() {
               public K getKey() {
                  return next.getKey();
               }

               public Collection<V> getValue() {
                  return next.getValue();
               }

               public Collection<V> setValue( final Collection<V> value ) {
                  return null;
               }
            };
         }

         public void remove() {
         }
      };
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Collection<V>> getAllCollections() {
      return new HashSet<Collection<V>>( values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<V> getCollection( final K key ) {
      final List<V> list = get( key );
      if ( list != null ) {
         return list;
      }
      return Collections.emptyList();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<V> obtainCollection( final K key ) {
      List<V> list = get( key );
      if ( list == null ) {
         list = new ArrayList<>();
         put( key, list );
      }
      return list;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final K key, final V value ) {
      final Collection<V> values = get( key );
      return values != null && values.contains( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeValue( final K key, final V value ) {
      List<V> list = get( key );
      if ( list == null ) {
         list = new ArrayList<>();
         put( key, list );
      }
      return list.add( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeMap( final Map<K, V> map ) {
      boolean placedAny = false;
      for ( Map.Entry<K, V> entry : map.entrySet() ) {
         final boolean placed = placeValue( entry.getKey(), entry.getValue() );
         placedAny = placedAny || placed;
      }
      return placedAny;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void removeValue( final K key, final V value ) {
      final List<V> list = get( key );
      if ( list == null ) {
         return;
      }
      list.remove( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int addAllValues( final K key, final Collection<V> collection ) {
      List<V> list = get( key );
      if ( list == null ) {
         list = new ArrayList<>();
         put( key, list );
      }
      final int oldSize = list.size();
      list.addAll( collection );
      return list.size() - oldSize;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearCollection( final K key ) {
      List<V> list = get( key );
      if ( list != null ) {
         list.clear();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<K, Collection<V>> toSimpleMap() {
      final Map<K, Collection<V>> simpleMap = new HashMap<>( size() );
      for ( K key : keySet() ) {
         simpleMap.put( key, obtainCollection( key ) );
      }
      return simpleMap;
   }

}
