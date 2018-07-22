package com.fftools.trade;

import java.util.Comparator;

import magellan.library.Region;
import magellan.library.rules.ItemType;

public class TradeRegionComparatorSell implements Comparator<TradeRegion>{
	ItemType itemType = null;
	
	public TradeRegionComparatorSell(ItemType whichItemType){
		this.itemType = whichItemType;
	}
	
	public int compare(TradeRegion tR1 ,TradeRegion tR2){
		Region r1 = tR1.getRegion();
		Region r2 = tR2.getRegion();
		int preis1 = tR1.getSellPrice(itemType);
		int preis2 = tR2.getSellPrice(itemType);
		int money1 = preis1 * r1.maxLuxuries();
		int money2 = preis2 * r2.maxLuxuries();
		return (money2 - money1);
	}
}
