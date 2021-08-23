package com.fftools.scripts;

import magellan.library.RegionResource;
import magellan.library.rules.ItemType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;

/**
 * 
 * Eine erste abgespeckte Version zur Produktion
 * Waffen und R�stungen
 * @author Fiete
 *
 */

public class Forester extends MatPoolScript{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf_vorMatPool = 350;
	private int Durchlauf_nachMatPool = 430;
	
	private int[] runners = {Durchlauf_vorMatPool,Durchlauf_nachMatPool};
	
	/**
	 * basisPrio zur Anforderung von WdL 
	 */
	private int WDL_Prio = 500;
	/**
	 * basisPrio zur Anforderung von Holz
	 */
	private int holzPrio = 500;
	
	/**
	 * minimale Menge
	 * 0 = ALLES
	 */
	private int minMenge = 0;
	
	/**
	 * maximale Menge
	 * -1 = nicht gesetzt, also alles
	 */
	private int maxMenge = -1;
	
	/**
	 * WDL Request f�r diese Runde
	 */
	private MatPoolRequest WDL_Request = null;
	
	/**
	 * Holzrequest f�r diese Runde
	 */
	private MatPoolRequest holzRequest = null;
	
	/**
	 * ist es eine Mallornregion ?
	 */
	private boolean isMallornregion = false;
	
	/**
	 * Erst wenn der Baumbestand unter den minBestand sinkt, erst dann wird der F�rster tats�chlich aktiv
	 * anforderungen bleiben trotzdem bestehen
	 */
	private int minBestand=0;
	
	
	/**
	 * Wenn der F�rster aktiv wird, wird er nur so viel WdL benutzen, dass der geplante Holzbestand den sollBestand nicht �berschreitet.
	 * Dabei werden "mache Holz"-Befehle nicht ber�cksichtigt.
	 */
	private int sollBestand=0;
	
	// Konstruktor
	public Forester() {
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
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Optionen lesen und Pr�fen
		
		// Prios
		int prio = OP.getOptionInt("Prio",0);
		if (prio>0) {
			this.WDL_Prio = prio;
			this.holzPrio = prio;
		}
		
		prio=OP.getOptionInt("Holz_Prio",0);
		if (prio>0){
			this.holzPrio=prio;
		}
		prio=OP.getOptionInt("WDL_Prio",0);
		if (prio>0){
			this.WDL_Prio=prio;
		}
		
		prio=OP.getOptionInt("minMenge",0);
		if (prio>0){
			this.minMenge = prio;
		}
		
		prio=OP.getOptionInt("maxMenge", -1);
		if (prio>0){
			this.maxMenge = prio;
		}
		
		
		// wieviel Holz anfordern?
		// alles oder minMenge
		int MengeWDL = Integer.MAX_VALUE;
		String infoText = "";
		if (this.minMenge>0){
			MengeWDL = this.minMenge;
			infoText += "minMenge=" + this.minMenge;
		}
		if (this.maxMenge>0 && MengeWDL>this.maxMenge){
			MengeWDL = this.maxMenge;
			if (infoText.length()>1){
				infoText += ", ";
			}
			infoText += "maxMenge=" + this.maxMenge;
		}
		
		this.minBestand = OP.getOptionInt("minBestand", 0);
		if (this.minBestand>0) {
			if (infoText.length()>1){
				infoText += ", ";
			}
			infoText += "minBestand=" + this.minBestand;
		}
		
		this.sollBestand = OP.getOptionInt("sollBestand", 0);
		if (this.sollBestand>0) {
			if (infoText.length()>1){
				infoText += ", ";
			}
			infoText += "sollBestand=" + this.sollBestand;
		}

		
		if (infoText.length()>0){
			infoText = "Forester diese Runde (" + infoText + ")";
		} else {
			infoText = "Forester diese Runde";
		}
		
		int MengeHolz = Integer.MAX_VALUE;
		if (MengeWDL!=Integer.MAX_VALUE) {
			MengeHolz = MengeWDL * 10;
		}
		
		
		// FF 20171203
		// sind wir in einer Mallornregion?
		ItemType IT = this.gd_Script.getRules().getItemType("Mallorn");
		RegionResource RR = this.region().getResource(IT);
		this.isMallornregion = false;
		if (RR!=null){
			// tats�chlich
			this.addComment("Forester: Mallornregion am Mallorn erkannt, fordere Mallorn statt Holz an");
			this.isMallornregion = true;
		} else {
			// 20200413: auch auf Sch�sslinge pr�fen
			IT = this.gd_Script.getRules().getItemType("Mallornsch��linge");
			RR = this.region().getResource(IT);
			if (RR!=null){
				// tats�chlich
				this.isMallornregion = true;
				this.addComment("Forester: Mallornregion an Mallornsch��lingen erkannt, fordere Mallorn statt Holz an");
			} else {
				if (this.region().isMallorn()) {
					this.isMallornregion = true;
					this.addComment("Forester: Mallornregion an CR-Info erkannt, fordere Mallorn statt Holz an");
				}
			}
		}
		
		if (this.isMallornregion) {
			this.holzRequest = new MatPoolRequest(this,MengeHolz,"Mallorn",this.holzPrio,infoText);
			this.addMatPoolRequest(this.holzRequest);
		} else {
			// nee Holz...
			this.holzRequest = new MatPoolRequest(this,MengeHolz,"Holz",this.holzPrio,infoText);
			this.addMatPoolRequest(this.holzRequest);
		}
		
		
		this.WDL_Request = new MatPoolRequest(this,MengeWDL,"Wasser des Lebens",this.WDL_Prio,infoText);
		this.addMatPoolRequest(this.WDL_Request);
		
		
		this.scriptUnit.findScriptClass("RequestInfo");
		
	}	
	
	/**
	 * Zweiter Lauf nach dem MatPool
	 */
	private void nachMatPool(){
		int Holz_verf�gbar = this.holzRequest.getBearbeitet();
		int WDL_verf�gbar = this.WDL_Request.getBearbeitet();
		
		String Gut = "Holz";
		if (this.isMallornregion){
			Gut="Mallorn";
		}
		
		this.addComment("Forester: " + Holz_verf�gbar + " " + Gut + " und " + WDL_verf�gbar + " WdL verf�gbar.");
		
		// wieviel kann damit maximal benutzt / gez�chtet werden...minmenge bezieht sich auf WDL!!!
		int Produktion=(int)Math.ceil(Holz_verf�gbar/10);
		if (WDL_verf�gbar<Produktion){
			Produktion = WDL_verf�gbar;
		}
		boolean mayProduce=false;
		if (Produktion>0){
			mayProduce=true;
		}
		if (this.minMenge>0 && Produktion<this.minMenge){
			mayProduce=false;
			this.addComment("Forester: m�gliche Produktion (" + Produktion + ") unter Minimalmenge!");
		}
		
		int Staemme = 0;
		if (mayProduce && (this.minBestand>0 || this.sollBestand>0)) {
			// checke, ob minBestand gesetzt und unterschritten
			this.addComment("pr�fe auf Unterschreitung des MindestBestands (" + this.minBestand + " St�mme)");
			
			ItemType IT = this.gd_Script.getRules().getItemType("Mallorn");
			RegionResource RR = this.region().getResource(IT);
			if (RR!=null){
				Staemme += RR.getAmount();
			}
			IT = this.gd_Script.getRules().getItemType("Mallornsch��linge");
			RR = this.region().getResource(IT);
			if (RR!=null){
				Staemme += RR.getAmount();
			}
			IT = this.gd_Script.getRules().getItemType("B�ume");
			RR = this.region().getResource(IT);
			if (RR!=null){
				Staemme += RR.getAmount();
			}
			// Sch��linge
			IT = this.gd_Script.getRules().getItemType("Sch��linge");
			RR = this.region().getResource(IT);
			if (RR!=null){
				Staemme += RR.getAmount();
			}
			
			if (this.minBestand>0) {
				if (Staemme>this.minBestand) {
					this.addComment(Staemme + " St�mme gefunden -> kein Aufforsten diese Runde");
					mayProduce=false;
				} else {
					this.addComment(Staemme + " St�mme gefunden -> Mindestbestand unterschritten.");
				}
			}
			
			if (mayProduce && this.sollBestand>0) {
				// wir wollen den Sollbestand nicht �berschreiten!
				int maxNeueStaemme = this.sollBestand - Staemme;
				int maxProduktion = maxNeueStaemme/10;
				if (maxProduktion>Produktion) {
					// keine Einschr�nkungen
					this.addComment("Durch das Benutzen von " + Produktion + " WdL wird der sollBestand von " + this.sollBestand + " nicht �berschritten");
				} else {
					Produktion = maxProduktion;
					this.addComment("Um den Soll-Bestand von " + this.sollBestand + " nicht zu �berschreiten, wird die Menge von WdL auf " + Produktion + " reduziert.");
					if (Produktion==0) {
						mayProduce=false;
						this.addComment("Klartext in diesem Fall: es wird kein WdL benutzt.");
					}
				}
				
			}
		}
		
		
		
		
		if (mayProduce){
			this.addOrder("Benutze " + Produktion + " Wasser~des~Lebens", true);
		}
	}
}
