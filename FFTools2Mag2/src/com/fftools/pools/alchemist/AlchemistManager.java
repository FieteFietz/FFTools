package com.fftools.pools.alchemist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import magellan.library.ID;
import magellan.library.Region;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Alchemist;

/**
 * Verwaltet Objekte vom Typ <code>Alchitrank</code> und <code>Alchimist</code>
 * steuert die Aufgaben der Alchimisten
 * @author Fiete
 *
 */
public class AlchemistManager implements OverlordRun,OverlordInfo {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	// einfach nach dem ersten MP
	// vor dem ersten MP sollte sich KrautDepot die Kräuter organisieren
	private static final int Durchlauf = 345;
	
	private int[] runners = {Durchlauf};
	
	/**
	 * Map der Pools. Key sind Regionen vom Typ <code>Region</code>
	 * Values sind Objecte <code>AlchemistPool</code>
	 */
	private HashMap<Region, AlchemistPool> pools = null;
	
	/**
	 * HashMap der Reportweiten Tränke
	 */
	private HashMap<ID, AlchemistTrank> potions = null; 
	
	/**
	 * Referenz auf den Overlord
	 */
	private Overlord overlord = null;
	
	
	/**
	 * Konstruktor
	 *
	 */
	public AlchemistManager(Overlord _overlord){
		this.overlord = _overlord;
	}
	
	/**
	 * liefert zu einer Region den Alchemistenpool
	 * @param r
	 * @return
	 */
	private AlchemistPool getPool(Region r){
		if (this.pools==null){
			this.pools = new HashMap<Region, AlchemistPool>();
		}
		if (this.pools.containsKey(r)){
			return this.pools.get(r);
		}
		
		// neu anlegen
		AlchemistPool AP = new AlchemistPool(this,r);
		this.pools.put(r, AP);
		
		return AP;
	}
	
	/**
	 * liefert zu einer Scriptunit den Alchemistenpool
	 * @param u
	 * @return
	 */
	private AlchemistPool getPool(ScriptUnit u){
		return this.getPool(u.getUnit().getRegion());
	}
	
	
	
	/**
	 * Fügt einen Trank dem System hinzu, inkl order
	 * wenn als Regional definiert, der entsprechenden Region
	 * ansonsten dem gesamten Report
	 * @param u
	 * @param args
	 */
	public void addTrankOrder(ScriptUnit u,ArrayList<String> args){
		AlchemistTrank trank = new AlchemistTrank(u,args);
		if (trank.getPotion()==null || trank.getRang()==AlchemistTrank.RANG_UNDEF){
			// unvollständige nicht hinzufügen
			return;
		}
		if (trank.getRegion()==null){
			// reportweit hinzufügen
			this.addAlchemistTrank(trank);
		} else {
			// in der Region hinzufügen
			this.getPool(u).addAlchemistTrank(trank);
		}
	}
	
	/**
	 * fügt einen definierten Trank den reportweiten tränken hinzu
	 * @param trank
	 */
	private void addAlchemistTrank(AlchemistTrank trank){
		if (trank.getPotion()==null){
			return;
		}
		if (this.potions==null){
			this.potions= new HashMap<ID, AlchemistTrank>();
		}
		this.potions.put(trank.getPotion().getID(),trank);
	}
	
	/**
	 * setzt das übergebene alchi-script als KrautDepot im
	 * entsprechenden Pool, falls dort noch keines ist
	 * wenn schon besetzt, Fehlermeldung
	 * @param u
	 */
	public void addKrautDepot(Alchemist alchi){
		AlchemistPool actPool = this.getPool(alchi.region());
		if (actPool.krautDepot==null){
			actPool.krautDepot = alchi;
		} else {
			// schon besetzt!
			alchi.addComment("!!!kann nicht als KrautDepot verwendet werden. Krautdepot ist bereits: " + actPool.krautDepot.unitDesc());
			alchi.scriptUnit.doNotConfirmOrders();
			outText.addOutLine("!!!doppelter Eintrag als KrautDepot: " + alchi.unitDesc(), true);
		}
	}
	
	/**
	 * ergänzt den alchi im entsprechenden pool
	 * @param alchi
	 */
	public void addAlchemist(Alchemist alchi){
		AlchemistPool actPool = this.getPool(alchi.region());
		actPool.addAlchemist(alchi);
	}
	
	
	/**
	 * startet den AlchemistenManager
	 *
	 */
	public void run(int durchlauf){
		if (this.pools==null || this.pools.size()==0){
			// nix zu tun
			return;
		}
		for (Iterator<AlchemistPool> iter = this.pools.values().iterator();iter.hasNext();){
			AlchemistPool actPool = (AlchemistPool)iter.next();
			actPool.run();
		}
	}
	
	
	/**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }

	/**
	 * @return the potions
	 */
	public HashMap<ID, AlchemistTrank> getPotions() {
		return potions;
	}

	/**
	 * @return the overlord
	 */
	public Overlord getOverlord() {
		return overlord;
	}
	
	
}
