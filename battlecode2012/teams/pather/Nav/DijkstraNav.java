package pather.Nav;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import battlecode.common.*;
import pather.*;
import pather.Nav.*;
import pather.util.*;

/**
 * Too many bytecodes for reasonable usage.
 * 
 * @author saf
 */
public class DijkstraNav extends Navigation {
	private RobotController myRC;
	private HashMap<MapLocation, Integer> distance;
	private HashMap<MapLocation, MapLocation> previous;
	private FastMinHeap<MapLocation> queue;
	private final int INFINITY = 1000000;
	private FastHashSet<MapLocation> mapLocationsRemovedFromQueue;
	private MapLocation start;
	private MapLocation end;

	public DijkstraNav(RobotController myRC) {
		this.myRC = myRC;
	}

	/**
	 * Resets all the queues and such. Should be called to reset dijkstra
	 * computations
	 */
	public void init(MapLocation goalLocation) {
		this.distance = new HashMap<MapLocation, Integer>();
		this.previous = new HashMap<MapLocation, MapLocation>();
		this.mapLocationsRemovedFromQueue = new FastHashSet<MapLocation>(200);
		this.queue = new FastMinHeap<MapLocation>();
		this.start = this.myRC.getLocation();
		this.end = goalLocation;
	}

	@Override
	public void getNextMove(MapLocation target) {
		try {
			// get the next location
			MapLocation next = null;
			while (next == null) {
				dijkstra(target, this.myRC.getLocation());
				next = this.previous.get(this.myRC.getLocation());
				// this.myRC.setIndicatorString(2, next.toString());
			}
			if (this.myRC.isMovementActive()) {
				return;
			}
			Direction ideal = myRC.getLocation().directionTo(next);
			this.myRC.setIndicatorString(1, next.toString());
			if (ideal == Direction.OMNI || ideal == Direction.NONE) {
				return;
			}
			// if this robot does not have enough flux to move, don't try to
			// move.
			if (this.myRC.getFlux() < this.myRC.getType().moveCost) {
				return;
			}
			if (myRC.canMove(ideal)) {
				if (myRC.getDirection() != ideal) {
					myRC.setDirection(ideal);
					myRC.setIndicatorString(1, "Turning ideal");
					return;
				}
				myRC.moveForward();
				myRC.setIndicatorString(1, "Moving ideal");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Uses dijkstra's algorithm to find a path to the target.
	 * 
	 * Nontraversible locations are given distance infinity.
	 * 
	 * The completed path is stored in previous.
	 * 
	 * @param target
	 */
	public void dijkstra(MapLocation start, MapLocation end) {
		init(end);
		this.distance.put(start, 0);
		this.queue.insert(start, 0);
		while (this.queue.size() != 0) {
			MapLocation u = this.queue.extractMin();
			if (this.distance.get(u) == INFINITY) {
				break;
			}
			if (u.equals(end)) {
				myRC.setIndicatorString(0,
						"path computed: " + Clock.getRoundNum());
				return;
			}
			this.mapLocationsRemovedFromQueue.insert(u);
			System.out.println("before insert: " + Clock.getBytecodeNum());
			this.mapLocationsRemovedFromQueue.search(u);
			System.out.println("after insert: " + Clock.getBytecodeNum());
			for (MapLocation v : getMapLocationNeighbors(u)) {
				// System.out.println("before minimum: " +
				// Clock.getBytecodeNum());
				// System.out.println("after minimum: " +
				// Clock.getBytecodeNum());
				if (this.mapLocationsRemovedFromQueue.search(v)) {
					continue;
				} else {
					if (!locationTraversible(v)) {
						this.distance.put(v, INFINITY);
					} else {
						double alt = this.distance.get(u)
								+ u.distanceSquaredTo(v)
								+ v.distanceSquaredTo(end);
						if (!this.distance.containsKey(v)
								|| alt < this.distance.get(v)) {
							this.distance.put(v, (int) alt);
							this.previous.put(v, u);
							int location = this.queue.locationOf(v);
							if (location == -1) {
								this.queue.insert(v, alt);
							} else {
								this.queue.decreaseKey(location, alt);
							}
						}
					}

				}
			}
		}
	}

	/**
	 * returns true when terrain is traversible, false otherwise. null terrain
	 * is considered traversible.
	 * 
	 * @param loc
	 *            MapLocation on which this robot will be travelling.
	 * @return returns true when terrain is traversible, false otherwise.
	 */
	public boolean locationTraversible(MapLocation loc) {
		try {
			TerrainTile vTerrain = this.myRC.senseTerrainTile(loc);
//			if (this.myRC.canSenseSquare(loc)) {
//				GameObject obstruction = this.myRC.senseObjectAtLocation(loc,
//						this.myRC.getType().level);
//				if (obstruction != null) {
//					return false;
//				}
//			}
			if (vTerrain == null) {
				return true;
			}
			if (vTerrain.equals(TerrainTile.VOID)) {
				if (this.myRC.getType().isAirborne()) {
					return true;
				} else {
					return false;
				}
			} else if (vTerrain.equals(TerrainTile.OFF_MAP)) {
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Returns the neighboring locations of a given maplocation m.
	 * 
	 * @param m
	 * @return array of neighbors of m.
	 */
	public static MapLocation[] getMapLocationNeighbors(MapLocation m) {
		// MapLocation[] neighbors = { m.add(Direction.EAST),
		// m.add(Direction.NORTH), m.add(Direction.NORTH_EAST),
		// m.add(Direction.NORTH_WEST), m.add(Direction.SOUTH),
		// m.add(Direction.SOUTH_EAST), m.add(Direction.SOUTH_WEST),
		// m.add(Direction.WEST) };
		MapLocation[] neighbors = { m.add(Direction.EAST),
				m.add(Direction.NORTH), m.add(Direction.SOUTH),
				m.add(Direction.WEST) };
		return neighbors;
	}

}
