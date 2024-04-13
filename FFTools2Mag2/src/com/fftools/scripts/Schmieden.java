package com.fftools.scripts;

import java.util.Iterator;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;

import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

/**
 * 
 * Eine erste abgespeckte Version zur Produktion
 * Waffen und Rüstungen
 * @author Fiete
 *
 */

public class Schmieden extends MatPoolScript{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf_vorMatPool = 350;
	private int Durchlauf_nachMatPool = 430;
	
	private int[] runners = {Durchlauf_vorMatPool,Durchlauf_nachMatPool};
	
	/**
	 * herzustellendes Object
	 */
	private ItemType itemType = null;
	
	/**
	 * basisPrio zur Anforderung von Eisen = Laen 
	 */
	private int eisenPrio = 500;
	/**
	 * basisPrio zur Anforderung von Holz
	 */
	private int holzPrio = 500;
	
	/**
	 * basisPrio zur Anforderung von Steinen
	 */
	private int steinPrio = 500;
	
	
	/**
	 * Anzahl Vorratsrunden
	 */
	private int vorratsRunden = 3;
	
	/**
	 * minimale Auslastung
	 */
	private int minAuslastung = 75;
	
	/**
	 * prioAnforderungsbonus für Schmiedeinsassen
	 */
	private int schmiedeBonus = 25;
	
	/**
	 * Eisen/Laen Request für diese Runde
	 */
	private MatPoolRequest eisenRequest = null;
	
	/**
	 * Holzrequest für diese Runde
	 */
	private MatPoolRequest holzRequest = null;
	
	/**
	 * Steinrequest für diese Runde
	 */
	private MatPoolRequest steinRequest = null;
	
	
	/**
	 * SkillType benötigt für Herstellung der Ware
	 */
	private SkillType neededSkillType = null;
	private int neededSkillLevel=0;
	
	/**
	 * vor MatPool maximale Produktionsmenge
	 */
	private int maxProduction = 0;
	
	boolean isInSchmiede = false;
	
	private int setMenge = 0;
	
	/**
	 * auf Wunsch von Jutta
	 * Talent optional machen
	 */
	private String LernTalent = ""; 
	
	// Konstruktor
	public Schmieden() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_vorMatPool){
			this.vorMatPool();
		}
        

		if (scriptDurchlauf==Durchlauf_nachMatPool){
			this.nachMatPool();
		}
		
	}
	
	
	private void vorMatPool(){
		super.addVersionInfo();		
		// FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Schmieden");
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Optionen lesen und Prüfen
		// Ware
		String warenName = OP.getOptionString("Ware");
		ItemType actItemType = this.gd_Script.getRules().getItemType(warenName, false);
		if (actItemType!=null){
			this.itemType = actItemType;
		} else {
			this.scriptUnit.doNotConfirmOrders("!!! Schmieden: Ware nicht erkannt (" + warenName + ") !");
		}
		
		// Prios
		int prio = OP.getOptionInt("Prio",0);
		if (prio>0) {
			this.eisenPrio = prio;
			this.holzPrio = prio;
			this.steinPrio = prio;
		}
		
		prio=OP.getOptionInt("holzPrio",0);
		if (prio>0){
			this.holzPrio=prio;
		}
		prio=OP.getOptionInt("eisenPrio",0);
		if (prio>0){
			this.eisenPrio=prio;
		}
		
		prio=OP.getOptionInt("steinPrio",0);
		if (prio>0){
			this.steinPrio=prio;
		}
		
		// Vorratsrunden
		int setVorrat = OP.getOptionInt("vorratsrunden",-1);
		if (setVorrat>-1){
			this.vorratsRunden = setVorrat;
		}
		
		// minAuslastung
		int setMinAuslastung = OP.getOptionInt("minAuslastung", -1);
		if (setMinAuslastung>-1){
			if (setMinAuslastung<=100){
				this.minAuslastung = setMinAuslastung;
			} else {
				// Fehler
				this.scriptUnit.doNotConfirmOrders("!!! Schmieden: minAuslastung fehlerhaft!");
			}
		}
		
		// Skilltype ermitteln
		if (this.itemType!=null){
			Skill skill = this.itemType.getMakeSkill();
			this.neededSkillType = skill.getSkillType();
			this.neededSkillLevel = skill.getLevel();
		}
		String talent = OP.getOptionString("Talent");
		if (talent.length()>1) {
			// ein bestimmtes Talent soll gelernt werden - kurzer check, ob es das gibt
			talent = talent.substring(0, 1).toUpperCase() + talent.substring(1).toLowerCase();
			
			SkillType skillType = super.gd_Script.getRules().getSkillType(talent);
			
			if (skillType==null){
				// wow, kein Lerntalent
				super.scriptUnit.doNotConfirmOrders("Schmieden mit Parameter Talent: Talent nicht erkannt! -> NICHT bestaetigt!");
				addOutLine("!!! ungueltiges Lerntalent bei " + this.unitDesc());
			} else {
				this.LernTalent=talent;
				this.addComment("Schmieden: als zu lernendes Talent erkannt: " + this.LernTalent);
			}
		}
		
		
		int prodPoints = 0;
		Skill neededSkill=null;
		int actSkillLevel = 0;
		int actSkillLevel_old = 0;
		int Peff = 0; // effektive Personenanzahl
		if (this.neededSkillType!=null){
			// this.addComment("Talentfrage: " + this.neededSkillType.getName());
			neededSkill = this.scriptUnit.getUnit().getModifiedSkill(this.neededSkillType);
			if (neededSkill!=null){
				actSkillLevel_old = neededSkill.getLevel(); 
				
				boolean getSchmiedeBonus = false;
				if (this.neededSkillType.getName().equalsIgnoreCase("Rüstungsbau")) {
					getSchmiedeBonus = true;
				}
				if (this.neededSkillType.getName().equalsIgnoreCase("Waffenbau")) {
					getSchmiedeBonus = true;
				}
				if (getSchmiedeBonus) {
					this.addComment("Schmiedebonus möglich, prüfe auf Arbeit in einer Schmiede");
					// actSkillLevel = FFToolsUnits.getModifiedSkillLevel(neededSkill,this.scriptUnit.getUnit(), true);
					actSkillLevel = neededSkill.getLevel();
					Building b = this.scriptUnit.getUnit().getModifiedBuilding();
					if (b!=null){
						if (b.getType().getName().equalsIgnoreCase("Schmiede")){
							actSkillLevel+=1;
							this.addComment("Schmiede erkannt, Talentlevel um 1 erhöht.");
						}
					}
					if (actSkillLevel==0 && actSkillLevel_old>0){
						actSkillLevel = actSkillLevel_old;
					}
				} else {
					actSkillLevel = actSkillLevel_old;
				}
				Peff = FFToolsUnits.getPersonenEffektiv(this.scriptUnit);
				prodPoints = actSkillLevel *  Peff;
				
			}
		}
		
		if (prodPoints==0){
			this.scriptUnit.doNotConfirmOrders("Keine Produktion möglich - keine Talentpunke.(modSkill:" + actSkillLevel +", modCount:" + Peff +",modSkill2:" + actSkillLevel_old + ")");
			return;
		} else {
			this.addComment("Wirksamer Talentlevel: " + actSkillLevel + " " + this.neededSkillType.getName());
		}
		
		if (this.neededSkillLevel>0) { 
			if (this.neededSkillLevel>actSkillLevel) {
				this.scriptUnit.doNotConfirmOrders("Keine Produktion möglich - zu geringer Talentwert (" + this.neededSkillLevel + "/" + actSkillLevel +  ")");
				return;
			} else {
				this.scriptUnit.addComment("Schmieden: mindestTalentcheck OK: " + actSkillLevel + ">=" + this.neededSkillLevel);
			}
		}
		
		
		
		// Maximale Anzahl an Ware ermitteln
		int maxMachenWare = 0;
		int warenLevel = 0;
		if (this.itemType!=null){
			warenLevel = this.itemType.getMakeSkill().getLevel();
		}
		if (warenLevel>0){
			maxMachenWare = (int)Math.floor((double)prodPoints/(double)warenLevel);
		} else {
			this.scriptUnit.doNotConfirmOrders("Keine Produktion möglich - kann benötigten Level nicht finden.");
			return;
		}

		if (maxMachenWare<=0){
			this.scriptUnit.doNotConfirmOrders("Schmieden: Keine Produktion möglich - ungenügendes Talent.");
			return;
		} else {
			this.addComment("Schmieden: Maximal mögliche Anzahl:" + maxMachenWare + " " + this.itemType.getName());
		}
		
		this.maxProduction = maxMachenWare;
		
		this.setMenge = OP.getOptionInt("menge", 0); 
		
		if (this.setMenge>0) {
			this.addComment("vorgegebene Menge erkannt: " + this.setMenge);
			if (this.setMenge<=this.maxProduction) {
				this.maxProduction = this.setMenge;
				maxMachenWare= this.setMenge;
				this.addComment("Gewünschte Menge angepasst auf " + this.maxProduction);
			} else {
				this.addComment("Gewünschte Menge kann nicht umgesetzt werden, es bleibt bei " + this.maxProduction);
			}
		}
		
		// die prios Festlegen
		Building actB = this.scriptUnit.getUnit().getModifiedBuilding();
		if (actB!=null){
			if (actB.getBuildingType().getName().equalsIgnoreCase("Schmiede")){
				this.isInSchmiede=true;
				this.addComment("Schmieden: Einheit erhält Schmiedebonus. (Prio +" + this.schmiedeBonus + ")");
			}
		}
		
		int actEisenPrio = this.eisenPrio + actSkillLevel;
		int actHolzPrio = this.holzPrio + actSkillLevel;
		int actSteinPrio = this.steinPrio + actSkillLevel;
		if (this.isInSchmiede){
			actHolzPrio += this.schmiedeBonus;
			actEisenPrio += this.schmiedeBonus;
			actSteinPrio += this.schmiedeBonus;
		}
		
		// benötigte Materialien erfassen
		for (Iterator<Item> iter = this.itemType.getResources();iter.hasNext();){
			Item actItem = (Item)iter.next();
			boolean isKnownItem = false;
			int anzahl = actItem.getAmount() * maxMachenWare;
			// für diese Runde
			if (actItem.getItemType().getName().equalsIgnoreCase("Eisen")){
				// wenn in Schmiede, Anzahl halbieren (Schmiedebonus)
				if (this.isInSchmiede){
					anzahl = (int)Math.ceil((double)anzahl/(double)2);
				}
				this.eisenRequest = new MatPoolRequest(this,anzahl,actItem.getItemType().getName(),actEisenPrio,"Schmieden diese Runde");
				this.addMatPoolRequest(this.eisenRequest);
				isKnownItem=true;
			}
			
			if ( actItem.getItemType().getName().equalsIgnoreCase("Laen")){
				// wenn in Schmiede, Anzahl halbieren (Schmiedebonus) !!! nicht für Laen!!
				if (this.isInSchmiede){
					anzahl = (int)Math.ceil((double)anzahl/(double)1);
				}
				this.eisenRequest = new MatPoolRequest(this,anzahl,actItem.getItemType().getName(),actEisenPrio,"Schmieden diese Runde");
				this.addMatPoolRequest(this.eisenRequest);
				isKnownItem=true;
			}
			
			if (actItem.getItemType().getName().equalsIgnoreCase("Holz") || actItem.getItemType().getName().equalsIgnoreCase("Mallorn")){
				this.holzRequest = new MatPoolRequest(this,anzahl,actItem.getItemType().getName(),actHolzPrio,"Schmieden diese Runde");
				this.addMatPoolRequest(this.holzRequest);
				isKnownItem=true;
			}
			if (actItem.getItemType().getName().equalsIgnoreCase("Stein")){
				this.steinRequest = new MatPoolRequest(this,anzahl,actItem.getItemType().getName(),actSteinPrio,"Schmieden diese Runde");
				this.addMatPoolRequest(this.steinRequest);
				isKnownItem=true;
			}
			if (!isKnownItem){
				this.scriptUnit.doNotConfirmOrders("Schmieden: unbekannte Zutat: " +  actItem.getItemType().getName());
			}
		}
		
		// falls Vorrat gewünscht, dann obige beiden Prio + 
		// Kommentar anpassen und neue MPR anlegen
		
		if (this.vorratsRunden>0){
			for (int i = 1;i<=this.vorratsRunden;i++){
				if (this.eisenRequest!=null){
					// Eisen
					this.setPrioParameter(this.eisenRequest.getPrio(), -0.5, 0,1);
					MatPoolRequest myMPR = new MatPoolRequest(this.eisenRequest);
					myMPR.setPrio(this.getPrio(i));
					myMPR.setKommentar("Schmiederessource in " + i);
					this.addMatPoolRequest(myMPR);
				}
				if (this.holzRequest!=null){
					// Holz
					this.setPrioParameter(this.holzRequest.getPrio(), -0.5, 0,1);
					MatPoolRequest myMPR = new MatPoolRequest(this.holzRequest);
					myMPR.setPrio(this.getPrio(i));
					myMPR.setKommentar("Schmiederessource in " + i);
					this.addMatPoolRequest(myMPR);
				}
				if (this.steinRequest!=null){
					// Stein
					this.setPrioParameter(this.steinRequest.getPrio(), -0.5, 0,1);
					MatPoolRequest myMPR = new MatPoolRequest(this.steinRequest);
					myMPR.setPrio(this.getPrio(i));
					myMPR.setKommentar("Schmiederessource in " + i);
					this.addMatPoolRequest(myMPR);
				}
			}
		}
		
		this.scriptUnit.findScriptClass("RequestInfo");
		
	}	
	
	/**
	 * Zweiter Lauf nach dem MatPool
	 */
	private void nachMatPool(){
		// für diese Runde Erfüllung der entscheidenden Requests 
		// ermitteln
		// actAnzahl berechnen (was ist tatsächlich möglich)
		// Auslastung ermitteln und prüfen
		// entweder machen oder Lernen
		
		if (this.itemType==null){
			return;
		}
		
		if (this.maxProduction==0){
			return;
		}
		
//		 benötigte Materialien erfassen
		int actProduction = this.maxProduction;
		boolean usingIron = false;
		for (Iterator<Item> iter = this.itemType.getResources();iter.hasNext();){
			Item actItem = (Item)iter.next();
			int resAnzahl = 0;
			// für diese Runde
			if (actItem.getItemType().getName().equalsIgnoreCase("Eisen")){
				resAnzahl = this.eisenRequest.getBearbeitet();
				if (this.isInSchmiede){
					resAnzahl *= 2;
				}
				int detProd = (int)Math.floor((double)resAnzahl/(double)actItem.getAmount());
				if (detProd<actProduction){
					this.addComment("Produktionsmenge reduziert von " + actProduction + " auf " + detProd + " wegen Menge Eisen");
					actProduction = detProd;
				}
				if (resAnzahl>0){
					usingIron=true;
				}
			}
			
			if (actItem.getItemType().getName().equalsIgnoreCase("Laen")){
				resAnzahl = this.eisenRequest.getBearbeitet();
				int detProd = (int)Math.floor((double)resAnzahl/(double)actItem.getAmount());
				if (detProd<actProduction){
					this.addComment("Produktionsmenge reduziert von " + actProduction + " auf " + detProd + " wegen Menge Laen");
					actProduction = detProd;
				}
			}
			
			
			if (actItem.getItemType().getName().equalsIgnoreCase("Holz") || actItem.getItemType().getName().equalsIgnoreCase("Mallorn")){
				resAnzahl = this.holzRequest.getBearbeitet();
				int detProd = (int)Math.floor((double)resAnzahl/(double)actItem.getAmount());
				if (detProd<actProduction){
					this.addComment("Produktionsmenge reduziert von " + actProduction + " auf " + detProd + " wegen Menge Holz/Mallorn");
					actProduction = detProd;
				}
			}
			if (actItem.getItemType().getName().equalsIgnoreCase("Stein")){
				resAnzahl = this.steinRequest.getBearbeitet();
				int detProd = (int)Math.floor((double)resAnzahl/(double)actItem.getAmount());
				if (detProd<actProduction){
					this.addComment("Produktionsmenge reduziert von " + actProduction + " auf " + detProd + " wegen Menge Stein");
					actProduction = detProd;
				}
			}
		}
		
		if (usingIron && this.isInSchmiede && this.setMenge==0 && !this.itemType.getName().equalsIgnoreCase("Bihänder")) {
			// Menge soll durch 2 Teilbar sein, laut Discord Chat ausser bei Bihänder relevant
			// checken, ob Menge durch 2 Teilbar ist.
			int Teiler=2;
			int mengeResult = (actProduction / Teiler) * Teiler;
			if (mengeResult==actProduction){
				addComment("Eisenarbeiten in der Schmiede erkannt, aber keine Anpassung auf gerade Produktionsmenge nötig, es bleibt bei " + mengeResult);
			} else {
				addComment("Eisenarbeiten in der Schmiede erkannt, Anpassung auf gerade Produktionsmenge erfolgt, Änderung von " + actProduction + " auf " + mengeResult);
			}
			actProduction=mengeResult;
		}
		
		
		// prozentsatz
		int Auslastung = (int)Math.ceil(((double)actProduction/(double)this.maxProduction)*100);
		this.addComment("Schmieden: " + actProduction + " von " + this.maxProduction + " möglich (" + Auslastung + "%, min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			// machen
			this.addOrder("MACHEN " + actProduction + " " + this.itemType.getName(), true);
			FFToolsUnits.leaveAcademy(this.scriptUnit, " Schmied arbeitet und verlässt Aka");
		} else {
			// lernen
			if (this.LernTalent.length()>1) {
				this.lerneTalent(this.LernTalent, true);
			} else {
				this.lerneTalent(this.itemType.getMakeSkill().getSkillType().getName(), true);
			}
		}
	}
			
		
}
