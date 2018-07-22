package com.fftools.pools.bau;

import java.util.Comparator;

import com.fftools.scripts.Bauen;

/**
 * small class to compare 2 Bauen-scripts
 * @author Fiete
 *
 */
public class BauScriptComparator implements Comparator<Bauen> {
	public int compare(Bauen b1, Bauen b2){
		int erg = 0;
		// normalerweise höhere Prio
		erg = b2.getPrioSteine()-b1.getPrioSteine();
		if (erg==0){
			// oder schneller fertig
			int status2 = b2.getTargetSize()-b2.getActSize();
			int status1 = b1.getTargetSize()-b1.getActSize();
			erg = status1-status2;
		}
		return erg;
	}
}
