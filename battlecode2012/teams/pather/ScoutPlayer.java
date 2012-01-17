package pather;

import pather.Nav.BugNav;

import pather.Nav.Navigation;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ScoutPlayer extends BasePlayer {

	private Navigation nav = null;
	private MapLocation targetLoc;
	private Robot closestTar;
	private Robot friendlyToFollow = null;
	private MapLocation friendlyMapLocationToFollow = null;

	public ScoutPlayer(RobotController rc) {
		super(rc);
		this.nav = new BugNav(rc);
	}

	// go explore, follow spawned archon, transfer energon to archon

	/**
	 * Code to run once per turn, at the very end.
	 * 
	 * Includes RobotController.yield() statement, so this method should be
	 * called when the Robot is done with its turn.
	 */
	@Override
	public void runAtEndOfTurn() {
		try {
			broadcastMessage();
			if (suitableTimeToHeal()) {
				this.myRC.regenerate();
			}
			aboutToDie();
			myRC.yield();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
				friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
				if (friendlyMapLocationToFollow == null) {
					// game over...
					myRC.suicide();
				}
				this.nav.getNextMove(friendlyMapLocationToFollow);
				myRC.setIndicatorString(0, "following a friendly");
				runAtEndOfTurn();
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
				closestTar = senseClosestEnemy();
				if (closestTar == null) {
					walkAimlessly();
				} else {
					this.myRC.setIndicatorString(0, "Weakest Target: "
							+ closestTar.getID());
					targetLoc = myRC.senseLocationOf(closestTar);
					this.myRC.setIndicatorString(1,
							"Target at: " + targetLoc.toString());
					if (targetLoc != null
							&& !myRC.getLocation().isAdjacentTo(targetLoc)) {
						attackClosestEnemy(closestTar);
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
	 * surroundings.
	 * 
	 * Returns true when any robot nearby is <95% energon. Returns false when
	 * this robot's flux is less than 10% max or if nearby weaklings are null.
	 * 
	 * @return true if it is a good time to heal the surroundings, false
	 *         otherwise
	 */
	public boolean suitableTimeToHeal() {
		try {
			if (this.myRC.getFlux() < 0.1 * this.myRC.getType().maxFlux) {
				return false;
			}
			Robot weakling = findALowEnergonFriendly();
			if (weakling == null) {
				return false;
			}
			RobotInfo weakInfo = myRC.senseRobotInfo(weakling);
			if (weakInfo.energon / weakInfo.type.maxEnergon < 0.95) {
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
