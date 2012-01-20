package team111;


import team111.Nav.BugNav;
import team111.Nav.Navigation;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
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

	public void runDefendCore() {
		while (true) {
			try {
				
			} catch (Exception e) {
				System.out.println("Exception Caught");
				e.printStackTrace();
			}
		}
	}

}
