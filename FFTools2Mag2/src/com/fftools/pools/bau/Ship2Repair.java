package com.fftools.pools.bau;

import com.fftools.ScriptUnit;

import magellan.library.CoordinateID;
import magellan.library.Ship;

public class Ship2Repair implements Comparable<Ship2Repair>{
	public Ship s = null;
	public ScriptUnit captn = null;
	public CoordinateID nextShipStop = null;
	
	public int repairsNeeded = 0; // wird durch SeeWerftPool ggf reduziert !!!

	public int compareTo(Ship2Repair pS) {
		return pS.repairsNeeded - this.repairsNeeded;
	}
	

}
