package com.fftools.trade;


import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.Item;
import magellan.library.LuxuryPrice;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.rules.ItemType;

import com.fftools.ScriptUnit;
import com.fftools.overlord.Overlord;

/**
 * What we need to store about an region with scripting traders
 * in it...
 * @author Fiete
 *
 */
public class TradeRegion {
	private Region region = null;
	
	private ItemType buyItemType = null;
	
	private Overlord ovi = null;
	/**
	 * wenn als origin explizit durch nutzer gesetzt
	 * keine automatische zuordnung bzw neuverteilung
	 */
	private boolean isSetAsTradeAreaOrigin = false;
	private String TradeAreaName = "";
	
	/**
	 * Liste aller in dieser Region bekannten Trader
	 */
	private ArrayList<Trader> traders = null;
	
	public TradeRegion(Region r, Overlord overlord){
		this.region = r;
		this.ovi=overlord;
	}

	/**
	 * @return the isSetAsTradeAreaOrigin
	 */
	public boolean isSetAsTradeAreaOrigin() {
		return isSetAsTradeAreaOrigin;
	}

	/**
	 * @param isSetAsTradeAreaOrigin the isSetAsTradeAreaOrigin to set
	 */
	public void setSetAsTradeAreaOrigin(boolean isSetAsTradeAreaOrigin) {
		this.isSetAsTradeAreaOrigin = isSetAsTradeAreaOrigin;
	}
	
	

	/**
	 * @return the region
	 */
	public Region getRegion() {
		return region;
	}

	/**
	 * @param region the region to set
	 */
	public void setRegion(Region region) {
		this.region = region;
	}

	/**
	 * @return the tradeAreaName
	 */
	public String getTradeAreaName() {
		return TradeAreaName;
	}

	/**
	 * @param tradeAreaName the tradeAreaName to set
	 */
	public void setTradeAreaName(String tradeAreaName) {
		TradeAreaName = tradeAreaName;
		this.setSetAsTradeAreaOrigin(true);
	}
	
	/**
	 * Returns the Item Type with negative price => cost
	 * @return
	 */
	public ItemType getBuyItemType(){
		if (this.buyItemType==null){
			if(region.getPrices() != null) {
				for(Iterator<LuxuryPrice> iter = region.getPrices().values().iterator(); iter.hasNext();) {
					LuxuryPrice p = (LuxuryPrice) iter.next();

					if(p.getPrice() < 0) {
						this.buyItemType = p.getItemType();
						break;
					}
				}
			}
		}
		return this.buyItemType;
	}
	
	
	/**
	 * liefert den Preis einer Ware zurück
	 * immer positiv!
	 * @param itemType
	 * @return
	 */
	public int getSellPrice(ItemType itemType){
		int erg = 0;
		if(region.getPrices() != null) {
			for(Iterator<LuxuryPrice> iter = region.getPrices().values().iterator(); iter.hasNext();) {
				LuxuryPrice p = (LuxuryPrice) iter.next();
				if (p.getItemType().equals(itemType)){
					erg = p.getPrice();
					break;
				}
			}
		}
		if (erg<0){erg = Math.abs(erg);}
		return erg;
	}

	/**
	 * @return true, if Trader es registred with this region
	 */
	public boolean hasTrader() {
		if (this.traders!=null && this.traders.size()>0){
			return true;
		}
		return false;
	}

	
	/** 
	 * liefert das (erste) Depot der Region...
	 * @param r
	 * @return
	 */
	public Unit getDepot(){
		for (Iterator<Unit> iter = region.units().iterator();iter.hasNext();){
			Unit u = (Unit)iter.next();
			ScriptUnit su = ovi.getScriptMain().getScriptUnit(u);
			if (su!=null) {
				if (su.isDepot()) {
					return u;
				}
			}
			
		}
		return null;
	}
	
	
	private String storageAmountInfo="";
	
	public String getStorageAmountInfo() {
		return storageAmountInfo;
	}

	/**
	 * liefert summe aller Depotbestände
	 * eines ItemTypes (Luxusgut) in der Region, wenn das ItemType dort zu kaufen
	 * ist oder mehr als vorratsrunden vorhanden ist
	 * @param itemType
	 * @return
	 */
	public int getStorageAmount(ItemType itemType){
			int erg = 0;
			storageAmountInfo="";
			Unit u = this.getDepot();
			if (u!=null){
				Item item = u.getItem(itemType);
				if (item!=null){
					if (this.getBuyItemType()!=null && this.getBuyItemType().equals(itemType)){
						// itemType kann hier gekauft werden
						erg += item.getAmount();
						
						storageAmountInfo="L:" + item.getAmount();
					} else {
						// kann hier nicht gekauft werden
						// nur das anbieten, was über Verkaufsvorratsrunden
						// hinausgeht
						Trader t = this.getTrader(true);
						if (t!=null && this.getRegion().maxLuxuries()>0){
							// Händler gefunden
							int eigenesLager = (-1) * t.getPrognoseRunden() * this.getRegion().maxLuxuries();
							storageAmountInfo=item.getAmount() + "" + eigenesLager;
							eigenesLager += item.getAmount();
							storageAmountInfo+="=" + eigenesLager;
							if (eigenesLager>0){
								erg += eigenesLager;
							}
						}
					}
				}	
			}
		return erg;
	}
	
	
	public void addTrader(Trader t){
		if (this.traders==null){
			this.traders = new ArrayList<Trader>();
		}
		if (!this.traders.contains(t)){
			this.traders.add(t);
		}
	}
	
	
	/**
	 * liefert den Trader einer TradeRegion
	 * wenn onlyBuyer=wahr, dann nur die Trader, die auch
	 * Kaufen
	 * @param TR
	 * @param onlyBuyer
	 * @return
	 */
	private Trader getTrader(boolean onlyBuyer){
		if (this.traders==null){
			return null;
		}
		
		for (Trader actT:this.traders){
			boolean foundIt=false;
			if (actT.getScriptUnit().getUnit().getRegion().equals(this.getRegion())){
				// regionen sind gleich
				foundIt=true;
				if (onlyBuyer && !actT.isKaufen()){
					foundIt=false;
				}
				if (foundIt){
					return actT;
				}
			}
		}
		// nicht gefunden
		return null;
	}
	
	/**
	 * liefert summe aller SCUs-Bestände
	 * @param itemType
	 * @return
	 */
	public int getTotalAmount(ItemType itemType){
		int erg = 0;
		for (Unit u:this.region.units()){
			ScriptUnit scu = this.ovi.getScriptMain().getScriptUnit(u);
			if (scu!=null){
				Item item = u.getItem(itemType);
				if (item!=null){
					erg += item.getAmount();
				}
			}
		}	
		return erg;
	}
	
	/**
	 * liegfert summe aller ausgaben der trader
	 * @return
	 */
	public int getBuySilverAmount() {
		int erg=0;
		if (this.traders!=null && this.traders.size()>0) {
			for (Trader T: this.traders) {
				if (T.isKaufen()) {
					erg += T.getRequestedSilver();
				}
			}
		}
		
		return erg;
	}
	
}
