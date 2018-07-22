package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;

/**
 * 
 * Job "RegionObserver": Watches region for monsters and other unfriendlies
 * Call: // script RegionObserver [EmbassyUnits=<UnitID>[,<UnitID>]]
 * - Learns according to plan "RegionObserver" (minimum defense, primarily observation)
 * - Guards region unless enemies are present, then he yells and flees combat
 * - Required Items: Cheap "talent-friendly" weapon
 * - Optional Items: AdwS 
 * 
 * @author Torsten
 *
 */

public class Regionobserver extends MatPoolScript{

	// Durchlauf vor Lernfix, wg. Lernplan
	// private static final int Durchlauf = 15;
	
	private int Durchlauf_vorMatPool = 15;
	private int Durchlauf_nachMatPool = 740;
	
	private int[] runners = {Durchlauf_vorMatPool,Durchlauf_nachMatPool};

	// Keine Bewachung ohne Waffen...
	private final String[] talentNamen = {"Hiebwaffen","Stangenwaffen","Bogenschießen","Armbrustschießen","Katapultbedienung"};
	// ... und erst werden die Wächter versorgt, dann die Steuereintreiber
	private int WaffenPrio = 350;
	// Ausrüstungs-Requests gehen über den Pool
	private ArrayList<MatPoolRequest> requests = new ArrayList<MatPoolRequest>();
	// möchten wir schreien oder bewachen ?
	private int alertStatus=0;
	// Falls Waffe explizit angegeben werden soll
	private String WaffenName="";
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Regionobserver() {
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
		
		if (scriptDurchlauf==Durchlauf_vorMatPool){
			this.scriptStart();
		}
		
		if (scriptDurchlauf==Durchlauf_nachMatPool){
			this.scriptEnde();
		}
		
	}
	
	private void scriptStart(){
		
		// Parameter
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		// Optionen lesen und Prüfen
		
		// Prios
		int prio = OP.getOptionInt("Prio",0);
		if (prio>0) {
			this.WaffenPrio = prio;
		}
		
		WaffenName = OP.getOptionString("Waffe");
		
		// 1. Lernen nach Plan...
		// this.scriptUnit.findScriptClass("Lernfix", "Lernplan=RegionObserver");
		// this.addComment("Hinweis: das script RegionObserver erwartet einen gleichnamigen Lernplan: RegionObserver");
		
		
		// 2. Geeignete Waffe anfordern (abgekupfert von "Treiben")
		String comment = "RegionObserver-Waffen";
		boolean didSomething = false;
		
		
		if (WaffenName.length()<=2){
			for (int i = 0;i<this.talentNamen.length;i++){
				String actName = this.talentNamen[i];
				SkillType actSkillType = this.gd_Script.rules.getSkillType(actName);
				if (actSkillType!=null){
					Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
					if (actSkill!=null && actSkill.getLevel()>0){
						// Was gefunden? --> Preiswerte Waffe anfordern
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
							MatPoolRequest MPR = new MatPoolRequest(this,this.scriptUnit.getUnit().getModifiedPersons(),matNameNeu,this.WaffenPrio,comment);
							this.addMatPoolRequest(MPR);
							didSomething=true;
							this.requests.add(MPR);
							this.addComment("RegionObserver (debug): fordere " + MPR.getOriginalGefordert() + " " + MPR.getOriginalGegenstand() + " mit Prio " + MPR.getPrio() + " an.");
						}
						
						
					}
				}
			}
		} else {
			// Waffe angegeben
			boolean WaffeOK=false;
			ItemType itemType = super.gd_Script.getRules().getItemType(WaffenName);
			if (itemType==null) {
				boolean isCat = reportSettings.isInCategories(WaffenName);
				if (isCat){
					WaffeOK=true;
				} 
			} else {
				// ItemType bekannt
				WaffeOK=true;
			}
			if (WaffeOK){
				MatPoolRequest MPR = new MatPoolRequest(this,this.scriptUnit.getUnit().getModifiedPersons(),WaffenName,this.WaffenPrio,comment);
				this.addMatPoolRequest(MPR);
				didSomething=true;
				this.requests.add(MPR);
				this.addComment("RegionObserver (debug): fordere " + MPR.getOriginalGefordert() + " " + MPR.getOriginalGegenstand() + " mit Prio " + MPR.getPrio() + " an.");
			} else {
				this.addComment("!!! RegionObserver: angegebene Waffe ist weder ein Gegenstand noch eine Kategorie ?!?");
				this.doNotConfirmOrders();
			}
			
		}
		
		// Amulett bestellen, aber mit niedriger Prio
		MatPoolRequest MPR = new MatPoolRequest(this,1,"Amulett des wahren Sehens",30,"RegionObserver-Amulett");
		this.addMatPoolRequest(MPR);
		this.addComment("RegionObserver (debug): fordere " + MPR.getOriginalGefordert() + " " + MPR.getOriginalGegenstand() + " mit Prio " + MPR.getPrio() + " an.");
		
		if (!didSomething){
			this.addComment("Keine Waffenanforderung - kein Talent vorhanden?");
			this.doNotConfirmOrders();
		}
		
		// 3. Bewachen, falls keine Gegner in der Region sind, sonst Alarm 
		
		// In dieser Region erlaubte Botschafter aus Einheit-Optionen auslesen
		String[] embassyUnits = null;
		String eU = OP.getOptionString("EmbassyUnits");
		embassyUnits = eU.split(",");
		Arrays.sort(embassyUnits);
		
		// Parteien, denen vertraut wird, aus den Report-Optionen holen
		String[] trustedFactions = null;
		String tF = reportSettings.getOptionString("TrustedFactions", this.region());
		if (tF==null) tF = "";
		trustedFactions = tF.split(",");
		List<String> trustedFactionList = Arrays.asList(trustedFactions);
		List<String> embassyUnitsList = Arrays.asList(embassyUnits);
		// Check all units in region
		
		
		for (Iterator<Unit> iter = this.region().units().iterator();iter.hasNext();){
			Unit actU = (Unit)iter.next();
			boolean badUnit=false;
			String actF = actU.getFaction().getID().toString().toLowerCase();
			
			// Do we trust this faction?
			if (!trustedFactionList.contains(actF)) {
				// No, but maybe we trust this unit?
				// if (Arrays.binarySearch(embassyUnits, actU.getID().toString()) < 0) {
				if (!embassyUnitsList.contains(actU.getID().toString())) {
					// Uh-oh, suspicious unit found!
					badUnit=true;
				}
			}
			
			if (actU.isSpy()){
				badUnit=true;
			}
			
			if (badUnit){
				// Uh-oh, suspicious unit found!
				this.addComment("WARNUNG: Einheit " + actU.toString(true) + " wird nicht vertraut!");
				alertStatus=1;
				this.doNotConfirmOrders();
			}
		}

		

		// war mal 1., jetzt 4.
		// Feststellung, was und wie gelernt werden soll
		boolean hasLearnOrder = false;
		if (OP.getOptionString("Lernplan")!=""){
			// es wurde ein LernplanName übergeben
			String LP_Name = OP.getOptionString("Lernplan");
			Lernplan LP = this.scriptUnit.getOverlord().getLernplanHandler().getLernplan(this.scriptUnit, LP_Name, false);
			if (LP==null){
				this.addComment("!!!Lernplan nicht bekannt: " + LP_Name);
				this.doNotConfirmOrders();
			} else {
				// alles schön
				this.scriptUnit.findScriptClass("Lernfix", "Lernplan=" + LP_Name);
				hasLearnOrder = true;
			}
		}
		
		if (OP.getOptionString("Talent")!="" && !hasLearnOrder){
			// es wurde ein LernplanName übergeben
			String talent = OP.getOptionString("Talent");
			talent = talent.substring(0, 1).toUpperCase() + talent.substring(1).toLowerCase();
			SkillType skillType = super.gd_Script.rules.getSkillType(talent);
			if (skillType==null){
				this.addComment("!!!Lerntalent nicht bekannt: " + talent);
				this.doNotConfirmOrders();
			} else {
				// alles schön
				this.scriptUnit.findScriptClass("Lernfix", "Talent=" + talent);
				hasLearnOrder = true;
			}
		}
		
		// existiert ein default-Lernplan RegionObserver?
		if (!hasLearnOrder){
			Lernplan LP = this.scriptUnit.getOverlord().getLernplanHandler().getLernplan(this.scriptUnit, "RegionObserver", false);
			if (LP==null){
				this.addComment("!!!Lernplan nicht bekannt: RegionObserver");
				this.addComment("Hinweis: wenn ein Lernplan RegionObserver definiert wurde, so wird dieser genutzt.");
				this.doNotConfirmOrders();
			} else {
				// alles schön
				this.scriptUnit.findScriptClass("Lernfix", "Lernplan=RegionObserver");
				hasLearnOrder = true;
			}
		}

		// Tja, was nun? 
		// Wenn keine Lernorder, dann unbestätigt Lassen und default anbieten
		if (!hasLearnOrder){
			this.doNotConfirmOrders();
			this.addComment("!!! Dem RegionObserver ist nicht bekannt, was gelernt werden soll!!!");
			this.addOrder("Lernen Wahrnehmung", true);
		}
		
		// 5. Further Tasks...
		// TODO oben: Lernplan für "Volks-Waffentalent" ermöglichen (Halbling=Armbrust,
		//		Troll=Hiebwaffen, Insekt=Stangenwaffen, usw.
		
	}
	
	
	private void scriptEnde(){
		// Alle gefundenen Einheiten vertrauenswürdig? Falls ja, bewachen und 
		this.addComment("RegionObserver: prüfe auf Bewaffnung:");
		int countWeapons = 0;
		for (MatPoolRequest MPR : this.requests) {
			countWeapons += MPR.getBearbeitet();
			this.addComment("RegionObserver: zähle " + MPR.getBearbeitet() + " " + MPR.getOriginalGegenstand());
		}
		if (alertStatus==0) {
			// Falls die Einheit noch nicht bewacht, setze Befehle
			if (this.scriptUnit.getUnit().getGuard()==0) {
				
				// Aber dazu muss eine Waffe vorhanden sein!
				// erhaltene Waffen zählen
				if (countWeapons >= this.getUnit().getModifiedPersons()){
					// reicht
					this.scriptUnit.addOrder("BEWACHEN", false);
					this.scriptUnit.addOrder("KÄMPFE NICHT", false);
				} else {
					// reicht nicht
					// jammern
					this.addComment("!!! Regionobserver kann nicht bewachen, da keine Waffenversorgung erkannt wurde. (" + countWeapons + "/" + this.getUnit().getModifiedPersons() + ")");
				}
			} else {
				// wir bewachen schon, wenn nun nicht genügend waffen, warnen, dass bewachung aufgelöst werden wird
				if (countWeapons < this.getUnit().getModifiedPersons()){
					this.addComment("!!! Bewachung durch Regionobserver wird scheitern, da keine Waffenversorgung erkannt wurde. (" + countWeapons + "/" + this.getUnit().getModifiedPersons() + ") !!!");
					this.doNotConfirmOrders();
				}
			}
		// Nicht alle Einheiten vertrauenswürdig? Volle Deckung...
		} else {
			if (this.scriptUnit.getUnit().getGuard()==1) {
				this.scriptUnit.addOrder("BEWACHEN NICHT", false);
				this.scriptUnit.addOrder("KÄMPFE FLIEHE", false);
			}
		}
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
}
