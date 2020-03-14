package com.fftools.scripts;

import java.text.DecimalFormat;

import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Ship;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeAreaConnector;
import com.fftools.utils.FFToolsOptionParser;



/**
 * 
 * Ordnet die aktuelle unit einer TAC zu
 * 
 * @author Fiete
 *
 */

public class Ontradeareaconnection extends TradeAreaScript{
	
	

	private static final int Durchlauf_vorMP1 = 110;
	private static final int Durchlauf_vorMP1_2 = 120;
	
	private static final int onRouteRequestPrio=51;
	
	private int[] runners = {Durchlauf_vorMP1,Durchlauf_vorMP1_2};
	
	private TradeAreaConnector myTAC = null;

	private String LernplanName = "";

	private int kapa = -1;
	private double anteil = -1;
	
	public double getAnteil() {
		return anteil;
	}


	public int getKapa() {
		return kapa;
	}


	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ontradeareaconnection() {
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
		
		if (scriptDurchlauf==Durchlauf_vorMP1){
			this.scriptStart();
		}
		
		if (scriptDurchlauf==Durchlauf_vorMP1_2){
			this.scriptStart2();
		}
		
		
	}
	
	/**
	 * checken und beim TAH registrieren
	 */
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"OnTradeAreaConnection");
		
		// Pflichtfelder
		// Name des TAC
		String Name = OP.getOptionString("name");
		if (Name.length()<2){
			this.doNotConfirmOrders("!!! setTAC: Name fehlt!!! -> unbestaetigt");
			return;
		}
		
		// TAC suchen...
		TradeAreaConnector actTAC = super.getTradeAreaHandler().getTAC(Name);
		if (actTAC==null || !actTAC.isValid()){
			this.doNotConfirmOrders("!!! ein TAC " + Name + " konnte nicht gefunden werden! -> nicht bestätigt");
			return;
		}
		this.myTAC = actTAC;
		
		
		// checken, ob diese Unit auch ein saetKapa hat
		Object o = this.scriptUnit.getScript(Setkapa.class);
		if (o==null){
			this.doNotConfirmOrders("!!! onTradeAreaConnection verlangt zwingend eine setKapa-Angabe -> unbestätigt");
			return;
		}
		Setkapa sk = (Setkapa)o;
		this.kapa = sk.getKapa();
		this.addComment("onTAC: übernommene Kapazität: " + this.kapa + " GE");
		
		// prüfen, ob speed ausreicht
		Ship s = this.scriptUnit.getUnit().getShip();
		if (s!=null){
			int speed = this.gd_Script.getGameSpecificStuff().getGameSpecificRules().getShipRange(s);
			if (speed<this.myTAC.getSpeed()){
				this.doNotConfirmOrders("!!! onTradeAreaConnection: Speed des Schiffes KLEINER als TAC-Speed -> unbestätigt");
			}
			if (speed>(this.myTAC.getSpeed()+2)){
				this.doNotConfirmOrders("!!! onTradeAreaConnection: Speed des Schiffes HÖHER als (TAC-Speed + 2) -> unbestätigt");
			}
		} else {
			// kein Schiff ?!
			this.doNotConfirmOrders("!!! onTradeAreaConnection: diese Einheit ist nicht auf einem Schiff ?!? -> unbestätigt");
		}
		
		
		
		// beim TAC vermerken
		this.myTAC.addMover(this);
		
		// Lernplan zuordnen
		this.LernplanName = OP.getOptionString("Lernplan");
		
		
	}
	
	/**
	 * alle Mover sind registriert, eigenen Anteil ausrechnen
	 * Position prüfen und requests auslösen
	 */
	private void scriptStart2(){
		if (this.kapa<=0){
			this.addComment("!!!onTAC:keine kapa");
			return;
		}
		if (this.myTAC==null || !this.myTAC.isValid()){
			this.addComment("!!!onTAC:kein TAC");
			return;
		}
		
		int allSumme = myTAC.getOverallMoverKapa();
		this.addComment(myTAC.getMoverInfo());
		int allWeight = myTAC.getNeededGE();
		this.addComment("GE insgesammt benötigt: " + allWeight + " (" + myTAC.getWeightInfo() + ")");
		
		// Anteil
		this.anteil = (double)this.kapa / (double)allSumme;
		DecimalFormat df = new DecimalFormat("0.00");
		this.addComment("TAC: Anteil der Unit nach Kapa:" + df.format(this.anteil * 100) + "%");
	
		
		// Rausfinden, wo wir sind
		
		if (this.getUnit().getRegion().equals(myTAC.getSU1().getUnit().getRegion())){
			// wir sind bei SCU1 und werden danach nach TA2 fahren
			this.myTAC.processMoverRequests(2, this);
			// usage in TA1
			this.myTAC.processMoverUsages(1, this);
			return;
		}
		
		if (this.getUnit().getRegion().equals(myTAC.getSU2().getUnit().getRegion())){
			// wir sind bei SCU2 und werden danach nach TA1 fahren
			this.myTAC.processMoverRequests(1, this);
			// usage in TA2
			this.myTAC.processMoverUsages(2, this);
			return;
		}
		
		// wir sind auf hoher See....
		// alles aktuelle knapp requesten ?
		saveMyGoods();
		
		// Routencheck
		checkTAC_Regions();
		
	}
	
	/**
	 * Fragt alle aktuelle Gegenstände selbst mit Prio an
	 * damit sie nicht in irgendein depot wandern
	 */
	private void saveMyGoods(){
		if (this.scriptUnit.getUnit().getItems()!=null){
			for (Item item:this.scriptUnit.getUnit().getItems()){
				MatPoolRequest mpr = new MatPoolRequest(this, item.getAmount(), item.getName(), onRouteRequestPrio, "on TAC-Route: save my goods!");
				this.addMatPoolRequest(mpr);
			}
		}
		this.addComment("TAC-Mover en route: saved my goods with prio=" + onRouteRequestPrio);
	}
	
	
	private void checkTAC_Regions() {
		// haben wir ein Route-Script?
		Object o = this.scriptUnit.getScript(Route.class);
		if (o!=null) {
			Route r = (Route)o;
			Region r1 = this.myTAC.getSU1().getUnit().getRegion();
			if (r.isInRegionList(r1)) {
				this.addComment("Routencheck I OK: " + r1.toString() + " ist auf der Route");
			} else {
				this.doNotConfirmOrders("!!! Fehler im Routencheck I: " + r1.toString() + " ist NICHT auf der Route (daher unbestätigt)");
			}
			r1 = this.myTAC.getSU2().getUnit().getRegion();
			if (r.isInRegionList(r1)) {
				this.addComment("Routencheck II OK: " + r1.toString() + " ist auf der Route");
			} else {
				this.doNotConfirmOrders("!!! Fehler im Routencheck II: " + r1.toString() + " ist NICHT auf der Route (daher unbestätigt)");
			}
		} else {
			this.addComment("!!! Routencheck: Mover ist nicht auf einer Route ?!?");
		}
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
	public String getLernplanName() {
		return this.LernplanName;
	}


	public void setLernplanName(String lernplanName) {
		this.LernplanName = lernplanName;
	}
	
	public TradeAreaConnector getMyTAC() {
		return myTAC;
	}

}
