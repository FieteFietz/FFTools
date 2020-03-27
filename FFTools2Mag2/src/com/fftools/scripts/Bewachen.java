package com.fftools.scripts;

import java.util.ArrayList;

import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Item;
import magellan.library.Order;
import magellan.library.Orders;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

public class Bewachen extends Script{
	
	
	private static final int Durchlauf = 725;
	
	private boolean noWeaponACK = false;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Bewachen() {
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
	
	private void scriptStart(){
		/*
		 * Die Unit setzt den Befehl BEWACHE, wenn folgende Prüfungen alle wahr liefern
		 * - nicht in einem Ozean
		 * - kein NACH oder FAHRE oder ROUTE Befehl || wobei: das könnte gewollt sein. Bei ATTACKIERE + NACH, wenn NACH scheitert, dann BEWACHE
		 * - nicht jetzt schon bereits bewachend
		 * - mehr als 0 Personen in der Einheit
		 * - mindestens 1 Waffentalent und mindestens 1 passende Waffen dazu
		 * - nicht auf FLIEHE stehen
		 * 
		 */
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		this.noWeaponACK = OP.getOptionBoolean("noWeaponACK",false);
		
		// Ozeanprüfung
		Region r = this.getUnit().getRegion();
		if (r == null) {
			this.addComment("Bewache: keine Region erkannt.");
			return;
		}
		
		if (r.getRegionType().isOcean()) {
			this.addComment("Bewache: auf dem Ozean wird nicht bewacht.");
			return;
		}
		
		// kein NACH oder FAHRE Befehl oder ROUTE
		boolean hasBadOrder = false;
		Orders o2 = this.getUnit().getOrders2();
		if (o2 == null) {
			this.addComment("Bewache: keine Befehle erkannt. (!! Befehlsobjekt fehlt)");
			return;
		}
		if (o2.size() == 0) {
			this.addComment("Bewache: keine Befehle erkannt.");
			return;
		}
		
		for (Order order : o2) {
			String text = order.getText();
			text = text.replace("@", "");
			text = text.replace("!", "");
			if (text.toLowerCase().startsWith("fahre")) {
				hasBadOrder=true;
				break;
			}
			if (text.toLowerCase().startsWith("nach")) {
				hasBadOrder=true;
				break;
			}
			if (text.toLowerCase().startsWith("route")) {
				hasBadOrder=true;
				break;
			}
		}
		
		if (hasBadOrder) {
			this.addComment("Bewache: Bewegung erkannt,kein Bewache möglich.");
			return;
		}
		
		// Bewachungsstatus
		if (this.getUnit().getGuard()>0 && this.getUnit().getModifiedGuard()>0) {
			this.addComment("Bewache: Einheit bewacht bereits.");
			return;
		}
		
		if (this.getUnit().getGuard()>0 && this.getUnit().getModifiedGuard()==0) {
			this.doNotConfirmOrders("!!!Bewache: Einheit bewacht bereits, hat aber Befehl, damit aufzuhören!!!");
			return;
		}
		
		// Personen
		if (this.getUnit().getModifiedPersons()==0) {
			this.addComment("Bewache: Einheit hat keine Personen mehr zum Bewachen.");
			return;
		}
		
		if (this.getUnit().getModifiedCombatStatus()>=4 && this.getUnit().getModifiedCombatStatus()!=this.getUnit().getCombatStatus()) {
			this.doNotConfirmOrders("!!!Bewachen: neuer Kampfstatus ist falsch!!!");
			return;
		}
		
		// Talente und Waffen
		// private final String[] talentNamen = {"Hiebwaffen","Stangenwaffen","Bogenschießen","Armbrustschießen","Katapultbedienung"};
		
		class bewTalent {
			public String TalentName = "";
			public String stand = "";
		}
		
		ArrayList<bewTalent> bewTalente = new ArrayList<bewTalent>();
		bewTalent T1 = new bewTalent();
		T1.TalentName = "Hiebwaffen";
		T1.stand = "";
		bewTalente.add(T1);
		
		T1 = new bewTalent();
		T1.TalentName = "Stangenwaffen";
		T1.stand = "";
		bewTalente.add(T1);
		
		T1 = new bewTalent();
		T1.TalentName = "Bogenschießen";
		T1.stand = "HINTEN";
		bewTalente.add(T1);
		
		T1 = new bewTalent();
		T1.TalentName = "Armbrustschießen";
		T1.stand = "HINTEN";
		bewTalente.add(T1);
		
		T1 = new bewTalent();
		T1.TalentName = "Katapultbedienung";
		T1.stand = "HINTEN";
		bewTalente.add(T1);
		
		boolean mayGuard =false;
		String bestStand="";
		for (bewTalent bewTalent : bewTalente) {
			SkillType actST = this.gd_Script.getRules().getSkillType(bewTalent.TalentName, false);
			if (actST == null) {
				this.doNotConfirmOrders("!!!Bewache: SkillType nicht gefunden:" + bewTalent.TalentName);
			} else {
				Skill s = this.getUnit().getSkill(actST);
				if (s!=null) {
					if (s.getLevel()>0) {
						// unit hat ein Kampftalent
						this.addComment("Bewachen: Kampftalent gefunden: " + actST.getName());
						
						for (ItemType iT :  this.gd_Script.getRules().getItemTypes()){
							if (iT.getUseSkill()!=null) {
								if (iT.getUseSkill().getSkillType().equals(actST)) {
									Item ii = this.getUnit().getModifiedItem(iT);
									if (ii != null) {
										if (ii.getAmount()>0) {
											// passt
											 mayGuard =true;
											 bestStand = bewTalent.stand;
											this.addComment("Bewache: passende Waffe gefunden: " + ii.getAmount() + " " + iT.getName());
										} else {
											// this.addComment("Debug - scriptfremd: checke " + iT.getName() + ", Anzahl: " + ii.getAmount());
										}
									} else {
										// this.addComment("Debug - scriptfremd: checke " + iT.getName() + ", nicht vorhanden.");
									}
									
									ii = this.scriptUnit.getModifiedItem(iT);
									if (ii != null) {
										if (ii.getAmount()>0) {
											// passt
											 mayGuard =true;
											 bestStand = bewTalent.stand;
											 this.addComment("Bewache: passende Waffe gefunden: " + ii.getAmount() + " " + iT.getName());
										} else {
											// this.addComment("Debug - inscript: checke " + iT.getName() + ", Anzahl: " + ii.getAmount());
										}
									} else {
										// this.addComment("Debug - inscript: checke " + iT.getName() + ", nicht vorhanden.");
									}
								}
							}
						}
					}
				}
			}
		}
		
		if (!mayGuard) {
			this.addComment("!!!Bewache: Einheit hat keine geeignete Waffe und/oder Talent!!!");
			if (!noWeaponACK) {
				this.doNotConfirmOrders("!!!Bewache:_Einheit daher unbestätigt!!!");
			} else {
				this.addComment("Wegen noWeaponACK=WAHR Einheit deswegen nicht unbestätigt...");
			}
			return;
		}
		
		
		
		if (this.getUnit().getModifiedCombatStatus()==this.getUnit().getCombatStatus() && this.getUnit().getModifiedCombatStatus()>=4) {
			this.addComment("Bewache: befehle neuen Kampfstatus");
			this.addOrder("KÄMPFE " + bestStand + " ;Bewachen", true);
		}
		
		// bis hierhin gekommen - also bewache neu befehlen...
		this.addComment("Bewache: setze Bewachungsbefehl");
		this.addOrder("BEWACHE", false);
		
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
