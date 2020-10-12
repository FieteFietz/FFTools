package com.fftools.scripts;

import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.transport.Transporter;




public class Transport extends TransportScript{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
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
		if (this.scriptUnit.getSetKapaPolicy()!=MatPoolRequest.KAPA_unbenutzt) {
			actPferdePolicy = this.scriptUnit.getSetKapaPolicy();
			this.addComment("Transport: vorhandene Kapa-Policy übernommen (" + actPferdePolicy +")");
		} else {
			this.addComment("Transport: keine gesetzte Kapa Policy erkannt, es bleibt bei kapa=Reiten");
		}
		MatPoolRequest mpr = null;
		if (!this.transporter.isLearning()){
			// jetzt pferde und wagen anfordern
			// wenn max, dann max, sonst aktuellen stand halten
			int menge = 0;
			if (this.transporter.isGetMaxPferde()){
				menge = this.scriptUnit.getSkillLevelNoRegionTypeBonus("Reiten") * this.scriptUnit.getUnit().getModifiedPersons() * 2;
				if (actPferdePolicy == MatPoolRequest.KAPA_max_zuFuss) {
					menge = this.scriptUnit.getSkillLevelNoRegionTypeBonus("Reiten") * this.scriptUnit.getUnit().getModifiedPersons() * 4;
					menge += this.scriptUnit.getUnit().getModifiedPersons();
					this.addComment("Transporter kapa=gehen, fordere " + menge + " Pferde an.");
				}
				// Spezialabfrage Talente Insekten in Gletscher oder Bergen
				if (this.scriptUnit.getSkillLevelNoRegionTypeBonus("Reiten")==1 && this.scriptUnit.getSkillLevel("Reiten")==0) {
					// Insekten können hier nicht reiten, hatten vorher T1 und nun T0, Pferde müssen
					// geführt werden! Jede Person ein Pferd, Bewegung zu Fuß!
					menge = this.scriptUnit.getUnit().getModifiedPersons();
					this.addComment("Insekten können hier nicht reiten: Pferde (1xPerson) werden geführt, Bewegung zu Fuß (kapa-policy)");
					actPferdePolicy = MatPoolRequest.KAPA_max_zuFuss;
				}
				if (this.scriptUnit.getSkillLevelNoRegionTypeBonus("Reiten")!=this.scriptUnit.getSkillLevel("Reiten") && this.scriptUnit.getSkillLevelNoRegionTypeBonus("Reiten")>1) {
					// Insekten behalten alle Pferde, gehen aber zu Fuß
					this.addComment("Insekten können hier nicht reiten: Alle Pferde werden geführt, Bewegung zu Fuß (kapa-policy)");
					actPferdePolicy = MatPoolRequest.KAPA_max_zuFuss;
				}
			} else {
				// aktuellen Stand halten
				// der ist wie?
				Item pferdItem = this.scriptUnit.getUnit().getItem(this.gd_Script.getRules().getItemType("Pferd"));
				if (pferdItem!=null){
					menge = pferdItem.getAmount();
				}
			}
			if (this.transporter.getSetAnzahlPferde()>=0) {
				menge = this.transporter.getSetAnzahlPferde();
			}
			// Requests aber nur basteln, wenn sollPferde > 0
			if (menge > 0){
				// dann auch keine Wagen....TrollTransporter bleiben aussen vor...
				mpr = null;
				
				// 20200413 neu, komplett, es wird pro Talentstufe 2x angefordert...
				int runTalent = 1;
				int actPrio=this.transporter.getPferdRequestPrio();
				if (this.transporter.getSetPferdePrio()>=0) {
					actPrio = this.transporter.getSetPferdePrio();
				}
				
				int actMenge=0;
				// gehend 1x vorneweg
				if (actPferdePolicy == MatPoolRequest.KAPA_max_zuFuss) {
					actMenge = this.scriptUnit.getUnit().getModifiedPersons();
					mpr = new MatPoolRequest(this,actMenge,"Pferd",actPrio,"Transporter (" + actMenge + " | " + menge + ", ohne Reiten)",actPferdePolicy);
					mpr.setOnlyRegion(true);
					this.addMatPoolRequest(mpr);
					this.transporter.addPferdeMPR(mpr);
					menge-=actMenge;
				}
				
				
				while (runTalent<=this.scriptUnit.getSkillLevel("Reiten") && menge>0) {
					actMenge = this.scriptUnit.getUnit().getModifiedPersons();
					if (actPferdePolicy == MatPoolRequest.KAPA_max_zuFuss) {
						actMenge = this.scriptUnit.getUnit().getModifiedPersons() * 2;
					}
					if (this.transporter.getSetAnzahlPferde()>=0) {
						actMenge = this.transporter.getSetAnzahlPferde();
					}
					if (actMenge>menge) {
						actMenge=menge;
					}
					
					mpr = new MatPoolRequest(this,actMenge,"Pferd",actPrio,"Transporter (" + actMenge + " | " + menge + ")",actPferdePolicy);
					mpr.setOnlyRegion(true);
					this.addMatPoolRequest(mpr);
					this.transporter.addPferdeMPR(mpr);
					menge-=actMenge;
					if (this.transporter.getSetPferdePrio()<0) {
						actPrio-=1;
						if (actPrio<2) {
						    actPrio=2;
						}
					}
					if (menge>0) {
						// Zweiter Lauf für diese Talentstufe
						actMenge = this.scriptUnit.getUnit().getModifiedPersons();
						if (actPferdePolicy == MatPoolRequest.KAPA_max_zuFuss) {
							actMenge = this.scriptUnit.getUnit().getModifiedPersons() * 2;
						}
						if (actMenge>menge) {
							actMenge=menge;
						}
						mpr = new MatPoolRequest(this,actMenge,"Pferd",actPrio,"Transporter (" + actMenge + " | " + menge + ")",actPferdePolicy);
						mpr.setOnlyRegion(true);
						this.addMatPoolRequest(mpr);
						this.transporter.addPferdeMPR(mpr);
						menge-=actMenge;
						if (this.transporter.getSetPferdePrio()<0) {
							actPrio-=1;
							if (actPrio<2) {
							    actPrio=2;
							}
						}
					}
					runTalent++;
			    }
				
				// sonderfall Insekten mit Talent=0 wegen Bponus
				if (menge>0 && this.scriptUnit.getSkillLevel("Reiten")==0) {
					mpr = new MatPoolRequest(this,menge,"Pferd",this.transporter.getPferdRequestPrio(),"Transporter zu Fuß",actPferdePolicy);
					mpr.setOnlyRegion(true);
					this.addMatPoolRequest(mpr);
					this.transporter.addPferdeMPR(mpr);
				}
				
			} else {
				this.addComment("Transporter: keine Pferdeanforderung");
			}
			// und gleich Wagen...
			// später einfach MAX bzw Zahl an MatPool übergeben
			// jetzt gehen wir mal davon aus, der Trans bekommt die sollPferde
			
			menge = 0;
			
			if (this.transporter.isGetMaxWagen()){
				// maximale Anzahl ermitteln
				menge = this.scriptUnit.getSkillLevel("Reiten") * this.scriptUnit.getUnit().getModifiedPersons();
			} else {
				// derzeitige Anzahl halten...
				Item wagenItem = this.scriptUnit.getUnit().getItem(wagenType);
				if (wagenItem!=null){
					menge = wagenItem.getAmount();
				}
			}
			
			if (this.transporter.getSetAnzahlWagen()>=0) {
				menge = this.transporter.getSetAnzahlWagen();
			}
			
			if (menge>0){
				// 20200413 komplett neu
				int runTalent = 1;
				int actPrio=this.transporter.getWagenRequestPrio();
				if (this.transporter.getSetWagenPrio()>=0) {
					actPrio = this.transporter.getSetWagenPrio();
				}
				int actMenge=0;
				while (runTalent<=this.scriptUnit.getSkillLevel("Reiten") && menge>0) {
					actMenge = this.scriptUnit.getUnit().getModifiedPersons();
					if (this.transporter.getSetAnzahlWagen()>=0) {
						actMenge =  this.transporter.getSetAnzahlWagen();
					}
					if (actMenge>menge) {
						actMenge=menge;
					}
					if (this.transporter.isGetMaxWagen()){
						mpr = new MatPoolRequest(this,actMenge,"Wagen",actPrio,"Transporter (" + actMenge + " | " + menge + ")",actPferdePolicy);
					} else {
						mpr = new MatPoolRequest(this,actMenge,"Wagen",actPrio,"Transporter (" + actMenge + " | " + menge + ")",MatPoolRequest.KAPA_unbenutzt);
					}
					// lokal
					mpr.setOnlyRegion(true);
					
					// Prio runter für nicht automatisierte transporter
					if (this.transporter.getMode()==Transporter.transporterMode_manuell){
						mpr.setPrio(mpr.getPrio()-1);
					}
					this.addMatPoolRequest(mpr);
					this.transporter.addWagenMPR(mpr);
					menge-=actMenge;
					if (this.transporter.getSetWagenPrio()<0) {
						actPrio-=1;
						if (actPrio<2) {
						    actPrio=2;
						}
					}
					runTalent++;
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
				if (reportSettings.getOptionBoolean("NoConfirmIdleTransport", this.region())){
					this.doNotConfirmOrders("iddle transport -> no confirm (scripter option)");
				}
			// } 
		}
	}
}
