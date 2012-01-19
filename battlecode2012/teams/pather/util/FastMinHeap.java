package pather.util;

/**
 * Implements a fast min-heap over an array backend. Makes a good priorityqueue!
 * 
 * Each object must be mapped to a double value for comparisons.
 * 
 * Implemented using the algorithms in CLRS.
 * 
 * Does not allow null keys.
 * 
 * @author saf
 * 
 */
public class FastMinHeap<E> {
	private E[] objectHeap;
	private double[] costHeap;
	private int size;
	private int capacity;
	private final int DEFAULT_CAPACITY = 8;
	private final int INFINITY = 1000000;

	/**
	 * Default constructor gives initial size DEFAULT_CAPACITY.
	 */
	public FastMinHeap() {
		this.objectHeap = (E[]) new Object[DEFAULT_CAPACITY];
		this.costHeap = new double[DEFAULT_CAPACITY];
		this.size = 0;
		this.capacity = DEFAULT_CAPACITY;
	}

	/**
	 * Sets the size of this heap to initialSize. initialSize must be >=1.
	 * 
	 * @param initialSize
	 *            Beginning capacity of this heap.
	 */
	public FastMinHeap(int initialSize) {
		if (initialSize < 1) {
			throw new RuntimeException("Initial size of heap must be >= 1");
		}
		this.objectHeap = (E[]) new Object[initialSize];
		this.costHeap = new double[initialSize];
		this.size = 0;
		this.capacity = initialSize;
	}

	/**
	 * Inserts an element of type E into this heap.
	 * 
	 * @param elementToInsert
	 *            Element of type E to insert into this heap.
	 * @param valueOfElem
	 *            Double value representing the cost of elementToInsert
	 */
	public void insert(E elementToInsert, double valueOfElem) {
		if (this.size == this.capacity) {
			increaseSize();
		}
		this.size += 1;
		this.objectHeap[this.size - 1] = elementToInsert;
		this.costHeap[this.size - 1] = this.INFINITY;
		this.decreaseKey(this.size - 1, valueOfElem);
	}

	/**
	 * Doubles the size of the backend arrays, copying all the elements to the
	 * new arrays.
	 */
	private void increaseSize() {
		E[] biggerE = (E[]) new Object[this.capacity * 2];
		double[] biggerC = new double[this.capacity * 2];
		for (int i = 0; i < this.size; i++) {
			biggerE[i] = this.objectHeap[i];
			biggerC[i] = this.costHeap[i];
		}
		this.costHeap = biggerC;
		this.objectHeap = biggerE;
		this.capacity = this.capacity * 2;
	}

	/**
	 * Removes and returns the element in this heap with the smallest key.
	 * 
	 * @return the element in this heap with the smallest key. throws heap
	 *         underflow exception when heap is empty.
	 */
	public E extractMin() {
		if (this.size == 0) {
			throw new RuntimeException(
					"Heap underflow. Cannot extractMin from an empty heap.");
		}
		E min = this.objectHeap[0];
		double costMin = this.costHeap[0];
		this.objectHeap[0] = this.objectHeap[this.size - 1];
		this.costHeap[0] = this.costHeap[this.size - 1];
		this.size -= 1;
		this.minHeapify(0);
		return min;
	}

	/**
	 * Returns the element in this heap with the smallest key.
	 * 
	 * @return the element in this heap with the smallest key. null is heap is
	 *         empty.
	 */
	public E minimum() {
		return objectHeap[0];
	}

	/**
	 * Decreases the value of elem's key to the new value given, which is
	 * assumed to be at least as small as elem's current value.
	 * 
	 * @param locationToChange
	 *            location in the heap of element that gets a new value.
	 * @param valueOfElem
	 *            new value of elem.
	 */
	public void decreaseKey(int locationToChange, double valueOfElem) {
		int i = locationToChange;
		if (valueOfElem > this.costHeap[i]) {
			throw new RuntimeException("New key is larger than current key");
		}
		this.costHeap[i] = valueOfElem;
		while (i > 0 && this.costHeap[this.parent(i)] > this.costHeap[i]) {
			// exchange i with parent(i)
			E tempE = objectHeap[i];
			double tempC = this.costHeap[i];
			this.objectHeap[i] = this.objectHeap[parent(i)];
			this.costHeap[i] = this.costHeap[parent(i)];
			this.objectHeap[parent(i)] = tempE;
			this.costHeap[parent(i)] = tempC;
			i = parent(i);
		}
	}

	/**
	 * Maintains the heap properties. When called, minHeapify assumes that the
	 * binary trees rooted at left(location) and right(location) are minheaps,
	 * but that objectHeap[location] might be larger than its children, thus
	 * violating the minHeap property. minHeapify lets the value at
	 * objectHeap[location] "float down" the heap so that the subtree rooted at
	 * location obeys the minHeap property.
	 * 
	 * @param location
	 *            Location to cascade downwards
	 */
	private void minHeapify(int location) {
		int l = left(location);
		int r = right(location);
		int smallest;
		if (l < this.size && this.costHeap[l] < this.costHeap[location]) {
			smallest = l;
		} else {
			smallest = location;
		}
		if (r < this.size && this.costHeap[r] < this.costHeap[smallest]) {
			smallest = r;
		}
		if (smallest != location) {
			// exchange i with smallest
			E tempE = objectHeap[location];
			double tempC = this.costHeap[location];
			this.objectHeap[location] = this.objectHeap[smallest];
			this.costHeap[location] = this.costHeap[smallest];
			this.objectHeap[smallest] = tempE;
			this.costHeap[smallest] = tempC;
			minHeapify(smallest);
		}
	}

	/**
	 * returns the location of the left child of i
	 * 
	 * @param i
	 *            location of interest in this heap
	 * @return location of left child of i
	 */
	private int left(int i) {
		return 2 * i + 1;
	}

	/**
	 * returns the location of the right child of i
	 * 
	 * @param i
	 *            location of interest in this heap
	 * @return location of right child of i
	 */
	private int right(int i) {
		return 2 * i + 2;
	}

	/**
	 * returns the location of the parent of i
	 * 
	 * @param i
	 *            location of interest in this heap
	 * @return location of parent of i
	 */
	private int parent(int i) {
		if (i % 2 == 0) {
			return i / 2 - 1;
		}
		return i / 2;
	}

	/**
	 * Prints out the objects next to the costs on separate lines.
	 * 
	 * @return string representing this heap.
	 */
	public String toString() {
		String out = "";
		for (int i = 0; i < this.size; i++) {
			out += this.objectHeap[i] + " " + this.costHeap[i] + "\n";
		}
		return out;
	}

	/**
	 * Returns the size of this heap
	 * 
	 * @return the size of this heap
	 */
	public int size() {
		return this.size;
	}

	/**
	 * Returns true if the size of this heap is zero, false otherwise.
	 * 
	 * @return true if the size of this heap is zero, false otherwise.
	 */
	public boolean isEmpty() {
		if (this.size == 0) {
			return true;
		}
		return false;
	}

	/**
	 * returns the location in the heap which contains elem (using .equals()).
	 * 
	 * returns -1 if elem is not in the heap.
	 * 
	 * @param elem
	 *            element to find in the heap
	 * @return the location in the heap which contains elem, or -1 if elem not
	 *         in the heap.
	 */
	public int locationOf(E elem) {
		for (int i = 0; i < this.size; i ++){
			if (this.objectHeap[i].equals(elem)){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Some tests... looks good from up here.
	 * 
	 * @param args
	 *            Command line arguments.
	 */
	public static void main(String[] args) {
		FastMinHeap<String> stringHeap = new FastMinHeap();
		stringHeap.insert("two", 2);
		stringHeap.insert("four", 4);
		stringHeap.insert("one", 1);
		stringHeap.insert("six", 6);
		stringHeap.insert("neg one", -1);
		System.out.println("min: " + stringHeap.extractMin());
		System.out.println(stringHeap.toString());
	}
}
