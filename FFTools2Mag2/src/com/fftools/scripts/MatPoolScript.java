package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.rules.ItemType;

import com.fftools.pools.bau.BauManager;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.MatPoolManager;
import com.fftools.pools.matpool.relations.MatPoolOffer;
import com.fftools.pools.matpool.relations.MatPoolRequest;

/**
 * alle Functions ans Objects needed to deel with MatPool and
 * MatPoolManager
 * @author Fiete
 *
 */
public class MatPoolScript extends LernPoolScript {
	
	private MatPool matPool = null;
	
	// Parameter für die Prioberechnung;
	private double[] prioParameter = {0.0, 0.0, 0.0, 0.0};
	
	
	
	public MatPoolScript() {
		
	}
	
	
	/**
	 * Fuegt eine MatPoolRelation zu den Offers hinzu
	 *
	 */
	public void addMatPoolOffer(MatPoolOffer m){
		if (m==null){
			return;
		}
		checkMatPool();
		this.matPool.addMatPoolOffer(m);
	}
	
	/**
	 * Fuegt eine MatPoolRelation zu den Requests hinzu
	 *
	 */
	public void addMatPoolRequest(MatPoolRequest m){
		if (m==null){
			return;
		}
		checkMatPool();
		this.matPool.addMatPoolRequest(m);
	}
	
	public boolean removeMatPoolRequest(MatPoolRequest m){
		if (m==null){
			return false;
		}
		checkMatPool();
		return this.matPool.removeMatPoolRequest(m);
	}
	
	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolRequest> getRequests() {
		this.checkMatPool();
		return this.matPool.getRequests(this.scriptUnit);
	}
	
	/**
	 * Liefert eine Array-List mit den Offers oder NULL, falls keine existieren
	 *
	 */

	
		
	public ArrayList<MatPoolOffer> getOffers() {
		this.checkMatPool();
		return this.matPool.getOffers(this.scriptUnit);
	}
	

	/**
	 * Liefert die (erste) MatPoolRelation der Offers eines bestimmten ItemTypes
	 * oder Null, falls keine existieren oder keine fuer diesen ItemType
	 */

	// wir gehen davon aus, dass jedes Item nur EIN mal als offer deklariert
	// wird pro Unit
	public MatPoolOffer getMatPoolOffer(ItemType itemType) {
		ArrayList<MatPoolOffer> myOffers = this.getOffers();
		if (myOffers == null) {
			return null;
		} else {
			for (Iterator<MatPoolOffer> i1 = myOffers.iterator();i1.hasNext();){
				MatPoolOffer r = (MatPoolOffer) i1.next();
				if (r.getItemType().equals(itemType)){
					return r;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * checks, if matPool is known
	 * if not, references it
	 *
	 */
	private void checkMatPool(){
		if (this.matPool==null){
			this.matPool = this.getMatPool();
		}
	}
	
	/**
	 * sets the according MatPool for our scriptunit
	 *
	 */
	public MatPool getMatPool(){
		// liefert immer einen MMM
		MatPoolManager MMM = this.scriptUnit.getScriptMain().getOverlord().getMatPoolManager();
		return MMM.getRegionsMatPool(this.scriptUnit);
	}

	/**
	 * Short cut to the BauManager
	 * @return
	 */
	public BauManager getBauManager(){
		return this.scriptUnit.getScriptMain().getOverlord().getBauManager();
	}
	
	/**
	 * Setzt Parameter für die Prioberechnung nach Prio(x)= A*exp(Bx)+Cx+D
	 * 
	 */
	public void setPrioParameter(int _index, double _param){
    	this.prioParameter[_index]=_param;
    	
    }
	
	public void setPrioParameter(double _param0, double _param1, double _param2, double _param3){
		this.prioParameter[0]=_param0;
		this.prioParameter[1]=_param1;
		this.prioParameter[2]=_param2;
		this.prioParameter[3]=_param3;
	}
    
	/**
	 * Gibt Parameter für die Prioberechnung nach Prio(x)= A*exp(Bx)+Cx+D aus
	 * 
	 */
	
	public double getPrioParameter(int _index){
		return this.prioParameter[_index];
	}
    
	
	/**
	 * Gibt die Prio in Abhängigkeit der Vorausrundenzeit x und der Parameter A,B,C,D an
	 * 
	 * Prio(x)= A*exp(Bx)+Cx+D aus
	 * 
	 * 	 * 
	 * @param _runde
	 * @return
	 */
	public int getPrio(int _runde){
		return (int) Math.round(prioParameter[0]*Math.exp(prioParameter[1]*_runde)+prioParameter[2]*_runde+prioParameter[3]);	
	}
	
	/**
	 * Gibt die Prio in Abhängigkeit der Parameter A,B,C,D für aktuelle Runde
	 * 
	 * Prio(0)= A*exp(B*0)+C*0+D aus
	 * 
	 */
	
	public int getPrio(){
		return getPrio(0);
	}
    

}
