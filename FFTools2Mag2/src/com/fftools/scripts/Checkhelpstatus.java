package com.fftools.scripts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import magellan.library.Alliance;
import magellan.library.EntityID;
import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.Group;
import magellan.library.Order;
import magellan.library.Unit;

import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;




public class Checkhelpstatus extends Script{
	
	private static final String ENEMY_FACTION_LIST_IDENTIFIER = "// EnemyFaction=";
	private static final String FRIENDLY_FACTION_LIST_IDENTIFIER = "// FriendlyFaction=";
	private static final String SILVER_FACTION_LIST_IDENTIFIER = "// SilverFaction=";
	private static final String GUARD_FACTION_LIST_IDENTIFIER = "// GuardFaction=";
	private static final String BROTHERS_FACTION_LIST_IDENTIFIER = "// BrothersInArmsFaction=";
	
	private static final String CHECK_GROUP_LIST_IDENTIFIER = "// checkgroup="; 
	
	private ArrayList<String> enemyFactionList = new ArrayList<String>();
	private ArrayList<String> friendlyFactionList = new ArrayList<String>();
	private ArrayList<String> silverFactionList = new ArrayList<String>();
	private ArrayList<String> guardFactionList = new ArrayList<String>();
	private ArrayList<String> brothersFactionList = new ArrayList<String>();
	
	private Map<String,ArrayList<String>> factiongroups = new HashMap<String,ArrayList<String>>();
	
	private static final int Durchlauf = 161;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Checkhelpstatus() {
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
	
	// Fiete: Load all enemy factions from orders 
    // order format: // EnemyFaction:abcd
    // not case sensitive, abcd is added
    private void getFactionsFromOrders(ArrayList<String> myList, String id, String ListName){
    	int cnt = 0;
    	GameData gd = this.gd_Script;
    	if (gd.getUnits()!=null && gd.getUnits().size()>0){
    		for (Unit u:gd.getUnits()){
    			// Fiete 20161119: ignore factions berücksichtigen
    			
    			boolean ignore = false;
				if (this.scriptUnit.getScriptMain().ignoreList != null){	
					for (String ss : this.scriptUnit.getScriptMain().ignoreList){
						if (u.getFaction().getID().toString().equalsIgnoreCase(ss)){
							ignore=true;
							break;
						}
					}
				}
    			if (u.getOrders2()!=null && u.getOrders2().size()>0 && !ignore){
    				for (Order order:u.getOrders2()){
    					String orderString = order.getText();
    					if (orderString.toUpperCase().startsWith(id.toUpperCase())){
    						// Treffer...
    						// faction number extrahieren
    						// Format
    						// ID=number {something}
    						String f_number=orderString.substring(id.length());
    						// nach erstem Leerzeichen abschneiden
    						int iPos = f_number.indexOf(" ");
    						if (iPos>1){
    							f_number = f_number.substring(0, iPos);
    						}
    						if (f_number.length()>0){
    							if (!myList.contains(f_number)){
    								myList.add(f_number);
	    							this.addComment("Checkhelpstatus ("+ ListName + "): added a Faction from orders of " + u.toString() + ": " + f_number);
	    							cnt++;
	    							
    							} else {
    								this.addComment("Checkhelpstatus ("+ ListName + "): Faction from orders of " + u.toString() + ": " + f_number +  " -> already on list");
    							}
    						} else {
    							this.addComment("Checkhelpstatus ("+ ListName + "): Error while inspecting orders of " + u.toString());
    						}
    					}
    				}
    			}
    		}
    	}
    	this.addComment("Checkhelpstatus: read " + cnt + " " + ListName + " from orders");
    }
	
    
    private void getCheckGroupsFromOrders(Map<String,ArrayList<String>> myMap, String id, String ListName){
    	int cnt = 0;
    	GameData gd = this.gd_Script;
    	if (gd.getUnits()!=null && gd.getUnits().size()>0){
    		for (Unit u:gd.getUnits()){
    			// Fiete 20161119: ignore factions berücksichtigen
    			
    			boolean ignore = false;
				if (this.scriptUnit.getScriptMain().ignoreList != null){	
					for (String ss : this.scriptUnit.getScriptMain().ignoreList){
						if (u.getFaction().getID().toString().equalsIgnoreCase(ss)){
							ignore=true;
							break;
						}
					}
				}
    			if (u.getOrders2()!=null && u.getOrders2().size()>0 && !ignore){
    				for (Order order:u.getOrders2()){
    					String orderString = order.getText();
    					if (orderString.toUpperCase().startsWith(id.toUpperCase())){
    						// Treffer...
    						// faction number extrahieren
    						// Format
    						// ID=number {something}
    						String f_number=orderString.substring(id.length());
    						String groupName = "";
    						// Format: Faction:GroupName
    						int iPos = f_number.indexOf(":");
    						if (iPos>0){
    							groupName = f_number.substring(iPos+1).trim();
    							f_number = f_number.substring(0, iPos).trim();
    							this.addComment("checkgroup erkannt für Partei " + f_number + ", Gruppe " + groupName);
    							if (f_number.length()>0 && groupName.length()>0){
        							if (myMap.containsKey(f_number)){
        								// es exitisert schon ein Eintrag für die faction
        								ArrayList<String> actList = myMap.get(f_number);
        								if (actList.contains(groupName)){
        									// schon drauf
        									this.addComment("checkgroup: faction-eintrag bereits vorhanden: faction=" + f_number + ", Gruppe=" + groupName);
        								} else {
        									// ergänzen
        									actList.add(groupName);
        									this.addComment("checkgroup: faction-eintrag ergänzt: faction=" + f_number + ", Gruppe=" + groupName);
        									cnt++;
        								}
        							} else {
        								// es existiert noch kein Eintrag, also legen wir einen an
        								ArrayList<String> actList = new ArrayList<String>();
        								actList.add(groupName);
        								myMap.put(f_number, actList);
        								this.addComment("checkgroup: faction-eintrag neu hinzugefügt: faction=" + f_number + ", Gruppe=" + groupName);
        								cnt++;
        							}
        						} else {
        							this.addComment("checkgroup ("+ ListName + "): Error while inspecting orders of " + u.toString()+ ", faction number or groupname to short");
        						}
    						} else {
    							this.addComment("!!! checkhelpstatus-checkgroup: kein : gefunden!!!");
    							this.doNotConfirmOrders();
    						}
    						
    					}
    				}
    			}
    		}
    	}
    	this.addComment("Checkhelpstatus: read " + cnt + " " + ListName + " from orders");
    }
    
    private ScriptUnit findFirstScriptUnit(Faction f, String GroupName){
    	ScriptMain sm = this.scriptUnit.getScriptMain();
    	for (ScriptUnit su : sm.getScriptUnits().values()){
    		if (su.getUnit().getFaction().equals(f)){
    			Unit u = su.getUnit();
    			boolean isInGroup=false;
    			for (Group g:f.getGroups().values()){
    				if (GroupName=="" || !g.getName().equalsIgnoreCase(GroupName)){
	    				if (g.units().contains(u)){
	    					isInGroup=true;
	    					break;
	    				}
    				}
    			}
    			if (!isInGroup){
    				return su;
    			}
    		}
    	}
    	return null;
    }
    
	
    private void checkStatus(Faction f, int AllianceID, ArrayList<String> factions, String OrderText, String ListName){
    	String FactionNumber = f.getID().toString().toLowerCase();
    	// gibt es einen Eintrag in check groups
    	if (this.factiongroups!=null && this.factiongroups.containsKey(FactionNumber)){
    		// die Gruppen abarbeiten
    		ArrayList<String> groupNames = this.factiongroups.get(FactionNumber);
    		for (String groupName:groupNames){
    			for (Group g:f.getGroups().values()){
    				if (g.getName().equalsIgnoreCase(groupName)){
    					// Treffer
    					Map<EntityID,Alliance> allies = g.allies();
    					checkStatusOfAllianceMap(allies, f, groupName, AllianceID, factions, OrderText, ListName);
    				}
    			}
    		}
    	} else  {
    		// die Partei abarbeiten 
    		Map<EntityID,Alliance> allies = f.getAllies();
    		checkStatusOfAllianceMap(allies, f, "", AllianceID, factions, OrderText, ListName);
    	}
    }
    
    private void checkStatusOfAllianceMap(Map<EntityID,Alliance> allies, Faction f, String GroupName, int AllianceID, ArrayList<String> factions, String OrderText, String ListName){
    	ArrayList<String> processedFactions = new ArrayList<String>();
    	String FactionName = f.toString();
    	if (GroupName.length()>1){
    		FactionName = FactionName.concat(" (Gruppe " + GroupName + ")");
    	}
    	if (allies!=null){
    		// durch alle Alliancen der Faction f
    		for (Alliance actA:allies.values()){
    			// durch alle Einträge der Faction-Liste
    			// this.addComment("Checking relation from "+f.getID().toString() +" to :" + actA.getFaction().getID().toString() + " (actual: "+ actA.toString() +")",false);
    			for (String s:factions){
    				// Status zu sich selber ausschliessen
    				if (!f.getID().toString().equalsIgnoreCase(s)){
	    				String actOrder = OrderText.replace("{faction}", s);
	    				if (s.equalsIgnoreCase(actA.getFaction().getID().toString())){
	    					// Bingo!
	    					processedFactions.add(s);
	    					if (!actA.getState(AllianceID) || AllianceID==0){
	    						// Hier mmuss was getan werden, der Status stimmt nicht
	    						// Irgendeine Unit finden, die in keiner Gruppe ist....und script unit ist!
		    					ScriptUnit su = findFirstScriptUnit(f,GroupName);
		    					if (su==null){
		    						this.addComment("!!! " + FactionName + " kann kein Befehl gegeben werden - keine Scriptnit gefunden.!!!");
		    						this.addComment("-> "+FactionName + " braucht die Order: " + actOrder + ", jetziger Status: " + actA.toString(),false);
		    					} else {
		    						this.addComment("Der Befehl wird " + su.getUnit().toString(true) + " gegeben.");
		    						this.addComment("-> "+FactionName + " bekommt die Order: " + actOrder + ", jetziger Status: " + actA.toString(),false);
		    						su.addOrder(actOrder + " ; Checkhelpstatus (" + ListName + ") from " + this.scriptUnit.getUnit().toString(), false);
		    					}
	    					} else {
	    						// this.addComment("OK: Status from " + f.toString() + " to " + s + " is: " + actA.toString() + " (" + actA.getState() + ")",false);
	    					}
	    				}
    				}
    			}
    		}
    	} else {
    		this.addComment("?!! Faction has no alliances!!: " + FactionName);
    	}
    	
    	if (AllianceID>0){
    		// alle factions durchgehen, ob sie auch erwischt worden sind, denn wenn nicht, entsprechenden befehl geben!
    		for (String s:factions){
    			if (!processedFactions.contains(s) && !f.getID().toString().equalsIgnoreCase(s)){
    				// hier haben wir das problem
    				String actOrder = OrderText.replace("{faction}", s);
    				ScriptUnit su = findFirstScriptUnit(f,GroupName);
					if (su==null){
						this.addComment("!!! " + FactionName + " kann kein Befehl gegeben werden - keine Scriptnit gefunden.!!!");
						this.addComment("-> "+FactionName + " braucht die Order: " + actOrder + ", jetziger Status: keine Allianz!",false);
					} else {
						this.addComment("*** für " + FactionName + " wird der Befehl " + su.getUnit().toString(true) + " gegeben.");
						this.addComment("-> "+FactionName + " bekommt die Order: " + actOrder + ", jetziger Status: keine Allianz!",false);
						su.addOrder(actOrder + " ; Checkhelpstatus (" + ListName + ") from " + this.scriptUnit.getUnit().toString(), false);
					}
    			}
    		}
    	}
    }
    
	private void scriptStart(){
		// zuerst checken, ob wir nicht schon am Start waren
		if (super.scriptUnit.getScriptMain().singleInstanceScriopts.contains(this)){
			this.addComment("!!! kein weiterer Aufruf von Checkhelpstatus zugelassen!!! -> unconfirmed");
			this.doNotConfirmOrders();
			return ;
		}
		
		// dann jetzt eintragen
		super.scriptUnit.getScriptMain().singleInstanceScriopts.add(this);
		
		// Faction-Listen einlesen
		getFactionsFromOrders(enemyFactionList, ENEMY_FACTION_LIST_IDENTIFIER, "EnemyFaction");
		getFactionsFromOrders(friendlyFactionList, FRIENDLY_FACTION_LIST_IDENTIFIER, "FriendlyFaction");
		getFactionsFromOrders(silverFactionList, SILVER_FACTION_LIST_IDENTIFIER, "SilverFaction");
		getFactionsFromOrders(guardFactionList, GUARD_FACTION_LIST_IDENTIFIER, "GuardFaction");
		getFactionsFromOrders(brothersFactionList, BROTHERS_FACTION_LIST_IDENTIFIER, "BrothersInArmsFaction");
		
		getCheckGroupsFromOrders(factiongroups, CHECK_GROUP_LIST_IDENTIFIER, "CheckGroupFaction");
		
		// alle Fractionen mit Passwort durchgehen
		GameData gd = this.gd_Script;
		for (Faction f:gd.getFactions()){
			if (f.getPassword()!=null && f.getPassword().length()>2){
				// Enemies
				// this.addComment("checking trusty faction " + f.toString());
				checkStatus(f, 0, enemyFactionList, "HELFEN {faction} ALLES NICHT","EnemyFaction");
				// Friendly...bekommen "alles" = "all" = 59
				checkStatus(f,59,friendlyFactionList, "HELFEN {faction} ALLES","FriendlyFaction");
			}
		}
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
