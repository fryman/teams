package team111.util;

/**
 * Implements a faster hashmap. Maps keys of type K to values of type V. Does
 * not accept null keys or values.
 * 
 * @author saf
 * 
 * @param <K>
 *            type of key
 * @param <V>
 *            type of value
 */
public class FastHashMap<K, V> {
	private K[] keys;
	private V[] values;
	private int size;
	private int capacity;
	private final double MAX_LOAD_FACTOR = 0.3;
	private final int DEFAULT_SIZE = 99999;
	private double loadFactor;

	/**
	 * Constructor that sets the initial size of this map to the default size.
	 */
	public FastHashMap() {
		this.capacity = DEFAULT_SIZE;
		this.loadFactor = 0;
		this.size = 0;
		this.keys = (K[]) new Object[DEFAULT_SIZE];
		this.values = (V[]) new Object[DEFAULT_SIZE];
	}

	/**
	 * Constructor that sets the initial size of this map to the given initial
	 * size.
	 * 
	 * @param initialSize
	 *            Beginning size of this map.
	 */
	public FastHashMap(int initialSize) {
		this.capacity = initialSize;
		this.loadFactor = 0;
		this.size = 0;
		this.keys = (K[]) new Object[initialSize];
		this.values = (V[]) new Object[initialSize];
	}

	/**
	 * Returns true when key is in this map, false otherwise. Does not accept
	 * null key.
	 * 
	 * @param key
	 *            Key to search for in this map.
	 * @return true when elem in this set, false otherwise.
	 */
	public boolean containsKey(K key) {
		if (key == null) {
			throw new RuntimeException(
					"Cannot search for a null element in this map.");
		}
		int bucket = (key.hashCode() & 0x7fffffff) % capacity;
		if (this.keys[bucket] == null) {
			return false;
		}
		return keys[bucket].equals(key);
	}

	/**
	 * Returns true when value is in this map, false otherwise. Slow!
	 * 
	 * @param value
	 *            Value to search for in this map.
	 * @return true when elem in this set, false otherwise.
	 */
	public boolean containsValue(V value) {
		if (value == null) {
			throw new RuntimeException(
					"Cannot search for a null element in this map.");
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Inserts (key,value) into this map
	 * 
	 * @param key
	 *            key to insert into this set. May not be null.
	 * @param value
	 *            value to insert into this set. May not be null.
	 */
	public void put(K key, V value) {
		if (key == null || value == null) {
			throw new RuntimeException(
					"Cannot insert a null element into this map.");
		}
		int bucket = (key.hashCode() & 0x7fffffff) % capacity;
		if (this.keys[bucket] == null) {
			size++;
		}
		this.keys[bucket] = key;
		this.values[bucket] = value;
		checkLoadFactorAndResize();
	}

	/**
	 * Returns the value associated with the given key in the map. Returns null
	 * if the key is not in the map.
	 * 
	 * @param key
	 *            Key to lookup associated value
	 * @return the value associated with the given key in the map. null if key
	 *         is not in map.
	 */
	public V get(K key) {
		if (key == null) {
			throw new RuntimeException("Cannot lookup a null key.");
		}
		int bucket = (key.hashCode() & 0x7fffffff) % capacity;
		if (this.keys[bucket] == null) {
			return null;
		}
		if (this.keys[bucket].equals(key)) {
			return this.values[bucket];
		}
		return null;
	}

	/**
	 * Deletes a key and returns the corresponding value from this set.
	 * 
	 * @param key
	 *            Key to delete from this set. May not be null.
	 * @return value mapped to key. null if key not in map.
	 */
	public V delete(K key) {
		if (key == null) {
			throw new RuntimeException(
					"Cannot delete a null element from this map.");
		}
		int bucket = (key.hashCode() & 0x7fffffff) % capacity;
		if (this.keys[bucket] == null) {
			return null;
		}
		if (this.keys[bucket].equals(key)) {
			this.keys[bucket] = null;
			this.size--;
			this.loadFactor = this.size / (1.0 * this.capacity);
			return this.values[bucket];
		}
		return null;
	}

	/**
	 * When the load factor is above the limit, double the size of this set.
	 */
	private void checkLoadFactorAndResize() {
		this.loadFactor = this.size / (1.0 * this.capacity);
		if (this.MAX_LOAD_FACTOR < this.loadFactor) {
			K[] biggerK = (K[]) new Object[capacity * 2];
			V[] biggerV = (V[]) new Object[capacity * 2];
			this.capacity = 2 * this.capacity;
			int bucket;
			this.size = 0;
			for (int i = 0; i < this.keys.length; i++) {
				if (this.keys[i] == null) {
					continue;
				}
				bucket = (this.keys[i].hashCode() & 0x7fffffff) % capacity;
				if (biggerK[bucket] == null) {
					size++;
				}
				biggerK[bucket] = this.keys[i];
				biggerV[bucket] = this.values[i];
			}
			this.keys = biggerK;
			this.values = biggerV;
			this.loadFactor = this.size / (1.0 * this.capacity);
		}
	}

	/**
	 * Returns the load factor ("fullness") of this set.
	 * 
	 * @return load factor for this set.
	 */
	public double getLoadFactor() {
		return this.loadFactor;
	}
	
	public String toString(){
		String out = "";
		for (int i = 0; i < keys.length; i ++){
			if (keys[i] != null){
				out += "Key: " + keys[i] + ", Val: " + values[i] + "\n";
			}
		}
		return out;
	}

	/**
	 * Expect:
	 * 
	 * 0.03
	 * 
	 * True
	 * 
	 * False
	 * 
	 * 1
	 * 
	 * 0.02
	 * 
	 * False
	 * 
	 * 4
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		FastHashMap<String, Integer> samMap = new FastHashMap<String, Integer>(
				100);
		samMap.put("One", 1);
		samMap.put("Two", 2);
		samMap.put("Three", 3);
		System.out.println(samMap.loadFactor);
		System.out.println(samMap.containsKey("One"));
		System.out.println(samMap.containsKey("Four"));
		System.out.println(samMap.get("One"));
		samMap.delete("One");
		System.out.println(samMap.loadFactor);
		System.out.println(samMap.containsKey("One"));
		samMap.put("Four", 4);
		System.out.println(samMap.get("Four"));
	}
}
