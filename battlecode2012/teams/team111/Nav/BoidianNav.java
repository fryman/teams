package team111.Nav;

import team111.util.FastHashSet;
import team111.util.PQSortedList;
import team111.util.PQUnsortedList;
import team111.util.PriorityQueue;
import battlecode.common.*;

public class BoidianNav extends Navigation {
	private RobotController myRC;

	public BoidianNav(RobotController myRC) {
		this.myRC = myRC;
	}

	/**
	 * The behavior we are looking for is Boid-like. The rules inherent are
	 * such:
	 * 
	 * separation: steer to avoid crowding local flockmates
	 * 
	 * alignment: steer towards the average heading of local flockmates
	 * 
	 * cohesion: steer to move toward the average position (center of mass) of
	 * local flockmates
	 * 
	 * obstacle avoidance: steer to avoid going over non-traversible land
	 */
	@Override
	public void getNextMove(MapLocation target) {
		try {
			MapLocation next = determineBestLocation();
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
			if (myRC.canMove(ideal)) {
				if (myRC.getDirection() != ideal) {
					myRC.setDirection(ideal);
					myRC.setIndicatorString(1, "Turning ideal");
					return;
				}
				myRC.moveForward();
				return;
			} else {
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MapLocation determineBestLocation() {
		try {
			MapLocation[] options = getNearestNineNeighbors(this.myRC
					.getLocation());
			PriorityQueue<MapLocation> pq = new PQUnsortedList<MapLocation>(10);

			Robot[] friends = this.myRC.senseNearbyGameObjects(Robot.class);
			double[] friendCOM = new double[2];
			double[] friendDir = new double[2];
			double numFriends = 0;
			for (Robot f : friends) {
				RobotInfo fInfo = myRC.senseRobotInfo(f);
				if (fInfo.team != this.myRC.getTeam()) {
					continue;
				}
				
				if (fInfo.type == RobotType.ARCHON) {
					friendCOM[0] += 5*fInfo.location.x;
					friendCOM[1] += 5*fInfo.location.y;
					friendDir[0] += 5*fInfo.direction.dx;
					friendDir[1] += 5*fInfo.direction.dy;
					numFriends += 5;
				} else {
					friendCOM[0] += fInfo.location.x;
					friendCOM[1] += fInfo.location.y;
					friendDir[0] += fInfo.direction.dx;
					friendDir[1] += fInfo.direction.dy;
					numFriends++;
				}
			}
			friendCOM[0] = friendCOM[0] / numFriends;
			friendCOM[1] = friendCOM[1] / numFriends;
			friendDir[0] = friendDir[0] / numFriends;
			friendDir[1] = friendDir[1] / numFriends;
			for (MapLocation m : options) {
				if (!locationTraversible(m)) {
					continue;
				} else {
					// we have a location that is traversible. now we need to
					// score it.
					Direction ideal = myRC.getLocation().directionTo(m);
					double score = 10 * (m.x - friendCOM[0])
							* (m.x - friendCOM[0]) + 10 * (m.y - friendCOM[1])
							* (m.y - friendCOM[1]);

					double cos = (friendDir[0] * ideal.dx + friendDir[1]
							* ideal.dy)
							/ (Math.sqrt(friendDir[0] * friendDir[0]
									+ friendDir[1] * friendDir[1]) * Math
									.sqrt(ideal.dx * ideal.dx + ideal.dy
											* ideal.dy));
					score = score / cos;
					pq.insert(m, score);
				}
			}
			return pq.extractMin();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the nearest 9 locations, including the current location
	 * 
	 * @param m
	 * @return
	 */
	public static MapLocation[] getNearestNineNeighbors(MapLocation m) {
		MapLocation[] neighbors = { m.add(0, 1), m.add(1, 1), m.add(1, 0),
				m.add(1, -1), m.add(0, -1), m.add(-1, -1), m.add(-1, 0),
				m.add(-1, 1), m.add(0, 0) };
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
			if (this.myRC.canSenseSquare(loc)) {
				GameObject obstruction = this.myRC.senseObjectAtLocation(loc,
						this.myRC.getType().level);
				if (obstruction != null) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

}
