package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.Building;
import magellan.library.Item;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.relations.MatPoolRequest;

/**
 * 
 * Fordert den Unterhalt für eine Gebäude an, wenn die Einheit das Kommando hat
 * @author Marc
 * 
 *  
 * 
 */
public class Gebaeudeunterhalt extends MatPoolScript{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private Building gebaeude = null;
	private int versorgungsRunden = 3;
	private int mindestVersorgungsRunden=1;
	
	private int defaultSilberPrio = 970;
	private int defaultEisenPrio = 970;
	private int defaultHolzPrio = 970;
	private int defaultSteinPrio = 970;

	
	private final String MauernDerEwigkeit = "Der Zahn der Zeit kann diesen Mauern nichts anhaben.";
		
	// Parameter des Scripts... Zuweisung im Konstruktor!
	
	private int durchlaufVorMatpool = 86;    // Anfordern von Unterhalt
	private int durchlaufNachMatpool = 340;  // Test ob Unterhalt gegeben wurde
	private int[] runners; 
	
	int aktuelleRunde=0;
	boolean fordertan =false; // Fordert das script überhaupt etwas an?
	
	/**
	 * Liste aller durch dieses script erzeugter MatPoolRequests
	 */
	private ArrayList<MatPoolRequest> matPoolRequests = null;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Gebaeudeunterhalt() {
		this.runners = new int[] {durchlaufVorMatpool, durchlaufNachMatpool};
		super.setRunAt(this.runners);
	}
	
	
	/**
	 * Eigentliche Prozedur
	 * runScript von Script.java MUSS/SOLLTE ueberschrieben werden
	 * Durchlauf kommt von ScriptMain
	 * 
	 * in Script steckt die ScriptUnit
	 * in der ScriptUnit steckt die Unit
	 * mit addOutLine jederzeit Textausgabe ans Fenster
	 * mit addComment Kommentare...siehe Script.java
	 */
	
	public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==durchlaufVorMatpool){
			this.anforderungsLauf();
		}
		
		if (scriptDurchlauf==durchlaufNachMatpool){
			this.kontrollLauf();
		}
		
		
		
		
	}
	
	private void anforderungsLauf(){
	    
// Setzt Prios für Vorausanforderungen
		
		// übergeordnete Prio?
		// silberprio eventuell anders?
		int newSilberPrio=reportSettings.getOptionInt("Gebaeudeunterhalt_SilberPrio", this.region());
		if (newSilberPrio>-1){
			this.defaultSilberPrio=newSilberPrio;
		}
		int newEisenPrio=reportSettings.getOptionInt("Gebaeudeunterhalt_EisenPrio", this.region());
		if (newEisenPrio>-1){
			this.defaultEisenPrio=newEisenPrio;
		}
		int newHolzPrio=reportSettings.getOptionInt("Gebaeudeunterhalt_HolzPrio", this.region());
		if (newHolzPrio>-1){
			this.defaultHolzPrio=newHolzPrio;
		}
		int newSteinPrio=reportSettings.getOptionInt("Gebaeudeunterhalt_SteinPrio", this.region());
		if (newSteinPrio>-1){
			this.defaultSteinPrio=newSteinPrio;
		}

		// Gebäude und Runde aus GD holen
		gebaeude = super.scriptUnit.getUnit().getModifiedBuilding();
		aktuelleRunde=super.scriptUnit.getScriptMain().gd_ScriptMain.getDate().getDate();                 		
		
		//Ist die Einheit in einem Gebäude und hat/bekommt zufällig das Komando? 
		if ( (gebaeude!=null) && (gebaeude.getOwnerUnit()==super.scriptUnit.getUnit()) && !this.sindMauernEwig(gebaeude)  && !this.scriptUnit.isLeavingBuilding){
		    
			// Unterhalskosten ermitteln!
			Iterator<Item> iter = gebaeude.getBuildingType().getMaintenanceItems().iterator();
			 if (iter != null){ 
				 for(;iter.hasNext();){
					 Item item = (Item) iter.next();		
					 super.addComment("Gebäudeunterhalt für " + versorgungsRunden + " Runden angefordert");  
					 for (int n=0;n<=versorgungsRunden-1;n++){
			    	      if (item!=null){
				    	      super.setPrioParameter(this.defaultSilberPrio, -0.5, 0, 1);
				    	      if (item.getName().equalsIgnoreCase("Eisen")){
				    	    	  super.setPrioParameter(this.defaultEisenPrio, -0.5, 0, 1);
				    	      }
				    	      if (item.getName().equalsIgnoreCase("Holz")){
				    	    	  super.setPrioParameter(this.defaultHolzPrio, -0.5, 0, 1);
				    	      }
				    	      if (item.getName().equalsIgnoreCase("Stein")){
				    	    	  super.setPrioParameter(this.defaultSteinPrio, -0.5, 0, 1);
				    	      }
				    	     
					          // Für jedes Item Matpoolrelation mit ID an den PoolMatpool senden!
				    	      MatPoolRequest actRequest = new MatPoolRequest(n+1,this,item.getAmount(),item.toString(),this.getPrio(n),"Gebäudeunterhalt Runde " + (aktuelleRunde+n));
				    	      
				    	      if (item.getName().equalsIgnoreCase("Silber pro Größenpunkt")){
				    	    	  // wow!  eine Taverne?! oder ähnliches
				    	    	  // neues Item nur Silber, Amount = item.amount * Größe
				    	    	  super.addComment("Besonderer unterhalt erkannt: " + item.getAmount() + " Silber pro Größenpunkt.");
				    	    	  super.addComment("Gebäudegröße:" + gebaeude.getSize() + ", Silbersumme: " + (gebaeude.getSize() * item.getAmount()));
				    	    	  actRequest = new MatPoolRequest(n+1,this,item.getAmount() * gebaeude.getSize(),"Silber",this.getPrio(n),"Gebäudeunterhalt Runde " + (aktuelleRunde+n));
				    	      }
				    	      
				    	      
					    	  this.addMatPoolRequest(actRequest);
					          fordertan = true;
					          if (this.matPoolRequests==null){
					        	  this.matPoolRequests=new ArrayList<MatPoolRequest>();
					          }
					          this.matPoolRequests.add(actRequest);
			    	      }	
				      }
				 }
			}
		
		}
		
		
	}
	
    /**
     * Geklaut aus Fietes Lohnscript
     *
     */
	
	private void kontrollLauf(){

		if (fordertan==true){	
		   if (this.matPoolRequests==null){
				//be fail safe..something's gone wrong
				outText.addOutLine("!!! Gebäudeunterhalt hat keine Requests !!: " + this.unitDesc());
				super.scriptUnit.doNotConfirmOrders("!!! Gebäudeunterhalt hat keine Requests !!: " + this.unitDesc());
				return;
			}
			// durch alle Requests dieser Unit durchgehen
			for (Iterator<MatPoolRequest> iter = this.matPoolRequests.iterator();iter.hasNext();){
				check_mpr((MatPoolRequest) iter.next());
			}
						
		}
	}

	
	private void check_mpr(MatPoolRequest _mpr){	
		// unerfülltes request da?
		if (_mpr.getOriginalGefordert()!=_mpr.getBearbeitet()) {
			// fuck, war es wenigstens ein weit zukünftiges?
			if(_mpr.getId()<=(this.mindestVersorgungsRunden)){
				super.scriptUnit.doNotConfirmOrders("Gebäudeunterhalt nicht erfüllt!" + " Es fehlen " +(_mpr.getOriginalGefordert()-_mpr.getBearbeitet())+ " " +_mpr.getOriginalGegenstand() +" für Gebäude in Runde " +(this.aktuelleRunde+_mpr.getId()-1) );
				this.scriptUnit.unterhaltProblem=true;
			}
		}	
	} 
	
	/**
	 * Überprüft, ob das Gebäude magisch vom Unterhalt befreit wurde.
	 * @param b Das Gebäude
	 * @return true, wenn kein Unterhalt fällig wird, sonst false
	 */
	private boolean sindMauernEwig(Building b){
		if (b!=null && b.getEffects()!=null && b.getEffects().size()>0){
			for (Iterator<String> iter = b.getEffects().iterator();iter.hasNext();){
				String s = (String)iter.next();
				if (s.startsWith(MauernDerEwigkeit)){
					return true;
				}
			}
		}
		return false;
	}
	
}