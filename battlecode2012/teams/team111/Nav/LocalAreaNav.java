package team111.Nav;

import battlecode.common.*;
import team111.util.*;

/**
 * Uses a priority queue. Adds neighboring locations to queue with cost
 * function: cost = distSqrFromEnd.^0.5 + distSqrFromStart.^0.5;
 * 
 * @author saf
 * 
 */
public class LocalAreaNav extends Navigation {
	private PriorityQueue<MapLocation> locationsToHit;
	private int movesSinceCalc = 0;
	private final int MAX_MOVES_TO_CALCULATE = 2;
	private MapLocation target = null;
	private final int INFINITY = 100000;
	private MapLocation next = null;
	private BugNav altNav;
	private boolean altNavInUse = false;
	private boolean waited = false;

	public LocalAreaNav(RobotController rc) {
		this.myRC = rc;
		this.altNav = new BugNav(rc);
	}

	public void emptyPQ() {
		locationsToHit = new PQUnsortedList<MapLocation>(30);
		this.next = null;
	}

	@Override
	public void getNextMove(MapLocation target) {
		try {
			if (altNavInUse) {
				useAltNav(target);
			}
			if (movesSinceCalc == MAX_MOVES_TO_CALCULATE || this.target == null
					|| !this.target.equals(target)) {
				// recalculate
				this.next = null;
				this.target = target;
				movesSinceCalc = 0;
				computeLocationsToHit(this.myRC.getLocation(), target);
				// System.out.println(locationsToHit.size() + " " +
				// locationsToHit.minimum());
			}
			// get the next location
			while (next == null) {
				next = locationsToHit.extractMin();
				movesSinceCalc++;
			}
			if (this.myRC.isMovementActive()) {
				return;
			}
			Direction ideal = myRC.getLocation().directionTo(next);
			this.myRC.setIndicatorString(1, next.toString());
			this.myRC.setIndicatorString(0, myRC.getLocation().add(ideal)
					.toString());
			if (ideal == Direction.OMNI || ideal == Direction.NONE) {
				myRC.setIndicatorString(0, "Good Dir: " + ideal);
				return;
			}
			// if this robot does not have enough flux to move, don't try to
			// move.
			if (this.myRC.getFlux() < this.myRC.getType().moveCost) {
				return;
			}
			this.myRC.setIndicatorString(
					2,
					Boolean.toString(myRC.canMove(ideal))
							+ " "
							+ this.myRC.senseTerrainTile(this.myRC
									.getLocation().add(ideal)) + " "
							+ this.locationsToHit.size() + " "
							+ locationTraversible(next));
			if (myRC.canMove(ideal)) {
				if (myRC.getDirection() != ideal) {
					myRC.setDirection(ideal);
					myRC.setIndicatorString(1, "Turning ideal");
					return;
				}
				waited = false;
				myRC.moveForward();
				next = null;
				myRC.setIndicatorString(1, "Moving ideal "
						+ this.movesSinceCalc);
				return;
			} else if (!waited) {
				this.waited = true;
				return;
			} else {
				altNavInUse = true;
				waited = false;
				useAltNav(target);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void computeLocationsToHit(MapLocation start, MapLocation goal) {
		emptyPQ();
		for (MapLocation m : getDistThreeNeighbors(start)) {
			double cost = costOfWaypoint(start, goal, m);
			if (cost != INFINITY) {
				locationsToHit.insert(m, cost);
			}
		}
	}

	private double costOfWaypoint(MapLocation start, MapLocation goal,
			MapLocation waypoint) {
		if (!locationTraversible(waypoint)) {
			return INFINITY;
		}
		return Math.sqrt(waypoint.distanceSquaredTo(goal))
				+ Math.sqrt(waypoint.distanceSquaredTo(start));
	}

	private void useAltNav(MapLocation m) {
		this.altNav.getNextMove(m);
		if (!this.altNav.onWall()) {
			altNavInUse = false;
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

	public static MapLocation[] getDistThreeNeighbors(MapLocation m) {
		MapLocation[] neighbors = { m.add(0, 1), m.add(1, 1), m.add(1, 0),
				m.add(1, -1), m.add(0, -1), m.add(-1, -1), m.add(-1, 0),
				m.add(-1, 1), m.add(0, 2), m.add(1, 2), m.add(2, 2),
				m.add(2, 1), m.add(2, 0), m.add(2, -1), m.add(2, -2),
				m.add(1, -2), m.add(0, -2), m.add(-1, -2), m.add(-2, -2),
				m.add(-2, -1), m.add(-2, 0), m.add(-2, 1), m.add(-2, 2),
				m.add(-1, 2) };
		return neighbors;
	}

	public static MapLocation[] getDistTwoNeighbors(MapLocation m) {
		MapLocation[] neighbors = { m.add(0, 1), m.add(1, 1), m.add(1, 0),
				m.add(1, -1), m.add(0, -1), m.add(-1, -1), m.add(-1, 0),
				m.add(-1, 1) };
		return neighbors;
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
			// if (this.myRC.canSenseSquare(loc)) {
			// GameObject obstruction = this.myRC.senseObjectAtLocation(loc,
			// this.myRC.getType().level);
			// if (obstruction != null) {
			// return false;
			// }
			// }
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
