package com.fftools.scripts;

public class Requestshiprepair extends MatPoolScript{
	
	private int[] runners = {64};
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Requestshiprepair() {
		super.setRunAt(this.runners);
	}
	
	/**
	 */
	
	public void runScript(int scriptDurchlauf){
		// Prüfung dort
		this.getOverlord().getSWM().addShip2Repair(scriptUnit);
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
