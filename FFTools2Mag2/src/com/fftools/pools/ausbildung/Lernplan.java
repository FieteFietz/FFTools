package com.fftools.pools.ausbildung;

import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.utils.FFToolsOptionParser;

/**
 * Enthält Definitionen und Hilfstools zu einem Lernplan
 * @author Fiete
 *
 */
public class Lernplan {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	public static final int level_unset=-1;
	public static final int level_afterSets=-2;
	
	/**
	 * Liste aller Talente im Lernplan
	 */
	private Hashtable<SkillType,LernplanTalent> talente = null;
	
	/**
	 * Name dieses Lernplanes (inklusive eventuellem %)
	 */
	private String name = null;
	
	/**
	 * der Level oder die Stufe dieses Lernplanes, oder den wert level_unset
	 */
	private int lernPlanLevel = level_unset; 
	
	/**
	 * Konstruktor
	 *
	 */
	public Lernplan(ScriptUnit u,String lernPlanName){
		this.name = lernPlanName;
		// level setzen
		if (lernPlanName.indexOf(LernplanHandler.lernplanDetailSeparator)>0){
			String[] parts = lernPlanName.split(LernplanHandler.lernplanDetailSeparator);
			// im zweiten Teil steckt die LernplanStufe
			String planLevelString = parts[1];
			// versuch in Int umzuwandeln
			boolean newLevelOK=true;
			try {
				Integer i = Integer.valueOf(planLevelString);
				int newLevel = i.intValue();
				if (newLevel>0){
					this.lernPlanLevel = newLevel;
				} else {
					newLevelOK=false;
				}
			} catch (NumberFormatException e){
				newLevelOK=false;
			}
			if (!newLevelOK){
				// Levelerkennung gescheitert
				outText.addOutLine("!!! Lernplan: Stufe nicht erkannt! " + u.unitDesc() , true);
				u.addComment("!!! Lernplan: Stufe nicht erkannt");
				u.doNotConfirmOrders();
			}
		}
	}
	
	
	/**
	 * 
	 * setzt parameter und Talente dieses Lernplanes nach den 
	 * angaben der Orderzeile (in Optionparser)
	 * 
	 * wir schleppen u mit für eventuelle parsefehler ->anzeigen, wo sie
	 * auftreten
	 * 
	 * @param u
	 * @param OP
	 */
	public void parseOptionLine(ScriptUnit u, FFToolsOptionParser OP){
		// TalentNamen herausfinden
		String talentName = OP.getOptionString("Talent");
		String talentZielStufe = "";
		// Talent ist zwingend erforderlich
		// bei Fehler abbruch
		// check1: überhaupt angegeben
		if (talentName.length()<2){
			outText.addOutLine("!!! Lernplan:TalentName nicht erkannt! " + u.unitDesc() , true);
			u.addComment("!!! Lernplan:TalentName nicht erkannt");
			u.doNotConfirmOrders();
			return;
		}
		// check2: Kein SkillType
		// dazu vorher den skillnamen extrahieren, falls mit % nach weitere Infos verknüpft
		if (talentName.indexOf(LernplanHandler.lernplanDetailSeparator)>0){
			String[] parts = talentName.split(LernplanHandler.lernplanDetailSeparator);
			// im ersten Teil steckt der TalentName
			talentName = parts[0];
			talentZielStufe = parts[1];
		} else {
			// Talentname ohne weitere angabe...->fehler
			outText.addOutLine("!!! Lernplan:Talentzielstufe nicht angegeben! " + u.unitDesc() , true);
			u.addComment("!!! Lernplan:Talentzielstufe nicht angegeben");
			u.doNotConfirmOrders();
			return;
		}
		// (wie gut dass wir scriptunit haben...link auf Gamedata vorhanden..
		SkillType skillType = u.getScriptMain().gd_ScriptMain.rules.getSkillType(talentName);
		if (skillType==null){
			// skill nicht gefunden -> abbruch
			outText.addOutLine("!!! Lernplan:Talent unbekannt! " + u.unitDesc() , true);
			u.addComment("!!! Lernplan:TalentName unbekannt");
			u.doNotConfirmOrders();
			return;
		}
		// OK, alles fein soweit...LernplanTalent finden bzw anlegen
		if (this.talente==null){
			this.talente = new Hashtable<SkillType, LernplanTalent>();
		}
		
		LernplanTalent actLPT = this.talente.get(skillType);
		if (actLPT==null){
			actLPT = new LernplanTalent(u,skillType,talentZielStufe);
			this.talente.put(skillType,actLPT);
		}
		// weiterparsen im Talent
		actLPT.parseOptionLine(u, OP);
	}


	/**
	 * überprüft, ob übergebener Lernplan zur gleichen Kategorie oder Masterplan gehört
	 * also mit dem gleichen Namen vor dem % anfängt
	 * @param actLP
	 * @return
	 */
	public boolean isSameMasterLernplan(Lernplan actLP){
		String myName = this.name;
		int pisI = myName.indexOf(LernplanHandler.lernplanDetailSeparator);
		if (pisI>0){
			myName = myName.substring(0, pisI);
		}
		String otherName = actLP.getName();
		pisI = otherName.indexOf(LernplanHandler.lernplanDetailSeparator);
		if (pisI>0){
			otherName = otherName.substring(0, pisI-1);
		}
		if (myName.equalsIgnoreCase(otherName)){
			return true;
		}
		
		
		return false;
	}
	
	
	/**
	 * @return the lernPlanLevel
	 */
	public int getLernPlanLevel() {
		return lernPlanLevel;
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * Überprüft, ob u noch "unter" einer der Anforderungen dieses Lernplanes
	 * ist.
	 * Wenn ja, ergänzt es die Ausbildungsrelation und liefert true
	 * sonst false - einheit hat die Ausbildungsziele bereits erreicht. 
	 * 
	 * @param u
	 * @param AR
	 */
	public boolean appendAusbildungsRelation(ScriptUnit u,AusbildungsRelation AR){
		boolean istUnterAnforderungen = false;
		SkillType defaultLernType = null;
		int minDefaultTypeLevel = Integer.MAX_VALUE;
		for (Iterator<LernplanTalent> iter=this.talente.values().iterator();iter.hasNext();){
			LernplanTalent LPT = (LernplanTalent)iter.next();
			if (LPT.istUnterAnforderungen(u)){
				istUnterAnforderungen=true;
				if (LPT.isInRestriktionen(u)){
					if (defaultLernType==null){
						defaultLernType = LPT.getSkillType();
						minDefaultTypeLevel = LPT.getActUnitLevel(u);
					} else {
						if (LPT.getActUnitLevel(u)<minDefaultTypeLevel){
							defaultLernType = LPT.getSkillType();
							minDefaultTypeLevel = LPT.getActUnitLevel(u);
						}
					}
				}
			}
		}
		
		if (istUnterAnforderungen){
			// 	yep...mindestens eine Anforderung ist (noch) nicht erfüllt
			// AusbRel setzen lassen
			for (Iterator<LernplanTalent> iter=this.talente.values().iterator();iter.hasNext();){
				LernplanTalent LPT = (LernplanTalent)iter.next();
				LPT.appendAusbildungsRelation(u, AR);
			}
			if (defaultLernType!=null){
				Skill defaultSkill = new Skill(defaultLernType,0,minDefaultTypeLevel,u.getUnit().getModifiedPersons(),true);
				// Marc: Nutzt nun nicht mehr subject
				AR.setDefaultTalent(defaultSkill);
			}
			// Level der AR mitteilen
			AR.setActLernplanLevel(this.lernPlanLevel);
			return true;
		} else {
			// nein, alles erfüllt bzw keine Restiktionen
			// erfüllte Talente wenn nicht anders vorgegeben als Lehrangebot hinzufügen
			for (Iterator<LernplanTalent> iter=this.talente.values().iterator();iter.hasNext();){
				LernplanTalent LPT = (LernplanTalent)iter.next();
				LPT.appendAusbildungsRelationTeacher(u, AR);
			}
			return false;
		}
	}
	

}
