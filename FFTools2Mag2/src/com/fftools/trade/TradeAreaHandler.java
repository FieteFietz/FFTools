package com.fftools.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Region;
import magellan.library.rules.ItemType;
import magellan.library.rules.RegionType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Script;
import com.fftools.scripts.Vorrat;
import com.fftools.transport.TransportRequest;
import com.fftools.utils.FFToolsRegions;



/**
 * 
 * a class to handle TradeAreas
 * shortform TAH
 * @author Fiete
 *
 */

public class TradeAreaHandler implements OverlordRun,OverlordInfo{
	public static final String MAPLINE_TAG_ID="FFTools_TAC_Line";
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	/*
	 * Ausgabe in TransportRequests_Name_X.txt ?
	 */
	private boolean reportOFF = false;
	
	/*
	 * Ausgabe in TransportRequests_Name_X.txt ?
	 */
	private boolean reportOFF_TAH_TAC = false;
	
	
	private static final int Durchlauf1 = 13;
	private static final int Durchlauf1_Recalc = 32; // parallel zum Trader => genau nach ihm
	private static final int Durchlauf2 = 105; // Vor OnTAC
	private static final int Durchlauf3 = 125; // Nach OnTAC
	private static final int Durchlauf4 = 864; // irgendwann
	
	
	private int[] runners = {Durchlauf1,Durchlauf1_Recalc,Durchlauf2,Durchlauf3,Durchlauf4};
	
	private boolean debugOutput = false;
	
	/**
	 * List of TradeAreas
	 */
	private ArrayList<TradeArea> tradeAreas = null;
	
	/**
	 * enthält alle Regionen und passene tradeRegionen
	 * wird benutzt, um die Information abzulegen, ob eine
	 * Region automatisch einem TA zugeordnet werden kann
	 * oder manuell zugeordnet wurde (TradeArea = Name)
	 */
	private Hashtable<Region,TradeRegion> tradeRegions = null;
	
	/**
	 * enthält die Scriptunits, die als Trader erkannt worden sind
	 * und die entsprechend angelegten <Trader>
	 */
	private Hashtable<ScriptUnit,Trader> traders = null;
	
	/**
	 * alle definierten TACs
	 */
	private ArrayList<TradeAreaConnector> tradeAreaConnectors = null;
	
	/**
	 * TAs, für die extra reporte angefordert wurden mit IslandInfo
	 */
	private ArrayList<TradeArea> islandInfos = new ArrayList<TradeArea>();
	
	
	/**
	 * the Game Data...needed for getting the rules for pathfinder
	 */
	private GameData data = null;
	
	/**
	 * keep reference to *all*
	 */
	public ScriptMain scriptMain = null;
	
	/**
	 * sammeln der setOrigins bis Run=32, dann recalc 1x anstoßen
	 */
	private boolean needRecalcTradeAreas = false;
	
	/**
	 * constructs a new TAH
	 * @param GameData  the game data
	 */
	public TradeAreaHandler(ScriptMain _scriptMain) {
		this.scriptMain = _scriptMain;
		this.data = _scriptMain.gd_ScriptMain;
		this.reportOFF = reportSettings.getOptionBoolean("disable_report_TradeAreas");
		this.reportOFF_TAH_TAC = reportSettings.getOptionBoolean("disable_report_TAH_TAC");
	}
	
	
	/**
	 * gives the TradeArea for the given Trader
	 * @param t
	 * @param allowNew
	 * @return
	 */
	public TradeArea getTradeArea(Trader t,boolean allowNew){
		TradeRegion tR = this.getTradeRegion(t.getScriptUnit().getUnit().getRegion());
		
		if (t.isAreaOriginSetter()){
			tR.setSetAsTradeAreaOrigin(true);
			tR.setTradeAreaName(t.getSetAreaName());
		}
		return this.getTradeArea(tR, allowNew);
	}
	
	
	
	/**
	 * Gets the TradeArea for the given TradeRegion
	 * if the region is not in one TA, a new may
	 * be set up, if allowNew = true
	 * 
	 * @param r
	 * @return
	 */
	public TradeArea getTradeArea(TradeRegion tradeRegion,boolean allowNew){
		Region r = tradeRegion.getRegion();
		TradeArea tA = null;
		// no TAs present..create List and add the first TA
		if (this.tradeAreas==null){
			if (debugOutput){
				outText.addNewLine();
				outText.addOutChars("Debug TAH: creating new TA-List");
			}
			this.tradeAreas = new ArrayList<TradeArea>();
			tA = new TradeArea(tradeRegion,this.scriptMain.getOverlord());
			
			if (tradeRegion.isSetAsTradeAreaOrigin()){
				tA.setName(tradeRegion.getTradeAreaName());
			}
			this.addTradeArea(tA);
			return tA;
		}
		
		// if we have tradeAreas designed and a units has a 
		// option TradeArea activated...another procedure is used..
		if (tradeRegion.isSetAsTradeAreaOrigin()){
			// first...is there already such an TA?
			
			if (debugOutput){
				outText.addNewLine();
				outText.addOutChars("Debug TAH: region is set as TA Origin - searching for a TA-Name");
			}
			
			
			tA = this.getTradeAreaByName(tradeRegion.getTradeAreaName());
			if (tA!=null){
				tA.addRegion(tradeRegion);
				if (debugOutput){
					outText.addNewLine();
					outText.addOutChars("Debug TAH: found and returning: " + tA.getName());
				}
				return tA;
			}
			// ok...nothing to find..create new one...
			// if we may...
			if (!allowNew){
				if (debugOutput){
					outText.addNewLine();
					outText.addOutChars("Debug TAH: region is set as TA Origin - no TA found, returning NULL.");
				}
				return null;
			}
			// lets do it
			if (debugOutput){
				outText.addNewLine();
				outText.addOutChars("Debug TAH: region is set as TA Origin - no TA found, creating new.");
			}
			tA = new TradeArea(tradeRegion,this.scriptMain.getOverlord());
			this.addTradeArea(tA);
			tA.setName(tradeRegion.getTradeAreaName());
			
			// with this addition other regions may be now
			// in another TA...
			// we have to check all regions again...
			// will be done after in calling proc
			return tA;
		}
		
		// we have to go through our TAs to find the right one
		// here we check only, if the region is already present
		if (debugOutput){
			outText.addNewLine();
			outText.addOutChars("Debug TAH: region is NOT set as TA Origin - searching the TAs, if one is our region in it.");
		}
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			tA = (TradeArea)iter.next();
			if (tA.contains(tradeRegion)) {
				if (debugOutput){
					outText.addNewLine();
					outText.addOutChars("Debug TAH: found TA with our regionm returning " + tA.getName());
				}
				return tA;
			}
		}
		
		// check, if we have in TA in Range
		if (debugOutput){
			outText.addNewLine();
			outText.addOutChars("Debug TAH: no TA woth region in it found, no searching TA in Range...");
		}
		tA = this.getTAinRange(r);
		if (tA!=null){
			// yes...lets add the region 
			tA.addRegion(tradeRegion);
			// and return the tA
			
			if (debugOutput){
				outText.addNewLine();
				outText.addOutChars("Debug TAH: found TA in Range, returning: " + tA.getName());
			}
			
			return tA;
		}
		
		
		// nothing found, if I cannot make one...i must give up
		if (!allowNew){
			if (debugOutput){
				outText.addNewLine();
				outText.addOutChars("Debug TAH: found no TA in Range, may Not make a new one, returning NULL.");
			}
			return null;
		}
		
		// still nothing found....making a new one
		tA = new TradeArea(tradeRegion,this.scriptMain.getOverlord());
		this.addTradeArea(tA);
		
		if (debugOutput){
			outText.addNewLine();
			outText.addOutChars("Debug TAH: found no TA in Range, created a new one:" + tA.getName());
		}
		
		
		return tA;
	}
	
	/**
	 * checks the known TAs (list must be non null) if there
	 * is a landpath betwenn r and originRegion
	 * if so..the region is added and the TA returned
	 * 
	 * FF made public and checked non null tradeareas
	 * 
	 * @param r Region to search a TradeArea for
	 * @return the TradeArea if found, or null
	 */
	public TradeArea getTAinRange(Region r){
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(this.data.getRules());
		RegionType Feuerwand = Regions.getFeuerwandRegionType(this.data);
		excludeMap.put(Feuerwand.getID(), Feuerwand);
		Region actOriginRegion = null;
		
		if (this.tradeAreas==null){
			return null;
		}
		
		Hashtable<TradeArea,Integer> matches = new Hashtable<TradeArea, Integer>(1);
		for (Iterator<TradeArea> iter=this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = ((TradeArea)iter.next());
			actOriginRegion = tA.getOriginRegion();
			
			// einschub: in origin
			if (actOriginRegion.equals(r)){
				return tA;
			}
			
			// alle originSetters finden und vergleichen
			if (tA.getTradeRegions()!=null) {
				for (TradeRegion tR:tA.getTradeRegions()) {
					if (tR.isSetAsTradeAreaOrigin() && tR.getRegion().equals(r)) {
						return tA;
					}
				}
			}
			
			
			if (TradeUtils.onSameIsland(r, actOriginRegion, this.data)) {
				String path = Regions.getDirections(this.data, actOriginRegion.getID() , r.getID(), excludeMap,1);
				if (path!=null && path.length()>0) {
					// weg gefunden
					// länge bestimmen = Anzahl der richtungen
					String[] zeichen = path.split(" ");
					int i = zeichen.length;
					matches.put(tA, Integer.valueOf(i));
				}
			}
		}
		
		// if matches is empty -> nothing was found...returning null		
		if (matches.isEmpty()){
			return null;
		}
		
		// ok..so we found something, at least one
		// i don´t want to sort the matches...using the old way
		TradeArea maxEntf = null;
		int actMaxEntf = Integer.MAX_VALUE;
		for (Iterator<TradeArea> iter = matches.keySet().iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			Integer actEntf = matches.get(tA);
			if (actEntf.intValue()<actMaxEntf){
				// ok...we have a new good tA
				maxEntf = tA;
				actMaxEntf = actEntf.intValue();
			}
		}
		
		// in maxEntf now the resulting TA
		return maxEntf;
	}
	
	public void informUs(){
		// if (outText.getTxtOut()==null) {return;}
		
		if (this.reportOFF){
			return;
		}
		
		outText.setFile("TradeAreas");
		
		outText.addOutLine("******TradeAreaHandlerInfo******");
		if (this.tradeAreas!=null){
			outText.addOutLine("I have " + this.tradeAreas.size() + " Trade Areas (TAs) registered");
		} else {
			outText.addOutLine("I have no Trade Areas registered");
			return;
		}
		outText.addOutLine("calling now the TradeAreas for their info...");
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			tA.informUs();
		}
		outText.setFile("TradeAreas");
		outText.addOutLine("***EndOF TadeAreaHandlerInfo***");
		
		this.informInterTARelations();
		
		outText.setFileStandard();
		
	}
	
	/**
	 * informiert exclusiv über die Requests (Wer verlangt was mit welcher Prio)
	 *
	 */
	
	public void informUsTransportRequests(){
		// if (outText.getTxtOut()==null) {return;}
		outText.setFile("TAH - TransportRequests");
		
		outText.addOutLine("******TradeArea - Transport Requests******");
		if (this.tradeAreas!=null){
			outText.addOutLine("I have " + this.tradeAreas.size() + " Trade Areas (TAs) registered");
		} else {
			outText.addOutLine("I have no Trade Areas registered");
			return;
		}
		outText.addOutLine("calling now the TradeAreas for their info...");
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			tA.informUsTradeTransportRequests(this.scriptMain.getOverlord());
		}
		outText.addOutLine("***EndOF TadeAreaTransportRequestInfo***");
		
		outText.setFileStandard();
		
	}
	
	/**
	 * liefert alle TransportRequests der TradeAreas
	 * @return
	 */
	public ArrayList<TransportRequest> getTransportRequests(){
		ArrayList<TransportRequest> erg = new ArrayList<TransportRequest>();
		if (this.tradeAreas==null){
			return erg;
		}
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			ArrayList<TransportRequest> actList = tA.getTransportRequests(this.scriptMain.getOverlord());
			if (actList==null) {
				outText.addOutLine("??? TradeArea " + tA.getName() + " hat keine Transportaufträge");
			} else {
				erg.addAll(actList);
			}
		}
		return erg;
	}
	
	
	
	
	/**
	 * beim pre-parsen der units auf einen Händler gestossen
	 * parst optionen
	 * fügt Region dem TA hinzu bzw legt ein TA neu an
	 * @param u
	 */
	public Trader addTrader(ScriptUnit u){
		
		if (this.traders==null){
			this.traders = new Hashtable<ScriptUnit,Trader>();
		}		
		Trader t = this.traders.get(u);
		if (t==null) {
			t = new Trader(u);
			this.traders.put(u,t);
		}
		
		TradeArea tA = this.getTradeArea(t, true);
		if (tA==null){
			outText.addOutLine("!!! TradeArea not created for unit (handel): " +  u.getUnit().toString(true));
			return null;
		}
		tA.addTrader(t);
		
		// wenn ein trader sein TA manuell setzt, müssen bekannte
		// TradeRegions untersucht werden, ob sie nicht nun einem
		// anderen TA zugeordnet werden müssen...
		if (t.isAreaOriginSetter()){
			this.recalcTradeAreas();
		}
		return t;
	}
	
	private TradeArea getTradeAreaByName(String name){
		if (this.tradeAreas==null){return null;}
		for (Iterator<TradeArea> iter = this.tradeAreas.iterator();iter.hasNext();){
			TradeArea tA = (TradeArea)iter.next();
			if (tA.getName()!=null){
				if (tA.getName().equalsIgnoreCase(name)){
					return tA;
				}
			}
		}
		return null;
	}
	
	private void addTradeArea(TradeArea _tA){
		if (this.tradeAreas==null){
			this.tradeAreas = new ArrayList<TradeArea>();
		}
		this.tradeAreas.add(_tA);
	}
	
	public TradeRegion getTradeRegion(Region r){
		if (this.tradeRegions==null){
			this.tradeRegions = new Hashtable<Region,TradeRegion>();
		}
		if (this.tradeRegions.containsKey(r)){
			return this.tradeRegions.get(r);
		} else {
			TradeRegion nTR = new TradeRegion(r,this.scriptMain.getOverlord());
			this.tradeRegions.put(r, nTR);
			return nTR;
		}
	}
	
	
	private void check4recalcTAs() {
		if (this.needRecalcTradeAreas) {
			recalcTradeAreas_process();
		}
	}
	
	/**
	 * speichert die Anforderung, die TAs neu zu berechnen
	 */
	public void recalcTradeAreas(){
		this.needRecalcTradeAreas=true;
	}
	
	/**
	 * löscht alle TAs und baut aus tradeRegions wieder auf
	 */
	public void recalcTradeAreas_process(){
		if (this.tradeRegions==null){return;}
		if (this.tradeAreas!=null){
			this.tradeAreas.clear();
		}
		//	alle mauellen zuordnen
		for (Iterator<TradeRegion> iter = this.tradeRegions.values().iterator();iter.hasNext();){
			TradeRegion tR = (TradeRegion)iter.next();
			if (tR.isSetAsTradeAreaOrigin()){
				TradeArea tA = this.getTradeArea(tR, true);
				if (tA==null){
					outText.addOutLine("!!! TradeArea not created for TradeRegion (recalc): " + tR.getRegion().toString());
					return;
				}
			}
		}
		// alle automatischen zuordnen
		for (Iterator<TradeRegion> iter = this.tradeRegions.values().iterator();iter.hasNext();){
			TradeRegion tR = (TradeRegion)iter.next();
			if (!tR.isSetAsTradeAreaOrigin()){
				TradeArea tA = this.getTradeArea(tR, true);
				if (tA==null){
					outText.addOutLine("!!! TradeArea not created for TradeRegion (recalc): " + tR.getRegion().toString());
					return;
				}
			}
		}
		
		// traders wieder zuordnen
		if (this.traders!=null) {
			for (Iterator<ScriptUnit> iter = this.traders.keySet().iterator();iter.hasNext();){
				ScriptUnit u = (ScriptUnit)iter.next();
				Region r = u.getUnit().getRegion();
				TradeArea TA = this.getTAinRange(r);
				if (TA!=null){
						Trader T = this.traders.get(u);
						TA.addTrader(T);
				} else {
					outText.addOutLine("!!!Fehler bei recalcTradeAreas: für " + u.unitDesc() + " kein TA gefunden.");
				}		
			}
		}
		
		// int i = 0;
	}
	
	/**
	 * liefert den gewünschten Durchlauf...oder die Durchläufe 
	 * an den Overlord
	 * @return
	 */
	public int[] runAt(){
		return runners;
	}

	/**
	 * @return the tradeAreas
	 */
	public ArrayList<TradeArea> getTradeAreas() {
		return tradeAreas;
	}
	
	
	private void informInterTARelations(){
		outText.setFile("Inter_TA_Relation");
		
		if (this.tradeAreas==null || this.tradeAreas.size()==0){
			outText.addOutLine("no trade areas known");
			return;
		}

		outText.addOutChars("Lux|", 10);
		for (TradeArea tA : this.tradeAreas){
			outText.addOutChars(tA.getName()+"|", 16);
		}
		outText.addNewLine();
		
		outText.addOutChars("Balance|", 10);
		for (int i=1;i<=this.tradeAreas.size();i++){
			outText.addOutChars("normal|", 8);
			outText.addOutChars("max|", 8);
		}
		outText.addNewLine();
		
		for (ItemType itemType : TradeUtils.handelItemTypes()){
			outText.addOutChars(itemType.getName()+"|",10);
			for (TradeArea tA : this.tradeAreas){
				int normalBuy = tA.getAreaBuyAmount(itemType);
				int maxBuy = tA.getAreaBuyMaxAmount(itemType);
				int normalSell = tA.getAreaSellAmount(itemType);
				outText.addOutChars((normalBuy-normalSell)+"|", 8);
				outText.addOutChars((maxBuy-normalSell)+"|", 8);
				// outText.addOutChars("A", 16);
			}
			outText.addNewLine();
		}
		
		
	}
	
	/**
	 * fügt einen neue TAC hinzu, nach Prüfung
	 * @param u1
	 * @param u2
	 * @return Ergebnis (Fehlermeldung) der Prüfung, wenn OK, dann ""
	 */
	public String addTradeAreaConnector(ScriptUnit u1, ScriptUnit u2, String Name, int maxNumberOfMoversIfIdle, int _speed ){
		TradeAreaConnector TAC = new TradeAreaConnector(u1, u2, Name,this);
		if (!TAC.isValid()){
			return "Aktuelle Connection (TAC) ungültig (" + Name + ": " + u1.unitDesc() + "->" + u2.unitDesc()+")";
		}
		if (isKnownTradeAreaConnector(TAC)){
			return "Beziehung dieser TAs bereits vorhanden (" + Name + ": " + u1.unitDesc() + "->" + u2.unitDesc()+")";
		}
		if (_speed<1 || _speed>8){
		  return "Ungültiger Speed (" + Name + ": " + u1.unitDesc() + "->" + u2.unitDesc()+")";
		}
		if (this.tradeAreaConnectors==null){
			this.tradeAreaConnectors = new ArrayList<TradeAreaConnector>();
		}
		TAC.setMaxNumberOfMoversIfIdle(maxNumberOfMoversIfIdle);
		TAC.setSpeed(_speed);
		this.tradeAreaConnectors.add(TAC);
		return "";
	}
	
	/**
	 * Prüft, ob ein TAC schon inhaltlich existiert, also die beiden TAs schion verknüpft sind
	 * @param TAC
	 * @return
	 */
	private boolean isKnownTradeAreaConnector(TradeAreaConnector TAC){
		if (this.tradeAreaConnectors==null || this.tradeAreaConnectors.size()==0 ){
			return false;
		}
		for (TradeAreaConnector actTAC:this.tradeAreaConnectors){
			if ((TAC.getTA1().equals(actTAC.getTA1()) && TAC.getTA2().equals(actTAC.getTA2())) ||(TAC.getTA1().equals(actTAC.getTA2()) && TAC.getTA2().equals(actTAC.getTA1()))){
				return true;
			}
			if (actTAC.getName().equalsIgnoreCase(TAC.getName())){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * bearbeitet die TACs
	 * informiert die SCUs
	 *
	 */
	
	public void run(int durchlauf){
		if (durchlauf==TradeAreaHandler.Durchlauf1){
			this.run1();
		}
		if (durchlauf==TradeAreaHandler.Durchlauf1_Recalc){
			this.check4recalcTAs();
		}
		if (durchlauf==TradeAreaHandler.Durchlauf2){
			this.run2();
		}
		if (durchlauf==TradeAreaHandler.Durchlauf3){
			this.run3();
		}
		if (durchlauf==TradeAreaHandler.Durchlauf4){
			this.runIslandInfo();
		}
	}
	
	
	private void run1(){
		// Löschen aller automatisch angelegten Vorratsscripte
		for (ScriptUnit scu:this.scriptMain.getScriptUnits().values()){
			if (scu.getFoundScriptList()!=null){
				for (Script s:scu.getFoundScriptList()){
					if (s instanceof Vorrat){
						Vorrat v = (Vorrat)s;
						if (v.isTAC_Vorrat()){
							scu.delAScript(v);
							scu.removeScriptOrder("Vorrat source=TAC");
						}
					}
				}
				scu.processScriptDeletions();
			}
		}
		
	}
	
	/**
	 * Die eigentliche Arbeit:
	 * Info über connectierte TAs und deren Balancen
	 * Erster Schritt: relative Aufteilung der Überschüsse + info
	 * Zweiter Schritt: Feinjustierung der Aufteilung + info
	 */
	private void run2(){
		if (this.tradeAreaConnectors==null || this.tradeAreaConnectors.size()==0){
			return;
		}
		String oldFile = outText.getActFileName();
		Boolean oldScreen = outText.isScreenOut();
		if (!this.reportOFF_TAH_TAC){
			outText.setFile("TAH_TAC");
			outText.setScreenOut(false);
		}
		
		for (TradeAreaConnector TAC:this.tradeAreaConnectors){
			this.infoSCU_TAC(1,TAC);
			this.infoSCU_TAC(2,TAC);
		}
		if (!this.reportOFF_TAH_TAC){
			outText.addOutLine("Starte Easy Distri");
		}
		// erste Stufe der Verteilung
		this.process_easy_Distribution();
		
		// info darüber
		for (TradeAreaConnector TAC:this.tradeAreaConnectors){
			this.infoSCU_TAC_2(1,TAC);
			this.infoSCU_TAC_2(2,TAC);
		}
		
		
		// hier eventuell optimierungen
		// und dann weitere info
		
		
		
		// jetzt die Vorräte bestellen
		for (TradeAreaConnector TAC:this.tradeAreaConnectors){
			TAC.process_Transfers();
		}
		
		
		if (!this.reportOFF_TAH_TAC){
			outText.setFile(oldFile);
			outText.setScreenOut(oldScreen);
		}
	}
	
	
	/**
	 * infos
	 */
	private void run3(){
		if (this.tradeAreaConnectors==null || this.tradeAreaConnectors.size()==0){
			return;
		}
		String oldFile = outText.getActFileName();
		Boolean oldScreen = outText.isScreenOut();
		if (!this.reportOFF_TAH_TAC){
			outText.setFile("TAH_TAC");
			outText.setScreenOut(false);
		}
		
		
		// Statistik
		// info darüber
		if (!this.reportOFF_TAH_TAC){
			outText.addOutLine(" ");
			outText.addOutLine("**** Kapa Statistik TACs ***** ");
			outText.addNewLine();
			for (TradeAreaConnector TAC:this.tradeAreaConnectors){
				TAC.informUsShort();
			}
			outText.addOutLine(" ");
		}

		if (!this.reportOFF_TAH_TAC){
			outText.setFile(oldFile);
			outText.setScreenOut(oldScreen);
		}
		
		// MapLines
		// alles bisherige entfernen
		for (TradeAreaConnector TAC:this.tradeAreaConnectors){
			TAC.clear_TAC_MapLines();
		}
		// neu setzen
		for (TradeAreaConnector TAC:this.tradeAreaConnectors){
			TAC.create_TAC_MapLine();
		}
	}
	
	/**
	 * informiert eine Scriptunit über durch sie connectierte TAs
	 * @param number
	 * @param TAC
	 */
	private void infoSCU_TAC(int number, TradeAreaConnector TAC){
		ScriptUnit scu = null;
		TradeArea TA1 = null;
		TradeArea TA2 = null;
		if (number==1){
			scu=TAC.getSU1();
			TA1 = TAC.getTA1();
			TA2 = TAC.getTA2();
		}
		if (number==2){
			scu=TAC.getSU2();
			TA1 = TAC.getTA2();
			TA2 = TAC.getTA1();
		}
		
		scu.addComment("Verbindung zum TA: " + TA2.getName() + "(Name: " + TAC.getName() + ")");
		// die einzelnen Güter durchgehen und Verbindungen und Balancen aufzeigen
		for (ItemType itemType:TradeUtils.handelItemTypes()){
			int actBalance = TA1.getAreaBalance(itemType);
			TA1.setAdjustedBalance(itemType, actBalance);
			String l = itemType.getName() + " (" + actBalance + ")<->:";
			ArrayList<TradeArea> connectedTAs = getConnectedAreas(TA1);
			if (connectedTAs!=null){
				for (TradeArea otherTA:connectedTAs){
					l+=" " + otherTA.getName() + "(" + otherTA.getAreaBalance(itemType) + ")";
				}
			} else {
				l += "keine TAs verbunden";
			}
			scu.addComment(l);
		}
	}
	
	/**
	 * liefert eine Liste connectierter TAs
	 * @param TA
	 * @return
	 */
	private ArrayList<TradeArea> getConnectedAreas(TradeArea TA){
		ArrayList<TradeArea> erg = null;
		if (this.tradeAreaConnectors==null || this.tradeAreaConnectors.size()==0){
			return null;
		}
		for (TradeAreaConnector TAC:this.tradeAreaConnectors){
			if (TAC.getTA1().equals(TA)){
				if (erg==null){
					erg = new ArrayList<TradeArea>();
				}
				erg.add(TAC.getTA2());
			}
			if (TAC.getTA2().equals(TA)){
				if (erg==null){
					erg = new ArrayList<TradeArea>();
				}
				erg.add(TAC.getTA1());
			}
		}
		return erg;
	}
	
	/**
	 * setzt erste Verteilung komplett um
	 */
	private void process_easy_Distribution(){
		if (this.tradeAreaConnectors==null || this.tradeAreaConnectors.size()==0){
			return;
		}
		// RequestListe bauen
		ArrayList<TAC_request> allRequests = new ArrayList<TAC_request>();
		// für alle TAs
		String oldFile = outText.getActFileName();
		Boolean oldScreen = outText.isScreenOut();
		if (!this.reportOFF_TAH_TAC){
			outText.setFile("TAH_TAC_Balances");
			outText.setScreenOut(false);
			outText.addNewLine();
			outText.addOutLine("+++*** TAH - getting Balances ***+++");
		}
		for (TradeArea actTA:this.tradeAreas){
			// für alle ItemTypes
			boolean withInfo=false;
			if (this.hasTAC(actTA)){
				withInfo=true;
			}
			if (this.reportOFF_TAH_TAC){
				withInfo=false;
			}
			if (!this.reportOFF_TAH_TAC && withInfo){
				outText.addOutLine("*** TAH - Balance for " + actTA.getName() + " ***");
				outText.addNewLine();
			}
			for (ItemType itemType:TradeUtils.handelItemTypes()){
				int actBal = actTA.getAreaBalance(itemType,withInfo);
				if (actBal<0){
					// wir haben bedarf...
					TAC_request actR = new TAC_request();
					actR.targetTA = actTA;
					actR.itemType = itemType;
					actR.original_request_amount = Math.abs(actBal);
					actR.act_request_amount = Math.abs(actBal);
					actR.prio = actTA.getAreaWeightedMeanSellPrice(itemType) * actR.original_request_amount;
					allRequests.add(actR);
				}
			}
		}
		
		if (!this.reportOFF_TAH_TAC){
			outText.addNewLine();
			outText.setFile(oldFile);
			outText.setScreenOut(oldScreen);
		}
		
		if (allRequests.size()==0){
			return;
		}
		
		// Sortierung
		Collections.sort(allRequests, new Comparator<TAC_request>() {
			public int compare(TAC_request o1,TAC_request o2){
				return (o2.prio-o1.prio);
			}
		});
		
		// Kurze INfo
		if (!this.reportOFF){
			outText.addOutLine("sortierte TAH Requests:");
			for (TAC_request actR:allRequests){
				outText.addOutLine(actR.toString());
			}
		}
		
		
		// Abarbeitung
		for (TAC_request actR:allRequests){
			this.process_TAC_Request_relative(actR);
		}
	}
	
	
	/**
	 * helper class for sorting and managing TAH-Requests
	 * @author Arndt
	 *
	 */
	private class TAC_request{
		// Ziel des Transfers, Wo was gebraucht wird!
		public TradeArea targetTA = null;  
		public ItemType itemType = null;
		public int original_request_amount = 0;
		public int act_request_amount = 0;
		public int prio = 0;
		
		
		public String toString(){
			return this.original_request_amount + " " + this.itemType.getName() + " nach " + this.targetTA.getName() + "(prio: " + this.prio + ")";
		}
	}
	
	/**
	 * helper class for sorting and managing TAH-Offers
	 * @author Arndt
	 *
	 */
	private class TAC_offer{
		// Herkunft des Transfers, Wo was über ist!
		public TradeArea sourceTA = null;
		// Menge
		public int amount = 0;
		// Entfernung in Karavellenrunden
		// Todo: später auch TransporterRunden?
		public int dist = 0;
		
		public String toString(){
			return "dist: " + dist + " Menge: " + amount + " von: " + sourceTA.getName();
		}
		
	}
	
	/**
	 * Bearbeitet einen bestimmten Request
	 * @param itemType
	 * @param TA
	 */
	private void process_TAC_Request_relative(TAC_request actTAC_Request){
		// habe ich da einen Überschuss?
		int actBalance = actTAC_Request.targetTA.getAdjustedBalance(actTAC_Request.itemType);
		if (actBalance>0){
			// der Request ist gar kein request....
			return;
		}
		// Ja, ich habe einen Bedarf: actBalance
		// Liste der connectierten Spender bauen und sortieren
		if (this.getConnectedAreas(actTAC_Request.targetTA)==null){
			return;
		}
		
		ArrayList <TAC_offer> allOffers = new ArrayList<TAC_offer>(); 
		
		for (TradeArea otherTA:this.getConnectedAreas(actTAC_Request.targetTA)){
			if (otherTA.getAdjustedBalance(actTAC_Request.itemType)>0){
				// Yep Spender gefunden
				TAC_offer actTO = new TAC_offer();
				actTO.sourceTA = otherTA;
				actTO.amount = otherTA.getAdjustedBalance(actTAC_Request.itemType);
				// Dist bestimmen
				// Passenden TAC hoeln
				actTO.dist=100;
				TradeAreaConnector actTAC = this.getTAC(actTAC_Request.targetTA, otherTA);
				if (actTAC!=null){
					// @ToDo: Speed konfigurierbar machen (im TAC)
					int speed = 6;
					int runden = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(actTAC.getSU1().getUnit().getRegion().getCoordinate(), Direction.INVALID ,actTAC.getSU2().getUnit().getRegion().getCoordinate(), this.scriptMain.gd_ScriptMain, speed,null);
					actTO.dist = runden;
				}
				allOffers.add(actTO);
			}
		}
		if (allOffers.size()==0){
			return;
		}
		
		// Sortieren
		Collections.sort(allOffers, new Comparator<TAC_offer>() {
			public int compare(TAC_offer o1,TAC_offer o2){
				if (o1.dist!=o2.dist){
					return o1.dist-o2.dist;
				}
				if (o1.amount!=o2.amount){
					return o2.amount-o1.amount;
				}
				return 0;
			}
		});
		
		if (!this.reportOFF_TAH_TAC){
			outText.addOutLine("**********");
			outText.addOutLine("process_TAC_Request_relative: " + actTAC_Request.toString());
			
			for (TAC_offer actTO:allOffers){
				outText.addOutLine(actTO.toString());
			}
			outText.addOutLine("------------");
			outText.addOutLine("");
		}
		// Abarbeiten
		for (TAC_offer actTO:allOffers){
			process_TAC_offer(actTAC_Request,actTO);
			if (actTAC_Request.act_request_amount<=0){
				break;
			}
		}
	}
	
	
	/**
	 * Setzt einen Transfer um, dabei wird aus der offer versucht, das request zu befriedigen
	 * @param request
	 * @param offer
	 */
	private void process_TAC_offer(TAC_request request, TAC_offer offer){
		int transfer = Math.min(request.act_request_amount, offer.amount);
		
		// Angebot verringern....
		offer.amount-=transfer;
		offer.sourceTA.changeAdjustedBalance(request.itemType, transfer * -1);
		
		// Request befriedigen
		request.act_request_amount-=transfer;
		request.targetTA.changeAdjustedBalance(request.itemType, transfer);
		
		// entsprechenden Transfer dem TAC hinzufügen
		TradeAreaConnector TAC = this.getTAC(offer.sourceTA,request.targetTA);
		TAC.addTransfer(request.targetTA, request.itemType, transfer);
	}
	
	/**
	 * liefert den passenden TAC
	 * @param TA1
	 * @param TA2
	 * @return
	 */
	private TradeAreaConnector getTAC(TradeArea TA1, TradeArea TA2){
		if (this.tradeAreaConnectors==null || this.tradeAreaConnectors.size()==0){
			return null;
		}
		for (TradeAreaConnector actTA:this.tradeAreaConnectors){
			if (actTA.getTA1().equals(TA1) && actTA.getTA2().equals(TA2)){
				return actTA;
			}
			if (actTA.getTA1().equals(TA2) && actTA.getTA2().equals(TA1)){
				return actTA;
			}
		}
		return null;
	}
	
	/**
	 * liefert true, wenn dieses TA einen TAC hat
	 * @param TA
	 * @return
	 */
	private boolean hasTAC(TradeArea TA){
		if (this.tradeAreaConnectors==null || this.tradeAreaConnectors.size()==0){
			return false;
		}
		for (TradeAreaConnector actTA:this.tradeAreaConnectors){
			if (actTA.getTA1().equals(TA)){
				return true;
			}
			if (actTA.getTA2().equals(TA)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Liefert den passenden TAC
	 * @param name
	 * @return
	 */
	public TradeAreaConnector getTAC(String name){
		if (this.tradeAreaConnectors==null || this.tradeAreaConnectors.size()==0){
			return null;
		}
		for (TradeAreaConnector actTA:this.tradeAreaConnectors){
			if (actTA.getName().equalsIgnoreCase(name)){
				return actTA;
			}
		}
		return null;
	}
	
	
	/**
	 * informiert eine Scriptunit über durch sie connectierte TAs
	 * @param number
	 * @param TAC
	 */
	private void infoSCU_TAC_2(int number, TradeAreaConnector TAC){
		ScriptUnit scu = null;
		TradeArea TA1 = null;
		
		if (number==1){
			scu=TAC.getSU1();
			TA1 = TAC.getTA1();
		}
		if (number==2){
			scu=TAC.getSU2();
			TA1 = TAC.getTA2();
		}
		
		// die scu über alle Beziehungen von und zu TA1 informieren
		scu.addComment("Ergebnisse der ersten Verteilung...");
		// die einzelnen Güter durchgehen und Verbindungen und Balancen aufzeigen
		for (ItemType itemType:TradeUtils.handelItemTypes()){
			String l = itemType.getName() + " (" + TA1.getAdjustedBalance(itemType) + ")<->:";
			ArrayList<TradeArea> connectedTAs = getConnectedAreas(TA1);
			if (connectedTAs!=null){
				for (TradeArea otherTA:connectedTAs){
					TradeAreaConnector actTAC = this.getTAC(TA1,otherTA);
					if (actTAC!=null){
						if (actTAC.getTransferAmount(otherTA, itemType, 1)>0){
							l+=" ->" + otherTA.getName() + ":" + actTAC.getTransferAmount(otherTA, itemType, 1);
						}
						if (actTAC.getTransferAmount(TA1, itemType, 1)>0){
							l+=" <-" + otherTA.getName() + ":" + actTAC.getTransferAmount(TA1, itemType, 1);
						}
					} else {
						// ops
						l += " kein TAC zwischen " + TA1.getName() + " und " + otherTA.getName();
					}
				}
			} else {
				l += "keine TAs verbunden";
			}
			
			scu.addComment(l);
		}
		
	}
	
	public GameData getData(){
		return this.scriptMain.gd_ScriptMain;
	}
	
	/**
	 * fügt ein TA zu den islandInfos hinzu
	 * @param s
	 * @param TA
	 */
	public void addIslandInfo(Script s,TradeArea TA){
		if (this.islandInfos.contains(TA)){
			s.addComment("IslandInfo: TA " + TA.getName() + " ist bereits registriert");
		} else {
			this.islandInfos.add(TA);
			s.addComment("IslandInfo: TA " + TA.getName() + " wurde registriert");
		}
	}
	
	/**
	 * erstellt den Ausdruck für die IslandInfos
	 */
	private void runIslandInfo(){
		for (TradeArea TA:this.islandInfos){
			TA.informAreaWideInfos();
		}
		
		// für alle die Transporter und Circus über TA-Status informieren, je 1 Zeile
		if (this.tradeAreas!=null && this.tradeAreas.size()>0) {
			for (TradeArea TA:this.tradeAreas){
				TA.informTransportsTA_Status();
				TA.informCircusTA_Status();
			}
			
			this.createTA_Compare_File();
		}
		
		this.scriptMain.createFactionCompare();
		
		// MonsterAlert
		if (reportSettings.getOptionBoolean("enableMonsterAlert")) {
			// aber nur die, die mit Depots...
			int countAlertMessages = 0;
			if (this.tradeAreas!=null && this.tradeAreas.size()>0) {
				boolean alertOnlyUnattacked = reportSettings.getOptionBoolean("alertOnlyUnattackedMonster"); 
				for (TradeArea TA:this.tradeAreas){
					if (TA.getTransporters()!=null && TA.getTransporters().size()>0) {
						countAlertMessages += TA.monsterAlert(alertOnlyUnattacked);
					}
				}
			}
			if (countAlertMessages>0) {
				outText.addNewLine();
			}
		}
	}
	
	
	public int getReportWeightedMeanSellPrice(ItemType itemType){
		int summe = 0;
		int anzahl = 0;
		if (this.tradeAreas==null || this.tradeAreas.size()==0){
			return 0;
		}
		for (TradeArea TA:this.tradeAreas){
			int actAWMSP = TA.getAreaWeightedMeanSellPrice(itemType);
			if (actAWMSP>0){
				summe+=actAWMSP;
				anzahl+=1;
			}
		}
		if (anzahl>0){
			return (int)Math.floor((double)summe/(double)anzahl);
		}
		return 0;
	}
	
	/**
	 * summarize and compare TAs (Transporter, Entertain...)
	 */
	public void createTA_Compare_File(){
		if (this.tradeAreas==null || this.tradeAreas.size()==0){
			return;
		}
		boolean oldScreenOut = outText.isScreenOut();
		String oldFile = outText.getActFileName();
		outText.setFile("TA_compare");
		outText.setScreenOut(false);
		
		
		
		int proz=0;
		
		// TabHead
		outText.addNewLine();
		outText.addOutChars("TA;",25);
		outText.addOutChars("#Reg;",7);
		outText.addOutChars("#Tra;",7);
		outText.addOutChars("#emp;",7);
		outText.addOutChars("#idl;",7);
		outText.addOutChars("%emp;",7);
		outText.addOutChars("%idl;",7);
		outText.addOutChars("%Ver;",7);
		
		
		for (TradeArea TA:this.tradeAreas){
			if (TA.getTransporters()!=null){
				outText.addNewLine();
				outText.addOutChars(TA.getName() + ";",25);
				// Anzahl Regionen
				outText.addOutChars(TA.anzahlRegionen()+";", 7);
				
				// Anzahl Transporter
				outText.addOutChars(TA.getTransporters().size() + ";", 7);
				// Anzahl leer
				outText.addOutChars(TA.anzahlEmptyTransports() + ";", 7);
				// Anzahl Idle
				outText.addOutChars(TA.anzahlIdleTransports() + ";", 7);
				// % leer
				proz=0;
				if (TA.getTransporters().size()>0){
					proz = (int)Math.round(((double)TA.anzahlEmptyTransports() / (double)TA.getTransporters().size())*100);
				}
				outText.addOutChars(proz+"%;", 7);
				// % idle
				proz=0;
				if (TA.getTransporters().size()>0){
					proz = (int)Math.round(((double)TA.anzahlIdleTransports() / (double)TA.getTransporters().size())*100);
				}
				outText.addOutChars(proz+"%;", 7);
				
				
				// Versorgung mit Unterhaltern
				double calcAreaAvailCircus = TA.calcAreaAvailCircus();
				double calcAreaCircus = TA.calcAreaCircus();
				proz=0;
				if (calcAreaAvailCircus>0){
					proz=(int)Math.round((calcAreaCircus / calcAreaAvailCircus)*100);
				}
				outText.addOutChars(proz+"%;", 7);
			}
		}
		outText.addNewLine();
		outText.addOutLine("E O F");
		outText.setFile(oldFile);
		outText.setScreenOut(oldScreenOut);
	}


	public void setDebugOutput(boolean debugOutput) {
		this.debugOutput = debugOutput;
	}
	
	
}
