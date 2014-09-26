package org.apache.ctakes.dictionary.lookup2.util.collection;

import java.util.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 6/24/14
 */
final public class HashSetMap<K, V> implements CollectionMap<K, V, Set<V>> {

   private final CollectionMap<K, V, Set<V>> _delegate;


   public HashSetMap() {
      final Map<K, Set<V>> hashMap = new HashMap<>();
      final CollectionCreator<V, Set<V>> creator = CollectionCreatorFactory.createSetCreator();
      _delegate = new DefaultCollectionMap<>( hashMap, creator );
   }

   /**
    * @param size initial size of the HashSetMap
    */
   public HashSetMap( final int size ) {
      final Map<K, Set<V>> hashMap = new HashMap<>( size );
      final CollectionCreator<V, Set<V>> creator = CollectionCreatorFactory.createSetCreator();
      _delegate = new DefaultCollectionMap<>( hashMap, creator );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<Map.Entry<K, Set<V>>> iterator() {
      return _delegate.iterator();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Set<V>> getAllCollections() {
      return new HashSet<>( _delegate.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> getCollection( final K key ) {
      return _delegate.getCollection( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> obtainCollection( final K key ) {
      return _delegate.obtainCollection( key );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final K key, final V value ) {
      return _delegate.containsValue( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeValue( final K key, final V value ) {
      return _delegate.placeValue( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean placeMap( final Map<K, V> map ) {
      return _delegate.placeMap( map );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void removeValue( final K key, final V value ) {
      _delegate.removeValue( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public <C extends Collection<V>> int addAllValues( final K key, final C collection ) {
      return _delegate.addAllValues( key, collection );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearCollection( final K key ) {
      _delegate.clearCollection( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int size() {
      return _delegate.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEmpty() {
      return _delegate.isEmpty();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsKey( final Object key ) {
      return _delegate.containsKey( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final Object value ) {
      return _delegate.containsValue( value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> get( final Object key ) {
      return _delegate.get( key );
   }

   // Modification Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> put( final K key, final Set<V> value ) {
      return _delegate.put( key, value );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<V> remove( final Object key ) {
      return _delegate.remove( key );
   }


   // Bulk Operations

   /**
    * {@inheritDoc}
    */
   @Override
   public void putAll( final Map<? extends K, ? extends Set<V>> map ) {
      _delegate.putAll( map );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clear() {
      _delegate.clear();
   }


   // Views

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<K> keySet() {
      return _delegate.keySet();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Set<V>> values() {
      return _delegate.values();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<Map.Entry<K, Set<V>>> entrySet() {
      return _delegate.entrySet();
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Map<K, Set<V>> toSimpleMap() {
      return _delegate;
   }

//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//   public HashSetMap() {
//      super();
//   }
//
//   /**
//    * @param size initial size of the HashSetMap
//    */
//   public HashSetMap( final int size ) {
//      super( size );
//   }
//
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public Iterator<Map.Entry<K, Collection<V>>> iterator() {
//      final Iterator<Map.Entry<K, Set<V>>> setIterator = entrySet().iterator();
//      return new Iterator<Map.Entry<K, Collection<V>>>() {
//         public boolean hasNext() {
//            return setIterator.hasNext();
//         }
//
//         public Map.Entry<K, Collection<V>> next() {
//            final Map.Entry<K, Set<V>> next = setIterator.next();
//            return new Map.Entry<K, Collection<V>>() {
//               public K getKey() {
//                  return next.getKey();
//               }
//
//               public Collection<V> getValue() {
//                  return next.getValue();
//               }
//
//               public Collection<V> setValue( final Collection<V> value ) {
//                  return null;
//               }
//            };
//         }
//
//         public void remove() {
//         }
//      };
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public Collection<Collection<V>> getAllCollections() {
//      return new HashSet<Collection<V>>( values() );
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public Collection<V> getCollection( final K key ) {
//      final Set<V> set = get( key );
//      if ( set != null ) {
//         return set;
//      }
//      return Collections.emptySet();
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public Collection<V> obtainCollection( final K key ) {
//      Set<V> set = get( key );
//      if ( set == null ) {
//         set = new HashSet<>();
//         put( key, set );
//      }
//      return set;
//   }
//
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public boolean containsValue( final K key, final V value ) {
//      final Collection<V> values = get( key );
//      return values != null && values.contains( value );
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public boolean placeValue( final K key, final V value ) {
//      Set<V> set = get( key );
//      if ( set == null ) {
//         set = new HashSet<>();
//         put( key, set );
//      }
//      return set.add( value );
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public boolean placeMap( final Map<K, V> map ) {
//      boolean placedAny = false;
//      for ( Map.Entry<K, V> entry : map.entrySet() ) {
//         final boolean placed = placeValue( entry.getKey(), entry.getValue() );
//         placedAny = placedAny || placed;
//      }
//      return placedAny;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void removeValue( final K key, final V value ) {
//      final Set<V> set = get( key );
//      if ( set == null ) {
//         return;
//      }
//      set.remove( value );
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public int addAllValues( final K key, final Collection<V> collection ) {
//      Set<V> set = get( key );
//      if ( set == null ) {
//         set = new HashSet<>();
//         put( key, set );
//      }
//      final int oldSize = set.size();
//      set.addAll( collection );
//      return set.size() - oldSize;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void clearCollection( final K key ) {
//      final Set<V> set = get( key );
//      if ( set != null ) {
//         set.clear();
//      }
//   }
//
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public Map<K, Collection<V>> toSimpleMap() {
//      final Map<K, Collection<V>> simpleMap = new HashMap<>( size() );
//      for ( K key : keySet() ) {
//         simpleMap.put( key, obtainCollection( key ) );
//      }
//      return simpleMap;
//   }

}
