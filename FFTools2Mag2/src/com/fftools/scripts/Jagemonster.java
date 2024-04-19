package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.pools.seeschlangen.MonsterJagdManager_MJM;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsUnits;

import magellan.library.Building;
import magellan.library.CoordinateID;
import magellan.library.ID;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.rules.SkillType;

public class Jagemonster extends TradeAreaScript{
	
	public static int role_Undef=0;
	public static int role_AttackFront=1;
	public static int role_AttackBack=2;
	public static int role_Support=3;
	
	private static final int Durchlauf_1 = 36;
	private static final int Durchlauf_2 = 38;
	private static final int Durchlauf_3 = 215;
	
	private int[] runners = {Durchlauf_1,Durchlauf_2,Durchlauf_3};
	
	
	/**
	 * kann gesetzt werden als Heimatbasis
	 */
	private CoordinateID homeDest = null;
	
	
	/**
	 * falls jetzt Bewegung ansteht - hier ist das Ziel
	 */
	private CoordinateID targetDest = null;
	
	/**
	 * wird im Lauf 3 gesetzt, wenn wir uns denn bewegen
	 */
	// private GotoInfo gotoInfo = null;
	
	/**
	 * Ziel des Jägers
	 */
	private Unit targetUnit = null;
	
	/**
	 * Beim Angriff zu gebender Lernbefehl
	 */
	private String AttackLernTalent = "Ausdauer";
	
	
	/**
	 * Beim Angriff zu gebender LernPlanBefehl
	 */
	private String AttackLernPlan = null;
	
	/*
	 * Rolle
	 */
	private int role=role_Undef;

	public int getRole() {
		return role;
	}
	
	private boolean Ready4AttackThisWeek = false;


	public boolean isReady4AttackThisWeek() {
		return Ready4AttackThisWeek;
	}
	
	public void setReady4AttackThisWeek(boolean set) {
		Ready4AttackThisWeek = set;
	}

   
	private boolean MJM_setAttack = false;
	
	 
	
	public boolean isMJM_setAttack() {
		return MJM_setAttack;
	}


	public void setMJM_setAttack(boolean mJM_setAttack) {
		MJM_setAttack = mJM_setAttack;
	}
	
	
	private boolean isTactican = false;
	
	public boolean isTactican() {
		return isTactican;
	}


	public void setTactican(boolean isTactican) {
		this.isTactican = isTactican;
	}
	
	// nutzt bei wahr den MJM_HC
	private boolean automode=false;
	
	// wahr, wenn HC Einheit fertig bearbeitet hat
	private boolean HC_ready = false;
	
	// wird vom HC benutzt um die MJ zu sortieren
	private int HC_weeks2target = 0;
	
	

	
	/*
	 * möchte volle MJM_Settings Informationen vom MJM haben
	 */
	private boolean info_MJM_Settings = false;
	
	public boolean wants_info_MJM_Settings() {
		return this.info_MJM_Settings;
	}
	

	/*
	 * möchte regionale MJM_Settings zu den Monstern vor Ort haben
	 */
	private boolean info_MJM_Region = false;
	
	public boolean wants_info_MJM_Region() {
		return this.info_MJM_Region;
	}
	
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Jagemonster() {
		super.setRunAt(runners);
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
	
	public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_1){
			this.scriptStart();
		}
		if (scriptDurchlauf==Durchlauf_2){
			this.afterDecision();
		}
		if (scriptDurchlauf==Durchlauf_3){
			this.orderMove();
		}
		
	}
	
	private void scriptStart(){
		/*
		 * bekommt eine Monster-UnitID als Target = Ziel
		 * wenn ziel sichtbar: 
		 * 	wenn beim zeil: 
		 * 		attackiert *alle* Monster (Warnung, wenn zu ungleich!!!)
		 * 		setzt FOLGE
		 * 		Lernt etwas harmloses oder definiertes
		 * 	wenn nicht beim Ziel
		 * 		bewegt sich zum Ziel
		 * wenn ziel nicht mehr sichtbar
		 * 	sind Monster noch in der aktuellen Region?
		 * 	JA:
		 * 		attackiert *alle* Monster (Warnung, wenn zu ungleich!!!)
		 * 		setzt FOLGE
		 * 		Lernt etwas harmloses oder definiertes
		 * 	NEIN:
		 * 		wenn in HOME Region
		 * 			gibt andere Befehle wieder frei
		 * 			parameterabhängig: betrete burg
		 * 		wenn nicht in HOME Region
		 * 			bewegt sich zurück zur HOME-Region
		 */
		
		this.addComment("Start JM: reitend GE=" + this.scriptUnit.getPayloadOnHorse());
		
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Jagemonster");
		OP.addOptionList(this.getArguments());
		
		// Informant
		this.info_MJM_Settings = OP.getOptionBoolean("info_MJM_settings", this.info_MJM_Settings);
		
		// RegionInfo  info_MJM_Region
		this.info_MJM_Region = OP.getOptionBoolean("info_MJM_Region", this.info_MJM_Region);
		
		// Falls eine info angefordert worden ist, dann beim MJM registrieren
		if (this.info_MJM_Settings || this.info_MJM_Region) {
			this.getOverlord().getMJM().addInformant(this);
			String erg = "JageMonster: als Informant beim MJM registriert";
			if (this.info_MJM_Settings) {
				erg += " (MJM_Settings)";
			}
			if (this.info_MJM_Region) {
				erg += " (MJM_Region)";
			}
			this.addComment(erg); 
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
				Region r = this.gd_Script.getRegion(this.homeDest);
				if (r!=null) {
					this.addComment("JageMonster - als HOME erkannt: " + r.toString());
				} else {
					this.doNotConfirmOrders("!!! HOME Region nicht im Datenbestand gefunden !!!");
					return;
				}
			} else {
				this.doNotConfirmOrders("!!! HOME Angabe nicht erkannt!");
				return;
			}
		}
		
		// target soll sich im TA aufhalten
		TradeArea TA = this.getOverlord().getTradeAreaHandler().getTAinRange(this.getUnit().getRegion());
		if (TA==null) {
			this.doNotConfirmOrders("!!! JageMonster - ich bin nicht in einem TradeArea! -> Unbestaetigt!!");
			return;
		}
		
		// mode= auto
		if (OP.getOptionString("mode").equalsIgnoreCase("auto")) {
			this.automode=true;
			// beim MJM HC anmelden - wird über das TA sortiert
			TA.getMJM_HC().addJäger(this); 
		}
		
		
		// target
		String targetUnitName = OP.getOptionString("ziel");
		if (targetUnitName.length()==0) {
			targetUnitName = OP.getOptionString("target");
		}
		if (targetUnitName.length()==0 && !this.automode){
			this.doNotConfirmOrders("!!! JageMonster - ziel fehlt! -> Unbestaetigt!!");
			return;
		}
		
		if (!this.automode) {
			for (TradeRegion TR:TA.getTradeRegions()) {
				Region actRegion = TR.getRegion();
				// this.addComment("debug: checking region " + actRegion.toString());
				for (Unit actU : actRegion.getUnits().values()) {
					String test = actU.getName();
					if (test==null){
						test = actU.getModifiedName();
					}
					ID test2 = actU.getID();
					if (test != null && test2 != null){
						if (test.equalsIgnoreCase(targetUnitName) || actU.toString(false).equalsIgnoreCase(targetUnitName)){
							this.targetUnit = actU;
							// this.addComment("Zielunit wurde im TA gefunden!");
						} 
					}
				}
			}
		}
		
		String setRolle = OP.getOptionString("Rolle");
		if (setRolle.length()==0) {
			setRolle = OP.getOptionString("role");
		}
		
		if (setRolle.equalsIgnoreCase("FRONT") || setRolle.equalsIgnoreCase("VORNE") || setRolle.equalsIgnoreCase("VORN")) {
			this.role = role_AttackFront;
		}
		if (setRolle.equalsIgnoreCase("BACK") || setRolle.equalsIgnoreCase("HINTEN")) {
			this.role = role_AttackBack;
		}
		if (setRolle.equalsIgnoreCase("SUPPORT")) {
			this.role = role_Support;
		}
		
		if (this.role==role_Undef) {
			this.doNotConfirmOrders("!!! Jagemonster: keine Rolle erkannt (FRONT|HINTEN|SUPPORT)");
			return;
		}
		
		
		String test = OP.getOptionString("AttackLernTalent");
		if (test.length()>2) {
			// Prüfen
			test = test.substring(0, 1).toUpperCase() + test.substring(1).toLowerCase();
			SkillType actSkillType = this.gd_Script.getRules().getSkillType(test);
			if (actSkillType!=null) {
				this.AttackLernTalent = test;
				this.addComment("JageMonster - beim Attackieren wird Lernfix Talent=" + this.AttackLernTalent + " befohlen.");
			} else {
				this.doNotConfirmOrders("!!! JageMonster - AttackLernTalent ist ungültig! -> Unbestaetigt!! (check war: " + test + ")");
				return;
			}
		}
		
		test = OP.getOptionString("AttackLernPlan");
		if (test.length()>2) {
			Lernplan L = super.getOverlord().getLernplanHandler().getLernplan(this.scriptUnit, test, false);
			if (L!=null) {
				this.AttackLernPlan = test;
				this.addComment("JageMonster - beim Attackieren wird Lernfix LernPlan=" + this.AttackLernPlan + " befohlen.");
			} else {
				this.doNotConfirmOrders("!!! JageMonster - AttackLernPlan ist unbekannt! -> Unbestaetigt!! (gesucht wurde " + test + ")");
				return;
			}
		}
		
		
		int cc = this.getUnit().getModifiedCombatStatus();
		if (this.role==role_AttackFront || this.role==role_AttackBack) {
			this.addComment("JageMonster - Ich werde angreifen!");
			if (cc==EresseaConstants.CS_FLEE || cc==EresseaConstants.CS_NOT) {
				this.doNotConfirmOrders("!!! JageMonster - Ich soll angreifen, habe dazu aber den falschen Kampfstatus!");
				return;
			}
			if (this.role==role_AttackFront) {
				if (cc==EresseaConstants.CS_REAR) {
					this.doNotConfirmOrders("!!! JageMonster - Ich soll FRONT angreifen, habe dazu aber den falschen Kampfstatus HINTEN !");
					return;
				}
			}
			if (this.role==role_AttackBack) {
				if (cc==EresseaConstants.CS_AGGRESSIVE || cc==EresseaConstants.CS_DEFENSIVE || cc==EresseaConstants.CS_FRONT) {
					this.doNotConfirmOrders("!!! JageMonster - Ich soll HINTEN angreifen, habe dazu aber den falschen Kampfstatus VORNE !");
					return;
				}
			}
			
		} else {
			this.addComment("JageMonster - Ich werde mich zurückhalten und nicht angreifen!");
			if (cc==EresseaConstants.CS_AGGRESSIVE || cc==EresseaConstants.CS_FRONT || cc==EresseaConstants.CS_REAR || cc==EresseaConstants.CS_DEFENSIVE) {
				this.doNotConfirmOrders("!!! JageMonster - Ich sollte nicht kämpfen müssen, habe dazu aber den falschen Kampfstatus!");
				return;
			}
		}
		
		
		this.isTactican = OP.getOptionBoolean("Taktiker", this.isTactican);
		this.isTactican = OP.getOptionBoolean("Taktik", this.isTactican);
		this.isTactican = OP.getOptionBoolean("tactican", this.isTactican);
		this.isTactican = OP.getOptionBoolean("tactic", this.isTactican);
		
		if (!this.automode) {
			/*
			 * *******************     T A S K   *****************************************
			 */
			// Feststellen, ob die Zieleinheit noch sichtbar ist - ist bereits erfolgt
			if (this.targetUnit==null) {
				// kein Ziel mehr vorhanden
				// sind wir in der Home-Region ?
				if (this.getUnit().getRegion().getCoordinate().equals(this.homeDest)) {
					// wir sind (wieder) in der Home Region
					this.addComment("JageMonster - Ziel im TA nicht auffindbar, ich wieder zu Hause, kein Auftrag mehr.");
					// Einheit erhält keinen langen Befehl und bleibt (vermutlich) unbestätigt
				} else {
					// wir müssen zur Home Region
					this.moveTo(this.homeDest, MonsterJagdManager_MJM.MAPLINE_MOVE_TAG);
				}
			} else {
				// Ziel ist noch vorhanden
				// sind wir in target Region ?
				if (this.getUnit().getRegion().equals(this.targetUnit.getRegion())) {
					// wir sind vor Ort
					// Check, ob Attackiere Sinnvoll, und ich kenne nur mich (diese Einheit), ich weiss nicht, wer sonst noch angreift...
					// melde die Bereitschaft zum Angriff, warte auf GO durch MJM
					// MJM prüft, ob Bedingungen erfüllt sind
					//	- genug Angreifer
					//  - kein BACK ohne FRONT
					this.Ready4AttackThisWeek=true;
					this.addComment("Melde dem MJM: bereit zum Angriff");
					this.getOverlord().getMJM().addJäger(this);
				} else {
					// wir müssen zum Ziel
					this.moveTo(this.targetUnit.getRegion().getCoordinate(), MonsterJagdManager_MJM.MAPLINE_ATTACK_TAG);
					this.getOverlord().getMJM().addTargetUnit(this.targetUnit);
				}
			}
		} else {
			this.addComment("JM: ich erwarte Befehle vom MJM_HC (HighCommand)");
		}
	}
	
	/**
	 * MJM hat ggf eine Entscheidung gefällt, angriff oder nicht
	 */
	private void afterDecision() {
		if (!this.Ready4AttackThisWeek) {
			// wir waren gar nicht kampfwillig
			return;
		}
		// Auf jeden Fall Lern-Befehl geben
		this.AttackLerne();
		if (MJM_setAttack) {
			// Angreifen - ATTACKIERE und FOLGE sind schon gesetzt, BEWACHE kommt von BEWACHE ?! Nope, wir bewachen auch
			if (!this.isTactican && this.getRole()!=Jagemonster.role_Support && this.getRole()!=Jagemonster.role_Undef) {
				this.addOrder("BEWACHEN ;Jagemonster - angreifende Einheiten bewachen.", true);
			}
		} else {
			// ok, wir wollten angreifen, MJM hat aber kein GO gegeben, wir sind wohl zu wenige
			// fürs erste: nicht bestätigen
			this.doNotConfirmOrders("angriffsbereit, aber vom MJM fehlt das OK!");
		}
	}
	
	
	
	
	/**
	 * setzt NACH-Befehle - soweit möglich
	 * @param dest
	 */
	public void moveTo(CoordinateID dest, String MapLineIdentifier) {
		this.addComment("JageMonster - befehle GOTO nach " + dest.toString());
		this.targetDest = dest;
		
		if (MapLineIdentifier.equalsIgnoreCase(MonsterJagdManager_MJM.MAPLINE_ATTACK_TAG)) {
			// rote Attack-Line
			FFToolsRegions.addMapLine(this.region(), dest, 255, 0, 0, 5, MonsterJagdManager_MJM.MAPLINE_ATTACK_TAG);
		}
		if (MapLineIdentifier.equalsIgnoreCase(MonsterJagdManager_MJM.MAPLINE_MOVE_TAG)) {
			// blaue Move-Line
			FFToolsRegions.addMapLine(this.region(), dest, 0, 0, 255, 5, MonsterJagdManager_MJM.MAPLINE_MOVE_TAG);
		}
	}
	
	/**
	 * setzt Goto um, *nachdem* wir Pferde haben....berechnet dadurch die ETA richtig(er)
	 */
	private void orderMove() {
		if (this.targetDest!=null) {
			FFToolsRegions.makeOrderNACH(this.scriptUnit, super.region().getCoordinate(), this.targetDest,true,"JageMonster", false);
		}
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
	/**
	 * Beim Angriff - nur dann? Wenn unbeschäftigt und mode=auto
	 */
	public void AttackLerne() {
		
		String LernFixOrder = "Talent=" + this.AttackLernTalent;
		
		if (this.AttackLernPlan!=null && this.AttackLernPlan.length()>0) {
			LernFixOrder = "Lernplan=" + this.AttackLernPlan;
		}
		
		
		this.scriptUnit.addComment("Lernfix wird initialisiert mit dem Parameter: " + LernFixOrder);
		Script L = new Lernfix();
		ArrayList<String> order = new ArrayList<String>();
		order.add(LernFixOrder);
		L.setArguments(order);
		L.setScriptUnit(this.scriptUnit);
		L.setGameData(this.scriptUnit.getScriptMain().gd_ScriptMain);
		if (this.scriptUnit.getScriptMain().client!=null){
			L.setClient(this.scriptUnit.getScriptMain().client);
		}
		this.scriptUnit.addAScript(L);
		
		// 20210627: betrete die grösste Burg
		Building b = FFToolsRegions.getBiggestCastle(this.region());
		if (b!=null && this.automode && !b.equals(this.getUnit().getModifiedBuilding())) {
			// Betreten
			this.addOrder("Betrete BURG " + b.getID().toString() + " ; Jagemonster (auto)", true);
		} 
	}

	public boolean isHC_ready() {
		return HC_ready;
	}

	public void setHC_ready(boolean hC_ready) {
		HC_ready = hC_ready;
	}

	public int getHC_weeks2target() {
		return HC_weeks2target;
	}

	public void setHC_weeks2target(int hC_weeks2target) {
		HC_weeks2target = hC_weeks2target;
	}

	public CoordinateID getTargetDest() {
		return targetDest;
	}

	public void setTargetDest(CoordinateID targetDest) {
		this.targetDest = targetDest;
	}
	
	public int getBattleValue() {
		SkillType ST = FFToolsUnits.getBestSkillType(this.getUnit());
		if (ST!=null) {
			// wir gehen davon aus:
			//   - Bewaffnung passt
			// - bestes talent ist kampftalent
			return this.getUnit().getModifiedSkill(ST).getLevel() * this.getUnit().getModifiedPersons();
		}
		return 0;
	}
	
	public String toString() {
		return this.scriptUnit.toString() + " [JM]";
	}

	public CoordinateID getHomeDest() {
		return homeDest;
	}
	

	
	
}
