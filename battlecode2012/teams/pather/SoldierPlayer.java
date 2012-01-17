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

	private MapLocation targetLoc;
	private Robot closestTar;
	private Random r = new Random();
	private Robot friendlyToFollow = null;
	private int numTurnsLookingForFriendly = 0;
	private MapLocation friendlyMapLocationToFollow = null;
	private final double MAX_DEVIATION_DISTANCE_SQUARE = 10000; // TODO find a
																// suitable
																// distance

	public SoldierPlayer(RobotController rc) {
		super(rc);
		this.nav = new BugNav(rc);
	}

	// go explore, follow spawned archon, transfer energon to archon

	public void run() {
		guardThenAttackMode();
	}

	/**
	 * Finds an enemy, chases and attacks it. If there is no enemy, walks
	 * aimlessly.
	 */
	public void guardThenAttackMode() {
		while (true) {
			try {
				//If it is on a power node get off of it so archons can build
				if (this.myRC.senseObjectAtLocation(myRC.getLocation(), RobotLevel.POWER_NODE) != null) {
					getOffPowerNode();
				}
				closestTar = senseClosestEnemy();
				if (closestTar == null) {
					try {
						Robot friend = findAFriendly();
						if (friend != null) {
							myRC.setIndicatorString(1, "Found friendly! "
									+ friend.getID());
							nav.getNextMove(myRC.senseLocationOf(friend));
							runAtEndOfTurn();
							continue;
						}
						walkAimlessly();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					myRC.setIndicatorString(0,
							"Weakest Target: " + closestTar.getID());
					targetLoc = myRC.senseLocationOf(closestTar);
					myRC.setIndicatorString(1,
							"Target at: " + targetLoc.toString());

					if (targetLoc != null
							&& !myRC.getLocation().isAdjacentTo(targetLoc)) {
						attackClosestEnemy(closestTar);
						this.nav.getNextMove(targetLoc);
						runAtEndOfTurn();
					} else {
						runAtEndOfTurn();
					}
				}
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Follows a friendly archon, does not attack other things.
	 */
	public void runFollowFriendlyMode() {
		while (true) {
			try {
				//If it is on a power node get off of it so archons can build
				if (this.myRC.senseObjectAtLocation(myRC.getLocation(), RobotLevel.POWER_NODE) != null) {
					getOffPowerNode();
				}
				// TODO Are both of these statements necessary ??
				if (friendlyToFollow == null) {
					friendlyToFollow = findAFriendly();
				}
				friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
				if (friendlyMapLocationToFollow == null) {
					// game over...
					myRC.suicide();
				}
				this.nav.getNextMove(friendlyMapLocationToFollow);
				myRC.setIndicatorString(1, "following a friendly");
				runAtEndOfTurn();
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Follows a friendly archon, attacks other things.
	 * 
	 * Only attacks other things as long as it is within
	 * MAX_DEVIATION_DISTANCE_SQUARE of nearest archon.
	 */
	public void runFollowFriendlyAndGuardMode() {
		while (true) {
			try {
				friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
				if (friendlyMapLocationToFollow == null) {
					// game over...
					myRC.suicide();
				}
				Robot closeEnemy = senseClosestEnemy();
				if (closeEnemy == null) {
					this.nav.getNextMove(friendlyMapLocationToFollow);
					runAtEndOfTurn();
				} else if (!myRC.canSenseObject(closeEnemy)) {
					this.nav.getNextMove(friendlyMapLocationToFollow);
					runAtEndOfTurn();
				} else if (myRC.senseLocationOf(closeEnemy).distanceSquaredTo(
						myRC.getLocation()) > MAX_DEVIATION_DISTANCE_SQUARE) {
					this.nav.getNextMove(friendlyMapLocationToFollow);
					runAtEndOfTurn();
				} else {
					// System.out.println("Ima attack now");
					// attack the enemy as long as it's nearby
					attackAndChaseClosestEnemy(closeEnemy);
				}
				runAtEndOfTurn();
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}
}
