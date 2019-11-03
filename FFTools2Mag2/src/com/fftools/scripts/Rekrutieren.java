package com.fftools.scripts;

import magellan.library.Order;
import magellan.library.Region;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.UnitID;
import magellan.library.rules.Race;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;


/**
 * eigentlich nutzlos
 * ToDo: check nach MatPool fehlt noch
 * @author Fiete
 *
 */
public class Rekrutieren extends MatPoolScript{
	
	private static final int Durchlauf_vorMP = 26;
	private static final int Durchlauf_nachMP = 720;
	
	private int[] runsAt = {Durchlauf_vorMP,Durchlauf_nachMP};
	
	private int silber_benoetigt = 0;
	private int rekrutierungskostenPrio = 900;
	private String kommentar = "Rekrutierungskosten";
	
	private MatPoolRequest myMPR = null;
	
	public static final String scriptCreatedTempMark = "// autoTEMP XXX!!!";
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Rekrutieren() {
		super.setRunAt(this.runsAt);
	}
	
	public void runScript(int scriptDurchlauf){
		switch (scriptDurchlauf){
		
		case Durchlauf_vorMP:this.vorMatPool(scriptDurchlauf);break;
	
		case Durchlauf_nachMP:this.nachMatPool(scriptDurchlauf);break;
		}
	}
	
	
	/**
	 * Eigentliche Prozedur
	 * runScript von Script.java MUSS/SOLLTE ueberschrieben werden
	 * Durchlauf kommt von ScriptMain
	 * 
	 * in Script steckt die ScriptUnit
	 * in der ScriptUnit steckt die Unit
	 * mit addOutLine jederzeit Textausgabe ans Fenster
	 * mit addComment Kommentare...siehe Script.java
	 */
	
	public void vorMatPool(int scriptDurchlauf){
		
		// falls kein parameter bzw zu viele
		if (super.getArgCount()< 1){
			addOutLine(super.scriptUnit.getUnit().toString(true) + ": Rekrutiere...unpassende Anzahl Parameter");
			super.scriptUnit.doNotConfirmOrders("Rekrutiere...unpassende Anzahl Parameter -> Unit unbestaetigt");
			return;
		}
		
		
		// Silber berechen...Race rausfinden
		// Race r = super.scriptUnit.getUnit().race;
		Race ra = super.scriptUnit.getUnit().getDisguiseRace();
		if (ra==null){
			ra = super.scriptUnit.getUnit().getRace();
		}
		
		
		int anzahl = 0;
		// check -> max
		if (super.getArgAt(0).equalsIgnoreCase("max")){
			anzahl = this.region().modifiedRecruit();
			// bei Orks verdoppeln
			// Hinweis von Argelas 20111114
			Race orkRace = this.gd_Script.getRules().getRace("Orks",false);
			if (orkRace==null){
				this.doNotConfirmOrders("Ork-Rasse nicht in den Regeln gefunden - FFTools braucht ein Update");
			} else {
				if (ra.equals(orkRace)){
					anzahl = anzahl*2;
					this.addComment("Rekrutieren: Orks erkannt. Maximal mögliche Rekruten verdoppelt auf:" + anzahl);
				}
			}
			
		} else {
			// falls nicht Anzahl angegeben wurde....
			anzahl = Integer.valueOf(super.getArgAt(0));
		}
		if (anzahl == 0){
			addOutLine(super.scriptUnit.getUnit().toString(true) + ": Rekrutiere...unpassende Anzahl Rekruten");
			super.scriptUnit.doNotConfirmOrders("Rekrutiere...unpassende Anzahl Rekruten -> Unit unbestaetigt");
			return;
		}
		
		// Optionen Parsen
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// RegionMax -> anzahl
		int regionsBestand=OP.getOptionInt("regionsBestand", -1);
		if (regionsBestand>-1){
			Region r = this.region();
			int nochFrei = r.getPeasants() - regionsBestand;
			if (nochFrei<=0){
				anzahl = 0;
				this.addComment("Regionsbestand von " + regionsBestand + " unterschritten.");
				return;
			} else {
				anzahl = Math.min(anzahl,nochFrei);
				this.addComment("zu Rekrutieren: " + anzahl + " Bauern");
			}
		}		
		
		
		// Silber berechen
		
		this.silber_benoetigt = anzahl * ra.getRecruitmentCosts();
		
		
		// silberprio eventuell anders?
		int newSilberPrio=reportSettings.getOptionInt("Rekrutieren_SilberPrio", super.region());
		if (newSilberPrio>-1){
			this.rekrutierungskostenPrio=newSilberPrio;
		}
		// order ergaenzen
		// eigentlich erst, wenn wir Silber erhalten haben..also nach MatPool...
		
		super.addOrder("REKRUTIEREN " + anzahl, true);
		
		// ?? sonderfall ??
		// this.region().refreshUnitRelations(true);
		this.getUnit().reparseOrders();
		this.scriptUnit.incRecruitedPersons(anzahl);
		
		
		// debug
		// int test = this.scriptUnit.getUnit().getModifiedPersons();
		
		// Rekrutierungskosten vom Pool anfordern
		myMPR = new MatPoolRequest(this,this.silber_benoetigt,"Silber",this.rekrutierungskostenPrio,this.kommentar);
		myMPR.setOnlyRegion(true);
		this.addMatPoolRequest(myMPR);
		
		
		// Anhängsel....Bauern automatisch versenden?
		// wenn eigene (unmodifizierte) Personenanzahl> Limit
		// -> temp unit mit befehlen hinter // tempunit: ausstatten
		
		int tempAB = OP.getOptionInt("tempAB", 0);
		if (tempAB>0){
			// checken
			if (this.scriptUnit.getUnit().getPersons()>tempAB){
				int personen = this.scriptUnit.getUnit().getPersons();
				// bingo !
				this.addComment("-> automatische TEMP Erstellung");
				while (personen>tempAB){
					// temp anlegen
					// neue Unit ID
					Unit parentUnit = this.scriptUnit.getUnit();
					UnitID id = UnitID.createTempID(this.gd_Script, this.scriptUnit.getScriptMain().getSettings(), parentUnit);
					// Die tempUnit anlegen
					TempUnit tempUnit = parentUnit.createTemp(this.gd_Script,id);
					tempUnit.addOrder(Rekrutieren.scriptCreatedTempMark);
					// Kommandos setzen
					// Kommandos durchlaufen
					for (Order o:this.scriptUnit.getUnit().getOrders2()){
						String s = o.getText();
						if (s.startsWith("// tempunit:")){
							s = s.substring(12);
							tempUnit.addOrder(s);
						}
					}
					tempUnit.setOrdersConfirmed(true);
					// Personen übergeben
					String newCommand = "GIB TEMP " + id.toString() + " " + tempAB + " Personen ;script Rekrutieren";
					super.addOrder(newCommand, true);
					personen = personen - tempAB;
				}

			} else {
				this.addComment("automatische Erstellung einer TEMP erst ab " + tempAB + " Personen.");
			}
		
		}
		
	}
	
	
	public void nachMatPool(int scriptDurchlauf){
		if (this.myMPR!=null && this.myMPR.getOriginalGefordert()>0) {
			int diff = myMPR.getOriginalGefordert() - myMPR.getBearbeitet();
			if (diff!=0){
				outText.addOutLine("Nicht genügend Silber zum Rekrutieren! " + this.unitDesc(), true);
				this.scriptUnit.doNotConfirmOrders("!!! Rekrut: nicht genügend Silber. " + diff + " Fehlen. (Prio:" + this.rekrutierungskostenPrio + ")");
			}
		}
	}
	
	
	
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
}
