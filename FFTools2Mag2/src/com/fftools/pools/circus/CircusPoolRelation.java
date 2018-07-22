package com.fftools.pools.circus;

import magellan.library.Region;
import magellan.library.rules.SkillType;

import com.fftools.ScriptUnit;
import com.fftools.scripts.Unterhalten;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

/**
 * Klasse die Auskunft über die Entertainerqualitäten einer ScriptUnit gibt
 * Wird benötigt falls später im Pool sortiert werden soll und stellt in diesem Fall 
 * dann Comparable bereit. 
 * 
 * @author Marc
 *
 */

public class CircusPoolRelation implements Comparable<CircusPoolRelation> {
    
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	// Welche ScriptUnit bietet sich als Unterhalter an?
	private ScriptUnit scriptUnit=null;
	private Unterhalten unterhalten = null;
	
	// Welcher CircusPool ist für die Relation zuständig?
	private CircusPool circusPool;
	
	// Daten zur Einheit selbst, die für das Unterhalten wichtig sind
	
	private int talentStufe =0;
	private int personenZahl =0;
	private int verdienst=0;
	private int proKopfVerdienst=0; 
	private int doUnterhaltung = 25000;
	// hilfsvariabkle zum Vergleichen von Entfernungen
	private int dist = -1;
	private GotoInfo gotoInfo = null;
	
	
	public CircusPoolRelation(Unterhalten _unterhalten, CircusPool _cp){
		scriptUnit = _unterhalten.scriptUnit;
		unterhalten = _unterhalten;
		circusPool = _cp;
		// FF 20070413 geändert auf modified persons
		personenZahl = scriptUnit.getUnit().getModifiedPersons();
		
		
		// Skilltype zu "Unterhalten" besorgen
		SkillType unterhaltenSkillType =  circusPool.circusPoolManager.scriptMain.gd_ScriptMain.rules.getSkillType("Unterhaltung");
		
		// kann die Einheit unterhalten? Falls nicht kommt null zurück.
		if (scriptUnit.getUnit().getModifiedSkill(unterhaltenSkillType) != null){
            // ja, dann kann man das talent abgreifen. 		
			// sonst bleibt es eben bei Stufe 0.
			// talentStufe = scriptUnit.getUnit().getSkill(unterhaltenSkillType).getLevel();
			// FF 20101227: geändert auf modified skill 
			talentStufe = scriptUnit.getUnit().getModifiedSkill(unterhaltenSkillType).getLevel();
        }	
		
		verdienst=personenZahl*talentStufe*20;
		proKopfVerdienst=talentStufe*20;
			
	};

	
	
	// ein paar fixe methoden für den CircusPool
	
	
	public ScriptUnit getSkriptUnit(){
		return scriptUnit;
	};
	
	public CircusPool getCircusPool(){
		return circusPool;
	};
	
	public int getTalentStufe(){
		return talentStufe;
	};
		
	public int getPersonenZahl(){
		return personenZahl;
	};
	
	public int getVerdienst(){
		return verdienst;
	};
	
	public int getProKopfVerdienst(){
	return proKopfVerdienst;
	};
	
		
	/**
	 * 
	 * Gibt nach dem Poollauf zurück welchen Betrag die Einheit unterhalten soll.
	 * Ist der Betrag negativ soll alternative zu Unterhalten von Script gewählt werden. 
	 */
	
	public int getDoUnterhaltung(){
		return doUnterhaltung;
	}
	
	/**
	 * Pool setzt den Betrag den die Einheit unterhalten soll
	 *
	 */
    public void setDoUnterhaltung(int _unt){
		doUnterhaltung = _unt;
	}
	
    /**
     * Pool schreibt die erwartete Unterhaltung in der gesamten Region zurück
     * Hilft Anzahl der Unterhalter zu regulieren
     * @param _regver
     */
    

			
	/**
	 * Sortierbarkeit in Array und ArrayList
	 
	 */
	
	public int compareTo(CircusPoolRelation cpr){
		// Weiß jetzt nicht ob negativ oder positiv größer heißt...???
		int Differenz = (cpr.talentStufe - this.talentStufe);
		return Differenz;
	}



	public Unterhalten getUnterhalten() {
		return unterhalten;
	}



	/**
	 * @return the dist
	 */
	public int getDist() {
		return dist;
	}



	/**
	 * @param dist the dist to set
	 */
	public void setDist(int dist) {
		this.dist = dist;
	}
	
	// setzt den dest wert auf die entfernung zur Region "to"
	public void setDistToRegion(Region to){
		this.gotoInfo = FFToolsRegions.makeOrderNACH(this.unterhalten.scriptUnit, this.unterhalten.scriptUnit.getUnit().getRegion().getCoordinate(), to.getCoordinate(), false,"CircusPoolRelation");
		this.dist = this.gotoInfo.getAnzRunden();
	}



	/**
	 * @return the gotoInfo
	 */
	public GotoInfo getGotoInfo() {
		return gotoInfo;
	}
	
	
	
}
