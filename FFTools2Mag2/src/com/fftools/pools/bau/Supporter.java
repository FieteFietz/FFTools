package com.fftools.pools.bau;

import com.fftools.scripts.Bauen;

public class Supporter {
	private int ETA=-1;
	public int getETA() {
		return ETA;
	}
	public void setETA(int eTA) {
		ETA = eTA;
	}
	public int getLevels() {
		return levels;
	}
	public void setLevels(int levels) {
		this.levels = levels;
	}
	private int levels=0;
	
	private Bauen bauen;
	public Bauen getBauen() {
		return bauen;
	}
	public void setBauen(Bauen bauen) {
		this.bauen = bauen;
	}
	
	
}
