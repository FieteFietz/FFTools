package com.fftools.scripts;

import java.util.ArrayList;

import magellan.library.Building;
import magellan.library.rules.SkillType;

import com.fftools.pools.akademie.AkademieManager;
import com.fftools.pools.akademie.AkademiePool;
import com.fftools.pools.akademie.AkademieTalent;
import com.fftools.utils.FFToolsRegions;

/**
 * Entscheidet, dass die Akademie, deren Besitzer diese Einheit ist
 * sich automatisch Nutzer aus dem Pool sucht
 * 
 * @author Fiete
 *
 */

public class Akademie extends Script{
	
	
	private static final int Durchlauf = 662;
	
	private AkademiePool AP=null;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Akademie() {
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
		
		
		// Gibt es überhaupt Argumente?
		if (super.getArgCount() > 0) {
            // gleich die übergebenen Talente parsen
			ArrayList<AkademieTalent> akademieTalente = new ArrayList<AkademieTalent>();
			for (String s:this.getArguments()){
				if (s.toLowerCase().startsWith("aka")){
					// das ist die aka-zeile
					String[] ss = s.split("=");
					if (ss.length!=2){
						this.addComment("!!! Fehler. Argument " + s + " hat nicht die Form aka=ID, es fehlt das =");
					} else {
						Building b = FFToolsRegions.getBuilding(this.region(), ss[1]);
						if (b==null){
							this.addComment("!!!aka ist kein Gebäude! (" + ss[1]+ ")");
							return;
						}
						if (!b.getBuildingType().toString().equalsIgnoreCase("Akademie")){
							this.addComment("!!!aka ist keine Akademie! (" + ss[1] + ")");
							return;
						}
						AkademieManager AM = this.getOverlord().getAkademieManager();
						AkademiePool _AP = AM.addAkademie(b,this);
						if (_AP == null){
							// Problem, Pool bereits vorhanden
							addComment("!!! Akademiepool " + b.getID().toString() + " konnte nicht angelegt werden, Besitzerwechsel??");
							doNotConfirmOrders();
						} else {
							// kein Problem
							addComment("AkaTalenten " + b.getID().toString() + ": die Aka wurde erfolgreich dem AkaManger bekannt gemacht.");
							this.AP = _AP;
						}
					}
				} else {
					// s hat die Form TalentName:Anzahl
					String[] ss = s.split(":");
					if (ss.length!=2){
						this.addComment("!!! Fehler. Argument " + s + " hat nicht die Form Talent:Anzahl, es fehlt das :",false);
					} else {
						String talentName = ss[0];
						String talentAnzahl = ss[1];
						SkillType sK = null;
						// SkillType festStellen
						if (talentName.equalsIgnoreCase("draig")||talentName.equalsIgnoreCase("illaun")||talentName.equalsIgnoreCase("tybied")||talentName.equalsIgnoreCase("gwyrrd")||talentName.equalsIgnoreCase("cerddor")){
							// !! Tada MagieTalent
							// entsprechenden SkillType generieren!
							sK=this.scriptUnit.getScriptMain().gd_ScriptMain.rules.getSkillType(talentName.toLowerCase(), true);
						} else {
							// keine Magiename, also muss Talent vorhanden sein
							sK = this.gd_Script.rules.getSkillType(talentName);
						}
						if (sK==null){
							this.addComment("!!! Akademie: Talent nicht erkannt: " + talentName,false);
						} else {
							// wir haben ein gültiges Talent...haben wir auch ne Anzahl
							int Anz = Integer.parseInt(talentAnzahl);
							if (Anz<=0 || Anz>25){
								this.addComment("!!! Akademie: Anzahl nicht erkannt in: " + s + " (" + talentAnzahl + ")",false);
							} else {
								// hier stimmt Alles
								AkademieTalent AT = new AkademieTalent(sK,Anz);
								akademieTalente.add(AT);
							}
						}
					}
				}
			}
			this.addComment("Akademie: " + akademieTalente.size() + " Talentangaben gelesen",false);
			if (this.AP!=null){
				this.AP.setAkademieTalente(akademieTalente);
			} else {
				this.addComment("(Akademie wurden wegen nicht erkannter Akademie nicht übergeben)",false);
			}
		}	
	
	 }
	
	/**
	 * mehrere sind OK
	 */
	public boolean allowMultipleScripts(){
		return true;
	}
	
	
}