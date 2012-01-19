package pather;

import pather.Nav.BugNav;

import pather.Nav.Navigation;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ScorcherPlayer extends BasePlayer {

	private Navigation nav = null;
	private MapLocation targetLoc;
	private Robot closestTar;
	private boolean set = false;
	private int moves = 2;
	private int tries = 0;
	private int timesMoved = 0;

	// private Robot friendlyToFollow = null;
	// private MapLocation friendlyMapLocationToFollow = null;

	public ScorcherPlayer(RobotController rc) {
		super(rc);
		this.nav = new BugNav(rc);
	}

	@Override
	public void runAtEndOfTurn() {
		try {
			broadcastMessage();
			myRC.yield();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		runDefendCore();
	}

	// archon - if can sense power core and not two scorchers - build
	// scorchers??

	public void runDefendCore() {
		// fix - if myRC.getFlux()<move cost - to check if can move
		// fix - navigation - after coming in
		while (true) {
			try {
				MapLocation core = myRC.sensePowerCore().getLocation();
				if (set == false) {
					myRC.setIndicatorString(2, "tries: "+tries);
					if (tries < 3) {
						while (!myRC.getLocation().isAdjacentTo(core)) {
							this.nav.getNextMove(core);
							runAtEndOfTurn();
						}
						myRC.setIndicatorString(0, "at powercore");
						myRC.setIndicatorString(1, "");
						myRC.setIndicatorString(2, "");
						while (myRC.isMovementActive()) {
							runAtEndOfTurn();
						}
						myRC.setDirection(myRC.getLocation().directionTo(core)
								.opposite());
						runAtEndOfTurn();
						if (myRC.getDirection().isDiagonal()) {
							moves = 1;
						} else {
							moves = 2;
						}
						tries++;
						myRC.setIndicatorString(1, "moves " + moves);
						int countMove = 0;
						int count = 0;
						while (countMove < moves && count < 10) {
							if (myRC.canMove(myRC.getDirection())
									&& !myRC.isMovementActive()
									&& myRC.getFlux() > myRC.getType().moveCost) {
								myRC.moveForward();
								countMove++;
							}
							runAtEndOfTurn();
							count++;
						}
						if (countMove != moves) {
							while (!myRC.getLocation().isAdjacentTo(core)) {
								this.nav.getNextMove(core);
								runAtEndOfTurn();
							}
							while (myRC.isMovementActive()) {
								runAtEndOfTurn();
							}
							if (myRC.getDirection().isDiagonal()
									&& myRC.getFlux() > myRC.getType().moveCost) {
								myRC.setDirection(myRC.getDirection()
										.rotateLeft());
							} else if (myRC.getFlux() > myRC.getType().moveCost) {
								myRC.setDirection(myRC.getDirection()
										.rotateLeft().rotateLeft());
							}
							int count3 = 0;
							while (count3 < 1) {
								if (myRC.canMove(myRC.getDirection())
										&& !myRC.isMovementActive()
										&& myRC.getFlux() > myRC.getType().moveCost) {
									myRC.moveForward();
									count3++;
								}
								runAtEndOfTurn();
							}
							set = false;
						} else {
							set = true;
						}
					} else {
						// stop facing out
						while (!myRC.getLocation().isAdjacentTo(core)) {
							this.nav.getNextMove(core);
							runAtEndOfTurn();
						}
						myRC.setIndicatorString(0, "at powercore");
						myRC.setIndicatorString(1, "");
						myRC.setIndicatorString(2, "");
						while (myRC.isMovementActive()) {
							runAtEndOfTurn();
						}
						myRC.setDirection(myRC.getLocation().directionTo(core)
								.opposite());
						runAtEndOfTurn();
						if (myRC.getDirection().isDiagonal()) {
							moves = 1;
						} else {
							moves = 2;
						}
					}
				} else {
					if (set == true) {
						myRC.setIndicatorString(0, "should be " + moves
								+ " away facing out");
						if (senseClosestGroundEnemy() != null
								&& !myRC.isAttackActive()
								&& !canSenseArchon()
								&& myRC.canAttackSquare(myRC.getLocation().add(
										myRC.getDirection()))) {
							myRC.setIndicatorString(0, "about to attack ");
							myRC.attackSquare(myRC.getLocation(),
									battlecode.common.RobotLevel.ON_GROUND);
							runAtEndOfTurn();
						} else
							runAtEndOfTurn();
					} else {
						// try to set up then shoot
						if (timesMoved >= moves) {
							set = true;
							runAtEndOfTurn();
						} else {
							if (myRC.canMove(myRC.getDirection())
									&& !myRC.isMovementActive()
									&& myRC.getFlux() > myRC.getType().moveCost) {
								myRC.moveForward();
								timesMoved++;
							}
							runAtEndOfTurn();
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

}
