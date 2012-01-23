package team111;

import java.util.Random;

import com.sun.org.apache.bcel.internal.generic.L2D;

import team111.Nav.BoidianNav;
import team111.Nav.BugNav;
import team111.Nav.LocalAreaNav;
import team111.Nav.Navigation;
import team111.util.FastArrayList;
import battlecode.common.*;

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
		// this.nav = new LocalAreaNav(rc);
		this.nav = new LocalAreaNav(rc);
	}

	// go explore, follow spawned archon, transfer energon to archon

	public void run() {
		runFollowFriendlyAndGuardMode();
	}

	/**
	 * Finds an enemy, chases and attacks it. If there is no enemy, walks
	 * aimlessly.
	 */
	public void guardThenAttackMode() {
		while (true) {
			try {
				// If it is on a power node get off of it so archons can build
				if (this.myRC.senseObjectAtLocation(myRC.getLocation(),
						RobotLevel.POWER_NODE) != null) {
					getOffPowerNode();
				}
				closestTar = senseBestEnemy();
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
				// If it is on a power node get off of it so archons can build
				if (this.myRC.senseObjectAtLocation(myRC.getLocation(),
						RobotLevel.POWER_NODE) != null) {
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
	 * Follows a friendly archon, attacks other things using
	 * attackAndChaseClosestEnemy.
	 * 
	 * Only attacks other things as long as it is within
	 * MAX_DEVIATION_DISTANCE_SQUARE of nearest archon.
	 */
	public void runFollowFriendlyAndGuardMode() {
		int justChasing = 0;
		while (true) {
			try {
				friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
				if (friendlyMapLocationToFollow == null) {
					// game over...
					myRC.suicide();
				}
				Robot unprotectedArchon = senseUnprotectedArchon();
				if (unprotectedArchon != null) {
					attackAndChaseUnprotectedArchon(unprotectedArchon);
				}
				Robot closeEnemy = senseBestEnemy();
				if (closeEnemy != null
						&& myRC.senseRobotInfo(closeEnemy).type == RobotType.ARCHON) {
					attackAndChaseClosestEnemy(closeEnemy);
					runAtEndOfTurn();
				} else {
					if (closeEnemy != null) {
						attackAndChaseClosestEnemy(closeEnemy);
						runAtEndOfTurn();
					}
					MapLocation archonEnemy = receiveMessagesReturnAttack();
					if (archonEnemy != null) {
						attackAndChaseMapLocation(archonEnemy);
						runAtEndOfTurn();
						continue;
					}
					if (closeEnemy == null) {
						this.nav.getNextMove(friendlyMapLocationToFollow);
						runAtEndOfTurn();
					} else if (!myRC.canSenseObject(closeEnemy)) {
						this.nav.getNextMove(friendlyMapLocationToFollow);
						runAtEndOfTurn();
					} else if (myRC.senseLocationOf(closeEnemy)
							.distanceSquaredTo(myRC.getLocation()) > MAX_DEVIATION_DISTANCE_SQUARE) {
						this.nav.getNextMove(friendlyMapLocationToFollow);
						runAtEndOfTurn();
					} else {
						attackAndChaseClosestEnemy(closeEnemy);
						runAtEndOfTurn();
					}
				}
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Finds and returns the BEST enemy to shoot at.
	 */
	public Robot senseBestEnemy() {
		try {
			Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class);

			FastArrayList<Robot> archons = new FastArrayList<Robot>(
					enemies.length);
			FastArrayList<Robot> soldiers = new FastArrayList<Robot>(
					enemies.length);
			FastArrayList<Robot> scorchers = new FastArrayList<Robot>(
					enemies.length);
			FastArrayList<Robot> others = new FastArrayList<Robot>(
					enemies.length);
			FastArrayList<Robot> disrupters = new FastArrayList<Robot>(
					enemies.length);

			for (Robot e : enemies) {
				if (e.getTeam() == myRC.getTeam() || !myRC.canSenseObject(e)) {
					continue;
				}
				RobotInfo eInfo = myRC.senseRobotInfo(e);
				switch (eInfo.type) {
				case ARCHON:
					archons.add(e);
					break;
				case SOLDIER:
					soldiers.add(e);
					break;
				case SCORCHER:
					others.add(e);
					break;
				case DISRUPTER:
					disrupters.add(e);
					break;
				default:
					others.add(e);
				}
			}
			FastArrayList<Robot> priorityTargets = null;
			if (archons.size() > 0) {
				priorityTargets = archons;
			} else if (disrupters.size() > 0) {
				priorityTargets = disrupters;
			} else if (soldiers.size() > 0) {
				priorityTargets = soldiers;
			} else if (others.size() > 0) {
				priorityTargets = others;
			}
			if (priorityTargets != null) {
				Robot closest = null;
				Robot weakest = null;
				if (priorityTargets.size() > 0) {
					for (int i = 0; i < priorityTargets.size(); i++) {
						Robot e = priorityTargets.get(i);
						if (e.getTeam() == myRC.getTeam()
								|| !myRC.canSenseObject(e)) {
							continue;
						}
						RobotInfo rInfo = this.myRC.senseRobotInfo(e);
						if (rInfo.type == RobotType.SCORCHER && rInfo.flux > 2) {
							continue;
						}
						if (closest == null || compareRobotDistance(e, closest)) {
							closest = e;
						}
						if (weakest == null || compareRobotEnergon(e, weakest)) {
							weakest = e;
						}
					}
				}
				if (weakest != null) {
					RobotInfo wInfo = myRC.senseRobotInfo(weakest);
					if (wInfo.type.equals(RobotType.TOWER)
							&& !ownAdjacentTower(wInfo.location)) {
						return null;
					}
				}
				return weakest;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
