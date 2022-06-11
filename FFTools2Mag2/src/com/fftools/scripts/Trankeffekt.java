package com.fftools.scripts;

import java.text.NumberFormat;

import magellan.library.Item;
import magellan.library.Unit;
import magellan.library.rules.ItemType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;




public class Trankeffekt extends MatPoolScript{
	
	
	private static final int Durchlauf_vorMP = 21;
	private static final int Durchlauf_nachMP = 710;
	
	private int[] runsAt = {Durchlauf_vorMP,Durchlauf_nachMP};
	
	private int requestPrio = 20;
	
	private int vorratsRunden = 2;
	
	private int otherPersons = 0;
	
	private int personenWirkung=10;
	
	private String trank = null;
	
	private ItemType itemType=null;
	
	private MatPoolRequest myMPR = null;

	private boolean notSuccessACK = false;
	
	private boolean useThisRound = false;
	
	private int addEffekteThisRound = 0;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Trankeffekt() {
		super.setRunAt(this.runsAt);
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
		switch (scriptDurchlauf){
		
		case Durchlauf_vorMP:this.vorMatPool(scriptDurchlauf);break;
	
		case Durchlauf_nachMP:this.nachMatPool(scriptDurchlauf);break;
		}
	}
	
	public void vorMatPool(int scriptDurchlauf){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		
		String trankName = OP.getOptionString("Trank");
		if (trankName.length()<2) {
			this.addComment("Trankeffekt: Trank nicht angegeben.");
			outText.addOutLine("!!!Trankeffekt: Trank nicht angegeben. " + this.unitDesc());
			return;
		}
		this.itemType = this.gd_Script.getRules().getItemType(trankName);
		if (this.itemType==null){
			this.addComment("Trankeffekt: Trank nicht erkannt.");
			outText.addOutLine("!!!Trankeffekt: Trank nicht erkannt. " + this.unitDesc());
			return;
		}
		this.trank = trankName;
		
		// wieviel trank auf Vorrat
		
		int userVorrat = OP.getOptionInt("vorrat",-1);
		if (userVorrat>0){
			this.vorratsRunden = userVorrat;
		}
		
		int userOtherPersons = OP.getOptionInt("other", -1);
		if (userOtherPersons>0){
			this.otherPersons = userOtherPersons;
		}
		
		int persZahl = this.scriptUnit.getUnit().getModifiedPersons()+this.otherPersons;
		
		if (this.trank.equalsIgnoreCase("Bauernblut")){
			this.personenWirkung = 100;
		}
		
		
		// wieviel Trank braucht die einheit pro runde?
		double usagePerRound = (double)(persZahl)/ this.personenWirkung;
		double peasants = 0;
		if (this.trank.equalsIgnoreCase("Bauernlieb")){
			peasants = this.region().getModifiedPeasants();
			usagePerRound = (int)Math.floor(peasants/1000);
		}
		
		int usageNeeded = (int)Math.ceil(usagePerRound * (double)this.vorratsRunden);

		NumberFormat NF = NumberFormat.getInstance();
		NF.setMaximumFractionDigits(0);
		NF.setMinimumFractionDigits(0);
		NF.setMinimumIntegerDigits(1);
		NF.setMaximumIntegerDigits(5);
		
		
		this.addComment("Vorrat " + this.trank + ": daher benötigt: " + usageNeeded + " Tränke (mit Prio=" + this.requestPrio + ")");
		if (this.trank.equalsIgnoreCase("Bauernlieb")){
			this.addComment("Vorrat: " + this.trank + " pro Runde benötigt:" + usagePerRound + " (bei " + peasants + " berücksichtigten Bauern)");
		} else {
			this.addComment("Vorrat: " + this.trank + " pro Runde benötigt:" + usagePerRound + " (bei " + persZahl + " berücksichtigten Personen)");
		}
		
		
		// priorität
		int userPrio = OP.getOptionInt("Prio",-1);
		if (userPrio>0){
			this.requestPrio=userPrio;
		}
		
		
		// notSuccessACK
		this.notSuccessACK = OP.getOptionBoolean("notSuccessACK", this.notSuccessACK);
		
		
		// entsprechenden request loswerden
		this.myMPR = new MatPoolRequest(this,usageNeeded,this.trank,this.requestPrio,"TrankEffekt");
		this.addMatPoolRequest(this.myMPR);
		// Effekt feststellen.
		if (!this.trank.equalsIgnoreCase("Bauernlieb")){
			int effects = this.scriptUnit.getEffekte(this.trank);
			if (effects<persZahl){
				//  Trank benutzen
				// nur wenn in Region vorhanden
				if (countPotionInRegion4Faction()>0){
					this.addOrder("BENUTZEN " +  NF.format(Math.ceil((double)persZahl/this.personenWirkung)) + " " + this.trank,false);
					this.useThisRound=true;
					this.addEffekteThisRound += Math.ceil((double)persZahl/this.personenWirkung) * 10;
				} else {
					this.addComment("Trank " + this.trank + " nicht ausreichend, aber auch nicht für diese Partei in dieser Region verfügbar.");
				}
				
			} else {
				this.addComment("Trank " + this.trank + " ausreichend.");
			}
		} else {
			// Bauernblut
			int availableBlood = countPotionInRegion4Faction();
			if (availableBlood>0){
				if (availableBlood>usagePerRound) {
					availableBlood=(int) usagePerRound;
				}
				this.addOrder("BENUTZEN " + availableBlood  + " " + this.trank,false);
				this.useThisRound=true;
				this.addEffekteThisRound += availableBlood * 100;
				if (availableBlood<usagePerRound) {
					this.addComment("Trankeffekt: nicht ausreichend Tränke vorhanden, benötigt: " + usagePerRound + ", bei der Partei hier vorhanden: " + availableBlood); 
				}
			} else {
				this.addComment("Trank " + this.trank + " aktuell nicht bei mir vorhanden, aber auch nicht für diese Partei in dieser Region verfügbar.");
			}
		}
		
	}
	
	public void nachMatPool(int scriptDurchlauf){
		if (this.myMPR!=null && this.myMPR.getOriginalGefordert()>0){
			if (this.myMPR.getBearbeitet()<this.myMPR.getOriginalGefordert()){
				if (!this.notSuccessACK) {
					this.scriptUnit.doNotConfirmOrders("Trank vorrat nicht ausreichend!. (" +  this.trank + ")");
				} else {
					this.addComment("Trank vorrat nicht ausreichend!. (" +  this.trank + ")");
				}
			}
		}
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
	
	/**
	 * liefert die Anzahl der Tränke bei den Einheiten der Partei in der Region
	 * @return
	 */
	private int countPotionInRegion4Faction(){
		int erg=0;
		if (this.itemType==null){
			return 0;
		}
		for (Unit u:this.region().getUnits().values()){
			if (u.getFaction()!=null && u.getFaction().equals(this.getUnit().getFaction())){
				Item i = u.getItem(this.itemType);
				if (i!=null){
					erg += i.getAmount();
				}
			}
		}
		return erg;
	}


	public String getTrank() {
		return trank;
	}


	public boolean isUseThisRound() {
		return useThisRound;
	}
	
	public int countUseThisRound() {
		return this.addEffekteThisRound;
	}
	
	
}
