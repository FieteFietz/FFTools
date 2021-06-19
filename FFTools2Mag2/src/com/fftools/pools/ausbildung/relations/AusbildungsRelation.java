package com.fftools.pools.ausbildung.relations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import magellan.library.Building;
import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.utils.FFToolsUnits;

/**
 * Wird vom AusbildungsPool verwendet. 
 * Parameter in Denglisch: teach/study, weil sich lernen/lehren so ähnlich sehen
 * 
 * @author Marc
 *
 */
public class AusbildungsRelation {
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private ScriptUnit scriptUnit=null;
	
	//Lernfach oder Lehrfach
	private HashMap<SkillType,Skill> subject =null;
	
	// der finale beim Order setzen benutzte skillType
	private SkillType orderedSkillType = null;
	
	// die finale beim Order setzen bnekante Schüleranzahl 
	private int ordererdSchüleranzahl = -1;
	
	// Ist die Einheit ein Lehrer?.
	private boolean isTeacher=false;
	private boolean isSchueler=false;

	private int gehirnSchmalzEffekte=-1;
		
	// Nimmt die vom Pool vergeben Schüler/Lehrer auf. Wichtig für UNDO einzelner Leute
	private ArrayList<AusbildungsRelation> pooledRelation = null;	
		
	// Nimmt den nächsten Lerncluster gleichwertiger Leranfragen auf	
	private HashMap<SkillType,Skill> studyRequest=null;
	
	// Nimmt das Default-Talent auf, das im Notfall gelernt wird.
	// Darf teures Talent sein!
	
	private Skill defaultTalent=null;
	
	// Nimmt kostenfreues Talent auf damit die Einheit bei Lehrer und Geldmangel lernen kann
	// Darf nie etwas kosten!!!
	private Skill defaultGratisTalent=null;
	
	
	// Nimmt die aktuell erlaubten Lehrmöglichkeiten auf
	private HashMap<SkillType,Skill> teachOffer=null;
	
	// Lernfix aka=nein => nicht in aka, wenn drinne, raus
	private boolean avoidAka = false;
    
	
	
    /**
     * Wenn durch Lernplan eines bestimmten Levels gesetzt, wird hier
     * die Info abgelegt (zur Userinfo)
     */
	private int actLernplanLevel = Lernplan.level_unset;
	
	/**
	 * zur Berechung der Lernkosten
	 */
	private Building akademie= null;
	
	/**
	 * für den Akademiemanager
	 */
	private Building akademieFromAM = null;
	
	/**
	 * damit schüler nicht von jedem Lehrer in die Aka geschickt werden
	 */
	private boolean orderedNewAka = false;
	
	
	/**
	 * Konstruktor
	 * @param _su
	 * @param _talent
	 * @param _stufe
	 */
	
	public AusbildungsRelation(ScriptUnit _su, HashMap<SkillType,Skill> _sL, HashMap<SkillType,Skill> _tL ){
    	scriptUnit=_su;
    	studyRequest =_sL;
    	teachOffer =_tL;
    	
    	// akademie feststellen falls einheit darin ist oder betritt.
    	Building gebaeude = this.scriptUnit.getUnit().getModifiedBuilding();
    	if (gebaeude != null){
    		if ((this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getBuildingType("Akademie").equals(gebaeude.getBuildingType()))&&(gebaeude.getSize()>=25)){
    			this.akademie = gebaeude;
    		}
      	}
    	
	}
	
	/**
	 * ergänzt die studyRequests
	 * @param skillType
	 * @param skill
	 */
	public void addStudyRequest(SkillType skillType,Skill skill){
		if (this.studyRequest==null){
			this.studyRequest = new HashMap<SkillType, Skill>();
		}
		if (!this.studyRequest.containsKey(skillType)){
			this.studyRequest.put(skillType, skill);
		}
	}
	
	
	/**
	 * ergänzt die teachOffers
	 * @param skillType
	 * @param skill
	 */
	public void addTeachOffer(SkillType skillType,Skill skill){
		if (this.teachOffer==null){
			this.teachOffer = new HashMap<SkillType, Skill>();
		}
		// 20121222: soll vorhandene Ersetzen!
		if (this.teachOffer.containsKey(skillType))
		{
			this.teachOffer.remove(skillType);
		}
		this.teachOffer.put(skillType, skill);
	}
	
	
	public ScriptUnit getScriptUnit(){
		return this.scriptUnit;
	}
	
	public HashMap<SkillType,Skill> getStudyRequest(){
		return this.studyRequest;
	}
	
	public HashMap<SkillType,Skill> getTeachOffer(){
		return this.teachOffer;
	}
	
	
	
	public void setStudyRequest(HashMap<SkillType,Skill> _sL){
		this.studyRequest=_sL;
	}
	
	public void setDefaultTalent(Skill _skill){
	this.defaultTalent=_skill;
	}
	
	
	public Skill getDefaultTalent(){
		return this.defaultTalent;
	}
	
	/** 
	 * Talent das bei Silber UND Lehrermangel gelernt wird
	 * @param _skill
	 */
	
	public void setDefaultGratisTalent(Skill _skill){
			this.defaultGratisTalent=_skill;
			this.scriptUnit.addComment("(AR:) setze defaultGratisTalent auf: " + _skill.toString());
		}
		
	/** 
	 * Talent das bei Silber UND Lehrermangel gelernt wird
	 * @param _skill
	 */
	public Skill getDefaultGratisTalent(){
			return this.defaultGratisTalent;
		}
		
	/**
	 * 
	 * @return modified * 10
	 */
	public int getTeachPlaetze(){
		return this.scriptUnit.getUnit().getModifiedPersons()*10;
	}
	
	/**
	 * 
	 * @return modified Pers
	 */
	public int getSchuelerPlaetze(){
		return this.scriptUnit.getUnit().getModifiedPersons();
	}
	
	
	
	
	/**
	 * Was lernt/lehrt die einheit?
	 * @return
	 */
	
	public HashMap<SkillType,Skill> getSubject(){
		return this.subject;
	}
	/**
	 * Setzt das lern/lehrfach
	 * @param _t
	 */
	
	public void setSubject(HashMap<SkillType,Skill> _tL){
		this.subject =_tL;
	}
	
	/**
	 * Added ein Skill zur Liste der Lern/Lehrfächer
	 * @param _t
	 */
	
	public void addSubject(SkillType _st, Skill _s){
		
		if (this.subject==null){
			this.subject = new HashMap<SkillType,Skill>();
		}
		
		this.subject.put(_st,_s);
	}
	
	
	public ArrayList<AusbildungsRelation> getPooledRelation(){
		return this.pooledRelation;
	}
	
	
	public void addPooledRelation(AusbildungsRelation _ar){
		if (this.pooledRelation==null){
			this.pooledRelation=new ArrayList <AusbildungsRelation>();
		}
		this.pooledRelation.add(_ar);
	}
	
	public boolean isTeacher(){
		return this.isTeacher;
	}
	
	public boolean isSchueler(){
		return this.isSchueler;
	}
	
	
	/**
	 * Setzt Flag und verzehnfacht die RestBedarf Variable.
	 * 
	 * @param _t
	 */
	public void setIsTeacher(){
		   this.isTeacher=true;
		  }
	 
	
	
	public void setIsSchueler(){
		this.isSchueler=true;
	}
	
	
	

	
	/**
	 * Setzt die Relation wieder auf den Status vor dem Poolen.
	 *
	 */
	
	public void resetRelation(){
		this.subject=null;
		this.isTeacher=false;
		this.gehirnSchmalzEffekte=-1;
		this.pooledRelation=null;
    	
	}
	
	/**
	 * Gibt das Verhältns von aktueller Beschäftigung zu Vollbeschäftigung wieder
	 * @return
	 */
	
	
	
	
	/**
	 * Sucht nach noch wirksamen Gehirnschmalzen 
	 * @return
	 */
	public int getGehirnschmalzEffekte(){
			if (this.gehirnSchmalzEffekte==-1){
				this.gehirnSchmalzEffekte =0;
				if (this.getScriptUnit().getUnit().getEffects()!=null){
					// durchlaufen der effekte
					for (Iterator<String> iter = this.getScriptUnit().getUnit().getEffects().iterator(); iter.hasNext();){
						
						String effect = (String) iter.next();
						
						// der Effect im CR lautet: 1 Gehirnschmalz
						
						if (effect.contains("Gehirnschmalz")){
							StringTokenizer token = new StringTokenizer(effect);
							this.gehirnSchmalzEffekte = Integer.parseInt(token.nextToken());
						}
					
					}
				} 
				
			}	
				
			return this.gehirnSchmalzEffekte;
	
	}


	public void informsUs(){
		outText.addOutLine("+++++++++++++++++++++++InformsUs AusbildungsRelation+++++++++++++++++");
		outText.addOutLine("Unit: (" + this.scriptUnit.getUnit().getID()+") " + this.scriptUnit.getUnit().getName());
		if ((!this.isSchueler)&&(!this.isTeacher)){outText.addOutLine("Status: FREI");}
		if (this.isTeacher){outText.addOutLine("Status: Lehrer");}
		if (this.isSchueler){outText.addOutLine("Status: Schüler");}
		outText.addOutLine("RestTeachbedarf: " + this.getTeachPlaetze()  );
		outText.addOutLine("RestStudyBedarf: " + this.getSchuelerPlaetze());
		if (this.subject!=null){
			for (Iterator<Skill> iter = this.subject.values().iterator();iter.hasNext();){
				Skill skill = (Skill) iter.next();
				outText.addOutLine("Subject: " + skill.getName());	
			}
				
		}
		
		if (this.defaultTalent==null){outText.addOutLine("DefaultTalent = null");}
		if (this.defaultTalent!=null){outText.addOutLine("DefaultTalent = "+ this.defaultTalent.getName()+ " T"+this.defaultTalent.getLevel());}
		
		if (this.defaultGratisTalent==null){outText.addOutLine("DefaultGratisTalent = null");}
		if (this.defaultGratisTalent!=null){outText.addOutLine("DefaultGratisTalent = "+ this.defaultTalent.getName());}
		
		
		
		if (this.subject==null){outText.addOutLine("Subject = null");}
		
		if (this.studyRequest==null){outText.addOutLine("studyRequest: null");}
		if (this.studyRequest!=null){
			outText.addOutLine("---------------------studyRequest-----------------------: ");
			for (Iterator<Skill> iter = this.studyRequest.values().iterator(); iter.hasNext();){
				Skill skill = (Skill) iter.next();
				outText.addOutLine("studyRequest: " + skill.getName()+ " T" +skill.getLevel());
				
			}
		}
		
		if (this.teachOffer==null){outText.addOutLine("studyRequest: null");}
		if (this.teachOffer!=null){
			outText.addOutLine("----------------------teachOffer----------------------- ");
			for (Iterator<Skill> iter = this.teachOffer.values().iterator(); iter.hasNext();){
				Skill skill = (Skill) iter.next();
				outText.addOutLine("teachOffer: " + skill.getName()+ " T" +skill.getLevel());
				
			}
		}
		
		
		
		
		if (this.pooledRelation==null){outText.addOutLine("pooledRelation: null");}
		if (this.pooledRelation!=null){
			outText.addOutLine("pooledRelation: ");
			for (Iterator<AusbildungsRelation> iter = this.pooledRelation.iterator(); iter.hasNext();){
				AusbildungsRelation link = (AusbildungsRelation) iter.next();
				link.informsUsShort();
			}
		}
		
	}

	public void informsUsShort(){
		if (this.isTeacher){outText.addOutLine("Status: Lehrer " + this.scriptUnit.getUnit().getID() + " Personen: " + this.scriptUnit.getUnit().getModifiedPersons());}
		if (!this.isTeacher){outText.addOutLine("Status: Schüler " + this.scriptUnit.getUnit().getID()+ " Personen: " + this.scriptUnit.getUnit().getModifiedPersons());}
		
	}

	// informiert die scriptunit mittels kommentaren über lernaufträge
	public void informScriptUnit(){
		// activer Lernplanlevel
		if (this.getActLernplanLevel()==Lernplan.level_unset){
			// nothing
			this.scriptUnit.addComment("aktiver Lernplan: keine info");
		} else if (this.getActLernplanLevel()==Lernplan.level_afterSets){
			this.scriptUnit.addComment("aktiver Lernplan: ohne Level");
		} else {
			this.scriptUnit.addComment("aktiver Lernplan: " + this.getActLernplanLevel());
		}
		// Lehrmöglichkeiten
		if (!(this.teachOffer==null || 
				this.teachOffer.size()==0)){
			String s = "Lehrer für:";
			for (Iterator<Skill> iter = this.teachOffer.values().iterator();iter.hasNext();){
				Skill actSkill = (Skill)iter.next();
				if (actSkill!=null){
					s += " " + actSkill.getName();
				} else {
					s += " " + "NULL";
				}
			}
			s += " (" + this.getTeachPlaetze() + " Schüler.)";
			this.scriptUnit.addComment(s);
		} else {
			this.scriptUnit.addComment("kein Lehrereinsatz");
		}
		// Lernanforderungen
		if (!(this.studyRequest ==null || this.studyRequest.size()==0)){
			String s = "Schüler für:";
			for (Iterator<Skill> iter = this.studyRequest.values().iterator();iter.hasNext();){
				Skill actSkill = (Skill)iter.next();
				if (actSkill!=null){
					s += " " + actSkill.getName();
				} else {
					s += " " + "NULL";
				}
			}
			s += " (" + this.getSchuelerPlaetze() + " Pers.)";
			this.scriptUnit.addComment(s);
		} else {
			this.scriptUnit.addComment("kein Lernbedarf");
		}
		// default
		String defaultInfo = "";
		if (this.getDefaultTalent()!=null){
			// Marc: liest jetzt aus this.defaultTalent nicht subject
			String s = "Default:";
			s += " " + this.getDefaultTalent().getName();
			defaultInfo+=s;
		} else {
			defaultInfo += "kein Standardlerntalent";
		}
		if (this.getDefaultGratisTalent()!=null){
			// Marc: liest jetzt aus this.defaultTalent nicht subject
			String s = " Gratis:";
			s += " " + this.getDefaultGratisTalent().getName();
			defaultInfo+=s;
		} else {
			defaultInfo += " kein Gratislerntalent";
		}
		
		this.scriptUnit.addComment(defaultInfo);
		
	}

	/**
	 * @return the actLernplanLevel
	 */
	public int getActLernplanLevel() {
		return actLernplanLevel;
	}

	/**
	 * @param actLernplanLevel the actLernplanLevel to set
	 */
	public void setActLernplanLevel(int actLernplanLevel) {
		this.actLernplanLevel = actLernplanLevel;
	}

	
	

	/**
	 * Gibt Lernkosten für teures Talent zurück
	 * ToDo: Taverne: Kosten erhöht!
	 *
	 */
	 public int getLernKosten(Skill _skill){
         
		 
		 if (_skill==null){
			 return 0;
		 }
		
		 if (_skill.getName().equals("Alchemie")){
			 
				if (this.akademie==null){
					return this.getScriptUnit().getUnit().getModifiedPersons()*200;
				}else{
					return this.getScriptUnit().getUnit().getModifiedPersons()*400;	
				}
		 }
		 
		 if (_skill.getName().equals("Kräuterkunde")){
			 
				if (this.akademie==null){
					return this.getScriptUnit().getUnit().getModifiedPersons()*200;
				}else{
					return this.getScriptUnit().getUnit().getModifiedPersons()*400;	
				}
		 }
		 
		 
		 if (_skill.getName().equals("Taktik")){
	        	if (this.akademie==null){
					return this.getScriptUnit().getUnit().getModifiedPersons()*200;
				}else{
					return this.getScriptUnit().getUnit().getModifiedPersons()*400;	
				}
			}
			
	        if (_skill.getName().equals("Spionage")){
	        	if (this.akademie==null){
					return this.getScriptUnit().getUnit().getModifiedPersons()*200;
				}else{
					return this.getScriptUnit().getUnit().getModifiedPersons()*400;	
				}
			}
	        
	        
	        if (_skill.getName().equals("Magie") || _skill.getName().equals("illaun") || _skill.getName().equals("cerddor")
	        		|| _skill.getName().equals("draig") || _skill.getName().equals("tybied")
	        		|| _skill.getName().equals("gwyrrd")){
	        	
	        	
	        	return FFToolsUnits.calcMagieLernKosten(this.getScriptUnit().getUnit(), this.getScriptUnit().getScriptMain().gd_ScriptMain);
			}
	        
	        
	       
	    	        
		  if (akademie!=null){
			  return this.getScriptUnit().getUnit().getModifiedPersons() *50;
		  }
	      
		  return 0;
		 
		 
		
	 }

		/**
		 * Gibt Lerkosten für teures Talent zurück.. eressea.rules gibt da nix her...
		 * 
		*/
	 
	public int getLernKosten(SkillType _skilltype){
	  return this.getLernKosten(this.getStudyRequest().get(_skilltype));
	}

		/**
		 * @return the orderedSkillType
		 */
		public SkillType getOrderedSkillType() {
			return orderedSkillType;
		}

		/**
		 * @param orderedSkillType the orderedSkillType to set
		 */
		public void setOrderedSkillType(SkillType orderedSkillType) {
			this.orderedSkillType = orderedSkillType;
		}

		/**
		 * @return the ordererdSchüleranzahl
		 */
		public int getOrdererdSchüleranzahl() {
			return ordererdSchüleranzahl;
		}

		/**
		 * @param ordererdSchüleranzahl the ordererdSchüleranzahl to set
		 */
		public void setOrdererdSchüleranzahl(int ordererdSchüleranzahl) {
			this.ordererdSchüleranzahl = ordererdSchüleranzahl;
		}

		/**
		 * @return the akademieFromAM
		 */
		public Building getAkademieFromAM() {
			return akademieFromAM;
		}

		/**
		 * @param akademieFromAM the akademieFromAM to set
		 */
		public void setAkademieFromAM(Building akademieFromAM) {
			this.akademieFromAM = akademieFromAM;
		}
	 
	
		/**
		 * Liefert Anzahl (modified) Personen in den pooled Relationen
		 * bei isTeacher -> Anzahl der Schüler
		 * bei isSchueler -> Anzahl der Lehrer
		 */
		public int getAnzahlPooledPersons(){
			if (this.pooledRelation==null || this.pooledRelation.isEmpty()){
				return 0;
			}
			int erg = 0;
			for (AusbildungsRelation AR:this.pooledRelation){
				erg += AR.getScriptUnit().getUnit().getModifiedPersons();
			}
			return erg;
		}

		/**
		 * @return the orderedNewAka
		 */
		public boolean isOrderedNewAka() {
			return orderedNewAka;
		}

		/**
		 * @param orderedNewAka the orderedNewAka to set
		 */
		public void setOrderedNewAka(boolean orderedNewAka) {
			this.orderedNewAka = orderedNewAka;
		}

		public boolean isAvoidAka() {
			return avoidAka;
		}

		public void setAvoidAka(boolean avoidAka) {
			this.avoidAka = avoidAka;
		}
		
		/*
		 * unit soll nicht Lehrer sein
		 */
		public void setNoTeacher() {
			this.teachOffer=null;
			this.isTeacher=false;
		}
		
}



  