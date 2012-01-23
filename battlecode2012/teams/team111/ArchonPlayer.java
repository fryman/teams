package team111;

import battlecode.common.*;

import java.util.*;

import team111.Nav.*;
import team111.util.FastHashSet;

public class ArchonPlayer extends BasePlayer {

	private Random r = new Random();
	private MapLocation targetLoc = null; // the location at which the tower
											// should be built
	private MapLocation[] capturablePowerNodes = myRC
			.senseCapturablePowerNodes();
	private ArrayList<MapLocation> locsToBuild = new ArrayList<MapLocation>();
	// need to remove from enemyTowerLocs if we destroy enemy towers
	private ArrayList<MapLocation> enemyTowerLocs = new ArrayList<MapLocation>();
	private PowerNode[] powerNodesOwned = myRC.senseAlliedPowerNodes();
	private int roundsUsedToMoveAway = 0; // TODO find a suitable maximum for
											// this.
	private double prevEnergon = 0;
	private int scorcherCount = 0;
	private int scoutCount = 0;
	private MapLocation locationApproaching;
	private MapLocation enemyPowerCoreEstimate;
	private boolean iSeeEnemy = false;
	private final int MIN_ENEMIES_FOR_SCORCHER = 3;

	public ArchonPlayer(RobotController rc) {
		super(rc);
		this.nav = new LocalAreaNav(rc);
	}

	/**
	 * Code to run once per turn, at the very end.
	 * 
	 * Includes RobotController.yield() statement, so this method should be
	 * called when the Robot is done with its turn.
	 */
	@Override
	public void runAtEndOfTurn() {
		myRC.yield();
		checkAndAttemptCreateConvoy();
		aboutToDie();
		sendPingOrEnemyLoc();
		this.findWeakFriendsAndTransferFlux();
		if (beingAttacked()) {
			bugOut();
		}
		this.prevEnergon = this.myRC.getEnergon();
		if (scorcherPresent()) {
			bugOut();
			spawnUnitAndTransferFlux(RobotType.DISRUPTER);
		}
	}

	/**
	 * Spawns disrupter.
	 * 
	 * @deprecated Replaced by spawnUnitAndTransferFlux();
	 */
	@Deprecated
	private void spawnDisrupterAndTransferFlux() {
		try {
			while (myRC.isMovementActive()) {
				super.runAtEndOfTurn();
			}
			MapLocation potentialLocation = myRC.getLocation().add(
					myRC.getDirection());
			TerrainTile potentialTerrain = myRC
					.senseTerrainTile(potentialLocation);
			if (potentialTerrain == TerrainTile.OFF_MAP
					|| potentialTerrain == TerrainTile.VOID) {
				return;
			}
			if (myRC.getFlux() >= RobotType.DISRUPTER.spawnCost
					&& myRC.senseObjectAtLocation(potentialLocation,
							RobotLevel.ON_GROUND) == null) {
				myRC.spawn(RobotType.DISRUPTER);
				runAtEndOfTurn();
				Robot recentDisrupter = (Robot) myRC.senseObjectAtLocation(
						potentialLocation, RobotLevel.ON_GROUND);
				if (recentDisrupter == null) {
					return;
				}
				runAtEndOfTurn();
				while ((RobotType.DISRUPTER.maxFlux / 2.0) > myRC.getFlux()
						&& myRC.canSenseObject(recentDisrupter)) {
					super.runAtEndOfTurn();
				}
				if (myRC.canSenseObject(recentDisrupter)
						&& acceptableFluxTransferLocation(myRC
								.senseLocationOf(recentDisrupter))
						&& myRC.senseRobotInfo(recentDisrupter).flux < RobotType.DISRUPTER.maxFlux / 2.0) {
					myRC.transferFlux(myRC.senseLocationOf(recentDisrupter),
							RobotLevel.ON_GROUND,
							RobotType.DISRUPTER.maxFlux / 2.0);
				}

			}
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
			return;
		}
	}

	/**
	 * 
	 * @return true when archon can see a scorcher, else false.
	 */
	public boolean scorcherPresent() {
		Robot scorch = findNearestEnemyRobotType(RobotType.SCORCHER);
		if (scorch == null) {
			return false;
		}
		return true;
	}

	/**
	 * Causes archon to move back to the powercore (assumes it is being
	 * attacked)
	 */
	public void bugOut() {
		// if (!myRC.isMovementActive()
		// && myRC.canMove(myRC.getDirection().opposite())) {
		// try {
		// myRC.moveBackward();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		this.nav.getNextMove(this.myRC.sensePowerCore().getLocation());
	}

	/**
	 * Run method allowing for case by case archon run methods.
	 * 
	 * @author brian
	 */
	public void run() {
		try {
			MapLocation[] archons = myRC.senseAlliedArchons();
			int[] IDNumbers = new int[battlecode.common.GameConstants.NUMBER_OF_ARCHONS];
			int Counter = 0;
			for (MapLocation m : archons) {
				Robot r = (Robot) myRC.senseObjectAtLocation(m,
						RobotLevel.ON_GROUND);
				IDNumbers[Counter] = r.getID();
				Counter++;
			}
			if (myRC.getRobot().getID() == IDNumbers[0]) {
				runDefendCoreWithScorchers();
			} else {
				runArchonBrain();
			}
			// enemyPowerCoreEstimate = estimateEnemyPowerCore();
			// runArchonBrain();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * First archon brain, for building towers.
	 * 
	 * @author saf
	 */
	public void runArchonBrain() {
		MapLocation core = this.myRC.sensePowerCore().getLocation();
		while (true) {
			try {
				// This causes the archons to spread out quickly, and limits
				// spreading to 50 rounds. Realistically spreading out is
				// limited to 20 rounds in spreadOutFromOtherArchons()
				while (Clock.getRoundNum() < 50 && !spreadOutFromOtherArchons()) {
					while (myRC.isMovementActive()) {
						runAtEndOfTurn();
					}
				}
				if (iSeeEnemy) {
					if (numEnemiesPresent() >= MIN_ENEMIES_FOR_SCORCHER) {
						spawnUnitAndTransferFlux(RobotType.SCORCHER);
					}
					this.nav.getNextMove(myRC.sensePowerCore().getLocation());
					runAtEndOfTurn();
					continue;
				}
				MapLocation capturing = getNewTarget();
				myRC.setIndicatorString(0, "capturing: " + capturing + " "
						+ Clock.getRoundNum());
				/*
				 * if (beingAttacked()) { if
				 * (myRC.canMove(myRC.getDirection().opposite())) {
				 * myRC.setDirection(myRC.getDirection().opposite()); } }
				 */
				if (Clock.getRoundNum() < 1000) {
					int roundNum = Clock.getBytecodeNum();
					if (roundNum % 10 == 0) {
						runAtEndOfTurn();
						continue;
					}
					switch (roundNum % 4) {
					case 0:
						this.nav.getNextMove(core.add(4, 4));
						break;
					case 1:
						this.nav.getNextMove(core.add(4, -4));
						break;
					case 2:
						this.nav.getNextMove(core.add(-4, 4));
						break;
					case 3:
						this.nav.getNextMove(core.add(-4, -4));
					}
					runAtEndOfTurn();
				} else {
					goToPowerNodeForBuild(capturing);
					buildOrDestroyTower(capturing);
					runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}

	private int numEnemiesPresent() {
		Robot[] sensedRobots = myRC.senseNearbyGameObjects(Robot.class);
		int e = 0;
		for (Robot r : sensedRobots) {
			if (r.getTeam() != this.myRC.getTeam()) {
				e++;
			}
		}
		return e;
	}

	public void runDefendCoreWithScorchers() {
		while (true) {
			try {
				MapLocation core = myRC.sensePowerCore().getLocation();
				while (myRC.isMovementActive()) {
					super.runAtEndOfTurn();
				}
				while (scoutCount < 1) {
					spawnUnitAndTransferFlux(RobotType.SCOUT);
					scoutCount++;
				}
				while (scorcherCount < 5) {
					int countMoves = 0;
					spawnUnitAndTransferFlux(RobotType.SCORCHER);
					scorcherCount++;
					while (countMoves < 4) {
						randomWalk();
						countMoves++;
					}
				}
				if (myRC.getLocation().distanceSquaredTo(core) > 9) {
					while (!myRC.getLocation().isAdjacentTo(core)) {
						this.nav.getNextMove(core);
						super.runAtEndOfTurn();
					}
				}
				fluxToFriends();
				super.runAtEndOfTurn();
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Archons rush and build soldiers on the way.
	 * 
	 * @author brian
	 */

	public void runArchonRush() {
		boolean trigger = false;
		MapLocation estimate = estimateEnemyPowerCore();
		while (true) {
			try {
				while (myRC.isMovementActive()) {
					runAtEndOfTurn();
				}
				nav.getNextMove(estimate);
				if (myRC.getFlux() >= 290) {
					spawnUnitAndTransferFlux(RobotType.SOLDIER);
					trigger = true;
				}
				if (trigger) {
					spawnUnitAndTransferFlux(RobotType.SOLDIER);
				}
				runAtEndOfTurn();
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}

	/**
	 * A method for archons to sit and refuel scorchers after building a
	 * defensive perimiter around the power core.
	 * 
	 * @author tburfield
	 */

	public void fluxToFriends() {
		try {
			Robot weakFriendlyUnit = findAWeakFriendly();
			if (weakFriendlyUnit != null) {
				myRC.setIndicatorString(0, "found friendly");
				MapLocation weakLoc = myRC.senseLocationOf(weakFriendlyUnit);
				while (!myRC.getLocation().isAdjacentTo(weakLoc)) {
					this.nav.getNextMove(weakLoc);
					runAtEndOfTurn();
				}
				// figure out how much flux he needs.
				RobotInfo weakRobotInfo = myRC.senseRobotInfo(weakFriendlyUnit);
				double weakFluxAmount = weakRobotInfo.flux;
				double maxFluxAmount = weakRobotInfo.type.maxFlux;
				double fluxAmountToTransfer = 0.5 * (maxFluxAmount - weakFluxAmount);
				if (fluxAmountToTransfer > myRC.getFlux()) {
					fluxAmountToTransfer = myRC.getFlux();
				}
				if (fluxAmountToTransfer > 0) {
					myRC.setIndicatorString(1, "transfered flux");
					myRC.transferFlux(weakRobotInfo.location,
							weakRobotInfo.robot.getRobotLevel(),
							fluxAmountToTransfer);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Attempts to build a tower on a given location, or destroy it if there's
	 * an enemy tower present.
	 * 
	 * @param capturing
	 *            MapLocation on which to build a tower, or destroy an enemy
	 *            tower. Destroy occurs simply because this archon has a convoy
	 *            which should automatically attack the tower.
	 */
	public void buildOrDestroyTower(MapLocation capturing) {
		try {
			if (myRC.getDirection() != myRC.getLocation()
					.directionTo(capturing)) {
				while (myRC.isMovementActive()) {
					runAtEndOfTurn();
				}
				myRC.setDirection(myRC.getLocation().directionTo(capturing));
				runAtEndOfTurn();
			}
			boolean enemyTower = enemyTowerPresent(capturing);

			if (enemyTower == true) {
				myRC.setIndicatorString(1, "attempting destroy at: "
						+ targetLoc.toString());
				enemyTowerLocs.add(targetLoc);
				// TODO this can be modified to create an army here.
				// since the archon is currently facing the enemy tower, we need
				// to turn away to allow a soldier to be created (if one does
				// not already exist)
				myRC.setIndicatorString(
						2,
						"enemy tower present, waiting for soldier: "
								+ Clock.getRoundNum());
				myRC.setIndicatorString(1,
						"soldier nearby: " + Boolean.toString(soldierNearby())
								+ " " + Clock.getRoundNum());
				if (!soldierNearby()
						&& myRC.senseObjectAtLocation(
								myRC.getLocation().add(myRC.getDirection()),
								RobotLevel.ON_GROUND) != null
						&& !this.myRC.isMovementActive()) {
					// turn so that we can spawn a soldier
					myRC.setIndicatorString(
							2,
							"turned right to allow soldier to spawn: "
									+ Clock.getRoundNum());
					myRC.setDirection(myRC.getDirection().rotateRight());
				}
			} else {
				myRC.setIndicatorString(1,
						"attempting build at: " + targetLoc.toString());
				buildTower(targetLoc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Determines if there's a soldier nearby. TODO this can be moved to
	 * baseplayer.
	 * 
	 * @return true if there's a soldier within sensor range, false otherwise.
	 */
	public boolean soldierNearby() {
		try {
			Robot[] neighbors = myRC.senseNearbyGameObjects(Robot.class);
			for (Robot n : neighbors) {
				if (n.getTeam() == this.myRC.getTeam()) {
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SOLDIER)) {
						return true;
					}
				}
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Causes this archon to go to a MapLocation. Does not attempt to build at
	 * that mapLocation. If another archon builds a tower at the given
	 * mapLocation, this method finds another MapLocation on which a tower
	 * should be built.
	 * 
	 * This method exits when there is no possible node to capture or we are
	 * adjacent to the capturable node.
	 * 
	 * @param capturing
	 *            The MapLocation on which the tower should be built.
	 * @return a MapLocation that this archon can immediately build on (we are
	 *         adjacent and the location is not null)
	 */
	public MapLocation goToPowerNodeForBuild(MapLocation capturing) {
		try {
			MapLocation locationToCapture = capturing;
			while (locationToCapture != null
					&& !myRC.getLocation().isAdjacentTo(locationToCapture)
					&& !beingAttacked()) {
				this.nav.getNextMove(locationToCapture);
				this.locationApproaching = locationToCapture;
				runAtEndOfTurn();
				// check if we're going to a loc with a tower already
				updateUnownedNodes();
				boolean quit = true;
				for (MapLocation possibleToCapture : capturablePowerNodes) {
					if (possibleToCapture.equals(locationToCapture)) {
						quit = false;
					}
				}
				if (quit) {
					locationToCapture = getNewTarget();
				}
			}
			// we've accidentally stopped on top of the powernode
			if (myRC.getLocation().equals(locationToCapture)) {
				// and need to move off.
				myRC.setIndicatorString(
						0,
						"whoops, stepping on the locationtocapture: "
								+ Clock.getRoundNum());
				if (myRC.canMove(myRC.getDirection())) {
					myRC.moveForward();
				}
			}
			return locationToCapture;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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
			int scoutPresent = 0;
			int soldierPresent = 0;
			for (Robot n : neighbors) {
				if (n.getTeam() == this.myRC.getTeam()) {
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SCOUT)) {
						scoutPresent++;
					}
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SOLDIER)) {
						soldierPresent++;
					}
				}
			}
			// if cannot see scout, spawn one.
			if (scoutPresent == 0) {
				spawnUnitAndTransferFlux(RobotType.SCOUT);
			}
			// if cannot see soldier, spawn one.
			if (soldierPresent < 4) {
				spawnUnitAndTransferFlux(RobotType.SOLDIER);
			}
			if (scoutPresent < 3) {
				spawnUnitAndTransferFlux(RobotType.SCOUT);
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
				if (weakFluxAmount / maxFluxAmount < 0.3
						&& weakRobotInfo.type != RobotType.SCOUT) {
					fluxAmountToTransfer = 0.3 * maxFluxAmount;
				} else if (weakFluxAmount / maxFluxAmount < 0.3) {
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

	/**
	 * Updates the list of capturable power nodes
	 * 
	 * Updates the list of locsToBuild
	 * 
	 * Updates the list of powerNodesOwned (where towers are present)
	 */
	public void updateUnownedNodes() {
		capturablePowerNodes = myRC.senseCapturablePowerNodes();
		// TODO this line takes 98 bytecodes. is this necessary??
		locsToBuild = new ArrayList<MapLocation>(
				Arrays.asList(capturablePowerNodes));
		powerNodesOwned = myRC.senseAlliedPowerNodes();
	}

	/**
	 * Finds PowerNode that we can build on "closest" to the opponent PowerCore.
	 * (Farthest from ours...). Sets targetLoc to this node's location.
	 * 
	 * If there is no powernode that we can build on, sets targetLoc to this
	 * team's powercore.
	 * 
	 * @return a MapLocation that we can build a tower on. Note: this may be an
	 *         enemy tower. Null if no such tower exists.
	 */
	public MapLocation getNewTarget() {
		updateUnownedNodes();
		if (capturablePowerNodes.length != 0) {
			MapLocation best = capturablePowerNodes[0];
			// MapLocation here = this.myRC.getLocation();
			// double[] com = archonCOM();
			// MapLocation archonCOM = getMapLocationFromCoordinates(com);

			// double smallestScoreToThis = here.distanceSquaredTo(best)
			// / (best.distanceSquaredTo(archonCOM) + 1.01)
			// + best.distanceSquaredTo(enemyPowerCoreEstimate);
			// double smallestScoreToThis = this.myRC.sensePowerCore()
			// .getLocation().distanceSquaredTo(capturablePowerNodes[0])
			// / Math.pow((archonCOM
			// .distanceSquaredTo(capturablePowerNodes[0]) + 1.0),
			// 2);
			// double smallestScoreToThis = this.myRC.sensePowerCore()
			// .getLocation().distanceSquaredTo(capturablePowerNodes[0]);
			// double sample;
			//
			// for (int i = 1; i < capturablePowerNodes.length; i++) {
			// // sample = capturablePowerNodes[i].distanceSquaredTo(here)
			// // / (capturablePowerNodes[i].distanceSquaredTo(archonCOM) +
			// // 0.01)
			// // * best.distanceSquaredTo(enemyPowerCoreEstimate);
			// // if(
			// //
			// capturablePowerNodes[i].distanceSquaredTo(estimateEnemyPowerCore())<6
			// // ){
			// // best = capturablePowerNodes[i];
			// // targetLoc = best;
			// // return best;
			// // }
			// sample = this.myRC.sensePowerCore().getLocation()
			// .distanceSquaredTo(capturablePowerNodes[i]);
			// if (sample < smallestScoreToThis) {
			// smallestScoreToThis = sample;
			// best = capturablePowerNodes[i];
			// }
			// }
			MapLocation[] allies = this.myRC.senseAlliedArchons();
			for (int i = 0; i < allies.length; i++) {
				if (allies[i].equals(this.myRC.getLocation())) {
					best = capturablePowerNodes[i % capturablePowerNodes.length];
				}
			}
			targetLoc = best; // to conform to method signature
			return best;
		} else {
			targetLoc = myRC.sensePowerCore().getLocation(); // to conform to
																// the method
																// signature.
			return null;
		}
	}

	/**
	 * Spawns a tower at the current location if this robot has enough flux to
	 * do so.
	 * 
	 * @param target
	 *            MapLocation at which to build the tower
	 */
	public void buildTower(MapLocation target) {
		try {
			if (!myRC.canSenseSquare(target)) {
				return;
			}
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
				if (!myRC.canSenseSquare(target)) {
					return;
				}
				getNewTarget();
				myRC.setIndicatorString(1, "null");
			}
		} catch (GameActionException e) {
			System.out.println(Clock.getBytecodeNum());
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
	 * 
	 * @deprecated Replaced by spawnUnitAndTransferFlux();
	 */
	@Deprecated
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
	 * Spawns a unit and transfers flux to it. Causes this archon to wait
	 * (yielding) until it has enough flux to create the unity, and then waits
	 * again until it has enough flux to give to the unit.
	 */
	public void spawnUnitAndTransferFlux(RobotType unitType) {
		while (true) {
			try {
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
				RobotLevel unitLevel = RobotLevel.ON_GROUND;
				double flux = unitType.maxFlux / 2;
				if (unitType == RobotType.SCOUT) {
					unitLevel = RobotLevel.IN_AIR;
					flux = RobotType.SCOUT.maxFlux;
				}
				if (myRC.getFlux() > unitType.spawnCost
						&& myRC.senseObjectAtLocation(potentialLocation,
								unitLevel) == null) {
					myRC.spawn(unitType);
					runAtEndOfTurn();
					Robot recentUnit = (Robot) myRC.senseObjectAtLocation(
							potentialLocation, unitLevel);
					if (recentUnit == null) {
						runAtEndOfTurn();
						continue;
					}
					runAtEndOfTurn();
					while ((flux) > myRC.getFlux()
							&& myRC.canSenseObject(recentUnit)) {
						super.runAtEndOfTurn();
					}
					if (myRC.canSenseObject(recentUnit)
							&& acceptableFluxTransferLocation(myRC
									.senseLocationOf(recentUnit))
							&& myRC.senseRobotInfo(recentUnit).flux < unitType.maxFlux) {
						myRC.transferFlux(myRC.senseLocationOf(recentUnit),
								unitLevel, flux);
					}
					return;
				}
				runAtEndOfTurn();
			} catch (GameActionException e) {
				System.out.println("Exception caught");
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * Overloaded spawnUnit function that allows for variable flux transfer.
	 */
	public void spawnUnitAndTransferFlux(RobotType unitType,
			double fractionOfMaxFlux) {
		while (true) {
			try {
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
				RobotLevel unitLevel = RobotLevel.ON_GROUND;
				double flux = unitType.maxFlux * fractionOfMaxFlux;
				if (fractionOfMaxFlux > 1) {
					flux = unitType.maxFlux;
				}
				if (unitType == RobotType.SCOUT) {
					unitLevel = RobotLevel.IN_AIR;
				}
				if (myRC.getFlux() > unitType.spawnCost
						&& myRC.senseObjectAtLocation(potentialLocation,
								unitLevel) == null) {
					myRC.spawn(unitType);
					runAtEndOfTurn();
					Robot recentUnit = (Robot) myRC.senseObjectAtLocation(
							potentialLocation, unitLevel);
					if (recentUnit == null) {
						runAtEndOfTurn();
						continue;
					}
					runAtEndOfTurn();
					while ((flux) > myRC.getFlux()
							&& myRC.canSenseObject(recentUnit)) {
						super.runAtEndOfTurn();
					}
					if (myRC.canSenseObject(recentUnit)
							&& acceptableFluxTransferLocation(myRC
									.senseLocationOf(recentUnit))
							&& myRC.senseRobotInfo(recentUnit).flux < unitType.maxFlux) {
						myRC.transferFlux(myRC.senseLocationOf(recentUnit),
								unitLevel, flux);
					}
					return;
				}
				runAtEndOfTurn();
			} catch (GameActionException e) {
				System.out.println("Exception caught");
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * ATTEMPTS to spawns a unit and transfers flux to it. Causes this archon to
	 * wait (yielding) until it has enough flux to transfer to a unit. Does NOT
	 * cause the archon to wait until it has enough flux to spawn a unit.
	 */
	public void attemptSpawnUnitAndTransferFlux(RobotType unitType) {
		try {
			while (myRC.isMovementActive()) {
				super.runAtEndOfTurn();
			}
			MapLocation potentialLocation = myRC.getLocation().add(
					myRC.getDirection());
			if (myRC.senseTerrainTile(potentialLocation) == TerrainTile.OFF_MAP) {
				return;
			}
			RobotLevel unitLevel = RobotLevel.ON_GROUND;
			double flux = unitType.maxFlux / 2;
			if (unitType == RobotType.SCOUT) {
				unitLevel = RobotLevel.IN_AIR;
				flux = RobotType.SCOUT.maxFlux;
			}
			if (myRC.getFlux() > unitType.spawnCost
					&& myRC.senseObjectAtLocation(potentialLocation, unitLevel) == null) {
				myRC.spawn(unitType);
				runAtEndOfTurn();
				Robot recentUnit = (Robot) myRC.senseObjectAtLocation(
						potentialLocation, unitLevel);
				if (recentUnit == null) {
					return;
				}
				runAtEndOfTurn();
				while (flux > myRC.getFlux() && myRC.canSenseObject(recentUnit)) {
					super.runAtEndOfTurn();
				}
				if (myRC.canSenseObject(recentUnit)
						&& acceptableFluxTransferLocation(myRC
								.senseLocationOf(recentUnit))
						&& myRC.senseRobotInfo(recentUnit).flux < unitType.maxFlux) {
					myRC.transferFlux(myRC.senseLocationOf(recentUnit),
							unitLevel, flux);
				}
				return;
			}
			return;
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Overloaded attemptSpawnUnit function that allows for custom flux
	 * transfer.
	 */
	public void attemptSpawnUnitAndTransferFlux(RobotType unitType,
			double fractionOfMaxFlux) {
		try {
			while (myRC.isMovementActive()) {
				super.runAtEndOfTurn();
			}
			MapLocation potentialLocation = myRC.getLocation().add(
					myRC.getDirection());
			if (myRC.senseTerrainTile(potentialLocation) == TerrainTile.OFF_MAP) {
				return;
			}
			RobotLevel unitLevel = RobotLevel.ON_GROUND;
			double flux = unitType.maxFlux * fractionOfMaxFlux;
			if (fractionOfMaxFlux > 1) {
				flux = unitType.maxFlux;
			}
			if (unitType == RobotType.SCOUT) {
				unitLevel = RobotLevel.IN_AIR;
			}
			if (myRC.getFlux() > unitType.spawnCost
					&& myRC.senseObjectAtLocation(potentialLocation, unitLevel) == null) {
				myRC.spawn(unitType);
				runAtEndOfTurn();
				Robot recentUnit = (Robot) myRC.senseObjectAtLocation(
						potentialLocation, unitLevel);
				if (recentUnit == null) {
					return;
				}
				runAtEndOfTurn();
				while (flux > myRC.getFlux() && myRC.canSenseObject(recentUnit)) {
					super.runAtEndOfTurn();
				}
				if (myRC.canSenseObject(recentUnit)
						&& acceptableFluxTransferLocation(myRC
								.senseLocationOf(recentUnit))
						&& myRC.senseRobotInfo(recentUnit).flux < unitType.maxFlux) {
					myRC.transferFlux(myRC.senseLocationOf(recentUnit),
							unitLevel, flux);
				}
				return;
			}
			return;
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Spawns a soldier and transfers flux to it. Causes this archon to wait
	 * (yielding) until it has enough flux to create a soldier, and then waits
	 * again until it has enough flux to give to the soldier.
	 * 
	 * TODO is this initial flux transfer the correct amount?
	 * 
	 * @deprecated Replaced by spawnUnitAndTransferFlux();
	 */
	@Deprecated
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
						&& this.myRC.senseTerrainTile(potentialLocation) == TerrainTile.LAND
						&& this.myRC.senseObjectAtLocation(potentialLocation,
								RobotLevel.POWER_NODE) == null) {
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
	 * Spawns a scorcher and transfers flux to it. Causes this archon to wait
	 * (yielding) until it has enough flux to create a scorcher, and then waits
	 * again until it has enough flux to give to the scorcher.
	 * 
	 * TODO is this initial flux transfer the correct amount?
	 * 
	 * @deprecated Replaced by spawnUnitAndTransferFlux();
	 */
	@Deprecated
	public void spawnScorcherAndTransferFlux() {
		while (true) {
			try {
				myRC.setIndicatorString(0, "creating SCORCHER");
				while (myRC.isMovementActive()) {
					super.runAtEndOfTurn();
				}
				MapLocation potentialLocation = myRC.getLocation().add(
						myRC.getDirection());
				if (this.myRC.senseTerrainTile(potentialLocation) != TerrainTile.LAND
						|| potentialLocation.equals(myRC.sensePowerCore()
								.getLocation())
						|| this.myRC.senseObjectAtLocation(potentialLocation,
								RobotLevel.ON_GROUND) != null) {
					this.myRC.setDirection(this.myRC.getDirection()
							.rotateRight());
				}
				if (myRC.getFlux() > RobotType.SCORCHER.spawnCost
						&& myRC.senseObjectAtLocation(potentialLocation,
								RobotLevel.ON_GROUND) == null
						&& this.myRC.senseTerrainTile(potentialLocation) == TerrainTile.LAND
						&& this.myRC.senseObjectAtLocation(potentialLocation,
								RobotLevel.POWER_NODE) == null) {
					myRC.spawn(RobotType.SCORCHER);
					myRC.setIndicatorString(2, "just spawned SCORCHER: ");
					super.runAtEndOfTurn();
					;
					Robot recentSCORCHER = (Robot) myRC.senseObjectAtLocation(
							potentialLocation, RobotLevel.ON_GROUND);
					myRC.setIndicatorString(2, "recent SCORCHER: "
							+ recentSCORCHER);
					if (recentSCORCHER == null) {
						super.runAtEndOfTurn();
						myRC.setIndicatorString(2, "recent SCORCHER null");
						continue;
					}
					super.runAtEndOfTurn();
					while ((RobotType.SCORCHER.maxFlux / 2) > myRC.getFlux()
							&& myRC.canSenseObject(recentSCORCHER)) {
						super.runAtEndOfTurn();
					}
					if (myRC.canSenseObject(recentSCORCHER)
							&& acceptableFluxTransferLocation(myRC
									.senseLocationOf(recentSCORCHER))
							&& myRC.senseRobotInfo(recentSCORCHER).flux < RobotType.SCORCHER.maxFlux / 2) {
						myRC.transferFlux(myRC.senseLocationOf(recentSCORCHER),
								RobotLevel.ON_GROUND,
								RobotType.SCORCHER.maxFlux / 2);
					}
					return;
				}
				myRC.setIndicatorString(1, "did not attempt to create SCORCHER");
				myRC.setIndicatorString(
						2,
						Boolean.toString(myRC.getFlux() > RobotType.SCORCHER.spawnCost));
				super.runAtEndOfTurn();
			} catch (GameActionException e) {
				System.out.println("Exception caught");
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * Spawns a scorcher and transfers flux to it. Does not cause this archon to
	 * wait (yielding) until it has enough flux to create a scorcher, and then
	 * waits again until it has enough flux to give to the scorcher.
	 * 
	 * TODO is this initial flux transfer the correct amount?
	 */
	public void attemptSpawnScorcherAndTransferFlux() {
		try {
			// myRC.setIndicatorString(0,
			// "creating soldier: " + Clock.getRoundNum());
			while (myRC.isMovementActive()) {
				super.runAtEndOfTurn();
			}
			MapLocation potentialLocation = myRC.getLocation().add(
					myRC.getDirection());
			if (this.myRC.senseTerrainTile(potentialLocation) != TerrainTile.LAND) {
				return;
			}
			if (myRC.getFlux() > RobotType.SCORCHER.spawnCost
					&& myRC.senseObjectAtLocation(potentialLocation,
							RobotLevel.ON_GROUND) == null
					&& this.myRC.senseTerrainTile(potentialLocation) == TerrainTile.LAND
					&& this.myRC.senseObjectAtLocation(potentialLocation,
							RobotLevel.POWER_NODE) == null) {
				myRC.spawn(RobotType.SCORCHER);
				// myRC.setIndicatorString(2, "just spawned SCORCHER: ");
				super.runAtEndOfTurn();
				Robot recentSCORCHER = (Robot) myRC.senseObjectAtLocation(
						potentialLocation, RobotLevel.ON_GROUND);
				myRC.setIndicatorString(2, "recent SCORCHER: " + recentSCORCHER);
				if (recentSCORCHER == null) {
					myRC.setIndicatorString(2, "recent SCORCHER null");
					return;
				}
				runAtEndOfTurn();
				while ((RobotType.SCORCHER.maxFlux / 2) > myRC.getFlux()
						&& myRC.canSenseObject(recentSCORCHER)) {
					super.runAtEndOfTurn();
				}
				myRC.setIndicatorString(2, "enough flux for recent SCORCHER: "
						+ recentSCORCHER);
				myRC.setIndicatorString(
						0,
						"can sense recent: "
								+ Boolean.toString(myRC
										.canSenseObject(recentSCORCHER)));
				if (myRC.canSenseObject(recentSCORCHER)
						&& acceptableFluxTransferLocation(myRC
								.senseLocationOf(recentSCORCHER))
						&& myRC.senseRobotInfo(recentSCORCHER).flux < RobotType.SCORCHER.maxFlux / 2) {
					myRC.transferFlux(myRC.senseLocationOf(recentSCORCHER),
							RobotLevel.ON_GROUND,
							RobotType.SCORCHER.maxFlux / 2);
				}
				return;
			}
			// myRC.setIndicatorString(1, "did not attempt to create soldier");
			// myRC.setIndicatorString(
			// 2,
			// Boolean.toString(myRC.getFlux() > RobotType.SOLDIER.spawnCost));
			return;
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
			return;
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
			if (roundsUsedToMoveAway >= 50) {
				return true;
			}
			int minimumDistance = GameConstants.PRODUCTION_PENALTY_R2 / 2;
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
										RobotLevel.ON_GROUND).getID() + " "
								+ Clock.getRoundNum());
				MapLocation fartherAwayTarget = currentLoc.add(currentLoc
						.directionTo(closest).opposite(), minimumDistance
						- (int) smallestDistance);
				this.nav.getNextMove(fartherAwayTarget);
				this.locationApproaching = fartherAwayTarget;
				roundsUsedToMoveAway++;
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * ATTEMPTS to spawns a scout and transfers flux to it. Causes this archon
	 * to wait (yielding) until it has enough flux to transfer to a scout. Does
	 * NOT cause the archon to wait until it has enough flux to spawn a scout.
	 * 
	 * Currently the amount of flux transferred to the scout is the *full*
	 * amount that the scout is allowed to carry.
	 * 
	 * @deprecated Replaced by attemptSpawnUnitAndTransferFlux();
	 */
	@Deprecated
	public void attemptSpawnScoutAndTransferFlux() {
		try {
			// myRC.setIndicatorString(0, "creating scout");
			// TODO are these three lines necessary?
			while (myRC.isMovementActive()) {
				super.runAtEndOfTurn();
			}
			MapLocation potentialLocation = myRC.getLocation().add(
					myRC.getDirection());
			if (myRC.senseTerrainTile(potentialLocation) == TerrainTile.OFF_MAP) {
				return;
			}
			if (myRC.getFlux() > 1.5 * RobotType.SCOUT.spawnCost
					&& myRC.senseObjectAtLocation(potentialLocation,
							RobotLevel.IN_AIR) == null) {
				myRC.spawn(RobotType.SCOUT);
				// myRC.setIndicatorString(2, "just spawned scout: ");
				runAtEndOfTurn();
				Robot recentScout = (Robot) myRC.senseObjectAtLocation(
						potentialLocation, RobotLevel.IN_AIR);
				// myRC.setIndicatorString(2, "recent scout: " + recentScout);
				if (recentScout == null) {
					// myRC.setIndicatorString(2,
					// "recent scout null: " + Clock.getRoundNum());
					return;
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
			// myRC.setIndicatorString(1, "did not attempt to create scout");
			// myRC.setIndicatorString(
			// 2,
			// Boolean.toString(myRC.getFlux() > RobotType.SCOUT.spawnCost));
			return;
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
			return;
		}
	}

	/**
	 * ATTEMPTS to spawns a soldier and transfers flux to it. Causes this archon
	 * to wait (yielding) until it has enough flux to transfer to a soldier.
	 * Does NOT cause the archon to wait until it has enough flux to spawn a
	 * soldier.
	 * 
	 * TODO is this initial flux transfer the correct amount?
	 * 
	 * @deprecated Replaced by attemptSpawnUnitAndTransferFlux();
	 */
	@Deprecated
	public void attemptSpawnSoldierAndTransferFlux() {
		try {
			// myRC.setIndicatorString(0,
			// "creating soldier: " + Clock.getRoundNum());
			while (myRC.isMovementActive()) {
				super.runAtEndOfTurn();
			}
			MapLocation potentialLocation = myRC.getLocation().add(
					myRC.getDirection());
			if (this.myRC.senseTerrainTile(potentialLocation) != TerrainTile.LAND) {
				return;
			}
			if (myRC.getFlux() > RobotType.SOLDIER.spawnCost
					&& myRC.senseObjectAtLocation(potentialLocation,
							RobotLevel.ON_GROUND) == null
					&& this.myRC.senseTerrainTile(potentialLocation) == TerrainTile.LAND
					&& this.myRC.senseObjectAtLocation(potentialLocation,
							RobotLevel.POWER_NODE) == null) {
				myRC.spawn(RobotType.SOLDIER);
				// myRC.setIndicatorString(2, "just spawned soldier: ");
				super.runAtEndOfTurn();
				Robot recentSoldier = (Robot) myRC.senseObjectAtLocation(
						potentialLocation, RobotLevel.ON_GROUND);
				myRC.setIndicatorString(2, "recent soldier: " + recentSoldier);
				if (recentSoldier == null) {
					myRC.setIndicatorString(2, "recent soldier null");
					return;
				}
				runAtEndOfTurn();
				while ((RobotType.SOLDIER.maxFlux / 2) > myRC.getFlux()
						&& myRC.canSenseObject(recentSoldier)) {
					super.runAtEndOfTurn();
				}
				myRC.setIndicatorString(2, "enough flux for recent soldier: "
						+ recentSoldier);
				myRC.setIndicatorString(
						0,
						"can sense recent: "
								+ Boolean.toString(myRC
										.canSenseObject(recentSoldier)));
				if (myRC.canSenseObject(recentSoldier)
						&& acceptableFluxTransferLocation(myRC
								.senseLocationOf(recentSoldier))
						&& myRC.senseRobotInfo(recentSoldier).flux < RobotType.SOLDIER.maxFlux / 2) {
					myRC.transferFlux(myRC.senseLocationOf(recentSoldier),
							RobotLevel.ON_GROUND, RobotType.SOLDIER.maxFlux / 2);
				}
				return;
			}
			// myRC.setIndicatorString(1, "did not attempt to create soldier");
			// myRC.setIndicatorString(
			// 2,
			// Boolean.toString(myRC.getFlux() > RobotType.SOLDIER.spawnCost));
			return;
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Archons sense each other and determine who has the lowest robot number.
	 * He executes special code.
	 * 
	 * @author brian
	 */

	public int checkLowestArchonNumber() {
		try {
			MapLocation[] archons = myRC.senseAlliedArchons();
			int LowestID = 1000;
			for (MapLocation m : archons) {
				Robot r = (Robot) myRC.senseObjectAtLocation(m,
						RobotLevel.ON_GROUND);
				if (r.getID() < LowestID) {
					LowestID = r.getID();
				}
			}
			return LowestID;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Checks to see if there's a scout and a soldier in the convoy around this
	 * archon (in the convoy means in sensor range).
	 * 
	 * If the convoy is deficient, attempts to create scout or soldier to
	 * complete the convoy.
	 * 
	 * The attempts rely on attemptSpawnScoutAndTransferFlux and
	 * attemptSpawnSoldierAndTransferFlux. Note: this method will still cause
	 * the archon to wait until it has enough flux to transfer to the robots it
	 * spawns, but will not cause the archon to stop and wait until it has
	 * enough flux to spawn the robots.
	 */
	public void checkAndAttemptCreateConvoy() {
		try {
			Robot[] neighbors = myRC.senseNearbyGameObjects(Robot.class);
			int scoutPresent = 0;
			int soldierPresent = 0;
			int disrupterPresent = 0;
			for (Robot n : neighbors) {
				if (n.getTeam() == this.myRC.getTeam()) {
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SCOUT)) {
						scoutPresent++;
					}
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SOLDIER)) {
						soldierPresent++;
					}
					if (myRC.senseRobotInfo(n).type.equals(RobotType.DISRUPTER)) {
						disrupterPresent++;
					}
				}
			}
			if (scoutPresent == 0) {
				attemptSpawnUnitAndTransferFlux(RobotType.SCOUT);
			}
			// if cannot see soldier, spawn one.
			if (soldierPresent < 3) {
				attemptSpawnUnitAndTransferFlux(RobotType.SOLDIER);
			}
			// if cannot see disrupter, spawn two.
			if (disrupterPresent < 2) {
				attemptSpawnUnitAndTransferFlux(RobotType.DISRUPTER);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Just to test the costs of running navs. Correct behavior is walking to a
	 * powernode
	 */
	public void runToTestNav() {
		this.nav = new LocalAreaNav(myRC);
		MapLocation capturing = getNewTarget();
		while (true) {
			try {
				this.nav.getNextMove(capturing);
				this.myRC.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send a "ping" (here's my location) message or the location of an enemy if
	 * one is present.
	 */
	public void sendPingOrEnemyLoc() {
		try {
			Robot bestEnemy = senseBestEnemy();
			if (bestEnemy == null) {
				iSeeEnemy = false;
				pingPresence();
			} else {
				iSeeEnemy = true;
				Message message = new Message();
				message.ints = new int[] { ARCHON_ENEMY_MESSAGE };
				message.locations = new MapLocation[] { this.myRC
						.senseLocationOf(bestEnemy) };
				if (myRC.getFlux() > battlecode.common.GameConstants.BROADCAST_FIXED_COST
						+ 16 * message.getFluxCost()
						&& !this.myRC.hasBroadcasted()) {
					myRC.broadcast(message);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends a message saying this robot is present and broadcasting its
	 * location.
	 */
	@Override
	public void pingPresence() {
		try {
			Message message = new Message();
			message.ints = new int[] { ARCHON_PING_MESSAGE };
			// message.locations = new MapLocation[] { this.myRC.getLocation()
			// .add(this.myRC.getLocation().directionTo(
			// locationApproaching), 3) };
			message.locations = new MapLocation[] { this.locationApproaching };
			if (myRC.getFlux() > battlecode.common.GameConstants.BROADCAST_FIXED_COST
					+ 16 * message.getFluxCost()
					&& !this.myRC.hasBroadcasted()) {
				myRC.broadcast(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MapLocation estimateEnemyPowerCore() {
		try {
			int dist = (int) Math.sqrt(myRC.getType().sensorRadiusSquared);
			MapLocation m = this.myRC.getLocation();
			MapLocation[] neighbors = { m.add(Direction.EAST, dist),
					m.add(Direction.NORTH, dist),
					m.add(Direction.NORTH_EAST, dist),
					m.add(Direction.NORTH_WEST, dist),
					m.add(Direction.SOUTH, dist),
					m.add(Direction.SOUTH_EAST, dist),
					m.add(Direction.SOUTH_WEST, dist),
					m.add(Direction.WEST, dist) };
			MapLocation[] walls = new MapLocation[100];
			int locOpen = 0;
			for (int i = 0; i < 8; i++) {
				TerrainTile t = myRC.senseTerrainTile(neighbors[i]);
				if (t == TerrainTile.OFF_MAP) {
					walls[locOpen] = neighbors[i];
					locOpen++;
				}
			}
			Message wallMsg = new Message();
			wallMsg.locations = walls;
			while (myRC.getFlux() < battlecode.common.GameConstants.BROADCAST_FIXED_COST
					+ 16 * wallMsg.getFluxCost()) {
				this.myRC.yield();
			}
			myRC.broadcast(wallMsg);
			this.myRC.yield();
			Message[] otherWallMsgs = this.myRC.getAllMessages();
			for (Message o : otherWallMsgs) {
				if (o.locations == null) {
					continue;
				}
				for (MapLocation q : o.locations) {
					if (q != null) {
						walls[locOpen] = q;
						locOpen++;
					}
				}
			}
			double xIdeal = 0; // based on walls
			double yIdeal = 0; // based on walls
			FastHashSet<Direction> wallsSeen = new FastHashSet<Direction>(9999);
			for (MapLocation n : walls) {
				if (n != null) {
					Direction d = n.directionTo(m);
					if (!wallsSeen.search(d)) {
						xIdeal += d.dx;
						yIdeal += d.dy;
						wallsSeen.insert(d);
					}
				}
			}
			MapLocation[] capturables = myRC.senseCapturablePowerNodes();
			double capX = 0;
			double capY = 0;
			for (MapLocation c : capturables) {
				capX += c.x - m.x;
				capY += c.y - m.y;
			}
			double xEstimate = xIdeal + capX;
			double yEstimate = yIdeal + capY;

			// System.out
			// .println("" + (int) (xEstimate) + " " + (int) (yEstimate));
			Message bestGuess = new Message();
			int[] suspectedLocation = new int[] { (int) (10 * xEstimate),
					(int) (10 * yEstimate) };
			bestGuess.ints = suspectedLocation;
			while (myRC.getFlux() < battlecode.common.GameConstants.BROADCAST_FIXED_COST
					+ 16 * bestGuess.getFluxCost()) {
				this.myRC.yield();
			}
			myRC.broadcast(bestGuess);
			for (int i = 0; i < 1; i++) {
				this.myRC.yield();
			}
			Message[] otherGuessMsgs = this.myRC.getAllMessages();
			for (Message o : otherGuessMsgs) {
				if (o.ints != null) {
					suspectedLocation[0] += o.ints[0];
					suspectedLocation[1] += o.ints[1];
				} else {
					// System.out.println("Null msg");
				}
			}
			MapLocation guess = m.add(suspectedLocation[0],
					suspectedLocation[1]);
			// System.out.println("Compiled guess: " + suspectedLocation[0] +
			// ", "
			// + suspectedLocation[1]);
			return guess;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void testBottleneck() {
		MapLocation capturing = getNewTarget();
		try {
			while (!bottleneckDetected()) {
				this.nav.getNextMove(capturing);
				this.myRC.yield();
			}
			while (true) {
				this.myRC.yield();
			}
		} catch (Exception e) {

		}
	}
}
