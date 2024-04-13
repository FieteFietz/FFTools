package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;

import magellan.library.Ship;
import magellan.library.rules.ShipType;

public class Seewerft extends MatPoolScript{
	
	
	private static final int Durchlauf_vorMP = 53;
	private static final int Durchlauf_nachMP = 62;
	
	private int[] runners = {Durchlauf_vorMP,Durchlauf_nachMP};
	
	// wird vom Reparaturschiff gelesen, wenn true -> RTB
	public boolean needNewWood = false;
	
	private int minHolz=0; 	// Mindestbestand, wenn erreicht oder unterschritten -> RTB
	private int maxHolz=0; 	// wird immer angeforert (mit prio)
	private int prio=75;	// für die Holzanforderung 
	
	private MatPoolRequest MPR = null; // die Holzanforderung
	
	private String Lernplan="";
	private String Lerntalent="";
	
	private int BauPunkte = 0;
	private int TalentLevel = 0;  // modified Talent Schiffbau
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Seewerft() {
		super.setRunAt(this.runners);
	}
	
	
	/**
	 */
	
	public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_vorMP){
			this.scriptStart();
		}
		
		if (scriptDurchlauf==Durchlauf_nachMP){
			this.scriptEnde();
		}
	}
	
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Seewerft");
		OP.addOptionList(this.getArguments());
		
		this.TalentLevel = this.scriptUnit.getSkillLevel("Schiffbau");
		
		Ship s = this.getUnit().getModifiedShip();
		if (s==null) {
			this.doNotConfirmOrders("!!! Seewerft ist nicht auf einem Schiff!");
			return;
		}
		
		int Peff = 0; // effektive Personenanzahl
		Peff = FFToolsUnits.getPersonenEffektiv(this.scriptUnit);
		this.BauPunkte = this.TalentLevel * Peff ;
		
		int setMaxHolz = OP.getOptionInt("maxHolz", 0);
		if (setMaxHolz>0 && setMaxHolz<300000) {
			this.maxHolz = setMaxHolz;
			this.addComment("max Holz gesetzt auf: " + this.maxHolz);
		} else {
			this.doNotConfirmOrders("!!! maxHolz nicht erkannt!");
			return;
		}
		
		int setMinHolz = OP.getOptionInt("minHolz", -1);
		if (setMinHolz>-1 && setMinHolz<300000 && setMinHolz<this.maxHolz) {
			this.minHolz = setMinHolz;
			this.addComment("min Holz gesetzt auf: " + this.minHolz);
		} else {
			this.addComment("min Holz auf Standard: " + this.minHolz);
		}
		
		int setPrio = OP.getOptionInt("prio", 0);
		if (setPrio>0 && setPrio<3000) {
			this.prio = setPrio;
			this.addComment("prio gesetzt auf: " + this.prio);
		} else {
			this.addComment("prio auf Standard: " + this.prio);
		}
		
		this.Lernplan = OP.getOptionString("Lernplan");
		this.Lerntalent = OP.getOptionString("Talent");
		
		// Holz anfordern
		this.MPR = new MatPoolRequest(this,this.maxHolz,"Holz",this.prio,"Seewerft");
		this.addMatPoolRequest(this.MPR);
		
		
	}
	
	
	private void scriptEnde(){
		// abbruch, wenn wir keinen Holz-request haben
		if (this.MPR==null) {
			return;
		}
		
		int Holz_verfügbar = 0;
		Holz_verfügbar = this.MPR.getBearbeitet();
		
		this.addComment("Seewerft: Holz verfügbar nach MatPool: " + Holz_verfügbar);
		if (Holz_verfügbar<=this.minHolz && Holz_verfügbar>0) {
			// Mindestbestand unterschritten
			this.addComment("Seewerft: Mindestbestand Holz unterschritten, fordere RTB an");
			this.needNewWood=true;
		}
		
		if (Holz_verfügbar==0) {
			// nix weiter zu tun
			// Lernen
			this.addComment("Seewerft: kein Holz verfügbar, ich lerne (RTB angefordert)");
			this.needNewWood=true;
			Lernen();
			return;
		}
		
		// Anmeldung beim SWM
		this.getOverlord().getSWM().addSeewerft(this);
		
	}
	
	public void orderLernen() {
		this.addComment("SWM: Lernen befohlen");
		Lernen();
	}
	
	private void Lernen() {
		String LernfixOrder=null;
		if (this.Lernplan.length()>2) {
			LernfixOrder="Lernplan=" + this.Lernplan;
		}
		if (LernfixOrder==null && this.Lerntalent.length()>2) {
			LernfixOrder="Talent=" + this.Lerntalent;
		}
		if (LernfixOrder==null) {
			this.doNotConfirmOrders("Seewerft soll Lernen, aber was denn?");
			return;
		}
		this.scriptUnit.addComment("Lernfix wird initialisiert mit dem Parameter: " + LernfixOrder);
		Lernfix L = new Lernfix();
		ArrayList<String> order = new ArrayList<String>();
		order.add(LernfixOrder);
		L.setArguments(order);
		L.setScriptUnit(this.scriptUnit);
		L.setAvoidAka(true);
		L.setGameData(this.scriptUnit.getScriptMain().gd_ScriptMain);
		if (this.scriptUnit.getScriptMain().client!=null){
			L.setClient(this.scriptUnit.getScriptMain().client);
		}
		this.scriptUnit.addAScript(L);
	}
	
	/*
	 * liefert finale Leistungsbereitschaft
	 */
	public int getBauPunkteMitHolz(){
		int erg = 0;
		
		if (this.MPR!=null){
			int HolzErwartet = this.MPR.getBearbeitet();
			this.addComment("Werft erwartet " + HolzErwartet + " Holz.");
			erg = HolzErwartet;
		}
		return erg;
	}
	
	
	/*
	 * liefert finale Leistungsbereitschaft für einen Schiffstyp
	 */
	public int getBauPunkteMitHolz(ShipType sT){
		int erg = 0;
		
		int TalentStufe = sT.getBuildSkillLevel();
		if (TalentStufe>0 && this.TalentLevel>=TalentStufe) {
			int TalentBP = (int) Math.floor(this.BauPunkte / TalentStufe);
			return Math.min(TalentBP, this.getBauPunkteMitHolz());
		}
		return erg;

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
