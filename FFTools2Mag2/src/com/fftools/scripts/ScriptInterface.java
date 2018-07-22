package com.fftools.scripts;

	/**
	 * 
	 * Diese Interface wird von Script.java implementier.
	 * Es stellt sicher, dass benoetigte methoden in den Scripten ueberschrieben werden
	 * 
	 * 
	 * @author Fiete
	 *
	 */


public interface ScriptInterface {
	
	
	
	
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
	public void runScript(int scriptDurchlauf);
		
	/**
	 * legt fest, ob mehere scripte dieser art pro unit registriert werden dürfen
	 * @return
	 */
	public boolean allowMultipleScripts();
	
	
	/**
	 * wenn allowMulitpleScripts = false und doch ein weiteres hinzugefügt werden
	 * soll, wird hier festgelegt, ob eine Warnung ausgegeben werden soll
	 * @return
	 */ 
	public boolean errorMsgIfNotAllowedAddedScript();
	
}
