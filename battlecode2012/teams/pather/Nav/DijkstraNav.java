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
 * 
 * @deprecated uses too many bytecodes for operation as implemented. use bugnav!
 */
@Deprecated
public class DijkstraNav extends Navigation {
	private RobotController myRC;
	private HashMap<MapLocation, Integer> distance;
	private HashMap<MapLocation, MapLocation> previous;
	private PriorityQueue<MapLocation> queue;
	private final int INFINITY = 1000000;
	private HashSet<MapLocation> mapLocationsRemovedFromQueue;

	public DijkstraNav(RobotController myRC) {
		this.myRC = myRC;
		init();
	}

	/**
	 * Resets all the queues and such. Should be called to reset dijkstra
	 * computations
	 */
	public void init() {
		this.distance = new HashMap<MapLocation, Integer>();
		this.previous = new HashMap<MapLocation, MapLocation>();
		this.mapLocationsRemovedFromQueue = new HashSet<MapLocation>();

		/**
		 * Inner class used for comparing MapLocations by distance.
		 * 
		 * @author saf
		 */
		class MapLocationComparer implements Comparator {

			private RobotController myRC;

			public MapLocationComparer(RobotController rc) {
				this.myRC = rc;
			}

			/**
			 * Returns a negative integer, zero, or a positive integer as the
			 * first argument is less than, equal to, or greater than the
			 * second.
			 */
			@Override
			public int compare(Object arg0, Object arg1) {
				try {
					if (arg0.getClass() != MapLocation.class
							|| arg1.getClass() != MapLocation.class) {
						throw new RuntimeException(
								"This is intended to compare maplocations");
					} else {
						double dist0 = this.myRC.getLocation()
								.distanceSquaredTo((MapLocation) arg0);
						double dist1 = this.myRC.getLocation()
								.distanceSquaredTo((MapLocation) arg1);
						return (int) (dist1 - dist0);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return 0;
			}
		}
		this.queue = new PriorityQueue<MapLocation>(100,
				new MapLocationComparer(this.myRC));
	}

	@Override
	public void getNextMove(MapLocation target) {
		try {
			// get the next location
			MapLocation next = null;
			while (next == null) {
				System.out.println(Clock.getRoundNum());
				dijkstra(target, this.myRC.getLocation());
				next = this.previous.get(this.myRC.getLocation());

			}
			if (this.myRC.isMovementActive()) {
				return;
			}
			Direction ideal = myRC.getLocation().directionTo(next);
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
		init();
		this.distance.put(start, 0);
		this.queue.add(start);
		while (this.queue.size() != 0) {
			MapLocation u = this.queue.peek();
			System.out.println(Clock.getBytecodeNum());
			// System.out.println("Considering: " + u);
			if (this.distance.get(u) == INFINITY) {
				break;
			}
			if (u.equals(end)) {
				// the path has been computed
				System.out.println("the path has been computed");
				return;
			}
			this.mapLocationsRemovedFromQueue.add(this.queue.remove());
			for (MapLocation v : getMapLocationNeighbors(u)) {
				if (this.mapLocationsRemovedFromQueue.contains(v)) {
					continue;
				} else {
					if (this.myRC.senseTerrainTile(v) != null) {
						if (this.myRC.senseTerrainTile(v).equals(
								TerrainTile.OFF_MAP)) {
							this.distance.put(v, INFINITY);
						} else if (this.myRC.senseTerrainTile(v).equals(
								TerrainTile.VOID)
								&& !this.myRC.getType().isAirborne()) {
							this.distance.put(v, INFINITY);
						} else {
							double alt = this.distance.get(u)
									+ u.distanceSquaredTo(v);
							if (!this.distance.containsKey(v)
									|| alt < this.distance.get(v)) {
								this.distance.put(v, (int) alt);
								this.previous.put(v, u);
								this.queue.add(v);
							}
						}
					} else {
						double alt = this.distance.get(u)
								+ u.distanceSquaredTo(v);
						if (!this.distance.containsKey(v)
								|| alt < this.distance.get(v)) {
							this.distance.put(v, (int) alt);
							this.previous.put(v, u);
							this.queue.add(v);
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the neighboring locations of a given maplocation m.
	 * 
	 * @param m
	 * @return array of neighbors of m.
	 */
	public static MapLocation[] getMapLocationNeighbors(MapLocation m) {
		MapLocation[] neighbors = { m.add(Direction.EAST),
				m.add(Direction.NORTH), m.add(Direction.NORTH_EAST),
				m.add(Direction.NORTH_WEST), m.add(Direction.SOUTH),
				m.add(Direction.SOUTH_EAST), m.add(Direction.SOUTH_WEST),
				m.add(Direction.WEST) };
		return neighbors;
	}

}
