package com.fftools.pools.seeschlangen;

import java.util.ArrayList;

import com.fftools.OutTextClass;
import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Jagemonster;

import magellan.library.Region;
import magellan.library.Unit;

/**
 * Verwaltet MonsterJ�ger
 * 
 * 
 * @author Fiete
 *
 */
public class MonsterJagdManager_MJM implements OverlordRun,OverlordInfo {

	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private Overlord overLord = null;
	
	private ArrayList<Jagemonster> J�ger = new ArrayList<Jagemonster>(0);
	
	private ArrayList<Region> attackRegions = new ArrayList<Region>(0);
	private ArrayList<Region> NOattackRegions = new ArrayList<Region>(0);
	private ArrayList<Region> needTacticRegions = new ArrayList<Region>(0);
	private ArrayList<Unit> attackedMonsterUnits = new ArrayList<Unit>(0);
	private ArrayList<Unit> targettedMonsterUnits = new ArrayList<Unit>(0);
		
	/**
	 * Wann soll er laufen
	 * VOR Lernfix , zwischen den beiden L�ufen von Jagemonster
	 */
	private static final int Durchlauf = 37;
	
	// R�ckgabe als Array
	private int[] runners = {Durchlauf};

	public MonsterJagdManager_MJM(Overlord overlord){
		this.overLord = overlord;
	}

	
	/**
	 * startet den MJM
	 */
	public void run(int durchlauf){
		if (durchlauf==Durchlauf) {
			this.makeDecisions();
		}
	}
	
	/**
	 * geht alle J�ger durch und trifft f�r die entsprechenden Regionen die Entscheidung
	 * Gen�gend Angreifer da, gen�gend 1. Reihe
	 */
	private void makeDecisions() {
		for (Jagemonster jm : J�ger) {
			Region r = jm.getUnit().getRegion();
			if (this.attackRegions.contains(r)) {
				continue;
			}
			if (this.NOattackRegions.contains(r)) {
				continue;
			}
			// Scheinbar noch nicht entschieden worden, also f�r diese Region die Entscheidung ansto�en
			this.decideRegion(r);
		}
	}
	
	/**
	 * Trifft die Entscheidung f�r eine Region
	 * @param r
	 */
	private void decideRegion(Region r) {
		// check 1 haben wir �berhaupt eine 1. Reihe
		int Reihe1 = countRow(r,Jagemonster.role_AttackFront);
		if (Reihe1==0) {
			// negativer Bescheid
			this.NOattackRegions.add(r);
			this.informJ�gerRegion(r, "MJM: kein Angriff - keine erste Reihe");
			return;
		}
		int Reihe2 = countRow(r,Jagemonster.role_AttackBack);
		int Support = countRow(r,Jagemonster.role_Support);
		
		// MonsterWert berechnen
		int monsterValue = countMonsterValue(r);
		int monsterCount = countMonster(r);
		informJ�gerRegion(r, "MJM: " + monsterCount + " Monster in der Region. Bedrohungswert: " + monsterValue);
		
		if (this.needTacticRegions.contains(r)) {
			informJ�gerRegion(r, "MJM: es werden taktische Talente ben�tigt.");
		}
		informJ�gerRegion(r, "MJM: eigene Kr�fte in der Region: " + (Reihe1 + Reihe2) + " (" + Reihe1 + "|" + Reihe2 +")");
		
		// Kann die 1. Reihe �berrannt werden ?
		// Faktor ist 3
		if (monsterCount>(Reihe1 * 3) && (Reihe2>0 || Support>0)) {
			// Wir k�nnten �berrannt werden und greifen nicht an.
			// ToDo - make it overridable
			this.NOattackRegions.add(r);
			informJ�gerRegion(r, "MJM: kein Angriff! Die erste Reihe w�rde �berrannt werden.");
			return;
		}
		
		// Haben wir einen Taktiker vor Ort?
		if (this.needTacticRegions.contains(r)) {
			if (!chkTactican(r)) {
				this.NOattackRegions.add(r);
				informJ�gerRegion(r, "MJM: kein Angriff! Es ist kein Taktiker vor Ort.");
				return;
			}
		}
		
		// Haben wir genug Firepower vor Ort ?
		if ((Reihe1+Reihe2)<monsterValue) {
			this.NOattackRegions.add(r);
			informJ�gerRegion(r, "MJM: kein Angriff! Eigene Kr�fte sind zu schwach: " + (Reihe1+Reihe2) + " K�mpfer <" + monsterValue + " Bedrohung");
			return;
		}
		
		// OK, spricht nix gegen einen Angriff - Shaka
		informJ�gerRegion(r, "MJM: Angriff!");
		this.attackRegions.add(r);
		// gleich die Befehle setzen
		attackInRegion(r);
	}
	
	/**
	 * setzt die Angriffsbefehle
	 * @param r
	 */
	private void attackInRegion(Region r) {
		for (Jagemonster jm : J�ger) {
			Region r_jm = jm.getUnit().getRegion();
			if (r_jm.equals(r)) {
				attackInRegionWithJM(r, jm);
				jm.setMJM_setAttack(true);
			}
		}
	}
	
	/**
	 * setzt die Angriffsbefehle f�r einen JM
	 * setzt folge auf die gr�sste einheite
	 * @param r
	 * @param JM
	 */
	private void attackInRegionWithJM(Region r, Jagemonster JM) {
		int maxMonsterCount = 0;
		String maxMonsterID = "";
		
		for(Unit u: r.units()) {
			if (u.getFaction()!=null && u.getFaction().getID().toString().equals("ii")) {
				if (JM.getRole()!=Jagemonster.role_Support) {
					JM.addOrder("ATTACKIERE " + u.getID().toString() + " ;MJM checked", true);
				}
				if (!this.attackedMonsterUnits.contains(u)) {
					this.attackedMonsterUnits.add(u);
				}
				if (u.getPersons()>maxMonsterCount) {
					maxMonsterCount=u.getPersons();
					maxMonsterID=u.getID().toString();
				}
			}
		}
		
		if (maxMonsterID.length()>0) {
			JM.addOrder("FOLGE EINHEIT " + maxMonsterID + " ;MJM checked", true);
		}
	}
	
	
	public boolean isAttacked(Unit monsterUnit) {
		return (this.attackedMonsterUnits.contains(monsterUnit));
	}
	
	
	
	/**
	 * informiert die J�ger per Comment �ber Entscheidungen des MJM
	 * @param r
	 * @param msg
	 */
	private void informJ�gerRegion (Region r, String msg) {
		for (Jagemonster jm : J�ger) {
			Region r_jm = jm.getUnit().getRegion();
			if (r_jm.equals(r)) {
				jm.addComment(msg);
			}
		}
	}
	
	
	/** 
	 * Z�hlt nicht modified, *vor* dem GIB kommt der ATTACK
	 * @param r
	 * @param rolle
	 * @return
	 */
	private int countRow(Region r, int rolle) {
		int erg  = 0;
		for (Jagemonster jm : J�ger) {
			Region r_jm = jm.getUnit().getRegion();
			if (r_jm.equals(r) && jm.getRole()==rolle) {
				// gleiche Region und richtige Rolle
				erg += jm.getUnit().getPersons();
			}
		}
		return erg;
	}
	
	/** 
	 * Pr�ft, ob Taktiker dabei ist
	 * @param r
	 * @param rolle
	 * @return
	 */
	private boolean chkTactican(Region r) {
		boolean erg  = false;
		for (Jagemonster jm : J�ger) {
			Region r_jm = jm.getUnit().getRegion();
			if (r_jm.equals(r) && (jm.getRole()==Jagemonster.role_AttackBack || jm.getRole()==Jagemonster.role_AttackFront) && jm.isTactican()) {
				// gleiche Region und richtige Rolle und Taktiker
				return true;
			}
		}
		return erg;
	}
	
	/**
	 * Berechnet den KampfWert aller Monster in der Region
	 * @param r
	 * @return
	 */
	private int countMonster(Region r) {
		int monsterValue=0;
		for(Unit u: r.units()) {
			if (u.getFaction()!=null && u.getFaction().getID().toString().equals("ii")) {				
				monsterValue += u.getPersons();
			}
		}
		return monsterValue;
	}
	
	/**
	 * Berechnet den KampfWert aller Monster in der Region
	 * @param r
	 * @return
	 */
	private int countMonsterValue(Region r) {
		int monsterValue=0;
		boolean needTaktik = false;
		double factor=1;
		for(Unit u: r.units()) {
			if (u.getFaction()!=null && u.getFaction().getID().toString().equals("ii")) {
				// Monster
				factor = 1;
				if (u.getRace().toString().equalsIgnoreCase("Skelette")) {
					factor = 0.6;
				}
				if (u.getRace().toString().equalsIgnoreCase("Ghoule")) {
					factor = 0.6;
				}
				if (u.getRace().toString().equalsIgnoreCase("Untote")) {
					factor = 0.6;
				}
				if (u.getRace().toString().equalsIgnoreCase("Zombies")) {
					factor = 0.6;
				}
				if (u.getRace().toString().equalsIgnoreCase("Dracoide")) {
					factor = 0.6;
				}
				
				if (u.getRace().toString().equalsIgnoreCase("Skelettherren")) {
					factor = 5;
				}
				if (u.getRace().toString().equalsIgnoreCase("Ghaste")) {
					factor = 5;
				}
				
				if (u.getRace().toString().equalsIgnoreCase("Juju-Zombies")) {
					factor = 20;
				}
				if (u.getRace().toString().equalsIgnoreCase("Juju-Ghaste")) {
					factor = 20;
				}
				if (u.getRace().toString().equalsIgnoreCase("Juju-Drachen")) {
					factor = 30;
				}
				if (u.getRace().toString().equalsIgnoreCase("Jungdrachen")) {
					factor = 15;
				}
				if (u.getRace().toString().equalsIgnoreCase("Drachen")) {
					factor = 200;
					needTaktik=true;
				}
				if (u.getRace().toString().equalsIgnoreCase("Wyrme")) {
					factor = 500;
					needTaktik=true;
				}
				
				if (u.getRace().toString().equalsIgnoreCase("Hirnt�ter")) {
					factor = 100;
					needTaktik=true;
				}
				
				monsterValue += Math.round(factor * (double)u.getPersons());
			}
		}
		
		if (needTaktik && !this.needTacticRegions.contains(r)) {
			this.needTacticRegions.add(r);
		}
		return monsterValue;
	}
	
	
	
	/**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
    
    /**
     * f�gt angriffsbereite Jagdeinheiten dem MJM hinzu
     * MJM entscheidet, ob genug JM vorhanden sind und ob keien BACK ohne FRONT angreift
     * @param JM
     */
    public void addJ�ger(Jagemonster JM) {
    	if (!this.J�ger.contains(JM)) {
    		this.J�ger.add(JM);
    	}
    }
    
    
    public void addTargetUnit(Unit monsterUnit) {
    	if (!this.targettedMonsterUnits.contains(monsterUnit)) {
    		this.targettedMonsterUnits.add(monsterUnit);
    	}
    }
    
    public boolean isMonsterTargetted(Unit monsterUnit) {
    	return this.targettedMonsterUnits.contains(monsterUnit);
    }
    
    

}
