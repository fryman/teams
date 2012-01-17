package pather;

import battlecode.common.*;

import java.util.*;

import pather.Nav.*;

public class ArchonPlayer extends BasePlayer {

	private PowerNode core = null;
	private Random r = new Random();
	private MapLocation targetLoc = null; // the location at which the tower
											// should be built
	private MapLocation[] powerNodes = myRC.senseCapturablePowerNodes();
	private ArrayList<MapLocation> locsToBuild = new ArrayList<MapLocation>();
	// need to remove from enemyTowerLocs if we destroy enemy towers
	private ArrayList<MapLocation> enemyTowerLocs = new ArrayList<MapLocation>();
	private PowerNode[] powerNodesOwned = myRC.senseAlliedPowerNodes();
	private int roundsUsedToMoveAway = 0; // TODO find a suitable maximum for
											// this.

	public ArchonPlayer(RobotController rc) {
		super(rc);
		// Today Archons use BugNav
		this.nav = new BugNav(rc);
	}

	/**
	 * Code to run once per turn, at the very end.
	 * 
	 * Includes RobotController.yield() statement, so this method should be
	 * called when the Robot is done with its turn.
	 */
	@Override
	public void runAtEndOfTurn() {
		aboutToDie();
		broadcastMessage();
		this.findWeakFriendsAndTransferFlux();
		myRC.yield();
	}

	public void run() {
		while (true) {
			try {
				while (myRC.isMovementActive()) {
					runAtEndOfTurn();
				}
				if (core == null) {
					core = myRC.sensePowerCore();
				}

				while (Clock.getRoundNum() < 200) {
					while (!spreadOutFromOtherArchons()) {
						while (myRC.isMovementActive()) {
							runAtEndOfTurn();
						}
					}
					break;
				}
				checkAndCreateConvoy();
				myRC.setIndicatorString(0, "convoy created");

				getNewTarget();

				while (targetLoc != null
						&& !myRC.getLocation().isAdjacentTo(targetLoc)) {
					this.nav.getNextMove(targetLoc);
					runAtEndOfTurn();
					// check if we're going to a loc with a tower already
					updateUnownedNodes();
					boolean quit = true;
					for (MapLocation i : locsToBuild) {
						if (i.equals(targetLoc)) {
							quit = false;
						}
					}
					if (quit) {
						getNewTarget();
					}
				}
				if (targetLoc == null) {
					continue;
				}
				// NOW we are guaranteed to be at the targetLoc adjacency
				if (myRC.getDirection() != myRC.getLocation().directionTo(
						targetLoc)) {
					while (myRC.isMovementActive()) {
						runAtEndOfTurn();
					}
					myRC.setDirection(myRC.getLocation().directionTo(targetLoc));
					runAtEndOfTurn();
				}
				// Now we can build a fucking tower
				boolean enemyTower = enemyTowerPresent(targetLoc);

				if (enemyTower == true) {
					myRC.setIndicatorString(1, "attempting destroy at: "
							+ targetLoc.toString());
					enemyTowerLocs.add(targetLoc);
					getNewTarget();

				} else {
					myRC.setIndicatorString(1, "attempting build at: "
							+ targetLoc.toString());
					buildTower(targetLoc);
				}
				runAtEndOfTurn();

			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks to see if there's a scout and a soldier in the convoy around this
	 * archon (in the convoy means in sensor range).
	 * 
	 * If the convoy is deficient, creates scout or soldier to complete the
	 * convoy.
	 */
	public void checkAndCreateConvoy() {
		try {
			Robot[] neighbors = myRC.senseNearbyGameObjects(Robot.class);
			boolean scoutPresent = false;
			boolean soldierPresent = false;
			for (Robot n : neighbors) {
				if (n.getTeam() == this.myRC.getTeam()) {
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SCOUT)) {
						scoutPresent = true;
					}
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SOLDIER)) {
						soldierPresent = true;
					}
				}
			}
			// if cannot see scout, spawn one.
			if (!scoutPresent) {
				spawnScoutAndTransferFlux();
			}
			// if cannot see soldier, spawn one.
			if (!soldierPresent) {
				spawnSoldierAndTransferFlux();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Identifies a weak friendly unit nearby and transfers flux to it. Flux
	 * amount depends on the weakness TODO find a suitable method of determining
	 * amount of flux to transfer.
	 * 
	 * Currently the cutoff for flux transfer is if the friendly has 30% max
	 * flux, transfer another 30% to it.
	 * 
	 * @return true when flux transfer was successful, false otherwise
	 */
	public boolean findWeakFriendsAndTransferFlux() {
		try {
			Robot weakFriendlyUnit = findAWeakFriendly();
			if (weakFriendlyUnit != null) {
				// figure out how much flux he needs.
				RobotInfo weakRobotInfo = myRC.senseRobotInfo(weakFriendlyUnit);
				double weakFluxAmount = weakRobotInfo.flux;
				double maxFluxAmount = weakRobotInfo.type.maxFlux;
				double fluxAmountToTransfer = 0;
				if (weakFluxAmount / maxFluxAmount < 0.3) {
					fluxAmountToTransfer = 0.3 * maxFluxAmount;
				}
				if (fluxAmountToTransfer > 0
						&& myRC.getFlux() > fluxAmountToTransfer) {
					myRC.transferFlux(weakRobotInfo.location,
							weakRobotInfo.robot.getRobotLevel(),
							fluxAmountToTransfer);
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void updateUnownedNodes() {
		powerNodes = myRC.senseCapturablePowerNodes();
		// TODO this line takes 98 bytecodes. is this necessary??
		locsToBuild = new ArrayList<MapLocation>(Arrays.asList(powerNodes));
		powerNodesOwned = myRC.senseAlliedPowerNodes();
	}

	/**
	 * Finds a new PowerNode that we can build on. Sets targetLoc to this node's
	 * location.
	 */
	public void getNewTarget() {
		updateUnownedNodes();
		if (locsToBuild.size() != 0) {
			for (MapLocation m : locsToBuild) {
				if (!enemyTowerLocs.contains(m)) {
					targetLoc = m;
					return;
				}
			} // does not handle case where all nodes are enemy towers
		} else {
			targetLoc = myRC.sensePowerCore().getLocation();
		}
	}

	/**
	 * Spawns a tower at the current location
	 * 
	 * @param target
	 *            MapLocation at which to build the tower
	 */
	public void buildTower(MapLocation target) {
		try {
			if (myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) != null
					&& myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND)
							.getTeam() == myRC.getTeam()) {
				return;
			}
			if (myRC.getFlux() >= RobotType.TOWER.spawnCost
					&& myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) == null) {
				while (myRC.isMovementActive()) {
					runAtEndOfTurn();
				}
				if (myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) == null) {
					myRC.spawn(RobotType.TOWER);
					runAtEndOfTurn();
				}
				getNewTarget();
				myRC.setIndicatorString(1, "null");

			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Spawns a scout and transfers flux to it. Causes this archon to wait
	 * (yielding) until it has enough flux to create a scout, and then waits
	 * again until it has enough flux to give to the scout.
	 * 
	 * Currently the amount of flux transferred to the scout is the *full*
	 * amount that the scout is allowed to carry.
	 */
	public void spawnScoutAndTransferFlux() {
		while (true) {
			try {
				myRC.setIndicatorString(0, "creating scout");
				while (myRC.isMovementActive()) {
					runAtEndOfTurn();
				}
				MapLocation potentialLocation = myRC.getLocation().add(
						myRC.getDirection());
				if (myRC.senseTerrainTile(potentialLocation) == TerrainTile.OFF_MAP) {
					// turn right
					if (!myRC.isMovementActive()) {
						myRC.setDirection(myRC.getDirection().rotateRight());
					}
					continue;
				}
				if (myRC.getFlux() > RobotType.SCOUT.spawnCost
						&& myRC.senseObjectAtLocation(potentialLocation,
								RobotLevel.IN_AIR) == null) {
					myRC.spawn(RobotType.SCOUT);
					myRC.setIndicatorString(2, "just spawned scout: ");
					runAtEndOfTurn();
					Robot recentScout = (Robot) myRC.senseObjectAtLocation(
							potentialLocation, RobotLevel.IN_AIR);
					myRC.setIndicatorString(2, "recent scout: " + recentScout);
					if (recentScout == null) {
						runAtEndOfTurn();
						myRC.setIndicatorString(2, "recent scout null");
						continue;
					}
					runAtEndOfTurn();
					while ((RobotType.SCOUT.maxFlux) > myRC.getFlux()
							&& myRC.canSenseObject(recentScout)) {
						super.runAtEndOfTurn();
					}
					if (myRC.canSenseObject(recentScout)
							&& acceptableFluxTransferLocation(myRC
									.senseLocationOf(recentScout))
							&& myRC.senseRobotInfo(recentScout).flux < RobotType.SCOUT.maxFlux) {
						myRC.transferFlux(myRC.senseLocationOf(recentScout),
								RobotLevel.IN_AIR, RobotType.SCOUT.maxFlux);
					}
					return;
				}
				myRC.setIndicatorString(1, "did not attempt to create scout");
				myRC.setIndicatorString(
						2,
						Boolean.toString(myRC.getFlux() > RobotType.SCOUT.spawnCost));
				runAtEndOfTurn();
			} catch (GameActionException e) {
				System.out.println("Exception caught");
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * Spawns a soldier and transfers flux to it. Causes this archon to wait
	 * (yielding) until it has enough flux to create a soldier, and then waits
	 * again until it has enough flux to give to the soldier.
	 * 
	 * TODO is this initial flux transfer the correct amount?
	 */
	public void spawnSoldierAndTransferFlux() {
		while (true) {
			try {
				myRC.setIndicatorString(0, "creating soldier");
				while (myRC.isMovementActive()) {
					runAtEndOfTurn();
				}
				MapLocation potentialLocation = myRC.getLocation().add(
						myRC.getDirection());
				if (this.myRC.senseTerrainTile(potentialLocation) != TerrainTile.LAND) {
					this.myRC.setDirection(this.myRC.getDirection()
							.rotateRight());
				}
				if (myRC.getFlux() > RobotType.SOLDIER.spawnCost
						&& myRC.senseObjectAtLocation(potentialLocation,
								RobotLevel.ON_GROUND) == null
						&& this.myRC.senseTerrainTile(potentialLocation) == TerrainTile.LAND) {
					myRC.spawn(RobotType.SOLDIER);
					myRC.setIndicatorString(2, "just spawned soldier: ");
					runAtEndOfTurn();
					Robot recentSoldier = (Robot) myRC.senseObjectAtLocation(
							potentialLocation, RobotLevel.ON_GROUND);
					myRC.setIndicatorString(2, "recent soldier: "
							+ recentSoldier);
					if (recentSoldier == null) {
						runAtEndOfTurn();
						myRC.setIndicatorString(2, "recent soldier null");
						continue;
					}
					runAtEndOfTurn();
					while ((RobotType.SOLDIER.maxFlux / 2) > myRC.getFlux()
							&& myRC.canSenseObject(recentSoldier)) {
						super.runAtEndOfTurn();
					}
					if (myRC.canSenseObject(recentSoldier)
							&& acceptableFluxTransferLocation(myRC
									.senseLocationOf(recentSoldier))
							&& myRC.senseRobotInfo(recentSoldier).flux < RobotType.SOLDIER.maxFlux / 2) {
						myRC.transferFlux(myRC.senseLocationOf(recentSoldier),
								RobotLevel.ON_GROUND,
								RobotType.SOLDIER.maxFlux / 2);
					}
					return;
				}
				myRC.setIndicatorString(1, "did not attempt to create soldier");
				myRC.setIndicatorString(
						2,
						Boolean.toString(myRC.getFlux() > RobotType.SOLDIER.spawnCost));
				runAtEndOfTurn();
			} catch (GameActionException e) {
				System.out.println("Exception caught");
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * Causes this archon to move away from neighboring archons, allowing
	 * greater flux regeneration. Useful for the beginning of the game.
	 * 
	 * 1) Sense neighboring archons.
	 * 
	 * 2) If distance^2 < X, move away. X is equal to the PRODUCTION_PENALTY_R2
	 * 
	 * 3) Direction moving away is opposite the ray to the closest archon.
	 * 
	 * @return returns true when the nearest archon is greater than X away,
	 *         false otherwise
	 */
	public boolean spreadOutFromOtherArchons() {
		try {
			if (roundsUsedToMoveAway >= 20) {
				// stop moving - you're stuck.
				return true;
			}
			int minimumDistance = GameConstants.PRODUCTION_PENALTY_R2;
			MapLocation[] archons = myRC.senseAlliedArchons();
			MapLocation currentLoc = this.myRC.getLocation();
			MapLocation closest = null;
			double smallestDistance = 0; // this should be the distance from
											// this
											// robot to closest
			double dist;
			for (MapLocation m : archons) {
				dist = m.distanceSquaredTo(currentLoc);
				if (dist == 0) {
					continue;
				}
				if (closest == null) {
					closest = m;
					smallestDistance = dist;
				} else if (dist < smallestDistance) {
					closest = m;
					smallestDistance = dist;
				}
			}
			if (smallestDistance < minimumDistance && closest != null) {
				myRC.setIndicatorString(
						0,
						"closest archon: "
								+ myRC.senseObjectAtLocation(closest,
										RobotLevel.ON_GROUND).getID());
				MapLocation fartherAwayTarget = currentLoc.add(currentLoc
						.directionTo(closest).opposite(), minimumDistance
						- (int) smallestDistance);
				this.nav.getNextMove(fartherAwayTarget);
				roundsUsedToMoveAway++;
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
