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
	private ArrayList<Region> NOattackRegions = new ArrayList<Region>(0);
	private ArrayList<Region> needTacticRegions = new ArrayList<Region>(0);
	
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
    			String text = JM.toString() + " in " + JM.getUnit().getRegion().toString() + ", " + JM.getUnit().getModifiedPersons() + " Pers";
    			if (JM.isTactican()) {
    				text += " (Taktiker)";
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
    		ArrayList<Jagemonster> infoCommentsReceiver = new ArrayList<Jagemonster>(0);
    		int countAttacker = MR.getAttackLevelReihe1();
    		int countRequestMovers = neededPersons - MR.getAttackLevelReihe1();
    		if (countRequestMovers>0) {
    			String infoComment = "MJM_HC: Die Situation in " + r.toString() + " erfordert " + neededPersons + " 1. Reihe, es sind aber derzeit nur " + MR.getAttackLevelReihe1() + " vor Ort.";
    			infoComments.add(infoComment);
    			// ok, wir k�nnten �berrannt werden, wir m�ssen mehr erste Reihe aus der N�he dazuziehen - oberste Prio - Lebensgefahr
    			// Liste der noch verf�gbaren J�ger bauen
    			ArrayList<Jagemonster> availJM = new ArrayList<Jagemonster>(0);
    			for (Jagemonster JM:this.J�ger) {
    				if (!JM.isHC_ready() && JM.getRole()==Jagemonster.role_AttackFront && JM.getUnit().getModifiedPersons()>0) {
	    				GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc");
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
    				// Abbrechen wenn ben�tigte Anzahl erreicht
    				if (countRequestMovers<=0) {
    					infoComment = "MJM_HC: Abbruch der Verst�rkungssuche 1. Reihe f�r " + r.toString();
    					break;
    				}
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
    		
    		
    		// check Gesamtanzahl...wieviel brauchen wir mindestens?
    		
    		infoComments = new ArrayList<String>(0);
    		infoCommentsReceiver = new ArrayList<Jagemonster>(0);
    		neededPersons = MR.getThreadLevel();
    		countRequestMovers = neededPersons - (MR.getAttackLevelReihe1() + MR.getAttackLevelReihe2() + countAttacker);
    		if (countRequestMovers>0) {
    			String infoComment = "MJM_HC: Die Situation in " + r.toString() + " erfordert " + neededPersons + " K�mpfer, es fehlen " + countRequestMovers + " vor Ort.";
    			infoComments.add(infoComment);
    			// ok, wir m�ssen mehr hinschicken, egal welche Reihe
    			// Liste der noch verf�gbaren J�ger bauen
    			ArrayList<Jagemonster> availJM = new ArrayList<Jagemonster>(0);
    			for (Jagemonster JM:this.J�ger) {
    				if (!JM.isHC_ready() && (JM.getRole()==Jagemonster.role_AttackFront || JM.getRole()==Jagemonster.role_AttackBack )  && JM.getUnit().getModifiedPersons()>0) {
	    				GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc");
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
    				// Abbrechen wenn ben�tigte Anzahl erreicht
    				if (countRequestMovers<=0) {
    					infoComment = "MJM_HC: Abbruch der Verst�rkungssuche f�r " + r.toString();
    					break;
    				}
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
    		
    		
    	    // check Taktiker?
    		if (MR.needTactican() && !MR.hasTactican()) {
	    		infoComments = new ArrayList<String>(0);
	    		infoCommentsReceiver = new ArrayList<Jagemonster>(0);
	    		neededPersons = 1;
	    		countRequestMovers = 1;
	    		if (countRequestMovers>0) {
	    			String infoComment = "MJM_HC: Die Situation in " + r.toString() + " erfordert 1 Taktiker, dieser wird gesucht.";
	    			infoComments.add(infoComment);
	    			// ok, wir m�ssen mehr hinschicken, nur Taktiker, nur 1
	    			// Liste der noch verf�gbaren J�ger bauen
	    			ArrayList<Jagemonster> availJM = new ArrayList<Jagemonster>(0);
	    			for (Jagemonster JM:this.J�ger) {
	    				if (!JM.isHC_ready() && JM.isTactican()  && JM.getUnit().getModifiedPersons()>0) {
		    				GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc");
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
	    		if (infoCommentsReceiver.size()>0 && infoComments.size()>0) {
	    			for (Jagemonster JM:infoCommentsReceiver) {
	    				for (String s:infoComments) {
	    					JM.addComment(s);
	    				}
	    			}
	    		}
    		}
    		
    		
    		// check Support?
    		if (MR.needTactican() && !MR.hasTactican()) {
	    		infoComments = new ArrayList<String>(0);
	    		infoCommentsReceiver = new ArrayList<Jagemonster>(0);
	    		neededPersons = 1;
	    		countRequestMovers = 1;
	    		if (countRequestMovers>0) {
	    			String infoComment = "MJM_HC: Die Situation in " + r.toString() + " erfordert 1 Taktiker, dieser wird gesucht.";
	    			infoComments.add(infoComment);
	    			// ok, wir m�ssen mehr hinschicken, nur Taktiker, nur 1
	    			// Liste der noch verf�gbaren J�ger bauen
	    			ArrayList<Jagemonster> availJM = new ArrayList<Jagemonster>(0);
	    			for (Jagemonster JM:this.J�ger) {
	    				if (!JM.isHC_ready() && JM.isTactican()  && JM.getUnit().getModifiedPersons()>0) {
		    				GotoInfo GI = FFToolsRegions.makeOrderNACH(JM.scriptUnit, JM.getUnit().getRegion().getCoordinate(), r.getCoordinate(),false,"MJM_HC calc");
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
	    		if (infoCommentsReceiver.size()>0 && infoComments.size()>0) {
	    			for (Jagemonster JM:infoCommentsReceiver) {
	    				for (String s:infoComments) {
	    					JM.addComment(s);
	    				}
	    			}
	    		}
    		}
    		
    		
    		
    		
    		
    		
    	}
    }
}
