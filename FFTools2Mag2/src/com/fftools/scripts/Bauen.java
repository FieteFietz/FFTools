package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import com.fftools.ScriptUnit;
import com.fftools.pools.ausbildung.AusbildungsPool;
import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.pools.bau.Supporter;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeArea;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

import magellan.library.Border;
import magellan.library.Building;
import magellan.library.CoordinateID;
import magellan.library.Item;
import magellan.library.Ship;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.UnitContainer;
import magellan.library.gamebinding.OrderChanger;
import magellan.library.rules.BuildingType;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;
import magellan.library.utils.Direction;
import magellan.library.utils.Locales;

/**
 * 
 * Dat Bauscript - für Bauarbeiter, und solche, die nur matreial für einen Bau anfordern sollen
 * @author Fiete
 *
 */

public class Bauen extends MatPoolScript implements Cloneable{
	
	
	// private static final ReportSettings reportSettings = ReportSettings.getInstance();
	private int Durchlauf_Baumanager = 56;
	private int Durchlauf_vorMatPool = 102;
	private int Durchlauf_nachMatPool = 440;   // und nach Gebäudeunterhalt bei 340!!
	private static final int Durchlauf_last = 855;
	
	private int[] runners = {Durchlauf_Baumanager,Durchlauf_vorMatPool,Durchlauf_nachMatPool,Durchlauf_last};
	
	private boolean parseOK = false;
	
	/**
	 * zum besseren Verständnis
	 */
	public static final int STRASSE=1;
	public static final int BURG=2;
	public static final int BUILDING=3;
	
	public static final int minReitLevel=-1;
	public static final int minDefaultBurgenbauTalent=3;  // Burgenbau
	public static final int minDefaultStrassenbauTalent=2; 
	
	private int pers_gewicht = -1;
	
	/**
	 * Typ dieses Scriptes
	 */
	private int actTyp = Bauen.BUILDING;
	
	/**
	 * wenn Buildung, dann hier der Type
	 */
	private BuildingType buildingType = null;
	
	/**
	 * wenn Building oder Burg optionale Zielnummer
	 */
	private String buildungNummer = "";
	
	/**
	 * wenn Building oder Burg optionale Zielnummer -> Building
	 */
	private Building selectedBuilding = null;
	
	
	
	/**
	 * aktuelle Grösse des (grössten) passenden Objektes in der Region
	 */
	private int actSize = 0;
	
	/**
	 * bei bedarf gwünschte Grösse des Buildings / der Burg
	 */
	private int targetSize = 0;
	
	/**
	 * bei Strasse, Richtung
	 */
	private Direction dir = null;
	private String dirLocal = "";
	
	/**
	 * Ort der MPRs, Items sind die rawMaterials für den Burgentyp
	 */
	private Hashtable<Item,MatPoolRequest> mprTable = null;
	
	/**
	 * wenn dieses Bauscript an der Reihe ist, dann mit diesem Befehl
	 */
	private String bauBefehl = "";
	
	/**
	 * wenn Typ grössenbegrenzt, mehrere Gebäude pro Region möglich, z.B. Akademie
	 * dann hier anzahl der gebäude pro Region
	 * paremeter: anzahl=1
	 */
	private int numberOfBuildings = 1;
	
	private String lernTalent = "Burgenbau";
	
	private int minBurgenbauTalent=Bauen.minDefaultBurgenbauTalent;
	private int minStassenbauTalent=Bauen.minDefaultStrassenbauTalent;
	private int spec = Bauen.BURG;
	private boolean isLearning=false;
	
	private String neededBuildingName = "";
	private Building preRequisteBuilding = null; 
	
	// folgende nur spannend bei Burgenbauer im Automode an einer Burg. 
	private int turnsToGo=0;
	private int remainingLevels=0; // Wieviel Baustufen verbleiben noch ?
	private int supportedLevels = 0; // Wieviele Level steuern supporter bei
	private int AnzahlLevelNachRessourcen = 0; // für wieviele level hätten wir den Material
	private int makingBurgLevelsNow = 0; // für wieviel geben wir uns den beim Burgenbau diese Runde den Befehl 
	
	public int getAnzahlLevelNachRessourcen() {
		return AnzahlLevelNachRessourcen;
	}


	private ArrayList<Supporter> supporters;  // Liste aller Bauarbeiter für dieses Projekt
	private boolean hasMovingSupporters = false;
	


	private String statusInfo = "";
	private String finalStatusInfo = "";
	
	// falls in PLanungsmodus...dann eventuell späterer zieleinheit überhelfen
	private ArrayList<MatPoolRequest> vorPlanungsMPR = null;
	
	private Unit preRequisiteBuildungOwner = null;
	
	public void setFinalStatusInfo(String finalStatusInfo) {
		this.finalStatusInfo += finalStatusInfo;
	}


	public String getFinalStatusInfo() {
		return finalStatusInfo;
	}


	/**
	 * wenn in planingMode, dann nur feststellen des Bedarfes
	 * kein MPRs etc
	 * genutzt von Bauauftrag
	 */
	private boolean planingMode=false;
	
	/**
	 * mode=auto
	 * Steuerung vom Baumanager
	 */
	private boolean automode=false;
	
	/**
	 * wird vom TA-Baumanager genutzt
	 */
	private boolean automode_hasPlan = false;
	
	/**
	 * wird vom TA-Baumanager genutzt
	 */
	private boolean hasGotoOrder = false;
	
	/**
	 * kann gesetzt werden als Heimatbasis
	 */
	private CoordinateID homeDest = null;
	
	/**
	 * 
	 * @param homeDest
	 */
	private boolean originatedFromBauMAnger = false;
	
	public void setHomeDest(CoordinateID homeDest) {
		this.addComment("Home Region zentral übernommen: " + homeDest.toString());
		this.homeDest = homeDest;
	}


	public boolean isHasGotoOrder() {
		return hasGotoOrder;
	}


	public void setHasGotoOrder(boolean hasGotoOrder) {
		this.hasGotoOrder = hasGotoOrder;
	}
	

	/**
	 * nix mehr zu tun!
	 */
	private boolean fertig=false;
	
	private int prioSilber = 650;
	private int prioSteine = 500;
	private int prioEisen = 650;
	private int prioHolz = 650;
	
	private String steinSpec = "";
	
	private MatPoolRequest steinRequest = null;
	
	private int minAuslastung = 75;
	
	public int getMinAuslastung() {
		return minAuslastung;
	}


	private String LernfixOrder = "Talent=Burgenbau";
	
	
	
	
	// Konstruktor
	public Bauen() {
		super.setRunAt(this.runners);
	}

	
	
public void runScript(int scriptDurchlauf){
		
	
		if (scriptDurchlauf==Durchlauf_Baumanager){
			this.BauManager();
		}
	
		if (scriptDurchlauf==Durchlauf_vorMatPool){
			this.vorMatPool();
		}
        

		if (scriptDurchlauf==Durchlauf_nachMatPool){
			this.nachMatPool();
			// debug
			// this.addComment("DEBUG: toString=" + this.scriptUnit.getUnit().toString(false));
		}
		
		if (scriptDurchlauf==Durchlauf_last){
			this.scriptSchluss();
		}
		
		
	}
	
	/** lediglich registrierung und ob auto
	 * 
	 */
	private void BauManager(){
		super.addVersionInfo();
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Bauen");
		OP.addOptionList(this.getArguments());

		// automode
		if (OP.getOptionString("mode").equalsIgnoreCase("auto")){
			this.setAutomode(true);
		}
		
		// wants info
		if (OP.getOptionBoolean("info", false)){
			this.getBauManager().addInformationListener(this.scriptUnit);
		}
		
		// home
		String homeString=OP.getOptionString("home");
		if (homeString.length()>2){
			CoordinateID actDest = null;
			if (homeString.indexOf(',') > 0) {
				actDest = CoordinateID.parse(homeString,",");
			} else {
			// Keine Koordinaten, also Region in Koordinaten konvertieren
				actDest = FFToolsRegions.getRegionCoordFromName(this.gd_Script, homeString);
			}
			if (actDest!=null){
				this.homeDest=actDest;
			} else {
				this.doNotConfirmOrders("!!! HOME Angabe nicht erkannt!");
			}
		}
		
		// spec
		if (OP.getOptionString("spec").equalsIgnoreCase("Strassenbau") 
			|| OP.getOptionString("spec").equalsIgnoreCase("Strasse")
			|| OP.getOptionString("spec").equalsIgnoreCase("Straße")
			|| OP.getOptionString("spec").equalsIgnoreCase("Straßenbau")
			){
			this.spec=Bauen.STRASSE;
			this.lernTalent="Straßenbau";
			this.LernfixOrder="Talent=Straßenbau";
			this.addComment("debug: Baumanager-Setup hat spec=Straße erkannt");
		}
		
		// minTalent
		this.minBurgenbauTalent=OP.getOptionInt("minTalent",this.minBurgenbauTalent);
		this.minBurgenbauTalent=OP.getOptionInt("minBurgenbau",this.minBurgenbauTalent);
		this.minStassenbauTalent = OP.getOptionInt("minStrassenbau", this.minStassenbauTalent);
		
		if (this.spec==Bauen.STRASSE){
			if (this.scriptUnit.getSkillLevel("Strassenbau")<this.minStassenbauTalent){
				this.addComment("Bauen: minTalent Strassenbau nicht erreicht, ich sollte lernen.");
				this.lernTalent="Strassenbau";
				this.setFinalStatusInfo("Min Strassenbau. ");
				this.setAutomode_hasPlan(true);
				isLearning=true;
				this.LernfixOrder="Talent=Straßenbau";
				this.Lerne();
			} else {
				this.addComment("Bauen: minTalent Strassenbau ist erreicht, ich suche Arbeit.");
			}
		} else {
			if (this.scriptUnit.getSkillLevel("Burgenbau")<this.minBurgenbauTalent){
				this.addComment("Bauen: minTalent Burgenbau nicht erreicht, ich sollte lernen.");
				this.lernTalent="Burgenbau";
				this.setFinalStatusInfo("Min Burgenbau. ");
				this.setAutomode_hasPlan(true);
				isLearning=true;
				this.LernfixOrder="Talent=Burgenbau";
				this.Lerne();
			} else {
				this.addComment("Bauen: minTalent Burgenbau ist erreicht, ich suche Arbeit.");
			}
		}
		
		if (!this.isInPlaningMode() && !this.isLearning){
			
			// eintragen
			this.getBauManager().addBauScript(this);
			
		} else {
			this.addComment("Bauen: keine Eintragung beim Baumanager.");
		}
		
		this.pers_gewicht = OP.getOptionInt("pers_gewicht", this.pers_gewicht);
		
	}


	public void vorMatPool(){
		
		this.actTyp = Bauen.BUILDING;
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Typbestimmung
		if (OP.getOptionString("Typ").equalsIgnoreCase("Strasse") || OP.getOptionString("Type").equalsIgnoreCase("Strasse")
				|| OP.getOptionString("Typ").equalsIgnoreCase("Road")
				|| OP.getOptionString("Type").equalsIgnoreCase("Road")
				|| OP.getOptionString("Typ").equalsIgnoreCase("Straße")
				){
			this.actTyp = Bauen.STRASSE;
			this.lernTalent="Straßenbau";
			this.addComment("debug: vorMatpool-Setup hat spec=Straße erkannt");
		}
		
		if (OP.getOptionString("Typ").equalsIgnoreCase("Burg") || OP.getOptionString("Type").equalsIgnoreCase("Burg")
				|| OP.getOptionString("Typ").equalsIgnoreCase("Castle")
				|| OP.getOptionString("Type").equalsIgnoreCase("Castle")){
			this.actTyp = Bauen.BURG;
		}
		
		
		if (this.isAutomode()){
			return;
		}
		
		if (this.isLearning){
			return;
		}
		
		this.parseOK = false;

		if (this.actTyp == Bauen.BUILDING){
			// Typ muss GebäudeType enthalten
			String s = OP.getOptionString("Typ");
			if (s.length()<2){
				s = OP.getOptionString("Type");
			}
			this.buildingType = this.gd_Script.getRules().getBuildingType(s,false);
			if (this.buildingType==null){
				// Abbruch
				statusInfo+="Fehler: unbekanntes Gebäude: " + s;
				this.doNotConfirmOrders("Bauen: unbekanntes Gebäude: " + s);
				return;
			}
			
			// wenn keine maxGrösse angegeben im Type wird grösse erwartet
			if (this.buildingType.getMaxSize()>0){
				// Grösse ist vorgegeben
				this.targetSize = this.buildingType.getMaxSize();
			} else {
				// Grösse MUSS angegeben werden
				int i = OP.getOptionInt("ziel",0);
				if (i==0){
					i = OP.getOptionInt("size",0);
				}
				if (i>0) {
					// alles OK
					this.targetSize=i;
				} else {
					// nix ist OK
					statusInfo+="Fehler: bei " + this.buildingType.getName() + " MUSS eine Zielgrösse angegeben werden! (ziel=X)";
					this.doNotConfirmOrders("Bauen: bei " + this.buildingType.getName() + " MUSS eine Zielgrösse angegeben werden! (ziel=X)");
					return;
				}
			}
			
			if (this.buildingType.getBuildSkillLevel() > this.scriptUnit.getSkillLevel("Burgenbau") && !this.isInPlaningMode()) {
				statusInfo+="Fehler: zu untalentiert für Gebäude: " + s + ", verlangt Burgenbau " + this.buildingType.getBuildSkillLevel() + ", Einheit hat:" + this.scriptUnit.getSkillLevel("Burgenbau");
				this.doNotConfirmOrders("Fehler: zu untalentiert für Gebäude: " + s + ", verlangt Burgenbau " + this.buildingType.getBuildSkillLevel()  + ", Einheit hat:" + this.scriptUnit.getSkillLevel("Burgenbau"));
				this.lernTalent="Burgenbau";
				this.setFinalStatusInfo("Burgtalent Burgenbau. ");
				this.setAutomode_hasPlan(true);
				isLearning=true;
				this.LernfixOrder="Talent=Burgenbau";
				this.Lerne();
				return;
			}

			// Anzahl vomBuildings
			this.numberOfBuildings=OP.getOptionInt("anzahl", 1);
		}
		if (this.actTyp == Bauen.BURG){
			// Grösse MUSS angegeben sein.
			int i = OP.getOptionInt("ziel",0);
			if (i==0){
				i = OP.getOptionInt("size",0);
			}
			if (i>0) {
				// alles OK
				this.targetSize=i;
			} else {
				// nix ist OK
				statusInfo+="Fehler: zum Bauen einer Burg MUSS eine Zielgrösse angegeben werden! (ziel=X)";
				this.doNotConfirmOrders("Bauen: beim Burgenbau MUSS eine Zielgrösse angegeben werden! (ziel=X)");
				return;
			}
		}
		
		if (this.actTyp== Bauen.STRASSE){
			// Richtung MUSS angegeben sein.
			String s = OP.getOptionString("Richtung");
			if (s.length()<1){
				s = OP.getOptionString("Ziel");
			}
			try {
				// this.dir = Direction.toDirection(s);
				this.dir = FFToolsRegions.getDirectionFromString(s);
				// Richtung in Deutsch...
				OrderChanger changer = super.gd_Script.getGameSpecificStuff().getOrderChanger();
				this.dirLocal = changer.getOrderO(Locales.getOrderLocale(), this.dir.getId()).getText();
			} catch (IllegalArgumentException e){
				this.dir=null;
				statusInfo+="Fehler: Strassenrichtung nicht erkannt: " + s;
				this.doNotConfirmOrders("Bauen: Strassenrichtung nicht erkannt: " + s);
				return;
			}
			if (this.dir.getDirCode()==Direction.DIR_INVALID){
				this.dir=null;
				statusInfo+="Fehler: Strassenrichtung nicht erkannt: " + s;
				this.doNotConfirmOrders("Bauen: Strassenrichtung nicht erkannt: " + s);
				return;
			}
			
			// notwendiges Gebäude vorhanden ??
			String regionTypeName = this.getUnit().getRegion().getRegionType().getName();
			this.addComment("Prüfe auf notwendiges Gebäude im Region-Typ " + regionTypeName);
			neededBuildingName = "";
			preRequisteBuilding = null;
			Integer neededSize = 0;
			if (regionTypeName.equalsIgnoreCase("Gletscher")) {
				neededBuildingName="Tunnel";
				neededSize=100;
			}
			if (regionTypeName.equalsIgnoreCase("Sumpf")) {
				neededBuildingName="Damm";
				neededSize=50;
			}
			if (regionTypeName.equalsIgnoreCase("Wüste")) {
				neededBuildingName="Karawanserei";
				neededSize=10;
			}
			if (neededBuildingName=="") {
				this.addComment("Kein Gebäude notwendig");
			} else {
				this.addComment("Prüfe auf Gebäude " + neededBuildingName);
				boolean foundNeededBuilding=false;
				for (Iterator<Building> iter = this.scriptUnit.getUnit().getRegion().buildings().iterator();iter.hasNext();){
					Building actBuilding = (Building)iter.next();
					if (actBuilding.getBuildingType().getName().equalsIgnoreCase(neededBuildingName)){
						if (actBuilding.getSize()>=neededSize) {
							preRequisteBuilding = actBuilding;
							if (actBuilding.getModifiedOwnerUnit()!=null) {
								// Treffer
								foundNeededBuilding=true;
								this.addComment(neededBuildingName + " gefunden: " + actBuilding.toString());
								this.preRequisiteBuildungOwner = actBuilding.getModifiedOwnerUnit();
								break;
							} else {
								this.addComment(neededBuildingName + " gefunden, aber ohne Besitzer: " + actBuilding.toString());
							}
						} else {
							this.addComment(neededBuildingName + " gefunden, aber nicht voll ausgebaut: " + actBuilding.toString());
						}
					}
				}
				if (!foundNeededBuilding) {
					this.doNotConfirmOrders("Notwendiges Gebäude für Straßenbau nicht gefunden: " + neededBuildingName + " (script Bauen)");
				}
			}
		}
		
		// Prioritäten
		// prioAnpassungen..Defaults setzen
		this.prioAdaption();
		// komplette Prio wird gesetzt
		int i = OP.getOptionInt("prio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioEisen = i;
				this.prioHolz = i;
				this.prioSilber = i;
				this.prioSteine = i;
			} else {
				statusInfo+="Fehler: Prio nicht erkannt: " + i;
				this.doNotConfirmOrders("Bauen: Prio nicht erkannt: " + i);
			}
		} 
		// Silberprio
		i = OP.getOptionInt("silberprio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioSilber = i;
			} else {
				this.doNotConfirmOrders("Bauen: SilberPrio nicht erkannt: " + i);
			}
		}
		// Eisenprio
		i = OP.getOptionInt("eisenprio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioEisen = i;
			} else {
				this.doNotConfirmOrders("Bauen: EisenPrio nicht erkannt: " + i);
			}
		}
		// Holzprio
		i = OP.getOptionInt("holzprio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioHolz = i;
			} else {
				this.doNotConfirmOrders("Bauen: HolzPrio nicht erkannt: " + i);
			}
		}
		// Steinprio
		i = OP.getOptionInt("steinprio",-1);
		if (i!=-1){
			if (i>0 && i<10000){
				this.prioSteine = i;
			} else {
				this.doNotConfirmOrders("Bauen: SteinPrio nicht erkannt: " + i);
			}
		}
		
		// Transporterspecs für Steine
		this.steinSpec = OP.getOptionString("steinspec");
		
		// min Auslastung
		this.minAuslastung = OP.getOptionInt("minAuslastung",this.minAuslastung);
		
		// lernTalent
		// this.lernTalent = OP.getOptionString("Talent");
		if (OP.getOptionString("Talent").length()>2){
			this.lernTalent = OP.getOptionString("Talent");
		}
		
		
		
		// Burg + Building können eine Nummer mitbekommen haben....checken
		String s = OP.getOptionString("nummer");
		if (s.length()>0 && (this.actTyp==Bauen.BUILDING || this.actTyp==Bauen.BURG)){
			// OK, wir haben eine Nummer
			// schauen, ob wir da was finden
			boolean foundNummber = false;
			if (this.scriptUnit.getUnit().getRegion().buildings() != null){
				for (Iterator<Building> iter = this.scriptUnit.getUnit().getRegion().buildings().iterator();iter.hasNext();){
					Building actBuilding = (Building)iter.next();
					if (actBuilding.getID().toString().equalsIgnoreCase(s)){
						// Treffer
						this.buildungNummer = s;
						this.selectedBuilding = actBuilding;
						this.actSize = this.selectedBuilding.getSize();
						foundNummber = true;
						this.addComment("Bauen: " + s + " als Gebäudenummer gesetzt.");
					}
				}
			}
			if (!foundNummber){
				// problem
				statusInfo+="Fehler: " + s + " kann nicht gefunden werden.";
				this.doNotConfirmOrders("Bauen: " + s + " kann nicht gefunden werden.");
				return;
			}
		}
		
		if (!this.isInPlaningMode()){
			this.scriptUnit.findScriptClass("RequestInfo");
		}
		
		this.parseOK = true;
		
		// eigentlicher Ablauf nu getrennt
		switch (this.actTyp){
			case Bauen.BUILDING: this.vorMP_Building();break;
			case Bauen.BURG: this.vorMP_Burg();break;
			case Bauen.STRASSE: this.vorMP_Strasse();break;
		}
		
		if (this.isAutomode()){
			statusInfo+=";automode";
		}
		if (this.isInPlaningMode()){
			statusInfo+="(planing)";
		}
		
		
	}	
	
	/**
	 * Building suchen
	 */
	private void vorMP_Building(){
		int numberOfFinishedBuildings = 0;
		if (this.buildungNummer.length()<1){
			// noch keine Nummer bekannt
			int maxFoundSize=0;
			for (Iterator<Building> iter = this.scriptUnit.getUnit().getRegion().buildings().iterator();iter.hasNext();){
				Building actB = (Building)iter.next();
				if (actB.getBuildingType().equals(this.buildingType)){
					// Das oder ein zielobject
					if (actB.getSize()>maxFoundSize && this.numberOfBuildings==1){
						maxFoundSize = actB.getSize();
						this.buildungNummer = actB.getID().toString();
						this.actSize = actB.getSize();
					}
					if (this.numberOfBuildings>1){
						if (actB.getSize()==this.targetSize){
							numberOfFinishedBuildings++;
						}
						if (actB.getSize()>maxFoundSize && actB.getSize()<this.targetSize){
							maxFoundSize = actB.getSize();
							this.buildungNummer = actB.getID().toString();
							this.actSize = actB.getSize();
						}
					}
				}
			}
			if (this.numberOfBuildings>1 && numberOfFinishedBuildings>=this.numberOfBuildings){
				// nix mehr zu tun
				String s = "Bauen: Fertig " + this.numberOfBuildings + " " + this.buildingType.getName();
				this.addComment(s);
				statusInfo+=s;
				this.fertig=true;
				return;
			}
		}
		if (this.buildungNummer.length()<1){
			// Neubau
			String s = "Bauen: Neubau " + this.buildingType.getName();
			if (this.targetSize>0){
				s += "(" + this.targetSize + ")";
			}
			s += " geplant.";
			this.addComment(s);
			statusInfo+=s;
			this.actSize=0;
		} else {
			// checken der Grösse
			if (this.actSize<this.targetSize){
				String s = "Bauen: Weiterbau von " + (this.targetSize-this.actSize) + " Stufen an " + this.buildingType.getName() + "(" + this.buildungNummer + ") geplant.";
				statusInfo+=s;
				this.addComment(s);
				
			} else {
				// schon fertig
				// haben wir eventuell mehrere? Dann vielleicht doch neubau?
				String s = "Bauen: Fertig " + this.buildingType.getName() + "(" + this.buildungNummer + ")";
				this.addComment(s);
				statusInfo+=s;
				this.fertig=true;
				return;
			}
		}
		
		
		
		int anzahl = this.targetSize - this.actSize;
		int prio = 0;
		// Notwendigen Ressourcen anfragen und MPRs ablegen
		for (Iterator<Item> iter = this.buildingType.getRawMaterials().iterator();iter.hasNext();){
			Item actItem = (Item)iter.next();
			prio = 10; // Default
			// Prio anpassen
			if (actItem.getItemType().getName().equalsIgnoreCase("Stein")){prio=this.prioSteine;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Eisen")){prio=this.prioEisen;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Laen")){prio=this.prioEisen;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Holz")){prio=this.prioHolz;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Mallorn")){prio=this.prioHolz;}
			if (actItem.getItemType().getName().equalsIgnoreCase("Silber")){prio=this.prioSilber;}
			MatPoolRequest MPR = new MatPoolRequest(this,anzahl * actItem.getAmount(),actItem.getItemType().getName(),prio,"Bauen: " + this.buildingType.getName());
			// steinSpec
			if (actItem.getItemType().getName().equalsIgnoreCase("Stein") && this.steinSpec.length()>0){
				MPR.addSpec(this.steinSpec);
			}
			if (!this.isInPlaningMode()){
				// hier hinzufügen
				this.addMPR(actItem, MPR);
				// MatPool
				this.addMatPoolRequest(MPR);
			} else {
				this.addComment("Bauauftrag-Request: " + MPR.toString() + " (wird aktiv, wenn sich Bauarbeiter auf den Weg macht)");
				this.addPlanungsMPR(MPR);
			}
			
			if (this.numberOfBuildings>1){
				// allgemein noch fertigzustellen
				int todoAnzahl = this.numberOfBuildings - numberOfFinishedBuildings;
				// dat nächste gerade angefordert mit voller prio
				todoAnzahl-=1;
				if (todoAnzahl>0){
					for (int x = 1;x<=todoAnzahl;x++){
						MatPoolRequest MPR2 = new MatPoolRequest(MPR);
						MPR2.setPrio((int)((double)MPR2.getPrio()*(double)0.75));
						MPR2.setPrioTM((int)((double)MPR2.getPrioTM()*(double)0.75));
						MPR2.setKommentar(MPR2.getKommentar().concat(" - Vorplanung " + x));
						MPR2.setOriginalGefordert(this.targetSize * actItem.getAmount());
						if (!this.isInPlaningMode()){
							this.addMatPoolRequest(MPR2);
						} else {
							this.addComment("Bauauftrag-Request: " + MPR2.toString() + " (wird aktiv, wenn sich Bauarbeiter auf den Weg macht)");
							this.addPlanungsMPR(MPR2);
						}
					}
				}
				
			}
			
		}
	}
	
	private void vorMP_Strasse(){
		// actSize ermitteln - gibt es schon eine strasse in diese richtung?
		int actProz = 0;
		for (Iterator<Border> iter=this.scriptUnit.getUnit().getRegion().borders().iterator();iter.hasNext();){
			Border actBorder = (Border)iter.next();
			if (actBorder.getType().equalsIgnoreCase("Straße") && actBorder.getDirection()==this.dir.getDirCode()){
				actProz = actBorder.getBuildRatio();
			}
		}
		this.targetSize = this.scriptUnit.getUnit().getRegion().getRegionType().getRoadStones();
		if (actProz>0){
			// es gibt eine...fehlende Steine berechnen
			this.actSize = (int)Math.floor(((double)actProz/(double)100) * (double)this.targetSize);
		}
		
		if (this.actSize<this.targetSize){
			// Strasse noch zu machen
			String s = "Bauen: noch " + (this.targetSize - this.actSize) + " Steine für Strasse nach " + this.dirLocal + " einzubauen.";
			this.addComment(s);
			statusInfo+=s;
			this.steinRequest = new MatPoolRequest(this,(this.targetSize - this.actSize),"Stein",this.prioSteine,"Strassenbau " + this.dirLocal);
			if (this.steinSpec.length()>0){
				this.steinRequest.addSpec(this.steinSpec);
			}
			if (!this.isInPlaningMode()){
				this.addMatPoolRequest(this.steinRequest);
			} else {
				this.addComment("Bauauftrag-Request: " + this.steinRequest.toString() + " (wird aktiv, wenn sich Bauarbeiter auf den Weg macht)");
				this.addPlanungsMPR(this.steinRequest);
			}
		} else {
			// Strasse fertig
			String s = "Bauen: Strasse nach " + this.dirLocal + " fertig.";
			this.addComment(s);
			statusInfo+=s;
			this.fertig = true;
			return;
		}
	}
	
	private void vorMP_Burg(){
		if (this.buildungNummer.length()<1){
			// noch keine Nummer bekannt
			Building actB = FFToolsRegions.getBiggestCastle(this.region());
			if (actB!=null){
				this.buildungNummer = actB.getID().toString();
				this.actSize = actB.getSize();
				this.addComment("Bauen: grösste Burg der Region ermittelt: " + this.buildungNummer + " (" + this.actSize + ")");
			}
		}
		if (this.buildungNummer.length()<1){
			// Neubau
			String s = "Bauen: Neubau einer Burg";
			if (this.targetSize>0){
				s+="(" + this.getTargetSize() + ")";
			}
			s += " geplant.";
			this.addComment(s);
			statusInfo+=s;
			this.actSize=0;
		} else {
			// checken der Grösse
			if (this.actSize<this.targetSize){
				String s = "Bauen: Weiterbau von " + (this.targetSize-this.actSize) + " Stufen an Burg (" + this.buildungNummer + ") geplant.";
				this.addComment(s);
				statusInfo+=s;
			} else {
				// schon fertig
				String s = "Bauen: Fertig Burg (" + this.buildungNummer + ")";
				this.addComment(s);
				statusInfo+=s;
				this.fertig=true;
				return;
			}
		}
		
		
		if (FFToolsGameData.getCastleSizeBuildSkillLevel( this.actSize)>this.scriptUnit.getSkillLevel("Burgenbau") && !this.isInPlaningMode()) {
			this.doNotConfirmOrders("Bauen: es wird ein höheres Talent zum Weiterbau der Burg verlangt!");
			this.lernTalent="Burgenbau";
			this.setFinalStatusInfo("Burgtalent Burgenbau. ");
			this.setAutomode_hasPlan(true);
			isLearning=true;
			this.LernfixOrder="Talent=Burgenbau";
			this.Lerne();
			return;
		}
		
		
		int anzahl = this.targetSize - this.actSize;
		
		this.steinRequest = new MatPoolRequest(this,anzahl,"Stein",this.prioSteine,"Burgenbau");
		if (this.steinSpec.length()>0){
			this.steinRequest.addSpec(this.steinSpec);
		}
		
		// this.addMatPoolRequest(this.steinRequest);
		if (!this.isInPlaningMode()){
			this.addMatPoolRequest(this.steinRequest);
		} else {
			this.addComment("Bauauftrag-Request: " + this.steinRequest.toString() + " (wird aktiv, wenn sich Bauarbeiter auf den Weg macht)");
			this.addPlanungsMPR(this.steinRequest);
		}
		
	}
	
	/**
	 * Zweiter Lauf nach dem ersten(?) MatPool
	 */
	
	private void nachMatPool(){
		if (!this.parseOK){return;}
		if (this.fertig){return;}
		if (this.isInPlaningMode()){
			return;
		}
		if (this.isAutomode()){
			return;
		}
		if (this.isLearning){
			return;
		}
		// Behandlung gleich nach Typ
		switch (this.actTyp){
			case Bauen.BUILDING: this.nachMP_Building();break;
			case Bauen.BURG: this.nachMP_Burg();break;
			case Bauen.STRASSE: this.nachMP_Strasse();break;
		}
		
	}
	
	private void nachMP_Building(){
		// Step 1 was könnte ich maximal nach Ressourcen bauen?
		int anzRes = this.targetSize - this.actSize;
		for (Iterator<Item> iter = this.buildingType.getRawMaterials().iterator();iter.hasNext();){
			// in actItem die benötigte Anzahl pro Grössenpunkt
			Item actItem = (Item)iter.next();
			// im MPR das Ergebnis des MPs
			MatPoolRequest MPR = this.mprTable.get(actItem);
			int actAnz = (int)Math.floor((double)MPR.getBearbeitet()/(double)actItem.getAmount());
			if (actAnz<anzRes){anzRes=actAnz;}
		}
		this.addComment("Bauen: Ressourcen für " + anzRes + " Stufen bei " + this.buildingType.getName() + " verfügbar.");
		this.AnzahlLevelNachRessourcen = anzRes;
		
		
		// Step 2 was könnten wir maximal nach Talentstand der Einheit bauen?
		SkillType bauType = this.gd_Script.getRules().getSkillType("Burgenbau",false);
		int anzTalPoints = 0;
		if (bauType!=null){
			Skill bauSkill = this.scriptUnit.getUnit().getModifiedSkill(bauType);
			if (bauSkill!=null){
				anzTalPoints = bauSkill.getLevel() * this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		
		// Schaffenstrunk?
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,false)){
			anzTalPoints *= 2;
			scriptUnit.addComment("Trankeffekt berücksichtigt");
		}
		
		
		
		
		
		
		int anzTal=0;
		if (this.buildingType.getBuildSkillLevel()>0){
			anzTal = (int)Math.floor((double)anzTalPoints/(double)this.buildingType.getBuildSkillLevel());
		}
		this.addComment("Bauen: Einheit ist fähig für " + anzTal + " Stufen bei " + this.buildingType.getName());
		
		// Jetzt erst RdF!
		ItemType rdfType=this.gd_Script.getRules().getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = this.scriptUnit.getModifiedItem(rdfType);
			if (rdfItem!=null && rdfItem.getAmount()>0){
				
				// RDF vorhanden...
				// produktion pro mann ausrechnen
				int prodProMann = (int)Math.floor((double)anzTal/(double)this.scriptUnit.getUnit().getModifiedPersons());
				int oldanzTal = anzTal;
				for (int i = 1;i<=rdfItem.getAmount();i++){
					if (i<=this.scriptUnit.getUnit().getModifiedPersons()){
						anzTal -= prodProMann;
						anzTal += (prodProMann * 10);
					} else {
						// überzähliger ring
						this.addComment("Bauen: zu viele RdF!",false);
					}
				}
				this.addComment("Bauen: " + rdfItem.getAmount() + " RdF. Prod von " + oldanzTal + " auf " + anzTal + " erhöht.");
			} else  {
				this.addComment("Bauen: kein RdF erkannt.");
			}
		} else {
			this.addComment("Bauen: RdF ist noch völlig unbekannt.");
		}
		
		
		int actAnz = Math.min(anzTal,anzRes);
		
		// wird gebäude fertig?
		boolean complete = false;
		if (actAnz + this.actSize>=this.targetSize){
			complete=true;
			this.addComment("Bauen: " + this.buildingType.getName() + " wird diese Runde fertig!");
		}
		
		
		if (!complete && anzRes>anzTal){
			int anzRunden = (int)Math.ceil((double)anzRes / (double) anzTal) - 1;
			this.remainingLevels = (this.targetSize - this.getActSize()) - anzTal;
			this.addComment("Bei meiner aktuellen Arbeitsleistung kann ich noch " + anzRunden + " weitere Runden bauen. (Verbleibend: " + this.remainingLevels + " Stufen.");
			this.turnsToGo = anzRunden;
		}
		
		
		// wird Mindestauslastung eingehalten
		boolean okAusl = false;
		int Auslastung = (int)Math.floor(((double)actAnz/(double)anzTal) * 100);
		this.addComment("Bauen: Auslastung bei " + this.buildingType.getName() + ": " + Auslastung + "% (min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			okAusl=true;
		}
		
		// Entscheidung
		if (okAusl || complete){
			// bauen, wenn nicht schon anderes Bauscript vorhanden
			if (this.actSize>0){
				this.setBauBefehl("MACHEN " + actAnz + " BURG " + this.buildungNummer,"nach MP Building");
				
			} else {
				this.setBauBefehl("MACHEN " + actAnz + " " + this.buildingType.getName(),"nach MP Building");
				
			}
			this.setFinalStatusInfo("baut " + actAnz + " " + this.buildingType.getName());
			this.setAutomode_hasPlan(true);
			this.addComment("Debug: Baubefehl: " + this.bauBefehl + " (" + this.scriptUnit.getMainDurchlauf() + ")");
			this.makingBurgLevelsNow=actAnz;
			
			Supporter sup = new Supporter();
			sup.setETA(0);
			sup.setLevels(actAnz);
			sup.setBauen(this);
			this.addSupporter(sup);
		} else {
			// nicht bauen
			this.addComment("Bauen: " + this.buildingType.getName() + " wird diese Runde nicht weitergebaut.");
			this.setBauBefehl("","nachMP-Building");
			this.setFinalStatusInfo("wartet auf Gebäude.");
		}	
	}
	
	private void nachMP_Burg(){
		// Step 1 was könnte ich maximal nach Ressourcen bauen?
		int anzRes = this.steinRequest.getBearbeitet();
		this.addComment("Bauen: Einheit hat Ressourcen für " + anzRes + " Stufen bei der Burg");
		this.AnzahlLevelNachRessourcen = anzRes;
		
		
		int anzTal=calcAnzTalBurg(this.actSize);

		int actAnz = Math.min(anzTal,anzRes);
		
		// wird gebäude fertig?
		boolean complete = false;
		if (actAnz + this.actSize>=this.targetSize){
			complete=true;
			this.addComment("Burg wird fertiggestellt. Shaka!");
		}
		
		if (!complete && anzRes>anzTal){
			int anzRunden = (int)Math.ceil((double)anzRes / (double) anzTal) - 1;
			this.remainingLevels = (this.targetSize - this.getActSize()) - anzTal;
			this.addComment("Bei meiner aktuellen Arbeitsleistung kann ich noch " + anzRunden + " weitere Runden bauen. (Verbleibend: " + this.remainingLevels + " Stufen.");
			this.turnsToGo = anzRunden;
		}
		
		
		// wird Mindestauslastung eingehalten
		boolean okAusl = false;
		int Auslastung = (int)Math.floor(((double)actAnz/(double)anzTal) * 100);
		this.addComment("Bauen: Auslastung bei Burgenbau: " + Auslastung + "% (min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			okAusl=true;
		}
		
		// Entscheidung
		if (okAusl || complete){
			// bauen, wenn nicht schon anderes Bauscript vorhanden
			if (this.actSize>0){
				this.setBauBefehl("MACHEN " + actAnz + " BURG " + this.buildungNummer,"nach MP Burg");
			} else {
				this.setBauBefehl( "MACHEN " + actAnz + " BURG","nach MP Burg");
			}
			this.setFinalStatusInfo("baut " + actAnz + " Burg");
			this.makingBurgLevelsNow=actAnz;
			
			Supporter sup = new Supporter();
			sup.setETA(0);
			sup.setLevels(actAnz);
			sup.setBauen(this);
			this.addSupporter(sup);
		} else {
			// nicht bauen
			this.addComment("Bauen: Burg wird diese Runde nicht weitergebaut.");
			this.setFinalStatusInfo("wartet auf Ressourcen. ");
			this.setBauBefehl("","nach MP Burg");
		}	

	}
	
	


	public int getTurnsToGo() {
		return turnsToGo;
	}


	private void nachMP_Strasse(){
		
		if (this.isFertig()){
			return;
		}
		
		// Step 1 was könnte ich maximal nach Ressourcen bauen?
		int anzRes = this.steinRequest.getBearbeitet();
		
		
		// Step 2 was könnten wir maximal nach Talentstand der Einheit bauen?
		SkillType bauType = this.gd_Script.getRules().getSkillType("Strassenbau",false);
		int anzTalPoints = 0;
		if (bauType!=null){
			Skill bauSkill = this.scriptUnit.getUnit().getModifiedSkill(bauType);
			if (bauSkill!=null){
				anzTalPoints = bauSkill.getLevel() * this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		int anzTal=anzTalPoints;
		
		// Schaffenstrunk?
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,false)){
			anzTalPoints *= 2;
			scriptUnit.addComment("Trankeffekt berücksichtigt");
		}
		
		this.addComment("Bauen: Einheit ist fähig für " + anzTal + " Stufen bei der Strasse");
		
		// Jetzt erst RdF!
		ItemType rdfType=this.gd_Script.getRules().getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = this.scriptUnit.getModifiedItem(rdfType);
			if (rdfItem!=null && rdfItem.getAmount()>0){
				
				// RDF vorhanden...
				// produktion pro mann ausrechnen
				int prodProMann = (int)Math.floor((double)anzTal/(double)this.scriptUnit.getUnit().getModifiedPersons());
				int oldanzTal = anzTal;
				for (int i = 1;i<=rdfItem.getAmount();i++){
					if (i<=this.scriptUnit.getUnit().getModifiedPersons()){
						anzTal -= prodProMann;
						anzTal += (prodProMann * 10);
					} else {
						// überzähliger ring
						this.addComment("Bauen: zu viele RdF!",false);
					}
				}
				this.addComment("Bauen: " + rdfItem.getAmount() + " RdF. Prod von " + oldanzTal + " auf " + anzTal + " erhöht.");
			} else  {
				this.addComment("Bauen: kein RdF erkannt.");
			}
		} else {
			this.addComment("Bauen: RdF ist noch völlig unbekannt.");
		}
		
		
		int actAnz = Math.min(anzTal,anzRes);
		
		// wird Strasse fertig?
		boolean complete = false;
		if (actAnz + this.actSize>=this.targetSize){
			complete=true;
		}
		
		// wird Mindestauslastung eingehalten
		boolean okAusl = false;
		int Auslastung = (int)Math.floor(((double)actAnz/(double)anzTal) * 100);
		this.addComment("Bauen: Auslastung bei Strassenbau: " + Auslastung + "% (min:" + this.minAuslastung + "%)");
		if (Auslastung>=this.minAuslastung){
			okAusl=true;
		}
		
		
		// Entscheidung
		if (okAusl || complete){
			this.setBauBefehl("MACHEN STRASSE " + this.dirLocal,"nach MP strasse");
			this.setFinalStatusInfo("baut Strasse");
			checkUnterhaltGebäude();
		} else {
			// nicht bauen
			this.addComment("Bauen: Strasse (" + this.dirLocal + ") wird diese Runde nicht weitergebaut.");
			this.setBauBefehl("","nach MP Strasse");
			this.setFinalStatusInfo("wartet auf Strassenbau");
		}	
	}
	
	private void checkUnterhaltGebäude() {
		if (this.preRequisiteBuildungOwner!=null) {
			// ScriptUnit finden
			ScriptUnit su = this.getOverlord().getScriptMain().getScriptUnit(this.preRequisiteBuildungOwner);
			if (su!=null) {
				if (su.unterhaltProblem) {
					this.doNotConfirmOrders("!!! Bauen: Unterhalt des notwendigen Gebäudes nicht erfüllt! (Versorgung des Besitzers!");
				}
			}
		}
		
		if (neededBuildingName!="" && preRequisteBuilding!=null) {
			// wir haben ein Gebäude
			if (preRequisteBuilding.getOwnerUnit()==null) {
				// gebäude steht leer
				// wir (Jawolltroll, Julian) wollen betreten
				this.addComment("Notwendiges Gebäude (" + neededBuildingName + ") steht leer, es wird betreten: " + preRequisteBuilding.toString());
				this.addOrder("BETRETE BURG " + preRequisteBuilding.getID().toString() + " ;Bauen: Betrete notwendiges (leeres) Gebäude für den Straßenbau", true);
			} else {
				// gebäude ist nicht leer
				if (!FFToolsRegions.hasSupportedRegionSpecificBuilding(this.region(), neededBuildingName)) {
					this.doNotConfirmOrders("!!! Bauen: Unterhalt des notwendigen Gebäudes (" + neededBuildingName + ") nicht erfüllt! (Siehe Regionsmeldung");
				}
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
	
	private void prioAdaption(){
		if (this.actTyp==Bauen.STRASSE){
			this.prioSteine = 600;
		}
		if (this.actTyp == Bauen.BURG){
			if (this.targetSize==1){this.prioSteine=580;}
			if (this.targetSize==2){this.prioSteine=570;}
			if (this.targetSize>2){this.prioSteine=560;}
			if (this.targetSize>10){this.prioSteine=550;}
			if (this.targetSize>50){this.prioSteine=540;}
			if (this.targetSize>250){this.prioSteine=530;}
			if (this.targetSize>1250){this.prioSteine=520;}
			if (this.targetSize>6250){this.prioSteine=510;}
		}
		if (this.actTyp == Bauen.BUILDING){
			String s = this.buildingType.getName();
			if (s.equalsIgnoreCase("Leuchtturm")){this.prioSteine = 590;}
			if (s.equalsIgnoreCase("Magierturm")){this.prioSteine = 610;}
			if (s.equalsIgnoreCase("Akademie")){this.prioSteine = 620;}
			if (s.equalsIgnoreCase("Bergwerk")){this.prioSteine = 630;}
			if (s.equalsIgnoreCase("Schmiede")){this.prioSteine = 640;}
			if (s.equalsIgnoreCase("Sägewerk")){this.prioSteine = 650;}
		}
		
	}
	
	private void addMPR(Item actItem, MatPoolRequest MPR){
		if (this.mprTable==null){
			this.mprTable = new Hashtable<Item, MatPoolRequest>();
		}
		mprTable.put(actItem,MPR);
	}

	
	private void addPlanungsMPR(MatPoolRequest MPR){
		if (this.vorPlanungsMPR==null){
			this.vorPlanungsMPR = new ArrayList<MatPoolRequest>();
		}
		if (!this.vorPlanungsMPR.contains(MPR)){
			this.vorPlanungsMPR.add(MPR);
		}
	}

	/**
	 * @return the bauBefehl
	 */
	public String getBauBefehl() {
		return bauBefehl;
	}


	/**
	 * @return the fertig
	 */
	public boolean isFertig() {
		return fertig;
	}


	/**
	 * @return the lernTalent
	 */
	public String getLernTalent() {
		return lernTalent;
	}


	/**
	 * @return the actSize
	 */
	public int getActSize() {
		return actSize;
	}


	/**
	 * @return the targetSize
	 */
	public int getTargetSize() {
		return targetSize;
	}


	/**
	 * @return the prioSteine
	 */
	public int getPrioSteine() {
		return prioSteine;
	}


	/**
	 * @return the planingMode
	 */
	public boolean isInPlaningMode() {
		return planingMode;
	}


	/**
	 * @param planingMode the planingMode to set
	 */
	public void setPlaningMode(boolean planingMode) {
		this.planingMode = planingMode;
	}


	/**
	 * @return the automode
	 */
	public boolean isAutomode() {
		return automode;
	}


	/**
	 * @param automode the automode to set
	 */
	private void setAutomode(boolean automode) {
		this.automode = automode;
	}
	
	
	public String toString(){
		String erg = "";
		if (statusInfo==""){
			erg = "Bauen ohne besonderen Status bei " + this.unitDesc();
		} else {
			erg = statusInfo;
			if (this.getPrioSteine()>0){
				erg = "(Stein-Prio " + this.getPrioSteine() + ") " + erg;
			}
		}
		return erg;
	}
	
	
	public String getUnitBauInfo(){
		String erg =this.unitDesc();
		SkillType sT = this.gd_Script.getRules().getSkillType("Burgenbau",false);
		Skill s = this.scriptUnit.getUnit().getModifiedSkill(sT);
		if (s==null){
			erg += ",kein Burgenbau";
		} else {
			int tp = this.scriptUnit.getUnit().getModifiedPersons() * s.getLevel();
			erg += ", " + tp + " Burgenbau (" + this.scriptUnit.getUnit().getModifiedPersons() + "x" + s.getLevel()+")";
		}
		sT = this.gd_Script.getRules().getSkillType("Straßenbau",false);
		s = this.scriptUnit.getUnit().getModifiedSkill(sT);
		if (s==null){
			erg += ",kein Strassenbau";
		} else {
			int tp = this.scriptUnit.getUnit().getModifiedPersons() * s.getLevel();
			erg += ", " + tp + " Strassenbau (" + this.scriptUnit.getUnit().getModifiedPersons() + "x" + s.getLevel()+")";
		}

		return erg;
	}


	/**
	 * @return the automode_hasPlan
	 */
	public boolean hasPlan() {
		return automode_hasPlan;
	}


	/**
	 * @param automode_hasPlan the automode_hasPlan to set
	 */
	public void setAutomode_hasPlan(boolean automode_hasPlan) {
		this.automode_hasPlan = automode_hasPlan;
	}


	/**
	 * @return the actTyp
	 */
	public int getActTyp() {
		return actTyp;
	}


	/**
	 * @return the buildingType
	 */
	public BuildingType getBuildingType() {
		return buildingType;
	}
	
	
	public Bauen clone(){
		try {
			return (Bauen)super.clone();
		}
	      catch(CloneNotSupportedException e) {
	      }
	     return null;
	}
	
	
	/**
	 * wird aufgerufen, wenn in Automode und keinen Auftrag erhalten
	 */
	public void autoLearn(){
		this.addComment("Keine Aufträge vom Baumanager erhalten.");
		if (this.scriptUnit.getSkillLevel("Reiten")<Bauen.minReitLevel){
			this.addComment("Mindestreitlevel unterschritten. Lerne Reiten");
			// this.addOrder("LERNEN Reiten ;mindestReitlevel", true);
			this.lerneTalent("Reiten", false);
			this.finalStatusInfo="Mindestreitlevel";
			return;
		}
		
		// wenn home gesetzt, dann dorthin laufen
		if (this.homeDest!=null){
			// sind wir da?
			CoordinateID actPos = super.scriptUnit.getUnit().getRegion().getCoordinate();
			if (!actPos.equals(this.homeDest)){
				// wir sind noch nicht da
				// ja, hinreiten und pferde requesten
				GotoInfo gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(), this.homeDest, true,"autoLearn");
				if (gotoInfo != null) {
					this.addComment("unterwegs in die HOME-Region");
					this.addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
					// Pferde requesten...
					
					int persons = this.scriptUnit.getUnit().getModifiedPersons();
					int anz_pferde = persons;
					if (this.pers_gewicht>0){
						anz_pferde = (int)Math.ceil(((double)persons * (double)this.pers_gewicht)/20);
					}
					SkillType reitType = this.gd_Script.getRules().getSkillType("Reiten");
					Skill reitSkill = this.scriptUnit.getUnit().getModifiedSkill(reitType);
					// schauen wir mal, ob unser reittalent ausreicht...
					int maxPferde=0;
					if (reitSkill!=null && reitSkill.getLevel()>0) {
						maxPferde = persons * reitSkill.getLevel() * 2;
						if (maxPferde<anz_pferde) {
							this.addComment("Bauen: ich würde gerne " + anz_pferde + " Pferde mitführen, mein Können reicht aber nur für " + maxPferde + " ...");
							anz_pferde=maxPferde;
						}
						this.addComment("Bauen: Pferdewunsch antizipiert, " + anz_pferde + " Pferde angefordert.");
						MatPoolRequest MPR = new MatPoolRequest(this,anz_pferde, "Pferd", 21, "Bauarbeiter unterwegs" );
						this.addMatPoolRequest(MPR);
					} else {
						this.addComment("Bauarbeiter unterwegs aber ohne Reittalent -> keine Pferde...");
					}
					this.finalStatusInfo="going HOME";
					if (this.scriptUnit.getUnit().getModifiedBuilding()!=null) {
						this.addOrder("VERLASSE ; -> HomeRegion", true);
						this.scriptUnit.isLeavingBuilding=true;
						this.remove_unterhalt();
					}
				} else {
					this.doNotConfirmOrders("!!! ungültiger GOTO-Befehl?! ggf HOME-Region nicht im TradeArea?");
				}
				return;
			} else {
				this.addComment("bereits in dier HOME-Region");
			}
		}
		
		
		// this.addOrder("LERNEN " + this.lernTalent + " ;unbeschäftigt", true);
		String Lernplanname = "";
		TradeArea TA = this.getOverlord().getTradeAreaHandler().getTAinRange(this.region());
		if (TA!=null){
			Lernplanname = TA.getTradeAreaBauManager().getLernplanname();
		}
		
		if (Lernplanname.length()>2) {
			AusbildungsRelation AR = super.getOverlord().getLernplanHandler().getAusbildungsrelation(this.scriptUnit, Lernplanname);
			if (AR!=null){
				AR.informScriptUnit();
				AusbildungsPool ausbildungsPool = this.getOverlord().getAusbildungsManager().getAusbildungsPool(super.scriptUnit);
				ausbildungsPool.addAusbildungsRelation(AR);
				this.finalStatusInfo="ABM: LERNEN (Lernplan)";
				if (AR.getActLernplanLevel()!=Lernplan.level_unset){
					// this.scriptUnit.ordersHaveChanged();
					// this.scriptUnit.setUnitOrders_adjusted(true);
				}
			} else {
				// keine AR -> Lernplan beendet ?!
				this.scriptUnit.doNotConfirmOrders("Lernplan liefert keine Aufgabe mehr");
				// default ergänzen - keine Ahnung, was, eventuell kan
				// die einheit ja nix..
				this.lerneTalent(this.lernTalent, true);
				this.finalStatusInfo="ABM: LERNEN (ohne Lernplan)";
			}
			
		} else {
			this.lerneTalent(this.lernTalent, true);
			this.finalStatusInfo="ABM: LERNEN";
			this.addComment("debug: autoLearn bestimmt Lerntalent");
		}
		if (this.scriptUnit.getUnit().getModifiedBuilding()!=null) {
			this.addOrder("VERLASSE ; -> fertig mit der Arbeit", true);
			this.scriptUnit.isLeavingBuilding=true;
			this.remove_unterhalt();
			
		}
		
	}


	public boolean isOriginatedFromBauMAnger() {
		return originatedFromBauMAnger;
	}


	public void setOriginatedFromBauMAnger(boolean originatedFromBauMAnger) {
		this.originatedFromBauMAnger = originatedFromBauMAnger;
	}
	
	
	public void transferPlanungsMPR(){
		if (this.vorPlanungsMPR!=null && this.vorPlanungsMPR.size()>0){
			for (MatPoolRequest MPR:this.vorPlanungsMPR){
				this.addMatPoolRequest(MPR);
			}
		}
	}
	
	public int getRemainingLevels() {
		return remainingLevels;
	}
	public void setRemainingLevels(int remainingLevels) {
		this.remainingLevels = remainingLevels;
	}
	
	
	/**
	 * Ein anderer Bauarbeiter soll unseren Unterstützen
	 * Bisher: nur beim Bauen von Burgen!
	 * Der andere hat noch keinen Plan oder muss warten
	 * @param b
	 */
	public void setSupporter(Bauen b){
		this.addComment("Unterstützung beim Bauen gefunden: " + b.unitDesc());
		b.addComment("Wird als Unterstützer eingesetzt für: " + this.unitDesc());
		int otherAnzTal = b.calcAnzTalBurg(this.actSize);
		if (this.getActTyp()==Bauen.BUILDING && this.getBuildingType()!=null) {
			otherAnzTal = b.calcAnzahlTalent(this.getBuildingType().getBuildSkillLevel());
		}
		this.addComment("kann nach Talenten für " + otherAnzTal + " bauen.");
		
		int availableRessourceLevels = this.AnzahlLevelNachRessourcen - this.supportedLevels - this.makingBurgLevelsNow;
		this.addComment("Material noch verfügbar für " + availableRessourceLevels + " Stufen.");
		
		int possibleLevels = Math.min(availableRessourceLevels, otherAnzTal);
		
		int otherAuslastung = (int)Math.floor(((double)possibleLevels/(double)otherAnzTal) * 100);
		this.addComment("damit Auslastung des Unterstützers bei " + otherAuslastung + "%. (" + possibleLevels + " Stufen)");
		
		boolean wirdFertig=false;
		if (this.remainingLevels - possibleLevels<=0) {
			wirdFertig=true;
		}
		
		if (otherAuslastung<b.getMinAuslastung()) {
			if (wirdFertig) {
				this.addComment("da die Burg durch die Unterstützung fertig wird, wird die MindestAuslastung ignoriert. Auf geht's!");
			} else {
				this.addComment("damit unter Mindestauslastung, unterstützer wird ignoriert.");
				b.addComment("Unterstützung nicht möglich, unter mindestAuslastung");
				return;
			}
		}
		
		int actStufen = Math.min(this.getRemainingLevels(), otherAnzTal);
		actStufen = Math.min(actStufen, availableRessourceLevels);
		if (actStufen<=0){
			this.addComment("!!! -> Unterstützer soll 0 Stufen bauen ?! Wird ignoriert.");
			b.addComment("Unterstützung nicht möglich, keine gültiger Auftrag vergeben");
			return;
		}
		
		this.addComment("Unterstützer hilft diese Runde für " + actStufen + " Stufen");
		this.remainingLevels-=actStufen;
		this.supportedLevels += actStufen;
		this.addComment("Verbleibende Stufen diese Runde: " + this.remainingLevels);
		this.addComment("Verbleibende Stufen nach Material diese Runde: " + (this.AnzahlLevelNachRessourcen - this.supportedLevels - this.makingBurgLevelsNow));
		
		if (this.actTyp==Bauen.BURG) {
		
			// Liefere
			Liefere L = new Liefere();
			ArrayList<String> order = new ArrayList<String>(); 
			order.add("Ware=Stein");
			order.add("Menge=" + actStufen);
			String targetNummer = b.getUnit().toString(false).replace(" ","_");
			
			order.add("Ziel=" + targetNummer);
			L.setArguments(order);
			this.addComment("Liefere-Befehl hinzugefügt: " + order.toString());
			this.getOverlord().addOverlordInfo(L);
			L.setScriptUnit(this.scriptUnit);
			if (this.getOverlord().getScriptMain().client!=null){
				L.setClient(this.getOverlord().getScriptMain().client);
			}
			L.setGameData(this.getOverlord().getScriptMain().gd_ScriptMain);
			this.scriptUnit.addAScriptNow(L);
		}
		
		if (this.actTyp==Bauen.BUILDING && this.buildingType!=null) {
			// alle ressourcen durchgehen und die Lieferscripte anlegen
			for (Iterator<Item> iter = this.buildingType.getRawMaterials().iterator();iter.hasNext();){
				Item actItem = (Item)iter.next();
				
				// Liefere
				Liefere L = new Liefere();
				ArrayList<String> order = new ArrayList<String>(); 
				order.add("Ware=" + actItem.getItemType().getName());
				order.add("Menge=" + (actStufen * actItem.getAmount()));
				String targetNummer = b.getUnit().toString(false).replace(" ","_");
				
				order.add("Ziel=" + targetNummer);
				L.setArguments(order);
				this.addComment("Liefere-Befehl hinzugefügt: " + order.toString());
				this.getOverlord().addOverlordInfo(L);
				L.setScriptUnit(this.scriptUnit);
				if (this.getOverlord().getScriptMain().client!=null){
					L.setClient(this.getOverlord().getScriptMain().client);
				}
				L.setGameData(this.getOverlord().getScriptMain().gd_ScriptMain);
				this.scriptUnit.addAScriptNow(L);
			}
		}
		
		
		
		// Plan setzen
		b.setAutomode_hasPlan(true);
		if (this.actSize>0){
			b.setBauBefehl("MACHEN " + actStufen + " BURG " + this.buildungNummer,"set Supporter Bauen");
		} else {
			b.setBauBefehl("MACHEN " + actStufen + " BURG","set Supporter Bauen");
		}
		b.setFinalStatusInfo("baut " + actStufen + " Burg (Unterstützer)");
		
		Supporter sup = new Supporter();
		sup.setETA(0);
		sup.setLevels(actStufen);
		sup.setBauen(b);
		this.addSupporter(sup);
		
		
		// turns to go...
		int newSum = this.calcAnzTalBurg(this.actSize) + this.supportedLevels;
		if (this.actTyp==Bauen.BUILDING && this.buildingType!=null) {
			newSum = this.calcAnzahlTalent(this.buildingType.getBuildSkillLevel());
		}
		int newTurnsToGo = (int)Math.ceil((double)this.AnzahlLevelNachRessourcen / (double)newSum);
		this.addComment("Mit Unterstützer diese Runde ein Weiterbau von " + newSum + " Stufe an der Burg. (Baudauer: " + newTurnsToGo + " Runden.)",false);
		this.turnsToGo = newTurnsToGo;
		this.turnsToGo = this.anzRundenWithSupporters(this.AnzahlLevelNachRessourcen, false);
		
		String sInfo = "Genaue Neuberechnung ergibt: " + this.turnsToGo;
		if (this.turnsToGo==0) {
			sInfo += " (wird fertig) ";
			b.setFinalStatusInfo(" (wird fertig!)");
			this.setFinalStatusInfo(" (wird fertig!)");
		}
		this.addComment(sInfo, false);
		
		
		
		// debug: zum Wiederfinden
		
		// outText.addOutLine("++++ *** ++++ **** ++++Bauunterstützung bei " + this.unitDesc(), true);
		
	}
	
	/**
	 * Ein anderer Bauarbeiter soll unseren Unterstützen
	 * Bisher: nur beim Bauen von Burgen!
	 * Der andere hat noch keinen Plan und ist in einer anderen region -> herholen
	 * @param b
	 */
	public void setSupporterOnRoute(Bauen b){
		// wir müssen erst hin.
		// können wir reiten?
		// debug
		int minReitLevel=Bauen.minReitLevel;
		if (b.scriptUnit.getSkillLevel("Reiten")>minReitLevel){
			// ja, hinreiten und pferde requesten
			GotoInfo gotoInfo = FFToolsRegions.makeOrderNACH(b.scriptUnit, b.region().getCoordinate(), this.region().getCoordinate(), false,"setSupporterOnRoute");
			if (gotoInfo.getAnzRunden()>=(this.turnsToGo - 1)){
				b.addComment("Kann nicht helfen bei: " + this.toString() + " (zu weit weg), ETA:" + gotoInfo.getAnzRunden() + " Runden bei noch " + (this.turnsToGo + 1) + " weiteren Runden Bauzeit.");
				this.addComment(b.unitDesc() + " zu weit weg für Hilfe hier.  ETA:" + gotoInfo.getAnzRunden() + " Runden bei noch " + (this.turnsToGo + 1) + " weiteren Runden Bauzeit.");
			} else {
				gotoInfo = FFToolsRegions.makeOrderNACH(b.scriptUnit, b.region().getCoordinate(), this.region().getCoordinate(), true,"setSupporterOnRoute");
				b.addComment("dieser Region NEU als Bauunterstützer zugeordnet: " +  this.region().toString());
				b.addComment("Auftrag: " + this.toString());
				b.addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden bei noch " + (this.turnsToGo + 1) + " weiteren Runden Bauzeit.");
				b.setAutomode_hasPlan(true);
				b.setHasGotoOrder(true);
				b.setFinalStatusInfo("moving to work (supporter)");
				this.addComment("Bauen: " + b.unitDesc() + " unterstützt beim Bauen. ETA: " + gotoInfo.getAnzRunden() + " Runden bei noch " + (this.turnsToGo + 1) + " weiteren Runden Bauzeit.");
				// Pferde requesten...
				if (b.scriptUnit.getSkillLevel("Reiten")>0){
					int PferdeAnzahl =  b.scriptUnit.getUnit().getModifiedPersons();
					
					if (b.scriptUnit.getUnit().getRace().getName().equalsIgnoreCase("Trolle")) {
						PferdeAnzahl = (int)Math.ceil(PferdeAnzahl * 1.1);
						b.addComment("Troll-Reiter: Anzahl der Pferde neu berechnet, " + PferdeAnzahl + " Pferde angefordert.");
					} else {
						// b.addComment("Keine trolle erkannt, sondern: " + this.scriptUnit.getUnit().getRace().getName());
					}
					MatPoolRequest MPR = new MatPoolRequest(b,PferdeAnzahl, "Pferd", 21, "Bauunterstützer unterwegs" );
					b.addMatPoolRequest(MPR);
				}
				
				if (b.scriptUnit.getUnit().getModifiedBuilding()!=null) {
					b.addOrder("VERLASSE ; -> moving to work", true);
					b.remove_unterhalt();
					b.scriptUnit.isLeavingBuilding=true;
				}
				
				
				// turns to go irgendwie anpassen...
				int otherAnzTal = b.calcAnzTalBurg(this.actSize);
				if (this.actTyp==Bauen.BUILDING && this.buildingType!=null) {
					otherAnzTal = b.calcAnzahlTalent(this.buildingType.getBuildSkillLevel());
				}
				this.addComment("Unterstützer kann nach Talenten für " + otherAnzTal + " bauen.");
				
				Supporter sup = new Supporter();
				sup.setETA(gotoInfo.getAnzRunden());
				sup.setLevels(otherAnzTal);
				sup.setBauen(b);
				this.addSupporter(sup);
				this.hasMovingSupporters=true;
				
				this.turnsToGo = this.anzRundenWithSupporters(this.AnzahlLevelNachRessourcen, false);
				this.addComment("neue Bauzeit: " + (this.turnsToGo + 1) + " Runden");
				
				
				// debug: zum Wiederfinden
				
				// outText.addOutLine("++++ *** ++++ **** ++++Bauunterstützung bei " + this.unitDesc() + " (moving)", true);
			}
			
		} else {
			// nein, auf T1 Reiten lernen
			// mit Ausreichend info versehen
			b.addComment("Als Bauunterstützer fast zugeordnet: " + this.toString() + " bei " + this.unitDesc());
			b.addComment("aber da noch nicht reiten könnend, erstmal lernen");
			b.scriptUnit.addOrder("Lernen Reiten", true, true);
			b.setFinalStatusInfo("Mindestreitlevel");
			b.setAutomode_hasPlan(true);
			b.setHasGotoOrder(false);
			if (b.scriptUnit.getUnit().getModifiedBuilding()!=null) {
				b.addOrder("VERLASSE ; -> lernen", true);
				b.remove_unterhalt();
				b.scriptUnit.isLeavingBuilding=true;
			}
		}
	}
	
	
	
	public int calcAnzTalBurg(int castleSize){
		// Step 2 was könnten wir maximal nach Talentstand der Einheit bauen?
		return calcAnzahlTalent(FFToolsGameData.getCastleSizeBuildSkillLevel(castleSize));
	}
	
	
	public int calcAnzahlTalent(int buildingNeededLevel) {
		// Step 2 was könnten wir maximal nach Talentstand der Einheit bauen?
		SkillType bauType = this.gd_Script.getRules().getSkillType("Burgenbau",false);
		int anzTalPoints = 0;
		if (bauType!=null){
			Skill bauSkill = this.scriptUnit.getUnit().getModifiedSkill(bauType);
			if (bauSkill!=null){
				anzTalPoints = bauSkill.getLevel() * this.scriptUnit.getUnit().getModifiedPersons();
			}
		}
		
		
		// Schaffenstrunk?
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,false)){
			anzTalPoints *= 2;
			this.scriptUnit.addComment("Trankeffekt berücksichtigt");
		}
		
		
		
		int anzTal=0;
		
		anzTal = (int)Math.floor((double)anzTalPoints/(double)buildingNeededLevel);
		
		this.addComment("Bauen: Einheit ist fähig für " + anzTal + " Stufen bei der Burg");
		
		// Jetzt erst RdF!
		ItemType rdfType=this.gd_Script.getRules().getItemType("Ring der flinken Finger",false);
		if (rdfType!=null){
			Item rdfItem = this.scriptUnit.getModifiedItem(rdfType);
			if (rdfItem!=null && rdfItem.getAmount()>0){
				
				// RDF vorhanden...
				// produktion pro mann ausrechnen
				int prodProMann = (int)Math.floor((double)anzTal/(double)this.scriptUnit.getUnit().getModifiedPersons());
				int oldanzTal = anzTal;
				for (int i = 1;i<=rdfItem.getAmount();i++){
					if (i<=this.scriptUnit.getUnit().getModifiedPersons()){
						anzTal -= prodProMann;
						anzTal += (prodProMann * 10);
					} else {
						// überzähliger ring
						this.addComment("Bauen: zu viele RdF!",false);
					}
				}
				this.addComment("Bauen: " + rdfItem.getAmount() + " RdF. Prod von " + oldanzTal + " auf " + anzTal + " erhöht.");
			} else  {
				this.addComment("Bauen: kein RdF erkannt.");
			}
		} else {
			this.addComment("Bauen: RdF ist noch völlig unbekannt.");
		}
		
		return anzTal;
	}
	
	
	private void addSupporter(Supporter sup){
		if (this.supporters==null){
			this.supporters=new ArrayList<Supporter>();
		}
		if (!this.supporters.contains(sup)) {
			this.supporters.add(sup);
		}
	}
	
	public int getCountSupporters() {
		if (this.supporters!=null) {
			return this.supporters.size();
		}
		return 0;
	}
	
	
	public int anzRundenWithSupporters(int levels, boolean addComment){
		if (this.supporters==null || this.supporters.size()==0){
			return -1;
		}
		
		int runningSum=0;
		int lastRunningSum=-1;
		int anzRunden = 0;
		if (addComment){
			this.addComment("Berechnung der finalen Bauzeit für " + levels + " Level", false);
		}
		int remaining = levels;
		while (runningSum<levels){
			// alle Aufsummieren, die ETA >= anzRunden haben
			int runningSumTurn=0;
			for (Supporter sup:this.supporters){
				if (sup.getETA()<=anzRunden){
					runningSumTurn+=sup.getLevels();
				}
			}
			runningSum+=runningSumTurn;
			remaining = Math.max(levels-runningSum,0);
			if (addComment){
				this.addComment("Runde " + anzRunden + ": " + runningSumTurn + " level, Summe: " + runningSum + ", verbleibend: " + remaining + " Level", false);
			}
			if (runningSum<levels){
				anzRunden++;
			}
			
			if (runningSum==lastRunningSum){
				this.addComment("Abbruch der Berechnung wg Konstanz in Runde " + anzRunden, false);
				return -1;
			}
			lastRunningSum=runningSum;
		}
		if (addComment){
			if (anzRunden==0) {
				this.addComment("Gebäude wird nächste Runde fertig sein!");
			} else {
				this.addComment("Geplante Fertigstellung in " + (anzRunden + 1) + " Runden.", false);
			}
		}
		return anzRunden;
	}


	public boolean hasMovingSupporters() {
		return hasMovingSupporters;
	}
	
	public void informTurnsToGo(){
		this.anzRundenWithSupporters(this.AnzahlLevelNachRessourcen, true);
	}


	public void setBauBefehl(String bauBefehl, String OriginInfo) {
		this.bauBefehl = bauBefehl;
		// this.addComment("DEBUG neuer Baubefehl: " + bauBefehl + " (" + this.scriptUnit.getMainDurchlauf() + ", " + OriginInfo + ", " + this.isInPlaningMode() + ")");
	}
	private void Lerne() {
		this.scriptUnit.addComment("Lernfix wird initialisiert mit dem Parameter: " + this.LernfixOrder);
		Script L = new Lernfix();
		ArrayList<String> order = new ArrayList<String>();
		order.add(this.LernfixOrder);
		L.setArguments(order);
		L.setScriptUnit(this.scriptUnit);
		L.setGameData(this.scriptUnit.getScriptMain().gd_ScriptMain);
		if (this.scriptUnit.getScriptMain().client!=null){
			L.setClient(this.scriptUnit.getScriptMain().client);
		}
		this.scriptUnit.addAScript(L);
	}
	
	private void remove_unterhalt() {
		Object o = this.scriptUnit.getScript(Gebaeudeunterhalt.class);
		if (o!=null) {
			Gebaeudeunterhalt g = (Gebaeudeunterhalt)o;
			g.set_zero_unterhalt(" verlässt Gebäude (Bauen)");
		}
	}
	
	private void scriptSchluss(){
		// waren wir am anfang der runde in einem Gebäude?
		UnitContainer UC = this.scriptUnit.getUnit().getUnitContainer();
		if (UC!=null && this.hasGotoOrder){
			// sind am ende der runde noch andere im Container?
			if (UC.modifiedUnits().size()==0){
				// scheinbar nicht
				this.doNotConfirmOrders("!!! Gebäude verbleibt unbesetzt !!!");
			}
		}
	}
	
}
