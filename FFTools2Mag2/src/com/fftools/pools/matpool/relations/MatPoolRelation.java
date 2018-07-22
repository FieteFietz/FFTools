package com.fftools.pools.matpool.relations;

import magellan.library.Region;
import magellan.library.Unit;

import com.fftools.ScriptUnit;

public class MatPoolRelation {
	
	/**
	 * Davon bereits durch Requests abgerufen
	 */
	private int bearbeitet = 0;
	
	/**
	 * doe scriptunit
	 */
	private ScriptUnit scriptUnit = null;

	
	
	/**
	 * liefert die ScriptUnit zu dieser Offer
	 */
	public ScriptUnit getScriptUnit(){
		return this.scriptUnit;
	}
	
	/**
	 * liefert die Unit zu dieser Offer
	 * @return
	 */
	public Unit getUnit(){
		return this.getScriptUnit().getUnit();
	}
	
	/**
	 * liefert die Region zu dieser Offer
	 */
	public Region getRegion(){
		return this.getUnit().getRegion();
	}



	/**
	 * @return the bearbeitet
	 */
	public int getBearbeitet() {
		return bearbeitet;
	}

	/**
	 * @param bearbeitet the bearbeitet to set
	 */
	public void setBearbeitet(int _bearbeitet) {
		this.bearbeitet = _bearbeitet;
	}
	
	/**
	 * reset des MatPools - Zustand herstellen, als wenn noch kein
	 * MatPool Zugriff auf die Relation gehabt hätte
	 */
	public void reset(){
		this.bearbeitet = 0;
	}

	/**
	 * @param scriptUnit the scriptUnit to set
	 */
	public void setScriptUnit(ScriptUnit scriptUnit) {
		this.scriptUnit = scriptUnit;
	}
	
}
