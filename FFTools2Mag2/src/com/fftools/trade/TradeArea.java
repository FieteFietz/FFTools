package com.fftools.trade;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.rules.ItemType;
import magellan.library.utils.Utils;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.overlord.Overlord;
import com.fftools.pools.bau.TradeAreaBauManager;
import com.fftools.pools.circus.CircusPool;
import com.fftools.pools.circus.CircusPoolManager;
import com.fftools.pools.circus.CircusPoolRelation;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.MatPoolManager;
import com.fftools.pools.treiber.TreiberPool;
import com.fftools.pools.treiber.TreiberPoolManager;
import com.fftools.scripts.Vorrat;
import com.fftools.transport.TransportRequest;
import com.fftools.transport.Transporter;
import com.fftools.utils.FFToolsArrayList;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.PriorityUser;



/**
 * Stores data about an TradeArea (normal case: an island)
 * 
 * @author Fiete
 *
 */

public class TradeArea {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final ReportSettings reportSettings = ReportSettings.getInstance();

	/** 
	 * Spezialfall, wenn Gut TA-weit nur gekauft werden kann, nicht verkauft....
	 */
	private final int maxXfachÜberkauf = 3;
	
	/**
	 * Wieviele Runde aktueller Theo-Einkauf bei depot.Items,
	 * damit Lager als voll angesehen wird und nicht weiter über-
	 * kauft wird?
	 * 
	 */
	private final int maxRundeEinkaufAufLager = 3;
	
	/**
	 * all Regions in this area
	 */
	private LinkedList<TradeRegion> tradeRegions = null;
	
	
	/**
	 * the "center" region or first region....
	 * other regions may join if they have a land path connection
	 * to originRegion or explizit wish to join this Trade Area
	 */
	private TradeRegion originRegion = null;
	
	/**
	 * name of TradeArea....set by scripter Option or by originRegion
	 */
	private String name = "N.N.";
	
	/**
	 * Liste der trader in diesem TradeArea
	 */
	private ArrayList<Trader> traders = null;
	
	/**
	 * Liste der transporter in diesem TradeArea
	 */
	private ArrayList<Transporter> transporters = null;
	
	
	private ArrayList<TransportRequest> transportRequests = null;
	
	/**
	 * die während der Optimierung angepasste Balance
	 */
	private HashMap<ItemType,Integer> adjustedBalance = null;
	
	
	/**
	 * Zur Veraltung der Bauscripte in diesem TA
	 */
	private TradeAreaBauManager tradeAreaBauManager = null;
	
	
	public int getAdjustedBalance(ItemType itemType) {
		if (this.adjustedBalance==null){
			return 0;
		}
		Integer I = this.adjustedBalance.get(itemType);
		if (I==null){
			return 0;
		}
		return I.intValue();
	}

	public void setAdjustedBalance(ItemType itemType,int amount) {
		if (this.adjustedBalance==null){
			this.adjustedBalance=new HashMap<ItemType, Integer>();
		}
		this.adjustedBalance.put(itemType, Integer.valueOf(amount));
	}

	
	public void changeAdjustedBalance(ItemType itemType,int change){
		int old = this.getAdjustedBalance(itemType);
		old += change;
		this.setAdjustedBalance(itemType, old);
	}

	/**
	 * eine Liste aller Vorrat - scripts, die beachtet werden müssen
	 */
	private ArrayList<Vorrat> vorratRequests = null;
	
	/**
	 * eine Liste *aller* requests, auch die nicht beachtet werden müssen
	 * (wird gebraucht, um TradeAreaBalance auszurechnen...mit allen Vorräten
	 */
	private ArrayList<Vorrat> vorratRequestsAll = null;
	
	private Overlord overlord = null;
	
	/**
	 * new Trade Area with param as orgin Region
	 * @param _originRegion
	 */
	public TradeArea(TradeRegion _originRegion, Overlord overlord){
		this.overlord = overlord;
		this.init(_originRegion);
	}

	/**
	 * initializes the TradeArea according to a given Region
	 * sets originRegion and name
	 * @param _originRegion A Region
	 */
	private void init(TradeRegion _originRegion){
		this.tradeRegions = new LinkedList<TradeRegion>();
		this.tradeRegions.add(_originRegion);
		this.name = _originRegion.getRegion().getName();
		this.originRegion = _originRegion;
	}
	
	/**
	 * adds the given Region to the tradearea
	 * if this action is valid must be checked before...
	 * if list if regions is empty, the given unit is set
	 * as the originRegion and name if TradeArea is set (init)
	 * 
	 * @param o the Region to add
	 */
	public void addRegion(TradeRegion o){
		if (this.tradeRegions==null){
			this.init(o);
		} else {
			if (!this.tradeRegions.contains(o)){
				this.tradeRegions.add(o);
			}
		}
	}
	
	
	/**
	 * @return the originRegion
	 */
	public Region getOriginRegion() {
		return originRegion.getRegion();
	}

	/**
	 * @param originRegion the originRegion to set
	 */
	public void setOriginRegion(TradeRegion originRegion) {
		this.originRegion = originRegion;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		if (name!=null){
			this.name = name;
		} else {
			this.name="N.N. (nulled)";
		}
	}

	/**
	 * @return the regions
	 */
	public LinkedList<TradeRegion> getTradeRegions() {
		return tradeRegions;
	}
	
	/**
	 * returns an iterator over all regions
	 * or null, if no regions are present
	 * @return iterator over all regions
	 */
	public Iterator<TradeRegion> getRegionIterator(){
		if (this.tradeRegions==null) {
			return null;
		}
		return this.tradeRegions.iterator();
	}
	
	/**
	 * returns true, if the given TradeRegion is part of this TradeArea
	 * otherwise false
	 * @param o the TradeRegion to check
	 * @return true or false
	 */
	public boolean contains(TradeRegion o){
		if (this.tradeRegions==null) {return false;}
		return this.tradeRegions.contains(o);
	}
	
	
	/**
	 * returns true, if the given region is part of this TradeArea
	 * otherwise false
	 * @param o the Region to check
	 * @return true or false
	 */
	public boolean contains(Region r){
		if (this.tradeRegions==null) {return false;}
		for (TradeRegion TR : this.tradeRegions){
			if (TR.getRegion().equals(r)){
				return true;
			}
		}
		return false;
	}
	
	
	public void informUs(){
		// if (outText.getTxtOut()==null) {return;}
		if (this.tradeRegions!=null && this.tradeRegions.size()>5){
			outText.setFile("TradeArea_" + this.getName());
		} else {
			return;
		}
		if (this.name==null) {
			outText.addOutLine("******Handelsgebiets-Info******(ohne Namen(!))");
		} else {
			outText.addOutLine("******Handelsgebiets-Info******" + this.name);
		}
		if (this.tradeRegions!=null){
			outText.addOutLine(this.tradeRegions.size() + " Regionen bekannt");
		} else {
			outText.addOutLine("Keine Regionen bekannt");
			outText.setFile("TradeAreas");
			return;
		}
		
		if (this.originRegion!=null){
			outText.addOutLine("Referenzregion: " + this.originRegion.getRegion().toString());
		}
		
		outText.addOutLine("vorhandene Regionsinformationen:");
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion r = (TradeRegion)iter.next();
			reportRegion(r);
		}
		
		
		this.informAreaWideInfos();
		
		outText.addOutLine("***Ende Handelsgebiets-Info***");
		outText.setFile("TradeAreas");
	}
	
	
	public ArrayList<Region> getBurgenbauRegionen(){
		ArrayList<Region> burgenRegionen = new ArrayList<Region>();
		if (this.tradeRegions!=null && this.tradeRegions.size()>0){
			for (TradeRegion TR:this.tradeRegions){
				Region r = TR.getRegion();
				if (FFToolsRegions.getValueOfBuiltStones(r)>0){
					burgenRegionen.add(r);
				}
			}
			if (burgenRegionen.size()>0){
				// Sortieren
				Collections.sort(burgenRegionen, new Comparator<Region>(){
					public int compare(Region o1,Region o2){
						double d1 = FFToolsRegions.getValueOfBuiltStones(o1);
						double d2 = FFToolsRegions.getValueOfBuiltStones(o2);
						if (d2>d1){
							return 1;
						}
						if (d1>d2){
							return -1;
						}
						return 0;
					}
				});
			}
		}
		
		return burgenRegionen;
	}
	
	
	public void informAreaWideInfos(){
		
		String oldFile = outText.getActFileName();
		Boolean oldScreen = outText.isScreenOut();
		
		outText.setFile("TradeAreaSummary_" + this.name);
		outText.setScreenOut(false);
		
		
		
		outText.addOutLine("************************************");
		outText.addOutLine("Regionen ohne Depots in " + this.name);
		outText.addOutLine("************************************");
		
		if (this.tradeRegions!=null && this.tradeRegions.size()>0){
			for (TradeRegion TR:this.tradeRegions){
				MatPool MP = this.overlord.getMatPoolManager().getRegionsMatPool(TR.getRegion());
				if (MP!=null){
					if (MP.getDepotUnit()==null){
						outText.addOutLine("!Kein Depot in " + TR.getRegion().toString(),true);
					}
				} else {
					outText.addOutLine("!!Kein Matpool in " + TR.getRegion().toString());
				}
			}
		} else {
			outText.addOutLine("!!!Keine TradeRegions ???");
		}
		
		outText.addOutLine("************************************");
		outText.addOutLine("Burgenstatus in " + this.name);
		outText.addOutLine("************************************");
		outText.addNewLine();
		ArrayList<Region> burgenRegionen = getBurgenbauRegionen();
		
		NumberFormat NF = NumberFormat.getInstance();
		NF.setMaximumFractionDigits(1);
		NF.setMinimumFractionDigits(1);
		NF.setMinimumIntegerDigits(1);
		NF.setMaximumIntegerDigits(5);
		
		
		if (burgenRegionen.size()>0){
			// Ausgaben
			for (Region r:burgenRegionen){
				outText.addOutChars(r.toString(), 30);
				outText.addOutChars(" v: ");
				outText.addOutChars("" + NF.format(FFToolsRegions.getValueOfBuiltStones(r)), 6);
				outText.addOutChars(" act: ");
				outText.addOutChars("" + (FFToolsRegions.getBiggestCastle(r)==null ? 0 : FFToolsRegions.getBiggestCastle(r).getSize()), 7);
				outText.addOutChars(" stones: ");
				// aktuelle Steine hier
				NF.setMinimumFractionDigits(0);
				outText.addOutChars("" + FFToolsRegions.getNumberOfItemsInRegion(r, this.overlord.getScriptMain().gd_ScriptMain.getRules().getItemType("Stein"), this.overlord.getScriptMain()), 5);
				outText.addOutChars(" talents:");
				outText.addOutChars("" + FFToolsRegions.getNumberOfTalentInRegion(r, this.overlord.getScriptMain().gd_ScriptMain.getRules().getSkillType("Burgenbau"), this.overlord.getScriptMain()), 6);
				
				outText.addNewLine();
			}
			
		} else {
			outText.addOutLine("!!!Keine TradeRegions ???");
		}
		
		outText.addOutLine("************************************");
		outText.addOutLine("Verdienststatus in " + this.name);
		outText.addOutLine("************************************");
		outText.addNewLine();
		double calcAreaAvailCircus = this.calcAreaAvailCircus();
		double calcAreaCircus = this.calcAreaCircus();
		outText.addOutLine("Maximal zu verdienen (Regionsunterhalt - Treiber): " + calcAreaAvailCircus);
		outText.addOutLine("Summe aller Script-Verdienste durch Unterhalter: " + calcAreaCircus);
		if (calcAreaAvailCircus>0){
			outText.addOutLine("Versorgung: " + (int)Math.round((calcAreaCircus / calcAreaAvailCircus)*100) + "%");
		}
		
		outText.setFile(oldFile);
		outText.setScreenOut(oldScreen);
	}
	
	/**
	 * informiert alle Transporter über die Auslastung des TA-Netzwerkes
	 */
	public void informTransportsTA_Status(){
		if (this.transporters==null || this.transporters.size()==0){
			return;
		}
		String s = "TA-Status: " + this.transporters.size() + " Transporter, " + this.anzahlEmptyTransports() + " leer, " + this.anzahlIdleTransports() + " ohne Auftrag (" + (int)Math.floor(((double)this.anzahlIdleTransports() / (double)this.transporters.size())*100) + "%)";
		for (Transporter t:this.transporters){
			t.getScriptUnit().addComment(s);
		}
	}
	
	/**
	 * informiert alle unterhalter über den Status des TA
	 */
	public void informCircusTA_Status(){
		if (this.tradeRegions==null || this.tradeRegions.size()==0){
			return;
		}
		
		double AreaAvailCircus = this.calcAreaAvailCircus();
		double AreaCircus = this.calcAreaCircus();
		String s = "TA-Status: Versorgung bei " + (int)Math.floor((AreaCircus/AreaAvailCircus)*100) + "% (Bilanz " + (AreaCircus - AreaAvailCircus) + ")";
		
		double AreaSurplusCircus = this.calcAreaSurplusCircus();
		String s2 = "TA-Status: Langzeit-Versorgung bei " + (int)Math.floor((AreaCircus/AreaSurplusCircus)*100) + "% (Bilanz " + (AreaCircus - AreaSurplusCircus) + ")";
		
		
		CircusPoolManager CPM = this.overlord.getCircusPoolManager();
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion tr = (TradeRegion)iter.next();
			Region r = tr.getRegion();
			CircusPool CP = CPM.getCircusPool(r);
			if (CP!=null){
				for (CircusPoolRelation cpr:CP.getListOfRelations()){
					cpr.getSkriptUnit().addComment(s);
					cpr.getSkriptUnit().addComment(s2);
				}
			}
		}
	}
	
	
	public void informUsTradeTransportRequests(Overlord OL){
		// if (outText.getTxtOut()==null) {return;}
		if (this.name==null) {
			outText.addOutLine("******Handelsgebiets-Info******(ohne Namen(!))");
		} else {
			outText.addOutLine("******Handelsgebiets-Info******" + this.name);
		}
		if (this.tradeRegions!=null){
			outText.addOutLine(this.tradeRegions.size() + " Regionen bekannt");
		} else {
			outText.addOutLine("Keine Regionen bekannt");
			return;
		}
		
		
		outText.addOutLine("vorhandene TransportRequests (alle!):");
		ArrayList<TransportRequest> actRequests = this.getTransportRequests(OL);
		if (actRequests==null){
			outText.addOutLine("keine Requests bekannt");
			return;
		}
		for (Iterator<TransportRequest> iter = actRequests.iterator();iter.hasNext();){
			TransportRequest TR = (TransportRequest)iter.next();
			String s = "";
			s += TR.getForderung() + "(" + TR.getOriginalGefordert() + ") ";
			s += TR.getOriginalGegenstand();
			s += " nach " + TR.getRegion().toString();
			s += " PRIO:" + TR.getPrio();
			outText.addOutLine(s);
		}
		
		
		outText.addOutLine("***Ende Handelsgebiets-Info zu TransporterRequests***");
	}
	
	
	private void reportRegion(TradeRegion r){
		outText.addOutLine("...." +  r.getRegion().toString());
		if (r.isSetAsTradeAreaOrigin()){
			outText.addOutLine(".........TradeArea gesetzt auf:" + r.getTradeAreaName());
		}
		LinkedList<String> info = this.getTradeAreaUnitInfo(r);
		if (info!=null){
			for (int i = info.size()-1;i>=0;i--){
				String s = info.get(i);
				outText.addOutLine("......" + s);
			}
		}
	}
	
	/**
	 * entfernt alle Regionen, die nicht manuell ihren
	 * TA gesetzt bekommen haben
	 * (die entfernten werden durch TAH wieder neu hinzugefügt...)
	 */
	public void removeNonManualOrigins(){
		if (this.tradeRegions==null){return;}
		LinkedList<TradeRegion> newList = null;
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion tR = (TradeRegion)iter.next();
			if (tR.isSetAsTradeAreaOrigin()){
				if (newList == null){
					newList = new LinkedList<TradeRegion>();
				}
				newList.add(tR);
			}
		}
		this.tradeRegions = newList;
	}
	
	
	
	
	
	/**
	 * liefert alle regionen, in denen itemType gekauft werden könnte
	 * @param itemType
	 * @return
	 */
	private ArrayList<TradeRegion> getBuyers(ItemType itemType){
		ArrayList<TradeRegion> buyers = new ArrayList<TradeRegion>();
		
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion r = (TradeRegion)iter.next();
			if (r.getRegion().maxLuxuries()>0 && r.hasTrader()){
				if (r.getBuyItemType()==null){
					return null;
				}
				if (!r.getBuyItemType().equals(itemType)){
					// hier kann verkauft weren
					// sellers.add(r);
				} else {
					// hier kann gekauft werden
					buyers.add(r);
				}
			}
		}
		return buyers;
	}
	
	
	/**
	 * liefert alle TradeRegionen, in denen itemType verkauft werden könnte
	 * @param itemType
	 * @return
	 */
	private ArrayList<TradeRegion> getSellers(ItemType itemType){
		ArrayList<TradeRegion> sellers = new ArrayList<TradeRegion>();
		
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion r = (TradeRegion)iter.next();
			if (r.getRegion().maxLuxuries()>0 && r.hasTrader()){
				if (r.getBuyItemType()==null){
					return null;
				}
				if (!r.getBuyItemType().equals(itemType)){
					// hier kann verkauft weren
					sellers.add(r);
				} else {
					// hier kann gekauft werden
					// buyers.add(r);
				}
			}
		}
		return sellers;
	}
	
	
	/**
	 * produces some lines for info about TA in comments of unit
	 * @param tR
	 * @return
	 */
	public LinkedList<String> getTradeAreaUnitInfo(TradeRegion tR){
		LinkedList<String> erg = new LinkedList<String>();
		if (!contains(tR)){
			erg.add("!wrong TradeArea!");
			return erg;
		}
		erg.add("Handelsgebiet " + this.getName() + " mit " + this.tradeRegions.size() + " Regionen.");
		ItemType buyItemType = tR.getBuyItemType();
		if (buyItemType==null){
			erg.addFirst("!cannot retrieve ItemType to buy!");
			return erg;
		}
		// wo kann überall das Gut verkauft werden?
		// und wo gekauft
		ArrayList<TradeRegion> sellers = this.getSellers(buyItemType);
		ArrayList<TradeRegion> buyers = this.getBuyers(buyItemType);

		// ausgabe
		String s = "";
		
		int extraVorrat = 0;
		if (this.vorratRequests!=null && this.vorratRequests.size()>0){
			for (Iterator<Vorrat>iter = this.vorratRequests.iterator();iter.hasNext();){
				Vorrat vorratScript = (Vorrat)iter.next();
				if (vorratScript.getItemType()!=null && vorratScript.getItemType().equals(buyItemType)){
					if (s.length()>0){
						s+=",";
					}
					s+= vorratScript.scriptUnit.unitDesc() + "(" + vorratScript.getProRunde() + ")";
					extraVorrat += vorratScript.getProRunde();
				}
			}
			if (extraVorrat>0){
				erg.addFirst("Vorrat " + extraVorrat + ":" + s);
			}
		}
		
		
		int amount_sellers = 0;
		int amount_buyers = 0;
		if (sellers.size()>0){
			TradeRegionComparatorSell tRC = new TradeRegionComparatorSell(buyItemType);
			Collections.sort(sellers, tRC);
			s="";
			for (Iterator<TradeRegion> iter = sellers.iterator();iter.hasNext();){
				TradeRegion myTR = (TradeRegion)iter.next();
				Region r = myTR.getRegion();
				if (s.length()>0){s+=",";}
				s += r.getName() + "(" + r.maxLuxuries() + "*" + myTR.getSellPrice(buyItemType) + "=" + (r.maxLuxuries()*myTR.getSellPrice(buyItemType)) + ")";
				amount_sellers += r.maxLuxuries();
			}
			erg.addFirst("Verkauf " + amount_sellers + ":" + s);
		}
		if (buyers.size()>0){
			TradeRegionComparatorBuy tRC = new TradeRegionComparatorBuy();
			Collections.sort(buyers, tRC);
			s="";
			for (Iterator<TradeRegion> iter = buyers.iterator();iter.hasNext();){
				TradeRegion myTR = (TradeRegion)iter.next();
				Region r = myTR.getRegion();
				if (s.length()>0){s+=",";}
				s += r.getName() + "(" + r.maxLuxuries() + ")";
				amount_buyers += r.maxLuxuries();
			}
			erg.addFirst("Kauf " + amount_buyers + ":" + s);
		}
		
		
		// Einfügen: Lagerbestand:
		int suggestedLagerBetsand = this.suggestedAreaStorage(buyItemType, tR);
		int Ta_VorratsFaktor = reportSettings.getOptionInt("ta-vorratsfaktor", tR.getRegion());
		if (Ta_VorratsFaktor<0){
			Ta_VorratsFaktor=0;
		}
		erg.addFirst("als Lagerbestand berücksichtigt: " + suggestedLagerBetsand + " (Vorratsfaktor=" + Ta_VorratsFaktor + "%)");
		int lagerBestand =  this.getAreaStorageAmount(buyItemType);
		erg.addFirst("auf Lager " + lagerBestand + ":" + this.areaStorageAmountInfo);
		
		
		int kaufTheo = this.suggestedBuyAmount(tR,true);
		erg.addFirst("Vorgeschlagen: " + kaufTheo + "(max:" + this.calcMaxAvailableAmount(tR, buyItemType) + ") " + buyItemType.getName());
		if (this.isLagerVoll(tR, this.suggestedBuyAmount(tR))){
			erg.addFirst("Lager voll! (Faktor:" + maxRundeEinkaufAufLager + ", theoMenge:" + this.suggestedBuyAmount(tR) + ")");
		}
		return erg;
	}
	
	/**
	 * macht einen Vorschlag zum Kauf...
	 * könnte mal von einem handelsmanager genutzt bzw
	 * überstimmt werden
	 * @param r
	 * @return
	 */
	public int suggestedBuyAmount(TradeRegion r){
		return suggestedBuyAmount(r, false);
	}
	
	/**
	 * macht einen Vorschlag zum Kauf...
	 * könnte mal von einem handelsmanager genutzt bzw
	 * überstimmt werden
	 * @param r
	 * @return
	 */
	public int suggestedBuyAmount(TradeRegion r, boolean withLager){
		if (!this.contains(r)){
			return 0;
		}
		
	
		
		ItemType itemType = r.getBuyItemType();
		int gesamtVerkauf = this.getAreaSellAmount(itemType);
		int gesamtEinkauf = this.getAreaBuyAmount(itemType);
		
		// Extras..z.B. Vorräte
		int vorräte = this.getAreaVorratProRundeAmount(itemType);
		
		
		// Einschub...was ist, wenn wir nirgends verkaufen können
		// dann scheitert die berechnung der theoretischen menge
		if (gesamtVerkauf==0 && vorräte>0 && this.maxXfachÜberkauf>10){
			// Sonderfall: reiner Vorratskauf
			// soll heissen: eine insel mit NUR weihrauch (Mea, 10. Welt)
			double relativeSollEinkauf = (double)vorräte/(double)gesamtEinkauf;
			// das bedeutet für diese Region eine relativen Anteil
			double actRelativeEinkaufD = (double)r.getRegion().maxLuxuries() * relativeSollEinkauf;
			int actRelativeEinkauf = (int)Math.ceil(actRelativeEinkaufD);
			// checken, ob grösser als einkaufsmenge
			// deckeln beu X-fachem überkauf...
			if (actRelativeEinkauf>r.getRegion().maxLuxuries()*this.maxXfachÜberkauf){
				actRelativeEinkauf = r.getRegion().maxLuxuries() * this.maxXfachÜberkauf;
			}
			if (isLagerVoll(r, actRelativeEinkauf)){
				actRelativeEinkauf=r.getRegion().maxLuxuries();
			}
			return actRelativeEinkauf;
		}
		
		if (gesamtVerkauf==0 && vorräte==0){
			// Sonderfall: wir können hier nicht verkaufen und brauchen auch keine Vorräte
			// dann tatsächlich nicht einkaufen..wird mit -1 signalisiert.
			return -1;
		}
		
		
		gesamtVerkauf += vorräte;
		
		// Einschub: auf Lager:
		// gesamtEinkauf += this.getAreaStorageAmount(itemType);
		// Einschub 2: nicht alle Vorräte ansätzen: VorratsFaktor beachten
		// TA-Vorratsfaktor: ab welchem Anteil des Gesamteinkaufswertes 
		// gelten Lagerbestände als Vorräte? Angabe in Prozent
		
		gesamtEinkauf += suggestedAreaStorage(itemType,r);
		
		// theoretische menge berechnen
		double kaufTheoD = (double)r.getRegion().maxLuxuries()*(double)gesamtVerkauf / gesamtEinkauf;
		int kaufTheo = (int)Math.ceil(kaufTheoD);
		
		// Hier einschieben berechnen der Lagerstände bei den Depots
		// und entsprechend Verringerung von gesamtEinkauf und Neuberechnung
		// Lagerbestand = ALLES - (kaufTheo * K Runden)
		// K Optional: Beispiel: 3 (3 Einkaufsrunden auf Lager)
		// Ausgabe bei "Auf Lager:" als Comment
		// Vereinfachung: nur Betrachtung der aktuellen Region
		
		
		
		if (kaufTheo>r.getRegion().maxLuxuries()){
			// Überkaufen?
			
			int maxMengeProfit = this.calcMaxAvailableAmount( r, itemType);
			
			// einfaches klassisches deckeln des einkaufes
			if (kaufTheo>maxMengeProfit){kaufTheo = maxMengeProfit;}
			if (withLager && isLagerVoll(r, kaufTheo)){
				kaufTheo=r.getRegion().maxLuxuries();
			}
			return kaufTheo;
			
		} else {
			// was bei weniger ???
			// vorerst: trotzdem alles zum billigsten preis kauf
			// aber das nicht hier entscheiden..hier wird nur vorgeschlagen...
			return kaufTheo;
		}
		
	}
	
	public int suggestedAreaStorage(ItemType itemType, TradeRegion r){
		int areaStorage = this.getAreaStorageAmount(itemType);
		int TA_Vorrat = reportSettings.getOptionInt("ta-vorratsfaktor", r.getRegion());
		if (TA_Vorrat<0){
			TA_Vorrat=0;
		}
		int gesamtVerkauf = this.getAreaSellAmount(itemType);
		int sockel = (int)Math.ceil((double)gesamtVerkauf * ((double)TA_Vorrat/100));
		areaStorage-=sockel;
		areaStorage = Math.max(0, areaStorage);
		return areaStorage;
	}
	
	
	private int calcMaxAvailableAmount(TradeRegion r, ItemType itemType){
        // checken, ob es sich rechnet...dazu brauchen wir einen maximalen
		// Einkaufspreis...der ergibt sich aus
		// minimalen Verkaufspreis + Aufschlag
		// oder durchschnittlichen Verkaufspreis? (später...)
		// Aufschlag muss parameterisierbar gemacht werden
		// wir gehen mal von mind. doppeltem Verkaufspreis aus
		double profit = 2;
		// double maxEinkaufspreisD = (double)this.getAreaMinSellPrice(itemType) / profit;
		double maxEinkaufspreisD = (double)this.getAreaWeightedMeanSellPrice(itemType) / profit;
		if (maxEinkaufspreisD==0){
			// kein Verkauf im TA...wir nehmen den reportweiten
			maxEinkaufspreisD = (double)this.overlord.getTradeAreaHandler().getReportWeightedMeanSellPrice(itemType) / profit;
		}
		int maxEinkaufspreis = (int)Math.ceil(maxEinkaufspreisD);
		return this.calcMaxAvailableAmount(maxEinkaufspreis, r, itemType);
	}
	
	private int calcMaxAvailableAmount(int maxDurchschnittsPreis, TradeRegion r, ItemType itemType){
		if (maxDurchschnittsPreis==0){return 0;}
		if (r.getRegion().maxLuxuries()==0){return 0;}
		int menge = 1;
		int preis = TradeUtils.getPrice(menge, r.getSellPrice(itemType), r.getRegion().maxLuxuries());
		double dPreis = (double)preis/(double)menge;
		while(dPreis<=maxDurchschnittsPreis){
			menge+=1;
			preis = TradeUtils.getPrice(menge, r.getSellPrice(itemType), r.getRegion().maxLuxuries());
			dPreis = (double)preis/(double)menge;
		}
		return menge;
	}
	
	/**
	 * liefert summe aller Verkaufsmglk
	 * eines ItemTypes im Area
	 * @param itemType
	 * @return
	 */
	public int getAreaSellAmount(ItemType itemType){
		if (this.tradeRegions==null){return 0;}
		int erg = 0;
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion r = (TradeRegion)iter.next();
			if (r.getRegion().maxLuxuries()>0 && r.hasTrader()){
				if (r.getBuyItemType()!=null){
					if (!r.getBuyItemType().equals(itemType)){
						erg+=r.getRegion().maxLuxuries();
					}
				}
			}
		}
		return erg;
	}
	
	
	
	/**
	 * liefert Summe aller pro Runde zu berücksichtigender
	 * Abgaben zum Vorratsaufbau
	 * @param itemType
	 * @return
	 */
	public int getAreaVorratProRundeAmount(ItemType itemType){
		int erg = 0;
		if (this.vorratRequests==null || this.vorratRequests.size()==0){
			return erg;
		}
		for (Iterator<Vorrat> iter = this.vorratRequests.iterator();iter.hasNext();){
			Vorrat vorratScript = (Vorrat)iter.next();
			if (vorratScript.getItemType()!=null && vorratScript.getItemType().equals(itemType)){
				erg+=vorratScript.getProRunde();
			}
		}
		return erg;
	}
	
	/**
	 * liefert Summe aller pro Runde zu berücksichtigender
	 * Abgaben zum Vorratsaufbau
	 * @param itemType
	 * @return
	 */
	public int getAreaVorratProRundeAmountAll(ItemType itemType){
		int erg = 0;
		if (this.vorratRequestsAll==null || this.vorratRequestsAll.size()==0){
			return erg;
		}
		for (Iterator<Vorrat> iter = this.vorratRequestsAll.iterator();iter.hasNext();){
			Vorrat vorratScript = (Vorrat)iter.next();
			if (vorratScript.getItemType()!=null && vorratScript.getItemType().equals(itemType)){
				erg+=vorratScript.getProRunde();
			}
		}
		return erg;
	}
	
	
	
	
	/**
	 * liefert summe aller Einkaufsmglk
	 * eines ItemTypes im Area
	 * @param itemType
	 * @return
	 */
	public int getAreaBuyAmount(ItemType itemType){
		if (this.tradeRegions==null){return 0;}
		int erg = 0;
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion r = (TradeRegion)iter.next();
			if (r.getRegion().maxLuxuries()>0 && r.hasTrader()){
				if (r.getBuyItemType()!=null){
					if (r.getBuyItemType().equals(itemType)){
						erg+=r.getRegion().maxLuxuries();
					}
				}
			}
		}
		return erg;
	}
	
	
	private String areaStorageAmountInfo = "";
	
	/**
	 * liefert summe aller Depotbestände
	 * eines ItemTypes (Luxusgut) im Area, wenn das ItemType dort zu kaufen
	 * ist oder mehr als vorratsrunden vorhanden ist
	 * @param itemType
	 * @return
	 */
	public int getAreaStorageAmount(ItemType itemType){
		areaStorageAmountInfo = "";
		if (this.tradeRegions==null){return 0;}
		int erg = 0;
		
		for (TradeRegion r:this.tradeRegions){
			int actErg = r.getStorageAmount(itemType);
			if (actErg>0){
				erg +=actErg;
				if (areaStorageAmountInfo.length()>1){
					areaStorageAmountInfo+=",";
				}
				areaStorageAmountInfo+=r.getRegion().getName() + "(" + r.getStorageAmountInfo() + ")" ;
			}
		}
		// Umrechnung des absoluten bestandes in eine
		// Menge Nutzbar pro Runde...
		// aber eigentloch ist alles nutzbar...hm.
		
		return erg;
	}
	
	
	
	
	
	/**
	 * liefert summe aller maximalen Einkaufsmglk
	 * eines ItemTypes im Area
	 * @param itemType
	 * @return
	 */
	public int getAreaBuyMaxAmount(ItemType itemType){
		if (this.tradeRegions==null){return 0;}
		int erg = 0;
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion r = (TradeRegion)iter.next();
			if (r.getRegion().maxLuxuries()>0 && r.hasTrader()){
				if (r.getBuyItemType()!=null){
					if (r.getBuyItemType().equals(itemType)){
						// erg+=r.getRegion().maxLuxuries();
						erg+=calcMaxAvailableAmount(r, itemType);
					}
				}
			}
		}
		return erg;
	}
	
	/**
	 * liefert minimalen Verkaufspreis im Area
	 * eines ItemTypes im Area
	 * @param itemType
	 * @return
	 */
	public int getAreaMinSellPrice(ItemType itemType){
		if (this.tradeRegions==null){return 0;}
		int erg = Integer.MAX_VALUE;
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion r = (TradeRegion)iter.next();
			if (r.getRegion().maxLuxuries()>0 && r.hasTrader()){
				if (r.getBuyItemType()!=null){
					if (!r.getBuyItemType().equals(itemType)){
						int actP = r.getSellPrice(itemType);
						if (actP>0 && actP<erg){
							erg = actP;
						}
					}
				}
			}
		}
		if (erg == Integer.MAX_VALUE){erg=0;}
		return erg;
	}
	
	/**
	 * liefert gewichteten durchschnittlichen Verkaufspreis im Area
	 * eines ItemTypes im Area
	 * @param itemType
	 * @return
	 */
	public int getAreaWeightedMeanSellPrice(ItemType itemType){
		if (this.tradeRegions==null){return 0;}
		long totalsum = 0;
		long totalAmount = 0;
		
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion r = (TradeRegion)iter.next();
			if (r.getRegion().maxLuxuries()>0 && r.hasTrader()){
				if (r.getBuyItemType()!=null){
					if (!r.getBuyItemType().equals(itemType)){
						long actP = r.getSellPrice(itemType);
						if (actP>0){
							totalsum += (actP*r.getRegion().maxLuxuries());
							totalAmount += r.getRegion().maxLuxuries();
						}
					}
				}
			}
		}
		int erg = (int)Math.floor((double)totalsum/(double)totalAmount);
		return erg;
	}
	
	/**
	 * fügt einen Trader zu traders hinzu
	 * @param t der Trader
	 */
	public void addTrader(Trader t){
		if (this.traders==null){
			this.traders = new ArrayList<Trader>();
		}
		if (!this.traders.contains(t)){
			this.traders.add(t);
		}
	}
	
	/**
	 * fügt einen Transporter zu transporters hinzu
	 * @param t der Transporter
	 */
	public void addTransporter(Transporter t){
		if (this.transporters==null){
			this.transporters = new ArrayList<Transporter>();
		}
		if (!this.transporters.contains(t)){
			this.transporters.add(t);
		}
	}
	
	/**
	 * liefert TransportRequests aller Trader dieses Areas
	 * @return
	 */
	private ArrayList<TransportRequest> getTraderTransportRequests(){
		if (this.transportRequests==null){
			this.transportRequests = makeTraderTransportRequests();
		}
		return this.transportRequests;
	}
	
	/**
	 * berechnet TransportRequests aller Trader dieses Areas
	 * @return
	 */
	private ArrayList<TransportRequest> makeTraderTransportRequests(){
			
			ArrayList<TransportRequest> requests = new ArrayList<TransportRequest>();;
			if (this.traders==null){return null;}
			for (Iterator<Trader> iter = this.traders.iterator();iter.hasNext();){
				Trader t = (Trader)iter.next();
				ArrayList<TransportRequest> newRequests = t.getTraderTransportRequests();
				if (newRequests!=null){
					if (requests==null){
						requests = new ArrayList<TransportRequest>();
					}
					requests.addAll(newRequests);
				}
			}
			
			// Casten of PriorityUsers - das sollte unnötig sein, weiss nicht, warum es nicht geht
			ArrayList<PriorityUser> workList = new ArrayList<PriorityUser>();
			
			for (TransportRequest TR : requests){
					workList.add((PriorityUser)TR);
			}
			
			// normalisieren
			FFToolsArrayList.normalizeArrayList(workList, 700, 100);
			
			// worklist zurück.
			for (PriorityUser PU : workList){
				requests.add((TransportRequest)PU);
			}
			return requests;
		}
	
	
	/**
	 * liefert weitere Anforderer an Handelswaren neben normalen Regionshändlern
	 * Denkbar sind extra scripts, die Vorräte zur Lieferung nach Extern
	 * bzw zur Lieferung an ein anderes TradeArea ansammeln
	 * @return
	 */
	private ArrayList<TransportRequest> getOtherTransportRequests(){
		// Vorrat
		ArrayList<TransportRequest> erg = null;
		
		if (this.vorratRequests!=null && this.vorratRequests.size()>0){
			if (erg==null){
				erg = new ArrayList<TransportRequest>();
			}
			for (Iterator<Vorrat> iter=this.vorratRequests.iterator();iter.hasNext();){
				Vorrat vorratScript = (Vorrat) iter.next();
				TransportRequest actR = vorratScript.createTransportRequest();
				if (actR!=null){
					erg.add(actR);
				}
			}
		}

		return erg;
	}
	
	/**
	 * liefert Anforderungen aus unerfüllten MatPoolRequests
	 * 
	 * @return
	 */
	private ArrayList<TransportRequest> getMatPoolTransportRequests(MatPoolManager MPM){
		
		ArrayList<TransportRequest> erg = new ArrayList<TransportRequest>();
		if (this.tradeRegions==null || this.tradeRegions.size()==0){
			return erg;
		}
		
		for (Iterator<TradeRegion> iter = this.getRegionIterator();iter.hasNext();){
			TradeRegion tradeRegion = (TradeRegion)iter.next();
			MatPool MP = MPM.getRegionsMatPool(tradeRegion.getRegion());
			if (MP!=null){
				// MatPool am Wickel...
				erg.addAll(MP.getTransportRequests());
			}
		}

		return erg;
	}
	
	/**
	 * liefert ALLE in diesem TradeArea mit Handelswaren verbundene TransportRequests
	 * @return
	 */
	public ArrayList<TransportRequest> getTransportRequests(Overlord OL){
		ArrayList<TransportRequest> erg = null;
		
		// Trader
		ArrayList<TransportRequest> add1 = this.getTraderTransportRequests();
		if (add1!=null){
			if (erg==null){
				erg = new ArrayList<TransportRequest>();
			}
			erg.addAll(add1);
		}
		
		//		 Trader
		if (OL!=null){
			MatPoolManager MMM = OL.getMatPoolManager();
			if (MMM==null){
				outText.addOutLine("!*! Kein MatPoolManager in " + this.name,true);
			} else {
				ArrayList<TransportRequest> addMP = this.getMatPoolTransportRequests(OL.getMatPoolManager());
				if (addMP!=null){
					if (erg==null){
						erg = new ArrayList<TransportRequest>();
					}
					erg.addAll(addMP);
				}
			}
		} else {
			outText.addOutLine("!*! Kein OverLord in " + this.name,true);
		}
		// other
		ArrayList<TransportRequest> add2 = this.getOtherTransportRequests();
		if (add2!=null){
			if (erg==null){
				erg = new ArrayList<TransportRequest>();
			}
			erg.addAll(add2);
		}
		
		// sortieren
		if (erg!=null){
			Collections.sort(erg);
		}
		return erg;
	}
	
	
	/**
	 * fügt ein Vorrat - Script zum TAH hinzu
	 * @param vorrat
	 */
	public void addVorratScript(Vorrat vorrat){
		if (this.vorratRequests==null){
			this.vorratRequests = new ArrayList<Vorrat>();
		}
		if (!this.vorratRequests.contains(vorrat)){
			this.vorratRequests.add(vorrat);
		}
	}
	
	/**
	 * fügt ein Vorrat - Script zum TAH hinzu
	 * @param vorrat
	 */
	public void addVorratScript2ALL(Vorrat vorrat){
		if (this.vorratRequestsAll==null){
			this.vorratRequestsAll = new ArrayList<Vorrat>();
		}
		if (!this.vorratRequestsAll.contains(vorrat)){
			this.vorratRequestsAll.add(vorrat);
		}
	}
	
	/**
	 * Prüft, ob das Depot der Region ausreichend Ware auf Vorrat hat 
	 * @param r
	 * @return
	 */
	public boolean isLagerVoll(TradeRegion r,int kaufMengeTheo){
		boolean erg = false;
		Unit depotUnit = r.getDepot();
		if (depotUnit==null){
			return false;
		}
		
		ItemType itemType = r.getBuyItemType();
		if (itemType==null){
			return false;
		}
		
		Item item = depotUnit.getItem(itemType);
		if (item==null) {
			return false;
		}
		
		if (maxRundeEinkaufAufLager*kaufMengeTheo<item.getAmount()){
			return true;
		}
		
		return erg;
	}
	
	
	public Trader getVerkaufsTrader(Region r){
		if (this.traders==null || this.traders.size()==0){
			return null;
		}
		for (Trader t:this.traders){
			if (t.getScriptUnit().getUnit().getRegion().equals(r) && t.isVerkaufen()) {
				return t;
			}
		}
		return null;
	}
	
	
	/**
	 * Liefert die areaweite Zusammenfassung - mit Info
	 * @param itemType
	 * @return
	 */
	public int getAreaBalance(ItemType itemType,boolean _informUs){
		int erg  =  0;
		
		
		
		// was kann selbst maximal gekauft werden...
		int AreaBuyMaxAmount = getAreaBuyMaxAmount(itemType);
		erg = AreaBuyMaxAmount;
		
		int rundenVerkauf = this.getAreaSellAmount(itemType) * 1;
		// minus was hier verkauft werden kann für X Runden
		erg -= (rundenVerkauf);
		
		int rundenVorrat = this.getAreaVorratProRundeAmountAll(itemType) * 1;
		// minus was an Vorräten extern definiert worden ist
		erg -=(rundenVorrat);
		
		
		// Abkapselung bei Vorrat für XX Runden (XX=10)
		int totalAmount = this.getAreaTotalAmount(itemType);
		int neededRundenSumme = (rundenVerkauf + rundenVorrat) * 10; 
		if (totalAmount > neededRundenSumme ){
			erg = totalAmount - neededRundenSumme;
			erg = Math.max(erg, AreaBuyMaxAmount);
			if (_informUs){
				// outText.addOutLine("uebervoll: " + itemType.getName() + " in " + this.getName());
				outText.addOutChars(itemType.getName(), 15);
				outText.addOutChars(" Balance:");
				outText.addOutChars(erg + "", 6);
				outText.addOutChars("  Total:");
				outText.addOutChars(totalAmount + "", 6);
				outText.addOutChars("  Max:");
				outText.addOutChars(neededRundenSumme + "", 6);
				outText.addOutChars(" MaxBuy:");
				outText.addOutChars(AreaBuyMaxAmount + "", 6);
				outText.addOutChars("    Sell:");
				outText.addOutChars(rundenVerkauf + "", 6);
				outText.addOutChars(" Vorräte:");
				outText.addOutChars(rundenVorrat + "", 6);
				outText.addNewLine();
			}
		} else {
			// normales erg....
			if (_informUs){
				outText.addOutChars(itemType.getName(), 15);
				outText.addOutChars(" Balance:");
				outText.addOutChars(erg + "", 6);
				outText.addOutChars("  MaxBuy:");
				outText.addOutChars(AreaBuyMaxAmount + "", 6);
				outText.addOutChars(" Sell:");
				outText.addOutChars(rundenVerkauf + "", 6);
				outText.addOutChars(" Vorräte:");
				outText.addOutChars(rundenVorrat + "", 6);
				outText.addNewLine();
			}
		}
		
		
		return erg;
	}
	
	/**
	 * Liefert die areaweite Zusammenfassung - ohne Info
	 * @param itemType
	 * @return
	 */
	public int getAreaBalance(ItemType itemType){
		return getAreaBalance(itemType,false);
	}
	
	
	/**
	 * liefert summe aller ScriptUnits
	 * @param itemType
	 * @return
	 */
	public int getAreaTotalAmount(ItemType itemType){
		
		if (this.tradeRegions==null){return 0;}
		int erg = 0;
		
		for (TradeRegion r:this.tradeRegions){
			erg += r.getTotalAmount(itemType);
		}
		return erg;
	}
	
	/**
	 * liefert die Liste der Transporter
	 * @return
	 */
	public ArrayList<Transporter> getTransporters() {
		return transporters;
	}

	
	/**
	 * Returns the TA-Baumanager
	 * @return TradeAreaBauManager
	 */
	public TradeAreaBauManager getTradeAreaBauManager() {
		if (this.tradeAreaBauManager==null){
			this.tradeAreaBauManager = new TradeAreaBauManager(this);
		}
		return this.tradeAreaBauManager;
	}
	
	
	/**
	 * Exists a BauManager ?
	 * @return bool
	 */
	public boolean hasBauManager(){
		if (this.tradeAreaBauManager==null){
			return false;
		}
		return true;
	}
	
	public boolean includesRegion(Region r){
		for (TradeRegion TR:this.tradeRegions){
			if (TR.getRegion().equals(r)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * calculates all available amounts for silver in all (trade)regions.
	 * takes silver avail in Region and deduct active "treiber"
	 * @return
	 */
	public int calcAreaAvailCircus(){
		if (this.tradeRegions==null || this.tradeRegions.size()==0){
			return 0;
		}
		int erg = 0;
		TreiberPoolManager TPM = this.overlord.getTreiberPoolManager();
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion tr = (TradeRegion)iter.next();
			Region r = tr.getRegion();
			erg += r.maxEntertain();
			if (TPM!=null) {
				TreiberPool TP = TPM.getTreiberpool(r);
				if (TP != null){
					erg -= TP.getRegionsVerdienst();
				}
			}
		}
		return erg;
	}
	
	/*
	 * int maxWorkers = Utils.getIntValue(getRules().getMaxWorkers(r), 0);
    int workers = Math.min(maxWorkers, r.getPeasants());
    int surplus = (workers * r.getPeasantWage()) - (r.getPeasants() * getPeasantMaintenance(r));
	 */
	/**
	 * calculates the long term available amounts for silver (surplus) in all (trade)regions.
	 * takes silver surplus in Region and deduct active "treiber"
	 * should add payment to region from traders
	 * @return
	 */
	public int calcAreaSurplusCircus(){
		if (this.tradeRegions==null || this.tradeRegions.size()==0){
			return 0;
		}
		int erg = 0;
		TreiberPoolManager TPM = this.overlord.getTreiberPoolManager();
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion tr = (TradeRegion)iter.next();
			Region r = tr.getRegion();
			
			int maxWorkers = Utils.getIntValue(this.overlord.getScriptMain().gd_ScriptMain.getGameSpecificRules().getMaxWorkers(r), 0);
		    int workers = Math.min(maxWorkers, r.getPeasants());
		    int surplus = (workers * r.getPeasantWage()) - (r.getPeasants() * this.overlord.getScriptMain().gd_ScriptMain.getGameSpecificRules().getPeasantMaintenance(r));
			erg += surplus;
			if (TPM!=null) {
				TreiberPool TP = TPM.getTreiberpool(r);
				if (TP != null){
					erg -= TP.getRegionsVerdienst();
				}
			}
			erg -= tr.getBuySilverAmount();
		}
		return erg;
	}
	
	
	/**
	 * calculates all available amounts for Entertain through script units.
	 * 
	 * @return
	 */
	public int calcAreaCircus(){
		if (this.tradeRegions==null || this.tradeRegions.size()==0){
			return 0;
		}
		int erg = 0;
		CircusPoolManager CPM = this.overlord.getCircusPoolManager();
		for (Iterator<TradeRegion> iter = this.tradeRegions.iterator();iter.hasNext();){
			TradeRegion tr = (TradeRegion)iter.next();
			Region r = tr.getRegion();
			CircusPool CP = CPM.getCircusPool(r);
			if (CP != null){
				erg+=CP.getMaxEntertainUnits();
			}
		}
		return erg;
	}
	
	/**
	 * 
	 * @return tghe amount of trade regions in this TA
	 */
	public int anzahlRegionen(){
		if (this.tradeRegions==null || this.tradeRegions.size()==0){
			return 0;
		}
		return this.tradeRegions.size();
	}
	
	/**
	 * 
	 * @return Amount of Transport without Requests = empty
	 */
	public int anzahlEmptyTransports(){
		if (this.transporters==null || this.transporters.size()==0){
			return 0;
		}
		int erg = 0;
		for (Transporter t:this.getTransporters()){
			if (t.getTransporterRequests()==null || t.getTransporterRequests().size()==0){
				erg++;
			}
		}
		return erg;
	}
	
	/**
	 * 
	 * @return Amount of Transport without GoToInfo = idle
	 */
	public int anzahlIdleTransports(){
		if (this.transporters==null || this.transporters.size()==0){
			return 0;
		}
		int erg = 0;
		for (Transporter t:this.getTransporters()){
			if (t.getGotoInfo()==null){
				erg++;
			}
		}
		return erg;
	}
}
