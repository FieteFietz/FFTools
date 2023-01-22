package com.fftools.scripts;

import com.fftools.pools.ausbildung.LernplanHandler;




public class Setlernplan extends Script{
	
	
	private static final int Durchlauf = 5;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Setlernplan() {
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
	
	/**
	 * here we go
	 *
	 */
	private void scriptStart(){
		LernplanHandler LPH = this.getOverlord().getLernplanHandler();
		LPH.parseOrders(this.scriptUnit,this);
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
	 * wenn beim start im client regionen selected sind, dann nur die
	 * scripts ausführen,die hier true liefern, alle anderen nicht.
	 * @return
	 */
	public boolean runIfNotInSelectedRegions(){
		return true;
	}
	
}
