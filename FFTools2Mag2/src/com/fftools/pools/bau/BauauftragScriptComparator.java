package com.fftools.pools.bau;

import java.util.Comparator;

import magellan.library.Region;

import com.fftools.scripts.Bauen;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

/**
 * small class to compare 2 Bauen-scripts (als Aufträge)
 * @author Fiete
 *
 */
public class BauauftragScriptComparator implements Comparator<Bauen> {
	
	private Region targetRegion;
	// private int level=1;
	private String talentName;
	// private int builtSize=1;
	
	
	
	public BauauftragScriptComparator(Region r, int _level,String _talentName, int _builtSize){
		this.targetRegion = r;
		// this.level=_level;
		// this.builtSize=_builtSize;
		this.talentName = _talentName;
	}
	
	
	
	
	public int compare(Bauen b1, Bauen b2){
		int wert1 = 1000;
		int wert2 = 1000;
		
		// 20191105: zuerst *nur* nach Entfernung gehen.
		
		// Berechnung: zurückzulegender Weg + Bauzeit
		if (targetRegion.equals(b1.scriptUnit.getUnit().getRegion())){
			wert1 =0;
		} else {
			GotoInfo gI1 = FFToolsRegions.makeOrderNACH(b1.scriptUnit, targetRegion.getCoordinate() ,b1.scriptUnit.getUnit().getRegion().getCoordinate(), false,"BauauftragScriptComp");
			wert1 = gI1.getAnzRunden();
		}
		if (targetRegion.equals(b2.scriptUnit.getUnit().getRegion())){
			wert2 =0;
		} else {
			GotoInfo gI2 = FFToolsRegions.makeOrderNACH(b2.scriptUnit, targetRegion.getCoordinate() ,b2.scriptUnit.getUnit().getRegion().getCoordinate(), false,"BauauftragScriptComp");
			wert2 = gI2.getAnzRunden();
		}
		
		int erg = wert1 - wert2;
		
		if (erg==0){
			// bei immer noch gleichheit
			// der mit der grösseren Talentanzahl
			erg = (b2.scriptUnit.getSkillLevel(this.talentName) * b2.scriptUnit.getUnit().getModifiedPersons())-(b1.scriptUnit.getSkillLevel(this.talentName)*  b1.scriptUnit.getUnit().getModifiedPersons());
		}
		
		if (erg==0){
			// der mit dem höchsten Talent soll sich bewegen, der andere eventuell lernen
			erg = (b2.scriptUnit.getSkillLevel(this.talentName) )-(b1.scriptUnit.getSkillLevel(this.talentName));
		}
		
		return erg;
	}
}

