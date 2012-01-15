package pather;

import battlecode.common.RobotController;

public class ScoutPlayer extends BasePlayer {

	public ScoutPlayer(RobotController rc) {
		super(rc);
	}

	//go explore, follow spawned archon, transfer energon to archon
	
	public void run() {
		while (true) {
			try {
				while (myRC.isMovementActive()) {
					myRC.yield();
				}
				if (myRC.canMove(myRC.getDirection())) {
					myRC.moveForward();
				} else {
					myRC.setDirection(myRC.getDirection().rotateLeft());
				}
				myRC.yield();
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	} 

}