package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;

import magellan.library.CoordinateID;
import magellan.library.ID;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.rules.SkillType;

public class Jagemonster extends Script{
	
	public static int role_Undef=0;
	public static int role_AttackFront=1;
	public static int role_AttackBack=2;
	public static int role_Support=3;
	
	private static final int Durchlauf_1 = 36;
	private static final int Durchlauf_2 = 38;
	
	private int[] runners = {Durchlauf_1,Durchlauf_2};
	
	
	/**
	 * kann gesetzt werden als Heimatbasis
	 */
	private CoordinateID homeDest = null;
	
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
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Jagemonster");
		OP.addOptionList(this.getArguments());
		
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
		
		// target
		String targetUnitName = OP.getOptionString("ziel");
		if (targetUnitName.length()==0) {
			targetUnitName = OP.getOptionString("target");
		}
		if (targetUnitName.length()==0){
			this.doNotConfirmOrders("!!! JageMonster - ziel fehlt! -> Unbestaetigt!!");
			return;
		}
		// target soll sich im TA aufhalten
		TradeArea TA = this.getOverlord().getTradeAreaHandler().getTAinRange(this.getUnit().getRegion());
		if (TA==null) {
			this.doNotConfirmOrders("!!! JageMonster - ich bin nicht in einem TradeArea! -> Unbestaetigt!!");
			return;
		}
		for (TradeRegion TR:TA.getTradeRegions()) {
			Region actRegion = TR.getRegion();
			for (Unit actU : actRegion.getUnits().values()) {
				String test = actU.getName();
				if (test==null){
					test = actU.getModifiedName();
				}
				ID test2 = actU.getID();
				if (test != null && test2 != null){
					if (test.equalsIgnoreCase(targetUnitName) || actU.toString(false).equalsIgnoreCase(targetUnitName)){
						this.targetUnit = actU;
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
				this.moveTo(this.homeDest);
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
				this.moveTo(this.targetUnit.getRegion().getCoordinate());
				this.getOverlord().getMJM().addTargetUnit(this.targetUnit);
			}
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
			// Angreifen - ATTACKIERE und FOLGE sind schon gesetzt, BEWACHE kommt von BEWACHE ?! Nope, wir beachen auch
			this.addOrder("BEWACHEN ;Jagemonster - angreifende Einheiten bewachen.", true);
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
	private void moveTo(CoordinateID dest) {
		this.addComment("JageMonster - befehle GOTO nach " + dest.toString());
		super.scriptUnit.findScriptClass("Goto",dest.toString(","));
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
	 * Beim Angriff - nur dann
	 */
	private void AttackLerne() {
		
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
	}
	
	
}
