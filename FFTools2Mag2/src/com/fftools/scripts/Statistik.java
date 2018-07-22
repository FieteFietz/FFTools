package com.fftools.scripts;

import com.fftools.transport.TransportManager;




public class Statistik extends Script{
	
	
	private static final int Durchlauf = 900;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Statistik() {
		super.setRunAt(Durchlauf);
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
		
		if (scriptDurchlauf==Durchlauf){
			this.scriptStart();
		}
	}
	
	private void scriptStart(){
		if (super.getArgCount()!=1){
			addOutLine(super.scriptUnit.getUnit().toString(true) + ": Statistik...unpassende Anzahl Parameter");
			super.addComment("Statistik...unpassende Anzahl Parameter -> Unit unbestaetigt", true);
			
			// Hilfetext
			this.addComment("Mögliche Statistiken: Requests, Transporter");
			
			super.scriptUnit.doNotConfirmOrders();
			return;
		}
		
		if (super.getArgAt(0).equalsIgnoreCase("Requests")){
			this.info_Requests();
		}
		if (super.getArgAt(0).equalsIgnoreCase("Transporter")){
			this.info_Transporter();
		}
	}
	/*
	 * Informiert über alle Requests des TA und der entsprechenden Lagerstände...
	 */
	private void info_Requests(){
		// TransportManager
		TransportManager TM = this.getOverlord().getTransportManager();
		if (TM==null){
			this.addComment("Sorry, no TM found...");
			return;
		}
		TM.statistik_Requests(this.scriptUnit);
	}
	
	
	/*
	 * Informiert über die Auslastung + Anzahl der Transporter
	 */
	private void info_Transporter(){
		// TransportManager
		TransportManager TM = this.getOverlord().getTransportManager();
		if (TM==null){
			this.addComment("Sorry, no TM found...");
			return;
		}
		TM.statistik_Transporter(this.scriptUnit);
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
	
}
