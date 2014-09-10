package org.apache.ctakes.dictionary.lookup2.util.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 6/24/14
 */
public interface CollectionMap<K, V> extends Iterable<Map.Entry<K, Collection<V>>> {


   public Set<K> keySet();

   /**
    * @return all of the collections for all keys
    */
   public Collection<Collection<V>> getAllCollections();


   /**
    * check the collection map for a key
    *
    * @param key key for internal collection
    * @return <tt>true</tt> if this CollectionMap contain the key
    */
   public boolean containsKey( K key );

   /**
    * gets a collection mapped with key.  If one does not exist then an empty collection is returned
    *
    * @param key key for internal collection
    * @return collection mapped with key or an empty collection if there is none
    */
   public Collection<V> getCollection( K key );

   /**
    * obtains a collection mapped with key.  If one does not exist then one is added to this CollectionMap
    *
    * @param key key for internal collection
    * @return (possibly new) collection mapped with key
    */
   public Collection<V> obtainCollection( K key );

   /**
    * check the collection map for a key and value combination
    *
    * @param key   key for internal collection
    * @param value value to check in internal collection
    * @return <tt>true</tt> if this CollectionMap contain the value for the given key
    */
   public boolean containsValue( K key, V value );

   /**
    * places value into a collection mapped with key
    *
    * @param key   key for internal collection
    * @param value value to placeValue in internal collection
    * @return <tt>true</tt> if this set did not already contain the value
    */
   public boolean placeValue( K key, V value );

   /**
    * places each value of a map into a collection mapped with the appropriate key
    *
    * @param map map to store
    * @return <tt>true</tt> if this set did not already contain the value
    */
   public boolean placeMap( Map<K, V> map );

   /**
    * removes value from a collection mapped with key
    *
    * @param key   key for internal collection
    * @param value value to remove from internal collection
    */
   public void removeValue( K key, V value );

   /**
    * adds everything from the given collection to the internal collection mapped with key
    *
    * @param key        key for internal collection
    * @param collection collection of values to place in internal collection
    * @return the number of new items added
    */
   public int addAllValues( K key, Collection<V> collection );

   /**
    * clearCollection the collection mapped with key
    *
    * @param key key for internal collection
    */
   public void clearCollection( K key );

   /**
    * Copy of this object as a simple (java.util.Collection) map of Collection
    *
    * @return map of java.util.Collection
    */
   public Map<K, Collection<V>> toSimpleMap();

   public boolean isEmpty();

}
