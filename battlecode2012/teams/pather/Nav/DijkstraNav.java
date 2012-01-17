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
 * TODO This is incomplete. We can continue implementation if the current navigation
 * system is deemed inadequate.
 * 
 * @author saf
 * 
 */
public class DijkstraNav extends Navigation {
	private RobotController myRC;
	private HashMap<MapLocation, Integer> distance;
	private HashMap<MapLocation, MapLocation> previous;
	private PriorityQueue<MapLocation> queue;
	private final int INFINITY = 1000000;
	private HashSet<MapLocation> mapLocationsRemovedFromQueue;

	public DijkstraNav(RobotController myRC) {
		this.myRC = myRC;
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
		dijkstra(target);
	}

	/**
	 * Uses dijkstra's algorithm to find a path to the target.
	 * 
	 * Nontraversible locations are given distance infinity.
	 * 
	 * @param target
	 */
	public void dijkstra(MapLocation target) {
		MapLocation currentLoc = this.myRC.getLocation();
		this.distance.put(currentLoc, 0);
		this.queue.add(currentLoc);
		while (this.queue.size() != 0) {
			MapLocation u = this.queue.peek();
			if (this.distance.get(u) == INFINITY) {
				break;
			}
			this.queue.remove();
			for (MapLocation v : getMapLocationNeighbors(u)) {
				if (this.mapLocationsRemovedFromQueue.contains(v)) {
					continue;
				} else {
					double alt = this.distance.get(u) + u.distanceSquaredTo(v);
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
