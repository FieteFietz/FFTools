package com.fftools.pools.bau;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;

// import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.pools.ausbildung.AusbildungsPool;
import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Bauauftrag;
import com.fftools.scripts.Bauen;
import com.fftools.scripts.Burgenbau;
import com.fftools.scripts.Strassenbau;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

import magellan.library.CoordinateID;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.gamebinding.OrderChanger;
import magellan.library.utils.Direction;
import magellan.library.utils.Locales;


/**
 *Verwaltet Bauen-scripte eines TradeAreas
 *
 */
public class TradeAreaBauManager {
	// private static final OutTextClass outText = OutTextClass.getInstance();

	private TradeArea tradeArea = null;
	
	// merken wir uns zu jeder ScriptUnit doch einfach die bauScripte
	private Hashtable<ScriptUnit, ArrayList<Bauen>> bauScripte = null;
	
	// merken wir uns zu jeder ScriptUnit doch einfach die bauAuftr�ge
	private Hashtable<ScriptUnit, ArrayList<Bauauftrag>> bauAuftr�ge = null;

	// Die Infozeilen
	private ArrayList<String> infoLines = new ArrayList<String>();
	
	// Die scriptUnit, die den automatischen Burgenbau registriert hat
	private Burgenbau registerBurgenbau = null;
	
	// Die scriptUnit, die den automatischen Strassenbau registriert hat
	private Strassenbau registerStrassenbau = null;
	
	// falls mit Parameter NotIn=X,Y|X2,Y2 Regionen vom Strassenbau ausgeschlossen worden sind.
	private ArrayList<Region> keinStrassenbauRegionen = null;
	
	// Liste mit Bauscripten, die extra vom TA-BM informiert werde wollen (info=ja)
	private ArrayList<ScriptUnit> informationListeners = null;
	
	// wird f�r alle Bauen=auto �bernommen
	private CoordinateID centralHomeDest = null;
	
	// Eine Liste der Bauarbeiter mit mode=auto
	// wird am Anfang von run0 einmalig gebaut 
	private ArrayList<Bauen> autoBauer = null;
	
	// Eine Liste von Bauarbeitern, die eventuell unterst�tzt werden k�nnten
	private ArrayList<Bauen> supportableBuilder = null;
	
	// Ein lernplan, der von unbesch�ftigten Bauarbeitern genutzt wird
	private String Lernplanname = "";
	
	public String getLernplanname() {
		return Lernplanname;
	}

	public void setLernplanname(String lernplanname) {
		Lernplanname = lernplanname;
	}

	public CoordinateID getCentralHomeDest() {
		return centralHomeDest;
	}




	public void setCentralHomeDest(CoordinateID centralHomeDest) {
		this.centralHomeDest = centralHomeDest;
	}




	public TradeAreaBauManager (TradeArea _tradeArea){
		this.tradeArea=_tradeArea;
	}
	
	

	
	/**
	 * Zentrale Zuordnung der Bauauftr�ge zu automatischen Bauarbeitern
	 */
	public void run0(){
		
		// long StartTime=System.currentTimeMillis();
		// long ActTime=System.currentTimeMillis();
		// long DiffTime=0;
		// outText.addOutLine(DiffTime + "ms: start TA-BM", true);
		
		this.processBurgenbau();
		// ActTime=System.currentTimeMillis();
		// DiffTime=ActTime-StartTime;
		// outText.addOutLine(DiffTime + "ms: rdy with processBurgenbau", true);
		
		this.processStrassenbau();
		// ActTime=System.currentTimeMillis();
		// DiffTime=ActTime-StartTime;
		// outText.addOutLine(DiffTime + "ms: rdy with processStrassenbau", true);
		
		if (this.bauAuftr�ge==null || this.bauAuftr�ge.size()==0){
			this.infoLines.add("keine Bauauftr�ge im TA bekannt");
			return;
		}
		
		if (this.bauScripte==null || this.bauScripte.size()==0){
			this.infoLines.add("keine Bauarbeiter im TA bekannt");
			return;
		}
		// Liste der automatischen Bauarbeiter bauen
		this.autoBauer = new ArrayList<Bauen>();
		for (ScriptUnit su:this.bauScripte.keySet()){
			ArrayList<Bauen> actList = this.bauScripte.get(su);
			if (actList!=null && actList.size()>0){
				for (Bauen b:actList){
					if (b.isAutomode()){
						autoBauer.add(b);
					}
				}
			}
		}
		// ActTime=System.currentTimeMillis();
		// DiffTime=ActTime-StartTime;
		// outText.addOutLine(DiffTime + "ms: rdy with building builders", true);
		if (autoBauer.size()==0){
			this.infoLines.add("keine automatischen Bauarbeiter im TA bekannt");
			return;
		}
		
		// Anlegen der supportableBuilders
		this.supportableBuilder = new ArrayList<Bauen>();
		
		processCentralHomeDest(autoBauer);
		// ActTime=System.currentTimeMillis();
		// DiffTime=ActTime-StartTime;
		// outText.addOutLine(DiffTime + "ms: rdy with procCentralHomeDest", true);

		
		// erste Infozeilen
		this.infoLines.add("TA-Bau: " + this.bauAuftr�ge.size() + " Bauauftragshalter und " + autoBauer.size()+ " autom. Bauarbeiter");
		if (this.registerBurgenbau!=null){
			this.infoLines.add("TA-Bau: Bauauftr�ge definiert bei: " + this.registerBurgenbau.unitDesc());
		}
		if (this.registerStrassenbau!=null){
			this.infoLines.add("TA-Bau: Strassenauftr�ge definiert bei: " + this.registerStrassenbau.unitDesc());
		}
		// Liste der Auftr�ge
		this.infoLines.add("bekannte Auftr�ge:");
		ArrayList<Bauen> auftragsBauscripte = new ArrayList<Bauen>();
		for (ScriptUnit su:this.bauAuftr�ge.keySet()){
			ArrayList<Bauauftrag> actList = this.bauAuftr�ge.get(su);
			if (actList!=null && actList.size()>0){
				for (Bauauftrag b:actList){
					if (b.getBauScript()!=null){
						// this.infoLines.add(b.getBauScript().toString());
						auftragsBauscripte.add(b.getBauScript());
					} else {
						this.infoLines.add("Auftrag ohne Bauscript bei " + su.unitDesc());
					}
				}
			}
		}
		
		// ActTime=System.currentTimeMillis();
		// DiffTime=ActTime-StartTime;
		// outText.addOutLine(DiffTime + "ms: rdy with build Orders", true);
		
		// Sortieren
		Collections.sort(auftragsBauscripte, new BauScriptComparator());
		for (Bauen b:auftragsBauscripte){
			this.infoLines.add(b.toString() + " bei " + b.unitDesc());
		}
		
		
		
		
		// Liste der auto-Bauarbeiter
		this.infoLines.add("bekannte automatische Bauarbeiter:");
		for (Bauen b:autoBauer){
			this.infoLines.add(b.getUnitBauInfo());
		}
		boolean istGeeignet=true;
		// Abarbeiten
		ArrayList<Bauen> availableBauarbeiter = new ArrayList<Bauen>();
		for (Bauen b:auftragsBauscripte){
			if (!b.isFertig()){
				// welches Talent brauchen wir
				String actTalentName = "Burgenbau";
				if (b.getActTyp()==Bauen.STRASSE){
					actTalentName="Stra�enbau";
				}
				
				// mit welchem level
				int level_needed = 1;
				if (b.getActTyp()==Bauen.BURG){
					level_needed = FFToolsGameData.getCastleSizeBuildSkillLevel(b.getActSize());
				}
				if (b.getActTyp()==Bauen.BUILDING && b.getBuildingType()!=null){
					level_needed = b.getBuildingType().getBuildSkillLevel();
				}
				
				
				// noch verf�gbare Bauarbeiter zusammensuchen
				availableBauarbeiter.clear();
				for (Bauen arbeiter:autoBauer){
					if (!arbeiter.hasPlan() && arbeiter.scriptUnit.getSkillLevel(actTalentName)>=level_needed){
						istGeeignet=true;
						// Insektenpr�fung
						if (b.getUnit().getRegion().getRegionType().getName().equalsIgnoreCase("Gletscher") && arbeiter.scriptUnit.isInsekt()) {
							arbeiter.addComment("Nicht geeignet f�r Gletscher (Insekt)");
							istGeeignet=false;
						} else {
							arbeiter.addComment("Geeignet f�r Gletscher (kein Insekt)");
						}
						if (istGeeignet) {
							availableBauarbeiter.add(arbeiter);
						}
					}
				}
				if (availableBauarbeiter.size()>0) {
					// Sortieren mit Relevanz zu:
					// ZielRegion und ben�tigtem TP und Level und Skill
					BauauftragScriptComparator bc = new BauauftragScriptComparator(b.region(),level_needed,actTalentName,(b.getTargetSize()-b.getActSize()));
					Collections.sort(availableBauarbeiter, bc);
					// Zuordnen an den ersten besten
					for (Bauen arbeiter:availableBauarbeiter){
						boolean ok =BauZuordnung(b, arbeiter, actTalentName);
						if (ok){
							break;
						}
					}
					
				}
			}
		}
		
		// ActTime=System.currentTimeMillis();
		// DiffTime=ActTime-StartTime;
		// outText.addOutLine(DiffTime + "ms: rdy with TA-BM", true);

		
		Collections.reverse(this.infoLines);
		
	}
	
	
	/**
	 * Durchforstet die Region von b nach Bauarbeitern noch ohne Auftrag
	 * wenn der geeignet ist und helfen kann, wird 
	 * - Liefere organisiert
	 * - b.turnsToGo entsprechend reduziert
	 * - dem Bauarbeiter ein Plan aufgedr�ckt
	 * @param b
	 */
	private void checkForIddleSupporterInRegion(Bauen b){
		// mit welchem level
		int	level_needed = FFToolsGameData.getCastleSizeBuildSkillLevel(b.getActSize());
		if (b.getActTyp()==Bauen.BUILDING) {
			if (b.getBuildingType()!=null) {
				level_needed = b.getBuildingType().getBuildSkillLevel();
			}
		}
		// Abarbeiten
		ArrayList<Bauen> availableBauarbeiter = new ArrayList<Bauen>();
		String actTalentName = "Burgenbau";
		
		// noch verf�gbare Bauarbeiter zusammensuchen
		boolean istGeeignet=false;
		availableBauarbeiter.clear();
		for (Bauen arbeiter:autoBauer){
			if (!arbeiter.hasPlan() && arbeiter.scriptUnit.getSkillLevel(actTalentName)>=level_needed && arbeiter.region().equals(b.region())){
				// Insektenpr�fung
				if (b.getUnit().getRegion().getRegionType().getName().equalsIgnoreCase("Gletscher") && arbeiter.scriptUnit.isInsekt()) {
					// arbeiter.addComment("checkForIddleSupporterInRegion: Nicht geeignet f�r Gletscher (Insekt)");
					istGeeignet=false;
				} else {
					// arbeiter.addComment("checkForIddleSupporterInRegion: Geeignet f�r Gletscher (kein Insekt)");
				}
				if (istGeeignet) {
					availableBauarbeiter.add(arbeiter);
					// b.addComment("Debug: Suche nach Unterst�tzer in der Region: " + arbeiter.unitDesc() + " kommt in Frage.");
				}
			}
		}
		
		if (availableBauarbeiter.size()>0) {
			// Sortieren mit Relevanz zu:
			// ZielRegion und ben�tigtem TP und Level und Skill
			BauauftragScriptComparator bc = new BauauftragScriptComparator(b.region(),level_needed,actTalentName,(b.getTargetSize()-b.getActSize()));
			Collections.sort(availableBauarbeiter, bc);
			// Zuordnen an den ersten besten
			for (Bauen arbeiter:availableBauarbeiter){
				if (b.getTurnsToGo()<=0){
					break;
				}
				// ok...umsetzen
				b.setSupporter(arbeiter);
			}
			
		} else {
			b.addComment("TABM: leider keine unbesch�ftigten passenden Bauarbeiter in der Region, keine Hilfe von dieser Seite");
			
		}
		
	}
	
	
	/**
	 * Durchforstet die Region von b nach Bauarbeitern, wegen Ressourcenmangel warten m�ssen
	 * wenn der geeignet ist und helfen kann, wird 
	 * - Liefere organisiert
	 * - b.turnsToGo entsprechend reduziert
	 * - dem Bauarbeiter ein Plan aufgedr�ckt / dessen Plan ge�ndert
	 * @param b
	 */
	private void checkForWaitingSupporterInRegion(Bauen b){
		// mit welchem level
		int	level_needed = FFToolsGameData.getCastleSizeBuildSkillLevel(b.getActSize());
		if (b.getActTyp()==Bauen.BUILDING) {
			if (b.getBuildingType()!=null) {
				level_needed = b.getBuildingType().getBuildSkillLevel();
			}
		}
		// Abarbeiten
		ArrayList<Bauen> availableBauarbeiter = new ArrayList<Bauen>();
		String actTalentName = "Burgenbau";
		
		
		// noch verf�gbare Bauarbeiter zusammensuchen
		boolean istGeeignet=true;
		availableBauarbeiter.clear();
		for (Bauen arbeiter:autoBauer){
			if (arbeiter.hasPlan() && arbeiter.isOriginatedFromBauMAnger() && arbeiter.getBauBefehl()=="" && arbeiter.scriptUnit.getSkillLevel(actTalentName)>=level_needed && arbeiter.region().equals(b.region())){
				if (!(arbeiter.scriptUnit.equals(b.scriptUnit)) && !(arbeiter.isHasGotoOrder())){
					istGeeignet=true;
					// Insektenpr�fung
					if (b.getUnit().getRegion().getRegionType().getName().equalsIgnoreCase("Gletscher") && arbeiter.scriptUnit.isInsekt()) {
						// arbeiter.addComment("checkForWaitingSupporterInRegion: Nicht geeignet f�r Gletscher (Insekt)");
						istGeeignet=false;
					} else {
						// arbeiter.addComment("checkForWaitingSupporterInRegion: Geeignet f�r Gletscher (kein Insekt)");
					}
					if (istGeeignet) {
						availableBauarbeiter.add(arbeiter);
						// b.addComment("DEBUG:Bauunterst�tzersuche: verf�gbar " + arbeiter.unitDesc() + " mit Baubefehl: " + arbeiter.getBauBefehl() + "!? (" + b.scriptUnit.getMainDurchlauf() + ")");
					}
				}
			}
		}
		
		if (availableBauarbeiter.size()>0) {
			// Sortieren mit Relevanz zu:
			// ZielRegion und ben�tigtem TP und Level und Skill
			BauauftragScriptComparator bc = new BauauftragScriptComparator(b.region(),level_needed,actTalentName,(b.getTargetSize()-b.getActSize()));
			Collections.sort(availableBauarbeiter, bc);
			// Zuordnen an den ersten besten
			for (Bauen arbeiter:availableBauarbeiter){
				if (b.getTurnsToGo()<=1){
					b.addComment("Abbruch der Zuordnung in der Region: Anzahl Runden: " + b.getTurnsToGo() );
					break;
				}
				// ok...umsetzen
				b.setSupporter(arbeiter);
			}
			
		} else {
			b.addComment("TABM: leider keine wartenden passenden Bauarbeiter in der Region, keine Hilfe von dieser Seite");
		}
	}
	
	
	/**
	 * Durchforstet das TA nach Bauarbeitern ohne Plan, die innerhalb 
	 * der aktuellen Fertigstellungszeit b erreichen k�nnen und durch ihr
	 * mitwirken die Fertigstellungsdauer reduzieren k�nnen
	 * Reisezeit + 1 > turns to go
	 * wenn der geeignet ist und helfen kann, wird 
	 * - b.turnsToGo entsprechend reduziert
	 * - dem Bauarbeiter ein Plan aufgedr�ckt / dessen Plan ge�ndert -> GoTo
	 * @param b
	 */
	private void checkForIddleSupporterInTA(Bauen b){
		// mit welchem level
		int	level_needed = FFToolsGameData.getCastleSizeBuildSkillLevel(b.getActSize());
		if (b.getActTyp()==Bauen.BUILDING) {
			if (b.getBuildingType()!=null) {
				level_needed = b.getBuildingType().getBuildSkillLevel();
			}
		}
		// Abarbeiten
		ArrayList<Bauen> availableBauarbeiter = new ArrayList<Bauen>();
		String actTalentName = "Burgenbau";
		
		
		// noch verf�gbare Bauarbeiter zusammensuchen
		boolean istGeeignet=true;
		availableBauarbeiter.clear();
		for (Bauen arbeiter:autoBauer){
			if (!arbeiter.hasPlan() && arbeiter.scriptUnit.getSkillLevel(actTalentName)>=level_needed && !arbeiter.region().equals(b.region())){
				istGeeignet=true;
				// Insektenpr�fung
				if (b.getUnit().getRegion().getRegionType().getName().equalsIgnoreCase("Gletscher") && arbeiter.scriptUnit.isInsekt()) {
					// arbeiter.addComment("checkForIddleSupporterInTA: Nicht geeignet f�r Gletscher (Insekt)");
					istGeeignet=false;
				} else {
					// arbeiter.addComment("checkForIddleSupporterInTA: Geeignet f�r Gletscher (kein Insekt)");
				}
				if (istGeeignet) {
					availableBauarbeiter.add(arbeiter);
				}
			}
		}
		
		if (availableBauarbeiter.size()>0) {
			// Sortieren mit Relevanz zu:
			// ZielRegion und ben�tigtem TP und Level und Skill
			BauauftragScriptComparator bc = new BauauftragScriptComparator(b.region(),level_needed,actTalentName,(b.getTargetSize()-b.getActSize()));
			Collections.sort(availableBauarbeiter, bc);
			// Zuordnen an den ersten besten
			for (Bauen arbeiter:availableBauarbeiter){
				if (b.getTurnsToGo()<=1){
					break;
				}
				// ok...umsetzen
				b.setSupporterOnRoute(arbeiter);
			}
			
		} else {
			b.addComment("TABM: leider keine unbesch�ftigten passenden Bauarbeiter im TA, keine Hilfe von dieser Seite");
		}
	}
	
	
	/**
	 * Baumanager hat diesem Arbeiter keinen Auftrag erteilt
	 * @param arbeiter
	 */
	private void processWaitingArbeiter(Bauen arbeiter){
		arbeiter.autoLearn();
	}
	
	/**
	 * vollzieht die Zuordnung
	 * Ergebniss:
	 * 	- entweder Bauauftrag
	 *  - oder GoTo bzw Reiten lernen
	 * @param Auftrag
	 * @param Arbeiter
	 */
	private boolean BauZuordnung(Bauen Auftrag,Bauen Arbeiter, String actTalentName){
		
		// sind wir in der gleichen Region?
		if (Auftrag.region().equals(Arbeiter.region())){
			// wir sind in der gleichen Region!
			// Dem Arbeiter das Bauscript �berhelfen
			// ohne mode=auto...
			// mit enstprechendem Lerntalent
			Bauen newBauscript = Auftrag.clone();
			newBauscript.setScriptUnit(Arbeiter.scriptUnit);
			newBauscript.setPlaningMode(false);
			newBauscript.setOriginatedFromBauMAnger(true);
			
			Arbeiter.addComment("�bernommener Auftrag: " + newBauscript.toString());
			Arbeiter.addComment("�bernommener Auftrag von: " + Auftrag.unitDesc());
			this.addBauScript(newBauscript);
			Arbeiter.scriptUnit.addAScriptNow(newBauscript);
			Arbeiter.setAutomode_hasPlan(true);
			Auftrag.addComment("Bauen: " + Arbeiter.unitDesc() + " �bernimmt: " + Auftrag.toString());
			
			
			this.supportableBuilder.add(newBauscript);
			newBauscript.addComment("DEBUG: Bauscript auf Liste supportableBuilder");
			
			return true;
			
		} else {
			// wir m�ssen erst hin.
			// k�nnen wir reiten?
			// debug
			int minReitLevel=Bauen.minReitLevel;
			if (Arbeiter.scriptUnit.getSkillLevel("Reiten")>minReitLevel){
				// ja, hinreiten und pferde requesten
				GotoInfo gotoInfo = FFToolsRegions.makeOrderNACH(Arbeiter.scriptUnit, Arbeiter.region().getCoordinate(), Auftrag.region().getCoordinate(), true,"TA-BM:BauZuordnung");
				Arbeiter.addComment("dieser Region NEU als Bauarbeiter zugeordnet: " +  Auftrag.region().toString());
				Arbeiter.addComment("Auftrag: " + Auftrag.toString());
				Arbeiter.addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
				Arbeiter.setAutomode_hasPlan(true);
				Arbeiter.setHasGotoOrder(true);
				Arbeiter.setFinalStatusInfo("moving to work");
				Auftrag.addComment("Bauen: " + Arbeiter.unitDesc() + " �bernimmt: " + Auftrag.toString());
				// Pferde requesten...
				if (Arbeiter.scriptUnit.getSkillLevel("Reiten")>0){
					int PferdeAnzahl =  Arbeiter.getUnit().getModifiedPersons();
					
					if (Arbeiter.getUnit().getRace().getName().equalsIgnoreCase("Trolle")) {
						PferdeAnzahl = (int)Math.ceil(PferdeAnzahl * 1.1);
						Arbeiter.addComment("Troll-Reiter: Anzahl der Pferde neu berechnet, " + PferdeAnzahl + " Pferde angefordert.");
					} else {
						// Arbeiter.addComment("Keine trolle erkannt, sondern: " + Arbeiter.getUnit().getRace().getName());
					}
					
					
					
					MatPoolRequest MPR = new MatPoolRequest(Arbeiter,PferdeAnzahl, "Pferd", 21, "Bauarbeiter unterwegs" );
					Arbeiter.addMatPoolRequest(MPR);
				}
				
				Auftrag.transferPlanungsMPR();
				
				
				return true;
				
			} else {
				// nein, auf T1 Reiten lernen
				// mit Ausreichend info versehen
				Arbeiter.addComment("Als Bauarbeiter fast zugeordnet: " + Auftrag.toString() + " bei " + Auftrag.unitDesc());
				Arbeiter.addComment("aber da noch nicht reiten k�nnend, erstmal lernen");
				Arbeiter.scriptUnit.addOrder("Lernen Reiten", true, true);
				Arbeiter.setFinalStatusInfo("Mindestreitlevel");
				Arbeiter.setAutomode_hasPlan(true);
				Arbeiter.setHasGotoOrder(false);
				
				return false;
			}
		}
		
	}
	
	
	
	/**
	 * Checkt den Status der bauscripte
	 *
	 */
	
	public void run1(){
		
		
		// 20120121: noch nicht versorgte Bauarbeiter k�nnten dort helfen
		// wo: - genug Material da ist
		// - und sie die Bauzeit verringern k�nnen.
		// Bauprojekte durchgehen, die bereits genug Material haben...wissen wir das schon?
		// 20120331 - fangen wir vorsichtig mit den Burgenbauern an
		if (this.supportableBuilder!=null && this.supportableBuilder.size()>0){
			for (Bauen b:this.supportableBuilder){
				if (!b.isFertig() && b.getTurnsToGo()>0 && (b.getActTyp()==Bauen.BURG || b.getActTyp()==Bauen.BUILDING)){
					b.addComment("Pr�fe Bauarbeiter auf Unterst�tzer...");
					// noch nicht fertig
					// m�sste noch diverse (>1) runden arbeiten (hat also genug ressourcen)
					// ist BURGenbauer
					
					
					b.addComment("Suche arbeitslose Unterst�tzer in dieser Region...");
					// in einem ersten Schritt die Burgenbauer durchgehen, die in der 
					// gleichen Region sind und *keinen* Plan haben
					checkForIddleSupporterInRegion(b);
					
					if (b.getTurnsToGo()>1){
						b.addComment("Suche wartende Unterst�tzer in dieser Region...");
						checkForWaitingSupporterInRegion(b);
					}
					if (b.getTurnsToGo()>1){
						b.addComment("Suche arbeitslose Unterst�tzer in diesem TA...");
						checkForIddleSupporterInTA(b);
					}
					b.addComment("Suche abgeschlossen. Aktuell verbleibende Runden: " + (b.getTurnsToGo() + 1));
				} else {
					b.addComment("Bauarbeiter sucht keine Unterst�tzung (Typ=" + b.getActTyp() + ")");
					b.addComment("DEBUG: fertig: " + b.isFertig() + ", Turns: " + (b.getTurnsToGo() + 1) + ", Typ:" + b.getActTyp());
				}
				if (b.getCountSupporters()>0) {
					b.informTurnsToGo();
				}
				
			}
			
		}
		
		// was ist mit nicht versorgten Bauarbeitern?
		if (this.autoBauer!=null && this.autoBauer.size()>0){
			for (Bauen arbeiter:autoBauer){
				if (!arbeiter.hasPlan() ){
					processWaitingArbeiter(arbeiter);
				}
				if (arbeiter.hasMovingSupporters()){
					// arbeiter.informTurnsToGo();
				}
			}
		}
		
		
		
		
		this.informUnits(1);
		
		if (this.bauScripte==null){return;}
		
		BauScriptComparator bauC = new BauScriptComparator();
		
		for (Iterator<ScriptUnit> iter = this.bauScripte.keySet().iterator();iter.hasNext();){
			ScriptUnit actUnit = (ScriptUnit)iter.next();
			
		
			boolean allFertig = true;
			String lernTalent = "";
			boolean hasCommand = false;
			boolean isOnAutomode = false;
			Bauen actBauen = null;
			ArrayList<Bauen> actList = this.bauScripte.get(actUnit);
			if (actList!=null && actList.size()>0){
				// sortieren
				Collections.sort(actList,bauC);
				for (Iterator<Bauen> iter2 = actList.iterator();iter2.hasNext();){
					actBauen = (Bauen)iter2.next();
					if (actBauen.isAutomode()){isOnAutomode=true;}
					if (!actBauen.isFertig()){
						allFertig = false;
						if (actBauen.getLernTalent().length()>0){
							lernTalent=actBauen.getLernTalent();
						}
						if (actBauen.getBauBefehl().length()>0){
							// Baubefehl
							hasCommand = true;
							actBauen.addOrder(actBauen.getBauBefehl(), true);
							break;
						}
						if (actBauen.isHasGotoOrder()){
							hasCommand = true;
							break;
						}
						if (actBauen.isAutomode() && !actBauen.hasPlan()){
							// automode einheiten erhalten vom bei der Zuordnung den Lernbefehl
							hasCommand = true;
							break;
						}
					}
				}
			}
			if (allFertig && !isOnAutomode){
				// alle bauauftr�ge fertig
				actUnit.doNotConfirmOrders("Alle Bauauftr�ge erledigt");
			}
			
			if (!hasCommand){
				// soll Lernen
				if (lernTalent.length()>0 && actBauen!=null){
					// alles fein
					actBauen.addComment("Keine Baut�tigkeit. Einheit soll Lernen.");
					if (this.Lernplanname.length()>2){
						
						AusbildungsRelation AR = actBauen.getOverlord().getLernplanHandler().getAusbildungsrelation(actBauen.scriptUnit, Lernplanname);
						if (AR!=null){
							AR.informScriptUnit();
							AusbildungsPool ausbildungsPool = actBauen.getOverlord().getAusbildungsManager().getAusbildungsPool(actBauen.scriptUnit);
							ausbildungsPool.addAusbildungsRelation(AR);
							actBauen.setFinalStatusInfo("ABM: LERNEN (Lernplan)");
							if (AR.getActLernplanLevel()!=Lernplan.level_unset){
								// this.scriptUnit.ordersHaveChanged();
								// this.scriptUnit.setUnitOrders_adjusted(true);
							}
						} else {
							// keine AR -> Lernplan beendet ?!
							actBauen.scriptUnit.doNotConfirmOrders("Lernplan liefert keine Aufgabe mehr");
							// default erg�nzen - keine Ahnung, was, eventuell kan
							// die einheit ja nix..
							actBauen.lerneTalent(lernTalent, true);
							actBauen.setFinalStatusInfo("ABM: LERNEN (ohne Lernplan)");
						}
					} else {
						actBauen.lerneTalent(lernTalent,true);
						actBauen.setFinalStatusInfo("Lerne " + lernTalent);
					}
					
					
				} else {
					// kann nicht lernen
					actUnit.doNotConfirmOrders("!!!Bauen: Unit soll Lernen, kann aber nicht!");
				}
			}
		}
		
		this.informUnits(2);
		
		this.checkBauftragshalter();
		
	}
	
	
	public void addBauScript(Bauen bauen){
		if (this.bauScripte==null){
			this.bauScripte = new Hashtable<ScriptUnit, ArrayList<Bauen>>();
		}
		ArrayList<Bauen> actList = this.bauScripte.get(bauen.scriptUnit);
		if (actList==null){
			actList = new ArrayList<Bauen>();
		} 
		if (!actList.contains(bauen)){
			actList.add(bauen);
			this.bauScripte.put(bauen.scriptUnit,actList);
		}
	}
	
	
	public void addInformationListener(ScriptUnit su){
		if (this.informationListeners==null){
			this.informationListeners = new ArrayList<ScriptUnit>();
		}
		if (!this.informationListeners.contains(su)){
			this.informationListeners.add(su);
		}
	}
	
	
	public void addBauAuftrag(Bauauftrag bauAuftrag){
		if (this.bauAuftr�ge==null){
			this.bauAuftr�ge = new Hashtable<ScriptUnit, ArrayList<Bauauftrag>>();
		}
		ArrayList<Bauauftrag> actList = this.bauAuftr�ge.get(bauAuftrag.scriptUnit);
		if (actList==null){
			actList = new ArrayList<Bauauftrag>();
		} 
		if (!actList.contains(bauAuftrag)){
			actList.add(bauAuftrag);
			this.bauAuftr�ge.put(bauAuftrag.scriptUnit,actList);
		}
	}


	/**
	 * @return the tradeArea
	 */
	public TradeArea getTradeArea() {
		return tradeArea;
	}
	
	
	private void informUnits(int step){
		// alle Bauauftragshalter informieren
		if (this.bauAuftr�ge!=null && this.bauAuftr�ge.size()>0){
			for (ScriptUnit su:this.bauAuftr�ge.keySet()){
				if (step==1){
					report2Unit(su);
				}
				if (step==2){
					report2Unit2(su);
				}
			}
		}
		
		// Burgenbau
		if (this.registerBurgenbau!=null){
			if (step==1){
				report2Unit(this.registerBurgenbau.scriptUnit);
			}
			if (step==2){
				report2Unit2(this.registerBurgenbau.scriptUnit);
			}
		}
		
		// ToDo: autobauer benachrichtigen - nur die wollen (info=ja)
		if (this.informationListeners!=null && this.informationListeners.size()>0){
			for (ScriptUnit su:this.informationListeners){
				if (step==1){
					report2Unit(su);
				}
				if (step==2){
					report2Unit2(su);
				}
			}
		}
	}
	
	// infolines an Unit
	private void report2Unit(ScriptUnit su){
		if (this.infoLines.size()>0){
			
			for (String s:this.infoLines){
				su.addComment(s);
			}
		} else {
			su.addComment("TA-Baumanager hat nix zu berichten");
		}
	}
	
	// Berichtet �betr final Status der autobauer
	private void report2Unit2(ScriptUnit su){
		if (this.autoBauer==null || this.autoBauer.size()==0){
			return;
		}
		ArrayList<String> comments = new ArrayList<String>();
		comments.add("---Final autobauer info---");
		for (Bauen b:autoBauer){
			if (b.getFinalStatusInfo().length()>1){
				comments.add(b.unitDesc() + ":" + b.getFinalStatusInfo());
			}
		}
		for (ScriptUnit u:this.bauScripte.keySet()){
			ArrayList<Bauen>list = this.bauScripte.get(u);
			for (Bauen b:list){
				if (b.isOriginatedFromBauMAnger()){
					comments.add(b.unitDesc() + ":" + b.getFinalStatusInfo());
				}
			}
		}
		comments.add("---  ---");
		Collections.reverse(comments);
		for (String s:comments){
			su.addComment(s);
		}
	}
	
	
	/**
	 * registriert die scriptUnit als Initiator f�r den Burgenbau
	 * @param u
	 */
	public void addBurgenBau(Burgenbau u){
		if (this.registerBurgenbau==null){
			this.registerBurgenbau = u;
			u.addComment("Automatischer Burgenbau f�r dieses TA registriert: " + this.getTradeArea().getName());
		} else {
			u.addComment("!! Automatischer Burgenbau konnte nicht registriert werden. Ist bereits geschehen durch: " +this.registerBurgenbau.unitDesc());
		}
	}
	
	/**
	 * registriert die scriptUnit als Initiator f�r den Strassenbau
	 * @param u
	 */
	public void addStrassenBau(Strassenbau u){
		if (this.registerStrassenbau==null){
			this.registerStrassenbau = u;
			u.addComment("Automatischer Stra�enbau f�r dieses TA registriert: " + this.getTradeArea().getName());
		} else {
			u.addComment("!! Automatischer Stra�enbau konnte nicht registriert werden. Ist bereits geschehen durch: " +this.registerStrassenbau.unitDesc());
		}
	}
	
	
	
	
	/**
	 * erg�nzt Bauauftr�ge f�r den automatischen Burgenbau
	 */
	private void processBurgenbau(){
		// haben wir ne entsprechende registrierung?
		if (this.registerBurgenbau==null){
			return;
		}
		if (this.registerBurgenbau.getAnzahl()<=0){
			this.registerBurgenbau.addComment("Burgenbau: keine automatischen Bauauftr�ge erstellt (Anzahl = " + this.registerBurgenbau.getAnzahl() + ")");
			return;
		}
		
		ArrayList<Region> burgenRegionen = this.tradeArea.getBurgenbauRegionen();
		if (burgenRegionen.size()==0){
			this.registerBurgenbau.addComment("Burgenbau: keine automatischen Bauauftr�ge erstellt: keine Regionen bekannt");
			return;
		}
		int i=1;
		int actPrio=0;
		ArrayList<String> infos = new ArrayList<String>();
		for (Region r:burgenRegionen){
			actPrio	= this.registerBurgenbau.getPrio()-(i-1);
			TradeRegion TR = this.registerBurgenbau.getOverlord().getTradeAreaHandler().getTradeRegion(r);
			Unit u = TR.getDepot();
			if (u == null){
				// kein Depot
				infos.add("Burgenbau ("+actPrio+"): kein Depot in " + r.toString());
			} else {
				ScriptUnit su = this.registerBurgenbau.scriptUnit.getScriptMain().getScriptUnit(u);
				if (su==null){
					infos.add("Burgenbau ("+actPrio+"): kein Depot-Scriptunit in " + r.toString());
				} else {
					// erg�nzen
					Bauauftrag bA = new Bauauftrag();
					ArrayList<String> newArgs = new ArrayList<String>();
					newArgs.add("typ=Burg");
					newArgs.add("Prio=" + actPrio);
					newArgs.add("Ziel=" + FFToolsRegions.getNextCastleSize(r));
					bA.setScriptUnit(su);
					bA.setArguments(newArgs);
					bA.setGameData(this.registerBurgenbau.gd_Script);
					su.addAScriptNow(bA);
					bA.run1();
					su.addComment("Burgenbau: Auftrag hier hinzugef�gt");
					infos.add("Burgenbau ("+actPrio+"): auf " + FFToolsRegions.getNextCastleSize(r) + " in " + r.toString() + " bei " + su.unitDesc());
				}
			}
			
			i++;
			if (i>this.registerBurgenbau.getAnzahl()){
				break;
			}
		}
		if (infos.size()>0){
			Collections.reverse(infos);
			for (String s:infos){
				this.registerBurgenbau.addComment(s);
			}
		}
	}
	
	/**
	 * erg�nzt den automatischen Strassenbau
	 * Ablauf  - Priorit�tenliste
	 * Regionen ohne notwendige Geb�ude und mit funktionierenden notw Geb�uden, Sortiert nach Bauernzahl
	 * 	- dort *alle* Strassen in Richtung von Regionen ohne notwendige Geb�ude
	 * 	- sortiert nach Bauernzahl der Nachbarregion
	 *  - Reparatur vor Neubau
	 *  - eine Region immer zuerst komplett ausbauen
	 * 
	 * Geb�udebauftragung in den Regionen, wo diese ben�tigt werden
	 * 	- sortiert nach Summe der Bauern in den umliegenden Regionen
	 * 
	 */
	private void processStrassenbau() {
		// Regionen durchgehen, wo ohne Geb�udebau gebaut werden kann, hinzuf�gen
		if (this.registerStrassenbau==null) {
			return;
		}
		int actCounter=0;
		int actPrio = this.registerStrassenbau.getPrioStrassen();
		if (this.registerStrassenbau.getAnzahlStrassen()>0) {
			
			ArrayList<Region> buildableRegions = new ArrayList<Region>();
			for (TradeRegion TR : this.tradeArea.getTradeRegions()) {
				Region r = TR.getRegion();
				if (this.keinStrassenbauRegionen==null || !this.keinStrassenbauRegionen.contains(r)) {
					if (FFToolsRegions.StreetsCanBeBuild(r)) {
						// gibt es richtungen, in die noch strassen fehlen?
						LinkedHashMap<Direction,Integer> listDir = FFToolsRegions.missingStreets(r,this.keinStrassenbauRegionen);
						if (!listDir.isEmpty()) {
							// dort ist was zu bauen, also erstmal in die neue Map
							buildableRegions.add(r);
						}
					}
				}
			}
			if (!buildableRegions.isEmpty()) {
				// sortieren
				Collections.sort(buildableRegions, new Comparator<Region>() {
	        		public int compare(Region r1,Region r2) {
	        			return (r2.getModifiedPeasants() - r1.getModifiedPeasants());
	        		}} 
				);
				
				
				
				// debugAusgabe
				this.registerStrassenbau.addComment("Liste der Stra�enbau-Regionen und ToDos:");
				OrderChanger changer = this.registerStrassenbau.gd_Script.getGameSpecificStuff().getOrderChanger();
				for (Region r:buildableRegions) {
					this.registerStrassenbau.addComment(r.toString() + " (" + r.getModifiedPeasants() + " Bauern):");
					LinkedHashMap<Direction,Integer> listDir = FFToolsRegions.missingStreets(r, this.keinStrassenbauRegionen);
					if (!listDir.isEmpty()) {
						for (Direction d: listDir.keySet()) {
							String dirS = changer.getOrderO(Locales.getOrderLocale(), d.getId()).getText();
							this.registerStrassenbau.addComment(" - Richtung " + dirS + ": " + listDir.get(d).toString() + " Steine", false);
							if (actCounter<this.registerStrassenbau.getAnzahlStrassen()) {
								actCounter++;
								this.registerStrassenbau.addComment(" - (als Bauauftrag (Stra�e) " + actCounter + " mit Prio " + actPrio + " beauftragt.)");
								
								TradeRegion TR = this.tradeArea.getTradeRegion(r);
								if (TR==null) {
									this.registerStrassenbau.doNotConfirmOrders("Stra�enbau ("+actPrio+"): f�r " + r.toString() + " kein TA gefunden - bl�der Fehler!");
								} else {
									Unit u = TR.getDepot();
									if (u == null){
										// kein Depot
										this.registerStrassenbau.doNotConfirmOrders("Stra�enbau ("+actPrio+"): kein Depot in " + r.toString());
									} else {
										ScriptUnit su = this.registerStrassenbau.scriptUnit.getScriptMain().getScriptUnit(u);
										if (su==null){
											this.registerStrassenbau.addComment("Stra�enbau ("+actPrio+"): kein Depot-Scriptunit in " + r.toString());
										} else {
											// erg�nzen
											Bauauftrag bA = new Bauauftrag();
											ArrayList<String> newArgs = new ArrayList<String>();
											newArgs.add("typ=Strasse");
											newArgs.add("Prio=" + actPrio);
											newArgs.add("Ziel=" + dirS);
											bA.setScriptUnit(su);
											bA.setArguments(newArgs);
											bA.setGameData(this.registerStrassenbau.gd_Script);
											su.addAScriptNow(bA);
											bA.run1();
											su.addComment("Stra�enbau: Auftrag hier hinzugef�gt nach " + dirS + ", Stra�enbau definiert bei " + this.registerStrassenbau.getUnit().toString());
											this.registerStrassenbau.addComment(" - Bauauftrag erg�nzt bei " + su.toString());
										}
									}
								}
								actPrio--;
							}
						}
					} else {
						this.registerStrassenbau.addComment("!! Problem: keine Stra�en in Region ben�tigt!!", false);
					}
					
				}
			} else {
				this.registerStrassenbau.addComment("Stra�enbau: keine Regionen mit Stra�enbauBedarf im TA erkannt.", false);
			}
		} else {
			this.registerStrassenbau.addComment("Stra�enbau: deaktiviert, weil Anzahl Stra�en = 0 gew�nscht");
		}
		
		int actCounterGeb�ude=0;
		actPrio = this.registerStrassenbau.getPrioGeb�ude();
		
		if (this.registerStrassenbau.getAnzahlGeb�ude()>0) {
			// Fehlende Geb�ude ermitteln
			HashMap<Region,String> buildableRegionsBuildings = new HashMap<Region,String>();
			for (TradeRegion TR : this.tradeArea.getTradeRegions()) {
				Region r = TR.getRegion();
				if (this.keinStrassenbauRegionen==null || !this.keinStrassenbauRegionen.contains(r)) {
					if (!FFToolsRegions.StreetsCanBeBuild(r)) {
						// mal schauen, was fehlt
						String regionTypeName = r.getRegionType().getName();
						String neededBuildingName = "";
						if (regionTypeName.equalsIgnoreCase("Gletscher")) {
							neededBuildingName="Tunnel";
						}
						if (regionTypeName.equalsIgnoreCase("Sumpf")) {
							neededBuildingName="Damm";
						}
						if (regionTypeName.equalsIgnoreCase("W�ste")) {
							neededBuildingName="Karawanserei";
						}
						if (neededBuildingName!="") {
							buildableRegionsBuildings.put(r, neededBuildingName);
						}
					}
				}
			}
			if (buildableRegionsBuildings.isEmpty()) {
				this.registerStrassenbau.addComment("Keine Regionen mit fehlenden Geb�uden f�r den Stra�enbau bekannt.");
			} else {
				// Sortieren
				ArrayList<Region> rS = new ArrayList<Region>();
				rS.addAll(buildableRegionsBuildings.keySet());
				Collections.sort(rS, new Comparator<Region>() {
	        		public int compare(Region r1,Region r2) {
	        			return (r2.getModifiedPeasants() - r1.getModifiedPeasants());
	        		}} 
				);
				// Ausgabe
				this.registerStrassenbau.addComment("fehlende Geb�ude: ");
				for (Region actRegion : rS) {
					this.registerStrassenbau.addComment("- in " + actRegion.toString() + " (" + actRegion.getModifiedPeasants() + " Bauern) fehlt: " + buildableRegionsBuildings.get(actRegion));
					if (actCounterGeb�ude<this.registerStrassenbau.getAnzahlGeb�ude()) {
						actCounterGeb�ude++;
						this.registerStrassenbau.addComment(" - (als Bauauftrag(Geb�ude) " + actCounterGeb�ude + " mit Prio " + actPrio + " beauftragt.)");
						
						
						TradeRegion TR = this.tradeArea.getTradeRegion(actRegion);
						Unit u = TR.getDepot();
						if (u == null){
							// kein Depot
							this.registerStrassenbau.addComment("Stra�enbau ("+actPrio+"): kein Depot in " + actRegion.toString());
						} else {
							ScriptUnit su = this.registerStrassenbau.scriptUnit.getScriptMain().getScriptUnit(u);
							if (su==null){
								this.registerStrassenbau.addComment("Stra�enbau ("+actPrio+"): kein Depot-Scriptunit in " + actRegion.toString());
							} else {
								// erg�nzen
								Bauauftrag bA = new Bauauftrag();
								ArrayList<String> newArgs = new ArrayList<String>();
								newArgs.add("typ=" + buildableRegionsBuildings.get(actRegion));
								newArgs.add("Prio=" + actPrio);
								
								bA.setScriptUnit(su);
								bA.setArguments(newArgs);
								bA.setGameData(this.registerStrassenbau.gd_Script);
								su.addAScriptNow(bA);
								bA.run1();
								su.addComment("Stra�enbau: " + buildableRegionsBuildings.get(actRegion) + " beauftragt, Stra�enbau definiert bei " + this.registerStrassenbau.getUnit().toString());
								this.registerStrassenbau.addComment(" - Bauauftrag erg�nzt bei " + su.toString());
							}
						}
						actPrio--;
					}
				}
			}
		} else {
			this.registerStrassenbau.addComment("Stra�enbau-geb�ude: deaktiviert, weil Anzahl Geb�ude = 0 gew�nscht.");
		}
		if (actCounter==0 && actCounterGeb�ude==0 && !this.registerStrassenbau.isConfirmUnemployed()) {
			this.registerStrassenbau.doNotConfirmOrders("Keine Stra�en und/oder notwendige Geb�ude mehr zu errichten.");
		}
	}
	
	
	private void processCentralHomeDest(ArrayList<Bauen> autoBauer){
		if (this.centralHomeDest==null){
			return;
		}
		if (autoBauer!=null && autoBauer.size()>0){
			for (Bauen b:autoBauer){
				b.setHomeDest(this.centralHomeDest);
			}
		}
	}
	
	
	private void checkBauftragshalter() {
		if (this.bauAuftr�ge==null || this.bauAuftr�ge.size()==0){
			return;
		}
		for (ScriptUnit su:this.bauAuftr�ge.keySet()){
			ArrayList<Bauauftrag> list = this.bauAuftr�ge.get(su);
			if (list==null || list.size()==0){
				// komisch...bauauftragshalter ohne Auftr�ge
				su.doNotConfirmOrders("!!! Als Halter von Bauauftr�gen gelistet aber keine Gefunden!");
			} else {
				int count_all=0;
				int count_rdy=0;
				int count_toDo=0;
				for (Bauauftrag bA:list){
					count_all++;
					Bauen b = bA.getBauScript();
					if (b.isFertig()){
						count_rdy++;
					} else {
						count_toDo++;
					}
				}
				// info
				su.addComment("TA-Bau: " + count_rdy + " / " + count_all + " OK, ToDo: " + count_toDo);
				if (count_toDo==0){
					su.doNotConfirmOrders("TA-Bau: keine offenen Bauauftr�ge mehr!");
				}
			}
		}
	}
	
	/**
	 * Regionen f�r den Strassenbau ausschliessen
	 * @param r  auszuschliessende Region
	 */
	public void addKeinStrassenbau(Region r) {
		if (this.keinStrassenbauRegionen==null) {
			this.keinStrassenbauRegionen = new ArrayList<Region>();
		}
		if (!this.keinStrassenbauRegionen.contains(r)) {
			this.keinStrassenbauRegionen.add(r);
		}
	}
	
	
}
