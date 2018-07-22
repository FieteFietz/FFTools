package com.fftools.scripts;

import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.GotoInfo;


/**
 * 
 * Der Pferdemacher
 * 
 * @author Fiete
 *
 */

public class Pferde extends MatPoolScript{
	
	
	private static final int Durchlauf_vorMP1 = 28;
	private static final int Durchlauf_nachMP1 = 310;
	
	/**
	 * Anforderung der Pferde
	 */
	private int pferdRequestPrio = 700;
	
	private int[] runners = {Durchlauf_vorMP1,Durchlauf_nachMP1};
	
	/**
	 * wieviele Pferde sollen mindestens in der Region als Bestand bleiben?
	 */
	private int minPferdRegion = 0;
	
	/**
	 * mit welcher MindestAuslastung soll dieser Macher arbeiten?
	 * Angabe in Prozent
	 */
	private static int minAuslastungDefault = 75;
	private int minAuslastung = minAuslastungDefault;
	
	/**
	 * wenn er nicht machen kann, welchen Lernplan soll er verfolgen
	 */
	private String LernPlanName = null;
	
	/**
	 * wenn er nicht machen kann, welches Talent soll er Lernen
	 */
	private String LernTalent = null;
	
	/**
	 * nur wahr, wenn Einheit in Pferdezucht
	 */
	private boolean isZuechter = false;
	
	/**
	 * wenn zuechter, dann hier der request nach den Pferden
	 */
	private MatPoolRequest zuechterRequest = null;
	
	/**
	 * Pferdezuchtlevel..dann brauchen wir ihn nur einmal holen
	 */
	private int SkillLevel = 0; 
	
	/**
	 * in automode ?
	 */
	private boolean automode = false;
	
	/**
	 * wenn automover, dann hierin die GotoInfo
	 */
	private GotoInfo gotoInfo = null;
	
	/**
	 * ab welchem Talent gehts erst mal los?
	 */
	private int minTalent = 1;
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Pferde() {
		super.setRunAt(this.runners);
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
		
		if (scriptDurchlauf==Durchlauf_vorMP1){
			this.scriptStart();
		}
		
		if (scriptDurchlauf==Durchlauf_nachMP1){
			this.scriptStart_nachMP1();
		}
		
		
	}
	
	private void scriptStart(){
		// Optionen Parsen
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Pferde");
		
		
		int unitMinTalent = OP.getOptionInt("minTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		unitMinTalent = OP.getOptionInt("mindestTalent", -1);
		if (unitMinTalent>this.minTalent){
			this.minTalent = unitMinTalent;
		}
		
		this.minPferdRegion = OP.getOptionInt("minRegion",this.minPferdRegion);
		if (this.minPferdRegion<0 || this.minPferdRegion>1000){
			this.addComment("minPferd nicht erkannt");
			this.scriptUnit.doNotConfirmOrders();
			outText.addOutLine("!!! Pferde: minRegion nicht erkannt! " + this.unitDesc(), true);
			this.minPferdRegion=0;
		}
		
		
		int reportAuslastung = reportSettings.getOptionInt("pferdeMindestAuslastung", this.region());
		if (reportAuslastung!=ReportSettings.KEY_NOT_FOUND){
			this.minAuslastung = reportAuslastung;
		}
		
		this.minAuslastung = OP.getOptionInt("minAuslastung", this.minAuslastung);
		if (this.minAuslastung<0 || this.minAuslastung>100){
			this.addComment("minAuslastung nicht erkannt");
			this.scriptUnit.doNotConfirmOrders();
			outText.addOutLine("!!! Pferde: minAuslastung nicht erkannt! " + this.unitDesc(), true);
			this.minAuslastung=minAuslastungDefault;
		}
		
		this.LernPlanName = OP.getOptionString("Lernplan");
		
		this.LernTalent = OP.getOptionString("LernTalent");
		
		if (this.LernTalent.length()>2 && this.LernPlanName.length()>2){
			this.addComment("Lernplan und Lerntalent nicht möglich!");
			this.scriptUnit.doNotConfirmOrders();
			outText.addOutLine("!!! Pferde: Lernplan und Lerntalent nicht möglich! " + this.unitDesc(), true);
			this.LernPlanName = "";
			this.LernTalent = "";
		}
		
		this.pferdRequestPrio = OP.getOptionInt("maxPferdPrio", this.pferdRequestPrio);
		if (this.pferdRequestPrio<0 || this.pferdRequestPrio>1500){
			this.addComment("Pferde Request Prio nicht erkannt");
			outText.addOutLine("!!! Pferde: request prio nicht erkannt! " + this.unitDesc(), true);
			this.scriptUnit.doNotConfirmOrders();
			this.pferdRequestPrio = 10;
		}
		
		int menge = this.maxMachenPferde();
		
		if (this.SkillLevel<this.minTalent){
			this.addComment("mindestTalent nicht erreicht. Versuche zu lernen.");
			this.alternativOrder();
		} else {
			// feststellen, ob in Pferdezucht
			if (FFToolsGameData.isInGebäude(this.scriptUnit,"Pferdezucht")){
				// wenn ja, nicht als PferdeMACHER registrieren!
				isZuechter=true;
				// Pferde zur Zucht requesten
				
				if (menge>0){
					this.zuechterRequest = new MatPoolRequest(this,menge,"Pferd",this.pferdRequestPrio,"Zuechter");
					this.addMatPoolRequest(this.zuechterRequest);
					this.addComment("ist Zuechter. " + menge + " Pferde angefordert. (Prio " + this.pferdRequestPrio + ")");
				} else {
					this.addComment("Zuechter kann nicht zuechten? (Talent?)");
					this.scriptUnit.doNotConfirmOrders();
					outText.addOutLine("!!! Zuechter kann nicht zuechten? (Talent?)! " + this.unitDesc(), true);
				}
			} else {
				// nur registrieren (?)
				this.getOverlord().getPferdeManager().addPferdeMacher(this);
			}
		}
		if (OP.getOptionString("mode").equalsIgnoreCase("auto")){
			this.automode=true;
		}
	}
	
	/**
	 * wenn zuechter, vielleicht züchten
	 */
	private void scriptStart_nachMP1(){
		// nur wenn zuechter
		if (this.isZuechter && this.zuechterRequest!=null){
			// erreichen wir Auslastung?
			if (this.isInMinAuslastung(this.zuechterRequest.getBearbeitet())){
				// jo, wir werden zuechten 
				this.addOrder("ZÜCHTE PFERDE", true);
				double chance = (double)this.SkillLevel / 100;
				int anzVersuche = Math.min(this.zuechterRequest.getBearbeitet(), this.SkillLevel * this.scriptUnit.getUnit().getModifiedPersons());
				int expected = (int)((double)anzVersuche * chance);
				this.addComment("Pferdezucht - zu erwartende Zucht: " + expected + " Pferde.");
			} else {
				// nein
				this.addComment("Nur " + this.zuechterRequest.getBearbeitet() + " Pferde verfügbar. Keine Zucht. MinAuslastung: " + this.minAuslastung + "%");
				this.alternativOrder();
			}
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
	 * liefert die maximale Anzahl zu machender Pferde durch
	 * diese Einheit
	 * @return
	 */
	public int maxMachenPferde(){
		int erg = 0;
		int skillLevel = 0;
		SkillType skillType = this.gd_Script.rules.getSkillType("Pferdedressur", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
				this.SkillLevel = skillLevel;
			}
		}
		erg = skillLevel * this.scriptUnit.getUnit().getModifiedPersons();
		return erg;
	}
	
	
	/**
	 * überprüft, ob eine gewünschet Anzahl Pferde mit der Restriktion
	 * der Mindestauslastung vereinbar ist
	 * @param zuFangen
	 * @return
	 */
	public boolean isInMinAuslastung(int zuFangen){
		boolean erg = true;
		
		double maxLeistung = (double)this.maxMachenPferde();
		double sollLeistung = (double)zuFangen;
		double sollAuslastung = (sollLeistung / maxLeistung) * 100;
		if (sollAuslastung < this.minAuslastung){
			erg = false;
		}
		
		
		return erg;
	}
	
	/**
	 * wird vom Manager aufgerufen, wenn nixh zu fangen ist
	 *
	 */
	public void alternativOrder(){
		// Haben wir einen Lernplan?
		if (this.LernPlanName.length()>2){
			this.addComment("Setze Lernfix mit Lernplan=" + this.LernPlanName);
			this.scriptUnit.findScriptClass("Lernfix", "Lernplan=" + this.LernPlanName);
			return;
		}
		if (this.LernTalent.length()>2){
			this.addComment("Setze Lernfix mit Talent=" + this.LernTalent);
			this.scriptUnit.findScriptClass("Lernfix", "Talent=" + this.LernTalent);
			return;
		}
		// default behaviour
		this.addComment("kein gesetztes Lernen -> Default.");
		this.addOrder("Lernen Pferdedressur",true);
	}
	
	/**
	 * @return the minAuslastung
	 */
	public int getMinAuslastung() {
		return minAuslastung;
	}


	/**
	 * @return the minPferdRegion
	 */
	public int getMinPferdRegion() {
		return minPferdRegion;
	}


	/**
	 * @return the automode
	 */
	public boolean isAutomode() {
		return automode;
	}


	public GotoInfo getGotoInfo() {
		return gotoInfo;
	}


	public void setGotoInfo(GotoInfo gotoInfo) {
		this.gotoInfo = gotoInfo;
	}


	
	
}
