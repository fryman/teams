package team111;

import battlecode.common.*;


public abstract class StaticStuff {
	public static RobotController myRC;
	public static MapLocation base;
	
	public static void init(RobotController rc){
		myRC = rc;
		base = myRC.sensePowerCore().getLocation();
	}
}