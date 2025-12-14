package com.fftools.scripts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.MaterialHubManager_MHM;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;

import magellan.library.CoordinateID;
import magellan.library.Item;
import magellan.library.Order;
import magellan.library.Unit;
import magellan.library.rules.ItemType;


/**
 * Verkehrt zwischen MaterialHUbs (MH) und transportiert durch // script Material benötigte Waren
 * @author Fiete
 *
 */
public class Materialtransport extends MatPoolScript {
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private static final int Durchlauf_01 = 22; // nach Depot, vor Sailto, nach MH
	private static final int Durchlauf_01_01 = 30; // nach MatPool -> Anmeldung beim ZielMH
	private static final int Durchlauf_02 = 520; // nach MHM und MatPool - Silbercheck / Lernen (?)
	
	private int[] runners = {Durchlauf_01,Durchlauf_01_01,Durchlauf_02};
	
	private String Lernplan="";
	private String Lerntalent="";
	
	private int startRunde = 0;
	private int ETA = 0;
	
	private boolean MaterialHubTransporterSettings_found = false;
	private boolean announce_me_in_target_region = false; 
	
	public CoordinateID targetRegionCoord = null;
	public CoordinateID actRegionCoord = null;
	public CoordinateID HomeRegionCoord = null;
	
	public Materialtransport(){
		super.setRunAt(this.runners);
	}
	
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf==Durchlauf_01){
			this.run_01();
		}
		if (scriptDurchlauf==Durchlauf_01_01){
			this.run_01_01();
		}
		if (scriptDurchlauf==Durchlauf_02){
			this.run_02();
		}
		
		
	}
	
	private void run_01(){
		
		super.addVersionInfo();
		
		// Optionen einlesen
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"MaterialTransport");
		OP.addOptionList(this.getArguments());
		
		/*
		 * // script MaterialTransport Home=X,Y [LernPlan=XYZ | LernTalent=XYZ]
		 */
		
		this.Lernplan = OP.getOptionString("Lernplan");
		this.Lerntalent = OP.getOptionString("Talent");
		
		String homeString=OP.getOptionString("home");
		if (homeString.length()>2){
			CoordinateID actDest = null;
			if (homeString.indexOf(',') > 0) {
				actDest = CoordinateID.parse(homeString,",");
			}
			if (actDest!=null){
				this.HomeRegionCoord = actDest;
			} 
		}
		
		if (this.HomeRegionCoord==null) {
			this.doNotConfirmOrders("!!! MT: HOME Angabe nicht erkannt! (zwingend erforderlich)");
			return;
		}
		
		if (!FFToolsUnits.checkShip(this)){
			this.doNotConfirmOrders("!!! MT: Einheit ist nicht Kapitän des Schiffes!");
			return;
		}
		
		this.actRegionCoord = this.scriptUnit.getUnit().getRegion().getCoordinate();
		
		
		/*
		 * // MaterialHubTransporterSettings Target=X,Y StartRunde=xxxx	ETA=XXXX ;doNotEdit
		 * Zeile suchen, Ergebnisse Eintragen
		 */
		// outText.addOutLine("MT: new OP");
		FFToolsOptionParser OP_settings = new FFToolsOptionParser(this.scriptUnit);
		OP_settings.enable_noScript_parsing=true;
		ArrayList<String> ff = new ArrayList<String>(0);
		ff.add("MaterialHubTransporterSettings");
		OP_settings.addFilter(ff);
		// outText.addOutLine("MT: filter added");
		
		homeString=OP_settings.getOptionString("target");
		if (homeString.length()>2){
			CoordinateID actDest = null;
			if (homeString.indexOf(',') > 0) {
				actDest = CoordinateID.parse(homeString,",");
			}
			if (actDest!=null){
				this.targetRegionCoord = actDest;
			} 
		}
		
		// outText.addOutLine("MT: target read");
		
		this.startRunde = OP_settings.getOptionInt("startRunde",0);
		this.ETA = OP_settings.getOptionInt("ETA",0);
		
		if (this.startRunde>0 && this.ETA>0 && this.targetRegionCoord!=null) {
			this.MaterialHubTransporterSettings_found= true;
			this.addComment("MT: MaterialHubTransporterSettings gefunden");
			this.process_with_Settings();
		} else {
			this.addComment("MT: MaterialHubTransporterSettings NICHT gefunden(" + this.startRunde + " | " + this.ETA + ")");
			this.process_withOUT_Settings();
		}
			
	}
	
	private void process_with_Settings() {
		// nicht in HomeRegion und nicht in Zielregion
		if (!this.actRegionCoord.equals(this.targetRegionCoord) && !this.actRegionCoord.equals(this.HomeRegionCoord)) {
			/*
			 * - (requests müssten noch da sein und sollen da bleiben)
				- SailTo ausführen und Umsetzen, DIST ermitteln (in Runden)
			 */
			this.addComment("MT: setze Kurs zur Zielregion " + this.targetRegionCoord.toString());
			super.scriptUnit.findScriptClass("Sailto",this.targetRegionCoord.toString(","));
			
			// Todo Anmeldung beim MH der Zielregion!
			this.announce_me_in_target_region=true;
			
			return;
		}
		
		// nur in Zielregion
		if (this.actRegionCoord.equals(this.targetRegionCoord) && !this.actRegionCoord.equals(this.HomeRegionCoord)) {
			/*
			 * - Zeile // MaterialHubTransporter Target=X,Y StartRunde=xxxx	ETA=XXXX umbenennen in // MaterialHubTransporter_endRunde_xxxx Target=X,Y StartRunde=xxxx	ETA=XXXX
			- alle // script requests mit ;MaterialHubTransporter<-doNotRemove umbenennen in // oldMH_request_Runde_xxxx request [...] ;MaterialHubTransporter<-youCanRemove
			- alle Request-Scripte mit originalOrder incl ;MaterialHubTransporter<-doNotRemove wieder aus der Scriptunit entfernen
			- Sailto nach HomeRegion setzen und durchführen!, Anzahl Runden ermitteln
			- Silbercheck durchführen
			- Route Zielregion -> HomeRegion beim MHM anmelden (ToDo)
			 * 
			 */
			this.addComment("MT: sind in Zielregion. Material abwerfen und nach Hause....");
			int actGameRunde = super.scriptUnit.getScriptMain().gd_ScriptMain.getDate().getDate();
			// Zeile finden und umbenennen
			Unit u = this.scriptUnit.getUnit();
			List<Order> orders =  u.getOrders2();
	  	    List<Order> newOrders = new LinkedList<Order>();
	  	    ArrayList<String> replacedOrders = new ArrayList<String>();
	  	    if (orders!=null && orders.size() >0) {
		  	    for (Order o:orders) {
		  	    	// outText.addOutLine("MT-ZielRegion: untersuche Zeile: " + o.getText());
		  	    	boolean isOK=true;
		  	    	String checkS = "// MaterialHubTransporterSettings";
		  	    	checkS = checkS.toUpperCase();
		  	    	if (o.getText().toUpperCase().startsWith(checkS)) {
		  	    		// outText.addOutLine("MHTS Zeile erkannt");
		  	    		isOK=false;
		  	    		String oldOrderText = o.getText();
		  	    		
		  	    		String newOrderText = oldOrderText.replace("MaterialHubTransporterSettings", "MHTS_endRunde_" + actGameRunde) + "<-youCanRemove(" + actGameRunde + ")";
		  	    		replacedOrders.add(newOrderText);
		  	    	}
		  	    	checkS = "MaterialHubTransporter<-doNotRemove";
		  	    	checkS = checkS.toUpperCase();
		  	    	if (o.getText().toUpperCase().indexOf(checkS)>0 && o.getText().toUpperCase().startsWith("// SCRIPT REQUEST ")) {
		  	    		isOK=false;
		  	    		String oldOrderText = o.getText();
		  	    		String newOrderText = oldOrderText.replace("MaterialHubTransporter<-doNotRemove", "MaterialHubTransporter<-youCanRemove(" + actGameRunde + ")");
		  	    		newOrderText = newOrderText.replace("script request", "oldMH_request_runde_" + actGameRunde);
		  	    		replacedOrders.add(newOrderText);
		  	    	}
		  	    	
		  	    	if (isOK) {
		  	    		newOrders.add(o);
		  	    	}
		  	    	
		  	    }
		  	    u.setOrders2(newOrders);
	  	    }
	  	    if (replacedOrders.size()>0) {
	  	    	// Doppelorders ausschliessen
	  	    	if (orders!=null && orders.size() >0) {
			  	    for (Order o:orders) {
			  	    	if (replacedOrders.contains(o.getText())) {
			  	    		replacedOrders.remove(o.getText());
			  	    	}
			  	    }
	  	    	}
	  	    }
	  	    if (replacedOrders.size()>0) {
	  	    	for(String s:replacedOrders){
	  	    		// this.addComment("MT: neuer Befehlszeile: " + s);
	  	    		u.addOrder(s);
	  	    	}
	  	    }
	  	    
	  	    // the request-scripts aus der ScriptUnit entfernen
	  	    // outText.addOutLine("MT: checking for request scripts to remove");
			for (ScriptUnit scu:super.scriptUnit.getScriptMain().getScriptUnits().values()){
				if (scu.getFoundScriptList()!=null){
					for (Script s:scu.getFoundScriptList()){
						if (s instanceof Request){
							Request r = (Request)s;
							ArrayList<String> args = r.getArguments();
							for (String a:args) {
								if (a.equalsIgnoreCase(";MaterialHubTransporter<-doNotRemove")) {
									scu.delAScript(r);
									// outText.addOutLine("MT: removed: " + r.toString());
								}
							}
						}
					}
					scu.processScriptDeletions();
				}
			}
	  	    
	  	    
	  	    // jetzt weiter, als hätten wir keine Settingszeile
	  	    this.MaterialHubTransporterSettings_found=false;
	  	    this.process_withOUT_Settings();
		}
		
		// nur in Homeregion
		if (!this.actRegionCoord.equals(this.targetRegionCoord) && this.actRegionCoord.equals(this.HomeRegionCoord)) {
			this.addComment("MT: sind in Homeregion. Beim MHM (MaterialHubManager) melden, alle bisherigen Festlegungen wieder löschen");
			// int actGameRunde = super.scriptUnit.getScriptMain().gd_ScriptMain.getDate().getDate();
			// Zeile finden und umbenennen
			Unit u = this.scriptUnit.getUnit();
			List<Order> orders =  u.getOrders2();
	  	    List<Order> newOrders = new LinkedList<Order>();
	  	    if (orders!=null && orders.size() >0) {
		  	    for (Order o:orders) {
		  	    	// outText.addOutLine("MT-ZielRegion: untersuche Zeile: " + o.getText());
		  	    	boolean isOK=true;
		  	    	String checkS = "// MaterialHubTransporterSettings";
		  	    	checkS = checkS.toUpperCase();
		  	    	if (o.getText().toUpperCase().startsWith(checkS)) {
		  	    		// outText.addOutLine("MHTS Zeile erkannt");
		  	    		isOK=false;
		  	    	}
		  	    	checkS = "MaterialHubTransporter<-doNotRemove";
		  	    	checkS = checkS.toUpperCase();
		  	    	if (o.getText().toUpperCase().indexOf(checkS)>0 && o.getText().toUpperCase().startsWith("// SCRIPT REQUEST ")) {
		  	    		isOK=false;
		  	    	}
		  	    	
		  	    	if (isOK) {
		  	    		newOrders.add(o);
		  	    	}
		  	    	
		  	    }
		  	    u.setOrders2(newOrders);
	  	    }
	  	  
	  	    // the request-scripts aus der ScriptUnit entfernen
	  	    // outText.addOutLine("MT: checking for request scripts to remove");
			for (ScriptUnit scu:super.scriptUnit.getScriptMain().getScriptUnits().values()){
				if (scu.getFoundScriptList()!=null){
					for (Script s:scu.getFoundScriptList()){
						if (s instanceof Request){
							Request r = (Request)s;
							ArrayList<String> args = r.getArguments();
							for (String a:args) {
								if (a.equalsIgnoreCase(";MaterialHubTransporter<-doNotRemove")) {
									scu.delAScript(r);
									// outText.addOutLine("MT: removed: " + r.toString());
								}
							}
						}
					}
					scu.processScriptDeletions();
				}
			}
	  	    
	  	    
	  	    // jetzt weiter, als hätten wir keine Settingszeile
	  	    this.MaterialHubTransporterSettings_found=false;
	  	    this.process_withOUT_Settings();
		}
	}
	
	private void process_withOUT_Settings() {
		
		
		// wenn wir nicht in der HomeRegion sind:
		if (!this.actRegionCoord.equals(this.HomeRegionCoord)) {
		
			this.addComment("MT: processing phase withOUT settings  - wir sind NICHT in HomeRegion - also hin!");
			this.addComment("MT: setze Kurs zur HomeRegion " + this.HomeRegionCoord.toString());
			super.scriptUnit.findScriptClass("Sailto",this.HomeRegionCoord.toString(","));
		} else {
			// wenn wir in der HomeRegion sind
			this.addComment("MT: processing phase withOUT settings  - wir sind IN HomeRegion - Beim MHM (MaterialHubManager) melden.");
		}
	}
	
	
	private void run_02() {
		// Silbercheck
		this.addComment("MT: silbercheck bei SailTo gestartet");
		Object o = this.scriptUnit.getScript(Sailto.class);
		if (o!=null && o instanceof Sailto) {
			Sailto s = (Sailto)o;
			int turnsExact = s.turns_exact;
			if (turnsExact>0) {
				int personenAnzahl = super.scriptUnit.getUnit().getModifiedPersons();
				int benötigtes_Silber = personenAnzahl * 10 * turnsExact;
				this.addComment("MT: SailTo liefert " + turnsExact + " Runden bis zum Ziel. Personen: " + personenAnzahl + ", " + benötigtes_Silber + " Silber.");
				ItemType silverType = reportSettings.getRules().getItemType("Silber");
				// Item silver = this.scriptUnit.getUnit().getModifiedItem(silverType);
				Item silver = this.scriptUnit.getModfiedItemMatPool2(silverType);
				if (silver.getAmount()>=benötigtes_Silber) {
					this.addComment("MT: Silbercheck erfolgreich - " + silver.getAmount() + " Silber erkannt");
				} else {
					this.doNotConfirmOrders("MT: Silbercheck gescheitert, nur " + silver.getAmount() + " Silber erkannt, aber " + benötigtes_Silber + " benötigt");
				}				
			} else {
				this.addComment("MT: SailTo liefert keibe Rundenangabe bis zum Ziel. (" + turnsExact + ")");
			}
		} else {
			this.addComment("MT: kein SailTo erkannt");
		}
	}
	
	
	private void run_01_01() {
		// nur anmelden beim MH in der Zielregion
		if (!this.announce_me_in_target_region) {
			return;
		}
		
		if (this.targetRegionCoord==null) {
			this.doNotConfirmOrders("MT run_01_01: kein Ziel bekannt");
			return;
		}
		
		MaterialHubManager_MHM MHM = this.getOverlord().getMaterialHubManager();
		Materialhub targetMH = MHM.getMH(this.targetRegionCoord);
		if (targetMH==null) {
			this.doNotConfirmOrders("MT run 01 01: kein MaterialHub (MH) in Zielregion (" + this.targetRegionCoord.toString() + ") gefunden.");
			return;
		}
		
		targetMH.addComment("MT unterwegs, ETA " + this.ETA + " Schiff " + this.scriptUnit.toString() + " mit diesen Waren:");
		
		// the request-scripts aus der MT-ScriptUnit durchgehen
  	    // outText.addOutLine("MT: checking for request scripts to remove");
		for (ScriptUnit scu:super.scriptUnit.getScriptMain().getScriptUnits().values()){
			if (scu.getFoundScriptList()!=null){
				for (Script s:scu.getFoundScriptList()){
					if (s instanceof Request){
						Request r = (Request)s;
						ArrayList<String> args = r.getArguments();
						for (String a:args) {
							if (a.equalsIgnoreCase(";MaterialHubTransporter<-doNotRemove")) {
								MatPoolRequest mpr = r.getMPR();
								if (mpr!=null) {
									targetMH.addComment(mpr.getBearbeitet() + " " + mpr.getOriginalGegenstand() + " ( von " + mpr.getOriginalGefordert() + ")");
								} else {
									this.doNotConfirmOrders("Fehler bei der MPR bestimmung");
								}
							}
						}
					}
				}
				scu.processScriptDeletions();
			}
		}
		
		
		
	}
	
	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
}
