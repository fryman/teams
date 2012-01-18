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
		while (true) {
			try {
				MapLocation core = myRC.sensePowerCore().getLocation();
				if (set == false) {
					while (!myRC.getLocation().isAdjacentTo(core)) {
						this.nav.getNextMove(core);
						runAtEndOfTurn();
					}
					myRC.setIndicatorString(0, "at powercore");
					while (myRC.isMovementActive()) {
						runAtEndOfTurn();
					}
					myRC.setDirection(myRC.getLocation().directionTo(core)
							.opposite());
					runAtEndOfTurn();
					int countMove = 0;
					int count = 0;
					while (countMove < 2 && count < 20) {
						if (myRC.canMove(myRC.getDirection())
								&& !myRC.isMovementActive()) {
							myRC.moveForward();
							countMove++;
						}
						runAtEndOfTurn();
						count++;
					}
					if (countMove != 2) {
							while (!myRC.getLocation().isAdjacentTo(core)) {
								this.nav.getNextMove(core);
								runAtEndOfTurn();
							}
						while (myRC.isMovementActive()) {
							runAtEndOfTurn();
						}
						if(myRC.getDirection().isDiagonal()){
							myRC.setDirection(myRC.getDirection().rotateLeft());
						}
						else{
							myRC.setDirection(myRC.getDirection().rotateLeft().rotateLeft());
						}
						int count3 = 0;
						while (count3 < 1) {
							if (myRC.canMove(myRC.getDirection())
									&& !myRC.isMovementActive()) {
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
					myRC.setIndicatorString(0, "should be two away facing out");
					if (senseClosestGroundEnemy() != null
							&& !myRC.isAttackActive()
							&& !canSenseArchon()) {
						myRC.setIndicatorString(0, "about to attack");
						myRC.attackSquare(myRC.getLocation(),
								battlecode.common.RobotLevel.ON_GROUND);
						runAtEndOfTurn();
					} else
						runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

}