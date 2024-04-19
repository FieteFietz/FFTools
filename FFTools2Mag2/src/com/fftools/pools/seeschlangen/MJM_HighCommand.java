package com.fftools.pools.seeschlangen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.fftools.scripts.Jagemonster;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

import magellan.library.Region;

/**
 * Verwaltet MonsterJ�ger - generiert automatische Zielzuweisungen
 * 
 * 
 * @author Fiete
 *
 */
public class MJM_HighCommand  {

	
	private ArrayList<Jagemonster> J�ger = new ArrayList<Jagemonster>(0);
	
	private ArrayList<Region> attackRegions = new ArrayList<Region>(0);
	// private ArrayList<Region> NOattackRegions = new ArrayList<Region>(0);
	// private ArrayList<Region> needTacticRegions = new ArrayList<Region>(0);
	
	private ArrayList<Region> orderedMoveToRegions = new ArrayList<Region>(0);
	
	private HashMap<Region, MonsterRegion> monsterregions = new HashMap<Region, MonsterRegion>();
	
	private TradeArea TA = null;
	
	private MonsterJagdManager_MJM MJM = null;
	
	
	public MJM_HighCommand(TradeArea TA) {
		this.TA = TA;
		this.MJM = TA.getOverlord().getMJM();
		// beim MJM registrieren
		this.MJM.addMJM_HC(this);
	}
	
	
	
    
    /**
     * f�gt angriffsbereite Jagdeinheiten dem MJM_HC hinzu
     * MJM_HC entscheidet �ber Zielzuweisung 
     * @param JM
     */
    public void addJ�ger(Jagemonster JM) {
    	if (!this.J�ger.contains(JM)) {
    		this.J�ger.add(JM);
    	}
    }

    public String addMonsterThreat(Region r, int d, boolean t) {
    	String erg="";
    	MonsterRegion MT = new MonsterRegion(r, d, t);
    	erg = MT.toString();
    	this.monsterregions.put(r, MT);
    	return erg;
    }
    
    /*
     * HC trifft entscheidungen....wird vom MJM *vor* der eigentlichen aller J�ger aufgerufen - f�r jedes HC (jedes TA)
     */
    public void run() {
    	if (this.J�ger.size()==0) {
    		// keine J�ger....warum auch immer dann dieses HC existiert
    		return;
    	}
    	// zumindest informativ das TA absuchen und die MJ informieren
    	for(TradeRegion tR:TA.getTradeRegions()) {
    		Region r = tR.getRegion();
    		MonsterRegion mR = this.MJM.countMonsterValue(r);
    		if (mR.getThreadLevel()>0) {
    			this.monsterregions.put(r, mR);
    		}
    	}
    	ArrayList<MonsterRegion> MR_regions_sorted = new ArrayList<MonsterRegion>(0);
		MR_regions_sorted.addAll(this.monsterregions.values());
		// Sortieren
		Collections.sort(MR_regions_sorted);
    	
    	for (Jagemonster JM:this.J�ger) {
    		// Info zu Monstern
    		if (this.monsterregions.size()==0) {
    			JM.addComment("MJM_HC: keine Monster in diesem TA (" + TA.getName() + ")");
    		} else {
    			JM.addComment("MJM_HC: Regionen mit Monstern in diesem TA: " + this.monsterregions.size());
    			for (MonsterRegion MR:MR_regions_sorted) {
    				JM.addComment(MR.toString());
    			}
    		}
    		
    		// Info zu J�gern
    		for (Jagemonster JM2:this.J�ger) {
    			String text = JM2.toString() + ", " + JM2.getUnit().getModifiedPersons() + " Pers";
    			if (JM2.isTactican()) {
    				text += " (Taktiker)";
    			}
    			if (JM2.getRole()==Jagemonster.role_Support) {
    				text += " (Support)";
    			}
    			JM.addComment(text);
    		}
    	}
    	
    	
    	// Phase 1: MJ, die in Region mit Monstern sind, diese auch angreifen lassen
    	for (Jagemonster JM:this.J�ger) {
    		Region r = JM.getUnit().getRegion();
    		if (this.monsterregions.keySet().contains(r)) {
    			// Bingo
    			
    			MonsterRegion MR = this.monsterregions.get(r);
    			// in der Region wird angegriffen werden
    			MR.setScheduled4attack(true);
    			// JM soll diese Woche angreifen
    			JM.setReady4AttackThisWeek(true);
    			this.MJM.addJ�ger(JM);
    			JM.setHC_ready(true);
    			// region wird (vermutlich) angegriffen...
    			if (!this.attackRegions.contains(r)) {
    				this.attackRegions.add(r);
    			}
    			if (JM.isTactican()) {
    				MR.setHasTactican(true);
    			}
    			// den rest soll der MJM machen
    			JM.addComment("MJM_HC: Region zur Liste der umk�mpften Regionen hinzugef�gt.");
    		}
    	}
    	
    	// Phase 2: attacked Regions pr�fen auf Verst�rkungsbedarf
    	for (Region r:this.attackRegions) {
    		MonsterRegion MR = this.monsterregions.get(r);
    		MR.setAttackLevelReihe1(MJM.countRow(r,Jagemonster.role_AttackFront));
    		MR.setAttackLevelReihe2(MJM.countRow(r,Jagemonster.role_AttackBack));
    		
    		// check 1. Reihe...wieviel brauchen wir mindestens?
    		int MonsterCount = this.MJM.countMonster(r);
    		int neededPersons = (int)(MonsterCount / 3);
    		ArrayList<String> infoComments = new ArrayList<String>(0);
    		String infoComment = "";
    		ArrayList<Jagemonster> infoCommentsReceiver = new ArrayList<Jagemonster>(0);
    		int countAttacker = MR.getAttackLevelReihe1();
    		int countRequestMovers = neededPersons - MR.getAttackLevelReihe1();
    		if (countRequestMovers>0) {
    			infoComment = "MJM_HC OVERRUN: Die Situation in " + r.toString() + " erfordert " + neededPersons + " 1. Reihe, es sind aber derzeit nur " + MR.getAttackLevelReihe1() + " vor Ort, es fehlen " + countRequestMovers;
    			infoComments.add(infoComment);
    			// ok, wir k�nnten �berrannt werden, wir m�ssen mehr erste Reihe aus der N�he dazuziehen - oberste Prio - Lebensgefahr
    			// Liste der noch verf�gbaren J�ger bauen
    			ArrayList<Jagemonster> availJM = new ArrayList<Jagemonster>(0);
    			for (Jagemonster JM:this.J�ger) {
    				if (!JM.isHC_ready() && JM.getRole()==Jagemonster.role_AttackFront && JM.getUnit().getModifiedPersons()>0) {
	    				GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc",false);
	    				if (GI.getAnzRunden()>0) {
		    				JM.setHC_weeks2target(GI.getAnzRunden());
		    				availJM.add(JM);
	    				}
    				}
    			}
    			Collections.sort(availJM, new MJ_Mover_Comparator());
    			for (Jagemonster JM:availJM) {
    				infoComment = "MJM_HC: Beauftrage " + JM.toString() + " (" + JM.getUnit().getModifiedPersons() + "Mann) mit Verlegung nach " + r.toString() + ", ETA: " + JM.getHC_weeks2target() + " Wochen.";
    				infoComments.add(infoComment);
    				// wir sollen zum Ziel
    				JM.moveTo(r.getCoordinate(), MonsterJagdManager_MJM.MAPLINE_ATTACK_TAG);
    				// JM als bearbeitet setzen
    				JM.setHC_ready(true);
    				// ben�tigte Anzahl Reduzieren
    				countRequestMovers -= JM.getUnit().getModifiedPersons();
    				countAttacker += JM.getUnit().getModifiedPersons();
    				infoComment = "MJM_HC: noch ausstehender Bedarf f�r die 1. Reihe: " + countRequestMovers;
    				infoComments.add(infoComment);
    				// zur Info erg�nzen
    				if (!infoCommentsReceiver.contains(JM)) {
    					infoCommentsReceiver.add(JM);
    				}
    				
    				// Nachvollziehbarkeit
    				if (!orderedMoveToRegions.contains(r)) {
    					orderedMoveToRegions.add(r);
    				}
    				
    				
    				// Abbrechen wenn ben�tigte Anzahl erreicht
    				if (countRequestMovers<=0) {
    					infoComment = "MJM_HC: Abbruch der Verst�rkungssuche 1. Reihe f�r " + r.toString();
    					infoComments.add(infoComment);
    					break;
    				}
    			}
    		} else {
    			// es gibt kein Problem in der Region?
    			infoComment = "MJM_HC: kein OVERRUN in " + r.toString() + ", keine Verst�rkung notwendig";
				infoComments.add(infoComment);
    		}
    		// Information an alle Verst�rker...
    		// neu: an alle
			for (Jagemonster JM:this.J�ger) {
				for (String s:infoComments) {
					JM.addComment(s);
				}
			}
    		
    		
    		
    		// check Gesamtanzahl...wieviel brauchen wir mindestens?
    		
    		infoComments = new ArrayList<String>(0);
    		infoCommentsReceiver = new ArrayList<Jagemonster>(0);
    		neededPersons = MR.getThreadLevel();
    		countRequestMovers = neededPersons - (MR.getAttackLevelReihe2() + countAttacker);
    		if (countRequestMovers>0) {
    			infoComment = "MJM_HC: Die Situation in " + r.toString() + " erfordert " + neededPersons + " K�mpfer, es fehlen " + countRequestMovers + " vor Ort.";
    			infoComments.add(infoComment);
    			// ok, wir m�ssen mehr hinschicken, egal welche Reihe
    			// Liste der noch verf�gbaren J�ger bauen
    			ArrayList<Jagemonster> availJM = new ArrayList<Jagemonster>(0);
    			for (Jagemonster JM:this.J�ger) {
    				if (!JM.isHC_ready() && (JM.getRole()==Jagemonster.role_AttackFront || JM.getRole()==Jagemonster.role_AttackBack )  && JM.getUnit().getModifiedPersons()>0) {
	    				GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc", false);
	    				if (GI.getAnzRunden()>0) {
		    				JM.setHC_weeks2target(GI.getAnzRunden());
		    				availJM.add(JM);
	    				}
    				}
    			}
    			Collections.sort(availJM, new MJ_Mover_Comparator());
    			for (Jagemonster JM:availJM) {
    				infoComment = "MJM_HC: Beauftrage " + JM.toString() + " (" + JM.getUnit().getModifiedPersons() + "Mann) mit Verlegung nach " + r.toString() + ", ETA: " + JM.getHC_weeks2target() + " Wochen.";
    				infoComments.add(infoComment);
    				// wir sollen zum Ziel
    				JM.moveTo(r.getCoordinate(), MonsterJagdManager_MJM.MAPLINE_ATTACK_TAG);
    				// JM als bearbeitet setzen
    				JM.setHC_ready(true);
    				// ben�tigte Anzahl Reduzieren
    				countRequestMovers -= JM.getUnit().getModifiedPersons();
    				infoComment = "MJM_HC: noch ausstehender Bedarf: " + countRequestMovers;
    				infoComments.add(infoComment);
    				// zur Info erg�nzen
    				if (!infoCommentsReceiver.contains(JM)) {
    					infoCommentsReceiver.add(JM);
    				}
    				// Nachvollziehbarkeit
    				if (!orderedMoveToRegions.contains(r)) {
    					orderedMoveToRegions.add(r);
    				}
    				// Abbrechen wenn ben�tigte Anzahl erreicht
    				if (countRequestMovers<=0) {
    					infoComment = "MJM_HC: Abbruch der Verst�rkungssuche f�r " + r.toString();
    					infoComments.add(infoComment);
    					break;
    				}
    			}
    		} else {
    			infoComment = "MJM_HC: Die Gesamtanzahl der K�mpfer ist ausreichend in " + r.toString();
    			infoComments.add(infoComment);
    		}
    		// Information an alle Verst�rker...
    		// neu: an alle
    		
			for (Jagemonster JM:this.J�ger) {
				for (String s:infoComments) {
					JM.addComment(s);
				}
			}
    		
    		
    		
    	    // check Taktiker?
    		if (MR.needTactican() && !MR.hasTactican()) {
	    		infoComments = new ArrayList<String>(0);
	    		infoCommentsReceiver = new ArrayList<Jagemonster>(0);
	    		neededPersons = 1;
	    		countRequestMovers = 1;
	    		if (countRequestMovers>0) {
	    			infoComment = "MJM_HC: Die Situation in " + r.toString() + " erfordert 1 Taktiker, dieser wird gesucht.";
	    			infoComments.add(infoComment);
	    			// ok, wir m�ssen mehr hinschicken, nur Taktiker, nur 1
	    			// Liste der noch verf�gbaren J�ger bauen
	    			ArrayList<Jagemonster> availJM = new ArrayList<Jagemonster>(0);
	    			for (Jagemonster JM:this.J�ger) {
	    				if (!JM.isHC_ready() && JM.isTactican()  && JM.getUnit().getModifiedPersons()>0) {
		    				GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc", false);
		    				if (GI.getAnzRunden()>0) {
			    				JM.setHC_weeks2target(GI.getAnzRunden());
			    				availJM.add(JM);
		    				}
	    				}
	    			}
	    			Collections.sort(availJM, new MJ_Mover_Comparator());
	    			for (Jagemonster JM:availJM) {
	    				infoComment = "MJM_HC: Beauftrage " + JM.toString() + "  mit Verlegung nach " + r.toString() + ", ETA: " + JM.getHC_weeks2target() + " Wochen.";
	    				infoComments.add(infoComment);
	    				// wir sollen zum Ziel
	    				JM.moveTo(r.getCoordinate(), MonsterJagdManager_MJM.MAPLINE_ATTACK_TAG);
	    				// JM als bearbeitet setzen
	    				JM.setHC_ready(true);
	    				// ben�tigte Anzahl Reduzieren
	    				countRequestMovers -= JM.getUnit().getModifiedPersons();
	    				infoComment = "MJM_HC: noch ausstehender Bedarf: " + countRequestMovers;
	    				infoComments.add(infoComment);
	    				// zur Info erg�nzen
	    				if (!infoCommentsReceiver.contains(JM)) {
	    					infoCommentsReceiver.add(JM);
	    				}
	    				// Abbrechen wenn ben�tigte Anzahl erreicht
	    				if (countRequestMovers<=0) {
	    					infoComment = "MJM_HC: Abbruch der Taktikersuche f�r " + r.toString();
	    					break;
	    				}
	    			}
	    		}
	    		// Information an alle Verst�rker...
	    		// neu, jetzt alle
	    		
    			for (Jagemonster JM:this.J�ger) {
    				for (String s:infoComments) {
    					JM.addComment(s);
    				}
    			}
	    		
    		}
    		
    		
    		// check Support?
    		// woher wei� ich denn, ob support ben�tigt wird?
    		// im Zweifel mitschicken
    		// Standard ist Wahrnehmer - dabei wichtig: gleiche Partei wie 1. und 2. Reihe.
    		// besser: einfach folgen lassen ;-)
    		
    		
    		// warnung wenn nicht genug verst�rkung im TA gefunden worden ist!!
    		
    		
    		
    	}
    	
    	// Phase 3 - Monsterregionen durchgehen, die noch nicht angegriffen werden, und dort Leute hinschicken
    	boolean TA_with_to_much_Monsters = false;
    	for (MonsterRegion MR:this.monsterregions.values()) {
    		if (!MR.isScheduled4attack() && MR.getThreadLevel()>0) {
    			work_on_MR(MR);
    			if (MR.getThreadLevel()>MR.getAttackLevelReihe1() || (MR.needTactican() && !MR.hasTactican())) {
    				// Problem, Monster k�nnen nicht bek�mpft werden
    				// alle J�ger hart informieren
    				TA_with_to_much_Monsters=true;
    			}
    		}
    	}
    	
    	if (TA_with_to_much_Monsters) {
    		for (Jagemonster JM:this.J�ger) {
    			JM.addComment("!!! Im TA k�nnen nicht alle Monster bek�mpft weren!!! Liste:");
    			for (MonsterRegion MR:this.monsterregions.values()) {
	    			if (MR.getThreadLevel()>MR.getAttackLevelReihe1() || (MR.needTactican() && !MR.hasTactican())) {
	    				JM.addComment("Problem in " + MR.toString());
    	    		}
    	    	}
    			JM.doNotConfirmOrders("Im TA k�nnen nicht alle Monster bek�mpft werden!");
    		}
    	}
    	
    	
    	// Phase 4
    	// JM, die nicht angreifen und nicht zu Hause sind, nach Hause schicken
    	for(Jagemonster JM:this.J�ger) {
    		if (!JM.isReady4AttackThisWeek() && JM.getTargetDest()==null) {
    			// sind wir zu Hause?
    			if (JM.getHomeDest()!=null && !JM.getHomeDest().equals(JM.scriptUnit.getUnit().getRegion().getCoordinate())) {
    				// Nach Hause gehen
    				JM.addComment("MJM_HC: RTB");
    				JM.moveTo(JM.getHomeDest(), MonsterJagdManager_MJM.MAPLINE_MOVE_TAG);
    			} else {
    				// wir haben kein HOME oder sind da
    				JM.addComment("MJM_HC: kein Einsatz - aber bereit (Lerne)");
    				JM.AttackLerne();
    			}
    			// JM als bearbeitet setzen
				JM.setHC_ready(true);
    		}
    	}
    	
    }
    
    
    private void work_on_MR(MonsterRegion MR) {
    	// Monsterj�ger nach Entfernung und Einheitengr�sse sortieren und hinschicken
    	// 1. Reihe - nur !
    	String infoComment;
    	ArrayList<String> infoComments = new ArrayList<String>(0);
    	ArrayList<Jagemonster> availJM = new ArrayList<Jagemonster>(0);
    	ArrayList<Jagemonster> infoCommentsReceiver = new ArrayList<Jagemonster>(0);
    	Region r = MR.getRegion();
		for (Jagemonster JM:this.J�ger) {
			if (!JM.isHC_ready() && JM.getUnit().getModifiedPersons()>0 && JM.getRole()==Jagemonster.role_AttackFront && JM.getTargetDest()==null) {
				GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc",false);
				if (GI.getAnzRunden()>0) {
    				JM.setHC_weeks2target(GI.getAnzRunden());
    				availJM.add(JM);
				}
			}
		}
		Collections.sort(availJM, new MJ_Mover_Comparator());
		for (Jagemonster JM:availJM) {
			// Hinschicken
			infoComment = "MJM_HC: Beauftrage " + JM.toString() + "  mit Verlegung nach " + r.toString() + ", ETA: " + JM.getHC_weeks2target() + " Wochen.";
			infoComments.add(infoComment);
			// wir sollen zum Ziel
			JM.moveTo(r.getCoordinate(), MonsterJagdManager_MJM.MAPLINE_ATTACK_TAG);
			// JM als bearbeitet setzen
			JM.setHC_ready(true);
			// ben�tigte Anzahl Reduzieren
			MR.addAttackLevelReihe1(JM.getUnit().getModifiedPersons());
			infoComment = "MJM_HC: noch ausstehender Bedarf: " + (MR.getThreadLevel() - MR.getAttackLevelReihe1());
			infoComments.add(infoComment);
			// zur Info erg�nzen
			if (!infoCommentsReceiver.contains(JM)) {
				infoCommentsReceiver.add(JM);
			}
			
			// Nachvollziehbarkeit
			if (!orderedMoveToRegions.contains(r)) {
				orderedMoveToRegions.add(r);
			}
			
			// Abbrechen wenn ben�tigte Anzahl erreicht
			if (MR.getThreadLevel()<=MR.getAttackLevelReihe1()) {
				infoComment = "MJM_HC: Abbruch der Siche 1. Reihe f�r " + r.toString();
				break;
			}
		}
		
		
		// Taktiker?
		if (MR.needTactican()) {
			

	    	availJM = new ArrayList<Jagemonster>(0);
			for (Jagemonster JM:this.J�ger) {
				if (!JM.isHC_ready() && JM.getUnit().getModifiedPersons()>0 && JM.isTactican() && JM.getTargetDest()==null) {
					GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc",false);
					if (GI.getAnzRunden()>0) {
	    				JM.setHC_weeks2target(GI.getAnzRunden());
	    				availJM.add(JM);
					}
				}
			}
			Collections.sort(availJM, new MJ_Mover_Comparator());
			for (Jagemonster JM:availJM) {
				// Hinschicken
				infoComment = "MJM_HC (Taktik): Beauftrage " + JM.toString() + "  mit Verlegung nach " + r.toString() + ", ETA: " + JM.getHC_weeks2target() + " Wochen.";
				infoComments.add(infoComment);
				// wir sollen zum Ziel
				JM.moveTo(r.getCoordinate(), MonsterJagdManager_MJM.MAPLINE_ATTACK_TAG);
				// JM als bearbeitet setzen
				JM.setHC_ready(true);
				// ben�tigte Anzahl Reduzieren
				MR.setHasTactican(true);
				// zur Info erg�nzen
				if (!infoCommentsReceiver.contains(JM)) {
					infoCommentsReceiver.add(JM);
				}
				break;
			}
		}
		
		
		// Information an alle Verst�rker...
		if (infoCommentsReceiver.size()>0 && infoComments.size()>0) {
			for (Jagemonster JM:infoCommentsReceiver) {
				for (String s:infoComments) {
					JM.addComment(s);
				}
			}
		}
		
    }
    
    public boolean isRegionMovedTo(Region r) {
    	if (this.orderedMoveToRegions.contains(r)) {
    		return true;
    	}
    	return false;
    }
    
}
