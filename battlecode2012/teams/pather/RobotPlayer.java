package pather;

//this is the top level
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
			new ArchonPlayer(rc).run();
		case SCOUT:
			new ScoutPlayer(rc).run();
		}
	}

}