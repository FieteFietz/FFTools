package com.fftools.pools.heldenregionen;

import java.util.Comparator;
import java.util.Iterator;

import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.ScriptUnit;
import com.fftools.scripts.Heldenregion;


/**
 * wird benutzt, um abstrakte Requests umzusetzen: die offers
 * werden nach Prio der begriffe sortiert
 * @author Fiete
 *
 */
public class HeldenScriptUnitsComparator implements Comparator<ScriptUnit>{
	
	/**
	 * name der Kategorie
	 */
	Heldenregion heldenRegion = null;
	
	public HeldenScriptUnitsComparator(Heldenregion heldenregion) {
		this.heldenRegion = heldenregion;
	}
	
	/**
	 * der Vergleich, richtet sich nach den Prios 
	 * @param o1
	 * @param o2
	 * @return
	 */
	public int compare(ScriptUnit u1,ScriptUnit u2){
		int maxTalent1 = 0;
		int maxTalent2 = 0;
		maxTalent1 = this.getMaxTalent(u1);
		maxTalent2 = this.getMaxTalent(u2);
		
		if (maxTalent1==maxTalent2){
			return (u1.getUnit().getModifiedPersons() - u2.getUnit().getModifiedPersons());
		}
		return (maxTalent2-maxTalent1);
	}
	
	
	private int getMaxTalent(ScriptUnit u){
		int erg = 0;
		for (Iterator<SkillType> iter=this.heldenRegion.getTalentSkillTypeListe().iterator();iter.hasNext();){
			SkillType actSkillType = (SkillType)iter.next();
			Skill actSkill = u.getUnit().getModifiedSkill(actSkillType);
			int actLevel = 0;
			if (actSkill!=null){
				actLevel = actSkill.getLevel();
				if (actLevel>erg){
					erg = actLevel;
				}
			}
		}
		return erg;
	}
	
}
