package patherNav;

import pather.StaticStuff;
import battlecode.common.*;

public abstract class NavGeneral extends StaticStuff{
	public void ngm(int sthg){
		myRC.setIndicatorString(0, "int "+sthg);
	}
}