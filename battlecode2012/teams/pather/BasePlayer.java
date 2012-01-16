package pather;

import battlecode.common.*;

public abstract class BasePlayer extends StaticStuff {
	public BasePlayer(RobotController rc) {

	}

	/**
	 * Code to run once per turn.
	 */
	public void runAtEndOfTurn() {
		broadcastMessage();
		myRC.yield();
	}

	/**
	 * Causes this Robot to walk around without direction, turning left or right
	 * at random when an obstacle is encountered.
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
	 */
	public void randomWalk() {
		try {
			while (myRC.isMovementActive()) {
				return;
			}
			if (this.myRC.getFlux() < this.myRC.getType().moveCost) {
				return;
			}
			// choices: rotate 45, 90, 135, or 180 deg right or 45, 90, 135 deg
			// left, move forward
			// weight moving forward more - 50% forward, 50% turn?
			double num = Math.random();
			if (num > 0.5 && myRC.canMove(myRC.getDirection())) {
				myRC.moveForward();
				return;
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
					return;
				}
				myRC.setDirection(dir);
				return;
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
	 * 
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

	public Robot senseWeakestEnemy() {
		Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class);
		Robot weakest = null;
		if (enemies.length > 0) {
			for (Robot e : enemies) {
				if (e.getTeam() == myRC.getTeam()) {
					continue;
				}
				if (weakest == null || compareRobotDistance(e, weakest)) {
					weakest = e;
				}
			}
		}
		return weakest;
	}

	public void attackWeakestEnemy(Robot weakestTar) {
		try {
			if (weakestTar == null) {
				return;
			}
			MapLocation attack = myRC.senseLocationOf(weakestTar);
			if (myRC.canAttackSquare(attack) && !myRC.isAttackActive()) {
				myRC.attackSquare(attack, RobotLevel.ON_GROUND);
				myRC.setIndicatorString(2, "Attacking: " + attack.toString());
			}
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * This is an archaic navigation method that is superceeded by Navigation.
	 * 
	 * @param target
	 *            Target location to got closer to
	 */
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
}
