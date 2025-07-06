package com.fftools.scripts;

import java.util.Iterator;

import magellan.library.ID;
import magellan.library.Region;
import magellan.library.Unit;

public class Ifunit extends Script{

	private int[] runners = {14,15,16};
	private boolean scriptCalled = false;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ifunit() {
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
		
		// an arg(0) die unit (name oder unitID)
		// ab arg(1) die befehle, mindestens einer
		// von wegen name..." " mit "_" austauschen und umgekehrt
		
		if (super.getArgCount()<2) {
			// falsche Anzahl Paras
			super.scriptUnit.doNotConfirmOrders("Falscher Aufruf von IfUnit: zu geringe Anzahl Parameter.");
			
			addOutLine("X....Falscher Aufruf von IfUnit: zu geringe Anzahl Parameter: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		} else {
			// gibts die unit?
			String unitDesc = super.getArgAt(0);
			// falls mit "_" durch " " ersetzen
			unitDesc = unitDesc.replace("_".charAt(0), " ".charAt(0));
			boolean found = false;
			
			// Witzbold check..sich selber suchen ?
			Unit checkU = super.scriptUnit.getUnit();
			if (checkU.getName().equalsIgnoreCase(unitDesc) || checkU.toString(false).equalsIgnoreCase(unitDesc)){
				// soso, ein Witzbold..
				super.scriptUnit.doNotConfirmOrders("Ich bin ich, meiner selbst, und mir - ganz ich. (IfUnit");
				addOutLine("X....Falscher Aufruf von IfUnit: auf sich selbst angewendet: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			} else {
				// Die Region mal holen
				Region actR = super.scriptUnit.getUnit().getRegion();
				
				// units durchlaufen
				for (Iterator<Unit> iter = actR.units().iterator();iter.hasNext();){
					Unit actU = (Unit)iter.next();
					// und prüfen
					String test = actU.getName();
					ID test2 = actU.getID();
					if (test != null && test2 != null){
						if (actU.getName().equalsIgnoreCase(unitDesc) || actU.toString(false).equalsIgnoreCase(unitDesc)){
							// ergänzung...und sich selbst ausschliessen
							if (!super.scriptUnit.getUnit().getID().equals(actU.getID())) {
								found = true;
								break;
							}
						}
					}
				}
			}
			// Ergebnis ?
			if (found){
				// gefunden...
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
						super.addComment("Unit " + unitDesc + " gefunden. Aktiviere script mit: " + super.getArgAt(2) + " " + newOrderLine, true);
					} else {
						// die befehlszeile endet mit dem keyWord script
						super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script IfUnit)");
						addOutLine("X....Unerwartetes Ende der Befehlszeile (IfUnit, script): " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
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
						newOrderLine = newOrderLine.concat(" ;script ifunit");
						super.addOrder(newOrderLine,true);
						super.addComment("Unit wurde durch IfUnit bestaetigt (" + unitDesc + " gefunden)", true);
					}
				}
			} else {
				// kann später raus...zeigen, dass man wenigstens gesucht hat
				super.addComment("Unit " + unitDesc + " nicht gefunden.", true);
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
