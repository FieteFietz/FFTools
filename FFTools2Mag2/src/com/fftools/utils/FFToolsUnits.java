package com.fftools.utils;

import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.GameData;
import magellan.library.Item;
import magellan.library.Ship;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.scripts.Script;

/**
 * Unit-Handling
 * @author Fiete
 *
 */

public class FFToolsUnits {
	
	/**
	 * Berechnet Magielernkosten
	 * Berücksichtight Rassenbonus und Akademieaufenthalt
	 * 
	 * 
	 * @param u
	 * @param data
	 * @return
	 */
	public static int calcMagieLernKosten(Unit u, GameData data){
		int erg = 0;
		SkillType mageSkillType = data.rules.getSkillType("Magie",false);
		Skill mageLevel = u.getModifiedSkill(mageSkillType);
		int raceBonus = 0;
		
//		 Berücksichtigung von Rassenbonus! (War zuerst nicht vorhanden)
    	if(u.getDisguiseRace() != null) {
			raceBonus = u.getDisguiseRace().getSkillBonus(mageSkillType);
		} else {
			if(u.getRace() != null) {
				raceBonus = u.getRace().getSkillBonus(mageSkillType);
			}
		}
    	int relevant_skill = 0;
    	if (mageLevel!=null){
    		relevant_skill = mageLevel.getLevel() - raceBonus;
    		if (relevant_skill<0){
    			relevant_skill=0;
    		}
    	}
    	erg = (50+25*(1+relevant_skill+1)*(relevant_skill+1))*u.getModifiedPersons();
    	if (u.getModifiedBuilding()!=null){
    		if (u.getModifiedBuilding().getBuildingType().getName().equalsIgnoreCase("Akademie")){
    			erg*=2;
    		}
    	}
		return erg;
	}
	
	public static int getModifiedSkillLevel(Skill skill,Unit unit, boolean includeBuilding) {
		if((unit != null) && (unit.getModifiedPersons() != 0)) {
			int raceBonus = 0;
			int terrainBonus = 0;
			int buildingBonus = 0;

			if(unit.getRace() != null) {
				raceBonus = unit.getRace().getSkillBonus(skill.getSkillType());
			}

			if(unit.getRegion() != null) {
				terrainBonus = unit.getRace().getSkillBonus(skill.getSkillType(), unit.getRegion().getRegionType());
			}

			if(includeBuilding && (unit.getBuilding() != null)) {
				buildingBonus = unit.getBuilding().getBuildingType().getSkillBonus(skill.getSkillType());
			}

			return Skill.getLevel(skill.getPoints() / unit.getModifiedPersons(), raceBonus, terrainBonus,
							buildingBonus, unit.isStarving());
		}

		return 0;
	}
	
	
	public static SkillType getBestSkillType(Unit u){
		// bestSkillType feststellen
		SkillType bestSkillType = null;
		int bestSkillLevel = 0;
		if (u.getModifiedSkills()!=null && u.getModifiedSkills().size()>0){
			for (Iterator<Skill> iter = u.getModifiedSkills().iterator();iter.hasNext();){
				Skill actSkill = (Skill)iter.next();
				if (actSkill.getLevel()>=bestSkillLevel){
					bestSkillType = actSkill.getSkillType();
					bestSkillLevel = actSkill.getLevel();
				}
			}
		}
		return bestSkillType;
	}
	
	public static String getBestSkillTypeName(Unit u){
		SkillType bst = FFToolsUnits.getBestSkillType(u);
		if (bst!=null){
			return bst.getName();
		}
		return null;
	}
	
	/**
	 * ?berpr?ft, ob einheit Kapitän eines schiffes
	 * versucht, das shiff zu setzen (wird für pathbuilding en?tigt)
	 * wenn beides OK -> true, sonst false
	 * @return
	 */
	public static boolean checkShip(Script aScript){
		// schiff checken
		Ship myS = aScript.scriptUnit.getUnit().getModifiedShip();
		if (myS==null){
			return false;
		}
		Ship ship = myS;
		
		// Kaptän checken
		Unit captn = ship.getOwnerUnit();
		// ist es unsere unit?
		if (captn!=null && captn.equals(aScript.scriptUnit.getUnit())){
			return true;
		}
		
		// Sonderfall: wir sind jetzt auf keinem oder einem anderen Shiff
		// und betreten ein neues ... dann doch OK geben
		Ship actShip = aScript.scriptUnit.getUnit().getShip();
		if (actShip==null || !actShip.equals(ship)){
			return true;
		}
		return false;
	}
	
	/*
	 * Ware kann Name von itemtype sein, oder catgory
	 */
	public static int getAmountOfWare(ScriptUnit su,String Ware){
		final ReportSettings reportSettings = ReportSettings.getInstance();
		// ist es eine Ware?
		ArrayList<ItemType> requestesTypes = new ArrayList<ItemType>();
		
		su.addComment("Debug (getAmountOfWare): Berechne Anzahl von " + Ware);
		
		ItemType itemType = su.getUnit().getData().getRules().getItemType(Ware);
		if (itemType!=null){
			// keine cat, direkt ergänzen
			su.addComment("Debug: Ware ist eine direkte Itembezeichnung...");
			requestesTypes.add(itemType);
		} else {
			boolean isCat = false;
			isCat = reportSettings.isInCategories(Ware);
			if (isCat){
				requestesTypes.addAll(reportSettings.getItemTypes(Ware));
			} else {
				su.addComment("!!! Ware war keine Gegenstand und keine Kategorie!!! (" + Ware + ")");
				su.doNotConfirmOrders();
			}
		}
		
		int erg=0;
		if (requestesTypes.size()>0){
			for (ItemType iT : requestesTypes) {
				su.addComment("Debug: Erfrage für itemType " + iT.getName());
				Item i = su.getUnit().getItem(iT);
				if (i!=null){
					erg+=i.getAmount();
					su.addComment("Betrag erhöht um " + i.getAmount());
				} else {
					su.addComment("Diese Einheit hat 0 " + iT.getName());
				}
			}
		}
		su.addComment("Ergebnis der Berechnung für " + Ware + ": " + erg);
		return erg;
	}
	
	
}
