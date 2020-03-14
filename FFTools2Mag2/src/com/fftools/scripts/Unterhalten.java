package com.fftools.scripts;

import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.rules.SkillType;

import java.util.ArrayList;

import com.fftools.ReportSettings;
import com.fftools.pools.circus.CircusPool;
import com.fftools.pools.circus.CircusPoolRelation;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;

/**
 * 
 * Erweiterte Version 2 des Unterhalters
 * die jetzt auch die anderen Unterhalter 
 * der Region im Auge hat.
 * @author Marc
 *
 */

public class Unterhalten extends TransportScript{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf_vorCircusPool = 56;
	private int Durchlauf_nachCircusPool = 78;
	
	private int[] runners = {Durchlauf_vorCircusPool,Durchlauf_nachCircusPool};
	
	// Ab welchem Talenwert für Unterhaltung soll Silber gemacht werden?
	// Default Runde 502 auf 2
	// mit setScripterOption minUnterhalterTalent=X gesetzt werden
		
	private int mindestTalent=2;
	private CircusPool circusPool = null;
	private int limit;
	// private static final OutTextClass outText = OutTextClass.getInstance();
	// Wird historisch von CircusPoolManager angelegt und muß dann mühsam über CP.getcpr geholt werden
	private CircusPoolRelation circusPoolRelation = null;
	
	private final int defaultMindestAuslastung = 90;
	
	private int mindestAuslastung = defaultMindestAuslastung; 
	private Skill skill=null;
	
	private boolean confirmIfunemployed = false;
	
	// wird per parameter gesetzt
	private boolean automode = false;
	
	// WENN in AutoMode UND soll in andere Region, dann hier das Ziel
	private Region targetRegion = null;
	private GotoInfo gotoInfo = null;
	
	private int finalOrderedUnterhaltung = 0;
	
	private String LernfixOrder = "Talent=Unterhaltung";
	
	// Konstruktor
	public Unterhalten() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_vorCircusPool){
			this.vorCircusPool();
		}
        

		if (scriptDurchlauf==Durchlauf_nachCircusPool){
			this.nachCircusPool();
		}
		
	}
	
	
	private void vorCircusPool(){
	
	// Erster Lauf mit direktem Lernen ODER Pool-Anmeldung
		
	
		int reportMinLevel = reportSettings.getOptionInt("minUnterhalterTalent",region());
		if (reportMinLevel>0){
			this.mindestTalent = reportMinLevel;
		}
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Unterhalten");
		OP.addOptionList(this.getArguments());
		int unitMinLevel = OP.getOptionInt("minTalent", -1);
		if (unitMinLevel>this.mindestTalent){
			this.mindestTalent = unitMinLevel;
		}
		unitMinLevel = OP.getOptionInt("mindestTalent", -1);
		if (unitMinLevel>this.mindestTalent){
			this.mindestTalent = unitMinLevel;
		}
		
		if (OP.getOptionString("mode").equalsIgnoreCase("auto")){
			this.automode = true;
		}
		
		
		// FF: eventuell hier setting für die Region ansetzen....falls nötig
		
		// skillType holen!
		SkillType skillType = super.gd_Script.getRules().getSkillType("Unterhaltung");
		
		// Kann die Einheit das Talent Unterhalten?
		// skill = super.scriptUnit.getUnit().getSkill(skillType);
		skill = super.scriptUnit.getUnit().getModifiedSkill(skillType);
		if (skill!= null) {
			
			// Einheit kann unterhalten ABER lohnt es sich? MindestTalent prüfen!
			if (skill.getLevel() >= this.mindestTalent){
				
				// Hurra! Ein Kandidat für den CircusPool! Aber welcher ist zuständig?
				// Registrieren läuft gleich mit durch Manager
				circusPool = super.scriptUnit.getScriptMain().getOverlord().getCircusPoolManager().getCircusPool(this);
							
				// Relation gleich mal referenzieren für später.
				circusPoolRelation = circusPool.getCircusPoolRelation(super.scriptUnit);
				// Möchte die Einheit die Gesamt-Unterhaltung in der Region beschränken?
				// Dann sollten wir ein Argument finden!
				// FF Neu..per Optionen...
				
				
				// entweder wird der geparste wert genommen, oder regionsmaximum
				limit = OP.getOptionInt("limit",super.scriptUnit.getUnit().getRegion().maxEntertain());
				
				// wenn wir schon den OP haben..gleich alles
				// gibts den nen reportSetting zur Mindestauslastung?
				int reportMindestAuslastung = reportSettings.getOptionInt("UnterhaltenMindestAuslastung", this.region());
				if (reportMindestAuslastung!=ReportSettings.KEY_NOT_FOUND){
					this.mindestAuslastung = reportMindestAuslastung;
				}
				
				// haben wir vielleicht noch einen direkten Parameter in den Optionen?
				this.mindestAuslastung = OP.getOptionInt("mindestAuslastung", this.mindestAuslastung);
				this.addComment("Hinweis: Mindestauslastung dieser Einheit: " + this.mindestAuslastung + "%");
				
				// Gibt es eine scripterOption für diese Region zum Limit ?
				int settingsLimit = reportSettings.getOptionInt("UnterhaltenLimit", this.region()); 
				if (settingsLimit>=0){
					circusPool.setUnterhaltungslimit(settingsLimit);
				}
				
				
				// Wurde das Regionslimit bereits durch eine andere Einheit verändert?
										
				if (circusPool.getRegionMaxUnterhaltung()!= circusPool.getUnterhaltungslimit()){
				     super.addComment("Warnung: Mehrere Einheiten setzen Unterhalungslimits für " + super.scriptUnit.getUnit().getRegion().getName());
				}     
				// ist das neue Limit strenger und positv?
				if ((limit < circusPool.getUnterhaltungslimit())&&(limit > 0 )){
				      // neues Limit gilt!
				      circusPool.setUnterhaltungslimit(limit);
				}
				// Bestätigen, wenn wegen Überzähliger Unterhaltereinheit eigentlich arbeitslos?
				this.confirmIfunemployed = OP.getOptionBoolean("confirmUnemployed",false);
				if (this.confirmIfunemployed){
					this.addComment("Unterhalten: Einheit wird auf Benutzerwunsch nicht unbestätigt bleiben.");
				}
			} else {
				// zu schlecht => lernen
				this.addComment("Mindesttalentwert " + mindestTalent + " unterschritten");
				this.Lerne();
			}
			
		} else {
			// Einheit kann garnicht Unterhalten!
			this.addComment("Mindesttalentwert " + mindestTalent + " unterschritten");
			this.Lerne();
		}	
	}	
	
	/**
	 * Zweiter Lauf nach dem runCircusPool
	 */
	
	private void nachCircusPool(){
	
		
		// Nimmt diese Einheit an einem Pool teil?
		if (circusPool != null){
		
			// Diverse Ausgaben 
		    
			super.addComment("Erwartetes Gesamteinkommen in " + super.scriptUnit.getUnit().getRegion().getName() +": " + circusPool.getRegionsVerdienst() + " Silber");
			if (circusPool.getRemainingUnterhalt() > 0){
				super.addComment("Möglicher Mehrverdienst: " +circusPool.getRemainingUnterhalt() + " Silber" );
			
			}
			if (circusPool.getUnterhaltungslimit() != circusPool.getRegionMaxUnterhaltung()){
				super.addComment("Unterhaltungslimit von " + circusPool.getUnterhaltungslimit() +" Silber wirksam!");
				
			}
		
			// Nun Befehle setzen!
			
			// Möglicher Verdienst größer als das was Pool erlaubt?
			
			// FF: ?!? mögliches Risiko: ist sicher, das Relation != null ?
			
			if (circusPoolRelation.getVerdienst() > circusPoolRelation.getDoUnterhaltung()){
				if (!automode){
					// Negativ wäre ein überzähliger Unterhalter!
					if (circusPoolRelation.getDoUnterhaltung() < 0 ){
						if (!this.confirmIfunemployed){
							super.scriptUnit.doNotConfirmOrders("Warnung: Überzählige Unterhalter Einheit!");
						} else {
							this.lerneUnterhaltung("Warnung: Überzählige Unterhalter Einheit!");
						}
					} else{
						
						// postiv aber nicht ausgelastet!
						super.addComment("Warnung: Einheit ist NICHT ausgelastet!");
						super.addComment("" + Math.round((circusPoolRelation.getVerdienst()-circusPoolRelation.getDoUnterhaltung())/circusPoolRelation.getProKopfVerdienst()) + " Unterhalter überflüssig");		
						super.addOrder("UNTERHALTEN " + circusPoolRelation.getDoUnterhaltung(), true);			
						this.setFinalOrderedUnterhaltung(circusPoolRelation.getDoUnterhaltung());
						double auslastung = ((double)circusPoolRelation.getDoUnterhaltung()/(double)circusPoolRelation.getVerdienst());
						
						// unter 90% auslastung unbestätigt. 
						if ( auslastung < ((double)this.mindestAuslastung/100)){
							if (!this.confirmIfunemployed){
								super.scriptUnit.doNotConfirmOrders("Warnung: Einheit ist NICHT ausgelastet!" + Math.round((circusPoolRelation.getVerdienst()-circusPoolRelation.getDoUnterhaltung())/circusPoolRelation.getProKopfVerdienst()) + " Unterhalter überflüssig");
							}
						}
						
						// FF: unter 100% angabe
						if ( auslastung < 1){
							this.addComment("(geplante Unterhaltung war hier: " + circusPoolRelation.getDoUnterhaltung());
							this.addComment("Auslastung: " + (int)Math.floor(auslastung*100) + "%, unbestätigt unter " + this.mindestAuslastung + "%");	
						}				
						
					}
				} else {
					// wir haben einen Unterhalter in Automode
					double auslastung = ((double)circusPoolRelation.getDoUnterhaltung()/(double)circusPoolRelation.getVerdienst());
					// FF: unter 100% angabe					
					if ( auslastung < 1){
						this.addComment("(geplante Unterhaltung war hier: " + circusPoolRelation.getDoUnterhaltung() + ")");
						this.addComment("Auslastung: " + (int)Math.floor(auslastung*100) + "%, kein Unterhalten unter unter " + this.mindestAuslastung + "%");	
					}
					if (this.targetRegion==null){
						// keine Zielregion...also lernen...oder trotzdem unterhalten?
						// unter X% auslastung unbestätigt. 
						if ( auslastung < ((double)this.mindestAuslastung/100)){
							
							addComment("Automode->Einheit lernt");
							int reittalent=this.scriptUnit.getSkillLevel("Reiten");
							if (reittalent>0){
								super.lerneTalent("Unterhaltung", true);
							} else {
								// neu, wir lernen auf T1 Reiten
								this.addComment("-> wir lernen prophylaktisch Reiten T1");
								this.lerneTalent("Reiten",false);
							}
						} else {
							super.addComment("Warnung: Einheit ist NICHT ausgelastet!");
							super.addComment("" + Math.round((circusPoolRelation.getVerdienst()-circusPoolRelation.getDoUnterhaltung())/circusPoolRelation.getProKopfVerdienst()) + " Unterhalter überflüssig");		
							super.addOrder("UNTERHALTEN " + circusPoolRelation.getDoUnterhaltung(), true);
							this.setFinalOrderedUnterhaltung(circusPoolRelation.getDoUnterhaltung());
						}

					} else {
						// wir haben doch tatsächlich ne Zielregion...
						
						int reittalent=this.scriptUnit.getSkillLevel("Reiten");
						if (reittalent>0){
							gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(),targetRegion.getCoordinate(), true,"Unterhalten");
							addComment("dieser Region NEU als Unterhalter zugeordnet: " + targetRegion.toString());
							addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
							// Pferde requesten...
							MatPoolRequest MPR = new MatPoolRequest(this,this.scriptUnit.getUnit().getModifiedPersons(), "Pferd", 20, "Unterhalter unterwegs" );
							this.addMatPoolRequest(MPR);
						} else {
							// neu, wir lernen auf T1 Reiten
							gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(),targetRegion.getCoordinate(), false,"Unterhalten");
							this.addComment("-> wir lernen aber erstmal Reiten T1");
							addComment("dieser Region NEU als Unterhalter zugeordnet: " + targetRegion.toString());
							addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
							this.lerneTalent("Reiten",false);
						}
					}
				}
	
			}
			else {
				super.addOrder("UNTERHALTEN " + circusPoolRelation.getDoUnterhaltung(), true);
				this.setFinalOrderedUnterhaltung(circusPoolRelation.getDoUnterhaltung());
			}
		}
	}
	  
		
   	/**
	 * Wenn die Einheit zu schlecht ist oder Unterhaltung nicht 
	 * kennt.
	 * 20070303 Lernpool ist angebunden
	 * ToDO umstellen auf lernPoolScript
	 */
	
	
	private void lerneUnterhaltung(String Meldung){
		this.addComment(Meldung);
		this.lerneTalent("Unterhaltung", true);
	}
	
	private void Lerne() {
		this.scriptUnit.addComment("Lernfix wird initialisiert mit dem Parameter: " + this.LernfixOrder);
		Script L = new Lernfix();
		ArrayList<String> order = new ArrayList<String>();
		order.add(this.LernfixOrder);
		L.setArguments(order);
		L.setScriptUnit(this.scriptUnit);
		L.setGameData(this.scriptUnit.getScriptMain().gd_ScriptMain);
		if (this.scriptUnit.getScriptMain().client!=null){
			L.setClient(this.scriptUnit.getScriptMain().client);
		}
		this.scriptUnit.addAScript(L);
	}


	public boolean isAutomode() {
		return automode;
	}


	public void setAutomode(boolean automode) {
		this.automode = automode;
	}


	public GotoInfo getGotoInfo() {
		return gotoInfo;
	}


	/**
	 * @return the targetRegion
	 */
	public Region getTargetRegion() {
		return targetRegion;
	}


	/**
	 * @param targetRegion the targetRegion to set
	 */
	public void setTargetRegion(Region targetRegion) {
		this.targetRegion = targetRegion;
	}
			
	
	public boolean isUnterMindestAuslastung(){
		boolean erg = false;
		if (circusPoolRelation!=null){
			double auslastung = ((double)circusPoolRelation.getDoUnterhaltung()/(double)circusPoolRelation.getVerdienst());
			if ( auslastung < ((double)this.mindestAuslastung/100)){
				erg = true;
			}
		}
		
		return erg;
	}


	public int getFinalOrderedUnterhaltung() {
		return finalOrderedUnterhaltung;
	}


	public void setFinalOrderedUnterhaltung(int finalOrderedUnterhaltung) {
		this.finalOrderedUnterhaltung = finalOrderedUnterhaltung;
	}
	
}
