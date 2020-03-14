package com.fftools.pools.circus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Region;
import magellan.library.Unit;

import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Unterhalten;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeAreaHandler;
import com.fftools.trade.TradeRegion;

/**
 * 
 * Fasst alle Circuspools zusammen und verbindet sie mit Scriptmain
 * Später kann hier eine Wanderung der Unterhalter zu freien Regionen oder anderen Berufen 
 * koordiniert werden.
 * 
 * Ein CircusPoolArea existiert (noch) nicht.
 *  
 * @author Marc
 *
 */

public class CircusPoolManager implements OverlordRun,OverlordInfo {
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final int Durchlauf1 = 74;
	private static final int Durchlauf2 = 79;
	
	
	private int[] runners = {Durchlauf1,Durchlauf2};
	
	//  Verbindung zu FFtools halten über Scriptmain
	public ScriptMain scriptMain;
	
	// Schwupps da stecken die Pools!
	private Hashtable<Region,CircusPool> circusPoolMap = null;
		
	
	/**
	 * Konstruktor noch gaanz einfach.
	 */
	public CircusPoolManager(ScriptMain _scriptmain){
	     scriptMain = _scriptmain;	
	}
	
/**
 * 
 * Gibt den CircusPool zurück UND meldet ScriptUnit über CircusPoolRelation dort an!
 * MatPool abgekupfert...
 * 
 */
	public CircusPool getCircusPool(Unterhalten _u){
		// gibt es schon eine Poolmap, falls nicht wird angelegt!
		if (circusPoolMap == null){circusPoolMap = new Hashtable <Region,CircusPool>();}
		Region region = _u.getUnit().getRegion();
		CircusPool cp = circusPoolMap.get(region);
		// falls noch kein CircusPool fuer region da ist...anlegen
		if (cp==null){
			cp = new CircusPool(this,region);

            // und nu natuerlich den CircusPool der circusPoolMap hinzufuegen
			// hat ich natuerlich vergessen beim ersten versuch
			circusPoolMap.put(region,cp);
		} 
		// Sucht ein Script über diese Methode nach dem CircusPool wird automatisch 
		// eine Relation im Pool angemeldet unter verwendund der ScriptUnit.
		// Skript muß daher nicht addCircusPoolRelation aufrufen (darf auch nicht)!
		cp.addCircusPoolRelation(new CircusPoolRelation(_u, cp));	
		return cp;
	
	}	

	
	/**
	 * liefert zu einer Region den Zirkuspool, wenn vorhanden, sonst null
	 * @param r
	 * @return
	 */
	public CircusPool getCircusPool(Region r){
		// gibt es schon eine Poolmap, falls nicht wird angelegt!
		if (circusPoolMap == null){return null;}
		return circusPoolMap.get(r);
	}
	
	

/**
 * 
 * Hier stoesst man die CircusPools an.
 *
 */	
	
    public void run(int Durchlauf){
    	if (Durchlauf == Durchlauf1){
    	if (circusPoolMap != null){
	        for (Iterator<CircusPool> iter = circusPoolMap.values().iterator();iter.hasNext();){
			     CircusPool cp = (CircusPool)iter.next();
			     cp.runPool();
		     }
	        
	        // und nu Stufe 2: Liste bauen mit automatisierten Unterhaltern, die nicht ausgelastet sind
	        // die sollen dann im TA wandern dürfen
	        workOnTradeAreas();
    	 }    
    	}
    	if (Durchlauf == Durchlauf2){
    		if (circusPoolMap != null){
    	        for (CircusPool iter : circusPoolMap.values()){
    			     CircusPool cp = (CircusPool)iter;
    			     cp.runPool2();
    		     }
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
     * ordnet nicht ausgelastete Unterhalter innerhalb des TAs neue Regionen zu
     */
    private void workOnTradeAreas(){
    	TradeAreaHandler TAH = this.scriptMain.getOverlord().getTradeAreaHandler();
    	if (TAH.getTradeAreas()==null || TAH.getTradeAreas().size()==0){
    		return;
    	}
    	for (TradeArea TA:TAH.getTradeAreas()){
    		// Schritt 1: unausgelastete Unterhalter pro TA feststellen
    		ArrayList<CircusPoolRelation> availableUnterhalter = new ArrayList<CircusPoolRelation>();
    		ArrayList<CircusPool>wantingCircusPools = new ArrayList<CircusPool>();
    		// durch alle Pools laufen und rausfinden, ob die im TA sind
    		for(CircusPool cp:this.circusPoolMap.values()){
    			if (TA.contains(cp.region)){
    				// treffer
    				// Unterhalter durchlaufen und schauen, ob sie ausgelastet sind
    				for (CircusPoolRelation crp:cp.getListOfRelations()){
    					if (crp.getUnterhalten().getTargetRegion()==null && crp.getUnterhalten().isAutomode() && crp.getUnterhalten().isUnterMindestAuslastung()){
    						// ein Automode unausgelastet....
    						availableUnterhalter.add(crp);
    					}	
    				}
    				// Hat dieser CP selbst bedarf?
    				if (cp.getRemainingUnterhalt()>0){
    					// treffer
    					wantingCircusPools.add(cp);
    				}
    			}
    		}
    		
    		// fertig...wenn eine der beiden Listen leer ist...weiter gehen
    		if (wantingCircusPools.size()==0 || availableUnterhalter.size()==0){
    			continue;
    		}
    		// beide Listen gefüllt
    		// sortieren der CPs nach Bedarf
    		Collections.sort(wantingCircusPools, new CircusPoolComparator());
    		// die CPs der Reihe nach abarbeiten
    		for (CircusPool cp:wantingCircusPools){
    			// Distances setzen in den CRPs
    			for (CircusPoolRelation cpr:availableUnterhalter){
    				cpr.setDistToRegion(cp.region);
    			}
    			// Sortieren...nach Dist und Unterforderung
    			Collections.sort(availableUnterhalter, new CircusPoolRelationComparator());
    			for (CircusPoolRelation cpr:availableUnterhalter){
    				// jetzt checken, ob ein Unterhalter in die aktuelle Bedarfslage passt...
    				// dazu muss sein maximalVerdienst < remainingUnterhal sein
    				if (cpr.getUnterhalten().isUnterMindestAuslastung() && cpr.getUnterhalten().getTargetRegion()==null && cpr.getUnterhalten().isAutomode() && cpr.getVerdienst()<cp.getRemainingUnterhalt()){
    					boolean istGeeignet=true;
    					
    					if (cp.region.getRegionType().getName().equalsIgnoreCase("Gletscher") && cpr.getUnterhalten().scriptUnit.isInsekt()) {
    						istGeeignet=false;
    					}
    					if (istGeeignet) {
	    					// Bingo
	    					// schicken wir ihn los.
	    					cpr.getUnterhalten().setTargetRegion(cp.region);
	    					// und jetzt paar Infos
	    					// im Zielgebiet
	    					informTargetCP(cp, cpr);
	    					break;
    					}
    				}
    			}
    		}
    		
    		
    	}
    	
    	workOnTradeRegionsNoPool(TAH);
    	
    }
    
    private void informTargetCP(CircusPool cp, CircusPoolRelation mover){
    	for (CircusPoolRelation cpr:cp.getListOfRelations()){
    		cpr.getUnterhalten().addComment("CPM: Weiterer Unterhalter ist unterwegs: " + mover.getUnterhalten().scriptUnit.unitDesc());
    		cpr.getUnterhalten().addComment("CPM: ETA: " + mover.getGotoInfo().getAnzRunden() + " Runden, kann für " + mover.getVerdienst() + " unterhalten.");
    	}
    }
    
    private void informTargetRegion(TradeRegion r, CircusPoolRelation mover){
    	Unit u = r.getDepot();
    	if (u!=null){
    		ScriptUnit su = this.scriptMain.getScriptUnit(u);
    		if (su!=null){
    			if (mover!=null){
		    		su.addComment("CPM: Weiterer Unterhalter ist unterwegs: " + mover.getUnterhalten().scriptUnit.unitDesc());
		    		su.addComment("CPM: ETA: " + mover.getGotoInfo().getAnzRunden() + " Runden, kann für " + mover.getVerdienst() + " unterhalten.");
    			} else {
    				// kein Erfolg...
    				su.addComment("CPM: die Suche nach einem Unterhalter hatte keinen Erfolg.");
    			}
    		}
    	}
    }
    
    /**
     * überprüft TAs auf Regionen *mit* Depot und ohne Unterhalter....
     */
    private void workOnTradeRegionsNoPool(TradeAreaHandler TAH){
    	for (TradeArea TA:TAH.getTradeAreas()){
    		ArrayList<TradeRegion>wantingRegions = new ArrayList<TradeRegion>();
    		ArrayList<CircusPoolRelation> availableUnterhalter = new ArrayList<CircusPoolRelation>();
    		for (TradeRegion TR :TA.getTradeRegions()){
    			if (getCircusPool(TR.getRegion())==null){
    				// nicht im Zirkuspool
    				if (TR.getDepot()!=null) {
    					// hat ein Depot!
    					if (TR.getRegion().maxEntertain()>0){
    						wantingRegions.add(TR);
    					}
    				}
    			}
    		}
    		if (wantingRegions.size()>0){
    			// wir haben welche..nu wiederum alle avails Erzeuegen
        		// durch alle Pools laufen und rausfinden, ob die im TA sind
        		for(CircusPool cp:this.circusPoolMap.values()){
        			if (TA.contains(cp.region)){
        				// treffer
        				// Unterhalter durchlaufen und schauen, ob sie ausgelastet sind
        				for (CircusPoolRelation crp:cp.getListOfRelations()){
        					if (crp.getUnterhalten().isAutomode() && crp.getUnterhalten().isUnterMindestAuslastung() && crp.getUnterhalten().getTargetRegion()==null){
        						// ein Automode unausgelastet....
        						availableUnterhalter.add(crp);
        					}	
        				}
        			}
        		}
    		}
    		if (availableUnterhalter.size()==0 || wantingRegions.size()==0){
    			continue;
    		}
    		// die TRs der Reihe nach abarbeiten
    		for (TradeRegion r:wantingRegions){
    			// Distances setzen in den CRPs
    			for (CircusPoolRelation cpr:availableUnterhalter){
    				cpr.setDistToRegion(r.getRegion());
    			}
    			// Sortieren...nach Dist und Unterforderung
    			Collections.sort(availableUnterhalter, new CircusPoolRelationComparator());
    			boolean foundOne = false;
    			for (CircusPoolRelation cpr:availableUnterhalter){
    				// jetzt checken, ob ein Unterhalter in die aktuelle Bedarfslage passt...
    				// dazu muss sein maximalVerdienst < remainingUnterhal sein
    				if (cpr.getVerdienst()<r.getRegion().maxEntertain() && cpr.getUnterhalten().getTargetRegion()==null && cpr.getUnterhalten().isAutomode() && cpr.getUnterhalten().isUnterMindestAuslastung()){
    					boolean istGeeignet=true;
    					if (r.getRegion().getRegionType().getName().equalsIgnoreCase("Gletscher") && cpr.getUnterhalten().scriptUnit.isInsekt()) {
    						istGeeignet=false;
    					}
    					if (cpr.getDist()<0) {
    						istGeeignet=false;
    					}
    					if (istGeeignet) {
	    					// Bingo
	    					// schicken wir ihn los.
	    					cpr.getUnterhalten().setTargetRegion(r.getRegion());
	    					// und jetzt paar Infos
	    					// im Zielgebiet
	    					informTargetRegion(r, cpr);
	    					foundOne=true;
	    					break;
    					}
    				}
    			}
    			if (!foundOne){
    				// Depot über Versuch informieren
    				
    			}
    		}
    		
    	}
    }
    
}
	
	
	
	
	
	
	
	
	

	
	
	
	
	

