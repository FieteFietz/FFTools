package com.fftools.scripts;

import java.util.ArrayList;

import magellan.library.rules.ItemType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;


public class Request extends MatPoolScript{
	
	private int Durchlauf_VorMatPool = 52;
	private int Durchlauf_NachMatPool = 410;
	
	private final int requestDefaultPrio=10;
	
	private int[] runners = {Durchlauf_VorMatPool,Durchlauf_NachMatPool};
	
	private ArrayList<String> specs=null;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	public Request() {
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
		
		if (scriptDurchlauf==this.Durchlauf_VorMatPool){
			this.anforderung();
		}
		
		if (scriptDurchlauf==this.Durchlauf_NachMatPool){
			// ToDo ?!
		}
		
	}
	
	
	private void anforderung(){
		//		 Request bildet einfach nur eine request der unit an den MatPool
		// format // script request <anzahl> <Gegenstand||Kategoriename> [Prio]
		// die Kategorienamen müssen definiert werden mittels
		// script setPrio <Kategorie> <Gegenstand> <Prio>
		
		//		 falls kein parameter bzw zu viele
		if (super.getArgCount()< 2){
			addOutLine(super.scriptUnit.getUnit().toString(true) + ": request...unpassende Anzahl Parameter");
			super.scriptUnit.doNotConfirmOrders("Request...unpassende Anzahl Parameter -> Unit unbestaetigt");
			return;
		}
		
		// Parameter durchgehen und checken
		// Anzahl
		//		 falls nicht Anzahl angegeben wurde....
		int anzahl = 0;
		
		if (super.getArgAt(0).equalsIgnoreCase("alles")){
			anzahl = Integer.MAX_VALUE;
		} else if (super.getArgAt(0).equalsIgnoreCase("je")){
			anzahl = this.scriptUnit.getUnit().getModifiedPersons();
			if (anzahl==0){
				super.addComment("Request je unbearbeitet, da Personenzahl = 0", true);
				return;
			}
		} else {
			try {
				anzahl = Integer.parseInt(super.getArgAt(0));
			} catch (NumberFormatException e){
				// Fehlerausgabe unten...weil anzahl bleibt 0
			}
			
		}
		if (anzahl == 0){
			addOutLine(super.scriptUnit.getUnit().toString(true) + ": Request...unpassende Anzahl");
			super.scriptUnit.doNotConfirmOrders("Request...unpassende Anzahl -> Unit unbestaetigt");
			return;
		}
		
		// Gegenstand oder Kategoriename
		// Versuch Gegenstand
		String itemName = super.getArgAt(1);
		
		// replacen
		if (itemName!=null && itemName.length()>0){
			itemName = FFToolsGameData.translateItemShortform(itemName);
		}
				
		ItemType itemType = super.gd_Script.getRules().getItemType(itemName);
		boolean isCat = false;
		if (itemType==null){
			// Versuch der Kategorie
			isCat = reportSettings.isInCategories(super.getArgAt(1));
		}
		if (itemType == null && !isCat){
			addOutLine("!!!:Request...WAS? ...kein Gegenstand und keine Kategorie:" + itemName + " " + this.unitDesc());
			super.scriptUnit.doNotConfirmOrders("Request...WAS? ...kein Gegenstand und keine Kategorie -> Unit unbestaetigt (" + itemName +")");
			return;
		}
		
		
		// Prio, falls angegeben
		int Prio = this.requestDefaultPrio;
		if (super.getArgCount()> 2){
			int test = 0;
			try {
				test = Integer.valueOf(super.getArgAt(2));
			} catch (NumberFormatException e){}
			if (test==0){
				addOutLine("!!! " + super.scriptUnit.getUnit().toString(true) + ": Request...keine gültige Prio");
				super.scriptUnit.doNotConfirmOrders(": Request...keine gültige Prio -> Unit unbestaetigt");
				return;
			}
			Prio = test;
		}
		
		
		
		// soweit gekommen..wohl alles schön
		
		int kapaPolicy = MatPoolRequest.KAPA_unbenutzt;
		int kapaUser = 0;
		// kapa-beschränkungen?
		// OptionParser benutzen...weil dass ja mit = Geschrieben wird
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// gibts es den kapaeintrag?
		if (OP.getOptionString("kapa").length()>0){
			// ja, wir haben eine kapa angabe...
			if (OP.isOptionString("kapa", "gehen")){
				kapaPolicy = MatPoolRequest.KAPA_max_zuFuss;
			}
			if (OP.isOptionString("kapa", "reiten")){
				kapaPolicy = MatPoolRequest.KAPA_max_zuPferd;
			}
			int benutzerKapa = OP.getOptionInt("kapa", -1);
			if (benutzerKapa>0){
				kapaPolicy = MatPoolRequest.KAPA_benutzer;
				kapaUser=benutzerKapa;
			}
		}
		
		int benutzerWeight=OP.getOptionInt("gewicht", -1);
		if (benutzerWeight<=0){
			benutzerWeight=OP.getOptionInt("weight", -1);
		}
		if (benutzerWeight>0){
			kapaPolicy = MatPoolRequest.KAPA_weight;
			kapaUser=benutzerWeight;
		}
		
		MatPoolRequest m = new MatPoolRequest(this,anzahl,itemName,Prio,"Request",kapaPolicy,kapaUser);
		
		// Specs
		String specsString = OP.getOptionString("Spec");
		if (specsString.length()>2){
			// splitten, falls mehrere da
			String[] specsArray = specsString.split(",");
			this.specs = new ArrayList<String>();
			for (int i=0;i<specsArray.length;i++){
				String s2 = specsArray[i];
				this.specs.add(s2);
				m.addSpec(s2);
			}
		}
		
		// nur innerhalb der Region
		if (OP.getOptionBoolean("region", false)){
			m.setOnlyRegion(true);
		}
		
		
		// sortMode
		if (OP.getOptionString("sort").equalsIgnoreCase("amount")){
			m.setTMsortMode(MatPoolRequest.TM_sortMode_amount);
		}
		
		// changePrio
		if (!OP.getOptionBoolean("prioChange", true)){
			m.setPrioChange(false);
		}
		
		// validation
		if (OP.getOptionBoolean("region", false) && specsString.length()>2){
			// in der region only und trotzdem transporterSpecs ?!
			this.scriptUnit.doNotConfirmOrders("!!! Region=ja und TransporterDefs gesetzt !!!");
			outText.addOutLine("!!! Region=ja und TransporterDefs gesetzt !!! (" + this.unitDesc() + ")", true);		
		}
		
		this.addMatPoolRequest(m);
	}
	
	/**
	private void check(){
		Collection myRequests = this.getRequests();
		if (myRequests==null){
			//be fail safe..something's gone wrong
			outText.addOutLine("!!! Request does not find it´s own request!");
			return;
		}
		// durch alle Requests dieser Unit durchgehen
		for (Iterator iter = myRequests.iterator();iter.hasNext();){
			// wir erhalten prinzipiell vom iterator ein object
			MatPoolRequest o = (MatPoolRequest)iter.next();
			// wenn dies eine Instanz von RequestRelation ist (keine andere MatPoolRelation)
			if (o.getScript().equals(this)){
				// und wie früher checken
				check_mpr(o);
			}
		}
	}
	**/
	
	/**
	 * Sollte bei Bedarf ueberschrieben werden
	 * wird aufgerufen, sobald MatPool die MatPoolRelation vollstaendig 
	 * in dem aktuellen Durchlauf bearbeitet hat.
	 * 
	 * 
	 * @param _mpr
	 */
	/**
	public void check_mpr(MatPoolRequest _mpr){
		// zu testzwecken erfolg kurz mitteilen...
		
		// einschränken,,,wenn nicht "alles" gefordert wurde
		if (!this.request_alles) {
			if (_mpr.getBearbeitet()<_mpr.getOriginalGefordert()){
				// nicht voll erfüllt
				super.addComment("Request nach " + _mpr.getOriginalGefordert() + " " + _mpr.getOriginalGegenstand() + "(Prio " + _mpr.getPrio() +  ") nicht voll erfüllt (" + _mpr.getBearbeitet() + ")", true);
			} else {
				// wenn alles schön, nix ausgeben,
				// fein, Marc?
			}
		}
		// wenn mit alles angefordert wurde, derzeit keine Ausgabe...
	}
	**/
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
}
