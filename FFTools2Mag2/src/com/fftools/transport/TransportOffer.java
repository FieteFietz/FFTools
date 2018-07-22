package com.fftools.transport;

import magellan.library.Item;
import magellan.library.Region;

import com.fftools.ScriptUnit;

/**
 * Wer hat Wo Was Wieviel...und was ist noch zu haben...
 * 
 * @author Fiete
 *
 */
public class TransportOffer implements Comparable<TransportOffer> {
	/**
	 * Quellregion
	 */
	private Region region = null;
	/**
	 * angebot : ItemType + Menge (amount)
	 */
	private Item item = null;
	
	/**
	 * tatsächlich offerierte Anzahl des Items
	 */
	private int anzahl_offeriert = 0;
	
	/**
	 * wieviel davon bereits bearbeitet....
	 */
	private int anzahl_bearbeitet = 0;
	
	/**
	 * die offerierende Unit...wer bekommt das GIB
	 */
	private ScriptUnit scriptUnit = null;
	
	/**
	 * wird benutzt beim offer-comparator
	 */
	private int actDist = 0;
	
	public TransportOffer(ScriptUnit scriptUnit,Item _item,int anzahl_offeriert){
		this.region = scriptUnit.getUnit().getRegion();
		this.scriptUnit = scriptUnit; 
		this.item = _item;
		this.anzahl_offeriert = anzahl_offeriert;
	}
	
	public TransportOffer(ScriptUnit scriptUnit,Item _item){
		this(scriptUnit,_item,_item.getAmount());
	}

	/**
	 * @return the anzahl_bearbeitet
	 */
	public int getAnzahl_bearbeitet() {
		return anzahl_bearbeitet;
	}

	
	public int getAnzahl_nochOfferiert(){
		return (Math.max(0,(this.anzahl_offeriert - anzahl_bearbeitet)));
	}
	
	/**
	 * @param anzahl_bearbeitet the anzahl_bearbeitet to set
	 */
	public void setAnzahl_bearbeitet(int anzahl_bearbeitet) {
		this.anzahl_bearbeitet = anzahl_bearbeitet;
	}

	public void incBearbeitet(int betrag){
		this.anzahl_bearbeitet+=betrag;
	}
	
	
	/**
	 * @return the item
	 */
	public Item getItem() {
		return item;
	}

	/**
	 * @return the region
	 */
	public Region getRegion() {
		return region;
	}
	
	public int compareTo(TransportOffer o){
		return o.getAnzahl_nochOfferiert() - this.getAnzahl_nochOfferiert();
	}

	/**
	 * @return the scriptUnit
	 */
	public ScriptUnit getScriptUnit() {
		return scriptUnit;
	}
	
	/**
	 * easy way to get the name of the item
	 * @return
	 */
	public String getItemName(){
		return this.item.getItemType().getName();
	}

	/**
	 * @return the actDist
	 */
	public int getActDist() {
		return actDist;
	}

	/**
	 * @param actDist the actDist to set
	 */
	public void setActDist(int actDist) {
		this.actDist = actDist;
	}
	
}
