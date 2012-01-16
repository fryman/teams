package pather;

import pather.Nav.BugNav;
import pather.Nav.Navigation;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ScoutPlayer extends BasePlayer {

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
						|| !myRC.canSenseObject(friendlyToFollow)
						|| myRC.senseRobotInfo(friendlyToFollow).type == RobotType.SCOUT
						|| myRC.senseRobotInfo(friendlyToFollow).type == RobotType.TOWER) {
					walkAimlessly();
					friendlyToFollow = null;
					myRC.setIndicatorString(1, "walking aimlessly");
					runOncePerTurn();
					myRC.yield();
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
					runOncePerTurn();
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
						attackWeakestEnemy(weakestTar);
						this.nav.getNextMove(targetLoc);
						runOncePerTurn();
						myRC.yield();
					}
					runOncePerTurn();
					myRC.yield();
				}
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}
}
