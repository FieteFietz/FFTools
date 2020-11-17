package com.fftools.scripts;


import magellan.library.CoordinateID;
import magellan.library.Skill;
import magellan.library.rules.SkillType;

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
			// kleinen Request für Pferde ergänzen
			int persons = this.scriptUnit.getUnit().getModifiedPersons();
			int user_pers_gewicht = OP.getOptionInt("pers_gewicht", -1);
			int anz_pferde = persons;
			if (user_pers_gewicht>0){
				anz_pferde = (int)Math.ceil(((double)persons * (double)user_pers_gewicht)/20);
			}
			SkillType reitType = this.gd_Script.getRules().getSkillType("Reiten");
			Skill reitSkill = this.scriptUnit.getUnit().getModifiedSkill(reitType);
			// schauen wir mal, ob unser reittalent ausreicht...
			int maxPferde=0;
			if (reitSkill!=null && reitSkill.getLevel()>0) {
				maxPferde = persons * reitSkill.getLevel() * 2;
				if (maxPferde<anz_pferde) {
					this.addComment("Goto: ich würde gerne " + anz_pferde + " Pferde mitführen, mein Können reicht aber nur für " + maxPferde + " ...");
					anz_pferde=maxPferde;
				}
				this.addComment("Pferdewunsch erkannt, " + anz_pferde + " Pferde angefordert.");
				this.addMatPoolRequest(new MatPoolRequest(this,anz_pferde,"Pferd",3,"GOTO mit Pferde=ja"));
			} else {
				this.doNotConfirmOrders("Goto: Einheit soll Pferde mitführen, kann aber gar nicht reiten!!!");
			}
		}
		
		// hier code fuer GoTo
		// addOutLine("....start GoTo mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<1) {
			super.scriptUnit.doNotConfirmOrders("Das Ziel fehlt beim Aufruf von GOTO!");
			super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
			addOutLine("X....fehlendes GOTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
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
						// 20200428 es könnte auch eine Option sein...
						if (super.getArgAt(1).indexOf('=') > 0) {
							// Hossa, eine Option, und damit sind wir schon am Ziel 
							// das wars...Ziel erreicht und gut
							super.scriptUnit.doNotConfirmOrders("GOTO: Einheit hat Ziel erreicht, daher NICHT bestätigt.");
						} else {
							// es gibt weitere Ziele
							// aktuelles Ziel aus der Liste nehmen..
							// neue script order erstellen und anfügen
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
				 				super.scriptUnit.doNotConfirmOrders("Fehler beim setzen der nächsten // script GOTO Anweisung");
				 				super.addComment("Unit wurde durch GOTO NICHT bestaetigt", true);
				 				addOutLine("X....Fehler beim setzen der nächsten // script GOTO Anweisung bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				 			}
						}
					} else {
						// das wars...Ziel erreicht und gut
						super.scriptUnit.doNotConfirmOrders("GOTO: Einheit hat Ziel erreicht, daher NICHT bestätigt.");
					}
				} else {
					// nope, da müssen wir noch hin
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
		addOutLine("X....ungültiges GOTO Ziel bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	
	private void makeOrderNACH(CoordinateID act,CoordinateID dest){		
		this.move_act=act;
		this.move_dest = dest;
		// Falls wir im Gebäude sind, und da nicht schon raus gehen, verlassen setzen
		// Hat den Effekt, was wir keinen Gebäudeunterhalt mehr bekommen, welcher
		// uns eventuell überlädt
		// if (this.scriptUnit.getUnit().getBuilding()!=null || this.scriptUnit.getUnit().getModifiedBuilding()!=null){
	    if (this.scriptUnit.getUnit().getModifiedBuilding()!=null){
			this.addComment("Goto: VERLASSEN wird explizit gesetzt");
			this.addOrder("VERLASSEN ;von GOTO", true);
			this.scriptUnit.isLeavingBuilding=true;
			this.remove_unterhalt();
		}
	}
	
	private void remove_unterhalt() {
		Object o = this.scriptUnit.getScript(Gebaeudeunterhalt.class);
		if (o!=null) {
			Gebaeudeunterhalt g = (Gebaeudeunterhalt)o;
			g.set_zero_unterhalt(" verlässt Gebäude (GoTo)");
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
