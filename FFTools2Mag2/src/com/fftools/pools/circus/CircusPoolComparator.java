package com.fftools.pools.circus;

import java.util.Comparator;

public class CircusPoolComparator implements Comparator<CircusPool>{

	
	/**
	 * Vergleicht 2 Regionen je nach Pferdeanzahl
	 */
	public int compare(CircusPool cp1,CircusPool cp2){
		
		return cp2.getRemainingUnterhalt() - cp1.getRemainingUnterhalt();

	}
	
}
