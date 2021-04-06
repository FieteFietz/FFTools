package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.ReportSettings;
import com.fftools.pools.ausbildung.AusbildungsPool;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.pools.treiber.TreiberPool;
import com.fftools.pools.treiber.TreiberPoolRelation;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsUnits;
import com.fftools.utils.GotoInfo;

import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

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
	
	private String UnterBeschäftigung="Treiben";
	
	private String LernplanName = null;
	private AusbildungsPool ausbildungsPool=null;
	
	private String WaffenTalent = null;
	
	// wird per parameter gesetzt
	private boolean automode = false;
	
	// WENN in AutoMode UND soll in andere Region, dann hier das Ziel
	private Region targetRegion = null;
	private GotoInfo gotoInfo = null;
	
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
		
		String rUnterBeschäftigung = reportSettings.getOptionString("TreiberUnterBeschäftigung", region());
		if (rUnterBeschäftigung!=null && rUnterBeschäftigung.length()>1) {
			this.UnterBeschäftigung=rUnterBeschäftigung;
		}
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Treiben");
		OP.addOptionList(this.getArguments());
		
		int unitMinLevel = OP.getOptionInt("minTalent", -1);
		if (unitMinLevel>0){
			this.mindestTalent=unitMinLevel;
		}
		unitMinLevel = OP.getOptionInt("mindestTalent", -1);
		if (unitMinLevel>0){
			this.mindestTalent=unitMinLevel;
		}
		
		rUnterBeschäftigung = OP.getOptionString("UnterBeschäftigung");
		if (rUnterBeschäftigung.length()>1) {
			this.UnterBeschäftigung=rUnterBeschäftigung;
		}
		
		if (this.UnterBeschäftigung.equalsIgnoreCase("Treiben") || this.UnterBeschäftigung.equalsIgnoreCase("Lernen")) {
			this.addComment("Treiber-Einstellung - bei Unterbeschäftigung: " + this.UnterBeschäftigung);
		} else {
			this.addComment("Treiber-Einstellung - bei Unterbeschäftigung hat KEINEN gültigen Wert!");
			this.doNotConfirmOrders("Treiber-Einstellung - bei Unterbeschäftigung hat KEINEN gültigen Wert!");
		}
		
		
		
		
		// skillType holen!
		SkillType skillType = super.gd_Script.getRules().getSkillType("Steuereintreiben");
		
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
										// sk=this.getUnit().getSkill(sT);
										sk=this.getUnit().getModifiedSkill(sT);
										if (sk!=null && sk.getLevel()>0){
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
					this.doNotConfirmOrders("!!! Zu viele Waffen beim Treiber?!");
				}
				if (waffenanzahl<this.scriptUnit.getUnit().getModifiedPersons()){
					this.doNotConfirmOrders("!!! Treiber hat nicht genügend Waffen! (" + waffenanzahl + "/" + this.scriptUnit.getUnit().getModifiedPersons() + ")");
				}
				int treibfPers = Math.min(waffenanzahl, this.getUnit().getModifiedPersons());
				treiberPoolRelation.setPersonenZahl(treibfPers);
				this.addComment("Treiben: treibfähige Personen: " + treibfPers);
				if (waffenanzahl<=0 && !this.unitIsLearning){
					this.addOrder("LERNEN Steuereintreiben", true);
					this.doNotConfirmOrders("Keine Waffen für Treiber gefunden.");
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

				
				// Settings mode
				if (treiberPool!=null) {
					if (reportSettings.getOptionString("TreiberMode",this.region())!=null) {
						if (reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("Kepler")){
							treiberPool.setKeplerMode(true);
							treiberPool.keplerRegionMaxTreiben();
						}
						if (reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("Kepler2")){
							treiberPool.setKeplerMode(true);
							treiberPool.maxTreibsilberFreigabe();
						}
						if (reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("Kepler3")){
							treiberPool.setKeplerMode(true);
							treiberPool.maxTreibsilberFreigabe_Kepler3();
						}
						if (reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("none")){
							treiberPool.setKeplerMode(false);
						}
						if (reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("aus")){
							treiberPool.setKeplerMode(false);
						}
						if (reportSettings.getOptionString("TreiberMode",this.region()).equalsIgnoreCase("normal")){
							treiberPool.setKeplerMode(false);
						}
					}
					
					if (OP.getOptionString("mode").equalsIgnoreCase("kepler")) {
						treiberPool.setKeplerMode(true);
						treiberPool.keplerRegionMaxTreiben();
					}
					if (OP.getOptionString("mode").equalsIgnoreCase("kepler2")) {
						treiberPool.setKeplerMode(true);
						treiberPool.maxTreibsilberFreigabe();
					}
					if (OP.getOptionString("mode").equalsIgnoreCase("kepler3")) {
						treiberPool.setKeplerMode(true);
						treiberPool.maxTreibsilberFreigabe_Kepler3();
					}
					if (OP.getOptionString("mode").equalsIgnoreCase("none")) {
						treiberPool.setKeplerMode(false);
					}
					if (OP.getOptionString("mode").equalsIgnoreCase("aus")) {
						treiberPool.setKeplerMode(false);
					}
					if (OP.getOptionString("mode").equalsIgnoreCase("normal")) {
						treiberPool.setKeplerMode(false);
					}
				}
				
				
				
			} else {
				// zu schlecht => lernen
				if (!this.unitIsLearning) {
					this.lerneTalent("Mindesttalentwert Treiben " + mindestTalent + " unterschritten");
					this.unitIsLearning=true;
				}
			}
			
		} else {
			// Einheit kann garnicht Treiben!
			
			if (!this.unitIsLearning) {
				this.lerneTalent("Mindesttalentwert " + mindestTalent + " unterschritten (Keine Fähigkeit gefunden)");
			} else {
				this.addComment("Mindesttalentwert Treiben " + mindestTalent + " unterschritten (Keine Fähigkeit gefunden), Einheit lernt aber bereits.");
			}
		}	
	}	
	
	/**
	 * Nur check Anzahl Personen.....
	 * nach Waffen
	 */
	private void vorMatPool(){
		
        //		 Hurra! Ein Kandidat für den TreiberPool! Aber welcher ist zuständig?
		// Registrieren läuft gleich mit durch Manager
		treiberPool = super.scriptUnit.getScriptMain().getOverlord().getTreiberPoolManager().getTreiberPool(this);
					
		// Relation gleich mal referenzieren für später.
		treiberPoolRelation = treiberPool.getTreiberPoolRelation(super.scriptUnit);
		
		// Entweder für alle Waffentalente waffen anfordern oder nur für das Argument(Itemgroup oder Item)
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Treiben");
		
		if (OP.getOptionInt("WaffenPrio", -1)>0){
			this.WaffenPrio = OP.getOptionInt("WaffenPrio", this.WaffenPrio);
			this.addComment("Waffenprio gesetzt auf: " + this.WaffenPrio);
		}
		
		// FF: eventuell hier setting für die Region ansetzen....falls nötig
		this.LernplanName = OP.getOptionString("LernPlan");
		
		// WaffenTalent
		this.WaffenTalent = OP.getOptionString("WaffenTalent");
		
		if (OP.getOptionString("mode").equalsIgnoreCase("auto")){
			this.automode = true;
		}

		// 20191210: Talentlevel zur Prio addieren
		// skillType holen!
		SkillType skillType = super.gd_Script.getRules().getSkillType("Steuereintreiben");
		
		// Kann die Einheit das Talent Treiben?
		skill = super.scriptUnit.getUnit().getModifiedSkill(skillType);
		if (skill!= null) {
			
			// Einheit kann Treiben ABER lohnt es sich? MindestTalent prüfen!
			if (skill.getLevel() > 0){
				this.WaffenPrio += skill.getLevel();
				this.addComment("Waffenprio um Talentlevel (Steuereintreiben) erhöht auf " + this.WaffenPrio);
			}
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
							if (sk!=null) {
								if (sk.getLevel()>0){
									myWeapons=true;
								}
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
					SkillType actSkillType = this.gd_Script.getRules().getSkillType(actName);
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
					if (confirmIfunemployed) {
						this.addComment("Treiben: keine Waffenanforderung! - kein Waffentalent?!");
					} else {
						this.doNotConfirmOrders("Treiben: keine Waffenanforderung! - kein Waffentalent?!");
					}
				}
			}
		} else {
			this.addComment("Treiben: keine weiteren Waffen angefordert - anderweitig genügend angefordert");
		}
		
		if (this.WaffenTalent!=null && this.WaffenTalent!="") {
			// skillType holen!
			SkillType skillTypeWT = super.gd_Script.getRules().getSkillType(this.WaffenTalent);
			if (skillTypeWT!=null) {
				// Kann die Einheit das Talent Treiben?
				skill = super.scriptUnit.getUnit().getModifiedSkill(skillTypeWT);
				boolean skillOK = false;
				if (skill!= null) {
					// Einheit kann kämpfen?!
					if (skill.getLevel() >= 1){
						skillOK = true;
					}
				}
				if (!skillOK) {
					// wir müssen lernen
					this.addComment("Einheit beherrscht angegebenes Waffentalent nicht - dies wird nun gelernt!");
					this.unitIsLearning=true;
					this.lerneTalent(skillTypeWT, false);
				}
			} else {
				this.doNotConfirmOrders("Angegebenes WaffenTalent unbekannt!");
			}
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
					if (!this.automode || this.targetRegion==null) {
						// Negativ wäre ein überzähliger Unterhalter!
						if (this.automode) {
							this.addComment("Treiber im AutoMode, aber keine Zielregion zugeordnet.");
						}
						if (treiberPoolRelation.getDoTreiben() < 0 ){
							this.lerneTalent("Warnung: Überzählige Treiber Einheit!");
							if (!this.confirmIfunemployed && this.getUnit().getRegion().getRegionType().isLand()){
								super.scriptUnit.doNotConfirmOrders("Warnung: Überzählige Treiber Einheit!");
							} 
						} else{
							
							// postiv aber nicht ausgelastet!
							super.addComment("Warnung: Einheit ist NICHT ausgelastet!");
							super.addComment("" + Math.round((treiberPoolRelation.getVerdienst()-treiberPoolRelation.getDoTreiben())/treiberPoolRelation.getProKopfVerdienst()) + " Treiber überflüssig");
							double auslastung = ((double)treiberPoolRelation.getDoTreiben()/(double)treiberPoolRelation.getVerdienst());
							
							// unter 90% auslastung unbestätigt. 
							if ( auslastung < ((double)this.mindestAuslastung/100)){
								
								if (this.UnterBeschäftigung.equalsIgnoreCase("Treiben")) {
									super.addOrder("TREIBEN " + treiberPoolRelation.getDoTreiben(), true);
									FFToolsUnits.leaveAcademy(this.scriptUnit, " Treiber verlässt Aka");
								}
								if (this.UnterBeschäftigung.equalsIgnoreCase("Lernen")) {
									this.addComment("vorgesehener Betrag zum Eintreiben für mich: " + treiberPoolRelation.getDoTreiben() + " Silber, da LERNE ich lieber....");
									this.lerneTalent("Warnung: Einheit ist nicht ausgelastet!");
									
								}
								if (!this.confirmIfunemployed && this.getUnit().getRegion().getRegionType().isLand()){
									super.scriptUnit.doNotConfirmOrders("Warnung: Einheit ist NICHT ausgelastet!" + Math.round((treiberPoolRelation.getVerdienst()-treiberPoolRelation.getDoTreiben())/treiberPoolRelation.getProKopfVerdienst()) + " Treiber überflüssig");
								}
							} else {
								// nicht voll ausgelastet, aber oberhalb min Auslastung
								super.addOrder("TREIBEN " + treiberPoolRelation.getDoTreiben(), true);
								FFToolsUnits.leaveAcademy(this.scriptUnit, " Treiber verlässt Aka");
							}
							
							// FF: unter 100% angabe
							if ( auslastung < 1){
								this.addComment("Auslastung: " + (int)Math.floor(auslastung*100) + "%, unbestätigt unter " + this.mindestAuslastung + "%");	
							}				
							
						}
					} else {
						// im AutoMode
						int reittalent=this.scriptUnit.getSkillLevel("Reiten");
						if (reittalent>0){
							gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(),targetRegion.getCoordinate(), true,"Treiben");
							addComment("dieser Region NEU als Treiber zugeordnet: " + targetRegion.toString());
							addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
							FFToolsUnits.leaveAcademy(this.scriptUnit, " Treiber en Route verlässt Aka");
							// Pferde requesten...
							MatPoolRequest MPR = new MatPoolRequest(this,this.scriptUnit.getUnit().getModifiedPersons(), "Pferd", 20, "Treiber unterwegs" );
							this.addMatPoolRequest(MPR);
							
						} else {
							// neu, wir lernen auf T1 Reiten
							gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.region().getCoordinate(),targetRegion.getCoordinate(), false,"Treiben");
							this.addComment("-> wir lernen aber erstmal Reiten T1");
							addComment("dieser Region NEU als Treiber zugeordnet: " + targetRegion.toString());
							addComment("ETA: " + gotoInfo.getAnzRunden() + " Runden.");
							this.lerneTalent("Reiten",false);
						}
					}
		
				} else {
				   super.addOrder("TREIBEN " + treiberPoolRelation.getDoTreiben(), true);
				   FFToolsUnits.leaveAcademy(this.scriptUnit, " Treiber verlässt Aka");
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
		this.unitIsLearning=true;
		if (this.LernplanName==null || this.LernplanName=="") {
			this.lerneTalent("Steuereintreiben", true);
		} else {
			this.addComment("Lernplan erkannt, aktiviere den Ausbildungspool");
			ausbildungsPool = super.scriptUnit.getScriptMain().getOverlord().getAusbildungsManager().getAusbildungsPool(super.scriptUnit);
			AusbildungsRelation AR = super.getOverlord().getLernplanHandler().getAusbildungsrelation(this.scriptUnit, this.LernplanName);
			if (AR!=null){
				AR.informScriptUnit();
				ausbildungsPool.addAusbildungsRelation(AR);
			} else {
				// keine AR -> Lernplan beendet ?!
				this.scriptUnit.doNotConfirmOrders("Lernplan liefert keine Aufgabe mehr");
				// default ergänzen - keine Ahnung, was, eventuell kan
				// die einheit ja nix..
				this.addOrder("Lernen Ausdauer", true);
			}
		}
	}


	public boolean isAutomode() {
		return automode;
	}


	public void setAutomode(boolean automode) {
		this.automode = automode;
	}


	public Region getTargetRegion() {
		return targetRegion;
	}


	public void setTargetRegion(Region targetRegion) {
		this.targetRegion = targetRegion;
	}


	public GotoInfo getGotoInfo() {
		return gotoInfo;
	}


	public void setGotoInfo(GotoInfo gotoInfo) {
		this.gotoInfo = gotoInfo;
	}
	
	public boolean isUnterMindestAuslastung(){
		boolean erg = false;
		if (treiberPoolRelation!=null){
			double auslastung = ((double)treiberPoolRelation.getDoTreiben()/(double)treiberPoolRelation.getVerdienst());
			if ( auslastung < ((double)this.mindestAuslastung/100)){
				erg = true;
			}
		}
		
		return erg;
	}
			
		
}
