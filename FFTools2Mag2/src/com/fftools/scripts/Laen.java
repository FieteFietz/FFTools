package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Building;
import magellan.library.Region;
import magellan.library.RegionResource;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

public class Laen extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 51;
	

	
	private int[] runners = {Durchlauf_1};
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Laen() {
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
		if (scriptDurchlauf==this.Durchlauf_1){
			this.start();
		}

	}
	
	
	
	/**
	 * eigentlich ganz einfach: nur so lange eisen machen,
	 * bis die Talentstufe nicht mehr ausreicht
	 */
	private void start(){
		// pre Check....werde ich im Bergwerk sein
		Building b = this.scriptUnit.getUnit().getModifiedBuilding();
		if (b==null || !b.getType().toString().equalsIgnoreCase("Bergwerk")){
			this.addComment("!!!Laen: ich bin nicht im Bergwerk!!!!");
			if (!(b==null)){
				this.addComment("Debug: ich bin nämlich in:" + b.getType().toString());
			}
			this.addOrder("Lernen Bergbau", true);
			this.doNotConfirmOrders();
			return;
		}
		// Eigene Talentstufe ermitteln
		int skillLevel = 0;
		SkillType skillType = this.gd_Script.getRules().getSkillType("Bergbau", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
			} else {
				this.addComment("!!! unit has no skill Bergbau!");
			}
		} else {
			this.addComment("!!! can not get SkillType Bergbau!");
		}
		if (skillLevel>0){
			// Regionslevel beziehen
			Region R = this.scriptUnit.getUnit().getRegion();
			ItemType IT = this.gd_Script.getRules().getItemType("Laen");
			RegionResource RR = R.getResource(IT);
			if (RR.getSkillLevel()<=skillLevel) {
				// weiter machen
				this.addComment("Laen in der Region bei T" + RR.getSkillLevel() + ", wir bauen weiter ab, ich kann ja T" + skillLevel);
				this.addOrder("machen Laen ;(script Laen)", true);
			} else {
				this.addComment("Laen in der Region bei T" + RR.getSkillLevel() + ", wir bauen NICHT weiter ab, ich kann ja nur T" + skillLevel);
				this.addComment("Ergänze Lernfix Eintrag mit Talent=Bergbau");
				// this.addOrder("Lernen Bergbau", true);
				Script L = new Lernfix();
				ArrayList<String> order = new ArrayList<String>();
				order.add("Talent=Bergbau");
				L.setArguments(order);
				L.setScriptUnit(this.scriptUnit);
				L.setGameData(this.gd_Script);
				if (this.scriptUnit.getScriptMain().client!=null){
					L.setClient(this.scriptUnit.getScriptMain().client);
				}
				this.scriptUnit.addAScript(L);
				FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Laen");
				String modeSetting = OP.getOptionString("mode");
				this.addComment("searching for automode setting, found: " + modeSetting);
				if (modeSetting.equalsIgnoreCase("auto")){
					this.addComment("AUTOmode detected....confirmed learning");
				} else {
					this.addComment("no AUTOmode detected....pls confirm learning / adjust orders");
					this.doNotConfirmOrders();
				}
			}
		} else {
			this.addOrder("Lernen Bergbau", true);
			this.doNotConfirmOrders();
		}
		
	}

}
