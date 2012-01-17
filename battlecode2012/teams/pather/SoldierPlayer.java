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
				// TODO Are both of these statements necessary ??
				if (friendlyToFollow == null) {
					friendlyToFollow = findAFriendly();
				}
				if (friendlyToFollow == null
						|| !myRC.canSenseObject(friendlyToFollow)
						|| myRC.senseRobotInfo(friendlyToFollow).type == RobotType.SCOUT
						|| myRC.senseRobotInfo(friendlyToFollow).type == RobotType.TOWER) {
					walkAimlessly();
					friendlyToFollow = null;
					myRC.setIndicatorString(1, "walking aimlessly");
					runAtEndOfTurn();
				} else {
					// we have a friend.
					if (!myRC.canSenseObject(friendlyToFollow)) {
						continue;
					}
					MapLocation friendLocation = myRC
							.senseLocationOf(friendlyToFollow);
					this.nav.getNextMove(friendLocation);
					myRC.setIndicatorString(1, "following a friendly");
					myRC.setIndicatorString(0, "friendly number: "
							+ friendlyToFollow.getID());
					runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}
}
