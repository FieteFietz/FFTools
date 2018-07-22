package com.fftools.pools.akademie;

import java.util.Comparator;

import magellan.library.Building;
import magellan.library.Skill;

import com.fftools.pools.ausbildung.relations.AusbildungsRelation;


public class AusbildungsRelationComparator implements Comparator<AusbildungsRelation> {

// 	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private Building akademie;
	
	public AusbildungsRelationComparator(Building akademie){
		this.akademie = akademie;
	}
	
	
	
	
	public int compare(AusbildungsRelation kandidat1, AusbildungsRelation kandidat2) {
		// muss 1 zurückgeben, wenn Kandidat 1 weiter vorne stehen soll
		// muss -1 zurückgeben, wenn Kandidat 1 weiter vorne stehen soll
		// muss 0 zurückgeben, wenn gleich gut
		
		// Besitzer der aka....
		if (this.akademie!=null && this.akademie.getModifiedOwnerUnit()!=null){
			if (kandidat1.getScriptUnit().getUnit().equals(akademie.getModifiedOwnerUnit())){
				return 1;
			}
			if (kandidat2.getScriptUnit().getUnit().equals(akademie.getModifiedOwnerUnit())){
				return -1;
			}
		}
		// OK, keiner ist besitzer
		
		// Helden
		if (kandidat1.getScriptUnit().getUnit().isHero() && !kandidat2.getScriptUnit().getUnit().isHero()) {
			return 1;
		}
		if (kandidat2.getScriptUnit().getUnit().isHero() && !kandidat1.getScriptUnit().getUnit().isHero()) {
			return -1;
		}
		
		// ok, beide sind Helden oder beide nicht

		// Jetzt bereits Talentstufen
		Skill skill1 = kandidat1.getScriptUnit().getUnit().getModifiedSkill(kandidat1.getOrderedSkillType());
		Skill skill2 = kandidat2.getScriptUnit().getUnit().getModifiedSkill(kandidat2.getOrderedSkillType());
		
		int skillLevel1 = 0;
		int skillLevel2 = 0;
		if (skill1!=null){
			skillLevel1 = skill1.getLevel();
		}
		if (skill2!=null){
			skillLevel2 = skill2.getLevel();
		}
		if (skillLevel1!=skillLevel2){
			return skillLevel1-skillLevel2;
		}
		
		// OK, gleicher Skilllevel
		// Lehrer vor Schüler
		if (kandidat1.isTeacher() && !kandidat2.isTeacher()){
			return 1;
		}
		if (kandidat2.isTeacher() && !kandidat1.isTeacher()){
			return -1;
		}
		
		// beide Lehrer oder beide nicht Lehrer
		// Grössere Einheiten zuerst 
		int diff = kandidat1.getScriptUnit().getUnit().getModifiedPersons() - kandidat2.getScriptUnit().getUnit().getModifiedPersons();
		if (diff!=0){
			return diff;
		}
		
		// OK, auch noch gleich viele...		
		return 0;

	}

}
