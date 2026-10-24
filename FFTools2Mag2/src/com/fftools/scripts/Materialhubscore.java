package com.fftools.scripts;

import com.fftools.utils.FFToolsOptionParser;

public class Materialhubscore extends Script{
	
	
	
	private static final int Durchlauf_1 = 20;
	
	private int[] runners = {Durchlauf_1};
	
	
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Materialhubscore() {
		super.setRunAt(runners);
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
		
		if (scriptDurchlauf==Durchlauf_1){
			this.scriptStart();
		}
		
		
	}
	
	private void scriptStart(){
		/*
		 *  // script MaterialHubScore name=[String: Gegenstand/Kategorie] faktor=[double]
		 */
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Materialhubscore");
		OP.addOptionList(this.getArguments());
		
		String Gegenstand=OP.getOptionString("name");
		if (Gegenstand.length()<2) {
			this.doNotConfirmOrders("!!! Materialhubscore - Name nicht angegeben.");
			return;
		}
		
		String sFaktor = OP.getOptionString("Faktor");
		if (sFaktor.length()==0) {
			this.doNotConfirmOrders("!!! Materialhubscore - Faktor nicht angegeben.");
			return;
		}
		
		double Faktor = Double.valueOf(sFaktor);
		if (Faktor<=0) {
			this.doNotConfirmOrders("!!! Materialhubscore - Faktor ungültig (<=0).");
			return;
		}
		
		String c_Gegenstand = Gegenstand.replace("_", " ");
    	c_Gegenstand = c_Gegenstand.replace("~", " ");
		
		int erg = this.getOverlord().getMaterialHubManager().add_MaterialScore(c_Gegenstand, Faktor, false);
		this.addComment("Materialhubscore ergänzt: " + erg + " (" + c_Gegenstand + " -> " + Faktor + ")");
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
