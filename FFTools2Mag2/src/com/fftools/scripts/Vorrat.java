package com.fftools.scripts;

import magellan.library.rules.ItemType;

import com.fftools.OutTextClass;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeUtils;
import com.fftools.transport.TransportRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;




public class Vorrat extends TransportScript{
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final int Durchlauf_vorMP = 190;
	private static final int Durchlauf_nachMP = 320;
	
	private int[] runsAt = {Durchlauf_vorMP,Durchlauf_nachMP};
	
	private final int defaultPrio = 10;
	
	/**
	 * das geforderte gut..
	 */
	private ItemType itemType = null;
	
	private int summe = -1;
	private int proRunde = -1;
	private int prio = defaultPrio;
	private int prioTM = defaultPrio;
	
	private boolean optionsOK = false;
	
	private int nochZuLiefern = 0;
	
	private TradeArea tradeArea = null;
	
	private MatPoolRequest myMPR = null;
	
	private TransportRequest myTR = null;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Vorrat() {
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
		
			case Durchlauf_vorMP:this.vorMatpool();break;

			case Durchlauf_nachMP:this.nachMatPool();break;
		
		}
		
	}
	
	public void vorMatpool(){
		
		// optionen parsen
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		String nameGut = OP.getOptionString("Ware");
		if (nameGut.length()<2) {
			this.addComment("Vorrat: Ware nicht angegeben.");
			outText.addOutLine("!!!Vorrat: Ware nicht angegeben. " + this.unitDesc());
			return;
		}
		// ersetzungen
		// nameGut = nameGut.replace("_", " ");
		
		// replacen
		if (nameGut!=null && nameGut.length()>0){
			nameGut = FFToolsGameData.translateItemShortform(nameGut);
		}
		
		
		this.itemType = this.gd_Script.rules.getItemType(nameGut);
		if (this.itemType==null){
			// vielleicht noch ein ItemGroup
			if (!reportSettings.isInCategories(nameGut)){
				this.addComment("Vorrat: Ware nicht erkannt:" + nameGut);
				outText.addOutLine("!!!Vorrat: Ware nicht erkannt. " + this.unitDesc());
				return;
			} 
		}
		
		this.summe = OP.getOptionInt("Summe", -1);
		if (this.summe<1){
			// doch noch die Angabe Mengec erlauben...;-)
			this.summe = OP.getOptionInt("Menge", -1);
		}
		
		if (this.summe<1){
			this.addComment("Vorrat: Summe nicht erkannt.");
			outText.addOutLine("!!!Vorrat: Summe nicht erkannt. " + this.unitDesc());
			return;
		}
		
		this.proRunde = OP.getOptionInt("proRunde",-1);
		if (this.proRunde<1 && TradeUtils.isTradeItemType(this.itemType)){
			// wenn kein TradeItem proRunde nicht wichtig
			this.addComment("Vorrat: proRunde nicht erkannt.");
			outText.addOutLine("!!!Vorrat: proRunde nicht erkannt. " + this.unitDesc());
			return;
		}
		
		// prio
		this.prio = OP.getOptionInt("prio", this.defaultPrio);
		if (this.prio<1) {
			this.addComment("Vorrat: Fehler bei PRIO Abgabe.");
			outText.addOutLine("!!!Vorrat: Fehler bei PRIO Abgabe. " + this.unitDesc());
			return;
		}
		
		
		// prioTM
		this.prioTM = OP.getOptionInt("prioTM", this.prio);
		if (this.prio<1) {
			this.addComment("Vorrat: Fehler bei PRIO-TM Abgabe.");
			outText.addOutLine("!!!Vorrat: Fehler bei PRIO-TM Abgabe. " + this.unitDesc());
			return;
		}
		
		// OK,alles da.
		
		this.optionsOK=true;
		
		// Im ersten Schritt alles in der Region verfügbare an sich ziehen
		// MatPool läuft
		// nach Matpool überprüfen, was habe ich und entsprechend
		// TM und TAH informieren
		
		// also jetzt gesamtsumme kurzerhand requesten
		myMPR = new MatPoolRequest(this,this.summe,nameGut,this.prio,"Vorrat");
		// bei vorrat ist angabe einer speziellen prioTM möglich...
		if (this.prioTM!=this.prio) {
			myMPR.setPrioTM(this.prioTM);
		}
		
		
		
		// sortMode
		if (OP.getOptionString("sort").equalsIgnoreCase("amount")){
			myMPR.setTMsortMode(MatPoolRequest.TM_sortMode_amount);
		}
		
		this.addMatPoolRequest(myMPR);
		
		// request INfo
		this.scriptUnit.findScriptClass("RequestInfo");
		
		
		// TradeArea mal feststellen, damit später danach sortiert werden kann
		this.tradeArea = this.getTradeAreaHandler().getTAinRange(this.region());
		// Eintrag ion requestAl
		if (this.tradeArea!=null) {
			this.tradeArea.addVorratScript2ALL(this);
		} else {
			outText.addOutLine("!!! Vorrat ohne TA !!! bei " + this.unitDesc(), true);
		}
		
	}
	
	
	private void nachMatPool(){
		// wenn optionen im lauf1 nicht ok, auch keinen zweiten lauf machen
		if (!optionsOK){
			return;
		}
		
		// checken, ob unit genug bekommt
	    /*
		if (myMPR.getPrio()==1 && !myMPR.isOnlyRegion()){
			// es handelt sich um Vorrat mit prio=1 und weniger als ALLES
			// umwandeln in ein TransportRequest um nach wie vor im TA verfügbar zu machen
			this.addComment("Vorrat mit Prio=1 erkannt (" + this.summe + " " + myMPR.getOriginalGegenstand() + "), Umwandlung in TransportRequest");
			myMPR.setBearbeitet(myMPR.getOriginalGefordert()); // damit der MP nicht noch daraus ein TR macht
			this.nochZuLiefern = this.summe; // damit der TM diese Summe beschafft.
			this.tradeArea.addVorratScript(this);
			return;
		}
		*/
		
		if (myMPR.getBearbeitet()==myMPR.getOriginalGefordert()){
			// alles schön
			this.addComment("Vorrat an " + myMPR.getOriginalGegenstand() + " ausreichend.");
			
			return;
		}
		
		// nicht alles schön...TAH informieren
		this.nochZuLiefern = this.summe - myMPR.getBearbeitet();
		
		
		// TradeArea mal feststellen, damit später danach sortiert werden kann
		// this.tradeArea = this.getTradeAreaHandler().getTAinRange(this.region());
		
		if (this.tradeArea==null){
			// oopsa...das ist ungewöhnlich
			this.addComment("!!! Region nicht in einem TradeArea ?!!!");
			outText.addOutLine("!!! Vorratsregion nicht in einem TradeArea: " + this.region().toString());
			return;
		}
		
		this.tradeArea.addVorratScript(this);
		
		// this.addComment("Vorrat: " + this.nochZuLiefern + " " + myMPR.getOriginalGegenstand() + " angefordert.");
		
	}
	
	
	/**
	 * liefert einen passenden TransportRequest zu diesem Script
	 * @return
	 */
	public TransportRequest createTransportRequest(){
		// FF 20070430: der request ist bereits via MatPool beim TM
		return null;
		/*
		if (this.prio>1){
			return null;
		}
		
		// FF 20161020
		// Vorrat mit prio=1 
		
		TransportRequest erg = new TransportRequest(this.scriptUnit,this.nochZuLiefern,myMPR.getOriginalGegenstand(),this.prioTM,"Vorrat(Prio1)");
		this.myTR = erg;
		return erg;
		*/
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
	 * @return the nochZuLiefern
	 */
	public int getNochZuLiefern() {
		return nochZuLiefern;
	}


	/**
	 * @return the itemType
	 */
	public ItemType getItemType() {
		return itemType;
	}


	/**
	 * @return the prio
	 */
	public int getPrio() {
		return prio;
	}


	/**
	 * @return the proRunde
	 */
	public int getProRunde() {
		return proRunde;
	}


	/**
	 * @return the tradeArea
	 */
	public TradeArea getTradeArea() {
		return tradeArea;
	}
	
	/**
	 * Prüft, ob dieses Vorratsscript von source=TAC hat
	 * @return
	 */
	public boolean isTAC_Vorrat(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		if (OP.getOptionString("source").equalsIgnoreCase("TAC")){
			return true;
		}
		return false;
	}
	
	
}
