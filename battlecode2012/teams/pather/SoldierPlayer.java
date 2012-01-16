package pather;

import java.util.Random;

import pather.Nav.BugNav;
import pather.Nav.Navigation;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class SoldierPlayer extends BasePlayer {

	private Navigation nav = null;
	private MapLocation targetLoc;
	private Robot weakestTar;
	private Random r = new Random();

	public SoldierPlayer(RobotController rc) {
		super(rc);
		this.nav = new BugNav(rc);
	}

	//go explore, follow spawned archon, transfer energon to archon
	
	public void run() {
		guardThenAttackMode();
	}
	
	public void guardThenAttackMode(){
		while (true) {
			try {
				weakestTar = senseClosestEnemy();
				if (weakestTar == null) {
					try {
						Robot friend = findAFriendly();
						if (friend != null){
							myRC.setIndicatorString(1, "Found friendly! " + friend.getID());
							nav.getNextMove(myRC.senseLocationOf(friend));
							runAtEndOfTurn();
							continue;
						}
						while (myRC.isMovementActive()) {
							runAtEndOfTurn();
						}
						if (myRC.canMove(myRC.getDirection())) {
							myRC.moveForward();
						} else {
							if (r.nextDouble() < .5) {
								myRC.setDirection(myRC.getDirection()
										.rotateLeft());
							} else {
								myRC.setDirection(myRC.getDirection()
										.rotateRight());
							}
							runAtEndOfTurn();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					this.myRC.setIndicatorString(0, "Weakest Target: " + weakestTar.getID());
					targetLoc = myRC.senseLocationOf(weakestTar);
					this.myRC.setIndicatorString(1, "Target at: " + targetLoc.toString());
					if (targetLoc != null
							&& !myRC.getLocation().isAdjacentTo(targetLoc)) {
						attackClosestEnemy(weakestTar);
						this.nav.getNextMove(targetLoc);
						runAtEndOfTurn();
					}
					runAtEndOfTurn();
				}
			} catch (Exception e) {
				System.out.println("caught exception:");
				e.printStackTrace();
			}
		}
	}
}

