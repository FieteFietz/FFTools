package com.fftools.scripts;

import java.util.Iterator;

import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;

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
	
	/**
	 * vor MatPool maximale Produktionsmenge
	 */
	private int maxProduction = 0;
	
	boolean isInSchmiede = false;
	
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
			this.neededSkillType = this.itemType.getMakeSkill().getSkillType();
		}
		int prodPoints = 0;
		Skill neededSkill=null;
		int actSkillLevel = 0;
		int actSkillLevel_old = 0;
		if (this.neededSkillType!=null){
			neededSkill = this.scriptUnit.getUnit().getModifiedSkill(this.neededSkillType);
			if (neededSkill!=null){
				actSkillLevel_old = neededSkill.getLevel(); 
				actSkillLevel = FFToolsUnits.getModifiedSkillLevel(neededSkill,this.scriptUnit.getUnit(), true);
				if (actSkillLevel==0 && actSkillLevel_old>0){
					actSkillLevel = actSkillLevel_old;
				}
				prodPoints = actSkillLevel *  this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		
		if (prodPoints==0){
			this.scriptUnit.doNotConfirmOrders("Keine Produktion möglich - keine Talentpunke.(modSkill:" + actSkillLevel +", modCount:" + this.scriptUnit.getUnit().getModifiedPersons() +",modSkill2:" + actSkillLevel_old + ")");
			return;
		}
		
		// Schaffenstrunk oder RdF verdoppeln prodPoints 
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			prodPoints *= 2;
			this.addComment("Schmieden: Einheit nutzt Schaffenstrunk. Produktion verdoppelt.");
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
	

		// RdF
		ItemType rdfType=this.gd_Script.getRules().getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = this.scriptUnit.getModifiedItem(rdfType);
			if (rdfItem!=null && rdfItem.getAmount()>0){
				
				// RDF vorhanden...
				// produktion pro mann ausrechnen
				int prodProMann = (int)Math.floor((double)maxMachenWare/(double)this.scriptUnit.getUnit().getModifiedPersons());
				int oldMaxMachenWare = maxMachenWare;
				for (int i = 1;i<=rdfItem.getAmount();i++){
					if (i<=this.scriptUnit.getUnit().getModifiedPersons()){
						maxMachenWare -= prodProMann;
						maxMachenWare += (prodProMann * 10);
					} else {
						// überzähliger ring
						this.addComment("Schmieden: zu viele RdF!",false);
					}
				}
				this.addComment("Schmieden: " + rdfItem.getAmount() + " RdF. Prod von " + oldMaxMachenWare + " auf " + maxMachenWare + " erhöht.");
			} 
		} else {
			this.addComment("Schmieden: RdF ist noch völlig unbekannt.");
		}
		
		
		
		if (maxMachenWare<=0){
			this.scriptUnit.doNotConfirmOrders("Schmieden: Keine Produktion möglich - ungenügendes Talent.");
			return;
		} else {
			this.addComment("Schmieden: Maximal mögliche Anzahl:" + maxMachenWare + " " + this.itemType.getName());
		}
		
		this.maxProduction = maxMachenWare;
		
		int setMenge = OP.getOptionInt("menge", 0); 
		
		if (setMenge>0) {
			this.addComment("vorgegebene Menge erkannt: " + setMenge);
			if (setMenge<=this.maxProduction) {
				this.maxProduction = setMenge;
				maxMachenWare= setMenge;
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
		
		// prozentsatz
		int Auslastung = (int)Math.ceil(((double)actProduction/(double)this.maxProduction)*100);
		this.addComment("Schmieden: " + actProduction + " von " + this.maxProduction + " möglich (" + Auslastung + "%, min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			// machen
			this.addOrder("MACHEN " + actProduction + " " + this.itemType.getName(), true);
		} else {
			// lernen
			this.lerneTalent(this.itemType.getMakeSkill().getSkillType().getName(), true);
		}
	}
			
		
}
