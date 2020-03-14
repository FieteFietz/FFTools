package com.fftools.utils;

import magellan.library.CoordinateID;
import magellan.library.GameData;

/**
 * for caching the calls to FfTools.getPathDistLand 
 * @author Fiete
 *
 */
public class PathDistLandInfo {
	private GameData data=null;
	private CoordinateID von = null;
	private CoordinateID nach = null;
	private boolean Insekten=false;
	
	public boolean isInsekten() {
		return Insekten;
	}

	/**
	 * Konstruktor
	 * @param data
	 * @param von
	 * @param nach
	 */
	public PathDistLandInfo(GameData data, CoordinateID von, CoordinateID nach, boolean _Insekten) {
		super();
		this.data = data;
		this.von = von;
		this.nach = nach;
		this.Insekten = _Insekten;
	}
	
	public boolean equals(Object o){
		boolean retVal=true;
		if (!(o instanceof PathDistLandInfo)){
			return false;
		}
		PathDistLandInfo other = (PathDistLandInfo)o;
		if (!other.getData().equals(this.data)){
			return false;
		}
		if (!other.getVon().equals(this.von)){
			return false;
		}
		if (!other.getNach().equals(this.nach)){
			return false;
		}
		if (this.Insekten!=other.isInsekten()) {
			return false;
		}
		
		return retVal;
	}
	
	public boolean is(GameData _data, CoordinateID _von,CoordinateID _nach, boolean _Insekten){
		boolean retVal=true;

		if (!_data.equals(this.data)){
			return false;
		}
		if (!_von.equals(this.von)){
			return false;
		}
		if (!_nach.equals(this.nach)){
			return false;
		}
		
		if (!_Insekten==this.Insekten){
			return false;
		}
		
		return retVal;
	}

	/**
	 * @return the data
	 */
	public GameData getData() {
		return data;
	}

	/**
	 * @return the nach
	 */
	public CoordinateID getNach() {
		return nach;
	}

	/**
	 * @return the von
	 */
	public CoordinateID getVon() {
		return von;
	}
	
}
