package com.fftools.scripts;




public class Runde extends Script{
	
	
	private static final int Durchlauf = 18;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Runde() {
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
	
	private void scriptStart(){
		// erwartetes format
		// script runde XXX befehl ...
		// script runde XXX script befehl ...
		
		// check für vollständigkeit notwendigkeit
		// Mindestparameterzahl: 2: runde und befehl/script
		
		if (super.getArgCount()<2){
			outText.addOutLine("!!! Fehlerhafter Rundenbefehl bei: " + this.unitDesc());
			this.scriptUnit.doNotConfirmOrders("!!! Fehlerhafter Rundenbefehl bei: " + this.unitDesc());
			return;
		}
		
		String rundenString = super.getArgAt(0);
		int sollRunde = 0;
		try {
			sollRunde = Integer.parseInt(rundenString);
		} catch (NumberFormatException e) {
			// pech gehabt
			outText.addOutLine("Fehlerhafte Rundenerkennung bei: " + this.unitDesc());
			this.scriptUnit.doNotConfirmOrders("!!! Fehlerhafte Rundenerkennung bei Runde");
			return;
		}
		
		int actRunde = this.gd_Script.getDate().getDate();
		this.addComment("Runde: erkannte Runde aktuell: " + actRunde);
		if (actRunde>sollRunde && sollRunde>0){
			// diese Zeile aus den orders entfernen
			this.scriptUnit.removeScriptOrder("Runde " + sollRunde);
			return;
		}
		if (actRunde==sollRunde){
			// gleiche runde!
			String keyWord = super.getArgAt(1);
			if (keyWord.equalsIgnoreCase("script")){
				// scriptbefehl dahinter
				if (super.getArgCount()>2) {
					// ok, in dieser Region soll ein script aufgerufen werden
					// eigentlich checken, ob dass von den scriptdurchläufen her passt
					// ansonsten parametersatz bauen und ergänzen....
					String newOrderLine = "";
					for (int i = 3;i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
					}
					this.addComment("Runde: Befehl aufgerufen " + super.getArgAt(2) + ", Params: " + newOrderLine);
					super.scriptUnit.findScriptClass(super.getArgAt(2), newOrderLine,true);
				} else {
					// die befehlszeile endet mit dem keyWord script
					super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script Runde)");
					addOutLine("X....Unerwartetes Ende der Befehlszeile (Runde, script):" + this.unitDesc());
				}
			} else {
				// kein scriptbefehl dahinter
				// kein script Befehl...alles was jetzt kommt als Order verpacken...
				// inkl des ersten wortes
				String newOrderLine = "";
				for (int i = 1;i<super.getArgCount();i++){
					newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
				}
				if (newOrderLine.length()>0){
					this.addComment("Runde - neuer Befehl: " + newOrderLine);
					// nun denn ... fertig und irgendetwas zu schreiben
					newOrderLine = newOrderLine.concat(" ;script Runde");
					super.addOrder(newOrderLine,true);
				} else {
					this.addComment("Runde - kein neuer Befehl erkannt");
				}
			}
		}
		// wenn sollrunde in der Zukunft: nix machen
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
