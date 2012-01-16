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

	/**
	 * Walks around aimlessly until finding an enemy, then attacks that enemy.
	 */
	public void runAttackMode() {
		while (true) {
			try {
				weakestTar = senseClosestEnemy();
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
						attackClosestEnemy(weakestTar);
						this.nav.getNextMove(targetLoc);
						runAtEndOfTurn();
					}
					runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void runFollowFriendlyModeWithHeal() {

	}

	/**
	 * Determines when it is cost effective for a scout to heal his
	 * surroundings. TODO determine a heuristic for when healing is useful.
	 * 
	 * @return true if it is a good time to heal the surroundings, false
	 *         otherwise
	 */
	public boolean suitableTimeToHeal() {
		// TODO
		return false;
	}

}
