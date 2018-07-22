package com.fftools.pools.matpool.relations;

import magellan.library.Item;
import magellan.library.rules.ItemType;

import com.fftools.ScriptUnit;

/**
 * Allgemeines Angebot eines Items an den MatPool
 * 
 * @author Fiete
 *
 */
public class MatPoolOffer extends MatPoolRelation {
	/**
	 * offeriertes Gut. Amount = Menge
	 */
	private Item item = null;
	
	
	
	/**
	 * Konstruktor
	 * @param _item
	 */
	public MatPoolOffer(ScriptUnit u,Item _item){
		this.item = _item;
		this.setScriptUnit(u);
	}

	/**
	 * @return the item
	 */
	public Item getItem() {
		return item;
	}
	
	/**
	 * liefert ItemType dieser Offer
	 * @return
	 */
	public ItemType getItemType(){
		return this.getItem().getItemType();
	}
	
	/**
	 * Anzahl des Gutes, welche noch zur Verfügung steht
	 * @return
	 */
	public int getAngebot(){
		return (Math.max(0,this.item.getAmount()-this.getBearbeitet()));
	}
	
	
	public int getAngebotsGewicht(){
		float weight = 0;
		if (this.getItemType()!=null){
			weight = this.getItemType().getWeight();
		}
		weight = weight * this.getAngebot();
		// return (int)Math.ceil(weight);
		return (int)Math.floor(weight);
	}
	
	
	public String toString(){
    	return this.item.getAmount() + " " + this.item.getName() + "  bearb.:" + this.getBearbeitet();
    }
    
	
	/**
	 * Erhöht die bereits verbrauchte Anzahl des Gutes
	 * @param _bearbeitet
	 */
	public void incBearbeitet(int _bearbeitet){
		super.setBearbeitet(super.getBearbeitet()+ _bearbeitet);
		
		// modifiedItems der scriptunit anpassen
		// wenn offer bearbeitet, wird anzahl offerierter gegenstände reduziert
		this.getScriptUnit().changeModifiedItemsMatPools(this.getItemType(), (-1)*(_bearbeitet));
		
	}
	
}
