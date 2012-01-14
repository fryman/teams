package pather;

import battlecode.common.*;

import java.util.*;

public class ArchonPlayer extends BasePlayer {

	private PowerNode core = null;
	private Random r = new Random();
	private MapLocation targetLoc = null; // the location at which the tower
											// should be built
	private MapLocation[] locsToBuild = myRC.senseCapturablePowerNodes();
	private PowerNode[] powerNodesOwned = myRC.senseAlliedPowerNodes();

	public ArchonPlayer(RobotController rc) {
		super(rc);
	}

	public void run() {
		while (true) {
			try {
				while (myRC.isMovementActive()) {
					myRC.yield();
				}
				if (core == null) {
					core = myRC.sensePowerCore();
				}
				getNewTarget();

				while (targetLoc != null
						&& !myRC.getLocation().isAdjacentTo(targetLoc)) {
					goCloser(targetLoc);
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
						myRC.yield();
					}
					myRC.setDirection(myRC.getLocation().directionTo(targetLoc));
					myRC.yield();
				}
				// Now we can build a fucking tower
				boolean enemyTower = enemyTowerPresent(targetLoc);

				if (enemyTower == true) {
					myRC.setIndicatorString(1, "attempting destroy at: "
							+ targetLoc.toString());
					destroyTower(targetLoc);
				} else {
					myRC.setIndicatorString(1, "attempting build at: "
							+ targetLoc.toString());
					buildTower(targetLoc);
				}
				myRC.yield();

			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}

	public void goCloser(MapLocation target) {
		try {
			while (myRC.isMovementActive()) {
				myRC.yield();
			}
			Direction targetDir = myRC.getLocation().directionTo(target);

			if (myRC.getDirection() != targetDir) {
				myRC.setDirection(targetDir);
				myRC.yield();
			}
			if (myRC.canMove(targetDir)) {
				myRC.moveForward();
			} else {
				if (r.nextDouble() < 2) {
					myRC.setDirection(myRC.getDirection().rotateLeft());
				} else {
					myRC.setDirection(myRC.getDirection().rotateRight());
				}
				myRC.yield();
				if (myRC.canMove(myRC.getDirection())) {
					myRC.moveForward();
					myRC.yield();
				}
			}
		} catch (Exception e) {
			System.out.println("caught exception:");
			e.printStackTrace();
		}
	}

	public void updateUnownedNodes() {
		locsToBuild = myRC.senseCapturablePowerNodes();
		powerNodesOwned = myRC.senseAlliedPowerNodes();
	}

	public void getNewTarget() {
		updateUnownedNodes();
		if (locsToBuild.length != 0) {
			targetLoc = locsToBuild[0];
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
				myRC.spawn(RobotType.TOWER);
				myRC.yield();
				getNewTarget();
				myRC.setIndicatorString(1, "null");
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

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
}