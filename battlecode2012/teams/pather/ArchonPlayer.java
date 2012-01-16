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

	public void run() {
		while (true) {
			try {
				while (myRC.isMovementActive()) {
					runOncePerTurn();
					myRC.yield();
				}
				if (core == null) {
					core = myRC.sensePowerCore();
				}

				getNewTarget();

				while (Clock.getRoundNum() < 200) {
					spawnScoutAndTransferFlux();
					runOncePerTurn();
					myRC.yield();
				}

				while (targetLoc != null
						&& !myRC.getLocation().isAdjacentTo(targetLoc)) {
					this.nav.getNextMove(targetLoc);
					runOncePerTurn();
					myRC.yield();
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
						runOncePerTurn();
						myRC.yield();
					}
					myRC.setDirection(myRC.getLocation().directionTo(targetLoc));
					runOncePerTurn();
					myRC.yield();
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
				runOncePerTurn();
				myRC.yield();

			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}
	
	public void findWeakFriendsAndTransferFlux(){
		
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

	public boolean enemyTowerPresent(MapLocation target) {
		try {
			if (myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) != null
					&& myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND)
							.getTeam() != myRC.getTeam()) {
				return true;
			} else {
				return false;
			}

		} catch (GameActionException e) {
			e.printStackTrace();
			return false;
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
					myRC.yield();
				}
				myRC.spawn(RobotType.TOWER);
				myRC.yield();
				getNewTarget();
				myRC.setIndicatorString(1, "null");

			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	// archons can't actually attack ...
	public void destroyTower(MapLocation target) {
		try {
			if (myRC.canAttackSquare(target)) {
				while (myRC.senseObjectAtLocation(target, RobotLevel.ON_GROUND) != null
						&& myRC.senseObjectAtLocation(target,
								RobotLevel.ON_GROUND).getTeam() != myRC
								.getTeam()) {
					myRC.attackSquare(target, RobotLevel.ON_GROUND);
					myRC.yield();
				}
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
				runOncePerTurn();
				myRC.yield();
				while ((RobotType.SCOUT.maxFlux) > myRC.getFlux()) {
					runOncePerTurn();
					myRC.yield();
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
				myRC.yield();
				while ((RobotType.SOLDIER.maxFlux / 2) > myRC.getFlux()) {
					myRC.yield();
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