package com.fftools.transport;

import java.util.Comparator;


/**
 * Sortiert Transporter nach Kapa, Grösste zuerst
 * 
 * @author Fiete
 *
 */
public class TransporterComparatorForInformStatus implements Comparator<Transporter>{
	private long callCount = 0;
	
	
	public TransporterComparatorForInformStatus(){

	}
	

	public int compare(Transporter o1,Transporter o2){		
		callCount++;	
		return o2.getKapa()-o1.getKapa();

}


	/**
	 * @return the callCount
	 */
	public long getCallCount() {
		return callCount;
	}
}
