package com.fftools.scripts;

import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Item;
import magellan.library.rules.ItemType;


public class Minbestand extends MatPoolScript{
	
	
	private int Durchlauf_NachMatPool = 620;
	
	private int[] runners = {Durchlauf_NachMatPool};
	
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	public Minbestand() {
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
		if (scriptDurchlauf==this.Durchlauf_NachMatPool){
			checkMinBestand();
		}
	}
	
	private void checkMinBestand() {
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		String ware=OP.getOptionString("ware");
		Boolean OK=true;
		if (ware.length()>0) {
			this.addComment("minBestand - Ware erkannt: " + ware);
		} else {
			this.addComment("!minBestand - keine Ware erkannt!");
			doNotConfirmOrders("!minBestand - keine Ware erkannt!");
			OK=false;
		}
		Integer menge = OP.getOptionInt("Menge", -1);
		if (menge>=0) {
			this.addComment("minBestand - mindestMenge erkannt: " + menge);
		} else {
			this.addComment("!minBestand - keine Menge erkannt!");
			doNotConfirmOrders("!minBestand - keine Menge erkannt!");
			OK=false;
		}
		
		if (!OK) {
			return;
		}
		
		ware = FFToolsGameData.translateItemShortform(ware);
		// item?
		ItemType itemType = super.gd_Script.getRules().getItemType(ware);
		boolean isCat = false;
		if (itemType==null){
			// Versuch der Kategorie
			isCat = reportSettings.isInCategories(ware);
		}
		if (itemType == null && !isCat){
			addOutLine("!!!:minBestand...WAS? ...kein Gegenstand und keine Kategorie:" + ware + " " + this.unitDesc());
			super.scriptUnit.doNotConfirmOrders("minbestand...WAS? ...kein Gegenstand und keine Kategorie -> Unit unbestaetigt (" + ware +")");
			return;
		}
		
		
		
		if (isCat) {
			// Kategorie
			Integer vorhanden=0;
			for (ItemType iT:reportSettings.getItemTypes(ware)) {
				Item i = this.scriptUnit.getModifiedItem(iT);
				if (i!=null) {
					vorhanden += i.getAmount();
					addComment("minBestand - " + i.getAmount() + " " + iT.getName() + " (" + ware + ") gefunden.");
				}
			}
			addComment("minbestand - Summe gefunden für " + ware + ": " + vorhanden);
			if (vorhanden < menge) {
				addComment("minBestand - " + menge + " (" + ware + ") unterschritten! Unbestätigt!");
				doNotConfirmOrders("minBestand - " + menge + " (" + ware + ") unterschritten! Unbestätigt!");
			} else {
				addComment("minbestand für " + ware + " ist erfüllt.");
			}
		} else {
			// item
			Item i = this.scriptUnit.getModifiedItem(itemType);
			if (i!=null) {
				addComment("minBestand - " + ware + " " + i.getAmount() + " vorhanden.");
				if (i.getAmount()<menge) {
					OK=false;
				} 
			} else {
				addComment("minbestand - keine Information zu " + ware + " vorhanden.");
				OK=false;
			}
			if (!OK) {
				addComment("minBestand - " + menge + " (" + ware + ") unterschritten! Unbestätigt!");
				doNotConfirmOrders("minBestand - " + menge + " (" + ware + ") unterschritten! Unbestätigt!");
			} else {
				addComment("minBestand für " + ware + " ist erfüllt.");
			}
		}
	}

	public boolean allowMultipleScripts(){
		return true;
	}
}
