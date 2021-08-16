package com.fftools.scripts;


import java.util.List;

import magellan.library.CoordinateID;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.rules.ShipType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;
import magellan.library.utils.logging.Logger;

import com.fftools.magellan2.ConnectorPlugin;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsUnits;

import jdk.internal.org.jline.reader.LineReader.RegionType;

public class Sailto extends Script{
	private static final int Durchlauf = 42;
	
	private static final Logger log = Logger.getInstance(Sailto.class);
	
	public Region nextShipStop = null;
	
	// Parameterloser constructor
	public Sailto() {
		super.setRunAt(Durchlauf);
	}
	
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf!=Durchlauf){return;}
		// hier code fuer Sailto
		// addOutLine("....start SAILTO mit " + super.getArgCount() + " Argumenten");
		if (FFToolsUnits.checkShip(this)){
			if (super.getArgCount()<1) {
				super.scriptUnit.doNotConfirmOrders("Das Ziel fehlt beim Aufruf von SAILTO!");
				addOutLine("X....fehlendes SAILTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			} else {
				
				if (!FFToolsUnits.checkShipTalents(this)) {
					this.doNotConfirmOrders("Talentcheck für das Schiff ist gescheitert!");
				}
				// wir haben zumindest ein Ziel
				// TH: Prüfen, ob Koordinate oder Regionsname, falls Komma in Regionsangabe sind es wohl Koordinaten...
				CoordinateID actDest = null;
				if (super.getArgAt(0).indexOf(',') > 0) {
					actDest = CoordinateID.parse(super.getArgAt(0),",");
				} else {
				// Keine Koordinaten, also Region in Koordinaten konvertieren
					actDest = FFToolsRegions.getRegionCoordFromName(this.gd_Script, super.getArgAt(0));
				}
				if (actDest!=null){
					// wir haben ein Ziel...sind wir da?
					CoordinateID actRegCoordID = super.scriptUnit.getUnit().getRegion().getCoordinate();
					if (actRegCoordID.equals(actDest)){
						// yep, wir sind da
						if (super.getArgCount()>1) {
							// es gibt weitere Ziele
							// aktuelles Ziel aus der Liste nehmen..
							// neue script order erstellen und anfügen
							// neuen Path berechnen
							
							// neue SAILTO bilden
							String newSAILTO = "SAILTO ";
							for (int i = 1;i<super.getArgCount();i++){
								newSAILTO = newSAILTO.concat(super.getArgAt(i) + " ");
							}
							// ersetzen
							if (super.scriptUnit.replaceScriptOrder(newSAILTO, "SAILTO ".length())) {
								// OK...soweit alles klar
								// neues Ziel setzen
								actDest = CoordinateID.parse(super.getArgAt(1),",");
								if (actDest == null) {
									zielParseFehler();
								} else {
									// fein 
									makeOrderNACH(actRegCoordID,actDest);
								}
				 			} else {
				 				// irgendetwas beim ersetzen ist schief gegangen
				 				super.scriptUnit.doNotConfirmOrders("Fehler beim setzen der nächsten // script SAILTO Anweisung");
				 				addOutLine("X....Fehler beim setzen der nächsten // script SAILTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				 			}
							
						} else {
							// das wars...Ziel erreicht und gut
							super.scriptUnit.doNotConfirmOrders("SAILTO: Einheit hat Ziel erreicht, daher NICHT bestätigt.");
						}
					} else {
						// nope, da müssen wir noch hin
						makeOrderNACH(actRegCoordID,actDest);
					}
				} else {
					// fehler beim parsen des Ziels
					zielParseFehler();
				}
				
			}
		} else {
			// kein Kaptn oder nicht auf dem schiff
			super.scriptUnit.doNotConfirmOrders("Einheit nicht als Kapitän eines Schiffes erkannt.");
			addOutLine("X....Einheit nicht als Kapitän eines Schiffes erkannt: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
	}
	
	private void zielParseFehler() {
		super.scriptUnit.doNotConfirmOrders("Ungueltiges Ziel beim Aufruf von SAILTO!");
		addOutLine("X....ungültiges SAILTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void makeOrderNACH(CoordinateID act,CoordinateID dest){

		// FF 20070103: eingebauter check, ob es actDest auch gibt?!
		if (!com.fftools.utils.FFToolsRegions.isInRegions(this.gd_Script.getRegions(), dest)){
			// Problem  actDest nicht im CR -> abbruch
			super.scriptUnit.doNotConfirmOrders("Sailto Ziel nicht im CR");
			addOutLine("!!! Sailto Ziel nicht im CR: " + this.unitDesc());
			return;
		} 
		
		if (reportSettings.getOptionBoolean("debug_SailTo_Unit")) {
			log.info("Sailto - makeOrderNach durch unit " + this.scriptUnit.toString());
		}
		
		
		int speed = super.gd_Script.getGameSpecificStuff().getGameSpecificRules().getShipRange(this.scriptUnit.getUnit().getModifiedShip());
		List<Region> pathL = Regions.planShipRoute(this.scriptUnit.getUnit().getModifiedShip(),super.gd_Script, dest);
		
		String path = null;
		if (pathL!=null){
			path = Regions.getDirections(pathL);
		}
		if (path!=null && path.length()>0) {
			// path gefunden
			// Reisezeitinfo
			int turns_easy=((int)Math.ceil(((double)pathL.size()-1)/speed));
			Unit u = this.scriptUnit.getUnit();
			this.addComment("Distance to target: " + (pathL.size()-1) + ", ETA in " + turns_easy + " turns.");
			
			Direction d = Direction.INVALID;
			if (this.scriptUnit.getUnit().getModifiedShip()!=null){
				d = Regions.getMapMetric(this.gd_Script).toDirection(this.scriptUnit.getUnit().getModifiedShip().getShoreId());
			}

			int turns_exact = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(u.getRegion().getCoordinate(),d, dest, this.gd_Script, speed, null);
			if (turns_exact!=turns_easy){
				this.addComment("ETA due ports and land regions in " + turns_exact + " turns.");
			}
			this.nextShipStop = FFToolsRegions.nextShipStop;
			if (this.nextShipStop!=null) {
				this.addComment("nächster Halt: " + this.nextShipStop.toString());
			} else {
				this.addComment("nächster Halt konnte leider nicht bestimmt werden");
			}
			// NACH-Order	
			super.addOrder("NACH " + path, true);
			
			// 20210816: Prüfe das Ziel, wenn wir nicht ein Boot steuern
			boolean destRegionOK=true;
			ShipType T = this.scriptUnit.getUnit().getModifiedShip().getShipType();
			if (T!=null) {
				if (!T.getName().equalsIgnoreCase("Boot")) {
					// ok, wir haben kein Boot - also brauchen wir entweder eine Ebene oder Wald - oder ein Hafen
					Region r = this.gd_Script.getRegion(dest);
					if (r!=null) {
						magellan.library.rules.RegionType RT = r.getRegionType();
						if (RT!=null) {
							if (!RT.getName().equalsIgnoreCase("Ebene") && !RT.getName().equalsIgnoreCase("Wald") && !RT.getName().equalsIgnoreCase("Ozean")) {
								// OK...wir visieren irgendein Landregion an - wir brauchen dann einen Hafen!
								// this.addComment("SailTo: Zielregion benötigt einen Hafen.");
								if (FFToolsRegions.hasSupportedRegionSpecificBuilding(r, "Hafen")) {
									this.addComment("SailTo: Zielregion hat einen Hafen, keine Infos über Unterhaltsproblem bekannt.");
								} else {
									this.doNotConfirmOrders("!!!Sailto: Zielregion hat keinen Hafen! (Oder Hafen mit Unterhaltsproblem)");
								}
							} else {
								// this.addComment("SailTo: Zielregion benötigt keinen Hafen.");
							}
						} else {
							this.addComment("!!!SailTo: unbekannter Regionstyp");
						}
					} else {
						this.addComment("!!!SailTo: unbekannte Zielregion");
					}
				} else {
					// this.addComment("SailTo: Boot erkannt - keine Zielprüfung");
				}
			} else {
				this.addComment("!!!SailTo: unbekannten Schiffstyp");
			}
			
			if (destRegionOK) {
				super.addComment("Einheit durch SAILTO bestätigt.",true);
			}
		} else {
			// path nicht gefunden
			super.scriptUnit.doNotConfirmOrders("Es konnte kein Weg gefunden werden. (SailTo)");
			addOutLine("X....kein Weg gefunden für " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
	}
	
	
}
