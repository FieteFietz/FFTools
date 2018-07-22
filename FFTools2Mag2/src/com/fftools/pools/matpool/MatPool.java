package com.fftools.pools.matpool;

import java.util.ArrayList;

import magellan.library.Region;
import magellan.library.rules.ItemType;

import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolOffer;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.transport.TransportRequest;


/**
 * Zentrale Abwicklung des Materialpools
 * 
 * @author Fiete
 *
 *Marc 20070104: Umgestellt auf Verwendung der methoden get/setAnzahlBearbeitet()
 *
 */
public interface MatPool {
	
	
	public static final String depotIdComment = ";DEPOT";
	
	
	/**
	 * alle in der Region bekannten requests
	 */
	public ArrayList<MatPoolRequest> requests = null;
	
	/**
	 * alle in der Region bekannten offers
	 */
	public ArrayList<MatPoolOffer> offers = null;
	
	/**
	 * Referenz auf den MatPoolManager
	 */
	public MatPoolManager matPoolManager = null;
	 
	 /**
	 * Registriert uebergebene ScriptUnit bei diesem MatPool
	 * wenn sie nicht schon registriert worden ist....
	 * @param _u
	 */
	public void addScriptUnit(ScriptUnit _u);
	
	
	/**
	 * wird vom MatPoolManager aufgerufen und bewirkt dass  
	 * reseten der Relations
	 */	
	public void resetRelations();
		
	
	/**
	 * poolt die Region: Versucht, alle Requests anhand der Offers zu bedienen
	 * 
	 * 20060812: handelt nicht abstrakte Gegenstaende ("Hiebwaffe" o.ae.)
	 * 20070111: FF complete rework
	 */
	public void runPool(int durchlauf,ItemType _itemType);
	
	// Überladener Aufruf ohne Itemtype, was zum Poolen aller Waren führt!
	public void runPool(int Durchlauf);
	
	public void processDepotOrders();
	
	
	

	
	
	
	public void informUs();
	
	/**
	 * adds the give MPR to this MatPool
	 * @param m the MPR to add
	 */
	public void addMatPoolRequest(MatPoolRequest m);
	
	/**
	 * entfernt den request aus dem MatPool
	 * @param m
	 */
	public boolean removeMatPoolRequest(MatPoolRequest m);
	
	/**
	 * adds the give MPO to this MatPool
	 * @param m the MPO to add
	 */
	public void addMatPoolOffer(MatPoolOffer m);
	
	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolRequest> getRequests() ;
	
	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolRequest> getRequests(ScriptUnit u);
	
	/**
	 * Liefert eine Array-List mit den Offers oder NULL, falls keine existieren
	 *
	 */

	public ArrayList<MatPoolOffer> getOffers();
	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolOffer> getOffers(ScriptUnit u);
	
	/**
	 * lässt alle scriptunits alles anbieten
	 */
	public void offerEverything();
	
	
	
	
	/**
	 * setzt die depotunit
	 * @param u
	 */
	public void setDepotUnit(ScriptUnit u);
	
	
	/**
	 * liefert eine ArrayList mit den TransportRequest aus
	 * unerfüllten MatPoolrequests, soweit die nicht region=ja haben
	 * 
	 * @return
	 */
	public ArrayList<TransportRequest> getTransportRequests();

	public String toString();
	/**
	 * @return the depotUnit
	 */
	public ScriptUnit getDepotUnit() ;
	
	/**
	 * ein weiterer versuch, die kapa in den griff zu bekommen
	 * berechnet von allen units im Matpool die kapa..
	 *
	 */
	public void calcAllKapas();
	

	/**
	 * erzeugt normale offers aller Gegenstände der unit
	 * @param sU
	 */
	public void offerEverything(ScriptUnit sU);
	
	public Region getRegion();
	
}
