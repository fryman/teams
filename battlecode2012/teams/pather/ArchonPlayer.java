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
	private Navigation nav = null;

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

				getNewTarget();

				while (Clock.getRoundNum() < 200) {
					spawnSoldierAndTransferFlux();
					runAtEndOfTurn();
				}

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
		locsToBuild = new ArrayList<MapLocation>(Arrays.asList(powerNodes));
		powerNodesOwned = myRC.senseAlliedPowerNodes();
	}

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

	public void buildTower(MapLocation target) {
		try {
			if (myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) != null
					&& myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND)
							.getTeam() == myRC.getTeam()) {
				getNewTarget();
				return;
			}
			if (myRC.getFlux() >= RobotType.TOWER.spawnCost) {
				while (myRC.isMovementActive()) {
					runAtEndOfTurn();
				}
				myRC.spawn(RobotType.TOWER);
				runAtEndOfTurn();
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
		try {
			if (myRC.getFlux() > RobotType.SCOUT.spawnCost
					&& myRC.senseObjectAtLocation(
							myRC.getLocation().add(myRC.getDirection()),
							RobotLevel.IN_AIR) == null) {
				myRC.spawn(RobotType.SCOUT);
				runAtEndOfTurn();
				while ((RobotType.SCOUT.maxFlux) > myRC.getFlux()) {
					super.runAtEndOfTurn();
				}  
				myRC.transferFlux(myRC.getLocation().add(myRC.getDirection()),
						RobotLevel.IN_AIR, (RobotType.SCOUT.maxFlux));
			}
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
	}

	/**
	 * Spawns a soldier and transfers flux to it. Causes this archon to wait
	 * (yielding) until it has enough flux to create a soldier, and then waits
	 * again until it has enough flux to give to the soldier.
	 */
	public void spawnSoldierAndTransferFlux() {
		try {
			if (myRC.getFlux() > RobotType.SOLDIER.spawnCost
					&& myRC.senseObjectAtLocation(
							myRC.getLocation().add(myRC.getDirection()),
							RobotLevel.ON_GROUND) == null) {
				myRC.spawn(RobotType.SOLDIER);
				runAtEndOfTurn();
				while ((RobotType.SOLDIER.maxFlux / 2) > myRC.getFlux()) {
					super.runAtEndOfTurn();
				}
				myRC.transferFlux(myRC.getLocation().add(myRC.getDirection()),
						RobotLevel.ON_GROUND, (RobotType.SOLDIER.maxFlux / 2));
			}
		} catch (GameActionException e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
	}
}
