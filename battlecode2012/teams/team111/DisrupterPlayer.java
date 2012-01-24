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
		followAndAttack();
	}

	/**
	 * Code to run once per turn, at the very end
	 * 
	 * Includes RobotController.yield() statement, so this method should be
	 * called when the Robot is done with its turn.
	 */
	@Override
	public void runAtEndOfTurn() {
		aboutToDie();
		if (beingAttacked() && myRC.canMove(myRC.getDirection().opposite())
				&& !this.myRC.isMovementActive()
				&& this.myRC.getFlux() > this.myRC.getType().moveCost) {
			try {
				myRC.moveBackward();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.prevEnergon = this.myRC.getEnergon();
		myRC.yield();
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
			int curRound = Clock.getRoundNum();
			if (curRound != lastRoundNumSurroundingsProcessed) {
				nearbyRobots = myRC.senseNearbyGameObjects(Robot.class);
				lastRoundNumSurroundingsProcessed = curRound;
			}

			FastArrayList<Robot> archons = new FastArrayList<Robot>(
					nearbyRobots.length);
			FastArrayList<Robot> soldiers = new FastArrayList<Robot>(
					nearbyRobots.length);
			FastArrayList<Robot> scorchers = new FastArrayList<Robot>(
					nearbyRobots.length);
			FastArrayList<Robot> others = new FastArrayList<Robot>(
					nearbyRobots.length);
			FastArrayList<Robot> disrupters = new FastArrayList<Robot>(
					nearbyRobots.length);

			for (Robot e : nearbyRobots) {
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
