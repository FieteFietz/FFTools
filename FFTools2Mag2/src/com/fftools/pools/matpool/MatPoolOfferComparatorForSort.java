package com.fftools.pools.matpool;

import java.util.Comparator;

import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolOffer;


/**
 * wird benutzt, um abstrakte Requests umzusetzen: die offers
 * werden nach Prio der begriffe sortiert
 * @author Fiete
 *
 */
public class MatPoolOfferComparatorForSort implements Comparator<MatPoolOffer>{
	
	private ScriptUnit requestingScriptUnit=null;
	
	public MatPoolOfferComparatorForSort(ScriptUnit requestingScriptUnit) {
		this.requestingScriptUnit = requestingScriptUnit;
	}
	
	/**
	 * der Vergleich, richtet sich nach der grösse des angebotes
	 * und neuerdings nach der unit
	 * @param o1
	 * @param o2
	 * @return
	 */
	public int compare(MatPoolOffer offer1,MatPoolOffer offer2){

		// eigenbedarf decken...;-))
		if (offer1.getScriptUnit().equals(this.requestingScriptUnit)){
			return -1;
		}
		if (offer2.getScriptUnit().equals(this.requestingScriptUnit)){
			return 1;
		}
		
		// dann noch checken, ob eine der beiden units ein depot ist
		// dass dann nach vorne
		if (offer1.getScriptUnit().isDepot()){
			return -1;
		}
		if (offer2.getScriptUnit().isDepot()){
			return 1;
		}
		
		
		return offer2.getAngebot() - offer1.getAngebot();
	}

	/**
	 * @param requestingScriptUnit the requestingScriptUnit to set
	 */
	public void setRequestingScriptUnit(ScriptUnit requestingScriptUnit) {
		this.requestingScriptUnit = requestingScriptUnit;
	}

	
	
}
