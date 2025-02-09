package com.fftools.pools.bau;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Reparaturschiff;
import com.fftools.scripts.Route;
import com.fftools.scripts.Sailto;
import com.fftools.scripts.Seeschlangenjagd;
import com.fftools.scripts.Seewerft;
import com.fftools.utils.FFToolsRegions;

import magellan.library.CoordinateID;
import magellan.library.Ship;
import magellan.library.Unit;
import magellan.library.rules.ShipType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;

public class SeeWerftManager_SWM implements OverlordRun,OverlordInfo {
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	public static final String MAPLINE_TAG="FFTools_SWM_MoveLine";
	
	private static final int Durchlauf = 65;
	
	// Rückgabe als Array
	private int[] runners = {Durchlauf};
	
	// als referenz
	private Overlord overLord = null;
	
	// Liste aller einsatzfähigen Reparaturschiff
	private ArrayList<Reparaturschiff> availableReparaturSchiffe = new ArrayList<Reparaturschiff>(0);
	
	// Liste aller einsatzfähigen Seewerften
	private ArrayList<Seewerft> availableSeeWerften = new ArrayList<Seewerft>(0);
	
	// Liste aller Schiffe, die repariert werden müssen
	private ArrayList<Ship2Repair> ships2Repair_list = new ArrayList<Ship2Repair>(0);
	
	private HashMap<CoordinateID, SeeWerftPool> pools = new HashMap<CoordinateID, SeeWerftPool>();
	
	/**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
    
    // Konstruktor
    public SeeWerftManager_SWM(Overlord overlord){
		this.overLord = overlord;
	}
    
    
    
    /**
	 * startet den SWM
	 */
	public void run(int durchlauf){
		// schiffe
		outText.addOutLine("SeeWerftManager SWM Start - ermittle zu reparierende Schiffe. (Reparaturschiffe: " + this.availableReparaturSchiffe.size() + ", Werften: " + this.availableSeeWerften.size() + ", SWPs: " + this.pools.keySet().size() + ")" ,true);
		for (Ship s:this.overLord.getScriptMain().gd_ScriptMain.getShips()) {
			int neededBP = neededBaupunkte(s);
			// AF 20250209: Boote auslassen, denn die fahren zu oft in einen Berg und könnten auch zwischen 2 Regionen ohne Hafen
			// pendeln und nie repariert werden - boote weg lassen
			ShipType ST = s.getShipType();
			boolean correctShiptype = true;
			if (ST.getName().equalsIgnoreCase("Boot")){
				correctShiptype=false;
			}
			if (neededBP>0 && correctShiptype) {
				Unit u = s.getModifiedOwnerUnit();
				if (u!=null) {
					ScriptUnit su = this.overLord.getScriptMain().getScriptUnit(u);
					if (su!=null) {
						// Test auf SSJ
						Object o = su.getScript(Seeschlangenjagd.class);
						if (o!=null) {
							Seeschlangenjagd SJ = (Seeschlangenjagd) o;
							// targetRegionCoord
							if (SJ.targetRegionCoord!=null) {
								// bingo: wir haben einen nächsten Stop
								Ship2Repair pS = new Ship2Repair();
								pS.captn = su;
								pS.nextShipStop = SJ.targetRegionCoord;
								pS.s = s;
								pS.repairsNeeded = neededBP;
								ships2Repair_list.add(pS);
								su.addComment("SWM: auf die Liste zu reparierender Schiffe gesetzt.");
							}
						}
						// SailTo + Route
						o = su.getScript(Sailto.class);
						Object o2 = su.getScript(Route.class);
						if (o!=null && o2!=null) {
							Sailto st = (Sailto) o;
							// targetRegionCoord
							if (st.nextShipStop!=null) {
								// bingo: wir haben einen nächsten Stop
								Ship2Repair pS = new Ship2Repair();
								pS.captn = su;
								pS.nextShipStop = st.nextShipStop.getCoordinate();
								pS.s = s;
								pS.repairsNeeded = neededBP;
								ships2Repair_list.add(pS);
								su.addComment("SWM: auf die Liste zu reparierender Schiffe gesetzt.");
							}
						}
					}
				}
			}
		}

		// Sortieren
		Collections.sort(ships2Repair_list);
		
		// Test: Reparaturschiffe informieren
		for (Reparaturschiff r: this.availableReparaturSchiffe) {
			int speed = this.overLord.getScriptMain().gd_ScriptMain.getGameSpecificStuff().getGameSpecificRules().getShipRange(r.s);
			r.addComment("SWM: anzahl zu reparierender Schiffe gesamt: " + ships2Repair_list.size());
			int zuweitweg=0;
			int keinWeg=0;
			int inReichweite=0;
			for (Ship2Repair pS:ships2Repair_list) {
				// Entfernung berechnen
				int Reisewochen = -1;
				// Pfad nach Hause ermitteln
				Direction d = Direction.INVALID;
				d = Regions.getMapMetric(this.overLord.getScriptMain().gd_ScriptMain).toDirection(pS.s.getShoreId());
				Reisewochen = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(pS.s.getRegion().getCoordinate(),d, r.HomeRegionCoord, this.overLord.getScriptMain().gd_ScriptMain, speed, null);
				String reiseInfo = "(kein Weg gefunden)";
				if (Reisewochen>=0 && Reisewochen<=r.maxWeeks) {
					// reiseInfo = " (Entf: " + Reisewochen + " Wochen)";
					// r.addComment("SWM - beschädigt: " + pS.s.toString() + " in " + pS.s.getRegion().toString() + reiseInfo + " [" + pS.repairsNeeded + "]");
					inReichweite+=1;
				}
				if (Reisewochen > r.maxWeeks) {
					zuweitweg+=1;
				}
				if (Reisewochen<0) {
					keinWeg+=1;
				}
			}
			if (inReichweite>0) {
				r.addComment("SWM: anzahl zu reparierender Schiffe in Reichweite: (" + r.maxWeeks + " Wochen ab Home) " + inReichweite);
			}
			if (zuweitweg>0) {
				r.addComment("SWM: anzahl zu reparierender Schiffe, die zu weit weg sind: " + zuweitweg);
			}
			if (keinWeg>0) {
				r.addComment("SWM: anzahl zu reparierender Schiffe, die nicht erreicht werden können: " + keinWeg);
			}
		}
		
		
		// für jedes Reparaturschiff die liste zu reparierender Schiffe dem SWP hinzufügen - wenn möglich
		// daraus ergeben sich die SWPs in den Regionen, in welcher die Reparaturschiffe sich gerade befinden
		outText.addOutLine("SeeWerftManager SWM: Reparaturschiffe zu den SWPs",true);
		for (Reparaturschiff r: this.availableReparaturSchiffe) {
			CoordinateID CID = r.s.getRegion().getCoordinate();
			SeeWerftPool SWP = this.pools.get(CID);
			if (SWP!=null) {
				// schiffe durchgehen
				for (Ship2Repair pS:ships2Repair_list) {
					if (pS.s.getRegion().getCoordinate().equals(CID)) {
						SWP.addShip2Repair(pS);
					}
				}
			} else {
				r.addComment("!!! SWM: kein SeeWeftPool für aktuelle Region gefunden - ist eine Seewerft einsetzbereit?");
			}
		}
		
		// die Werftpools anschieben
		outText.addOutLine("SeeWerftManager SWM: SWPs starten",true);
		for (CoordinateID CID:this.pools.keySet()) {
			outText.addOutLine("SeeWerftManager SWM: SWP starten für " + CID.toString(),true);
			SeeWerftPool SWP = this.pools.get(CID);
			if (SWP!=null) {
				SWP.runPool();
			} else {
				outText.addOutLine("!!! SeeWerftManager SWM: SWP nicht gefunden für " + CID.toString(),true);
			}
		}
		
		
		// Ziele für die einzelnen Reparaturschiffe finden
		planReparaturschiffe();
		
		// nicht verplante Reparaturschiffe RTB
		sendRS_RTB();
		
	}
	
	
	/*
	 * weist die Reparaturschiffe jeweils einer Region mit Ships2Repair zu
	 */
	private void planReparaturschiffe() {
		// wir suchen für jedes zu reparierende Schiff in der Reihenfolge des Schadens !!! 
		// sortieren alle Reparaturschiffe nach Entfernung - wenn in Reichweite
		// und nehmen das dichteste
		// jede Zielregion nur 1 Schiff - egal wie viele zu reparierende Schiffe es sind (einfacher Ansatz)
		
		// Sortieren
		Collections.sort(this.ships2Repair_list);
		outText.setFile("SeeWerftManager_RepSchiff");
		boolean oldScreenOut = outText.isScreenOut();
		outText.setScreenOut(false);
		outText.addNewLine();
		outText.addOutLine("******SWM - Info******");
		outText.addOutLine("Liste der zu reparierenden Schiffe (" + this.ships2Repair_list.size() + " Einträge): ");
		for (Ship2Repair sh:this.ships2Repair_list) {
			if (sh.repairsNeeded>0 && sh.nextShipStop!=null) {
				outText.addOutLine(sh.s.toString() + " in " + sh.s.getRegion().toString() + " | Schaden: " + sh.repairsNeeded);
			}
		}
		outText.addNewLine();
		
		for (Ship2Repair sh:this.ships2Repair_list) {
			if (sh.repairsNeeded>0 && sh.nextShipStop!=null) {
				outText.addOutLine("checking: " + sh.s.toString() + " in " + sh.s.getRegion().toString() + " | Schaden: " + sh.repairsNeeded);
				// neue Liste zusammenbauen mit den noch verfügbaren Reparaturschiffen
				ArrayList<Reparaturschiff> actAvailReparaturschiffe = new ArrayList<Reparaturschiff>(0);
				for (Reparaturschiff RS:this.availableReparaturSchiffe) {
					if (RS.actTargetRegionID==null) {
						// noch kein Ziel - ist das Ziel innerhalb der Reichweite?
						int speed = this.overLord.getScriptMain().gd_ScriptMain.getGameSpecificStuff().getGameSpecificRules().getShipRange(RS.s);
						int ReisewochenHome = -1;
						// Pfad nach Hause ermitteln
						
						Direction d = Direction.INVALID;
						d = Regions.getMapMetric(this.overLord.getScriptMain().gd_ScriptMain).toDirection(sh.s.getShoreId());
						ReisewochenHome = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(sh.s.getRegion().getCoordinate(),d, RS.HomeRegionCoord, this.overLord.getScriptMain().gd_ScriptMain, speed, null);
						outText.addOutLine("prüfe RepSchiff: " + RS.s.toString() + " in " + RS.s.getRegion().toString() + ", RW_home: " +ReisewochenHome + "/" + RS.maxWeeks);
						if (ReisewochenHome<=RS.maxWeeks && ReisewochenHome>=0) {
							// wir sind innerhalb der maxWeeks von der HomeRegion
							// Entfernung zwischen sh und RS ermitteln und bei RS eintragen, damit danach sortiert werden kann
							int ReisewochenTarget = -1;
							// Pfad zum sh ermitteln
							d = Direction.INVALID;
							d = Regions.getMapMetric(this.overLord.getScriptMain().gd_ScriptMain).toDirection(RS.s.getShoreId());
							ReisewochenTarget = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(RS.s.getRegion().getCoordinate(),d, sh.nextShipStop, this.overLord.getScriptMain().gd_ScriptMain, speed, null);
							if (ReisewochenTarget>=0) {
								// ok - wir kommen da hin
								RS.actDist2target = ReisewochenTarget;
								// RS.actDist2target_Regions = Regions.getDist(RS.s.getRegion().getCoordinate(), sh.nextShipStop);
								RS.actDist2target_Regions = FFToolsRegions.lastPathLength;
								outText.addOutLine("Weg gefunden zur Zielregion: " + ReisewochenTarget + " Wochen, " + RS.actDist2target_Regions + " Regionen, RS verfügbar");
								actAvailReparaturschiffe.add(RS);
							} else {
								outText.addOutLine("kein Weg gefunden zur Zielregion: " + ReisewochenTarget);
							}
						}
					}
				}
				
				// Reparaturschiffe in Reichweite nach Entfernung sortieren - wenn welche da sind
				if (actAvailReparaturschiffe.size()>1) {
					Collections.sort(actAvailReparaturschiffe);
					outText.addOutLine("Verfügbare RS nach Entf: " + actAvailReparaturschiffe.size());
					for (Reparaturschiff RS:actAvailReparaturschiffe) {
						outText.addOutLine(RS.s.toString() + " Wochen: " + RS.actDist2target + ", Regionen: " + RS.actDist2target_Regions);
					}
				}
				
				// Nun dem ersten das Ziel zuordnen
				if (actAvailReparaturschiffe.size()>0) {
					Reparaturschiff RS = actAvailReparaturschiffe.get(0);
					// Ziel setzen
					RS.actTargetRegionID=sh.nextShipStop;
					// Infos:
					sh.captn.addComment("SWM: zur Reparatur kommt: " + RS.s.toString()+ " (" + RS.actDist2target + " Wochen)");
					RS.addComment("SWM: Reparaturziel: " + sh.s.toString() + " (" + RS.actDist2target + " Wochen, " + RS.actDist2target_Regions + " Regionen)");
					
					// Anweisungen umsetzen
					RS.makeOrderNach();
					outText.addOutLine("RS gesetzt:" + RS.s.toString());
				} else {
					outText.addOutLine("keine RS verfügbar");
				}
				outText.addNewLine();
				outText.addNewLine();
			} else {
				sh.captn.addComment("SWM: Schiff wird diese Runde vollständig repariert - kein Ziel mehr für Reparaturschiffe.");
			}
		}
		
		outText.addNewLine();
		outText.addOutLine("Ende SWM plan RS");
		
		outText.setFileStandard();
		outText.setScreenOut(oldScreenOut);

	}
	
	private void sendRS_RTB() {
		for (Reparaturschiff RS:this.availableReparaturSchiffe) {
			if (RS.actTargetRegionID==null) {
				// hat kein Ziel bekommen
				// sind wir bereits in der HomeRegion?
				if (RS.s.getRegion().getCoordinate().equals(RS.HomeRegionCoord)) {
					// wir sind zu Hause - Lernen
					RS.addComment("SWM: keine Reparaturziele und in HomeRegion: Lernen");
					RS.Lernen();
				} else {
					// wir sind nicht zu Hause, RTB
					RS.addComment("SWM: keine Reparaturziele: RTB");
					RS.actTargetRegionID = RS.HomeRegionCoord;
					RS.makeOrderNach();
				}
			}
		}
	}
	

	public void addReparaturschiff(Reparaturschiff s) {
		if (!this.availableReparaturSchiffe.contains(s)) {
			this.availableReparaturSchiffe.add(s);
		} else {
			// bereits vorhanden
			s.addComment("!!! SWM: erneuter Eintrag in verfügbare ReparaturSchiffe verweigert.");
		}
	}
	
	public void addSeewerft(Seewerft s) {
		if (!this.availableSeeWerften.contains(s)) {
			this.availableSeeWerften.add(s);
		} else {
			// bereits vorhanden
			s.addComment("!!! SWM: erneuter Eintrag in verfügbare ReparaturSchiffe verweigert.");
		}
		
		CoordinateID CID = s.getUnit().getRegion().getCoordinate();
		SeeWerftPool SWP = this.pools.get(CID);
		if (SWP==null) {
			// Neu anlegen
			SWP = new SeeWerftPool(this);
		} 	this.pools.put(CID, SWP);
		// Seewerft ergänzen	
		SWP.addWerft(s);
		
		// das Schiff der Seewerft prüfen und ggf ergänzen
		Ship sh = s.getUnit().getModifiedShip();
		if (sh!=null) {
			if (sh.getDamageRatio()>0) {
				// das eigene schiff ist beschädigt, zum SWP hinzufügen
				SWP.addShip(sh);
			}
		}
	}
	
	
	public void addShip2Repair(ScriptUnit kapitän) {
		
		Unit u = kapitän.getUnit();
		Ship s = u.getModifiedShip();
		
		// check ship
		if (s==null) {
			kapitän.addComment("!!! es konnte kein Schiff zur Reparaturliste hinzugefügt werden - nicht auf einem Schiff?");
			return;
		}
		
		// check Kapitän
		if (s.getModifiedOwnerUnit()==null || !s.getModifiedOwnerUnit().equals(u)) {
			kapitän.addComment("!!! es konnte kein Schiff zur Reparaturliste hinzugefügt werden - nicht der Kapitän?");
			return;
		}
		
		
		int neededBP = neededBaupunkte(s);
		// check Baupunkte
		if (neededBP<=0) {
			kapitän.addComment("Schiff nicht zur Reparaturliste hinzugefügt - kein Schaden festgestellt");
			return;
		}
		
		
		
		// SailTo
		Object o = kapitän.getScript(Sailto.class);
		
		if (o!=null) {
			Sailto st = (Sailto) o;
			// targetRegionCoord
			if (st.nextShipStop!=null) {
				// bingo: wir haben einen nächsten Stop
				Ship2Repair pS = new Ship2Repair();
				pS.captn = kapitän;
				pS.nextShipStop = st.nextShipStop.getCoordinate();
				pS.s = s;
				pS.repairsNeeded = neededBP;
				ships2Repair_list.add(pS);
				kapitän.addComment("SWM: auf die Liste zu reparierender Schiffe gesetzt.");
			}
		} else {
			// kein SailTo - wir gehen von keiner Bewegung aus
			Ship2Repair pS = new Ship2Repair();
			pS.captn = kapitän;
			pS.nextShipStop = u.getRegion().getCoordinate();
			pS.s = s;
			pS.repairsNeeded = neededBP;
			ships2Repair_list.add(pS);
			kapitän.addComment("SWM: ich gehe davon aus, dass dieses Schiff *nicht* bewegt wird!. (kein SailTo gefunden)");
			kapitän.addComment("SWM: auf die Liste zu reparierender Schiffe gesetzt.");
		}
		
		
	}
	

	private int neededBaupunkte(Ship s){
		int erg=0;
		if (s.getDamageRatio()>0){
			double actDamage = s.getDamageRatio();
			double normalSize = (s.getShipType().getMaxSize() * s.getAmount());
			erg = (int) (Math.ceil(normalSize * (actDamage/100)));
		}
		if (erg==0){
			erg = (s.getShipType().getMaxSize() * s.getAmount()) - s.getSize();
		}
		return erg;
	}
	
	public Overlord getOverlord() {
		return this.overLord;
	}
	
	
}
