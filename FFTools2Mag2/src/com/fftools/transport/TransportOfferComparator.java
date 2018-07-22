package com.fftools.transport;

import java.util.Comparator;


/**
 * Sortiert Offers nach der Entfernung zu einer Region.
 * Falls Entfernung gleich, wird die Offer mit der grösseren noch offer-
 * ierten Menge bevorzugt
 * 
 * @author Fiete
 *
 */
public class TransportOfferComparator implements Comparator<TransportOffer>{
	
	public TransportOfferComparator(){

	}
	
	public int compare(TransportOffer o1,TransportOffer o2){
		
		int dist1 = o1.getActDist();
		int dist2 = o2.getActDist();
		
		if (dist1==dist2){
			return (o2.getAnzahl_nochOfferiert()-o1.getAnzahl_nochOfferiert());
		} else {
			return (dist1 - dist2);
		}
	}
	
}
