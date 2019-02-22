package com.fftools.scripts;

import java.util.ArrayList;

import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.pools.treiber.TreiberPool;
import com.fftools.pools.treiber.TreiberPoolRelation;
import com.fftools.utils.FFToolsOptionParser;

/**
 * 
 * Treiber
 * die jetzt auch die anderen Treiber 
 * der Region im Auge hat.
 * @author Fiete
 *
 */

public class Treiben extends TransportScript{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private int Durchlauf_VorMatPool = 112;
	private int Durchlauf_vorTreiberPool = 210;
	private int Durchlauf_nachTreiberPool = 260;
	
	private int[] runners = {Durchlauf_VorMatPool,Durchlauf_vorTreiberPool, Durchlauf_nachTreiberPool};
	
	
	private final String[] talentNamen = {"Hiebwaffen","Stangenwaffen","Bogenschießen","Armbrustschießen","Katapultbedienung"};
	
	private int WaffenPrio = 400;
	
	// Ab welchem Talenwert für Steuereinrteiben soll Silber gemacht werden?
	// Default Runde 502 auf 2
	// mit setScripterOption minTreiberTalent=X gesetzt werden
		
	private int mindestTalent=2;
	private TreiberPool treiberPool = null;
	private int limit;
	// private static final OutTextClass outText = OutTextClass.getInstance();
	// Wird historisch von TreiberPoolManager angelegt und muß dann mühsam über CP.getcpr geholt werden
	private TreiberPoolRelation treiberPoolRelation = null;
	
	private final int defaultMindestAuslastung = 90;
	
	private int mindestAuslastung = defaultMindestAuslastung; 
	private Skill skill=null;
	
	
	private boolean noWeapons = false;
	
	private boolean confirmIfunemployed = false;
	
	private ArrayList<MatPoolRequest> requests = new ArrayList<MatPoolRequest>();
	
	private boolean unitIsLearning = false;
	
	// Konstruktor
	public Treiben() {
		super.setRunAt(this.runners);
	}
	
	
public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf_vorTreiberPool){
			this.vorTreiberPool();
		}
		
		if (scriptDurchlauf==Durchlauf_VorMatPool){
			this.vorMatPool();
		}

		if (scriptDurchlauf==Durchlauf_nachTreiberPool){
			this.nachTreiberPool();
		}
		
	}
	
	
	private void vorTreiberPool(){
	
	// Erster Lauf mit direktem Lernen ODER Pool-Anmeldung
		
	
		int reportMinLevel = reportSettings.getOptionInt("minTreiberTalent",region());
		if (reportMinLevel>0){
			this.mindestTalent = reportMinLevel;
		}
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Treiben");
		int unitMinLevel = OP.getOptionInt("minTalent", -1);
		if (unitMinLevel>this.mindestTalent){
			this.mindestTalent=unitMinLevel;
		}
		unitMinLevel = OP.getOptionInt("mindestTalent", -1);
		if (unitMinLevel>this.mindestTalent){
			this.mindestTalent=unitMinLevel;
		}
		
		
		// FF: eventuell hier setting für die Region ansetzen....falls nötig
		
		// skillType holen!
		SkillType skillType = super.gd_Script.rules.getSkillType("Steuereintreiben");
		
		// Kann die Einheit das Talent Treiben?
		skill = super.scriptUnit.getUnit().getModifiedSkill(skillType);
		if (skill!= null) {
			
			// Einheit kann Treiben ABER lohnt es sich? MindestTalent prüfen!
			if (skill.getLevel() >= mindestTalent){
				
				// Waffentest
				int waffenanzahl = 0;
				if (this.requests.size()>0){
					for (MatPoolRequest MPR : this.requests){
						waffenanzahl+=MPR.getBearbeitet();
						if (MPR.getBearbeitet()>0){
							this.addComment("Treiben - gefundene Waffen:" + MPR.toString());
						}
					}
				}
				
				// :ToDo
				// Fremde Requests ? (Material, Request) 
				// Bug Report von Thorsten Holst, 20121104
				MatPool MP = this.getMatPool();
				if (MP.getRequests(this.scriptUnit)!=null){
					for (MatPoolRequest mpr : MP.getRequests(this.scriptUnit)){
						// this.addComment("Debug-Treiben...checking another request: " + mpr.toString(),false);
						if (mpr.getBearbeitet()>0 && !this.requests.contains(mpr)){
							// handelt es sich um einen passenden Request?
							if (mpr.getItemTypes()!=null){
								boolean myWeapons = false;
								for (ItemType it : mpr.getItemTypes()){
									// passt eine der Waffen zu meinem Talent
									// this.addComment("Debug-Treiben...checking an Item of last Request: " + it.toString(),false);
									Skill sk = it.getUseSkill();
									if (sk!=null){
										// this.addComment("Debug-Treiben...checking the requested skill: " + sk.toString(),false);
										SkillType sT = sk.getSkillType();
										sk=this.getUnit().getSkill(sT);
										if (sk.getLevel()>0){
											myWeapons=true;											
										} else {
											// this.addComment("Debug-Treiben...no skill, level: " + sk.getLevel(),false);
										}
									}
								}
								if (myWeapons){
									waffenanzahl+=mpr.getBearbeitet();
									this.addComment("Treiben - gefundene Waffen:" + mpr.toString());
								}
							}
						}
					}
				}
				
				
				if (waffenanzahl>this.scriptUnit.getUnit().getModifiedPersons()){
					waffenanzahl = this.scriptUnit.getUnit().getModifiedPersons();
					this.addComment("!!! Zu viele Waffen beim Treiber?!");
					this.doNotConfirmOrders();
				}
				
				treiberPoolRelation.setPersonenZahl(waffenanzahl);
				if (waffenanzahl<=0){
					this.addComment("Keine Waffen für Treiber gefunden.");
					this.addOrder("LERNEN Steuereintreiben", true);
					this.doNotConfirmOrders();
					this.noWeapons=true;
					this.unitIsLearning=true;
				}
				
				
				// Möchte die Einheit die Gesamt-Treibmenge in der Region beschränken?
				// Dann sollten wir ein Argument finden!
				// FF Neu..per Optionen...
				
				
				// entweder wird der geparste wert genommen, oder regionsmaximum
				limit = OP.getOptionInt("limit",1000000);
				
				// wenn wir schon den OP haben..gleich alles
				// gibts den nen reportSetting zur Mindestauslastung?
				int reportMindestAuslastung = reportSettings.getOptionInt("TreiberMindestAuslastung", this.region());
				if (reportMindestAuslastung!=ReportSettings.KEY_NOT_FOUND){
					this.mindestAuslastung = reportMindestAuslastung;
				}
				
				// haben wir vielleicht noch einen direkten Parameter in den Optionen?
				this.mindestAuslastung = OP.getOptionInt("mindestAuslastung", this.mindestAuslastung);
				
				// Wurde das Regionslimit bereits durch eine andere Einheit verändert?
										
				if (treiberPool.getRegionMaxTreiben()!= treiberPool.getUnterhaltungslimit()){
				     super.addComment("Warnung: Mehrere Einheiten setzen Unterhalungslimits für " + super.scriptUnit.getUnit().getRegion().getName());
				}     
				// ist das neue Limit strenger und positv?
				if ((limit < treiberPool.getUnterhaltungslimit())&&(limit > 0 )){
				      // neues Limit gilt!
				      treiberPool.setTreibenLimit(limit);
				}
				// Bestätigen, wenn wegen Überzähliger Treibereinheit eigentlich arbeitslos?
				this.confirmIfunemployed = OP.getOptionBoolean("confirmUnemployed",false);
				if (this.confirmIfunemployed){
					this.addComment("Treiben: Einheit wird auf Benutzerwunsch nicht unbestätigt bleiben.");
				}
				
				// mode
				if (OP.getOptionString("mode").equalsIgnoreCase("kepler") || 
						(reportSettings.getOptionString("TreiberMode",this.region())!=null && 
								reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("Kepler"))){
					if (treiberPool!=null){
						treiberPool.setKeplerMode(true);
						treiberPool.keplerRegionMaxTreiben();
					}
				}
				if (OP.getOptionString("mode").equalsIgnoreCase("kepler2") || 
						(reportSettings.getOptionString("TreiberMode",this.region())!=null && 
								reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("Kepler2"))){
					if (treiberPool!=null){
						treiberPool.setKeplerMode(true);
						treiberPool.maxTreibsilberFreigabe();
					}
				}
				
			} else {
				// zu schlecht => lernen
				this.lerneTalent("Mindesttalentwert " + mindestTalent + " unterschritten");
				this.unitIsLearning=true;
			}
			
		} else {
			// Einheit kann garnicht Unterhalten!
			this.lerneTalent("Mindesttalentwert " + mindestTalent + " unterschritten (Keine Fähigkeit gefunden)");
			this.unitIsLearning=true;
		}	
	}	
	
	/**
	 * Nur check Anzahl Personen.....
	 * nach Waffen
	 */
	private void vorMatPool(){
		
        //		 Hurra! Ein Kandidat für den CircusPool! Aber welcher ist zuständig?
		// Registrieren läuft gleich mit durch Manager
		treiberPool = super.scriptUnit.getScriptMain().getOverlord().getTreiberPoolManager().getTreiberPool(super.scriptUnit);
					
		// Relation gleich mal referenzieren für später.
		treiberPoolRelation = treiberPool.getTreiberPoolRelation(super.scriptUnit);
		
		// Entweder für alle Waffentalente waffen anfordern oder nur für das Argument(Itemgroup oder Item)
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Treiben");
		
		if (OP.getOptionInt("WaffenPrio", -1)>0){
			this.WaffenPrio = OP.getOptionInt("WaffenPrio", this.WaffenPrio);
			this.addComment("Waffenprio gesetzt auf: " + this.WaffenPrio);
		}
		int newFaktor = OP.getOptionInt("Faktor_Silberbestand_Region", -1);
		if (newFaktor>0){
			this.treiberPool.setFaktor_silberbestand_region(newFaktor);
			this.addComment("Silberstandsfaktor gesetzt auf: " + newFaktor);
		}
		
		// Gibt es bereits MPR für diese Unit?
		// :ToDo
		// Fremde Requests ? (Material, Request) 
		// Bug Report von Thorsten Holst, 20121104
		int otherWeaponRequests = 0;
		MatPool MP = this.getMatPool();
		if (MP.getRequests(this.scriptUnit)!=null){
			for (MatPoolRequest mpr : MP.getRequests(this.scriptUnit)){
				// handelt es sich um einen passenden Request?
				boolean myWeapons = false;
				if (mpr.getItemTypes()!=null){
					for (ItemType it : mpr.getItemTypes()){
						// passt eine der Waffen zu meinem Talent
						Skill sk = it.getUseSkill();
						if (sk!=null){
							SkillType sT = sk.getSkillType();
							sk=this.getUnit().getSkill(sT);
							if (sk.getLevel()>0){
								myWeapons=true;
							}
						}
					}
				}
				if (myWeapons){
					otherWeaponRequests+=mpr.getOriginalGefordert();
					addComment("Treiben. found other request for Weapons: " + mpr.toString());
				}
			}
		}
		if (otherWeaponRequests>0){
			addComment("Treiben: " + otherWeaponRequests + " bereit angeforderte Waffen gefunden - nicht erneut angefordert");
		}
		
		int remainingWeaponsNeeded = this.scriptUnit.getUnit().getModifiedPersons() - otherWeaponRequests;
		String comment = "Treiberbewaffnung";
		if (remainingWeaponsNeeded>0){
			if (OP.getOptionString("Waffe").length()>0){
				// Mit Waffen-Argument
				// einfach übergeben
				MatPoolRequest MPR = new MatPoolRequest(this,remainingWeaponsNeeded,OP.getOptionString("Waffe"),this.WaffenPrio,comment);
				this.addMatPoolRequest(MPR);
				this.requests.add(MPR);
			} else {
				// Kein Waffen-Argument
				// Liste bauen
				boolean didSomething = false;
				for (int i = 0;i<this.talentNamen.length;i++){
					String actName = this.talentNamen[i];
					SkillType actSkillType = this.gd_Script.rules.getSkillType(actName);
					if (actSkillType!=null){
						Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
						if (actSkill!=null && actSkill.getLevel()>0){
							// Was gefunden
							// Zum Talent das passendste Gerät definieren
							String materialName = actSkillType.getName();
							String matNameNeu="nix";
							if (materialName.equalsIgnoreCase("Hiebwaffen")) {
								matNameNeu = "Schwert";
							} else if(materialName.equalsIgnoreCase("Stangenwaffen")){
								matNameNeu = "Speer";
							} else if(materialName.equalsIgnoreCase("Bogenschießen")){
								matNameNeu = "Bogen";
							} else if(materialName.equalsIgnoreCase("Armbrustschießen")){
								matNameNeu = "Armbrust";
							} else if (materialName.equalsIgnoreCase("Katapultbedienung")){
								matNameNeu="Katapult";
							} 
							if (matNameNeu!="nix"){
								// Bestellen
								MatPoolRequest MPR = new MatPoolRequest(this,remainingWeaponsNeeded,matNameNeu,this.WaffenPrio,comment);
								this.addMatPoolRequest(MPR);
								didSomething=true;
								this.requests.add(MPR);
							}
						}
					}
				}
				if (!didSomething){
					this.addComment("Treiben: keine Waffenanforderung! - kein Waffentalent?!");
					this.doNotConfirmOrders();
				}
			}
		} else {
			this.addComment("Treiben: keine weiteren Waffen angefordert - anderweitig genügend angefordert");
		}
	}
	
	
	/**
	 * Zweiter Lauf nach dem runCircusPool
	 */
	
	private void nachTreiberPool(){
	
		this.addComment("Debug: nach TreiberPool. Einheit lernt:" + this.unitIsLearning);
		// Nimmt diese Einheit an einem Pool teil?
		if (treiberPool != null && !this.unitIsLearning){
		
			// Diverse Ausgaben 
		    
			super.addComment("Erwartetes Gesamteinkommen(Treiben) in " + super.scriptUnit.getUnit().getRegion().getName() +": " + treiberPool.getRegionsVerdienst() + " Silber");
			if (treiberPool.getRemainingTreiben() > 0){
				super.addComment("Möglicher Mehrverdienst: " +treiberPool.getRemainingTreiben() + " Silber" );
			
			}
			if (treiberPool.getUnterhaltungslimit() != treiberPool.getRegionMaxTreiben()){
				super.addComment("Treiben-limit von " + treiberPool.getUnterhaltungslimit() +" Silber wirksam!");
				
			}
		    
			
			if (treiberPool.isSilberknapp()){
				super.addComment("Treiben: in der Region wurde die MindestSilbermenge unterschritten");
			}
			
			if (treiberPool.isKeplerMode()){
				super.addComment("Kepler-Modus erkannt:");
				treiberPool.addKeplerInfo(this.scriptUnit);
			}
			
			
			if (!this.noWeapons){
			
				// Nun Befehle setzen!
				
				// Möglicher Verdienst größer als das was Pool erlaubt?
				
				// FF: ?!? mögliches Risiko: ist sicher, das Relation != null ?
				
				if (treiberPoolRelation.getVerdienst() > treiberPoolRelation.getDoTreiben()){
					
					// Negativ wäre ein überzähliger Unterhalter!
					if (treiberPoolRelation.getDoTreiben() < 0 ){
						this.lerneTalent("Warnung: Überzählige Treiber Einheit!");
						if (!this.confirmIfunemployed){
							super.scriptUnit.doNotConfirmOrders();
						}
					} else{
						
						// postiv aber nicht ausgelastet!
						super.addComment("Warnung: Einheit ist NICHT ausgelastet!");
						super.addComment("" + Math.round((treiberPoolRelation.getVerdienst()-treiberPoolRelation.getDoTreiben())/treiberPoolRelation.getProKopfVerdienst()) + " Treiber überflüssig");		
						super.addOrder("TREIBEN " + treiberPoolRelation.getDoTreiben(), true);			
						
						double auslastung = ((double)treiberPoolRelation.getDoTreiben()/(double)treiberPoolRelation.getVerdienst());
						
						// unter 90% auslastung unbestätigt. 
						if ( auslastung < ((double)this.mindestAuslastung/100)){
							if (!this.confirmIfunemployed){
								super.scriptUnit.doNotConfirmOrders();
							}
						}
						
						// FF: unter 100% angabe
						if ( auslastung < 1){
							this.addComment("Auslastung: " + (int)Math.floor(auslastung*100) + "%, unbestätigt unter " + this.mindestAuslastung + "%");	
						}				
						
					}
		
				} else {
				 super.addOrder("TREIBEN " + treiberPoolRelation.getDoTreiben(), true);
				}
			}
		}	
	}
	  
		
   	/**
	 * Wenn die Einheit zu schlecht ist oder Steuereintreiben nicht 
	 * kennt.
	 * 20070303 Lernpool ist angebunden
	 * ToDO umstellen auf lernPoolScript
	 */
	
	
	private void lerneTalent(String Meldung){
		this.addComment(Meldung);
		this.lerneTalent("Steuereintreiben", true);
	}
			
		
}
