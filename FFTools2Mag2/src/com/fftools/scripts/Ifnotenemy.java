package com.fftools.scripts;

import com.fftools.utils.FFToolsRegions;


public class Ifnotenemy extends Script{
	
	private static final int Durchlauf = 15;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ifnotenemy() {
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
		
		if (scriptDurchlauf!=Durchlauf){return;}
		

		
		if (super.getArgCount()<1) {
			// falsche Anzahl Paras
			super.scriptUnit.doNotConfirmOrders("Falscher Aufruf von IfNotEnenemy: zu geringe Anzahl Parameter.");
			addOutLine("X....Falscher Aufruf von IfNotEnemy: zu geringe Anzahl Parameter: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
		
		if (super.getArgCount()>0) {
			// nächsten Parameter anschaunen..entweder eressea-befgehl = irgendetwas
			// oder schlüsselwort script...
			// 20190908: guardingonly
			String keyWord = super.getArgAt(0);
			boolean guardingOnly=false;
			int argStartCount=1;
			
			if (keyWord.equalsIgnoreCase("guardingonly")) {
				guardingOnly=true;
				this.addComment("ifNotEnemy: nur bewachende feindliche Einheiten sollen berücksichtigt werden (guardingOnly)");
				keyWord = super.getArgAt(1);
				argStartCount=2;
			}
			
			// String enemyUnit = FFToolsRegions.isEnemyInRegion(this.scriptUnit.getUnit().getRegion(),this.scriptUnit);
			String enemyUnit = FFToolsRegions.isEnemyInRegion(this.scriptUnit.getUnit().getRegion(),null,guardingOnly); 
			if (enemyUnit==""){
				this.addComment("IfNotEnemy: no Enemy detected.");
				if (keyWord.equalsIgnoreCase("script")) {
					if (super.getArgCount()>argStartCount) {
						// ok, in dieser Region soll ein script aufgerufen werden
						// eigentlich checken, ob dass von den scriptdurchläufen her passt
						// ansonsten parametersatz bauen und ergänzen....
						String newOrderLine = "";
						for (int i = (argStartCount+1);i<super.getArgCount();i++){
							newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
						}
						super.scriptUnit.findScriptClass(super.getArgAt(argStartCount), newOrderLine,true);
					} else {
						// die befehlszeile endet mit dem keyWord script
						super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script)");
						addOutLine("X....Unerwartetes Ende der Befehlszeile (IfNotEnemy,script): " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
					}
				} else {
					// kein script Befehl...alles was jetzt kommt als Order verpacken...
					// inkl des ersten wortes
					String newOrderLine = "";
					for (int i = (argStartCount-1);i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
					}
					if (newOrderLine.length()>0){
						// nun denn ... fertig und irgendetwas zu schreiben
						newOrderLine = newOrderLine.concat(" ;script ifNotEnemy");
						super.addOrder(newOrderLine,true);
						super.addComment("Unit wurde durch IfNotEnemy befehligt", true);
					}
				}
			} else {
				this.addComment("IfNotEnemy: enemy presence found in this region: " + enemyUnit);
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
