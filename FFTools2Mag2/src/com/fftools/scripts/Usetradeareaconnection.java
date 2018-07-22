package com.fftools.scripts;

import magellan.library.Unit;

import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.MatPoolManager;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeAreaConnector;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;



/**
 * 
 * Definiert eine Handelsbeziehung zwischen 2 TradeAreas anhand 2er Units
 * 
 * @author Fiete
 *
 */

public class Usetradeareaconnection extends TradeAreaScript{
	
	

	private static final int Durchlauf_vorMP1 = 104;
	
	private static final int default_request_prio=50;
	
	
	private int[] runners = {Durchlauf_vorMP1};
	
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Usetradeareaconnection() {
		super.setRunAt(this.runners);
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
		
		if (scriptDurchlauf==Durchlauf_vorMP1){
			this.scriptStart();
		}
		
		
		
		
	}
	
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Pflichtfelder
		// TAC-Name
		String Name = OP.getOptionString("name");
		if (Name.length()<2){
			this.doNotConfirmOrders();
			this.addComment("!!! setTAC: Name fehlt!!! -> unbestaetigt");
			return;
		}
		// gibts den namen
		TradeAreaConnector myTAC = super.getTradeAreaHandler().getTAC(Name);
		if (myTAC==null){
			this.addComment("!useTAC gescheitert: so einen TAC gibt es nicht: " + Name);
			this.doNotConfirmOrders();
			return;
		}
		if (!myTAC.isValid()){
			this.addComment("!useTAC gescheitert: der TAC ist ungültig : " + Name);
			this.doNotConfirmOrders();
			return;
		}
		
		// Ware
		String Ware = OP.getOptionString("Ware");
		if (Ware.length()<2){
			this.addComment("!useTAC gescheitert: Ware nicht erkannt : " + Ware);
			this.doNotConfirmOrders();
			return;
		}
		
		// Summe
		int Summe = OP.getOptionInt("Summe",-1);
		if (Summe<1){
			Summe = OP.getOptionInt("Menge",-1);
			if (Summe<1){
				this.addComment("!useTAC gescheitert: Summe nicht erkannt : " + Summe);
				this.doNotConfirmOrders();
				return;
			}
		}
		
		// prio 
		int prio = OP.getOptionInt("Prio",-1);
		if (prio<1){
			prio = default_request_prio;
			this.addComment("useTAC: benutze für " + Ware + " auf " + Name + " Defaultprio: " + prio);
		} else {
			this.addComment("useTAC: benutze für " + Ware + " auf " + Name + " gesetzte Prio: " + prio);
		}
		
		// prio 
		int prioTM = OP.getOptionInt("PrioTM",-1);
		if (prioTM<1){
			prioTM = prio;
		} else {
			this.addComment("useTAC: benutze für " + Ware + " auf " + Name + " gesetzte PrioTM: " + prioTM);
		}
		
		boolean useIT = true;
		// 20161019
		if (OP.getOptionBoolean("depotAusgleich", false)){
			this.addComment("Depotausgleich für " + Summe + " " + Ware + " nach " + Name + " erkannt. Prüfe Vorraussetzungen...",false);
			String realWare = FFToolsGameData.translateItemShortform(Ware);
			if (!realWare.equalsIgnoreCase(Ware)){
				this.addComment("Name der Ware geändert in: " + realWare,false);
				Ware=realWare;
			}
				
			realWare = Ware.replace("_", " ");
			if (!realWare.equalsIgnoreCase(Ware)){
				this.addComment("Name der Ware geändert in: " + realWare,false);
				Ware=realWare;
			}
			
			// checken wir das Depot hier..dass muss die ware haben
			MatPool MP = this.getMatPool();
			int AnzahlHier = 0;
			int AnzahlDa = 0;
			ScriptUnit depotSU = null;
			if (MP!=null){
				depotSU = MP.getDepotUnit();
				if (depotSU!=null){
					Unit depotU = depotSU.getUnit();
					AnzahlHier = FFToolsUnits.getAmountOfWare(depotSU, Ware);
					this.addComment("Beim hiesigen Depot " + depotU.toString(true) + " wurden " + AnzahlHier + " " + Ware + " erkannt",false);
				}
			}
			if (AnzahlHier>=Summe){
				// checken wir das Gegendepot, dass muss weniger Haben...
				ScriptUnit gegenSU = myTAC.getSU2(); 
				if (gegenSU!=null){
					if (gegenSU.getUnit().getRegion().equals(depotSU.getUnit().getRegion())){
						// vertauschte Units...
						gegenSU = myTAC.getSU1(); 
					}
					if (gegenSU!=null){
						MatPoolManager MMM = gegenSU.getScriptMain().getOverlord().getMatPoolManager();
						MatPool MP2 = MMM.getRegionsMatPool(gegenSU);
						if (MP2!=null){
							ScriptUnit depotSU2 = MP2.getDepotUnit();
							if (depotSU2!=null){
								Unit depotU2 = depotSU2.getUnit();
								AnzahlDa = FFToolsUnits.getAmountOfWare(depotSU2, Ware);
								this.addComment("Beim dortigen Depot " + depotU2.toString(true) + " wurden " + AnzahlDa + " " + Ware + " erkannt",false);
								if (AnzahlDa<Summe){
									this.addComment("Fazit: depotAusgleich ist aktiv für " + Ware,false);
									// Summe anpassen?
									if (AnzahlDa>0){
										Summe = Summe - AnzahlDa;
										this.addComment("Menge angepasst auf " + Summe + " Ware.",false);
									}
								} else {
									this.addComment("Fazit: depotAusgleich ist NICHT aktiv für " + Ware,false);
									useIT=false;
								}
							} else {
								this.addComment("!!!depotausgleich " + Ware + ": kein Depot-Scriptunit auf der Gegenseite gefunden.",false);
								useIT=false;
							}
						} else {
							this.addComment("!!!depotausgleich " + Ware + ": kein Materialpool auf der Gegenseite gefunden.",false);
							useIT=false;
						}
					} else {
						this.addComment("!!!depotausgleich " + Ware + ": keine ScriptUnit auf der Gegenseite gefunden.",false);
						useIT=false;
					}
				} else {
					this.addComment("!!!depotausgleich " + Ware + ": keine ScriptUnit auf der Gegenseite gefunden.",false);
					useIT=false;
				}
			} else {
				this.addComment("Beim hiesigen Depot liegt zu wenig " + Ware + ", kein Depotausgleich möglich.",false);
				useIT=false;
			}
		}
		
		
		if (useIT){
			TradeArea myTA = this.getTradeArea();
			// Ergänzen
			myTAC.addUsage(myTA, Ware, Summe, prio,prioTM);
		}
		
	}
	
	
	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}

}
