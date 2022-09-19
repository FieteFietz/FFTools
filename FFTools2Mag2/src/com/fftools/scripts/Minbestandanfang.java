package com.fftools.scripts;

import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Item;
import magellan.library.rules.ItemType;

public class Minbestandanfang extends Script{
	
	private int[] runners = {14,15,16};
	private boolean scriptCalled = false;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Minbestandanfang() {
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
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		String ware=OP.getOptionString("ware");
		Boolean OK=true;
		if (ware.length()>0) {
			this.addComment("minBestandAnfang - Ware erkannt: " + ware);
		} else {
			doNotConfirmOrders("!minBestandAnfang - keine Ware erkannt!");
			OK=false;
		}
		Integer menge = OP.getOptionInt("Menge", -1);
		if (menge>=0) {
			this.addComment("minBestandAnfang - Minimalmenge erkannt: " + menge);
		}
		
		Integer mengeJe = OP.getOptionInt("MengeJe", -1);
		if (mengeJe>=0) {
			this.addComment("minBestandAnfang - Minimalmenge pro Person erkannt: " + mengeJe);
		}
		
		if (menge<=0 && mengeJe<=0) {
			this.doNotConfirmOrders("!!! minBestandAnfang: weder Menge noch MengeJe angegeben - Abbruch!");
			OK=false;
		}
		
		if (menge>0 && mengeJe>0) {
			this.doNotConfirmOrders("!!! minBestandAnfang: Menge UND MengeJe angegeben - nur ein Wert zulässig. Abbruch!");
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
			addOutLine("!!!:minBestandAnfang...WAS? ...kein Gegenstand und keine Kategorie:" + ware + " " + this.unitDesc());
			super.scriptUnit.doNotConfirmOrders("minBestandAnfang...WAS? ...kein Gegenstand und keine Kategorie -> Unit unbestaetigt (" + ware +")");
			return;
		}
		
		
		boolean runMyScript=false;
		
		
		if (isCat) {
			// Kategorie
			Integer vorhanden=0;
			for (ItemType iT:reportSettings.getItemTypes(ware)) {
				Item i = this.scriptUnit.getUnit().getModifiedItem(iT);
				if (i!=null) {
					vorhanden += i.getAmount();
					addComment("minBestandAnfang - " + i.getAmount() + " " + iT.getName() + " (" + ware + ") gefunden.");
				}
			}
			addComment("minBestandAnfang - Summe gefunden für " + ware + ": " + vorhanden);
			Integer vorhandenJe=0;
			if (mengeJe>0) {
				if (this.getUnit().getModifiedPersons()>0) {
					vorhandenJe = Math.floorDiv(vorhanden,this.getUnit().getModifiedPersons());
					addComment("minBestandAnfang - Summe " + ware + " pro Person: " + vorhandenJe);
				} else {
					this.doNotConfirmOrders("minBestandAnfang pro Person ohne Personen ?!? (keine Personen mehr in der Einheit)");
				}
			}
			
			if (menge>0) {
				if (vorhanden < menge) {
					addComment("minBestandAnfang - " + menge + " (" + ware + ") unterschritten.");
					
				} else {
					addComment("minBestandAnfang - " + menge + " (" + ware + ") ist erreicht, Befehl wird ausgeführt.");
					runMyScript=true;
				}
			}
			
			if (mengeJe>0) {
				if (vorhandenJe < mengeJe) {
					addComment("minBestandAnfang - " + mengeJe + " (" + ware + ") pro Person unterschritten.");
					
				} else {
					addComment("minBestandAnfang - " + mengeJe + " (" + ware + ") pro Person ist erreicht, Befehl wird ausgeführt.");
					runMyScript=true;
				}
			}
		} else {
			// item
			Item i = this.scriptUnit.getUnit().getModifiedItem(itemType);
			if (i!=null) {
				Integer vorhanden = i.getAmount();
				addComment("minBestandAnfang - " + ware + " " + vorhanden + " vorhanden.");
				Integer vorhandenJe=0;
				if (mengeJe>0) {
					if (this.getUnit().getModifiedPersons()>0) {
						vorhandenJe = Math.floorDiv(vorhanden,this.getUnit().getModifiedPersons());
						addComment("minBestandAnfang - Summe " + ware + " pro Person: " + vorhandenJe);
					} else {
						this.doNotConfirmOrders("minBestandAnfang pro Person ohne Personen ?!? (keine Personen mehr in der Einheit)");
					}
				}
				
				if (menge>0) {
					if (vorhanden >= menge) {
						addComment("minBestandAnfang - " + menge + " (" + ware + ") ist erreicht, Befehl wird ausgeführt.");
						runMyScript=true;
					} else {
						addComment("minBestandAnfang - " + menge + " (" + ware + ") unterschritten.");
					}
				}
				
				if (mengeJe>0) {
					if (vorhandenJe >= mengeJe) {
						addComment("minBestandAnfang - " + mengeJe + " (" + ware + ") pro Person ist erreicht, Befehl wird ausgeführt.");
						runMyScript=true;
					} else {
						addComment("minBestandAnfang - " + mengeJe + " (" + ware + ") pro Person unterschritten.");
					}
				}

			} else {
				addComment("minBestandAnfang - keine Information zu " + ware + " vorhanden.");
				
			}
		}
		
		if (runMyScript) {
			// OK...los gehts
			String keyWord = super.getArgAt(2);
			if (keyWord.equalsIgnoreCase("script")) {
				if (super.getArgCount()>3) {
					// ok, in dieser Region soll ein script aufgerufen werden
					// eigentlich checken, ob dass von den scriptdurchläufen her passt
					// ansonsten parametersatz bauen und ergänzen....
					String newOrderLine = "";
					for (int i = 4;i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
					}
					super.scriptUnit.findScriptClass(super.getArgAt(3), newOrderLine,true);
				} else {
					// die befehlszeile endet mit dem keyWord script
					super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script minBestandAnfang)");
					addOutLine("X....Unerwartetes Ende der Befehlszeile (minBestandAnfang,script): " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				}
			} else {
				// kein script Befehl...alles was jetzt kommt als Order verpacken...
				// inkl des ersten wortes
				String newOrderLine = "";
				for (int i = 2;i<super.getArgCount();i++){
					newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
				}
				if (newOrderLine.length()>0){
					// nun denn ... fertig und irgendetwas zu schreiben
					newOrderLine = newOrderLine.concat(" ;script minBestandAnfang");
					super.addOrder(newOrderLine,true);
				} else {
					super.addComment("!!! minBestandAnfang: kein Befehl erkannt!", true);
					super.doNotConfirmOrders("minBestandAnfang ohne Befehl");
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
