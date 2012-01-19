package team111.util;

import java.util.HashMap;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

/**
 * Represents a model of the game board. This stores the TerrainTiles that a
 * Robot has encountered.
 * 
 * Currently uses a HashTable, though a more efficient method may be possible.
 * 
 * TODO determine bytecode cost of this method of storing the board
 * 
 * @author saf
 * 
 */
public class BoardModel {
	private HashMap<MapLocation, TerrainTile> mapModel;

	public BoardModel() {
		this.mapModel = new HashMap<MapLocation, TerrainTile>();
	}

	/**
	 * Maps m as a key to value t
	 * 
	 * @param m
	 *            The MapLocation to store
	 * @param t
	 *            The TerrainTile at m
	 */
	public void put(MapLocation m, TerrainTile t) {
		this.mapModel.put(m, t);
	}

	/**
	 * Gets the value of the TerrainTile at m, or null if it is not in the
	 * mapModel
	 * 
	 * @param m
	 *            The MapLocation to determine
	 * @return the TerrainTile thought to be at m, or null if m not in mapModel
	 */
	public TerrainTile get(MapLocation m) {
		return this.mapModel.get(m);
	}
	
	/**
	 * Returns true when m is in the mapModel, else returns false
	 * @param m The MapLocation possibly in mapModel
	 * @return true when mapModel contains m
	 */
	public boolean containsKey(MapLocation m){
		return this.mapModel.containsKey(m);
	}
}
