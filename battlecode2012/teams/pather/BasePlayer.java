package pather;

import pather.Nav.BugNav;
import pather.Nav.Navigation;
import pather.util.BoardModel;
import battlecode.common.*;

public abstract class BasePlayer extends StaticStuff {
	private Navigation nav = null;
	
	public BasePlayer(RobotController rc) {
		// Today use BugNav
		this.nav = new BugNav(rc);
	}

	/**
	 * Code to run once per turn, at the very end
	 * 
	 * Includes RobotController.yield() statement, so this method should be
	 * called when the Robot is done with its turn.
	 */
	public void runAtEndOfTurn() {
		aboutToDie();
		broadcastMessage();
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
		if (nearbyObjects.length > 0) {
			for (Robot e : nearbyObjects) {
				if (e.getTeam() != myRC.getTeam()) {
					continue;
				}
				if (closestFriend == null
						|| compareRobotDistance(e, closestFriend)) {
					closestFriend = e;
				}
			}
		}
		return closestFriend;
	}

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
	 * Finds the friendly nearby that has the lowest energon and is not a
	 * tower. This is useful for scouts that need to heal neighbors.
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
		return this.myRC.getLocation().distanceSquaredTo(attempt) <= 1;
	}

	/**
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

	public void destroyTower(MapLocation target) {
		try {
			if (myRC.canAttackSquare(target) && !myRC.isAttackActive()) {
				while (myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) != null
						&& myRC.senseObjectAtLocation(target,
								RobotLevel.ON_GROUND).getTeam() != myRC
								.getTeam()) {
					myRC.attackSquare(target, RobotLevel.ON_GROUND);
					runAtEndOfTurn();
				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public Robot senseClosestEnemy() {
		Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class);
		Robot closest = null;
		if (enemies.length > 0) {
			for (Robot e : enemies) {
				if (e.getTeam() == myRC.getTeam()) {
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
			if (myRC.canAttackSquare(attack) && !myRC.isAttackActive()) {
				myRC.attackSquare(attack, RobotLevel.ON_GROUND);
				myRC.setIndicatorString(2, "Attacking: " + attack.toString());
			}
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
	}

	public boolean ownAdjacentTower(PowerNode p) {
		MapLocation[] neighbors = p.neighbors();
		PowerNode[] ownedTowers = myRC.senseAlliedPowerNodes();
		boolean ownAdjacent = false;

		search: {
			for (MapLocation m : neighbors) {
				for (PowerNode owned : ownedTowers) {
					if (m.equals(owned.getLocation())) {
						ownAdjacent = true;
						break search;
					}
				}
			}
		}
		return ownAdjacent;
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

	public void aboutToDie(){
		//if less than 5% health...
		try {		
			if (myRC.getEnergon() < 0.5*myRC.getMaxEnergon()
					&& myRC.getType()!=battlecode.common.RobotType.ARCHON){
				Robot weak = findAWeakFriendly();
				if (weak != null) {
					//System.out.println("about to die, found weak");
					while(!myRC.getLocation().isAdjacentTo(myRC.senseLocationOf(weak)) 
							&& myRC.getLocation()!= myRC.senseLocationOf(weak)){
						nav.getNextMove(myRC.senseLocationOf(weak));	
					}
					RobotInfo weakRobotInfo = myRC.senseRobotInfo(weak);
					double weakFlux = weakRobotInfo.flux;
					double maxFlux = weakRobotInfo.type.maxFlux;
					double fluxAmountToTransfer = Math.min(maxFlux-weakFlux,myRC.getFlux());
					if (fluxAmountToTransfer > 0) {
						//System.out.println("transfer");
						//System.out.println(maxFlux-weakFlux);
						//System.out.println(myRC.getFlux());
						myRC.transferFlux(weakRobotInfo.location,
								weakRobotInfo.robot.getRobotLevel(),
								fluxAmountToTransfer);
						//System.out.println(myRC.getFlux());
					}
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//
}
