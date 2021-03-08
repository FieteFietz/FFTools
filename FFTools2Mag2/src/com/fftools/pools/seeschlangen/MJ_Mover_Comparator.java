package com.fftools.pools.seeschlangen;

import java.util.Comparator;

import com.fftools.scripts.Jagemonster;


/**
 * wird benutzt, um Jagemonster zu sortieren, nach Entfernung zum Target
 * 
 * @author Fiete
 *
 */
public class MJ_Mover_Comparator implements Comparator<Jagemonster>{
	
	
	
	public MJ_Mover_Comparator() {
		
	}
	
	/**
	 * der Vergleich, richtet sich nach den weeks2target
	 * @param o1
	 * @param o2
	 * @return
	 */
	public int compare(Jagemonster u1,Jagemonster u2){
		if (u1.getHC_weeks2target() != u2.getHC_weeks2target()) {
			return u1.getHC_weeks2target() - u2.getHC_weeks2target(); // kleinste Entfernung nach oben
		} else {
			return u2.getBattleValue() - u1.getBattleValue();  // grösste Stärke nach oben
		}
	}
	
	
	
	
}
