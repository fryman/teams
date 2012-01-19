package team111.Nav;

import battlecode.common.*;

public class BoidianNav extends Navigation {
	private RobotController myRC;
	private RobotType ownerType;

	public BoidianNav(RobotController myRC) {
		this.myRC = myRC;
		this.ownerType = this.myRC.getType();
	}

	/**
	 * The behavior we are looking for is Boid-like. The rules inherent are
	 * such:
	 * 
	 * separation: steer to avoid crowding local flockmates
	 * 
	 * alignment: steer towards the average heading of local flockmates
	 * 
	 * cohesion: steer to move toward the average position (center of mass) of
	 * local flockmates
	 * 
	 * obstacle avoidance: steer to avoid going over non-traversible land
	 */
	@Override
	public void getNextMove(MapLocation target) {

	}

}
