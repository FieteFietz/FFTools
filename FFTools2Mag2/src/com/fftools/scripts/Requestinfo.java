package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Iterator;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;




public class Requestinfo extends MatPoolScript{
	
	
	private static final int Durchlauf = 860;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Requestinfo() {
		super.setRunAt(Durchlauf);
	}
	
	
	/**
	 * Eigentliche Prozedur
	 * runScript von Script.java MUSS/SOLLTE ueberschrieben werden
	 * Durchlauf kommt von ScriptMain
	 * 
	 * in Script steckt die ScriptUnit
	 * in der ScriptUnit steckt die Unit
	 * mit addOutLine jederzeit Textausgabe ans Fenster
	 * mit addComment Kommentare...siehe Script.java
	 */
	
	public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf){
			this.scriptStart();
		}
	}
	
	private void scriptStart(){
		
		// nenne TA
		TradeRegion TR = getOverlord().getTradeAreaHandler().getTradeRegion(this.scriptUnit.getUnit().getRegion());
		if (TR!=null){
			TradeArea TA = getOverlord().getTradeAreaHandler().getTradeArea(TR,false);
			if (TA!=null){
				addComment("Zugeordnetes TradeArea: " + TA.getName() + " (def in Region: " + TA.getOriginRegion().toString()+")");
			} else {
				addComment("!! kein TA !!");
			}
		} else {
			addComment("!! keine TR !!");
		}
		ArrayList<MatPoolRequest> MPRs = new ArrayList<MatPoolRequest>();
		MPRs = getOverlord().getMatPoolManager().getRequests(this.scriptUnit);
		if (MPRs==null || MPRs.size()==0){
			addComment("RequestInfo: kein MP requests");
		} else {
			for (Iterator<MatPoolRequest> iter = MPRs.iterator();iter.hasNext();){
				MatPoolRequest MPR = (MatPoolRequest)iter.next();
				String s = "RequI:";
				if (MPR.getOriginalGefordert()!=Integer.MAX_VALUE){
					s += MPR.getOriginalGefordert();
				} else {
					s+= "ALLES";
				}
				s += " " + MPR.getOriginalGegenstand();
				s += "(Prio" + MPR.getPrio() + "), OKs MP:" + MPR.getBearbeitet()+ ",TM:";
				if (MPR.getTransportRequest()==null){
					s+="-";
				} else {
					s+=MPR.getTransportRequest().getBearbeitet();
				}
				
				if (MPR.getPrioTM()!=MPR.getPrio()){
					s+=" (prioTM " + MPR.getPrioTM() + ")";
				}
				
				s += ", " + MPR.getKommentar();
				this.addComment(s);
			}
		}
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
	/**
	 * kein meckern, wenn doch 2. script dazu
	 */
	public boolean errorMsgIfNotAllowedAddedScript(){
		return false;
	}
	
}
