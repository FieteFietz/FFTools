package com.fftools.pools.akademie;

import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;


public class AkademieTalent {

	  
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private SkillType skillType;
	private int Anzahl;
	
	
	
	/**
	 * Konstruktor 
	 *
	 */
	public AkademieTalent(SkillType sT, int _Anzahl){
		this.skillType = sT;
		this.Anzahl = _Anzahl;
    }



	/**
	 * @return the skillType
	 */
	public SkillType getSkillType() {
		return skillType;
	}



	/**
	 * @return the anzahl
	 */
	public int getAnzahl() {
		return Anzahl;
	}

	
}
