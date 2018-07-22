package com.fftools.pools.ausbildung.relations;

import java.util.Comparator;

import magellan.library.Skill;

public class SchuelerLernKostenComparator implements Comparator<AusbildungsRelation> {
	Skill lernFach=null;
	
	/**
	 * Konstruktor ist noch leer... brauch ich die Klasse eigentlich?
	 *
	 */
	public SchuelerLernKostenComparator(Skill _lehrfach){
		lernFach = _lehrfach;
		
	}
	
	
	
	public int compare(AusbildungsRelation schueler1, AusbildungsRelation schueler2) {
	       // Aufsteigende Kosten in der List erwünscht
		   return schueler1.getLernKosten(lernFach)-schueler2.getLernKosten(lernFach);
		   
		   
		  
	}

}
