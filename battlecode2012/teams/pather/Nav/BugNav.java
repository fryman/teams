package pather.Nav;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BugNav extends Navigation {
	/*
	 * This class will provide the methods necessary for bug navigation. Walk
	 * toward the target until obstructed, then follow the wall until it is
	 * possible to approach the target again.
	 * 
	 * Strategy: -Build in patience (if you bump into an ally, back up once,
	 * then yield subsequent turn) -Problem occurs when multiple robots crash
	 * into a wall and then bump around in a clump, not moving productively
	 * 
	 * Solutions: -Don't send all the robots to the same place (this isn't
	 * feasible when you want to have swarm movement). -Recalculate line when
	 * you get to the end of an object (every turn?)
	 * 
	 * One move at a time is requested by each robot. Robot supplies whether it
	 * is on_wall or not. If robot doesn't know, passes in false.
	 */
	private boolean tracing = false;

	public BugNav(RobotController myRC) {
		this.myRC = myRC;
	}

	@Override
	public void getNextMove(MapLocation target) {
		/*
		 * if (dir to target obstructed) trace around wall else go to target
		 */
		try {
			if (myRC.isMovementActive()) {
				return;
			}
			Direction dir = myRC.getLocation().directionTo(target);
			if (!tracing) {
				if (myRC.canMove(dir)) {
					if (myRC.getDirection() != dir) {
						myRC.setDirection(dir);
						return;
					}
					myRC.moveForward();
					return;
				} else {
					tracing = true;
					double num = Math.random();
					if (num > 0.5) {
						myRC.setDirection(dir.rotateRight());
					} else {
						myRC.setDirection(dir.rotateLeft());
					}
				}
			} else {
				if (myRC.canMove(myRC.getLocation().directionTo(target))) {
					tracing = false;
				} else {
					myRC.moveForward();
					return;
				}
			}
		} catch (Exception e) {
			System.out.println("caught exception:");
			e.printStackTrace();
		}
	}
}
