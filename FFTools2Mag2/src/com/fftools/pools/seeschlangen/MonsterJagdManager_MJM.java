package com.fftools.pools.seeschlangen;

import java.util.ArrayList;
import java.util.HashMap;

import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Jagemonster;

import magellan.library.Region;
import magellan.library.Unit;

/**
 * Verwaltet MonsterJäger
 * 
 * 
 * @author Fiete
 *
 */
public class MonsterJagdManager_MJM implements OverlordRun,OverlordInfo {

	public static final String MAPLINE_MOVE_TAG="FFTools_MJM_MoveLine";
	public static final String MAPLINE_ATTACK_TAG="FFTools_MJM_AttackLine";
	
	
	private ArrayList<Jagemonster> Jäger = new ArrayList<Jagemonster>(0);
	
	private ArrayList<Region> attackRegions = new ArrayList<Region>(0);
	private ArrayList<Region> NOattackRegions = new ArrayList<Region>(0);
	private ArrayList<Region> needTacticRegions = new ArrayList<Region>(0);
	private ArrayList<Unit> attackedMonsterUnits = new ArrayList<Unit>(0);
	private ArrayList<Unit> targettedMonsterUnits = new ArrayList<Unit>(0);
	
	private HashMap<String, MonsterThreat> monsterThreats = new HashMap<String, MonsterJagdManager_MJM.MonsterThreat>();
	private ArrayList<String> monsterThreatInfos = new ArrayList<String>(0);
	
	private ArrayList<Jagemonster> MJM_Informanten = new ArrayList<Jagemonster>(0);
	
	
	private class MonsterThreat {
		private double factor=1;
		private boolean needTactican = false;
		private String RaceName = "";
		
		public double getFactor() {
			return factor;
		}
		public boolean needTactican() {
			return needTactican;
		}
		public String getRaceName() {
			return RaceName;
		}
		public MonsterThreat(String RaceName, double factor) {
			this(RaceName, factor, false);
		}
		
		public MonsterThreat(String RaceName, double factor, boolean needTactican) {	
			this.RaceName = RaceName;
			this.factor = factor;
			this.needTactican = needTactican;
		}	
		
		public String toString() {
			String erg = "MonsterBedrohungsFaktor - " + this.getRaceName() + ": Faktor=" + this.getFactor();
			if (this.needTactican) {
				erg += " (erfordert Taktiker)";
			}
			return erg;
		}
		
	}
	
	/**
	 * Wann soll er laufen
	 * VOR Lernfix , zwischen den beiden Läufen von Jagemonster
	 */
	private static final int Durchlauf = 37;
	
	// Rückgabe als Array
	private int[] runners = {Durchlauf};

	public MonsterJagdManager_MJM(Overlord overlord){
		// this.overLord = overlord;
		
		// Standard Bedrohungswerte
		this.monsterThreats.put("Skelette", new MonsterThreat("Skelette", 0.6));
		this.monsterThreats.put("Ghoule", new MonsterThreat("Ghoule", 0.6));
		this.monsterThreats.put("Untote", new MonsterThreat("Untote", 0.6));
		this.monsterThreats.put("Zombies", new MonsterThreat("Zombies", 0.6));
		this.monsterThreats.put("Dracoide", new MonsterThreat("Dracoide", 0.6));
		this.monsterThreats.put("Skelettherren", new MonsterThreat("Skelettherren", 7));
		this.monsterThreats.put("Ghaste", new MonsterThreat("Ghaste", 10));
		this.monsterThreats.put("Juju-Zombies", new MonsterThreat("Juju-Zombies", 20));
		this.monsterThreats.put("Juju-Ghaste", new MonsterThreat("Juju-Ghaste", 20));
		this.monsterThreats.put("Juju-Drachen", new MonsterThreat("Juju-Drachen", 30));
		this.monsterThreats.put("Jungdrachen", new MonsterThreat("Jungdrachen", 15));
		this.monsterThreats.put("Drachen", new MonsterThreat("Drachen", 100,true));
		this.monsterThreats.put("Wyrme", new MonsterThreat("Wyrme", 500,true));
		this.monsterThreats.put("Hirntöter", new MonsterThreat("Hirntöter", 10,true));
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
	 * geht alle Jäger durch und trifft für die entsprechenden Regionen die Entscheidung
	 * Genügend Angreifer da, genügend 1. Reihe
	 */
	private void makeDecisions() {
		for (Jagemonster jm : Jäger) {
			Region r = jm.getUnit().getRegion();
			if (this.attackRegions.contains(r)) {
				continue;
			}
			if (this.NOattackRegions.contains(r)) {
				continue;
			}
			// Scheinbar noch nicht entschieden worden, also für diese Region die Entscheidung anstoßen
			this.decideRegion(r);
		}
		
		
		// InformationsService
		for (Jagemonster jm : this.MJM_Informanten) {
			if (jm.wants_info_MJM_Settings()) {
				// Überblick über aktuelle Settings des MJM
				jm.addComment("MJM: Monster Bedrohungs Settings");
				for (String s:this.monsterThreats.keySet()) {
					MonsterThreat MT = this.monsterThreats.get(s);
					String erg = "- " + s + ": Faktor " + MT.getFactor();
					if (MT.needTactican()) {
						erg += " (erfordert Taktiker)";
					}
					jm.addComment(erg);
				}
			}
			
			if (jm.wants_info_MJM_Region()) {
				int i = countMonsterValue(jm.region());
				if (i>0) {
					jm.addComment("MJM_RegionInfo: Bedrohungswert in der Region: " + i);
				} else {
					jm.addComment("MJM_RegionInfo: keine Bedrohung in der Region.");
				}
				if (this.monsterThreatInfos.size()>0) {
					for (String s:this.monsterThreatInfos) {
						jm.addComment("MJM_Region: " + s);
					}
				}
			}
		}
	}
	
	/**
	 * Trifft die Entscheidung für eine Region
	 * @param r
	 */
	private void decideRegion(Region r) {
		// check 1 haben wir überhaupt eine 1. Reihe
		int Reihe1 = countRow(r,Jagemonster.role_AttackFront);
		if (Reihe1==0) {
			// negativer Bescheid
			this.NOattackRegions.add(r);
			this.informJägerRegion(r, "MJM: kein Angriff - keine erste Reihe");
			return;
		}
		int Reihe2 = countRow(r,Jagemonster.role_AttackBack);
		int Support = countRow(r,Jagemonster.role_Support);
		
		// MonsterWert berechnen
		int monsterValue = countMonsterValue(r);
		int monsterCount = countMonster(r);
		informJägerRegion(r, "MJM: " + monsterCount + " Monster in der Region. Bedrohungswert: " + monsterValue);
		
		if (this.needTacticRegions.contains(r)) {
			informJägerRegion(r, "MJM: es werden taktische Talente benötigt.");
		}
		informJägerRegion(r, "MJM: Kampfwert eigene Kräfte in der Region: " + (Reihe1 + Reihe2) + " (" + Reihe1 + "|" + Reihe2 +")");
		
		// Kann die 1. Reihe überrannt werden ?
		// Faktor ist 3
		if (monsterCount>(Reihe1 * 3) && (Reihe2>0 || Support>0)) {
			// Wir könnten überrannt werden und greifen nicht an.
			// ToDo - make it overridable
			this.NOattackRegions.add(r);
			informJägerRegion(r, "MJM: kein Angriff! Die erste Reihe würde überrannt werden.");
			return;
		}
		
		// Haben wir einen Taktiker vor Ort?
		if (this.needTacticRegions.contains(r)) {
			if (!chkTactican(r)) {
				this.NOattackRegions.add(r);
				informJägerRegion(r, "MJM: kein Angriff! Es ist kein Taktiker vor Ort.");
				return;
			}
		}
		
		// Haben wir genug Firepower vor Ort ?
		if ((Reihe1+Reihe2)<monsterValue) {
			this.NOattackRegions.add(r);
			informJägerRegion(r, "MJM: kein Angriff! Eigene Kräfte sind zu schwach: " + (Reihe1+Reihe2) + " Kämpfer <" + monsterValue + " Bedrohung");
			return;
		}
		
		// OK, spricht nix gegen einen Angriff - Shaka
		informJägerRegion(r, "MJM: Angriff!");
		this.attackRegions.add(r);
		// gleich die Befehle setzen
		attackInRegion(r);
	}
	
	/**
	 * setzt die Angriffsbefehle
	 * @param r
	 */
	private void attackInRegion(Region r) {
		for (Jagemonster jm : Jäger) {
			Region r_jm = jm.getUnit().getRegion();
			if (r_jm.equals(r)) {
				attackInRegionWithJM(r, jm);
				jm.setMJM_setAttack(true);
			}
		}
	}
	
	/**
	 * setzt die Angriffsbefehle für einen JM
	 * setzt folge auf die grösste einheite
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
	 * informiert die Jäger per Comment über Entscheidungen des MJM
	 * @param r
	 * @param msg
	 */
	private void informJägerRegion (Region r, String msg) {
		for (Jagemonster jm : Jäger) {
			Region r_jm = jm.getUnit().getRegion();
			if (r_jm.equals(r)) {
				jm.addComment(msg);
			}
		}
	}
	
	
	/** 
	 * Zählt nicht modified, *vor* dem GIB kommt der ATTACK
	 * @param r
	 * @param rolle
	 * @return
	 */
	private int countRow(Region r, int rolle) {
		int erg  = 0;
		for (Jagemonster jm : Jäger) {
			Region r_jm = jm.getUnit().getRegion();
			if (r_jm.equals(r) && jm.getRole()==rolle) {
				// gleiche Region und richtige Rolle
				erg += jm.getUnit().getPersons();
			}
		}
		return erg;
	}
	
	/** 
	 * Prüft, ob Taktiker dabei ist
	 * @param r
	 * @param rolle
	 * @return
	 */
	private boolean chkTactican(Region r) {
		boolean erg  = false;
		for (Jagemonster jm : Jäger) {
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
		String erg="";
		this.monsterThreatInfos = new ArrayList<String>(0);
		for(Unit u: r.units()) {
			if (u.getFaction()!=null && u.getFaction().getID().toString().equals("ii")) {
				// Monster		
				MonsterThreat MT = this.monsterThreats.get(u.getRace().toString());
				if (MT!=null) {
					long actMonsterValue = Math.round(MT.getFactor() * (double)u.getPersons()); 
					monsterValue += actMonsterValue;
					erg = u.getPersons() + " " + u.getRace().toString() + ", Faktor: " + MT.getFactor() + "; Wert: " + actMonsterValue;
					if (MT.needTactican()) {
						needTaktik=true;
						erg += " (erfordert Taktiker)";
					}
					
				} else {
					erg = "!!! keine Vorgabe für " + u.getRace().toString() + " vorhanden !!! (" + u.getPersons() + " Monster)";
				}
				this.monsterThreatInfos.add(erg);
			}
		}
		if (needTaktik && !this.needTacticRegions.contains(r)) {
			this.needTacticRegions.add(r);
		}
		return monsterValue;
	}
	
	/**
	 * liefert die Infos von der Threat-Berechnung
	 * vorher!!! countMonsterValue aufrufen
	 * @return
	 */
	public ArrayList<String> getMonsterThreatInfo() {
		return this.monsterThreatInfos;
	}
	
	
	
	/**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
    
    /**
     * fügt angriffsbereite Jagdeinheiten dem MJM hinzu
     * MJM entscheidet, ob genug JM vorhanden sind und ob keien BACK ohne FRONT angreift
     * @param JM
     */
    public void addJäger(Jagemonster JM) {
    	if (!this.Jäger.contains(JM)) {
    		this.Jäger.add(JM);
    	}
    }
    
    /**
     * fügt Jagdeinheiten dem MJM hinzu, die über Settings des MJM umfassend informiert werden wollen
     * 
     * @param JM
     */
    public void addInformant(Jagemonster JM) {
    	if (!this.MJM_Informanten.contains(JM)) {
    		this.MJM_Informanten.add(JM);
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
    
    
    public String addMonsterThreat(String s, double d, boolean t) {
    	String erg="";
    	MonsterThreat MT = new MonsterThreat(s, d, t);
    	erg = MT.toString();
    	this.monsterThreats.put(s, MT);
    	return erg;
    }

}
