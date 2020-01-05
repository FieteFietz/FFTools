package com.fftools.pools.bau;


import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Bauauftrag;
import com.fftools.scripts.Bauen;
import com.fftools.scripts.Burgenbau;
import com.fftools.scripts.Strassenbau;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeAreaHandler;

import magellan.library.CoordinateID;


/**
 *Verwaltet Bauen-scripte
 *
 */
public class BauManager implements OverlordRun,OverlordInfo{
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final int Durchlauf0 = 100;
	private static final int Durchlauf1 = 450;
	
	
	private int[] runners = {Durchlauf0,Durchlauf1};
	
	private ScriptMain scriptMain = null;
	private TradeAreaHandler tradeAreaHandler = null;

	public BauManager (ScriptMain _scriptMain){
		this.scriptMain = _scriptMain;
		this.tradeAreaHandler = this.scriptMain.getOverlord().getTradeAreaHandler();
	}
	
	
	
	public void informUs(){
		
	}
	
	
	/**
	 * Checkt den Status der bauscripte
	 *
	 */
	
	public void run(int durchlauf){
		
		// outText.addOutLine("Start Baumanager", true);
		// long StartTime=System.currentTimeMillis();
		// long ActTime=System.currentTimeMillis();
		// long DiffTime=0;
		if (this.tradeAreaHandler.getTradeAreas()==null || this.tradeAreaHandler.getTradeAreas().size()==0){
			return;
		}
		
		// outText.addOutLine("Baumanager - Anzahl TAs: " + this.tradeAreaHandler.getTradeAreas().size(), true);
		
		
		for (TradeArea tA:this.tradeAreaHandler.getTradeAreas()){
			if (tA.hasBauManager()){
				if (durchlauf==Durchlauf0){
					// ActTime=System.currentTimeMillis();
					// DiffTime = ActTime - StartTime;
					// outText.addOutLine(DiffTime + "ms: Baumanger run0 für TA " + tA.getName(), true);
					tA.getTradeAreaBauManager().run0();
				}
				if (durchlauf==Durchlauf1){
					tA.getTradeAreaBauManager().run1();
				}
			}
		}
	}
	
	
	public void addBauScript(Bauen bauen){
		TradeArea tA = this.tradeAreaHandler.getTAinRange(bauen.region());
		if (tA==null){
			outText.addOutLine("!!!addBauscript nicht erfolgreich: kein TA :" + bauen.unitDesc());
			return;
		}
		tA.getTradeAreaBauManager().addBauScript(bauen);
		bauen.addComment("zugeordnet zum TA: " + tA.getName());
	}
	
	public void addInformationListener(ScriptUnit su){
		TradeArea tA = this.tradeAreaHandler.getTAinRange(su.getUnit().getRegion());
		if (tA==null){
			outText.addOutLine("!!!addInformationListener nicht erfolgreich: kein TA :" + su.unitDesc());
			return;
		}
		tA.getTradeAreaBauManager().addInformationListener(su);
		
	}
	
	
	public void addBauAuftrag(Bauauftrag bauAuftrag){
		TradeArea tA = this.tradeAreaHandler.getTAinRange(bauAuftrag.region());
		if (tA==null){
			outText.addOutLine("!!!addBauAuftrag nicht erfolgreich: kein TA :" + bauAuftrag.unitDesc());
			return;
		}
		tA.getTradeAreaBauManager().addBauAuftrag(bauAuftrag);
		bauAuftrag.addComment("zugeordnet zum TA: " + tA.getName());
	}
	
	
	public void addBurgenbau(Burgenbau b){
		TradeArea tA = this.tradeAreaHandler.getTAinRange(b.region());
		if (tA==null){
			outText.addOutLine("!!!addBurgenbau nicht erfolgreich: kein TA :" + b.unitDesc());
			return;
		}
		tA.getTradeAreaBauManager().addBurgenBau(b);
	}
	
	public void addStrassenbau(Strassenbau b){
		TradeArea tA = this.tradeAreaHandler.getTAinRange(b.region());
		if (tA==null){
			outText.addOutLine("!!!addStrassenbau nicht erfolgreich: kein TA :" + b.unitDesc());
			return;
		}
		tA.getTradeAreaBauManager().addStrassenBau(b);
	}
	
	public void setCentralHomeDest(CoordinateID centralHomeDest,ScriptUnit fromSU){
		TradeArea tA = this.tradeAreaHandler.getTAinRange(fromSU.getUnit().getRegion());
		if (tA==null){
			outText.addOutLine("!!!addBurgenbau nicht erfolgreich: kein TA :" + fromSU.unitDesc());
			return;
		}
		tA.getTradeAreaBauManager().setCentralHomeDest(centralHomeDest);
	}
	
	
	// public void setCentralHomeDest(CoordinateID centralHomeDest) {
	
	public int[] runAt(){
		return runners;
	}
	
}
