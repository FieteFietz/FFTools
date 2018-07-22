package com.fftools.scripts;

import com.fftools.trade.TradeArea;




public class Islandinfo extends TradeAreaScript{
	
	
	private static final int Durchlauf = 220;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Islandinfo() {
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
	
	/*
	 * liefert die IslandInfo als Kommentare
	 * je nach Parameter beim Aufruf
	 */
	private void scriptStart(){
		
			TradeArea TA =this.getTradeArea();
			if (TA == null){
				this.addComment("! Fehler bei IslandInfo: diese Region gehört zu keinem TradeArea");
				return;
			}
			
			this.getTradeAreaHandler().addIslandInfo(this, TA);

	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
}
