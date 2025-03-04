package com.fftools.scripts;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fftools.pools.ausbildung.AusbildungsPool;
import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Skill;
import magellan.library.StringID;
import magellan.library.rules.SkillType;
/**
 * AusbildungsRelationemitter, das zentrale Lernscript im Monopol
 * 
 * @author Marc
 *
 */

public class Lernfix extends MatPoolScript{
	
	
	private static final int Durchlauf = 68;
	private AusbildungsPool ausbildungsPool=null;
	
	private String LernplanName = null;
	
	/**
	 * parameter aka=false - bewirkt, dass trotz Lernfix kein Zugriff durch Akademiemanager 
	 */
	private boolean avoidAka=false;
	
	/**
	 * @return the lernplanName
	 */
	public String getLernplanName() {
		return LernplanName;
	}


	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Lernfix() {
		super.setRunAt(Durchlauf);
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
		
		if (scriptDurchlauf==Durchlauf){
			this.scriptStart();
		}
	}
	
	public void scriptStart(){
		
		if (this.getUnit().getModifiedPersons()<=0) {
			this.addComment("Lernfix nicht aktiviert, da keine Personen in der Einheit sind");
			return;
		}
		
		
		// Pool holen, man das ist umst�ndlich....
		ausbildungsPool = super.scriptUnit.getScriptMain().getOverlord().getAusbildungsManager().getAusbildungsPool(super.scriptUnit);
		// reation abschicken... 
		// FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Lernfix");
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		
		if (!this.avoidAka) {
			this.setAvoidAka(!OP.getOptionBoolean("aka", true));
			if (this.isAvoidAka()) {
				this.addComment("Lernfix: ich werde eine Einladung in eine Akademie ausschlagen!");
			} else {
				this.addComment("Lernfix: Akademieplatz denkbar!");
			}
		} else {
			this.addComment("Lernfix: gem�� Scriptorder werde ich eine Einladung in eine Akademie ausschlagen!");
		}
		
		String talentName = OP.getOptionString("Talent");
		if (talentName.length()>1){
			// wir haben Talent=XXX angabe in der scriot anweiseiung
			
			// checken, ob wir auch eine Ziellernstufe dabei haben...
			int talentZiel = OP.getOptionInt("Ziel",0);
			if (talentZiel>0){
				// OK, wir haben ziellevel...
				// levelcheck
				if (!FFToolsGameData.talentUnderLevel(this.getUnit(), this.gd_Script, talentName, talentZiel)){
					// nicht mehr lernfixen...
					// soll user entscheiden..zumindest nicht best�tigen
					// und Kommentar setzen
					this.scriptUnit.doNotConfirmOrders("Talentwert nicht unter Ziellevel (" + talentName + ": " + talentZiel + ")");
				}
			}
			
			// vorerst nur das angegebene Talent zum Lehren und Lernen setzen.
			HashMap<SkillType,Skill> sL = this.erzeugeSingleSkillList(talentName);
			if (sL.size()>0){
				// nur, wenn SkillIste durch TalentName gef�llt....
				AusbildungsRelation AR = new AusbildungsRelation(this.scriptUnit,sL,sL);
				
				// Einschub: alles Lehren....
				String setLehrer_DE = OP.getOptionString("Lehrer");
				String setLehrer_EN = OP.getOptionString("Teacher");
				if (setLehrer_DE.equalsIgnoreCase("ALLES") || setLehrer_EN.equalsIgnoreCase("ALL")){
					this.addComment("Lernfix: Universallehrer erkannt.");
					Map<StringID,Skill> mmTest = this.scriptUnit.getUnit().getSkillMap();
					if (mmTest==null){
						// keine Skillmap bei Temp-Units...
						// this.addComment("!!! keine Skills ??!! Lernfix als Lehrer unsinnig...");
						// this.doNotConfirmOrders();
						this.addComment("Lernfix-Universallehrer: leider nicht bei TEMP Units m�glich... (keine SkillMap)");
					} else {
						AR = new AusbildungsRelation(this.scriptUnit,sL, this.erzeugeSkillList(this.scriptUnit.getUnit().getSkillMap()));
					}
				}
				
				// Einschub: wenn nicht als Lehrer t�tig.....
				if (OP.getOptionBoolean("Lehrer", true)==false){
					this.addComment("Lernfix: Biete keine Talente zum Lehren an.");
					AR = new AusbildungsRelation(this.scriptUnit,sL, null);
				}
				if (OP.getOptionBoolean("Teacher", true)==false){
					this.addComment("Lernfix: Offer no skills for teaching.");
					AR = new AusbildungsRelation(this.scriptUnit,sL, null);
				}
				
				
				// Einschub Gratistalent
				if (OP.getOptionString("Gratistalent").length()>2){
					this.addGratisTalent(AR,OP.getOptionString("Gratistalent"));
				}
				
				AR.informScriptUnit();
				ausbildungsPool.addAusbildungsRelation(AR);
			}
			return;
		} 
		
		this.LernplanName = OP.getOptionString("Lernplan");
		if (this.LernplanName.length()>0){
			AusbildungsRelation AR = super.getOverlord().getLernplanHandler().getAusbildungsrelation(this.scriptUnit, this.LernplanName);
			if (AR!=null){
				// Einschub Gratistalent
				if (OP.getOptionString("Gratistalent").length()>2){
					this.addGratisTalent(AR,OP.getOptionString("Gratistalent"));
				}
				
				// 20210619: Pr�fe auch bei LernPlan auf Lehrer=nein
				boolean keinLehrer = false;
				if (OP.getOptionBoolean("Lehrer", true)==false){
					this.addComment("Lernfix: Biete keine Talente zum Lehren an.");
					keinLehrer=true;
				}
				if (OP.getOptionBoolean("Teacher", true)==false){
					this.addComment("Lernfix: Offer no skills for teaching.");
					keinLehrer=true;
				}
				
				if (keinLehrer) {
					AR.setNoTeacher();
				}
				
				AR.informScriptUnit();
				ausbildungsPool.addAusbildungsRelation(AR);
				if (AR.getActLernplanLevel()!=Lernplan.level_unset){
					// this.scriptUnit.ordersHaveChanged();
					// this.scriptUnit.setUnitOrders_adjusted(true);
				}

				// 20250215: Check Combat Status
				String checkString="Lernplan%" + this.LernplanName + "%AlwaysAGGRESSIVE";
				if (reportSettings.getOptionBoolean(checkString)) {
					if (this.scriptUnit.setAGGRESSIVE()) {
						this.addComment("Kampfstatus gesetzt (scripterSetting: Lernplan_AGGRESSIVE)");
					}
				}
				checkString="Lernplan%" + this.LernplanName + "%AlwaysFRONT";
				if (reportSettings.getOptionBoolean(checkString)) {
					if (this.scriptUnit.setFRONT()) {
						this.addComment("Kampfstatus gesetzt (scripterSetting: Lernplan_FRONT)");
					}
				}
				checkString="Lernplan%" + this.LernplanName + "%AlwaysBACK";
				if (reportSettings.getOptionBoolean(checkString)) {
					if (this.scriptUnit.setBACK()) {
						this.addComment("Kampfstatus gesetzt (scripterSetting: Lernplan_BACK)");
					}
				}
				checkString="Lernplan%" + this.LernplanName + "%AlwaysFLEE";
				if (reportSettings.getOptionBoolean(checkString)) {
					if (this.scriptUnit.setFlee()) {
						this.addComment("Kampfstatus gesetzt (scripterSetting: Lernplan_FLEE)");
					}
				}
				
				
			} else {
				// keine AR -> Lernplan beendet ?!
				this.scriptUnit.doNotConfirmOrders("Lernplan liefert keine Aufgabe mehr");
				// default erg�nzen - keine Ahnung, was, eventuell kan
				// die einheit ja nix..
				this.addOrder("Lernen Ausdauer", true);
			}
			return;
		}
		
		
		// wir haben keine Hauptanwqeisung, also default
		if (this.scriptUnit.getUnit().getSkills()!=null){
			ausbildungsPool.addAusbildungsRelation(new AusbildungsRelation(this.scriptUnit, this.erzeugeSkillList(this.scriptUnit.getUnit().getSkillMap()), this.erzeugeSkillList(this.scriptUnit.getUnit().getSkillMap()) ));
		}
		
		
	
	}

	
	/**
	 * Setzt die SkillMap aus Magellan in eine ScriptSkillListe um, die die Ausbildungsrelation schluckt.
	 * @param _m
	 * @return
	 */
      
	  private HashMap<SkillType,Skill> erzeugeSkillList(Map<StringID,Skill> _m){
		HashMap<SkillType,Skill> liste = new HashMap<SkillType,Skill>();
		if (_m==null || _m.values()==null) {
			this.doNotConfirmOrders("!!!Debug: erzeuge SkillList scheitert!!!");
		} else {
			for(Iterator<Skill> iter = _m.values().iterator();iter.hasNext();){
				Skill skill =( Skill) iter.next();
				liste.put(skill.getSkillType(), skill);	
			}
		}
		return liste;
	  }


	  
	  /**
	   * erzeugt eine SkillListe mit nur einem Eintrag, welcher durch einen Talentnamen
	   * angegeben wird, oder eine Leere Liste, falls ein Schreibfehler aufgetreten ist 
	   * bzw die Unit diesen Skill nicht besitzt.
	   * @param talentName
	   * @return
	   */
	  private HashMap<SkillType,Skill> erzeugeSingleSkillList(String talentName){
		  HashMap<SkillType,Skill> liste = new HashMap<SkillType,Skill>();
		  SkillType actSkillType = this.gd_Script.getRules().getSkillType(talentName);
		  SkillType magieType = null;
		  Skill magieSkill = null;
		  
		  if (actSkillType==null){
			  // Screibweise falsch?!
			  doNotConfirmOrders("!!!Lernfix: Talent nicht erkannt");
			  outText.addOutLine("!!!Lernfix Talent nicht erkannt: " + this.unitDesc());
		  } else {
			  Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
			  if (actSkill==null){
				  // neuen Skill erzeugen
				  actSkill = new Skill(actSkillType,0,0,this.scriptUnit.getUnit().getModifiedPersons(),true);
			  }
			  // Wenns ein Magier ist m�ssen wir fiktive Skills unterschieben...
			  if (actSkill.getSkillType().equals(this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getSkillType("Magie"))){
				  // OK ist ein Magier...
				  
				  magieType = this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getSkillType(this.scriptUnit.getUnit().getFaction().getSpellSchool(), true);
				  if (magieType==null){
					  this.doNotConfirmOrders("!!!F�r diese Einheit konnte das Magiegebiet nicht erkannt werden!!!");
				  } else {
					  this.addComment("Lernfix: (debug) SillType-Name generiert mit :" + this.scriptUnit.getUnit().getFaction().getSpellSchool());
					  this.addComment("Lernfix: (debug) SillType to String liefert 1:" + magieType.toString());
					  this.addComment("Lernfix: (debug) SillType to String liefert 2:" + magieType.getName());
					  
	                  magieSkill = new Skill (magieType,0, actSkill.getLevel()  ,this.scriptUnit.getUnit().getModifiedPersons(),true);
	                  // magische skills einf�gen 		  
	                  liste.put(magieType, magieSkill);
				  }
			  }
			  
			  else {
				  // nicht magische skills einf�gen
				 liste.put(actSkillType, actSkill);  
			  }
			  
			  
		  }
		  return liste;
	  }


	  private void addGratisTalent(AusbildungsRelation AR,String Name){
		  SkillType actSkillType = this.gd_Script.getRules().getSkillType(Name);
		  if (actSkillType==null){
			  // Screibweise falsch?!
			  addComment("!!!Lernfix: GratisTalent nicht erkannt");
			  outText.addOutLine("!!!Lernfix GratisTalent nicht erkannt: " + this.unitDesc());
		  } else {
			  Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
			  if (actSkill==null){
				  // neuen Skill erzeugen
				  actSkill = new Skill(actSkillType,0,0,this.scriptUnit.getUnit().getModifiedPersons(),true);
			  }
			  AR.setDefaultGratisTalent(actSkill);
		  }
	  }
	  
	  /**
		 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
		 * dieserart script registriert werden soll
		 * wird �berschrieben mit return true z.B. in ifregion, ifunit und request...
		 */
		public boolean allowMultipleScripts(){
			return false;
		}


	public boolean isAvoidAka() {
		return avoidAka;
	}


	public void setAvoidAka(boolean avoidAka) {
		this.avoidAka = avoidAka;
	}




}
