package team111;


import team111.Nav.BugNav;
import team111.Nav.LocalAreaNav;
import team111.Nav.Navigation;
import battlecode.common.Clock;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class DisrupterPlayer extends BasePlayer {
	
	public DisrupterPlayer(RobotController rc) {
		super(rc);
		this.nav = new LocalAreaNav(rc);
	}
	
	public void run() {
		runAtEndOfTurn();
	}

}
