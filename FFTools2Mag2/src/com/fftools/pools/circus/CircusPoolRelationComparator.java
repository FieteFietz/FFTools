package com.fftools.pools.circus;

import java.util.Comparator;

public class CircusPoolRelationComparator implements Comparator<CircusPoolRelation>{

	
	
	/**
	 * Vergleicht 2 Regionen je nach Entfernung zu targetRegion und wenn gleich, je nach unterforderung
	 */
	public int compare(CircusPoolRelation cpr1,CircusPoolRelation cpr2){

		if (cpr1.getDist()==cpr2.getDist()){
			// gleich weit entfernte
			return (cpr2.getVerdienst() - cpr2.getDoUnterhaltung())- (cpr1.getVerdienst() - cpr1.getDoUnterhaltung());
		} else {
			// liefere den Näheren
			return cpr1.getDist()-cpr2.getDist();
		}

	}
	
}
