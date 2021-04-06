package com.fftools.scripts;


import java.util.ArrayList;

import com.fftools.ScriptUnit;

import magellan.library.Order;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.io.cr.CRParser;
import magellan.library.rules.SkillType;

public class Lehren extends Script{
	
	private static final int Durchlauf = 82;
	private static final int Nachlauf =850;
	
	private int[] runners = {Durchlauf,Nachlauf};
	
	private ArrayList<String> pupils = new ArrayList<String>(0);
	private int countPupils = 0;
	
	// Parameterloser constructor
	public Lehren() {
		super.setRunAt(this.runners);
	}
	
	public void runScript(int scriptDurchlauf){
		if (scriptDurchlauf==Durchlauf) {
			runDurchlauf();
		}
		if (scriptDurchlauf==Nachlauf) {
			runNachlauf();;
		}
	}
	
	
	public void runDurchlauf(){
		countPupils = 0;
		// hier code fuer Lehren
		// addOutLine("....start Lehren mit " + super.getArgCount() + " Argumenten");
		if (super.getArgCount()<1) {
			super.scriptUnit.doNotConfirmOrders("Das Ziel fehlt beim Aufruf von LEHREN!");
			addOutLine("!!!fehlendes LEHRE - Ziel bei " + this.unitDesc());
		} else {
			// wir haben zumindest einen Schüler, eventuell mehrere
			// Argumente einer nach dem anderen durchgehen
			// temps erstmal rauslassen...
			// auf lehrmöglichkeit checken....und wenn alles OK, bestätigen
			boolean allPupilOK = true;
			for (int i = 0;i<super.getArgCount();i++){
				String actUnitNumber = super.getArgAt(i);
				int actPupils = checkNumber(actUnitNumber);
				if ((actPupils==-1)){
					allPupilOK = false;
					break;
				} else {
					countPupils += actPupils;
					this.pupils.add(actUnitNumber);
				}
			}
			// zu viele Schööler ?
			
			if (allPupilOK){
				super.addComment("Lehren ok",true);
			}
			// Order setzen, trotzdem, egal ob fehler und Tag setzen
			this.scriptUnit.putTag(CRParser.TAGGABLE_STRING3, "Lehrer - Skript");
			String newOrder = "LEHREN ";
			for (int i = 0;i<super.getArgCount();i++){
				newOrder = newOrder.concat(super.getArgAt(i) + " ");
			}
			super.addOrder(newOrder, true);
		}
	}
	
	private int checkNumber(String unitNumber){
		Unit u = super.scriptUnit.findUnitInSameRegion(unitNumber);
		if (u==null){
			super.scriptUnit.doNotConfirmOrders("Ein Schüler konnte nicht in der Region gefunden werden!");
			addOutLine("X....Ein Schüler konnte nicht in der Region gefunden werden bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			return -1;
		}
		if (u.getModifiedPersons()==0){
			super.addComment("Unit wurde durch LEHREN NICHT bestaetigt", true);
			super.scriptUnit.doNotConfirmOrders("Ein Schüler hat keine Personen mehr");
			addOutLine("X....Ein Schüler hat keine Personen mehr bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			return -1;
		}
		String lernTalent = getLearnSkillName(u);
		if (lernTalent.length()<2){
			super.scriptUnit.doNotConfirmOrders("Ein Schüler hat kein gefundenes Lerntalent (" + unitNumber + ")");
			addOutLine("X....Ein Schüler hat kein gefundenes Lerntalent (" + unitNumber + ") bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			return -1;
		}
		lernTalent = lernTalent.substring(0, 1).toUpperCase() + lernTalent.substring(1).toLowerCase();
		SkillType skillType = super.gd_Script.getRules().getSkillType(lernTalent);
		if (skillType==null){
			super.scriptUnit.doNotConfirmOrders("Ein Schüler hat kein erkanntes Lerntalent (" + unitNumber + "): " + lernTalent);
			addOutLine("X....Ein Schüler hat kein erkanntes Lerntalent (" + unitNumber + ", " + lernTalent + ") bei " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
			return -1;
		}
		// SkillVergleich
		Skill pupilSkill = u.getModifiedSkill(skillType);
		Skill teacherSkill = super.scriptUnit.getUnit().getModifiedSkill(skillType);
		int pupilSkillLevel = 0;
		if (pupilSkill!=null){
			pupilSkillLevel = pupilSkill.getLevel();
		}
		int teacherSkillLevel = 0;
		if (teacherSkill!=null){
			teacherSkillLevel = teacherSkill.getLevel();
		}
		
		
		
		if (!(teacherSkillLevel>pupilSkillLevel+1)){
			super.scriptUnit.doNotConfirmOrders("Ein Schüler kann nicht gelehrt werden (" + unitNumber + ")");
			return -1;
		}
		
		return u.getModifiedPersons();
	}
	
	private String getLearnSkillName(Unit u){
		String erg = "";
		for(Order o : u.getOrders2()) {
			String s = o.getText();
			String s_low = s.toLowerCase();
			if (s_low.startsWith("lerne")){
				// eine Lernorder gefunden. Kann nun lerne oder lernen sein
				// ist egal..wir suchen uns erstes Space..dahinter müsste talent 
				// folgen
				int i = s.indexOf(" ");
				if (i<2 || i>=s.length()){
					// nüscht
					return erg;
				}
				erg = s.substring(i+1);
				// falls auch da noch ein Leerzeichen drin ist, dahinter abschneiden
				i = erg.indexOf(" ");
				if (i>2) {
					erg = erg.substring(0,i).trim();
				}
			}
		}
		return erg;
	}
	
	
	/**
	 * wir prüfen, ob die Zahlen von Lehreren und Schülern zusammenpassen
	 */
	private void runNachlauf() {
		if (countPupils>(super.scriptUnit.getUnit().getModifiedPersons()*10)){
			int countTeacher = 0;
			ArrayList<String> foundTeacher = new ArrayList<String>(0);
			// wir haben mehr schüler als gedacht...mehr Lehrer suchen
			// durch alle einheiten der Region gehen, nur scriptunits 
			for (Unit u: this.scriptUnit.getUnit().getRegion().getUnits().values()){
				ScriptUnit su = this.scriptUnit.getScriptMain().getScriptUnit(u);
				if (su!=null) {
					Object o = su.getScript(Lehren.class);
					if (o!=null) {
						Lehren L = (Lehren)o;
						// wir haben einen Lehrer...lehrt der auch unsere Schüler?
						boolean isOurTeacher = false;
						for (String e : this.pupils) {
							if (L.isAPupil(e)) {
								isOurTeacher=true;
								break;
							}
						}
						if (isOurTeacher) {
							countTeacher+=u.getModifiedPersons();
							foundTeacher.add(u.toString(false));
						}
					}
				}
			}
			
			if (countPupils>(countTeacher*10)){
				super.scriptUnit.doNotConfirmOrders("Zu viele Schüler beim Aufruf von LEHREN!");
				super.scriptUnit.addComment(countTeacher + " Lehrers: " + foundTeacher.toString());
				addOutLine("!!!.Zu viele Schüler beim Aufruf von LEHREN bei " + this.unitDesc());
			} else {
				super.scriptUnit.addComment("advanced teacher check: " + countTeacher + " teachers: " + foundTeacher.toString());
			}
		}
	}
	
	
	public boolean isAPupil(String e) {
		boolean erg = false;
		for (String s : this.pupils) {
			if (s.equalsIgnoreCase(e)) {
				return true;
			}
		}
		return erg;
	}
	
	
}
