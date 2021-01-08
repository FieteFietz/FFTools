package com.fftools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import magellan.library.CoordinateID;
import magellan.library.GameData;
import magellan.library.Region;
import magellan.library.Rules;
import magellan.library.Unit;
import magellan.library.rules.ItemType;
import magellan.library.utils.Islands;

import com.fftools.utils.ItemTypePriorityComparator;

/**
 * Die Klasse wird statisch in alle Classen eingebunden, die
 * lesen und schreiben wollen bezüglich von Settings
 * die mittels CR gesetzt werden
 * 
 * @author Fiete
 *
 */

public class ReportSettings {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private static ReportSettings DEFAULT = new ReportSettings();
	
	public static final int KEY_NOT_FOUND = -1;
	
	/**
	 * das gamedata object
	 */
	private GameData _gd = null;
	
	/**
	 * aktuellestes scriptMain object
	 */
	private ScriptMain scriptMain = null;
	

	/**
	 * der optionensatz, der für den gesamten report gilt
	 */
	private ReportOptions reportOptions = null;
	
	/**
	 * kann für jede Region spezial optionen enthalten
	 */
	private Hashtable<Region,ReportOptions> regionOptions = null;
	
	
	private ArrayList<MonsterBedrohung> monsterBedrohungsList = null;
	
	
	
	/**
	 * Ordnet Categorienamen den Detailsätzen zu
	 */
	private Hashtable<String,settingDetail> settingCategories = null;
	
	/**
	 * Indikator, ob ReportSettings gesetzt wurden
	 * (ob FFTools2 innerhalb von Mag bereits gelaufen sind)
	 */
	private boolean empty = true;

	/**
	 * Klasse der Detailsätze
	 * Ordnet einem ItemType eine Priorität zu
	 * 
	 * @author Fiete
	 *
	 */
	private class settingDetail{
		private Hashtable<ItemType,Integer> detailMap = new Hashtable<ItemType,Integer>(1);
	}
	
	/**
	 * Tabelle aller Itemtypes mit eintsprechend sortierten LinkListen
	 */
	private Hashtable<String,LinkedList<ItemType>> detailListen = null;

	
	
	private class detailSettingComparator implements Comparator<ItemType>{
		
		settingDetail actDetail = null;
		
		public detailSettingComparator(settingDetail actSD){
			this.actDetail = actSD;
		}
		public int compare(ItemType o1,ItemType o2){
			
			int prio1 = actDetail.detailMap.get(o1);
			int prio2 = actDetail.detailMap.get(o2);
			
			return (prio2-prio1);
		}
	}
	
	
	/**
	 * setzt das GameData object
	 * @param gd
	 */
	public void setGameData(GameData gd){
		this._gd = gd;
	}
	
	/**
	 * liefert die Rules aus GameData
	 * @return
	 */
	public Rules getRules(){
		if (this._gd==null){return null;}
		return this._gd.rules;
	}
	
	
	/**
	 * 
	 * Setzt den angegebenen Parametersatz in die Settings
	 * wenn bereits vorhanden, wird überschrieben
	 * 
	 * 
	 * @param catName
	 * @param itemName
	 * @param Prio
	 */
	public void setData(String catName,String itemName, int Prio){
		if (this._gd==null){
			outText.addOutLine("Report.Settings.setData with no GameData set!");
			return;
		}
		this.setNotEmpty();
		// gibts catName schon ?
		// bzw gibts die Hashtable schon
		settingDetail sD = new settingDetail();
		if (this.settingCategories==null) {
			this.settingCategories = new Hashtable<String,settingDetail>(1);
			this.settingCategories.put(catName, sD);
		} else  {
			if (this.settingCategories.containsKey(catName)){
				// name vorhanden
				sD = this.settingCategories.get(catName);
			} else {
				// name nicht vorhanden: dazu
				this.settingCategories.put(catName, sD);
			}
		}
		// in sD auf jedenFall settingDetails
		
		// itemType vomItemName organisieren
		// ItemType itemType = super.c.getData().rules.getItemType("Silber");
		// eventuelle UNterstriche ersetzen...
		String processedItemName = itemName.replace("_", " ");
		ItemType itemType = this._gd.rules.getItemType(processedItemName);
		if (itemType==null){
			outText.addOutLine("Report.Settings.setData: itemType not found: " + itemName);
			return;
		}
		// ob vorhanden oder nicht: setzen
		// wenn vorhanden, wird ja überschrieben
		sD.detailMap.put(itemType,Prio);
	}
	
	/**
	 * liefert eine ArrayList mit allen ItemTypes der gewünschten Category
	 * sortiert nach deren Prio
	 * @param catName
	 * @return
	 */
	
	public ArrayList<ItemType> getItemTypes(String catName){
		if (this.settingCategories==null){
			return null;
		}
		settingDetail sD = this.settingCategories.get(catName);
		if (sD==null){
			catName = catName.substring(0,1).toUpperCase() + catName.substring(1);
			sD = this.settingCategories.get(catName);
		}
		if (sD==null){
			for (String s : this.settingCategories.keySet()){
				if (s.equalsIgnoreCase(catName)){
					sD = this.settingCategories.get(s);
				}
			}
		}
		
		
		if (sD==null){
			return null;
		} else {
			// return array bauen
			ArrayList<ItemType> myItemTypes = new ArrayList<ItemType>();
			for (Iterator<ItemType> iter = sD.detailMap.keySet().iterator();iter.hasNext();){
				ItemType actItemType = (ItemType)iter.next();
				myItemTypes.add(actItemType);
			}
			// sortieren
			ItemTypePriorityComparator compi = new ItemTypePriorityComparator(catName);
			Collections.sort(myItemTypes,compi);
			return myItemTypes;
		}
	}
	
	
	public int getReportSettingPrio(String catName,ItemType itemType){
		if (this.settingCategories==null){
			return 0;
		}
		settingDetail sD = this.settingCategories.get(catName);
		if (sD == null){
			return 0;
		}
		if (sD.detailMap.containsKey(itemType)){
			return sD.detailMap.get(itemType);
		} else {
			return 0;
		}
	}
	
	
	/**
	 * liefert für alle Aufrufe die beim Initialiserung erzeugte
	 * Default-ReportSettingsClass
	 * @return
	 */
	
	public static ReportSettings getInstance() {
		return DEFAULT;
	}
	
	public void reset(){
		this.settingCategories = null;
		this.detailListen = null;
		this.regionOptions = null;
		this.reportOptions = null;
		
	}
	
	/**
	 * liefert wahr, wenn angegebener String bereits als Kategorie 
	 * definiert worden ist
	 * @param catName
	 * @return
	 */
	
	public boolean isInCategories(String catName){
		if (this.settingCategories==null){
			return false;
		}
		for (Iterator<String> iter = this.settingCategories.keySet().iterator();iter.hasNext();){
			String actCatName = (String)iter.next();
			if (actCatName.equalsIgnoreCase(catName)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * liefert wahr, wenn angegebener String bereits als Kategorie 
	 * definiert worden ist
	 * @param catName
	 * @return
	 */
	
	public String getCategorieName(String catName){
		if (this.settingCategories==null){
			return null;
		}
		for (Iterator<String> iter = this.settingCategories.keySet().iterator();iter.hasNext();){
			String actCatName = (String)iter.next();
			if (actCatName.equalsIgnoreCase(catName)){
				return actCatName;
			}
		}
		return null;
	}
	
	
	
	/**
	 * erwartet string hinter // script setscripteroption
	 * parst den string und setzt entsprechend optionen
	 * @param optionS
	 */
	public boolean parseOption(String optionS,Unit u,boolean toLowerCase) {
		/** mögliche angabe des Geltungsbereiches der Option
		 * region
		 * insel
		 * also:
		 * setscripteroption insel x=y
		 */
		Region r = u.getRegion();
		String geltungsbereich = null;
		boolean erg = true;
		// habe  wir mehrere Angaben?
		String[] couple = optionS.split(" ");
		if (couple.length>0){
			for (int i = 0;i < couple.length ; i++){
				String option = couple[i];
				if (option.length()>0){
					if (option.indexOf("=")>0){
						// ne Option mit Gleichheitszeichen
						if (!this.parseOption2(geltungsbereich,option,r,toLowerCase)) {
							erg = false;
						}
							
					} else {
						// keine option, eventuell ein geltungsbereich
						if (option.equalsIgnoreCase("region")||option.equalsIgnoreCase("insel")){
							geltungsbereich = option;
						} else {
							outText.addOutLine("Reportsettings: unbekannter Geltungsbereich " + option +  " bei" + u.toString(true) + " (" + optionS +";part " + option + ")");
							erg = false;
						}
					}
				}
			}
		}
		return erg;
	}
	
	


	
	/**
	 * parst einen optionsparameter der ein = enthält
	 * und fügt diesen der liste hinzu bzw setzt neu
	 * @param s
	 */
	private boolean parseOption2(String geltungsbereich, String s,Region r, boolean tolowercase){
		String[] setting = s.split("=");
		if (setting.length!=2){
			outText.addOutLine("!! Reportsettings: not korrect option:" + s);
			outText.addOutLine("!!! ReportSettings: Region " + r.toString() + ", text=" + s);
			return false;
		}
		String key = setting[0].toLowerCase();
		String value = setting[1];
		if (tolowercase){
			value = setting[1].toLowerCase();
		}
		this.setNotEmpty();
		// in diese Options soll geschrieben werden
		// entweder vom report oder von der region
		// deswegen liste, um gleich für ne ganze insel eintragen zu können
		ArrayList<ReportOptions> myReportOptions = new ArrayList<ReportOptions>();
		// inselsettings dürfen regionssettings nicht überschrieben
		boolean doNotOverride = false;
		if (geltungsbereich == null){
			// reportweite option
			if (this.reportOptions==null){
				this.reportOptions = new ReportOptions(null);
			}
			
			myReportOptions.add(this.reportOptions);
		} else if (geltungsbereich.equalsIgnoreCase("region")){
			myReportOptions.add(this.getRegionOptions(r));
		} else if (geltungsbereich.equalsIgnoreCase("insel")){
			// ok..für die ganze Insel
			Map<CoordinateID,Region> regions = Islands.getIsland(r);
			if (regions==null){
				// should never happen
				outText.addOutLine("!!! ReportSettings: keine Insel gefunden.");
				outText.addOutLine("!!! ReportSettings: Region " + r.toString() + ", text=" + s);
				return false;
			}
			// alle regionen der insel dazupacken..genauer: die Optionen
			for (Iterator<Region> iter = regions.values().iterator();iter.hasNext();){
				Region actR = (Region)iter.next();
				myReportOptions.add(this.getRegionOptions(actR));
			}
			doNotOverride = true;
		}
			
		if (myReportOptions.size()==0){
			// tja, irgendetwas schief gelaufen...keine Optionen zum ablegen
			outText.addOutLine("!!! Reportsettings: Geltungsbereich nicht erkannt!");
			outText.addOutLine("!!! ReportSettings: Region " + r.toString() + ", text=" + s);
			return false;
		}
		
		for (Iterator<ReportOptions> iter = myReportOptions.iterator();iter.hasNext();){
			ReportOptions actRO = (ReportOptions)iter.next();
			// only say something when necessary..(error)
			// outText.addOutLine("Reportsetting " + key + "=>" + value);
			actRO.addReportOption(key, value,doNotOverride);
		}
		
		return true;
		
		
	}
	
	/**
	 * liefert zu einer Region die RegionOptions bzw 
	 * legt sie neu an und liefert sie
	 * @param r
	 * @return
	 */
	private ReportOptions getRegionOptions(Region r){
		if (this.regionOptions==null){
			this.regionOptions = new Hashtable<Region,ReportOptions>();
		}
		// haben wir bereits einen Optionensatz?
		ReportOptions actRO = (ReportOptions) this.regionOptions.get(r);
		if (actRO!=null){
			// wir haben einen...
			return actRO;
		} else {
			// wir haben keinen
			actRO = new ReportOptions(r);
			this.regionOptions.put(r,actRO);
			this.setNotEmpty();
			return actRO;
		}
	}
	
	/**
	 * liefert den passenden optionsstring
	 * entweder für die spezielle region
	 * oder vom report
	 * oder null
	 * @param key
	 * @return
	 */
	public String getOptionString(String key,Region r){
		// gibt es regionsspezifische settings?
		if (this.regionOptions!=null){
			ReportOptions actRO = (ReportOptions)this.regionOptions.get(r);
			if (actRO!=null){
				String value = actRO.getOptionString(key);
				if (value!=null){
					return value;
				}
			}
		}
		// gibt es reportweite settings?
		if (this.reportOptions!=null){
			String value = this.reportOptions.getOptionString(key);
			if (value!=null){
				return value;
			}
		}
		return null;
	}
	
	/**
	 * liefert den passenden Wert lediglich aus den reportOptionen
	 * für die ganz allgemeinen Fälle
	 * @param key
	 * @return
	 */
	public String getOptionString(String key){
		// gibt es reportweite settings?
		if (this.reportOptions!=null){
			String value = this.reportOptions.getOptionString(key);
			if (value!=null){
				return value;
			}
		}
		return null;
	}
	
	
	
	/**
	 * liefert den passenden optionsstring als int
	 * entweder für die spezielle region
	 * oder vom report
	 * oder den DEFAULT_not found wert
	 * @param key
	 * @return
	 */
	public int getOptionInt(String key,Region r){
		String value = getOptionString(key, r);
		if (value==null){return KEY_NOT_FOUND;}
		int erg = KEY_NOT_FOUND;
		try {
			erg = Integer.parseInt(value);
		} catch (NumberFormatException e){
			outText.addOutLine("!!!ReportSettings getOptionInt fehlerhafter Wert. Region " + r.toString() + " Fehler: " + e.toString());
			outText.addOutLine("!!!angefragter KEY: " + key);
		}
		return erg;
	}
	
	/**
	 * liefert den passenden optionsstring als int
	 * entweder vom report
	 * oder den DEFAULT_not found wert
	 * @param key
	 * @return
	 */
	public int getOptionInt(String key){
		String value = getOptionString(key);
		if (value==null){return KEY_NOT_FOUND;}
		int erg = KEY_NOT_FOUND;
		try {
			erg = Integer.parseInt(value);
		} catch (NumberFormatException e){
			outText.addOutLine("!!!ReportSettings getOptionInt fehlerhafter Wert. Fehler: " + e.toString());
			outText.addOutLine("!!!angefragter KEY: " + key);
		}
		return erg;
	}
	
	
	/**
	 * liefert den passenden optionsstring als bool
	 * entweder für die spezielle region
	 * oder vom report
	 * oder false (!!!)
	 * @param key
	 * @return
	 */
	public boolean getOptionBoolean(String key,Region r){
		String value = getOptionString(key, r);
		if (value==null){return false;}
		boolean erg = false;
		if (value.equalsIgnoreCase("ja")||value.equalsIgnoreCase("an")||
				value.equalsIgnoreCase("wahr")||value.equalsIgnoreCase("true")){
			erg = true;
		}
		return erg;
	}
	
	
	/**
	 * liefert den passenden optionsstring als bool
	 * vom report
	 * oder false (!!!)
	 * @param key
	 * @return
	 */
	public boolean getOptionBoolean(String key){
		String value = getOptionString(key);
		if (value==null){return false;}
		boolean erg = false;
		if (value.equalsIgnoreCase("ja")||value.equalsIgnoreCase("an")||
				value.equalsIgnoreCase("wahr")||value.equalsIgnoreCase("true")){
			erg = true;
		}
		return erg;
	}
	
	public void informUs(){
		outText.setFile("ReportSettings");
		outText.addOutLine("**********************ReportSettings***********");
		if (this.settingCategories==null){
			outText.addOutLine("no setting Categories found");
		} else {
			outText.addOutLine("Number of Categories: " + this.settingCategories.size());
			for (Iterator<String> iter = this.settingCategories.keySet().iterator();iter.hasNext();){
				String catName = (String)iter.next();
				outText.addOutLine("settings for category: " + catName);
				settingDetail sD = this.settingCategories.get(catName);
				for (Iterator<ItemType> iter2 = sD.detailMap.keySet().iterator();iter2.hasNext();){
					ItemType itemType = (ItemType)iter2.next();
					outText.addOutLine(catName + ":" + itemType.getName() + ":" + sD.detailMap.get(itemType));
				}
				outText.addOutLine(catName + " finished ..................................");
			}
		}
		
		if (this.reportOptions==null){
			outText.addOutLine("keine reportweiten optionen gefunden");
		} else {
			outText.addOutLine("*******  <=> Optionen:  *********");
			this.reportOptions.informUs();
		}
		
		if (this.regionOptions==null){
			outText.addOutLine("keine regionsspezifischen optionen gefunden");
		} else {
			for (Iterator<ReportOptions> iter = this.regionOptions.values().iterator();iter.hasNext();){
				ReportOptions actRO = (ReportOptions)iter.next();
				actRO.informUs();
			}
		}
		
		outText.addOutLine("beendet <=> Optionen");
		outText.addOutLine("***************End of ReportSettings***********");
		outText.setFileStandard();
	}
	
	
	
	public LinkedList<ItemType> getDetailList(String name){
		
		if (this.detailListen==null){
			this.detailListen = new Hashtable<String, LinkedList<ItemType>>();
		}
		
		LinkedList<ItemType> actList = this.detailListen.get(name);
		if (actList!=null){
			return actList;
		}
		
		// OK, noch nicht vorhanden
		// existiert ein Detailsatz
		settingDetail actDetail = this.settingCategories.get(name);
		if (actDetail==null){
			// es existiert kein Detailsatz
			// ist es vielleicht direkt ein ItemType?
			String processedItemName = name.replace("_", " ");
			ItemType itemType = this._gd.rules.getItemType(processedItemName);
			if (itemType==null){
				// nope..auch kein Name direkt
				this.detailListen.put(name,null);
				return null;
			} else {
				// aha! also nur ein ItemType
				LinkedList<ItemType> ergList = new LinkedList<ItemType>();
				ergList.add(itemType);
				this.detailListen.put(name,ergList);
				return ergList;
			}
			
		}
		
		// OK, es gibt auch einen Detaildatensatz
		ArrayList<ItemType> sortList = new ArrayList<ItemType>();
		for (Iterator<ItemType> iter=actDetail.detailMap.keySet().iterator();iter.hasNext();){
			sortList.add((ItemType)iter.next());
		}
		
		// comparator anlegen
		detailSettingComparator compi = new detailSettingComparator(actDetail);
		// sortieren
		Collections.sort(sortList,compi);
		// LinkList bauen
		LinkedList<ItemType> ergList = new LinkedList<ItemType>();
		for (Iterator<ItemType> iter = sortList.iterator();iter.hasNext();){
			ergList.add((ItemType)iter.next());
		}
		// LinkList ergänzen
		this.detailListen.put(name,ergList);
		
		return ergList;
	}

	/**
	 * @return the empty
	 */
	public boolean isEmpty() {
		return empty;
	}

	/**
	 * @param empty the empty to set
	 */
	private void setNotEmpty() {
		this.empty = false;
	}

	/**
	 * @return the scriptMain
	 */
	public ScriptMain getScriptMain() {
		return scriptMain;
	}

	/**
	 * @param scriptMain the scriptMain to set
	 */
	public void setScriptMain(ScriptMain scriptMain) {
		this.scriptMain = scriptMain;
	}
	
	
	private class MonsterBedrohung {
		private String raceName = "";
		private double Faktor = 1;
		private int minTaktikLevel=0;
		
		public String getRaceName() {
			return raceName;
		}
		public double getFaktor() {
			return Faktor;
		}
		public int getMinTaktikLevel() {
			return minTaktikLevel;
		}		
	}
	
	
	public void addMonsterBedrohungsList(String _raceName, double _Faktor, int _minTaktikLevel) {
		if (this.monsterBedrohungsList==null) {
			this.monsterBedrohungsList = new ArrayList<ReportSettings.MonsterBedrohung>();
		}
		
		
		
		
		
	}
	
	
	
}
