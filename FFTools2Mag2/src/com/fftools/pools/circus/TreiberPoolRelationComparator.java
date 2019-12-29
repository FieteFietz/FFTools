package com.fftools.pools.circus;

import java.util.Comparator;

import com.fftools.pools.treiber.TreiberPoolRelation;

public class TreiberPoolRelationComparator implements Comparator<TreiberPoolRelation>{

	
	
	/**
	 * Vergleicht 2 Regionen je nach Entfernung zu targetRegion und wenn gleich, je nach unterforderung
	 */
	public int compare(TreiberPoolRelation cpr1,TreiberPoolRelation cpr2){

		if (cpr1.getDist()==cpr2.getDist()){
			// gleich weit entfernte
			return (cpr2.getVerdienst() - cpr2.getDoTreiben())- (cpr1.getVerdienst() - cpr1.getDoTreiben());
		} else {
			// liefere den Näheren
			return cpr1.getDist()-cpr2.getDist();
		}
	}
	
}
