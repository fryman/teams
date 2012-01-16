package pather;

import battlecode.common.*;
public abstract class BasePlayer extends StaticStuff{
	public BasePlayer(RobotController rc){
		 
	}
	public void TestMethod2(){
		myRC.setIndicatorString(1, "BasePlayer");
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