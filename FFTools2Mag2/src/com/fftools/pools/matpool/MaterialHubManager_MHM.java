package com.fftools.pools.matpool;

import java.util.ArrayList;

import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Materialhub;

import magellan.library.CoordinateID;

/**
 * Verwaltet MaterialTransports zwischen MaterialHubs
 * 
 * 
 * @author Fiete
 *
 */
public class MaterialHubManager_MHM implements OverlordRun,OverlordInfo {

	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	
	// private Overlord overLord = null;
	
	private ArrayList<Materialhub> MHs = new ArrayList<Materialhub>();

		
	/**
	 * Wann soll er laufen
	 * VOR Lernfix 
	 */
	private static final int Durchlauf = 520;
	
	// Rückgabe als Array
	private int[] runners = {Durchlauf};
	
	
	
	
	public MaterialHubManager_MHM(Overlord overlord){
		// this.overLord = overlord;
	}

	
	/**
	 * startet den SeeschlangenJagdManager_SJM
	 */
	public void run(int durchlauf){
		
	}
	
	
	/**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
    
    public void addMH(Materialhub mh) {
    	if (!this.MHs.contains(mh)) {
    		this.MHs.add(mh);
    	}
    }
    
    
    /*
     * liefert das registrierte MH zu einer RegionID
     */
    public Materialhub getMH(CoordinateID cID) {
    	if (this.MHs==null) {
    		return null;
    	}
    	for (Materialhub _mh:this.MHs) {
    		if (_mh.getUnit().getRegion().getID().equals(cID)) {
    			// bingo
    			return _mh;
    		}
    	}

    	return null;
    }
    
   
    
}
