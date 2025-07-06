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

public class Eisen extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 51;
	/*
	 * 13.12.2015 war 135...gesetzt auf 51, um vor Lernfix zu sein
	 */
	
	int Durchlauf_2 = 205;
	/*
	 * 14.10.2017 neu, um RdfF nach Matpool zu berücksichtgen
	 */
	

	
	private int[] runners = {Durchlauf_1,Durchlauf_2};
	
	private boolean makeEisen=false;
	private int myStandardSkillLevel = 0;
	
	/**
	 * ab welchem Talent gehts erst mal los?
	 */
	private int minTalent = 1;
	
	private String LernfixOrder = "Talent=Bergbau";
	
	private ArrayList<String> LernFixArguments = new ArrayList<String>();
	
	/**
	 * soll der Bergbauer Laen fördern, wenn er kann und kein Eisen (mehr) verfügbar ist?
	 */
	private boolean Laen = false; 
	
	/**
	 * Für Laen ist Bergwerk Pflicht, bei Eisen optional..
	 */
	private boolean Bergwerk = true;
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Eisen() {
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
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Eisen");
		OP.addOptionList(this.getArguments());
		
		int unitMinTalent = OP.getOptionInt("minTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		unitMinTalent = OP.getOptionInt("mindestTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		
		this.Laen = OP.getOptionBoolean("Laen", this.Laen);
		this.Bergwerk = OP.getOptionBoolean("Bergwerk", this.Bergwerk);
		
		// ggf Optionen für Lernfix abfragen
		if (!OP.getOptionBoolean("aka", true)) {
			this.LernFixArguments.add("aka=nein");
		}
		if (OP.getOptionInt("Ziel", -1)>0) {
			this.LernFixArguments.add("Ziel=" + OP.getOptionInt("Ziel", 1));
		}
		if (OP.getOptionString("Lehrer").length()>1) {
			this.LernFixArguments.add("Lehrer=" + OP.getOptionString("Lehrer"));
		}
		if (OP.getOptionString("Teacher").length()>1) {
			this.LernFixArguments.add("Teacher=" + OP.getOptionString("Teacher"));
		}
		// wenn Lernplan gesetzt wird, diesen Nutzen
		if (OP.getOptionString("Lernplan").length()>1) {
			this.LernfixOrder = "Lernplan=" + OP.getOptionString("Lernplan"); 
		}
		
		// Eigene Talentstufe ermitteln
		int skillLevel = 0;
		SkillType skillType = this.gd_Script.getRules().getSkillType("Bergbau", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
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
			ItemType IT = this.gd_Script.getRules().getItemType("Eisen");
			RegionResource RR = R.getResource(IT);
			String modeSetting = OP.getOptionString("mode");
			this.addComment("searching for automode setting, found: " + modeSetting);
			if (RR == null) {
				this.addComment("Region hat kein Eisenvorkommen!!!");
				// this.addComment("Ergänze Lernfix Eintrag mit Talent=Bergbau");

				if (modeSetting.equalsIgnoreCase("auto") || modeSetting.equalsIgnoreCase("search")){
					this.addComment("AUTOmode detected....confirmed learning");
				} else {
					// this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
					nextJob = "LerneNoConfirm";
				}
			} else {
				if (RR.getSkillLevel()<=skillLevel) {
					if (modeSetting.equalsIgnoreCase("search")) {
						// erforschung abgeschlossen - abbrechen
						this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir brechen die Suche ab.");
						this.doNotConfirmOrders("Eisenlevel erforscht, Suche abgebrochen");
						this.makeEisen=false;
						nextJob="noConfirm" ;
					} else {
						// weiter machen
						if (this.Bergwerk) {
							if (checkBergwerk()) {
								this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
								this.makeEisen=true;
								this.myStandardSkillLevel = skillLevel;
								nextJob="Eisen" ;
							}
						} else {
							this.addComment("Eisen: Prüfung auf Bergwerk ist deaktiviert.");
							this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
							this.makeEisen=true;
							this.myStandardSkillLevel = skillLevel;
							nextJob="Eisen" ;
						}
						
					}
				} else {
					this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel);
					this.addComment("Ergänze Lernfix Eintrag mit Talent=Bergbau");
					// this.addOrder("Lernen Bergbau", true);
					// this.Lerne();
					if (modeSetting.equalsIgnoreCase("auto") || modeSetting.equalsIgnoreCase("search")){
						this.addComment("AUTOmode detected....confirmed learning");
					} else {
						// this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
						nextJob = "LerneNoConfirm";
					}
				}
			}
			
			// Sondereinschub - wir sind auf search und treffen auf Laen statt Eisen
			if (modeSetting.equalsIgnoreCase("search") && !this.Laen) {
				// kurzer Check, ob wir Laen sehen
				IT = this.gd_Script.getRules().getItemType("Laen");
				RR = R.getResource(IT);
				if (RR!=null) {
					if (RR.getSkillLevel()<=skillLevel) {
						this.addComment("!!!Laen!!! in der Region bei T" + RR.getSkillLevel() + ", wir brechen die Suche ab.");
						this.doNotConfirmOrders("Laen gefunden, Suche abgebrochen");
						this.makeEisen=false;
						nextJob = "noConfim";
					} else {
						this.addComment("search: Laen hier auf höherem Niveau.");
					}
				} else {
					this.addComment("search: kein Laen hier bekannt.");
				}
			}
			
			if (this.Laen && nextJob=="Lerne") {
				this.makeEisen=false;
				IT = this.gd_Script.getRules().getItemType("Laen");
				RR = R.getResource(IT);
				this.addComment("Eisen: Laenmode erkannt. Suche nach Laen.");
				if (RR == null) {
					this.addComment("Region hat kein Laen, es bleibt beim Lernen...");
				} else {
					if (RR.getSkillLevel()<=skillLevel) {
						if (checkBergwerk()) {
							// weiter machen
							this.addComment("Laen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
							this.addOrder("machen Laen ;(script Eisen, mode Laen)", true);
							FFToolsUnits.leaveAcademy(this.scriptUnit, " arbeitender Bergbauer verlässt Aka");
							nextJob = "Laen";
							
						} 
					} else {
						this.addComment("Laen in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel + ", ich lerne");
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
				this.makeEisen=false;
			}
			if (nextJob=="LerneNoConfirm") {
				this.Lerne();
				this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
				this.makeEisen=false;
			}
			
			
		} else {
			this.Lerne();
			// this.doNotConfirmOrders();
			this.makeEisen=false;
			this.addComment("Lerne, weil mindestTalent nicht erreicht (" + this.minTalent + ")");
		}
		
	}
	
	private void produce(){
		if (!this.makeEisen){
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
				this.addComment("Eisen, Bergwerk erkannt, Talentlevel um 1 erhöht.");
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
			FFToolsUnits.leaveAcademy(this.scriptUnit, " arbeitender Bergbauer verlässt Aka");
		} else {
			this.addComment("daraus folgt -> ich Lerne, leider ohne Lernpool, dafür ist es jetzt zu spät..");
			this.makeEisen=false;
			this.addOrder("Lerne Bergbau ;(script Eisen)", true);
		}
	}
	
	private void Lerne() {
		this.scriptUnit.addComment("Lernfix wird initialisiert mit dem Parameter: " + this.LernfixOrder);
		Script L = new Lernfix();
		ArrayList<String> order = new ArrayList<String>();
		order.add(this.LernfixOrder);
		if (this.LernFixArguments!=null && this.LernFixArguments.size()>0) {
			order.addAll(this.LernFixArguments);
			this.scriptUnit.addComment("weitere Lernfix Parameter: " + String.join(" ", this.LernFixArguments));
		}
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
			String s = "!!!Eisen: ich bin nicht im Bergwerk!!!! Wenn ich wirklich abbauen soll, bitte mit bergwerk=nein  bestätigen.";
			if (this.Laen) {
				s = "!!!Eisen (Modus Laen): ich bin nicht im Bergwerk!!!! (Und das ist zwingend notwendig)";
			}
			this.doNotConfirmOrders(s);
			if (!(b==null)){
				this.addComment("Debug: ich bin nämlich in:" + b.getType().toString());
			}
			return false;
		}
		return true;
	}

}
