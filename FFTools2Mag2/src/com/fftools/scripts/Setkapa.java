package com.fftools.scripts;


import magellan.library.Ship;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;




/**
 * 
 * @author Fiete
 * speziell zum setzen der Kapazität über alle anderen scripte hinweg
 */

public class Setkapa extends Script {
	
	private static final int Durchlauf = 12;
	
	private int kapa=-1;
	
	/**
	 * liefert den wahrscheinlichen Wert der resultierenden Kapa 
	 * @return
	 */
	public int getKapa() {
		return kapa;
	}

	/**
	 * wichtig: parameterloser construktor..
	 * bei jedem script dabei!
	 *
	 */
	public Setkapa() {
		super.setRunAt(Durchlauf);
	}
	
	/**
	 * 
	 * auszufuehrende methode, herz des scriptes
	 * 
	 * 
	 * 
	 */
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf!=Durchlauf){return;}
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// gibts es den kapaeintrag?
		int kapaPolicy = MatPoolRequest.KAPA_unbenutzt;
		int kapaUser=0;
		if (OP.getOptionString("kapa").length()>0){
			// ja, wir haben eine kapa angabe...
			if (OP.isOptionString("kapa", "gehen")){
				kapaPolicy = MatPoolRequest.KAPA_max_zuFuss;
			}
			if (OP.isOptionString("kapa", "reiten")){
				kapaPolicy = MatPoolRequest.KAPA_max_zuPferd;
			}
			int benutzerKapa = OP.getOptionInt("kapa", -1);
			if (benutzerKapa>0){
				kapaPolicy = MatPoolRequest.KAPA_benutzer;
				kapaUser=benutzerKapa;
			}
		}
		
		int benutzerWeight=OP.getOptionInt("gewicht", -1);
		if (benutzerWeight<=0){
			benutzerWeight=OP.getOptionInt("weight", -1);
		}
		if (benutzerWeight>0){
			kapaPolicy = MatPoolRequest.KAPA_weight;
			kapaUser=benutzerWeight;
		}
		
		if(OP.isOptionString("gewicht", "schiff") || OP.isOptionString("weight","ship")){
			// gewicht = maximale Kapa des schiffes
			Ship ship = this.scriptUnit.getUnit().getModifiedShip();
			if (ship!=null){
				// Kapitän?
				if (FFToolsUnits.checkShip(this)){
					// alles fein
					benutzerWeight = ship.getMaxCapacity();
					if (benutzerWeight>0){
						benutzerWeight=(int) Math.floor(((double)benutzerWeight)/100);
						kapaPolicy = MatPoolRequest.KAPA_weight;
						kapaUser=benutzerWeight;
						this.addComment("setKapa Schiff auf: " + benutzerWeight + " GE");
						this.scriptUnit.setIncludeSailorsWeight(true);
					} else {
						this.doNotConfirmOrders("!!! setkapa -> Schiff: Schiff hat ungültige Kapazität (" + benutzerWeight + ")");
					}
				} else {
					// Kein Kapitän
					this.doNotConfirmOrders("!!! setkapa -> Schiff: unit ist nicht Kapitän");
				}
			} else {
				// ist gar nicht auf einem Schiff
				this.doNotConfirmOrders("!!! setkapa -> Schiff: unit ist nicht auf einem Schiff");
			}
		}
		
		
		if (kapaPolicy==MatPoolRequest.KAPA_unbenutzt){
			this.doNotConfirmOrders("!!Vermuteter Fehler: setKapa und keine Kapa erkannt!");
		} else {
			this.scriptUnit.setSetKapaPolicy(kapaPolicy);
			this.kapa = this.scriptUnit.getFreeKapaMatPool2(kapaPolicy, kapaUser);
			this.addComment("SetKapa: erkannte Kapazität: " + this.kapa + " GE");
		}
		
	}
}
