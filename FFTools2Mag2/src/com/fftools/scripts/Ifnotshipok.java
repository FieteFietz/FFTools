package com.fftools.scripts;

import magellan.library.Ship;
import magellan.library.Unit;


public class Ifnotshipok extends Script{
	
	private static final int Durchlauf = 20;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ifnotshipok() {
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
		
		
		boolean shipNotOK=false;
		
		// nicht OK, wenn nicht an Bord...
		Ship s = this.scriptUnit.getUnit().getShip();
		if (s==null){
			this.doNotConfirmOrders("!!! IfNotShipOK macht keinen Sinn, wenn ich nicht an Bord bin!!!");
			return;
		}
		
		// nicht OK, wenn kein Kapitän
		Unit u = s.getOwnerUnit();
		if (u==null){
			this.doNotConfirmOrders("!!! IfNotShipOK macht keinen Sinn, den das Schiff hat keinen Kapitän!!!");
			return;
		}
		
		if (!u.equals(this.scriptUnit.getUnit())){
			this.doNotConfirmOrders("!!! IfNotShipOK macht keinen Sinn, wenn ich nicht der Kapitän bin!!!");
			return;
		}
		
		
		// nicht OK, wenn noch zu reparieren
		if (s.getDamageRatio()>0){
			this.addComment("IfNotShipOK: Schiff noch nicht repariert.");
			shipNotOK = true;
		}
		
		// nicht OK, wenn noch zu bauen
		if (s.getSize()<s.getShipType().getMaxSize()){
			this.addComment("IfNotShipOK: Schiff noch nicht fertig.");
			shipNotOK = true;
		}
				
		// shipOK ist true, wenn schiff nicht repariert werden muss 
		// nächsten Parameter anschaunen..entweder eressea-befgehl = irgendetwas
		// oder schlüsselwort script...
		if (shipNotOK){
			String keyWord = super.getArgAt(0);
			if (keyWord.equalsIgnoreCase("script")) {
				if (super.getArgCount()>1) {
					// ok, in dieser Region soll ein script aufgerufen werden
					// eigentlich checken, ob dass von den scriptdurchläufen her passt
					// ansonsten parametersatz bauen und ergänzen....
					String newOrderLine = "";
					for (int i = 2;i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
					}
					super.scriptUnit.findScriptClass(super.getArgAt(1), newOrderLine,true);
					// this.addComment("added script order: " + newOrderLine + " (IfNotShipOK)");
				} else {
					// die befehlszeile endet mit dem keyWord script
					super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script)");
					addOutLine("X....Unerwartetes Ende der Befehlszeile (IfNotShipOK,script): " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				}
			} else {
				// kein script Befehl...alles was jetzt kommt als Order verpacken...
				// inkl des ersten wortes
				String newOrderLine = "";
				for (int i = 0;i<super.getArgCount();i++){
					newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
				}
				if (newOrderLine.length()>0){
					// nun denn ... fertig und irgendetwas zu schreiben
					newOrderLine = newOrderLine.concat(" ;script ifNotShipOK");
					super.addOrder(newOrderLine,true);
					super.addComment("Unit wurde durch IfNotShipOK bestaetigt", true);
				}
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
