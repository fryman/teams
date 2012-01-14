package examplefuncsplayer;

import battlecode.common.*;
//ksadpofjasdk;lfjadsk;lfjdsk;lfjasd;lkfjsdk;lfj

// taylor contribution!

// and now brians!! And stephen changed it
public class RobotPlayer {

    public static void run(RobotController myRC) {
        while (true) {
            try {
                while (myRC.isMovementActive()) {
                    myRC.yield();
                }
                if (myRC.canMove(myRC.getDirection())) {
                    myRC.moveForward();
                } else {
                    myRC.setDirection(myRC.getDirection().rotateRight());
                }
                myRC.yield();
            } catch (Exception e) {
                System.out.println("caught exception:");
                e.printStackTrace();
            }
        } 
    }
}
