package com.fftools.scripts;

public class Ifregiontype extends Script{
	
	private int[] runners = {14,15,16};
	private boolean scriptCalled = false;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ifregiontype() {
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
		
		// hier code fuer Script
		boolean parseOK = true;
		boolean rightRegionType = false;
		// zur Sicherheit..falls parsen schief geht, wird keine Region gefunden...
		
		// Integer xInt = Integer.MIN_VALUE;
		// Integer yInt = Integer.MIN_VALUE;
		// addOutLine("....scriptstart IfRegion mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<2) {
			// falsche Anzahl Paras
			super.scriptUnit.doNotConfirmOrders("Falscher Aufruf von IfRegionType: zu geringe Anzahl Parameter.");
			parseOK=false;
			addOutLine("X....Falscher Aufruf von IfRegionType: zu geringe Anzahl Parameter: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
		
		if (super.getArgCount()>1) {

			String regionWanted = super.getArgAt(0).replace("_", " ");
			if (regionWanted == null || regionWanted.length()==0) {
				super.scriptUnit.doNotConfirmOrders("Fehler beim Erkennen des RegionsTypes (IfRegionType");
				parseOK=false;
				addOutLine("X....Fehler beim Erkennen des RegionsTypes: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			}
			// Ok, Name eingelesen, also Prüfung auf richtigen RegionsTypen
			regionWanted = regionWanted.replace("~", " ");
			if (parseOK) {
				if (this.getUnit().getRegion().getRegionType().getName().equalsIgnoreCase(regionWanted)) {
					rightRegionType=true;
					this.addComment("IfRegionType: " + regionWanted + " als vorhanden erkannt");
				} else {
					this.addComment("IfRegionType: " + regionWanted + " als NICHT vorhanden erkannt");
				}
			}
			
				
			// rightRegion ist nur dann true, wenn in Richtiger Region und ParsenOK..somit alles fein
			// nächsten Parameter anschaunen..entweder eressea-befgehl = irgendetwas
			// oder schlüsselwort script...
			if (rightRegionType){
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
						super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script IfRegionType)");
						addOutLine("X....Unerwartetes Ende der Befehlszeile (IfRegionType,script): " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
					}
				} else {
					// kein script Befehl...alles was jetzt kommt als Order verpacken...
					// inkl des ersten wortes
					String newOrderLine = "";
					for (int i = 1;i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
					}
					if (newOrderLine.length()>0){
						// nun denn ... fertig und irgendetwas zu schreiben
						newOrderLine = newOrderLine.concat(" ;script ifregionType");
						super.addOrder(newOrderLine,true);
						super.addComment("Unit wurde durch IfRegionType bestaetigt", true);
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
