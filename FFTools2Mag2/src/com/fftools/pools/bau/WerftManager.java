package com.fftools.pools.bau;


import java.util.Hashtable;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Werft;
import com.fftools.scripts.Werftsegler;

import magellan.library.Region;
import magellan.library.Ship;



/**
 *Verwaltet Werft-scripte
 *
 */
public class WerftManager implements OverlordRun,OverlordInfo{
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	// 212 läuft Werftsegler
	public static final int Durchlauf0 = 220;
	public static final int Durchlauf0_2 = 225;
	public static final int Durchlauf1 = 650;
	public static final int Durchlauf2 = 730;  // nach letztem MatPool
	
	
	private int[] runners = {Durchlauf0,Durchlauf0_2,Durchlauf1,Durchlauf2};
	
	public ScriptMain scriptMain = null;
	
	private Hashtable<Region,WerftPool> WerftPoolMap = new Hashtable<Region, WerftPool>();

	public WerftManager (ScriptMain _scriptMain){
		this.scriptMain = _scriptMain;	
	}
	
	public void informUs(){
		
	}
	
	
	public void run(int durchlauf){
		for (WerftPool wP:this.WerftPoolMap.values()){
			wP.runPool(durchlauf);
		}
	}
	
	/*
	 * sucht den passenden WerftPool bzw legt ihn an
	 * fügt dem werftpool das werft script hinzu
	 */
	public void addWerftUnit(Werft _w){
		Region actRegion = _w.scriptUnit.getUnit().getRegion();
		WerftPool actWerftPool;
		if (WerftPoolMap.containsKey(actRegion)){
			actWerftPool = WerftPoolMap.get(actRegion);
		} else {
			actWerftPool = new WerftPool(this, actRegion);
			WerftPoolMap.put(actRegion, actWerftPool);
		}
		actWerftPool.addWerft(_w);	
	}
	
	public void addWerftseglerUnit(Werftsegler _w, boolean as_Flottenkapitän){
		Region actRegion = _w.scriptUnit.getUnit().getRegion();
		WerftPool actWerftPool;
		if (WerftPoolMap.containsKey(actRegion)){
			actWerftPool = WerftPoolMap.get(actRegion);
		} else {
			actWerftPool = new WerftPool(this, actRegion);
			WerftPoolMap.put(actRegion, actWerftPool);
		}
		if (as_Flottenkapitän) {
			actWerftPool.addWerftseglerAsFlottenkapitän(_w);
		} else {
			actWerftPool.addWerftseglerAsHafenkapitän(_w);
		}
	}
	
	public void addFreiWerdendesSchiff(Werft _w, Ship _s){
		Region actRegion = _w.scriptUnit.getUnit().getRegion();
		WerftPool actWerftPool;
		if (WerftPoolMap.containsKey(actRegion)){
			actWerftPool = WerftPoolMap.get(actRegion);
		} else {
			actWerftPool = new WerftPool(this, actRegion);
			WerftPoolMap.put(actRegion, actWerftPool);
		}
		actWerftPool.addFreiWerdendesSchiff(_s);
	}
	
	/*
	 * sucht den passenden WerftPool bzw legt ihn an
	 * setzt die Eigenschaft allAShips
	 */
	public void setAllShips(Werft _w, boolean _allShips){
		Region actRegion = _w.scriptUnit.getUnit().getRegion();
		WerftPool actWerftPool;
		if (WerftPoolMap.containsKey(actRegion)){
			actWerftPool = WerftPoolMap.get(actRegion);
		} else {
			actWerftPool = new WerftPool(this, actRegion);
			WerftPoolMap.put(actRegion, actWerftPool);
		}
		actWerftPool.setAllShips(_allShips);
	}
	
	
	public int[] runAt(){
		return runners;
	}
	
}
