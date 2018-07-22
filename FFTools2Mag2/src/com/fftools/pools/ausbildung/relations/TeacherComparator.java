package com.fftools.pools.ausbildung.relations;

import java.util.Comparator;

import magellan.library.Skill;

public class TeacherComparator implements Comparator<AusbildungsRelation> {
    
	Skill lehrFach=null;
	
	/**
	 * Konstruktor ist noch leer... brauch ich die Klasse eigentlich?
	 *
	 */
	public TeacherComparator(Skill _lehrfach){
		lehrFach = _lehrfach;
		
	}
	
	
	
	public int compare(AusbildungsRelation teacher1, AusbildungsRelation teacher2) {
	   
		   
		if (teacher1.getTeachOffer()!=null && teacher2.getTeachOffer()!=null && teacher1.getTeachOffer().get(this.lehrFach.getSkillType()).getLevel()!= teacher2.getTeachOffer().get(this.lehrFach.getSkillType()).getLevel()){
			// Talent aufsteigend sortieren
			return teacher1.getTeachOffer().get(this.lehrFach.getSkillType()).getLevel()- teacher2.getTeachOffer().get(this.lehrFach.getSkillType()).getLevel();
			
		}else{
			// Anzahl absteigend sortieren
			return teacher2.getTeachPlaetze() - teacher1.getTeachPlaetze();
		}
	
	
	
	}
	
	
}
