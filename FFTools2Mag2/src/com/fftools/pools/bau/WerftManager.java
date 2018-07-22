package com.fftools.pools.bau;


import java.util.Hashtable;

import magellan.library.Region;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Werft;



/**
 *Verwaltet Werft-scripte
 *
 */
public class WerftManager implements OverlordRun,OverlordInfo{
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	public static final int Durchlauf0 = 220;
	public static final int Durchlauf1 = 650;
	
	
	private int[] runners = {Durchlauf0,Durchlauf1};
	
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
