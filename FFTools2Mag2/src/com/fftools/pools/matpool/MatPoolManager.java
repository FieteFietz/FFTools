package com.fftools.pools.matpool;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Region;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.pools.matpool.relations.MatPoolOffer;
import com.fftools.pools.matpool.relations.MatPoolRequest;

public class MatPoolManager implements OverlordRun,OverlordInfo{
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	/*
	 * Ausgabe in .txt ?
	 */
	private boolean reportOFF = false;
	
	private static final int Durchlauf1 = 200;
	private static final int Durchlauf2 = 400;
	private static final int Durchlauf3 = 600; // Lernpool braucht den sehr spät
	private static final int Durchlauf4 = 700;
	
	private int[] runners = {Durchlauf1,Durchlauf2,Durchlauf3, Durchlauf4};
	
	public ScriptMain scriptMain = null;
	private Hashtable<Region,MatPool> matPoolMap = null;
	
	// private boolean scriptUnitOffersprocessed = false;
	
	
	
	public MatPoolManager (ScriptMain _scriptMain){
		this.scriptMain = _scriptMain;
		this.reportOFF = reportSettings.getOptionBoolean("disable_report_MatPoolManager");
	}
	
	/**
	 * zum Umleiten der Ausgaben der verschiedenen Läufe
	 */
	private int run_counter = 0;
	
	/**
	 * liefert zu einer scriptunit den matpool der region
	 * @param _u
	 * @return
	 */
	public MatPool getRegionsMatPool(ScriptUnit _u){
		// falls noch keine Map MatPools angelegt wurde
		// eine anlegen
		Region r = _u.getUnit().getRegion();
		if (matPoolMap==null){
			this.matPoolMap = new Hashtable<Region,MatPool>();
		}
		MatPool mp = this.getRegionsMatPool(r);
		
		// und auf jeden Fall selber eintragen
		mp.addScriptUnit(_u);
		return mp;
	}
	
	/**
	 * liefert den Matpool einer Region
	 * im Gegensatz zum Aufruf mit ScriptUnit hier kein
	 * Eintrag einer ScriptUnit in den Matpool..der könnte also
	 * komplett leer sein. 
	 * Ist kein Pool für die Region vorhanden, dann wird er angelegt.
	 */
	public MatPool getRegionsMatPool(Region r){
		
		if (matPoolMap==null){
			this.matPoolMap = new Hashtable<Region,MatPool>();
		}
		MatPool mp = matPoolMap.get(r);
		// falls noch kein MatPool fuer r da ist...anlegen
		if (mp==null){
			mp = new MatPool2(this,r);
            // und nu natuerlich den MatPool der matPoolMap hinzufuegen
			// hat ich natuerlich vergessen beim ersten versuch
			matPoolMap.put(r,mp);
		} 
	    return mp;
	}
	
	
	/**
	 * stoesst die registrierten MatPools an, die Offers und
	 * Requests zu reseten
	 */
	
	public void resetRelations(Collection<ScriptUnit> scriptUnits){
		if (matPoolMap==null){return;}
		for (Iterator<ScriptUnit> iter = scriptUnits.iterator();iter.hasNext();){
			ScriptUnit su= (ScriptUnit)iter.next();
			su.deleteSomeOrders("GIB");
			su.deleteSomeOrders("RESERVIERE");
			su.resetFreeKapa();
			// test
			// su.getUnit().refreshRelations();
			outText.addPoint();
		}
	}
	
	public void informUs(){
		if (this.reportOFF) {
			return;
		}
		// if (outText.getTxtOut()==null) {return;}
		outText.setFile("MatPoolInfos_" + this.run_counter);
		
		outText.addOutLine("******MatPoolManagerInfo******");
		if (this.matPoolMap!=null){
			outText.addOutLine("I have " + this.matPoolMap.size() + " MatPools registered");
		} else {
			outText.addOutLine("I have no MatPools registered");
			return;
		}
		outText.addOutLine("calling now the MatPools for there info...");
		for (Iterator<MatPool> iter = matPoolMap.values().iterator();iter.hasNext();){
			MatPool mp = (MatPool)iter.next();
			mp.informUs();
		}
		outText.addOutLine("***EndOF MatPoolManagerInfo***");
		
		outText.setFileStandard();
		
	}
	
	
	/**
	 * fordert alle registrierten MatPools auf, zu poolen
	 *
	 */
	
	public void run(int durchlauf){
		run_counter++;
		long startT = System.currentTimeMillis();
		outText.setScreenOut(true);
		outText.addOutLine("MatPoolManager -> MatPools. (" + run_counter + ")");
		
		
		
		if (this.scriptMain.getScriptUnits()==null){
			outText.addOutLine("MatPoolManager hat keine Scriptunits zum starten.");
			return;
		}
		
		if (matPoolMap==null){
			outText.addOutLine("MatPoolManager hat keine MatPools zum starten.");
			return;
		}
		
		
		
		if (!this.reportOFF) {
			outText.setScreenOut(false);
			outText.setFile("MatPoolManager_" + durchlauf);
		}
		for (Iterator<MatPool> iter = matPoolMap.values().iterator();iter.hasNext();){
			MatPool mp = (MatPool)iter.next();
			mp.runPool(durchlauf);
		}
	
		
		// long endT5 = System.currentTimeMillis();
		// outText.addOutLine("eigentlicher MP benötigte " + (endT5-endT4) + " ms!");
		/**
		outText.setScreenOut(false);
		informUs();
		*/
		if (!this.reportOFF) {
			outText.setScreenOut(true);
			outText.setFileStandard();
		}
		long endT = System.currentTimeMillis();
		outText.addOutLine("MatPoolManager benötigte " + (endT-startT) + " ms!");
		
	}
	
	
	
	/**
	 * Adds a MatPollrelation as Request to the according Matpool
	 * @param m a MatPoolRelation to Add
	 */
	public void addMatPoolRequest(MatPoolRequest m){
		ScriptUnit u = m.getScriptUnit();
		if (u==null){
			outText.addOutLine("!!!cannot add a MatPoolRelation without scriptUnit");
			return;
		}
		MatPool mp = this.getRegionsMatPool(u);
		mp.addMatPoolRequest(m);
	}
	
	/**
	 * entfernt den request aus dem entsprechenden MatPool
	 * @param m der Request
	 * @return true, wenn MatPoolListe geändert wurde
	 */
	public boolean removeMatPoolRequest(MatPoolRequest m){
		ScriptUnit u = m.getScriptUnit();
		if (u==null){
			outText.addOutLine("!!!cannot remove a MatPoolRelation without scriptUnit");
			return false;
		}
		MatPool mp = this.getRegionsMatPool(u);
		return mp.removeMatPoolRequest(m);
	}
	
	
	/**
	 * Adds a MatPollrelation as Offer to the according Matpool
	 * @param m a MatPoolRelation to Add
	 */
	public void addMatPoolOffer(MatPoolOffer m){
		ScriptUnit u = m.getScriptUnit();
		if (u==null){
			outText.addOutLine("!!!cannot add a MatPoolRelation without scriptUnit");
			return;
		}
		MatPool mp = this.getRegionsMatPool(u);
		mp.addMatPoolOffer(m);
	}
	
	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolOffer> getOffers(ScriptUnit u) {
		MatPool mp = this.getRegionsMatPool(u);
		return mp.getOffers(u);
	}
	

	/**
	 * Liefert eine Array-List mit den Requests oder NULL, falls keine existieren
	 *
	 */
	public ArrayList<MatPoolRequest> getRequests(ScriptUnit u) {
		MatPool mp = this.getRegionsMatPool(u);
		return mp.getRequests(u);
	}

	
	/**
	 * liefert den gewünschten Durchlauf...oder die Durchläufe 
	 * an den Overlord
	 * @return
	 */
	public int[] runAt(){
		return runners;
	}

	public boolean isReportOFF() {
		return reportOFF;
	}

	
	
}
