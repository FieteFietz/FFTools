package com.fftools.pools.seeschlangen;

import magellan.library.Region;

public class MonsterRegion implements Comparable<MonsterRegion>{

	
	private int threadLevel=0;
	private int attackLevelReihe1 = 0;
	private int attackLevelReihe2 = 0;
	private boolean needTactican = false;
	private boolean hasTactican = false;
	private Region r = null;
	
	private boolean scheduled4attack = false;
	
	
	public boolean needTactican() {
		return needTactican;
	}
	public Region getRegion() {
		return this.r;
	}
	
	public MonsterRegion(Region r, int threadLevel) {
		this(r, threadLevel, false);
	}
	
	public MonsterRegion(Region r, int threadLevel, boolean needTactican) {	
		this.r = r;
		this.threadLevel = threadLevel;
		this.needTactican = needTactican;
	}	
	
	public String toString() {
		String erg = "MonsterRegion: " + this.r.toString() + ": Bedrohung=" + this.threadLevel;
		if (this.needTactican) {
			erg += " (erfordert Taktiker)";
		}
		return erg;
	}
	
	public void addThread(int addT) {
		this.threadLevel += addT;
	}

	
	
	
	public int compareTo(MonsterRegion pS) {
		if (pS.threadLevel > this.threadLevel) {
			return 1;
		}
		
		if (pS.threadLevel < this.threadLevel) {
			return -1;
		}
		return 0;
	}
	
	public void setNeedTactican(boolean needTactican) {
		this.needTactican = needTactican;
	}
	public int getThreadLevel() {
		return threadLevel;
	}
	public boolean isScheduled4attack() {
		return scheduled4attack;
	}
	public void setScheduled4attack(boolean scheduled4attack) {
		this.scheduled4attack = scheduled4attack;
	}
	public int getAttackLevelReihe1() {
		return attackLevelReihe1;
	}
	public void setAttackLevelReihe1(int attackLevel) {
		this.attackLevelReihe1 = attackLevel;
	}
	
	public void addAttackLevelReihe1(int addLevel) {
		this.attackLevelReihe1 += addLevel;
	}
	
	public int getAttackLevelReihe2() {
		return attackLevelReihe2;
	}
	public void setAttackLevelReihe2(int attackLevel) {
		this.attackLevelReihe2 = attackLevel;
	}
	
	public void addAttackLevelReihe2(int addLevel) {
		this.attackLevelReihe2 += addLevel;
	}
	
	
	public boolean hasTactican() {
		return hasTactican;
	}
	public void setHasTactican(boolean hasTactican) {
		this.hasTactican = hasTactican;
	}
	
	
	
}
