package pather.Nav;

import pather.StaticStuff;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Navigation extends StaticStuff{
	/*
	 * Must obey contract:
	 * 	Executes one move per getNextMove call. 
	 * 	Executes move through myRC.
	 * 	One move is defined as a turn, move forward/backward,
	 * 		or a wait.
	 */
	public Navigation(){
	}
	public RobotController myRC;
	public abstract void getNextMove(MapLocation target);
}
