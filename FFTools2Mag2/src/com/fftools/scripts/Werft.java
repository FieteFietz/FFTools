package com.fftools.scripts;

import magellan.library.Item;
import magellan.library.Ship;
import magellan.library.rules.ItemType;
import magellan.library.rules.ShipType;

import com.fftools.pools.bau.WerftManager;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;




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
		// waren wir am anfang der runde auf einem Schiff?
		Ship s = this.scriptUnit.getUnit().getShip();
		if (s!=null){
			// sind am ende der runde noch andere an Bord?
			if (s.modifiedUnits().size()==0){
				// scheinbar nicht
				this.doNotConfirmOrders();
				this.addComment("!!! Schiff verbleibt unbesetzt !!!");
			}
		}
	}
	
	private void scriptStart(){
		// Werftmanager besorgen und sich dort anmelden
		WerftManager WM = this.getOverlord().getWerftManager();
		WM.addWerftUnit(this);
		
		// Berechnen, wieviel Baupunkte wir an einer Trireme bauen *könnten*
		int TalentLevel = this.scriptUnit.getSkillLevel("Schiffbau");
		int AnzahlPersonen = this.getUnit().getModifiedPersons();
		int BauPunkte = TalentLevel * AnzahlPersonen ;
		
		
		ItemType rdfType=this.gd_Script.getRules().getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = this.scriptUnit.getModifiedItem(rdfType);
			if (rdfItem!=null && rdfItem.getAmount()>0){
				
				// RDF vorhanden...
				// produktion pro mann ausrechnen
				int prodProMann = (int)Math.floor((double)BauPunkte/(double)this.scriptUnit.getUnit().getModifiedPersons());
				int oldanzTal = BauPunkte;
				for (int i = 1;i<=rdfItem.getAmount();i++){
					if (i<=this.scriptUnit.getUnit().getModifiedPersons()){
						BauPunkte -= prodProMann;
						BauPunkte += (prodProMann * 10);
					} else {
						// überzähliger ring
						this.addComment("Werft: zu viele RdF!",false);
					}
				}
				this.addComment("Werft: " + rdfItem.getAmount() + " RdF. Prod von " + oldanzTal + " auf " + BauPunkte + " erhöht.");
			} else  {
				this.addComment("Werft: kein RdF erkannt.");
			}
		} else {
			this.addComment("Werft: RdF ist noch völlig unbekannt.");
		}
		
		
		
		
		int HolzBedarf = (int) (Math.ceil(BauPunkte/4));
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		
		if (HolzBedarf>0){
			// Prio bestimmen
			
			int Prio = this.DefaultPrio;
			int setPrio = OP.getOptionInt("prio", 0); 
			if (setPrio>0){
				Prio=setPrio;
			}
			
			this.MPR = new MatPoolRequest(this, HolzBedarf, "Holz", Prio, "Holzbedarf für Werft");
			this.addMatPoolRequest(this.MPR);
			this.addComment("Fordere " + HolzBedarf + " Holz mit Prio " + Prio + "an.");
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
