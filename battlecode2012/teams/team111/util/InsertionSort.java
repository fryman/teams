package team111.util;

public class InsertionSort {

	/**
	 * Not useful right now. Contains an example of an insertion sort. Remember:
	 * insertion sort is O(n^2), but is in-place and stable. It is efficient for
	 * short lists.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		int[] enemies = { 2, 5, 3, 1, 6, 4 };
		for (int i : enemies) {
			System.out.print(i + " ");
		}
		long start = System.currentTimeMillis();
		int sortedLength = 1;
		int length = enemies.length;
		while (sortedLength < length) {
			int test = enemies[sortedLength];
			int compareTo = sortedLength - 1;
			while (true) {
				if (compareTo < 0) {
					sortedLength++;
					break;
				}
				if (test < enemies[compareTo]) {
					int temp = enemies[compareTo];
					enemies[compareTo] = test;
					enemies[compareTo + 1] = temp;
					compareTo--;
				} else {
					sortedLength++;
					break;
				}
			}
		}
		long end = System.currentTimeMillis();
		System.out.println();
		for (int i : enemies) {
			System.out.print(i + " ");
		}
		System.out.println();
		System.out.println("Time elapsed: " + (end - start));
	}
	
}
