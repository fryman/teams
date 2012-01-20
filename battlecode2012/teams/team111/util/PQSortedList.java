package team111.util;

/**
 * Elements are kept sorted. Variety of algirthms used. Works as a circular
 * buffer.
 * 
 * @author saf
 * 
 * @param <T>
 */
public class PQSortedList<T> implements PriorityQueue<T> {

	private T[] objects;
	private double[] values;
	private int size;
	private int capacity;
	private final int DEFAULT_CAPACITY = 8;
	private final double INFINITY = Double.MAX_VALUE;
	private int head;
	private int tail;

	public PQSortedList() {
		this.objects = (T[]) new Object[DEFAULT_CAPACITY];
		this.values = new double[DEFAULT_CAPACITY];
		this.size = 0;
		this.capacity = DEFAULT_CAPACITY;
		this.head = 0;
		this.tail = 0;
	}

	public PQSortedList(int initialSize) {
		if (initialSize < DEFAULT_CAPACITY) {
			this.objects = (T[]) new Object[DEFAULT_CAPACITY];
			this.values = new double[DEFAULT_CAPACITY];
			this.capacity = DEFAULT_CAPACITY;
		} else {
			this.objects = (T[]) new Object[initialSize];
			this.values = new double[initialSize];
			this.capacity = initialSize;
		}
		this.size = 0;
		this.head = 0;
		this.tail = 0;
	}

	@Override
	public T extractMin() {
		if (this.size == 0) {
			return null;
		}
		int prevHead = head;
		head = (head + 1)%capacity;
		this.size--;
		return this.objects[prevHead];
	}

	@Override
	public T minimum() {
		if (this.size == 0) {
			return null;
		}
		return this.objects[head];
	}

	@Override
	public void decreaseKey(int loc, double cost) {
		throw new RuntimeException("Not implemented.");
	}

	@Override
	public void insert(T elem, double cost) {
		// put element at end of buffer
		this.objects[tail] = elem;
		this.values[tail] = cost;
		sort(this.size);
		this.tail = (this.tail + 1) % this.capacity;
		this.size++;
		checkAndResize();
	}

	private void sort(int offsetFromHead) {
		if (offsetFromHead <= 0) {
			return;
		}
		int offset = offsetFromHead;
		if (this.values[(head + offset) % capacity] < this.values[(head
				+ offset - 1)
				% capacity]) {
			// swap
			swap((head + offset) % capacity, (head + offset - 1) % capacity);
			sort(offset - 1);
		}
	}

	/**
	 * loc1 and loc2 should be real indices in the underlying array.
	 * 
	 * @param loc1
	 * @param loc2
	 */
	private void swap(int loc1, int loc2) {
		T temp = this.objects[loc1];
		double tempD = this.values[loc1];
		this.objects[loc1] = this.objects[loc2];
		this.values[loc1] = this.values[loc2];
		this.objects[loc2] = temp;
		this.values[loc2] = tempD;
	}

	/**
	 * If the buffer is:
	 * 
	 * X X 1(head) 2(tail) X X
	 * 
	 * fake index 0 (head) is real index 2 (useful in the underlying array).
	 * 
	 * @param loc
	 * @return
	 */
	private int realIndexFromFake(int loc) {
		if (loc >= this.size) {
			throw new RuntimeException("Index out of bounds.");
		}
		return (head + loc) % this.capacity;
	}

	private void checkAndResize() {
		if (this.size == this.capacity) {
			T[] biggerT = (T[]) new Object[this.capacity * 2];
			double[] biggerV = new double[this.capacity * 2];
			for (int i = 0; i < this.size; i++) {
				biggerT[i] = this.objects[(this.head + i) % this.capacity];
				biggerV[i] = this.values[(this.head + i) % this.capacity];
			}
			this.objects = biggerT;
			this.values = biggerV;
			this.capacity = this.capacity * 2;
			this.head = 0;
			this.tail = this.size;
		}
	}
	
	@Override
	public int size(){
		return this.size;
	}

	/**
	 * Expect:
	 * 
	 * 1
	 * 
	 * 0
	 * 
	 * 0
	 * 
	 * 1
	 * @param args
	 */
	public static void main(String[] args) {
		PQUnsortedList<String> test = new PQUnsortedList<String>();
		test.insert("one", 1);
		System.out.println(test.minimum());
		test.insert("zero", 0);
		System.out.println(test.minimum());
		test.insert("two", 2);
		System.out.println(test.minimum());
		test.extractMin();
		System.out.println(test.minimum());
	}

}
