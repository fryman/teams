package team111;

import team111.Nav.Navigation;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class ScorcherPlayer2 extends BasePlayer {

	private Navigation nav = null;
	private MapLocation targetLoc;
	private Robot closestTar;
	private boolean set = false;
	private int moves = 2;
	private int tries = 0;
	private int timesMoved = 0;

	// private Robot friendlyToFollow = null;
	// private MapLocation friendlyMapLocationToFollow = null;

	public ScorcherPlayer2(RobotController rc) {
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
		runChokePoints();
	}

	public void runChokePoints() {
		while (true) {
			try {
				if (shouldAttack()) {
					//scorcher ignores inputs to attackSquare()
					myRC.attackSquare(myRC.getLocation(), RobotLevel.ON_GROUND);
					runAtEndOfTurn();
				}else {
					runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns true if there are good conditions for the scorcher to
	 * attack. A good condition would mean that there are enemies
	 * in range to shoot and that there are more enemies than allies
	 * in range since scorchers can do damage with friendly fire.  
	 * It also makes sure there are NO archons in range of attack.
	 * 
	 * @return Returns true if there are good conditions for the 
	 * scorcher to attack.
	 */
	public boolean shouldAttack() {
		Robot[] surroundings = myRC.senseNearbyGameObjects(Robot.class);
		int numAllies = 0;
		int numEnemies = 0;

		try {
			if (surroundings.length > 0) {	//there are enemies in range
				for (Robot r : surroundings) {
					if (r.getTeam() == myRC.getTeam()) {
						numAllies++;
					} else if (r.getTeam() != myRC.getTeam()
							&& myRC.senseRobotInfo(r).type != RobotType.SCOUT) {
						numEnemies++;
					}
				}
				if (numEnemies > numAllies && !canSenseArchon()) {		//more enemies
					return true;
				} else {		//if you will be doing more damage to yourself, do not attack
					return false;
				}
			} else {		//there is no one in range
				return false;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
			return false;
		}
	}
}
