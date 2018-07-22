package com.fftools.pools.ausbildung.relations;

import java.util.ArrayList;
import java.util.Comparator;

import magellan.library.rules.SkillType;


public class AutoDidaktenComparator implements Comparator<AusbildungsRelation> {
	ArrayList<SkillType> sortedSkillType = null;

// 	private static final OutTextClass outText = OutTextClass.getInstance();
	
	public AutoDidaktenComparator(ArrayList<SkillType> _sortedSkillType){
		// Priorisierte Talentreihenfolge aus AusbildungsPool
		sortedSkillType =_sortedSkillType;
	}
	
	
	
	
	public int compare(AusbildungsRelation kandidat1, AusbildungsRelation kandidat2) {
		
		// Gibt es Default Talente für beide?
		if ((kandidat1.getDefaultTalent()!=null)&&(kandidat2.getDefaultTalent()!=null)){
			// ungleiche talente?
			if (kandidat1.getDefaultTalent().getSkillType()!=(kandidat2.getDefaultTalent().getSkillType())){
			   // aufsteigen nach index aber abfallend in bezug auf prio...
				return sortedSkillType.indexOf(kandidat1.getDefaultTalent().getSkillType())-sortedSkillType.indexOf(kandidat2.getDefaultTalent().getSkillType());
			}else{
				// gleiche talente, dann nach stufe absteigend.
				return kandidat2.getDefaultTalent().getLevel()-kandidat1.getDefaultTalent().getLevel();
				
				
			}
			
			
			
		}
	    
		// beide null dann gleichstand
		if ((kandidat1.getDefaultTalent()==null)&&(kandidat2.getDefaultTalent()==null)){
			return 0;
		}
        
		// Nur einer ist null 
		
		if (kandidat1.getDefaultTalent()==null){
			return 1;
		}else{
			return -1;
		}
		
		
		

	}

}
