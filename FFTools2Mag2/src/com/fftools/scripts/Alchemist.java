package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import magellan.library.Item;
import magellan.library.Potion;
import magellan.library.Skill;
import magellan.library.StringID;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;



/**
 * Alles rund um den Alchemisten
 * dient lediglich zum Anmelden beim Pool
 * wenn KrautDepot 
 * @author Fiete
 *
 */
public class Alchemist extends MatPoolScript{
	
	
	private static final int Durchlauf = 106;
	
	private final int DEFAULT_KRAUT_PRIO=800;
	private final int DEFAULT_MIN_AUSLASTUNG=70;
	private final int DEFAULT_ZUBEHOER_PRIO = 800;
	
	
	/**
	 * funguiert die einheit als Krautdepot?
	 */
	private boolean krautDepot = false;
	
	
	/**
	 * wenn ausschliesslich KrautDepot->machen=false
	 * kein einsatz als alchemist
	 */
	private boolean machen = true;
	
	/**
	 * Prio zum Anfordern der Kräuter im KrautDepot
	 */
	private int krautPrio = DEFAULT_KRAUT_PRIO;
	
	private int zubehoerPrio = DEFAULT_ZUBEHOER_PRIO;
	
	/**
	 * legt fest, ob der alchi vom manager bereits einen brauauftrag bekommen hat
	 */
	private boolean brauend = false;
	
	/**
	 * Alchemie-Talentstufe des Alchemisten
	 */
	private int talentLevel=0;
	
	/**
	 * verfügbare Talentpunkte
	 */
	private int talentPunkte=0;
	
	/**
	 * Mindestauslastung in Talentpunkten gerechnet
	 * Interpretation als Prozentwert
	 */
	private int minAuslastung = DEFAULT_MIN_AUSLASTUNG;
	
	
	/**
	 * reiner Merker beim suchen eines Alchis für einen Trank
	 */
	public double helpAuslastung=0;
	
	/**
	 * kann gesetzt werden, dann wird diese Group statt allen Kräutern requestet
	 */
	private String itemGroup = "";
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Alchemist() {
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
		
		super.addVersionInfo();
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Alchemist");
		this.krautDepot = OP.getOptionBoolean("krautdepot", false);
		this.itemGroup = OP.getOptionString("itemGroup");
		this.machen = OP.getOptionBoolean("machen", true);
		this.krautPrio = OP.getOptionInt("prio", DEFAULT_KRAUT_PRIO);
		this.minAuslastung = OP.getOptionInt("minAuslastung", DEFAULT_MIN_AUSLASTUNG);
		this.zubehoerPrio = OP.getOptionInt("zubehoerPrio",DEFAULT_ZUBEHOER_PRIO);
		
		this.talentPunkte = this.getSchaffenspunkte();
		
		if (this.krautDepot){
			this.getOverlord().getAlchemistManager().addKrautDepot(this);
			this.requestAllKraut();
		}
		
		if (this.machen){
			this.getOverlord().getAlchemistManager().addAlchemist(this);
			// Tränke, Ringe...
			this.getZubehoer();
		}
		
		
		
		

	}
	
	/**
	 * durchläuft alle bekannten Tränke und fordert entsprechend ItemTypes an
	 *
	 */
	@SuppressWarnings("deprecation")
	private void requestAllKraut(){
		ArrayList<ItemType> itemTypes = new ArrayList<ItemType>();
		if (this.gd_Script.potions()==null || this.gd_Script.potions().size()==0){
			return;
		}
		if (this.itemGroup==""){
			for (Iterator<Potion> iter = this.gd_Script.potions().values().iterator();iter.hasNext();){
				Potion actPotion = (Potion)iter.next();
				// Zutaten durchforsten
				Collection<Item> list = actPotion.ingredients();
				if (list!=null && list.size()>0){
					for (Iterator<Item> iter2 = list.iterator();iter2.hasNext();){
						Item actItem = (Item) iter2.next();
						ItemType actItemType = actItem.getItemType();
						if (actItemType!=null && !itemTypes.contains(actItemType)){
							itemTypes.add(actItemType);
						}
					}
				}
			}
			for (Iterator<ItemType> iter = itemTypes.iterator();iter.hasNext();){
				ItemType actItemType = (ItemType)iter.next();
				MatPoolRequest MPR = new MatPoolRequest(this,Integer.MAX_VALUE,actItemType.getName(),this.krautPrio,"KrautDepot");
				this.addMatPoolRequest(MPR);
			}
		} else {
			MatPoolRequest MPR = new MatPoolRequest(this,Integer.MAX_VALUE,this.itemGroup,this.krautPrio,"KrautDepot");
			this.addMatPoolRequest(MPR);
		}
	}
	
	/**
	 * versucht, zubehör zu organisieren (Schaffenstrunk, Gehirnschmalz, Ring der Flinken Finger)
	 *
	 */
	private void getZubehoer(){
		this.getZubehoerDetail("Schaffenstrunk", 1);
		this.getZubehoerDetail("Gehirnschmalz", 1);
		// this.getZubehoerDetail("Ring der flinken Finger", 1);
	}
	
	/**
	 * Organisiert spezielles Zubehör
	 * @param Name
	 * @param Anzahl
	 */
	public void getZubehoerDetail(String Name,int Anzahl){
		ItemType testType = this.region().getData().rules.getItemType(Name,false);
		if (testType!=null){
			MatPoolRequest MPR = new MatPoolRequest(this,Anzahl,Name,this.zubehoerPrio + this.talentLevel,"Zubehoer");
			this.addMatPoolRequest(MPR);
		}
	}
	
	
	/**
	 * liefert die schaffenspunkte dieses alchies
	 * dabei werden schaffenstrunk und benutze schaffenstrunk berücksichtigt
	 * ToCheck Ring der Flinken Finger
	 * @return
	 */
	private int getSchaffenspunkte(){
		int erg = 0;
		// normal: Personenanzahl * Talentpunkte
		SkillType skillType = this.gd_Script.rules.getSkillType(StringID.create("Alchemie"));
		int actTalentLevel = 0;
		Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
		if (skill!=null) {
			actTalentLevel = skill.getLevel();
		}
		
		this.talentLevel = actTalentLevel;
		
		int persAnzahl = this.scriptUnit.getUnit().getModifiedPersons();
		
		erg = persAnzahl * actTalentLevel;
		
		// Schaffenstrunk?
		if (FFToolsGameData.hasSchaffenstrunkEffekt(this.scriptUnit,true)){
			erg = erg * 2;
			scriptUnit.addComment("Trankeffekt berücksichtigt");
		}
		
		// RdF ?
		if (FFToolsGameData.hasRdfFEffekt(this.scriptUnit)){
			erg = erg * 2;
			scriptUnit.addComment("RdfF berücksichtigt");
		}

		return erg;
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}


	/**
	 * @return the brauend
	 */
	public boolean isBrauend() {
		return brauend;
	}


	/**
	 * @param brauend the brauend to set
	 */
	public void setBrauend(boolean brauend) {
		this.brauend = brauend;
	}


	/**
	 * @return the talentLevel
	 */
	public int getTalentLevel() {
		return talentLevel;
	}


	/**
	 * @return the talentPunkte
	 */
	public int getTalentPunkte() {
		return talentPunkte;
	}


	/**
	 * @return the minAuslastung
	 */
	public int getMinAuslastung() {
		return minAuslastung;
	}


	/**
	 * @return the krautPrio
	 */
	public int getKrautPrio() {
		return krautPrio;
	}
	
}
