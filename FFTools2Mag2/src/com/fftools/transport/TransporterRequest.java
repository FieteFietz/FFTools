package com.fftools.transport;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.TransportScript;
import com.fftools.utils.FFToolsRegions;

/**
 * dient als Datenstruktur zur Kommunikation der Requests, die der 
 * TM dem Transporter übergibt und durch das script Transport dann
 * tatsächlich dem MatPoolMg übergeben werden
 * 20080311: ergänzt um die passende request um beide units in einem rutsch zu
 * informieren
 * @author Fiete
 *
 */
public class TransporterRequest {
	private int Anzahl = 0;
	private TransportOffer transportOffer = null;
	private TransportRequest transportRequest = null;
	private Transporter transporter = null;
	private String intro = "";
	
	private int ETA_cache=-1;
	
	private static int defaultTransporterPrio = 2;
	
	/**
	 * Konstruktor
	 * @param anzahl
	 * @param transportRequest
	 * @param transportOffer
	 */
	public TransporterRequest(int anzahl, TransportOffer transportOffer, TransportRequest transportRequest, Transporter transporter, String intro) {
		this.Anzahl = anzahl;
		this.transportOffer = transportOffer;
		this.transportRequest = transportRequest;
		this.transporter = transporter;
		this.intro = intro;
	}
	
	/**
	 * liefert den passenden request an den MatPool zu diesem Request
	 * benötigt ein Transportscript zum einfachen Erstellen des MatPoolRequests
	 * @param s
	 * @return
	 */
	public MatPoolRequest getMatPoolRequest(TransportScript s){	
		MatPoolRequest mpr = new MatPoolRequest(s,this.Anzahl,this.transportOffer.getItemName(),defaultTransporterPrio,"TM");
		// grundsätzlich nur in der Region
		mpr.setOnlyRegion(true);
		return mpr;
	}
	
	/**
	 * informiert die betroffenen requester und offerer über die Zuteilung des transportes
	 * und die vorraussichtliche Ankunftszeit
	 *
	 */
    public void informUnits(){
		// ETA berechnung
		int ETAAnzahlRunden = this.getETA();
		transportOffer.getScriptUnit().addComment("TM(-):" + Anzahl + " " + transportOffer.getItemName() + " (P:" + transportRequest.getPrio()  + ",ETA:" + ETAAnzahlRunden + ") nach " + transportRequest.getRegion().toString() + " mit " + this.transporter.getScriptUnit().getUnit().toString(true) + " in " + this.transporter.getScriptUnit().getUnit().getRegion().toString() + " für " + transportRequest.getScriptUnit().toString());
		transportRequest.getScriptUnit().addComment("TM(+):" + Anzahl + " " + transportOffer.getItemName() + " (P:" + transportRequest.getPrio() + ",ETA:" + ETAAnzahlRunden +  ") von " + transportOffer.getRegion().toString() + ") mit " + this.transporter.getScriptUnit().getUnit().toString(true) + " in " + this.transporter.getScriptUnit().getUnit().getRegion().toString() + " von " + transportOffer.getScriptUnit().toString());
		this.transporter.getScriptUnit().addComment(intro + ":" + Anzahl + " " + transportOffer.getItemName() + "(P:" + transportRequest.getPrio() + ",ETA:" + ETAAnzahlRunden + ") von " + transportOffer.getRegion().toString() + " nach " + transportRequest.getRegion().toString() + " für " + transportRequest.getScriptUnit().toString());
	}
	
    private int getETA(){
		// ETA berechnung
		// immer: Entfernung Offer->request
    	if (this.ETA_cache<0){
			int ETAAnzahlRunden = FFToolsRegions.getPathDistLand(this.transporter.getScriptUnit().getScriptMain().gd_ScriptMain, transportOffer.getRegion().getCoordinate(),transportRequest.getRegion().getCoordinate(), true);
			// wenn Transporter nicht in OfferRegion kommt enfernung dorthin noch dazu
			if (!this.transporter.getScriptUnit().getUnit().getRegion().equals(transportOffer.getRegion())){
				ETAAnzahlRunden+=FFToolsRegions.getPathDistLand(this.transporter.getScriptUnit().getScriptMain().gd_ScriptMain, this.transporter.getScriptUnit().getUnit().getRegion().getCoordinate(), transportOffer.getRegion().getCoordinate(), true);
			}
			this.ETA_cache = ETAAnzahlRunden;
    	} 
		return this.ETA_cache;
	}
    
    /**
     * das Gewicht dieser Transaktion
     * @return
     */
    public int getWeight(){
    	int verbrauch = (int)Math.ceil((transportOffer.getItem().getItemType().getWeight()*Anzahl));
    	return verbrauch;
    }
    
}
