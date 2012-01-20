package team111.util;

/**
 * Elements are order of insertion.
 * 
 * Probably only good for super short lists.
 * 
 * @author saf
 * 
 * @param <T>
 */
public class PQUnsortedList<T> implements PriorityQueue<T> {

	private T[] objects;
	private double[] values;
	private int size;
	private int capacity;
	private final int DEFAULT_CAPACITY = 8;
	private final double INFINITY = Double.MAX_VALUE;

	public PQUnsortedList() {
		this.objects = (T[]) new Object[DEFAULT_CAPACITY];
		this.values = new double[DEFAULT_CAPACITY];
		this.size = 0;
		this.capacity = DEFAULT_CAPACITY;
	}
	
	public PQUnsortedList(int initialSize){
		if (initialSize < DEFAULT_CAPACITY){
			this.objects = (T[]) new Object[DEFAULT_CAPACITY];
			this.values = new double[DEFAULT_CAPACITY];
			this.size = 0;
			this.capacity = DEFAULT_CAPACITY;
		} else {
			this.objects = (T[]) new Object[initialSize];
			this.values = new double[initialSize];
			this.size = 0;
			this.capacity = initialSize;
		}
	}
	
	@Override
	public int size(){
		return this.size;
	}

	@Override
	public T extractMin() {
		if (this.size == 0){
			return null;
		}
		double min = INFINITY;
		int loc = -1;
		for (int i = 0; i < this.size; i++){
			if (this.values[i] < min){
				min = this.values[i];
				loc = i;
			}
		}
		T lowest =  this.objects[loc];
		for (int i = loc; i < this.size - 1; i++) {
			this.objects[i] = this.objects[i + 1];
			this.values[i] = this.values[i + 1];
		}
		this.size -= 1;
		return lowest;
	}

	@Override
	public T minimum() {
		if (this.size == 0){
			return null;
		}
		double min = INFINITY;
		int loc = -1;
		for (int i = 0; i < this.size; i++){
			if (this.values[i] < min){
				min = this.values[i];
				loc = i;
			}
		}
		return this.objects[loc];
	}

	@Override
	public void decreaseKey(int loc, double cost) {
		this.values[loc] = cost;
	}

	@Override
	public void insert(T elem, double cost) {
		this.objects[this.size] = elem;
		this.values[this.size] = cost;
		this.size++;
		checkResize();
	}
	
	private void checkResize() {
		if (this.size == this.capacity) {
			T[] biggerT = (T[]) new Object[this.capacity * 2];
			double[] biggerV = new double[this.capacity * 2];
			for (int i = 0; i < this.size; i++) {
				biggerT[i] = this.objects[i];
				biggerV[i] = this.values[i];
			}
			this.objects = biggerT;
			this.values = biggerV;
			this.capacity = this.capacity * 2;
		}
	}

}
