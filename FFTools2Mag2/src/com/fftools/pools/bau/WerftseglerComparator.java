package com.fftools.pools.bau;

import java.util.Comparator;
import com.fftools.scripts.Werftsegler;


/**
 * small class to compare 2 Werftsegler als Flottenkaitäne
 * derjenige, der näher drann an seinem Ziel ist, soll zuerst bearbeitet werden 
 * @author Fiete
 *
 */
public class WerftseglerComparator implements Comparator<Werftsegler> {
	public int compare(Werftsegler w1, Werftsegler w2){
		return  (w1.getMaxSchiffe() - w1.getActSchiffe_bevor_GIB()) - (w2.getMaxSchiffe() - w2.getActSchiffe_bevor_GIB());  
	}
}

