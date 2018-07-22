package com.fftools.pools.bau;

import java.util.Comparator;

import magellan.library.Ship;


/**
 * small class to compare 2 Schiffe 
 * @author Fiete
 *
 */
public class WerftSchiffComparator implements Comparator<Ship> {
	
	
	
	
	
	public WerftSchiffComparator(){
		
	}
	
	
	
	
	public int compare(Ship s1, Ship s2){
		int erg=0;
		
		
		// erst Triremen, dann Karavelle, dann Drachenschiffe, dann Boote
		String Typ1 = s1.getShipType().getName();
		String Typ2 = s2.getShipType().getName();
		
		if (Typ1=="Trireme"){
			if (Typ2=="Karavelle"){
				return -1;
			}
			if (Typ2=="Drachenschiff"){
				return -1;
			}
			if (Typ2=="Langboot"){
				return -1;
			}
			if (Typ2=="Boot"){
				return -1;
			}
		}
		
		if (Typ1=="Karavelle"){
			if (Typ2=="Trireme"){
				return 1;
			}
			if (Typ2=="Drachenschiff"){
				return -1;
			}
			if (Typ2=="Langboot"){
				return -1;
			}
			if (Typ2=="Boot"){
				return -1;
			}
		}
		
		if (Typ1=="Drachenschiff"){
			if (Typ2=="Trireme"){
				return 1;
			}
			if (Typ2=="Karavelle"){
				return 1;
			}
			if (Typ2=="Langboot"){
				return -1;
			}
			if (Typ2=="Boot"){
				return -1;
			}
		}
		
		if (Typ1=="Langboot"){
			if (Typ2=="Trireme"){
				return 1;
			}
			if (Typ2=="Karavelle"){
				return 1;
			}
			if (Typ2=="Drachenschiff"){
				return 1;
			}
			if (Typ2=="Boot"){
				return -1;
			}
		}
		
		if (Typ1=="Boot"){
			if (Typ2=="Trireme"){
				return 1;
			}
			if (Typ2=="Karavelle"){
				return 1;
			}
			if (Typ2=="Drachenschiff"){
				return 1;
			}
			if (Typ2=="Langboot"){
				return 1;
			}
		}
		
		// gleicher Typ...grösserer Schaden zuerst
		erg = s2.getDamageRatio() - s1.getDamageRatio();
		if (erg==0){
			// bei gleichem Schaden, eventuell noch nicht fertig, dann nach Weiterbaustatus...die schnell fertig werdenden zuerst
			int NochZuBauen_1 = s1.getShipType().getMaxSize() - s1.getSize();
			int NochZuBauen_2 = s2.getShipType().getMaxSize() - s2.getSize();
			erg = NochZuBauen_1 - NochZuBauen_2;
		}
		
		return erg;
	}
}

