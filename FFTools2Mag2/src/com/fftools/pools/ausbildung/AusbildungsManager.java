package com.fftools.pools.ausbildung;

import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Region;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;

public class AusbildungsManager implements OverlordRun,OverlordInfo {
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	
	public static final int Durchlauf1 = 80;
	public static final int Durchlauf2 = 550;
	public static final int Durchlauf3 = 650;
	
	private int[] runners = {Durchlauf1, Durchlauf2, Durchlauf3};
	
	//  Verbindung zu FFtools halten über Scriptmain
	public ScriptMain scriptMain;
	
	// Da stecken die Pools!
	private Hashtable<Region,AusbildungsPool> AusbildungsPoolMap = null;
		
	//	 enthält die maximal genutzte Rekursionstiefe der pools
	private long maxUsedRecursion=0;
	
	
	/**
	 * Konstruktor noch gaanz einfach.
	 */
	public AusbildungsManager(ScriptMain _scriptmain){
	     scriptMain = _scriptmain;	
	}
	
/**
 * 
 * Gibt den AusbildungsPool zurück UND meldet ScriptUnit über CircusPoolRelation dort an!
 * MatPool abgekupfert...
 * 
 */
	public AusbildungsPool getAusbildungsPool(ScriptUnit _u){
		// gibt es schon eine Poolmap, falls nicht wird angelegt!
		if (AusbildungsPoolMap == null){AusbildungsPoolMap = new Hashtable <Region,AusbildungsPool>();}
		Region region = _u.getUnit().getRegion();
		AusbildungsPool ap = AusbildungsPoolMap.get(region);
		// falls noch kein AusbildungsPool fuer region da ist...anlegen
		if (ap==null){
			ap = new AusbildungsPool(this,region);

            // und nun natuerlich den AusbildungsPool der AusbildungsPoolMap hinzufuegen
			AusbildungsPoolMap.put(region,ap);
		} 
		
		return ap;
	
	}	

	/**
	 * Liefert den AP zu einer Region, falls nicht da, wird der auch nicht angelegt!
	 * @param r Die angefragte Region
	 * @return
	 */
	public AusbildungsPool getAusbildungsPool(Region r){
		if (AusbildungsPoolMap==null) {
			return null;
		}
		return AusbildungsPoolMap.get(r);
	}
	

/**
 * 
 * Hier stoesst man die AusbildungsPools an.
 *
 */	
	
    public void run(int Durchlauf){
    	outText.addOutLine("Ausbildungsmanager (" + Durchlauf + ")",true);
    	long start = System.currentTimeMillis();
    	if (AusbildungsPoolMap != null){
	        for (Iterator<AusbildungsPool> iter = AusbildungsPoolMap.values().iterator();iter.hasNext();){
	        	AusbildungsPool ap = (AusbildungsPool)iter.next();
		        ap.runPool(Durchlauf);
		        outText.addPoint();
	        }
    	 }
    	long ende = System.currentTimeMillis();
    	outText.addOutLine("Ausbildungsmanager (" + Durchlauf + ") benötigte:" + (ende-start) + "ms",true);
    	if (Durchlauf==Durchlauf3){
			if (this.getMaxUsedRecursion()>=50){
				// outText.addOutLine("Ausbildungsmanager: Überhöhten Rekursionswert: "+  this.getMaxUsedRecursion() + " an monopol-tools@googlegroups.com melden!", true); 
			} else{
				outText.addOutLine("Ausbildungsmanager: max Rekursionen: " + this.getMaxUsedRecursion(), true);
			}
    	
    	}
    }
    
    /**
     * 
     */
    public void runAkaWarnungen(){
    	if (AusbildungsPoolMap != null){
	        for (Iterator<AusbildungsPool> iter = AusbildungsPoolMap.values().iterator();iter.hasNext();){
	        	AusbildungsPool ap = (AusbildungsPool)iter.next();
		        ap.AkaWarnungen();
		        outText.addPoint();
	        }
    	 }
    }
    
    
    /**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }

	/**
	 * @return the maxUsedRecursion
	 */
	public long getMaxUsedRecursion() {
		return maxUsedRecursion;
	}

	/**
	 * @param maxUsedRecursion the maxUsedRecursion to set
	 */
	public void setMaxUsedRecursion(long maxUsedRecursion) {
		this.maxUsedRecursion = maxUsedRecursion;
	}
  	
}
