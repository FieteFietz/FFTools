package com.fftools.scripts;

import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.transport.Transporter;




public class Transport extends TransportScript{
	
	// vor erstem Matpool, nach Goto und Route...
	private static final int Durchlauf_anfang = 50;
	// nach erstem Matpool, aber vor TM
	private static final int Durchlauf_mitte = 300;
	// nach TM
	private static final int Durchlauf_ende = 520;
	
	private int[] runners = {Durchlauf_anfang,Durchlauf_mitte,Durchlauf_ende};
	
	private Transporter transporter = null;

	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Transport() {
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

		
		switch (scriptDurchlauf){
		
		case Durchlauf_anfang:this.scriptStart();break; 
		
		case Durchlauf_mitte:this.scriptMitte();break;
		
		case Durchlauf_ende:this.scriptEnd();break;
		}
	}
	
	/**
	 * registriert den transporter
	 * und je nach option requestet pferde und wagen
	 *
	 */
	private void scriptStart(){
		super.addVersionInfo();
		// dabei wird der transporter gleich initialisiert und orders nach optionen
		// geparst
		this.transporter = super.scriptUnit.getScriptMain().getOverlord().getTransportManager().addTransporter(this.scriptUnit);
		
		// Einschub: minReitTalent abfragen
		if (this.transporter.getMinReitTalent()>0){
			this.addComment("DEBUG: minReitTalent ist " + this.transporter.getMinReitTalent());
			SkillType reitSkillType = super.gd_Script.getRules().getSkillType("Reiten");
			if (reitSkillType!=null){
				Skill reitSkill = super.getUnit().getModifiedSkill(reitSkillType);
				if ((reitSkill==null) || reitSkill.getLevel()<this.transporter.getMinReitTalent()){
					// muss erst reiten Lernen
					this.addComment("MindestReitTalent von " + this.transporter.getMinReitTalent() + " nicht erreicht.");
					super.scriptUnit.findScriptClass("Lernfix", "Talent=Reiten");
					this.transporter.setLearning(true);
				}
			}
		} else {
			this.addComment("DEBUG: kein minReitTalent");
		}
		
		ItemType wagenType = super.gd_Script.getRules().getItemType("Wagen");
		int actPferdePolicy = MatPoolRequest.KAPA_max_zuPferd;
		MatPoolRequest mpr = null;
		if (!this.transporter.isLearning()){
			// jetzt pferde und wagen anfordern
			// wenn max, dann max, sonst aktuellen stand halten
			int menge = 0;
			if (this.transporter.isGetMaxPferde()){
				menge = this.scriptUnit.getSkillLevel("Reiten") * this.scriptUnit.getUnit().getModifiedPersons() * 2;
			} else {
				// aktuellen Stand halten
				// der ist wie?
				Item pferdItem = this.scriptUnit.getUnit().getItem(this.gd_Script.getRules().getItemType("Pferd"));
				if (pferdItem!=null){
					menge = pferdItem.getAmount();
				}
			}
			// Requests aber nur basteln, wenn sollPferde > 0
			if (menge > 0){
				// dann auch keine Wagen....TrollTransporter bleiben aussen vor...
				mpr = null;
				
				
				
				
				// erstmal jeder 1 Pferd
				mpr = new MatPoolRequest(this,1,"Pferd",this.transporter.getPferdRequestPrio(),"Transporter",actPferdePolicy);
				// lokal
				mpr.setOnlyRegion(true);
				this.addMatPoolRequest(mpr);
				this.transporter.addPferdeMPR(mpr);
				menge-=1;
				if (menge>0) {
					// nu jeder ein wenig mehr...dass jeder ein Pferd hat
					boolean einMehr = false;
					if (this.scriptUnit.getUnit().getModifiedPersons()>1){
						mpr = new MatPoolRequest(this,(this.scriptUnit.getUnit().getModifiedPersons()*2)-1,"Pferd",this.transporter.getPferdRequestPrio()-1,"Transporter",actPferdePolicy);
						// lokal
						mpr.setOnlyRegion(true);
						this.addMatPoolRequest(mpr);
						this.transporter.addPferdeMPR(mpr);
						menge-=((this.scriptUnit.getUnit().getModifiedPersons()*2)-1);
					} else {
						einMehr=true;
					}
					if (menge>0){
						// jetzt pro Talentstufe weitere Anfragen
						if (this.scriptUnit.getSkillLevel("Reiten")>0){
							for(int i = 1;i<=this.scriptUnit.getSkillLevel("Reiten") && menge>0;i++){
								int actMenge = this.scriptUnit.getUnit().getModifiedPersons() * 2;
								if (einMehr){
									actMenge-=1;
									einMehr=false;
								} 
								mpr = new MatPoolRequest(this,actMenge,"Pferd",this.transporter.getPferdRequestPrio()-(1 + i),"Transporter",actPferdePolicy);
								// lokal
								mpr.setOnlyRegion(true);
								this.addMatPoolRequest(mpr);
								this.transporter.addPferdeMPR(mpr);
								menge-=actMenge;
							}
						}
					}
				}
			}
			// und gleich Wagen...
			// später einfach MAX bzw Zahl an MatPool übergeben
			// jetzt gehen wir mal davon aus, der Trans bekommt die sollPferde
			
			menge = 0;
			
			if (this.transporter.isGetMaxWagen()){
				// maximale Anzahl ermitteln
				menge = Integer.MAX_VALUE;
			} else {
				// derzeitige Anzahl halten...
				Item wagenItem = this.scriptUnit.getUnit().getItem(wagenType);
				if (wagenItem!=null){
					menge = wagenItem.getAmount();
				}
			}
			if (menge>0){
				
				// erstmal jeder nur einen Wagen...
				
				if (this.transporter.isGetMaxWagen()){
					mpr = new MatPoolRequest(this,1,"Wagen",this.transporter.getWagenRequestPrio(),"Transporter",actPferdePolicy);
				} else {
					mpr = new MatPoolRequest(this,1,"Wagen",this.transporter.getWagenRequestPrio(),"Transporter",MatPoolRequest.KAPA_unbenutzt);
				}
				// lokal
				mpr.setOnlyRegion(true);
				
				// Prio runter für nicht automatisierte transporter
				if (this.transporter.getMode()==Transporter.transporterMode_manuell){
					mpr.setPrio(mpr.getPrio()-1);
				}
				
				this.addMatPoolRequest(mpr);
				this.transporter.addWagenMPR(mpr);
				
				menge-=1;
				if (menge>0) {
					// nu jeder ein wenig mehr...dass jeder ein Wagen hat
					boolean einMehr = false;
					if (this.scriptUnit.getUnit().getModifiedPersons()>1){
						mpr = new MatPoolRequest(this,(this.scriptUnit.getUnit().getModifiedPersons())-1,"Wagen",this.transporter.getWagenRequestPrio()-1,"Transporter",actPferdePolicy);
						// lokal
						mpr.setOnlyRegion(true);
						this.addMatPoolRequest(mpr);
						this.transporter.addWagenMPR(mpr);
						menge-=((this.scriptUnit.getUnit().getModifiedPersons())-1);
					} else {
						einMehr=true;
					}
					if (menge>0){
						// jetzt pro Talentstufe weitere Anfragen
						if (this.scriptUnit.getSkillLevel("Reiten")>0){
							for(int i = 1;i<=this.scriptUnit.getSkillLevel("Reiten") && menge>0;i++){
								int actMenge = this.scriptUnit.getUnit().getModifiedPersons();
								if (einMehr){
									actMenge-=1;
									einMehr=false;
								} 
								mpr = new MatPoolRequest(this,actMenge,"Wagen",this.transporter.getWagenRequestPrio()-(1 + i),"Transporter",actPferdePolicy);
								// lokal
								mpr.setOnlyRegion(true);
								this.addMatPoolRequest(mpr);
								this.transporter.addWagenMPR(mpr);
								menge-=actMenge;
							}
						}
					}
				}
			}

			// fertig, pferde und wagen beantragt
			
		}
		
		
		// später Einschub
		this.getTradeArea().addTransporter(this.transporter);
		
	}
	
	/**
	 * jetzt ist klar, wieviele Pferde und Wagen die Unit hat
	 * Jetziges Material ist abgeladen
	 * Kapa neu berechnen....
	 *
	 */
	private void scriptMitte(){
		if (this.transporter.isLearning()){return ;}
		this.transporter.recalcKapa();
		this.transporter.checkFesteRoute();
	}
	
	
	/**
	 * muss die vom TM gesetzten requests absetzen (denn die gehen nur von einem script aus)
	 *
	 */
	private void scriptEnd(){
		if (this.transporter.isLearning()){return ;}
		this.transporter.generateTransporterRequests(this);
		if (this.transporter.getGotoInfo()==null && this.transporter.getMode()==Transporter.transporterMode_fullautomatic ){
			// wenn keine GoTo anliegt....irgendetwas machen
			// if (this.transporter.getTransporterErstPferdePrio()<=0){
				this.addOrder("LERNEN Reiten", true);
				this.addComment("TM: keine Aufträge, freie Kapa:" + this.transporter.getKapa_frei());
			// } 
		}
	}
}
