package com.fftools.scripts;


import com.fftools.pools.seeschlangen.MonsterJagdManager_MJM;
import com.fftools.pools.seeschlangen.MonsterRegion;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;

import magellan.library.CoordinateID;
import magellan.library.Region;

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
		
		// 20210815: mode=auto und jhetzt mit OP
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Avoidregion");
		OP.addOptionList(this.getArguments());
		if (OP.getOptionString("mode").equalsIgnoreCase("auto")) {
			// neues verhalten: wenn Monster in der Region, dann auf avoidregion setzen, sonst nicht
			Region r2 = this.region();
			MonsterJagdManager_MJM MJM = this.getOverlord().getMJM();
    		int countMonster = MJM.countMonster(r2);
    		if (countMonster>0) {
    			this.addComment("AvoidRegion (auto): hier " + countMonster + " Monster erkannt. Region wird beim Pathfinding vermieden.");
    			FFToolsRegions.excludeRegion(r2);
    		} else {
    			this.addComment("AvoidRegion (auto): hier keine Monster erkannt.");
    		}
			
		} else {
			// normales altes Verhalten
			if (super.getArgCount()<1) {
				super.scriptUnit.doNotConfirmOrders("Das Ziel fehlt beim Aufruf von AvoidRegion!");
				super.addComment("Unit wurde durch AvoidRegion nicht bestaetigt", true);
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
	}
	
	private void zielParseFehler() {
		super.scriptUnit.doNotConfirmOrders("Ungueltiges Ziel beim Aufruf von AvoidRegion!");
		super.addComment("Unit wurde durch AvoidRegion NICHT bestaetigt", true);
		addOutLine("X....ungültiges AvoidRegion bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	public boolean allowMultipleScripts(){
		return true;
	}
	
}
