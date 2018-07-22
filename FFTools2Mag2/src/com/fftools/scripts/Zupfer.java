package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Faction;
import magellan.library.Message;
import magellan.library.Skill;
import magellan.library.rules.SkillType;

public class Zupfer extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 51;
	private int[] runners = {Durchlauf_1};
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Zupfer() {
		super.setRunAt(this.runners);
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
		if (scriptDurchlauf==this.Durchlauf_1){
			this.start();
		}

	}
	
	
	
	/**
	 * ToDo: Comment
	 */
	private void start(){
		// Eigene Talentstufe ermitteln
		int skillLevel = 0;
		SkillType skillType = this.gd_Script.getRules().getSkillType("Kräuterkunde", false);
		if (skillType!=null){
			Skill skill = this.scriptUnit.getUnit().getModifiedSkill(skillType);
			if (skill!=null){
				skillLevel = skill.getLevel();
			} else {
				skillLevel=0;
			}
		} else {
			this.addComment("!!! can not get SkillType Kräuterkunde!");
		}
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Zupfer");
		int unitMinLevel = OP.getOptionInt("minTalent", 5);
		int menge = OP.getOptionInt("menge", 5);
		this.addComment("erkanntes minTalent: " + unitMinLevel + ", erkannte Menge " + menge);
		
		// finde Messages, wieviele Kräuter vorher gefunden wurden...
		// wenn unter menge, hinweis bzw selbstständig reduzieren
		int letzteProd=-1;
		Faction f = this.getUnit().getFaction();
		if ((f.getMessages() != null) && (f.getMessages().size() > 0)) {
			Iterator<Message> iter = f.getMessages().iterator();
			
		    while (iter.hasNext() == true) {
		    	Message m = iter.next();
		    	boolean validM=false;
		    	if (m.getAttributes() != null) {
		            Iterator<String> iter2 = m.getAttributes().values().iterator();
		            while (iter2.hasNext()) {
		              try {
		                int i = Integer.parseInt(iter2.next());

		                // it would be cleaner to compare UnitID
		                // objects here but that's too expensive
		                if ((this.getUnit().getID()).intValue() == i) {
		                	validM=true;
		                }
		              } catch (NumberFormatException e) {
		              }
		            }
		          }
		    	
		    	if (validM) {		    	
			    	this.addComment("untersuche Nachricht: " + m.getText());
			    	/*
			    	MESSAGETYPE 861989530
			    	"\"$unit($unit) in $region($region) kann keine Kräuter finden.\"";text
			    	*/
			    	if (m.getMessageType().getID().intValue()==861989530) {
			    		letzteProd=0;
			    		this.addComment("Nachricht gefunden MESSAGETYPE 861989530 ");
			    	}
			    	
			    	/*
			    	 * MESSAGETYPE 1233714163
						"\"$unit($unit) in $region($region): '$order($command)' - Es sind keine Kräuter zu finden.\"";text
			    	 * 
			    	 */
			    	if (m.getMessageType().getID().intValue()==1233714163) {
			    		letzteProd=0;
			    		this.addComment("Nachricht gefunden MESSAGETYPE 1233714163 ");
			    	}
			    	/*
			    	MESSAGETYPE 1511758069
			    	"\"$unit($unit) in $region($region) findet $int($amount) $resource($herb,$amount).\"";text
			    	*/
			    	if (m.getMessageType().getID().intValue()==1511758069) {
			    		this.addComment("Nachricht gefunden MESSAGETYPE 1511758069 (xxx findet)");
			    		Map<String,String> map = m.getAttributes();
			    		if (map.containsKey("amount")) {
			    			String anzahlStr = map.get("amount");
			    			letzteProd = Integer.valueOf(anzahlStr);
			    			this.addComment("Nachricht enthält  Wert für die Menge: " + letzteProd);
			    		} else {
			    			this.addComment("Nachricht enthält keinen Wert für die Menge");
			    		}
			    	}
		    	}
		    }
		} else {
			this.addComment("Einheit hat keine Nachrichten");
		}
		
		if (letzteProd>=0) {
			this.addComment("Produktion letzte Runde: " + letzteProd);
		} else {
			this.addComment("keine Information über Produktion letzte Runde");
		}
		
		if (skillLevel<unitMinLevel){
			// Lernen
			this.addComment("Ergänze Lernfix Eintrag mit Talent=Kräuterkunde");
			Script L = new Lernfix();
			ArrayList<String> order = new ArrayList<String>();
			order.add("Talent=Kräuterkunde");
			L.setArguments(order);
			L.setScriptUnit(this.scriptUnit);
			L.setGameData(this.gd_Script);
			if (this.scriptUnit.getScriptMain().client!=null){
				L.setClient(this.scriptUnit.getScriptMain().client);
			}
			this.scriptUnit.addAScript(L);
		} else {
			// Zupfen
			if (letzteProd>=0) {
				// wenn wir mehr als 1 Stück weg liegen vom Soll, dann Meldung
				if (menge==letzteProd) {
					// alles schön, weitermachen
					this.addOrder("machen " + menge + " Kräuter ;(script Zupfer, Prod der letzten Runde OK)", true);
				}
				if (menge==(letzteProd + 1)) {
					// Schwankung um 1 - 1 Runde aussetzen, Ausdauer Lernen
					this.addOrder("Lerne Ausdauer ;(script Zupfer, Produktionsschwankung)", true);
				}
				if (menge>=(letzteProd + 2)) {
					// Deutliche Abweichung, manueller Eingriff erforderlich
					this.addOrder("Lerne Ausdauer ;(script Zupfer, Produktionsausfall)", true);
					this.addComment("!!! Zupfer - Produktionsausfall!!!");
					this.doNotConfirmOrders();
				}
			} else {
				// ohne letzte Prod machen wir standard
				this.addOrder("machen " + menge + " Kräuter ;(script Zupfer, ohne Prod der letzten Runde)", true);
			}

		}
	}

}
