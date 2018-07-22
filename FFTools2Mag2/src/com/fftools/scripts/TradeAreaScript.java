package com.fftools.scripts;

import com.fftools.trade.TradeAreaHandler;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.trade.Trader;

/**
 * everything you need to handle the TradeArea
 * @author Fiete
 *
 */
public class TradeAreaScript extends MatPoolScript {
	private TradeRegion tradeRegion = null;
	private Trader trader = null;
	private TradeArea tradeArea = null;
	private TradeAreaHandler tradeAreaHandler = null;
	
	
	public TradeAreaScript(){
		
	}
	
	
	public TradeRegion getTradeRegion(){
		if (this.tradeRegion==null){
			this.tradeRegion = this.getTradeAreaHandler().getTradeRegion(this.scriptUnit.getUnit().getRegion());
		}
		return this.tradeRegion;
	}
	
	
	/**
	 * Returns the Trader for this scriptunit
	 * @see Trader for more Details
	 * @return the Trader
	 */
	public Trader getTrader(){
		if (this.trader==null){
			this.trader = this.getTradeAreaHandler().addTrader(this.scriptUnit);
		}
		return this.trader;
	}
	
	/**
	 * returns the trade area of this script unit
	 * @return the TradeArea
	 */
	public TradeArea getTradeArea(){
		if (this.tradeArea==null){
			TradeAreaHandler TAH = this.getTradeAreaHandler();
			TradeRegion tR = this.getTradeRegion();
			this.tradeArea = TAH.getTradeArea(tR, true);
		}
		return this.tradeArea;
	}
	
	/**
	 * gets the overall TradeAreaHandler
	 * @return
	 */
	public TradeAreaHandler getTradeAreaHandler(){
		if (tradeAreaHandler==null){
			this.tradeAreaHandler = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler();
		} 
		return this.tradeAreaHandler;
	}
	
}
