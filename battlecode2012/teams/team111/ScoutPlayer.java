package team111;

import java.util.Random;

import team111.Nav.BoidianNav;
import team111.Nav.BugNav;
import team111.Nav.LocalAreaNav;
import team111.Nav.Navigation;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.Message;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ScoutPlayer extends BasePlayer {

	private Navigation nav;
	private MapLocation targetLoc;
	private Robot closestTar;
	private Robot friendlyToFollow = null;
	private MapLocation friendlyMapLocationToFollow = null;
	private final static int HOLDING_PATTERN_DISTANCE = 10;
	private MapLocation[] holdingPatternWaypoints;
	private Random r = new Random(Clock.getBytecodeNum() * Clock.getRoundNum());

	public ScoutPlayer(RobotController rc) {
		super(rc);
		this.nav = new LocalAreaNav(rc);
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
			if (suitableTimeToHeal()) {
				myRC.setIndicatorString(2, "healing: " + Clock.getRoundNum());
				this.myRC.regenerate();
			}
			int a = Clock.getBytecodeNum();
			Robot closestTar = senseClosestEnemy();
			
			if (closestTar != null
					&& myRC.senseRobotInfo(closestTar).type != RobotType.TOWER
					&& myRC.senseRobotInfo(closestTar).flux > .5) {
				MapLocation Location = myRC.senseLocationOf(closestTar);
				if (myRC.canAttackSquare(Location) && !myRC.isAttackActive()) {
					myRC.setIndicatorString(0,
							"Attacking at the end of the turn.");
					if (closestTar.getRobotLevel() == RobotLevel.ON_GROUND) {
						myRC.attackSquare(Location, RobotLevel.ON_GROUND);
					} else {
						myRC.attackSquare(Location, RobotLevel.IN_AIR);
					}
				}
			}
			
			Robot archon = this.findNearestEnemyRobotType(RobotType.ARCHON);
			MapLocation soldier = this.findNearestFriendlyRobotType(RobotType.SOLDIER);
			if (archon != null && soldier != null) {
				MapLocation archonLoc = this.myRC.senseLocationOf(archon);
				Message msg = new Message();
				msg.ints = new int[] { BasePlayer.ENEMY_ARCHON_LOCATION_MESSAGE };
				msg.locations = new MapLocation[] { archonLoc };
				nav.getNextMove(archonLoc);
				if (myRC.getFlux() > battlecode.common.GameConstants.BROADCAST_FIXED_COST
						+ 16 * msg.getFluxCost() && !this.myRC.hasBroadcasted()) {
					myRC.broadcast(msg);
				}
			}
			aboutToDie();
			myRC.yield();
		} catch (Exception e) {
			System.out.println(Clock.getBytecodeNum());
			e.printStackTrace();
		}
	}

	public void run() {
		 double decider = r.nextDouble();
		// System.out.println(decider);
		 if (decider > .1) {
		// System.out.println("Normal Pattern");
		runFollowFriendlyMode();
		 } else {
		// System.out.println("Holding Pattern");
		seekAndBroadcastEnemyArchon();
		 }
	}

	/**
	 * Finds and follows around a friendly unit. If a friendly unit cannot be
	 * found, walks aimlessly until finding a friendly unit. Stores the friendly
	 * unit in instance variable: this.friendlyToFollow.
	 */
	public void runFollowFriendlyMode() {
		while (true) {
			try {
				Robot target = findNearestEnemyRobotType(RobotType.SCORCHER);
				Robot soldier = findNearestEnemyRobotType(RobotType.SOLDIER);
				if (target != null && myRC.senseRobotInfo(target).flux > 2
						&& soldier == null) {
					while (myRC.canSenseObject(target)
							&& myRC.senseRobotInfo(target).flux > 2
							&& soldier == null) {
						soldier = findNearestEnemyRobotType(RobotType.SOLDIER);
						attackAndFollowScorcher(target);
						runAtEndOfTurn();
					}
				} else {
					friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
					if (friendlyMapLocationToFollow == null) {
						// game over...
						myRC.suicide();
					}
					this.nav.getNextMove(friendlyMapLocationToFollow);
					myRC.setIndicatorString(0, "following a friendly");
					// attackEnemy();
					runAtEndOfTurn();
				}
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
						// attackEnemy();
						runAtEndOfTurn();
					}
					// attackEnemy();
					runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
				runAtEndOfTurn();
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
	 * Returns true when any robot nearby is <100% energon. Returns false when
	 * this robot's flux is less than 5% max or if nearby weaklings are null.
	 * 
	 * @return true if it is a good time to heal the surroundings, false
	 *         otherwise
	 */
	public boolean suitableTimeToHeal() {
		try {
			if (this.myRC.getFlux() < 0.05 * this.myRC.getType().maxFlux) {
				return false;
			}
			Robot weakling = findALowEnergonFriendly();
			if (weakling == null) {
				return false;
			}
			RobotInfo weakInfo = myRC.senseRobotInfo(weakling);
			if (weakInfo.energon / weakInfo.type.maxEnergon < 1) {
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Attacks enemy scorchers until they run out of flux
	 * 
	 * @param Robot
	 *            scorcher
	 */
	public void attackAndFollowScorcher(Robot scorcher) {
		try {
			MapLocation scorcherLoc = myRC.senseLocationOf(scorcher);
			if (myRC.getLocation().distanceSquaredTo(scorcherLoc) > 3
					&& !myRC.isMovementActive()) {
				this.nav.getNextMove(scorcherLoc);
			}
			if (myRC.canAttackSquare(scorcherLoc) && !myRC.isAttackActive()) {
				myRC.attackSquare(scorcherLoc, RobotLevel.ON_GROUND);
			}
			runAtEndOfTurn();
		} catch (Exception e) {
			e.printStackTrace();
			runAtEndOfTurn();
		}
	}

	/**
	 * <<<<<<< HEAD Designed so that the scouts can go, find a friggen archon,
	 * and tell the soldier buddies to go get it.
	 * 
	 * Scouts walk in a "holding pattern" around the powercore looking for
	 * archons.
	 */
	public void seekAndBroadcastEnemyArchon() {
		int waypointNum = 0;
		initializeHoldingPatternWaypoints();
		int movesToCurrentWaypoint = 0;
		int roundsInSeek = 0;
		while (true) {
			try {
				roundsInSeek++;
				if (this.myRC.getFlux() < 0.05 * this.myRC.getType().maxFlux) {
					// find an archon friendly to refeul.
					friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
					if (friendlyMapLocationToFollow == null) {
						// game over...
						myRC.suicide();
					}
					this.nav.getNextMove(friendlyMapLocationToFollow);
					myRC.setIndicatorString(0, "following a friendly");
					runAtEndOfTurn();
					continue;
				}

				// sense enemy archons.
				Robot archon = this.findNearestEnemyRobotType(RobotType.ARCHON);
				if (archon != null) {
					MapLocation archonLoc = this.myRC.senseLocationOf(archon);
					Message msg = new Message();
					msg.ints = new int[] { BasePlayer.ENEMY_ARCHON_LOCATION_MESSAGE };
					msg.locations = new MapLocation[] { archonLoc };
					nav.getNextMove(archonLoc);
					this.myRC.broadcast(msg);
					runAtEndOfTurn();
					continue;
				}

				nav.getNextMove(this.holdingPatternWaypoints[waypointNum]);
				if (this.myRC.getLocation().distanceSquaredTo(
						this.holdingPatternWaypoints[waypointNum]) < 2
						|| (this.myRC
								.canSenseSquare(this.holdingPatternWaypoints[waypointNum]) && this.myRC
								.senseObjectAtLocation(
										this.holdingPatternWaypoints[waypointNum],
										RobotLevel.IN_AIR) != null)) {
					waypointNum = (waypointNum + 1)
							% this.holdingPatternWaypoints.length;
					movesToCurrentWaypoint = 0;
					runAtEndOfTurn();
					continue;
				}
				// rotate around counterclockwise. if wall on right, nexxtttt.
				if (this.myRC.senseTerrainTile(this.myRC.getLocation().add(
						this.myRC.getDirection())) == TerrainTile.OFF_MAP
						&& movesToCurrentWaypoint > 10) {
					waypointNum = (waypointNum + 1)
							% this.holdingPatternWaypoints.length;
					movesToCurrentWaypoint = 0;
				}
				movesToCurrentWaypoint++;
				runAtEndOfTurn();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void initializeHoldingPatternWaypoints() {
		MapLocation core = this.myRC.sensePowerCore().getLocation();
		MapLocation[] waypoints = new MapLocation[] {
				core.add(Direction.EAST, HOLDING_PATTERN_DISTANCE),
				// core.add(Direction.NORTH_EAST, HOLDING_PATTERN_DISTANCE),
				core.add(Direction.NORTH, HOLDING_PATTERN_DISTANCE),
				// core.add(Direction.NORTH_WEST, HOLDING_PATTERN_DISTANCE),
				core.add(Direction.WEST, HOLDING_PATTERN_DISTANCE),
				// core.add(Direction.SOUTH_WEST, HOLDING_PATTERN_DISTANCE),
				core.add(Direction.SOUTH, HOLDING_PATTERN_DISTANCE),
		// core.add(Direction.SOUTH_EAST, HOLDING_PATTERN_DISTANCE)
		};
		this.holdingPatternWaypoints = waypoints;
	}

	// not sure if this is needed -- used this to try and get rid of Game
	// Exception but now it is not being thrown anymore
	public void attackEnemy() {
		Robot closestTar = senseClosestEnemy();
		try {
			if (closestTar != null
					&& myRC.senseRobotInfo(closestTar).type != RobotType.TOWER
					&& myRC.senseRobotInfo(closestTar).flux > .5) {
				MapLocation Location = myRC.senseLocationOf(closestTar);
				if (myRC.canAttackSquare(Location) && !myRC.isAttackActive()) {
					myRC.setIndicatorString(0,
							"Attacking at the end of the turn.");
					if (closestTar.getRobotLevel() == RobotLevel.ON_GROUND) {
						myRC.attackSquare(Location, RobotLevel.ON_GROUND);
					} else {
						myRC.attackSquare(Location, RobotLevel.IN_AIR);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
