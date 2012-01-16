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

public class ScoutPlayer extends BasePlayer {

	private Robot[] enemies;
	private Navigation nav = null;
	private MapLocation targetLoc;
	private Robot weakestTar;
	private Robot friendlyToFollow = null;

	public ScoutPlayer(RobotController rc) {
		super(rc);
		this.nav = new BugNav(rc);
	}

	// go explore, follow spawned archon, transfer energon to archon

	public void run() {
		runFollowFriendlyMode();
	}

	/**
	 * Finds and follows around a friendly unit. If a friendly unit cannot be
	 * found, walks aimlessly until finding a friendly unit. Stores the friendly
	 * unit in instance variable: this.friendlyToFollow.
	 */
	public void runFollowFriendlyMode() {
		while (true) {
			try {
				// TODO Are both of these statements necessary ??
				if (friendlyToFollow == null) {
					friendlyToFollow = findAFriendly();
				}
				if (friendlyToFollow == null
						|| myRC.senseRobotInfo(friendlyToFollow).type == RobotType.SCOUT
						|| myRC.senseRobotInfo(friendlyToFollow).type == RobotType.TOWER) {
					walkAimlessly();
					friendlyToFollow = null;
					myRC.setIndicatorString(1, "walking aimlessly");
					myRC.yield();
				} else {
					// we have a friend.
					if (!myRC.canSenseObject(friendlyToFollow)){
						continue;
					}
					MapLocation friendLocation = myRC
							.senseLocationOf(friendlyToFollow);
					this.nav.getNextMove(friendLocation);
					myRC.setIndicatorString(1, "following a friendly");
					myRC.setIndicatorString(0, "friendly number: " + friendlyToFollow.getID());
					myRC.yield();
				}
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Walks around aimlessly until finding an enemy, then attacks that enemy.
	 */
	public void runAttackMode() {
		while (true) {
			try {
				weakestTar = senseWeakestEnemy();
				if (weakestTar == null) {
					walkAimlessly();
				} else {
					this.myRC.setIndicatorString(0, "Weakest Target: "
							+ weakestTar.getID());
					targetLoc = myRC.senseLocationOf(weakestTar);
					this.myRC.setIndicatorString(1,
							"Target at: " + targetLoc.toString());
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

	/**
	 * Not useful right now. Contains an example of an insertion sort. Remember:
	 * insertion sort is O(n^2), but is in-place and stable. It is efficient for
	 * short lists.
	 * 
	 * @param args
	 */
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

}
