package com.fftools.scripts;

import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Item;
import magellan.library.rules.ItemType;


public class Maxbestand extends MatPoolScript{
	
	
	private int Durchlauf_NachMatPool = 620;
	
	private int[] runners = {Durchlauf_NachMatPool};
	
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	public Maxbestand() {
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
			checkmaxBestand();
		}
	}
	
	private void checkmaxBestand() {
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		String ware=OP.getOptionString("ware");
		Boolean OK=true;
		if (ware.length()>0) {
			this.addComment("maxBestand - Ware erkannt: " + ware);
		} else {
			this.addComment("!maxBestand - keine Ware erkannt!");
			doNotConfirmOrders("!maxBestand - keine Ware erkannt!");
			OK=false;
		}
		Integer menge = OP.getOptionInt("Menge", -1);
		if (menge>=0) {
			this.addComment("maxBestand - Maximalmenge erkannt: " + menge);
		} else {
			this.addComment("!maxBestand - keine Menge erkannt!");
			doNotConfirmOrders("!maxBestand - keine Menge erkannt!");
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
			addOutLine("!!!:maxBestand...WAS? ...kein Gegenstand und keine Kategorie:" + ware + " " + this.unitDesc());
			super.scriptUnit.doNotConfirmOrders("maxBestand...WAS? ...kein Gegenstand und keine Kategorie -> Unit unbestaetigt (" + ware +")");
			return;
		}
		
		
		
		if (isCat) {
			// Kategorie
			Integer vorhanden=0;
			for (ItemType iT:reportSettings.getItemTypes(ware)) {
				Item i = this.scriptUnit.getModifiedItem(iT);
				if (i!=null) {
					vorhanden += i.getAmount();
					addComment("maxBestand - " + i.getAmount() + " " + iT.getName() + " (" + ware + ") gefunden.");
				}
			}
			addComment("maxBestand - Summe gefunden für " + ware + ": " + vorhanden);
			if (vorhanden > menge) {
				addComment("maxBestand - " + menge + " (" + ware + ") überschritten! Unbestätigt!");
				doNotConfirmOrders("maxBestand - " + menge + " (" + ware + ") überschritten! Unbestätigt!");
			} else {
				addComment("maxBestand für " + ware + " ist nicht erreicht.");
			}
		} else {
			// item
			Item i = this.scriptUnit.getModifiedItem(itemType);
			if (i!=null) {
				addComment("maxBestand - " + ware + " " + i.getAmount() + " vorhanden.");
				if (i.getAmount()>menge) {
					OK=false;
				} 
			} else {
				addComment("maxBestand - keine Information zu " + ware + " vorhanden.");
				OK=true;
			}
			if (!OK) {
				addComment("maxBestand - " + menge + " (" + ware + ") überschritten! Unbestätigt!");
				doNotConfirmOrders("maxBestand - " + menge + " (" + ware + ") überschritten! Unbestätigt!");
			} else {
				addComment("maxBestand für " + ware + " ist nicht erreicht.");
			}
		}
	}

	public boolean allowMultipleScripts(){
		return true;
	}
}
