package patherNav;

public class BugNav {
/*
 * This class will provide the methods necessary for bug navigation.
 * Walk toward the target until obstructed, then follow the wall 
 * until it is possible to approach the target again.
 * 
 * Strategy:
 * -Build in patience (if you bump into an ally, back up once, then yield
 * 	subsequent turn)
 * -Problem occurs when multiple robots crash into a wall and then 
 *  bump around in a clump, not moving productively
 * 
 * Solutions:
 * -Don't send all the robots to the same place (this isn't feasible 
 *  when you want to have swarm movement).
 * -Recalculate line when you get to the end of an object (every turn?)
 * 
 * One move at a time is requested by each robot. Robot supplies
 * whether it is on_wall or not. If robot doesn't know, passes in false.
 * 
 * 
 */
}
