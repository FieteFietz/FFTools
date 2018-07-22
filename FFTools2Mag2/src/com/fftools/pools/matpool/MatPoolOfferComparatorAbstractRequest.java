package com.fftools.pools.matpool;

import java.util.Comparator;

import magellan.library.Item;

import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolOffer;


/**
 * wird benutzt, um abstrakte Requests umzusetzen: die offers
 * werden nach Prio der begriffe sortiert
 * @author Fiete
 *
 */
public class MatPoolOfferComparatorAbstractRequest implements Comparator<MatPoolOffer>{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	/**
	 * name der Kategorie
	 */
	private String catName = null;
	private ScriptUnit requestingScriptUnit=null;
	
	public MatPoolOfferComparatorAbstractRequest(String catName,ScriptUnit requestingScriptUnit) {
		this.catName = catName;
		this.requestingScriptUnit = requestingScriptUnit;
	}
	
	/**
	 * der Vergleich, richtet sich nach den Prios 
	 * @param o1
	 * @param o2
	 * @return
	 */
	public int compare(MatPoolOffer offer1,MatPoolOffer offer2){
		
		if (this.catName==null){
			return 0;
		}
		
		
		Item item1 = offer1.getItem();
		Item item2 = offer2.getItem();
		
		int prio1 = reportSettings.getReportSettingPrio(this.catName, item1.getItemType());
		int prio2 = reportSettings.getReportSettingPrio(this.catName, item2.getItemType());
		
		if (prio1==prio2){
			// bei gleicher prio checken, ob eine der offers von der requesting unit kommen
			// die dann nach vorne
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
			
			return 0;
		}
		return (prio2-prio1);
	}

	/**
	 * @return the catName
	 */
	public String getCatName() {
		return catName;
	}

	/**
	 * @param catName the catName to set
	 */
	public void setParameter(String catName, ScriptUnit requestingScriptUnit) {
		this.catName = catName;
		this.requestingScriptUnit = requestingScriptUnit;
		if (!reportSettings.isInCategories(this.catName)){
			this.catName=null;
		}
	}
	
	
}
