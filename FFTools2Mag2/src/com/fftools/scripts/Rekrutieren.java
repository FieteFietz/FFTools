package com.fftools.scripts;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Building;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.UnitID;
import magellan.library.gamebinding.EresseaRelationFactory;
import magellan.library.rules.CastleType;
import magellan.library.rules.Race;


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
	
	private int recruitCosts = 10;
	
	private int anzahlGeplant=0;
	
	/**
	 * spezialmodi
	 * 1) EON
	 */
	private String mode="";
	
	
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
		
		/*
		 * alt: wir geben diese Runde befehle für nächste Runde, müssen also Jahreszeit nächste Runde prüfen
		if (this.scriptUnit.isInsekt() && FFToolsGameData.isNextTurnWinter(this.getOverlord().getScriptMain().gd_ScriptMain)) {
			if (this.region().getRegionType().getName().equalsIgnoreCase("Wüste")) {
				this.addComment("Rekrutieren: wie gut, dass hier so viel warmer Sand ist! (Insekten im Winter in der Wüste");
			} else {
				this.addComment("Nächste Runde ist Winter...ich kann (vermutlich) nicht rekrutieren! (Insekt)");
				if (FFToolsGameData.hasNestwaermeEffekt(this.scriptUnit)) {
					this.addComment("Nestwärme erkannt!...ich kann doch rekrutieren!");
				} else {
					this.addComment("Keine Nestwärme erkannt - ich bin ein frierendes armes Insekt und habe auf nix Lust");
					return;
				}
			}
		}
		*/

		// neu 20250216: es gilt, was im Report steht - also aktuelle runde
		if (this.scriptUnit.isInsekt() && FFToolsGameData.isThisTurnWinter(this.getOverlord().getScriptMain().gd_ScriptMain)) {
			if (this.region().getRegionType().getName().equalsIgnoreCase("Wüste")) {
				this.addComment("Rekrutieren: wie gut, dass hier so viel warmer Sand ist! (Insekten im Winter in der Wüste");
			} else {
				this.addComment("Diese Runde ist Winter...ich kann (vermutlich) nicht rekrutieren! (Insekt)");
				if (FFToolsGameData.hasNestwaermeEffekt(this.scriptUnit)) {
					this.addComment("Nestwärme erkannt!...ich kann doch rekrutieren!");
				} else {
					this.addComment("Keine Nestwärme erkannt - ich bin ein frierendes armes Insekt und habe auf nix Lust");
					return;
				}
			}
		}
		
		
		// Silber berechen...Race rausfinden
		// Race r = super.scriptUnit.getUnit().race;
		/*
		Race ra = super.scriptUnit.getUnit().getDisguiseRace();
		if (ra==null){
			ra = super.scriptUnit.getUnit().getRace();
		}
		*/
		Race ra = super.scriptUnit.getUnit().getRace();
		
		
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
			try {
				anzahl = Integer.valueOf(super.getArgAt(0));
			} catch (NumberFormatException NFE) {
				anzahl=0;
				this.doNotConfirmOrders("!Fehler in Erkennung der Anzahl!");
				this.addComment(NFE.toString());
				addOutLine("!!! " + super.scriptUnit.getUnit().toString(true) + ": Rekrutiere Fehler in Erkennung der Anzahl");
				return;
			}
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
		
		// 20250502 FF Kleingeldprinz
		/*
		 * Moin. Ist irgendwann geplant, den Befehl rekrutieren noch irgendwann anzupassen? Ich wünsche mir (wenn das Leben ein Wunschkonzert wäre), dass man folgendes eingibt:
			// script Rekrutieren max ziel=100
			Wenn die Region z.B. 33 Rekruten pro Runde zulassen würde, würde FFTools folgendes machen:
			Runde 1: 33 Rekrutieren
			Runde 2, 33 Rekrutieren
			Runde 3, 33 Rekrutieren (nun haben wir insgesamt schon 99 Personen in der Einheit)
			Runde 4, 1 Rekrutieren, da das Ziel ja 100 ist.
			Was FFtools bisher macht, ist jetzt noch einmal 33 zu Rekrutieren. Aber ein hinarbeiten auf eine echte Zielgröße wäre echt supercool 
		 */
		
		int ziel = OP.getOptionInt("ziel", 0);
		if (ziel>0 && this.anzahlGeplant>0) {
			if (this.anzahlGeplant + this.getUnit().getPersons()>ziel) {
				this.addComment("Geplante Anzahl Rekruten (" + this.anzahlGeplant + ") führt zum Überschreiten des Ziels (" + ziel + ")");
				this.anzahlGeplant = ziel - this.getUnit().getPersons();
				if (this.anzahlGeplant<0) {
					this.anzahlGeplant=0;
				}
				this.addComment("Anzahl Rekruten angepasst auf: " + this.anzahlGeplant);
				if (this.anzahlGeplant==0) {
					this.addComment("!!!Rekrutieren: Ziel bereits erreicht -> Einheit rekrutiert nicht und verbleibt unbestätigt");
					this.doNotConfirmOrders("!!!Rekrutieren: Ziel bereits erreicht -> Einheit rekrutiert nicht und verbleibt unbestätigt");
				} else {
					this.addComment("Rekrutieren: Ziel wird erreicht -> Einheit verbleibt unbestätigt");
					this.doNotConfirmOrders("Rekrutieren: Ziel wird erreicht -> Einheit verbleibt unbestätigt");
				}
			}
		}
		
		
		
		// 20240428 FF EON Also Unterhalten, bei genug Silbervorrat Unterhaltung lernen bis 3, dann verwässern auf 2. 
		// Das geht recht einfach mit 2/3 der aktuellen Personenzahl. Und das ganze wird wiederholt bis die Anzahl 
		// an Zwergen 1/10 des Überschusses den die Bauern erarbeiten entspricht. 
		
		String actMode = OP.getOptionString("mode");
		if (actMode.equalsIgnoreCase("EON")) {
			anzahl = this.getUnit().getPersons();
			if (anzahl==0) {
				this.doNotConfirmOrders("!!!Rekrutieren: mode EON - aktuelle Anzahl der Personen ist 0!!!");
				return;
			}
			anzahl = (int) Math.floor((2*anzahl)/3);
			this.addComment("mode EON: zu rekrutieren 2/3 der Personen: " + anzahl + " Personen");
			
			// Überschuss der Bauern
			// Bauernverdienst
			int bauernverdienst = (this.region().getPeasantWage()-10)*this.region().getModifiedPeasants();
			if (this.region().getTrees()>0){
				bauernverdienst-=this.region().getTrees() * 8 * (this.region().getPeasantWage()-10);
			}
			if (this.region().getSprouts()>0){
				bauernverdienst-=this.region().getSprouts() * 4 * (this.region().getPeasantWage()-10);
			}
			this.addComment("mode EON: Bauernverdienst: " + bauernverdienst + " Silber");
			int maxPersonenEON=(int) Math.floor(bauernverdienst/10);
			this.addComment("mode EON: maxPersonen: " + maxPersonenEON);
			int maxAnzahlEON = maxPersonenEON - this.getUnit().getPersons();
			this.addComment("mode EON: zu Rekrutieren gemäß Bauernverdienst: " + maxAnzahlEON);
			anzahl = Math.min(anzahl, maxAnzahlEON);
			if (anzahl<0) {anzahl=0;}
			
			// ceck grösse der Burg
			Building b = this.getUnit().getModifiedBuilding();
			if (b!=null) {
				if (b.getBuildingType() instanceof CastleType) {
					// Anzahl der Insassen ermiiteln
					int anz_insassen = 0;
					for (Unit u:b.modifiedUnits()) {
						anz_insassen += u.getModifiedPersons();
					}
					int max_dazu = b.getSize()-anz_insassen;
					if (max_dazu<0) {
						max_dazu=0;
					}
					this.addComment("mode EON: maximale Rekruten wegen Burgengröße: " + max_dazu + " (Burg: " + b.getSize() + ", Insassen: " + anz_insassen + ")");
					if (anzahl>max_dazu) {
						anzahl=max_dazu;
					}
				} else {
					this.addComment("mode EON: kein Check der Burggrösse, Einheit ist nicht in einer Burg");
				}
			} else {
				this.addComment("mode EON: kein Check der Burggrösse, Einheit ist nicht in einem Gebäude");
			}
			
			// Abgleich mit maximal zu rekrutierenden Bauern in der Region
			int maxRecruitRegion = this.getUnit().getRegion().getRecruits();
			if (maxRecruitRegion>anzahl) {
				this.addComment("mode EON: geplante Anzahl in der Region verfügbar (max: " + maxRecruitRegion + ")");
			} else {
				this.addComment("mode EON: geplante Anzahl nicht der Region verfügbar, setze auf Rekrutierungslimit: " + maxRecruitRegion);
				anzahl=maxRecruitRegion;
			}
			
			this.addComment("mode EON: zu Rekrutieren final: " + anzahl);
			this.anzahlGeplant=anzahl;
		}
		
		// Silber berechen
		this.recruitCosts = ra.getRecruitmentCosts();
		this.silber_benoetigt = anzahl * this.recruitCosts;
		
		if (this.anzahlGeplant>0) {
			String prioInfo="nix";
			// silberprio eventuell anders?
			int newSilberPrio=reportSettings.getOptionInt("Rekrutieren_SilberPrio", super.region());
			if (newSilberPrio>-1){
				this.rekrutierungskostenPrio=newSilberPrio;
				prioInfo="Rekrutierungskosten aus den Repoortsettings übernommen";
			}
			
			int localPrio = OP.getOptionInt("prio", 0);
			if (localPrio>0) {
				this.rekrutierungskostenPrio=localPrio;
				prioInfo="Rekrutierungskosten aus den Befehlsparametern übernommen";
			}
			
			if (prioInfo.length()>5) {
				this.addComment(prioInfo);
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
				// 20240526: Hinweis von EON
				/*
				 * Nicht sehr wichtig, aber script Rekrutieren und vermutlich auch andere, versucht auch mit hungernden Einheiten Personen 
				 * abzugeben, was eressea nicht erlaubt.
				 */
				
				if (this.getUnit().isStarving()) {
					this.doNotConfirmOrders("!!! temp kann nicht erstellt werden, einheit hungert !!!");
				} else {
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
							if (s.toLowerCase().startsWith("// tempunit:")){
								s = s.substring(12);
								tempUnit.addOrder(s);
							}
						}
						tempUnit.setOrdersConfirmed(true);
						// Personen übergeben
						int tempModPersons= tempAB;
						if (OP.getOptionBoolean("tempSingleUnit", false)) {
							tempModPersons = personen-1;
						}
						String newCommand = "GIB TEMP " + id.toString() + " " + tempModPersons + " Personen ;script Rekrutieren";
						super.addOrder(newCommand, true);
						personen -= tempModPersons;
					}
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
			if (this.anzahlGeplant>0) {
				this.addComment("!!!Rekrutieren: überraschend keine Silberanforderung gefunden - Abbruch.");
			}
		}
	}
	
	
	private void doRekrutiere(int anzahl) {
		// order ergaenzen
		// eigentlich erst, wenn wir Silber erhalten haben..also nach MatPool...
		if (anzahl<=0) {
			return;
		}
		super.addOrder("REKRUTIEREN " + anzahl, true);
		
		// ?? sonderfall ??
		// this.region().refreshUnitRelations(true);
		this.getUnit().reparseOrders();
		this.scriptUnit.incRecruitedPersons(anzahl);
		
		// Test!! -> funktioniert
		EresseaRelationFactory ERF = ((EresseaRelationFactory) this.gd_Script.getGameSpecificStuff().getRelationFactory());
		boolean updaterStopped = ERF.isUpdaterStopped();
		if (!updaterStopped){
			ERF.stopUpdating();
		}
		ERF.processRegionNow(this.region());
		if (!updaterStopped){
			ERF.restartUpdating();
		}
		
		
	}
}
