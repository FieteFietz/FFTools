package com.fftools.scripts;

import java.util.HashMap;
import java.util.Iterator;

import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.pools.ausbildung.AusbildungsPool;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;


public class LernPoolScript extends Script {
    
	AusbildungsPool lernPool = null;
	
	
	public LernPoolScript(){
		
	}
	
	
	public void lerneTalent(String _string, boolean _lehrAuftrag){
		this.lerneTalent(super.gd_Script.rules.getSkillType(_string), _lehrAuftrag);
	}
	
	
	public void lerneTalent(SkillType _skillType, boolean _lehrAuftrag){
		Skill skill = null;
		// gibt es den skilltype?
		if (_skillType !=null){
		   skill = super.scriptUnit.getUnit().getSkill(_skillType);
           // kann die einheit das schon?
		   
		   if (skill == null){
        	   // ok kann sie noch nicht, dann erzeuge wir den skill...
        	   skill = new Skill(_skillType,0,0,this.scriptUnit.getUnit().getModifiedPersons(),true);
           }
        	
		   // dann weiter zur nächsten übeladenen..
		   this.lerneTalent(skill, _lehrAuftrag);
		   
		}
	}
	
	public void lerneTalent(Skill _skill, boolean _lehrAuftrag){
			
		// Lernliste aufbauen
		HashMap<SkillType, Skill> teachMap = null;
		HashMap<SkillType, Skill> studyMap = new HashMap<SkillType, Skill>();
	    studyMap.put(_skill.getSkillType(), _skill);
	    // Lehrliste aufbauen
	    if ((this.scriptUnit.getUnit().getSkills() !=null)&&(_lehrAuftrag)){
	    teachMap =  new HashMap<SkillType, Skill>();
	      for (Iterator<Skill> iter = this.scriptUnit.getUnit().getSkills().iterator();iter.hasNext();){
	           Skill aktskill = (Skill) iter.next();
	    	  teachMap.put(aktskill.getSkillType(), aktskill);
	      }
	    }
		// nochmal überladen...
	    this.lerneTalent(studyMap, teachMap);
		
	}

	
    public void lerneTalent(HashMap<SkillType, Skill> _lernMap, HashMap<SkillType, Skill> _teachMap){
    	lernPool = this.scriptUnit.getScriptMain().getOverlord().getAusbildungsManager().getAusbildungsPool(this.scriptUnit);		
    	lernPool.addAusbildungsRelation(new AusbildungsRelation(this.scriptUnit, _lernMap, _teachMap ));
    }
	
}	
	

