package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.pools.bau.WerftManager;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;

import magellan.library.Ship;
import magellan.library.rules.ShipType;




public class Werft extends MatPoolScript{
	
	
	private static final int Durchlauf = 210;  // nach erstem MP, wenn RdfF schon bei modified sind... | vor Werftmanager
	private static final int Durchlauf_last = 855;
	private int[] runsAt = {Durchlauf,Durchlauf_last};
	
	private MatPoolRequest MPR;
	
	private int DefaultPrio = 100;
	
	public String LernTalent="Schiffbau" ;
	
	public boolean showInfos = true;
	
	public ShipType shipType = null;
	
	public boolean hasLernfixOrder = false;
	
	private int BauPunkte = 0;
	
	private int TalentLevel = 0;  // modified Talent Schiffbau
	
	private int minTalentLevel = 1; // mindestens zu habender Level Schiffbau 
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Werft() {
		super.setRunAt(this.runsAt);
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
		if (scriptDurchlauf==Durchlauf_last){
			this.scriptSchluss();
		}
	}
	
	/*
	 * werft-scripte verlassen grundsätzlich und warnen, wenn das schiff nicht anders besetzt wird
	 */
	private void scriptSchluss(){
		
		if (this.hasLernfixOrder) {
			return;
		}
		
		// waren wir am anfang der runde auf einem Schiff?
		Ship s = this.scriptUnit.getUnit().getShip();
		if (s!=null){
			// sind am ende der runde noch andere an Bord?
			if (s.modifiedUnits().size()==0){
				// scheinbar nicht
				this.doNotConfirmOrders("!!! Schiff verbleibt unbesetzt !!!");
			}
		}
	}
	
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		this.TalentLevel = this.scriptUnit.getSkillLevel("Schiffbau");
		
		this.minTalentLevel = OP.getOptionInt("minTalent", this.minTalentLevel);
		
		if (this.TalentLevel<this.minTalentLevel) {
			this.addComment("Talentlevel unter mindestTalent..Lerne " + this.LernTalent);
			// w.scriptUnit.findScriptClass("Lernfix", "Talent=" + w.LernTalent);
			Lernfix LF = new Lernfix();
			LF.setScriptUnit(this.scriptUnit);
			if (this.c!=null){
				LF.setClient(this.c);
			}
			LF.setGameData(this.gd_Script);
			ArrayList<String> ll = new ArrayList<String>();
			ll.add("Talent=Schiffbau");
			LF.setArguments(ll);
			LF.scriptStart();
			this.scriptUnit.addAScript(LF);
			this.hasLernfixOrder=true;
			return;
		}
		
		
		// Werftmanager besorgen und sich dort anmelden
		WerftManager WM = this.getOverlord().getWerftManager();
		WM.addWerftUnit(this);
		
		// Berechnen, wieviel Baupunkte wir an einer Trireme bauen *könnten*
		// warum Trireme, der kann doch auch Langboot bauen wollen... 
		int Peff = 0; // effektive Personenanzahl
		Peff = FFToolsUnits.getPersonenEffektiv(this.scriptUnit);
		this.BauPunkte = this.TalentLevel * Peff ;
		
		int HolzBedarf = (int) (Math.ceil(BauPunkte));
		
		
		// Vorbereiteter BauLevel abfragen, sonst 1
		int Baulevel = OP.getOptionInt("BauLevel", 1);
		HolzBedarf = (int) (Math.ceil(BauPunkte / Baulevel));
		
		if (OP.getOptionInt("Holz", 0)>0) {
			this.addComment("manueller Holzbedarf festgelegt auf " + OP.getOptionInt("Holz", 0) );
			HolzBedarf = OP.getOptionInt("Holz", 0);
		}
		
		if (HolzBedarf>0){
			// Prio bestimmen
			
			int Prio = this.DefaultPrio;
			int setPrio = OP.getOptionInt("prio", 0); 
			if (setPrio>0){
				Prio=setPrio;
			}
			
			this.MPR = new MatPoolRequest(this, HolzBedarf, "Holz", Prio, "Holzbedarf für Werft, BauLevel=" + Baulevel);
			this.addMatPoolRequest(this.MPR);
			this.addComment("Fordere " + HolzBedarf + " Holz mit Prio " + Prio + "an. Ergibt sich aus dem Baulevel von " + Baulevel);
		} else {
			// keine Chance...wir lernen
			this.addComment("Kann noch nix Bauen...setze Lernfix mit Talent=" + this.LernTalent);
			this.scriptUnit.findScriptClass("Lernfix", "Talent=" + this.LernTalent);
		}
		
		// wenn auf einem Schiff, dieses verlassen
		if (this.scriptUnit.getUnit().getShip()!=null){
			this.addOrder("verlasse ; Werftarbeiter verlässt das Schiff",true);
		}
		
		this.showInfos = OP.getOptionBoolean("info", true);
		
		String NeuBauTypName = OP.getOptionString("Neubau");
		if (NeuBauTypName.length()>2){
			// prüfen auf ShipTypes...
			this.addComment("Werft: Neubauauftrag erkannt (" + NeuBauTypName + ")");
			this.shipType=null;	
			for (ShipType sT:this.gd_Script.getRules().getShipTypes()){
				if (sT.getName().equalsIgnoreCase(NeuBauTypName)){
					this.shipType=sT;
					this.addComment("werft: Schiffstyp erkannt: " + sT.getName());
					// Level check
					if (this.shipType.getBuildSkillLevel()>this.TalentLevel) {
						this.addComment("werft: Schiffstyp kann nicht gebaut werden, ich bin nicht gut genug dafür.");
						this.shipType=null;
					}
				}
			}
			
		}
		
		if (OP.getOptionBoolean("allShips", false)){
			WM.setAllShips(this, true);
		}
		if (OP.getOptionBoolean("alleSchiffe", false)){
			WM.setAllShips(this, true);
		}
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
	
	public boolean hasNeubauOrder(){
		boolean erg=false;
		if (this.shipType!=null){
			erg=true;
		}
		return erg;
	}
	
	
}
