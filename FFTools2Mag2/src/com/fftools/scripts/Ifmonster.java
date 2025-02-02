package com.fftools.scripts;

import com.fftools.utils.FFToolsRegions;


public class Ifmonster extends Script{
	
	
	private int[] runners = {14,15,16};
	private boolean scriptCalled = false;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ifmonster() {
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
		
		if (this.scriptCalled) {
			return;
		}
		
		this.scriptCalled=true;
		
		if (super.getArgCount()<1) {
			// falsche Anzahl Paras
			super.scriptUnit.doNotConfirmOrders("Falscher Aufruf von IfMonster: zu geringe Anzahl Parameter.");
			super.addComment("Unit wurde durch IfMonster NICHT bestaetigt", true);
			addOutLine("X....Falscher Aufruf von IfMonster: zu geringe Anzahl Parameter: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
		
		if (super.getArgCount()>0) {
			// nächsten Parameter anschaunen..entweder eressea-befgehl = irgendetwas
			// oder schlüsselwort script...
			
			
			String keyWord = super.getArgAt(0);
			boolean includingFactionDisguised=false;
		
			
			int argStartCount=1;
			if (keyWord.equalsIgnoreCase("includingFactionDisguised")) {
				includingFactionDisguised=true;
				this.addComment("ifMonster: auch parteigetarnte Einheiten, deren Fraktion uns unbekannt ist, gelten als Monster... (includingFactionDisguised)");
				argStartCount=2;
			}
			
			
			
			if (argStartCount==2) {	
				keyWord = super.getArgAt(1);
			}
			
			
			
			

			String enemyUnit = FFToolsRegions.isMonsterInRegion(this.scriptUnit.getUnit().getRegion(),this.scriptUnit,includingFactionDisguised);
			if (enemyUnit!=""){
				this.addComment("IfMonster: Monster detected: " + enemyUnit);
				if (keyWord.equalsIgnoreCase("script")) {
					if (super.getArgCount()>argStartCount) {
						// ok, in dieser Region soll ein script aufgerufen werden
						// eigentlich checken, ob dass von den scriptdurchläufen her passt
						// ansonsten parametersatz bauen und ergänzen....
						String newOrderLine = "";
						for (int i = (argStartCount+1);i<super.getArgCount();i++){
							newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
						}
						// Trim
						newOrderLine = newOrderLine.trim();
						super.scriptUnit.findScriptClass(super.getArgAt(argStartCount), newOrderLine,true);
					} else {
						// die befehlszeile endet mit dem keyWord script
						super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script)");
						addOutLine("X....Unerwartetes Ende der Befehlszeile (IfMonster,script): " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
					}
				} else {
					// kein script Befehl...alles was jetzt kommt als Order verpacken...
					// inkl des ersten wortes
					String newOrderLine = "";
					for (int i = (argStartCount-1);i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
					}
					// Trim
					newOrderLine = newOrderLine.trim();
					if (newOrderLine.length()>0){
						// nun denn ... fertig und irgendetwas zu schreiben
						newOrderLine = newOrderLine.concat(" ;script ifMonster");
						super.addOrder(newOrderLine,true);
						super.addComment("Unit wurde durch IfMonster befehligt", true);
					}
				}
			} else {
				this.addComment("IfMonster: no presence of monsters found in this region.");
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
}
