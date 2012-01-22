package team111;

import team111.Nav.BugNav;
import team111.Nav.LocalAreaNav;
import team111.Nav.Navigation;
import team111.util.FastArrayList;
import battlecode.common.Clock;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class DisrupterPlayer extends BasePlayer {
	private MapLocation friendlyMapLocationToFollow = null;
	private final double MAX_DEVIATION_DISTANCE_SQUARE = 10000;

	public DisrupterPlayer(RobotController rc) {
		super(rc);
		this.nav = new LocalAreaNav(rc);
	}

	public void run() {
		runAtEndOfTurn();
	}

	public void followAndAttack() {
		while (true) {
			try {
				friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
				if (friendlyMapLocationToFollow == null) {
					// game over...
					myRC.suicide();
				}
				Robot closeEnemy = senseBestEnemy();
				if (closeEnemy != null
						&& myRC.senseRobotInfo(closeEnemy).type == RobotType.SCORCHER) {
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
				} else if (myRC.senseLocationOf(closeEnemy).distanceSquaredTo(
						myRC.getLocation()) > MAX_DEVIATION_DISTANCE_SQUARE) {
					this.nav.getNextMove(friendlyMapLocationToFollow);
					runAtEndOfTurn();
				} else {
					attackAndChaseClosestEnemy(closeEnemy);
					runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

	@Override
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
					scorchers.add(e);
					break;
				case DISRUPTER:
					disrupters.add(e);
					break;
				default:
					others.add(e);
				}
			}
			FastArrayList<Robot> priorityTargets = null;
			if (scorchers.size() > 0) {
				priorityTargets = scorchers;
			} else if (soldiers.size() > 0) {
				priorityTargets = soldiers;
			} else if (archons.size() > 0) {
				priorityTargets = archons;
			} else if (disrupters.size() > 0) {
				priorityTargets = disrupters;
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
					if (wInfo.type == RobotType.TOWER
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
