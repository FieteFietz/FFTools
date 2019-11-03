package com.fftools.scripts;

import magellan.library.CoordinateID;


public class Ifnotregion extends Script{
	
	private static final int Durchlauf = 16;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ifnotregion() {
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
		
		// hier code fuer Script
		boolean parseOK = true;
		boolean rightRegion = true;
		// zur Sicherheit..falls parsen schief geht, wird keine Region gefunden...
		
		// Integer xInt = Integer.MIN_VALUE;
		// Integer yInt = Integer.MIN_VALUE;
		// addOutLine("....scriptstart IfRegion mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<2) {
			// falsche Anzahl Paras
			super.scriptUnit.doNotConfirmOrders("Falscher Aufruf von IfNotRegion: zu geringe Anzahl Parameter.");
			parseOK=false;
			addOutLine("X....Falscher Aufruf von IfNotRegion: zu geringe Anzahl Parameter: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
		
		if (super.getArgCount()>1) {

			// Falls Komma in Regionsangabe sind es wohl Koordinaten...
			if (super.getArgAt(0).indexOf(',') > 0) {
				CoordinateID actDest = CoordinateID.parse(super.getArgAt(0),",");
				if (actDest == null){
					// komische Regionsangabe
					super.scriptUnit.doNotConfirmOrders("Fehler beim Erkennen der Regionskoordinaten");
					parseOK=false;
					addOutLine("X....Fehler beim Erkennen der Regionskoordinaten: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				}
				// sind wir in der richtigen Region ? == andere als angegeben
				// erst dann macht es Sinn, sich mit den sonstigen Argumenten zu beschäftigen...
				if (parseOK) {
					CoordinateID regionCoordinateID = super.scriptUnit.getUnit().getRegion().getCoordinate();
					if (regionCoordinateID.equals(actDest)){
						rightRegion = false;
					}
				}
			} else {
				String regionWanted = super.getArgAt(0).replace("_", " ");
				if (regionWanted == null) {
					super.scriptUnit.doNotConfirmOrders("Fehler beim Erkennen des Regionsnamens");
					parseOK=false;
					addOutLine("X....Fehler beim Erkennen des Regionsnamens: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				}
				// Ok, Name eingelesen, also Prüfung auf gewünschte Region --> NICHT die Region, in der wir gerade sind!
				if (parseOK) {
					String regionName = super.scriptUnit.getUnit().getRegion().getName();
					if (regionName.equals(regionWanted)) {
						rightRegion = false;
					}
				}
			}
				
				
				
			// rightRegion ist nur dann true, wenn nicht in Richtiger Region und ParsenOK..somit alles fein
			// nächsten Parameter anschaunen..entweder eressea-befgehl = irgendetwas
			// oder schlüsselwort script...
			if (rightRegion){
				String keyWord = super.getArgAt(1);
				if (keyWord.equalsIgnoreCase("script")) {
					if (super.getArgCount()>2) {
						// ok, in dieser Region soll ein script aufgerufen werden
						// eigentlich checken, ob dass von den scriptdurchläufen her passt
						// ansonsten parametersatz bauen und ergänzen....
						String newOrderLine = "";
						for (int i = 3;i<super.getArgCount();i++){
							newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
						}
						super.scriptUnit.findScriptClass(super.getArgAt(2), newOrderLine,true);
					} else {
						// die befehlszeile endet mit dem keyWord script
						super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script)");
						addOutLine("X....Unerwartetes Ende der Befehlszeile (IfNotRegion,script): " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
					}
				} else {
					// kein script Befehl...alles was jetzt kommt als Order verpacken...
					// inkl des ersten wortes
					String newOrderLine = "";
					boolean needDNT = false;
					for (int i = 1;i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
						if (super.getArgAt(i).equalsIgnoreCase("GIB")){
							// damit die order nicht beim nächsten MatPool gelöscht wird...
							needDNT=true;
						}
					}
					if (newOrderLine.length()>0){
						// nun denn ... fertig und irgendetwas zu schreiben
						if (needDNT){
							newOrderLine = newOrderLine.concat(" ;dnt");
						}
						super.addOrder(newOrderLine,true);
						super.addComment("Unit wurde durch IfNotRegion bestaetigt", true);
					}
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
