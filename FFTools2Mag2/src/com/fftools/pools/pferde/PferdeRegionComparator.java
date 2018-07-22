package com.fftools.pools.pferde;

import java.util.Comparator;

import magellan.library.Region;

public class PferdeRegionComparator implements Comparator<Region> {

	
	public PferdeRegionComparator(){
		// nix zu setzen
	}
	
	/**
	 * Vergleicht 2 Regionen je nach Pferdeanzahl
	 */
	public int compare(Region pferde1,Region pferde2){
		
		return pferde2.getHorses()-pferde1.getHorses();

	}

}
