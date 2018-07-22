package com.fftools.utils;

import magellan.library.Region;

/**
 * enthält ein ausgearbeitetes Element eines Pfades
 * normalerweise innerhalb einer GoTo Info
 * @author Fiete
 *
 */


public class RegionPathElement {
	
	private Region von_Region = null;
	private Region nach_Region = null;
	
	/**
	 * @return the von_Region
	 */
	public Region getVon_Region() {
		return von_Region;
	}
	/**
	 * @param von_Region the von_Region to set
	 */
	public void setVon_Region(Region von_Region) {
		this.von_Region = von_Region;
	}
	/**
	 * @return the nach_Region
	 */
	public Region getNach_Region() {
		return nach_Region;
	}
	/**
	 * @param nach_Region the nach_Region to set
	 */
	public void setNach_Region(Region nach_Region) {
		this.nach_Region = nach_Region;
	}
	

}
