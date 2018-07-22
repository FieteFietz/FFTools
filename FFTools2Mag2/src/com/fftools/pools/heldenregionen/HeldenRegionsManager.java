package com.fftools.pools.heldenregionen;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Faction;
import magellan.library.Skill;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.rules.SkillType;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Heldenregion;
import com.fftools.scripts.Lernfix;


/**
 * eigentliche Aufgabe: stellt sicher, dass für jede Partei nur 
 * 1 Heldenregion existiert
 * @author Fiete
 * 
 * Mögliche erweiterung: mehrere Regionen pro Partei, die nacheinander
 * (nach prio) abgearbeitet werden, je nachdem, wo:
 * - die besten Einheiten sind
 * - ausreichend Silber vorhanden ist
 *
 */
public class HeldenRegionsManager implements OverlordRun,OverlordInfo{
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static final int Durchlauf1 = 180;
	private static final int Durchlauf2 = 230;
	
	
	private int[] runners = {Durchlauf1,Durchlauf2};
	
	private ScriptMain scriptMain = null;
	
	// merken wir uns zu jeder Faction doch einfach die Region
	private Hashtable<Faction, Heldenregion> heldenScripte = null;
	

	

	
	public HeldenRegionsManager (ScriptMain _scriptMain){
		this.scriptMain = _scriptMain;
	}
	
	
	
	public void informUs(){
		
	}
	
	
	/**
	 * Versucht, fehlende Helden der Partei in der Heldenregion zu befördern
	 *
	 */
	
	public void run(int durchlauf){
		if (this.heldenScripte==null || this.heldenScripte.size()==0){
			// nix zu tun
			return;
		}
		
		switch (durchlauf){
			case HeldenRegionsManager.Durchlauf1: this.runVorMatPool();break;
			case HeldenRegionsManager.Durchlauf2: this.runNachMatPool();break;
		}
		
	}
	
	/**
	 * stellt bedarf fest und requestet
	 */
	private void runVorMatPool(){
		for (Iterator<Heldenregion> iter=this.heldenScripte.values().iterator();iter.hasNext();){
			Heldenregion actHeldenregion = (Heldenregion)iter.next();
			// anzahl Helden feststellen
			Faction actFaction = actHeldenregion.getFaction();
			int Bedarf = actFaction.getMaxHeroes()-actFaction.getHeroes();
			if (actFaction.getMaxHeroes()>0 && Bedarf>0){
				this.processRegionVorher(actHeldenregion, Bedarf);
			}
		}
	}
	
	
	/**
	 * wenn bedarf vorhanden und silber zugeordnet, dann befördern
	 */
	private void runNachMatPool(){
		for (Iterator<Heldenregion> iter=this.heldenScripte.values().iterator();iter.hasNext();){
			Heldenregion actHeldenregion = (Heldenregion)iter.next();
			this.processRegionNachher(actHeldenregion);
		}
	}
	
	/**
	 * Sortiert die units und legt mögliche Helden fest
	 * Zählt personen der oartei
	 * requested entsprechend silber
	 * @param heldenregion
	 * @param Anzahl
	 */
	private void processRegionVorher(Heldenregion heldenregion, int Anzahl){
		// Liste alle Units der faction in der Region
		// die die eventuelle Bedingung des Lernplanes erfüllen
		ArrayList<ScriptUnit> factionUnits = new ArrayList<ScriptUnit>();
		// Region actRegion = heldenregion.scriptUnit.getUnit().getRegion();
		// alle scriptunits der region durchlaufen
		for (Iterator<ScriptUnit> iter=this.scriptMain.getScriptUnits(heldenregion.scriptUnit.getUnit().getRegion()).values().iterator();iter.hasNext();){
			ScriptUnit actScriptUnit=(ScriptUnit)iter.next();
			boolean toAdd = false;
			if (actScriptUnit.getUnit().getFaction().equals(heldenregion.getFaction()) && actScriptUnit.getUnit().getModifiedPersons()>0 && !actScriptUnit.getUnit().isHero()){
				// richtige faction und hat Personen
				// Lernplanbedingung
				if (heldenregion.getLernplanListe()!=null && heldenregion.getLernplanListe().size()>0){
					Object o = actScriptUnit.getScript(Lernfix.class);
					if (o==null){
						// Lernplanbedingung angegeben aber kein lernfix...hm
						
					} else {
						// Lernfix gefunden
						Lernfix lernFix = (Lernfix)o;
						String actLernplanName = lernFix.getLernplanName();
						if (actLernplanName!=null && actLernplanName.length()>0){
							// Lernfix kennt einen LernplanNamen
							for (Iterator<String> iter2 = heldenregion.getLernplanListe().iterator();iter2.hasNext();){
								String searchLernplanName = (String)iter2.next();
								if (searchLernplanName.equalsIgnoreCase(actLernplanName)){
									// Treffer
									toAdd=true;
									break;
								}
							}
						}
					}
				} else {
					// keine Lernplanbedingungen bekannt
					// pauschal hinzu
					toAdd=true;
				}	
			}
			if (toAdd){
				factionUnits.add(actScriptUnit);
			}
		}
		
		if (factionUnits.size()>0){
			// sortieren
			// neuen comparator
			HeldenScriptUnitsComparator comp = new HeldenScriptUnitsComparator(heldenregion);
			Collections.sort(factionUnits,comp);
		} else {
			return;
		}
		
		int numberPersons = 0;
		// Anzahl von Personen dieser Faction ermitteln
		for (Unit actUnit:this.scriptMain.gd_ScriptMain.getUnits()){
			if (actUnit.getFaction().equals(heldenregion.getFaction())){
				numberPersons += actUnit.getModifiedPersons();
			}
		}
		for (@SuppressWarnings("deprecation")
		Iterator<TempUnit> iter=this.scriptMain.gd_ScriptMain.tempUnits().values().iterator();iter.hasNext();){
			Unit actUnit = (Unit)iter.next();
			if (actUnit.getFaction().equals(heldenregion.getFaction())){
				numberPersons += actUnit.getModifiedPersons();
			}
		}
		
		
		heldenregion.addComment(Anzahl + " neue Helden bei " + numberPersons + " Personen in " + heldenregion.getFaction());
		
		
		int maxLevel = 0;
		for (Iterator<ScriptUnit> iter=factionUnits.iterator();iter.hasNext();){
			ScriptUnit actScriptUnit = (ScriptUnit)iter.next();
			if (actScriptUnit.getUnit().getModifiedPersons()<=Anzahl){
				int actMaxLevel=this.getMaxTalent(actScriptUnit, heldenregion);
				if (actMaxLevel>=maxLevel){
					// vor ort erledigung im script:
					heldenregion.addHeld(actScriptUnit, numberPersons);
					Anzahl -= actScriptUnit.getUnit().getModifiedPersons();
					maxLevel = actMaxLevel;
				}
			}
			if (Anzahl<=0){
				break;
			}
		}
		
		heldenregion.addComment(Anzahl + " Helden noch offen in " + heldenregion.getFaction() + " (geforderte TalStufe:" + maxLevel + ")");
	}
	
	
	
	/**
	 * nach dem Matpoollauf
	 * heldenregion-script auffordern, seine Helden zu untersuchen
	 * @param heldenregion
	 */
	private void processRegionNachher(Heldenregion heldenregion){
		int erg = heldenregion.checkHelden();
		heldenregion.addComment("Beförderungen für " + heldenregion.getFaction() + ": " + erg);
	}
	
	
	/**
	 * fügt ein script Heldenregion zum Manager hinzu
	 * @param heldenRegion
	 * @return
	 */
	public boolean addHeldenregion(Heldenregion heldenRegion){
		// herausfinden, ob Partei bereits heldenregion besitzt
		if (heldenRegion.getFaction()==null){
			return false;
		}
		
		if (this.heldenScripte!=null){
			Heldenregion actHR = this.heldenScripte.get(heldenRegion.getFaction());
			if (!(actHR==null)){
				// schon vorhanden
				heldenRegion.addComment("Für Partei " + heldenRegion.getFaction() + " bereits Heldenregion festgelegt: " + actHR.scriptUnit.getUnit().getRegion().toString());
				outText.addOutLine("!!! Weitere Heldenregion für Partei " + heldenRegion.getFaction() + ". Ignoriert: " + actHR.scriptUnit.getUnit().getRegion().toString(), true);
				return false;
			}
		}
		// ok...alles OK
		// hinzufügen
		if (this.heldenScripte==null){
			this.heldenScripte = new Hashtable<Faction, Heldenregion>();
		}
		this.heldenScripte.put(heldenRegion.getFaction(), heldenRegion);
		return true;
	}
	
	
	
	public int[] runAt(){
		return runners;
	}
	
	private int getMaxTalent(ScriptUnit u, Heldenregion heldenRegion){
		int erg = 0;
		for (Iterator<SkillType> iter=heldenRegion.getTalentSkillTypeListe().iterator();iter.hasNext();){
			SkillType actSkillType = (SkillType)iter.next();
			Skill actSkill = u.getUnit().getModifiedSkill(actSkillType);
			int actLevel = 0;
			if (actSkill!=null){
				actLevel = actSkill.getLevel();
				if (actLevel>erg){
					erg = actLevel;
				}
			}
		}
		return erg;
	}
	
	
}
