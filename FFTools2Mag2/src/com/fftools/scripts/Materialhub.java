package com.fftools.scripts;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.MatPoolManager;
import com.fftools.pools.matpool.MaterialHubManager_MHM;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsOptionParser;

import magellan.library.CoordinateID;
import magellan.library.Region;



/**
 * Verwaltet Materialforderungen im TA
 * @author Fiete
 *
 */
public class Materialhub extends MatPoolScript {
	
	/*
	 * TransportManager läuft erst bei 500 und muss vorher laufen
	 * dann dann weiss script Material rest. welche Güter im TA zur Befriedigung der Anfragen unterwegs ist
	 */
	
	private static final int Durchlauf_01 = 20; // vor MT
	private static final int Durchlauf_02 = 510;
	
	private int[] runners = {Durchlauf_01,Durchlauf_02};
	
	private Map<String,Integer> mprMap = new TreeMap<String, Integer>();
	private Map<String,Integer> mprMap_Delivery = new TreeMap<String, Integer>();
	private Map<String,Integer> mprMap_needAfterDelivery = new TreeMap<String, Integer>();
	private Map<String,Integer> mprMap_HOME_Decisions = new TreeMap<String, Integer>();
	private Map<String,Integer> mprMap_AFTER_HOME_Decisions = new TreeMap<String, Integer>();
	
	private int talklevel=0 ;
	/*
	 * 0 - nix - nur Start und ende
	 * 1 - Zusammenfassung
	 * 2 - alle Details
	 */
	
	private TradeArea myTA = null;
	private CoordinateID HOME_region = null;
	
	private DecimalFormat dfTausend = new DecimalFormat("#,###.000");
	
	public double actMaterialScore = 0.0;
	private boolean final_processed_by_MHM = false;
		
	public Materialhub(){
		super.setRunAt(this.runners);
	}
	
	public void runScript(int scriptDurchlauf){
		switch (scriptDurchlauf){
			case Durchlauf_01:this.run0();break;
			case Durchlauf_02:this.run1();break; 
		}
	}
	
	private void run0(){
		super.addVersionInfo();
		// nur beim MHM anmelden
		MaterialHubManager_MHM MHM = this.getOverlord().getMaterialHubManager();
		MHM.addMH(this);
	}
	
	private void run1(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Material");
		OP.addOptionList(this.getArguments());
		talklevel = OP.getOptionInt("talklevel", 0);
		this.addComment("MaterialHub (MH) start. Talklevel=" + talklevel);
		// nenne TA
		TradeRegion TR = getOverlord().getTradeAreaHandler().getTradeRegion(this.scriptUnit.getUnit().getRegion());
		TradeArea TA = null;
		if (TR!=null){
			TA = getOverlord().getTradeAreaHandler().getTradeArea(TR,false);
			if (TA!=null){
				if (talklevel>=0) {
					addComment("MH: Zugeordnetes TradeArea: " + TA.getName() + " (def in Region: " + TA.getOriginRegion().toString()+")");
					this.myTA = TA;
				}
			} else {
				this.doNotConfirmOrders("!! MaterialHub: !! kein TA !!");
				return;
			}
		} else {
			this.doNotConfirmOrders("!! MaterialHub: !! keine TR !!");
			return;
		}
		// check - nur 1 matHub pro TA
		if (TA.hasMaterialHub()) {
			this.doNotConfirmOrders("!! MaterialHub: !! TA hat bereits ein MaterialHub (MH): " + TA.getMaterialHub().toString());
			return;
		}
		TA.setMaterialHub(this.scriptUnit);
		
		MatPoolManager MPM = getOverlord().getMatPoolManager();
		// HashMap<String,Integer> mprMap = new HashMap<String, Integer>();
		// Map<String,Integer> mprMap = new TreeMap<String, Integer>();
		
		if (TA.getRegionIterator()!=null) {
			for (Iterator<TradeRegion> iter = TA.getRegionIterator();iter.hasNext();){
				TradeRegion actTR = (TradeRegion)iter.next();
				Region actR = actTR.getRegion();
				if (talklevel>=2) {
					this.addComment("MH: bearbeite Region " + actR.toString());
				}
				MatPool mp = MPM.getRegionsMatPool(actR);
				// jetzt durch die request des mp gehen: nicht alles, nur script material
				if (mp.getRequests()!=null) {
					for (MatPoolRequest mpr : mp.getRequests()) {
						if ((mpr.getScript() instanceof Material || mpr.isMH()) && mpr.getOriginalGefordert()!=Integer.MAX_VALUE) {
							// untersuchen
							int TM_amount=0;
							if (mpr.getTransportRequest()!=null){
								TM_amount=mpr.getTransportRequest().getBearbeitet();
							}
							
							int Fehlbestand = mpr.getOriginalGefordert() - (mpr.getBearbeitet() + TM_amount);
							if (talklevel>=2) {
								this.addComment("MH: untersuche Einheit: " + mpr.getScriptUnit().toString() + " | " + mpr.getOriginalGefordert() + " " + mpr.getOriginalGegenstand() + " MP: " + mpr.getBearbeitet() + " TM: " + TM_amount + " | Fehlbestand: " + Fehlbestand);
								if (mpr.isMH()) {
									this.addComment("Info: per Option MH relevant");
								}
							}
							if (Fehlbestand>0) {
								Integer GesamtFehlbestand = this.mprMap.get(mpr.getOriginalGegenstand());
								if (GesamtFehlbestand != null) {
									GesamtFehlbestand += Fehlbestand;
								} else {
									GesamtFehlbestand = Fehlbestand;
								}
								this.mprMap.put(mpr.getOriginalGegenstand(), GesamtFehlbestand);
							}
						}
					}
				}
			}
		}
		
		// Ausgabe der TA- Summary
		if (talklevel>=1) {
			if (this.mprMap.size()>0) {
				this.addComment("MH: Fehlbestand im TA - aufsummiert.");
				for (Map.Entry<String, ?> entry : this.mprMap.entrySet()) {
					this.addComment("MH_need: " + entry.getKey() + ": " + entry.getValue());
				}
			} else {
				this.addComment("MH: kein Fehlbestand im TA - traumhaft.");
			}
			
			// Ankündigung der Lieferungen
			if (this.mprMap_Delivery.size()>0) {
				this.addComment("MH: Lieferungen unterwegs zu diesem MH - aufsummiert.");
				for (Map.Entry<String, ?> entry : this.mprMap_Delivery.entrySet()) {
					this.addComment("MH_deliver: " + entry.getKey() + ": " + entry.getValue());
				}
			} else {
				this.addComment("MH: kein Lieferungen zu diesem MH anzukündigen.");
			}
		}
		
		// mprMap_needAfterDelivery
		if (this.mprMap.size()>0) {
			for (Map.Entry<String, ?> entry : this.mprMap.entrySet()) {
				// this.addComment("MH: " + entry.getKey() + ": " + entry.getValue());
				int neededValue = (int) entry.getValue();
				int deliveredValue = 0;
				if (this.mprMap_Delivery.get(entry.getKey())!=null) {
					deliveredValue = (int) this.mprMap_Delivery.get(entry.getKey());
				}
				int remainingNeedValue = neededValue - deliveredValue;
				this.mprMap_needAfterDelivery.put(entry.getKey(), remainingNeedValue);
			}
		}
		if (talklevel>=1 && this.mprMap_needAfterDelivery.size()>0 && this.mprMap_Delivery.size()>0) {
			this.addComment("MH: verbleibender Bedarf nach angekündigten Lieferungen");
			for (Map.Entry<String, ?> entry : this.mprMap_needAfterDelivery.entrySet()) {
				this.addComment("MH_need_after_Delivery: " + entry.getKey() + ": " + entry.getValue());
			}
		}
		
		// mprMap_AFTER_HOME_Decisions vorab füllen
		if (this.mprMap_needAfterDelivery.size()>0) {
			this.mprMap_AFTER_HOME_Decisions = new TreeMap<String, Integer>(this.mprMap_needAfterDelivery);
		} else {
			if (this.mprMap.size()>0) {
				this.mprMap_AFTER_HOME_Decisions = new TreeMap<String, Integer>(this.mprMap);
			}
		}
	
		
		if (talklevel>=0) {
			this.addComment("MH: ende");
		}
		
	}
	
	
	public void addDelivery(String Gegenstand, int Anzahl_Delivery) {
		Integer GesamtFehlbestand = this.mprMap_Delivery.get(Gegenstand);
		if (GesamtFehlbestand != null) {
			GesamtFehlbestand += Anzahl_Delivery;
		} else {
			GesamtFehlbestand = Anzahl_Delivery;
		}
		this.mprMap_Delivery.put(Gegenstand, GesamtFehlbestand);
	}
	
	public void addFromHOME(String Gegenstand, int Anzahl_Delivery) {
		Integer GesamtHomeLieferung = this.mprMap_HOME_Decisions.get(Gegenstand);
		if (GesamtHomeLieferung != null) {
			GesamtHomeLieferung += Anzahl_Delivery;
		} else {
			GesamtHomeLieferung = Anzahl_Delivery;
		}
		this.mprMap_HOME_Decisions.put(Gegenstand, GesamtHomeLieferung);
		
		Integer GesamtHomeBedarf = this.mprMap_AFTER_HOME_Decisions.get(Gegenstand);
		if (GesamtHomeBedarf != null) {
			GesamtHomeBedarf -= Anzahl_Delivery;
			this.mprMap_AFTER_HOME_Decisions.put(Gegenstand, GesamtHomeBedarf);
		} 
	}
	
	public String getTAName() {
		if (this.myTA!=null) {
			return myTA.getName();
		}
		return "error_NO_TA";
	}
	
	
	
	public double getScoreOfMPR_mprMap_AFTER_HOME_Decisions(ScriptUnit su, int loopIndex) {
		MaterialHubManager_MHM MHM = this.getOverlord().getMaterialHubManager();
    	double res = 0.0;
    	if (su!=null && talklevel>=2) {
    		su.addComment("MHM - Berechnung Score Material Anforderung: (loop " + loopIndex + ")");
    	}
    	for (Map.Entry<String, Integer> entry : this.mprMap_AFTER_HOME_Decisions.entrySet()) {
    		double score = MHM.getMaterialScore(entry.getKey());
    		double add = score * entry.getValue();
    		res += add;
    		if (su!=null && talklevel>=2) {
    			su.addComment(entry.getKey() +  ": " + dfTausend.format(add) + " (" + dfTausend.format(score) + " x " + entry.getValue() + ")");
    		}
		}
    	
    	if (su!=null && talklevel>=2) {
    		su.addComment("MHM - score final: " + dfTausend.format(res) + " (loop " + loopIndex + ")");
    	}
    	
    	this.actMaterialScore = res;
    	
    	return res;
    }
	
	
	
	public void setHOME_Region(CoordinateID r) {
		this.HOME_region = r;
	}
	
	public CoordinateID getHOME_Region() {
		return this.HOME_region;
	}
	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}

	public boolean isFinal_processed_by_MHM() {
		return final_processed_by_MHM;
	}

	public void setFinal_processed_by_MHM(boolean final_processed_by_MHM) {
		this.final_processed_by_MHM = final_processed_by_MHM;
	}

	public Map<String, Integer> getMprMap_AFTER_HOME_Decisions() {
		return mprMap_AFTER_HOME_Decisions;
	}
	
	private boolean has_received_from_HOME() {
		if (this.mprMap_HOME_Decisions.size()>0) {
			return true;
		}
		return false;
	}
	
	public void inform_HOME_Decisions() {
		if (talklevel>=1) {
			if (!has_received_from_HOME()) {
				this.addComment("MH: keine Lieferung aus der HOME Region diese Runde neu festgelegt");
			} else {
				this.addComment("MH: Lieferungen aus der HOME Region dieser Runde:");
				for (Map.Entry<String, ?> entry : this.mprMap_HOME_Decisions.entrySet()) {
					this.addComment("MH_HOME this round: " + entry.getKey() + ": " + entry.getValue());
				}
				this.addComment("MH: verbleibender Bedarf Total");
				for (Map.Entry<String, ?> entry : this.mprMap_AFTER_HOME_Decisions.entrySet()) {
					this.addComment("MH_need_after_HOME: " + entry.getKey() + ": " + entry.getValue());
				}
				
			}
		}
	}
	
	public int getTalkLevel() {
		return talklevel;
	}
	
}
