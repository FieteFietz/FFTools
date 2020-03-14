package com.fftools.scripts;

import java.util.ArrayList;

import sun.dc.DuctusRenderingEngine;

import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.RegionResource;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

public class Lumberjack extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 51;
	/*
	 * 13.12.2015 war 135...gesetzt auf 51, um vor Lernfix zu sein
	 */
	int Durchlauf_2 = 205;
	/*
	 * 08.10.2017 neu, damit wir wissen, ob wir RdfF // Schaffenstrunk bekommen
	 * 
	 */
	

	
	private int[] runners = {Durchlauf_1,Durchlauf_2};
	
	private boolean isLearning=false;
	private int possibleHolzMengeRegion = 0;
	private int skillLevel = 0;
	private String Gut="Holz";
	
	/**
	 * ab welchem Talent gehts erst mal los?
	 */
	private int minTalent = 1;
	
	private String LernfixOrder = "Talent=Holzfällen";
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Lumberjack() {
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
			this.makeProd();
		}

	}
	
	
	
	/**
	 * eigentlich ganz einfach: nur so lange holz machen,
	 * bis Holz alle oder minBestand erreicht oder minMenge nicht erreicht
	 */
	private void start(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Lumberjack");
		OP.addOptionList(this.getArguments());
		
		int unitMinTalent = OP.getOptionInt("minTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		unitMinTalent = OP.getOptionInt("mindestTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		
		// Eigene Talentstufe ermitteln
		this.skillLevel = 0;
		SkillType skillType = this.gd_Script.getRules().getSkillType("Holzfällen", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
			} else {
				this.addComment("!!! unit has no skill Holzfällen!");
			}
		} else {
			this.addComment("!!! can not get SkillType Holzfällen!");
		}
		if (skillLevel>=this.minTalent){
			// Regionslevel beziehen
			Region R = this.scriptUnit.getUnit().getRegion();
			int Menge=0;
			this.Gut = "Holz";
			ItemType IT = this.gd_Script.getRules().getItemType("Holz");
			RegionResource RR = R.getResource(IT);
			int actM=0;
			if (RR!=null){
				actM=0;
				actM = RR.getAmount();
				if (actM>0){
					this.addComment("Lumberjack: " + actM + " Holz gefunden...");
					Menge+=actM;
				}
			}
			// IT = this.gd_Script.rules.getItemType("Bäume");
			IT = this.gd_Script.getRules().getItemType("Bäume");
			RR = R.getResource(IT);
			if (RR!=null){
				actM=0;
				actM = RR.getAmount();
				if (actM>0){
					this.addComment("Lumberjack: " + actM + " Bäume gefunden...");
					Menge+=actM;
				}
			}
			// IT = this.gd_Script.rules.getItemType("Schößlinge");
			IT = this.gd_Script.getRules().getItemType("Schößlinge");
			RR = R.getResource(IT);
			if (RR!=null){
				actM=0;
				actM = RR.getAmount();
				if (actM>0){
					this.addComment("Lumberjack: " + actM + " Schößlinge gefunden...");
					Menge+=actM;
				}
			}
			// IT = this.gd_Script.rules.getItemType("Mallorn");
			IT = this.gd_Script.getRules().getItemType("Mallorn");
			RR = R.getResource(IT);
			if (RR!=null){
				actM=0;
				actM = RR.getAmount();
				if (actM>0){
					this.addComment("Lumberjack: " + actM + " Mallorn gefunden...");
					Menge+=actM;
					this.Gut="Mallorn";
				}
			}
			// IT = this.gd_Script.rules.getItemType("Mallornschößlinge");
			IT = this.gd_Script.getRules().getItemType("Mallornschößlinge");
			RR = R.getResource(IT);
			if (RR!=null){
				actM=0;
				actM = RR.getAmount();
				if (actM>0){
					this.addComment("Lumberjack: " + actM + " Mallornschößlinge gefunden...");
					Menge+=actM;
					this.Gut="Mallorn";
				}
			}
			
			
			
			if (Menge>0){
				this.addComment("Lumberjack: Arbeit ("+ Menge +") vorhanden.");
				
				
				int minMenge=OP.getOptionInt("minMenge", 0);
				int minBestand=OP.getOptionInt("minBestand", 0);
				
				if (minBestand>0){
					addComment("Lumberjack: Berücksichtige Mindestbestand (" + minBestand + ")");
					Menge -= minBestand;
					if (Menge<0){
						Menge=0;
					}
					addComment("Lumberjack: zu schlagende Menge nun: " + Menge);
				}
				
				
				// 20171008: hiert einschub unter mindestMenge -> lernen, produktion später
				possibleHolzMengeRegion = Menge;
				
				if (Menge<minMenge){
					addComment("Lumberjack: zu schlagende Menge (" + Menge + ") ist unter Mindestmenge (" + minMenge + ")...keine Arbeit.");
					this.addComment("Lumberjack: kein Einschlag. Lerne.");
					this.Lerne();
					this.isLearning=true;
				}	
			} 
			
			if (Menge<=0) {
				this.addComment("nix zum Schlagen gefunden, muss lernen...");
				this.Lerne();
				this.isLearning=true;
			}
		} else {
			this.isLearning=true;
			// this.doNotConfirmOrders();
			this.addComment("Lerne, weil mindestTalent nicht erreicht (" + this.minTalent + ")");
			this.Lerne();
		}
		
	}
	
	private void makeProd(){
		// 20171008: Begrenzung auf machbares Talent
		// Sägewerk feststellen
		
		if (this.isLearning){
			return ;
		}
		
		boolean imSägewerk = false;
		Building b = this.scriptUnit.getUnit().getModifiedBuilding();
		if (b != null){
			if (b.getType().getName().equalsIgnoreCase("Sägewerk")){
				imSägewerk=true;
			} else {
				this.addComment("Lumberjack: ich bin nicht in einem Sägewerk ?!");
			}
		} else {
			this.addComment("Lumberjack: ich bin nicht in einem Sägewerk ?! (noch nicht mal in irgendeinem Gebäude....");
		}
		
		int relevantLevel = skillLevel;
		if (imSägewerk){
			relevantLevel +=1;
		}
		
		int Menge = this.possibleHolzMengeRegion;
		
		int machbareMenge = relevantLevel * this.scriptUnit.getUnit().getModifiedPersons();
		
		// wenn es sich um Mallorn handelt, das braucht minTalent=2 !! halbierte Produktionsmenge
		if (this.Gut=="Mallorn"){
			machbareMenge *= 2;
			this.addComment("Lumberjack: Mallorn soll geschlagen werden. Mögliche Produktion halbiert auf: " + machbareMenge);
		}
		
		// Schaffenstrunk oder RdF verdoppeln prodPoints 
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			machbareMenge *= 2;
			this.addComment("Lumberjack: Einheit nutzt Schaffenstrunk. Produktion verdoppelt auf: " + machbareMenge);
		} 
		
		
		// 20170708: berücksichtigung von RdfF
		ItemType rdfType=this.gd_Script.rules.getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = this.scriptUnit.getModifiedItem(rdfType);
			if (rdfItem!=null && rdfItem.getAmount()>0){
				// Aufteilung der Personen in mit und ohne Ring
				int PersonenMitRing = Math.min(rdfItem.getAmount(),this.scriptUnit.getUnit().getModifiedPersons());
				int PersonenOhneRing = this.scriptUnit.getUnit().getModifiedPersons() - PersonenMitRing;
				int RingLevel = relevantLevel;
				if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
					RingLevel *= 2;
				}
				int RingMenge = PersonenOhneRing * RingLevel;
				RingMenge += PersonenMitRing * RingLevel * 10;
				addComment("RdfF berücksichtigt, max Produktion geändert von " + machbareMenge + " auf " + RingMenge + " (" + PersonenMitRing + " Personen mit Ring erkannt.)");
				machbareMenge = RingMenge;
			} else {
				this.addComment("(debug-lumberjack: keine RdfF erkannt)");
				
			}
		} else {
			this.addComment("Lumberjack: RdfF ist noch völlig unbekannt.");
		}
		
		
		
		
		if (machbareMenge<Menge){
			addComment("Begrenzung der Produktionsmenge durch das Talent der Einheit, Änderung von " + Menge + " auf " + machbareMenge);
			Menge = machbareMenge;
		} else {
			addComment("Talent der Einheit lässt volle Produktionsmenge zu (möglich sind " + machbareMenge + ")");
		}
		
		
		
		
		// 20171008: im Sägewerk, gerade Menge berücksichtigen
		boolean notImportantChange=false;
		if (imSägewerk){
			// checken, ob Menge durch 2 Teilbar ist.
			int Teiler=2;
			int mengeResult = (Menge / Teiler) * Teiler;
			if (mengeResult==Menge){
				addComment("Arbeiten im Sägewerk erkannt, aber keine Anpassung auf gerade Produktionsmenge nötig, es bleibt bei " + mengeResult);
			} else {
				addComment("Arbeiten im Sägewerk erkannt, Anpassung auf gerade Produktionsmenge erfolgt, Änderung von " + Menge + " auf " + mengeResult);
				notImportantChange=true;
			}
			Menge=mengeResult;
		}
		
		if (Menge>0){
			this.addOrder("MACHE " + Menge + " " + this.Gut, true);
		} else {
			if (notImportantChange) {
				// zu spät für Lernfix
				this.addOrder("Lernen Holzfällen", true);
				this.addComment("Lernen ohne Lernpool - zu spät entschieden");
				this.isLearning=true;
			} else {
				this.doNotConfirmOrders("!!! Lumberjack: unerwartete zu produzierende Menge ist 0 !!! (Einheit unbestätigt)");
			}
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
