package com.fftools.scripts;

import java.util.ArrayList;
import java.util.List;

import com.fftools.ScriptUnit;
import com.fftools.pools.seeschlangen.SeeschlangenJagdManager_SJM;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsUnits;

import magellan.library.CoordinateID;
import magellan.library.Faction;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.rules.ItemType;
import magellan.library.rules.Race;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;

public class Seeschlangenjagd extends MatPoolScript{

	int Durchlauf_Attacker = 58; // bei Manager registrieren, nach MatPool nach Request und Lohn 
	// 54: der manager verteilt aufgaben  // vor Lernfix!
	int Durchlauf_Mover = 65; // Prüfungen, infos, etc
	private int[] runners = {Durchlauf_Attacker,Durchlauf_Mover};
	
	public boolean is_attacking = false;
	public boolean is_moving_home = false;
	public boolean is_Learning = false;
	
	public CoordinateID targetRegionCoord = null;
	public CoordinateID actRegionCoord = null;
	public CoordinateID HomeRegionCoord = null;
	
	public int Entfernung = 0;
	
	public int ReserveWochen = 3;
	
	private List<Region> homePath = null;
	
	private SeeschlangenJagdManager_SJM SJM=null;
	
	public int speed = 0;
	
	/*
	 * freies Patroullieren ohne Ziel?
	 */
	public boolean mayPatrol = true;
	
	private String Lernplan="";
	private String Lerntalent="";
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Seeschlangenjagd() {
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
		if (scriptDurchlauf==this.Durchlauf_Attacker){
			this.attacker();
		}
		if (scriptDurchlauf==this.Durchlauf_Mover){
			// this.mover();
		}

	}
	
	/*
	 * meldet beim SeeschlangenJagdManager an
	 * attackiert, wenn gerade jetzt mit einer Seeschlange in einer Region...
	 */
	private void attacker() {
		
		// bin ich Kapitän?
		if (!FFToolsUnits.checkShip(this)){
			this.doNotConfirmOrders("!!! SJ: Nicht als Kapitän eines Schiffes erkannt");
			return;	
		}
		
		
		// die Basics checken - wir benötigen Zwingend Parameter HOME und Entfernung
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"SeeschlangenJagd");
		OP.addOptionList(this.getArguments());
		
		String homeString=OP.getOptionString("home");
		if (homeString.length()>2){
			CoordinateID actDest = null;
			if (homeString.indexOf(',') > 0) {
				actDest = CoordinateID.parse(homeString,",");
			}
			if (actDest!=null){
				this.HomeRegionCoord = actDest;
			} else {
				this.doNotConfirmOrders("!!! HOME Angabe nicht erkannt!");
				return;
			}
		}
		
		int setEntfernung = OP.getOptionInt("Entfernung", 0);
		if (setEntfernung>0 && setEntfernung<50) {
			this.Entfernung = setEntfernung;
		} else {
			this.doNotConfirmOrders("!!! Entfernung nicht erkannt!");
			return;
		}
		
		int setReserveWochen = OP.getOptionInt("ReserveWochen", 0);
		if (setReserveWochen>0 && setReserveWochen<50) {
			this.ReserveWochen = setReserveWochen;
		}
		
		this.mayPatrol = OP.getOptionBoolean("Patrouille", true);
		this.mayPatrol = OP.getOptionBoolean("Patrol", this.mayPatrol);
		
		String patrolInfo = "darf Patrouille fahren";
		if (!this.mayPatrol) {
			patrolInfo = "soll keine Patrouille fahren";
		}
		
		this.addComment("SJ: HOME=" + this.HomeRegionCoord.toString(",", false) + ", Entfernung=" + this.Entfernung + " Regionen, ReserveWochen=" + this.ReserveWochen + ", " + patrolInfo);
		this.Lernplan = OP.getOptionString("Lernplan");
		this.Lerntalent = OP.getOptionString("Talent");
		
		// meldet nur an, wenn einsatzfähig + auf Ozean
		// Silber feststellen
		ItemType silverType=this.gd_Script.getRules().getItemType("Silber",false);
		if (silverType==null) {
			this.doNotConfirmOrders("!!! Silber nicht definiert!!!");
			return;
		}
		Item i = this.scriptUnit.getModifiedItem(silverType);
		int Silberbestand = i.getAmount();
		this.addComment("SJ: aktueller Silberbestand: " + Silberbestand + " Silber");
		
		this.actRegionCoord = this.scriptUnit.getUnit().getRegion().getCoordinate();
		
		this.speed = super.gd_Script.getGameSpecificStuff().getGameSpecificRules().getShipRange(this.scriptUnit.getUnit().getModifiedShip());
		
		// Reisewochen nach HOME bestimmen
		int Reisewochen=0;
		if (!this.actRegionCoord.equals(this.HomeRegionCoord)) {
			// Pfad nach Hause ermitteln
			this.homePath = Regions.planShipRoute(this.scriptUnit.getUnit().getModifiedShip(),super.gd_Script, HomeRegionCoord);
			
			Direction d = Direction.INVALID;
			if (this.scriptUnit.getUnit().getModifiedShip()!=null){
				d = Regions.getMapMetric(this.gd_Script).toDirection(this.scriptUnit.getUnit().getModifiedShip().getShoreId());
			} 
			
			Reisewochen = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(this.actRegionCoord,d, this.HomeRegionCoord, this.gd_Script, this.speed, null);
			if (Reisewochen>1) {
				this.addComment("SJ: Weg nach HOME benötigt " + Reisewochen + " Wochen");
			}  else {
				this.addComment("SJ: Weg nach HOME benötigt " + Reisewochen + " Woche");
			}
		}
		int MindestSilberbestand = (Reisewochen + this.ReserveWochen) * 10 * this.scriptUnit.getUnit().getModifiedPersons();
		this.addComment("SJ: MindestSilberbestand=" + MindestSilberbestand + " Silber (" +  this.ReserveWochen + " Reservewochen berücksichtigt)");
		
		if (Silberbestand<MindestSilberbestand) {
			if (!this.actRegionCoord.equals(this.HomeRegionCoord)) {
				// auf See, muss nach Hause
				this.is_moving_home=true;
				this.addComment("SJ: RTB (returning to base) => es geht nach Hause!!!");
				super.addOrder("NACH " + Regions.getDirections(this.homePath), true);
			} else {
				// in der HOME Region: Problem
				this.is_moving_home=true;
				this.is_attacking=false;
				this.doNotConfirmOrders("!!!SJ: Kein Silbervorrat zum Ablegen verfügbar. So kann ich nicht arbeiten!!!");
				return;
			}
		}
		
		// Herausfinden, ob genau in unserer Region eine Seeschlange ist
		ArrayList<String> monsters = new ArrayList<String>(0);
		if (!this.actRegionCoord.equals(this.HomeRegionCoord)) {
			for (Unit u :this.getUnit().getRegion().getUnits().values()) {
				Faction f = u.getFaction();
				if (f!=null) {
					if (f.getID().toString().equalsIgnoreCase("ii")) {
						Race r = u.getRace();
						if (r!=null) {
							if (r.getName().equalsIgnoreCase("Seeschlangen")) {
								monsters.add(u.getID().toString());
							}
						}
					}
				}
			}
		}
		if (monsters.size()>0) {
			// ok, wir haben genau hier was zum Angreifen
			// alle scriptunits auf diesem Schiff durchgehen und // angreifer suchen
			this.addComment("SJ: es befindet sich ein Ziel in der Region....suche Angreifer (// Angreifer)");
			int countFront = 0 ;
			for (Unit u : this.getUnit().getModifiedShip().getUnits().values()) {
				ScriptUnit  su = this.getOverlord().getScriptMain().getScriptUnit(u);
				if (su!=null) {
					if (su.hasOrder("// Angreifer")) {
						// Gefunden
						this.addComment("Angreifer gefunden und bekommt ATTACKIERE-Befehl: " + su.toString());
						for (String so : monsters) {
							su.addOrder("ATTACKIERE " + so + " ; SJ von " + this.scriptUnit.toString(), true, false);
							this.is_attacking=true;
						}
						if (su.getUnit().getCombatStatus()==EresseaConstants.CS_AGGRESSIVE || su.getUnit().getCombatStatus() == EresseaConstants.CS_FRONT || su.getUnit().getCombatStatus() == EresseaConstants.CS_DEFENSIVE) {
							countFront += su.getUnit().getModifiedPersons();
						}
					}
				}
			}
			if (countFront<10) {
				this.doNotConfirmOrders("SSJ: weniger als 10 Angreifer in der Frontreihe - sicher?");
			}
			if (this.is_attacking) {
				// Jetzt dem SJM mitteilen, dass wir in dieser Region bereits zuschlagen
				this.SJM = this.getOverlord().getSJM();
				this.SJM.addAttackRegion(this.region().getCoordinate());
			}
		}
		
		
		
		
		// finale: wenn bis hierhin gekommen und nicht auf dem Heimweg: als mover melden
		if (!this.is_moving_home) {
			if (this.SJM==null) {
				this.SJM = this.getOverlord().getSJM();
			}
			this.SJM.addMover(this);
			this.addComment("SJ: Bereitschaft beim SeeschlangenJagdKommando angemeldet (SJM=SeeschlangenJagdManager)");
		}
		
		if (OP.getOptionBoolean("SJM-Info", false)) {
			if (this.SJM==null) {
				this.SJM = this.getOverlord().getSJM();
			}
			this.SJM.addInfoReceiver(this);
		}
		
	}
	
	/**
	 * wird vom SJM aufgerufen, nachdem targetRegion gesetzt worden ist
	 */
	public void makeOrderNach() {
		List<Region> targetPath = Regions.planShipRoute(this.scriptUnit.getUnit().getModifiedShip(),super.gd_Script, this.targetRegionCoord);
		if (targetPath!=null && targetPath.size()>0) {
			super.addOrder("NACH " + Regions.getDirections(targetPath) + " ; vom SJM zugewiesen", true);
			FFToolsRegions.addMapLine(this.region(), this.targetRegionCoord, 255, 0, 0, 5, SeeschlangenJagdManager_SJM.MAPLINE_TAG);
		} else {
			this.doNotConfirmOrders("!!! vom SJM ungültige Befehle erhalten, Zielregion nicht erreichbar: " + this.targetRegionCoord.toString(",", false));
		}
	}
	
	
	
	/*
	 * vom SJM
	 */
	public void Lerne() {
		String LernfixOrder=null;
		if (this.Lernplan.length()>2) {
			LernfixOrder="Lernplan=" + this.Lernplan;
		}
		if (LernfixOrder==null && this.Lerntalent.length()>2) {
			LernfixOrder="Talent=" + this.Lerntalent;
		}
		if (LernfixOrder==null) {
			this.doNotConfirmOrders("Seeschlangenjäger soll Lernen, aber was denn?");
			return;
		}
		this.scriptUnit.addComment("Lernfix wird initialisiert mit dem Parameter: " + LernfixOrder);
		Script L = new Lernfix();
		ArrayList<String> order = new ArrayList<String>();
		order.add(LernfixOrder);
		L.setArguments(order);
		L.setScriptUnit(this.scriptUnit);
		L.setGameData(this.scriptUnit.getScriptMain().gd_ScriptMain);
		if (this.scriptUnit.getScriptMain().client!=null){
			L.setClient(this.scriptUnit.getScriptMain().client);
		}
		this.scriptUnit.addAScript(L);
	}


}
