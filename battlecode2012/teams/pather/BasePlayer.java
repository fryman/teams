package pather;

import battlecode.common.*;

public abstract class BasePlayer extends StaticStuff {
	public BasePlayer(RobotController rc) {

	}

	/**
	 * Code to run once per turn.
	 */
	public void runOncePerTurn() {
		broadcastMessage();
	}

	/**
	 * Causes this Robot to walk around without direction, turning left or right
	 * at random when an obstacle is encountered.
	 */
	public void walkAimlessly() {
		try {
			while (myRC.isMovementActive()) {
				myRC.yield();
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
				myRC.yield();
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

	/**
	 * This is an archaic navigation method that is superceeded by Navigation.
	 * 
	 * @param target
	 *            Target location to got closer to
	 */
	public void goCloser(MapLocation target) {
		try {
			while (myRC.isMovementActive()) {
				myRC.yield();
			}
			Direction targetDir = myRC.getLocation().directionTo(target);

			if (myRC.getDirection() != targetDir) {
				myRC.setDirection(targetDir);
				myRC.yield();
			}
			if (myRC.canMove(targetDir)) {
				myRC.moveForward();
			} else {
				if (Math.random() < 2) {
					myRC.setDirection(myRC.getDirection().rotateLeft());
				} else {
					myRC.setDirection(myRC.getDirection().rotateRight());
				}
				myRC.yield();
				if (myRC.canMove(myRC.getDirection())) {
					myRC.moveForward();
					myRC.yield();
				}
			}
		} catch (Exception e) {
			System.out.println("caught exception:");
			e.printStackTrace();
		}
	}
}
