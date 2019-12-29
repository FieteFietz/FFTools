package com.fftools.scripts;

import java.util.ArrayList;

import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.RegionResource;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

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
		int unitMinTalent = OP.getOptionInt("minTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		unitMinTalent = OP.getOptionInt("mindestTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
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
		if (skillLevel>=this.minTalent){
			// Regionslevel beziehen
			Region R = this.scriptUnit.getUnit().getRegion();
			ItemType IT = this.gd_Script.getRules().getItemType("Eisen");
			RegionResource RR = R.getResource(IT);
			String modeSetting = OP.getOptionString("mode");
			this.addComment("searching for automode setting, found: " + modeSetting);
			if (RR == null) {
				this.addComment("Region hat kein Eisenvorkommen!!!");
				this.addComment("Ergänze Lernfix Eintrag mit Talent=Bergbau");
				// this.addOrder("Lernen Bergbau", true);
				Script L = new Lernfix();
				ArrayList<String> order = new ArrayList<String>();
				order.add("Talent=Bergbau");
				L.setArguments(order);
				L.setScriptUnit(this.scriptUnit);
				L.setGameData(this.gd_Script);
				if (this.scriptUnit.getScriptMain().client!=null){
					L.setClient(this.scriptUnit.getScriptMain().client);
				}
				this.scriptUnit.addAScript(L);
				
				
				if (modeSetting.equalsIgnoreCase("auto") || modeSetting.equalsIgnoreCase("search")){
					this.addComment("AUTOmode detected....confirmed learning");
				} else {
					this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
				}
			} else {
				if (RR.getSkillLevel()<=skillLevel) {
					if (modeSetting.equalsIgnoreCase("search")) {
						// erforschung abgeschlossen - abbrechen
						this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir brechen die Suche ab.");
						this.doNotConfirmOrders("Eisenlevel erforscht, Suche abgebrochen");
						this.makeEisen=false;
					} else {
						// weiter machen
						this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
						this.makeEisen=true;
						this.myStandardSkillLevel = skillLevel;
					}
				} else {
					this.addComment("Eisen in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel);
					this.addComment("Ergänze Lernfix Eintrag mit Talent=Bergbau");
					// this.addOrder("Lernen Bergbau", true);
					this.Lerne();
					if (modeSetting.equalsIgnoreCase("auto") || modeSetting.equalsIgnoreCase("search")){
						this.addComment("AUTOmode detected....confirmed learning");
					} else {
						this.doNotConfirmOrders("no AUTOmode detected....pls confirm learning / adjust orders");
					}
				}
			}
			
			// Sondereinschub - wir sind auf search und treffen auf Laen statt Eisen
			if (modeSetting.equalsIgnoreCase("search")) {
				// kurzer Check, ob wir Laen sehen
				IT = this.gd_Script.getRules().getItemType("Laen");
				RR = R.getResource(IT);
				if (RR!=null) {
					if (RR.getSkillLevel()<=skillLevel) {
						this.addComment("!!!Laen!!! in der Region bei T" + RR.getSkillLevel() + ", wir brechen die Suche ab.");
						this.doNotConfirmOrders("Laen gefunden, Suche abgebrochen");
						this.makeEisen=false;
					} else {
						this.addComment("search: Laen hier auf höherem Niveau.");
					}
				} else {
					this.addComment("search: kein Laen hier bekannt.");
				}
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
		
		int machbareMenge = skillLevel * this.scriptUnit.getUnit().getModifiedPersons();
		
		// Schaffenstrunk oder RdF verdoppeln prodPoints 
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			machbareMenge *= 2;
			this.addComment("Eisen: Einheit nutzt Schaffenstrunk. Produktion verdoppelt auf: " + machbareMenge);
		} 
		
		
		// 20170708: berücksichtigung von RdfF
		ItemType rdfType=this.gd_Script.getRules().getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = this.scriptUnit.getModifiedItem(rdfType);
			if (rdfItem!=null && rdfItem.getAmount()>0){
				// Aufteilung der Personen in mit und ohne Ring
				int PersonenMitRing = Math.min(rdfItem.getAmount(),this.scriptUnit.getUnit().getModifiedPersons());
				int PersonenOhneRing = this.scriptUnit.getUnit().getModifiedPersons() - PersonenMitRing;
				int RingLevel = skillLevel;
				if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
					RingLevel *= 2;
				}
				int RingMenge = PersonenOhneRing * RingLevel;
				RingMenge += PersonenMitRing * RingLevel * 10;
				addComment("RdfF berücksichtigt, max Produktion geändert von " + machbareMenge + " auf " + RingMenge + " (" + PersonenMitRing + " Personen mit Ring erkannt.)");
				machbareMenge = RingMenge;
			} else {
				this.addComment("(debug-Eisen: keine RdfF erkannt)");
				
			}
		} else {
			this.addComment("Eisen: RdfF ist noch völlig unbekannt.");
		}
		
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
		L.setArguments(order);
		L.setScriptUnit(this.scriptUnit);
		L.setGameData(this.scriptUnit.getScriptMain().gd_ScriptMain);
		if (this.scriptUnit.getScriptMain().client!=null){
			L.setClient(this.scriptUnit.getScriptMain().client);
		}
		this.scriptUnit.addAScript(L);
	}

}
