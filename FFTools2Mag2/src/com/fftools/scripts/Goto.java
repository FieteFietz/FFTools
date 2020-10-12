package com.fftools.scripts;


import magellan.library.CoordinateID;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

public class Goto extends MatPoolScript implements WithGotoInfo{
	
	private int Durchlauf1 = 44;
	private int Durchlauf2 = 214;
	
	private int[] runners = {Durchlauf1,Durchlauf2};
	
	private GotoInfo gotoInfo= null;
	
	private CoordinateID move_dest = null;
	private CoordinateID move_act = null;
	
	
	// Parameterloser constructor
	public Goto() {
		super.setRunAt(this.runners);
	}
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf1){
			this.firstRun();
		}
		if (scriptDurchlauf==Durchlauf2){
			this.sndRun();
		}
	}
	
	
	public void firstRun(){		
		// 20200428: Parameter abfragen
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Goto");
		OP.addOptionList(this.getArguments());
		if (OP.getOptionBoolean("pferde", false)) {
			// kleinen Request f�r Pferde erg�nzen
			this.addComment("Pferdewunsch erkannt, Pferde angefordert.");
			this.addMatPoolRequest(new MatPoolRequest(this,this.getUnit().getModifiedPersons(),"Pferd",3,"GOTO mit Pferde=ja"));
		}
		
		// hier code fuer GoTo
		// addOutLine("....start GoTo mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<1) {
			super.scriptUnit.doNotConfirmOrders("Das Ziel fehlt beim Aufruf von GOTO!");
			super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
			addOutLine("X....fehlendes GOTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		} else {
			// wir haben zumindest ein Ziel
			// TH: Pr�fen, ob Koordinate oder Regionsname, falls Komma in Regionsangabe sind es wohl Koordinaten...
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
						// 20200428 es k�nnte auch eine Option sein...
						if (super.getArgAt(1).indexOf('=') > 0) {
							// Hossa, eine Option, und damit sind wir schon am Ziel 
							// das wars...Ziel erreicht und gut
							super.scriptUnit.doNotConfirmOrders("GOTO: Einheit hat Ziel erreicht, daher NICHT best�tigt.");
						} else {
							// es gibt weitere Ziele
							// aktuelles Ziel aus der Liste nehmen..
							// neue script order erstellen und anf�gen
							// neuen Path berechnen
							
							// neue GOTO bilden
							String newGOTO = "GOTO ";
							for (int i = 1;i<super.getArgCount();i++){
								newGOTO = newGOTO.concat(super.getArgAt(i) + " ");
							}
							// ersetzen
							if (super.scriptUnit.replaceScriptOrder(newGOTO, "GOTO ".length())) {
								// OK...soweit alles klar
								// neues Ziel setzen
								actDest = CoordinateID.parse(super.getArgAt(1),",");
								if (actDest == null) {
									this.addComment("GOTO, Problem bei " + super.getArgAt(1));
									zielParseFehler();
								} else {
									// fein 
									makeOrderNACH(actRegCoordID,actDest);
								}
				 			} else {
				 				// irgendetwas beim ersetzen ist schief gegangen
				 				super.scriptUnit.doNotConfirmOrders("Fehler beim setzen der n�chsten // script GOTO Anweisung");
				 				super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
				 				addOutLine("X....Fehler beim setzen der n�chsten // script GOTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				 			}
						}
					} else {
						// das wars...Ziel erreicht und gut
						super.scriptUnit.doNotConfirmOrders("GOTO: Einheit hat Ziel erreicht, daher NICHT best�tigt.");
					}
				} else {
					// nope, da m�ssen wir noch hin
					makeOrderNACH(actRegCoordID,actDest);
				}
			} else {
				// Fehler beim Parsen des Ziels
				this.addComment("GOTO, Problem bei " + super.getArgAt(0));
				zielParseFehler();
			}
		}
	}
	
	private void zielParseFehler() {
		super.scriptUnit.doNotConfirmOrders("Ungueltiges Ziel beim Aufruf von GOTO!");
		super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
		addOutLine("X....ung�ltiges GOTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void makeOrderNACH(CoordinateID act,CoordinateID dest){		
		this.move_act=act;
		this.move_dest = dest;
		// Falls wir im Geb�ude sind, und da nicht schon raus gehen, verlassen setzen
		// Hat den Effekt, was wir keinen Geb�udeunterhalt mehr bekommen, welcher
		// uns eventuell �berl�dt
		if (this.scriptUnit.getUnit().getBuilding()!=null && this.scriptUnit.getUnit().getModifiedBuilding()!=null){
			this.addComment("Goto: VERLASSEN wird explizit gesetzt");
			this.addOrder("VERLASSEN ;von GOTO", true);
			this.scriptUnit.isLeavingBuilding=true;
		}
	}
	
	private void sndRun() {
		this.gotoInfo = new GotoInfo();
		if (this.move_act!=null && this.move_dest!=null) {
			this.gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.move_act, this.move_dest,true,"Goto - makeOrderNach 2ndRun");
		}
	}
	

	public GotoInfo getGotoInfo(){
		return this.gotoInfo;
	}
	
	
}
