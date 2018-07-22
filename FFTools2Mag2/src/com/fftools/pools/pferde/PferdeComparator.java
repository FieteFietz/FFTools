package com.fftools.pools.pferde;

import java.util.Comparator;

import com.fftools.scripts.Pferde;

public class PferdeComparator implements Comparator<Pferde> {
	
	private int zuFangen = 0;

	
	public PferdeComparator(){
		// nix zu setzen
	}
	
	/**
	 * Vergleicht 2 PferdeMacher je nach Abstand zur SollFangZahl
	 */
	public int compare(Pferde pferde1,Pferde pferde2){
		
		
		int dist1 =  Math.abs(pferde1.maxMachenPferde() - zuFangen);
		int dist2 =  Math.abs(pferde2.maxMachenPferde() - zuFangen);
		
		return dist1-dist2;
		
		
	}
	
	
	/**
	 * @param zuFangen the zuFangen to set
	 */
	public void setZuFangen(int zuFangen) {
		this.zuFangen = zuFangen;
	}
	
	
	

}
