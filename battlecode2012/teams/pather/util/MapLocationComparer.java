package pather.util;

import java.util.Comparator;

import battlecode.common.MapLocation;

/**
 * Inner class used for comparing MapLocations by distance.
 * 
 * @author saf
 */
class MapLocationComparer implements Comparator {

	private MapLocation startLocation;
	private MapLocation goalLocation;

	public MapLocationComparer(MapLocation startLocation,
			MapLocation goalLocation) {
		this.startLocation = startLocation;
		this.goalLocation = goalLocation;
	}

	/**
	 * Returns a negative integer, zero, or a positive integer as the first
	 * argument is less than, equal to, or greater than the second.
	 */
	@Override
	public int compare(Object arg0, Object arg1) {
		try {
			if (arg0.getClass() != MapLocation.class
					|| arg1.getClass() != MapLocation.class) {
				throw new RuntimeException(
						"This is intended to compare maplocations");
			} else {
				double dist0 = this.startLocation
						.distanceSquaredTo((MapLocation) arg0)
						+ ((MapLocation) arg0).distanceSquaredTo(goalLocation);
				double dist1 = this.startLocation
						.distanceSquaredTo((MapLocation) arg1)
						+ ((MapLocation) arg0).distanceSquaredTo(goalLocation);
				return (int) (dist1 - dist0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}