package team111;

//this is the top level
import battlecode.common.*;

public class RobotPlayer {

	// public static RobotController myRC;
	// public static int alliedBots = 0;
	private int count = 0;
	
	public static void run(RobotController rc) {
		StaticStuff.init(rc);// defines myRC and base location for later use
		new RobotPlayer().nonStaticRun(rc);
	}
	public void nonStaticRun(RobotController rc){
		switch (rc.getType()) {
		case ARCHON:
			new ArchonPlayer(rc).run();
//			if (count < (battlecode.common.GameConstants.NUMBER_OF_ARCHONS-2)){
//				new ArchonPlayer(rc).run();
//				count++;
//				System.out.println("count "+count);
//			} else{
//				System.out.println("GOT HERE. count "+count);
//				new ArchonPlayer2(rc).run();
//			}
			//new ArchonPlayer(rc).runToTestDijkstraNav();
		case SCOUT:
			new ScoutPlayer(rc).run();
		case SOLDIER:
			new SoldierPlayer(rc).run();
		case SCORCHER:
			new ScorcherPlayer(rc).run();
		case DISRUPTER:
			new DisrupterPlayer(rc).run();
		}
	}

}
