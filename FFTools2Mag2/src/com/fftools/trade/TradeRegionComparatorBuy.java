package com.fftools.trade;

import java.util.Comparator;

import magellan.library.Region;

public class TradeRegionComparatorBuy implements Comparator<TradeRegion>{
		
	public TradeRegionComparatorBuy(){
		
	}
	
	public int compare(TradeRegion tR1,TradeRegion tR2){
		Region r1 = tR1.getRegion();
		Region r2 = tR2.getRegion();
		
		return (r2.maxLuxuries() -r1.maxLuxuries());
	}
}
