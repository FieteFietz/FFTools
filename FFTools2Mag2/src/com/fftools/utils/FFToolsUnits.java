package com.fftools.utils;

import java.util.ArrayList;
import java.util.Iterator;

import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.scripts.Script;

import magellan.library.Building;
import magellan.library.GameData;
import magellan.library.Item;
import magellan.library.Ship;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.rules.ItemType;
import magellan.library.rules.ShipType;
import magellan.library.rules.SkillType;

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

			if(includeBuilding && (unit.getModifiedBuilding() != null)) {
				buildingBonus = unit.getModifiedBuilding().getBuildingType().getSkillBonus(skill.getSkillType());
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
	 * Überprüft, ob einheit Kapitän eines schiffes
	 * versucht, das shiff zu setzen (wird für pathbuilding benötigt)
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
		
		// Speed checken
		// data.getGameSpecificRules().getShipRange(ship)
		if (aScript.scriptUnit.getScriptMain().gd_ScriptMain.getGameSpecificRules().getShipRange(ship) == 0) {
			aScript.doNotConfirmOrders("!!! Problem: Geschwindigkeit dieses Schiffes wird mit 0 berechnet!!!");
			return false;
		}
		
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
	
	/**
	 * Überprüft die Talente, die bekannt sind, auf dem Schiff
	 * @param aScript
	 * @return
	 */
	public static boolean checkShipTalents(Script aScript) {
		Ship myS = aScript.scriptUnit.getUnit().getModifiedShip();
		if (myS==null){
			aScript.addComment("Problem: Talentcheck unmöglich, nicht auf einem Schiff");
			return false;
		}
		Ship ship = myS;
		
		// Kaptän checken
		Unit captn = ship.getModifiedOwnerUnit();
		// ist es unsere unit?
		if (captn==null) {
			aScript.addComment("Problem: Talentcheck unmöglich, Schiff hat keinen Kapitän");
			return false;
		}
			
		if (!captn.equals(aScript.scriptUnit.getUnit())){
			aScript.addComment("Problem: Talentcheck unmöglich, Einheit ist nicht der Kapitän");
			return false;
		}
		
		// Schiffstyp herausfinden
		ShipType ST = ship.getShipType();
		if (ST==null) {
			aScript.addComment("Problem: Talentcheck unmöglich, Schiffstyp ist unbekannt");
			return false;
		}
		
		// Kapitänslevel
		SkillType SegelType = aScript.gd_Script.getRules().getSkillType("Segeln");
		Skill SegelSkill = aScript.getUnit().getModifiedSkill(SegelType);
		if (SegelSkill==null) {
			aScript.addComment("Problem: Talentcheck gescheitert, Kapitän kann gar nicht Segeln!");
			return false;
		}
		
		if (SegelSkill.getLevel()<ST.getCaptainSkillLevel()) {
			aScript.addComment("Problem: Talentcheck gescheitert, Kapitänstalent nicht hoch genug. (" + SegelSkill.getLevel() + " < " + ST.getCaptainSkillLevel() + ")");
			return false;
		}
		
		// Gesamttalent 
		int talentsNeeded = ST.getSailorSkillLevel() * ship.getModifiedAmount();
		if (talentsNeeded<=0) {
			aScript.addComment("Problem: Talentcheck gescheitert, benötigtes Mannschaftstalent war nicht feststellbar.");
			return false;
		}
		
		if (ship.modifiedUnits()==null) {
			aScript.addComment("Problem: Talentcheck gescheitert, Schiff wird unbesetzt sein.");
			return false;
		}
		
		// Mannschaftstalent berechnen
		int vorhandeneTalente = 0;
		for (Unit u:ship.modifiedUnits()) {
			if (u.getModifiedSkill(SegelType)!=null) {
				Skill sK = u.getModifiedSkill(SegelType);
				if (sK.getLevel()>=ST.getMinSailorLevel()) {
					vorhandeneTalente+=sK.getLevel() * u.getModifiedPersons();
				}
			}
		}
		
		if (vorhandeneTalente<talentsNeeded) {
			aScript.addComment("Problem: Talentcheck gescheitert, Gesamtsegeltalent reicht nicht aus. (" + vorhandeneTalente + " < " + talentsNeeded + ")");
			return false;
		}
		
		// Anzahl der Kapitäne
		if (aScript.getUnit().getModifiedPersons()<ship.getModifiedAmount()) {
			aScript.addComment("Problem: Talentcheck gescheitert, Anzahl der Kapitäne reicht nicht für die Flottengröße (" + aScript.getUnit().getModifiedPersons() + " < " + ship.getModifiedAmount() + ")");
			return false;
		}
		
		
		return true;
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
				su.doNotConfirmOrders("!!! Ware war kein Gegenstand und keine Kategorie!!! (" + Ware + ")");
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
	
	/*
	 * ermittelt die Anzahl an Waffen einer Einheit
	 */
	public static int getAmountOfWeapons(Unit u) {
		int erg= 0;
		for (Item i : u.getModifiedItems()) {
			ItemType t = i.getItemType();
			if (t!=null && t.getCategory()!=null && (t.getCategory().getName().equalsIgnoreCase("Waffen") || t.getCategory().getName().equalsIgnoreCase("Distanz-Waffen"))){
				erg+=i.getAmount();
			}
		}
		return erg;
	}
	
	
	/**
	 * verlässt die Aka, wenn denn drinn
	 * @param su
	 */
	public static void leaveAcademy(ScriptUnit su, String comment) {
		Building b = su.getUnit().getBuilding();
		if (b!=null) {
			if (b.getBuildingType().getName().equalsIgnoreCase("akademie") && !su.isLeavingBuilding) {
				// bingo
				su.addOrder("verlasse ;" + comment, true);
				su.isLeavingBuilding=true;
			}
		}
	}
	
	/**
	 * berechnet effektive Personenanzahl mit Berücksichtigung von RdfF und Schaffenstrunk
	 * nach Regeländerung zum 04.06.2022 
	 * @param su
	 * @return
	 */
	public static int getPersonenEffektiv(ScriptUnit su) {
		int erg=0;
		// Anzahl RdfF ermitteln
		int AnzahlRdfF = 0;
		ItemType rdfType=su.getUnit().getData().getRules().getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = su.getModifiedItem(rdfType);
			if (rdfItem!=null) {
				AnzahlRdfF = rdfItem.getAmount();
			}
		}
		
		if (AnzahlRdfF>su.getUnit().getModifiedPersons()) {
			su.addComment("!!! Einheit hat zu viele RdfF !");
			AnzahlRdfF = su.getUnit().getModifiedPersons();
		}
		
		
		// Anzahl Schaffenstrunk effekte ermitteln und dabei auf TrankEffekt und Benutze prüfen
		int actEffekte = FFToolsGameData.countSchaffenstrunkEffekt(su);
		if (actEffekte>su.getUnit().getModifiedPersons()) {
			actEffekte = su.getUnit().getModifiedPersons();
		}
		
		int Po = su.getUnit().getModifiedPersons();  // Gesamtanzahl aller Personen 
		// Wieviel haben R und E ? => das Minimum der beiden Anzahlen
		
		int PRE = Math.min(AnzahlRdfF, actEffekte);  // Anzahl Personen mit R und E
		Po -= PRE;  // Rest hat *nicht* beide Effekte
		
		// Jetzt schauen, ob noch welche übrig bleiben, die nur eines von beiden haben
		int PR = Math.max(0, AnzahlRdfF - PRE); // Wieviele haben nur einen RdfF
		int PE = Math.max(0, actEffekte - PRE); // Wieviele haben nur einen Effekt
		
		// Gesamtanzahl um diese Personen reduzieren, und sicherstellen, dass sie nicht negativ wird... ,-)
		Po = Math.max(0, Po - (PR + PE));
		
		// lass uns Reden:
		su.addComment("effPers: RdfF und Trank: " + PRE + " (x11), nur RdfF: " + PR + " (x10), nur Trank: " + PE + " (x2), normale: " + Po);
		
		// finale Berechnung:
		erg = PRE*11 + PR*10 + PE *2 + Po;
		su.addComment("effPers: " + erg);

		return erg;
	}
	

}
