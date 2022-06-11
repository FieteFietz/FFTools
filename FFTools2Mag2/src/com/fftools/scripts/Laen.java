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

public class Laen extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 51;
	
	int Durchlauf_2 = 205;
	/*
	 * 14.10.2017 neu, um RdfF nach Matpool zu berücksichtgen
	 */
	

	
	private int[] runners = {Durchlauf_1,Durchlauf_2};
	
	/**
	 * ab welchem Talent gehts erst mal los?
	 */
	private int minTalent = 1;
	
	private String LernfixOrder = "Talent=Bergbau";
	
	/**
	 * soll der Bergbauer Eisen fördern, wenn er kann und kein Laen (mehr) verfügbar ist?
	 */
	private boolean Eisen = false; 
	
	/**
	 * Für Laen ist Bergwerk Pflicht, bei Eisen optional..
	 */
	private boolean Bergwerk = true;
	
	private int myStandardSkillLevel = 0;
	private boolean makeEisen=false;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Laen() {
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
			this.produceEisen();
		}

	}
	
	
	
	/**
	 * eigentlich ganz einfach: nur so lange eisen machen,
	 * bis die Talentstufe nicht mehr ausreicht
	 */
	private void start(){
		super.addVersionInfo();
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Laen");
		OP.addOptionList(this.getArguments());
		
		int unitMinTalent = OP.getOptionInt("minTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		unitMinTalent = OP.getOptionInt("mindestTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		
		this.Eisen = OP.getOptionBoolean("Eisen", this.Eisen);
		this.Bergwerk = OP.getOptionBoolean("Bergwerk", this.Bergwerk);
		
		
		
		// Eigene Talentstufe ermitteln
		int skillLevel = 0;
		SkillType skillType = this.gd_Script.getRules().getSkillType("Bergbau", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
				this.myStandardSkillLevel = skillLevel;
			} else {
				this.addComment("!!! unit has no skill Bergbau!");
			}
		} else {
			this.addComment("!!! can not get SkillType Bergbau!");
		}
		
		
		String nextJob = "Lerne";
		
		
		
		if (skillLevel>=this.minTalent){
			// Regionslevel beziehen
			Region R = this.scriptUnit.getUnit().getRegion();
			ItemType IT = this.gd_Script.getRules().getItemType("Laen");
			RegionResource RR = R.getResource(IT);
			String modeSetting = OP.getOptionString("mode");
			this.addComment("searching for automode setting, found: " + modeSetting);
			if (RR == null) {
				this.addComment("Region hat kein Laenvorkommen!!!, Lerne...");
				
				// this.addOrder("Lernen Bergbau", true);
				
				if (modeSetting.equalsIgnoreCase("auto") || modeSetting.equalsIgnoreCase("search")){
					this.addComment("AUTOmode detected....confirmed learning");
				} else {
					// this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
					nextJob = "LerneNoConfirm";
				}
			} else {
				if (RR.getSkillLevel()<=skillLevel) {
					if (modeSetting.equalsIgnoreCase("search")) {
						this.addComment("Laen in der Region bei T" + RR.getSkillLevel() + ", wir brechen die Suche ab.");
						this.doNotConfirmOrders("Laenlevel erforscht, Suche abgebrochen");
						nextJob = "noConfirm";
					} else {
						if (checkBergwerk()) {
							// weiter machen
							this.addComment("Laen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
							this.addOrder("machen Laen ; (script Laen)", true);
							nextJob = "Laen";
						} else {
							nextJob = "noConfirm";
						}
					}
				} else {
					this.addComment("Laen in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel + ", ich lerne");
					if (modeSetting.equalsIgnoreCase("auto") || modeSetting.equalsIgnoreCase("search")){
						this.addComment("AUTOmode detected....confirmed learning");
					} else {
						// this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
						nextJob = "LerneNoConfirm";
					}
				}
			}
			
			if (this.Eisen && nextJob=="Lerne") {
				this.addComment("Laen: Eisenmode erkannt. Suche nach Eisen.");
				IT = this.gd_Script.getRules().getItemType("Eisen");
				RR = R.getResource(IT);
				if (RR == null) {
					this.addComment("Region hat kein Eisen, es bleibt beim Lernen...");
				} else {
					if (RR.getSkillLevel()<=skillLevel) {
						if (this.Bergwerk) { 
							if (checkBergwerk()) {
								// weiter machen
								this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
								// this.addOrder("machen Eisen ;(script Laen, mode Eisen)", true);
								nextJob = "Eisen";
								this.makeEisen=true;
							} else {
								nextJob = "noConfirm";
							}
						} else {
							this.addComment("Laen: Prüfung auf Bergwerk für Eisenförderung ist deaktiviert");
							// weiter machen
							this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
							// this.addOrder("machen Eisen ; (script Laen, mode Eisen)", true);
							nextJob = "Eisen";
							this.makeEisen=true;
						}
					} else {
						this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel + ", ich lerne");
						if (modeSetting.equalsIgnoreCase("auto")){
							this.addComment("Laen: Eisen-AUTOmode detected....confirmed learning");
						} else {
							// this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
							nextJob = "LerneNoConfirm";
						}
					}
				}
				
			}
			
			if (nextJob=="Lerne") {
				this.Lerne();
			}
			if (nextJob=="LerneNoConfirm") {
				this.Lerne();
				this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
			}
			
			
			
			
			
			
			
		} else {
			// this.doNotConfirmOrders();
			this.addComment("Lerne, weil mindestTalent nicht erreicht (" + this.minTalent + ")");
			nextJob = "Lerne";
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
	
	private boolean checkBergwerk() {
		// pre Check....werde ich im Bergwerk sein
		Building b = this.scriptUnit.getUnit().getModifiedBuilding();
		if (b==null || !b.getType().toString().equalsIgnoreCase("Bergwerk")){
			this.doNotConfirmOrders("!!!Laen: ich bin nicht im Bergwerk!!!!");
			if (!(b==null)){
				this.addComment("Debug: ich bin nämlich in:" + b.getType().toString());
			}
			return false;
		}
		return true;
	}
	
	private void produceEisen(){
		
		if (!this.makeEisen) {
			return;
		}
		
		
		int skillLevel = this.myStandardSkillLevel;
		
		boolean isInBergwerk = false;
		
		// Einschub, Boni optimal nutzen
		// welche Menge können wir denn maximal abbauen...
		// skillevel +1, wenn im bergwerk
		Building b = this.scriptUnit.getUnit().getModifiedBuilding();
		if (b!=null){
			if (b.getType().getName().equalsIgnoreCase("Bergwerk")){
				skillLevel+=1;
				this.addComment("Laen(Eisen), Bergwerk erkannt, Talentlevel um 1 erhöht.");
				isInBergwerk=true;
			}
		}
		
		int Peff = 0; // effektive Personenanzahl
		Peff = FFToolsUnits.getPersonenEffektiv(this.scriptUnit);
		
		int machbareMenge = skillLevel * Peff;
		
		int Teiler=1;
		// wenn Rasse = Zwerge, dann auf 10 / 5 gehen
		if (this.scriptUnit.getUnit().getRace().getName().equalsIgnoreCase("Zwerge")){
			if (isInBergwerk){
				Teiler = 10;  // 0.3 mal Produktionsmenge wird vom Vorrat abgezogen, nur bei X x 10 ohne Verlust
				addComment("Zwerg im Bergwerk erkannt, versuche Produktionsmenge zu ermitteln, die durch " + Teiler + " teilbar ist.");
			} else {
				Teiler = 5;
				addComment("Zwerg ohne Bergwerk erkannt, versuche Produktionsmenge zu ermitteln, die durch " + Teiler + " teilbar ist.");
			}
			
		} else {
			if (isInBergwerk){
				// Nicht zwerg im Bergwerk
				Teiler=2;
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
			this.addOrder("machen " + machbareMenge + " Eisen ;(script Eisen)", true);
			FFToolsUnits.leaveAcademy(this.scriptUnit, " Bergbauer baut Laen ab und verlässt Aka");
		} else {
			this.addComment("daraus folgt -> ich Lerne, leider ohne Lernpool, dafür ist es jetzt zu spät..");
			
			this.addOrder("Lerne Bergbau ;(script Laen(Eisen))", true);
		}
	}

}
