package pather.Nav;

import battlecode.common.Direction;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;

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
	private boolean turnedLeft = false;
	private boolean waited = false;
	public BugNav(RobotController myRC) {
		this.myRC = myRC;
	}

	@Override
	public void getNextMove(MapLocation target) {
		/*
		 * if (dir to target obstructed) trace around wall else go to target
		 */
		/*
		 * if (myRC.isMovementActive()) { return; } try { Direction ideal =
		 * myRC.getLocation().directionTo(target); if (myRC.canMove(ideal)){ //
		 * if facing properly, go forward if
		 * (myRC.getDirection().equals(ideal)){ myRC.moveForward(); return; }
		 * else { // else turn move forward next round myRC.setDirection(ideal);
		 * return; } } else { if (myRC.canMove(myRC.getDirection())){
		 * myRC.moveForward(); return; } else { double num = Math.random(); //
		 * this choice can be made with a heuristic such as // dot product of
		 * new direction with direction to // target if (num > .5) {
		 * myRC.setDirection(myRC.getDirection().rotateRight()); return; } else
		 * { myRC.setDirection(myRC.getDirection().rotateLeft()); return; } } }
		 * } catch (Exception e){ System.out.println("Exception caught");
		 * e.printStackTrace(); }
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
						myRC.setIndicatorString(1, "Turning ideal");
						return;
					}
					myRC.moveForward();
					myRC.setIndicatorString(1, "Moving ideal");
					return;
				} else {
					// here we deal with *what* we hit
					// by sensing and if we hit a friendly,
					// back up rather than turn
					// sense whats in front of us
					GameObject obstruction = myRC.senseObjectAtLocation(myRC.getLocation().add(dir), RobotLevel.ON_GROUND);
					if (obstruction != null && obstruction.getTeam() == myRC.getTeam() && !waited){
						waited = true;
						return;
					}
					waited = false;
					tracing = true;
					double num = Math.random();
					// this choice can be made with a heuristic such as
					// dot product of new direction with direction to
					// target
					
					// "pick direction to trace"
					if (num > 2) {
						myRC.setDirection(dir.rotateRight());
						turnedLeft = false;
						myRC.setIndicatorString(1, "Turning right to avoid obstacle");
						return;
					} else {
						myRC.setDirection(dir.rotateLeft());
						turnedLeft = true;
						myRC.setIndicatorString(1, "Turning left to avoid obstacle");
						return;
					}
				}
			} else {
				// tracing
				if (clearOfObstacle(target)) {
					// robot clear of obstacle
					// *** this is definitely not a sufficient "clear of obstacle" condition 
					tracing = false;
					myRC.setIndicatorString(1, "Clear of obstacle");
					return;
				} else {
					// follow the wall / obstacle boundary
					// it is ALWAYS on our right side given the condition above
					if (myRC.canMove(myRC.getDirection().rotateRight())){
						myRC.setDirection(myRC.getDirection().rotateRight());
						myRC.setIndicatorString(1, "Rotating right to follow obstacle border");
						return;
					}
					if (myRC.canMove(myRC.getDirection())) {
						myRC.moveForward();
						myRC.setIndicatorString(1, "Walking around obstacle");
						return;
					} else {
						// we have problems
						if (turnedLeft) {
							// System.out.println("Turning left!");
							myRC.setDirection(myRC.getDirection().rotateLeft());
							myRC.setIndicatorString(1, "Turning more left to avoid obstacle");
							return;
						} else {
							myRC.setDirection(myRC.getDirection().rotateRight());
							myRC.setIndicatorString(1, "Turning more right to avoid obstacle");
							return;
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("caught exception:");
			e.printStackTrace();
		}
	}
	public boolean clearOfObstacle(MapLocation target){
		return myRC.getDirection().equals(myRC.getLocation().directionTo(target));
		//return myRC.canMove(myRC.getLocation().directionTo(target));
	}
}
