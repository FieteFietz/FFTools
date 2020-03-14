package com.fftools.pools.treiber;

import com.fftools.ScriptUnit;
import com.fftools.scripts.Treiben;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

import magellan.library.Region;
import magellan.library.rules.SkillType;

/**
 * Klasse die Auskunft über die Treiberqualitäten einer ScriptUnit gibt
 * Wird benötigt falls später im Pool sortiert werden soll und stellt in diesem Fall 
 * dann Comparable bereit. 
 * 
 * @author Marc
 *
 */

public class TreiberPoolRelation implements Comparable<TreiberPoolRelation> {
    
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	// Welche ScriptUnit bietet sich als Treiber an?
	private ScriptUnit scriptUnit=null;
	private Treiben treiben=null;
	
	// Welcher TreiberPool ist für die Relation zuständig?
	private TreiberPool treiberPool;
	
	
	
	// Daten zur Einheit selbst, die für das Treiben wichtig sind
	
	private int talentStufe =0;
	private int personenZahl =0;
	private int verdienst=0;
	private int proKopfVerdienst=0; 
	private int doTreiben = 250000;
	
	// hilfsvariabkle zum Vergleichen von Entfernungen
	private int dist = -1;
	private GotoInfo gotoInfo = null;
	
	
	public TreiberPoolRelation(Treiben _tu, TreiberPool _cp){
		this.treiben = _tu;
		scriptUnit = _tu.scriptUnit;
		
		treiberPool = _cp;
		// FF 20070413 geändert auf modified persons
		personenZahl = scriptUnit.getUnit().getModifiedPersons();
		
		// Skilltype zu "Treiben" besorgen
		SkillType treibenSkillType =  treiberPool.treiberPoolManager.scriptMain.gd_ScriptMain.getRules().getSkillType("Steuereintreiben");
		
		// kann die Einheit treiben? Falls nicht kommt null zurück.
		if (scriptUnit.getUnit().getModifiedSkill(treibenSkillType) != null){
            // ja, dann kann man das talent abgreifen. 		
			// sonst bleibt es eben bei Stufe 0.
			talentStufe = scriptUnit.getUnit().getModifiedSkill(treibenSkillType).getLevel();
        }	
		
		verdienst=personenZahl*talentStufe*20;
		proKopfVerdienst=talentStufe*20;
			
	};


	// ein paar fixe methoden für den TreiberPool
	
	public ScriptUnit getSkriptUnit(){
		return scriptUnit;
	};
	
	public TreiberPool getTreiberPool(){
		return treiberPool;
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
	 * Gibt nach dem Poollauf zurück welchen Betrag die Einheit treiben soll.
	 * Ist der Betrag negativ soll alternative zu Treiben von Script gewählt werden. 
	 */
	
	public int getDoTreiben(){
		return doTreiben;
	}
	
	/**
	 * Pool setzt den Betrag den die Einheit treiben soll
	 *
	 */
    public void setDoTreiben(int _unt){
		doTreiben = _unt;
	}
	
    
   
			
	/**
	 * Sortierbarkeit in Array und ArrayList
	 
	 */
	
	public int compareTo(TreiberPoolRelation cpr){
		// Weiß jetzt nicht ob negativ oder positiv größer heißt...???
		int Differenz = (cpr.talentStufe - this.talentStufe);
		if (Differenz==0){
			Differenz = (cpr.personenZahl - this.personenZahl);
		}
		return Differenz;
	}


	public void setPersonenZahl(int personenZahl) {
		this.personenZahl = personenZahl;
		this.verdienst=this.personenZahl*talentStufe*20;
	}


	public Treiben getTreiben() {
		return treiben;
	}
	
	// setzt den dest wert auf die entfernung zur Region "to"
	public void setDistToRegion(Region to){
		this.gotoInfo = FFToolsRegions.makeOrderNACH(this.treiben.scriptUnit, this.treiben.scriptUnit.getUnit().getRegion().getCoordinate(), to.getCoordinate(), false,"TreiberPoolRelation");
		this.dist = this.gotoInfo.getAnzRunden();
	}


	public int getDist() {
		return dist;
	}
	
}
