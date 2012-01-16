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

	public void TestMethod2() {
		myRC.setIndicatorString(1, "BasePlayer");
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
		}
		 catch (Exception e) {
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
}
