package org.apache.ctakes.dictionary.lookup2.util.collection;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 9/5/2014
 */
@Immutable
final public class ImmutableCollectionMap<K, V> implements CollectionMap<K, V> {

   final private CollectionMap<K, V> _protectedMap;

   public ImmutableCollectionMap( final CollectionMap<K, V> collectionMap ) {
      _protectedMap = collectionMap;
   }

   public Iterator<Map.Entry<K, Collection<V>>> iterator() {
      return _protectedMap.iterator();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<K> keySet() {
      return Collections.unmodifiableSet( _protectedMap.keySet() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Collection<V>> getAllCollections() {
      return Collections.unmodifiableCollection( _protectedMap.getAllCollections() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsKey( final K key ) {
      return _protectedMap.containsKey( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<V> getCollection( final K key ) {
      return _protectedMap.getCollection( key );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean containsValue( final K key, final V value ) {
      return _protectedMap.containsValue( key, value );
   }

   /**
    * {@inheritDoc}
    *
    * @throws java.lang.UnsupportedOperationException
    */
   @Override
   public boolean placeValue( final K key, final V value ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    *
    * @throws java.lang.UnsupportedOperationException
    */
   @Override
   public boolean placeMap( final Map<K, V> map ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<V> obtainCollection( final K key ) {
      return getCollection( key );
   }

   /**
    * {@inheritDoc}
    *
    * @throws java.lang.UnsupportedOperationException
    */
   @Override
   public void removeValue( final K key, final V value ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    *
    * @throws java.lang.UnsupportedOperationException
    */
   @Override
   public int addAllValues( final K key, final Collection<V> collection ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    *
    * @throws java.lang.UnsupportedOperationException
    */
   @Override
   public void clearCollection( final K key ) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<K, Collection<V>> toSimpleMap() {
      return _protectedMap.toSimpleMap();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEmpty() {
      return _protectedMap.isEmpty();
   }

}
