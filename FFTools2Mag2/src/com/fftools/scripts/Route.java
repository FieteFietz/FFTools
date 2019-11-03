package com.fftools.scripts;


import magellan.library.CoordinateID;
import magellan.library.Region;

import java.util.ArrayList;

import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsUnits;

public class Route extends Script{
	
	private static final int Durchlauf = 40;
	
	// Parameterloser constructor
	public Route() {
		super.setRunAt(Durchlauf);
	}
	
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf!=Durchlauf){return;}
		
		// hier code fuer Route
		// addOutLine("....start Route mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<2) {
			super.addComment("Unit wurde durch ROUTE NICHT bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders("Ein Ziel fehlt beim Aufruf von ROUTE!");
			addOutLine("X....fehlendes ROUTE Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
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
					// es gibt weitere Ziele, zumindest eines
					// aktuelles Ziel aus der Liste nehmen und hinten anfügen
					// aktuelle GOTO erstellen und an scriptList anfügen
					// neuen Path berechnen -> macht GOTO
					
					// neue ROUTE bilden
					String newROUTE = "ROUTE ";
					for (int i = 1;i<super.getArgCount();i++){
						newROUTE = newROUTE.concat(super.getArgAt(i) + " ");
					}
					// noch hinten den bisher ersten wieder dranne
					newROUTE = newROUTE.concat(super.getArgAt(0) + " ");
					
					// ersetzen
					if (super.scriptUnit.replaceScriptOrder(newROUTE, "ROUTE ".length())) {
						// OK...soweit alles klar
						// neues Ziel setzen; TH: wieder mit Unterscheidung Coordinate vs. Region
						if (super.getArgAt(1).indexOf(',') > 0) {
							actDest = CoordinateID.parse(super.getArgAt(1),",");
						} else {
						// Keine Koordinaten, also Region in Koordinaten konvertieren
							actDest = FFToolsRegions.getRegionCoordFromName(this.gd_Script, super.getArgAt(1));
						}
						if (actDest == null) {
							zielParseFehler();
						} else {
							// fein 
							setGOTO(actDest);
							super.addComment("Routenpunkt erreicht. Nächster Routenpunkt ausgewählt.",true);
						}
		 			} else {
		 				// irgendetwas beim ersetzen ist schief gegangen
		 				super.scriptUnit.doNotConfirmOrders("Fehler beim setzen der nächsten // script GOTO Anweisung");
		 				addOutLine("X....Fehler beim setzen der nächsten // script GOTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		 			}
						
					 
				} else {
					// nope, da müssen wir noch hin
					setGOTO(actDest);
				}
			} else {
				// fehler beim parsen des Ziels
				zielParseFehler();
			}
		}
	}
	
	private void zielParseFehler() {
		super.scriptUnit.doNotConfirmOrders("Ungueltiges Ziel beim Aufruf von ROUTE!");
		addOutLine("X....ungültiges ROUTE Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void setGOTO(CoordinateID dest){
		// lediglich ein GOTO script zum dest erzeugen...wird in späterem Lauf dort bearbeitet
		// naja...wenn Kapitän, dann ein SAILTO....
		if (FFToolsUnits.checkShip(this)){
			super.scriptUnit.findScriptClass("Sailto",dest.toString(","));
		} else {
			super.scriptUnit.findScriptClass("Goto",dest.toString(","));
		}
	}

	public boolean isInRegionList(Region r) {
		boolean erg = false;
		CoordinateID actRegCoordID = r.getCoordinate();
		String s1 = actRegCoordID.getX() + "," + actRegCoordID.getY();
		ArrayList<String> args = super.getArguments();
		if (args!=null && args.size()>0) {
			for (String s2 : args) {
				if (s2.equalsIgnoreCase(s1)) {
					erg=true;
					break;
				}
			}
		}
		return erg;
	}
	
	
}
