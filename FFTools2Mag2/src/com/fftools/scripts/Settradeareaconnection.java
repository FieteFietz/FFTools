package com.fftools.scripts;

import com.fftools.ScriptUnit;
import com.fftools.trade.TradeAreaConnector;
import com.fftools.utils.FFToolsOptionParser;



/**
 * 
 * Definiert eine Handelsbeziehung zwischen 2 TradeAreas anhand 2er Units
 * 
 * @author Fiete
 *
 */

public class Settradeareaconnection extends TradeAreaScript{
	
	

	private static final int Durchlauf_vorMP1 = 33;
	private static final int Durchlauf_nachMP1 = 330;
	
	
	private int[] runners = {Durchlauf_vorMP1,Durchlauf_nachMP1};
	
	private TradeAreaConnector myTAC = null;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Settradeareaconnection() {
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
		
		if (scriptDurchlauf==Durchlauf_vorMP1){
			this.scriptStart();
		}
		
		if (scriptDurchlauf==Durchlauf_nachMP1){
			this.scriptStart_nachMP1();
		}
		
		
	}
	
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Pflichtfelder
		// Zielunit
		String targetUnitName = OP.getOptionString("ziel");
		if (targetUnitName.length()==0){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: ziel fehlt! -> Unbestaetigt!!");
			return;
		}
		
		ScriptUnit scu = this.scriptUnit.getScriptMain().getScriptUnit(targetUnitName);
		if (scu==null){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: ziel nicht gefunden! -> Unbestaetigt!!");
			return;
		}
		String Name = OP.getOptionString("name");
		if (Name.length()<2){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: Name fehlt!!! -> unbestaetigt");
			return;
		}
		
		int maxNumberOfMoversIfIdle = 1;
		maxNumberOfMoversIfIdle = OP.getOptionInt("maxNumbersOfMoversIfIdle", 1);
		
		int speed = OP.getOptionInt("speed", 6);
		
		String erg = super.getTradeAreaHandler().addTradeAreaConnector(this.scriptUnit,scu, Name,maxNumberOfMoversIfIdle,speed);
		if (erg!=""){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC gescheitert: " + erg);
			return;
		} else {
			this.addComment("setTAC erfolgreich (" + Name + ")");
			TradeAreaConnector actTAC = super.getTradeAreaHandler().getTAC(Name);
			if (actTAC==null || !actTAC.isValid()){
				this.doNotConfirmOrders();
				this.addComment("!!! ein TAC " + Name + " konnte nicht gefunden werden! -> nicht bestätigt");
			} else {
				this.myTAC = actTAC;
			}
		}
	}
	
	
	private void scriptStart_nachMP1(){
		if (this.myTAC!=null) {
			this.addComment("TAC-Info zu: " + this.myTAC.getName());
			int allMovers = this.myTAC.getOverallMoverKapa();
			this.addComment(this.myTAC.getMoverInfo());
			int allWeight = this.myTAC.getNeededGE();
			this.addComment("GE insgesammt benötigt: " + allWeight + " (" + myTAC.getWeightInfo() + ")" + " (for TAC " + this.myTAC.getName() + ")");
			this.addComment("GE zugeordnet: " + allMovers + " GE ");
			if (allMovers<allWeight) {
				// Problem, oder?!
				this.addComment("!!! Dem TAC " + this.myTAC.getName() + " ist zu wenig Kapazität zugeodnet!!! (daher unbestätigt)");
				this.doNotConfirmOrders();
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
