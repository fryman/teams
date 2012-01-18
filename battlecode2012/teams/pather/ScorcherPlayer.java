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
	//private Robot friendlyToFollow = null;
	//private MapLocation friendlyMapLocationToFollow = null;

	public ScorcherPlayer(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
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
	
	//archon - if can sense power core and not two scorchers - build scorchers??
	
	public void runDefendCore() {
		while (true) {
			try {
				MapLocation core = myRC.sensePowerCore().getLocation();
				while(!myRC.getLocation().isAdjacentTo(core)){
					this.nav.getNextMove(core);
					runAtEndOfTurn();
				}
				myRC.setIndicatorString(0,"at powercore");
				myRC.setDirection(myRC.getLocation().directionTo(core).opposite());
				runAtEndOfTurn();
				myRC.moveForward();
				runAtEndOfTurn();
				myRC.moveForward();
				runAtEndOfTurn();
				myRC.setIndicatorString(0,"should be two away facing out");
				if(senseClosestEnemy() != null){
					myRC.setIndicatorString(0,"about to attack");
					myRC.attackSquare(myRC.getLocation(), battlecode.common.RobotLevel.ON_GROUND);
					runAtEndOfTurn();
					}
				else runAtEndOfTurn();
//				if (myRC.getLocation().directionTo(core)== battlecode.common.Direction.NORTH_EAST ||
//					myRC.getLocation().directionTo(core)== battlecode.common.Direction.NORTH_WEST ||
//					myRC.getLocation().directionTo(core)== battlecode.common.Direction.SOUTH_WEST ||
//					myRC.getLocation().directionTo(core)== battlecode.common.Direction.SOUTH_EAST){
//					myRC.setDirection(myRC.getLocation().directionTo(core));
//					
//				}
				//face power core, back up until don't sense scorcher
				//have them orthogonal to powercore
				
				//if see other scorcher - move away one - check again
				//if in front of power node turn opposite
				//if sense enemy and no friendly fire
				
//				Robot[] robots = myRC.senseNearbyGameObjects(battlecode.common.Robot.class);
//				if( robots !=null){
//					for(int i=0; i< robots.length; i++){
//						
//					}
//				}
//				if(myRC.getLocation().isAdjacentTo(myRC.sensePowerCore().getLocation())){
//					
//				}
//				
//				this.nav.getNextMove();
//				//myRC.setIndicatorString(0, "following a friendly");
//				runAtEndOfTurn();
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}
	
}