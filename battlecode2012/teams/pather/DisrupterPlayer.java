package pather;

import pather.Nav.BugNav;

import pather.Nav.Navigation;
import battlecode.common.Clock;
import battlecode.common.RobotInfo;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class DisrupterPlayer extends BasePlayer {
	
	public DisrupterPlayer(RobotController rc) {
		super(rc);
		this.nav = new BugNav(rc);
	}
	
	public void run() {
		runAtEndOfTurn();
	}

}
