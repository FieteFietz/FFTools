package com.fftools.pools.ausbildung;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.scripts.Script;
import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Order;
import magellan.library.StringID;

/**
 * Verwaltet Objekte vom Typ <ttt>Lernplan</ttt>
 * @author Fiete
 *
 */
public class LernplanHandler implements OverlordInfo {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	/**
	 * Die Map der Lernpläne, Key ist der Lernplan-Name -> toLowerCase
	 * Values sind Objekte des Types Lernplan
	 */
	private Hashtable<StringID,Lernplan> lernPlanMap = null; 
	
	public static final String lernplanDetailSeparator = "%"; 
	
	
	/**
	 * Konstruktor
	 *
	 */
	public LernplanHandler(){
		
	}
	
	/**
	 * liefert den Lernplan zu einem Namen (String) oder null, wenn noch nicht
	 * bekannt und allowNew=false, anderenfalls einen neuen Lernplan
	 * 
	 * u: an wen sollen eventuelle Fehlermeldungen ausgegeben werden ?!
	 * 
	 * @param lernPlanName
	 * @return
	 */
	public Lernplan getLernplan(ScriptUnit u,String lernPlanName,boolean allowNew){
		if ((this.lernPlanMap==null || this.lernPlanMap.size()==0) && !allowNew){
			u.addComment("Problem: keine Lernpläne definiert");
			return null;
		}
		
		if (this.lernPlanMap==null && allowNew){
			u.addComment("Hinweis: Lernplan wird neu angelegt:" + lernPlanName);
			this.lernPlanMap = new Hashtable<StringID, Lernplan>();
		}
		
		String lowName = lernPlanName.toLowerCase();
		StringID actStringID = StringID.create(lowName);
		
		Lernplan actLP = this.lernPlanMap.get(actStringID);
		
		if (actLP==null && allowNew){
			actLP = new Lernplan(u,lernPlanName);
			this.lernPlanMap.put(actStringID, actLP);
			u.addComment("Lernplan hinzugefügt: " + lernPlanName);
		}
		return actLP;
	}
	
	/**
	 * Service für die Scripts : parst die Orders einer ScriptUnit komplett
	 * nach Definitionen zum LernPlan
	 * @param u
	 */
	public void parseOrders(ScriptUnit u, Script s){
		if (u==null){
			return;
		}
		
		
		
		// neuen Optionparser
		FFToolsOptionParser OP = new FFToolsOptionParser(u);
		OP.addOptionList(s.getArguments());
		this.processOptionParser(u,OP);
	}
	
	/**
	 * findet passenden Lernplan und übergibt dann dem den OP
	 * wir schleppen u mit für eventuelle parsefehler ->anzeigen, wo sie
	 * auftreten
	 * @param OP Optionparser mit Optionen einer Zeile
	 */
	private void processOptionParser(ScriptUnit u,FFToolsOptionParser OP){
		String name = OP.getOptionString("name");
		if (name.length()<2){
			// name nicht vorhanden oder 1 Zeichen lang -> ablehnen
			outText.addOutLine("!!! LernplanName nicht erkannt! " + u.unitDesc() , true);
			u.doNotConfirmOrders("!!! Lernplanname nicht erkannt");
			return;
		}
		
		Lernplan actLP = this.getLernplan(u,name, true);
		// übergibt an den Lernplan
		actLP.parseOptionLine(u,OP);
	}
	
	
	/**
	 * liefert die Ausbildungsrelation zur Benutzung mit den Ausbildungspool
	 * @param u Die betroffene Scriptunit
	 * @param LernplanName Der Name des Lernplanes, eventuell noch mit % Parameter
	 * @return die fertige Ausbildungsrelation oder null
	 */
	public AusbildungsRelation getAusbildungsrelation(ScriptUnit u,String lernplanName){
		// LernplanNamen checken...
		// wurden abgelegt inkl %
		Lernplan actLP = this.getLernplan(u, lernplanName, false);
		if (actLP==null){
			// nix dazu gefunden....
			outText.addOutLine("!!! Lernplan mit diesem Namen nicht gefunden! " + u.unitDesc() , true);
			u.doNotConfirmOrders("!!! Lernplan mit diesem Namen nicht gefunden! ");
			return null;
		}
		
		AusbildungsRelation retVal = null;
		
		// String masterPlanName = lernplanName;
		String levelString = "";
		// maxLevel herausfinden
		int maxLevel = Lernplan.level_unset;
		if (lernplanName.indexOf(LernplanHandler.lernplanDetailSeparator)>0){
			// hier wird ein bestimmter Level angestrebt
			String[] paare = lernplanName.split(LernplanHandler.lernplanDetailSeparator);
			// masterPlanName = paare[0];
			levelString = paare[1];
			try {
				Integer i = Integer.parseInt(levelString);
				maxLevel = i.intValue();
				if (maxLevel<1){
					// dumm
					outText.addOutLine("!!! Lernplan: gewünschte Stufe fehlerhaft: " + u.unitDesc() , true);
					u.doNotConfirmOrders("!!! Lernplan: gewünschte Stufe fehlerhaft");
					return null;
				}
			} catch (NumberFormatException e){
				// dumm....
				outText.addOutLine("!!! Lernplan: gewünschte Stufe fehlerhaft: " + u.unitDesc() , true);
				u.doNotConfirmOrders("!!! Lernplan: gewünschte Stufe fehlerhaft");
				return null;
			}
		}
		
		// jetzt eigentlich die vorhandenen Level checken, wenn nicht erfüllt,
		// dann dem ersten nicht erfüllten die Lernsettings setzen
		// herausfinden, ob wir überhaupt einen maximalen Level haben.
		int maxPlanLevel = Lernplan.level_unset;
		for (Iterator<Lernplan> iter=this.lernPlanMap.values().iterator();iter.hasNext();){
			Lernplan actLernplan = (Lernplan)iter.next();
			if (actLernplan.getLernPlanLevel()!=Lernplan.level_unset 
					&& actLernplan.getLernPlanLevel()>maxPlanLevel
					&& actLernplan.isSameMasterLernplan(actLP)){
				maxPlanLevel = actLernplan.getLernPlanLevel();
			}
		}
		
		if (maxPlanLevel!=Lernplan.level_unset){
			// OK, wir haben einen maxPlanLevel
			retVal = this.getAusbildungsRelationMultipleLernplan(u, actLP,maxPlanLevel,maxLevel);
		} else {
			// keinen maxPlanLevel...wir gehen davon aus, wir haben nur 
			// einen passenden LernPlan
			retVal = this.getAusbildungsRelationSingleLernplan(u, actLP);
		}
		return retVal;
	}
	
	/**
	 * Liefert AusbRel aus genau einem Lernplan oder null, wenn der nicht mehr greift
	 * @param u
	 * @param LP
	 * @return
	 */
	private AusbildungsRelation getAusbildungsRelationSingleLernplan(ScriptUnit u,Lernplan LP){
		AusbildungsRelation retVal = new AusbildungsRelation(u,null,null);
		if (!LP.appendAusbildungsRelation(u, retVal)){
			// nichts ergänzt worden...keine Lernziele mehr verfügbar!
			retVal=null;
		}
		return retVal;
	}
	
	
	/**
	 * liefert AusbRel aus dem Lernplan, der noch nicht durch die unit erfüllt wird
	 * @param u
	 * @param LP
	 * @param maxPlanLevel
	 * @return
	 */
	private AusbildungsRelation getAusbildungsRelationMultipleLernplan(ScriptUnit u,Lernplan LP,int maxPlanLevel, int userSetMaxPlanLevel){
		String masterName = LP.getName();
		AusbildungsRelation AB = new AusbildungsRelation(u,null,null);
		int posI = masterName.indexOf(LernplanHandler.lernplanDetailSeparator);
		if (posI>0){
			masterName = masterName.substring(0,posI);
		}
		boolean hasMatched = false;
		int actMaxPlanLevel = maxPlanLevel;
		// anpassen, wenn user gesetzt
		if (userSetMaxPlanLevel!=Lernplan.level_unset){
			actMaxPlanLevel = userSetMaxPlanLevel;
		}
		
		
		for (int i = 1;i<=actMaxPlanLevel;i++){
			String neuS = masterName + LernplanHandler.lernplanDetailSeparator + i;
			StringID ID = StringID.create(neuS);
			Lernplan actLP = this.lernPlanMap.get(ID);
			hasMatched = false;
			if (actLP!=null){
				hasMatched = actLP.appendAusbildungsRelation(u, AB);
			}
			if (hasMatched){
				// da hat ein Lernplan zugeschlagen...
				break;
			}
		}
		
		if (!hasMatched && userSetMaxPlanLevel==Lernplan.level_unset){
			// abschliessend nach Lernplan ohne % als Bonus suchen..
			StringID ID = StringID.create(masterName);
			Lernplan actLP = this.lernPlanMap.get(ID);
			if (actLP!=null){
				hasMatched = actLP.appendAusbildungsRelation(u, AB);
				if (hasMatched){
					AB.setActLernplanLevel(Lernplan.level_afterSets);
				}
			}
		}
		
		
		if (!hasMatched){
			// gar nix geliefert...also auch null zurückgeben
			AB = null;
		}
		return AB;
	}
	
	
	/**
	 * wird nicht bei einem bestimmten durchlauf aufgerufen
	 * für OverlordInfo
	 */
	public int[] runAt(){
		return null;
	}
	
	  //inner class //
	  private class LernplanComparator implements Comparator<Lernplan> {
	    public int compare(Lernplan arg0, Lernplan arg1) {
	      return arg0.getName().compareToIgnoreCase(arg1.getName());
	    }
	  }
	
	  
	  /**
	   * Liefert sortierte Liste der bekannten Lernpläne
	   * @return
	   */
	  public ArrayList<Lernplan> getSortedLernPläne(){
		  if (this.lernPlanMap==null || this.lernPlanMap.size()==0){
		     return null;	  
		  }
		  ArrayList<Lernplan> erg = new ArrayList<Lernplan>();
		  erg.addAll(this.lernPlanMap.values());
		  Collections.sort(erg, new LernplanComparator());
		  return erg;  
	  }
	  
	  public void reset(){
		  this.lernPlanMap = null;
	  }
	
	
}
