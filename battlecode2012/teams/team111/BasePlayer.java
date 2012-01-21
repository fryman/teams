package team111;

import team111.Nav.BoidianNav;
import team111.Nav.BugNav;
import team111.Nav.DijkstraNav;
import team111.Nav.LocalAreaNav;
import team111.Nav.Navigation;
import team111.util.BoardModel;
import team111.util.FastArrayList;
import battlecode.common.*;

public abstract class BasePlayer extends StaticStuff {
	protected Navigation nav = null;
	public static final int ARCHON_PING_MESSAGE = 47;
	public static final int ARCHON_ENEMY_MESSAGE = 98;
	public static final int NON_ARCHON_PING_MESSAGE = 982;
	public static final int SOLDIER_PING_MESSAGE = 9394;
	public static final int SCOUT_PING_MESSAGE = 212;
	public static final int AIRBORNE_PING_MESSAGE = 2123;
	public static final int GROUND_PING_MESSAGE = 21122;
	protected Message[] messages;

	public BasePlayer(RobotController rc) {
		this.nav = new LocalAreaNav(rc);
	}

	/**
	 * Code to run once per turn, at the very end
	 * 
	 * Includes RobotController.yield() statement, so this method should be
	 * called when the Robot is done with its turn.
	 */
	public void runAtEndOfTurn() {
		aboutToDie();
		// broadcastMessage();
		pingPresence();
		myRC.yield();
	}

	/**
	 * Causes this Robot to walk around without direction, turning left or right
	 * at random when an obstacle is encountered.
	 * 
	 * runAtEndOfTurn is called after the move is chosen.
	 */
	public void walkAimlessly() {
		try {
			while (myRC.isMovementActive()) {
				runAtEndOfTurn();
			}
			// if there's not enough flux to move, don't try
			if (this.myRC.getFlux() < this.myRC.getType().moveCost) {
				return;
			}
			if (myRC.canMove(myRC.getDirection())) {
				myRC.moveForward();
			} else {
				if (Math.random() < .5) {
					myRC.setDirection(myRC.getDirection().rotateLeft());
				} else {
					myRC.setDirection(myRC.getDirection().rotateRight());
				}
				runAtEndOfTurn();
			}
		} catch (Exception e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
	}

	/**
	 * If on powernode get off of it.
	 */
	public void getOffPowerNode() {
		try {
			while (myRC.isMovementActive()) {
				runAtEndOfTurn();
			}
			// if there's not enough flux to move, don't try
			if (this.myRC.getFlux() < this.myRC.getType().moveCost) {
				return;
			}
			if (myRC.canMove(myRC.getDirection())) {
				myRC.moveForward();
			} else {
				myRC.setDirection(myRC.getDirection().rotateLeft());
				runAtEndOfTurn();
			}
		} catch (Exception e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
	}

	/**
	 * Similar to walk aimlessly, except that this robot will perform a random
	 * walk.
	 * 
	 * runAtEndOfTurn is called after the move is chosen.
	 */
	public void randomWalk() {
		try {
			while (myRC.isMovementActive()) {
				runAtEndOfTurn();
			}
			if (this.myRC.getFlux() < this.myRC.getType().moveCost) {
				runAtEndOfTurn();
			}
			// choices: rotate 45, 90, 135, or 180 deg right or 45, 90, 135 deg
			// left, move forward
			// weight moving forward more - 50% forward, 50% turn?
			double num = Math.random();
			if (num > 0.5 && myRC.canMove(myRC.getDirection())) {
				myRC.moveForward();
				runAtEndOfTurn();
			} else {
				Direction dir;
				if (num > 0.4375)
					dir = battlecode.common.Direction.EAST;
				else if (num > 0.375)
					dir = battlecode.common.Direction.NORTH_EAST;
				else if (num > 0.3125)
					dir = battlecode.common.Direction.SOUTH_EAST;
				else if (num > 0.25)
					dir = battlecode.common.Direction.WEST;
				else if (num > 0.1875)
					dir = battlecode.common.Direction.NORTH_WEST;
				else if (num > 0.125)
					dir = battlecode.common.Direction.SOUTH_WEST;
				else if (num > 0.0625)
					dir = battlecode.common.Direction.NORTH;
				else
					dir = battlecode.common.Direction.SOUTH;
				if (dir == myRC.getDirection()
						&& myRC.canMove(myRC.getDirection())) {
					myRC.moveForward();
					runAtEndOfTurn();
				}
				myRC.setDirection(dir);
				runAtEndOfTurn();
			}
		} catch (Exception e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}

	}

	/**
	 * Broadcast a random message.
	 */
	public void broadcastMessage() {
		try {
			if (myRC.getFlux() > battlecode.common.GameConstants.BROADCAST_FIXED_COST
					+ 16
					* battlecode.common.GameConstants.BROADCAST_COST_PER_BYTE) {
				Message message = new Message();
				int num[] = { 5 };
				message.ints = num;
				myRC.broadcast(message);
			}
		} catch (Exception e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
	}

	/**
	 * @return the Robot closest to this Robot that is on the same team as this.
	 */
	public Robot findAFriendly() {
		Robot[] nearbyObjects = myRC.senseNearbyGameObjects(Robot.class);
		Robot closestFriend = null;
		try {
			if (nearbyObjects.length > 0) {
				for (Robot e : nearbyObjects) {
					if (e.getTeam() != myRC.getTeam()
							|| myRC.senseRobotInfo(e).type == RobotType.TOWER) {
						continue;
					}
					if (closestFriend == null
							|| compareRobotDistance(e, closestFriend)) {
						closestFriend = e;
					}
				}
			}
			return closestFriend;
		} catch (GameActionException e1) {
			e1.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @return the closest enemy Robot of 
	 */

	/**
	 * Finds the friendly nearby that has the lowest flux and is not an archon.
	 * This is useful for archons that need to resupply friendlies.
	 * 
	 * Since flux transfers can only occur with adjacent robots or robots on the
	 * same location, this method will only return a robot in one of those
	 * acceptable receiving locations.
	 * 
	 * @return a robot that is friendly adjacent with low flux count. null if
	 *         there is no nearby robot.
	 */
	public Robot findAWeakFriendly() {
		try {
			Robot[] nearbyObjects = myRC.senseNearbyGameObjects(Robot.class);
			Robot weakestFriend = null;
			if (nearbyObjects.length > 0) {
				for (Robot e : nearbyObjects) {
					if (e.getTeam() != myRC.getTeam()
							|| myRC.senseRobotInfo(e).type == RobotType.ARCHON
							|| myRC.senseRobotInfo(e).type == RobotType.TOWER
							|| !acceptableFluxTransferLocation(myRC
									.senseLocationOf(e))) {
						continue;
					}
					if (weakestFriend == null
							|| compareRobotFlux(e, weakestFriend)) {
						weakestFriend = e;
					}
				}
			}
			return weakestFriend;
		} catch (Exception e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Finds the friendly nearby that has the lowest energon and is not a tower.
	 * This is useful for scouts that need to heal neighbors.
	 * 
	 * Since the heal range is exactly the attack range, only considers robots
	 * within the attack range.
	 * 
	 * @return a robot that is friendly nearby with low energon count. null if
	 *         there is no nearby robot.
	 */
	public Robot findALowEnergonFriendly() {
		try {
			Robot[] nearbyObjects = myRC.senseNearbyGameObjects(Robot.class);
			Robot weakestFriend = null;
			if (nearbyObjects.length > 0) {
				for (Robot e : nearbyObjects) {
					if (e.getTeam() != myRC.getTeam()
							|| myRC.senseRobotInfo(e).type == RobotType.TOWER
							|| !myRC.canAttackSquare((myRC.senseLocationOf(e)))) {
						continue;
					}
					if (weakestFriend == null
							|| compareRobotEnergon(e, weakestFriend)) {
						weakestFriend = e;
					}
				}
			}
			return weakestFriend;
		} catch (Exception e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Boolean valued function that determines whether a location is valid
	 * (adjacent or equal to this location) for a flux transfer.
	 * 
	 * @param attempt
	 *            MapLocation to test the validity of
	 * @return true when attempt is equal to or adjacent to this robot's
	 *         location
	 */
	public boolean acceptableFluxTransferLocation(MapLocation attempt) {
		return this.myRC.getLocation().distanceSquaredTo(attempt) <= 2;
	}

	/**
	 * Compares the flux of two robots, returns true if one is lower than two
	 * 
	 * @param one
	 *            Robot to compare flux of
	 * @param two
	 *            Robot to compare flux of
	 * @return truw when Robot one has a lower flux than Robot two
	 */
	public boolean compareRobotFlux(Robot one, Robot two) {
		try {
			double fluxOne = myRC.senseRobotInfo(one).flux;
			double fluxTwo = myRC.senseRobotInfo(two).flux;
			return fluxOne < fluxTwo;
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Compares the energon of two robots, returns true if one is lower than two
	 * 
	 * @param one
	 *            Robot to compare energon of
	 * @param two
	 *            Robot to compare energon of
	 * @return truw when Robot one has a lower energon than Robot two
	 */
	public boolean compareRobotEnergon(Robot one, Robot two) {
		try {
			double energonOne = myRC.senseRobotInfo(one).energon;
			double energonTwo = myRC.senseRobotInfo(two).energon;
			return energonOne < energonTwo;
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 
	 * @param one
	 *            Robot to compare distance between
	 * @param two
	 *            Robot to compare distance between
	 * @return true when Robot one is closer than Robot two
	 */
	public boolean compareRobotDistance(Robot one, Robot two) {
		try {
			if (!myRC.canSenseObject(one) || !myRC.canSenseObject(two)) {
				return false;
			}
			int distToOne = myRC.getLocation().distanceSquaredTo(
					myRC.senseLocationOf(one));
			int distToTwo = myRC.getLocation().distanceSquaredTo(
					myRC.senseLocationOf(two));
			return distToOne < distToTwo;
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean enemyTowerPresent(MapLocation target) {
		try {
			if (!myRC.canSenseSquare(target)) {
				return true;
			}
			if (myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) != null
					&& myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND)
							.getTeam() != myRC.getTeam()) {
				return true;
			} else {
				return false;
			}

		} catch (GameActionException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Causes this robot to destroy a tower given at MapLocation target.
	 * 
	 * Takes all of this robots time (yields in a while) as long as the target
	 * is still shootable.
	 * 
	 * @param target
	 *            MapLocation to shoot at
	 */
	public void destroyTower(MapLocation target) {
		try {
			if (!myRC.canSenseSquare(target)) {
				return;
			}
			while (myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) != null
					&& myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND)
							.getTeam() != myRC.getTeam()) {
				if (myRC.canAttackSquare(target) && !myRC.isAttackActive()) {
					myRC.attackSquare(target, RobotLevel.ON_GROUND);
				} else if (!myRC.isAttackActive()) {
					nav.getNextMove(target);
				}
				if (!myRC.canSenseSquare(target)) {
					break;
				}
				runAtEndOfTurn();
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Determines nearby enemy robots and returns the closest of them.
	 * 
	 * @return Robot that is the closest, not on our team. Null if no enemy
	 *         robots are nearby.
	 */
	public Robot senseClosestEnemy() {
		Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class);
		Robot closest = null;
		if (enemies.length > 0) {
			for (Robot e : enemies) {
				if (e.getTeam() == myRC.getTeam() || !myRC.canSenseObject(e)) {
					continue;
				}
				if (closest == null || compareRobotDistance(e, closest)) {
					closest = e;
				}
			}
		}
		return closest;
	}

	/**
	 * Determines nearby ground enemy robots and returns the closest of them.
	 * 
	 * @return Robot that is the closest, not on our team. Null if no enemy
	 *         robots are nearby.
	 */
	public Robot senseClosestGroundEnemy() {
		Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class);
		Robot closest = null;
		if (enemies.length > 0) {
			for (Robot e : enemies) {
				if (e.getTeam() == myRC.getTeam()
						|| !myRC.canSenseObject(e)
						|| e.getRobotLevel() == battlecode.common.RobotLevel.IN_AIR) {
					continue;
				}
				if (closest == null || compareRobotDistance(e, closest)) {
					closest = e;
				}
			}
		}
		return closest;
	}

	public void attackClosestEnemy(Robot closestTar) {
		try {
			if (closestTar == null) {
				return;
			}
			MapLocation attack = myRC.senseLocationOf(closestTar);

			if (myRC.senseRobotInfo(closestTar).type == RobotType.TOWER) {
				if (ownAdjacentTower(attack)) {
					myRC.setIndicatorString(0, "Attempting tower destroy");
					destroyTower(attack);
				} else {
					return;
				}
			}

			if (myRC.canAttackSquare(attack) && !myRC.isAttackActive()) {
				myRC.setIndicatorString(7, "Attacking closest enemy");
				if (closestTar.getRobotLevel() == RobotLevel.ON_GROUND) {
					myRC.attackSquare(attack, RobotLevel.ON_GROUND);
				} else {
					myRC.attackSquare(attack, RobotLevel.IN_AIR);
				}
				myRC.setIndicatorString(2, "Attacking: " + attack.toString());
			}
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Same as attackClosestEnemy, except causes this robot to chase the enemy
	 * as well.
	 * 
	 * @param closestTar
	 *            Target to seek and destroy.
	 */
	public void attackAndChaseClosestEnemy(Robot closestTar) {
		try {
			if (closestTar == null) {
				return;
			}
			if (myRC.canSenseObject(closestTar)) {
				MapLocation attack = myRC.senseLocationOf(closestTar);
				if (myRC.senseRobotInfo(closestTar).type == RobotType.TOWER) {
					if (ownAdjacentTower(attack)) {
						myRC.setIndicatorString(0, "Attempting tower destroy");
						destroyTower(attack);
					} else {
						return;
					}
				}
				if (myRC.canAttackSquare(attack) && !myRC.isAttackActive()) {
					if (closestTar.getRobotLevel() == RobotLevel.ON_GROUND) {
						myRC.attackSquare(attack, RobotLevel.ON_GROUND);
					} else {
						myRC.attackSquare(attack, RobotLevel.IN_AIR);
					}
					myRC.setIndicatorString(2,
							"Attacking: " + attack.toString());
				}
				if (!myRC.isMovementActive() && !myRC.isAttackActive()) {
					this.nav.getNextMove(attack);
				}
			}
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Determines if an adjacent tower is owned. Returns true if yes.
	 * 
	 * @param m
	 *            Location to examine neighbors of
	 * @return true if a tower adjacent to m is owned, false otherwise
	 */
	public boolean ownAdjacentTower(MapLocation m) {
		PowerNode p;
		try {
			p = (PowerNode) myRC
					.senseObjectAtLocation(m, RobotLevel.POWER_NODE);

			MapLocation[] neighbors = p.neighbors();
			PowerNode[] ownedTowers = myRC.senseAlliedPowerNodes();
			boolean ownAdjacent = false;

			search: {
				for (MapLocation neigh : neighbors) {
					for (PowerNode owned : ownedTowers) {
						if (neigh.equals(owned.getLocation())) {
							ownAdjacent = true;
							break search;
						}
					}
				}
			}
			return ownAdjacent;
		} catch (GameActionException e) {
			return false;
		}
	}

	/**
	 * This is an archaic navigation method that is superceeded by Navigation.
	 * 
	 * 
	 * @param target
	 *            Target location to got closer to
	 * @deprecated This method is replaced by bugNav
	 */
	@Deprecated
	public void goCloser(MapLocation target) {
		try {
			while (myRC.isMovementActive()) {
				runAtEndOfTurn();
			}
			Direction targetDir = myRC.getLocation().directionTo(target);

			if (myRC.getDirection() != targetDir) {
				myRC.setDirection(targetDir);
				runAtEndOfTurn();
			}
			if (myRC.canMove(targetDir)) {
				myRC.moveForward();
			} else {
				if (Math.random() < 2) {
					myRC.setDirection(myRC.getDirection().rotateLeft());
				} else {
					myRC.setDirection(myRC.getDirection().rotateRight());
				}
				runAtEndOfTurn();
				if (myRC.canMove(myRC.getDirection())) {
					myRC.moveForward();
					runAtEndOfTurn();
				}
			}
		} catch (Exception e) {
			System.out.println("caught exception:");
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param one
	 *            MapLocation to compare distance between
	 * @param two
	 *            MapLocation to compare distance between
	 * @return true when MapLocation one is closer than Robot two
	 */
	public boolean compareMapLocationDistance(MapLocation one, MapLocation two) {
		try {
			int distToOne = myRC.getLocation().distanceSquaredTo(one);
			int distToTwo = myRC.getLocation().distanceSquaredTo(two);
			return distToOne < distToTwo;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * If this robot has energon <= SOLDIER.attackPower and is not an archon,
	 * finds a neighbor. If there is no neighbor, retreats (if possible). This
	 * robot then moves toward the neighbor, disregarding all other activity. It
	 * then transfers flux to that neighbor.
	 */
	public void aboutToDie() {
		try {
			if (myRC.getEnergon() <= battlecode.common.RobotType.SOLDIER.attackPower
					&& myRC.getType() != battlecode.common.RobotType.ARCHON) {
				Robot neighbor = findAFriendly();
				if (neighbor == null) {
					// move backward
					if (this.myRC.canMove(this.myRC.getDirection().opposite())
							&& !this.myRC.isMovementActive()) {
						this.myRC.moveBackward();
					}
				}
				if (neighbor != null) {
					// System.out.println("about to die, found weak");
					while (this.myRC.canSenseObject(neighbor)
							&& !myRC.getLocation().isAdjacentTo(
									myRC.senseLocationOf(neighbor))) {
						nav.getNextMove(myRC.senseLocationOf(neighbor));
						myRC.yield();
					}
					if (this.myRC.canSenseObject(neighbor)) {
						RobotInfo weakRobotInfo = myRC.senseRobotInfo(neighbor);
						double weakFlux = weakRobotInfo.flux;
						double maxFlux = weakRobotInfo.type.maxFlux;
						double fluxAmountToTransfer = Math.min(maxFlux
								- weakFlux, myRC.getFlux());
						if (fluxAmountToTransfer > 0) {
							// System.out.println("transfer");
							// System.out.println(maxFlux-weakFlux);
							// System.out.println(myRC.getFlux());
							myRC.transferFlux(weakRobotInfo.location,
									weakRobotInfo.robot.getRobotLevel(),
									fluxAmountToTransfer);
							// System.out.println(myRC.getFlux());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the nearest friendly archon. Since many robots have a limited
	 * field of view, this is necessary for when a nearby archon turns away and
	 * out of the field of view.
	 * 
	 * @return nearest friendly archon. null if no archons remain on the board.
	 */
	public MapLocation reacquireNearestFriendlyArchonLocation() {
		try {
			MapLocation closestArchonLocation = null;
			MapLocation[] alliedArchonsLocations = myRC.senseAlliedArchons();
			if (alliedArchonsLocations.length > 0) {
				for (MapLocation e : alliedArchonsLocations) {
					if (closestArchonLocation == null
							|| compareMapLocationDistance(e,
									closestArchonLocation)) {
						closestArchonLocation = e;
					}
				}
			}
			if (closestArchonLocation == null) {
				return null;
			}
			return closestArchonLocation;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Note that this method depends on sensor range, so not a good option to
	 * sense friendly towers
	 * 
	 * @param rt
	 * @return returns MapLocation of closest friendly robot of type rt
	 */
	public MapLocation findNearestFriendlyRobotType(RobotType rt) {
		MapLocation closestLoc = null;
		Robot closestRob = null;
		Robot[] nearbyRobots = myRC.senseNearbyGameObjects(Robot.class);
		try {
			if (nearbyRobots.length > 0) {
				for (Robot r : nearbyRobots) {
					if (r.getTeam() != myRC.getTeam()
							|| myRC.senseRobotInfo(r).type != rt) {
						continue;
					}
					if (closestRob == null
							|| compareRobotDistance(r, closestRob)) {
						closestRob = r;
						closestLoc = myRC.senseLocationOf(r);
					}
				}
			}
			return closestLoc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Note that this method depends on sensor range, so not a good option to
	 * sense enemy towers
	 * 
	 * @param rt
	 * @return returns closest enemy Robot of type rt
	 */
	public Robot findNearestEnemyRobotType(RobotType rt) {
		Robot closestRob = null;
		Robot[] nearbyRobots = myRC.senseNearbyGameObjects(Robot.class);
		try {
			if (nearbyRobots.length > 0) {
				for (Robot r : nearbyRobots) {
					if (r.getTeam() == myRC.getTeam()
							|| myRC.senseRobotInfo(r).type != rt) {
						continue;
					}
					if (closestRob == null
							|| compareRobotDistance(r, closestRob)) {
						closestRob = r;
					}
				}
			}
			return closestRob;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** 
	 * @return MapLocation of closest allied tower
	 */
	public MapLocation findNearestAlliedTower() {
		MapLocation closestLoc = null;
		PowerNode[] towers = myRC.senseAlliedPowerNodes();
		if (towers.length > 0) {
			for (PowerNode t : towers) {
				if (closestLoc == null
						|| myRC.getLocation()
								.distanceSquaredTo(t.getLocation()) < myRC
								.getLocation().distanceSquaredTo(closestLoc)) {
					closestLoc = t.getLocation();
				}

			}
		}
		return closestLoc;
	}

	public boolean canSenseArchon() {
		MapLocation[] archons = myRC.senseAlliedArchons();
		for (int i = 0; i < archons.length; i++) {
			if (myRC.canSenseSquare(archons[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sends a message saying this robot is present and broadcasting its
	 * location.
	 */
	public void pingPresence() {
		try {
			Message message = new Message();
			if (this.myRC.getType().isAirborne()) {
				message.ints = new int[] { AIRBORNE_PING_MESSAGE };
			} else {
				message.ints = new int[] { GROUND_PING_MESSAGE };
			}
			message.locations = new MapLocation[] { this.myRC.getLocation() };
			if (myRC.getFlux() > battlecode.common.GameConstants.BROADCAST_FIXED_COST
					+ 16 * message.getFluxCost()) {
				myRC.broadcast(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Finds and returns the BEST enemy to shoot at.
	 */
	public Robot senseBestEnemy() {
		try {
			Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class);

			FastArrayList<Robot> archons = new FastArrayList<Robot>(
					enemies.length);
			FastArrayList<Robot> soldiers = new FastArrayList<Robot>(
					enemies.length);
			FastArrayList<Robot> scorchers = new FastArrayList<Robot>(
					enemies.length);
			FastArrayList<Robot> others = new FastArrayList<Robot>(
					enemies.length);

			for (Robot e : enemies) {
				if (e.getTeam() == myRC.getTeam() || !myRC.canSenseObject(e)) {
					continue;
				}
				RobotInfo eInfo = myRC.senseRobotInfo(e);
				switch (eInfo.type) {
				case ARCHON:
					archons.add(e);
					break;
				case SOLDIER:
					soldiers.add(e);
					break;
				case SCORCHER:
					others.add(e);
					break;
				default:
					others.add(e);
				}
			}
			FastArrayList<Robot> priorityTargets = null;
			if (soldiers.size() > 0) {
				priorityTargets = soldiers;
			} else if (archons.size() > 0) {
				priorityTargets = archons;
			} else if (others.size() > 0) {
				priorityTargets = others;
			}
			if (priorityTargets != null) {
				Robot closest = null;
				Robot weakest = null;
				if (priorityTargets.size() > 0) {
					for (int i = 0; i < priorityTargets.size(); i++) {
						Robot e = priorityTargets.get(i);
						if (e.getTeam() == myRC.getTeam()
								|| !myRC.canSenseObject(e)) {
							continue;
						}
						if (closest == null || compareRobotDistance(e, closest)) {
							closest = e;
						}
						if (weakest == null || compareRobotEnergon(e, weakest)) {
							weakest = e;
						}
					}
				}
				return weakest;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Receives all messages in the queue. Returns an attack location, else
	 * null.
	 */
	public MapLocation receiveMessagesReturnAttack() {
		this.messages = this.myRC.getAllMessages();
		for (Message r : this.messages) {
			if (r.ints != null && r.ints[0] == ARCHON_ENEMY_MESSAGE) {
				return r.locations[0];
			}
		}
		return null;
	}

	/**
	 * Useful when a robot only knows where an enemy is, rather than having
	 * information about the object.
	 * 
	 * @param n
	 */
	public void attackAndChaseMapLocation(MapLocation n) {
		try {
			if (n == null) {
				return;
			}
			if (myRC.canAttackSquare(n) && !myRC.isAttackActive()) {
				myRC.attackSquare(n, RobotLevel.ON_GROUND);
				myRC.setIndicatorString(2, "Attacking: " + n.toString());
			}
			if (!myRC.isMovementActive() && !myRC.isAttackActive()) {
				this.nav.getNextMove(n);
			}
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
	}
}
