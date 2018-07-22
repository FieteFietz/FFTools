package com.fftools.scripts;


import java.util.List;

import magellan.library.CoordinateID;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;

import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsUnits;

public class Sailto extends Script{
	private static final int Durchlauf = 42;
	
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
				super.addComment("Das Ziel fehlt beim Aufruf von SAILTO!",true);
				super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
				super.scriptUnit.doNotConfirmOrders();
				addOutLine("X....fehlendes SAILTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			} else {
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
				 				super.addComment("Fehler beim setzen der nächsten // script SAILTO Anweisung",true);
				 				super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
				 				super.scriptUnit.doNotConfirmOrders();
				 				addOutLine("X....Fehler beim setzen der nächsten // script SAILTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				 			}
							
						} else {
							// das wars...Ziel erreicht und gut
							super.addComment("SAILTO: Einheit hat Ziel erreicht, daher NICHT bestätigt.",true);
							super.scriptUnit.doNotConfirmOrders();
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
			super.addComment("Einheit nicht als Kapitän eines Schiffes erkannt.",true);
			super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....Einheit nicht als Kapitän eines Schiffes erkannt: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
	}
	
	private void zielParseFehler() {
		super.addComment("Ungueltiges Ziel beim Aufruf von SAILTO!",true);
		super.addComment("Unit wurde durch SAILTO NICHT bestaetigt", true);
		super.scriptUnit.doNotConfirmOrders();
		addOutLine("X....ungültiges SAILTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void makeOrderNACH(CoordinateID act,CoordinateID dest){

		// FF 20070103: eingebauter check, ob es actDest auch gibt?!
		if (!com.fftools.utils.FFToolsRegions.isInRegions(this.gd_Script.getRegions(), dest)){
			// Problem  actDest nicht im CR -> abbruch
			super.addComment("Sailto Ziel nicht im CR",true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("!!! Sailto Ziel nicht im CR: " + this.unitDesc());
			return;
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
			// NACH-Order	
			super.addOrder("NACH " + path, true);
			super.addComment("Einheit durch SAILTO bestätigt.",true);
		} else {
			// path nicht gefunden
			super.addComment("Einheit durch SAILTO NICHT bestätigt.",true);
			super.addComment("Es konnte kein Weg gefunden werden.",true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....kein Weg gefunden für " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		}
	}
	
	
}
