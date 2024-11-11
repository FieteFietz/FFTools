package com.fftools.scripts;

import magellan.library.Skill;
import magellan.library.rules.SkillType;


public class Ifskill extends Script{
	
	private int[] runners = {14,15,16};
	private boolean scriptCalled = false;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Ifskill() {
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
		
		if (this.scriptCalled) {
			return;
		}
		
		this.scriptCalled=true;
		
		// hier code fuer Script
		boolean skillOK = false;

		
		// zur Sicherheit..falls parsen schief geht, ist skillOK nicht wahr
		

		if (super.getArgCount()<2) {
			// falsche Anzahl Paras
			this.doNotConfirmOrders("Falscher Aufruf von IfSkill: zu geringe Anzahl Parameter.");
			addOutLine("X....Falscher Aufruf von IfSkill: zu geringe Anzahl Parameter: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			return;
		}
		
		if (super.getArgCount()>1) {

			// Falls = in nächster Angabe ist es Talent=Level
			if (super.getArgAt(0).indexOf('=') > 0) {
				String[] tokens = super.getArgAt(0).split("=");
				String TalentName = tokens[0];
				String SkillLevel = tokens[1];
				
				String test = TalentName.substring(0, 1).toUpperCase() + TalentName.substring(1).toLowerCase();
				SkillType actSkillType = this.gd_Script.getRules().getSkillType(test);
				if (actSkillType!=null) {
					TalentName = test;
				} else {
					this.doNotConfirmOrders("!!! IfSkill - Talent ist ungültig! -> Unbestaetigt!! (check war: " + test + ")");
					return;
				}
				int SkillLevelInt = -1;
				try {
					SkillLevelInt = Integer.parseInt(SkillLevel);
				} catch (NumberFormatException e) {
					this.doNotConfirmOrders("!!! IfSkill - Talentziel ist ungültig! -> Unbestaetigt!! (check war: " + SkillLevel + ")");
					return;
				}
				
				if (SkillLevelInt<0 || SkillLevelInt>99) {
					this.doNotConfirmOrders("!!! IfSkill - Talentziel ist ungültig! -> Unbestaetigt!! (check war: " + SkillLevel + ", muss zwischen 0 und 99 sein)");
					return;
				}
				
				
				Skill skill = this.getUnit().getModifiedSkill(actSkillType);
				
				if (skill==null) {
					if (SkillLevelInt==0) {
						skillOK=true;
						this.addComment("IfSkill: Talent " + TalentName + " hat Level 0 und die Bedingung (=" + SkillLevelInt + ") ist erfüllt" );
					} else {
						this.addComment("IfSkill: Talent " + TalentName + " hat Level 0 und die Bedingung (=" + SkillLevelInt + ") ist NICHT erfüllt" );
						return;
					}
				} else {
					if (skill.getLevel()>=SkillLevelInt) {
						skillOK=true;
						this.addComment("IfSkill: Talent " + TalentName + " hat Level " + skill.getLevel() + " und die Bedingung (=" + SkillLevelInt + ") ist erfüllt" );
					} else {
						this.addComment("IfSkill: Talent " + TalentName + " hat Level " + skill.getLevel() + " und die Bedingung (=" + SkillLevelInt + ") ist NICHT erfüllt" );
						return;
					}
					}
			} else {
				this.doNotConfirmOrders("Falscher Aufruf von IfSkill: {Talent}={Level} als Parameter erwartet");
				addOutLine("X....Falscher Aufruf von IfSkill: {Talent}={Level} als Parameter erwartet: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
				return;
			}
				
			// skillOK ist nur dann true, wenn in Bedingung erfüllt  und ParsenOK..somit alles fein
			// nächsten Parameter anschaunen..entweder eressea-befgehl = irgendetwas
			// oder schlüsselwort script...
			if (skillOK){
				String keyWord = super.getArgAt(1);
				if (keyWord.equalsIgnoreCase("script")) {
					if (super.getArgCount()>2) {
						// ok, es soll ein script aufgerufen werden
						// eigentlich checken, ob dass von den scriptdurchläufen her passt
						// ansonsten parametersatz bauen und ergänzen....
						String newOrderLine = "";
						for (int i = 3;i<super.getArgCount();i++){
							newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
						}
						super.scriptUnit.findScriptClass(super.getArgAt(2), newOrderLine,true);
					} else {
						// die befehlszeile endet mit dem keyWord script
						super.scriptUnit.doNotConfirmOrders("Unerwartetes Ende der Befehlszeile (script IfSkill)");
						addOutLine("X....Unerwartetes Ende der Befehlszeile (IfSkill,script): " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
					}
				} else {
					// kein script Befehl...alles was jetzt kommt als Order verpacken...
					// inkl des ersten wortes
					String newOrderLine = "";
					for (int i = 1;i<super.getArgCount();i++){
						newOrderLine = newOrderLine.concat(super.getArgAt(i) + " ");
					}
					if (newOrderLine.length()>0){
						// nun denn ... fertig und irgendetwas zu schreiben
						newOrderLine = newOrderLine.concat(" ;script IfSkill");
						super.addOrder(newOrderLine,true);
						super.addComment("Unit wurde durch IfSkill bestaetigt", true);
					}
				}
			} else {
				this.doNotConfirmOrders("Unerwarteter Fehler bei IfSkill");
				addOutLine("X....Unerwarteter Fehler bei IfSkill: " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			}
		}
	}
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
}
