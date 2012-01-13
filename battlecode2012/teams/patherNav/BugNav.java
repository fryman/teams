package patherNav;

import battlecode.common.*;

import pather.StaticStuff;

public class BugNav extends NavGeneral{
	static int leet = 1337;
	public static void BugTo(){
		myRC.setIndicatorString(2,"bug to "+leet);
	}
	public void BugNav(){
		myRC.setIndicatorString(2,"bug to "+(leet+1));
	}
}