package com.fftools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import magellan.client.Client;
import magellan.client.event.UnitOrdersEvent;
import magellan.library.Item;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.Skill;
import magellan.library.StringID;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.gamebinding.EresseaMovementEvaluator;
import magellan.library.gamebinding.MovementEvaluator;
import magellan.library.io.cr.CRParser;
import magellan.library.rules.ItemType;
import magellan.library.rules.Race;
import magellan.library.rules.RegionType;
import magellan.library.rules.SkillType;

import com.fftools.overlord.Overlord;
import com.fftools.pools.matpool.MatPool;
import com.fftools.pools.matpool.relations.MatPoolOffer;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Rekrutieren;
import com.fftools.scripts.Script;
import com.fftools.scripts.Setlernplan;
import com.fftools.scripts.Settrankorder;


/**
 * 
 * @author Fiete
 *
 * wird angelegt fuer jede durch FfTools bearbeitet Einheit
 * merkt sich u.a., ob bereits "alte" orderzeilen geloescht worden sind
 */
public class ScriptUnit {
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	private Unit unit = null;
	private boolean NotNeededOrdersDeleted = false;
	public ArrayList<String> originalScriptOrders = null;
	public ArrayList<String> originalOrders_All = new ArrayList<String>(0);
	
	private boolean builtfoundScriptList = false;
	private ArrayList<Script> foundScriptList = null;
	
	// workaround, da nach rekrutiere nicht meht modified persons neu berechnet werden
	private int recruitedPersons = 0;
	
	// wird von addComment und addOrder auf true gesetzt
	// und dann wird das orderchangeevent gefeuert beim letzten durchlauf
	private boolean orders_changed = false;
	
	// jedes script hat die chance, diesen wert auf false zu setzen
	// dann wird diese unit NICHT bestaetigt
	private boolean unitOrders_may_confirm = true;
	
	// setOK {Runde}
	private boolean unitOrders_confirm_anyway = false;
	
	// jedes script, welches einen Befehl vollständig erfolgreich umsetzt
	// kann diesen Wert auf true setzen
	// ist nach allen Scripten der Wert immer noch false
	// hat gar kein script gegriffen und die units sollte NICHT
	// bestätigt werden
	// wird bei addOrder immer gesetzt
	private boolean unitOrders_adjusted = false;
	
	// um auf die Pools zuzugreifen brauchen wir eine Referenz auf ScriptMain
	private ScriptMain scriptMain = null;
	
	// Referenz auf den Client
	private Client c = null; 

	// Sondereinstellung für MatPool: darf Einheit was von sich weg geben?
	private boolean gibNix = false;
	
	// neu, bei jedem doNotConfirm muss ein Grund geliefert werden, der wird am Ende der Kommentare // am Anfang noch mit ergänzt
	private ArrayList<String> NoConfirmReasonList = null;
	
	
	// wenn scripte andere scripte aufrufen, können die nicht 
	// sofort in die scriptLsite eingetragen werden..da läuft ja
	// bei runscript der Iterator drüber
	// gibt eine java.util.ConcurrentModificationException
	// daher hier eine private List..die nach dem durchlauf angefügt wird
	// und vor dem Durchlauf gecleart wird
	private ArrayList<Script> addScriptList = null;
	
	private ArrayList<Script> delScriptList = null;
	
	/**
	 * Freie Kapazität der Unit, wird von MatPool genutzt
	 */
	private int originalFreeKapaFood = -1;
	private int originalFreeKapaHorse = -1;
	private int originalFreeKapaUser = -1;
	private int originalFreeKapaUserWeight = -1;
	private int originalModifiedLoad = 0;
	private int usedKapa = 0;
	private int setKapaPolicy = MatPoolRequest.KAPA_unbenutzt;
	
	/**
	 * damit die blöde nachricht nur 1x kommt, nicht bei jedem scriptlauf
	 */
	private boolean noScriptsMessageSaid=false;
	
	/**
	 * Ausschliesslich für Kapitäne mit gewicht=schiff
	 * Dann wird auch dass Gewicht von Einheiten mit // onboard bzw // Crew
	 * berücksichtigt
	 */
	private boolean includeSailorsWeight = false;
	
	
	/*
	 * wird nur gesetzt, wenn gebäudebesitzer und Unterhalt des Gebäudesnicht erfüllt 
	 */
	public boolean unterhaltProblem=false;
	
	
	// MatPool2
	/**
	 * enthält die Items nach geschützten Ordern und Matpoollauf/läufen
	 */
	private HashMap<ItemType, Item> modifiedItemsMatPool2 = null;
	
	
	private boolean isDepotUnit = false;
	private boolean setDepotStatus = false;
	
	private boolean isInClientSelectedRegions = false;
	
	public boolean isInClientSelectedRegions() {
		return isInClientSelectedRegions;
	}

	public void setInClientSelectedRegions(boolean isInClientSelectedRegions) {
		this.isInClientSelectedRegions = isInClientSelectedRegions;
	}

	public ScriptUnit(Unit _u,ScriptMain _scriptMain) {
		this.unit = _u;
		this.scriptMain = _scriptMain;
	}
	
	/**
	 * Liefert true, wenn nicht benoetigte Orderzeilen bereits geloescht worden, sonst false
	 * 
	 * @return
	 */
	
	public boolean areNotNeededOrdersDeleted() {
		return NotNeededOrdersDeleted;
	}
	
	public void setClient(Client _c){
		this.c = _c;
	}
	
	
	/**
	 * 
	 * Loescht alle orderzeilen bis auf scriptzeilen (mit //)
	 * und welche mit do_not_touch
	 * und welche mit @ vorne
	 * 20200304 und welche mit ATTACKIERE vorne
	 * 20200327: weitere Befehle sollen nicht gelöscht werden, wir setzen ein Array auf
	 * @return int Anzahl der geloeschten orderzeilen
	 */
	
	public int deleteNotNeededOrders(){
		int cnt =0;
		if (this.unit == null) {NotNeededOrdersDeleted = false;return -1;}
		int actRunde = getOverlord().getScriptMain().gd_ScriptMain.getDate().getDate();
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		// newOrders.add(this.getUnit().createOrder("; Debug Test delNotNeeO"));
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			// newOrders.add(this.getUnit().createOrder("; Debug checking " + s + " (Runde " + actRunde + ")"));
			if (s.toLowerCase().startsWith("// zupferinfo runde=" + actRunde)) {
				cnt++;
			} else if (s.toLowerCase().startsWith("// setok " + actRunde)){
				cnt++;
				// newOrders.add(this.getUnit().createOrder("; Debug setOK actRunde detected"));
				this.unitOrders_confirm_anyway=true;
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toLowerCase().startsWith("// setok")){
				cnt++;
				// newOrders.add(this.getUnit().createOrder("; Debug setOK lastRunde detected"));
			} else if (s.startsWith("//")){
				// Kommentare beibehalten
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toLowerCase().indexOf("do_not_touch")>0 || s.toLowerCase().indexOf(";dnt")>0) {
				// nicht anfassen!
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.startsWith("@")){
				// permanente orders...
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.startsWith("!@")){
				// permanente orders mit Fehlerunterdrückung
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("ATTACKIERE") && s.toLowerCase().indexOf(";mjm checked")<0 && s.toLowerCase().indexOf("; sj von")<0){
				// Attackiere befehle..., die nicht vom MJM oder SJM kommen
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("BETRETE") && s.toLowerCase().indexOf("; akapool->")<0 && s.toLowerCase().indexOf(";enterbuilding")<0 && s.toLowerCase().indexOf(";script auramaxwarning")<0){
				// Betrete ausser ; AkaPool->
				// ausser ;EnterBuilding
				// ausser ;script Auramaxwarning
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("BESCHREIBE")){
				// Beschreibe
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("BENENNE")){
				// Benenne
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("BANNER")){
				// Banner
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("BOTSCHAFT")){
				// Botschaft
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("EMAIL")){
				// Email
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("GRUPPE")){
				// Gruppe
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("KÄMPFE")  && s.toLowerCase().indexOf(";bewachen")<0  && s.toLowerCase().indexOf(";regionobserver")<0){
				// Kämpfe
				// ausser ;RegionObserver
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("KONTAKTIERE")){
				// Kontaktiere
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("NUMMER")){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("OPTION")){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("PASSWORT")){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("PRÄFIX")){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("SABOTIERE")){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("TARNE")){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("VERGISS")){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("VERLASSE") && s.toLowerCase().indexOf("; akapool")<0 && s.toLowerCase().indexOf(";von goto")<0 && s.toLowerCase().indexOf("; werftarbeiter")<0){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("ZEIGE")){
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toUpperCase().startsWith("ZERSTÖRE")){
				newOrders.add(this.getUnit().createOrder(s));
			} else {
				cnt++;
			}
		}
		this.unit.setOrders2(newOrders);
		NotNeededOrdersDeleted = true;
		
		// this.unit.getRegion().refreshUnitRelations(true);
		
		// hat diese Unit durch ein Script (rekrutieren) TEMPs angelegt?
		if (this.unit.tempUnits()!=null && this.unit.tempUnits().size()>0){
			ArrayList<TempUnit> Temps4Deletion = new ArrayList<TempUnit>();
			for (TempUnit t:this.unit.tempUnits()){
				if (t.getOrders2()!=null && t.getOrders2().size()>0){
					for (Order o:t.getOrders2()){
						String s = o.getText();
						if (s.equals(Rekrutieren.scriptCreatedTempMark)){
							// bingo!!
							Temps4Deletion.add(t);
							break;
						}
					}
				}
			}
			
			// Jetzt löschen
			if (Temps4Deletion.size()>0){
				for (TempUnit t:Temps4Deletion){
					this.addComment("Lösche vorher angelegte Temp Unit: " + t.getID().toString());
					this.unit.deleteTemp(t.getID(), this.scriptMain.gd_ScriptMain);
					ScriptUnit suDel = this.scriptMain.getScriptUnit(t);
					if (suDel!=null){
						this.getOverlord().deleteScriptUnit(suDel);
					} else {
						this.addComment("!!! Zu löschende Unit nicht in ScriptUnis!!!(" + t.getID().toString() + ")(" + this.unitDesc()+")");
					}
				}
			}
		}
		return cnt;
	}
	
	/**
	 * 
	 * Loescht alle orderzeilen bis auf scriptzeilen (mit //)
	 * und welche mit do_not_touch
	 * und welche mit @ vorne
	 * und beschränkt sich auf orders, die mit ordersStartWith beginnen
	 * 
	 * @return int Anzahl der geloeschten orderzeilen
	 */
	
	public int deleteSomeOrders(String ordersStartWith){
		int cnt =0;
		if (this.unit == null) {NotNeededOrdersDeleted = false;return -1;}
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.startsWith("//")){
				// Kommentare beibehalten
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toLowerCase().indexOf("do_not_touch")>0 || s.toLowerCase().indexOf(";dnt")>0) {
				// nicht anfassen!
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.toLowerCase().indexOf(";script")>0) {
				// nicht anfassen!
				newOrders.add(this.getUnit().createOrder(s));
			} else if (s.startsWith("@")){
				// permanente orders...
				newOrders.add(this.getUnit().createOrder(s));
			} else if (!(s.toLowerCase().startsWith(ordersStartWith.toLowerCase()))){
				// orders, die nicht mit ordersStartWith beginnen
				newOrders.add(this.getUnit().createOrder(s));
			} else {
				cnt++;
			}
		}
		if (cnt>0){
			this.unit.setOrders2(newOrders);
		}
		// this.unit.getRegion().refreshUnitRelations(true);
		return cnt;
	}
	
	/**
	 * 
	 * Loescht alle orderzeilen die mit ordersStartWith beginnen (inkl //)
	 * 
	 * @return int Anzahl der geloeschten orderzeilen
	 */
	
	public int deleteSpecialOrder(String ordersStartWith){
		int cnt =0;
		if (this.unit == null) {return -1;}
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (!(s.toLowerCase().startsWith(ordersStartWith.toLowerCase()))){
				// orders, die nicht mit ordersStartWith beginnen
				newOrders.add(this.getUnit().createOrder(s));
			} else {
				cnt++;
			}
		}
		if (cnt>0){
			this.unit.setOrders2(newOrders);
		}
		return cnt;
	}
	
	
	/**
	 * 
	 * Loescht alle orderzeilen bis auf scriptzeilen (mit //)
	 * und welche mit do_not_touch
	 * und welche mit @ vorne
	 * und beschränkt sich auf orders, die mit ordersStartWith beginnen
	 * 
	 * @return int Anzahl der geloeschten orderzeilen
	 */
	
	public boolean hasOrder(String orderStartWith){
		if (this.unit.getOrders2()==null || this.unit.getOrders2().size()==0){
			return false;
		}
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if ((s.length()>=orderStartWith.length()) &&  s.substring(0, orderStartWith.length()).equalsIgnoreCase(orderStartWith)){
				return true;
			} 
		}
		return false;
	}
	
	
	public void saveOriginalScriptOrders(){
		if (!NotNeededOrdersDeleted) {deleteNotNeededOrders();}
		originalScriptOrders = new ArrayList<String>(1);

		// sichern der urspruenglichen // script orders falls durch scripte selbst
		// welche hinzugefuegt werden...die sollen nicht abgearbeitet werden
		// (Ergänzung: in dieser runDurchlauf-runde)
		// und wuerden ausserdem den Iterator ueber u.orders stoeren...
		
		for(Iterator<Order>iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script ")) {
				String s2 = s.substring(10);
				originalScriptOrders.add(s2);
			}
			this.originalOrders_All.add(s);
		}
	}
	
	/**
	 * 
	 * Stellt sicher, dass andere Orders als // geloescht worden sind
	 * 
	 * Stellt sicher, dass die OriginalScriptorders gesichert sind
	 * und das die scriptListe erstellt worden ist
	 * 
	 * durchlaeuft die scripte der unit und ruft sie mit dem entsprechenden
	 * Durchlauf int auf
	 * 
	 * @param scriptDurchlauf
	 */
	
	public void runScripts(int scriptDurchlauf){

		long time1 = System.currentTimeMillis();
		
		
		if (this.unit==null){
			return;
		}
		if (this.getOverlord().isDeleted(this)){
			return;
		}
		
	
		if (!NotNeededOrdersDeleted){deleteNotNeededOrders();}
		long timeX=System.currentTimeMillis();
		long tDiffX =0;
		tDiffX = timeX - time1;
		this.getOverlord().Zeitsumme1+=tDiffX;
		
		
		/*
		if (this.getOverlord().isDeleted(this)){
			return;
		}
		*/
		
		if (originalScriptOrders==null){saveOriginalScriptOrders();}
		timeX=System.currentTimeMillis();
		tDiffX =0;
		tDiffX = timeX - time1;
		this.getOverlord().Zeitsumme2+=tDiffX;
		
		
		if (!builtfoundScriptList) {builtScriptList();}
		timeX=System.currentTimeMillis();
		tDiffX =0;
		tDiffX = timeX - time1;
		this.getOverlord().Zeitsumme3+=tDiffX;
		
		
		
		if (foundScriptList==null){
			if (!noScriptsMessageSaid) {
				outText.addOutLine("keine Scripte gefunden: " + unit.toString(true));
				noScriptsMessageSaid=true;
			}
			return;
		}
		
		// for (Iterator<Script> iter = this.foundScriptList.iterator();iter.hasNext();){
		for (Script sc : this.foundScriptList) {
			timeX=System.currentTimeMillis();
			tDiffX =0;
			tDiffX = timeX - time1;
			this.getOverlord().Zeitsumme6+=tDiffX;	
			// boolean b1 = isInRegionSelected(this.unit.getRegion());
			boolean b1 = this.isInClientSelectedRegions;
			timeX=System.currentTimeMillis();
			tDiffX =0;
			tDiffX = timeX - time1;
			this.getOverlord().Zeitsumme7+=tDiffX;
			boolean b2 = isAllwaysRunScript(sc);
			timeX=System.currentTimeMillis();
			tDiffX =0;
			tDiffX = timeX - time1;
			this.getOverlord().Zeitsumme8+=tDiffX;
			if (b1 || b2){
				timeX=System.currentTimeMillis();
				tDiffX =0;
				tDiffX = timeX - time1;
				this.getOverlord().Zeitsumme9+=tDiffX;	
			   if (sc.shouldRun(scriptDurchlauf)){	
				   sc.runScript(scriptDurchlauf);
			   } 
			} else {
				timeX=System.currentTimeMillis();
				tDiffX =0;
				tDiffX = timeX - time1;
				this.getOverlord().Zeitsumme10+=tDiffX;
			}
		}
		timeX=System.currentTimeMillis();
		tDiffX =0;
		tDiffX = timeX - time1;
		this.getOverlord().Zeitsumme4+=tDiffX;
		
		// Iterator ist durch..jetzt eventuelle script anhängen
		if (this.addScriptList!=null && this.addScriptList.size()>0){
			this.foundScriptList.addAll(this.addScriptList);
			this.addScriptList.clear();
		}
		
		timeX=System.currentTimeMillis();
		tDiffX =0;
		tDiffX = timeX - time1;
		this.getOverlord().Zeitsumme5+=tDiffX;
		
		
		this.checkOrderRefresh();
		
		
	}
	
	/**
	 * Überprüft, ob dieses script ausgeführt werden soll, egal ob
	 * in selektierten regionen oder nicht
	 * @param s
	 * @return
	 */
	private boolean isAllwaysRunScript(Script s){
		boolean erg=false;
		if (s.getClass().equals(Setlernplan.class)){
			return true;
		}
		if (s.getClass().equals(Settrankorder.class)){
			return true;
		}
		
		return erg;
	}
	
	/**
	 * fügt ein weiteres script der scriptliste hinzu
	 * gedacht für aufrufe aus anderen scripten
	 * @param s
	 */
	public void addAScript(Script s){
		
		if (this.addScriptList==null){
			this.addScriptList = new ArrayList<Script>(1);
		}
		this.addScriptList.add(s);
	}
	
	/**
	 * fügt ein weiteres script der scriptliste hinzu
	 * gedacht für aufrufe aus anderen scripten
	 * Scripte dieser Unit dürfen nicht gerade abgearbeitet werden!
	 * @param s
	 */
	public void addAScriptNow(Script s){
		this.foundScriptList.add(s);
	}
	
	/**
	 * entfernt ein script der scriptliste
	 * gedacht für aufrufe aus anderen scripten
	 * @param s
	 */
	public void delAScript(Script s){
		
		if (this.delScriptList==null){
			this.delScriptList = new ArrayList<Script>(1);
		}
		this.delScriptList.add(s);
	}
	
	
	/**
	 * falls sich orders irgendwann seit letztem Refresh geaendert haben
	 * und ein client - refernz vorhanden ist
	 * feuert das OrdersChangeEvent fuer die unit
	 */
	
	public void checkOrderRefresh() {
		if (this.orders_changed && this.c!=null) {
			this.c.getMagellanContext().getEventDispatcher().fire(new UnitOrdersEvent(this, this.unit));
			this.orders_changed = false;
		}
	}

	/**
	 * Baut die Liste der erkannten scripte
	 *
	 */
	public void builtScriptList(){
		ArrayList<String> arguments = null;
		String scriptName = null;
		if (this.unit==null){
			return;
		}
		
		if (builtfoundScriptList) {
			return;
		}
		
		if (originalScriptOrders==null){saveOriginalScriptOrders();}
		for (Iterator<String> i2 = originalScriptOrders.iterator();i2.hasNext();){
			String order = (String)i2.next();
			scriptName = null;
			arguments=null;
			StringTokenizer st = new StringTokenizer(order);
			int i = 0;
		     while (st.hasMoreTokens()) {
		         // println(st.nextToken());
		    	 if (i==0) {
		    		 // nach // script steht der scriptname
		    		 scriptName = st.nextToken();
		    	 } else {
		    		 // ansonsten folgen Parameter
		    		 if (arguments==null){arguments = new ArrayList<String>(1);}
		    		 arguments.add(st.nextToken());
		    	 }
		    	 i++;
		     }
		     if (scriptName!=null){
		    	 // wenn keine Ausnahme, script finden
		    	 boolean noScript = false;
		    	 
		    	 if (scriptName.equalsIgnoreCase("setprio")){
		    		 noScript = true;
		    	 }
		    	 
		    	 if (scriptName.equalsIgnoreCase("setscripteroption")){
		    		 noScript = true;
		    	 }
		    	 if (scriptName.equalsIgnoreCase("setscripteroption2")){
		    		 noScript = true;
		    	 }
		    	 
		    	 if (scriptName.equalsIgnoreCase("setOK")){
		    		 noScript = true;
		    	 }
		    	 
		    	 if (!noScript){
			    	 // Spannung...classe finden
			    	 this.findScriptClass(scriptName, arguments,false);
		    	 }
		     } else {
		    	 // es kam nuescht hinter script
		    	 // Fehlermeldung
		    	 outText.addOutLine("Kein Scriptbefehl erkannt");
		     }
		}
		
		
		// extra für Marc..Lohn hinzufügen
		// ist nur erfolgreich, wenn nicht bereits explizit
		// Lohnscript in den befehlen angefordert wurde
		if (reportSettings.getOptionBoolean("Lohn", this.unit.getRegion())) {
		  	
			this.findScriptClass("Lohn","",false);
		}
		
		// Gebäudeunterhalt benutzen können
		if (this.getUnit().getModifiedBuilding()!=null) {
			if (reportSettings.getOptionBoolean("Gebaeudeunterhalt", this.unit.getRegion())) {
				   this.findScriptClass("Gebaeudeunterhalt","",false);
		    }
		}
		
		
		
		builtfoundScriptList = true;
	}
	
	public void doNotConfirmOrders(String reason) {
		this.unitOrders_may_confirm = false;
		if (this.NoConfirmReasonList==null) {
			this.NoConfirmReasonList=new ArrayList<String>(0);
		}
		if (!this.NoConfirmReasonList.contains(reason)) {
			this.NoConfirmReasonList.add(reason);
		}
		this.addComment(reason);
	}
	
	public void ordersHaveChanged() {
		this.orders_changed=true;
	}
	
	/**
	 * ueberprueft, ob eines der scripte 
	 * unitOrders_may_confirm via public void doNotConfirmOrders()
	 * auf false gesetzt hat, anderenfalls wird die einheit bestaetigt
	 * anderenfalls wird sie NICHT bestaetigt
	 * 
	 */
	public void setFinalConfim() {
		// gibts lange befehle mit ;dnt
		
		this.checkForLongOrder();
		if (this.unitOrders_adjusted) {
			this.unit.setOrdersConfirmed(this.unitOrders_may_confirm);
		} else {
			this.unit.setOrdersConfirmed(false);
		}
		if (this.unitOrders_confirm_anyway) {
			this.addComment("!!! Einheit wird durch setOK bestätigt!!!");
			this.unit.setOrdersConfirmed(true);
		}
	}
	
	// falls Gründe vorliegen, die die Einheit als unbestätigt markieren, die ganz an den Anfang setzen....
	public void informDoNotConfirmReason() {
		if (this.NoConfirmReasonList!=null && this.NoConfirmReasonList.size()>0) {
			
			if (this.NoConfirmReasonList.size()>1) {
				this.addComment("****** GRÜNDE FÜR UNBESTÄTIGT: ******");
			} else {
				this.addComment("****** GRUND FÜR UNBESTÄTIGT: ******");
			}
			for (String s : NoConfirmReasonList) {
				this.addComment(s,false);
			}
			
		}
	}
	
	/**
	 * überprüft, ob Kapitän von einem Schiff mit move-order und dieses Schiff segelfähig
	 */
	public void checkShipOK(){
		Ship s = this.unit.getShip();
		// bin ich auf Schiff
		if (s!=null){
			// bin ich kapitän
			Unit u = s.getOwnerUnit();
			if (u!=null){
				if (u.equals(this.unit)){
					// habe ich eine MOVE order
					if (this.hasOrder("NACH")){
						// ok, wir wollen prüfen....
						if (s.getModifiedLoad()>s.getMaxCapacity()){
							// Problem
							this.doNotConfirmOrders("!!! Schiff ÜBERLADEN !!!");
						}
					}
				}
			}
		}
	}
	
	/**
	 * überprüft, ob ein NACH Befehl vorhanden ist und die Einheit *NICHT* Kapitän eines Schiffes ist
	 * und ob sie dann überladen ist
	 */
	public void checkNACHOK(){
		if (this.hasOrder("NACH") || this.hasOrder("FOLGE EINHEIT")){
			// kein Kapitän
			boolean isCaptn = false;
			Ship s = this.unit.getShip();
			if (s!=null){
				// bin ich kapitän
				Unit u = s.getOwnerUnit();
				if (u!=null){
					if (u.equals(this.unit)){
						isCaptn=true;
					}
				}
			}
			if (!isCaptn){
				MovementEvaluator ME = this.unit.getData().getGameSpecificStuff().getMovementEvaluator();
				if (ME instanceof EresseaMovementEvaluator) {
					EresseaMovementEvaluator EMV = (EresseaMovementEvaluator) ME;
					if (!EMV.canWalk(this.unit)){
						// Problem
						this.doNotConfirmOrders("!!! Einheit ÜBERLADEN !!!");
						this.addComment("Load: " + EMV.getModifiedLoad(this.unit));
						this.addComment("Max: " + EMV.getPayloadOnFoot(this.unit));
					}
				}
			}	
		}
	}
	
	
	
	/**
	 * überprüft, ob ein langer befehl überhaupt vorhanden ist
	 *
	 */
	private void checkForLongOrder(){
		boolean may_confirm = false;
		// alle order durchlaufen
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order actOrder = (Order)iter.next();
			if (actOrder.isLong()){
				// jo..lange order
				may_confirm = true;
				// schleife kann verlassen werden, ein treffer reicht
				break;
			}
		}
		
		
		// Ozeancheck
		if (!may_confirm){
			// trifft nur zu, wenn auf Ozean und nicht MM
			Race mmRace = this.scriptMain.gd_ScriptMain.getRules().getRace("Meermenschen", false);
			RegionType ozeanRegionType = this.scriptMain.gd_ScriptMain.getRules().getRegionType("Ozean",false);
			Ship ship = this.unit.getShip();
			boolean isCaptain = false;
			if (ship!=null && ship.getOwnerUnit().equals(this.unit)){
				isCaptain=true;
			}
			if (mmRace != null && 
				  !mmRace.equals(this.unit.getRace()) &&
				  ozeanRegionType != null &&
				  ozeanRegionType.equals(this.unit.getRegion().getRegionType())
				  && !isCaptain){
				// kein MM
				// und auf dem Ozean
				// darf eh keinen langen Befehl ausführen
				// darf bestätigt werden
				may_confirm = true;
				this.addComment("Nicht-MM auf Ozean: bestätigt.");
			}
		}
		
		// MyMonster
		if (!may_confirm){
			if (this.isMyMonster()){
				may_confirm=true;
				this.addComment("Eigenes Monster: bestätigt");
			}
		}
		
		
		
		if (may_confirm){
			this.setUnitOrders_adjusted(true);
		} else {
			// im Zweifel hier abbrechen
			this.doNotConfirmOrders("KEIN langer Befehl erkannt!");
		}
	}
	
	
	
	public Unit getUnit(){
		if (this.unit==null) {return null;} else {return this.unit;}
	}
	
	
	// Vollstaendigkeit halber 
	public void setScriptMain(ScriptMain _scriptMain){
		this.scriptMain = _scriptMain;
	}
	
	public ScriptMain getScriptMain() {
		return this.scriptMain;
	}
	
	/**
	 * parst die orders nochmal und sucht nach optionalen konfig einträgen
	 * 
	 * 
	 */
	
	public void readReportSettings(){
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script setitemgroup")) {
				String s2 = s.substring(23);
				// ToDo: in ReportSettings verschieben
				StringTokenizer st = new StringTokenizer(s2);
				int i = 0;
				String catName = "";
				String itemTypeName = "";
				String stringPrio = "";
				 while (st.hasMoreTokens()) {
				     // println(st.nextToken());
					 switch (i){
					 case 0:catName = st.nextToken();break;
					 case 1:itemTypeName = st.nextToken();break;
					 case 2:stringPrio = st.nextToken();break;
					 }
					 i++;
				 }
			     int intPrio = Integer.parseInt(stringPrio);
			     
			     // platzhalter _ durch space ersetzen
				 itemTypeName = itemTypeName.replace("_", " ");
	 
			     reportSettings.setData(catName, itemTypeName, intPrio);
			}
			if (s.toLowerCase().startsWith("// script setscripteroption")) {
				String s2 = s.substring(28);
				reportSettings.parseOption(s2,this.getUnit(),true);
			}
			String findKey = "// script setscripteroption2 ";
			if (s.toLowerCase().startsWith(findKey)) {
				String s2 = s.substring(findKey.length());
				reportSettings.parseOption(s2,this.getUnit(),false);
			}
		}
	}
	/**
	 * Überprüft, ob angegebe Unit als gescriptet erkannt wird
	 * nur dann wird sie Grundlage einer ScriptUnit
	 * 
	 * @param u Die Unit
	 * @return wahr, wenn unit als gescriptet erkannt
	 */
	public static boolean isScriptUnit(Unit u){
		// orders nach // script einträgen durchsuchen
		for(Iterator<Order> iter = u.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script ")) { return true;}
		}

		return false;
	}
	
	/**
	 * Überprüft, ob angegebe Unit als Depot erkannt wird
	 * 
	 * @param u Die Unit
	 * @return wahr, wenn unit als gescriptet erkannt
	 */
	public static boolean isDepot(Unit u){
		// orders nach // script Depot einträgen durchsuchen
		for(Iterator<Order> iter = u.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script depot")) { return true;}
		}
		return false;
	}
	
	public boolean isDepot(){
		if (!this.setDepotStatus){
			this.isDepotUnit=ScriptUnit.isDepot(this.unit);
			this.setDepotStatus = true;
		}
		return this.isDepotUnit; 
	}
	
	/**
	 * Überprüft, ob angegebe Unit als Depot erkannt wird
	 * 
	 * @param u Die Unit
	 * @return wahr, wenn unit als gescriptet erkannt
	 */
	private boolean isMyMonster(){
		// orders nach // script Depot einträgen durchsuchen
		Unit u = this.getUnit();
		for(Iterator<Order> iter = u.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			if (s.toLowerCase().startsWith("// script mymonster")) { return true;}
		}
		return false;
	}
	
	/**
	 * Ersetzt umlaute
	 * alle buchstaben klein bis auf den ersten
	 * @param s
	 * @return
	 */
	private String cleanReportString(String s){
		
		if (s == null){
			return "";
		}
		
		String erg = s;
		//		 alles klein schreiben
	   	 erg = erg.toLowerCase();
	   	 // umlaute ersetzen
	   	 erg = erg.replaceAll("ö","oe");
	   	 erg = erg.replaceAll("ü","ue");
	   	 erg = erg.replaceAll("ä","ae");
	   	 erg = erg.replaceAll("ß","ss");
	   	 // erster Buchstabe gross
	   	 if (erg.length()>0){
	   		 erg = erg.substring(0,1).toUpperCase() + erg.substring(1);
	   	 }
		return erg;
	}
	/**
	 * Vereinfachung für den Aufruf aus anderen Scripten
	 * keine Parameter
	 * while running = true...
	 * @param scriptName
	 */
	public void findScriptClass(String scriptName){
		this.findScriptClass(scriptName, "");
	}
	
	
	/**
	 * Vereinfachung für den Aufruf aus anderen Scripten...
	 * Die Parameter können einfach übergeben werdeb
	 * ArrayList wird erstellt....
	 * @param scriptName
	 * @param args
	 */
	public void findScriptClass(String scriptName,String args){
		StringTokenizer st = new StringTokenizer(args);
		ArrayList<String> arguments = null;
	     while (st.hasMoreTokens()) {
	    	 if (arguments==null){
	    		 arguments = new ArrayList<String>(1);
	    	 }
    		 arguments.add(st.nextToken());
	     }
	     this.findScriptClass(scriptName, arguments,true);
	}
	
	/**
	 * Vereinfachung für den Aufruf aus anderen Scripten...
	 * Die Parameter können einfach übergeben werdeb
	 * ArrayList wird erstellt....
	 * @param scriptName
	 * @param args
	 */
	public void findScriptClass(String scriptName,String args,boolean whileRunning){
		StringTokenizer st = new StringTokenizer(args);
		ArrayList<String> arguments = null;
	     while (st.hasMoreTokens()) {
	    	 if (arguments==null){
	    		 arguments = new ArrayList<String>(1);
	    	 }
    		 arguments.add(st.nextToken());
	     }
	     this.findScriptClass(scriptName, arguments,whileRunning);
	}
	
	/**
	 * Versucht, angegebenes script als Klasse zu laden und 
	 * an die scriptList anzufügen
	 * 
	 * @param scriptName
	 */
	public void findScriptClass(String _scriptName,ArrayList<String> arguments,boolean whileRunning){
		
         // hier nun ne kleine Umsetzer-liste
		 // oder auch namenstransformation
		 // aktuelles Beispiel:
		 // tags(RegionDepot) soll auf // script Depot
		 // konvertiert werden
		 // machen wir mal ganz einfach..erstmal
		
		 if (_scriptName.equals("RegionDepot")){_scriptName = "Depot";}
	
		 // na dann mal paar Scriptsachen rausnehmen, die gar keine scriptsachen sind

		 if (_scriptName.equalsIgnoreCase("setitemgroup")) {return;}
		 if (_scriptName.equalsIgnoreCase("setScripterOption")) {return;}
		 
		 // zur Sicherheit nochmal den Namen clearen...
		 String scriptName = this.cleanReportString(_scriptName);
		 
		 Class<?> t = null;
	   	 Object o = null;
	   	 try {
	   		 t = Class.forName("com.fftools.scripts." + scriptName);
	   	 } catch (ClassNotFoundException e) {
	   		 outText.addOutLine("Klasse nicht gefunden: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   	 } catch (ExceptionInInitializerError e) {
	   		 outText.addOutLine("Klasse nicht initialisiert: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   	 } catch (LinkageError e) {
	   		 outText.addOutLine("Klasse nicht gelinkt: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   	 }
	   	 if (t!=null){
	   		 try {
	   			 o = t.newInstance();
	   		 } catch (IllegalAccessException e) {
	   			 outText.addOutLine("Konstruktor der Klasse nicht verfuegbar: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   		 } catch (InstantiationException e) {
	   			 outText.addOutLine("Klasse konnte nicht instanziert werden: " + scriptName + ": " + this.unit.toString(true) + " in " + this.unit.getRegion().getName());
	   		 }
	   	 }
	   	 if (o!=null) {
	   		 
	   		 // OK, wir haben einen Treffer .. ;-)
	   		 // Das Object auf Script.java casten
	   		 Script sc = (Script) o;
	   		 
	   		 // FF 20070103 beim Overlord registrieren
	   		 this.scriptMain.getOverlord().addOverlordInfo(sc);
	   		 
	   		 // checken, ob auf doppelte zu checken ist
	   		 if (!sc.allowMultipleScripts()){
	   			 // ok, wir müssen checken
	   			 // normale liste
	   			 if (this.foundScriptList!=null){
		   			 for (Iterator<Script> iter = this.foundScriptList.iterator();iter.hasNext();){
		   				 Script actSc = (Script) iter.next();
		   				 if (actSc.getClass().getName().equalsIgnoreCase(sc.getClass().getName())){
		   					 // Treffer...sowas haben wir schon
		   					 if (sc.errorMsgIfNotAllowedAddedScript()){
		   						 outText.addOutLine("info: nicht zugelassen weiterer eintrag von: " + sc.getClass().getName() + " bei " + this.getUnit().toString(true));
		   					 }
		   					 return;
						} 
		   			 }
	   			 }
	   			 // die besondere Liste addScr
	   			 if (this.addScriptList!=null){
		   			 for (Iterator<Script> iter = this.addScriptList.iterator();iter.hasNext();){
		   				 Script actSc = (Script) iter.next();
		   				 if (actSc.getClass().getName().equalsIgnoreCase(sc.getClass().getName())){
		   					 // Treffer...sowas haben wir schon
		   					 if (sc.errorMsgIfNotAllowedAddedScript()){
		   						 outText.addOutLine("info: nicht zugelassen weiterer eintrag von: " + sc.getClass().getName() + " bei " + this.getUnit().toString(true));
		   					 }	
		   					 return;
						} 
		   			 }
	   			 }
	   		 }
	   		 
	   		 // wichtige Daten uebergeben
	   		 sc.setScriptUnit(this);
	   		 // inkl Client, wenn wir einen haben
	   		 if (this.c!=null){sc.setClient(c);}
	   		 // aber GameData brauchen wir schon
	   		 if (this.scriptMain.gd_ScriptMain!=null){
	   			 sc.setGameData(this.scriptMain.gd_ScriptMain);
	   		 }
	   		 if (arguments!=null) {sc.setArguments(arguments);}
	   		 // Und nu anhaengen 
	   		 if (!whileRunning){
	   			if (foundScriptList==null) {
		   			 foundScriptList = new ArrayList<Script>(1);
		   		 }
	   			 foundScriptList.add(sc);
	   		 } else {
	   			 this.addAScript(sc);
	   		 }
	   	 }
	}

	

	/**
	 * @param unitOrders_adjusted the unitOrders_adjusted to set
	 */
	public void setUnitOrders_adjusted(boolean unitOrders_adjusted) {
		this.unitOrders_adjusted = unitOrders_adjusted;
	}
	
	/**
	 * Ersetzt gezielt eine script order durch die übergeben
	 * wirksam erst bei nächstem aufruf, da die script liste ja bereits gebaut worden ist
	 * Ob eine script order passend ist wird durch einen vergleich mit
	 * der new order erreicht, dazu wird angegeben, wieviele Zeichen verglichen werden sollen
	 * minimum dafür ist "X".length
	 * @param newOrder
	 * @param compareLength
	 */
	public boolean replaceScriptOrder(String newOrder,int compareLength){
		// vorhandenen script eintrag löschen
		if (this.removeScriptOrder(newOrder.substring(0,compareLength))) {
			// löschen war erfolgreich
			// neuen Bauen...via order
			this.getUnit().addOrder("// script " + newOrder,false, 1);
			// this.getUnit().reparseOrders();
			this.ordersHaveChanged();
			this.setUnitOrders_adjusted(true);
			// fertig.
			return true;
		} else {
			// löschen war nicht erfolgreich....zur Sicherheit false zurückgeben
			// und nicht ergänzen
			return false;
		}
	}
	
	/**
	 * löscht ein ein script order aus den orders der unit
	 * gelöscht werden alle orders, die nach // script  mit matchingChars beginnen
	 * not case sensitive
	 * @param matchingChars
	 */
	public boolean removeScriptOrder(String matchingChars){
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		ArrayList<Order> newComments = new ArrayList<Order>(1);
		boolean didSomething = false;
		for(Iterator<Order> iter = this.unit.getOrders2().iterator(); iter.hasNext();) {
			Order o = (Order) iter.next();
			String s = o.getText();
			boolean toDelete = false;
			if (s.startsWith("// script ")){
				String checkS = s.substring("// script ".length()).toLowerCase();
				String checkMatchS = matchingChars.toLowerCase();
				if (checkS.startsWith(checkMatchS)){
					toDelete = true;
					didSomething = true;
				}
			}
			if (!toDelete){
				newOrders.add(this.getUnit().createOrder(s));
			} else {
				// kleiner Kommentar, dass gelöscht?
				newComments.add(this.getUnit().createOrder("; script order entfernt: " + s + "  do_not_touch"));
			}
		}
		if (didSomething){
			newOrders.addAll(newComments);
			this.unit.setOrders2(newOrders);
			// this.unit.reparseOrders();
		}
		return didSomething;
	}
	
	/**
	 * Findet in unit in der gleichen Region und gibt sie zurück
	 * bzw null, wenn nicht gefunden
	 * @param unitDesc Numer der Unit (kein Temp...(?))
	 * @return
	 */
	public Unit findUnitInSameRegion(String unitDesc){
        //	Die Region mal holen
		Region actR = this.getUnit().getRegion();
		boolean found = false;
		Unit actU = null;
		// units durchlaufen
		for (Iterator<Unit> iter = actR.units().iterator();iter.hasNext();){
			actU = (Unit)iter.next();
			// und prüfen
			if (actU.toString(false).equalsIgnoreCase(unitDesc)){
				// ergänzung...und sich selbst ausschliessen
				if (!this.getUnit().toString(false).equals(actU.toString(false))) {
					found = true;
					break;
				}
			}
		}
		
		if (found){
			return actU;
		} else {
			return null;
		}
	}
	
	/**
	 * liefert freie Kapa, keine Angabe einer Benutzer-Kapa
	 * @param kapaPolicy
	 * @return
	 */
	public int getFreeKapa(int kapaPolicy) {
		return this.getFreeKapa(kapaPolicy, 0);
	}
	
	/**
	 * 
	 * @return the freeKapa
	 */
	public int getFreeKapa(int kapaPolicy, int userKapa) {
		
		if (this.originalFreeKapaFood==-1){
			// kapa war noch nicht gesetzt
			
			MovementEvaluator ME = scriptMain.gd_ScriptMain.getGameSpecificStuff().getMovementEvaluator(); 
			this.originalFreeKapaFood = (int)Math.floor((ME.getPayloadOnFoot(this.getUnit()) - ME.getModifiedLoad(this.getUnit()))/100);
			this.originalFreeKapaHorse = (int)Math.floor((ME.getPayloadOnHorse(this.getUnit()) - ME.getModifiedLoad(this.getUnit()))/100);
			this.originalFreeKapaUser = userKapa;
			this.originalModifiedLoad = (int)Math.ceil(ME.getModifiedLoad(this.getUnit())/100);
		}
			// test debug:
			// this.unit.getRegion().refreshUnitRelations(true);
			// nur die unit müsste reichen
			// this.unit.refreshRelations();
		switch (kapaPolicy){
		case MatPoolRequest.KAPA_unbenutzt:
			// eigentlich ein Fehler..wenn ich kapa nicht benutze
			// wozu brauche ich dann diese Angabe
			return -1;
		case MatPoolRequest.KAPA_max_zuFuss:
			int freeFood = this.originalFreeKapaFood - this.usedKapa;
			return Math.max(0, freeFood);
		case MatPoolRequest.KAPA_max_zuPferd:
			int freeHorse = this.originalFreeKapaHorse - this.usedKapa;
			return Math.max(0, freeHorse);
		case MatPoolRequest.KAPA_benutzer:
			int freeUser = this.originalFreeKapaUser - (this.originalModifiedLoad + this.usedKapa);
			return Math.max(0, freeUser);
		default:
			return -1;
		}
	}

	/**
	 * liefert wahr, wenn diese scriptUnit aktuell die 
	 * freeKapa schin berechnet hat
	 * @return
	 */
	public boolean freeKapaUsed(){
		boolean erg = false;
		if (this.originalFreeKapaFood!=-1){
			erg=true;
		}
		return erg;
	}
	
	
	
	/**
	 * reduziert die freie Kapa um den entsprechenden Betrag
	 * @param betrag
	 */
	public void decFreeKapa(int betrag){
		
		
		// erhöht die genutzte Kapa
		this.usedKapa+=betrag;
	}


	public void resetFreeKapa(){
		
		
		this.originalFreeKapaFood = -1;
		this.usedKapa = 0;
		// kapaPolicy auch auf unbenutzt setzen?! (vermutlich nicht...)
		
	}
	
	/**
	 * Fügt einen Befehl zur Scriptunit hinzu
	 * und markiert, dass sie durch FFTools bestätigt werden kann
	 * @param s der Befehl
	 * @param no_doubles wenn wahr, wird nur hinzugefügt, wenn nicht bereits eingleichlautender Befehl bei der Einheit existiert
	 */
	public void addOrder(String s,boolean no_doubles) {
		addOrder(s, no_doubles, true);
	}
	
	/**
	 * Fügt einen Befehl zur Scriptunit hinzu
	 * @param s der Befehl
	 * @param no_doubles wenn wahr, wird nur hinzugefügt, wenn nicht bereits eingleichlautender Befehl bei der Einheit existiert
	 * @param unit_may_confirm wenn wahr wird das Flag gesetzt, dass die Unit duch FfTools bestätigt werden KANN
	 */
	public void addOrder(String s,boolean no_doubles,boolean unit_may_confirm) {
		boolean add_ok = true;
		
		if (no_doubles){
			for (Iterator<Order> i1 = this.getUnit().getOrders2().iterator();(i1.hasNext() && add_ok);){
				Order o = (Order)i1.next();
				String s_old = o.getText();
				if (s_old.equalsIgnoreCase(s)) {add_ok = false;}
			}
		}
		if (add_ok){
			// this.getUnit().addOrder(s,false, 1);
			this.getUnit().addOrder(s);
			// this.getUnit().reparseOrders();
			this.ordersHaveChanged();
			if (unit_may_confirm){
				this.setUnitOrders_adjusted(true);
			}			
		} else {
			this.addComment("addOrder: wg double nicht ergänzt: " + s, false);
		}
	}

	
	public void addComment(String s){
		addComment(s, true);
	}
	
	public void addComment(String s,boolean no_doubles){
		boolean add_ok = true;
		
		if (no_doubles){
			for (Iterator<Order> i1 = this.getUnit().getOrders2().iterator();(i1.hasNext() && add_ok);){
				Order o = (Order)i1.next();
				String s_old = o.getText();
				if (s_old.equalsIgnoreCase("; " + s)) {add_ok = false;}
			}
		}
		if (add_ok){
			// this.getUnit().addOrder("; " + s,false, 1);
			this.getUnit().addOrder("; " + s);
			// this.getUnit().reparseOrders();
			this.ordersHaveChanged();
		}
	}

	/**
	 * @return the foundScriptList
	 */
	public ArrayList<Script> getFoundScriptList() {
		return foundScriptList;
	}
	
	public String unitDesc(){
		return this.getUnit().toString(true) + " in " + this.getUnit().getRegion().toString(); 
	}

	public Overlord getOverlord(){
		return this.scriptMain.getOverlord();
	}
	
	public String toString(){
		return this.unitDesc();
	}
	
	public String getUnitNumber(){
		return this.getUnit().toString(false);
	}
	
	/**
	 * liefert die Anzahl der Effekte oder 0
	 * @param Name
	 * @return
	 */
	public int getEffekte(String Name){
		int erg = 0;
		Unit u = this.getUnit();
		if (u.getEffects()!=null && u.getEffects().size()>0){
			for (Iterator<String> iter = u.getEffects().iterator();iter.hasNext();){
				String effect = (String)iter.next();
				String[] pairs = effect.split(" ");
				if (pairs.length>0 &&  pairs[1].equalsIgnoreCase(Name)){
					try {
						Integer am = Integer.parseInt(pairs[0]);
						return am.intValue();
					} catch (NumberFormatException e){
						this.addComment("!*!unerwarteter Wert für Trank im CR");
						outText.addOutLine("!*!unerwarteter Wert für Trank im CR:" + this.unitDesc(),true);
						return 0;
					}
					
				}
			}
		}
		return erg;
	}
	
	/**
	 * 
	 * @param TagName  Name des Tags
	 * @param TagValue Wert des Tags
	 */
	public void putTag(String TagName,String TagValue){
		this.getUnit().putTag(TagName, TagValue);
	}
	
	/**
	 * liefert (erstes) vorkommen eines scriptes
	 * einer bestimmten Klasse
	 * wird null übergeben, wird erstes (beliebiges)
	 * script zurückgegeben
	 * @param searchClass
	 * @return
	 */
	public Object getScript(Class<?> searchClass){
		// normale liste
		 if (this.foundScriptList!=null){
			 for (Iterator<Script> iter = this.foundScriptList.iterator();iter.hasNext();){
				 Script actSc = (Script) iter.next();
				 if (searchClass==null){
					 return actSc;
				 }
				 if (searchClass.isInstance(actSc)){
					 return actSc;
				} 
			 }
		  }
		 // die besondere Liste addScr
		 if (this.addScriptList!=null){
			 for (Iterator<Script> iter = this.addScriptList.iterator();iter.hasNext();){
				 Script actSc = (Script) iter.next();
				 if (searchClass==null){
					 return actSc;
				 }
				 if (searchClass.isInstance(actSc)){
					 return actSc;
				} 
			 }
		 } 
		return null;
	}

	/**
	 * @return the modifiedItemsMatPool2
	 */
	public HashMap<ItemType, Item> getModifiedItemsMatPool2() {
		return modifiedItemsMatPool2;
	}

	/**
	 * @param modifiedItemsMatPool2 the modifiedItemsMatPool2 to set
	 */
	public void setModifiedItemsMatPool2(
			HashMap<ItemType, Item> modifiedItemsMatPool2) {
		this.modifiedItemsMatPool2 = modifiedItemsMatPool2;
	}
	
	/**
	 * analog unit.getModifiedItem
	 * @param type : which ItemType
	 * @return the modified Item or NULL
	 */
	public Item getModfiedItemMatPool2(ItemType type){
		if (this.modifiedItemsMatPool2==null || this.modifiedItemsMatPool2.size()==0){
			return null;
		}
		return this.modifiedItemsMatPool2.get(type);
	}
	
	
	/**
	 * Fiete 20080305 taken from EresseaMovementElevator
	 * used by MatPool2 only!
	 * @return
	 */
	public int getPayloadOnHorse() {
		int capacity = 0;
		int horses = 0;
		Item i = null;
		/**
        Item i = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_HORSE));
			
		if(i != null) {
			horses = i.getAmount();
		}
		*/
		horses = countActHorses();
		if(horses <= 0) {
			return -1;
		}

		int skillLevel = 0;
		Skill s = this.unit.getModifiedSkill(new SkillType(StringID.create("Reiten")));

		if(s != null) {
			skillLevel = s.getLevel();
		}

		if(horses > (skillLevel * this.unit.getModifiedPersons() * 2)) {
			return -2;
		}

		int carts = 0;
		i = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_CART));

		if(i != null) {
			carts = i.getAmount();
		}

		int horsesWithoutCarts = horses - (carts * 2);

		Race race = getRace(this.unit);

		if(horsesWithoutCarts >= 0) {
			capacity = (((carts * 140) + (horsesWithoutCarts * 20)) * 100) -
					   (((int) ((race.getWeight()) * 100)) * unit.getModifiedPersons());
		} else {
			int cartsWithoutHorses = carts - (horses / 2);
			horsesWithoutCarts = horses % 2;
			capacity = (((((carts - cartsWithoutHorses) * 140) + (horsesWithoutCarts * 20)) -
					   (cartsWithoutHorses * 40)) * 100) -
					   (((int) ((race.getWeight()) * 100)) * unit.getModifiedPersons());
		}
		// Fiete 20070421 (Runde 519)
		// GOTS not active when riding! (tested)
		// return respectGOTS(unit, capacity);
		return capacity;
	}

	
	/**
	 * für den Matpool:
	 * anzahl modified
	 * wenn Depot in Region: minus noch offerierte Pferde
	 * Nur für MatPool2 !
	 * @return
	 */
	private int countActHorses(){
		int erg = 0;
		ItemType horseType = this.getScriptMain().gd_ScriptMain.getRules().getItemType("Pferd",false);
		Item i = this.getModfiedItemMatPool2(horseType);
		if(i != null) {
			erg = i.getAmount();
		}
		MatPool MP = this.getOverlord().getMatPoolManager().getRegionsMatPool(this);
		if (MP!=null){
			ArrayList<MatPoolOffer> offers = MP.getOffers(this);
			if (offers!=null && offers.size()>0){
				for (MatPoolOffer actOffer : offers){
					if (actOffer.getItemType().equals(horseType)) {
						erg -=actOffer.getAngebot();
					}
				}
			}
		}
		return erg;
	}
	
	
	/**
	 * Returns the maximum payload in GE  100 of this unit when it travels on foot. Horses, carts
	 * and persons are taken into account for this calculation. If the unit has a sufficient skill
	 * in horse riding but there are too many carts for the horses, the weight of the additional
	 * carts are also already considered. The calculation also takes into account that trolls can
	 * tow carts.
	 *
	 *
	 * @return the payload in GE  100, CAP_UNSKILLED if the unit is not sufficiently skilled in
	 * 		   horse riding to travel on horseback.
	 */
	@SuppressWarnings("deprecation")
	public int getPayloadOnFoot() {
		int capacity = 0;
		int horses = 0;
		Item i = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_HORSE));

		if(i != null) {
			horses = i.getAmount();
		}

		if(horses < 0) {
			horses = 0;
		}

		int skillLevel = 0;
		Skill s = this.unit.getModifiedSkill(new SkillType(StringID.create("Reiten")));

		if(s != null) {
			skillLevel = s.getLevel();
		}

		if(horses > ((skillLevel * this.unit.getModifiedPersons() * 4) + this.unit.getModifiedPersons())) {
			// too many horses
			return -1;
		}

		int carts = 0;
		i = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_CART));

		if(i != null) {
			carts = i.getAmount();
		}

		if(carts < 0) {
			carts = 0;
		}

		int horsesWithoutCarts = 0;
		int cartsWithoutHorses = 0;

		if(skillLevel == 0) {
			// can't use carts!!!
			horsesWithoutCarts = horses;
			cartsWithoutHorses = carts;
		} else if(carts > (horses / 2)) {
			// too many carts
			cartsWithoutHorses = carts - (horses / 2);
		} else {
			// too many horses (or exactly right number)
			horsesWithoutCarts = horses - (carts * 2);
		}

		Race race = getRace(this.unit);

		if((race == null) || (race.getID().equals(EresseaConstants.R_TROLLE) == false)) {
			capacity = (((((carts - cartsWithoutHorses) * 140) + (horsesWithoutCarts * 20)) -
					   (cartsWithoutHorses * 40)) * 100) +
					   (((int) (race.getCapacity() * 100)) * this.unit.getModifiedPersons());
		} else {
			int horsesMasteredPerPerson = (skillLevel * 4) + 1;
			int trollsMasteringHorses = horses / horsesMasteredPerPerson;

			if((horses % horsesMasteredPerPerson) != 0) {
				trollsMasteringHorses++;
			}

			int cartsTowedByTrolls = Math.min((this.unit.getModifiedPersons() - trollsMasteringHorses) / 4,
											  cartsWithoutHorses);
			int trollsTowingCarts = cartsTowedByTrolls * 4;
			int untowedCarts = cartsWithoutHorses - cartsTowedByTrolls;
			capacity = (((((carts - untowedCarts) * 140) + (horsesWithoutCarts * 20)) -
					   (untowedCarts * 40)) * 100) +
					   (((int) (race.getCapacity() * 100)) * (this.unit.getModifiedPersons() -
					   trollsTowingCarts));
		}

		return respectGOTS(this.unit, capacity);
	}

	private int respectGOTS(Unit unit, int capacity) {
		Item gots = this.getModfiedItemMatPool2(new ItemType(EresseaConstants.I_GOTS));

		if(gots == null) {
			return capacity;
		}

		int multiplier = Math.max(0, Math.min(unit.getModifiedPersons(), gots.getAmount()));
		Race race = getRace(unit);

		if((multiplier == 0) || (race == null)) {
			return capacity;
		}

		// increase capacity by 49*unit.race.capacity per GOTS
		return capacity + (multiplier * (49 * (int) (race.getCapacity() * 100)));
	}

	private Race getRace(Unit unit) {
		Race race = unit.getRace();

		if(unit.getDisguiseRace() != null) {
			race = unit.getDisguiseRace();
		}

		return race;
	}
	
	/**
	 * liefert freie Kapa, keine Angabe einer Benutzer-Kapa
	 * @param kapaPolicy
	 * @return
	 */
	public int getFreeKapaMatPool2(int kapaPolicy) {
		return this.getFreeKapaMatPool2(kapaPolicy, 0);
	}
	
	
	
	public int getModifiedLoad() {
		int load = this.getLoad();

		// also take care of passengers
		Collection<Unit> passengers = this.unit.getPassengers();
		MovementEvaluator ME = scriptMain.gd_ScriptMain.getGameSpecificStuff().getMovementEvaluator();

		for(Iterator<Unit> iter = passengers.iterator(); iter.hasNext();) {
			Unit passenger = (Unit) iter.next();
			// checken, ob das auch ne scriptUnit ist
			ScriptUnit passengerSU = this.getScriptMain().getScriptUnit(passenger);
			if (passengerSU==null){
				load += ME.getModifiedWeight(passenger);
			} else {
				load += passengerSU.getWeight();
			}
		}

		return load;
	}
	
	@SuppressWarnings("deprecation")
	private int getLoad() {
		int load = 0;
		ItemType horse = this.unit.getRegion().getData().rules.getItemType(EresseaConstants.I_HORSE);
		ItemType cart = this.unit.getRegion().getData().rules.getItemType(EresseaConstants.I_CART);
		if (this.modifiedItemsMatPool2!=null && this.modifiedItemsMatPool2.size()>0) {
			for(Iterator<Item> iter = this.modifiedItemsMatPool2.values().iterator(); iter.hasNext();) {
				Item i = (Item) iter.next();
	
				if(!i.getItemType().equals(horse) && !i.getItemType().equals(cart)) {
					// pavkovic 2003.09.10: only take care about (possibly) modified items with positive amount
					if(i.getAmount() > 0) {
						load += (((int) (i.getItemType().getWeight() * 100)) * i.getAmount());
					}
				}
			}
		}
		return load;
	}
	
	@SuppressWarnings("deprecation")
	private int getWeight(){
		int load = 0;
		// Pferde und Wagen ergänzen
		ItemType horse = this.unit.getRegion().getData().rules.getItemType(EresseaConstants.I_HORSE);
		ItemType cart = this.unit.getRegion().getData().rules.getItemType(EresseaConstants.I_CART);
		if (this.modifiedItemsMatPool2!=null && this.modifiedItemsMatPool2.size()>0) {
			for(Iterator<Item> iter = this.modifiedItemsMatPool2.values().iterator(); iter.hasNext();) {
				Item i = (Item) iter.next();
				if(i.getItemType().equals(horse) || i.getItemType().equals(cart)) {
					// pavkovic 2003.09.10: only take care about (possibly) modified items with positive amount
					if(i.getAmount() > 0) {
						load += (((int) (i.getItemType().getWeight() * 100)) * i.getAmount());
					}
				}
			}
		}
		// andere Items
		load+=this.getLoad();
		// Gewicht der Personen
		int raceWeight =(int)((this.unit.getDisguiseRace() != null) ? this.unit.getDisguiseRace().getWeight() : this.unit.getRace().getWeight());
		load+=raceWeight *  this.unit.getModifiedPersons() * 100;
		
		return load;
	}
	
	
	/**
	 * 
	 * @return the freeKapa
	 */
	public int getFreeKapaMatPool2(int kapaPolicy, int userKapa) {	
		
		if (this.getSetKapaPolicy()!=MatPoolRequest.KAPA_unbenutzt){
			kapaPolicy=this.getSetKapaPolicy();
		}
		
		
		if (this.originalFreeKapaUser<0 && userKapa>=0 && kapaPolicy==MatPoolRequest.KAPA_benutzer){
			this.originalFreeKapaUser = userKapa;
		}
		if (this.originalFreeKapaUserWeight<0 && userKapa>=0 && kapaPolicy==MatPoolRequest.KAPA_weight){
			this.originalFreeKapaUserWeight = userKapa;
		}
		
		// this.addComment("Debug: kapaweight=" + this.originalFreeKapaUserWeight);

		switch (kapaPolicy){
		case MatPoolRequest.KAPA_unbenutzt:
			// eigentlich ein Fehler..wenn ich kapa nicht benutze
			// wozu brauche ich dann diese Angabe
			return -1;
		case MatPoolRequest.KAPA_max_zuFuss:
			// int freeFood = this.originalFreeKapaFood - this.usedKapa;
			return (int)Math.floor(((double)this.getPayloadOnFoot() - (double)this.getModifiedLoad())/100);
		case MatPoolRequest.KAPA_max_zuPferd:
			return (int)Math.floor(((double)this.getPayloadOnHorse() - (double)this.getModifiedLoad())/100);
		case MatPoolRequest.KAPA_benutzer:
			return this.originalFreeKapaUser - (int)Math.ceil((double)this.getModifiedLoad()/100);
		case MatPoolRequest.KAPA_weight:
			if (!this.isIncludeSailorsWeight()){
				return this.originalFreeKapaUserWeight - (int)Math.ceil((double)this.getWeight()/100);
			} else {
				// Gewicht von weiteren Insassen auf dem Schiff miberechnen
				int erg = this.originalFreeKapaUserWeight;
				// wie gehabt, unser Gewicht abziehen
				erg -=  (int)Math.ceil((double)this.getWeight()/100);
				// Gewicht der anderen abziehen
				erg -= calcWeightOfSailors();
				return erg;
			}
		default:
			return -1;
		}
	}
	
	/**
	 * Berechnet das Gewicht der als Mitfahrer deklarierten Einheiten
	 * (// onboard oder // crew)
	 * @return
	 */
	private int calcWeightOfSailors(){
		int erg = 0;
		Ship ship = this.getUnit().getModifiedShip();
		if (ship==null){
			this.doNotConfirmOrders("Insassen des Schiffes sollen berücksichtigt werden, aber wir sind nicht auf einem Schiff!!");
			return 0;
		}
		// Liste der Insassen
		Collection<Unit> insassen = ship.modifiedUnits();
		if (insassen==null || insassen.isEmpty()){
			this.doNotConfirmOrders("!!! Insassen des Schiffes sollen berücksichtigt werden, aber es gibt keine Insassen!!");
			return 0;
		}
		for (Unit u:insassen){
			// eigene Unit ausschliessen
			if (!u.equals(this.getUnit())){
				// orders durchgehen und die richtige finden
				boolean isCrew = false;
				for (Order o:u.getOrders2()){
					String s = o.getText();
					if (s.toLowerCase().startsWith("// onboard")){
						isCrew=true;
						break;
					}
					if (s.toLowerCase().startsWith("// crew")){
						isCrew=true;
						break;
					}
				}
				if (isCrew){
					// scriptUnit finden
					ScriptUnit otherSC = this.scriptMain.getScriptUnit(u);
					if (otherSC==null){
						// ok, wir haben keine ScriptUnit...dann das Gewicht so
						// abziehen
						MovementEvaluator EME=this.scriptMain.gd_ScriptMain.getRules().getGameSpecificStuff().getMovementEvaluator();
						int unitWeight = (int)Math.ceil(EME.getModifiedWeight(u)/100);
						erg+=unitWeight;
						this.addComment("Crew " + u.toString(true) + " ist keine ScriptUnit, das Gewicht wurde mit " + unitWeight + " GE calculiert.");
					} else {
						// ganz normale ScriptUnit
						int unitWeight = (int)Math.ceil((double)otherSC.getWeight()/100);
						erg+=unitWeight;
						this.addComment("Crew " + u.toString(true) + " wurde mit " + unitWeight + " GE calculiert.");
						otherSC.addComment("als Crew von " + ship.toString() + " berücksichtigt.");
					}
				} else {
					// Wir haben eine Unit an Board, die nicht Crew ist...
					// dass kann schief gehen
					this.doNotConfirmOrders("!!! Hinweis: " + u.toString(true) + " ist an Bord und wurde nicht bei Kapa-Berechnung berücksichtigt.");
				}
			}
		}
		return erg;
	}
	
	
	/**
	 * Setzt eine Änderung der Items um
	 * @param type Welcher ItemType
	 * @param amount Positiv: zunahme, Negativ: abnahme
	 */
	public void changeModifiedItemsMatPools(ItemType type, int amount){
		if (this.modifiedItemsMatPool2==null){
			if (amount>0){
				this.modifiedItemsMatPool2 = new HashMap<ItemType, Item>();
			} else {
				// hier wohl eigentlich blöder fehler
				// nix da und trotzdem abziehen
				outText.addOutLine("!!!Versuch, einen nicht vorhanden Typ in der Menge zu reduzieren: " + unitDesc(), true);
				return;
			}
		}
		Item item = this.modifiedItemsMatPool2.get(type);
		if (item==null){
			// noch nicht vorhanden
			if (amount<0){
				// problem!
				outText.addOutLine("!!!Versuch, einen nicht vorhanden Typ in der Menge zu reduzieren: " + unitDesc(), true);
			} else {
				this.modifiedItemsMatPool2.put(type,new Item(type,amount));
			}
		} else {
			item.setAmount(item.getAmount() + amount);
		}
	}
	
	/**
	 * je nach MatPoolversion wird modifiedItems geliefert
	 * @param itemType
	 * @return
	 */
	public Item getModifiedItem(ItemType itemType){
		return this.getModfiedItemMatPool2(itemType);
	}

	/**
	 * @return the setKapaPolicy
	 */
	public int getSetKapaPolicy() {
		return setKapaPolicy;
	}

	/**
	 * @param setKapaPolicy the setKapaPolicy to set
	 */
	public void setSetKapaPolicy(int setKapaPolicy) {
		this.setKapaPolicy = setKapaPolicy;
	}

	public boolean isGibNix() {
		return gibNix;
	}

	public void setGibNix(boolean gibNix) {
		this.gibNix = gibNix;
	}
	
	/**
	 * 
	 * @param TalentName
	 * @return
	 */
	public int getSkillLevel(String TalentName){
		int erg=0;
		
		SkillType sT = this.scriptMain.gd_ScriptMain.getRules().getSkillType(TalentName, false);
		if (sT!=null){
			Skill skill = this.unit.getModifiedSkill(sT);
			if (skill!=null){
				erg=skill.getLevel();
			}
		}
		return erg;
	}
	
	public int getSkillLevelNoRegionTypeBonus(String TalentName){
		int erg=0;
		
		erg = getSkillLevel(TalentName);
		// Betrachtung nur, wenn es sich um Insekten in Gletscher oder Gebirge handelt
		if (this.getUnit().getRace().getName().equalsIgnoreCase("Insekten")) {
			if (this.getUnit().getRegion().getRegionType().getName().equalsIgnoreCase("Gletscher") || 
					this.getUnit().getRegion().getRegionType().getName().equalsIgnoreCase("Berge")) {
				erg = erg + 1;
			}
		}
		return erg;
	}
	
	/**
	 * prüft, ob Tags für die Scripte definiert sind und setzt diese ggf, wenn
	 * noch nicht vorhanden
	 */
	public void autoTags(){
		if (reportSettings.getOptionBoolean("useReportTags",this.unit.getRegion()) && this.foundScriptList!=null){
			for (Script s:this.foundScriptList){
				String searchOption="tag1_" + s.getClass().getSimpleName().toLowerCase();
				String reportOptionString = reportSettings.getOptionString(searchOption,this.unit.getRegion());
				if (reportOptionString!=null){
					// erster Buchstabe groß
					reportOptionString = reportOptionString.substring(0,1).toUpperCase() + reportOptionString.substring(1).toLowerCase();
					String actTag = this.unit.getTag(CRParser.TAGGABLE_STRING);
					if (actTag==null || !actTag.equals(reportOptionString)){
						this.unit.putTag(CRParser.TAGGABLE_STRING, reportOptionString);
						this.addComment("autoTag gesetzt auf :" + reportOptionString);
					} else {
						this.addComment("autoTag ist bereits auf:" + reportOptionString);
					}
				} else {
					// this.addComment("kein autotag eintrag für:" + searchOption);
				}
			}
		} else {
			// this.addComment("autoTag deactiviert");
		}
	}
	
	/**
	 * setzt die DELs von der delScriptList um
	 */
	public void processScriptDeletions(){
		if (this.delScriptList==null || this.delScriptList.size()==0){
			return;
		}
		for (Script s : this.delScriptList){
			this.foundScriptList.remove(s);
		}
		this.delScriptList.clear();
	}
	
	
	/**
	 * 
	 * @return mainDurchlauf beim Overlord
	 */
	public int getMainDurchlauf(){
		return this.getOverlord().getMainDurchlauf();
	}

	/**
	 * @return the includeSailorsWeight
	 */
	public boolean isIncludeSailorsWeight() {
		return includeSailorsWeight;
	}

	/**
	 * @param includeSailorsWeight the includeSailorsWeight to set
	 */
	public void setIncludeSailorsWeight(boolean includeSailorsWeight) {
		this.includeSailorsWeight = includeSailorsWeight;
	}

	public int getRecruitedPersons() {
		return recruitedPersons;
	}

	public void setRecruitedPersons(int recruitedPersons) {
		this.recruitedPersons = recruitedPersons;
	}
	
	public void incRecruitedPersons(int recruitedPersons) {
		this.recruitedPersons += recruitedPersons;
	}
	
	public void checkOverallCommand() {
		String s = reportSettings.getOptionString("allUnitOrder", this.getUnit().getRegion());
		if (s!=null && s.length()>0) {
			s= s.replace("_"," ");
			this.addOrder(s + " ; scripterOption allUnitOrder", true);
		}
	}
	
	public boolean isInsekt() {
		if (this.getUnit().getRace().getName().equalsIgnoreCase("Insekten")) {
			return true;
		}
		return false;
	}
	
	
}
