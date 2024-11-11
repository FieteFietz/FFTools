package com.fftools.scripts;

import com.fftools.pools.bau.TradeAreaBauManager;
import com.fftools.trade.TradeArea;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;

import magellan.library.CoordinateID;
import magellan.library.Region;


/**
 * 
 * St�sst den automatischen Strassenbau in dem TA an
 * @author Fiete
 *
 */

public class Strassenbau extends MatPoolScript{
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf1 = 90;
	
	private int[] runners = {Durchlauf1};
	
	/**
	 * wieviel Strassenauftr�ge werden automatisch vergeben ?
	 */
	private int AnzahlStrassen = 3;

	/**
	 * wieviel Geb�udeauftr�ge werden automatisch vergeben ?
	 */
	private int AnzahlGeb�ude = 1;
	
	
	public int getAnzahlStrassen() {
		return AnzahlStrassen;
	}


	/**
	 * Mit welcher Prio fordert der wichtigste Bauauftrag an?
	 */
	private int PrioStrassen = 100;
	
	
	public int getPrioStrassen() {
		return PrioStrassen;
	}
	
	/**
	 * Mit welcher Prio fordert der wichtigste geb�ude-Bauauftrag an?
	 */
	private int PrioGeb�ude = 90;
	
	
	public int getPrioGeb�ude() {
		return PrioGeb�ude;
	}
	
	/**
	 * wenn nix mehr zu tun ist - eingeit trotzdem best�tigen?
	 */
	private boolean confirmUnemployed=false;


	// Konstruktor
	public Strassenbau() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf==Durchlauf1){
			this.run1();
		}
	}
	
	
	private void run1(){
		
		super.addVersionInfo();
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		
		
		this.AnzahlStrassen = OP.getOptionInt("AnzahlStrassen", this.AnzahlStrassen);
		this.AnzahlGeb�ude = OP.getOptionInt("Anzahlgeb�ude", this.AnzahlGeb�ude);
		this.PrioStrassen = OP.getOptionInt("PrioStrassen", this.PrioStrassen);
		this.PrioGeb�ude = OP.getOptionInt("PrioGeb�ude", this.PrioGeb�ude);
		
		this.addComment("Stra�enbau-Einstellungen: " + this.AnzahlStrassen + " Stra�en mit Prio " + this.PrioStrassen + ", " + this.AnzahlGeb�ude + " Geb�ude mit Prio " + this.PrioGeb�ude );
		
		this.confirmUnemployed = OP.getOptionBoolean("confirmUnemployed", this.confirmUnemployed);
		
		// notIn, kein Strassenbau
		String notIn = OP.getOptionString("notIn");
		if (notIn.length()==0) {
			notIn = OP.getOptionString("keineStrasse");
		}
		TradeArea TA = null;
		TradeAreaBauManager TABM = null;
		if (notIn.length()>0) {
			// this.addComment("Strassenbau: notIn erkannt:" + notIn);
			// X1,Y1|X2,Y2|X3,Y3
			String[] tokens = notIn.split("\\|");
			if (tokens.length>0) {
				for (int i = 0; i < tokens.length; i++) {
					// this.addComment("Strassenbau: notIn i:" + i);
					String s1 = tokens[i];
					// X1,Y1
					// this.addComment("untersuche notIn " + s1,false);
					String[] koords = s1.split(",");
					if (koords.length==2) {
						CoordinateID actDest = CoordinateID.parse(s1,",");
						if (actDest!=null) {
							Region actR = this.gd_Script.getRegion(actDest);
							if (TA==null) {
								TA = this.getOverlord().getTradeAreaHandler().getTAinRange(this.region());
							}
							if (TA!=null){
								if (TABM==null) {
									TABM =  TA.getTradeAreaBauManager();
								}
								TABM.addKeinStrassenbau(actR);
								this.addComment("Stra�enbau: notIn erkannt f�r: " + actR.toString());
							} else {
								this.doNotConfirmOrders("TA in Range not found ?! - could not add notIn-Region");
								return;
							}
							
						} else {
							this.doNotConfirmOrders("Stra�enbau - notIn: Bestandteil " + s1 + " kann nicht als X,Y erkannt werden (Parser-Fehler");
							return;
						}
						
					} else {
						this.doNotConfirmOrders("Stra�enbau - notIn: Bestandteil " + s1 + " kann nicht als X,Y erkannt werden");
						return;
					}
				}
			} else {
				this.doNotConfirmOrders("Stra�enbau - notIn: Liste der Regionen hat keinen Eintrag");
				return;
			}
		}
		
		// home
		String homeString=OP.getOptionString("home");
		if (homeString.length()>2){
			CoordinateID actDest = null;
			if (homeString.indexOf(',') > 0) {
				actDest = CoordinateID.parse(homeString,",");
			} else {
			// Keine Koordinaten, also Region in Koordinaten konvertieren
				actDest = FFToolsRegions.getRegionCoordFromName(this.gd_Script, homeString);
			}
			if (actDest!=null){
				this.getBauManager().setCentralHomeDest(actDest, this.scriptUnit);
			} else {
				this.doNotConfirmOrders("!!! HOME Angabe nicht erkannt!");
			}
		}
		
		// Lernplanname
		String Lernplanname=OP.getOptionString("Lernplan");
		if (Lernplanname.length()>2){
			if (TA == null) {
				TA = this.getOverlord().getTradeAreaHandler().getTAinRange(this.region());
			}
			if (TA!=null){
				if (TABM==null) {
					TABM =  TA.getTradeAreaBauManager();
				}
				TABM.setLernplanname(Lernplanname);
				this.addComment("Lernplan " + Lernplanname + " gesetzt f�r automatische Bauarbeiter im TA " + TA.getName());
			} else {
				this.doNotConfirmOrders("TA in Range not found ?! - could not set Lernplan");
			}
		}
		
		this.getBauManager().addStrassenbau(this);
	}	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}


	public int getAnzahlGeb�ude() {
		return AnzahlGeb�ude;
	}


	public boolean isConfirmUnemployed() {
		return confirmUnemployed;
	}


	public void setConfirmUnemployed(boolean confirmUnemployed) {
		this.confirmUnemployed = confirmUnemployed;
	}



}
