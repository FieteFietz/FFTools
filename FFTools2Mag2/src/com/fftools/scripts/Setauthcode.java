package com.fftools.scripts;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import magellan.client.Client;
import magellan.library.GameData;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.rules.Date;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.VersionInfo;
import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.utils.FFToolsOptionParser;




/**
 * 
 * @author Fiete
 * Basisklasse fuer alle scripte
 * runScript sollte ueberschrieben werden durch
 * extendende KLassen
 *
 */

public class Setauthcode extends Script implements ScriptInterface,OverlordInfo{
	public static final OutTextClass outText = OutTextClass.getInstance();
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	public ScriptUnit scriptUnit = null;
	public Client c = null;
	public GameData gd_Script=null;
	
	private ArrayList<String> arguments = null;
	
	/**
	 * Array von ints...die durchläufe, die für dieses script spannend sind
	 */
	private int[] runsAt = {1};
	
	/**
	 * wichtig: parameterloser construktor..
	 * bei jedem script dabei!
	 *
	 */
	public Setauthcode() {
		
	}
	
	/**
	 * setzt gleich script unit
	 * @param su
	 */
	public Setauthcode(ScriptUnit su){
		this.scriptUnit = su;
	}
	
	/**
	 * 
	 * auszufuehrende methode, herz des scriptes
	 * sollte / muss in jedem script ueberschrieben werden
	 * 
	 * 
	 */
	
	
	public void runScript(int scriptDurchlauf){
		if (this.scriptUnit==null){
			addOutLine("Da wurde vergessen, unit zu setzen");
			return;
		}
		
		/*
		 * gesuchte Zeile: // authcode runde=XXX code=XXXX
		 */
		// Runde ermitteln
		Date d = this.gd_Script.getDate();
		int runde = d.getDate();
		String searchS = "// authcode runde=" + runde;
		String orderLine = "";
		boolean foundIt = false;
		for(Iterator<Order> iter = this.getUnit().getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith(searchS.toLowerCase())){
				foundIt=true;
				orderLine=s;
			}
		}
		if (foundIt){
			FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
			OP.addOptionString(orderLine);
			String code = OP.getOptionString("Code");
			if (code.length()>0){
				reportSettings.parseOption("authcode="+code, this.getUnit(),false);
				this.addComment("authcode: code wurde bereits generiert:" + code);
			} else {
				this.addComment("!!! authcode: code nicht erkannt für Runde " + runde);
				this.doNotConfirmOrders();
			}
			
		} else {
			// nicht gefunden, code generieren, Zeile generieren, reportSettings ergänzen
			// code soll 4-stellige Zufallszahl sein
			Random random = new Random();
		    int code = random.nextInt(9000 + 1) + 1000;
			orderLine = "// authcode Runde=" + runde + " code=" + code;
			this.scriptUnit.addOrder(orderLine, false);
			this.addComment("authcode: code wurde neu generiert");
			reportSettings.parseOption("authcode="+code, this.getUnit(),false);
		}

	}
	
	public void setClient(Client _c){
		this.c = _c;
		this.gd_Script = _c.getData();
	}
	
	public void setGameData(GameData _gd) {
		this.gd_Script = _gd;
	}
	
	public void setScriptUnit(ScriptUnit _u) {
		this.scriptUnit = _u;
	}
	
	
	public void setArguments(ArrayList<String> args) {
		this.arguments = args;
	}
	
	
	public int getArgCount(){
		if (arguments==null) {
			return 0;
		} else {
			return arguments.size();
		}
	}
	
	public String getArgAt(int i){
		if (arguments==null){
			return "";
		}
		if (arguments.size()<i){
			addOutLine("Argument angefordert welches nicht uebergeben wurde (Anzahl)");
			return "";
		}
		return arguments.get(i);
	}
	
	public void addComment(String s){
		addComment(s, true);
	}
	
	public void addComment(String s,boolean no_doubles){
		if (this.scriptUnit.getUnit()==null){
			return;
		}
		this.scriptUnit.addComment(s, no_doubles);
	}
	
	public void addOrder(String s,boolean no_doubles) {
		if (this.scriptUnit==null){
			return;
		}
		
		this.scriptUnit.addOrder(s, no_doubles);
	}
	
	public Region region(){
		return this.scriptUnit.getUnit().getRegion();
	}
	
	public void addOutLine(String s){
		outText.addOutLine(s);
	}
	
	public String unitDesc(){
		return this.scriptUnit.unitDesc(); 
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
	 * @see ScriptInterface
	 */
	public boolean errorMsgIfNotAllowedAddedScript(){
		return true;
	}
	
	
	
	/**
	 * liefert ein array mit den durchläufen, wann dieses script aktiv wird..
	 */
	public int[] runAt(){
		return runsAt;
	}
	
	/**
	 * Hilfsfunktion zum komfortablen setzen eines Durchlaufes als array
	 * @param i
	 */
	public void setRunAt(int i){
		if (this.runsAt==null){
			int[] test ={0};
			this.runsAt = test;
		}
		this.runsAt[0] = i;
	}
	
	/**
	 * setzt das DurchlaufNummer-Array
	 * @param i
	 */
	public void setRunAt(int[] i){
		this.runsAt = i;
	}
	
	
	/**
	 * Läuft dieses script im angefragten durchlauf?
	 * @param s
	 * @param check
	 * @return true or false
	 */
	public boolean shouldRun(int check){
		if (this.runAt()==null){return false;}
		for (int i = 0;i<this.runAt().length;i++){
			int actX = (int)this.runAt()[i];
			if (actX==check){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * liefert die unit zu diesem Script
	 * @return
	 */
	public Unit getUnit(){
		return this.scriptUnit.getUnit();
	}

	/**
	 * @return the arguments
	 */
	public ArrayList<String> getArguments() {
		return arguments;
	}
	
	/**
	 * shortcut zum overlord
	 * @return
	 */
	public Overlord getOverlord(){
		if (this.scriptUnit==null){
			addOutLine("Da wurde vergessen, unit zu setzen! (Script.getOverlord");
			
			return null;
		}
		return this.scriptUnit.getScriptMain().getOverlord();
	}
	
	/**
	 * liefert eine ArrayListe der kommagetrennten Optionswerte
	 * @param SpecName
	 * @return
	 */
	public ArrayList<String> getKommaSeparated(FFToolsOptionParser OP,String optionName){
		ArrayList<String> erg = null;
		String specsString = OP.getOptionString(optionName);
		if (specsString.length()>1){
			// splitten, falls mehrere da
			String[] specsArray = specsString.split(",");
			erg = new ArrayList<String>();
			for (int i=0;i<specsArray.length;i++){
				String s2 = specsArray[i];
				erg.add(s2);
			}
		}
		return erg;
	}
	
	/**
	 * short cut 
	 */
	public void doNotConfirmOrders(){
		this.scriptUnit.doNotConfirmOrders();
	}
	
	/**
	 * wenn beim start im client regionen selected sind, dann nur die
	 * scripts ausführen,die hier true liefern, alle anderen nicht.
	 * @return
	 */
	public boolean runIfNotInSelectedRegions(){
		return false;
	}
	
	/**
	 * fügt Versions info als Kommentar hinzu
	 */
	public void addVersionInfo(){
		this.addComment("FFTools Version: " + VersionInfo.getVersionInfo(),true);
	}
	
}
