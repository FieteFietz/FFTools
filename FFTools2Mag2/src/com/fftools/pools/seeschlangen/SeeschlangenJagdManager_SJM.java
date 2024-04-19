package com.fftools.pools.seeschlangen;

import java.util.ArrayList;
import java.util.Collections;

import com.fftools.ScriptUnit;
import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.scripts.Sailto;
import com.fftools.scripts.Seeschlangenjagd;
import com.fftools.utils.FFToolsRegions;

import magellan.library.Battle;
import magellan.library.CoordinateID;
import magellan.library.Faction;
import magellan.library.Message;
import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.Unit;
import magellan.library.rules.MessageType;
import magellan.library.rules.Race;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;

/**
 * Verwaltet SeeschlangenJäger
 * 
 * 
 * @author Fiete
 *
 */
public class SeeschlangenJagdManager_SJM implements OverlordRun,OverlordInfo {

	// private static final OutTextClass outText = OutTextClass.getInstance();
	
	public static final String MAPLINE_TAG="FFTools_SSJM_MoveLine";
	
	private Overlord overLord = null;
	
	// in diesen Regionen hat schon irgendjemand einen ATTACK auf Monster erhalten
	private ArrayList<CoordinateID> attackRegions = new ArrayList<CoordinateID>(0);
	
	// in diese Regionen hat schon irgendjemand einen MOVE erhalten
	private ArrayList<CoordinateID> moveToRegions = new ArrayList<CoordinateID>(0);
	
	// Liste aller einsatzfähigen Mover
	private ArrayList<Seeschlangenjagd> availableMovers = new ArrayList<Seeschlangenjagd>(0);
	
	// Liste aller InformationReceiver
	private ArrayList<Seeschlangenjagd> informationReceiver = new ArrayList<Seeschlangenjagd>(0);
	
		
	/**
	 * Wann soll er laufen
	 * VOR Lernfix 
	 */
	private static final int Durchlauf = 59;
	
	// Rückgabe als Array
	private int[] runners = {Durchlauf};
	
	
	
	
	public SeeschlangenJagdManager_SJM(Overlord overlord){
		this.overLord = overlord;
	}

	
	/**
	 * startet den SeeschlangenJagdManager_SJM
	 */
	public void run(int durchlauf){
		/*
		 * 1. aktuelle Seeschlangendurchgehen, nicht attackierte auf die Liste der anzufahrenden Regionen setzen
		 * 2. Kampfreporte durchgehen und überlebene Seeschlangen eintragen, inkl der Regionen drumherum
		 * 3. Befreundete Schiffe mit Ziel=Ozean anfahren, sortieren, und auf die Liste
		 * 4. Liste abarbeiten
		 * 		- Nächsten Mover suchen
		 * 		- Mover hinschicken
		 * 		- Ziel abhaken
		 */
		
		// Aktuelle Seeschlangen im Report finden...
		ArrayList<CoordinateID> bekannteSchlangen = new ArrayList<CoordinateID>(0);
		ArrayList<CoordinateID> vermuteteSchlangen = new ArrayList<CoordinateID>(0);
		
		for (Unit u :this.overLord.getScriptMain().gd_ScriptMain.getUnits()) {
			Faction f = u.getFaction();
			if (f!=null) {
				if (f.getID().toString().equalsIgnoreCase("ii")) {
					Race r = u.getRace();
					if (r!=null) {
						if (r.getName().equalsIgnoreCase("Seeschlangen")) {
							CoordinateID actC = u.getRegion().getCoordinate();
							if (!this.attackRegions.contains(actC)) {
								bekannteSchlangen.add(actC);
							}
							
							if (u.getAttackAggressors().size()>0) {
								// wird bereits angegriffen
								this.attackRegions.add(actC);
							}
							
							// Umgebung als vermutete Hinzufügen
							for (CoordinateID c : Regions.getAllNeighbours(actC, 1)) {
								Region reg = this.overLord.getScriptMain().gd_ScriptMain.getRegion(c);
								if (reg!=null && reg.getRegionType()!=null && reg.getRegionType().isOcean()) {
									if (!bekannteSchlangen.contains(c) && !vermuteteSchlangen.contains(c)) {
										vermuteteSchlangen.add(c);
									}
								}
							}
						}
					}
				}
			}
		}
		
		// schiffe
		ArrayList<ProtectedShip> protectedShips = new ArrayList<ProtectedShip>(0);
		for (Ship s:this.overLord.getScriptMain().gd_ScriptMain.getShips()) {
			Unit u = s.getModifiedOwnerUnit();
			if (u!=null) {
				ScriptUnit su = this.overLord.getScriptMain().getScriptUnit(u);
				if (su!=null) {
					Object o = su.getScript(Sailto.class);
					if (o!=null) {
						Sailto st = (Sailto) o;
						boolean protectableShip = true;
						if (su.hasOrder("// kein_SJM_Schutz") || su.hasOrder("// no_Protection")) {
							su.addComment("SJM: Schiff wird nicht geschützt.");
							protectableShip = false;
						}
						
						if (protectableShip && st.nextShipStop!=null && st.nextShipStop.getRegionType().isOcean()) {
							// bingo: wir haben einen nächsten Stop
							ProtectedShip pS = new ProtectedShip();
							pS.captn = su;
							pS.nextShipStop = st.nextShipStop;
							pS.s = s;
							pS.calcShip();
							protectedShips.add(pS);
							su.addComment("SJM: auf die Liste zu schützender Schiffe gesetzt.");
						}
					}
				}
			}
		}

		// Sortieren
		Collections.sort(protectedShips);
		
		// Battlereports
		ArrayList<CoordinateID> SchlangenSchlachten = new ArrayList<CoordinateID>(0);
		ArrayList<CoordinateID> SchlangenSchlachtenUmgebung = new ArrayList<CoordinateID>(0);
		for (Seeschlangenjagd su : this.informationReceiver) {
			for (Faction f:this.overLord.getScriptMain().gd_ScriptMain.getFactions()) {
				if (f!=null && f.getBattles()!=null) {
					for (Battle b:f.getBattles()) {
						if (!SchlangenSchlachten.contains(b.getID())) {
							boolean SeeschlangeInvolviert = false;
							boolean MonsterÜberlebt = false;
							for (Message m:b.messages()) {
								MessageType mT = m.getMessageType();
								if (mT.getID().intValue()==1803906635) {
									if (m.getText().contains("Seeschlange")) {
										SeeschlangeInvolviert=true;
									}
								}
								if (mT.getID().intValue()==1109807897) {
									/*
									 * "Heer 0(ii): 0 Tote, 0 Geflohene, 1 Überlebende.";rendered
											0;index
											"ii";abbrev
											0;dead
											0;fled
											1;survived
									 */
									/* old:
									String factionNO = m.getAttributes().get("abbrev");
									String survivedS = m.getAttributes().get("survived");
									String fledS = m.getAttributes().get("fled");
									*/
									
									String factionNO = m.getAttribute("abbrev");
									String survivedS = m.getAttribute("survived");
									String fledS = m.getAttribute("fled");
									
									int survided=0;
									int fled = 0;
									if (survivedS!=null) {
										survided = Integer.parseInt(survivedS);
									}
									if (fledS!=null) {
										fled = Integer.parseInt(fledS);
									}
									if (factionNO.equalsIgnoreCase("ii") && (survided>0 || fled>0) ) {
										MonsterÜberlebt=true;
									}
								}
							}
							if (SeeschlangeInvolviert && MonsterÜberlebt) {
								SchlangenSchlachten.add(b.getID());
								su.addComment("SJM: SS hat überlebt in " + b.getID().toString());
								for (CoordinateID c : Regions.getAllNeighbours(b.getID(), 1)) {
									if (!SchlangenSchlachten.contains(c) && !SchlangenSchlachtenUmgebung.contains(c) 
											&& !vermuteteSchlangen.contains(c) && !bekannteSchlangen.contains(c)) {
										SchlangenSchlachtenUmgebung.add(c);
									}
								}
							}
						}
					}
				}
			}
		}
		
		
		
		
		
		// Zuordnen
		// Sichtungen
		for (CoordinateID c:bekannteSchlangen) {
			if (!this.attackRegions.contains(c)) {
				Seeschlangenjagd su = findSJ(c,true);
				if (su!=null) {
					// gefunden
					if (su.targetRegionCoord==null) {
						su.targetRegionCoord = c;
						su.addComment("SJM: Ziel zugewiesen (bekannte SS):" + c.toString(",", false));
						su.makeOrderNach();
						addMoveToRegionC(c);
					} else {
						su.addComment("SJM: Ziel ERNEUT zugewiesen (bekannte SS):" + c.toString(",", false));
					}
				} 
			}
		}
		
		// Schlachten
		for (CoordinateID c:SchlangenSchlachten) {
			if (!this.moveToRegions.contains(c)) {
				if (!this.attackRegions.contains(c)) {
					Seeschlangenjagd su = findSJ(c,true);
					if (su!=null) {
						// gefunden
						if (su.targetRegionCoord==null) {
							su.targetRegionCoord = c;
							su.addComment("SJM: Ziel zugewiesen (überlebende SS nach Schlacht):" + c.toString(",", false));
							su.makeOrderNach();
							addMoveToRegionC(c);
						} else {
							su.addComment("SJM: Ziel ERNEUT zugewiesen (überlebende SS nach Schlacht):" + c.toString(",", false));
						}
					} 
				}
			}
		}
		
		// zu schützende Schiffe
		int countProtectedShips=0;
		for (ProtectedShip pS : protectedShips) {
			if (!this.moveToRegions.contains(pS.nextShipStop.getCoordinate())) {
				Seeschlangenjagd su = findSJ(pS.nextShipStop.getCoordinate(),false);
				if (su!=null) {
					// gefunden
					if (su.targetRegionCoord==null) {
						su.targetRegionCoord = pS.nextShipStop.getCoordinate();
						su.addComment("SJM: Ziel zugewiesen (zu schützendes Schiff):" + pS.nextShipStop.getCoordinate().toString(",", false));
						su.addComment("zu schützen: " + pS.s.toString() + ", " + pS.Silber + " Silber, " + pS.Personen + " Personen"); 
						su.makeOrderNach();
						addMoveToRegionC(pS.nextShipStop.getCoordinate());
					} else {
						su.addComment("SJM: zu schützen: " + pS.s.toString() + ", " + pS.Silber + " Silber, " + pS.Personen + " Personen");
					}
					pS.captn.addComment("SJM: Schiff wird nächste Runde geschützt von: " + su.getUnit().getModifiedShip().toString());
					countProtectedShips++;
					pS.SJ=su;
				} else {
					pS.captn.addComment("SJM: Schiff kann leider nächste Runde nicht geschützt werden. Viel Glück!");
				}
			} else {
				pS.captn.addComment("SJM: Schiff wird nächste Runde geschützt von einem SJ, der bereits Kurs auf diese Region hat.");
				countProtectedShips++;
			}
		}
		
		// Vermutete
		for (CoordinateID c:vermuteteSchlangen) {
			if (!this.moveToRegions.contains(c)) {
				if (!this.attackRegions.contains(c)) {
					Seeschlangenjagd su = findSJ(c, true);
					if (su!=null) {
						// gefunden
						if (su.targetRegionCoord==null) {
							su.targetRegionCoord = c;
							su.addComment("SJM: Ziel zugewiesen (vermutete SS):" + c.toString(",", false));
							su.makeOrderNach();
							addMoveToRegionC(c);
						} else {
							su.addComment("SJM: Ziel ERNEUT zugewiesen (vermutete SS):" + c.toString(",", false));
						}
					}
				}
			}
		}
		
		// SchlachtenUmgebung
		for (CoordinateID c:SchlangenSchlachtenUmgebung) {
			if (!this.moveToRegions.contains(c)) {
				if (!this.attackRegions.contains(c)) {
					Seeschlangenjagd su = findSJ(c,true);
					if (su!=null) {
						// gefunden
						if (su.targetRegionCoord==null) {
							su.targetRegionCoord = c;
							su.addComment("SJM: Ziel zugewiesen (überlebende SS nach Schlacht in Umgebung):" + c.toString(",", false));
							su.makeOrderNach();
							addMoveToRegionC(c);
						} else {
							su.addComment("SJM: Ziel ERNEUT zugewiesen (überlebende SS nach Schlacht in Umgebung):" + c.toString(",", false));
						}
					} 
				}
			}
		}
		
		
		// freie Bewegung
		int count_randomers=0;
		int count_RTB = 0 ;
		for (Seeschlangenjagd SJ:this.availableMovers) {
			if (SJ.targetRegionCoord==null && !SJ.is_attacking && !SJ.is_moving_home && SJ.mayPatrol) {
				// freier Mover
				// Sicherheitscheck: bin ich - warum auch immer, innerhalb der Entfernug zu HOME?
				int dist = Regions.getDist(SJ.actRegionCoord, SJ.HomeRegionCoord);
				if (dist>SJ.Entfernung) {
					// huch...1x ab nach Hause
					SJ.addComment("SJM: Entfernung zu HOME momentan " + dist + " Regionen, damit größer als zulässig (" + SJ.Entfernung + ")");
					SJ.addComment("SJM: Bewegung in Richtung HOME!");
					SJ.targetRegionCoord = SJ.HomeRegionCoord;
					SJ.makeOrderNach();
					SJ.is_moving_home=true; // RTB
					count_RTB++;
				} else {
					// Zufallsplätzchen suchen
					// Regionen mit R=Entfernung um HOME herum
					// Regionen mit R=speed um actPOs herum
					// nur Ozean
					// in 1 Runde erreichbar
					ArrayList<CoordinateID> shipDist = new ArrayList<CoordinateID>(0); 
					shipDist.addAll(Regions.getAllNeighbours(SJ.actRegionCoord, SJ.speed));
					
					ArrayList<CoordinateID> HOMEDist = new ArrayList<CoordinateID>(0);
					HOMEDist.addAll(Regions.getAllNeighbours(SJ.HomeRegionCoord, SJ.Entfernung));
					
					Direction d = Direction.INVALID;
					if (SJ.getUnit().getModifiedShip()!=null){
						d = Regions.getMapMetric(SJ.gd_Script).toDirection(SJ.getUnit().getModifiedShip().getShoreId());
					}
					
					ArrayList<Region> possibleRegions = new ArrayList<Region>(0);
					for (CoordinateID c : shipDist) {
						if (HOMEDist.contains(c)) {
							if (!moveToRegions.contains(c)) {
								if (!this.attackRegions.contains(c)) {
									Region r = SJ.getOverlord().getScriptMain().gd_ScriptMain.getRegion(c);
									if (r!=null && r.getRegionType().isOcean()) {
										
										// letzter Check....komme ich da auch in 1 Woche hin?
										int Reisewochen = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(SJ.actRegionCoord,d, c, SJ.gd_Script, SJ.speed, null);
										if (Reisewochen==1) {
											possibleRegions.add(r);
										}
									}
								}
							}
						}
					}
					
					if (possibleRegions.size()==0) {
						SJ.addComment("SJM: kein gutes Ziel in der Umgebung gefunden, nehme Kurs auf HOME");
						SJ.targetRegionCoord = SJ.HomeRegionCoord;
						SJ.makeOrderNach();
						SJ.is_moving_home=true; // RTB
						count_RTB++;
					} else {
						// SJ.addComment("SJM: Suche zufälliges neues Ziel aus: " + possibleRegions.toString());
						SJ.addComment("SJM: Suche zufälliges neues Ziel aus...(" + possibleRegions.size() + " Regionen möglich)");
						Region targetR = null;
						if (possibleRegions.size()==1) {
							SJ.addComment("SJM: nur ein Ziel, die Wahl fällt nicht schwer...");
							targetR = possibleRegions.get(0);
						} else {
							int zufallszahl = (int)(Math.random() * (possibleRegions.size())); 
							SJ.addComment("SJM: Zufallszahl zw 0 und " + (possibleRegions.size()-1)+" lautet: " + zufallszahl);
							targetR = possibleRegions.get(zufallszahl);
						}
						SJ.addComment("SJM: neues Ziel gesetzt: " + targetR.toString());
						SJ.targetRegionCoord = targetR.getCoordinate();
						SJ.makeOrderNach();
						count_randomers++;
					}
				}
			}
			// wichtig: SSJ auf Ozean mit mayPatrol=false => moveHOME
			// wichtig: SSJ könnte auch in der HOME liegen!
			if (SJ.targetRegionCoord==null && !SJ.is_attacking && !SJ.is_moving_home && !SJ.mayPatrol) {
				if (!SJ.getUnit().getRegion().getCoordinate().equals(SJ.HomeRegionCoord)) {
					// 
					SJ.addComment("Returning to Base : no target and patrol not allowed.");
					SJ.targetRegionCoord = SJ.HomeRegionCoord;
					SJ.is_moving_home=true;
					SJ.makeOrderNach();
				}
				
			}
		}
		long cntLearnen=0;
		for (Seeschlangenjagd SJ:this.availableMovers) {
			if (SJ.targetRegionCoord==null && !SJ.is_attacking && !SJ.is_moving_home && !SJ.is_Learning) {
				// die haben nix bekommen
				SJ.Lerne();
				cntLearnen++;
			}
		}
		
		
		// Informationen
		for (Seeschlangenjagd su : this.informationReceiver) {
			su.addComment("SJM: Seeschlangen werden attackiert in: " + this.attackRegions.toString());
			su.addComment("SJM: Seeschlangen bekannt in: " + bekannteSchlangen.toString());
			su.addComment("SJM: Seeschlangen Schlacht überlebt in: " + SchlangenSchlachten.toString());
			su.addComment("SJM: Seeschlangen vermutet nach Sichtung in: " + vermuteteSchlangen.size() + " Regionen");
			su.addComment("SJM: Seeschlangen vermutet nach Schlacht in: " + SchlangenSchlachtenUmgebung.size()+ " Regionen");
			su.addComment("SJM: SJ als verfügbar gemeldet: " + this.availableMovers.size() + ", davon " + count_randomers + " auf Zufallskurs, " + count_RTB + " RTB, " + cntLearnen + " lernen.");
			su.addComment("SJM: zu schützende Schiffe: " + protectedShips.size() + ", davon " + countProtectedShips + " geschützt.");
		}
		
	}
	
	/**
	 * findet den nächsten Mover für diese C, wenn verfügbar
	 * @param c
	 * @return
	 */
	
	private Seeschlangenjagd findSJ(CoordinateID c, boolean respectWeeksToAttack) {
		
		// die Mover nach Entfernung zu c Sortieren
		SJ_Mover_Comparator SJC = new SJ_Mover_Comparator(c);
		Collections.sort(this.availableMovers, SJC);

		for (Seeschlangenjagd SJ : this.availableMovers) {
			if (SJ.targetRegionCoord!=null && SJ.targetRegionCoord.equals(c)) {
				// der ist schon auf dem Weg dahin...
				return SJ;
			}
			if (SJ.targetRegionCoord==null) {
				// schnell mal Distance checken - geht am schnellsten - als Vorprüfung
				int dist = Regions.getDist(SJ.actRegionCoord, c);
				
				int maxWeeks = 1;
				if (respectWeeksToAttack) {
					maxWeeks = SJ.weeks2attack;
				}
				
				if (dist<=(SJ.speed * maxWeeks)) {
					// prinzipiell möglich, genau berechnen
					Direction d = Direction.INVALID;
					if (SJ.getUnit().getModifiedShip()!=null){
						d = Regions.getMapMetric(this.overLord.getScriptMain().gd_ScriptMain).toDirection(SJ.getUnit().getModifiedShip().getShoreId());
					} 
					
					dist = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(SJ.actRegionCoord,d, c, this.overLord.getScriptMain().gd_ScriptMain, SJ.speed, null);
					if (dist<=maxWeeks && dist>0) {
						// er kann hin...noch HOME checken
						int actEntf = Regions.getDist(c, SJ.HomeRegionCoord);
						if (actEntf<=SJ.Entfernung) {
							// Treffer
							return SJ;
						}
					}
				}
			}
		}
		// keinen gefunden
		return null;
	}
	
	
	
	/**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
    
    /*
     * nimmt die Meldung entgegen, dass in c bereist attackiert wird
     */
    public void addAttackRegion(CoordinateID c) {
    	if (!this.attackRegions.contains(c)) {
    		this.attackRegions.add(c);
    	}
    }
    
    /*
     * nimmt eine Seeschlangenjagd-Unit als Mover auf
     */
    public void addMover(Seeschlangenjagd su) {
    	if (!this.availableMovers.contains(su)) {
    		this.availableMovers.add(su);
    	}
    }
    
    /*
     * nimmt eine Seeschlangenjagd-Unit als Info-Receiver auf
     */
    public void addInfoReceiver(Seeschlangenjagd su) {
    	if (!this.informationReceiver.contains(su)) {
    		this.informationReceiver.add(su);
    	}
    }
    
    private void addMoveToRegionC(CoordinateID c) {
    	if (!this.moveToRegions.contains(c)) {
    		this.moveToRegions.add(c);
    	}
    }
    
}
