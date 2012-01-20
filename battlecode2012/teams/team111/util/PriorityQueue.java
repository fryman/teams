package team111.util;

public interface PriorityQueue<T> {
	public T extractMin();

	public T minimum();

	public void decreaseKey(int loc, double cost);

	public void insert(T elem, double cost);
	
	public int size();
}
