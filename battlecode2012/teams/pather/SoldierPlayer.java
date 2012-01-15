package pather;

import java.util.Random;

import pather.Nav.BugNav;
import pather.Nav.Navigation;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class SoldierPlayer extends BasePlayer {

	private Robot[] enemies;
	private Navigation nav = null;
	private MapLocation targetLoc;
	private Robot weakestTar;
	private Random r = new Random();

	public SoldierPlayer(RobotController rc) {
		super(rc);
		this.nav = new BugNav(rc);
	}

	//go explore, follow spawned archon, transfer energon to archon
	
	public void run() {
		while (true) {
			try {
				weakestTar = senseWeakestEnemy();
				if (weakestTar == null) {
					try {
						Robot friend = findAFriendly();
						if (friend != null){
							myRC.setIndicatorString(1, "Found friendly! " + friend.getID());
							nav.getNextMove(myRC.senseLocationOf(friend));
							myRC.yield();
							continue;
						}
						while (myRC.isMovementActive()) {
							myRC.yield();
						}
						if (myRC.canMove(myRC.getDirection())) {
							myRC.moveForward();
						} else {
							if (r.nextDouble() < .5) {
								myRC.setDirection(myRC.getDirection()
										.rotateLeft());
							} else {
								myRC.setDirection(myRC.getDirection()
										.rotateRight());
							}
							myRC.yield();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					this.myRC.setIndicatorString(0, "Weakest Target: " + weakestTar.getID());
					targetLoc = myRC.senseLocationOf(weakestTar);
					this.myRC.setIndicatorString(1, "Target at: " + targetLoc.toString());
					if (targetLoc != null
							&& !myRC.getLocation().isAdjacentTo(targetLoc)) {
						attackWeakestEnemy();
						this.nav.getNextMove(targetLoc);
						myRC.yield();
					}
					myRC.yield();
				}
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}

	public Robot senseWeakestEnemy() {
		enemies = myRC.senseNearbyGameObjects(Robot.class);
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
	
	public Robot findAFriendly() {
		Robot[] nearbyObjects = myRC.senseNearbyGameObjects(Robot.class);
		Robot closestFriend = null;
		if (nearbyObjects.length > 0) {
			for (Robot e : nearbyObjects) {
				if (e.getTeam() != myRC.getTeam()) {
					continue;
				}
				if (closestFriend == null || compareRobotDistance(e, closestFriend)) {
					closestFriend = e;
				}
			}
		}
		return closestFriend;
	}

	public void attackWeakestEnemy() {
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

	public static void main(String[] args) {
		int[] enemies = { 2, 5, 3, 1, 6, 4 };
		for (int i : enemies) {
			System.out.print(i + " ");
		}
		long start = System.currentTimeMillis();
		int sortedLength = 1;
		int length = enemies.length;
		while (sortedLength < length) {
			int test = enemies[sortedLength];
			int compareTo = sortedLength - 1;
			while (true) {
				if (compareTo < 0) {
					sortedLength++;
					break;
				}
				if (test < enemies[compareTo]) {
					int temp = enemies[compareTo];
					enemies[compareTo] = test;
					enemies[compareTo + 1] = temp;
					compareTo--;
				} else {
					sortedLength++;
					break;
				}
			}
		}
		long end = System.currentTimeMillis();
		System.out.println();
		for (int i : enemies) {
			System.out.print(i + " ");
		}
		System.out.println();
		System.out.println("Time elapsed: " + (end - start));
	}

	public boolean compareRobotDistance(Robot one, Robot two) {
		// returns true if one weaker than two
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

