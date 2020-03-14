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
	
	private String LernfixOrder = "Talent=Holzf�llen";
	
	
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
		SkillType skillType = this.gd_Script.getRules().getSkillType("Holzf�llen", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
			} else {
				this.addComment("!!! unit has no skill Holzf�llen!");
			}
		} else {
			this.addComment("!!! can not get SkillType Holzf�llen!");
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
			// IT = this.gd_Script.rules.getItemType("B�ume");
			IT = this.gd_Script.getRules().getItemType("B�ume");
			RR = R.getResource(IT);
			if (RR!=null){
				actM=0;
				actM = RR.getAmount();
				if (actM>0){
					this.addComment("Lumberjack: " + actM + " B�ume gefunden...");
					Menge+=actM;
				}
			}
			// IT = this.gd_Script.rules.getItemType("Sch��linge");
			IT = this.gd_Script.getRules().getItemType("Sch��linge");
			RR = R.getResource(IT);
			if (RR!=null){
				actM=0;
				actM = RR.getAmount();
				if (actM>0){
					this.addComment("Lumberjack: " + actM + " Sch��linge gefunden...");
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
			// IT = this.gd_Script.rules.getItemType("Mallornsch��linge");
			IT = this.gd_Script.getRules().getItemType("Mallornsch��linge");
			RR = R.getResource(IT);
			if (RR!=null){
				actM=0;
				actM = RR.getAmount();
				if (actM>0){
					this.addComment("Lumberjack: " + actM + " Mallornsch��linge gefunden...");
					Menge+=actM;
					this.Gut="Mallorn";
				}
			}
			
			
			
			if (Menge>0){
				this.addComment("Lumberjack: Arbeit ("+ Menge +") vorhanden.");
				
				
				int minMenge=OP.getOptionInt("minMenge", 0);
				int minBestand=OP.getOptionInt("minBestand", 0);
				
				if (minBestand>0){
					addComment("Lumberjack: Ber�cksichtige Mindestbestand (" + minBestand + ")");
					Menge -= minBestand;
					if (Menge<0){
						Menge=0;
					}
					addComment("Lumberjack: zu schlagende Menge nun: " + Menge);
				}
				
				
				// 20171008: hiert einschub unter mindestMenge -> lernen, produktion sp�ter
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
		// S�gewerk feststellen
		
		if (this.isLearning){
			return ;
		}
		
		boolean imS�gewerk = false;
		Building b = this.scriptUnit.getUnit().getModifiedBuilding();
		if (b != null){
			if (b.getType().getName().equalsIgnoreCase("S�gewerk")){
				imS�gewerk=true;
			} else {
				this.addComment("Lumberjack: ich bin nicht in einem S�gewerk ?!");
			}
		} else {
			this.addComment("Lumberjack: ich bin nicht in einem S�gewerk ?! (noch nicht mal in irgendeinem Geb�ude....");
		}
		
		int relevantLevel = skillLevel;
		if (imS�gewerk){
			relevantLevel +=1;
		}
		
		int Menge = this.possibleHolzMengeRegion;
		
		int machbareMenge = relevantLevel * this.scriptUnit.getUnit().getModifiedPersons();
		
		// wenn es sich um Mallorn handelt, das braucht minTalent=2 !! halbierte Produktionsmenge
		if (this.Gut=="Mallorn"){
			machbareMenge *= 2;
			this.addComment("Lumberjack: Mallorn soll geschlagen werden. M�gliche Produktion halbiert auf: " + machbareMenge);
		}
		
		// Schaffenstrunk oder RdF verdoppeln prodPoints 
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			machbareMenge *= 2;
			this.addComment("Lumberjack: Einheit nutzt Schaffenstrunk. Produktion verdoppelt auf: " + machbareMenge);
		} 
		
		
		// 20170708: ber�cksichtigung von RdfF
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
				addComment("RdfF ber�cksichtigt, max Produktion ge�ndert von " + machbareMenge + " auf " + RingMenge + " (" + PersonenMitRing + " Personen mit Ring erkannt.)");
				machbareMenge = RingMenge;
			} else {
				this.addComment("(debug-lumberjack: keine RdfF erkannt)");
				
			}
		} else {
			this.addComment("Lumberjack: RdfF ist noch v�llig unbekannt.");
		}
		
		
		
		
		if (machbareMenge<Menge){
			addComment("Begrenzung der Produktionsmenge durch das Talent der Einheit, �nderung von " + Menge + " auf " + machbareMenge);
			Menge = machbareMenge;
		} else {
			addComment("Talent der Einheit l�sst volle Produktionsmenge zu (m�glich sind " + machbareMenge + ")");
		}
		
		
		
		
		// 20171008: im S�gewerk, gerade Menge ber�cksichtigen
		boolean notImportantChange=false;
		if (imS�gewerk){
			// checken, ob Menge durch 2 Teilbar ist.
			int Teiler=2;
			int mengeResult = (Menge / Teiler) * Teiler;
			if (mengeResult==Menge){
				addComment("Arbeiten im S�gewerk erkannt, aber keine Anpassung auf gerade Produktionsmenge n�tig, es bleibt bei " + mengeResult);
			} else {
				addComment("Arbeiten im S�gewerk erkannt, Anpassung auf gerade Produktionsmenge erfolgt, �nderung von " + Menge + " auf " + mengeResult);
				notImportantChange=true;
			}
			Menge=mengeResult;
		}
		
		if (Menge>0){
			this.addOrder("MACHE " + Menge + " " + this.Gut, true);
		} else {
			if (notImportantChange) {
				// zu sp�t f�r Lernfix
				this.addOrder("Lernen Holzf�llen", true);
				this.addComment("Lernen ohne Lernpool - zu sp�t entschieden");
				this.isLearning=true;
			} else {
				this.doNotConfirmOrders("!!! Lumberjack: unerwartete zu produzierende Menge ist 0 !!! (Einheit unbest�tigt)");
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
