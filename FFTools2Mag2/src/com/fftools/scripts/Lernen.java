package com.fftools.scripts;

import magellan.library.Building;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.io.cr.CRParser;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsUnits;

public class Lernen extends MatPoolScript{
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private static final int Vorlauf = 60;
	private static final int Nachlauf =850; //Nach letzten Matpool?
	
	private int[] runners = {Vorlauf,Nachlauf};
	
	
	private Building akademie= null;
	private MatPoolRequest request = null;
	private int SilberPrio=550;
	private int anforderungsRunden=4;
    private double prioParameterB=-0.5;
	private double prioParameterC=0.0;	
	private double prioParameterD=100.0;	
	private double prioParameterA=SilberPrio-prioParameterD;
	
	Skill actSkill = null;
	int actLernTalent = 0;
	
	boolean LerneAuto=false;
	boolean LerneNoAuto=false;
	
	// Parameterloser constructor
	public Lernen() {
		super.setRunAt(this.runners);
     
	}
	
	public void runScript(int scriptDurchlauf){
		boolean mayConfirm = true;
		
		if (scriptDurchlauf==Vorlauf){
			
//			akademie feststellen falls einheit darin ist oder betritt.
	    	Building gebaeude = this.scriptUnit.getUnit().getModifiedBuilding();
	    	if (gebaeude != null){
	    		if ((this.scriptUnit.getScriptMain().gd_ScriptMain.rules.getBuildingType("Akademie").equals(gebaeude.getBuildingType()))&&(gebaeude.getSize()>=25)){
	    			this.akademie = gebaeude;
	    		}
	      	}
		// hier code fuer Lernen
		// addOutLine("....start Lernen mit " + super.getArgCount() + " Argumenten");
	    if (reportSettings.getOptionBoolean("LerneAuto", this.region())) {
	    	this.LerneAuto=true;
	    	this.addComment("Lerne: AUTO in den scripter-optionen erkannt");
	    }
		
		// erstmal ganz Einfach: Das erste Argument weiter lernen und bestaetigen
		if (super.getArgCount()<1) {
			super.scriptUnit.doNotConfirmOrders("Das Talent fehlt beim Aufruf von Lernen!");
		} else {
			String talent = getArgAt(0);
			
			if (talent.equalsIgnoreCase("auto")){
				this.LerneAuto=true;
				this.addComment("Lerne: AUTO erkannt");
				if (super.getArgCount()<2) {
					super.scriptUnit.doNotConfirmOrders("Das Talent fehlt beim Aufruf von Auto Lernen!");
					return;
				}
				talent = getArgAt(1);
				this.addComment("Prüfe auf Talentangabe: " + talent);
			}
			
			if (talent.equalsIgnoreCase("noauto")){
				this.LerneAuto=false;
				this.LerneNoAuto=true;
				this.addComment("Lerne: NO-AUTO erkannt");
				if (super.getArgCount()<2) {
					super.scriptUnit.doNotConfirmOrders("Das Talent fehlt beim Aufruf von Auto Lernen!");
					return;
				}
				talent = getArgAt(1);
				this.addComment("Prüfe auf Talentangabe: " + talent);
			}
			
			talent = talent.substring(0, 1).toUpperCase() + talent.substring(1).toLowerCase();
			
			SkillType skillType = super.gd_Script.getRules().getSkillType(talent);
			
			if (skillType==null){
				// wow, kein Lerntalent
				super.scriptUnit.doNotConfirmOrders("Kein Lerntalent! -> NICHT bestaetigt!");
				addOutLine("!!! ungueltiges Lerntalent bei " + this.unitDesc());
			} else {
				// Alles OK...Lerntalent erkannt
				// checken..haben wir nen max Talentstufe ?
				int maxTalent = 100;
				if ((!this.LerneAuto && !this.LerneNoAuto && super.getArgCount()> 1) || (this.LerneAuto && super.getArgCount()>2) || (this.LerneNoAuto && super.getArgCount()>2)) {
					String maxTalentS = getArgAt(1);
					if (this.LerneAuto) {
						maxTalentS = getArgAt(2);
					}
					if (this.LerneNoAuto) {
						maxTalentS = getArgAt(2);
					}
					int newMaxTalent = -1;
					try {
						newMaxTalent = Integer.parseInt(maxTalentS);
					} catch (NumberFormatException e){
						// wird unten behandelt..
					}
					if (newMaxTalent>0 && newMaxTalent<100) {
						maxTalent = newMaxTalent;
					} else {
						addOutLine("!!! ungueltiges LernMaxtalent bei " + this.unitDesc());
						mayConfirm = false;
						super.scriptUnit.doNotConfirmOrders("Fehler bei maxTalentStufe");
					}
				}
				// int actLernTalent = super.scriptUnit.getUnit().getSkill(skillType).getLevel();
				Unit actUnit = super.scriptUnit.getUnit();
				actSkill = actUnit.getModifiedSkill(skillType);
				// kein skill da, dann legen wir einen an..
				if (actSkill ==null){
					actSkill = new Skill(skillType,0,0,super.getUnit().getModifiedPersons(),true);
				    // Kannmann ja mal eintragen in unit
					super.getUnit().addSkill(actSkill);
				}
				if (actSkill!=null){
					// actLernTalent = actSkill.getModifiedLevel(actUnit,true);
					actLernTalent = FFToolsUnits.getModifiedSkillLevel(actSkill, actUnit, false);
				}
				this.scriptUnit.putTag(CRParser.TAGGABLE_STRING3, "Schüler - Skript");
				this.scriptUnit.putTag(CRParser.TAGGABLE_STRING4, talent);
				
				
				// Koste das Lernen was?
				int Kosten = this.getLernKosten(actSkill); 
				if(Kosten>0){
				    // Silber anfordern!
					this.workLernkosten();
				}
				
				String Befehl="LERNEN " + talent;
				if (this.LerneAuto) {
					Befehl = "LERNEN AUTO " + talent;
				}
				if (Kosten>0) {
					Befehl += " " + Kosten;
				}
				super.addOrder(Befehl, true);
				
				if (actLernTalent >= maxTalent) { 
					super.scriptUnit.doNotConfirmOrders("Unit hat maxTalentStufe erreicht.");
					mayConfirm = false;
				}
				if (mayConfirm){
					super.addComment("Lernen ok", true);
				}
			}
		}
		}
		
		// Nachlaufprüft ob Silber erhalten wurde
		if (scriptDurchlauf==Nachlauf){
			
			if (request!=null){
				if (request.getBearbeitet()<this.getLernKosten(actSkill)){
				// int a =  this.getLernKosten(actSkill);
				// int b = request.getBearbeitet();
				super.scriptUnit.doNotConfirmOrders("Lernkosten ungedeckt: " +request.getBearbeitet()+" von " +this.getLernKosten(actSkill)+ " Silber erhalten");
				}
			}
			
		}
		
		
		
		
	}

	
	
	private void workLernkosten(){
		
		//	eventuell veränderte VorratsRundenanzahl
		int newVorratsRunden=reportSettings.getOptionInt("Ausbildung_LernSilberVorratsRunden", this.region());
    	if (newVorratsRunden>-1 && newVorratsRunden<=10){
    		this.anforderungsRunden = newVorratsRunden;
    	}
		
		
		int userSilberPrio = reportSettings.getOptionInt("Ausbildung_SilberPrio", this.region());
		if (userSilberPrio>0){
			this.SilberPrio = userSilberPrio;
		}
		this.setPrioParameter(this.prioParameterA  ,this.prioParameterB,this.prioParameterC, this.prioParameterD);
		// runtergezählt damit der letzte request der aktuelle ist, dessen auszahlung im nachlauf geprüft werden kann.
		// FF: cleverly!
		if (this.anforderungsRunden>0){
			for (int n=this.anforderungsRunden;n>0;n--){
				request = new MatPoolRequest(this,this.getLernKosten(actSkill), "Silber",this.getPrio(n) ,"Lernen (in " + n + ")");	
				this.addMatPoolRequest(request);
			}
		}
		super.addComment("Lernkosten: " + this.getLernKosten(actSkill), true);
	}
	

		/**
		 * Gibt Lerkosten für teures Talent zurück.. eressea.rules gibt da nix her...
		 * FF: 20100912 Das war falsch, wurde ergänzt zwischenzeitlich
		*/
	 
	public int getLernKosten(Skill _skill){
		
		// für Magie extra...
		 if (_skill.getName().equals("Magie")){
	        	return FFToolsUnits.calcMagieLernKosten(this.getUnit(), this.gd_Script);
		}

		SkillType _skillType = null;
		_skillType = _skill.getSkillType();
		int basicCost = 0;
		basicCost = _skillType.getCost(1);
		
		if (this.akademie!=null){
			if (basicCost>0){
				basicCost *= 2;
			} else {
				basicCost = 50;
			}
		} 
	
		// Personenanzahl
		basicCost *= super.getUnit().getModifiedPersons();
		
		return basicCost;
	}
}
