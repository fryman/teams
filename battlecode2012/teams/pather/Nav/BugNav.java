package pather.Nav;

import battlecode.common.Direction;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.TerrainTile;

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
	private boolean justHitObstacleLastTurn = false; // this indicates if we've
														// moved off the mline
														// since hitting an
														// obstacle
	private MapLocation target;
	private Line mline = null;
	private int turnsPassedSinceLastMove = 0; // safety to prevent spinning in
												// place when an obstacle moves
												// out of the way
	private MapLocation qStart = null;
	private MapLocation qClosest = null;
	private MapLocation qObstruction = null;
	private double moveCost;
	private int turnsStuckOnWall = 0;
	private final int MAX_TURNS_STUCK_ON_WALL = 100;// TODO determine a good
													// cutoff

	public BugNav(RobotController myRC) {
		this.myRC = myRC;
		this.moveCost = this.myRC.getType().moveCost;
	}

	@Override
	public void getNextMove(MapLocation target) {
		getNextMoveBug0(target);
		return;
	}

	public void getNextMoveBug0(MapLocation target) {
		try {
			if (myRC.isMovementActive()) {
				return;
			}
			this.target = target;
			Direction ideal = myRC.getLocation().directionTo(target);
			if (ideal == Direction.OMNI || ideal == Direction.NONE) {
				return;
			}
			// if this robot does not have enough flux to move, don't try to
			// move.
			if (this.myRC.getFlux() < this.moveCost) {
				return;
			}
			if (!tracing) {
				if (myRC.canMove(ideal)) {
					if (myRC.getDirection() != ideal) {
						myRC.setDirection(ideal);
						myRC.setIndicatorString(1, "Turning ideal");
						return;
					}
					myRC.moveForward();
					myRC.setIndicatorString(1, "Moving ideal");
					return;
				} else {
					// here we deal with *what* we hit
					// by sensing and if we hit a friendly, // back up rather
					// than turn
					// sense whats in front of us
					if (!myRC.canSenseSquare(myRC.getLocation().add(ideal))) {
						myRC.setIndicatorString(2,
								"I can't see whats in front of me");
						myRC.setDirection(ideal);
						return;
					}
					GameObject obstruction = myRC.senseObjectAtLocation(myRC
							.getLocation().add(ideal), RobotLevel.ON_GROUND);
					if (obstruction != null
							&& obstruction.getTeam() == myRC.getTeam()
							&& !waited) {
						waited = true;
						return;
					}
					waited = false;
					tracing = true;
					// this choice can be made with a heuristic such as // dot
					// product of
					// new direction with direction to // target

					// "pick direction to trace"
					MapLocation currentLoc = myRC.getLocation();
					MapLocation left = currentLoc.add(myRC.getDirection()
							.rotateLeft());
					MapLocation right = currentLoc.add(myRC.getDirection()
							.rotateRight());
					int[] vectorLeft = { left.x - currentLoc.x,
							left.y - currentLoc.y };
					int[] vectorRight = { right.x - currentLoc.x,
							right.y - currentLoc.y };
					int[] vectorToGoal = { target.x - currentLoc.x,
							target.y - currentLoc.y };

					double leftCos = (vectorLeft[0] * vectorToGoal[0] + vectorLeft[1]
							* vectorToGoal[1])
							/ (Math.sqrt(vectorLeft[0] * vectorLeft[0]
									+ vectorLeft[1] * vectorLeft[1]) * Math
									.sqrt(vectorToGoal[0] * vectorToGoal[0]
											+ vectorToGoal[1] * vectorToGoal[1]));

					double rightCos = (vectorRight[0] * vectorToGoal[0] + vectorRight[1]
							* vectorToGoal[1])
							/ (Math.sqrt(vectorRight[0] * vectorRight[0]
									+ vectorRight[1] * vectorRight[1]) * Math
									.sqrt(vectorToGoal[0] * vectorToGoal[0]
											+ vectorToGoal[1] * vectorToGoal[1]));

					if (leftCos > rightCos) {
						myRC.setDirection(ideal.rotateLeft());
						turnedLeft = true;
						myRC.setIndicatorString(1,
								"Turning left to avoid obstacle");
						return;
					} else {
						myRC.setDirection(ideal.rotateRight());
						turnedLeft = false;
						myRC.setIndicatorString(1,
								"Turning right to avoid obstacle");
						return;
					}
				}
			} else { // tracing
				turnsStuckOnWall ++;
				if (clearOfObstacleBug0(target)) { //
					// robot clear of obstacle // *** this is definitely not a
					// sufficient //
					// "clear of obstacle" condition
					tracing = false;
					myRC.setIndicatorString(1, "Clear of obstacle");
					turnsStuckOnWall = 0;
					return;
				} else { //
					if (turnsStuckOnWall >= MAX_TURNS_STUCK_ON_WALL){
						tracing = false;
						myRC.setIndicatorString(1, "Clear of obstacle");
						turnsStuckOnWall = 0;
						return;
					}
					// follow the wall / obstacle boundary
					if (turnedLeft) {
						if (myRC.canMove(myRC.getDirection().rotateRight())) {
							myRC.setDirection(myRC.getDirection().rotateRight());
							myRC.setIndicatorString(1,
									"Rotating right to follow obstacle border");
							return;
						}
					} else {
						if (myRC.canMove(myRC.getDirection().rotateLeft())) {
							myRC.setDirection(myRC.getDirection().rotateLeft());
							myRC.setIndicatorString(1,
									"Rotating Left to follow obstacle border");
							return;
						}
					}
					if (myRC.canMove(myRC.getDirection())) {
						myRC.moveForward();
						myRC.setIndicatorString(1, "Walking around obstacle");
						return;
					} else { // we have problems
						if (turnedLeft) { //
							myRC.setDirection(myRC.getDirection().rotateLeft());
							myRC.setIndicatorString(1,
									"Turning more left to avoid obstacle");
							return;
						} else {
							myRC.setDirection(myRC.getDirection().rotateRight());
							myRC.setIndicatorString(1,
									"Turning more right to avoid obstacle");
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

	public boolean clearOfObstacleBug0(MapLocation target) {
		return myRC.getDirection().equals(
				myRC.getLocation().directionTo(target));
	}

	public boolean clearOfObstacleBug2(MapLocation target) {
		// Bug2 model (using mline)
		MapLocation currentLoc = myRC.getLocation();
		return this.mline.onLine(new int[] { currentLoc.x, currentLoc.y });
	}

	public void setTargetBug2(MapLocation target) {
		// used to set the M-line for Bug2 navigation
		MapLocation start = myRC.getLocation();
		this.target = target;
		int[] init = { start.x, start.y };
		int[] end = { target.x, target.y };
		this.mline = new Line(init, end);
		this.mline.setDirection(start.directionTo(target));
	}

	public void getNextMoveBug2(MapLocation target) {
		try {
			if (myRC.isMovementActive()) {
				return;
			}
			this.myRC.setIndicatorString(1, Boolean.toString(tracing));
			if (!target.equals(this.target) || this.mline == null) {
				setTargetBug2(target);
			}
			if (this.myRC.getLocation().equals(this.target)) {
				return;
			}
			if (!tracing) {
				// this means we're on the mline
				// if we are on the mline, then ideal direction should be equal
				// to the direction of the mline
				Direction ideal = this.myRC.getLocation().directionTo(target);
				if (myRC.canMove(ideal)) {
					// moving safely along mline
					if (myRC.getDirection().equals(ideal)) {
						this.myRC.moveForward();
						this.myRC.setIndicatorString(2, "moving ideal");
					} else {
						this.myRC.setDirection(ideal);
						this.myRC.setIndicatorString(2, "turning ideal");
					}
				} else {
					// obstacle acquired
					if (waited) {
						waited = false;
						tracing = true;
						// turn left!
						this.myRC.setDirection(this.myRC.getDirection()
								.rotateLeft());
						justHitObstacleLastTurn = true;
					} else {
						waited = true;
						return;
					}
				}
			} else {
				this.myRC.setIndicatorString(0,
						Boolean.toString(justHitObstacleLastTurn));
				// following the side of an obstacle
				MapLocation current = myRC.getLocation();
				if (!justHitObstacleLastTurn) {
					// check if we're back on the mline
					if (this.mline.onLine(new int[] { current.x, current.y })) {
						tracing = false;
						return;
					}
				}
				// check if can turn right to follow obstacle. if not, walk
				// around it.
				if (turnsPassedSinceLastMove == 8
						&& this.myRC.canMove(this.myRC.getDirection())) {
					this.myRC.moveForward();
					justHitObstacleLastTurn = false;
					this.myRC.setIndicatorString(2, "noted: spun in place");
					turnsPassedSinceLastMove = 0;
					tracing = false;
					return;
				}
				if (turnsPassedSinceLastMove < 8
						&& this.myRC.canMove(this.myRC.getDirection()
								.rotateRight())) {
					this.myRC.setDirection(this.myRC.getDirection()
							.rotateRight());
					this.myRC.setIndicatorString(2,
							"turning right to follow obstacle border");
					turnsPassedSinceLastMove++;
					return;
				} else if (this.myRC.canMove(this.myRC.getDirection())) {
					this.myRC.moveForward();
					justHitObstacleLastTurn = false;
					this.myRC.setIndicatorString(2,
							"moving along the outside of an obstacle");
					turnsPassedSinceLastMove = 0;
					return;
				} else {
					// obviously the obstruction is concave on our side
					this.myRC.setDirection(this.myRC.getDirection()
							.rotateLeft());
					this.myRC.setIndicatorString(2,
							"obstacle is convex, turning left");
				}
			}
		} catch (Exception e) {
			System.out.println("Caught Exception");
			e.printStackTrace();
		}
	}

	public void getNextMoveBug1(MapLocation target) {
		try {
			// TODO finish getNextMoveBug1 ? Maybe?
			if (myRC.isMovementActive()) {
				return;
			}
			if (qStart == null) {
				this.qStart = this.myRC.getLocation();
			}
			if (!target.equals(this.target)) {
				this.target = target;
			}
			if (this.myRC.getLocation().equals(this.target)) {
				return;
			}
			Direction ideal = this.myRC.getLocation().directionTo(target);
			if (myRC.canMove(ideal)) {
				if (myRC.getDirection().equals(ideal)) {
					myRC.moveForward();
				} else {
					myRC.setDirection(ideal);
				}
			}
		} catch (Exception e) {
			System.out.println("Caught Exception");
			e.printStackTrace();
		}
	}
}
