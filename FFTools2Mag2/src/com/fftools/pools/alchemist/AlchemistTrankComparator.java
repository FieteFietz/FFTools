package com.fftools.pools.alchemist;

import java.util.Comparator;


/**
 * Sortiert Tränke nach deren Rang.
 * 
 * 
 * @author Fiete
 *
 */
public class AlchemistTrankComparator implements Comparator<AlchemistTrank>{
	
	public AlchemistTrankComparator(){

	}
	
	public int compare(AlchemistTrank trank1,AlchemistTrank trank2){

		return trank1.getRang() - trank2.getRang();

	}
	
}
