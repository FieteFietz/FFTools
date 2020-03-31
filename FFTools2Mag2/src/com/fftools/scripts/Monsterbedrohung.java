package com.fftools.scripts;

import com.fftools.utils.FFToolsOptionParser;

public class Monsterbedrohung extends Script{
	
	
	
	private static final int Durchlauf_1 = 30;
	
	private int[] runners = {Durchlauf_1};
	
	
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Monsterbedrohung() {
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
		 * // script Monsterbedrohung Rasse={String} Faktor={double} Taktiker={Wahrheitswert}
		 */
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Jagemonster");
		OP.addOptionList(this.getArguments());
		
		String Race=OP.getOptionString("Rasse");
		if (Race.length()<2) {
			this.doNotConfirmOrders("!!! Monsterbedrohung - Rasse nicht angegeben.");
			return;
		}
		
		String sFaktor = OP.getOptionString("Faktor");
		if (sFaktor.length()==0) {
			this.doNotConfirmOrders("!!! Monsterbedrohung - Faktor nicht angegeben.");
			return;
		}
		
		double Faktor = Double.valueOf(sFaktor);
		if (Faktor<=0) {
			this.doNotConfirmOrders("!!! Monsterbedrohung - Faktor ungültig (<=0).");
			return;
		}
		
		boolean Taktiker = OP.getOptionBoolean("Taktiker", false);
		
		String erg = this.getOverlord().getMJM().addMonsterThreat(Race,Faktor, Taktiker);
		this.addComment("Monsterbedrohung ergänzt: " + erg);
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
