package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
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
	
	// 20191112, Wunsch Zarox
	// wenn enemy da ist, weiter bewachen
	private boolean GuardIfEnemey = false;
	
	// 20191230 BugFix Zarox, Thoran: keine eigene Waffenanforderung
	private boolean requestWeapon = true;
	
	// 20191230 BugFix Zarox, Thoran: nicht selber lernen
	private boolean lernen = true;
	
	
	
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
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"RegionObserver");
		OP.addOptionList(this.getArguments());
		// Optionen lesen und Prüfen
		
		// Prios
		int prio = OP.getOptionInt("Prio",0);
		if (prio>0) {
			this.WaffenPrio = prio;
		}
		
		WaffenName = OP.getOptionString("Waffe");
		
		this.requestWeapon = OP.getOptionBoolean("Waffenanforderung", this.requestWeapon);
		
		this.lernen = OP.getOptionBoolean("Lernen", this.lernen);
		
		// 1. Lernen nach Plan...
		// this.scriptUnit.findScriptClass("Lernfix", "Lernplan=RegionObserver");
		// this.addComment("Hinweis: das script RegionObserver erwartet einen gleichnamigen Lernplan: RegionObserver");
		
		
		// 2. Geeignete Waffe anfordern (abgekupfert von "Treiben")
		String comment = "RegionObserver-Waffen";
		boolean didSomething = false;
		
		if (this.requestWeapon) {
			
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
					this.doNotConfirmOrders("!!! RegionObserver: angegebene Waffe ist weder ein Gegenstand noch eine Kategorie ?!?");
				}
				
			}
			
			if (!didSomething){
				this.doNotConfirmOrders("Keine Waffenanforderung - kein Talent vorhanden?");
			}
		} else {
			this.addComment("keine Waffenanforderung durch RegionObserver - nicht gewünscht.");
		}
		
		// Amulett bestellen, aber mit niedriger Prio
		MatPoolRequest MPR = new MatPoolRequest(this,1,"Amulett des wahren Sehens",30,"RegionObserver-Amulett");
		this.addMatPoolRequest(MPR);
		this.addComment("RegionObserver (debug): fordere " + MPR.getOriginalGefordert() + " " + MPR.getOriginalGegenstand() + " mit Prio " + MPR.getPrio() + " an.");
		
		
		
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
				alertStatus=1;
				this.doNotConfirmOrders("WARNUNG: Einheit " + actU.toString(true) + " wird nicht vertraut!");
			}
		}

		
		if (this.lernen) {
			// war mal 1., jetzt 4.
			// Feststellung, was und wie gelernt werden soll
			boolean hasLearnOrder = false;
			if (OP.getOptionString("Lernplan")!=""){
				// es wurde ein LernplanName übergeben
				String LP_Name = OP.getOptionString("Lernplan");
				Lernplan LP = this.scriptUnit.getOverlord().getLernplanHandler().getLernplan(this.scriptUnit, LP_Name, false);
				if (LP==null){
					this.doNotConfirmOrders("!!!Lernplan nicht bekannt: " + LP_Name);
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
					this.doNotConfirmOrders("!!!Lerntalent nicht bekannt: " + talent);
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
					this.addComment("Hinweis: wenn ein Lernplan RegionObserver definiert wurde, so wird dieser genutzt.");
					this.doNotConfirmOrders("!!!Lernplan nicht bekannt: RegionObserver");
				} else {
					// alles schön
					this.scriptUnit.findScriptClass("Lernfix", "Lernplan=RegionObserver");
					hasLearnOrder = true;
				}
			}
	
			// Tja, was nun? 
			// Wenn keine Lernorder, dann unbestätigt Lassen und default anbieten
			if (!hasLearnOrder){
				this.doNotConfirmOrders("!!! Dem RegionObserver ist nicht bekannt, was gelernt werden soll!!!");
				this.addOrder("Lernen Wahrnehmung", true);
			}
		
		}
		
		// 5. Further Tasks...
		// TODO oben: Lernplan für "Volks-Waffentalent" ermöglichen (Halbling=Armbrust,
		//		Troll=Hiebwaffen, Insekt=Stangenwaffen, usw.
		
		// sollen wir weiter bewachen ?
		this.GuardIfEnemey = OP.getOptionBoolean("GuardIfEnemy", false);
		
		
	}
	
	
	private void scriptEnde(){
		// Alle gefundenen Einheiten vertrauenswürdig? Falls ja, bewachen und 
		
		int countWeapons = 0;
		if (this.requestWeapon) {
			this.addComment("RegionObserver: prüfe auf Bewaffnung (selbst angefordert):");
			for (MatPoolRequest MPR : this.requests) {
				countWeapons += MPR.getBearbeitet();
				this.addComment("RegionObserver: zähle " + MPR.getBearbeitet() + " " + MPR.getOriginalGegenstand());
			}
		} else {
			// keine eigene Waffenanforderung, prüfen, was wir bekommen
			this.addComment("RegionObserver: prüfe auf Bewaffnung (keine eigene Anforderung):");
			// this.scriptUnit.getModifiedItemsMatPool2()   // HashMap<ItemType, Item> 
			for (ItemType iT: this.scriptUnit.getModifiedItemsMatPool2().keySet()) {
				Item actItem = this.scriptUnit.getModifiedItemsMatPool2().get(iT);
				this.addComment("Prüfe " + actItem.getAmount() + " " + iT.toString() + " (Kategorie: " + iT.getCategory().toString() + ")");
				if ((iT.getCategory().getName().equalsIgnoreCase("Waffen") || iT.getCategory().getName().equalsIgnoreCase("Distanz-Waffen")) && actItem.getAmount()>0) {
					this.addComment("Waffe erkannt, untersuche auf Talente...",true);
					Skill useSkill = iT.getUseSkill();
					if (useSkill!=null) {
						Skill actSkill = this.getUnit().getModifiedSkill(useSkill.getSkillType());
						if (actSkill!=null && actSkill.getLevel()>0) {
							this.addComment("Nutzbare Waffe(n) gefunden: " + actItem.getAmount() + " " + iT.toString());
							countWeapons += actItem.getAmount();
						} else {
							this.addComment("Waffe(n) nicht nutzbar: " + actItem.getAmount() + " " + iT.toString());
						}
					} else {
						this.doNotConfirmOrders("!!! Probelm: für Waffe " + iT.toString() + " ist kein Talent zur Nutzung bekannt!!!");
					}
				} 
			}
		}
		
		if (countWeapons>this.getUnit().getModifiedPersons()) {
			this.addComment("Hinweis: es sind mehr Waffen als benötigt bei dieser Einheit !!!");
		}
		if (countWeapons==0) {
			this.addComment("Hinweis: der RegionObserver hat keine Waffe zum Bewachen...");
		}
		
		if (alertStatus==0) {
			// Falls die Einheit noch nicht bewacht, setze Befehle
			if (this.scriptUnit.getUnit().getGuard()==0) {
				
				// Aber dazu muss eine Waffe vorhanden sein!
				// erhaltene Waffen zählen
				if (countWeapons > 0){
					// reicht
					this.scriptUnit.addOrder("BEWACHEN ;RegionObserver ohne Feind. Waffen: " + countWeapons, false);
					if (!this.GuardIfEnemey) {
						this.scriptUnit.addOrder("KÄMPFE NICHT ;RegionObserver", false);
					}
					if (this.scriptUnit.getUnit().getCombatStatus()==EresseaConstants.CS_FLEE) {
						this.doNotConfirmOrders("!!! RegionObserver: Kampfstatus ist FLIEHE, bitte prüfen!!!");
					}
				} else {
					// reicht nicht
					// jammern
					this.addComment("!!! Regionobserver kann nicht bewachen, da keine Waffenversorgung erkannt wurde.");
				}
			} else {
				// wir bewachen schon, wenn nun nicht genügend waffen, warnen, dass bewachung aufgelöst werden wird
				if (countWeapons <= 0){
					this.doNotConfirmOrders("!!! Bewachung durch Regionobserver wird scheitern, da keine Waffenversorgung erkannt wurde. !!!");
				}
			}
		// Nicht alle Einheiten vertrauenswürdig? Volle Deckung...
		} else {
			
			if (this.scriptUnit.getUnit().getGuard()>0) {
				// wir bewachen bereits
				if (!this.GuardIfEnemey) {
					this.scriptUnit.addOrder("BEWACHEN NICHT", false);
					this.scriptUnit.addOrder("KÄMPFE FLIEHE ;RegionObserver", false);
				} else {
					this.addComment("RegionObserver: Obwohl mir mulmig ist, werde ich nicht weichen! (Option GuardIfEnemy ist aktiv)");
					
				}
			} else {
				// wir bewachen noch nicht
				if (this.GuardIfEnemey) {
					if (countWeapons>0) {
						this.scriptUnit.addOrder("BEWACHEN ;RegionObserver trotz Feind, Waffen: " + countWeapons, false);
					} else {
						this.scriptUnit.addComment("RegionObserver: Hätte ich auch nur eine Waffe, dann würde ich das Bewachen jetzt beginnen.");;
					}
					if (this.scriptUnit.getUnit().getCombatStatus()==EresseaConstants.CS_FLEE) {
						this.doNotConfirmOrders("!!! RegionObserver: Kampfstatus ist FLIEHE, bitte prüfen!!!");
					}
				}
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
