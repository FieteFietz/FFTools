package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Building;
import magellan.library.Item;
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
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
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
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Steine");
		
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
		SkillType skillType = this.gd_Script.rules.getSkillType("Steinbau", false);
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
			ItemType IT = this.gd_Script.rules.getItemType("Steine");
			RegionResource RR = R.getResource(IT);
			if (RR == null) {
				this.addComment("Region hat kein Steinvorkommen!!!");
				this.addComment("Ergänze Lernfix Eintrag mit Talent=Steinbau");
				// this.addOrder("Lernen Bergbau", true);
				Script L = new Lernfix();
				ArrayList<String> order = new ArrayList<String>();
				order.add("Talent=Steinbau");
				L.setArguments(order);
				L.setScriptUnit(this.scriptUnit);
				L.setGameData(this.gd_Script);
				if (this.scriptUnit.getScriptMain().client!=null){
					L.setClient(this.scriptUnit.getScriptMain().client);
				}
				this.scriptUnit.addAScript(L);
				
				String modeSetting = OP.getOptionString("mode");
				this.addComment("searching for automode setting, found: " + modeSetting);
				if (modeSetting.equalsIgnoreCase("auto")){
					this.addComment("AUTOmode detected....confirmed learning");
				} else {
					this.addComment("no AUTOmode detected....pls confirm learning / adjust orders");
					this.doNotConfirmOrders();
				}
			} else {
				if (RR.getSkillLevel()<=skillLevel) {
					// weiter machen
					this.addComment("Steine in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
					// this.addOrder("machen Stein ;(script Steine)", true);
					this.myStandardSkillLevel = skillLevel;
					this.makeStein=true;
				} else {
					this.addComment("Steine in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel);
					this.addComment("Ergänze Lernfix Eintrag mit Talent=Steinbau");
					// this.addOrder("Lernen Bergbau", true);
					Script L = new Lernfix();
					ArrayList<String> order = new ArrayList<String>();
					order.add("Talent=Steinbau");
					L.setArguments(order);
					L.setScriptUnit(this.scriptUnit);
					L.setGameData(this.gd_Script);
					if (this.scriptUnit.getScriptMain().client!=null){
						L.setClient(this.scriptUnit.getScriptMain().client);
					}
					this.scriptUnit.addAScript(L);
					String modeSetting = OP.getOptionString("mode");
					this.addComment("searching for automode setting, found: " + modeSetting);
					if (modeSetting.equalsIgnoreCase("auto")){
						this.addComment("AUTOmode detected....confirmed learning");
					} else {
						this.addComment("no AUTOmode detected....pls confirm learning / adjust orders");
						this.doNotConfirmOrders();
					}
				}
			}
		} else {
			this.addOrder("Lernen Steinbau", true);
			// this.doNotConfirmOrders();
			this.makeStein=false;
			this.addComment("Lerne, weil mindestTalent nicht erreicht (" + this.minTalent + ")");
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
		
		int machbareMenge = skillLevel * this.scriptUnit.getUnit().getModifiedPersons();
		
		// Schaffenstrunk oder RdF verdoppeln prodPoints 
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			machbareMenge *= 2;
			this.addComment("Eisen: Einheit nutzt Schaffenstrunk. Produktion verdoppelt auf: " + machbareMenge);
		} 
		
		
		// 20170708: berücksichtigung von RdfF
		ItemType rdfType=this.gd_Script.rules.getItemType("Ring der flinken Finger",false);
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
				this.addComment("(debug-Steine: keine RdfF erkannt)");
				
			}
		} else {
			this.addComment("Steine: RdfF ist noch völlig unbekannt.");
		}
		
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
		this.addOrder("machen " + machbareMenge + " Stein ;(script Steine)", true);
		
	}

}
