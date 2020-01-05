package com.fftools.pools.treiber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.pools.circus.TreiberPoolRelationComparator;
import com.fftools.scripts.Treiben;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeAreaHandler;
import com.fftools.trade.TradeRegion;

import magellan.library.Region;

/**
 * 
 * Fasst alle Treiberpools zusammen und verbindet sie mit Scriptmain
 * Später kann hier eine Wanderung der Treiber zu freien Regionen oder anderen Berufen 
 * koordiniert werden.
 * 
 * Ein TreiberPoolArea existiert (noch) nicht.
 *  
 * @author Fiete
 *
 */

public class TreiberPoolManager implements OverlordRun,OverlordInfo {
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final int Durchlauf = 250;
	
	private int[] runners = {Durchlauf};
	//  Verbindung zu FFtools halten über Scriptmain
	public ScriptMain scriptMain;
	
	// Schwupps da stecken die Pools!
	private Hashtable<Region,TreiberPool> treiberPoolMap = null;
		
	
	
	
	/**
	 * Konstruktor noch gaanz einfach.
	 */
	public TreiberPoolManager(ScriptMain _scriptmain){
	     scriptMain = _scriptmain;	
	}
	
/**
 * 
 * Gibt den TreiberPool zurück UND meldet ScriptUnit über TreiberPoolRelation dort an!
 * MatPool abgekupfert...
 * 
 */
	public TreiberPool getTreiberPool(Treiben _u){
		// gibt es schon eine Poolmap, falls nicht wird angelegt!
		if (treiberPoolMap == null){treiberPoolMap = new Hashtable <Region,TreiberPool>();}
		Region region = _u.getUnit().getRegion();
		TreiberPool cp = treiberPoolMap.get(region);
		// falls noch kein MatPool fuer region da ist...anlegen
		if (cp==null){
			cp = new TreiberPool(this,region);

            // und nu natuerlich den TreiberPool der treiberPoolMap hinzufuegen
			// hat ich natuerlich vergessen beim ersten versuch
			treiberPoolMap.put(region,cp);
		} 
		// Sucht ein Script über diese Methode nach dem TreiberPool wird automatisch 
		// eine Relation im Pool angemeldet unter verwendund der ScriptUnit.
		// Skript muß daher nicht addTreiberPoolRelation aufrufen (darf auch nicht)!
		cp.addTreiberPoolRelation(new TreiberPoolRelation(_u, cp));	
		return cp;
	
	}
	
	/*
	 * liefert zu einer Region den Treiberpool oder NULL, falls keiner existiert
	 */
	public TreiberPool getTreiberpool(Region r){
		if (treiberPoolMap==null){
			return null;
		}
		return treiberPoolMap.get(r);
	}


/**
 * 
 * Hier stoesst man die TreiberPools an.
 *
 */	
	
    public void run(int Durchlauf){
    	if (treiberPoolMap != null){
	        for (Iterator<TreiberPool> iter = treiberPoolMap.values().iterator();iter.hasNext();){
		     TreiberPool cp = (TreiberPool)iter.next();
		     cp.runPool();}
    	 }
    	workOnTradeAreas();
    }
    /**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
    
    
    /**
     * ordnet nicht ausgelastete Treiber innerhalb des TAs neue Regionen zu
     */
    private void workOnTradeAreas(){
    	TradeAreaHandler TAH = this.scriptMain.getOverlord().getTradeAreaHandler();
    	if (TAH.getTradeAreas()==null || TAH.getTradeAreas().size()==0){
    		return;
    	}
    	if (this.treiberPoolMap==null || this.treiberPoolMap.isEmpty()) {
    		return;
    	}
    	for (TradeArea TA:TAH.getTradeAreas()){
    		// Schritt 1: unausgelastete Unterhalter pro TA feststellen
    		ArrayList<TreiberPoolRelation> availableTreiber = new ArrayList<TreiberPoolRelation>();
    		LinkedHashMap<Region, Long>wantingRegions = new LinkedHashMap<Region, Long>(); // Long: verfügbarer betrag
    		// durch alle Pools laufen und rausfinden, ob die im TA sind
    		for(TreiberPool tp:this.treiberPoolMap.values()){
    			if (TA.contains(tp.region)){
    				// treffer
    				// Treiber durchlaufen und schauen, ob sie ausgelastet sind
    				for (TreiberPoolRelation trp:tp.getListOfRelations()){
    					if (trp.getTreiben().getTargetRegion()==null && trp.getTreiben().isAutomode() && trp.getTreiben().isUnterMindestAuslastung()){
    						// ein Automode unausgelastet....
    						availableTreiber.add(trp);
    					}	
    				}
    				if (tp.getRemainingTreiben()>0) {
    					if (!wantingRegions.containsKey(tp.region)) {
    						wantingRegions.put(tp.region, Long.valueOf(tp.getRemainingTreiben()));
    					}
    				}
    			}
    		}
    		
    		for (TradeRegion TR:TA.getTradeRegions()) {
    			if (!wantingRegions.containsKey(TR.getRegion())) {
    				long maxTreiben = maxTreibsilberFreigabe_Kepler3(TR.getRegion());
    				if (maxTreiben>0) {
    					wantingRegions.put(TR.getRegion(), Long.valueOf(maxTreiben));
    				}
    			}
    		}
    		
    		
    		// fertig...wenn eine der beiden Listen leer ist...weiter gehen
    		if (wantingRegions.size()==0 || availableTreiber.size()==0){
    			continue;
    		}
    		// beide Listen gefüllt
    		// sortieren der Regions nach Bedarf
			List<Map.Entry<Region, Long>> entries =
			  new ArrayList<Map.Entry<Region, Long>>(wantingRegions.entrySet());
			
			Collections.sort(entries, new Comparator<Map.Entry<Region, Long>>() {
			  public int compare(Map.Entry<Region, Long> a, Map.Entry<Region, Long> b){
			    return b.getValue().compareTo(a.getValue());
			  }
			});
			Map<Region, Long> sortedMap = new LinkedHashMap<Region, Long>();
			for (Map.Entry<Region, Long> entry : entries) {
			  sortedMap.put(entry.getKey(), entry.getValue());
			}
			

    		
    		
    		// die CPs der Reihe nach abarbeiten
    		for (Region r:wantingRegions.keySet()){
    			// Distances setzen in den CRPs
    			for (TreiberPoolRelation tpr:availableTreiber){
    				tpr.setDistToRegion(r);
    			}
    			// Sortieren...nach Dist und Unterforderung
    			Collections.sort(availableTreiber, new TreiberPoolRelationComparator());
    			for (TreiberPoolRelation tpr:availableTreiber){
    				// jetzt checken, ob ein Treiber in die aktuelle Bedarfslage passt...
    				// dazu muss sein maximalVerdienst < remainingUnterhal sein
    				if (tpr.getTreiben().isUnterMindestAuslastung() && tpr.getTreiben().getTargetRegion()==null && tpr.getTreiben().isAutomode()){
    					// Bingo
    					// schicken wir ihn los.
    					tpr.getTreiben().setTargetRegion(r);
    					// und jetzt paar Infos
    					// im Zielgebiet
    					informTargetTPR(tpr,r);
    					informTargetRegion(r, tpr);
    					break;
    				}
    			}
    			
    		}
    	}
    }
    
    public long maxTreibsilberFreigabe_Kepler3(Region r){
    	/**
    	 * // script setScripterOption Treibsilberfreigabe=max
    	 *		mit folgender Semantik:
    	 * Einkommen = Bauern * Lohn
    	 * Bevölkerung = Bauern * 1,001 // abgerundet
    	 * Versorgung = Bevölkerung * 10
    	 * Treibsilber = Regionssilber + Einkommen - Versorgung
    	 * Alle sonstigen Aktivitäten wie Unterhaltung, Handel, Rekrutierungen, Veränderungen durch GIB 0, Krieg usw. sollen nicht berücksichtigt werden. Diese Option ist für Spieler gedacht, die in stabilen Regionen nur treiben und optimal abschöpfen wollen, ohne dass jemand hungert.
    	 */
    	
    	
    	
    	// Einkommen
    	long einkommen = (r.getPeasantWage()) * r.getModifiedPeasants();
    	long bev = (long)Math.floor(r.getModifiedPeasants() * 1.001);
    	long vers=bev * 10;
    	long medVorsorge = bev * 30;
    	int erg = (int)(r.getSilver() + einkommen - vers - medVorsorge);
    	if (erg<0) {
    		erg =0;
    	}
    	return erg;
    }
    
    private void informTargetTPR(TreiberPoolRelation mover, Region r){
    	mover.getTreiben().addComment("TPM: Treiber ist unterwegs nach: " + r.toString());
    	mover.getTreiben().addComment("TPM: ETA: " + mover.getTreiben().getGotoInfo().getAnzRunden() + " Runden, kann für " + mover.getVerdienst() + " treiben.");
    }
    
    private void informTargetRegion(Region r, TreiberPoolRelation mover){
    	ScriptUnit su = mover.getSkriptUnit().getOverlord().getMatPoolManager().getRegionsMatPool(r).getDepotUnit();
		if (su!=null){
    		su.addComment("TPM: Weiterer Treiber ist unterwegs: " + mover.getTreiben().scriptUnit.unitDesc());
    		su.addComment("TPM: ETA: " + mover.getTreiben().getGotoInfo().getAnzRunden() + " Runden, kann für " + mover.getVerdienst() + " treiben.");
		}
    }
    
}
	
	
	
	
	
	
	
	
	

	
	
	
	
	

