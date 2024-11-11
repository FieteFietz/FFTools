package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.ScriptUnitComparator_natural;
import com.fftools.pools.bau.SeeWerftManager_SWM;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;

import magellan.library.CoordinateID;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.Unit;
import magellan.library.rules.ItemType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;

public class Reparaturschiff extends Script implements Comparable<Reparaturschiff>  {
	
	
	private static final int Durchlauf = 64;
	
	public CoordinateID HomeRegionCoord = null;
	public int maxWeeks=10 ;
	public int ReserveWochen = 3;
	
	public Ship s = null;
	
	public int actDist2target = 0;  // in ReiseWochen
	public int actDist2target_Regions = 0; // in Regionen
	
	public CoordinateID actTargetRegionID = null;
	
	private String Lernplan="";
	private String Lerntalent="";
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Reparaturschiff() {
		super.setRunAt(Durchlauf);
	}
	
	
	/**
	 * Eigentliche Prozedur
	 */
	
	public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf){
			this.scriptStart();
		}
	}
	
	private void scriptStart(){
		
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Reparaturschiff");
		OP.addOptionList(this.getArguments());
		
		// die Basics checken - wir benötigen Zwingend Parameter HOME und maxDist

		
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
		
		int setEntfernung = OP.getOptionInt("maxWeeks", 0);
		if (setEntfernung>0 && setEntfernung<50) {
			this.maxWeeks = setEntfernung;
		} else {
			this.doNotConfirmOrders("!!! Entfernung nicht erkannt!");
			return;
		}
		
		
		this.Lernplan = OP.getOptionString("Lernplan");
		this.Lerntalent = OP.getOptionString("Talent");
		
		int setReserveWochen = OP.getOptionInt("ReserveWochen", 0);
		if (setReserveWochen>0 && setReserveWochen<500) {
			this.ReserveWochen = setReserveWochen;
		}

		Ship myS = this.getUnit().getModifiedShip();
		if (myS==null) {
			this.doNotConfirmOrders("!!! script Reparaturschiff macht nur auf einem Schiff Sinn!");
			return;
		}
		
		this.s = myS;
		
		// habe ich mindestens eine Seewerft an Bord?
		// nein -> fehler + abbruch
		
		int countSeewerften = 0;
		int countSeewerftenNeedWood = 0;
		Collection<Unit> insassen = myS.modifiedUnits();
		for (Unit u:insassen) {
			if (!u.equals(this.getUnit())) {
				// eigenen Kapitän brauchen wir nicht
				ScriptUnit su = this.scriptUnit.getScriptMain().getScriptUnit(u);
				if (su!=null) {
					// ist eine ScriptUnit - ist es auch eine Seewerft?
					Object o = su.getScript(Seewerft.class);
					if (o!=null) {
						// Bingo
						countSeewerften++;
						Seewerft SW = (Seewerft)o;
						if (SW.needNewWood) {
							countSeewerftenNeedWood++;
						}
					}
				}
			}
		}
		if (countSeewerften==0) {
			this.doNotConfirmOrders("!!! Reparaturschiff: keine Seewerft an Bord!");
			return;
		} else {
			// wenn alle Seewerften Holz brauchen -> RTB
			if (countSeewerftenNeedWood == countSeewerften) {
				// RTB
				this.actTargetRegionID = this.HomeRegionCoord;
				if (countSeewerftenNeedWood==1) {
					this.addComment("Reparaturschiff: Seewerft meldet Holzbedarf -> RTB");
				} else {
					this.addComment("Reparaturschiff: " + countSeewerftenNeedWood + " (alle) Seewerften melden Holzbedarf -> RTB");
				}
				if (this.s.getRegion().getCoordinate().equals(this.HomeRegionCoord)) {
					this.addComment("Reparaturschiff: wir sind in der Home Region - Lernen");
					this.Lernen();
				} else {
					this.makeOrderNach();
				}

				return;
			}
			
			if (countSeewerftenNeedWood > 0) {
				if (countSeewerftenNeedWood==1) {
					this.addComment("Reparaturschiff: Seewerft meldet Holzbedarf, aber es ist scheinbar noch genug Holz an Bord");
				} else {
					this.addComment("Reparaturschiff: " + countSeewerftenNeedWood + " Seewerften melden Holzbedarf, aber es ist scheinbar noch genug Holz an Bord");
				}
			} else {
				if (countSeewerften==1) {
					this.addComment("Reparaturschiff: Seewerft hat noch genug Holz ");
				} else {
					this.addComment("Reparaturschiff: Seewerften haben noch genug Holz ");
				}
			}
		}
		
		// Prüfung auf Silberbestand !!!!
		
		// Entfernung nach Home ermitteln
		int ReisewochenHome = -1;
		// Pfad nach Hause ermitteln
		int speed = this.gd_Script.getGameSpecificStuff().getGameSpecificRules().getShipRange(this.s);
		
		Direction d = Direction.INVALID;
		d = Regions.getMapMetric(this.gd_Script).toDirection(this.s.getShoreId());
		ReisewochenHome = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(this.s.getRegion().getCoordinate(),d, this.HomeRegionCoord, this.gd_Script, speed, null);
		 
		if (ReisewochenHome<0) {
			this.doNotConfirmOrders("!!! Reparaturschiff: weg nach Hause nicht zu finden!!!");
			return;
		} else {
			this.addComment("Reparaturschiff: Anzahl Wochen nach Hause: " + ReisewochenHome);
		}
		
		
		// Silber feststellen
		ItemType silverType=this.gd_Script.getRules().getItemType("Silber",false);
		if (silverType==null) {
			this.doNotConfirmOrders("!!! Silber nicht definiert!!!");
			return;
		}
		Item i = this.scriptUnit.getModifiedItem(silverType);
		int Silberbestand = i.getAmount();
		this.addComment("Reparaturschiff: aktueller Silberbestand: " + Silberbestand + " Silber");
		
		int MindestSilberbestand = (ReisewochenHome + this.ReserveWochen) * 10 * this.scriptUnit.getUnit().getModifiedPersons();
		this.addComment("Reparaturschiff: MindestSilberbestand=" + MindestSilberbestand + " Silber (" +  this.ReserveWochen + " Reservewochen berücksichtigt)");
		
		if (Silberbestand<MindestSilberbestand) {
			
			if (this.s.getRegion().getCoordinate().equals(this.HomeRegionCoord)) {
				this.addComment("Reparaturschiff: wird sind in der Home Region - Lernen");
				this.Lernen();
				this.doNotConfirmOrders("!!!Reparaturschiff in der HomeRegion mit zu wenig Silber!!!");
				// nicht beim SWM anmelden
				return;
			} else {
				this.addComment("Reparaturschiff: SilberMindestbestand unterschritten. Suche nach Silber.");
				// this.makeOrderNach();
				this.search4silverOrRTB();
				// 20240707: trotzdem beim SWM anmelden, damit auch jetzt noch repariert werden kann
				// targetRegion ist ja bereits gesetzt...
			}
		}

		// Anmeldung beim SWM
		this.getOverlord().getSWM().addReparaturschiff(this);
		
	}
	
	/**
	 * zu wenig Silber an Bord
	 * Innerhalb der Reservewochen nach einem Depot in Ebene, Wald oder Hafenregion suchen
	 * 	mit genügend Silber
	 * wenn nichts aufzufinden: RTB
	 */
	private void search4silverOrRTB() {
		
		// Lohn - Script suchen und anzahl wochen ermitteln
		int Lohnwochen=-1;
		int LohnPrio=1000;
		Object o = this.scriptUnit.getScript(Lohn.class);
		if (o!=null) {
			Lohn L = (Lohn)o;
			Lohnwochen = L.getAnzRunden();
			LohnPrio = L.Prioritaet_Lohn;
		} else {
			this.doNotConfirmOrders("!!! Reparaturschiff: kein Lohn-Script gefunden - kann den Silberbedarf nicht berechnen !!!");
			RTB();
			return;
		}
		
		// Anzahl Personen auf dem Schiff berechnen - es wird angenommen, alle haben analoge Lohn-Einstellungen
		Ship s = this.scriptUnit.getUnit().getModifiedShip();
		int AnzahlPersonen=0;
		for (Unit u : s.modifiedUnits()) {
			AnzahlPersonen+=u.getModifiedPersons();
		}
		
		if (AnzahlPersonen<=0) {
			this.doNotConfirmOrders("!!! Reparaturschiff: Berechnung der Personenanzahl ist gescheitert !!!");
			RTB();
			return;
		}
		
		int neededSilber = (Lohnwochen + this.ReserveWochen) * 10 * AnzahlPersonen;
		this.addComment("Silberbedarf: " + neededSilber + " Silber (" + (Lohnwochen + this.ReserveWochen) + " Wochen mit " + AnzahlPersonen + " Personen)");

		// durch alle SUs durchgehen
		ArrayList<ScriptUnit> possibleDepots = new ArrayList<ScriptUnit>();
		ScriptMain sm = this.scriptUnit.getScriptMain();
    	for (ScriptUnit su : sm.getScriptUnits().values()){
    		if (su.isDepot()){
    			int Reisewochen = Dist2ValidSilverRegion(su.getUnit().getRegion());
    			if (Reisewochen>=0 && enoughSilver(su, neededSilber)) {
    				su.sortValue = Reisewochen;
    				// su.sortValue_2 = Regions.getDist(this.region().getCoordinate(), su.getUnit().getRegion().getCoordinate());
    				su.sortValue_2 = FFToolsRegions.lastPathLength;
    				possibleDepots.add(su);
    			}
    		}
    	}
    	if (possibleDepots.size()==0) {
    		this.addComment("Kein geeignetes Depot in Reichweite (Reservewochen) gefunden -> RTB");
    		RTB();
    		return;
    	}
    	
    	// Liste der Depots sortieren
    	Collections.sort(possibleDepots, new ScriptUnitComparator_natural());
    	// Sicherheitsausgabe
    	this.addComment("Silbernachschub: verfügbare Depots in Reichweite (sortiert): ");
    	int seq_num = 1;
    	for (ScriptUnit su:possibleDepots) {
    		this.addComment(seq_num++ + ": " + su.toString() + " (" + su.sortValue + " Reisewochen)");
    	}
    	
    	ScriptUnit silberDepot = possibleDepots.get(0);
    	this.addComment("Anfahrt für Silbernachschub: " + silberDepot.toString() + " (" + silberDepot.sortValue + " Reisewochen)");
    	this.actTargetRegionID = silberDepot.getUnit().getRegion().getCoordinate();
    	makeOrderNach(0,0,255);
    	
    	// Gimmick: beim Depot entsprechende Silberforderung anlegen
    	Object o2 = silberDepot.getScript(Depot.class);
    	if (o2!=null) {
    		Depot d = (Depot)o2;
    		MatPoolRequest MPR = new MatPoolRequest(d,neededSilber,"Silber",LohnPrio,"Silbernachschub für Reparaturschiff " + s.toString());
    		d.addMatPoolRequest(MPR);
    	} else {
    		this.doNotConfirmOrders("!!! Reparaturschiff: beim anvisierten Silberdepot konnte kein Depot-Script gefunden werden !!!");
    	}
    	
	}
	
	
	private void RTB() {
		this.actTargetRegionID = this.HomeRegionCoord;
		makeOrderNach();
	}
	
	
	private int Dist2ValidSilverRegion(Region r) {
		// Regionstyp
		if (!(r.getRegionType().getName().equalsIgnoreCase("Ebene") || r.getRegionType().getName().equalsIgnoreCase("Wald"))) {
			return -1;
		}
		// Einfache Abstandsberechnung
		if (Regions.getDist(this.getUnit().getRegion().getCoordinate(), r.getCoordinate())>this.ReserveWochen) {
			return -1;
		}
		// Genaue Abstandsberechung
		int Reisewochen = -1;
		int speed = this.gd_Script.getGameSpecificStuff().getGameSpecificRules().getShipRange(this.s);
		Direction d = Direction.INVALID;
		d = Regions.getMapMetric(this.gd_Script).toDirection(this.s.getShoreId());
		Reisewochen = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(this.s.getRegion().getCoordinate(),d, r.getCoordinate(), this.gd_Script, speed, null);
		
		if (Reisewochen>this.ReserveWochen) {
			return -1;
		}
		
		return Reisewochen;
	}
	
	private boolean enoughSilver(ScriptUnit su, int neededSilber) {
		ItemType silverType=this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getItemType("Silber",false);
		if (silverType==null) {
			return false;
		}
		Item i = su.getModifiedItem(silverType);
		if (i!=null) {
			if (i.getAmount()>=neededSilber) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * standard linie gelb
	 */
	public void makeOrderNach() {
		makeOrderNach(0, 255, 0);
	}

	
	/**
	 * mit expliziter Farbangabe
	 * @param R
	 * @param G
	 * @param B
	 */
	public void makeOrderNach(int R, int G, int B) {
		if (this.actTargetRegionID==null) {
			return;
		}
		
		if (this.s.getRegion().getCoordinate().equals(this.actTargetRegionID)) {
			// wir sind schon da.
			this.addComment("Reparaturschiff: Zielregion entspricht aktueller Region - Lerne");
			this.Lernen();
			return;
		}
		
		
		List<Region> targetPath = Regions.planShipRoute(this.scriptUnit.getUnit().getModifiedShip(),super.gd_Script, this.actTargetRegionID);
		if (targetPath!=null && targetPath.size()>0) {
			super.addOrder("NACH " + Regions.getDirections(targetPath) + " ; vom SWM zugewiesen", true);
			FFToolsRegions.addMapLine(this.region(), this.actTargetRegionID, R, G, B, 5, SeeWerftManager_SWM.MAPLINE_TAG);
		} else {
			this.doNotConfirmOrders("!!! vom SWM ungültige Befehle erhalten, Zielregion nicht erreichbar: " + this.actTargetRegionID.toString(",", false));
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
	
	
	public int compareTo(Reparaturschiff a) {
		/*
		 * actDist2target: Entfernung in Reisewochen
		 * falls gleich, dann den nehmen, der von der normalen Entfernung dichter drann ist - ggf auch Pfadlänge
		 */
		
		
		if (this.actDist2target == a.actDist2target) {
			return this.actDist2target_Regions - a.actDist2target_Regions;
		}
		
		return this.actDist2target - a.actDist2target;
		
	}
	
	public void Lernen() {
		String LernfixOrder=null;
		if (this.Lernplan.length()>2) {
			LernfixOrder="Lernplan=" + this.Lernplan;
		}
		if (LernfixOrder==null && this.Lerntalent.length()>2) {
			LernfixOrder="Talent=" + this.Lerntalent;
		}
		if (LernfixOrder==null) {
			this.doNotConfirmOrders("Reparaturschiff soll Lernen, aber was denn?");
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
	
}
