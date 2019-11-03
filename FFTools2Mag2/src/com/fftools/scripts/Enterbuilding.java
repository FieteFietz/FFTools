package com.fftools.scripts;

import magellan.library.Building;
import magellan.library.Region;

import com.fftools.utils.FFToolsOptionParser;


public class Enterbuilding extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 25;
	

	
	private int[] runners = {Durchlauf_1};
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Enterbuilding() {
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
		if (scriptDurchlauf==this.Durchlauf_1){
			this.start();
		}

	}
	
	
	
	/**
	 * eigentlich ganz einfach: betrete das Gebäude, wenn ich noch nicht drinne bin
	 */
	private void start(){
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		String target = OP.getOptionString("target");
		// target angegeben ?
		if (target==""){
			this.doNotConfirmOrders("!!! EnterBuilding: Building NOT given !!!");
			return;
		}
		
		// target in region ?
		Region r;
		
		r = this.region();
		boolean isOK = false;
		Building targetBuilding = null;
		for (Building b:r.buildings()){
			if (b.getID().toString().equalsIgnoreCase(target)){
				isOK=true;
				targetBuilding = b;
				break;
			}
		}
		
		if (!isOK){
			this.doNotConfirmOrders("!!! EnterBuilding: Building NOT in region !!! : " + target);
			return;
		}
		
		// Bin ich schon drinne?
		Building actualBuilding;
		actualBuilding = this.getUnit().getModifiedBuilding();
		if (actualBuilding!=null && targetBuilding!=null && actualBuilding.equals(targetBuilding)){
			this.addComment("EnterBuilding: unit ist bereits in " + target);
			return;
		}
		
		
		// ich muss da noch rein
		this.addOrder("Betreten BURG " + target, true);
		this.addComment("EnterBuilding: Betrete-Befehl für " + target + " generiert");
		
	}

}
