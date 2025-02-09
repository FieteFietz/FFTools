package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

import magellan.library.Building;
import magellan.library.CoordinateID;
import magellan.library.Faction;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.UnitID;
import magellan.library.rules.BuildingType;
import magellan.library.rules.ItemType;
import magellan.library.rules.Race;
import magellan.library.utils.Utils;

public class Depot extends TransportScript{
	
	private static final int Duchlauf_main=20;
	private static final int Duchlauf_Bauern=29;
	private static final int Durchlauf_nachLetztemMP = 800; //nur Info
	
	private int[] runsAt = {Duchlauf_main,Duchlauf_Bauern,Durchlauf_nachLetztemMP};
	
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
	
	private int minFreeUnitsFaction_BauernHome = 20;
	
	
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
			
			case Duchlauf_Bauern: this.checkBauernHome();break;

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
			} else {
				this.addComment("**Depot-debug: AnzahlRunden=0");
			}
			
		} else {
			this.addComment("**Depot-debug: Reportsettings - DepotSilber=nein");
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
	 * wenn repoprtsetting BauernHome gesetzt ist, prüfen, ob wir eine Temp auf den Weg bringen sollten...
	 */
	private void checkBauernHome() {
		Region r = this.getUnit().getRegion();
		String BauernHome = reportSettings.getOptionString("BauernHome", r);
		if (BauernHome!=null) {
			if (BauernHome.length()>2){
				CoordinateID actDest = null;
				if (BauernHome.indexOf(',') > 0) {
					actDest = CoordinateID.parse(BauernHome,",");
				}
				if (actDest==null){
					this.doNotConfirmOrders("!!! BauernHOME Angabe nicht erkannt (2)!");
					return;
				} else {
					
					if (actDest.equals(this.region().getCoordinate())) {
						// wir sind in der Home-Region - da machen wir keinen Bauerntransfer
						this.addComment("BauernHome: Depot ist in der HomeRegion - kein Bauerntransfer");
						return;
					}
					
					// sieht gut aus
					// Bauerncheck durchführen
					int actPersonen = r.getModifiedPeasants();
					int maxPersonen = Utils.getIntValue(this.gd_Script.getGameSpecificRules().getMaxWorkers(r), 0);
					
					// Bauernwachstum prognistizieren
					int Bauernwachstum = (int) Math.ceil(actPersonen * 0.001);
					actPersonen += Bauernwachstum;
					int todoRekrutieren = actPersonen - maxPersonen;
					if (todoRekrutieren>r.modifiedRecruit()) {
						todoRekrutieren = r.modifiedRecruit();
					}
					if (actPersonen>maxPersonen) {
						this.addComment("Bauernhome: Bauernprognose " + actPersonen + " ist größer als RegionsMax " + maxPersonen + ", versende "  + todoRekrutieren + " Bauern");
						
						if (this.getUnit().isStarving()) {
							this.doNotConfirmOrders("!!! Das Depot hungert: überschüssige Bauern können nicht versendet werden!!!");
						} else {
							Race ra = super.scriptUnit.getUnit().getDisguiseRace();
							if (ra==null){
								ra = super.scriptUnit.getUnit().getRace();
							}
							
							// bei Orks verdoppeln
							
							Race orkRace = this.gd_Script.getRules().getRace("Orks",false);
							if (orkRace==null){
								this.doNotConfirmOrders("Ork-Rasse nicht in den Regeln gefunden - FFTools braucht ein Update");
							} else {
								if (ra.equals(orkRace)){
									todoRekrutieren = todoRekrutieren*2;
									this.addComment("Rekrutieren: Orks erkannt. Maximal mögliche Rekruten verdoppelt auf:" + todoRekrutieren);
									
								}
							}
							
							
							int silber_benoetigt = todoRekrutieren * ra.getRecruitmentCosts();
							ItemType silverType=this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getItemType("Silber",false);
							Item i = this.scriptUnit.getModifiedItem(silverType);
							
							if (i==null) {
								this.doNotConfirmOrders("!!! BauernHome: Depot hat gar kein Silber ??!");
								return;
							}
							if (i.getAmount()<silber_benoetigt) {
								this.doNotConfirmOrders("!!! BauernHome: Depot hat zu wenig Silber ??! (" + i.getAmount() + "/" + silber_benoetigt + " Silber)");
								return;
							}
						
						
							if (!BauernMitreise(todoRekrutieren)) {
								
								// Hat die Fraktion noch genügend Einheiten frei
								Faction f = this.getUnit().getFaction();
								if (f.modifiedUnits()!=null) {
									if (f.modifiedUnits().size()>(2500 - this.minFreeUnitsFaction_BauernHome)) {
										this.doNotConfirmOrders("!!! Bauernhome: nicht mehr genügend Einheiten frei!!! (aktuell neue Einheitenanzahl: " + f.modifiedUnits().size() + ", Reserve " + this.minFreeUnitsFaction_BauernHome + " unterschritten)");
										return;
									}
								}
								
								
								// temp anlegen
								// neue Unit ID
								int oldUnitCount = f.modifiedUnits().size();
								Unit parentUnit = this.scriptUnit.getUnit();
								UnitID id = UnitID.createTempID(this.gd_Script, this.scriptUnit.getScriptMain().getSettings(), parentUnit);
								// Die tempUnit anlegen
								TempUnit tempUnit = parentUnit.createTemp(this.gd_Script,id);
								tempUnit.addOrder(Rekrutieren.scriptCreatedTempMark);
								// Kommandos setzen
								// Kommandos durchlaufen
								
								tempUnit.addOrder("BENENNE Einheit \"Bauernwanderung\"");
								String RekrutierString = "Rekrutieren " + todoRekrutieren + " ;Bauernwanderung"; 
								tempUnit.addOrder(RekrutierString);
								tempUnit.addOrder("// script Bauern Home=" + BauernHome);
								tempUnit.addOrder("// aus " + this.region().toString());
								tempUnit.addOrder("// setTag eTag1 Bauernwanderung");
								tempUnit.addOrder("KÄMPFE FLIEHE ;Bauernwanderung");
								tempUnit.setOrdersConfirmed(true);
								
								ScriptUnit su = this.scriptUnit.getScriptMain().addUnitLater(tempUnit);
								GotoInfo GI = FFToolsRegions.makeOrderNACH(su, this.region().getCoordinate(), actDest,true,"BauernHome");
								su.specialProtectedOrders.add(RekrutierString);
								su.specialProtectedOrders.add("NACH " + GI.getPath());
								
								Lohn L = new Lohn();
								L.setScriptUnit(su);
								L.setGameData(this.gd_Script);
								L.setClient(this.c);
								su.addAScript(L);
								
								MatPoolRequest MPR = new MatPoolRequest(L,silber_benoetigt,"Silber",1000,"Rekrutier Silber für Bauernwanderung");
								L.addMatPoolRequest(MPR);
								
								this.addComment("Bauernhome - Anzahl Units vorher: " + oldUnitCount + ", nun: " + f.modifiedUnits().size());
							}
						}
						
					} else {
						this.addComment("Bauernhome: Bauernprognose " + actPersonen + " ist kleiner als RegionsMax " + maxPersonen + " Bauern");
					}  // actPersonen>maxPersonen
					
				} 
			} else {
				//  // Länge <=2
				this.doNotConfirmOrders("!!! BauernHOME Angabe nicht erkannt (1)!");
				return;
			}
		} // Bauernhome überhaupt gesetzt
	}
	
	/*
	 * Sind bereits Bauern auf Wanderschaft in der Region? Dann sollen die Rekrutieren
	 */
	private boolean BauernMitreise(int AnzahlRekruten) {
		
		// alle scriptunits in der Region durchgehen
		Hashtable <Unit, ScriptUnit> regionsScriptUnits = this.scriptUnit.getScriptMain().getScriptUnits(this.region());
		if (regionsScriptUnits!=null) {
			for (ScriptUnit su:regionsScriptUnits.values()) {
				// ist su ein Bauern-script
				Object o = su.getScript(Bauern.class);
				if (o!=null) {
					// Bingo
					Bauern bauern = (Bauern) o;
					// Rasse
					Race ra = bauern.scriptUnit.getUnit().getDisguiseRace();
					if (ra==null){
						ra = bauern.scriptUnit.getUnit().getRace();
					}
					// bei Orks verdoppeln
					int todoRekrutieren = AnzahlRekruten;
					Race orkRace = this.gd_Script.getRules().getRace("Orks",false);
					if (orkRace==null){
						this.doNotConfirmOrders("Ork-Rasse nicht in den Regeln gefunden - FFTools braucht ein Update");
					} else {
						if (ra.equals(orkRace)){
							todoRekrutieren = todoRekrutieren*2;
							bauern.addComment("Rekrutieren: Orks erkannt. Maximal mögliche Rekruten verdoppelt auf:" + todoRekrutieren);
						}
					}
					
					String RekrutierString = "REKRUTIERE " + todoRekrutieren + " ;BauernHome";
					bauern.addOrder(RekrutierString, true);
					
					int silber_benoetigt = todoRekrutieren * ra.getRecruitmentCosts();
					ItemType silverType=this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getItemType("Silber",false);
					Item i = this.scriptUnit.getModifiedItem(silverType);
					
					if (i==null) {
						this.doNotConfirmOrders("!!! BauernHome: Depot hat gar kein Silber ??!");
						return true;
					}
					if (i.getAmount()<silber_benoetigt) {
						this.doNotConfirmOrders("!!! BauernHome: Depot hat zu wenig Silber ??! (" + i.getAmount() + "/" + silber_benoetigt + " Silber)");
						return true;
					}

					MatPoolRequest MPR = new MatPoolRequest(bauern,silber_benoetigt,"Silber",1000,"Rekrutier Silber für Bauernwanderung");
					bauern.addMatPoolRequest(MPR);
					this.addComment("BauernHome: Rekrutierung übernimmt: " + bauern.scriptUnit.toString());
					return true;
				}
			}
		}
		return false;
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
			if (this.scriptUnit.isInsekt() && FFToolsGameData.isNextTurnWinter(this.getOverlord().getScriptMain().gd_ScriptMain)) {
				insektenImWinter = true;
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
					
					if ((actRekrutierungen<maxRekrutierungen && actRekrutierungen<diffPersonen) || maxRekrutierungen==0) {
						if (maxRekrutierungen>0) {
							this.doNotConfirmOrders("Bauerncheck!!! Nur " + actRekrutierungen + " von " + maxRekrutierungen + " rekrutiert in dieser Runde!");
						} else {
							this.doNotConfirmOrders("Bauerncheck!!! Zu viele Bauern in der Region - aber es kann nicht rekrutiert werden!");
						}
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
