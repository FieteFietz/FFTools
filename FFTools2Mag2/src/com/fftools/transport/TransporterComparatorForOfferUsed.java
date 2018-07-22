package com.fftools.transport;

import java.util.Comparator;

import magellan.library.Region;


/**
 * Sortiert bereits benutzte Transporter in einer Beladungsregion
 * 
 * @author Fiete
 *
 */
public class TransporterComparatorForOfferUsed implements Comparator<Transporter>{
	private long callCount = 0;
	
	/**
	 * zu transportierende Menge
	 */
	private int weight = 0;
	
	
	public TransporterComparatorForOfferUsed(int weight){
		this.weight = weight;
	}
	
	public void setDest(Region r){
		callCount=0;
	}
	
	public int compare(Transporter o1,Transporter o2){
		
		int dist1 = o1.getActDist();
		int dist2 = o2.getActDist();
		
		callCount++;
		
		if (dist1==dist2){
			// gleicher Abstand...nun der, der von der kapa besser passt
			// ok, passen den wirklich beide
			// 1 geht nicht, 2 geht
			if (o1.getKapa_frei()<weight && o2.getKapa_frei()>=weight){
				// o2 bevorzugen
				return 1;
			}
			// 1 geht, 2 nicht
			if (o1.getKapa_frei()>=weight && o2.getKapa_frei()<weight){
				// o1 bevorzugen
				return -1;
			}
			// wenn beide zu klein sind, liefer den grösseren...
			if (o1.getKapa_frei()<weight && o2.getKapa_frei()<weight){
				return (o2.getKapa_frei() - o1.getKapa_frei());
			}
			// ok..also in beide passt beides rein
			// liefer den kleineren => weniger Überhang
			return (o1.getKapa_frei() - o2.getKapa_frei());
			
		} else {
			return (dist1 - dist2);
		}
	}
	

	/**
	 * @return the weight
	 */
	public int getWeight() {
		return weight;
	}

	/**
	 * @param weight the weight to set
	 */
	public void setWeight(int weight) {
		this.weight = weight;
	}

	/**
	 * @return the callCount
	 */
	public long getCallCount() {
		return callCount;
	}
	
}
