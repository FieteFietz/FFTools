package com.fftools.pools.ausbildung;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import magellan.library.Building;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaRelationFactory;
import magellan.library.io.cr.CRParser;
import magellan.library.rules.SkillType;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.pools.ausbildung.relations.AutoDidaktenComparator;
import com.fftools.pools.ausbildung.relations.SchuelerComparator;
import com.fftools.pools.ausbildung.relations.SchuelerLernKostenComparator;
import com.fftools.pools.ausbildung.relations.TeacherComparator;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Script;

public class AusbildungsPool{ 

private MatPoolRequest vorlaufRequest = null;
private static final OutTextClass outText = OutTextClass.getInstance();
public static final ReportSettings reportSettings = ReportSettings.getInstance();
private AusbildungsManager ausbildungsManager = null;
private Region region=null;
private ArrayList<AusbildungsRelation> relationList=null;

// Reihenfolge der Skills nach Prio in der Region.
private ArrayList<Skill> sortedSkill =null;
private ArrayList<SkillType> sortedSkillType =null;
// setzt default Reihenfolge des Lernkettenaufbaus!
private String[] defaultSkillOrderString = {"gwyrrd", "cerddor", "illaun", "tybied", "draig", "Alchemie","Taktik" ,"Hiebwaffen", "Armbrustschießen",  "Bogenschießen", 
		                                    "Stangenwaffen", "Ausdauer", "Tarnung", "Reiten", "Wahrnehmung"};  
// Soviel besser darf der Lehrer max besser sein als das aktuelle talent
private int maxTalentDiff = 6;


// Wieweit sollen dabei Nachzügler einbezogen werden? und maxTalentDiff aufweichen?
 private int minNachzuegler = 8;



// Drunter lehrt der Lehrer nicht!
private  double minAuslastung = 0.95; 
private  double maxAuslastung = 1.05;
 
private  double minMagierAuslastung = 1.00; 
private  double maxMagierAuslastung = 10.0;

// Damit arbeiter der Pool und wechselt zwischen magiern und normalos

private  double minAkteulleAuslastung = minAuslastung; 
private  double maxAktuelleAuslastung = maxAuslastung;


// Schaltet den Pool geschwätzig
// private boolean verbose=true;
private boolean verbose=false;

// wird benutzt um Overrun der Rekursion einmalig zu melen und nicht den ganzen Rekursionsbaum entlang.

private boolean overrun=false;


// Listen die für die jeweilige stufe/talent lehrer und schüler aufnehmen.



// Listen die durch rekursionen beschreiben werden und daher global sein müssen

ArrayList <AusbildungsRelation> teacher_gobal = new ArrayList<AusbildungsRelation>();
ArrayList<AusbildungsRelation> schueler_global =   new ArrayList<AusbildungsRelation>();
ArrayList<AusbildungsRelation> teureSchueler_global = new ArrayList<AusbildungsRelation>();





// Counter für rekursionen auf Startwerte
int teachRekursion=0;
int schuelerRekursion=0;
int teureSchuelerRekursion=0;
int maxRekursion=10000;
int bestTeacherFitting=Integer.MAX_VALUE;
int highestTeacherValue=0;
int bestSchuelerFitting=Integer.MAX_VALUE;
int bestSchuelerFittingValue=Integer.MAX_VALUE;
int bestTeureSchuelerFittng=Integer.MAX_VALUE;
int highestTeureSchuelerValue=0;
double bestTeacherAuslastung = 0;


//Welches Talent baut gerade seine Kette?
Skill aktuellerSkill = null;

// Wieviel Silber hat der Pool für teure Talente zur Verfügung
private int LernSilber=Integer.MAX_VALUE;

// Welche Prio hat das Silberanfordern?
private int LernsilberPrio=600;

// Parameter für den PrioAbfall für zukünftige Anfragen
double prioParameterA = LernsilberPrio;
double prioParameterB= -0.5;
double prioParameterC = 0;
double prioParameterD = 100;
	
// Runden die bevorratet werden sollen
int vorratsRunden=4;



// Welche Position hat die Silberanforderung im Matpool?
// Wirkt quasi als Kontonummer und macht es möglich den große Testrequest in 
// kleine reale aufzuteilen.

private int requestReihenfolgeNummer=Integer.MAX_VALUE;

//MatPool holen...
private MatPool matPool = null;

/**
	 * Konstruktor 
	 *
	 */
	
	public AusbildungsPool(AusbildungsManager _am, Region _region){
		ausbildungsManager=_am;
		region =_region;	   
        //  MatPool holen...
  		this.matPool = this.ausbildungsManager.scriptMain.getOverlord().getMatPoolManager().getRegionsMatPool(this.region);
   	    
    }
    
	
	/**
	 * Fügt dem Pool ein Request hinzu
	 * @param _ar
	 */
	
	public void addAusbildungsRelation(AusbildungsRelation _ar){
        
		// falls keine Liste da, zaubere eine Neue...
		if (relationList == null){
			relationList = new ArrayList<AusbildungsRelation>();
		}
		
		this.relationList.add(_ar);
		
		
		
	}


    /**
     * Hier rennt der Pool. 
     * und zwar zunächst der anforderungslauf, dann der nachlauf
     */
	
	public void runPool(int Durchlauf){

		switch (Durchlauf){
			case AusbildungsManager.Durchlauf1:
				this.runPoolVorlauf();
				break;
			case AusbildungsManager.Durchlauf2:
				this.runPoolMittellauf();
				break;
			case AusbildungsManager.Durchlauf3:
				this.runPoolNachlauf();
				break;
		}
	}
	
	
	/**
	 * Vorlauf fordert das Lernsilber für die zukünftigen Runden an und schiebt demn Depot
	 * ein VORRATSSCRIPT unter.
	 */
	
	
	private void runPoolVorlauf(){
		
		// eventuell veränderte VorratsRundenanzahl
		int newVorratsRunden=reportSettings.getOptionInt("Ausbildung_LernSilberVorratsRunden", this.region);
    	if (newVorratsRunden>-1 && newVorratsRunden<=10){
    		this.vorratsRunden = newVorratsRunden;
    	}
		
    	if (this.vorratsRunden==0){
    		// nix zu machen, wenn so eingestellt..;_))
    		return;
    	}
    	
		int silberbedarf = this.getGesamtLernkosten(this.relationList);
	    int zukunftsPrio =0;
		ScriptUnit regionsDepot = this.getRegionsDepot();
	    if (regionsDepot!=null&&silberbedarf>0){
			for (int n=1; n<=this.vorratsRunden;n++){
				zukunftsPrio=(int) Math.round((prioParameterA-prioParameterD)*Math.exp(prioParameterB*n)+prioParameterC*n+prioParameterD);
				this.vorlaufRequest  = new MatPoolRequest(new Script(regionsDepot),silberbedarf, "Silber",zukunftsPrio ,"Lernpoolsilber für nächste Runden");
				this.vorlaufRequest.setScriptUnit(regionsDepot);
	      	    this.matPool.addMatPoolRequest(this.vorlaufRequest); 
		    	// Marc: Request statt Vorlauf
		    	// regionsDepot.findScriptClass("Vorrat","Ware=Silber Summe="+silberbedarf+ " prio=10 prioTM="+zukunftsPrio);
		    }
	    }
	   
	}
	
	
	/**
	 * Gibt mögliche Gesamtlernkosten einer Liste über alle Einheiten und Talente wieder
	 * Dient im Vorlauf zur Ermittlung Silbervorbestellung 
	 * @param liste
	 * @return
	 */
	
   private int getGesamtLernkosten(ArrayList<AusbildungsRelation> liste){
	    int gesamtKosten=0;
		if (liste!=null){   
		   for (Iterator<AusbildungsRelation> iter = liste.iterator(); iter.hasNext();){
			   AusbildungsRelation kandidat = (AusbildungsRelation) iter.next();
			   int teuerstesTalent =0;
			   
			   if (kandidat.getStudyRequest()==null) {
				   // kandidat.getScriptUnit().doNotConfirmOrders();
				   kandidat.getScriptUnit().addComment("DEBUG: no Study Request");
			   } else {
			   
				   for (Iterator<Skill> iter2 = kandidat.getStudyRequest().values().iterator(); iter2.hasNext();){
					   Skill skill = (Skill) iter2.next();
				   // Zählen nur das teuerste teure Talent...und merken uns den Preis in Variable..
					   if (kandidat.getLernKosten(skill)>teuerstesTalent){
						   teuerstesTalent = kandidat.getLernKosten(skill);
					   }
				   }
			   }
			   gesamtKosten = gesamtKosten + teuerstesTalent;
			   
		   }
		return gesamtKosten;
		   // ohne Liste keine Kosten!
		} else  return 0;   
   }
	
   
   /**
	 * Gibt Depot zurück oder null
	 * 	 * @return
	 */
	
	private ScriptUnit getRegionsDepot(){
		// scriptunits der region holen
		Hashtable <Unit, ScriptUnit> regionsScriptUnits = this.ausbildungsManager.scriptMain.getScriptUnits(this.region);
		if (regionsScriptUnits != null){
			for (Iterator<ScriptUnit> iter = regionsScriptUnits.values().iterator(); iter.hasNext();){
				ScriptUnit kandidat = (ScriptUnit) iter.next();
				if (kandidat.getFoundScriptList()!=null){
				   for (Iterator<Script> iter2 = kandidat.getFoundScriptList().iterator();iter2.hasNext();){
					   Script akt_script = (Script) iter2.next();
					   if (akt_script.getClass().getName().equalsIgnoreCase("com.fftools.scripts.Depot")){
						  return kandidat;
					   }
				   }   
				}
			}
		}
	return null;
	}
	
	
	
	/**
	 * Fordert soviel silber wie möglich an für aktelle Runde
	 * Das erhaltene Silber wird nicht wirklich verwendet sondern dient nur als 
	 * Maß für das verfügbare Lernsilber des Pools in der aktuellen
	 * Runde, dann wird der Request gelöscht und durch definierte Einzelrequest gleicher ID
	 * ersetzt, die wirklich anfordern! => Das tut der Nachlauf!
	 * 
	 */
	
	private void runPoolMittellauf(){
		
		int newLernSilberPrio=reportSettings.getOptionInt("Ausbildung_SilberPrio", this.region);
    	if (newLernSilberPrio>-1){
    		this.LernsilberPrio= newLernSilberPrio;
    	}
		this.verboseOutText("AusbildungsPool: LernSilberPrio: " + this.LernsilberPrio);
		
		if (this.relationList!=null){
			if (this.relationList.size()>0){
			   AusbildungsRelation kandidat = this.relationList.get(0);
			   this.vorlaufRequest  = new MatPoolRequest(new Script(kandidat.getScriptUnit()),this.LernSilber, "Silber",this.LernsilberPrio ,"LernpoolMittellauf!");
	    	   this.vorlaufRequest.setScriptUnit(kandidat.getScriptUnit());
	      	   this.matPool.addMatPoolRequest(this.vorlaufRequest);   
			}
		}
		
		
	}
	
    private void runPoolNachlauf(){
    	// Variablen
    	
    	int newMaxTalentDiff=reportSettings.getOptionInt("Ausbildung_maxTalentDiff", this.region);
    	if (newMaxTalentDiff>1 && newMaxTalentDiff<99){
    		maxTalentDiff = newMaxTalentDiff;
    	}
    	
    	int newMinNachzuegler=reportSettings.getOptionInt("Ausbildung_nachZugTalentDiff", this.region);
    	if (newMinNachzuegler>-1 && newMaxTalentDiff<99){
    		minNachzuegler = newMinNachzuegler;
    	}
    	
    	double newMinAuslastung = reportSettings.getOptionInt("Ausbildung_minLehrerAuslastung", this.region);
    	if (newMinAuslastung>=0 && newMinAuslastung<=100){
    		minAuslastung = newMinAuslastung/100;
    	}
    	
    	double newMaxAuslastung = reportSettings.getOptionInt("Ausbildung_maxLehrerAuslastung", this.region);
    	if (newMaxAuslastung>=0 && newMaxAuslastung<=1000){
    		maxAuslastung = newMaxAuslastung/100;
    	}
    	
    	double newMinMagierAuslastung = reportSettings.getOptionInt("Ausbildung_minMagierLehrerAuslastung", this.region);
    	if (newMinMagierAuslastung>=0 && newMinMagierAuslastung<=100){
    		minMagierAuslastung = newMinMagierAuslastung/100;
    	}
    	
    	double newMaxMagierAuslastung = reportSettings.getOptionInt("Ausbildung_maxMagierLehrerAuslastung", this.region);
    	if (newMaxMagierAuslastung>=0 && newMaxMagierAuslastung<=1000){
    		maxMagierAuslastung = newMaxMagierAuslastung/100;
    	}
    	
    	
    	
    	int newMaxRekursion = reportSettings.getOptionInt("Ausbildung_maxRekursion", this.region);
    	if (newMaxRekursion>=0 && newMaxRekursion<=10000000){
    		this.maxRekursion =  (int)newMaxRekursion;
    	}
    	
    	boolean newVerbose = reportSettings.getOptionBoolean("Ausbildung_verbose", this.region);
    	if (newVerbose){
    		this.verbose=true;
    	}
    	
    	
    	ArrayList <AusbildungsRelation> teacherKandidat = new ArrayList <AusbildungsRelation>();
    	ArrayList<AusbildungsRelation> schuelerKandidat = new ArrayList <AusbildungsRelation>();
    	
    	
    	
    	
        // Um welche Schüler Lehrer geht es gerade?
    	
    	
    	// Datei
    	outText.setFile("Ausbildungspool");
    	
        // Hier beginnt der dauerhafte Teil des Pools
    	 
         // VorlaufRequest bearbeiten
    	this.LernSilber=0;
    	this.requestReihenfolgeNummer=0;
    	
    	if (this.vorlaufRequest!=null){
	    	this.LernSilber = this.vorlaufRequest.getBearbeitet();
	    	this.verboseOutText("AusbildungsPool: Mögliches Ausbildungsbudget: "+ this.LernSilber+" Silber");
	    	this.requestReihenfolgeNummer=this.vorlaufRequest.getReihenfolgeNummer();
	    	// Request löschen
	    	this.matPool.removeMatPoolRequest(this.vorlaufRequest);
    	} else {
    		this.verboseOutText("!!!Problem - kein Vorlaufrequest!!!???!!!");
    	}
    	
         this.verboseOutText("AusbildungsPool: Bearbeite Region " + this.region.getName());	
         this.verboseOutText("Settings: maxTalentDiff:" + maxTalentDiff + ",NachZügler:" + this.minNachzuegler + ",minAuslastung:" + this.minAuslastung*100 + "%, maxAuslastung:" + this.maxAuslastung*100 + "%" );
           // Erst mal schauen für welche Talente und Stufen der Pool laufen muß!
         if (this.getSortedSkill()!=null){
            // Nun hübsch der Reihe nach abarbeiten!
        	 for (Iterator<Skill> iter = this.getSortedSkill().iterator();iter.hasNext();){
                 aktuellerSkill = (Skill) iter.next();
        	      
                    // Borderlines setzen in abhängigkeit vom Talent: Magie einseitig locker, andere beidseitig streng
                 this.verboseOutText("Bearbeite Talent: " + aktuellerSkill.getName());
                 if (aktuellerSkill.getName().equals("draig")||aktuellerSkill.getName().equals("illaun")||aktuellerSkill.getName().equals("tybied")||aktuellerSkill.getName().equals("gwyrrd")||aktuellerSkill.getName().equals("cerddor")){
 				   this.maxAktuelleAuslastung=this.maxMagierAuslastung;
 				   this.minAkteulleAuslastung=this.minMagierAuslastung;
 				 }else{
 					 this.maxAktuelleAuslastung=this.maxAuslastung;
 					 this.minAkteulleAuslastung=this.minAuslastung;
 				 }
                 this.verboseOutText("benutze Auslastungen: max: " + this.maxAktuelleAuslastung*100 + "%, min: " + this.minAkteulleAuslastung*100 + "%");
                 
                    for (int schuelerStufe=0;schuelerStufe<=aktuellerSkill.getLevel();schuelerStufe++){
                    	 this.verboseOutText("untersuche schuelerStufe " + schuelerStufe);
                    	 schuelerKandidat = this.getSchueler(this.relationList, aktuellerSkill, schuelerStufe);
                    	 if (schuelerKandidat!=null){
                    		 teacherKandidat = this.getTeacher(this.relationList, schuelerStufe, aktuellerSkill);
                    	 } else {
                    		 teacherKandidat=null;
                    	 }
		                 
		                 // gibt es überhaupt anwärter für eine lernkette?
		                 if ((teacherKandidat!=null)&&(schuelerKandidat!=null)){
			                 this.verboseOutText("AusbildungsPool: ++++ Ermittle Lehrer Kombinationen für " + this.getSchuelerAnzahl(schuelerKandidat) + " Schüler " + aktuellerSkill.getName()+ " T"+schuelerStufe + " in " +schuelerKandidat.size()+ " Einheiten +++");
			                 // Debug
			                 /*
			                 if (aktuellerSkill.getName().equalsIgnoreCase("cerddor")){
			                	 this.verboseOutText("Liste aller Schüler:");
			                	 for (AusbildungsRelation ARx:schuelerKandidat){
			                		 this.verboseOutText("....." + ARx.getScriptUnit().unitDesc());
			                	 }
			                	 this.verboseOutText("Liste aller Lehrer:");
			                	 for (AusbildungsRelation ARx:teacherKandidat){
			                		 this.verboseOutText("....." + ARx.getScriptUnit().unitDesc());
			                	 }
			                 }
			                 */
		                	 schueler_global.clear();
			                 this.schueler_global.addAll(schuelerKandidat);
		                	 this.teacher_gobal.clear();
		                	 // direkter versuch lehrer für alle schüler zu bekommen
		                	 this.shelterbuildTeacherList(teacherKandidat, schuelerKandidat);
		                	 if(this.teacher_gobal.size()==0){
		                		 this.verboseOutText("direct matching was NOT successfull, need to pool, " + teacher_gobal.size()+ " Lehrer, " + schueler_global.size() + " Schüler, " + this.highestTeacherValue + " HTV");
		                		int maxrun = 0; 
			                	// teacher==null, wenn es noch keine gültigen lehrer gab.
		                		for (;((teacher_gobal.size()==0)&&(schueler_global.size()>1)&&(this.highestTeacherValue>0));){
			                		// option: notfallabbruch falls nach 10 runden noch kein lehrer da ist kommt auch keiner mehr 
		                			maxrun++;
			                		 if (maxrun> 10){
			                			 this.verboseOutText("AusbildungsPool: Maxrun für Schülerreduktion überschritten");
			                			 break;
			                		 }
			                		 this.verboseOutText("shorten schuelerlist, run " + maxrun);
			                		 // Schüler verkürzen auf highestTeacherValue vo letzten lauf.
			                		 debugListPool(this.schueler_global, this.teacher_gobal, aktuellerSkill);
			                		 this.shelterbuildSchuelerList(schuelerKandidat);
			                		 this.verboseOutText("new list: ");
			                		 debugListPool(this.schueler_global, this.teacher_gobal, aktuellerSkill);
			                		 // Nutzt schueler_global, weil diese durch die rekursion verkürzt wurde.
			                		 this.shelterbuildTeacherList(teacherKandidat, this.schueler_global);
			                		 this.verboseOutText("after build teacher list: ");
			                		 debugListPool(this.schueler_global, this.teacher_gobal, aktuellerSkill);
		                		} 
		                	 } else {
		                		 this.verboseOutText("direct matching was successfull!");
		                		 debugListPool(this.schueler_global, this.teacher_gobal, aktuellerSkill);
		                	 }
		                	 // verknüpfungen aufbauen
		                	 this.performPooling(this.schueler_global, this.teacher_gobal, aktuellerSkill);		                 
                             
		                 } else {
		                	 String s = "nicht ausreichend Lehrer/Schüler";
		                	 if (teacherKandidat==null){
		                		 s+=", keine Lehrer";
		                	 }
		                	 if (schuelerKandidat==null){
		                		 s+=", keine Schueler";
		                	 }
		                	 this.verboseOutText(s);
		                 }
		             }
                 }
         }
          
         
         // Schueler die keinen Erfolg im Pool hatten setzen Eigenständiges Lernen nach Prio 
         // oder default-Talet, falls vorhannden
         this.autoDidaktenSetzen();
         
         // -> !!
         // Lernketten wenn möglich splitten, damit die in die Akademien passen...
         this.verboseOutText("AusbildungsPool: splitte Lernketten für Region " + this.region.getName());
         this.splitLernKetten();
         
         //Hurra der pool ist durch
         this.verboseOutText("AusbildungsPool: Schreibe Lernketten für Region " + this.region.getName());
         
         // Relations werden in Befehle umgesetzt. Wegen übersicht in SubMethode
         if ( this.relationList!=null){
       	    for (Iterator<AusbildungsRelation> iter = this.relationList.iterator();iter.hasNext();){
       	    	AusbildungsRelation AR = (AusbildungsRelation) iter.next();
       	    	this.relation2Order(AR); 
       	    	// this.akademieWarnungscheck(AR);
        	 }
         }
     
    	 outText.setFileStandard();
    
    } // end run
    
    /**
     * herausnehmbare x:10*x Ketten entfernen
     */
    private void splitLernKetten(){
    	// wir brauchen nur die ARs mit 1 oder 2 Personen als Lehrer.
    	// wir sparen uns das Sortieren und fügen erste die 1 Personen hinzu
    	// dann die 2 Personen
    	if (this.relationList==null) {
    		return;
    	}
    	ArrayList<AusbildungsRelation> relList = new ArrayList<AusbildungsRelation>();
    	for (AusbildungsRelation AR:this.relationList){
    		if (AR.isTeacher() && AR.getSchuelerPlaetze()==1){
    			relList.add(AR);
    		}
    	}
    	for (AusbildungsRelation AR:this.relationList){
    		if (AR.isTeacher() && AR.getSchuelerPlaetze()==2){
    			relList.add(AR);
    		}
    	}
    	
    	// diese Liste jetzt durchgehen
    	for (AusbildungsRelation AR:relList){
    		// wir müssen checken, ob die Lernbeziehung "sauber" ist...1:10
    		// Anzahl der Schüler checken
    		int actAnzSchüler = AR.getAnzahlPooledPersons();
    		// Anzahl Lehrer schwieriger...einen Schüler nehmen
    		AusbildungsRelation einSchüler = AR.getPooledRelation().get(0);
    		int actAnzLehrer = einSchüler.getAnzahlPooledPersons();
    		
    		if (actAnzSchüler == 10 * actAnzLehrer){
	    		// yep, eine "feine" Relation	
	    		// finde einen Schüler, der die geforderte Anzahl an Leuten hat
	    		int neededSchuelerAnzahl = AR.getTeachPlaetze();
	    		AusbildungsRelation passenderSchüler = null;
	    		for (AusbildungsRelation suchSchüler:AR.getPooledRelation()){
	    			if (suchSchüler.getSchuelerPlaetze()==neededSchuelerAnzahl){
	    				// bingo
	    				passenderSchüler = suchSchüler;
	    				break;
	    			}
	    		}
	    		if (passenderSchüler!=null){
	    			// bingo Fall
	    			ArrayList<AusbildungsRelation> LehrerListe = passenderSchüler.getPooledRelation();
	    			// für alle Lehrer diesen Schüler entfernen, so lange es nicht "unser" Lehrer ist
	    			for (AusbildungsRelation einLehrer:LehrerListe){
	    				if (!einLehrer.equals(AR)){
		    				if (einLehrer.getPooledRelation().contains(passenderSchüler)){
		    					einLehrer.getPooledRelation().remove(passenderSchüler);
		    				}
	    				}
	    			}
	    			// für alle Schüler unseren Lehrer entfernen
	    			for (AusbildungsRelation laufSchüler:AR.getPooledRelation()){
	    				if (laufSchüler.getPooledRelation().contains(AR)){
	    					laufSchüler.getPooledRelation().remove(AR);
	    				}
	    			}
	    			
	    			// für unseren Lehrer nur den passenden Setzen
	    			AR.getPooledRelation().clear();
	    			AR.getPooledRelation().add(passenderSchüler);
	    			
	    			//  für unseren Schüler nur unseren Lehrer setzen
	    			passenderSchüler.getPooledRelation().clear();
	    			passenderSchüler.getPooledRelation().add(AR);
	    			
	    			// fertig ?!
	    		}
    		}
    	}
    
    	
    }
    
    
    public void AkaWarnungen(){
    	// Aka-Zuteilungen checken
    	// vorher betrete usw umsetzen, Region refresh ?!
    	// wenn wir keine Aka haben - dann nicht!
    	boolean akaDa = false;
    	if (this.region.buildings()!=null && !this.region.buildings().isEmpty()){
    		for (Building b:this.region.buildings()){
    			if (b.getBuildingType().getName().equalsIgnoreCase("Akademie") && b.getSize()==25){
    				akaDa=true;
    				break;
    			}
    		}
    	}
    	if (!akaDa){
    		return;
    	}
    	EresseaRelationFactory ERF = ((EresseaRelationFactory) reportSettings.getScriptMain().gd_ScriptMain.getGameSpecificStuff().getRelationFactory());
		ERF.processRegionNow(this.region);		
        if ( this.relationList!=null){
      	    for (Iterator<AusbildungsRelation> iter = this.relationList.iterator();iter.hasNext();){
      	    	AusbildungsRelation AR = (AusbildungsRelation) iter.next();
      	    	this.akademieWarnungscheck(AR);
       	   }
        }
    	
    }
	
   /**
    * Baut eine Lehrer-Liste vorgegebener Länge rekursiv auf. Wegen Performance gibt es keine if(!null) abfragen   voricht!!
    *
    */
    private void buildTeacherList(ArrayList<AusbildungsRelation> quellList, ArrayList<AusbildungsRelation> _vorgaengerList, int _position, int _aktuelleLaenge, int _minLaenge, int _maxLaenge, int _idealeLaenge){
	    AusbildungsRelation[] quellArray = quellList.toArray(new AusbildungsRelation[0]);
	    
	    if ((this.teachRekursion++)<= this.maxRekursion){
	    
	    	for (int n = _position; n<quellArray.length;n++){
	    		// liste erreicht die geforderte länge
		    	// 
	    		
	    		double actAuslastung = actTeacherAuslastung(quellArray[n],_vorgaengerList,_idealeLaenge);
	    		
	    		// Debug:
	    		String s = "Rekursion: " + this.teachRekursion + ", ";
	    		s += "Position geg:" + _position + ", act:" + n + ",";
	    		s += "quelle:" + quellArray[n].getTeachPlaetze() + ", minLaenge:" + _minLaenge;
	    		s += ", aktuelle:" + _aktuelleLaenge + ", maxLaenge:" + _maxLaenge + ", idealLaenge:" + _idealeLaenge;
	    		s += ", Auslastung: " + actAuslastung*100 + "%";
	    		this.verboseOutText(s);
		    	// if (((quellArray[n].getTeachPlaetze() +_aktuelleLaenge) >= _minLaenge)&&(quellArray[n].getTeachPlaetze() +_aktuelleLaenge) <= _maxLaenge){
		    	if (actAuslastung>=this.minAkteulleAuslastung && actAuslastung<=this.maxAktuelleAuslastung) {
		    	   		
		    	   
		    		// ist die neue liste näher an der geforderten länge?
		    	   int actTeacherFitting = Math.abs((quellArray[n].getTeachPlaetze() +_aktuelleLaenge - _idealeLaenge));
		    	   s = "actual Fitting: " + actTeacherFitting + ", best until now: " + this.bestTeacherFitting;
		    	   s += ", last Auslastung: " + actTeacherAuslastung(null, _vorgaengerList, _idealeLaenge) *100 + "%, bestTeacherAuslastung: " + this.bestTeacherAuslastung;
		    	   this.verboseOutText(s);
		    	   // if (actTeacherFitting < this.bestTeacherFitting){
		    	   if (Math.abs(actAuslastung - 1) < Math.abs(this.bestTeacherAuslastung - 1)) {	   
		    		   if ((this.highestTeacherValue<quellArray[n].getTeachPlaetze() + _aktuelleLaenge)&&(quellArray[n].getTeachPlaetze() + _aktuelleLaenge<=_idealeLaenge)){
			        	   this.highestTeacherValue=quellArray[n].getTeachPlaetze() + _aktuelleLaenge;
			        	}
		    		    this.teacher_gobal.clear();
			    		this.teacher_gobal.addAll(_vorgaengerList);
			    		this.teacher_gobal.add(quellArray[n]);
			    		// neues bestfitting setzen
			    		this.bestTeacherFitting=Math.abs(quellArray[n].getTeachPlaetze() +_aktuelleLaenge - _idealeLaenge);
			    		this.bestTeacherAuslastung = actAuslastung;
			    		this.verboseOutText("AusbildungsPool: Mögliche LehrerListe erfolgreich aufgebaut für " + (quellArray[n].getTeachPlaetze() + _aktuelleLaenge)+ " Lehrer mit "+ _idealeLaenge+ " Schüler (Rekursion: " + this.teachRekursion + " / "+ this.maxRekursion+")");
			    		this.verboseOutText("Lehrer: " + quellArray[n].getScriptUnit().toString());
			    		
			    		// maxRekursion setzen
			    		if (this.teachRekursion>this.ausbildungsManager.getMaxUsedRecursion()){
			    			this.ausbildungsManager.setMaxUsedRecursion(this.teachRekursion);
			    		}
			    		
			    		
		    	   } else {
			           // Ist sie gleich lang und muß nun auf Qualität geprüft werden?
			    	   // if (Math.abs((quellArray[n].getTeachPlaetze() +_aktuelleLaenge - _idealeLaenge)) == this.bestTeacherFitting){
			    	   if (Math.abs(actAuslastung - 1) == Math.abs(this.bestTeacherAuslastung - 1)) {	 	   
			    		    // neue liste um vorgaengerList für die rekursion nicht zu überschreiben
			    		    ArrayList<AusbildungsRelation> aktuelleList = new ArrayList<AusbildungsRelation>();
			    		    // füllen mit vorgängern
			    		    aktuelleList.addAll(_vorgaengerList);
			    		    // akuelles Relation drauf
			    		    aktuelleList.add(quellArray[n]);
			    		    if (this.isBetterTeacherListOfSameSize(aktuelleList, this.teacher_gobal)){
				    		   	this.teacher_gobal = aktuelleList;
					    		this.verboseOutText("AusbildungsPool: Mögliche LehrerListe erfolgreich verbessert für " + (quellArray[n].getTeachPlaetze() + _aktuelleLaenge)+ " Lehrer mit "+ _idealeLaenge+ " Schüler (Rekursion: " + this.teachRekursion + " / "+ this.maxRekursion+")");
					    		//	maxRekursion setzen
					    		if (this.teachRekursion>this.ausbildungsManager.getMaxUsedRecursion()){
					    			this.ausbildungsManager.setMaxUsedRecursion(this.teachRekursion);
					    		}			    	 
			    		    }
			    	   }
		    	   }
		    	}
		    	// kanditat macht liste nicht zu groß?
		        // if (quellArray[n].getTeachPlaetze()<= (_maxLaenge - _aktuelleLaenge)){
		        if (actAuslastung>this.maxAktuelleAuslastung) {
		        	String ss = "Changing HTV from " + this.highestTeacherValue + " to ";
		        	if ((this.highestTeacherValue<quellArray[n].getTeachPlaetze() + _aktuelleLaenge)&&(quellArray[n].getTeachPlaetze() + _aktuelleLaenge<=_idealeLaenge)){
		        		this.highestTeacherValue=quellArray[n].getTeachPlaetze() + _aktuelleLaenge;
		        	}
		        	ss = ss.concat(this.highestTeacherValue + ", rekursive starting the list build ");
		        	this.verboseOutText(ss);
		        	ArrayList<AusbildungsRelation>  nachfolgerList = new ArrayList<AusbildungsRelation>();
		    	   nachfolgerList.addAll(_vorgaengerList);
		    	   nachfolgerList.add(quellArray[n]);
		    	   // neue rekursion mit aktuelleren daten füttern
		    	   buildTeacherList(quellList, nachfolgerList, n+1,  _aktuelleLaenge+quellArray[n].getTeachPlaetze(), _minLaenge, _maxLaenge, _idealeLaenge);
		         }
		    }
	     }else{
	    	 if (!this.overrun){
	    		 // outText.addOutLine("AusbildungsPool: RekursionsÜberlauf bei Lehrersuche für " + this.aktuellerSkill.getName() + " in "+ this.region.getName(),true);
	    		 this.verboseOutText("AusbildungsPool: RekursionsÜberlauf bei Lehrersuche für " + this.aktuellerSkill.getName() + " in "+ this.region.getName());
	    		 this.overrun=true;
	    	 }
	    	 
	     }
    }
    
    /*
     * Ermittelt die aktuelle Auslastung der Lehrer 
     */
    private double actTeacherAuslastung(AusbildungsRelation actTeacher, ArrayList<AusbildungsRelation> vorgänger, int AnzahlSchüler) {
    	double erg=0;
    	// geamtanzahl der Lehrer Berechnen
    	double teacherSlots;
    	teacherSlots = getTeacherAnzahl(vorgänger);
    	if (actTeacher!=null) {
    		teacherSlots += actTeacher.getTeachPlaetze();
    	}
    	double schülerSlots = AnzahlSchüler;
    	if (teacherSlots>0) {
    		erg =  Math.round((schülerSlots/teacherSlots)*1000.0) / 1000.0;
    	}
    	return erg;
    }
    
    
    
    
    
    // Bereitet buildTeacherList() auf den Rekursionslauf vor, setzt counter zurück, löscht listen...
    
   
    private void shelterbuildTeacherList(ArrayList<AusbildungsRelation> teachList,ArrayList<AusbildungsRelation> schuelerList ){
    	this.overrun=false;
    	this.verboseOutText("AusbildungsPool: Lehrersuche läuft aus " + this.getTeacherAnzahl(teachList)+ " Lehrern für " +this.getSchuelerAnzahl(schuelerList)+ " Schüler" );
    	// Löscht alte Ergebnise aus dem LehrerFeld
   		this.teacher_gobal.clear();
   	
   	     // Rücksetzen der Counter
    	 this.teachRekursion=0;
   	     this.bestTeacherFitting=Integer.MAX_VALUE;
   	     this.highestTeacherValue =0;
   	     this.bestTeacherAuslastung=0;
   	     // Hier gehts los!
   	     this.buildTeacherList(teachList, new ArrayList<AusbildungsRelation>(), 0, 0, (int) Math.ceil(this.getSchuelerAnzahl(schuelerList)* this.minAkteulleAuslastung), (int) Math.floor(this.getSchuelerAnzahl(schuelerList)*1*this.maxAktuelleAuslastung), this.getSchuelerAnzahl(schuelerList) );
   	     this.verboseOutText("AusbildungsPool: Lehrersuche abgeschlossen...");
    }
    
    /*
     * Vergleicht zwei Lehrerlisten gleicher Lehrplatzstärke: Neu besser als alt => true
     */
    private boolean isBetterTeacherListOfSameSize(ArrayList<AusbildungsRelation> _neuList, ArrayList<AusbildungsRelation> _altList ){
    	
    	int neuTalentSumme=0;
    	int neuFremdTalentSumme=0;
    	int altTalentSumme=0;
    	int altFremdTalentSumme=0;
    	
    	// alle neuen einheiten durchgehen
    	for (Iterator<AusbildungsRelation> iter = _neuList.iterator();iter.hasNext();){
    		AusbildungsRelation kandidat = (AusbildungsRelation) iter.next();
    		// gibt es überhaupt teachoffers? sollte aber wer weiß
    		if (kandidat.getTeachOffer()!=null){
	    		HashMap<SkillType, Skill> talentMap = kandidat.getTeachOffer(); 
	    		for (Iterator<Skill> iter2 =talentMap.values().iterator(); iter2.hasNext();){
	    			Skill talent = (Skill) iter2.next();
	    			// ist es das akuelle talent?
	    			if (talent.getSkillType()==this.aktuellerSkill.getSkillType()){
	    				neuTalentSumme = neuTalentSumme + talent.getLevel() * kandidat.getScriptUnit().getUnit().getModifiedPersons();
	    			}else{
	    				neuFremdTalentSumme=neuFremdTalentSumme + talent.getLevel() * kandidat.getScriptUnit().getUnit().getModifiedPersons();	
	    			}
	    		}
    		}
    	}
    	
    	
//    	 alle alten einheiten durchgehen
    	for (Iterator<AusbildungsRelation> iter = _altList.iterator();iter.hasNext();){
    		AusbildungsRelation kandidat = (AusbildungsRelation) iter.next();
    		// gibt es überhaupt teachoffers? sollte aber wer weiß
    		if (kandidat.getTeachOffer()!=null){
	    		HashMap<SkillType, Skill> talentMap = kandidat.getTeachOffer(); 
	    		for (Iterator<Skill> iter2 =talentMap.values().iterator(); iter2.hasNext();){
	    			Skill talent = (Skill) iter2.next();
	    			// ist es das akuelle talent?
	    			if (talent.getSkillType()==this.aktuellerSkill.getSkillType()){
	    				altTalentSumme = altTalentSumme + talent.getLevel() * kandidat.getScriptUnit().getUnit().getModifiedPersons() ;
	    			}else{
	    				altFremdTalentSumme=altFremdTalentSumme + talent.getLevel()* kandidat.getScriptUnit().getUnit().getModifiedPersons() ;
	    			}
	    		}
    		}
    	}
    	
    	
    	
    	
    	//this.verboseOutText("isBetter: neuTalent: " + neuTalentSumme + " altTalent: " + altTalentSumme + " neuFremdTalent: " + neuFremdTalentSumme  + " altFremdTalent: " + altFremdTalentSumme );
    	
    	
    	// nun wir es spannend!
    	
    	//Neues Lehrerkomi kommt mit weniger hochstufigen lehrern aus? Geil!
    	if (neuTalentSumme < altTalentSumme){
    		return true;
    	} 
    	
    	// OK gleiche Lehrerstufen aber vielleicht weniger fremde talente, die dann nicht mehr gelehrt werden können?
    	if ((neuTalentSumme==altTalentSumme)&& (neuFremdTalentSumme < altFremdTalentSumme)){
    		return true;
    	}
    	  	
    	return false;
    	
    }
    
    
    
    
    
    /**
     * Baut eine Schueler-Liste vorgegebener Länge rekursiv auf.
     *
     */
    
    private void buildSchuelerList(ArrayList<AusbildungsRelation> quellList, ArrayList<AusbildungsRelation> _vorgaengerList, int _position, int _aktuelleLaenge, int _minLaenge, int _maxLaenge, int _idealeLaenge){
	    AusbildungsRelation[] quellArray = quellList.toArray(new AusbildungsRelation[0]);
	    	   
	    if (this.schuelerRekursion++<= this.maxRekursion){
	    	outText.addPoint();
	    	//this.verboseOutText("Position: " + _position);
	    	//this.verboseOutText("Length: " + quellArray.length);
	    	for (int n = _position; n<quellArray.length;n++){
	    		//quellArray[n].informsUsShort();
	    		// this.verboseOutText("inBuild: " + _aktuelleLaenge + "  "+ _idealeLaenge);
	    		//this.verboseOutText("n: " + n);
	    		// liste erreicht die geforderte länge
		    	// 
		    	if (((quellArray[n].getSchuelerPlaetze() +_aktuelleLaenge) >= _minLaenge)&&(quellArray[n].getSchuelerPlaetze() +_aktuelleLaenge) <= _maxLaenge){
		    		
		    		// ist die neue liste besser als eine alte?
		    	   if (Math.abs((quellArray[n].getSchuelerPlaetze() +_aktuelleLaenge - _idealeLaenge)) < this.bestSchuelerFitting){
		    		    schueler_global.clear();
			    		this.schueler_global.addAll(_vorgaengerList);
			    		this.schueler_global.add(quellArray[n]);
			    		// neues bestfitting setzen
			    		this.bestSchuelerFitting=Math.abs(quellArray[n].getSchuelerPlaetze() +_aktuelleLaenge - _idealeLaenge);
			    		this.verboseOutText("AusbildungsPool: Mögliche SchülerListe erfolgreich gekürzt " + (quellArray[n].getSchuelerPlaetze() + _aktuelleLaenge)+ " Schüler für "+ _idealeLaenge+ " Lehrer (Rekursion: " + this.schuelerRekursion + " / "+ this.maxRekursion+")"); 
//			    		maxRekursion setzen
			    		if (this.schuelerRekursion>this.ausbildungsManager.getMaxUsedRecursion()){
			    			this.ausbildungsManager.setMaxUsedRecursion(this.schuelerRekursion);
			    		}	
		    	   }
		        }
		    	// kanditat macht liste nicht zu groß?
		        if (quellArray[n].getSchuelerPlaetze()<= (_maxLaenge - _aktuelleLaenge)){
		           ArrayList<AusbildungsRelation> nachfolgerList = new ArrayList<AusbildungsRelation>();
		    	   nachfolgerList.addAll(_vorgaengerList);
		    	   nachfolgerList.add(quellArray[n]);
		    	   // neue rekursion mit aktuelleren daten füttern
		    	   buildSchuelerList(quellList, nachfolgerList, n+1,  _aktuelleLaenge+quellArray[n].getSchuelerPlaetze(), _minLaenge, _maxLaenge, _idealeLaenge);
		         }  
	    	}
	     } else {
	    	 if (!this.overrun){
		    	 // outText.addOutLine("AusbildungsPool:RekursionsÜberlauf bei Schuelerreduktion für " + this.aktuellerSkill.getName() + " in "+ this.region.getName(),true);
		    	 this.overrun=true;
	    	 }
	     }
    }

    private void shelterbuildSchuelerList(ArrayList<AusbildungsRelation> schuelerList){
    	this.overrun=false;
    	this.verboseOutText("AusbildungsPool: Kein Lehrer gefunden, versuche " + this.getSchuelerAnzahl(schuelerList)  +" Schüler in "+ schuelerList.size() +" Einheiten auf " +this.highestTeacherValue +" Schüler zu reduzieren...");
    	// Löscht alte Ergebnise aus dem schuelerFeld
    	this.schueler_global.clear();
   	    
   	     // Rücksetzen der Counter
    	 this.schuelerRekursion=0;
   	     this.bestSchuelerFitting=Integer.MAX_VALUE;
   	     // Hier gehts los!
   	     this.buildSchuelerList(schuelerList, new ArrayList<AusbildungsRelation>(), 0, 0, 1, (int) Math.round(this.highestTeacherValue*1*this.maxAktuelleAuslastung), this.highestTeacherValue );
    }
    
    
    
    /**
     * Baut eine teureSchueler-Liste vorgegebener Länge rekursiv auf.
     *
     */
    
    private void buildTeureSchuelerList(ArrayList<AusbildungsRelation> quellList, ArrayList<AusbildungsRelation> _vorgaengerList, SkillType aktSkillType, int _position, int _aktuelleLaenge, int _maxLaenge){
	    AusbildungsRelation[] quellArray = quellList.toArray(new AusbildungsRelation[0]);
	    	   
	    if (this.teureSchuelerRekursion++<= this.maxRekursion){
	    	//this.verboseOutText("Position: " + _position);
	    	//this.verboseOutText("Length: " + quellArray.length);
	    	for (int n = _position; n<quellArray.length;n++){
	    		//quellArray[n].informsUsShort();
	    		//this.verboseOutText("inBuild: " + _aktuelleLaenge + "  "+ _maxLaenge);
	    		//this.verboseOutText("n: " + n);
	    		
	    		
	    		// liste ´wird nicht zu lang?
		    		    		
		    	if ((quellArray[n].getLernKosten(aktSkillType) +_aktuelleLaenge) <= _maxLaenge){
		    		
		    		// ist die neue liste besser als eine alte?
		    	   if (Math.abs((quellArray[n].getLernKosten(aktSkillType) +_aktuelleLaenge - _maxLaenge)) < this.bestTeureSchuelerFittng){
		    		   this.teureSchueler_global.clear();
			    		this.teureSchueler_global.addAll(_vorgaengerList);
			    		this.teureSchueler_global.add(quellArray[n]);
			    		// neues bestfitting setzen
			    		this.bestTeureSchuelerFittng=Math.abs(quellArray[n].getLernKosten(aktSkillType) +_aktuelleLaenge - _maxLaenge);
			    		this.verboseOutText("AusbildungsPool: Gebe "+ (quellArray[n].getLernKosten(aktSkillType)+ _aktuelleLaenge) + " von " + this.LernSilber + " Silber für "+ aktSkillType.toString()+ " frei. (Rekursion: " + this.teureSchuelerRekursion + " / "+ this.maxRekursion+")");  
			    		// maxRekursion setzen
			    		if (this.teureSchuelerRekursion>this.ausbildungsManager.getMaxUsedRecursion()){
			    			this.ausbildungsManager.setMaxUsedRecursion(this.teureSchuelerRekursion);
			    		}	
		    	   }
		        }
		    	// kanditat macht liste nicht zu groß?
		        if ((quellArray[n].getLernKosten(aktSkillType)<= (_maxLaenge - _aktuelleLaenge))&&(this.bestTeureSchuelerFittng>=200)){
		           ArrayList<AusbildungsRelation> nachfolgerList = new ArrayList<AusbildungsRelation>();
		    	   nachfolgerList.addAll(_vorgaengerList);
		    	   nachfolgerList.add(quellArray[n]);
		    	   // neue rekursion mit aktuelleren daten füttern
		    	   buildTeureSchuelerList(quellList, nachfolgerList,aktSkillType, n+1,  _aktuelleLaenge+quellArray[n].getLernKosten(aktSkillType), _maxLaenge);
		         }
		         
		        
	    	}
	     }else{
	    	 if (!this.overrun){
	    		 // outText.addOutLine("AusbildungsPool: RekursionsÜberlauf bei teurer Schülerreduktion für " + this.aktuellerSkill.getName() + " in "+ this.region.getName(),true);
	    		 this.overrun=true;
	    	 }
	    	 
	     }
    }
    
    
    private void shelterbuildTeureSchuelerList(ArrayList<AusbildungsRelation> teureSchuelerList, SkillType aktSkillType){
    	this.overrun=false;
    	this.verboseOutText("AusbildungsPool: Verkürze Liste der Schüler für " + aktSkillType.toString() + " wegen Silbermangels");
    	// Löscht alte Ergebnise aus dem LehrerFeld
    	
   		this.teureSchueler_global.clear();
   	     
   	     // Rücksetzen der Counter
    	 this.teureSchuelerRekursion=0;
   	     this.bestTeureSchuelerFittng=Integer.MAX_VALUE;
   	     // Hier gehts los!
   	    this.buildTeureSchuelerList(teureSchuelerList, new ArrayList<AusbildungsRelation>(),aktSkillType, 0, 0, this.LernSilber);
   	    
    }
    
    
    
    
    
    /**
	 * Setzt die Relation in LERNEN und LEHREN um. Einheiten ohne Lehrer bleiben unbestätigt 
	 *
	 */

	private void relation2Order(AusbildungsRelation relation) {
		// Diverse Abfragen, die ich in der erste Version unglaublich kompliziert verschachtelt habe
		// Jetzt sehr dümmlich, dafür läuft es ;o)		
		// Weder Lern noch Lehrfach vorhanden => Abbruch
		
						
		
		if (relation.getSubject() == null) {
			// keine Lernkosten???
			
		   if (relation.getLernKosten(relation.getDefaultTalent())==0){
				outText.addOutLine("AusbildungsPool: ("
						+ relation.getScriptUnit().getUnit().getID()
						+ ") keine Aufgabe gepoolt");
				relation.getScriptUnit().doNotConfirmOrders("AusbildungsPool: Keine Aufgabe zugewiesen");
			} else{
				// lernkosten wahrscheinlich nicht gedeckt
				outText.addOutLine("AusbildungsPool: Einheit "+ relation.getScriptUnit().getUnit().getID()+ " benötigt "+ relation.getLernKosten(relation.getDefaultTalent())+ " Silber für " + relation.getDefaultTalent().getName()+" oder ein kostenfreis Lernfach");
				relation.getScriptUnit().doNotConfirmOrders("AusbildungsPool: Nicht genug Silber für " + relation.getDefaultTalent().getName());
				
			}
		}
        
	// Im Wasser und kein MM? 
	if (this.region.getRegionType().isOcean()&&!relation.getScriptUnit().getUnit().getRace().getName().equals("Meermenschen")){	
		 relation.getScriptUnit().addComment("AusbildungsPool: Auf See lernen " + relation.getScriptUnit().getUnit().getRace().getName() + " nicht!");
	}else{
		// OK einheit ist an Land oder MM
		// Wir haben ein LernLehrTalent vom Pool!
		if ((relation.getSubject() != null)&&(!relation.getSubject().isEmpty())) {
			if (relation.isTeacher()) {
				String spacer = " "; // Stellt Leerzeichen bereit.
				String schuelerId = ""; // Sammelt Ids
				int anzSchueler = 0;
				if (relation.getPooledRelation() != null) {
					// Die zugepoolten Relations der Schüler des Lehrers abfragen
					for (Iterator<AusbildungsRelation> iter1 = relation.getPooledRelation().iterator(); iter1.hasNext();) {
						AusbildungsRelation schueler = (AusbildungsRelation) iter1.next();
						// Schrittweise Liste der Schüler Id's in String sammeln und Leerzeichen dran!
						schuelerId += schueler.getScriptUnit().getUnit().toString(false) + spacer;
						anzSchueler += schueler.getSchuelerPlaetze();
					}
					relation.getScriptUnit().addComment("AusbildungsPool: Einheit ist Lehrer! (Lehrt insgesammt " + anzSchueler + " Schüler)");
					Skill lehrfach = (Skill)relation.getSubject().values().toArray()[relation.getSubject().values().toArray().length-1];
					relation.setOrderedSkillType(lehrfach.getSkillType());
					// Tags setzen
					relation.getScriptUnit().putTag(CRParser.TAGGABLE_STRING3, "Lehrer - Pool");				
					relation.getScriptUnit().putTag(CRParser.TAGGABLE_STRING4, lehrfach.getName());
					relation.getScriptUnit().addOrder("LEHREN " + schuelerId,true);
				} else { // PooldedRelations ist null!
					outText.addOutLine("AusbildungsPool: Lehrer (" + relation.getScriptUnit().getUnit().getID()+") ohne Schüler");
					relation.getScriptUnit().doNotConfirmOrders("AusbildungsPool: Lehrer ohne Schüler!");
				}
				relation.setOrdererdSchüleranzahl(anzSchueler);
			}
			// relation.isTeacher=false => Schüler oder Selbstlerner! 
			else {
				// Lernbefehl setzen... letzter Eintrag in subject ist der aktuellste für Schüler
				// ToDo: Subject = null abfangen
				Skill lernfach = (Skill)relation.getSubject().values().toArray()[relation.getSubject().values().toArray().length-1];
				
				// Tags setzen
				relation.getScriptUnit().putTag(CRParser.TAGGABLE_STRING4, lernfach.getName());	
				
				int Kosten = relation.getLernKosten(lernfach.getSkillType());
				String Befehl="LERNEN ";
				// Lernfach ein fiktives magisches Talent?
				if (lernfach.getName().equals("draig")||lernfach.getName().equals("illaun")||lernfach.getName().equals("tybied")||lernfach.getName().equals("gwyrrd")||lernfach.getName().equals("cerddor")){
					Befehl += "Magie";
					// relation.getScriptUnit().addOrder("LERNEN Magie",true);
				}
				else{
					// Ok kein magier.. dann standard
					Befehl += lernfach.getName();
					// relation.getScriptUnit().addOrder("LERNEN " + lernfach.getName(),true);
					
				}
				
				if (Kosten>0){
					Befehl += " " + Kosten;
				}
				relation.getScriptUnit().addOrder(Befehl,true);
				
				relation.setOrderedSkillType(lernfach.getSkillType());
                // Kommentare und Bestätigungen setzen
				if (relation.getPooledRelation() == null) {
					// Schüler Selbstlerner ohne Lehrer
					// Tag setzen
					relation.getScriptUnit().putTag(CRParser.TAGGABLE_STRING3, "Autodidakten - Pool");				
					relation.getScriptUnit().addComment("AusbildungsPool: Einheit findet keinen Lehrer!");
			    	} else {
			    		// Anzahl der Lehrer ermitteln
			    		// Die zugepoolten Relations der Schüler des Lehrers abfragen
			    		int anzLehrer=0;
						for (Iterator<AusbildungsRelation> iter1 = relation.getPooledRelation().iterator(); iter1.hasNext();) {
							AusbildungsRelation lehrer = (AusbildungsRelation) iter1.next();
							// Schrittweise Liste der Lehrer checken
							anzLehrer += lehrer.getSchuelerPlaetze();
						}			    		
			    		
						// Schüler hat Liste mit Lehrer!
				    	// Tag setzen	
				    	relation.getScriptUnit().putTag(CRParser.TAGGABLE_STRING3, "Schüler - Pool");				
						relation.getScriptUnit().addComment("AusbildungsPool: Einheit wird gelehrt! (von " + anzLehrer + " Lehrern)");
				}

			}
		}
	 }
	}
   
	/**
	 * Fiete 20080804
	 * einfacher Test, ob Akademieplatz verschwendet wird, wenn:
	 * Lehrer in Akademie und einer der schüler nicht
	 * kann/sollte  später nach einem Akademiemanager trotzdem laufen
	 * @param relation
	 */
	private void akademieWarnungscheck(AusbildungsRelation relation){
		
		// Wir haben ein LernLehrTalent vom Pool!
		if ((relation.getSubject() != null)&&(!relation.getSubject().isEmpty())) {
			// einen einfachen Schüler nicht testen
			// darf von einem Lehrer ausserhalb der Aka gelehrt werden
			if (relation.isSchueler()) {return;}
			// ist der Lehrer in einer Akademie? Wenn nicht, Prüfung beenden
			ScriptUnit su = relation.getScriptUnit();
			Unit u = su.getUnit();			
			Building b = u.getModifiedBuilding();
			if (b==null){return;}
			if (!b.getBuildingType().getName().equalsIgnoreCase("Akademie")){
				su.addComment("Akacheck: Einheit nicht in Akademie, sondern in anderem Gebäude.", true);
				return;
			}
			// OK, also ein Lehrer in einer Akademie
		
			// checken, ob seine Schüler auch in einer Akademie sind
			if (relation.getPooledRelation() != null) {
				// Die zugepoolten Relations der Schüler des Lehrers abfragen
				String wrongSchueler = "";
				boolean schuelerNotInAka=false;
				for (Iterator<AusbildungsRelation> iter1 = relation.getPooledRelation()
						.iterator(); iter1.hasNext();) {
					AusbildungsRelation schueler = (AusbildungsRelation) iter1.next();
					// Schrittweise Liste der Schüler Id's in String sammeln und Leerzeichen dran!
					boolean SchuelerInAka=false;
					ScriptUnit schuelerSU = schueler.getScriptUnit();
					Unit schuelerU = schuelerSU.getUnit();
					Building schuelerB = schuelerU.getModifiedBuilding();
					if (schuelerB!=null){
						/*
						if (schuelerB.getBuildingType().getName().equalsIgnoreCase("Akademie")){
							SchuelerInAka = true;
						}
						*/
						// es muss die gleiche Aka sein !
						if (schuelerB.equals(b)){
							SchuelerInAka = true;
						}
					}
					if (!SchuelerInAka){
						schuelerNotInAka=true;
						wrongSchueler += schuelerU.getID() + " ";
					}
				}
				if (schuelerNotInAka){
					// noch checkn, ob Lehrer besitzer der aka ist
					// dann durchgehen lassen
					if (b.getOwner()!=null){
						if (b.getOwnerUnit().equals(u)){
							su.addComment("!(Warnung-Akabesitzer) Schüler nicht in Aka: " + wrongSchueler, true);
						} else {
							su.doNotConfirmOrders("!!!Schüler nicht in Aka: " + wrongSchueler);
						}
					}
				}
			}
		}
	}
	
	
	
	
   /**
    * Gibt die nach Talentprio (getReportSkillOrder) sortierte ArrayList an vorkommenden Talente 
    * wieder. Die Rückgabe ArrayList bestimmt die Reihenfolge und die Talenstufe (Skill.getLevel) bis zu der 
    * Lernketten gebaut werden.
    * 
    *   *
    * @return ArrayList <Skill>
    */
	
		
   private ArrayList<Skill> getSortedSkill(){
	   // Gibt es schon die sortedSkill?
	   if (this.sortedSkill==null){	 
		   this.sortedSkill = new ArrayList<Skill>();   
		   ArrayList<Skill> gesamtSkill = new ArrayList<Skill>();
	           // zunächst Liste der Skills besorgen, die in den Relations stecken
			   if (this.relationList!=null){
					   for(Iterator<AusbildungsRelation> iter = this.relationList.iterator(); iter.hasNext();){
						   AusbildungsRelation relation = (AusbildungsRelation) iter.next();
						   // TeachOffer abfragen
						   if (relation.getTeachOffer()!=null){
							   for (Iterator<Skill> iter1 = relation.getTeachOffer().values().iterator();iter1.hasNext();){
							       gesamtSkill.add((Skill) iter1.next());
							   } 
					        }
						   // StudyRequest afragen
						   if (relation.getStudyRequest()!=null){
							   for (Iterator<Skill> iter1 = relation.getStudyRequest().values().iterator();iter1.hasNext();){
							       gesamtSkill.add((Skill) iter1.next());
							   } 
					        } 
					   }   
			    }
		        // Bis jetzt haben wir nur eine Liste an Skills, nun die 
			    // höchsten suchen und doppelte Einträge rauswerfen.
			   
			   // set dient zum Ausschalten doppelter Einträge;
			   Set<String> doubleCheck = new HashSet<String>();
			   
			   // ArrayList nimmt die Maxima aus gesamtSkill auf
			   ArrayList<Skill> noDoublesSkill = new ArrayList<Skill>();
			   for (Iterator<Skill> iter3 = gesamtSkill.iterator(); iter3.hasNext();){
			    	Skill kandidat = (Skill) iter3.next();
			    	for(Iterator<Skill> iter4 = gesamtSkill.iterator();iter4.hasNext();){
			    		Skill vergleich = (Skill) iter4.next();
			            // Gleiches Talent?
			    		if (kandidat!=null && vergleich!=null && kandidat.getName().equals(vergleich.getName())){
			            	if (vergleich.getLevel()>kandidat.getLevel()){
			            		kandidat = vergleich;
			            	}
			            }
			    	}
			        // 
			    	if (kandidat!=null && doubleCheck.add(kandidat.getName())){
			        	noDoublesSkill.add(kandidat);
			        }
			    }
			    
			   // Sortieren nach ReportSettings
			   if (this.getReportSkillOrder()!=null){
			       // Zunächst einsortieren, was wir von reportSettings aus getreportSkillOrder wissen 
				   doubleCheck = new HashSet<String>();
				   for(Iterator<String> iter = this.getReportSkillOrder().iterator(); iter.hasNext();){
					   String orderedtype = (String) iter.next();
					   for (Iterator<Skill> iter1 = noDoublesSkill.iterator(); iter1.hasNext();){
						   Skill skill = (Skill) iter1.next();
						   if (skill!=null && skill.getName().equals(orderedtype)){
							   this.sortedSkill.add(skill);
							   doubleCheck.add(skill.getName());
						   }
					   }
				   }
				   // Jetzt restliche Skills in die finale Liste und dabei darauf achten, dass sich keine Doppelnennung einschleicht!
				   for (Iterator<Skill> iter3 = noDoublesSkill.iterator();iter3.hasNext();){
					   Skill skill = (Skill) iter3.next();
					   if (skill!=null && doubleCheck.add(skill.getName())){
						   this.sortedSkill.add(skill);
					   }
				   }
			   } 
	   }
        // Juhu, finale zuweisung
		return this.sortedSkill;
    }
    
   
   /**
    * Gibt Priosortierte Liste der Talente als Skilltype zurück
    * Nutzt dabei getSortedSkill()
    */
   
    private ArrayList<SkillType> getSortedSkillType(){
    	if (this.sortedSkillType == null){
    		this.sortedSkillType = new ArrayList<SkillType>();
    		for (Iterator<Skill> iter = this.getSortedSkill().iterator();iter.hasNext();){
	    	  Skill skill = (Skill) iter.next();
		    	  // es înteressiert nur der typ, daher auch bei mehreren skills nur einmal der passende typ in die liste
	    	      if (!this.sortedSkillType.contains(skill.getSkillType())){
		    	      this.sortedSkillType.add(skill.getSkillType());
		    	  }    	
	    	  }
    	}
    	return this.sortedSkillType;
    }
       
   
   
   /**
    * wird intern benutzt um die im Report festgelegte Reihenfolge der Talentabarbeitung an 
    * getSortedSkill zu melden. 
    * 
    * TODO Verarbeitet derzeit nur default, weil ich keine Ahnung habe wie es das geht!
    * @return
    */
    
   private ArrayList<String> getReportSkillOrder(){
    	ArrayList<String> defaultSkillOrder = new ArrayList<String>();
    	for (int n=0;n<=this.defaultSkillOrderString.length-1;n++){
    		defaultSkillOrder.add(defaultSkillOrderString[n]);	
    	}
    return defaultSkillOrder;
    	
    }
   
   
    /**
     * Gibt SkillType zu String zurück
     * @param _s
     * @return
     */
    /**
    private SkillType getSkillTypeOfString(String _s){
    	return this.ausbildungsManager.scriptMain.gd_ScriptMain.rules.getSkillType(_s);
    }
    */
   
   /**
    * Ausgaben die nur bei verbose = true erscheinen
    * @param _text
    */
   private void verboseOutText(String _text){
	   if (this.verbose){
	      outText.addOutLine(_text);}
       
   }
   
   /**
    * Stellt Liste der Schüler zu einem Talent und einer bestimmten Stufe zusammen
    * Die ausgewählten Schüler lioegen mit ihren Lernkosten unter dem this.LernSilber
    * @param relation
    * @param skill
    * @param schuelerStufe
    * @return
    */
   private ArrayList<AusbildungsRelation>getSchueler(ArrayList<AusbildungsRelation> _relation, Skill _lernTalent, int _schuelerStufe){
	 	   
	 AusbildungsRelation kandidat = null;
	 ArrayList<AusbildungsRelation> schueler = new ArrayList<AusbildungsRelation>(); 
	 ArrayList<AusbildungsRelation> schuelerKandidatGratis = new ArrayList<AusbildungsRelation>();
	 ArrayList<AusbildungsRelation> schuelerKandidatTeuer = new ArrayList<AusbildungsRelation>();
	 
	
	
	// Wurde eine Liste übergeben?
	 
	 if (_relation!=null){	   
			// Wandern durch die komplete übergebene Liste.
			 for(Iterator<AusbildungsRelation> iter = _relation.iterator(); iter.hasNext();){
				 kandidat= (AusbildungsRelation) iter.next();
				 // In Anland oder MM?
				 if (!this.region.getRegionType().isOcean()||kandidat.getScriptUnit().getUnit().getRace().getName().equals("Meermenschen")){
				 //Kandidat darf nicht bereits Schüler oder Lehrer sein!
				  if ((!kandidat.isSchueler())&&(!kandidat.isTeacher())){	
					 // kandidat hat Lerntalent?
					 if (kandidat.getStudyRequest()!=null && kandidat.getStudyRequest().containsKey(_lernTalent.getSkillType())){
						 // Kandidat ist auf gefordertem level oder ein nachzuegler?
						 if ((kandidat.getStudyRequest().get(_lernTalent.getSkillType()).getLevel()<= _schuelerStufe)&&(kandidat.getStudyRequest().get(_lernTalent.getSkillType()).getLevel()>= _schuelerStufe-this.minNachzuegler)){ 
							 schueler.add(kandidat);										 
						 }
					 }
				 }
			   } 
			  }
	       }
		     
	 
	         //Eine Schuelerliste ist da aber vielleicht ist sie zu teuer für den Pool?
	         if (this.getListenLernKosten(schueler, _lernTalent)>this.LernSilber){
	        	 // Ok die Schueler sind zu teuer.. welche Kosten eigentlich Geld?
	        	 schuelerKandidatTeuer =  new ArrayList<AusbildungsRelation>();
	        	 schuelerKandidatGratis = new ArrayList<AusbildungsRelation>();
	        	 // Schueler in schuelerKandidatTeuer/gratis kopieren, wenn sie teuer/gratis sind...
	        	
	        	 for (Iterator<AusbildungsRelation> iter = schueler.iterator();iter.hasNext();){
	        		 kandidat = (AusbildungsRelation) iter.next();
	        		 // kandidat.getScriptUnit().addComment("AP: untersuche Kanidat auf Talente...");
	        		 if (kandidat.getLernKosten(kandidat.getStudyRequest().get(_lernTalent.getSkillType()))>0){
	        			 schuelerKandidatTeuer.add(kandidat);
	        			 // kandidat.getScriptUnit().addComment("AP: ist in teuere Talente einsortiert");
	        		 } else { 
	        			 schuelerKandidatGratis.add(kandidat);
	        			 // kandidat.getScriptUnit().addComment("AP: ist in Gratis Talente einsortiert");
	        		 }
	        	  }
	        	 
	        	 // Sortieren nach Lernkosten...
	        	 Collections.sort(schuelerKandidatTeuer, new SchuelerLernKostenComparator(_lernTalent));
	        	  // Best-fitting für teure schueler...
	        	 this.shelterbuildTeureSchuelerList(schuelerKandidatTeuer, _lernTalent.getSkillType());
	        	  // 
	        	 schueler= new ArrayList<AusbildungsRelation>();
	        	 
	        	 schueler.addAll(this.teureSchueler_global);
	        	 
	        	 if (schuelerKandidatGratis!=null){
	        		 schueler.addAll(schuelerKandidatGratis);
	        	 }
	        	  
	        	  
	        	 // debugging
	        	  
		        // this.verboseOutText("getschueler: lernsilber:" + this.LernSilber); 
	        	// this.verboseOutText("getschueler: lernkosten " +this.getListenLernKosten(this.schuelerKandidat, _lernTalent));
		        	 
	         
	         
	         }
		     // Finale Zuweisung
		     // falls später sinnvoll lassen sich Schüler mit und ohne Subject auch in 2
		     // ArrayList speichern, sortieren und dann die eine zur anderen adden.
		     // 
		     if (schueler.isEmpty()){
            	 return null; 
             } else {
            	 Collections.sort(schueler, new SchuelerComparator(_lernTalent));
            	 return schueler;
             }
    }
     
   
   /**
    * Ermittelt die Lernkosten für eine ArrayList mit Relations zusammen
    * @param _relation
    * @param _skill
    * @return
    */
    private int getListenLernKosten(ArrayList<AusbildungsRelation> _relation, Skill _skill){
    	int LernSumme = 0;
    	for (Iterator<AusbildungsRelation> iter = _relation.iterator(); iter.hasNext();){
    	   AusbildungsRelation kandidat = (AusbildungsRelation) iter.next();
    	   LernSumme = LernSumme + kandidat.getLernKosten(_skill.getSkillType());    		
    	}
        return LernSumme;
    }
   
    private int getSchuelerAnzahl(ArrayList<AusbildungsRelation> _relation){
    	int anzahl= 0;
    	if (_relation !=null){
    		for (Iterator<AusbildungsRelation> iter = _relation.iterator(); iter.hasNext();){
    			AusbildungsRelation schueler = (AusbildungsRelation) iter.next();
    			anzahl = anzahl + schueler.getSchuelerPlaetze();
    		}
    		return anzahl;
    	}else{
    		return 0;
    	} 
    }
   
    
    private int getTeacherAnzahl(ArrayList<AusbildungsRelation> _relation){
    	int anzahl= 0;
    	if (_relation !=null){
    		for (Iterator<AusbildungsRelation> iter = _relation.iterator(); iter.hasNext();){
    			AusbildungsRelation teacher = (AusbildungsRelation) iter.next();
    			anzahl = anzahl + teacher.getTeachPlaetze();
    		}
    		return anzahl;
    	}else{
    		return 0;
    	} 
    }
    
    
    
   /**
    * Sucht Lehrer für ein Talent auf Stufe aus ener Liste und gibt Liste der Lehrer zurück.
    * @param _relation
    * @param _schuelerStufe
    * @param _teachTalent
    * @return
    */
   private ArrayList<AusbildungsRelation> getTeacher(ArrayList<AusbildungsRelation> _relation , int _schuelerStufe, Skill _teachTalent){
	  AusbildungsRelation kandidat = null;
	  ArrayList<AusbildungsRelation> teacher = new ArrayList<AusbildungsRelation>();
	   // Wurde eine Liste übergeben?
		    if (_relation!=null){	   
				// Wandern durch die komplete übergebene Liste.
				 for(Iterator<AusbildungsRelation> iter = _relation.iterator(); iter.hasNext();){
					 kandidat= (AusbildungsRelation) iter.next();
					 
					// An Land oder MM?
					 if (!this.region.getRegionType().isOcean()||kandidat.getScriptUnit().getUnit().getRace().getName().equals("Meermenschen")){
					 //Kandidat darf nicht bereits Schüler oder Lehrer sein!
					 if ((!kandidat.isSchueler())&&(!kandidat.isTeacher())){	
							 // kandidat hat Lehrtalent? Dann sollte es ein teachoffer geben und man sollte nicht noch teuren LernTrank im Blut haben.
							 // if ((kandidat.getTeachOffer()!=null) && (kandidat.getTeachOffer().containsKey(_teachTalent.getSkillType()))&& (kandidat.getGehirnschmalzEffekte()<= 0)){
							 // geändert 20120713: gehirnschmalz ist kein Problem...
								 if ((kandidat.getTeachOffer()!=null) && (kandidat.getTeachOffer().containsKey(_teachTalent.getSkillType()))){	
								 // Kandidat hat geforderten Level? also mindestens 2 besser aber nicht zu gut?
								  if ((_schuelerStufe +2 <= kandidat.getTeachOffer().get(_teachTalent.getSkillType()).getLevel())&&(_schuelerStufe + this.maxTalentDiff >= kandidat.getTeachOffer().get(_teachTalent.getSkillType()).getLevel())){ 
                                    teacher.add(kandidat);										 
								  }
							 }
					 }
				  }
				}
		       }
   
             if (teacher.isEmpty()){
            	 return null; 
             }else{
            	 Collections.sort(teacher, new TeacherComparator(_teachTalent));
            	 return teacher;
             }
		    
   
   }// methode
   
   
   private void debugListPool(ArrayList<AusbildungsRelation> _schueler, ArrayList<AusbildungsRelation> _teacher, Skill teachTalent){
	  	  String s="";
	  	  /*
		  if ((_schueler.size()>0)&&(_teacher.size()>0)){
			   for (Iterator<AusbildungsRelation> iter = _teacher.iterator(); iter.hasNext();){
				  AusbildungsRelation teacher = (AusbildungsRelation) iter.next();
				  s="Lehrer: " + teacher.getScriptUnit().getUnitNumber() + " -> ";
				  for (Iterator<AusbildungsRelation> iter2 = _schueler.iterator(); iter2.hasNext();){
					  // Jeder Lehrer lehrt jeden Schüler
					  AusbildungsRelation schueler = (AusbildungsRelation) iter2.next();
					  s = s.concat(schueler.getScriptUnit().getUnitNumber() + " ");
				  }
				  this.verboseOutText(s);
			  }
		  }
		  */
		  if (_teacher.size()==0) {
			  this.verboseOutText("Lehrer: keiner");
		  } else {
			 s = "Lehrer: ";
			 for (Iterator<AusbildungsRelation> iter = _teacher.iterator(); iter.hasNext();){
				  AusbildungsRelation teacher = (AusbildungsRelation) iter.next();
				  s = s.concat(teacher.getScriptUnit().getUnitNumber() + " ");
			 }
			 this.verboseOutText(s);
		  }
		  if (_schueler.size()==0) {
			  this.verboseOutText("Schüler: keiner");
		  } else {
			  s = "Schüler: ";
			 for (Iterator<AusbildungsRelation> iter = _schueler.iterator(); iter.hasNext();){
				  AusbildungsRelation schueler = (AusbildungsRelation) iter.next();
				  s = s.concat(schueler.getScriptUnit().getUnitNumber() + " ");
			 }
			 this.verboseOutText(s);
		  }
	   }
   
   private void performPooling(ArrayList<AusbildungsRelation> _schueler, ArrayList<AusbildungsRelation> _teacher, Skill teachTalent){
	   double actAuslastung = actTeacherAuslastung(null, _teacher, getSchuelerAnzahl(_schueler));
	  if ((_schueler!=null)&&(_teacher!=null)){
		   for (Iterator<AusbildungsRelation> iter = _teacher.iterator(); iter.hasNext();){
			  AusbildungsRelation teacher = (AusbildungsRelation) iter.next();
			  for (Iterator<AusbildungsRelation> iter2 = _schueler.iterator(); iter2.hasNext();){
				  // Jeder Lehrer lehrt jeden Schüler
				  AusbildungsRelation schueler = (AusbildungsRelation) iter2.next();
				  schueler.setIsSchueler();
				  schueler.addSubject(teachTalent.getSkillType(), teachTalent);
				  schueler.addPooledRelation(teacher);
				  schueler.getScriptUnit().addComment("Ausbildungspool: Lehrerauslastung für " + teachTalent.getSkillType().toString() + ": " + actAuslastung*100 + "%");
				  teacher.addSubject(teachTalent.getSkillType(), teachTalent);
				  teacher.setIsTeacher();
				  teacher.addPooledRelation(schueler);
			  }
			  teacher.getScriptUnit().addComment("Ausbildungspool: Auslastung als Lehrer für " + teachTalent.getSkillType().toString() + ": " + actAuslastung*100 + "%");
		  }
		   // Lernkosten beantragen
		   
		   
		   for (Iterator<AusbildungsRelation> iter = _schueler.iterator(); iter.hasNext();){
                AusbildungsRelation kandidat = (AusbildungsRelation)iter.next();
			   // Silber reduzieren, 
	      		this.LernSilber= this.LernSilber-kandidat.getLernKosten(kandidat.getStudyRequest().get(teachTalent.getSkillType()));
	       	    // Silber anfordern
	      		
	      		// debugging
	      		// this.verboseOutText("....");
	      		// this.verboseOutText("performpooling: Listenkosten: " + this.getListenLernKosten(this.schueler, teachTalent));
	      		// this.verboseOutText("performPooling: Lernsilber: " + this.LernSilber);
	      		
	      		
	      		MatPoolRequest request  = new MatPoolRequest(new Script(kandidat.getScriptUnit()),kandidat.getLernKosten(kandidat.getStudyRequest().get(teachTalent.getSkillType())), "Silber",this.LernsilberPrio ,"Lernkosten");
	    	    request.setScriptUnit(kandidat.getScriptUnit());
	      		request.setReihenfolgeNummer(this.requestReihenfolgeNummer);
	    	    this.matPool.addMatPoolRequest(request);
	    	    if (kandidat.getLernKosten(kandidat.getStudyRequest().get(teachTalent.getSkillType()))>0){
	    	        kandidat.getScriptUnit().addComment("Lernkosten: " + kandidat.getLernKosten(kandidat.getStudyRequest().get(teachTalent.getSkillType()))+ " Silber");
	    	    }
		   }
	  		
	  } 
	   
	   
   }
  
 /**
  * Wer im pool keinen Lehrer hat bekommt üer den aufruf aus runPool heraus ein default-Talent.
  *
  * Autodidakten suchen... nach Talentreihenfolge sortieren, intern nach Stufe.. dann Subject zuweisen
  * Im Falle teuerer Talente kosten kalkulieren...
  *
  */
   
   private void autoDidaktenSetzen(){
	   // Hier sammeln wir die Leute die weder Schüler noch Lehrer wurden im Pool
	   ArrayList<AusbildungsRelation> autoDidakten = new ArrayList<AusbildungsRelation>();
	   if (this.relationList==null){
		   return;
	   }
	   
	   for (Iterator<AusbildungsRelation> iter = this.relationList.iterator(); iter.hasNext();){
	      AusbildungsRelation kandidat = (AusbildungsRelation) iter.next();
	        if ((!kandidat.isSchueler())&&(!kandidat.isTeacher())){
            //	 Gibt kein von scripten gesetztes default talent.
	      		  if (kandidat.getDefaultTalent()==null){
		        		 // Skills der Region nach Prio abgehen 
		        		 for (Iterator<Skill> iter2 = this.sortedSkill.iterator();iter2.hasNext();){
		        			 Skill aktSkill = (Skill) iter2.next();
		        			 // Hat der Kandidat das Aktuelle Talent im request? dann in DefaultSubject
		        			 if (kandidat.getStudyRequest()!=null && kandidat.getStudyRequest().containsKey(aktSkill.getSkillType())){
		        				 if (kandidat.getDefaultTalent()==null){
		        				    kandidat.setDefaultTalent(kandidat.getStudyRequest().get(aktSkill.getSkillType()));
		        			     }
		        			 }
		        		 }
		        		 
	      		    }
	        	// Nun hat die Relation ein Defaultsubject und darf in autodidakten...
	      		autoDidakten.add(kandidat);  
	        }
	   }
	   
	   // autodidakten muß sortiert werden, in der Talentprio und nach absteigenden Stufen...
	   Collections.sort(autoDidakten, new AutoDidaktenComparator(this.getSortedSkillType()));
	   
	   // Jetzt Autodidakten von vorne durchgehen und wenn Silber da ist oder keines gebraucht wird Subkjects setzen
	   // und falls nötig die Matpoolrelation zum Silber
	    
	   
	    
	   for (Iterator<AusbildungsRelation> iter = autoDidakten.iterator();iter.hasNext();){
	    	AusbildungsRelation kandidat = (AusbildungsRelation) iter.next();
	    	// kandidat.getScriptUnit().addComment("AP: prüfe Autodidakten");
	    	if (kandidat.getDefaultTalent()!=null) {
	    		// kandidat.getScriptUnit().addComment("AP: Autodidakt, defaultTalent = " + kandidat.getDefaultTalent().toString());
	    	} else {
	    		// kandidat.getScriptUnit().addComment("AP: kandidat hat kein defaultTalent");
	    	}
	      	// Genug Silber da 
	    	if (kandidat.getDefaultTalent()!=null && kandidat.getLernKosten(kandidat.getDefaultTalent())<=this.LernSilber){
	    		// kandidat.getScriptUnit().addComment("AP: Autodidakt: genug Silber vorhanden");
	      		// Subject zuweisen...
	      		kandidat.addSubject(kandidat.getDefaultTalent().getSkillType(),kandidat.getDefaultTalent());
	      	    
	      		// brauchen wir überhaupt silberanforderung?
	      		if (kandidat.getLernKosten(kandidat.getDefaultTalent())>0){
	      		
		      		// Silber reduzieren
		      		this.LernSilber= this.LernSilber-kandidat.getLernKosten(kandidat.getDefaultTalent());
		       	    // Silber anfordern
		      		MatPoolRequest request  = new MatPoolRequest(new Script(kandidat.getScriptUnit()),kandidat.getLernKosten(kandidat.getDefaultTalent()), "Silber",this.LernsilberPrio ,"Lernkosten");
		    	    request.setScriptUnit(kandidat.getScriptUnit());
		      		// request auf altes Konto buchen
		    	    request.setReihenfolgeNummer(this.requestReihenfolgeNummer);
		    	    this.matPool.addMatPoolRequest(request); 
		    	    // Ausgabe nur wenn Kosten > 0
		    	    kandidat.getScriptUnit().addComment("Lernkosten: " + kandidat.getLernKosten(kandidat.getDefaultTalent())+ " Silber");
	      		}
	        }
	    	// Nicht genug Silber da aber Notfalltalent...
	    	// kandidat.getScriptUnit().addComment("AP: Autodidakt Lernsilberstatus=" + this.LernSilber + ", Kosten default: " + kandidat.getLernKosten(kandidat.getDefaultTalent()) + "Silber");
	    	if (kandidat.getDefaultGratisTalent()==null) {
	    		// kandidat.getScriptUnit().addComment("AP: Autodidakt getDefaultGratis ist NULL!");
	    	}
	    	if ((kandidat.getLernKosten(kandidat.getDefaultTalent())>this.LernSilber)&&(kandidat.getDefaultGratisTalent()!=null)){
	    		// kandidat.getScriptUnit().addComment("AP: Autodidakt kann teures Talent nicht lernen");
	      		// Subject zuweisen...
	      		if (kandidat.getLernKosten(kandidat.getDefaultGratisTalent())==0){
	      			// kandidat.getScriptUnit().addComment("AP: Autodidaktenhat GratisTalent und setzt dieses...");
	      			kandidat.addSubject(kandidat.getDefaultGratisTalent().getSkillType(),kandidat.getDefaultGratisTalent());
	      		}
	      		
	       	}
	    	
	    	
	    	
	    }
	    
	   // for (Iterator test = autoDidakten.iterator();test.hasNext();){
		//   AusbildungsRelation tester1 = (AusbildungsRelation)test.next();
		//   tester1.informsUs();
	    //    }
	   
   }


/**
 * @return the relationList
 */
public ArrayList<AusbildungsRelation> getRelationList() {
	return relationList;
}
  
   
   
}// ende class
