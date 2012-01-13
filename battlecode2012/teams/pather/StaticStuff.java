package pather;

import battlecode.common.*;

import patherNav.BugNav;
import patherNav.NavGeneral;

public abstract class StaticStuff {
	public static RobotController myRC;
	public static MapLocation base;
	public static NavGeneral myNav;
	
	public static void init(RobotController rc){
		myRC = rc;
		base = myRC.sensePowerCore().getLocation();
		myNav = new BugNav();
	}
}