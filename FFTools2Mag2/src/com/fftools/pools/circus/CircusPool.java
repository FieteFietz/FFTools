package com.fftools.pools.circus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import magellan.library.Region;

import com.fftools.ScriptUnit;
import com.fftools.trade.TradeRegion;


/**
 * Klasse die Unterhalter einer Region koordiniert und selbst 
 * von CircusPoolManager verwaltet wird
 * @author Marc
 *
 */

public class CircusPool {
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	// der PoolManager sammelt die CirusPools 
	public CircusPoolManager circusPoolManager;
	
	// Region für die der Pool arbeiten soll
	public Region region = null;
	
	// Liste mit den CircusPoolRelations
	private ArrayList <CircusPoolRelation> listOfRelations = null;
	
	private int regionMaxUnterhaltung;
	
	// Wird vom Pool runtergezählt bis alles abgeschöpft ist.
	private int remainingUnterhaltung;	
	
	private int regionsVerdienst=0;
	
	// falls Region nicht komplett von Monopol bewirtschaftet wird
	private int limit = 100000;
	
	/**
	 * Konstruktor
	 * @param _cpm
	 * @param _region
	 */
	public CircusPool(CircusPoolManager _cpm, Region _region ){
		circusPoolManager = _cpm;
		
		region = _region;
		regionMaxUnterhaltung = region.maxEntertain();
		remainingUnterhaltung = regionMaxUnterhaltung;
		// zunächst gleich Max, damit mehrfache Aenderungen auffallen!
		limit = regionMaxUnterhaltung;
	}
	
	
	
	
	/**
	 * Fügt dem Circuspool eine CircusPoolRelation hinzu
	 * @param _cpr CircusPoolRelation 
	 */
	public void addCircusPoolRelation(CircusPoolRelation _cpr){
		// gibt es schon eine liste mit Relations? Falls nicht anlegen!
		if (listOfRelations == null){
	    	listOfRelations = new ArrayList <CircusPoolRelation>();
		}
	    
		listOfRelations.add(_cpr);	
	}
	
	/**
	 * Zentrale Methode zum CircusPool anstossen
	 *
	 */
	public void runPool(){
		
		
		
		// Check ob es Relations gibt
		if (this.listOfRelations != null){
			 
			// Sortiert Relations nach Talentwert
			// sortieren hinter != null gelegt, damit nix leeres sortiert wird
			Collections.sort(this.listOfRelations);
			
			//Durchiterieren der cpr   
			for (Iterator<CircusPoolRelation> iter1 = this.listOfRelations.iterator();iter1.hasNext();){
			     CircusPoolRelation cpr = (CircusPoolRelation) iter1.next();
	             
			     // Ist Silber der Bauern verschleudert?
			     if (remainingUnterhaltung <= 0){
			    	 
			    	 // Sende Vorzeichen-Signal an Relation und damit an Script
			    	 cpr.setDoUnterhaltung(-1);
			     }
			     
                  //   Es ist Silber da aber zu wenig für die Leistung der Einheit.
			     if ((remainingUnterhaltung > 0) && (cpr.getVerdienst() > remainingUnterhaltung)){
			    	 
			    	 cpr.setDoUnterhaltung(remainingUnterhaltung);
			    	 regionsVerdienst = regionsVerdienst + remainingUnterhaltung;
			    	 // Silber der Bauern ist aufgebraucht...
			    	 remainingUnterhaltung = 0;
			     }
			     
			     
			     // Check: Noch Silber zu verdienen AND Verdienst geringer als Restsilber?
			     if ((remainingUnterhaltung > 0) && (cpr.getVerdienst()<= remainingUnterhaltung)){
			    	 // Unterhalte soviel wie geht!
			    	 cpr.setDoUnterhaltung(cpr.getVerdienst());
			    	 regionsVerdienst = regionsVerdienst + cpr.getVerdienst();
			    	 // Freies Silber reduzieren um Unterhalungsvermögen der Einheit 
			    	 remainingUnterhaltung = remainingUnterhaltung - cpr.getVerdienst();
			       
			     }
			    
			
		     }
		
		}	
	
	}
	
	/**
	 * Zum Informieren über tatsächlich befohlene Unterhaltung
	 */
	public void runPool2(){
		// gesamtsumme bestimmen
		int summe = 0;
		if (this.listOfRelations==null || this.listOfRelations.size()==0){
			return;
		}
		for (CircusPoolRelation rel:this.listOfRelations){
			summe += rel.getUnterhalten().getFinalOrderedUnterhaltung();
		}
		// informieren
		for (CircusPoolRelation rel:this.listOfRelations){
			ScriptUnit su = rel.getSkriptUnit();
			su.addComment("tatsächlich befohlene Unterhaltung in der Region: " + summe);
		}
	}

/**
 * 
 * Gibt den Verdienst aller Unterhalter in der Region zurück
 */	
public int getRegionsVerdienst(){
	return regionsVerdienst;
}

public int getRemainingUnterhalt(){
	return remainingUnterhaltung;
}
	
public int getUnterhaltungslimit(){
	return limit;
}

/**
 * 
 * Setzt limit und korrigiert remainigUnterhalung
 * 
 */
public void setUnterhaltungslimit(int _lim){
	limit = _lim;
	if (limit < remainingUnterhaltung){
		remainingUnterhaltung = limit;
	}
}

/**
 * Gibt maximale Unterhaltung in PoolRegion zurück
 */
public int getRegionMaxUnterhaltung(){
	return regionMaxUnterhaltung;
}

/**
 * 
 * Gibt null wenn nichts gefunden wird.
 * 
 */
public CircusPoolRelation getCircusPoolRelation(ScriptUnit _su){
	boolean go_on= true;
	CircusPoolRelation cpr = null;
	for (Iterator<CircusPoolRelation> iter2 = this.listOfRelations.iterator();(iter2.hasNext()&&(go_on));){
	    // casten, vergesse das zu gern!
		cpr = (CircusPoolRelation) iter2.next();
	    if (cpr.getSkriptUnit() == _su){
	    	go_on=false;
	    	return cpr;
	    }
	}
	return cpr;  
}


/**
 * @return the listOfRelations
 */
public ArrayList<CircusPoolRelation> getListOfRelations() {
	return listOfRelations;
}

/**
 * max Entertain of all relations
 * @return
 */
public int getMaxEntertainUnits(){
	int erg=0;
	if (this.listOfRelations.size()==0){
		return 0;
	}
	
	for (Iterator<CircusPoolRelation> iter = this.listOfRelations.iterator();iter.hasNext();){
		CircusPoolRelation CPR = (CircusPoolRelation)iter.next();
		erg += CPR.getVerdienst();
	}
	
	return erg;
}


}
