package com.fftools.scripts;


/**
 * 
 * Dat Bauscript - zur fetslegung, was in der Region gebaut werden soll
 * verwendet vom Baumanager des TAs zum Zuordnen von Bauscripten
 * @author Fiete
 *
 */

public class Bauauftrag extends MatPoolScript{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf1 = 92;
	
	private int[] runners = {Durchlauf1};
	
	// Umsetzung des Auftrages in ein Script...
	private Bauen bauScript = null;
	
	
	// Konstruktor
	public Bauauftrag() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf1){
			this.run1();
		}
        

		
	}
	
	
	public void run1(){
		
		super.addVersionInfo();
		
		// Bauauftrag registrieren
		// eintragen
		this.getBauManager().addBauAuftrag(this);
		
		// Parameter wie bei einem Bauscript parsen
		// aber nix veranlassen
		
		this.bauScript = new Bauen();
		this.bauScript.setScriptUnit(this.scriptUnit);
		this.bauScript.setArguments(this.getArguments());
		this.bauScript.setGameData(this.gd_Script);
		this.bauScript.setPlaningMode(true);
		
		// Parsen und Ausgaben...
		this.bauScript.vorMatPool();
		
		
		
	}	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}


	/**
	 * @return the bauScript
	 */
	public Bauen getBauScript() {
		return bauScript;
	}	

}
