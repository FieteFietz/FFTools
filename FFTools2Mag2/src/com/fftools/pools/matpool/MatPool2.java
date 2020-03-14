package com.fftools.pools.matpool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import magellan.library.Item;
import magellan.library.Region;
import magellan.library.gamebinding.EresseaRelationFactory;
import magellan.library.rules.ItemCategory;
import magellan.library.rules.ItemType;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolOffer;
import com.fftools.pools.matpool.relations.MatPoolRelation;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Depot;
import com.fftools.transport.TransportRequest;


/**
 * Zentrale Abwicklung des Materialpools
 * 
 * @author Fiete
 *
 *Marc 20070104: Umgestellt auf Verwendung der methoden get/setAnzahlBearbeitet()
 *Fiete 20080305: Neubau MatPool2
 *
 */
public class MatPool2 implements MatPool{
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	public static final String depotIdComment = ";DEPOT";
	
	private static final int pferdeOffset=20000;
	private static final int wagenOffset= 10000;


	/**
	 * enthaelt die grundlegende Region
	 */
	private Region region = null;
	
	/**
	 * alle in der Region bekannten requests
	 */
	public ArrayList<MatPoolRequest> requests = null;
	
	/**
	 * alle in der Region bekannten offers
	 */
	public ArrayList<MatPoolOffer> offers = null;
	
	/**
	 * Für jeden OriginalGegenstand eine Collection von (allen) Offers
	 * auf das Raussuchen der "passenden" Offers wird verzichtet
	 * jede ArrayList wird nur 1 mal sortiert!
	 * 
	 * String: OriginalGegenstand des Requests
	 * 
	 */
	private HashMap<String,ArrayList<MatPoolOffer>> gegenstandOffers = null;
	
	
	/**
	 * zum schnellen finden der offers einer unit
	 * zum check der selfGratification
	 */
	private HashMap<ScriptUnit,ArrayList<MatPoolOffer>> scriptUnitOffers = null;
	
	/**
	 * enthaelt alle ScriptUnits dieser Region
	 */
	private ArrayList<ScriptUnit> matPoolUnits = null;
	
	
	/**
	 * Referenz auf den MatPoolManager
	 */
	public MatPoolManager matPoolManager = null;
	
	private MatPoolOfferComparatorAbstractRequest matPoolOfferComparatorAbstractRequest = null;
	
	private MatPoolOfferComparatorForSort matPoolOfferComparatorForSort = null;
	
	/**
	 * wenn depot in MatPoolRegion, dann hier drinne
	 */
	private ScriptUnit depotUnit = null;
	
	/**
	 * so lange falsch, bis alle units mal alles geoffert hatten und depots 
	 */
	private boolean generatedOffers = false;
	
	/**
	 * nach dem ersten udn einzigen region.refreshRelations zwishcen speichern der sich 
	 * ergebenen modifiedItem szur nutzung bei reset des MatPools2
	 */
	private HashMap<ScriptUnit,HashMap<ItemType,Item>> originalModifiedScriptUnitItems = new HashMap<ScriptUnit, HashMap<ItemType,Item>>();
	
	private int timeSortOffers = 0;
	private int timeProcessOffers = 0;
	
	/**
	 * Legt neuen MatPool an und setzt die Region, Listen noch undef (Null)
	 */
	public MatPool2(MatPoolManager _mpm,Region r){
		this.region = r;
		this.matPoolManager = _mpm;
	}
	
	/**
	 * Registriert uebergebene ScriptUnit bei diesem MatPool
	 * wenn sie nicht schon registriert worden ist....
	 * @param _u
	 */
	public void addScriptUnit(ScriptUnit _u){
		if (this.matPoolUnits==null){
			this.matPoolUnits = new ArrayList<ScriptUnit>();
			this.matPoolUnits.add(_u);
		} else {
			// es gibt schon matPoolUnits...meine dabei ?
			if (!this.matPoolUnits.contains(_u)) {
				this.matPoolUnits.add(_u);
			}
		}
	}
	
	
	
	/**
	 * 
	 * reseten der Relations
	 */	
	public void resetRelations(){
		// alle Listen loeschen + neu
		if (this.requests != null) {
			for (Iterator<MatPoolRequest> iter1 = this.requests.iterator();iter1.hasNext();){
				MatPoolRelation mpr = (MatPoolRequest) iter1.next();
				mpr.reset();
			}
		}
		
		if (this.offers != null) {
			for (Iterator<MatPoolOffer> iter1 = this.offers.iterator();iter1.hasNext();){
				MatPoolRelation mpr = (MatPoolOffer) iter1.next();
				mpr.reset();
			}
		}
		if (this.gegenstandOffers!=null){
			this.gegenstandOffers.clear();
		}
		// die modifiedMaps bei den ScriptUnits
		// wird durch originaldaten nach region.refreshRelataions ersetzt
		this.resetModifiedItemsMatPool2();
		
	}
	
	/**
	 * poolt die Region: Versucht, alle Requests anhand der Offers zu bedienen
	 */
	public void runPool(int durchlauf) {
		
		long startT = System.currentTimeMillis();
        //	Löschen der ungeschützten orders im Zusammenhang mit GIB, Reserviere
		// dass machen wir immer, dann können wir immer gleich GIB orders schreiben...
		this.cleanOrders();
		this.timeProcessOffers=0;
		this.timeSortOffers = 0;
		// haben schon die units alles geoffert?
		// gleichzeitig check, ob wir im ersten Lauf sind!
		if (!this.generatedOffers){
			// Hier relations reset der Region
			// this.region.refreshUnitRelations(true);
			
			// this.matPoolManager.scriptMain.gd_ScriptMain.getGameSpecificStuff().getRelationFactory().createRelations(this.region);
			EresseaRelationFactory ERF = ((EresseaRelationFactory) this.matPoolManager.scriptMain.gd_ScriptMain.getGameSpecificStuff().getRelationFactory());
			ERF.processRegionNow(this.region);
			// modified setzen
			this.firstInitialModifiedItems();
			// für verbliebene Gegenstände offers generieren
			this.offerEverything();
			this.generatedOffers=true;
			long endT1 = System.currentTimeMillis();
			if (!this.matPoolManager.isReportOFF()) {
				outText.addOutLine("**** " + this.region.toString() + " - First inits took: " + (endT1-startT) + " ms");
			}
		}
		
		if (this.requests== null){return;}
		if (this.offers== null){return;}
		
		String s = "MatPool2 " + this.region.toString()+": ";
		if (this.requests!=null){
			s+=this.requests.size();
		} else {
			s+="0";
		}
		s+=" Requests, ";
		if (this.offers!=null){
			s+=this.offers.size();
		} else {
			s+="0";
		}
		s+=" Offers, ";
		
		if (!this.matPoolManager.isReportOFF()) {
			outText.addOutLine(s);
		}
		
		
		
		// intern die vorarbeiten
		// resettet requests und offers
		// resettet modifiedItemsMatPool2 bei Scriptunits
		this.resetRelations();
		// Offeset für Pferde und Wagen
		this.preRequestOffsets(true);
		// Sortieren der Requests
		
		/*
		if (this.getRegion().getName()!=null && this.getRegion().getName().equalsIgnoreCase("Kusufybín")){
			int iiii=2;
		}
		*/
		Collections.sort(this.requests);
	
		// zurücksetzen der Offsets, werden nicht mehr benötigt, da requests nicht 
		// erneut sortiert werden
		this.preRequestOffsets(false);
		
		
		// Request werden 1. nach Prio sortiert, dann nach ReihenfolgeNummer
		// dann nach Grösse der Forderung
		// hier nun nach der Sortierung bisher nicht vergebene ReihenfolgeNummern
		// vergeben
		int actReihenfolge = 1;
		for (Iterator<MatPoolRequest> iter = this.requests.iterator();iter.hasNext();){
			MatPoolRequest actMPR = (MatPoolRequest)iter.next();
			if (actMPR.getReihenfolgeNummer()==Integer.MAX_VALUE){
				actMPR.setReihenfolgeNummer(actReihenfolge);
			}
			actReihenfolge++;
		}
		
		
		long endT2 = System.currentTimeMillis();
		if (!this.matPoolManager.isReportOFF()) {
			outText.addOutLine("ms to process requests: " + (endT2-startT) + " ms");
		}
		
		// alle Requests durchlaufen, sortierung stellt Priorisierung sicher, 
		for (MatPoolRequest actRequest : this.requests){
			/*
			if 	(actRequest.getReihenfolgeNummer()==1488){
				int jjjj = 200;
			}
			if (actRequest.getOriginalGegenstand().equalsIgnoreCase("Silber")){
				int jjjj2 = 200;
			}
			*/
			this.processRequest(actRequest);
			outText.addPoint();
		}
		
		
		long endT = System.currentTimeMillis();
		if (!this.matPoolManager.isReportOFF()) {
			outText.addOutLine("MatPool2 " + region.toString() + ": " + (endT-startT) + " ms");
			outText.addOutLine("ms for sorting offers: " + this.timeSortOffers + " ms, for processing:" + this.timeProcessOffers+ " ms");
		}
		
	}
	
	/**
	 * Versucht, den uebergebenen Request zu bearbeiten
	 * der Bedarf kann abhängen von dem ItemType und damit von der entsprechenden Offer
	 * und muss entsprechend zu jeder Offer neu berechnet werden.
	 * @param actRequest
	 */
	private void processRequest(MatPoolRequest actRequest){
		// Liste aller Offers, die während der Bearbeitung eines Request
		// als bereits vollständig erledigt erkannt werden
		ArrayList<MatPoolOffer>fullfilledOffers=null;
		
		// existiert denn schon die HashMap mit den Collections?
		if (this.gegenstandOffers==null){
			this.gegenstandOffers = new HashMap<String, ArrayList<MatPoolOffer>>();
		}
		
		// enthält die HashMap schon eine nutzbare (und sortierte Collection?)
		ArrayList<MatPoolOffer> actOfferList = this.gegenstandOffers.get(actRequest.getOriginalGegenstand());
		if (actOfferList==null){
			// es existiert noch keine Liste
			actOfferList = new ArrayList<MatPoolOffer>();
			// doch nur die auf die Liste, die etwas für den Gegenstand tun können
			for (Iterator<MatPoolOffer> iter = this.offers.iterator();iter.hasNext();){
				MatPoolOffer thisOffer = (MatPoolOffer)iter.next();
				if (actRequest.containsItemtype(thisOffer.getItemType())){
					actOfferList.add(thisOffer);
				}
			}
			
			
			// neue Liste anfügen
			this.gegenstandOffers.put(actRequest.getOriginalGegenstand(), actOfferList);
		} 
		
		long startT1 = System.currentTimeMillis();
		// Liste sortieren
		// offers sortieren, falls abstrakter Begriff
		if (reportSettings.isInCategories(actRequest.getOriginalGegenstand())){
			// yep, wir müssen sortieren
			if (this.matPoolOfferComparatorAbstractRequest==null){
				this.matPoolOfferComparatorAbstractRequest = 
					new MatPoolOfferComparatorAbstractRequest(actRequest.getOriginalGegenstand(),actRequest.getScriptUnit());
			} else {
				this.matPoolOfferComparatorAbstractRequest.setParameter(actRequest.getOriginalGegenstand(),actRequest.getScriptUnit());
			}
			Collections.sort(actOfferList,this.matPoolOfferComparatorAbstractRequest);
		} else {
			// kein abstrakter, trotzdem sortieren, damit zuerst offers der eigenen
			// einheit abgearbeitet werden bei gleicher prio
			// das wird zwar durch die selfGratification unnötig, schadet aber auch nix
			// denn der Comparator sortiert standardmässig nach offer.getAngebot
			if (this.matPoolOfferComparatorForSort==null){
				this.matPoolOfferComparatorForSort = 
					new MatPoolOfferComparatorForSort(actRequest.getScriptUnit());
			} else {
				this.matPoolOfferComparatorForSort.setRequestingScriptUnit(actRequest.getScriptUnit());	
			}
			Collections.sort(actOfferList,this.matPoolOfferComparatorForSort);
		}
		long endT1 = System.currentTimeMillis();
		this.timeSortOffers+=(endT1-startT1);
		
		for (Iterator<MatPoolOffer> iter = actOfferList.iterator();iter.hasNext();){
			MatPoolOffer actOffer = (MatPoolOffer)iter.next();
			if (actOffer.getAngebot()>0 && actRequest.containsItemtype(actOffer.getItemType())){
				
				ItemType actItemType = actOffer.getItemType();

				// aktuelle Forderung nach diesem ItemType
				int actForderung = actRequest.getForderung(actItemType);
				if (actForderung<=0){
					// abbruch, eventuell auch gewichtsbeschränkung!
					break;
				}
				
				
				this.checkSelfGratification(actRequest, actOffer);
				
				// schneller werden
				if (actRequest.getBearbeitet()>=actRequest.getOriginalGefordert()){
					// return;
					break;
				}

				actForderung = actRequest.getForderung(actItemType);
				if (actForderung<=0){
					// abbruch, eventuell auch gewichtsbeschränkung!
					break;
				}
				
				
				// aktuelles Angebot
				int actAngebot = actOffer.getAngebot();
				
				// Forderung und Angebot müssen beide grösser 0 sein
				if (actForderung >0 && actAngebot>0){
					// OK..dann mal los
					int Transfer = Math.min(actForderung, actAngebot);				
					String s = "GIB " + actRequest.getUnit().toString(false) + " ";
					// Einschub...wenn Leerzeichen im Namen müssen Anführungszeichen gesetzt werden
					String checkIt = actOffer.getItemType().getName();
					if (checkIt.indexOf(" ")>0) {
						checkIt = "\"" + checkIt + "\"";
					}
					s = s.concat(Transfer + " " + checkIt);
					s = s.concat(" ; " + actRequest.getKommentar());
					s = s.concat("(P:" + actRequest.getPrio() + ")");
					// bei unit setzen
					
					// falls Unit gleich, auskommentieren
					if (actOffer.getScriptUnit().equals(actRequest.getScriptUnit())){
						s = "; selber: " + s;
					}
					
					
					actOffer.getScriptUnit().addOrder(s, false);
					
					// offer: GIB + bearbeitet
					actOffer.incBearbeitet(Transfer);
					
					// requester: bearbeitet
					actRequest.incBearbeitet(Transfer,actItemType);
					
					// schneller werden
					if (actRequest.getBearbeitet()>=actRequest.getOriginalGefordert()){
						// return;
						break;
					}
				}
			}
			// Offer angearbeitet..noch was da?
			if (actOffer.getAngebot()<=0){
				// diese offer kann entfernt werden
				if (fullfilledOffers==null){
					fullfilledOffers = new ArrayList<MatPoolOffer>();
				}
				fullfilledOffers.add(actOffer);
			}
		}
		
		long endT2 = System.currentTimeMillis();
		this.timeProcessOffers+=(endT2-endT1);
		
		// die nicht mehr benötigten Offers aus der Offer-Collection rausnehmen
		if (fullfilledOffers != null && fullfilledOffers.size()>0){
			actOfferList.removeAll(fullfilledOffers);
			// und diese spezialliste leeren
			fullfilledOffers.clear();
		}
		
			
	}
	
	
	
	/**
	 * ueberprueft, ob uebergebene Forderung mit der offer Einheit selbst befriedigt werden kann
	 * zumindest teilweise
	 * checkt dazu die entsprechende offer der unit
	 * @param actRequest
	 * @param Bedarf
	 * @return
	 * 
	 */
	
	private void checkSelfGratification(MatPoolRequest actRequest,MatPoolOffer actOffer){
		// betroffene unit
		ScriptUnit u = actRequest.getScriptUnit();
		if (!u.equals(actOffer.getScriptUnit())){
			return;
		}
				
		// gleicher ItemType? 
		ItemType actItemType = actOffer.getItemType();
		// aktuelle Forderung nach diesem ItemType
		int actForderung = actRequest.getForderung(actItemType);
		// aktuelles Angebot
		int actAngebot = actOffer.getAngebot();
		// Forderung und Angebot müssen beide grösser 0 sein
		if (actForderung >0 && actAngebot>0){
			// tatsächlicher Transfer als Minimum 
			int Transfer = Math.min(actForderung, actAngebot);
			// bei Offer: bearbeitet
			actOffer.incBearbeitet(Transfer);
			actRequest.incBearbeitet(Transfer,actItemType);
			// Keine Generierung irgendwelcher GIBs
			// das Zeug bleibt einfach bei der Unit
			if (!this.matPoolManager.isReportOFF()){
				// u.addComment("debug: self grat " + Transfer + " " + actItemType.toString() + " (" + this.actDurchlauf + ")", false);
			}
		}
	}

	
	public void informUs(){
		// if (outText.getTxtOut()==null){return;}
		if (this.matPoolManager.isReportOFF()) {
			return;
		}
		outText.addOutLine("----------------------------------------------------");
		outText.addOutLine(".......Region MatPool Info Service.......");
		String regionname = "";
		if (this.region!=null){
			outText.addOutLine("Region: " + this.region.toString());
			regionname = this.region.toString();
			outText.addOutLine(".......Region: " + regionname + " ..........");
		} else {
			outText.addOutLine("Region: is not set!");
		}
		outText.addOutLine("----------------------------------------------------");
		if (this.matPoolUnits!=null){
			outText.addOutLine("I have " + this.matPoolUnits.size() + " Units registered.");
		} else {
			outText.addOutLine("The Unit List is not set!");
		}
		if (this.requests!=null){
			outText.addOutLine("I have " + this.requests.size() + " requests registered.");
		} else {
			outText.addOutLine("The request list is not set!");
		}
		if (this.offers!=null){
			outText.addOutLine("I have " + this.offers.size() + " offers registered.");
		} else {
			outText.addOutLine("The offer list is not set!");
		}
		
		// Info
		
		// Arraylist mit ItemTypes aller Angebote+Reuests...nach diesen soll sortiert ausgegeben werden
		// outText.addOutLine("----------------------------------------------------");
		// outText.addOutLine("analysis for............(" + regionname + ")");
		ArrayList<ItemType> offeredItemTypes = new ArrayList<ItemType>(1);
		if (this.offers!=null){
			for (Iterator<MatPoolOffer> iter = this.offers.iterator();iter.hasNext();){
				MatPoolOffer mpr = (MatPoolOffer)iter.next();
				if (mpr.getItem()!=null){
					ItemType actT = mpr.getItemType();
					if (!offeredItemTypes.contains(actT)) {
						offeredItemTypes.add(actT);
						// outText.addOutLine("dazu: " + actT.getName());
					}
				}
			}
			// Collections.sort(this.offers,this.matPoolOfferComparatorForSort);
		}
		if (this.requests!=null){
			for (Iterator<MatPoolRequest> iter = this.requests.iterator();iter.hasNext();){
				MatPoolRequest mpr = (MatPoolRequest)iter.next();
				if (mpr.getItemTypes()!=null){
					for (Iterator<ItemType>iter2 = mpr.getItemTypes().iterator();iter2.hasNext();){
						ItemType actT = (ItemType) iter2.next();
						if (!offeredItemTypes.contains(actT)) {
							offeredItemTypes.add(actT);
							// outText.addOutLine("dazu: " + actT.getName());
						}
					}
				}
			}
			Collections.sort(this.requests);
		}
		
		// sortieren Ergs
		// warum ? FF 20070625
		// Collections.sort(offeredItemTypes);
		int i = 0;
		// und durchlaufen
		for (Iterator<ItemType> iter = offeredItemTypes.iterator();iter.hasNext();){
			ItemType itemType = (ItemType)iter.next();
			outText.addOutLine("----------------------------------------------------");
			outText.addOutLine(".......OFFER " + itemType.getName().toUpperCase());
			i = 0;
			if (this.offers!=null){
				for (Iterator<MatPoolOffer> iter2 = this.offers.iterator();iter2.hasNext();){
					MatPoolOffer mpr = (MatPoolOffer)iter2.next();
					ItemType otherItemType = mpr.getItemType();
					if (otherItemType!=null){
						if (otherItemType.equals(itemType)){
							String s = String.format("%8d",mpr.getAngebot());
							outText.addOutLine(s + " " + otherItemType.getName() + " verbraucht: " + mpr.getBearbeitet() + "  (" + mpr.getUnit().toString(true) + ")");
							i++;
						}
					}
				}
			}
			if (i==0){
				outText.addOutLine("[no offers]");
			}
			outText.addOutLine(".......REQUEST " + itemType.getName().toUpperCase());
			i = 0;
			if (this.requests!=null){
				for (Iterator<MatPoolRequest> iter2 = this.requests.iterator();iter2.hasNext();){
					MatPoolRequest mpr = (MatPoolRequest)iter2.next();
					if (mpr.getItemTypes()!=null){
						for (Iterator<ItemType> iter3 = mpr.getItemTypes().iterator();iter3.hasNext();){
							ItemType otherItemType = (ItemType) iter3.next();
							if (otherItemType!=null){
								if (otherItemType.equals(itemType)){
									// String s = String.format("%8d",mpr.getForderung(otherItemType));
									String s = String.format("%8d",mpr.getOriginalGefordert());
									outText.addOutLine(s + " " + otherItemType.getName() + " Prio " + mpr.getPrio() + " zugeordnet: " + mpr.getBearbeitet() + "  (" + mpr.getUnit().toString(true) + ", " + mpr.getKommentar()+ ")");
									i++;
								}
							}
						}
					}
				}
			}
			if (i==0){
				outText.addOutLine("[no requests]");
			}
		}
	
		outText.addOutLine("----------------------------------------------------");
	}
	
	/**
	 * adds the give MPR to this MatPool
	 * @param m the MPR to add
	 */
	public void addMatPoolRequest(MatPoolRequest m){

		if (m.getItemTypes()==null){
			// hier Fehler abfangen!
			if (!this.matPoolManager.isReportOFF()) {
				outText.addOutLine("!!!! Request ohne ItemType (" + m.getOriginalGegenstand() + ") Script: " + m.getScript().toString() + ", Einheit:" + m.getUnit().toString(true));
			}
			return;
		}
		
		if (this.requests == null) {
			this.requests = new ArrayList<MatPoolRequest>();
		}
		
		// Überprüfung, ob unit auch registriert?!
		this.addScriptUnit(m.getScriptUnit());
		
		this.requests.add(m);
	}
	
	/**
	 * entfernt den request aus dem MatPool
	 * @param m
	 */
	public boolean removeMatPoolRequest(MatPoolRequest m){
		if (this.requests==null || this.requests.size()==0){
			if (!this.matPoolManager.isReportOFF()) {
				outText.addOutLine("!!! removeMatPoolRequest: requests empty!");
			}
			return false;
		}
		return this.requests.remove(m);
	}
	
	
	/**
	 * adds the give MPO to this MatPool
	 * @param m the MPO to add
	 */
	public void addMatPoolOffer(MatPoolOffer m){
		// doch mal ein check
		if (m.getAngebot()<=0){
			// warum den anbieten ?
			if (!this.matPoolManager.isReportOFF()) {
				outText.addOutLine("!!!! Offer ohne Angebot: " + m.getUnit().toString(true));
			}
			return;
		}
		
		
		if (this.offers == null) {
			this.offers = new ArrayList<MatPoolOffer>();
		}
		
		// Überprüfung, ob unit auch registriert?!
		this.addScriptUnit(m.getScriptUnit());
		
		
		// HashMap füttern der Offers pro ScriptUnit
		if (this.scriptUnitOffers==null){
			this.scriptUnitOffers = new HashMap<ScriptUnit, ArrayList<MatPoolOffer>>();
		}
		// gibt es für diese Unit bereits eine Liste?
		ArrayList<MatPoolOffer> actList = this.scriptUnitOffers.get(m.getScriptUnit());
		if (actList == null){
			// offenbar nicht...anlegen
			actList = new ArrayList<MatPoolOffer>();
			this.scriptUnitOffers.put(m.getScriptUnit(), actList);
		}
		
		// sicherheitscheck, ob genau die offer bereits schon enthalten ist..dürfte nie so sein...
		if (!actList.contains(m)){
			// wenn nicht, hinzufügen
			actList.add(m);
		}
		
		this.offers.add(m);
	}
	
	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolRequest> getRequests() {
		/**
		 * old FF20061227
		return (this.requests != null) ? this.requests : null;
		*/
		if (this.requests==null){
			return null;
		}
		return this.requests;
	}
	
	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolRequest> getRequests(ScriptUnit u) {
		if (this.requests==null){
			return null;
		}
		ArrayList<MatPoolRequest> erg = null;
		for (Iterator<MatPoolRequest> iter = this.requests.iterator();iter.hasNext();){
			MatPoolRequest mpr = (MatPoolRequest)iter.next();
			if (mpr.getScriptUnit().equals(u)){
				if (erg==null){
					erg = new ArrayList<MatPoolRequest>();
				}
				erg.add(mpr);
			}
		}
		return erg;
	}
	
	/**
	 * Liefert eine Array-List mit den Offers oder NULL, falls keine existieren
	 *
	 */

	public ArrayList<MatPoolOffer> getOffers() {
		/**
		 * old FF20061227
		return (this.offers != null) ? this.offers : null;
		*/
		if (this.offers==null){return null;}
		return this.offers;
	}
	
	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolOffer> getOffers(ScriptUnit u) {
		if (this.offers==null){
			return null;
		}
		ArrayList<MatPoolOffer> erg = null;
		for (Iterator<MatPoolOffer> iter = this.offers.iterator();iter.hasNext();){
			MatPoolOffer mpr = (MatPoolOffer)iter.next();
			if (mpr.getScriptUnit().equals(u)){
				if (erg==null){
					erg = new ArrayList<MatPoolOffer>();
				}
				erg.add(mpr);
			}
		}
		return erg;
	}
	
	/**
	 * lässt alle scriptunits alles anbieten
	 */
	public void offerEverything(){
		if (this.matPoolUnits==null){return;}
		for (Iterator<ScriptUnit> iter = this.matPoolUnits.iterator();iter.hasNext();){
			ScriptUnit u = (ScriptUnit)iter.next();
			this.offerEverything(u);
		}
	}
	
	
	/**
	 * erzeugt normale offers aller Gegenstände der unit
	 * @param sU
	 */
	public void offerEverything(ScriptUnit sU){
		/*
		if (!this.matPoolManager.isReportOFF()) {
			outText.addOutLine("DEBUG: offerEverything for " + sU.toString());
		}
		*/
		if (sU.isGibNix()){
			/*
			if (!this.matPoolManager.isReportOFF()) {
				outText.addOutLine("isGibNix for " + sU.toString());
			}
			*/
			return;
		}
		for (Iterator<Item> iter = sU.getUnit().getItems().iterator();iter.hasNext();){
			Item item = (Item)iter.next();
			/*
			if (!this.matPoolManager.isReportOFF()) {
				outText.addOutLine("DEBUG: " + sU.toString() + ": checking item " + item.getName());
			}
			*/
			if (this.isMatPoolItemType(item.getItemType())){
				// OK, anbieten
				// checken ob modified geringer ist, dann nur das benutzen
				Item itemModified = sU.getUnit().getModifiedItem(item.getItemType());
				if (itemModified!=null && itemModified.getAmount()>0){
					MatPoolOffer mpr = new MatPoolOffer(sU,item);
					/*
					if (!this.matPoolManager.isReportOFF()) {
						outText.addOutLine("DEBUG: " + sU.toString() + ": taking standard item count:" + item.getAmount());
					}
					*/
					
					if (itemModified.getAmount()<item.getAmount()){
						Item newModifiedItem = new Item(itemModified.getItemType(),itemModified.getAmount());
						mpr = new MatPoolOffer(sU,newModifiedItem);
						/*
						if (!this.matPoolManager.isReportOFF()) {
							outText.addOutLine("DEBUG: " + sU.toString() + ": taking modifie item count:" + newModifiedItem.getAmount());
						}
						*/
					}
					this.addMatPoolOffer(mpr);
				} else {
					if (!this.matPoolManager.isReportOFF()) {
						/*
						outText.addOutLine("DEBUG: " + sU.toString() + ": checking item " + item.getName() + " modifiesItem=null or modified=0");
						if (itemModified==null) {
							outText.addOutLine("-- itemMod is NULL!");
						} else {
							outText.addOutLine("-- amount modified = " + itemModified.getAmount());
						}
						*/
					}
				}
			}
		}
	}
	
	/**
	 * checked, ob ein Item am MatPool teilnehmen sollte oder nicht
	 * @param itemType
	 * @return
	 */
	private boolean isMatPoolItemType(ItemType itemType){

		// prinzipiell alles zulassen
		boolean erg = true;
		// Name der Kategorie extrahieren
		ItemCategory itemCat = itemType.getCategory();
		if (itemCat !=null){
			// ausschliessen :
			// alle Trophaeen
			if (itemCat.getName().equalsIgnoreCase("Trophaeen")) {
				erg = false;
			}
			// Sonstiges ausser Silber
			// 20100630: Sonstige zulassen
			// 
			// if (itemCat.getName().equalsIgnoreCase("Sonstiges")) {
			//	if (!itemType.getName().equalsIgnoreCase("Silber")) {
			//		erg = false;
			//	}
			// }
			// RdF explizit zulassen
			if (itemType.getName().equalsIgnoreCase("Ring der flinken Finger")) {
				erg=true;
			}
		} else {
			erg = false;
			// RdU und AwS zulassen
			if (itemType.getName().equalsIgnoreCase("Ring der Unsichtbarkeit")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Amulett des wahren Sehens")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Gürtel der Trollstärke")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Ring der flinken Finger")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Weihnachtsbaum")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Antimagiekristall")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Sphäre der Unsichtbarkeit")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Ring der Macht")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Feenstiefel")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Drachenblut")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Geburtstagstorte")) {
				erg = true;
			}
			if (itemType.getName().equalsIgnoreCase("Tiegel mit Krötenschleim")) {
				erg = true;
			}
		}
		return erg;
	}
	
	/**
	 * setzt die depotunit
	 * @param u
	 */
	public void setDepotUnit(ScriptUnit u){
		this.depotUnit = u;
	}
	
	
	
	/**
	 * liefert eine ArrayList mit den TransportRequest aus
	 * unerfüllten MatPoolrequests, soweit die nicht region=ja haben
	 * 
	 * @return
	 */
	public ArrayList<TransportRequest> getTransportRequests(){
		ArrayList<TransportRequest> erg = new ArrayList<TransportRequest>();
		if (this.requests==null){
			return erg;
		}
		for (Iterator<MatPoolRequest> iter = this.requests.iterator();iter.hasNext();){
			MatPoolRequest MPR = (MatPoolRequest)iter.next();
			
		
			if (MPR.getBearbeitet()<MPR.getOriginalGefordert() && !MPR.isOnlyRegion() && MPR.getPrio()>1){	
				boolean itemIsGoodForTM = true;
				// FF 20070430: Pferde ausschliessen
				if (MPR.getOriginalGegenstand().equalsIgnoreCase("Pferd") || MPR.getOriginalGegenstand().equalsIgnoreCase("Pferde")){
					itemIsGoodForTM = false;
				}
				if (itemIsGoodForTM){
					TransportRequest TR = new TransportRequest(MPR.getScriptUnit(),MPR.getOriginalGefordert()-MPR.getBearbeitet(),MPR.getOriginalGegenstand(),MPR.getPrio(),"TM->" + MPR.getKommentar());
					// SpezialPrio? (genutzt durch Vorrat)
					if (MPR.getPrioTM()!=MPR.getPrio()) {
						TR.setPrio(MPR.getPrioTM());
					}
					// Specs
					if (MPR.getTransporterSpecs()!=null){
						TR.addSpec(MPR.getTransporterSpecs());
					}
					
					// sortMode
					TR.setTM_sortMode(MPR.getTMsortMode());
					
					MPR.setTransportRequest(TR);
					erg.add(TR);
				}
			}
			
			
			// 20161218: prio=1 requests über den TM noch mal laufen lassen, immer, alle...
			if (MPR.getPrio()==1 && !MPR.isOnlyRegion()){
				boolean itemIsGoodForTM = true;
				
				if (MPR.getOriginalGegenstand().equalsIgnoreCase("Pferd") || MPR.getOriginalGegenstand().equalsIgnoreCase("Pferde")){
					itemIsGoodForTM = false;
				}
				if (itemIsGoodForTM){
					TransportRequest TR = new TransportRequest(MPR.getScriptUnit(),MPR.getOriginalGefordert(),MPR.getOriginalGegenstand(),MPR.getPrio(),"TM_prio1->" + MPR.getKommentar());
					// SpezialPrio? (genutzt durch Vorrat)
					if (MPR.getPrioTM()!=MPR.getPrio()) {
						TR.setPrio(MPR.getPrioTM());
					}
					// Specs
					if (MPR.getTransporterSpecs()!=null){
						TR.addSpec(MPR.getTransporterSpecs());
					}
					
					// sortMode
					TR.setTM_sortMode(MPR.getTMsortMode());
					
					MPR.setTransportRequest(TR);
					erg.add(TR);
				}
			}
			
		}
		
		return erg;
	}
	
	public String toString(){
		return "Matpool " + this.region.toString();
	}

	/**
	 * @return the depotUnit
	 */
	public ScriptUnit getDepotUnit() {
		return depotUnit;
	}
	
	/**
	 * Pferde und Wagen - Prio Offset
	 * bewirkt die erst-behandlung von Pferden, dann wagen
	 * @param start true: erhöhen, false:verringern (normal-niveau)
	 */
	private void preRequestOffsets(boolean start){
		if (this.requests!=null && this.requests.size()>0){
			for (MatPoolRequest mpr : this.requests){
				// Pferde
				boolean workOn = false;
				int offset=0;
				if (mpr.getOriginalGegenstand().equalsIgnoreCase("Pferd") && mpr.isPrioChange() && mpr.getPrio()!=Depot.default_request_prio){
					workOn=true;
					offset=MatPool2.pferdeOffset;
				}
				if (mpr.getOriginalGegenstand().equalsIgnoreCase("Wagen") && mpr.isPrioChange()&& mpr.getPrio()!=Depot.default_request_prio){
					workOn=true;
					offset=MatPool2.wagenOffset;
				}
				if (!start){
					offset*=-1;
				}
				if (workOn){
					mpr.setPrio(mpr.getPrio()+offset);
				}
			}
		}
	}
	
	/**
	 * löscht ungeschützte orders
	 *
	 */
	private void cleanOrders(){
		if (this.matPoolUnits!=null && this.matPoolUnits.size()>0){
			for (ScriptUnit su : this.matPoolUnits){
				su.deleteSomeOrders("GIB");
				su.deleteSomeOrders("RESERVIERE");
			}
		}
	}
	
	/**
	 * setzt einmalig die modified Items nach refreshRegion als modified
	 * bei der scriptunit
	 *
	 */
	private void firstInitialModifiedItems(){
		if (this.matPoolUnits!=null && this.matPoolUnits.size()>0){
			for (ScriptUnit su : this.matPoolUnits){
				this.firstInitialModifiedItems(su);
			}
		}
	}
	
	private void firstInitialModifiedItems(ScriptUnit su){

		HashMap<ItemType, Item> newMap = new HashMap<ItemType, Item>();
		HashMap<ItemType, Item> originalMap = new HashMap<ItemType, Item>();
		for (Iterator<Item> iter = su.getUnit().getModifiedItems().iterator();iter.hasNext();){
			Item actItem = (Item)iter.next();
			newMap.put(actItem.getItemType(), actItem);
			originalMap.put(actItem.getItemType(), new Item(actItem.getItemType(),actItem.getAmount()));
		}
		if (newMap.size()>0){
			su.setModifiedItemsMatPool2(newMap);
			this.originalModifiedScriptUnitItems.put(su,originalMap);
		}
	}
	
	
	
	private void resetModifiedItemsMatPool2(){
		if (this.matPoolUnits!=null && this.matPoolUnits.size()>0 && this.originalModifiedScriptUnitItems!=null){
			for (ScriptUnit su : this.matPoolUnits){
				resetModifiedItemsMatPool2(su);
			}
		}
	}
	
	private void resetModifiedItemsMatPool2(ScriptUnit su){
		
		HashMap<ItemType, Item> originalMap = this.originalModifiedScriptUnitItems.get(su);
		HashMap<ItemType, Item> oldMap = su.getModifiedItemsMatPool2();		
		// resetten -> 0
		if (oldMap!=null){
			for (Item item : oldMap.values()){
				item.setAmount(0);
			}
		}
		
		// wieder auf Originalwert setzen
		if (originalMap!=null && oldMap!=null){
			for (Item item : originalMap.values()){
				Item oldItem = oldMap.get(item.getItemType());
				if (oldItem!=null){
					oldItem.setAmount(item.getAmount());
				}
			}
		} 
	}
	
	
	/**
	 * not needed in MatPool2
	 */
	public void runPool(int Durchlauf,ItemType type){
	}

	/* (non-Javadoc)
	 * @see com.fftools.pools.matpool.MatPool#calcAllKapas()
	 */
	public void calcAllKapas() {
	}

	/* (non-Javadoc)
	 * @see com.fftools.pools.matpool.MatPool#processDepotOrders()
	 */
	public void processDepotOrders() {
	}

	/**
	 * @return the region
	 */
	public Region getRegion() {
		return region;
	}
	
}
