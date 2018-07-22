package com.fftools.scripts;


import magellan.library.CoordinateID;
import magellan.library.Region;

import com.fftools.utils.FFToolsRegions;

public class Avoidregion extends Script {
	
	private int Durchlauf1 = 5;
	
	private int[] runners = {Durchlauf1};
	
	
	
	// Parameterloser constructor
	public Avoidregion() {
		super.setRunAt(this.runners);
	}
	
	public void runScript(int scriptDurchlauf){		
		// hier code fuer GoTo
		// addOutLine("....start GoTo mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<1) {
			super.addComment("Das Ziel fehlt beim Aufruf von AvoidRegion!",true);
			super.addComment("Unit wurde durch AvoidRegion nicht bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders();
			addOutLine("X....fehlende Region bei AvoidRegion bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
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
				// region besorgen
				Region r = gd_Script.getRegion(actDest);
				if (r!=null){
					FFToolsRegions.excludeRegion(r);
					this.addComment("Region als ausgeschlossen hinzugefügt: " + r.toString());
				}
			} else {
				// Fehler beim Parsen des Ziels
				zielParseFehler();
			}
		}
	}
	
	private void zielParseFehler() {
		super.addComment("Ungueltiges Ziel beim Aufruf von AvoidRegion!",true);
		super.addComment("Unit wurde durch AvoidRegion NICHT bestaetigt", true);
		super.scriptUnit.doNotConfirmOrders();
		addOutLine("X....ungültiges AvoidRegion bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
}
