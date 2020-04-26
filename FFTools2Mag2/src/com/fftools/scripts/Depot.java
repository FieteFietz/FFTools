package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;

import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.rules.BuildingType;
import magellan.library.rules.ItemType;
import magellan.library.utils.Utils;

public class Depot extends TransportScript{
	
	private static final int Duchlauf_main=20;
	private static final int Durchlauf_nachLetztemMP = 800; //nur Info
	
	private int[] runsAt = {Duchlauf_main,Durchlauf_nachLetztemMP};
	
	public static int default_request_prio = 1;
	private String default_request_kommentar = "Depot";
	
	private int default_runden_silbervorrat = 3;
	private int default_silbervorrat_maxPrio = 100;
	private int default_silbervorrat_minPrio = 10;
	
	private int used_silbervorrat_maxPrio = 0;
	
	private String default_silbervorrat_kommentar = "SilberDepot";

	private LinkedList<MatPoolRequest> silberDepotMPRs = null;
	
	private boolean debugOutput = false;
	
	private boolean mitBauernCheck = true;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Depot() {
		super.setRunAt(this.runsAt);
	}
	
	
	public void runScript(int scriptDurchlauf){
		
		switch (scriptDurchlauf){
		
			case Duchlauf_main:this.runDepot(scriptDurchlauf);break;

			case Durchlauf_nachLetztemMP:this.runNachMP(scriptDurchlauf);break;
			
		
		}
		
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
	
	public void runDepot(int scriptDurchlauf){
				
		// "registrierung" beim TransportManager, damit der später nicht
		// seine Depots = Lieferanten zusammensuchen muss
		this.getOverlord().getTransportManager().addDepot(this.scriptUnit);
		
		// Falls KEIN Handler in der Region ist, diese Region trotzdem
		// beim zuständigen TAH anmelden
		TradeRegion tR = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeRegion(this.scriptUnit.getUnit().getRegion());
		// und damit sie auch beim TA landet, den aktuellen TA anfordern
		// TradeArea tA = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeArea(tR, true);
		TradeArea TA = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeArea(tR, true);
		if (debugOutput){
			this.addComment("debug depot init: actual TA assigend: " + TA.getName());
		}
		
		
		// Depotunit auch beim MatPool setzen
		this.getOverlord().getMatPoolManager().getRegionsMatPool(this.scriptUnit).setDepotUnit(this.scriptUnit);
		
		// also selber alles zusammensuchen....
		// Region getItem funzt wohl...also nur ne Liste der ItemTypes erstellen in der Region
		ArrayList<ItemType> itemTypes = new ArrayList<ItemType>();
		Region r = this.scriptUnit.getUnit().getRegion();
		for (Iterator<Unit> iter = r.units().iterator();iter.hasNext();){
			Unit u = (Unit) iter.next();
			for (Iterator<Item> iter2 = u.getItems().iterator();iter2.hasNext();){
				Item item = (Item)iter2.next();
				ItemType itemType = item.getItemType();
				if (!itemTypes.contains(itemType)){
					itemTypes.add(itemType);
				}
			}
		}
		for (Iterator<ItemType> iter = itemTypes.iterator();iter.hasNext();) {
			ItemType itemType = (ItemType)iter.next();
			// Request doch mit "alles"
			this.addMatPoolRequest(new MatPoolRequest(this,Integer.MAX_VALUE,itemType.getName(),Depot.default_request_prio,this.default_request_kommentar));
		}
		
		// fertig (?)  dass depot ist soooo einfach? da muss noch was kommen...
		
		//ja: der Lernbefehl...standardmässig tarnung
		//neu: optional:
		// noch neuer: Lernpool wird verwendet
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Depot");
		OP.addOptionList(this.getArguments());
		if (OP.getOptionBoolean("Lernen", true)){
			super.lerneTalent("Tarnung", true);
		}
		
		this.mitBauernCheck = OP.getOptionBoolean("BauernCheck", true);
		
		
		// Silbervorrat für die Region...für Region/Insel/Report gesetzt ?
		if (reportSettings.getOptionBoolean("DepotSilber", this.region())){
			int anzahl_silber_runden = this.default_runden_silbervorrat;
			this.addComment("DEBUG: DepotSilber ist aktiviert");
			// reportweite settings
			int reportRunden = reportSettings.getOptionInt("DepotSilberRunden", this.region());
			if (reportRunden>0){
				anzahl_silber_runden = reportRunden;
				this.addComment("DEBUG: Reportsettings -> depotsilberrunden = " + reportRunden);
			} else {
				this.addComment("DEBUG: Reportsettings -> keine Info");
			}
			
			// aus den Optionen
			int optionRunden = OP.getOptionInt("DepotSilberRunden", -1);
			if (optionRunden>0){
				anzahl_silber_runden = optionRunden;
				this.addComment("DEBUG: Optionen -> depotsilberrunden = " + optionRunden);
			} else {
				this.addComment("DEBUG: Optionen -> keine Info");
			}
			// maxSilberrequestPrio...gleiches Spiel
			int silberRequestPrio = this.default_silbervorrat_maxPrio;
			int reportSilberDepotPrio = reportSettings.getOptionInt("DepotSilberPrio", this.region());
			if (reportSilberDepotPrio>0){
				silberRequestPrio = reportSilberDepotPrio;
			}
			int optionenPrio = OP.getOptionInt("DepotSilberPrio", -1);
			if (optionenPrio>0){
				silberRequestPrio = optionenPrio;
			}
			
			if (anzahl_silber_runden>0){
				super.setPrioParameter(silberRequestPrio-this.default_silbervorrat_minPrio,-0.5,0,this.default_silbervorrat_minPrio);
				this.used_silbervorrat_maxPrio = silberRequestPrio;
				int kostenProRunde = this.getKostenProRunde();
			
				this.silberDepotMPRs = new LinkedList<MatPoolRequest>();
				
				int prioTM = reportSettings.getOptionInt("DepotSilberPrioTM", this.region());
				
				// los gehts
				for (int i = 1;i<=anzahl_silber_runden;i++){
					// regionsinterne Prio
					super.setPrioParameter(silberRequestPrio-this.default_silbervorrat_minPrio,-0.5,0,this.default_silbervorrat_minPrio);
					int actPrio = super.getPrio(i-1);
					// TM Prio
					int actPrioTM=actPrio;
					if (prioTM>0){
						super.setPrioParameter(prioTM-this.default_silbervorrat_minPrio,-0.5,0,this.default_silbervorrat_minPrio);
						actPrioTM = super.getPrio(i-1);
					}
					
					MatPoolRequest actMPR = new MatPoolRequest(this,kostenProRunde,"Silber",actPrio,this.default_silbervorrat_kommentar);
					if (prioTM>0){
						actMPR.setPrioTM(actPrioTM);
					}
					this.addMatPoolRequest(actMPR);
					this.silberDepotMPRs.add(actMPR);
				}
			}
			
		}
		
		// versuch, auch das Depot als TR-Name Setter zuzulassen
		if (OP.getOptionString("TradeArea").length()>0){
			String setAreaName = OP.getOptionString("TradeArea");
			// TR bekommen
			TradeRegion TR = this.getTradeRegion();
			if (TR!=null){
				TR.setTradeAreaName(setAreaName);
				this.addComment("trying to name TradeArea as: " + setAreaName);
				this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().recalcTradeAreas();
			} else {
				this.doNotConfirmOrders("!!! could not set TradeArea Name: no TradeRegion found.");
			}
		}
		
		
		// requestInfo
		this.scriptUnit.findScriptClass("RequestInfo");
	}
	
	private int getKostenProRunde(){
		int erg=0;
		// ScriptPersonen * 10
		erg += FFToolsRegions.countScriptPersons(this.getOverlord().getScriptMain().getScriptUnits(), this.region()) * 10;
		
		// Gebäudekosten
		Region r = this.region();
		if (r.buildings()!=null && r.buildings().size()>0){
			for (Iterator<Building> iter = r.buildings().iterator();iter.hasNext();){
				Building b = (Building) iter.next();
				BuildingType bT = b.getBuildingType();
				if (bT.getMaintenanceItems()!=null){
					for (Iterator<Item> iter2 = bT.getMaintenanceItems().iterator();iter2.hasNext();){
						Item item = (Item)iter2.next();
						if (item.getName().equalsIgnoreCase("Silber")){
							erg+=item.getAmount();
						}
					}
				}
			}
		}
		
		// SilberSockel
		int sockel = reportSettings.getOptionInt("DepotSilberSockel", this.region());
		if (sockel>0){
			erg+=sockel;
		}
		
		
		
		return erg;
	}
	
	/**
	 * rein informativ für die unit
	 * @param scriptDurchlauf
	 */
	public void runNachMP(int scriptDurchlauf){
		
		// 20180506: Einbau Warnung wenn zu viele Bauern vorhanden
		Region r = this.getUnit().getRegion();
		if (r!=null && !(r.getRegionType().isOcean()) && this.mitBauernCheck) {
			// 20200419: nicht bei Insekten im Winter, es sei denn, in einer Wüste
			boolean insektenImWinter=false;
			if (this.scriptUnit.isInsekt()) {
				int Runde=this.getOverlord().getScriptMain().gd_ScriptMain.getDate().getDate();
				int RundenFromStart = Runde - 1;
				int iWeek = (RundenFromStart % 3) + 1;
				int iMonth = (RundenFromStart / 3) % 9;
				
				// Herdfeuer oder Eiswind
				if (iMonth==1 || iMonth==2) {
					// Herdfeuer oder Eiswind
					insektenImWinter = true;
				}
				if (iMonth==3) {
					// Schneebann, nur Wochen 1+2
					if (iWeek==1 || iWeek==2) {
						insektenImWinter = true;
					}
				}
				if (iMonth==0) {
					// Sturmmond, nur Woche 3
					if (iWeek==3) {
						insektenImWinter = true;
					}
				}
			}
			if (insektenImWinter) {
				// nicht, wenn in Wüste
				if (this.region().getRegionType().getName().equalsIgnoreCase("Wüste")) {
					insektenImWinter=false;
				}
			}

			if (insektenImWinter) {
				this.addComment("Bauerncheck: wird nicht durchgeführt (Insekten erkannt und nächste Woche ist Winter, und nicht in einer Wüste)");
			} else {
				// Bauerncheck durchführen
				int actPersonen = r.getModifiedPeasants();
				int maxPersonen = Utils.getIntValue(this.gd_Script.getGameSpecificRules().getMaxWorkers(r), 0);
				
				int actRekrutierungen = r.maxRecruit() - r.modifiedRecruit();
				int maxRekrutierungen = r.maxRecruit();
				
				if (actPersonen>maxPersonen) {
					int diffPersonen = actPersonen - maxPersonen;
					this.addComment("Bauerncheck!!! " + diffPersonen + " Bauern zu viel in der Region! !!!");
					if (actRekrutierungen<maxRekrutierungen && actRekrutierungen<diffPersonen) {
						this.doNotConfirmOrders("Bauerncheck!!! Nur " + actRekrutierungen + " von " + maxRekrutierungen + " rekrutiert in dieser Runde!");
					} else {
						this.addComment("Bauerncheck!!! Es wird gut rekrutiert...(" + actRekrutierungen + ")");
					}
				} else {
					this.addComment("Bauerncheck: " + actPersonen + " von " + maxPersonen + " Bauern in der Region. OK");
				}
			}
		}
		
		
		if (this.silberDepotMPRs==null || this.silberDepotMPRs.size()==0){
			return;
		}
		
		// AlertStatus
		boolean SilberDepotVorratAlarmAus = reportSettings.getOptionBoolean("SilberDepotVorratAlarmAus", this.region());
		
		for (Iterator<MatPoolRequest> iter = this.silberDepotMPRs.iterator();iter.hasNext();){
			MatPoolRequest MPR = (MatPoolRequest)iter.next();
			/**
			String erg = "DepotSilber gefordert:";
			erg += MPR.getOriginalGefordert() + "(Prio " + MPR.getPrio() + ")";
			erg += ",bearbeitet:" + MPR.getBearbeitet();
			this.addComment(erg);
			**/
			// Problem
			if (MPR.getPrio()==this.used_silbervorrat_maxPrio && MPR.getBearbeitet()<MPR.getOriginalGefordert()){
				// max prio und nicht erfüllt ?!
				
				this.addComment("!!! DepotSilber ungenügend !!! (" + MPR.getBearbeitet() + "/" + MPR.getOriginalGefordert() + ")");
				if (SilberDepotVorratAlarmAus) {
					this.addComment("kein Alarm, da SilberDepotVorratAlarmAus aktiviert");
				} else {
					this.scriptUnit.doNotConfirmOrders("!!! DepotSilber ungenügend (" + MPR.getBearbeitet() + "/" + MPR.getOriginalGefordert() + "): " + this.unitDesc());
				}
			}
			
		}
	}
	
	
}
