package com.fftools.pools.bau;

import java.util.Comparator;

import com.fftools.scripts.Werft;


/**
 * small class to compare 2 Schiffe 
 * @author Fiete
 *
 */
public class WerftScriptComparator implements Comparator<Werft> {
	
	
	private int BauPunkte=0;
	
	
	public WerftScriptComparator(int _BauPunkte){
		this.BauPunkte = _BauPunkte;
	}
	
	
	public void setBauPunkte(int bauPunkte){
		this.BauPunkte = bauPunkte;
	}
	
	public int compare(Werft w1, Werft w2){
		int erg=0;
		
		if (this.BauPunkte==0){
			// nur nach Leistung
			erg = w2.getBauPunkteMitHolz() - w1.getBauPunkteMitHolz();
			if (erg==0){
				// erst den ohne Neubauauftrag
				if (w1.hasNeubauOrder() && !w2.hasNeubauOrder()){
					erg= 1;
				}
				if (!w1.hasNeubauOrder() && w2.hasNeubauOrder()){
					erg= -1;
				}
			}
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
		
		if (erg==0){
			// erst den ohne Neubauauftrag
			if (w1.hasNeubauOrder() && !w2.hasNeubauOrder()){
				erg= 1;
			}
			if (!w1.hasNeubauOrder() && w2.hasNeubauOrder()){
				erg= -1;
			}
		}

		return erg;
	}
}

