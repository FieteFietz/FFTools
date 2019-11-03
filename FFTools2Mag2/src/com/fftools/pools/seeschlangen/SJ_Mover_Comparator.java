package com.fftools.pools.seeschlangen;

import java.util.Comparator;

import com.fftools.scripts.Seeschlangenjagd;

import magellan.library.CoordinateID;
import magellan.library.utils.Regions;


/**
 * wird benutzt, um Seeschlangenjagd zu sortieren, nach Entfernung zum Target
 * werden nach Prio der begriffe sortiert
 * @author Fiete
 *
 */
public class SJ_Mover_Comparator implements Comparator<Seeschlangenjagd>{
	
	/**
	 * name der Kategorie
	 */
	CoordinateID targetC = null;
	
	public SJ_Mover_Comparator(CoordinateID _target) {
		this.targetC = _target;
	}
	
	/**
	 * der Vergleich, richtet sich nach den Prios 
	 * @param o1
	 * @param o2
	 * @return
	 */
	public int compare(Seeschlangenjagd u1,Seeschlangenjagd u2){
		
		int dist1 = Regions.getDist(u1.getUnit().getRegion().getCoordinate(), this.targetC);
		int dist2 = Regions.getDist(u2.getUnit().getRegion().getCoordinate(), this.targetC);
		
		return dist1 - dist2; // kleinste Entfernung nach oben
		
	}
	
	
	
	
}
