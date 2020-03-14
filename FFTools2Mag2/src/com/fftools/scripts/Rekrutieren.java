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
	private static final int Durchlauf_nachMP = 28; // war 720
	
	private int[] runsAt = {Durchlauf_vorMP,Durchlauf_nachMP};
	
	private int silber_benoetigt = 0;
	private int rekrutierungskostenPrio = 900;
	private String kommentar = "Rekrutierungskosten";
	
	private MatPoolRequest myMPR = null;
	
	public static final String scriptCreatedTempMark = "// autoTEMP XXX!!!";
	
	private boolean ConfirmLessRecruitment=false;
	
	private boolean weAreOrk=false;
	
	private int recruitCosts = 10;
	
	private int anzahlGeplant=0;
	
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
		
		
		// this.addComment("Rekrutieren - vorMP, Durchlauf: " + scriptDurchlauf);
		
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
					this.weAreOrk=true;
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
		// this.addComment("Rekrutieren - Anzahl: " + anzahl);
		// Optionen Parsen
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Rekrutieren");
		OP.addOptionList(this.getArguments());
		
		this.ConfirmLessRecruitment = OP.getOptionBoolean("ConfirmLessRecruitment", this.ConfirmLessRecruitment);
		if (this.ConfirmLessRecruitment) {
			this.addComment("Option erkannt: ConfirmLessRecruitment -> es wird rekrutiert, soweit das Silber reicht.");
		}
		
		// RegionMax -> anzahl
		int regionsBestand=OP.getOptionInt("regionsBestand", -1);
		if (regionsBestand>-1){
			Region r = this.region();
			int nochFrei = r.getPeasants() - regionsBestand;
			if (nochFrei<=0){
				anzahl = 0;
				this.addComment("Regionsbestand von " + regionsBestand + " unterschritten.");
			} else {
				anzahl = Math.min(anzahl,nochFrei);
				this.addComment("zu Rekrutieren: " + anzahl + " Bauern");
			}
		}		
		
		this.anzahlGeplant=anzahl;
		
		
		// 20200202 FF EON nur bis zu einer bestimmten größe der Einheit
		int maxPersonen = OP.getOptionInt("maxPersonen", 0);
		if (maxPersonen>0 && this.getUnit().getPersons()>=maxPersonen) {
			this.addComment("!!!Rekrutieren: maxPersonen erreicht -> Einheit rekrutiert nicht und verbleibt unbestätigt");
			this.anzahlGeplant=0;
			this.doNotConfirmOrders("!!!Rekrutieren: maxPersonen erreicht -> Einheit rekrutiert nicht und verbleibt unbestätigt");
		}
		
		// Silber berechen
		this.recruitCosts = ra.getRecruitmentCosts();
		this.silber_benoetigt = anzahl * this.recruitCosts;
		
		if (this.anzahlGeplant>0) {
		
			// silberprio eventuell anders?
			int newSilberPrio=reportSettings.getOptionInt("Rekrutieren_SilberPrio", super.region());
			if (newSilberPrio>-1){
				this.rekrutierungskostenPrio=newSilberPrio;
			}
			
			
			
			// debug
			// int test = this.scriptUnit.getUnit().getModifiedPersons();
			
			// Rekrutierungskosten vom Pool anfordern
			myMPR = new MatPoolRequest(this,this.silber_benoetigt,"Silber",this.rekrutierungskostenPrio,this.kommentar);
			myMPR.setOnlyRegion(true);
			this.addMatPoolRequest(myMPR);
			// this.addComment("Rekrutieren - fordere an: " + this.silber_benoetigt + " Silber mit Prio " + this.rekrutierungskostenPrio);
			
		}
		
		// Anhängsel....Bauern automatisch versenden?
		// wenn eigene (unmodifizierte) Personenanzahl> Limit
		// -> temp unit mit befehlen hinter // tempunit: ausstatten
		
		int tempAB = OP.getOptionInt("tempAB", 0);
		if (tempAB>0){
			// checken
			int personen = this.scriptUnit.getUnit().getPersons();
			if (personen>tempAB){
				
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
					personen -= tempAB;
				}

			} else {
				this.addComment("automatische Erstellung einer TEMP erst ab " + tempAB + " Personen.");
			}
		
		}
		// this.addComment("Rekrutieren - EOM 1 ");
		
	}
	
	
	public void nachMatPool(int scriptDurchlauf){
		if (this.myMPR!=null && this.myMPR.getOriginalGefordert()>0) {
			int diff = myMPR.getOriginalGefordert() - myMPR.getBearbeitet();
			if (diff!=0){
				if (this.ConfirmLessRecruitment) {
					// mit weniger zufrieden geben
					int neueAnzahl = myMPR.getBearbeitet() / this.recruitCosts;
					if (neueAnzahl>0) {
						this.addComment("nur " + myMPR.getBearbeitet() + " Silber erhalten, wird für " + neueAnzahl + " Rekruten verwendet");
						doRekrutiere(neueAnzahl);
					} else {
						this.addComment("nur " + myMPR.getBearbeitet() + " Silber erhalten, es wird nicht rekrutiert");
					}
				} else {
					outText.addOutLine("Nicht genügend Silber zum Rekrutieren! " + this.unitDesc(), true);
					this.scriptUnit.doNotConfirmOrders("!!! Rekrut: nicht genügend Silber. " + diff + " Fehlen. (Prio:" + this.rekrutierungskostenPrio + ")");
				}
			} else {
				// alles OK
				doRekrutiere(this.anzahlGeplant);
			}
		} else {
			this.addComment("!!!Rekrutieren: überraschend keine Silberanforderung gefunden - Abbruch.");
		}
	}
	
	
	private void doRekrutiere(int anzahl) {
		// order ergaenzen
		// eigentlich erst, wenn wir Silber erhalten haben..also nach MatPool...
		
		super.addOrder("REKRUTIEREN " + anzahl, true);
		
		// ?? sonderfall ??
		// this.region().refreshUnitRelations(true);
		this.getUnit().reparseOrders();
		this.scriptUnit.incRecruitedPersons(anzahl);
	}
}
