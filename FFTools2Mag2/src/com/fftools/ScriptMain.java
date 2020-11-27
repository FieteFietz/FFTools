package com.fftools;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JTextArea;

import magellan.client.Client;
import magellan.library.Building;
import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.Item;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.event.GameDataEvent;
import magellan.library.gamebinding.EresseaRelationFactory;
import magellan.library.utils.Regions;

import com.fftools.overlord.Overlord;
import com.fftools.pools.bau.TradeAreaBauManager;
import com.fftools.pools.circus.CircusPoolManager;
import com.fftools.pools.seeschlangen.MonsterJagdManager_MJM;
import com.fftools.pools.treiber.TreiberPoolManager;
import com.fftools.scripts.Script;
import com.fftools.trade.TradeAreaHandler;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsTags;


/**
 * 
 * @author Fiete
 *
 * Oberklasse der Scriptverarbeitung...verwaltet ScriptUnit, durchlaeufe etc
 * 
 */


public class ScriptMain {
	public static final String MAPLINE_TAG="prepared_mapline";
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private Hashtable<Unit,ScriptUnit> scriptUnits = null;
	
	private ArrayList<Region> scriptRegions = null;
	private ArrayList<Building> scriptProductionBuildings = null;
	
	public Client client = null;
	public GameData gd_ScriptMain = null;
	private Properties settings=null;
	
	private final static String FFTools2_ignoreFactions = "FFTools2.ignoreFactions";
	public ArrayList<String> ignoreList=null;
	
	
	public ArrayList<Script> singleInstanceScriopts = new ArrayList<Script>(0);
	
	/**
	 * zentrale verwaltung aller objecte, die in den durchlaufen
	 * aufgerufen werden können: scripte und manager (handler)
	 */
	private Overlord overlord = null;

	/**
	 * Table of factions with changed Trustlevels und old Trustlevel
	 */
	private Hashtable<Faction,Integer> changedTrustlevels = null;
	
	
	public ScriptMain(){
		// nuescht zu tun.... ?
		// outline KANN noch nicht funktionieren...
		
		outText.addOutLine("new Script Main initialized");
	}
	
	public ScriptMain(GameData _gd){
		this.gd_ScriptMain = _gd;
		outText.addOutLine("new Script Main initialized with a GameData object..parsing units");
		extractScriptUnits();
	}
	
	public ScriptMain(Client _client,JTextArea _txtOutput){
		super();
		outText.setTxtOut(_txtOutput);
		this.client = _client;
		this.gd_ScriptMain = client.getData();
		this.setSettings(_client.getProperties());
		outText.addOutLine("new Script Main initialized (client)");
	}
	
	/**
	 * Eine als scriptunit erkannte unit wird der Map scriptUnits hinzugefuegt
	 * falls aus irgendeinem Grund eine unit 2x registriert werden soll, verhindert
	 * die Map dass, weil unit als key dient und nur 1x vorkommen kann
	 * 
	 * @param u Die zu addende Unit
	 * 
	 */
	
	public ScriptUnit addUnit(Unit u){
		// ist die Map schon angelegt worden?
		if (scriptUnits == null){
			scriptUnits = new Hashtable<Unit, ScriptUnit>();
		}
		
		// neue ScriptUnit anlegen
		ScriptUnit new_su = new ScriptUnit(u,this);
		// falls wir einen client context haben...
		if (this.client!=null) {new_su.setClient(this.client);}
		// hinzufuegen
		scriptUnits.put(u,new_su);
		// regionen hinzufügen
		Region r = u.getRegion();
		if (this.scriptRegions==null){
			this.scriptRegions = new ArrayList<Region>();
		}
		if (!this.scriptRegions.contains(r)){
			this.scriptRegions.add(r);
		}
		
		// outText.addOutLine("..zu ScriptMain hinzugefuegt: " + u.toString(true));
		
		// 20171014: scriptPoductionBuildings pflegen
		Building b = u.getModifiedBuilding();
		if (b!=null){
			boolean toAdd=false;
			if (b.getType().getName().equalsIgnoreCase("Sägewerk")){
				toAdd=true;
			}
			if (b.getType().getName().equalsIgnoreCase("Schmiede")){
				toAdd=true;
			}
			if (b.getType().getName().equalsIgnoreCase("Bergwerk")){
				toAdd=true;
			}
			if (b.getType().getName().equalsIgnoreCase("Steinbruch")){
				toAdd=true;
			}
			if (b.getType().getName().equalsIgnoreCase("Akademie")){
				toAdd=true;
			}
			if (b.getType().getName().equalsIgnoreCase("Pferdezucht")){
				toAdd=true;
			}
			if (toAdd){
				if (scriptProductionBuildings==null){
					scriptProductionBuildings = new ArrayList<Building>();
				}
				if (!scriptProductionBuildings.contains(b)){
					scriptProductionBuildings.add(b);
				}
			}
		}
		
		return new_su;
		
	}
	
	public int getNumberOfScriptUnits(){
		if (scriptUnits==null){return 0;} else {return scriptUnits.size();}
	}
	
	public int getNumberOfScriptPersons(){
		if (scriptUnits==null){return 0;} else {
			int erg = 0;
			for (ScriptUnit su:scriptUnits.values()){
				erg += su.getUnit().getPersons();
			}
			return erg;
		}
	}
	
	
	
	/**
	 * HERE WE GO
	 * hier wird festgeleget, was in welcher Reihenfolge passiert
	 * Idee: durchlauf 1+2, dann MatPool und andere, dann durchlauf 3...
	 */
	
	public void runScripts(){
		
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		
		long startT = System.currentTimeMillis();
		if (scriptUnits==null){
			outText.addOutLine("runScripts nicht moeglich: keine scriptUnits");
			return;
		}
		if (this.gd_ScriptMain==null){
			outText.addOutLine("runScripts nicht moeglich: kein GameData Object");
			return;
		}
		reportSettings.setGameData(this.gd_ScriptMain);
		reportSettings.reset();
		reportSettings.setScriptMain(this);
		
		
		this.stopRelationUpdates();
		
		FFToolsRegions.remove_AllMapLines_CR(this.gd_ScriptMain);
		
		
		this.readReportSettings();
		
		// Debug Tests
		TreiberPoolManager TPM = this.getOverlord().getTreiberPoolManager();
		
		// Debug Tests
		CircusPoolManager CPM = this.getOverlord().getCircusPoolManager();
		
		// Debug Tests
		TradeAreaHandler TAH = this.getOverlord().getTradeAreaHandler();
		
		MonsterJagdManager_MJM MJM = this.getOverlord().getMJM();
		
		// just in case we run twice with changed excluded regions
		Regions.setExcludedRegions(null);
		
		// not the the console, but to the logfile...
		outText.setScreenOut(false);
		
		// gettimg some info from reportsettings...
		reportSettings.informUs();
		
		// gettimg some info from TRade Areas...
		// this.getOverlord().getTradeAreaHandler().informUs();
		
		outText.setScreenOut(true);
		
		// verlagere die abarbeitung zum overlord
		this.getOverlord().run();

		// zeige info, was wan gelaufen ist/sein könnte ;)
		this.getOverlord().informUs();
		
		// TM info
		this.getOverlord().getTransportManager().informUs();
		// MP getInfo
		outText.setScreenOut(false);
		this.getOverlord().getMatPoolManager().informUs();
		// gettimg some info from TRade Areas...
		this.getOverlord().getTradeAreaHandler().informUs();
		outText.setScreenOut(true);
		
		long bet_start = System.currentTimeMillis();
		outText.addOutLine("Test - refreshing between runs");
		this.restartRelationUpdates();
		this.refreshScripterRegions();
		this.refreshClient();
		this.stopRelationUpdates();
		long bet_end = System.currentTimeMillis();
		outText.addOutLine("refreshing between runs benötigte " + (bet_end-bet_start) + " ms.");
		
		
		outText.addOutLine("unit final confirm");
		// als Vorletztes den confirm-status der units setzen
		// und autoTags setzen
		long ufc_start = System.currentTimeMillis();
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			scrU.checkShipOK(); // 20160724 FF
			scrU.checkNACHOK(); // 20161015 FF
			scrU.checkOverallCommand(); // 20190216 FF
			scrU.setFinalConfim();
			// autoTags
			scrU.autoTags();
		}
		long ufc_end = System.currentTimeMillis();
		outText.addOutLine("unit final confirm benötigte " + (ufc_end-ufc_start) + " ms.");
		
		// 20171014 - BEZAHLE NICHT check
		String orderStartWith = "MACHE";
		String orderStartWith2 = "LERNE";
		String orderStartWith3 = "LEHRE";
		String orderStartWith4 = "ZÜCHTE";
		String suZahleNICHT = "";
		ufc_start = System.currentTimeMillis();
		outText.addOutLine("start: Gebäudecheck für BEZAHLE NICHT");
		if (scriptProductionBuildings!=null && scriptProductionBuildings.size()>0){
			for (Iterator<Building> iter = scriptProductionBuildings.iterator(); iter.hasNext();){
				Building b = (Building)iter.next();
				boolean istAkademie = false;
				if (b.getType().getName().equalsIgnoreCase("Akademie")){
					istAkademie=true;
				}
				Unit u = b.getModifiedOwnerUnit();
				// outText.addOutLine("checke " + b.toString());
				if (u!=null){
					ScriptUnit su = scriptUnits.get(u);
					if (su!=null){
						// OK, owner ist ScriptUnit
						// checken, ob alle arbeiten
						ArrayList<Unit> insiders = new ArrayList<Unit>();
						// outText.addOutLine("creating insiders");
						for (Unit u3 : u.getRegion().getUnits().values()){
							// outText.addOutLine("checking " + u3.toString());
							if (u3.getModifiedBuilding()!=null){
								if (u3.getModifiedBuilding().equals(b)){
									// outText.addOutLine("added");
									insiders.add(u3);
								}
							}
						}
						// outText.addOutLine("checking insiders");
						if (insiders.size()>0){
							boolean OneInsideWorking=false;
							String Workingers = "";
							String WorkingersForeigners = "";
							for (Unit u2:insiders){
								// outText.addOutLine("checking " + u2.toString(true));
								// kenne ich die befehle vom Insassen?
								if (u2.isDetailsKnown()){
									// outText.addOutLine("checking orders");
									boolean unitWorking = false;
									if (u2.getOrders2()!=null && u2.getOrders2().size()>0){
										for(Iterator<Order> iterO = u2.getOrders2().iterator(); iterO.hasNext();) {
											Order o = (Order) iterO.next();
											String s = o.getText();
											// @entfernen
											s = s.replace("@", "");
											if (istAkademie){
												// LERNE
												if ((s.length()>=orderStartWith2.length()) &&  s.substring(0, orderStartWith2.length()).equalsIgnoreCase(orderStartWith2)){
													unitWorking=true;
													break;
												}
												// LEHRE
												if ((s.length()>=orderStartWith3.length()) &&  s.substring(0, orderStartWith3.length()).equalsIgnoreCase(orderStartWith3)){
													unitWorking=true;
													break;
												}
											} else {
												// MACHE
												if ((s.length()>=orderStartWith.length()) &&  s.substring(0, orderStartWith.length()).equalsIgnoreCase(orderStartWith)){
													unitWorking=true;
													break;
												} 
												// ZÜCHTE
												if ((s.length()>=orderStartWith4.length()) &&  s.substring(0, orderStartWith4.length()).equalsIgnoreCase(orderStartWith4)){
													unitWorking=true;
													break;
												} 
											}
										}
									}
									if (unitWorking){
										OneInsideWorking=true;
										Workingers += " " + u2.toString(true);
									} 
								} else {
									// den kenn ich nicht
									OneInsideWorking=true;
									WorkingersForeigners += " " + u2.toString(true);
								}
							}
							
							if (OneInsideWorking){
								// irgendjemand arbeitet
								String sText = "Gebäudebesitzer, Prüfung auf Unterhaltsbedarf ergibt Bedarf durch diese Einheiten: ";
								if (Workingers.length()>1){
									sText += Workingers;
								}
								if (WorkingersForeigners.length()>1){
									sText += " (unbekannte Befehle von: " + WorkingersForeigners + ")";
								}
								su.addComment(sText);
							} else {
								// niemand arbeitet
								su.addComment("Gebäudebesitzer und niemand arbeitet, Unterhalt nicht nötig");
								su.addOrder("BEZAHLE NICHT ;niemand arbeitet...", true);
								suZahleNICHT += " " + su.toString();
							}
							
						} else {
							su.doNotConfirmOrders("!!! keine Insassen im Gebäude ?!?!?");
							su.addComment("!!! keine Insassen im Gebäude ?!?!?");
						}
					}
				}
			}
		}
		ufc_end = System.currentTimeMillis();
		if (suZahleNICHT.length()>2){
			// outText.addOutLine("BEZAHLE NICHT bei: " + suZahleNICHT);
		}
		
		outText.addOutLine("BEZAHLE NICHT benötigte " + (ufc_end-ufc_start) + " ms.");
		
		
		// doNotConfirmGründe extra ausgeben
		// Zahlen mitzählen
		int confirmedScripterUnits=0;
		int unConfirmedScripterUnits=0;
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			scrU.informDoNotConfirmReason();
			if (scrU.getUnit().isOrdersConfirmed()) {
				confirmedScripterUnits++;
			} else {
				unConfirmedScripterUnits++;
			}
		}
		
		
		outText.addOutLine("setting tags");
		// finally..tags FF 20070531
		FFToolsTags.AllOrders2Tags(this.gd_ScriptMain);
		
		// FF 20080804 : trustlevels
		this.resetFactionTrsutLevel();
		
		// und zum schluss refreshen
		// natuerlich nur, wenn wir nen client haben..
		outText.addOutLine("refreshing client");
		
		this.restartRelationUpdates();
		
		this.refreshClient();
		
		long endT = System.currentTimeMillis();
		
		if (this.client!=null && this.client.getSelectedRegions()!=null && this.client.getSelectedRegions().size()>0){
			outText.addOutLine("!Achtung. Nur selektierte Regionen wurden bearbeitet. Anzahl: " + this.client.getSelectedRegions().size());
		}
		outText.addOutLine("Statistik: " + confirmedScripterUnits + " Script-Einheiten bestätigt, " + unConfirmedScripterUnits + " unbestätigt.");
		outText.addOutLine("runScripts benötigte " + (endT-startT) + " ms.");

	}
	
	
	public void refreshClient(){
		if (this.client!=null){
			// durchläuft alle registrierten Scriptunits
			// diese checken selsbt, ob orders geändert wurden und wenn ja
			// (und auch diese einen client haben)
			// wird OrdersChangeEvent gefeuert
			/**
			for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
				ScriptUnit scrU = (ScriptUnit)iter.next();
				scrU.refreshClient();
			}
			*/
			// neuer Anlauf...regionen refreshen
			/*
			for (Region r:this.scriptRegions){
				r.refreshUnitRelations(true);
			}
			*/
			this.client.getMagellanContext().getEventDispatcher().fire(new GameDataEvent(this, this.client.getData()));
			
		}
	}
	
	
	/**
	 * durchläuft sämtliche scriptunits und checked, ob in den orders
	 * optionale konfigs vorliegen
	 * 
	 * wird aufgebohrt für alle Sachen, die VOR den eigentlichen scripts
	 * über "ALLES" laufen sollen.
	 */
	private void readReportSettings(){
		
		if (scriptUnits==null || scriptUnits.size()==0){
			System.out.println("aborting readReportSettings - no script Units");
			return;
		}
		
		
		// ok durch 2 durchläufe
		// weil für prepare Handel schon reportsettungs notwendig
		// Lauf 1
		// System.out.println("ReadReportSettings 1");
		outText.addOutLine("ReadReportSettings 1");
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			// auf reportsetting einträge prüfen
			if (!this.getOverlord().isDeleted(scrU)){
				scrU.readReportSettings();
				outText.addPoint();
			}
			
		}
		// Lauf 2
		// System.out.println("ReadReportSettings 2");
		outText.addOutLine("ReadReportSettings 2 and creating the scripts");
		long lastProzentAngabe = 0;
		long scriptU_Counter=0;
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			// auf Handel prüfen (TradeArea bauen)
			if (!this.getOverlord().isDeleted(scrU)){
				this.prepareHandel(scrU);
				// gleich mal eben die scriptlisten bauen
				scrU.builtScriptList();
				outText.addPoint();
			}
			
			scriptU_Counter++;
			if ((System.currentTimeMillis() - lastProzentAngabe)>1000) {
				double actProz = (double)((double)scriptU_Counter/(double)scriptUnits.values().size());
				long actProzL = Math.round(actProz * 100);
				outText.addOutChars("|" + actProzL + "%");
				lastProzentAngabe = System.currentTimeMillis();
			}
			
		}
		
		// aus den settings resultierende Paramter usw
		
		
		// TM anstossen
		// System.out.println("TM");
		outText.addOutLine("TM-presetup");
		this.getOverlord().getTransportManager().initReportSettings(reportSettings);
		
		
		// Jetzt alle scriptunits bei den matpools anmelden..
		// System.out.println("MP-Anmeldungen");
		outText.addOutLine("Units to Matpools");
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			if (!this.getOverlord().isDeleted(scrU)){
			// MatPool mp = this.overlord.getMatPoolManager().getRegionsMatPool(scrU);
				this.overlord.getMatPoolManager().getRegionsMatPool(scrU);
			}
		}
		// System.out.println("finished ReadReportSettings\n");
		outText.addOutLine("Removing deletet units");
		// Entfernte ScriptUnits tatsächlich entfernen
		ArrayList<ScriptUnit> removeIt = new ArrayList<ScriptUnit>();
		for (Iterator<ScriptUnit> iter = scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			if (this.getOverlord().isDeleted(scrU)){
				removeIt.add(scrU);
			}
		}
		for (ScriptUnit su:removeIt){
			this.scriptUnits.remove(su.getUnit());
		}
	}
	
	public void extractScriptUnits(){
		if (gd_ScriptMain == null) {
			return;
		}
		int numberOfRegions = gd_ScriptMain.getRegions().size();
		int numberOfUnits = gd_ScriptMain.getUnits().size();
		outText.addOutLine("Overall: found " + numberOfRegions + " Regions and " + numberOfUnits + " Units.");
		
		// ignore Factions ermittlen
		this.ignoreList=null;
		String s = null;
		if (getSettings()!=null){
			s = getSettings().getProperty(ScriptMain.FFTools2_ignoreFactions, null);
		}
		if (s!=null){
			String[] splitter = s.split(",");
			for (String ss : splitter){
				if (this.ignoreList==null){
					this.ignoreList = new ArrayList<String>();
				}
				this.ignoreList.add(ss);
				outText.addOutLine("Faction to be ignored (from ini file: " + ss);
			}
		}
		
		// Neu: ignore List aus befehle ergänzen
		// -> // private final static String FFTools2_ignoreFactions = "FFTools2.ignoreFactions";
		// durch die Regionen wandern..
		String ids = "// " + ScriptMain.FFTools2_ignoreFactions + " ";
		for (Region r: gd_ScriptMain.getRegions()){
			if (r.getUnits()!=null && r.getUnits().size()>0){
				for (Unit u:r.getUnits().values()){			
					if (u.getOrders2()!=null && u.getOrders2().size()>0){
						for (Order ord:u.getOrders2()){
							String so = ord.getText();
							if (so.startsWith(ids)){
								String factionToAdd =  so.substring(ids.length());
								if (factionToAdd.length()>1){
									if (this.ignoreList==null){
										this.ignoreList = new ArrayList<String>();
									}
									this.ignoreList.add(factionToAdd);
									outText.addOutLine("Faction to be ignored (from unitorder): " + factionToAdd + " (" + u.toString() + ")");
								}
							}
						}
					}
				}
			}
		}
		
		
		
		// durch die Regionen wandern..
		boolean isInActualSelectedRegions = true;
		Client c = this.client;
		for (Region r:gd_ScriptMain.getRegions()){
			if (r.getUnits()!=null && r.getUnits().size()>0){
				isInActualSelectedRegions = true;
				if (c.getSelectedRegions()!=null && c.getSelectedRegions().size()>0){
					if (!c.getSelectedRegions().values().contains(r)){
						isInActualSelectedRegions=false;
					}
				}
				for (Unit u:r.getUnits().values()){
					boolean ignore = false;
					if (this.ignoreList != null){	
						for (String ss : this.ignoreList){
							if (u.getFaction().getID().toString().equalsIgnoreCase(ss)){
								ignore=true;
								break;
							}
						}
					}
					
					
					if (!ignore && ScriptUnit.isScriptUnit(u)){
						ScriptUnit su = this.addUnit(u);
						this.setFactionTrustlevel(u.getFaction());
						su.setInClientSelectedRegions(isInActualSelectedRegions);
					}
				}
			}
		}
		
		// Gleich die Orders einmal saven = clearen
		outText.addOutLine("removing unprotected orders");
		for (ScriptUnit su: this.scriptUnits.values()){
			su.saveOriginalScriptOrders();
		}
		
		// einmal updaten
		outText.addOutLine("refreshing regions after adding the scriptunits and removing unprotected orders");
		this.refreshScripterRegions();
		
		outText.addOutLine("Scripter enthaelt " + getNumberOfScriptUnits() + " units...starte scripter\n");
	}
	
	private void setFactionTrustlevel(Faction f){
		if (!f.isPrivileged()){
			if (this.changedTrustlevels==null){
				this.changedTrustlevels = new Hashtable<Faction, Integer>(0);
			}
			
			Integer knownTrustLevel = this.changedTrustlevels.get(f);
			if (knownTrustLevel==null){
				knownTrustLevel = Integer.valueOf(f.getTrustLevel());
				this.changedTrustlevels.put(f,knownTrustLevel);
			}
			f.setTrustLevel(100);
			f.setTrustLevelSetByUser(true);
		}
	}
	
	
	private void resetFactionTrsutLevel(){
		if (this.changedTrustlevels==null || this.changedTrustlevels.size()==0){
			return;
		}
		for (Iterator<Faction> i = this.changedTrustlevels.keySet().iterator();i.hasNext();){
			Faction f = (Faction) i.next();
			Integer trustLevel = this.changedTrustlevels.get(f);
			f.setTrustLevel(trustLevel.intValue());
			f.setTrustLevelSetByUser(true);
		}
	}
	
	/**
	 * checked, ob unit einen Händler enthält
	 * wenn ja wird TadeArea gehandelt...
	 * festsetzung // script handeln
	 * @param u
	 */
	private void prepareHandel(ScriptUnit u){
		
		// Einschub: jede scriptunit bei ihrem MaterialPool anmelden
		// MatPool mp = this.getOverlord().getMatPoolManager().getRegionsMatPool(u);
		
		boolean isHandel = false;
		for(Order o:u.getUnit().getOrders2()) {
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script handeln")){
				isHandel=true;
				break;
			}
		}
		if (!isHandel){
			// kein Händler
			return;
		} else {
			// alles weitere in TAH
			this.getOverlord().getTradeAreaHandler().addTrader(u);	
		}
	}
	

	
	


	
	/**
	 * wir müssen tatsächlich die relations refreshen...eventuell nur für console...
	 * 
	 */
	public void refreshScripterRegions(){
		if (this.scriptUnits==null){return;}
		if (this.scriptRegions==null) {return;}
		EresseaRelationFactory ERF = ((EresseaRelationFactory) gd_ScriptMain.getGameSpecificStuff().getRelationFactory());
		boolean updaterStopped = ERF.isUpdaterStopped();
		if (!updaterStopped){
			ERF.stopUpdating();
		}
		for (Iterator<Region> iter = this.scriptRegions.iterator();iter.hasNext();){
			Region r = (Region)iter.next();
			// r.refreshUnitRelations(true);
			// gd_ScriptMain.getGameSpecificStuff().getRelationFactory().createRelations(r);
			
			ERF.processRegionNow(r);
		}
		if (!updaterStopped){
			ERF.restartUpdating();
		}
	}

	
	public void stopRelationUpdates(){
		EresseaRelationFactory ERF = ((EresseaRelationFactory) gd_ScriptMain.getGameSpecificStuff().getRelationFactory());
		ERF.stopUpdating();
	}
			
	public void restartRelationUpdates(){
		EresseaRelationFactory ERF = ((EresseaRelationFactory) gd_ScriptMain.getGameSpecificStuff().getRelationFactory());
		ERF.restartUpdating();
	}

  public Overlord getOverlord(){
	  if (this.overlord==null){
		  this.overlord = new Overlord(this);
	  }
	  return this.overlord;
  }

	/**
	 * @return the scriptUnits
	 */
	public Hashtable<Unit, ScriptUnit> getScriptUnits() {
		return scriptUnits;
	}
	
	/**
	 * liefert zu einer einheitennummer die scriptunit, wenn vorhanden
	 * @param unitID
	 * @return
	 */
	public ScriptUnit getScriptUnit(String unitID){
		ScriptUnit erg = null;
		for (Iterator<ScriptUnit> iter=this.scriptUnits.values().iterator();iter.hasNext();){
			ScriptUnit actUnit = (ScriptUnit)iter.next();
			if (actUnit.getUnit().toString(false).equalsIgnoreCase(unitID)){
				return actUnit;
			}
		}
		return erg;
	}
	
	/**
	 * liefert zu einer Unit die ScriptUnit oder Null
	 * falls nicht vorhanden
	 * @param u
	 * @return
	 */
	public ScriptUnit getScriptUnit(Unit u){
		ScriptUnit erg = null;
		if (this.scriptUnits!=null){
			return this.scriptUnits.get(u);
		}
		return erg;
	}
	
	
/**
 * Liefert Hashtable der ScriptUnits einer Region oder Null
 * @param Region _region
 * @return
 */
	public Hashtable <Unit, ScriptUnit> getScriptUnits(Region _region){
		Hashtable <Unit, ScriptUnit> regionsScriptUnits =null;
		// alle ScriptUnits durchiterieren...und auf region checken
		for (Iterator<ScriptUnit> iter = this.scriptUnits.values().iterator(); iter.hasNext();){
			ScriptUnit kandidat = (ScriptUnit) iter.next();
			if (kandidat.getUnit().getRegion().equals(_region)){
				// Table erzeugen
				if (regionsScriptUnits == null){
					regionsScriptUnits = new Hashtable <Unit, ScriptUnit> ();
				}
				regionsScriptUnits.put(kandidat.getUnit(), kandidat);
			}
		}
		return regionsScriptUnits;
		
	}

/**
 * @return the settings
 */
public Properties getSettings() {
	return settings;
}

/**
 * @param settings the settings to set
 */
public void setSettings(Properties settings) {
	this.settings = settings;
}
	
/**
 * compares report-factions: hereos, mages, items (?)
 */
public void createFactionCompare(){
	
	boolean oldScreenOut = outText.isScreenOut();
	String oldFile = outText.getActFileName();
	outText.setFile("Faction_compare");
	outText.setScreenOut(false);
	
	
	// TabHead
	outText.addNewLine();
	outText.addOutChars("Faction;",40);
	outText.addOutChars("#Uni;",9);
	outText.addOutChars("#Pers;",9);
	outText.addOutChars("#Her;",9);
	outText.addOutChars("#Mag;",9);
	outText.addOutChars("#Ver;",9);
	outText.addOutChars("Items;");

	
	for (Faction f:this.gd_ScriptMain.getFactions()){
		if (f.getAge()>0){
			// wir haben report Infos
			outText.addNewLine();
			// Name
			outText.addOutChars(f.toString() + ";", 40);
			// # Anzahl units
			if (f.units()!=null && f.units().size()>0){
				outText.addOutChars(f.units().size() + ";", 9);
				// # Personen
				outText.addOutChars(f.getPersons() + ";", 9);
				// Hereos avail
				outText.addOutChars((f.getMaxHeroes() - f.getHeroes()) + ";", 9);
				// Mages...
				int countMages = 0;
				int countVertraute = 0;
				for (Unit u:f.units()){
					if (u.getAuraMax()>-1 && u.getFamiliarmageID()==null){
						countMages++;
					}
					if (u.getFamiliarmageID()!=null){
						countVertraute++;
					}
				}
				outText.addOutChars(countMages + ";", 9);
				// Vertraute
				outText.addOutChars(countVertraute + ";", 9);
				// Items
				for (Item it:f.getItems()){
					outText.addOutChars(it.getName()+" ");
				}
				outText.addOutChars(";");
			} else {
				outText.addOutChars("!!! no units !!!");
			}
		}
	}
	
	
	outText.addNewLine();
	outText.addOutLine("E O F");
	outText.setFile(oldFile);
	outText.setScreenOut(oldScreenOut);
	
}

public void updateRelations(String extraText) {
	long bet_start = System.currentTimeMillis();
	outText.addOutLine("Test - refreshing  - updating relations (" + extraText + ")");
	this.restartRelationUpdates();
	this.refreshScripterRegions();
	this.refreshClient();
	this.stopRelationUpdates();
	long bet_end = System.currentTimeMillis();
	outText.addOutLine("refreshing benötigte " + (bet_end-bet_start) + " ms.");
}


	
}
