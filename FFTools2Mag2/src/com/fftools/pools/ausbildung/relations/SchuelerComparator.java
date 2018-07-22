package com.fftools.pools.ausbildung.relations;

import java.util.Comparator;

import magellan.library.Skill;

public class SchuelerComparator implements Comparator<AusbildungsRelation> {
	Skill lernFach=null;
	
	/**
	 * Konstruktor ist noch leer... brauch ich die Klasse eigentlich?
	 *
	 */
	public SchuelerComparator(Skill _lernfach){
		lernFach = _lernfach;
		
	}
	
	public int compare(AusbildungsRelation teacher1, AusbildungsRelation teacher2) {

	
		if (teacher1.getStudyRequest()!=null && teacher2.getStudyRequest()!=null && teacher1.getStudyRequest().get(this.lernFach.getSkillType()).getLevel()!= teacher2.getStudyRequest().get(this.lernFach.getSkillType()).getLevel()){
			// Talent absteigend sortieren
			return teacher2.getStudyRequest().get(this.lernFach.getSkillType()).getLevel()- teacher1.getStudyRequest().get(this.lernFach.getSkillType()).getLevel();
			
		}else{
			// Anzahl absteigend sortieren
			return teacher2.getSchuelerPlaetze() - teacher1.getSchuelerPlaetze()  ;
		}
	
	
	
	}
	
	
}
