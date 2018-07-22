package com.fftools.utils;

import magellan.library.Region;

/**
 * enthält ergebnisse einer makeOrderNach - Anweisung
 * @author Fiete
 *
 */


public class GotoInfo {
	private int AnzRunden = -1;
	private String Path = null;
	private Region nextHold = null;
	private Region destRegion = null;
	/**
	 * Index: Planungsrunde, 0 = jetzt
	 * RegionPathelement: geplante strecke in der Planungsrunde
	 */
	private RegionPathElement[] pathRegions = null;
	
	/**
	 * @return the anzRunden
	 */
	public int getAnzRunden() {
		return AnzRunden;
	}
	/**
	 * @param anzRunden the anzRunden to set
	 */
	public void setAnzRunden(int anzRunden) {
		AnzRunden = anzRunden;
	}
	/**
	 * @return the nextHold
	 */
	public Region getNextHold() {
		return nextHold;
	}
	/**
	 * @param nextHold the nextHold to set
	 */
	public void setNextHold(Region nextHold) {
		this.nextHold = nextHold;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return Path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		Path = path;
	}
	
	public void setPathElement(int PlanungsRunde, Region von, Region nach){
		if (PlanungsRunde>19){
			return;
		}
		if (this.pathRegions==null){
			this.pathRegions = new RegionPathElement[20];
		}
		
		if (this.pathRegions[PlanungsRunde]==null){
			this.pathRegions[PlanungsRunde] = new RegionPathElement();
		}
		
		this.pathRegions[PlanungsRunde].setVon_Region(von);
		this.pathRegions[PlanungsRunde].setNach_Region(nach);
	}
	
	public RegionPathElement getPathElement(int Planungsrunde){
		RegionPathElement erg = null;
		if (this.pathRegions==null){
			return null;
		}
		erg = this.pathRegions[Planungsrunde];
		return erg;
	}
	/**
	 * @return the destRegion
	 */
	public Region getDestRegion() {
		return destRegion;
	}
	/**
	 * @param destRegion the destRegion to set
	 */
	public void setDestRegion(Region destRegion) {
		this.destRegion = destRegion;
	}
	
}
