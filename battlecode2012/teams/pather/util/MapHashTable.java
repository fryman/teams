package pather.util;

import battlecode.common.TerrainTile;
import battlecode.common.GameConstants;

public class MapHashTable {
	private static final int SIZE = GameConstants.MAP_MAX_WIDTH
			* GameConstants.MAP_MAX_HEIGHT;

	MapInfo[] table;

	MapHashTable() {
		table = new MapInfo[SIZE];
		for (int i = 0; i < SIZE; i++)
			table[i] = null;
	}

	public TerrainTile get(int[] key) {
		int hash = key.hashCode() % SIZE;
		return table[hash].getValue();
	}

	public void put(int[] key, TerrainTile value) {
		int hash = key.hashCode() % SIZE;
		table[hash] = new MapInfo(key, value);
	}
}

// private int capacity = 100;
// private int size = 0;
//
// MapInfo[] table;
//
// MapHashTable(){
// table = new MapInfo[size];
// for (int i = 0; i < size; i++)
// table[i] = null;
// }
//
// public void Add(MapInfo m){
// int loc = m.hashCode() % capacity;
// table[loc]=m;
// size+=1;
// if (capacity< 2*size){
// //move all entries to bigger table
// }
//
// }
//
// public TerrainTile get(int[] loc){
// //String hash = "x"+loc[0]+"y"+loc[1];
// return table[hash].getValue();
// }

