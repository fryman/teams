package pather.util;

/**
 * Implements a fast array list (backed by an array).
 * 
 * @author saf
 * 
 */
public class FastArrayList<T> {
	private int size;
	private T[] data;
	private int capacity;
	private final int DEFAULT_CAPACITY = 8;

	public FastArrayList() {
		this.data = (T[]) new Object[DEFAULT_CAPACITY];
		this.size = 0;
		this.capacity = DEFAULT_CAPACITY;
	}

	public FastArrayList(int initialSize) {
		if (initialSize < 1) {
			throw new RuntimeException("Initial Size must be >= 1");
		}
		this.data = (T[]) new Object[initialSize];
		this.size = 0;
		this.capacity = initialSize;
	}

	public void add(T elem) {
		this.data[this.size] = elem;
		this.size++;
		checkResize();
	}

	public void remove(T elem) {
		int locToRemove = -1;
		for (int i = 0; i < this.size; i++) {
			if (this.data[i] != null && this.data[i].equals(elem)) {
				locToRemove = i;
				break;
			}
		}
		if (locToRemove != -1) {
			for (int i = locToRemove; i < this.size - 1; i++) {
				this.data[i] = this.data[i + 1];
			}
			this.size -= 1;
		}
	}

	public void remove(int loc) {

	}

	public void get(int loc) {

	}

	public void set(int loc) {

	}

	public void contains(T elem) {

	}

	private void checkResize() {
		if (this.size == this.capacity) {
			T[] biggerT = (T[]) new Object[this.capacity * 2];
			for (int i = 0; i < this.size; i++) {
				biggerT[i] = this.data[i];
			}
			this.data = biggerT;
			this.capacity = this.capacity * 2;
		}
	}
}
