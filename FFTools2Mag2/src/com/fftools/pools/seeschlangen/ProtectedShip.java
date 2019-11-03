package com.fftools.pools.seeschlangen;

import com.fftools.ScriptUnit;
import com.fftools.scripts.Seeschlangenjagd;

import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.Unit;
import magellan.library.rules.ItemType;

public class ProtectedShip implements Comparable<ProtectedShip>{
	public Ship s = null;
	public ScriptUnit captn = null;
	public Region nextShipStop = null;
	public Seeschlangenjagd SJ = null;
	
	public int Silber = 0;
	public int Personen = 0;

	
	
	
	public int compareTo(ProtectedShip pS) {
		// TODO Auto-generated method stub
		
		if (pS.Silber > this.Silber) {
			return 1;
		}
		
		if (pS.Silber < this.Silber) {
			return -1;
		}
		
		if (pS.Personen > this.Personen) {
			return 1;
		}
		
		if (pS.Personen < this.Personen) {
			return -1;
		}
		
		if (pS.s.getModifiedLoad()>this.s.getModifiedLoad()) {
			return 1;
		}
		if (pS.s.getModifiedLoad()<this.s.getModifiedLoad()) {
			return -1;
		}
		return 0;
	}
	
	
	public void calcShip() {
		// Silber und Personen
		this.Silber=0;
		this.Personen=0;
		ItemType silverType=this.captn.getScriptMain().gd_ScriptMain.getRules().getItemType("Silber",false);
		if (silverType==null) {
			return;
		}
		
		
		for (Unit u:this.s.getUnits().values()) {
			ScriptUnit su = this.captn.getScriptMain().getScriptUnit(u);
			if (su!=null) {
				Item i = su.getModifiedItem(silverType);
				if (i!=null) {
					this.Silber += i.getAmount();
				}
			}
			this.Personen += u.getModifiedPersons();
		}
	}
	
}
