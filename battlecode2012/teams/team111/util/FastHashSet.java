package team111.util;

/**
 * Implements a generically typed hashset. Does not accept null entries.
 * 
 * @author saf
 * 
 */
public class FastHashSet<T> {
	private T[] set;
	private int capacity;
	private int size;
	private final int DEFAULT_SIZE = 8;
	private final double MAX_LOAD_FACTOR = 0.3;
	private double loadFactor;

	/**
	 * Constructor that sets the initial size of this set to the default size.
	 */
	public FastHashSet() {
		this.set = (T[]) new Object[DEFAULT_SIZE];
		this.capacity = DEFAULT_SIZE;
		this.size = 0;
		this.loadFactor = 0;
	}

	/**
	 * Constructor that sets the initial size of this set to the given initial
	 * size.
	 * 
	 * @param initialSize
	 *            Beginning size of this set.
	 */
	public FastHashSet(int initialSize) {
		this.set = (T[]) new Object[initialSize];
		this.capacity = initialSize;
		this.size = 0;
		this.loadFactor = 0;
	}

	/**
	 * Returns true when elem is in this set, false otherwise.
	 * 
	 * @param elem
	 *            Element to search for in this set.
	 * @return true when elem in this set, false otherwise.
	 */
	public boolean search(T elem) {
		if (elem == null) {
			throw new RuntimeException(
					"Cannot search for a null element in this set.");
		}
		int bucket = (elem.hashCode() & 0x7fffffff) % capacity;
		if (this.set[bucket] == null) {
			return false;
		}
		return set[bucket].equals(elem);
	}

	/**
	 * Inserts elem into this set.
	 * 
	 * @param elem
	 *            Element to insert into this set. May not be null.
	 */
	public void insert(T elem) {
		if (elem == null) {
			throw new RuntimeException(
					"Cannot insert a null element into this set.");
		}
		int bucket = (elem.hashCode() & 0x7fffffff) % capacity;
		if (this.set[bucket] == null) {
			size++;
		}
		this.set[bucket] = elem;
		checkLoadFactorAndResize();
	}

	/**
	 * Deletes an element from this set.
	 * 
	 * @param elem
	 *            Element to delete from this set. May not be null.
	 */
	public void delete(T elem) {
		if (elem == null) {
			throw new RuntimeException(
					"Cannot delete a null element from this set.");
		}
		int bucket = (elem.hashCode() & 0x7fffffff) % capacity;
		if (this.set[bucket].equals(elem)) {
			this.set[bucket] = null;
			this.size--;
			this.loadFactor = this.size / (1.0 * this.capacity);
		}
	}

	/**
	 * When the load factor is above the limit, double the size of this set.
	 */
	private void checkLoadFactorAndResize() {
		this.loadFactor = this.size / (1.0 * this.capacity);
		if (this.MAX_LOAD_FACTOR < this.loadFactor) {
			T[] biggerT = (T[]) new Object[capacity * 2];
			this.capacity = 2 * this.capacity;
			int bucket;
			this.size = 0;
			for (int i = 0; i < this.set.length; i++) {
				if (this.set[i] == null) {
					continue;
				}
				bucket = (this.set[i].hashCode() & 0x7fffffff) % capacity;
				if (biggerT[bucket] == null) {
					size++;
				}
				biggerT[bucket] = this.set[i];
			}
			this.set = biggerT;
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

	/**
	 * Expect:
	 * 
	 * 0.03
	 * 
	 * True
	 * 
	 * False
	 * 
	 * 0.02
	 * 
	 * False
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		FastHashSet<String> strSet = new FastHashSet<String>(100);
		strSet.insert("Tall");
		strSet.insert("dark");
		strSet.insert("and handsome");
		System.out.println(strSet.loadFactor);
		System.out.println(strSet.search("Tall"));
		System.out.println(strSet.search("tall"));
		strSet.delete("Tall");
		System.out.println(strSet.loadFactor);
		System.out.println(strSet.search("Tall"));
	}
}
