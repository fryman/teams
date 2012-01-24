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
	
	//intended for these scorchers to sit at choke points and 
	//attack all enemies as they try to pass through
	private MapLocation friendlyMapLocationToFollow = null;

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
				if (shouldAttack() && !myRC.isAttackActive()) {
					//scorcher ignores inputs to attackSquare()
					myRC.attackSquare(myRC.getLocation(), RobotLevel.ON_GROUND);
					runAtEndOfTurn();
				}else {
					friendlyMapLocationToFollow = reacquireNearestFriendlyArchonLocation();
					if (friendlyMapLocationToFollow == null) {
						// game over...
						myRC.suicide();
					}
					this.nav.getNextMove(friendlyMapLocationToFollow);
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
		int curRound = Clock.getRoundNum();
		if (curRound != lastRoundNumSurroundingsProcessed) {
			nearbyRobots = myRC.senseNearbyGameObjects(Robot.class);
			lastRoundNumSurroundingsProcessed = curRound;
		}
		int numAllies = 0;
		int numEnemies = 0;

		try {
			if (nearbyRobots.length > 0) {	//there are enemies in range
				for (Robot r : nearbyRobots) {
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
