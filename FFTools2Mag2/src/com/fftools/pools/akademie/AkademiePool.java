package com.fftools.pools.akademie;

import java.util.ArrayList;
import java.util.Collections;

import magellan.library.Building;
import magellan.library.Unit;

import com.fftools.ReportSettings;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.scripts.Akademie;


public class AkademiePool {

	  
	
	// private static final OutTextClass outText = OutTextClass.getInstance();
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private Building akademieBuilding;
	private Akademie verwalterScript;
	
	private ArrayList<AusbildungsRelation> relevantRelations;
	private ArrayList<AkademieTalent> akademieTalente;
	
	/**
	 * Konstruktor - setzt das Gebäude
	 *
	 */
	
	public AkademiePool(Building akademie, Akademie AT){
		this.akademieBuilding = akademie;
		this.verwalterScript = AT;
    }
	
	
	/**
     * Hier rennt der Pool. 
     */
	
	public void runPool(){
		
		this.verwalterScript.addComment("Akademie Pool start für: " + this.akademieBuilding.getID().toString() + ")");
		
		// die Liste der relevanten ARs besorgen
		this.relevantRelations = reportSettings.getScriptMain().getOverlord().getAkademieManager().getRelevantAR(this.akademieBuilding.getRegion());
		if (this.relevantRelations==null || this.relevantRelations.isEmpty()){
			this.verwalterScript.addComment("Akademieverwaltung: keine relevanten Ausbildungsteilnehmer mehr in der Region");
			return;
		}
		
		this.verwalterScript.addComment("Debug: Anzahl relavanter Lernfixer (alle Talente):" + this.relevantRelations.size(),false);
		
		if (this.akademieTalente==null || this.akademieTalente.isEmpty()){
			this.akademieTalente = verwalterScript.getOverlord().getAkademieManager().getDefaultTalentList();
		} else {
			// die defaults kurzerhand anfügen, wenn noch nicht vorhanden
			for (AkademieTalent AT:verwalterScript.getOverlord().getAkademieManager().getDefaultTalentList()){
				if (!this.akademieTalente.contains(AT)){
					this.akademieTalente.add(AT);
				}
			}
				
		}
		
		if (this.akademieTalente==null || this.akademieTalente.isEmpty()){
			this.verwalterScript.addComment("Akademieverwaltung: keine relevanten Talente bekannt - nanu?");
			return;
		}
		
		// OK, wir haben units und wir habe Talente...
		// wieviele Plätze haben wir zu vergeben?
		int verfPlätze = 25;
		for (Unit u:this.akademieBuilding.modifiedUnits()){
			// diese Unit wird nur dann von den verfügbaren abgezogen
			// wenn sie nicht Teil der relevantenARs ist
			boolean isRelevant = false;
			for (AusbildungsRelation AR:this.relevantRelations){
				if (AR.getScriptUnit().getUnit().equals(u)){
					// bingo
					isRelevant=true;
					break;
				}
			}
			if (!isRelevant){
				verfPlätze-=u.getModifiedPersons();
				this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + "- ignoriere Einheit " + u.toString() + " (kein Lernfix...)");
			} else {
				// ist relevant
				this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + " - Insasse wird gepoolt " + u.toString());
			}
		}
		this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": verfügbare Plätze:" + verfPlätze);
		if (verfPlätze<=0){
			this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": abbruch, nix zu tun und voll...");
			return;
		}
		
		int übergabePlätze = 0;
		
		// alle Talente durchgehen
		for (AkademieTalent AT:this.akademieTalente){
			// die relevanten Sortieren, nach dem aktuellen SkillType...
			// Submenge der relevanten Bilden, mit dem richtigen
			// Skilltype und der maximalen Anzahl
			int übernommeneÜbergabeplätze = übergabePlätze;
			übergabePlätze += AT.getAnzahl();
			this.verwalterScript.addComment("Debug " + this.akademieBuilding.getID().toString() + ": poole Talent:" + AT.getSkillType().toString() + " definiertes Limit: " + übergabePlätze + ", davon " + übernommeneÜbergabeplätze + " bereits übernommen.",false);
			ArrayList<AusbildungsRelation> actRel = new ArrayList<AusbildungsRelation>();
			for (AusbildungsRelation AR:this.relevantRelations){
				if (AR.getOrderedSkillType()==null){
					AR.getScriptUnit().doNotConfirmOrders("!!!Unbekanntes Talent!!!");
				} else {
					if (AR.getAkademieFromAM()==null && AR.getOrderedSkillType().equals(AT.getSkillType())){
						if (!AR.isAvoidAka() && !AR.isTeacher()){
							actRel.add(AR);
						}
						
					}
				}
			}
			
			this.verwalterScript.addComment("Debug " + this.akademieBuilding.getID().toString() + ": Anzahl der reduzierten relevanten Lernfixer: " + actRel.size(),false);
			
			// fertige Liste...>0 ?
			if (actRel.isEmpty()){
				// nächstes Talent
				continue;
			}
			// Liste sortieren
			Collections.sort(actRel, new AusbildungsRelationComparator(this.akademieBuilding));
			// stehen jetzt aber falsch rum
			Collections.reverse(actRel);
			// Liste abarbeiten
			for (AusbildungsRelation AR:actRel){
				boolean passtNoch = false;
				// passen wir hier noch rein?
				if (AR.getScriptUnit().getUnit().getModifiedPersons()<=verfPlätze && AR.getScriptUnit().getUnit().getModifiedPersons()<=übergabePlätze){
					// richtiger Type und passige Einheit
					passtNoch = true;
				}
				if (passtNoch){
					// Gebäude setzen
					AR.setAkademieFromAM(this.akademieBuilding);
					// verf reduzieren
					verfPlätze -= AR.getSchuelerPlaetze();
					übergabePlätze -= AR.getSchuelerPlaetze();
					
					// info
					this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": IN für " + AT.getSkillType().toString() + ": " + AR.getScriptUnit().getUnit().toString(true) + " (" + verfPlätze + " verbleibend)" );
					AR.getScriptUnit().addComment("(AkaPool definiert bei: " + this.verwalterScript.scriptUnit.toString() + ")(1)");
				}
				// was passiert, wenn verfPl = 0 ?!
				if (übergabePlätze<=0){
					this.verwalterScript.addComment("Debug: definiertes Limit erreicht.",false);
					// fertig
					break;
				}
				// was passiert, wenn verfPl = 0 ?!
				if (verfPlätze<=0){
					// fertig
					break;
				}
			}
			if (verfPlätze<=0){
				// fertig
				break;
			}
		}
		this.verwalterScript.addComment("AkaPool " + this.akademieBuilding.getID().toString() + " final Statement: noch " + verfPlätze + " Plätze frei.");
	}
	
	/**
	 * schmeisst aus dieser Akademie alle raus, die nicht bleiben dürfen
	 */
	public void leaveAkademie(){
		for (Unit u:this.akademieBuilding.modifiedUnits()){
			// relevant = gepoolt und darf bleiben
			boolean isRelevant = false;
			for (AusbildungsRelation AR:this.relevantRelations){
				if (AR.getScriptUnit().getUnit().equals(u)){
					// bingo
					if (AR.getAkademieFromAM()!=null && AR.getAkademieFromAM().equals(this.akademieBuilding)){
						isRelevant=true;
					}
					if (isRelevant){
						AR.getScriptUnit().addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": Einheit darf in " + this.akademieBuilding.toString() + " bleiben.");
					} else {
						AR.getScriptUnit().addComment("AkaPool " + this.akademieBuilding.getID().toString() + ": Einheit muss " + this.akademieBuilding.toString() + " verlassen.");
						if (AR.getAkademieFromAM()==null){
							AR.getScriptUnit().addOrder("verlassen ; Akapool",true);
						}
					}
					AR.getScriptUnit().addComment("(AkaPool definiert bei: " + this.verwalterScript.scriptUnit.toString() + ")(2)");
					break;
				}
			}			
		}
	}
	

	/**
	 * @return the akademieBuilding
	 */
	public Building getAkademieBuilding() {
		return akademieBuilding;
	}


	/**
	 * @return the verwalterScript
	 */
	public Akademie getVerwalterScript() {
		return verwalterScript;
	}


	/**
	 * @param akademieTalente the akademieTalente to set
	 */
	public void setAkademieTalente(ArrayList<AkademieTalent> akademieTalente) {
		this.akademieTalente = akademieTalente;
	}
	
	
	
}
