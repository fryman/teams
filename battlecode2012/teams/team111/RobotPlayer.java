package team111;

//this is the top level
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {

	// public static RobotController myRC;
	// public static int alliedBots = 0;
	
	public static void run(RobotController rc) {
		StaticStuff.init(rc);// defines myRC and base location for later use
		new RobotPlayer().nonStaticRun(rc);
	}
	public void nonStaticRun(RobotController rc){
		switch (rc.getType()) {
		case ARCHON:
			//new ArchonPlayer(rc).runArchonRush();
			new ArchonPlayer(rc).run();
			break;
		case SCOUT:
			new ScoutPlayer(rc).run();
			break;
		case SOLDIER:
			new SoldierPlayer(rc).run();
			break;
		case SCORCHER:
			new ScorcherPlayer(rc).run();
			break;
		case DISRUPTER:
			new DisrupterPlayer(rc).run();
		}
	}

}
