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

	private Navigation nav = null;
	private MapLocation targetLoc;
	private Robot closestTar;
	private Random r = new Random();
	private Robot friendlyToFollow = null;
	private int numTurnsLookingForFriendly = 0;
	private MapLocation friendlyMapLocationToFollow = null;

	public SoldierPlayer(RobotController rc) {
		super(rc);
		this.nav = new BugNav(rc);
	}

	// go explore, follow spawned archon, transfer energon to archon

	public void run() {
		runFollowFriendlyMode();
	}

	/**
	 * Finds an enemy, chases and attacks it. If there is no enemy, walks
	 * aimlessly.
	 */
	public void guardThenAttackMode() {
		while (true) {
			try {
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

}
