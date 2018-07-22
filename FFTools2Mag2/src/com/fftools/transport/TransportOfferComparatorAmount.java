package com.fftools.transport;

import java.util.Comparator;


/**
 * Sortiert Offers nach der Menge des Angebotes.
 * Falls Menge gleich, wird die Offer mit der kürzesten
 * Entfernung bevorzugt
 * 
 * @author Fiete
 *
 */
public class TransportOfferComparatorAmount implements Comparator<TransportOffer>{
	
	public TransportOfferComparatorAmount(){

	}
	
	public int compare(TransportOffer o1,TransportOffer o2){
		
		if (o2.getAnzahl_nochOfferiert()==o1.getAnzahl_nochOfferiert()){
			return (o1.getActDist() - o2.getActDist());
		} else {
			return (o2.getAnzahl_nochOfferiert()-o1.getAnzahl_nochOfferiert());
		}
	}
	
}
