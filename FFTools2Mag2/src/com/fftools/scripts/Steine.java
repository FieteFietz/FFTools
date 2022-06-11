package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;

import magellan.library.Building;
import magellan.library.Region;
import magellan.library.RegionResource;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

public class Steine extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 51;
	/*
	 * 13.12.2015 war 135...gesetzt auf 51, um vor Lernfix zu sein
	 */
	int Durchlauf_2 = 205;
	/*
	 * 14.10.2017 neu, um RdfF nach Matpool zu berücksichtgen
	 */
	

	
	private int[] runners = {Durchlauf_1,Durchlauf_2};
	
	private boolean makeStein=false;
	private int myStandardSkillLevel = 0;
	
	/**
	 * ab welchem Talent gehts erst mal los?
	 */
	private int minTalent = 1;
	
	/**
	 * Wir wollen in einem Steinbruch arbeiten, oder es explizit geagt bekommen, dass wir ausserhalb arbeiten sollen
	 */
	private boolean Steinbruch = true;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	
	private String LernfixOrder = "Talent=Steinbau";
	
	public Steine() {
		super.setRunAt(this.runners);
	}
	
	
	/**
	 * Eigentliche Prozedur
	 * runScript von Script.java MUSS/SOLLTE ueberschrieben werden
	 * Durchlauf kommt von ScriptMain
	 * 
	 * in Script steckt die ScriptUnit
	 * in der ScriptUnit steckt die Unit
	 * mit addOutLine jederzeit Textausgabe ans Fenster
	 * mit addComment Kommentare...siehe Script.java
	 */
	
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf==this.Durchlauf_1){
			this.start();
		}
		if (scriptDurchlauf==this.Durchlauf_2){
			this.produce();
		}

	}
	
	
	
	/**
	 * eigentlich ganz einfach: nur so lange eisen machen,
	 * bis die Talentstufe nicht mehr ausreicht
	 */
	private void start(){
		super.addVersionInfo();
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Steine");
		OP.addOptionList(this.getArguments());
		
		int unitMinTalent = OP.getOptionInt("minTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		unitMinTalent = OP.getOptionInt("mindestTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		
		this.Steinbruch = OP.getOptionBoolean("Steinbruch", this.Steinbruch);
		
		// Eigene Talentstufe ermitteln
		int skillLevel = 0;
		SkillType skillType = this.gd_Script.getRules().getSkillType("Steinbau", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
			} else {
				this.addComment("!!! unit has no skill Steinbau!");
			}
		} else {
			this.addComment("!!! can not get SkillType Steinbau!");
		}
		if (skillLevel>=this.minTalent){
			// Regionslevel beziehen
			Region R = this.scriptUnit.getUnit().getRegion();
			ItemType IT = this.gd_Script.getRules().getItemType("Steine");
			RegionResource RR = R.getResource(IT);
			String modeSetting = OP.getOptionString("mode");
			this.addComment("searching for automode setting, found: " + modeSetting);
			if (RR == null) {
				this.addComment("Region hat kein Steinvorkommen!!!, Lerne...");
				// this.addOrder("Lernen Bergbau", true);
				this.Lerne();
				if (modeSetting.equalsIgnoreCase("auto") || modeSetting.equalsIgnoreCase("search")){
					this.addComment("AUTOmode detected....confirmed learning");
				} else {
					this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
				}
			} else {
				if (RR.getSkillLevel()<=skillLevel) {
					if (modeSetting.equalsIgnoreCase("search")) {
						// erforschung abgeschlossen - abbrechen
						this.addComment("Steine in der Region bei T" + RR.getSkillLevel() + ", wir brechen die Suche ab.");
						this.doNotConfirmOrders("Steinlevel erforscht, Suche abgebrochen");
						this.makeStein=false;
					} else {
						// weiter machen
						this.addComment("Steine in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
						if (!this.Steinbruch) {
							this.addComment("Steine: keine Prüfung auf Arbeit im Steinbruch.");
						}
						if (!this.Steinbruch || this.checkSteinbruch()) {
							this.myStandardSkillLevel = skillLevel;
							this.makeStein=true;
						}
					}
				} else {
					this.addComment("Steine in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel + ", ich lerne");
					// this.addOrder("Lernen Bergbau", true);
					this.Lerne();
					if (modeSetting.equalsIgnoreCase("auto") || modeSetting.equalsIgnoreCase("search")){
						this.addComment("AUTOmode detected....confirmed learning");
					} else {
						this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
					}
				}
			}
		} else {
			this.makeStein=false;
			this.addComment("Lerne, weil mindestTalent nicht erreicht (" + this.minTalent + ")");
			this.Lerne();
		}
	}
	
	private void produce(){
		if (!this.makeStein){
			return;
		}
		
		int skillLevel = this.myStandardSkillLevel;
		
		boolean isInSteinbruch = false;
		
		// Einschub, Boni optimal nutzen
		// welche Menge können wir denn maximal abbauen...
		// skillevel +1, wenn im bergwerk
		Building b = this.scriptUnit.getUnit().getModifiedBuilding();
		if (b!=null){
			if (b.getType().getName().equalsIgnoreCase("Steinbruch")){
				skillLevel+=1;
				this.addComment("Steine, Steinbruch erkannt, Talentlevel um 1 erhöht.");
				isInSteinbruch=true;
			}
		}
		
		int Peff = 0; // effektive Personenanzahl
		
		Peff = FFToolsUnits.getPersonenEffektiv(this.scriptUnit);
		int machbareMenge = skillLevel * Peff;

		int Teiler=2;
		// wenn Rasse = Trolle, dann auf 3 / 5 gehen
		if (this.scriptUnit.getUnit().getRace().getName().equalsIgnoreCase("Trolle")){
			if (isInSteinbruch){
				Teiler = 8;
				addComment("Troll im Steinbruch erkannt, versuche Produktionsmenge zu ermitteln, die durch " + Teiler + " teilbar ist.");
			} else {
				Teiler = 4;
				addComment("Troll ohne Steinbruch erkannt, versuche Produktionsmenge zu ermitteln, die durch " + Teiler + " teilbar ist.");
			}
			
		} else {
			if (isInSteinbruch){
				// nicht-Troll im Steinbruch, Teiler bleibnt 2
				Teiler=2;
				addComment("Steinbruch erkannt, versuche Produktionsmenge zu ermitteln, die durch " + Teiler + " teilbar ist.");
			} else {
				// nicht-Troll auf freier Wildbahn
				Teiler=1;
				addComment("Produktion ohne Rassenboni und ohne Steinbruch, super. Keine Prüfung der optimalen Produktionsmenge.");
			}
		}
		int mengeResult = (machbareMenge / Teiler) * Teiler;
		if (mengeResult==machbareMenge){
			addComment("Keine Anpassung auf gut teilbare Produktionsmenge nötig, es bleibt bei " + mengeResult);
		} else {
			addComment("Anpassung auf gut teilbare Produktionsmenge erfolgt, Änderung von " + machbareMenge + " auf " + mengeResult);
		}
		machbareMenge=mengeResult;

		if (machbareMenge>0) {
			this.addOrder("machen " + machbareMenge + " Steine ;(script Steine)", true);
			FFToolsUnits.leaveAcademy(this.scriptUnit, " Steinbauer arbeitet und verlässt Aka");
		} else {
			this.addComment("daraus folgt -> ich Lerne, leider ohne Lernpool, dafür ist es jetzt zu spät..");
			this.makeStein=false;
			this.addOrder("Lerne Steinbau ;(script Steine)", true);
		}		
	}
	
	private void Lerne() {
		this.scriptUnit.addComment("Lernfix wird initialisiert mit dem Parameter: " + this.LernfixOrder);
		Script L = new Lernfix();
		ArrayList<String> order = new ArrayList<String>();
		order.add(this.LernfixOrder);
		L.setArguments(order);
		L.setScriptUnit(this.scriptUnit);
		L.setGameData(this.scriptUnit.getScriptMain().gd_ScriptMain);
		if (this.scriptUnit.getScriptMain().client!=null){
			L.setClient(this.scriptUnit.getScriptMain().client);
		}
		this.scriptUnit.addAScript(L);
	}
	
	private boolean checkSteinbruch() {
		// pre Check....werde ich im Bergwerk sein
		Building b = this.scriptUnit.getUnit().getModifiedBuilding();
		if (b==null || !b.getType().toString().equalsIgnoreCase("Steinbruch")){
			String s = "!!!Steine: ich bin nicht im Steinbruch!!!! Wenn ich wirklich abbauen soll, bitte mit steinbruch=nein  bestätigen.";
			this.doNotConfirmOrders(s);
			if (!(b==null)){
				this.addComment("Debug: ich bin nämlich in:" + b.getType().toString());
			}
			return false;
		}
		return true;
	}

}
