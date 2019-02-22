package com.fftools.transport;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Item;
import magellan.library.Region;
import magellan.library.rules.ItemType;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Depot;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeAreaHandler;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

/**
 * Registriert Transporter
 * Nimmt Transportaufträge entgegen
 * Ordnet zu und setzt orders
 * @author Fiete
 *
 */
public class TransportManager implements OverlordInfo,OverlordRun{
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	/*
	 * Ausgabe in TransportRequests_Name_X.txt ?
	 */
	private boolean reportOFF = false;
	
	private static final int Durchlauf = 500;

	private int[] runners = {Durchlauf};
	
	private ScriptMain scriptMain = null;
	
	private Hashtable<ScriptUnit,Transporter> transporters = null;
	
	private Hashtable<Region,ScriptUnit> depots = null;
	
	private Hashtable<ScriptUnit,Integer> pferdeDepots = null;
	
	private Hashtable<ScriptUnit,Transporter> allTransporters = null;
	
	private ArrayList<TransportRequest> transportRequests = null;
	
	private ArrayList<TransportOffer> transportOffers = null;
	
	private TransporterComparatorForOffer transporterComparatorForOffer = null;
	
	private TransporterComparatorForOfferUsed transporterComparatorForOfferUsed = null;
	
	private TransportOfferComparator  transportOfferComparator = null;
	
	private TransportOfferComparatorAmount transportOfferComparatorAmount = null;
	
	private String actLogName = "TransportRequest";
	
	private int specialCounter = 0;
	
	private boolean TMoptimize1 = false;
	
	private boolean TMoptimize2 = false;
	
	public TransportManager(ScriptMain _scriptMain){
		this.scriptMain = _scriptMain;
		this.reportOFF = reportSettings.getOptionBoolean("disable_report_TransportRequests");
	}
	
	/**
	 * liefert zu einer Scriptunit den Transporter.
	 * Legt einen an, wenn noch keiner existiert.
	 * @param u
	 * @return
	 */
	public Transporter addTransporter(ScriptUnit u){
		if (this.allTransporters==null){
			this.allTransporters = new Hashtable<ScriptUnit, Transporter>();
		} else {
			if (this.allTransporters.containsKey(u)){
				return (Transporter)this.allTransporters.get(u);
			}
		}
		Transporter t = new Transporter(u);
		this.allTransporters.put(u, t);
		return t;
	}
	
	/**
	 * fügt eine ScriptUnit als Depot hinzu
	 * @param u
	 */
	public void addDepot(ScriptUnit u){
		if (this.depots==null){
			this.depots = new Hashtable<Region, ScriptUnit>();
		}
		
		Region r = u.getUnit().getRegion();
		if (this.depots.containsKey(r)){
			outText.addOutLine("!!! In " + r.getName() + " ist mehr als ein Depot!",true);
			ScriptUnit uDa = this.depots.get(r);
			outText.addOutLine(uDa.toString() + " wird ersetzt durch " + u.toString(),true);
		}
		
		this.depots.put(r,u);
	}
	
	
	/**
	 * info-zeilen über Anzahl und Zustand der Transporter
	 *
	 */
	public void informUs(){
		
		if (this.reportOFF) {
			return;
		}
		
		if (this.allTransporters==null || this.allTransporters.size()==0){
			outText.addOutLine("TransportManager: keine Transporter bekannt.");
		} else {
			outText.addOutLine("TransportManager: " + this.allTransporters.size() + " Transporter.");
		}
		if (this.depots==null || this.depots.size()==0){
			outText.addOutLine("TransportManager: keine Depots bekannt.");
		} else {
			outText.addOutLine("TransportManager: " + this.depots.size() + " Depots.");
		}
		
		if (this.TMoptimize1){
		   outText.addOutLine("TM optimize1 is aktive, optimized transports: " + this.specialCounter);
		} else {
			outText.addOutLine("TM optimize1 is inaktive");
		}
		if (this.TMoptimize2){
		   outText.addOutLine("TM optimize2 is aktive");
		} else {
			outText.addOutLine("TM optimize2 is inaktive");
		}
		
	}
	
	/**
	 * Overlord run
	 */
	public void run(int Durchlauf){
		
		
		
		
		long startT1 = System.currentTimeMillis();
		outText.addOutLine("TransportManager startet...generiere Anforderungen.");
		if (this.transportRequests==null){
			this.transportRequests = new ArrayList<TransportRequest>();
		} else {
			this.transportRequests.clear();
		}
		// zuerst mal die TradeAreas auffordern ihre Requests einzutragen
		TradeAreaHandler TAH = this.scriptMain.getOverlord().getTradeAreaHandler();
		this.transportRequests.addAll(TAH.getTransportRequests());
		
		// hier später weitere manager abfragen nach deren requests...
		
		// alle Offers aus den Depots generieren
		outText.addOutLine("generiere Offers");
		this.createOffers();
		
		if (this.transportOffers!=null && this.transportRequests!=null){
			outText.addOutLine("Info: TransportManager kennt " + this.transportRequests.size() + " Anforderungen und " + this.transportOffers.size() + " Angebote");
		}
		outText.setScreenOut(false);
		outText.setFile("TransportRequests");
		
		this.runByTradeAreas(TAH);
		
		outText.setScreenOut(true);
		outText.setFileStandard();
		long endT1 = System.currentTimeMillis();
		outText.addOutLine("TransportManager benötigte " + (endT1-startT1) + " ms.");
		FFToolsRegions.informUs();
		outText.setScreenOut(false);
	}
	
	/**
	 * arbeitet die TAs ab
	 * @param TAH
	 */
	private void runByTradeAreas(TradeAreaHandler TAH){
		
		if (TAH.getTradeAreas()==null || TAH.getTradeAreas().size()==0){
				outText.addOutLine("****** TransportManager: keine TadeAreas bekannt. Exit!");
			return;
		}

		outText.addOutLine("****** TransportManager: " + TAH.getTradeAreas().size() + " Areas bekannt.");
		for (Iterator<TradeArea> iter = TAH.getTradeAreas().iterator();iter.hasNext();){
			TradeArea TA = (TradeArea)iter.next();
			ArrayList<TradeRegion> tradeRegions = new ArrayList<TradeRegion>();
			outText.setScreenOut(true);
			outText.addNewLine();
			outText.addOutChars("***TM " + TA.getName() + ": ",30);
			tradeRegions.addAll(TA.getTradeRegions());
			this.sortTransportersOutByTradeArea(TAH,TA);
			
			if (this.allTransporters!=null){
				outText.addOutChars("T: " + this.transporters.size() + "/" + this.allTransporters.size() + "", 15);
			} else {
				outText.addOutLine("keine Transporter im CR ?!?");
			}
			if (this.transporters.size()>0){
				outText.setScreenOut(false);
				long startT = System.currentTimeMillis();
				this.runRegions(tradeRegions,TA.getName());
				// Transporter inform UNits
				int countEmpty = 0 ;
				int countIdle = 0 ;
				for (Transporter transporter : this.transporters.values()){
					transporter.informUnits();
					if (transporter.getGotoInfo()==null){
						countIdle++;
					}
					if (transporter.getTransporterRequests()!=null && transporter.getTransporterRequests().size()>0){
						countEmpty++;
					}
				}
				long endT = System.currentTimeMillis();
				outText.setScreenOut(true);
				outText.addOutChars(", t=" + (endT-startT) + " ms.", 10);
				outText.addOutChars(", empty:" + countEmpty, 10);
				int proz = (int)Math.floor(((double)countEmpty / (double)this.transporters.size())*100);
				outText.addOutChars(" (" + proz + "%)", 6);
				outText.addOutChars(", idle:" + countIdle, 10);
				proz = (int)Math.floor(((double)countIdle / (double)this.transporters.size())*100);
				outText.addOutChars(" (" + proz + "%)", 6);
				
				double calcAreaAvailCircus = TA.calcAreaAvailCircus();
				double calcAreaCircus = TA.calcAreaCircus();
				proz=0;
				if (calcAreaAvailCircus>0){
					proz=(int)Math.round((calcAreaCircus / calcAreaAvailCircus)*100);
				}
				outText.addOutChars(" | Unterh-Vers:" + proz + "%", 25);
				outText.setScreenOut(false);
			} else {
				
				outText.setScreenOut(true);
				outText.addOutChars(" - keine Transporter.");
				outText.setScreenOut(false);
			}
		}
	}
	
	private void sortTransportersOutByTradeArea(TradeAreaHandler TAH,TradeArea TA){
		// liste löschen
		if (this.transporters==null){
			this.transporters = new Hashtable<ScriptUnit, Transporter>();
		} else {
			this.transporters.clear();
		}
		if (this.allTransporters==null){
			return;
		}
		
		if (TA.getTransporters()==null){
			return;
		}
		
		for (Transporter t:TA.getTransporters()){
			this.transporters.put(t.getScriptUnit(),t);
		}
	}
	
	
	/**
	 * evaluiert anstehende transporte in den angegeben regionen
	 * @param regions
	 */
	@SuppressWarnings("unused")
	private void runRegions(ArrayList<TradeRegion> tradeRegions, String infoName){
		// eigene Region Liste bauen
		// neu: unter berücksichtigung der selektierten Regionen
		ArrayList<Region> regions = new ArrayList<Region>();
		
		
		long startT = System.currentTimeMillis();
		
		for (Iterator<TradeRegion> iter = tradeRegions.iterator();iter.hasNext();){
			Region r = ((TradeRegion)iter.next()).getRegion();
			regions.add(r);
		}
		
		
		// request liste anpassen auf Regionen
		ArrayList<TransportRequest> actRequests = new ArrayList<TransportRequest>();
		for (Iterator<TransportRequest> iter = this.transportRequests.iterator();iter.hasNext();){
			TransportRequest tR = (TransportRequest)iter.next();
			if (regions.contains(tR.getRegion())){
				actRequests.add(tR);
			}
		}
		
		//	offer liste anpassen auf Regionen
		ArrayList<TransportOffer> actOffers = new ArrayList<TransportOffer>();
		for (Iterator<TransportOffer> iter = this.transportOffers.iterator();iter.hasNext();){
			TransportOffer tO = (TransportOffer)iter.next();
			if (regions.contains(tO.getRegion())){
				actOffers.add(tO);
			}
		}
		
		// ItemType liste aller Offers bauen, um damit requestliste nochmals
		// zu verkleinern (nur die requests drinne lassen, für die es überhaupt 
		// offers gibt
		
		ArrayList<ItemType> actItemTypes = new ArrayList<ItemType>();
		for (Iterator<TransportOffer> iter = actOffers.iterator();iter.hasNext();){
			TransportOffer actOffer = (TransportOffer)iter.next();
			ItemType actItemType = actOffer.getItem().getItemType();
			if (!actItemTypes.contains(actItemType)){
				actItemTypes.add(actItemType);
			}
		}
		
		// Requests durchlaufen...wenn gar nicht erfüllt werden könne, nicht
		// übernehmen
		ArrayList<TransportRequest> areaRequests = new ArrayList<TransportRequest>();
		for (Iterator<TransportRequest> iter = actRequests.iterator();iter.hasNext();){
			TransportRequest actRequest = (TransportRequest)iter.next();
			// Liste der OfferTypes durchlaufen..ein geoffertes sollte dabei sein
			boolean isTheoreticalOK = false;
			for (Iterator<ItemType> iterOfferType = actItemTypes.iterator();iterOfferType.hasNext();){
				ItemType actItemType = (ItemType)iterOfferType.next();
				if (actRequest.containsItemType(actItemType)){
					isTheoreticalOK = true;
					break;
				}
			}
			if (isTheoreticalOK){
				areaRequests.add(actRequest);
			}
		}
		
		// umgekehrter weg..offers auf die reduzieren, die auch requested werden
		// dazu itemtypelist aller requests bauen
		ArrayList<ItemType> requestItemTypes = new ArrayList<ItemType>();
		for (Iterator<TransportRequest> iter = areaRequests.iterator();iter.hasNext();){
			TransportRequest TR = (TransportRequest)iter.next();
			for (Iterator<ItemType> iterRequestItems = TR.getItemTypes().iterator();iterRequestItems.hasNext();){
				ItemType actItemType = (ItemType)iterRequestItems.next();
				if (!requestItemTypes.contains(actItemType)){
					requestItemTypes.add(actItemType);
				}
			}
		}
		
		// offers aussieben nach denen, die gebraucht werden
		ArrayList<TransportOffer> areaOffers = new ArrayList<TransportOffer>();
		for (Iterator<TransportOffer> iter = actOffers.iterator();iter.hasNext();){
			TransportOffer TO = (TransportOffer)iter.next();
			if (requestItemTypes.contains(TO.getItem().getItemType())){
				areaOffers.add(TO);
			}
		}
		
		this.actLogName = "TransportRequests_" + infoName;
		
		// hier erstmal info ?!
		if (!this.reportOFF) {
			outText.setScreenOut(false);
			outText.setFile(this.actLogName + "_1");
		}
		if (areaRequests!=null){
			Collections.sort(areaRequests);
			if (!this.reportOFF) {
				outText.addOutLine("Requests: " + areaRequests.size());
				listRequests(areaRequests);
			}
		} else {
			if (!this.reportOFF) {
				outText.addOutLine("keine Requests vorhanden.");
			}
		}
		if (areaOffers!=null){
			Collections.sort(areaOffers);
			if (!this.reportOFF) {
				outText.addOutLine("Offers: " + areaOffers.size());
				listOffers(areaOffers);
			}
		} else {
			if (!this.reportOFF) {
				outText.addOutLine("keine Offers vorhanden.");
			}
		}	
		if (!this.reportOFF) {
			outText.addOutLine("Warenarten: " + requestItemTypes.size());
		}
		
		if (areaRequests==null || areaOffers==null){
			if (!this.reportOFF) {
				outText.addOutLine("leaving Transportmanager");
			}
			return;
		}
		
		long endT = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Vorbereitungen für " + infoName + " brauchten: " + (endT-startT) + "ms (R: " + areaRequests.size() + ", O: " + areaOffers.size() +")", true);
		}
		
		
		this.orderTransports(areaRequests, areaOffers);
		
		
		// abschliessend wieder infos
		if (!this.reportOFF) {
			outText.setScreenOut(false);
			outText.setFile(this.actLogName + "_3");
		}
		if (areaRequests!=null){
			Collections.sort(areaRequests);
			if (!this.reportOFF) {
				outText.addOutLine("Requests: " + areaRequests.size());
				listRequests(areaRequests);
			}
		} else {
			if (!this.reportOFF) {
				outText.addOutLine("keine Requests vorhanden.");
			}
		}
		if (areaOffers!=null){
			Collections.sort(areaOffers);
			if (!this.reportOFF) {
				outText.addOutLine("Offers: " + areaOffers.size());
				listOffers(areaOffers);
			}
		} else {
			if (!this.reportOFF) {
				outText.addOutLine("keine Offers vorhanden.");
			}
		}
		
	    // Statistiken zu den abgearbeiteten Requests und offers
		if (areaRequests!=null){
			if (!this.reportOFF) {
				this.statRequests(areaRequests);
			}
		}
		
		// und nu noch transporter infos
		if (!this.reportOFF) {
			outText.setScreenOut(false);
			outText.setFile(this.actLogName + "_Transporter");
		}
		if (this.transporters==null || this.transporters.size()==0){
			if (!this.reportOFF) {
				outText.addOutLine("Keine transporter im TA");
			}
 		} else {
 			// Sortieren
 			ArrayList<Transporter> trans = new ArrayList<Transporter>();
 			trans.addAll(this.transporters.values());
 			Collections.sort(trans, new TransporterComparatorForInformStatus());
 			int allKapa = 0;
 			int allKapa_frei = 0;
 			
 			NumberFormat NF = NumberFormat.getInstance();
 			NF.setMaximumFractionDigits(1);
 			NF.setMinimumFractionDigits(1);
 			NF.setMinimumIntegerDigits(3);
 			NF.setMaximumIntegerDigits(3);
 			int cnt_leerfahrten=0;
 			for (Iterator<Transporter> iter = trans.iterator();iter.hasNext();){
 				Transporter actTrans = (Transporter)iter.next();
 				if (actTrans.getMode()==Transporter.transporterMode_fullautomatic){
 					if (!this.reportOFF) {
 						outText.addOutLine(actTrans.informLogTransporterStatus(NF));
 					}
	 				allKapa+=actTrans.getKapa();
	 				allKapa_frei += actTrans.getKapa_frei();
	 				if (actTrans.getKapa_frei()==actTrans.getKapa()){
	 					cnt_leerfahrten++;
	 				}
 				}
 			}
 			if (allKapa>0){
 				double gesamtAuslastung = 0;
 				gesamtAuslastung = (1 - ((double)allKapa_frei/(double)allKapa))*100;
 				if (!this.reportOFF) {
	 				outText.addOutLine("***");
	 				outText.addOutLine("GesamtAuslastung:" + NF.format(gesamtAuslastung) + "%");
	 				if (cnt_leerfahrten>0){
	 					outText.addOutLine("Leerfahrten:" + cnt_leerfahrten);
	 				}
 				}
 			}
 		}
		if (!this.reportOFF) {
			outText.setFileStandard();
			outText.setScreenOut(true);
		}
	}
	
	/**
	 * Statistik über Abarbeitung / erfüllung der Requests
	 * @param transportRequests
	 */
	private void statRequests(ArrayList<TransportRequest> transportRequests){
		int maxPrio = 0;
		// maxPrio herausfinden
		for (TransportRequest request : transportRequests){
			maxPrio = Math.max(maxPrio,request.getPrio());
		}
		// quantile festlegen (einordnen in 4 Kategorien)
		int quantenStufe = (int)((double)maxPrio / 4);
		// zähler
		int[] cntRequests = new int[4];
		int[] cntRequestsWork = new int[4];
		int[] cntRequestsRequested = new int[4];
		int[] cntRequestsBearbeitet = new int[4];
		int summeRequests = 0;
		int summeRequestsWork = 0;
		int summeRequestsRequested = 0;
		int summeRequestsBearbeitet = 0;
		
		// Zählen...
		for (TransportRequest request : transportRequests){
			// welches quantil?
			int q = (int)((double)request.getPrio()/(double)quantenStufe);
			q = Math.min(3, q);
			summeRequests++;
			cntRequests[q]++;
			if (request.getBearbeitet()>0){
				summeRequestsWork++;
				cntRequestsWork[q]++;
			}
			cntRequestsRequested[q]+=request.getOriginalGefordert();
			cntRequestsBearbeitet[q]+=request.getBearbeitet();
			summeRequestsRequested+=request.getOriginalGefordert();
			summeRequestsBearbeitet+=request.getBearbeitet();
		}
		NumberFormat NF = NumberFormat.getInstance();
		NF.setMaximumFractionDigits(1);
		NF.setMinimumFractionDigits(1);
		NF.setMinimumIntegerDigits(3);
		NF.setMaximumIntegerDigits(3);
		
		outText.addOutLine("**************Statistik zu den Requests**********");
		// quantile
		for (int i = 0;i<=3;i++){
			outText.addOutLine("Kat " + i + " von Prio " + i*quantenStufe + " bis " + (i+1)*quantenStufe + ": " + cntRequestsWork[i] + " von " + cntRequests[i]+ " bearbeitet. (" + NF.format(((double)cntRequestsWork[i]/(double)cntRequests[i])*100) + "%)");
			outText.addOutLine("von " + cntRequestsRequested[i] + " Stück gefordert " + cntRequestsBearbeitet[i]+ " bearbeitet. (" + NF.format(((double)cntRequestsBearbeitet[i]/(double)cntRequestsRequested[i])*100) + "%)");
		}
		outText.addOutLine("+++Übersicht++++");
		outText.addOutLine("Insgesamt " + summeRequestsWork + " von " + summeRequests + " bearbeitet. (" + NF.format(((double)summeRequestsWork/(double)summeRequests)*100) + "%)");
		outText.addOutLine("von " + summeRequestsRequested + " Stück gefordert " + summeRequestsBearbeitet+ " bearbeitet. (" + NF.format(((double)summeRequestsBearbeitet/(double)summeRequestsRequested)*100) + "%)");
	}
	
	/**
	 * Versucht, zu den requests offers zu finden und transport zuzuordnen
	 * erwartet bereits sortierte Liste mit requests
	 * @param requests
	 * @param offers
	 */
	private void orderTransports(ArrayList<TransportRequest> requests, ArrayList<TransportOffer> offers){
		long startT = System.currentTimeMillis();
		long endT = System.currentTimeMillis();
		int cnter = 0;
		long lastInfoT = 0;
		for (Iterator<TransportRequest> iter = requests.iterator();iter.hasNext();){
			TransportRequest actRequest = (TransportRequest)iter.next();
			
			ArrayList<TransportOffer> actOffers = new ArrayList<TransportOffer>();
			for (Iterator<TransportOffer> iter2 = offers.iterator();iter2.hasNext();){
				TransportOffer actOffer = (TransportOffer) iter2.next();
				if (actOffer.getAnzahl_nochOfferiert()>0 && actRequest.containsItemType(actOffer.getItem().getItemType())){
					actOffers.add(actOffer);
				}
			}
			if (actOffers.size()>0){
				this.processRequest(actRequest, actOffers);
			}
			
			// userInfo
			endT = System.currentTimeMillis();
			long zeitSeitStart =  (endT-startT);
			cnter++;
			if (zeitSeitStart>=10000){
				// 10s vergangen....wir sollten anfangen den status zu posten
				double realAnteil = (double) cnter / (double) requests.size();
				int proz = (int)Math.floor( realAnteil * 100 );
				if ((endT-lastInfoT)>=10000){
					// yep!
					outText.setScreenOut(true);
					outText.addOutChars("" + proz + "%");
					outText.setScreenOut(false);
					lastInfoT=endT;
				}
			}
		}
	}
	
	/**
	 * bearbeitet einen bestimmten Request, findet solange offers, bis request abgearbeitet oder keine fähigen
	 * offers mehr da
	 * @param request
	 * @param offers
	 * @return
	 */
	private void processRequest(TransportRequest request, ArrayList<TransportOffer> offers){
		
		// kleine info an den user
		outText.addPoint();
		
		// log
		if (!this.reportOFF) {
			outText.setScreenOut(false);
			outText.setFile(this.actLogName + "_2");
			outText.addOutLine("Request: " + request.getForderung() + " " + request.getOriginalGegenstand() + " (Prio:" + request.getPrio() + ") nach " + request.getRegion().toString());
		}
		long startT = System.currentTimeMillis();
		long start1=0;
		long endT= 0;
		// offers nach Entfernung zum Request sortieren...
		// den Comparator auf aktuelle request-region setzen
		if (transportOfferComparator==null){
			this.transportOfferComparator = new TransportOfferComparator();
		} 
		if (transportOfferComparatorAmount ==null){
			this.transportOfferComparatorAmount = new TransportOfferComparatorAmount();
		} 
		
		long vorDist = System.currentTimeMillis();
		
		// neu: dist setzen
		for (Iterator<TransportOffer> iter = offers.iterator();iter.hasNext();){
			TransportOffer offer = (TransportOffer)iter.next();
			int dist = FFToolsRegions.getPathDistLand(this.scriptMain.gd_ScriptMain, offer.getRegion().getCoordinate(),request.getRegion().getCoordinate(), true);
			offer.setActDist(dist);
		}

		// die offers sortieren, die dichtesten an den anfang
		// oder nach mode die fettesten an den Anfang
		start1 = System.currentTimeMillis();
		switch (request.getTM_sortMode()){
		case MatPoolRequest.TM_sortMode_dist:
			Collections.sort(offers, transportOfferComparator);
			if (!this.reportOFF) {
				outText.addOutLine("offer sort benutzt offerComparator DIST");
			}
			break;
		case MatPoolRequest.TM_sortMode_amount:
			Collections.sort(offers, transportOfferComparatorAmount);
			if (!this.reportOFF) {
				outText.addOutLine("offer sort benutzt offerComparator AMOUNT");
			}
			break;
		}
		endT = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Sortieren der Offer (" + offers.size() + " Stück), benötigt:" + (endT-start1) + " ms. (setDist:" + (start1-vorDist) + "ms");
			FFToolsRegions.informUs();
		}
		// die einzelnen Offers abarbeiten, bis request erfüllt oder offers alle
		for (Iterator<TransportOffer> iter = offers.iterator();iter.hasNext();){
			TransportOffer offer = (TransportOffer)iter.next();
			if (offer.getAnzahl_nochOfferiert()>0 && request.containsItemType(offer.getItem().getItemType())){
				this.processRequestAndOffer(request, offer);
			}
			// abbruch, wenn request erfüllt
			if (request.getForderung()==0){
				break;
			}
		}
		endT = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("insgesamt benötigt:" + (endT-startT) + " ms.");
		}
		// request abgearbeitet ODER keine Offers mehr da
	}
	
	/**
	 * Versucht, für ein übergebenes Paar (Request und Offer) die
	 * Transporter zu allozieren
	 * Vorrausgesetzt wird bereits, dass offer auch ein requestetes Item 
	 * anbietet.
	 * @param request zu bearbeitender TransportRequest
	 * @param offer zu nutzende TransportOffer
	 */
	private void processRequestAndOffer(TransportRequest request,TransportOffer offer){
		// kein Transporternotwendig, wenn in gleicher Region
		if (request.getRegion().equals(offer.getRegion())){
			this.processSameRegion(request, offer);
			if (request.getForderung()<=0){
				return;
			}
		}
		// ok, nicht in gleicher Region oder noch was offen
		
		if (!this.reportOFF) {
			outText.addOutLine("++" + offer.getAnzahl_nochOfferiert() + " " + offer.getItemName() + " von " + offer.getRegion().toString() + " nach " + request.getRegion().toString());
		}
		
		// bereits festgelegte Transporter nach Mitfahrgelegenheit suchen
		long start1 = System.currentTimeMillis();
		this.searchForAvailableUsedTransports(request, offer);
		long end1 = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Suche nach bereits benutzten Ts:" + (end1-start1) + " ms.");
		}
		if (request.getForderung()<=0){
			return;
		}
		
		// bereits festgelegte LeerfahrtTransporter nach Mitfahrgelegenheit suchen
		long start1a = System.currentTimeMillis();
		this.searchForAvailableLeerfahrtTransports_Planungsziel(request, offer);
		long end1a = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Suche nach Leerfahrten:" + (end1a-start1a) + " ms.");
		}
		if (request.getForderung()<=0){
			return;
		}
		
		
		/**
		freien Transporter in der Beladungsregion oder dicht dranne suchen
		wenn in offerRegion:
			Ziel setzen (requestRegion)
			Beladungsrequests ergänzen
			Transporterbeladung erhöhen
		wenn nicht in offerRegion
		    Ziel setzen (offerRegion)
		
		in allen Fällen
			incBearbeitet bei offer und Request
		*/
		long start2 = System.currentTimeMillis();
		// erst nur in der Region
		this.searchForAvailableFreeTransports(request, offer,true);
		// hier einstigesstelle für TMoptimize2
		// in der Region gibt es keinen Transport
		// bevor freier genutzt wird, die Transporte durchsuchen
		// die nächste Runde *hier* sein werden und genutzt werden können
		
		
		
		// dann überall
		this.searchForAvailableFreeTransports(request, offer,false);
		long end2 = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Suche nach freien Ts:" + (end2-start2) + " ms.");
		}
		
	}
	
	/**
	 * Versucht, für den request einen Transport zu finden, der *nächste* Runde
	 * in der Region ist UND dann noch was frei hat UND dann in die richtige Richtung
	 * weitergeht
	 * @param request
	 * @param offer
	 */
	/*

	private void searchForAvailableUsedNextRoundTransports(TransportRequest request,TransportOffer offer){
		if (this.transporters!=null &&  !this.transporters.isEmpty()){
			for (Iterator<Transporter> iter = this.transporters.values().iterator();iter.hasNext();){
				Transporter T = (Transporter)iter.next();
				// boolean isgeeignet=true;
				// Bedingung 1: nächste runde in der offer region
				if (T.getDestRegion()==null || T.getGotoInfo()==null || !T.getGotoInfo().getNextHold().equals(offer.getRegion())){
					// isgeeignet=false;
				}
				// Bedingung 2: wir suchen transporter, die weiter gehen
				
				
			}
		}
	}
	*/
	
	/**
	 * durchforstet noch nicht genutzte Transporter
	 * @param request
	 * @param offer
	 */
	private void searchForAvailableFreeTransports(TransportRequest request,TransportOffer offer,boolean onlyInRegion){
		// TransporterListe sortieren
		// Prio 1 : Entfernung
		// Prio 2 : passende Kapa...falls 2 passen, den mit weniger Überhang
		// notwendig..benötigtes Gewicht
		int Anzahl = Math.min(request.getForderung(),offer.getAnzahl_nochOfferiert());
		if (Anzahl<=0){return;}
		float weight = offer.getItem().getItemType().getWeight();
		if (weight<0) {weight =0;}
		int weightInt = (int)Math.ceil((weight * Anzahl));
		
		if (this.transporterComparatorForOffer==null){
			this.transporterComparatorForOffer = new TransporterComparatorForOffer(weightInt);
		} else {
			this.transporterComparatorForOffer.setWeight(weightInt);
		}
		
		// Liste sortieren
		long start1 = System.currentTimeMillis();
		ArrayList<Transporter> actTransporters = new ArrayList<Transporter>(1);
		if (this.transporters!=null &&  !this.transporters.isEmpty()){
			for (Iterator<Transporter> iter = this.transporters.values().iterator();iter.hasNext();){
				Transporter T = (Transporter)iter.next();
				// nur Ts aufführen, die noch nicht vergeben sind...
				// und die auf Automatik stehen
				// FF 20070425 und die nach Specs OK sind
				if (T.getDestRegion()==null && T.getMode()==Transporter.transporterMode_fullautomatic
						&& T.isOK4TransportRequest(request)){
					actTransporters.add(T);
				}
			}
		}
		long mid1 = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Filtern der unbenutzten Transporter dauerte: " + (mid1-start1) + "ms");
		}
		
		
		if (actTransporters.isEmpty()){
			// keine TRansporter verfügbar
			return;
		}
		
		// neu: dist vorher setzen
		for (Iterator<Transporter> iter = actTransporters.iterator();iter.hasNext();){
			Transporter actTransporter = (Transporter)iter.next();
			int dist = FFToolsRegions.getPathDistLand(this.scriptMain.gd_ScriptMain, actTransporter.getActRegion().getCoordinate(),offer.getRegion().getCoordinate(), true);
			actTransporter.setActDist(dist);
		}
		
		
		long mid2 = System.currentTimeMillis();
		Collections.sort(actTransporters, this.transporterComparatorForOffer);
		long end1 = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Sortieren der unbenutzten Transporter (" + actTransporters.size() + " Stück) dauerte: " + (end1-mid2) + "ms, Anzahl Comparatoraufrufe: " + this.transporterComparatorForOffer.getCallCount());
		}
		
		// Transporter durchgehen
		for (Iterator<Transporter> iter = actTransporters.iterator();iter.hasNext();){
			Transporter actTransporter = (Transporter)iter.next();
			if (actTransporter.getKapa_frei()>0){
				// diese Transporter bearbeiten
				// nur in dieser Region ?
				if (!onlyInRegion || actTransporter.getActDist()==0){
					this.processFreeTransporter(request, offer, actTransporter);
				}
			}
			// abbruch kriterium
			if (request.getForderung()<=0){
				break;
			}
		}		
		// Entweder Forderung abgearbeitet oder keine Transporter mehr verfügbar
	}
	
	/**
	 * Nutzt den angegebenen Transporter, um übergebenen Request durch den übergebenen
	 * offer zu befriedigen
	 * @param request
	 * @param offer
	 * @param transporter
	 */
	private void processFreeTransporter(TransportRequest request, TransportOffer offer, Transporter transporter){
		// wir haben vor uns einen freien Transporter
		// der soll Ladung aufnehmen oder sich zum Ladeort bewegen
		// Goto Befehl muss gleich noch umgesetzt werden....
		
		// check, wieviel kann dieser Transport mitnehmen?
		int Anzahl = Math.min(request.getForderung(), offer.getAnzahl_nochOfferiert());
		// kann verringert werden durch Kapa des Transporters?
		float weight = offer.getItem().getItemType().getWeight();
		int maxAnzahlTransport = Integer.MAX_VALUE;
		if (weight>0){
			maxAnzahlTransport = (int)Math.floor((transporter.getKapa_frei() / weight));
		}
		// Minimum bilden
		Anzahl = Math.min(Anzahl, maxAnzahlTransport);
		
		// check
		if (Anzahl<=0){
			return;
		}
		
		if (transporter.getActRegion().equals(offer.getRegion())){
			// wir sind da...Ziel auf Request setzen, Transporter beladen
			transporter.processRequest(request, offer, Anzahl,"TMo");
			
		} else {
			// wir sind nicht da Ziel auf Offer setzen -> Leerfahrt
			transporter.setToLeerFahrt(offer, request, Anzahl,"TMo");
		}
		// checken, ob dieser Transport andere (höher priorisierte, aber kleinere)
		// übernehmen kann
		this.checkTakeOverTransports(transporter);
		// transporter informieren
		// transporter.informUser(Anzahl, request, offer,"TMo");
		if (!this.reportOFF) {
			outText.addOutLine("neuer Trans: " + transporter.getScriptUnit().unitDesc() + " schafft " + Anzahl + ", noch offen:" + request.getForderung());	
		}
	}
	
	/**
	 * checkt, ob der transporter höher priorisierte Transporter (aber kleinere) 
	 * ersetzen kann
	 * @param transporter
	 */
	private void checkTakeOverTransports(Transporter transporter){
		/**
		 * wir haben hier einen Transport, der gerade sein nextDest bekommen hat
		 * 1) wir suchen transporter, die das gleichen nextDest haben und komplett
		 * übernommen werden können (später auch nur teilweise?....)
		 * 2) wir suche später auch transporter, die bis zum nächsten Halt übernommen werden 
		 * können ,wenn nextHold = destRegion ist
		 */
		
		if (!this.TMoptimize1){return;}
		
		// TRansporter auf parallelen Routen suchen, automatischer Modus
		for (Transporter searchTrans : this.transporters.values()){
			if (searchTrans.getActRegion().equals(transporter.getActRegion())
					&& searchTrans.getDestRegion()!=null 
					&& searchTrans.getDestRegion().equals(transporter.getDestRegion())
					&& transporter.getKapa_frei()>=(searchTrans.getKapa()-searchTrans.getKapa_frei())
					&& transporter.getKapa_frei_planung()>=(searchTrans.getKapa_planung()-searchTrans.getKapa_frei_planung())
					&& !searchTrans.equals(transporter)){
				
				// Extra Check
				// damit sicherstellen, dass auch planung übereinstimmt
				boolean optimizeIt=true;
				if (transporter.getVon_region_planung()!=null && (!transporter.getVon_region_planung().equals(searchTrans.getVon_region_planung())
						|| !transporter.getNach_region_planung().equals(searchTrans.getNach_region_planung()))){			
						optimizeIt=false;
				}
				if (optimizeIt){
					this.specialCounter++;
					searchTrans.unload2Transport(transporter);
				}
			}
		}
		
		
	}
	
	
	/**
	 * durchforstet bereits gesetzte Transporter, müssen in der Region sein, wo beladen wird
	 * @param request
	 * @param offer
	 */
	private void searchForAvailableUsedTransports(TransportRequest request,TransportOffer offer){
		// TransporterListe sortieren
		// Prio 1 : Entfernung  (entfällt)
		// Prio 2 : passende Kapa...falls 2 passen, den mit weniger Überhang
		// notwendig..benötigtes Gewicht
		int Anzahl = Math.min(request.getForderung(),offer.getAnzahl_nochOfferiert());
		if (Anzahl<=0){return;}
		float weight = offer.getItem().getItemType().getWeight();
		if (weight<0) {weight =0;}
		int weightInt = (int)Math.ceil((weight * Anzahl));
		
		if (this.transporterComparatorForOfferUsed==null){
			this.transporterComparatorForOfferUsed = new TransporterComparatorForOfferUsed(weightInt);
		} else {
			this.transporterComparatorForOfferUsed.setWeight(weightInt);
		}
		
		// Liste sortieren
		ArrayList<Transporter> actTransporters = new ArrayList<Transporter>(1);
		if (this.transporters!=null &&  !this.transporters.isEmpty()){
			for (Iterator<Transporter> iter = this.transporters.values().iterator();iter.hasNext();){
				Transporter T = (Transporter)iter.next();
				// nur Ts aufführen, die vergeben sind und in der aktuellen Region sind
				// und die auf Automatik stehen
				if (T.getActRegion().equals(offer.getRegion()) && T.getDestRegion()!=null && T.getMode()==Transporter.transporterMode_fullautomatic){
					actTransporters.add(T);
				}
			}
		}
		
		if (actTransporters.isEmpty()){
			// keine TRansporter verfügbar
			if (!this.reportOFF) {
				outText.addOutLine("keine benutzten Transporter verfügbar.");
			}
			return;
		}
		
		// neu Entfernung des nächsten Haltes zum Request setzen
		for (Iterator<Transporter> iter = actTransporters.iterator();iter.hasNext();){
			Transporter actTransporter = (Transporter)iter.next();
			Region r = null;
			if (actTransporter.getDestRegion()==null){
				r = actTransporter.getActRegion();
			} else {
				// schon bekannt...
				GotoInfo gotoInfo = actTransporter.getGotoInfo();
				if (gotoInfo==null || gotoInfo.getAnzRunden()<0){
					r = actTransporter.getActRegion();
				} else {
					r = gotoInfo.getNextHold();
				}
			}
			int dist = FFToolsRegions.getPathDistLand(this.scriptMain.gd_ScriptMain, r.getCoordinate(),offer.getRegion().getCoordinate(), true);
			actTransporter.setActDist(dist);
		}
		
		long start1 = System.currentTimeMillis();
		Collections.sort(actTransporters, this.transporterComparatorForOfferUsed);
		long end1 = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Sortieren der bereits benutzten Ts (" + actTransporters.size() + " Stück) benötigt: " + (end1-start1) + " ms. Anzahl Comparatoraufrufe: " + this.transporterComparatorForOfferUsed.getCallCount());
		}

		// Transporter durchgehen
		for (Iterator<Transporter> iter = actTransporters.iterator();iter.hasNext();){
			Transporter actTransporter = (Transporter)iter.next();
			if (actTransporter.getKapa_frei()>0){
				// diese Transporter bearbeiten
				this.processUsedTransporter(request, offer, actTransporter);
			}
			// abbruch kriterium
			if (request.getForderung()<=0){
				break;
			}
		}		
		// Entweder Forderung abgearbeitet oder keine Transporter mehr verfügbar
	}
	
	/**
	 * durchforstet bereits gesetzte Transporter auf Leerfahrt, auf dem Weg zur Offer
	 * @param request
	 * @param offer
	 */
	private void searchForAvailableLeerfahrtTransports_Planungsziel(TransportRequest request,TransportOffer offer){
		// TransporterListe sortieren
		// Prio 1 : Entfernung  (entfällt)
		// Prio 2 : passende Kapa...falls 2 passen, den mit weniger Überhang
		// notwendig..benötigtes Gewicht
		int Anzahl = Math.min(request.getForderung(),offer.getAnzahl_nochOfferiert());
		if (Anzahl<=0){return;}
		float weight = offer.getItem().getItemType().getWeight();
		if (weight<0) {weight =0;}
		int weightInt = (int)Math.ceil((weight * Anzahl));
		
		if (this.transporterComparatorForOfferUsed==null){
			this.transporterComparatorForOfferUsed = new TransporterComparatorForOfferUsed(weightInt);
		} else {
			this.transporterComparatorForOfferUsed.setWeight(weightInt);
		}
		
		// Liste sortieren
		ArrayList<Transporter> actTransporters = new ArrayList<Transporter>(1);
		if (this.transporters!=null &&  !this.transporters.isEmpty()){
			for (Iterator<Transporter> iter = this.transporters.values().iterator();iter.hasNext();){
				Transporter T = (Transporter)iter.next();
				// nur Ts aufführen, die vergeben sind und als Ziel die offer haben und als Planungsziel die request
				// und die auf Automatik stehen
				if (T.samePlanungOK(offer, request)){
					actTransporters.add(T);
				}
			}
		}
		
		if (actTransporters.isEmpty()){
			// keine TRansporter verfügbar
			if (!this.reportOFF) {
				outText.addOutLine("keine Leerfahrten verfügbar.");
			}
			return;
		}
		
		// Entfernung von actRegion zur Offer setzen
		for (Iterator<Transporter> iter = actTransporters.iterator();iter.hasNext();){
			Transporter actTransporter = (Transporter)iter.next();
			Region r = actTransporter.getActRegion();
			int dist = FFToolsRegions.getPathDistLand(this.scriptMain.gd_ScriptMain, r.getCoordinate(),offer.getRegion().getCoordinate(), actTransporter.isRiding());
			actTransporter.setActDist(dist);
		}
		
		long start1 = System.currentTimeMillis();
		Collections.sort(actTransporters, this.transporterComparatorForOfferUsed);
		long end1 = System.currentTimeMillis();
		if (!this.reportOFF) {
			outText.addOutLine("Sortieren der Leerfahrten (" + actTransporters.size() + " Stück) benötigt: " + (end1-start1) + " ms. Anzahl Comparatoraufrufe: " + this.transporterComparatorForOfferUsed.getCallCount());
		}

		// Transporter durchgehen
		for (Iterator<Transporter> iter = actTransporters.iterator();iter.hasNext();){
			Transporter actTransporter = (Transporter)iter.next();
			if (actTransporter.getKapa_frei_planung()>0){
				// diese Transporter bearbeiten
				this.processLeerfahrtTransporter(request, offer, actTransporter);
			}
			// abbruch kriterium
			if (request.getForderung()<=0){
				break;
			}
		}		
		// Entweder Forderung abgearbeitet oder keine Transporter mehr verfügbar
	}
	
	/**
	 * Nutzt den angegebenen Transporter, um übergebenen Request durch den übergebenen
	 * offer zu befriedigen
	 * @param request
	 * @param offer
	 * @param transporter
	 */
	private void processUsedTransporter(TransportRequest request, TransportOffer offer, Transporter transporter){
		// wir haben vor uns einen Transporter mit gesetztem Ziel
		// der soll Ladung aufnehmen oder sich zum Ladeort bewegen
		// Goto Befehl muss nicht noch umgesetzt werden....
		
		
		// sicherheitscheck: wir müssen schon näher ran ans Ziel, sonst hats keinen Zweck.
		if (transporter.getGotoInfo()==null){
			return;
		}
		
		GotoInfo gotoInfo = transporter.getGotoInfo();
		if (gotoInfo==null){
			return;
		}
		
		boolean reitend = false;
		if (transporter.isRiding()){
			reitend = true;
		}
		
		int distOffer = FFToolsRegions.getPathDistLand(offer.getScriptUnit().getScriptMain().gd_ScriptMain, 
					offer.getRegion().getCoordinate(), request.getRegion().getCoordinate(), 
						reitend);
		
		if (gotoInfo==null || gotoInfo.getNextHold()==null) {
			// für die Fälle, indenen kein Weg gefunden werden konnte
			return;
		}
		
		
		int distNextHold = FFToolsRegions.getPathDistLand(offer.getScriptUnit().getScriptMain().gd_ScriptMain, 
				gotoInfo.getNextHold().getCoordinate(), request.getRegion().getCoordinate(), 
					reitend);
		
		if (distOffer<=distNextHold){
			// hat keinen sinn
			return;
		}
		
		// check, wieviel kann dieser Transport mitnehmen?
		int Anzahl = Math.min(request.getForderung(), offer.getAnzahl_nochOfferiert());
		// kann verringert werden durch Kapa des Transporters?
		float weight = offer.getItem().getItemType().getWeight();
		int maxAnzahlTransport = Integer.MAX_VALUE;
		if (weight>0){
			maxAnzahlTransport = (int)Math.floor((transporter.getKapa_frei() / weight));
		}
		// Minimum bilden
		Anzahl = Math.min(Anzahl, maxAnzahlTransport);
		
		// check
		if (Anzahl<=0){
			return;
		}
		
		
		transporter.processRequest(request, offer, Anzahl,"TMu");
		
		// transporter informieren
		// transporter.informUser(Anzahl, request, offer,"TMu");
		if (!this.reportOFF) {
			outText.addOutLine("Benutze Trans: " + transporter.getScriptUnit().unitDesc() + ", schafft " + Anzahl + ", noch offen:" + request.getForderung());
		}
	}
	
	
	/**
	 * Nutzt den angegebenen Transporter, um übergebenen Request durch den übergebenen
	 * offer zu befriedigen
	 * Transporter auf Leerfahrt...alles nur geplant!
	 * @param request
	 * @param offer
	 * @param transporter
	 */
	private void processLeerfahrtTransporter(TransportRequest request, TransportOffer offer, Transporter transporter){
		// wir haben vor uns einen Transporter mit gesetztem Ziel
		// der soll Ladung aufnehmen oder sich zum Ladeort bewegen
		// Goto Befehl muss nicht noch umgesetzt werden....
		
		// check, wieviel kann dieser Transport mitnehmen?
		int Anzahl = Math.min(request.getForderung(), offer.getAnzahl_nochOfferiert());
		// kann verringert werden durch Kapa des Transporters?
		float weight = offer.getItem().getItemType().getWeight();
		int maxAnzahlTransport = Integer.MAX_VALUE;
		if (weight>0){
			maxAnzahlTransport = (int)Math.floor((transporter.getKapa_frei_planung() / weight));
		}
		// Minimum bilden
		Anzahl = Math.min(Anzahl, maxAnzahlTransport);
		
		// check
		if (Anzahl<=0){
			return;
		}
		
		transporter.processLeerfahrtRequest(request, offer, Anzahl,"TMo2");
		
		// transporter informieren
		// transporter.informUser(Anzahl, request, offer,"TMo2");
		if (!this.reportOFF) {
			outText.addOutLine("Benutze Leerfahrt: " + transporter.getScriptUnit().unitDesc() + ", schafft " + Anzahl + ", noch offen:" + request.getForderung());
		}
	}
	
	
	/**
	 * request und offer in der gleichen region, kein Transport notwendig
	 * einzige handlung: request reduzieren, offer reduzieren
	 * @param request
	 * @param offer
	 */
	private void processSameRegion(TransportRequest request,TransportOffer offer){
		int anzahl = Math.min(request.getForderung(), offer.getAnzahl_nochOfferiert());
		if (anzahl>0) {
			request.incBearbeitet(anzahl);
			request.getScriptUnit().addComment("TM: von dieser Region: " + anzahl + " " + offer.getItemName() + " (Prio " + request.getPrio() + " von " + offer.getScriptUnit().getUnit().toString(true) + ")");
			offer.incBearbeitet(anzahl);
			offer.getScriptUnit().addComment("TM: für diese Region: " + anzahl + " " + offer.getItemName()+ " (Prio " + request.getPrio() + " für " + request.getScriptUnit().getUnit().toString(true) + ")");
			if (!this.reportOFF) {
				outText.addOutLine("Bereits in " + request.getRegion().toString() + ": " + anzahl + " " + offer.getItemName() + ". Noch offen:" + request.getForderung());
			}
		}
	}
	
	
	
	/**
	 * erstellt aus den gelisteten Depots alle offers
	 *
	 */
	private void createOffers(){
		if (this.transportOffers==null){
			this.transportOffers=new ArrayList<TransportOffer>();
		} else {
			this.transportOffers.clear();
		}
		if (this.depots==null || this.depots.size()==0){
			return;
		}
		
		for (Iterator<ScriptUnit> iter = this.depots.values().iterator();iter.hasNext();){
			ScriptUnit sU = (ScriptUnit)iter.next();
			ArrayList<Item> list = new ArrayList<Item>();
			
			if (sU.getModifiedItemsMatPool2()!=null){
				for (Iterator<Item> it = sU.getModifiedItemsMatPool2().values().iterator();it.hasNext();){
					list.add((Item)it.next());
				}
			}
			for (Iterator<Item> iterItem = list.iterator();iterItem.hasNext();){
				Item actItem = (Item)iterItem.next();
				int actAmount = actItem.getAmount();
				if (actAmount>0){	
					// von dem Betrag die selbst angeforderten Items
					// oberhalb Default abziehen
					actAmount = Math.max(0,actAmount - this.depotForderungen(sU,actItem.getItemType()));
					if (actAmount>0){
						TransportOffer newOffer = new TransportOffer(sU,actItem,actAmount);
						this.transportOffers.add(newOffer);
					}
				}
			}
		}
	}
	
	
	private int depotForderungen(ScriptUnit su,ItemType itemType){
		int erg = 0;
		MatPool MP = su.getOverlord().getMatPoolManager().getRegionsMatPool(su);
		ArrayList<MatPoolRequest> list = MP.getRequests(su);
		if (list!=null && list.size()>0){
			for (Iterator<MatPoolRequest> iter = list.iterator();iter.hasNext();){
				MatPoolRequest MPR = (MatPoolRequest)iter.next();
				if (MPR.containsItemtype(itemType) && MPR.getPrio() > Depot.default_request_prio ){
					erg+=MPR.getOriginalGefordert();
				}
			}
		}
		
		return erg;
	}
	
	
	/**
	 * Overlord Info
	 */
	public int[] runAt(){
		return runners;
	}

	/**
	 * @return the scriptMain
	 */
	public ScriptMain getScriptMain() {
		return this.scriptMain;
	}
	
	
	public void listRequests(ArrayList<TransportRequest> requests){
		for (Iterator<TransportRequest> iter = requests.iterator();iter.hasNext();){
			TransportRequest TR = (TransportRequest)iter.next();
			String s = "";
			s += TR.getForderung() + "(von " + TR.getOriginalGefordert() + ") ";
			s += TR.getOriginalGegenstand();
			s += " nach " + TR.getRegion().toString();
			s += " PRIO:" + TR.getPrio();
			s += " (" + TR.getKommentar() + ")";
			if (!this.reportOFF) {
				outText.addOutLine(s);
			}
		}
	}
	
	public void listOffers(ArrayList<TransportOffer> offers){
		for (Iterator<TransportOffer> iter = offers.iterator();iter.hasNext();){
			TransportOffer TO = (TransportOffer)iter.next();
			String s = "";
			s += TO.getItem().getAmount() + "(" + TO.getAnzahl_nochOfferiert() + ") ";
			s += TO.getItem().getName();
			s += " in " + TO.getRegion().toString();
			if (!this.reportOFF) {
				outText.addOutLine(s);
			}
		}
	}
	
	public void initReportSettings(ReportSettings reportSettings){
		if (reportSettings.getOptionBoolean("TMoptimize1")){
			this.TMoptimize1=true;
		}
		if (reportSettings.getOptionBoolean("TMoptimize2")){
			this.TMoptimize2=true;
		}
	}
	
	/**
	 * Versucht, für diese Transporter Pferde aufzutreiben
	 * wenn alles scheitert, sitzt der Transporter selber Lernbefehl!
	 * @param T
	 */
	public void erstPferde(Transporter T){
		if (this.pferdeDepots==null){
			this.buildPferdeDepots();
		}
		if (this.pferdeDepots.size()==0){
			T.getScriptUnit().addComment("Keine Depots mit Pferden im TM");
			return;
		}
		// TA des TRansporters
		TradeArea transporterTA = this.scriptMain.getOverlord().getTradeAreaHandler().getTAinRange(T.getActRegion());
		// pferdedepots durchlaufen
		for (ScriptUnit su : this.pferdeDepots.keySet()){
			Integer anzI = this.pferdeDepots.get(su);
			if (anzI.intValue()>=T.getTransporterErstPferdeForderung()){
				// mögliches Depot mit ausreichend Pferden
				// gleiches TA?
				TradeArea depotTA = this.scriptMain.getOverlord().getTradeAreaHandler().getTAinRange(su.getUnit().getRegion());
				if (depotTA.equals(transporterTA) && !T.getActRegion().equals(su.getUnit().getRegion())){
					// Treffer! -> dahin!
					T.setDestRegion(su.getUnit().getRegion());
					T.processDestRegion();
					int ETA = T.getGotoInfo().getAnzRunden();
					su.addComment("TM_Pferde (ETA " + ETA +"): " + T.getTransporterErstPferdeForderung() + " Pferde vorgemerkt für: " + T.getScriptUnit().unitDesc(),false);
					T.getScriptUnit().addComment(T.getTransporterErstPferdeForderung() + " Pferde vorgemerkt in: " + su.getUnit().getRegion().toString(),false);
					T.getScriptUnit().addComment("ETA bei den Pferden: " + ETA + " Runden",false);
					int anzNeu = anzI.intValue() - T.getTransporterErstPferdeForderung();
					this.pferdeDepots.put(su, new Integer(anzNeu));
					return;
				}
			}
		}
		T.getScriptUnit().addComment("Keine Pferde im TA " + transporterTA.getName() + " gefunden.");
	}
	
	/**
	 * Baut die HashTable mit verfügbaren Pferdezahlen in den Depots auf
	 * betrifft alle depots des TMs, nicht nur eines TA! 
	 *
	 */
	private void buildPferdeDepots(){
		if (this.pferdeDepots!=null){
			this.pferdeDepots.clear();
		} else {
			this.pferdeDepots = new Hashtable<ScriptUnit, Integer>();
		}
		if (this.depots==null){
			return;
		}
		ItemType pferdeType = this.scriptMain.gd_ScriptMain.getRules().getItemType("Pferd", false);
		if (pferdeType==null){
			return;
		}
		for (ScriptUnit depot : this.depots.values()){
			Item pferdeItem = depot.getModifiedItem(pferdeType);
			int anzahl = 0;
			if (pferdeItem!=null){
				anzahl=pferdeItem.getAmount();
			}
			if (anzahl>0){
				this.pferdeDepots.put(depot,new Integer(anzahl));
			}
		}
		
	}
	
	/**
	 * bereitet Request-Statistim vor und kommentiert diese zur Unit
	 * @param su
	 */
	public void statistik_Requests(ScriptUnit su){
		HashMap<String,TransportStatistikEintrag> liste = new HashMap<String,TransportStatistikEintrag>();
		// alle Requests durchgehen
		TradeArea TA = su.getOverlord().getTradeAreaHandler().getTAinRange(su.getUnit().getRegion());
		if (TA==null){
			su.addComment("no statistik available - no TA found.");
			return;
		}
		su.addComment("checking Requests in TA " + TA.getName());
		int cntI = 0;
		long time1 = System.currentTimeMillis();
		for (TransportRequest TR:this.transportRequests){
			// Schon vorhanden
			// nur die nicht *alle* und nicht mit prio 1
			// *alle* werden gar nicht an den TM weiter gebeben
			// und wir brauchen alle die, die auch im gleichen TA sind
			boolean isGoodRequest=true;
			if (TR.getPrio()<200){isGoodRequest=false;}
			
			if (!TA.includesRegion(TR.getRegion())){
				isGoodRequest=false;
			}
			
			if (TR.getForderung()==0){
				isGoodRequest=false;
			}
			
			if (isGoodRequest){
				TransportStatistikEintrag TSE = null;
				if (!liste.containsKey(TR.getOriginalGegenstand().toLowerCase())){
					TSE = new TransportStatistikEintrag(TR.getOriginalGegenstand().toLowerCase());
					liste.put(TR.getOriginalGegenstand().toLowerCase(),TSE);
				} else {
					TSE = liste.get(TR.getOriginalGegenstand().toLowerCase());
				}
				
				TSE.add(TR.getBearbeitet(),TR.getForderung());
				cntI+=1;
			}
		}
		su.addComment("Found " + liste.keySet().size() + " different items. (checked " + cntI + " Requests)");
		// Sortieren
		ArrayList<TransportStatistikEintrag> sortList = new ArrayList<TransportStatistikEintrag>();
		sortList.addAll(liste.values());
		Collections.sort(sortList);
		for (TransportStatistikEintrag TE:sortList){
			String ss = TE.getGegenstand() + ": offen " + TE.getAnzahl_offen() + ", " + TE.getAnzahl_bearbeitet() + " bearbeitet. ";
			int available = 0;
			int off = 0;
			// itemgroup
			if (reportSettings.getItemTypes(TE.getGegenstand())!=null){
				String ss2 = "(";
				for (ItemType IT : reportSettings.getItemTypes(TE.getGegenstand())){
					off = this.offerd_Items(IT, TA);
					available+=off;
					ss2 += IT.getName() + "[" + off + "]";
				}
				ss2 = "vorhanden: " + available + " (" + ss2 + ")";
				ss += ss2;
			}
			
			// direkt ein Item
			ItemType IT2 = this.scriptMain.gd_ScriptMain.getRules().getItemType(TE.getGegenstand(), false);
			if (IT2!=null){
				ss += " vorhanden: " + this.offerd_Items(IT2, TA) + "."; 
			}
			
			su.addComment(ss);
		}
		long time2 = System.currentTimeMillis();
		long tDiff = time2 - time1;
		su.addComment("Request-Statistik benötigte " + tDiff + "ms.");
	}
	
	/**
	 * liefert Anzahl noch offerierter Items im TA
	 * @param IT
	 * @param TA
	 * @return
	 */
	private int offerd_Items(ItemType IT, TradeArea TA){
		int erg = 0;
		
		for (TransportOffer TO:this.transportOffers){
			boolean isGoodOffer=true;
			if (!TA.includesRegion(TO.getRegion())){
				isGoodOffer=false;
			}
			if (isGoodOffer && TO.getAnzahl_nochOfferiert()<=0){
				isGoodOffer=false;
			}
			if (isGoodOffer && !TO.getItem().getItemType().equals(IT)){
				isGoodOffer=false;
			}
			if (isGoodOffer){
				erg+=TO.getAnzahl_nochOfferiert();
			}
		}
		
		return erg;
	}
	
	
	/**
	 * bereitet Request-Statistim vor und kommentiert diese zur Unit
	 * @param su
	 */
	public void statistik_Transporter(ScriptUnit su){
		
		// alle Requests durchgehen
		TradeArea TA = su.getOverlord().getTradeAreaHandler().getTAinRange(su.getUnit().getRegion());
		if (TA==null){
			su.addComment("no statistik available - no TA found.");
			return;
		}
		su.addComment("checking Transporter in TA " + TA.getName());
		long time1 = System.currentTimeMillis();
		
		// Gesamtanzahl (Personen, Einheiten, Pferde)
		// wieviele Leerfahrten
		// Auslastung
		// geringste Prio als erstAuslöser... (? geht dass?)
		int Gesamtanzahl_Einheiten = 0;
		int Gesamtanzahl_Personen = 0;
		int Gesamtanzahl_Pferde = 0;
		int Gesamtanzahl_Wagen = 0;
		int Anzahl_Leerfahrten = 0;
		int Anzahl_Iddle = 0;
		double MaxKapa_alle_Fahrten = 0;
		double FreieKapa_alle_Fahrten = 0;
		double MaxKapa_keine_Leerfahrten = 0;
		double FreieKapa_keine_Leerfahrten = 0;
		
		ItemType PferdType = this.scriptMain.gd_ScriptMain.getRules().getItemType("Pferd", false);
		ItemType WagenType = this.scriptMain.gd_ScriptMain.getRules().getItemType("Wagen", false);
		
		for (Transporter T:this.allTransporters.values()){
			if (TA.includesRegion(T.getActRegion())){
				Gesamtanzahl_Einheiten+=1;
				Gesamtanzahl_Personen+=T.getScriptUnit().getUnit().getModifiedPersons();
				Item i = T.getScriptUnit().getModifiedItem(PferdType);
				if (i!=null){
					Gesamtanzahl_Pferde+=i.getAmount();
				}
				i = T.getScriptUnit().getModifiedItem(WagenType);
				if (i!=null){
					Gesamtanzahl_Wagen+=i.getAmount();
				}
				if (T.getTransporterRequests()==null || T.getTransporterRequests().size()==0){
					// noch komplett frei...leerfahrt
					Anzahl_Leerfahrten+=1;
				} else {
					// Kapa keine Leerfahrten
					MaxKapa_keine_Leerfahrten += T.getKapa();
					FreieKapa_keine_Leerfahrten += T.getKapa_frei();
				}
				if (T.getGotoInfo()==null){
					Anzahl_Iddle+=1;
				} else {
					// Kapa alle Fahrten
					MaxKapa_alle_Fahrten += T.getKapa();
					FreieKapa_alle_Fahrten += T.getKapa_frei();
				}
			}
		}
		
		NumberFormat NF = NumberFormat.getInstance();
		NF.setMaximumFractionDigits(1);
		NF.setMinimumFractionDigits(0);
		NF.setMinimumIntegerDigits(1);
		NF.setMaximumIntegerDigits(3);
		
		su.addComment("Anzahl Transporter: " + Gesamtanzahl_Einheiten);
		su.addComment("Anzahl Personen: " + Gesamtanzahl_Personen);
		su.addComment("Pferde: " + Gesamtanzahl_Pferde + ", Wagen: " + Gesamtanzahl_Wagen);
		su.addComment("Leerfahrten: " + Anzahl_Leerfahrten + ", unbeschäftigt: " + Anzahl_Iddle);
		su.addComment("Auslastung bewegte Transporter: " + NF.format(((MaxKapa_alle_Fahrten - FreieKapa_alle_Fahrten)/MaxKapa_alle_Fahrten)*100) + "%");
		su.addComment("Auslastung beladene Transporter: " + NF.format(((MaxKapa_keine_Leerfahrten - FreieKapa_keine_Leerfahrten)/MaxKapa_keine_Leerfahrten)*100) + "%");
		
		long time2 = System.currentTimeMillis();
		long tDiff = time2 - time1;
		su.addComment("Transporter-Statistik benötigte " + tDiff + "ms.");
	}
}
