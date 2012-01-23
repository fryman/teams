package team111;

import team111.Nav.Navigation;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ScorcherPlayer extends BasePlayer {
	private boolean in_position = false;
	private int desired_squares_from_core = 2;
	private int attempts_to_position = 0;
	private MapLocation core = myRC.sensePowerCore().getLocation();
	private int actual_squares_from_core = 0;
	private MapLocation friendlyMapLocationToFollow = null;
	private final int MAX_DEVIATION_DISTANCE_SQUARE = 100;

	// private Robot friendlyToFollow = null;
	// private MapLocation friendlyMapLocationToFollow = null;

	public ScorcherPlayer(RobotController rc) {
		super(rc);
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
		//sense power core if can then defend core else follow and attack
		if (myRC.getLocation().distanceSquaredTo(myRC.sensePowerCore().getLocation()) < 17){
			runDefendCore();
		} else {
		followAndAttack();
		}
	}

	public void runDefendCore() {
		while (true) {
			try {
				if (!in_position) {
					if (attempts_to_position < 3) {
						myRC.setIndicatorString(0, "attempts_to_position: "
								+ attempts_to_position + " actual "
								+ actual_squares_from_core + " desired "
								+ desired_squares_from_core);
						navToCoreAndAboutFace();
						attempts_to_position++;
						advanceIntoPosition();
						if (actual_squares_from_core != desired_squares_from_core) {
							navToCoreAndMoveToNextCardinal();
							in_position = false;
						} else {
							in_position = true;
							//break;
						}
						if(rangeOffMap()){
							myRC.setIndicatorString(0, "range off map");
							System.out.println("off map");
							attempts_to_position -= 1;
							in_position = false;
						}
						myRC.setIndicatorString(0,"");
					} else {
						navToCoreAndAboutFace();
					}
				} else {
					scorcherAttackAvoidArchons();
					myRC.setIndicatorString(2, "in attack mode");
					if (actual_squares_from_core != desired_squares_from_core && myRC.canMove(myRC.getDirection())) {
						attempts_to_position = 0;
					}
				}

			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

	public void navToCore() {
		myRC.setIndicatorString(0, "navigating to core");
		while (!myRC.getLocation().isAdjacentTo(core)) {
			this.nav.getNextMove(core);
			runAtEndOfTurn();
		}
	}

	public boolean rangeOffMap() {
		myRC.setIndicatorString(3, "checking range on map");
		boolean ans = false;
		int dist = ((int) Math.sqrt(myRC.getType().sensorRadiusSquared) - 1);
		TerrainTile t = myRC.senseTerrainTile(myRC.getLocation().add(myRC.getDirection(), dist));
		if (t == TerrainTile.OFF_MAP) {
			ans = true;
		}
		return ans;
	}
	
	public void navToCoreAndAboutFace() {
		try {
			navToCore();
			while (myRC.isMovementActive()) {
				runAtEndOfTurn();
			}
			myRC.setDirection(myRC.getLocation().directionTo(core).opposite());
			runAtEndOfTurn();
		} catch (Exception e) {
			System.out.println("Exception Caught");
			e.printStackTrace();
		}
	}

	public void advanceIntoPosition() {
		try {
			if (myRC.getDirection().isDiagonal()) {
				desired_squares_from_core = 1;
			} else {
				desired_squares_from_core = 2;
			}
			myRC.setIndicatorString(0, "attempting move forward "
					+ desired_squares_from_core + "squares");
			int patience = 0;
			while (actual_squares_from_core < desired_squares_from_core
					&& patience < 10) {
				if (myRC.canMove(myRC.getDirection())
						&& !myRC.isMovementActive()
						&& myRC.getFlux() > myRC.getType().moveCost) {
					myRC.moveForward();
					actual_squares_from_core++;
				}
				patience++;
				runAtEndOfTurn();
			}
			if (actual_squares_from_core == desired_squares_from_core) {
				in_position = true;
			}
		} catch (Exception e) {
			System.out.println("Exception Caught");
			e.printStackTrace();
		}
	}

	public void navToCoreAndMoveToNextCardinal() {
		try {
			navToCore();
			if (myRC.getDirection().isDiagonal()
					&& myRC.getFlux() > myRC.getType().moveCost) {
				myRC.setDirection(myRC.getDirection().rotateLeft());
			} else if (myRC.getFlux() > myRC.getType().moveCost) {
				myRC.setDirection(myRC.getDirection().rotateLeft().rotateLeft());
			}
			int patience = 0;
			while (patience < 1) {
				if (myRC.canMove(myRC.getDirection())
						&& !myRC.isMovementActive()
						&& myRC.getFlux() > myRC.getType().moveCost) {
					myRC.moveForward();
					patience++;
				}
				runAtEndOfTurn();
			}

		} catch (Exception e) {
			System.out.println("Exception Caught");
			e.printStackTrace();
		}
	}

	public void scorcherAttackAvoidArchons() {
		try {
			myRC.setIndicatorString(0, "in position and attacking");
			if (senseClosestGroundEnemy() != null
					&& !myRC.isAttackActive()
					&& !canSenseArchon()
					&& myRC.canAttackSquare(myRC.getLocation().add(
							myRC.getDirection()))) {
				myRC.setIndicatorString(0, "about to attack ");
				myRC.attackSquare(myRC.getLocation(),
						battlecode.common.RobotLevel.ON_GROUND);
			}
			runAtEndOfTurn();
		} catch (Exception e) {
			System.out.println("Exception Caught");
			e.printStackTrace();
		}
	}
	
	public void followAndAttack() {
		while (true) {
			try {
				friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
				if (friendlyMapLocationToFollow == null) {
					// game over...
					myRC.suicide();
				}
				myRC.setIndicatorString(0, "in position and attacking");
				if (senseClosestGroundEnemy() != null
						&& !myRC.isAttackActive()
						&& !canSenseArchon()
						&& myRC.canAttackSquare(myRC.getLocation().add(
								myRC.getDirection()))) {
					myRC.setIndicatorString(0, "about to attack ");
					myRC.attackSquare(myRC.getLocation(),
							battlecode.common.RobotLevel.ON_GROUND);
					runAtEndOfTurn();
				} else {
					this.nav.getNextMove(friendlyMapLocationToFollow);
					runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

}
