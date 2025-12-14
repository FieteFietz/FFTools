package com.fftools.scripts;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.MatPoolManager;
import com.fftools.pools.matpool.MaterialHubManager_MHM;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsOptionParser;

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
	
	private Map<String,Integer> mprMap_Delivery = new TreeMap<String, Integer>();
	
	private int talklevel=0 ;
	/*
	 * 0 - nix - nur Start und ende
	 * 1 - Zusammenfassung
	 * 2 - alle Details
	 */
		
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
		Map<String,Integer> mprMap = new TreeMap<String, Integer>();
		
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
						if (mpr.getScript() instanceof Material && mpr.getOriginalGefordert()!=Integer.MAX_VALUE) {
							// untersuchen
							int TM_amount=0;
							if (mpr.getTransportRequest()!=null){
								TM_amount=mpr.getTransportRequest().getBearbeitet();
							}
							
							int Fehlbestand = mpr.getOriginalGefordert() - (mpr.getBearbeitet() + TM_amount);
							if (talklevel>=2) {
								this.addComment("MH: untersuche Einheit: " + mpr.getScriptUnit().toString() + " | " + mpr.getOriginalGefordert() + " " + mpr.getOriginalGegenstand() + " MP: " + mpr.getBearbeitet() + " TM: " + TM_amount + " | Fehlbestand: " + Fehlbestand);
							}
							if (Fehlbestand>0) {
								Integer GesamtFehlbestand = mprMap.get(mpr.getOriginalGegenstand());
								if (GesamtFehlbestand != null) {
									GesamtFehlbestand += Fehlbestand;
								} else {
									GesamtFehlbestand = Fehlbestand;
								}
								mprMap.put(mpr.getOriginalGegenstand(), GesamtFehlbestand);
							}
						}
					}
				}
			}
		}
		
		// Ausgabe der TA- Summary
		if (talklevel>=1) {
			if (mprMap.size()>0) {
				this.addComment("MH: Fehlbestand im TA - aufsummiert.");
				for (Map.Entry<String, ?> entry : mprMap.entrySet()) {
					this.addComment("MH: " + entry.getKey() + ": " + entry.getValue());
				}
			} else {
				this.addComment("MH: kein Fehlbestand im TA - traumhaft.");
			}
			
			// Ankündigung der Lieferungen
			if (this.mprMap_Delivery.size()>0) {
				this.addComment("MH: Lieferungen unterwegs zu diesem MH - aufsummiert.");
				for (Map.Entry<String, ?> entry : this.mprMap_Delivery.entrySet()) {
					this.addComment("MH: " + entry.getKey() + ": " + entry.getValue());
				}
			} else {
				this.addComment("MH: kein Lieferungen zu diesem MH anzukündigen.");
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
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
}
