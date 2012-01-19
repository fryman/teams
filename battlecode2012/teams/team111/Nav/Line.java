package team111.Nav;

import battlecode.common.Direction;

public class Line {
	// map coordinates are modeled by int[]
	private int[] start;
	private int[] end;
	private Direction dir = null;

	public Line(int[] s, int[] e) {
		this.start = s;
		this.end = e;
	}

	public Direction getDirection() {
		return this.dir;
	}

	public void setDirection(Direction direction) {
		this.dir = direction;
	}

	public boolean onLine(int[] candidate) {
		/*
		 * // if slopeDifference is 0, then candidate is on the line formed
		 * between // start and end if ((start[0] - candidate[0] == 0 && end[0]
		 * - candidate[0] == 0) || (start[1] - candidate[1] == 0 && end[1] -
		 * candidate[1] == 0)) { return true; } else if (start[1] - candidate[1]
		 * == 0){ return false; } else if (end[1] - candidate[1] == 0){ return
		 * false; } else if (start[0] - candidate[0] == 0){ return false; } else
		 * if (end[0] - candidate[0] == 0){ return false; } double
		 * slopeDifference = (1.0 * start[0] - candidate[0]) / (1.0 * start[1] -
		 * 1.0 * candidate[1]) - (1.0 * end[0] - 1.0 * candidate[0]) / (1.0 *
		 * end[1] - 1.0 * candidate[1]); // to account for int vs. double math,
		 * we incorporate a tolerance System.out.println(slopeDifference); if
		 * (Math.abs(slopeDifference) < .15) { return true; } return false;
		 */

		// perhaps better off doing the atan method.
		/*
		 * double angleToStart = Math.atan2(1.0*candidate[1] - 1.0 * start[1],
		 * 1.0 * candidate[0]-1.0 * start[0]); double angleToEnd =
		 * Math.atan2(1.0 * end[1] - 1.0 * candidate[1], 1.0 * end[0] - 1.0 *
		 * candidate[0]); double angleDifference = angleToStart - angleToEnd; //
		 * System.out.println("" + angleToStart + ", " + angleToEnd); //
		 * System.out.println(" "+(1.0 * start[1] - 1.0*candidate[1])+" "+(1.0 *
		 * start[0] - 1.0 * candidate[0]) + " " + // (1.0 * end[1] - 1.0 *
		 * candidate[1]) + " " + (1.0 * end[0] - 1.0 * candidate[0]));
		 * System.out.println(angleDifference); if (Math.abs(angleDifference) <
		 * .2) { return true; } return false;
		 */
		// many cases
		int a = end[0] - candidate[0];
		int b = end[1] - candidate[1];
		int c = candidate[0] - start[0];
		int d = candidate[1] - start[1];
		// if very close to start or to end, then return true. otherwise, the
		// slope errors blow up.
		double distSquareToStart = c * c + d * d;
		double distSquareToEnd = a * a + b * b;
		double distSquareTolerance = 4;
		if (distSquareToEnd < distSquareTolerance
				|| distSquareToStart < distSquareTolerance) {
			return true;
		}
		// System.out.printlqn("a: " + a + ", b: " + b + ", c: " + c +
		// ", d: "+d);
		if (a == 0) {
			if (c == 0) {
				return true;
			}
			return false;
		}
		if (c == 0) {
			if (a == 0) {
				return true;
			}
			return false;
		}
		// System.out.println((1.0*b)/(1.0*a));
		double slopeDifference = (1.0 * b) / (1.0 * a) - (1.0 * d) / (1.0 * c);
		//System.out.println(slopeDifference);
		return Math.abs(slopeDifference) < .7;
	}
}
