package com.fftools.scripts;


import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

import magellan.library.CoordinateID;
import magellan.library.Region;
import magellan.library.gamebinding.EresseaRelationFactory;

public class Bauern extends MatPoolScript implements WithGotoInfo{
	
	private int Durchlauf1 = 23;
	
	private int[] runners = {Durchlauf1};
	
	private GotoInfo gotoInfo= null;
	
	private CoordinateID HomeRegionCoord = null;
	
	// Parameterloser constructor
	public Bauern() {
		super.setRunAt(this.runners);
	}
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf1){
			this.firstRun();
		}
		
	}
	
	
	public void firstRun(){		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Bauern");
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
		
		CoordinateID actRegCoordID = super.scriptUnit.getUnit().getRegion().getCoordinate();
		if (actRegCoordID.equals(HomeRegionCoord)){
			// wir sind am Ziel
			this.addComment("Bauern am Ziel - Tschüß !");
			this.addOrder("arbeiten", true);
			String GIB_Order="GIB 0 ALLES Personen ;Bauern am Ziel";
			this.addOrder(GIB_Order, true);
			this.scriptUnit.specialProtectedOrders.add(GIB_Order);
			// Test!! -> funktioniert
			EresseaRelationFactory ERF = ((EresseaRelationFactory) this.gd_Script.getGameSpecificStuff().getRelationFactory());
			boolean updaterStopped = ERF.isUpdaterStopped();
			if (!updaterStopped){
				ERF.stopUpdating();
			}
			Region r = this.region();
			ERF.processRegionNow(r);
			if (!updaterStopped){
				ERF.restartUpdating();
			}
			// fertig
		} else {
			// wir müssen noch zum Ziel
			this.gotoInfo = new GotoInfo();
			if (this.HomeRegionCoord!=null) {
				this.gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(), this.HomeRegionCoord,true,"Bauern zur Home-Region");
				this.addComment("Restweg: " + this.gotoInfo.getAnzRunden() + " Wochen");
			} else {
				this.doNotConfirmOrders("!!! Bauern - Probleme beim Weg zur Home Region");
			}
		}
	}
	

	public GotoInfo getGotoInfo(){
		return this.gotoInfo;
	}
	
	
}
