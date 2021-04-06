package com.fftools.scripts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsUnits;

import magellan.library.Faction;
import magellan.library.Message;
import magellan.library.Skill;
import magellan.library.rules.SkillType;

public class Zupfer extends MatPoolScript{
	
	
	
	int Durchlauf_1 = 51;
	private int[] runners = {Durchlauf_1};
	
	private int MindestZupfBestandProzent=40;
	
	
	
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
		super.addVersionInfo();
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
		OP.addOptionList(this.getArguments());
		int neuerMindestZupfBestandProzent = OP.getOptionInt("MindestZupfBestandProzent", 0); 
		if (neuerMindestZupfBestandProzent>0 && neuerMindestZupfBestandProzent<101) {
			this.addComment("neuer Wert für MindestZupfBestandProzent erkannt, ändere von " + MindestZupfBestandProzent + "% auf " + neuerMindestZupfBestandProzent + "%");
			this.MindestZupfBestandProzent = neuerMindestZupfBestandProzent;
		}
		
		
		int unitMinLevel = OP.getOptionInt("minTalent", 5);
		int menge = OP.getOptionInt("menge", 0);
		int testMenge = 5;
		String talent=OP.getOptionString("LernTalent");
		if (talent.length()<2) {
			talent=OP.getOptionString("Talent");
		}
		SkillType LernTalentSkillType = null;
		
		if (talent.length()>1) {
			talent = talent.substring(0, 1).toUpperCase() + talent.substring(1).toLowerCase();
			LernTalentSkillType = super.gd_Script.getRules().getSkillType(talent);
			if (LernTalentSkillType == null) {
				super.scriptUnit.doNotConfirmOrders ("!!! ungueltiges Zupfer-Lerntalent bei " + this.unitDesc());
				addOutLine("!!! ungueltiges Lerntalent bei " + this.unitDesc());
			}
		}
		
		this.addComment("erkanntes minTalent: " + unitMinLevel + ", erkannte vorgegebene Menge " + menge);
		
		if (LernTalentSkillType!=null) {
			this.addComment("erkanntes LernTalent: " + LernTalentSkillType.toString());
		}
		
		
		String GratisTalent=OP.getOptionString("GratisTalent");
		
		if (GratisTalent.length()<=2) {
			GratisTalent = reportSettings.getOptionString("ZupferGratisTalent", this.scriptUnit.getUnit().getRegion());
			if (GratisTalent==null) {
				GratisTalent="";
			}
		}
		
		
		SkillType GratisTalentSkillType = null;
		
		if (GratisTalent.length()>1) {
			GratisTalent = GratisTalent.substring(0, 1).toUpperCase() + GratisTalent.substring(1).toLowerCase();
			GratisTalentSkillType = super.gd_Script.getRules().getSkillType(GratisTalent);
			if (GratisTalentSkillType == null) {
				super.scriptUnit.doNotConfirmOrders ("!!! ungueltiges Zupfer-Gratistalent bei " + this.unitDesc());
				addOutLine("!!! ungueltiges Gratistalent bei " + this.unitDesc());
			}
		}
		
		if (GratisTalentSkillType!=null) {
			this.addComment("erkanntes GratisTalent: " + GratisTalentSkillType.toString());
		}
		
		
		
		String LernPlan = OP.getOptionString("Lernplan");
		if (LernPlan.length()>1) {
			AusbildungsRelation AR = super.getOverlord().getLernplanHandler().getAusbildungsrelation(this.scriptUnit, LernPlan);
			if (AR==null) {
				this.scriptUnit.doNotConfirmOrders("!!! Lernplan liefert keine Aufgabe mehr");
				// default ergänzen - keine Ahnung, was, eventuell kan
				// die einheit ja nix..
				LernPlan = "";
			}
		}
		
		if (LernPlan.length()>1) {
			this.addComment("erkannter LernPlan: " + LernPlan);
		}
		
		
		
		
		int regionsBestand = -1; // in Prozent, -1 = unbekannt
		
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
			    	// this.addComment("untersuche Nachricht: " + m.getText());
			    	/*
			    	MESSAGETYPE 861989530
			    	"\"$unit($unit) in $region($region) kann keine Kräuter finden.\"";text
			    	*/
			    	if (m.getMessageType().getID().intValue()==861989530) {
			    		letzteProd=0;
			    		this.addComment("Nachricht gefunden MESSAGETYPE 861989530 ");
			    		this.doNotConfirmOrders("!!! Verdacht auf aufgebrauchten Kräuterbestand in dieser Region !!! (Zupfer unbestätigt)");
			    	}
			    	
			    	/*
			    	 * MESSAGETYPE 1233714163
						"\"$unit($unit) in $region($region): '$order($command)' - Es sind keine Kräuter zu finden.\"";text
			    	 * 
			    	 */
			    	if (m.getMessageType().getID().intValue()==1233714163) {
			    		letzteProd=0;
			    		this.addComment("Nachricht gefunden MESSAGETYPE 1233714163 ");
			    		this.doNotConfirmOrders("!!! Verdacht auf aufgebrauchten Kräuterbestand in dieser Region !!! (Zupfer unbestätigt)");
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
			    	
			    	/*
			    	MESSAGETYPE 1349776898
			    	"\"$unit($unit) in $region($region) stellt fest, dass es hier $localize($amount) $resource($herb,0) gibt.\"";text
			    	*/
			    	if (m.getMessageType().getID().intValue()==1349776898) {
			    		this.addComment("Nachricht gefunden MESSAGETYPE 1349776898 (xxx stellt fest)");
			    		Map<String,String> map = m.getAttributes();
			    		if (map.containsKey("amount")) {
			    			String anzahlStr = map.get("amount");
			    			this.addComment("Nachricht enthält  Wert für die Menge: " + anzahlStr);
			    			if (anzahlStr.equalsIgnoreCase("sehr viele")) {
			    				regionsBestand = 90;
			    			}
			    			if (anzahlStr.equalsIgnoreCase("viele")) {
			    				regionsBestand = 60;
			    			}
			    			if (anzahlStr.equalsIgnoreCase("relativ viele")) {
			    				regionsBestand = 30;
			    			}
			    			if (anzahlStr.equalsIgnoreCase("wenige")) {
			    				regionsBestand = 10;
			    			}
			    			if (anzahlStr.equalsIgnoreCase("sehr wenige")) {
			    				regionsBestand = 0;
			    			}
			    			if (regionsBestand>=0) {
			    				this.addComment("auszugehen ist von einem relativem Kräuterbestand von: " + regionsBestand + "% (gezupft wird ab " + MindestZupfBestandProzent + "%)");
			    			}
			    		} else {
			    			this.addComment("Nachricht enthält keinen Wert für die Menge");
			    		}
			    	}
			    	
		    	}
		    }
		} else {
			this.addComment("Einheit hat keine Nachrichten?! Keine Suche nach Infos zur Produktion in der letzten Runde...");
		}
		
		// haben wir in den Kommentaren eine Info über den SOLL-Zupfbetrag von letzter Runde??
		// form // Zupferinfo Runde=XXXX Sollmenge=YY
		// aktuelle Runde:
		int Runde=this.getOverlord().getScriptMain().gd_ScriptMain.getDate().getDate();
		int VorRunde = Runde - 1;
		int sollProdLetzteRunde = -1;
		
		if (this.scriptUnit.originalOrders_All.size()>0) {
			this.addComment("Prüfe Befehle auf Zupferinfos aus der letzten Runde...");
			for (String aOrder  : this.scriptUnit.originalOrders_All) {
				if (aOrder.toLowerCase().startsWith("// zupferinfo")){
					// this.addComment("untersuche zeile: " + aOrder);
					String payLoad = aOrder.substring(14);
					// this.addComment("untersuche payload: " + payLoad);
					if (payLoad.length()>3) {
						// zerlegen in durch space getrennte abschnitte
						boolean actRunde = false;
						int actValue=0;
						String[] pairs = payLoad.split(" ");
						for (int i=0;i<pairs.length;i++){
							String s2 = pairs[i];
							if (s2.indexOf("=")>0){
								String[] pair = s2.split("=");
								if (pair.length!=2){
									outText.addOutLine("!!Zupfer - Optionenparser Fehler:" + s2 + " (" + this.scriptUnit.getUnit().toString(true) + ")");
									return;
								}
								String key = pair[0];
								String value = pair[1];
								
								if (key.equalsIgnoreCase("Runde")) {
									if (Integer.parseInt(value)==VorRunde) {
										actRunde=true;
										this.addComment("passende Runde gefunden!!!");
									}
									if (Integer.parseInt(value)<(VorRunde-9)) {
										this.addComment("entferne ZupferInfo aus Runde " + value);
										this.scriptUnit.deleteSpecialOrder(aOrder);
									}
								}
								
								if (key.equalsIgnoreCase("Sollmenge")) {
									actValue = Integer.parseInt(value);
								}
								
							}
						}
						
						if (actRunde) {
							sollProdLetzteRunde = actValue;
							this.addComment("setze Sollproduktion aus Runde " + VorRunde + " auf " + sollProdLetzteRunde + " Kräuter.");
						}
						
					} else {
						// this.addComment("skipping " + aOrder + ", no payload in comment");
					}
				}
			}
		}
		
		if (sollProdLetzteRunde==-1) {
			// keine Info...wir gehen von menge aus
			sollProdLetzteRunde = testMenge;
			this.addComment("keine Information zur geplanten Zupfmenge aus letzter Runde vorhanden, gehe von " + sollProdLetzteRunde + " aus.");
		}
		
		if (letzteProd>=0) {
			this.addComment("Produktion letzte Runde: " + letzteProd);
		} else {
			this.addComment("keine Information über Produktion letzte Runde");
		}
		
		if (regionsBestand==-1 && letzteProd>=0 && sollProdLetzteRunde>0) {
			// regionsBestand berechnen
			regionsBestand = (int) Math.round(((double)letzteProd/(double)sollProdLetzteRunde)*100);
			this.addComment("Berechne aus den Zupfergebnissen den ungefähren Regionsbestand zu " + regionsBestand + "% (gezupft wird ab " + MindestZupfBestandProzent + "%)");
		}
		
		
		// Jahreszeit / Monat / Woche herausfinden
		
		int RundenFromStart = Runde - 1;
		int iWeek = (RundenFromStart % 3) + 1;
		int iMonth = (RundenFromStart / 3) % 9;
		// this.addComment("Datumsberechnung (aktuell): Runde " + Runde + ", Woche " + iWeek + ", Monat " + iMonth); 
		
		boolean nextTurnWinter = FFToolsGameData.isNextTurnWinter(this.getOverlord().getScriptMain().gd_ScriptMain);
		
		if (nextTurnWinter) {
			this.addComment("Nächste Woche ist Winter!!! Kräuter wachsen im Winter nicht, somit ernten wir nicht.");
		} else {
			this.addComment("Nächste Woche ist kein Winter - normales Kräuterwachstum, wir können ernten.");
		}
		if (skillLevel<unitMinLevel){
			// Lernen
			this.addComment("Ergänze Lernfix Eintrag mit Talent=Kräuterkunde");
			Script L = new Lernfix();
			ArrayList<String> order = new ArrayList<String>();
			order.add("Talent=Kräuterkunde");
			if (GratisTalentSkillType!=null) {
				order.add("Gratistalent=" + GratisTalentSkillType.toString());
				this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
			}
			L.setArguments(order);
			L.setScriptUnit(this.scriptUnit);
			L.setGameData(this.gd_Script);
			if (this.scriptUnit.getScriptMain().client!=null){
				L.setClient(this.scriptUnit.getScriptMain().client);
			}
			this.scriptUnit.addAScript(L);
		} else {
			// Wintercheck
			boolean hasWinterpausenOrder = false;
			String LernTalentName = "Kräuterkunde";
			if (nextTurnWinter) {
				String ZupferWinterOption = reportSettings.getOptionString("ZupferWinterOption", this.region());
				if (ZupferWinterOption==null) {
					// defaultwert
					ZupferWinterOption="LernenForschen";
				}
				if (ZupferWinterOption.equalsIgnoreCase("Forschen")) {
					if (skillLevel<7) {
						this.addComment("ZupferWinterOption *Forschen* bewirkt Lernen von Kräuterkunde, da noch nicht T7");
						// Lernen
						this.addComment("Ergänze Lernfix Eintrag mit Talent=Kräuterkunde");
						Script L = new Lernfix();
						ArrayList<String> order = new ArrayList<String>();
						order.add("Talent=Kräuterkunde");
						if (GratisTalentSkillType!=null) {
							order.add("Gratistalent=" + GratisTalentSkillType.toString());
							this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
						}
						L.setArguments(order);
						L.setScriptUnit(this.scriptUnit);
						L.setGameData(this.gd_Script);
						if (this.scriptUnit.getScriptMain().client!=null){
							L.setClient(this.scriptUnit.getScriptMain().client);
						}
						this.scriptUnit.addAScript(L);
					} else {
						this.addComment("ZupferWinterOption *Forschen* bewirkt Forschen");
						this.addOrder("FORSCHE KRÄUTER", true);
					}
					hasWinterpausenOrder=true;
				}
				
				if (ZupferWinterOption.equalsIgnoreCase("Lernen")) {

					
					if (LernPlan.length()==0) {
					
						if (LernTalentSkillType!=null) {
							LernTalentName = LernTalentSkillType.toString();
						}
						
						
						this.addComment("ZupferWinterOption *Lernen* bewirkt Lernen von " + LernTalentName + ", da in Winterpause");
						// Lernen
						this.addComment("Ergänze Lernfix Eintrag mit Talent=" + LernTalentName);
						Script L = new Lernfix();
						ArrayList<String> order = new ArrayList<String>();
						order.add("Talent=" + LernTalentName);
						if (GratisTalentSkillType!=null) {
							order.add("Gratistalent=" + GratisTalentSkillType.toString());
							this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
						}
						L.setArguments(order);
						L.setScriptUnit(this.scriptUnit);
						L.setGameData(this.gd_Script);
						if (this.scriptUnit.getScriptMain().client!=null){
							L.setClient(this.scriptUnit.getScriptMain().client);
						}
						this.scriptUnit.addAScript(L);
					} else {
						this.addComment("ZupferWinterOption *Lernen* bewirkt Lernplan " + LernPlan + ", da in Winterpause");
						// Lernen
						this.addComment("Ergänze Lernfix Eintrag mit Lernplan=" + LernPlan);
						Script L = new Lernfix();
						ArrayList<String> order = new ArrayList<String>();
						order.add("Lernplan=" + LernPlan);
						if (GratisTalentSkillType!=null) {
							order.add("Gratistalent=" + GratisTalentSkillType.toString());
							this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
						}
						L.setArguments(order);
						L.setScriptUnit(this.scriptUnit);
						L.setGameData(this.gd_Script);
						if (this.scriptUnit.getScriptMain().client!=null){
							L.setClient(this.scriptUnit.getScriptMain().client);
						}
						this.scriptUnit.addAScript(L);
						
					}
					hasWinterpausenOrder=true;
					
				}
				
				if (ZupferWinterOption.equalsIgnoreCase("LernenForschen")) {
					// immer Lernen, in der letzten Winterwoche Forschen
					boolean vorletzteWinterwoche = false;
					if (iMonth==3 && iWeek==2) {
						// 2. Woche Schneebann
						vorletzteWinterwoche=true;
					}
					
					
					if (skillLevel<7 || !vorletzteWinterwoche) {
						if (skillLevel<7) {
							this.addComment("ZupferWinterOption *LernenForschen* bewirkt Lernen von Kräuterkunde, da noch nicht T7");
							// Lernen
							this.addComment("Ergänze Lernfix Eintrag mit Talent=" + LernTalentName);
							Script L = new Lernfix();
							ArrayList<String> order = new ArrayList<String>();
							order.add("Talent=" + LernTalentName);
							if (GratisTalentSkillType!=null) {
								order.add("Gratistalent=" + GratisTalentSkillType.toString());
								this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
							}
							L.setArguments(order);
							L.setScriptUnit(this.scriptUnit);
							L.setGameData(this.gd_Script);
							if (this.scriptUnit.getScriptMain().client!=null){
								L.setClient(this.scriptUnit.getScriptMain().client);
							}
							this.scriptUnit.addAScript(L);
						} else {
							if (LernPlan.length()==0) {
								if (LernTalentSkillType!=null) {
									LernTalentName = LernTalentSkillType.toString();
								}
								this.addComment("ZupferWinterOption *LernenForschen* bewirkt Lernen von " + LernTalentName + ", da in Winterpause");
								// Lernen
								this.addComment("Ergänze Lernfix Eintrag mit Talent=" + LernTalentName);
								Script L = new Lernfix();
								ArrayList<String> order = new ArrayList<String>();
								order.add("Talent=" + LernTalentName);
								if (GratisTalentSkillType!=null) {
									order.add("Gratistalent=" + GratisTalentSkillType.toString());
									this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
								}
								L.setArguments(order);
								L.setScriptUnit(this.scriptUnit);
								L.setGameData(this.gd_Script);
								if (this.scriptUnit.getScriptMain().client!=null){
									L.setClient(this.scriptUnit.getScriptMain().client);
								}
								this.scriptUnit.addAScript(L);
							} else {
								this.addComment("ZupferWinterOption *LernenForschen* bewirkt Lernplan " + LernPlan + ", da in Winterpause");
								// Lernen
								this.addComment("Ergänze Lernfix Eintrag mit Lernplan=" + LernPlan);
								Script L = new Lernfix();
								ArrayList<String> order = new ArrayList<String>();
								order.add("Lernplan=" + LernPlan);
								if (GratisTalentSkillType!=null) {
									order.add("Gratistalent=" + GratisTalentSkillType.toString());
									this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
								}
								L.setArguments(order);
								L.setScriptUnit(this.scriptUnit);
								L.setGameData(this.gd_Script);
								if (this.scriptUnit.getScriptMain().client!=null){
									L.setClient(this.scriptUnit.getScriptMain().client);
								}
								this.scriptUnit.addAScript(L);
							}
						}
					} else {
						
						this.addComment("ZupferWinterOption *LernenForschen* bewirkt Forschen, in 2 Wochen ist der Winter vorbei.");
						this.addOrder("FORSCHE KRÄUTER", true);
					}
					hasWinterpausenOrder=true;
				}
				
				
			}
			
			
			if (!hasWinterpausenOrder) {
				// Zupfen
				
				
				int maxProdTotal = 18;
				if (menge==0) {
					menge=maxProdTotal;
				}
				int maxSkillMenge = skillLevel * this.getUnit().getModifiedPersons();
				if (regionsBestand>=MindestZupfBestandProzent) {
					// genügend da, maximal zupfen
					
					if (maxSkillMenge<=menge) {
						this.addOrder("mache " + maxSkillMenge + " Kräuter", true);
						this.addOrder("// Zupferinfo Runde=" + Runde + " Sollmenge=" + maxSkillMenge, true);
					} else {
						this.addOrder("mache " + menge + " Kräuter ;maximale ProdMenge", true);
						this.addOrder("// Zupferinfo Runde=" + Runde + " Sollmenge=" + menge, true);
					}
					FFToolsUnits.leaveAcademy(this.scriptUnit, " Zupfer arbeitet und verlässt Aka");
				}
				if (regionsBestand>=0 && regionsBestand<MindestZupfBestandProzent) {
					// Pausieren, Forschen oder Lernen
					if (skillLevel>=7) {
						// Forschen
						if (LernTalentSkillType!=null) {
							this.addComment("Region hat wenig Kräuter, LernTalent ist gesetzt, ich lerne...");
							LernTalentName = LernTalentSkillType.toString();
							this.addComment("Ergänze Lernfix Eintrag mit Talent=" + LernTalentName);
							Script L = new Lernfix();
							ArrayList<String> order = new ArrayList<String>();
							order.add("Talent=" + LernTalentName);
							if (GratisTalentSkillType!=null) {
								order.add("Gratistalent=" + GratisTalentSkillType.toString());
								this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
							}
							L.setArguments(order);
							L.setScriptUnit(this.scriptUnit);
							L.setGameData(this.gd_Script);
							if (this.scriptUnit.getScriptMain().client!=null){
								L.setClient(this.scriptUnit.getScriptMain().client);
							}
							this.scriptUnit.addAScript(L);
						} else if (LernPlan.length()>0){
							this.addComment("Region hat wenig Kräuter, LernPlan ist gesetzt:" + LernPlan);
							this.addComment("Ergänze Lernfix Eintrag mit Lernplan=" + LernPlan);
							Script L = new Lernfix();
							ArrayList<String> order = new ArrayList<String>();
							order.add("Lernplan=" + LernPlan);
							if (GratisTalentSkillType!=null) {
								order.add("Gratistalent=" + GratisTalentSkillType.toString());
								this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
							}
							L.setArguments(order);
							L.setScriptUnit(this.scriptUnit);
							L.setGameData(this.gd_Script);
							if (this.scriptUnit.getScriptMain().client!=null){
								L.setClient(this.scriptUnit.getScriptMain().client);
							}
							this.scriptUnit.addAScript(L);
							
						} else {
							this.addComment("Region hat wenig Kräuter, ich Forsche...");
							this.addOrder("FORSCHE KRÄUTER ;Regionsbestand niedrig" , true);
						}
						
					} else {
						this.addComment("Region hat wenig Kräuter, ich würde gerne Forschen, muss dafür aber noch lernen...");
						// Lernen
						Script L = new Lernfix();
						ArrayList<String> order = new ArrayList<String>();
						order.add("Talent=Kräuterkunde");
						if (GratisTalentSkillType!=null) {
							order.add("Gratistalent=" + GratisTalentSkillType.toString());
							this.addComment("Ergänze Lernfix mit Parameter Gratistalent=" + GratisTalentSkillType.toString());
						}
						L.setArguments(order);
						L.setScriptUnit(this.scriptUnit);
						L.setGameData(this.gd_Script);
						if (this.scriptUnit.getScriptMain().client!=null){
							L.setClient(this.scriptUnit.getScriptMain().client);
						}
						this.scriptUnit.addAScript(L);
					}
				}
				
				if (regionsBestand==-1) {
					// Keine Info verfügbar
					// Pausieren, Forschen oder Lernen
					if (skillLevel>=7) {
						// Forschen
						this.addComment("Region hat unbekannten Kräuterbestand, ich Forsche...");
						this.addOrder("FORSCHE KRÄUTER ;Regionsbestand niedrig" , true);
					} else {
						int testZupfmenge= testMenge; // ToDo: setscripteroption konfigurierbar machen
						if (menge>0) {
							testZupfmenge= menge;
						}
						if (maxSkillMenge<testZupfmenge) {
							testZupfmenge=maxSkillMenge;
						}
						this.addComment("Region hat unbekannten Kräuterbestand, ich versuche testweise zu ernten.");
						this.addOrder("mache " + testZupfmenge + " Kräuter ;Testmenge", true);
						this.addOrder("// Zupferinfo Runde=" + Runde + " Sollmenge=" + testZupfmenge, true);
						FFToolsUnits.leaveAcademy(this.scriptUnit, " Zupfer testet und verlässt Aka");
					}
				}
			}

		}
	}

}
