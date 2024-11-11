package com.fftools;

import java.util.Comparator;


/**
 * small class to compare 2 SUs 
 * kleinster wert von ScriptUnit.sortValue zuerst 
 * @author Fiete
 *
 */
public class ScriptUnitComparator_natural implements Comparator<ScriptUnit> {
	
	public ScriptUnitComparator_natural(){
		
	}

	public int compare(ScriptUnit s1, ScriptUnit s2){
		// kleinster wert nach vorne
		if (s1.sortValue == s2.sortValue) {
			return (s1.sortValue_2 - s2.sortValue_2);
		}
		return (s1.sortValue - s2.sortValue);
	}
}

