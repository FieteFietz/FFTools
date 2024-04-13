package com.fftools.pools.bau;

import java.util.Comparator;

import com.fftools.scripts.Seewerft;


/**
 * small class to compare 2 Schiffe 
 * @author Fiete
 *
 */
public class SeeWerftScriptComparator implements Comparator<Seewerft> {
	
	
	private int BauPunkte=0;
	
	
	public SeeWerftScriptComparator(int _BauPunkte){
		this.BauPunkte = _BauPunkte;
	}
	
	
	public void setBauPunkte(int bauPunkte){
		this.BauPunkte = bauPunkte;
	}
	
	public int compare(Seewerft w1, Seewerft w2){
		int erg=0;
		
		if (this.BauPunkte==0){
			// nur nach Leistung
			erg = w2.getBauPunkteMitHolz() - w1.getBauPunkteMitHolz();
		}
		
		if (erg==0){
		
			// sortieren, wer knapper über den Baupunkten liegt
			int diff1 = w1.getBauPunkteMitHolz()-this.BauPunkte;
			int diff2 = w2.getBauPunkteMitHolz()-this.BauPunkte;
			
			// wenn nur einer fertig bauen kann....
			if (diff1>=0 && diff2<0){
				erg= -1;
			}
			if (diff2>=0 && diff1<0){
				erg= 1;
			}
			
			// wenn beide nicht fertig bauen können, dann den, der mehr bauen kann...
			if (diff1<0 && diff2<0){
				erg = w2.getBauPunkteMitHolz() - w1.getBauPunkteMitHolz();
				
			}
			
			// wenn beide fertig bauen können, dann den kleineren..
			if (diff1>=0 && diff2>=0){
				erg = w1.getBauPunkteMitHolz() - w2.getBauPunkteMitHolz();
				
			}
		
		}
		
		return erg;
	}
}

