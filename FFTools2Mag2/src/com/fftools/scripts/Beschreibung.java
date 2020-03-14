package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import magellan.library.Item;
import magellan.library.Skill;
import magellan.library.rules.ItemType;

import com.fftools.utils.FFToolsOptionParser;


/**
 * Sehr simpel zum setzen von Einheiteninfos in 
 * die offizielle einheitenbeschreibung
 * @author Fiete
 *
 */

public class Beschreibung extends Script{
	
	/**
	 * Rundenstempel an den Anfang?
	 */
	private boolean showRunde=false;
	
	/**
	 * sollen die Talente mit in die Beschreibung ?
	 */
	private boolean showTalente=false;
	
	
	private static final String separator = ";";
	private static final String detailSeparator = ",";
	
	private static final int Durchlauf = 862;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Beschreibung() {
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
		// Optionen Parsen
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Beschreibung");
		OP.addOptionList(this.getArguments());
		
		this.showTalente = OP.getOptionBoolean("Talente", this.showTalente);
		this.showRunde = OP.getOptionBoolean("Runde", this.showRunde);
		
		// optionen drinne
		String neueBeschreibung = "";
		
		// Runde
		if (this.showRunde){neueBeschreibung = this.addRunde(neueBeschreibung);}
		// Talente
		if (this.showTalente) {neueBeschreibung = this.addTalente(neueBeschreibung);}
		
		
		// Items?
		ArrayList<String> itemList = OP.getOptionStringList("ware");
		if (itemList!=null && itemList.size()>0){
			neueBeschreibung = this.addItems(itemList,neueBeschreibung);
		}
		
		// text ?
		String s = OP.getOptionString("text");
		if (s.length()>1){
			// richtigen Text holen?
			String optionText = reportSettings.getOptionString(s);
			if (optionText.length()>1){
				this.addComment("Beschreibung: Text übernommen aus Optionen: " + s);
				s = optionText;
			}
			
			
			// Replacer?
			this.addComment("Beschreibung - Text! erkannt");
			String s_work = s.toLowerCase();
			// authcode
			if (s_work.indexOf("$authcode$")>0){
				String code = reportSettings.getOptionString("authcode");
				if (code == null || code.length()<=0){
					code="error";
				}
				s = s.replace("$authcode$", code);
			}
			
			// Runde
			if (s_work.indexOf("$runde$")>0){
				int code = this.gd_Script.getDate().getDate() + 1;
				s = s.replace("$runde$", code + "");
			}
			
			// Leerzeichen
			s = s.replace("_", " ");
			
			// festsetzen
			if (neueBeschreibung.length()>0){
				neueBeschreibung = neueBeschreibung.concat(";");
			}
			
			neueBeschreibung = neueBeschreibung.concat(s);
			
		} else {
			if (this.showRunde || this.showTalente){
				this.addComment("kein weiterer Text zur Beschreibung erkannt.");
			} else {
				this.doNotConfirmOrders("!!! Beschreibung - kein Text?");
			}
		}
		
		
		
		// setzen
		if (neueBeschreibung.length()>1){
			if (neueBeschreibung.length()>400){
				this.addComment("Beschreibung zu lang: " + neueBeschreibung.length() + " Zeichen (max 400).");
				neueBeschreibung = neueBeschreibung.substring(0, 399);
			}	
			this.addOrder("BESCHREIBE EINHEIT \"" + neueBeschreibung + "\"", true);
		} else {
			this.addComment("Beschreibung: nix zum Beschreiben");
		}
	}
	
	/**
	 * ergänzt Rundeninfo
	 * @param work
	 */
	private String addRunde(String work){
		String erg = "";
		int actRunde = this.gd_Script.getDate().getDate();
		if (actRunde>0){
			erg = "Stand Runde " + actRunde;
		}
		if (erg.length()>1){
			if (work.length()>1){
				work += separator;
			}
			work += erg;
		}
		return work;
	}
	
	/**
	 * Ergämzt Auflistug aller (modified) Talente > 0
	 * @param work
	 */
	private String addTalente(String work){
		String erg="";
		Collection<Skill> c = this.scriptUnit.getUnit().getModifiedSkills();
		
		
		if (c!=null && c.size()>0){
			ArrayList<Skill> c2 = new ArrayList<Skill>(); 
			c2.addAll(c);
			Collections.sort(c2,new skillComparator()); 
			for (Iterator<Skill> iter = c2.iterator();iter.hasNext();){
				Skill actSkill = (Skill)iter.next();
				if (actSkill.getLevel()>0){
					if (erg.length()>1){
						erg += detailSeparator;
					}
					erg+=actSkill.getName() + ":" + actSkill.getLevel();
				}
			}
		}
		if (erg.length()>1){
			if (work.length()>1){
				work+=separator;
			}
			work += erg;
		}
		return work;
	}
	
	
	private String addItems(ArrayList<String> itemNames, String work){
		String erg = "";
		
		for (Iterator<String> iter = itemNames.iterator();iter.hasNext();){
			String actName = (String)iter.next();
			if (erg.length()>1){
				erg += separator;
			}
			erg+=this.getItemString(actName);
		}
		
		if (erg.length()>1){
			if (work.length()>1){
				work+=separator;
			}
			work += erg;
		}
		return work;
	}
	
	
	/**
	 * liefert Info zu aktuellen (modified) Gegenstand itemName 
	 * @param work
	 * @param itemName
	 * @return
	 */
	private String getItemString(String itemName){
		int anzahl = 0;
		ItemType itemType = this.gd_Script.rules.getItemType(itemName, false);
		if (itemType==null){
			this.doNotConfirmOrders("Beschreibung: " + itemName + " ist unbekannt");
			return itemName + ":?";
		}
		
		Item item = this.scriptUnit.getModifiedItem(itemType);
		if (item!=null){
			anzahl = item.getAmount();
		}
		
		
		return itemName + ":" + anzahl;
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
	 * zum sortieren der skills
	 * @author Fiete
	 *
	 */
	private class skillComparator implements Comparator<Skill>{
		public int compare(Skill o1,Skill o2){
			return o2.getLevel() - o1.getLevel();
		}
	}
	
}
