package team111;

import battlecode.common.*;

import java.util.*;

import team111.Nav.*;

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

	public ArchonPlayer(RobotController rc) {
		super(rc);

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
		broadcastMessage();
		this.findWeakFriendsAndTransferFlux();
	}

	public void run() {
		while (true) {
			try {
				while (myRC.isMovementActive()) {
					runAtEndOfTurn();
				}
				// This causes the archons to spread out quickly, and limits
				// spreading to 200 rounds. Realistically spreading out is
				// limited to 20 rounds in spreadOutFromOtherArchons()
				while (Clock.getRoundNum() < 50
						&& !spreadOutFromOtherArchons()) {
					while (myRC.isMovementActive()) {
						runAtEndOfTurn();
					}
				}
				if (this.nav.getClass() != DijkstraNav.class) {
					this.nav = new DijkstraNav(myRC);
				}
				// spawnScorcherAndTransferFlux();
				MapLocation capturing = getNewTarget();
				myRC.setIndicatorString(0, "capturing: " + capturing + " "
						+ Clock.getRoundNum());
				
				/*if (beingAttacked()) {
				if (myRC.canMove(myRC.getDirection().opposite())) {
					myRC.setDirection(myRC.getDirection().opposite());
					}
				}*/
				
				goToPowerNodeForBuild(capturing);
				buildOrDestroyTower(capturing);
				runAtEndOfTurn();

			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
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
								RobotLevel.ON_GROUND) != null) {
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
					&& !myRC.getLocation().isAdjacentTo(locationToCapture)) {
				this.nav.getNextMove(locationToCapture);
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
	 * Finds closest PowerNode that we can build on. Sets targetLoc to this
	 * node's location.
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
			MapLocation closest = capturablePowerNodes[0];
			for (int i = 1; i < capturablePowerNodes.length; i++) {
				if (compareMapLocationDistance(capturablePowerNodes[i], closest)) {
					closest = capturablePowerNodes[i];
				}
			}
			targetLoc = closest; // to conform to method signature
			if (Clock.getRoundNum() < 200) {
				closest = capturablePowerNodes[(int)Math.random()*capturablePowerNodes.length];
			}
			return closest;
			/*
			 * The commented code here is archaic. It is saved for safety.
			 * 
			 * for (MapLocation m : capturablePowerNodes) { // if
			 * (!enemyTowerLocs.contains(m)) { targetLoc = m; return m; // } }
			 * // does not handle case where all nodes are enemy towers
			 */
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

	public void spawnScorcherAndTransferFlux() {
		while (true) {
			try {
				myRC.setIndicatorString(0, "creating scorcher");
				while (myRC.isMovementActive()) {
					// runAtEndOfTurn();
					super.runAtEndOfTurn();
				}
				MapLocation potentialLocation = myRC.getLocation().add(
						myRC.getDirection());
				if (this.myRC.senseTerrainTile(potentialLocation) != TerrainTile.LAND) {
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
					myRC.setIndicatorString(2, "just spawned scorcher: ");
					// runAtEndOfTurn();
					super.runAtEndOfTurn();
					Robot recentScorcher = (Robot) myRC.senseObjectAtLocation(
							potentialLocation, RobotLevel.ON_GROUND);
					myRC.setIndicatorString(2, "recent scorcher: "
							+ recentScorcher);
					if (recentScorcher == null) {
						// runAtEndOfTurn();
						super.runAtEndOfTurn();
						myRC.setIndicatorString(2, "recent scorcher null");
						continue;
					}
					// runAtEndOfTurn();
					super.runAtEndOfTurn();
					while ((RobotType.SCORCHER.maxFlux / 2) > myRC.getFlux()
							&& myRC.canSenseObject(recentScorcher)) {
						super.runAtEndOfTurn();
					}
					if (myRC.canSenseObject(recentScorcher)
							&& acceptableFluxTransferLocation(myRC
									.senseLocationOf(recentScorcher))
							&& myRC.senseRobotInfo(recentScorcher).flux < RobotType.SOLDIER.maxFlux / 2) {
						myRC.transferFlux(myRC.senseLocationOf(recentScorcher),
								RobotLevel.ON_GROUND,
								RobotType.SCORCHER.maxFlux / 2);
					}
					return;
				}
				myRC.setIndicatorString(1, "did not attempt to create scorcher");
				myRC.setIndicatorString(
						2,
						Boolean.toString(myRC.getFlux() > RobotType.SCORCHER.spawnCost));
				// runAtEndOfTurn();
				super.runAtEndOfTurn();
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
			if (roundsUsedToMoveAway >= 50) {
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
										RobotLevel.ON_GROUND).getID() + " "
								+ Clock.getRoundNum());
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

	/**
	 * ATTEMPTS to spawns a scout and transfers flux to it. Causes this archon
	 * to wait (yielding) until it has enough flux to transfer to a scout. Does
	 * NOT cause the archon to wait until it has enough flux to spawn a scout.
	 * 
	 * Currently the amount of flux transferred to the scout is the *full*
	 * amount that the scout is allowed to carry.
	 */
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
			if (myRC.getFlux() > RobotType.SCOUT.spawnCost
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
	 */
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
	 * @TODO Find a way to sense objects out of range.
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
			boolean scoutPresent = false;
			int soldierPresent = 0;
			for (Robot n : neighbors) {
				if (n.getTeam() == this.myRC.getTeam()) {
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SCOUT)) {
						scoutPresent = true;
					}
					if (myRC.senseRobotInfo(n).type.equals(RobotType.SOLDIER)) {
						soldierPresent++;
					}
				}
			}
			// if cannot see scout, spawn one.
			if (!scoutPresent) {
				attemptSpawnScoutAndTransferFlux();
			}
			// if cannot see soldier, spawn one.
			if (soldierPresent < 3) {
				attemptSpawnSoldierAndTransferFlux();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean beingAttacked() {
		if (myRC.getEnergon() < prevEnergon) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Just to test the costs of running dijkstra.
	 */
	public void runToTestDijkstraNav() {
		this.nav = new DijkstraNav(myRC);
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
}
