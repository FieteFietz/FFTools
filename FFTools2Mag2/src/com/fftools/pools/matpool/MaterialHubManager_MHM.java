package com.fftools.pools.matpool;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Materialhub;
import com.fftools.scripts.Materialtransport;
import com.fftools.scripts.Sailto;
import com.fftools.utils.FFToolsRegions;

import magellan.library.CoordinateID;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.rules.ItemType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;

/**
 * Verwaltet MaterialTransports zwischen MaterialHubs
 * 
 * 
 * @author Fiete
 *
 */
public class MaterialHubManager_MHM implements OverlordRun,OverlordInfo {

	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	public static final String MAPLINE_TAG="FFTools_MHM_MoveLine";
	public static final int MAPLINE_RGB_R = 0;
	public static final int MAPLINE_RGB_G = 0;
	public static final int MAPLINE_RGB_B = 0;
	
	
	private Overlord overLord = null;
	
	private ArrayList<Materialhub> MHs = new ArrayList<Materialhub>();
	private ArrayList<Materialtransport> MTs = new ArrayList<Materialtransport>();
	
	private HashMap<String,Double> MaterialScores = new HashMap<String,Double>();
	private ArrayList<CoordinateID> HOME_Regions = new ArrayList<CoordinateID>();
	
	private HashMap<CoordinateID,HashMap<ItemType,Item>> HOME_MatPoolItems = new HashMap<CoordinateID, HashMap<ItemType,Item>>();
	
	private int maxMH_HOME_Dist_Weeks = 20; // Wochen !!!
	private int MT_Request_Prio=50;
	private int MHM_max_Loops = 300;

		
	/**
	 * Wann soll er laufen
	 * VOR Lernfix 
	 */
	private static final int Durchlauf = 515;
	
	// Rückgabe als Array
	private int[] runners = {Durchlauf};
	
	private DecimalFormat dfTausend = new DecimalFormat("#,###.000");
	
	
	public MaterialHubManager_MHM(Overlord overlord){
		this.overLord = overlord;
	}

	
	/**
	 * startet den MHM
	 */
	public void run(int durchlauf){
		
		
		this.init_Materialscores();
		
		String oldFileName = outText.getActFileName();
		boolean oldScreenOut = outText.isScreenOut();
		
		outText.setFile("MaterialHubManager");
		outText.setScreenOut(false);
		
		outText.addOutLine("MHM gestartet");
		
		if (this.MTs.size()==0) {
			outText.addOutLine("MHM  - keine MTs - abort.");
			outText.setFile(oldFileName);
			outText.setScreenOut(oldScreenOut);
			return;
		}
		if (this.MHs.size()==0) {
			outText.addOutLine("MHM  - keine MHs - abort.");
			outText.setFile(oldFileName);
			outText.setScreenOut(oldScreenOut);
			return;
		}
		
		outText.addOutLine("MHM: MTs:" + this.MTs.size() + ", MHs:" + this.MHs.size() + " | Berechne Scores der MHs");
		
		
		
		outText.addOutLine("extrahiere HOME Regions der MTs: ");
		// Aus den MTs die HOME-Regionen extrahieren
		for (Materialtransport MT: this.MTs) {
			outText.addOutLine("--working on " + MT.scriptUnit.toString() + " (kapa " + MT.getAvailable_capacity() + " GE)");
			CoordinateID r = MT.HomeRegionCoord;
			if (r!=null) {
				if (!this.HOME_Regions.contains(r)) {
					this.HOME_Regions.add(r);
					Region r2 = this.overLord.getScriptMain().gd_ScriptMain.getRegion(r);
					if (r2!=null) {
						outText.addOutLine("added: " + r2.toString() + " as valid HOME Region");
					} else {
						outText.addOutLine("added: " + r.toString() + " (!!! keine Region gefunden!!!");
					}
				}
			} else {
				outText.addOutLine("!!!! Keine HomeRegion bei: " + MT.scriptUnit.toString());
			}
		}
		outText.addOutLine("Zuordnung der HOME-Regionen zu den MHs (max: " + this.maxMH_HOME_Dist_Weeks + " Reisewochen)");
		// zu jedem MH die passende Home-Region finden
		for (Materialhub MH: this.MHs) {
			outText.addOutLine("working on " + MH.getTAName());
			double minDist = Double.MAX_VALUE;
			MH.setFinal_processed_by_MHM(false);
			CoordinateID selectedHOME = null;
			for (CoordinateID r:this.HOME_Regions) {
				int actDistWeeks = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(r, Direction.INVALID, MH.scriptUnit.getUnit().getRegion().getCoordinate(), this.overLord.getScriptMain().gd_ScriptMain, 6,null);
				outText.addOutLine("distance to " + r.toString() + ": " + actDistWeeks);
				if (actDistWeeks <= this.maxMH_HOME_Dist_Weeks) {
					if (actDistWeeks<minDist) {
						outText.addOutLine("new selected HOME: " + r.toString());
						selectedHOME = r;
						minDist = actDistWeeks;
					}
				} else {
					outText.addOutLine("over max Dist - ignoring");
				}
			}
			if (selectedHOME!=null) {
				outText.addOutLine("finale value: " + selectedHOME.toString());
				MH.setHOME_Region(selectedHOME);
				Region r_sel = MH.gd_Script.getRegion(selectedHOME);
				MH.addComment("MHM: zugeordnete HOME Region: " + r_sel.toString() + " (Entfernung " + minDist + " Karavellenwochen)");
			} else {
				MH.doNotConfirmOrders("MHM - keine HOME region in Reichweite von " + this.maxMH_HOME_Dist_Weeks + " Reisewochen.");
			}
		}

		outText.addOutLine("Rufe MatPools der Home-Regionen ab");
		init_Home_MatpoolItems();
		
		// Main Loop
		outText.addOutLine("start main loop");
		int loopcounter = 0;
		while (true) {
			loopcounter++;
			outText.addOutLine("Loop start (" + loopcounter + ")");
			if (loopcounter>MHM_max_Loops) {
				outText.addOutLine("Loop break max Counter reached(" + loopcounter + ")");
				break;
			}
			for (Materialhub MH: this.MHs) {
				if (MH.getHOME_Region()!=null) {
					double res = MH.getScoreOfMPR_mprMap_AFTER_HOME_Decisions(MH.scriptUnit, loopcounter);
					outText.addOutLine("TA: " + MH.getTAName() + ": " + dfTausend.format(res));
				}
			}
			
			
			// Sortieren
			this.MHs.sort(Comparator.comparingDouble((Materialhub p) -> p.actMaterialScore).reversed());
			outText.addOutLine(" sortiert: ");
			// Ausgaben
			
			ArrayList<Materialhub> act_MHs = new ArrayList<Materialhub>();
			for (Materialhub MH: this.MHs) {
				if (MH.getHOME_Region()!=null && MH.actMaterialScore>0 && !MH.isFinal_processed_by_MHM()) {
					outText.addOutLine("TA: " + MH.getTAName() + ": " + dfTausend.format(MH.actMaterialScore));
					
					act_MHs.add(MH);
				}
			}
			if (act_MHs.size()==0) {
				outText.addOutLine("leaving Main Loop: no more MHs available");
				break;
			}
			
			// mit MH an Pos 1 anfangen
			Materialhub MH = act_MHs.get(0);
			outText.addOutLine("start working on MH: " + MH.getTAName() + ", checking for MTs in HOME");
			
			// Liste der freien MT in der HOME generieren - nur die, die noch kein Ziel haben
			// und nur die, die diese Region auch als HOME haben !!
			ArrayList<Materialtransport> act_MTs = new ArrayList<Materialtransport>();
			for (Materialtransport MT: this.MTs) {
				if (MT.actRegionCoord.equals(MH.getHOME_Region()) && MT.targetRegionCoord==null && MT.getAvailable_capacity()>0 && MT.HomeRegionCoord.equals(MH.getHOME_Region())) {
					outText.addOutLine("found: " + MT.scriptUnit.toString() + ", free Kapa=" + MT.getAvailable_capacity());
					act_MTs.add(MT);
				}
			}
			
			if (act_MTs.size()==0) {
				// keine MTs in HOME -> MH als erledigt markieren
				outText.addOutLine("aborting on MH: " + MH.getTAName() + ", no free MTs in HOME-Region");
				MH.setFinal_processed_by_MHM(true);
			} else {
				// wir haben MTs in der HOME
				// sortieren nach freier Kapa
				outText.addOutLine("sorting MTs");
				act_MTs.sort(Comparator.comparingInt((Materialtransport p) -> p.getAvailable_capacity()).reversed());
				// wählen den grössten
				Materialtransport MT_work = act_MTs.get(0);
				outText.addOutLine("working on MT " + MT_work.scriptUnit.toString() + " (" + MT_work.getAvailable_capacity() + " GE)" );
				int geladen = process_MT(MT_work, MH);
				if (geladen<=0) {
					// nix mehr für das MH zu holen
					outText.addOutLine("kein weiterer Fortschritt, MH " + MH.getTAName() + " als abgeschlossen markiert" );
					MH.setFinal_processed_by_MHM(true);
				}
			}
		}
		
		// aktualisieren der MHs - reine Info
		for (Materialhub MH: this.MHs) {
			MH.inform_HOME_Decisions();
		}
		
		// kurze Statistik über Lernende MTs - also alle, die in HOME sind aber keinen Auftrag bekommen haben
		outText.addOutLine("Übersicht lernende MTs:");
		for (Materialtransport MT: this.MTs) {
			if (MT.isLearning) {
				outText.addOutLine(MT.scriptUnit.toString());
			}
		}
		
		// Statistiken zu den HOMEs: wieviele MH und MTs verbunden
		outText.addOutLine("Übersicht HOMEs and MH and MT:");
		for (CoordinateID homeC_ID:this.HOME_Regions) {
			Region Home_Reg = this.overLord.getScriptMain().gd_ScriptMain.getRegion(homeC_ID);
			// MHs zählen
			int count_MH = 0;
			for (Materialhub MH: this.MHs) {
				if (MH.getHOME_Region().equals(homeC_ID)) {
					count_MH++;
				}
			}
			// MTs zählen
			int count_MT = 0;
			for (Materialtransport MT:this.MTs) {
				if (MT.HomeRegionCoord.equals(homeC_ID)) {
					count_MT++;
				}
			}
			
			outText.addOutLine("Home " + Home_Reg.toString() +  ": " + count_MH + " MHs | " + count_MT + " MTs");
		}
		
		outText.addOutLine("MHM  - exiting");
		outText.setFile(oldFileName);
		outText.setScreenOut(oldScreenOut);
	}
	
	/*
	 * versucht, die Anforderungen des MH durch den MT (in HOME Reg) zu bedienen
	 * MT dient dabei nur zur Kapa beschränkung und bekommt die requests...
	 * eigentlicher virtueller Transfer zwischen MH und Depot der HOME
	 */
	private int process_MT(Materialtransport MT, Materialhub MH) {
		// durch die Anorderungen des MH gehen
		Map<String,Integer> mprMap_AFTER_HOME_Decisions = MH.getMprMap_AFTER_HOME_Decisions();
		CoordinateID rID = MT.actRegionCoord;
		HashMap<ItemType,Item> HOME_MatPool = this.HOME_MatPoolItems.get(rID);
		if (HOME_MatPool==null) {
			outText.addOutLine("!!! MHM-MH: kein HOME-Matpool in " + rID.toString() + " für MT " + MT.toString());
			MH.setFinal_processed_by_MHM(true);
			return -1;
		}
		int Liefermenge_MT = 0;
		for (Map.Entry<String, Integer> entry : mprMap_AFTER_HOME_Decisions.entrySet()) {
    		String gegenstand = entry.getKey();
    		int anzahl = entry.getValue();
    		if (anzahl<=0) {
    			continue;
    		}
    		outText.addOutLine("MHM-MH fordert: " + anzahl + " " + gegenstand);
    		ItemType itemType = reportSettings.getRules().getItemType(gegenstand); 
    		ArrayList<ItemType> itemTypes = null;
    		if (itemType!=null){
    			// es ist ein ItemType
    			itemTypes = new ArrayList<ItemType>();
    			itemTypes.add(itemType);
    		} else {
    			// ist es eine Kategorie?
    			ArrayList<ItemType> _itemTypes = reportSettings.getItemTypes(gegenstand);
    			if (_itemTypes!=null){
    				// reportSettings liefert bereits sortierte Liste
    				itemTypes = new ArrayList<ItemType>();
    				itemTypes.addAll(_itemTypes);
    			}
    		}
    		if (itemTypes==null) {
    			outText.addOutLine("!!! MHM-MH: " + gegenstand + " nicht erkannt (kein Gegenstand, keine Kategorie)");
    		} else {
    			// wir haben eine Liste
    			int Liefermenge_gesamt = 0;
    			for (ItemType iT:itemTypes) {
    				outText.addOutLine("MHM-MH: prüfe " + iT.getName() + " auf Vorrat beim HOME Depot");
    				// hat das HOME Depot diesen ItemType?
    				Item home_Item = HOME_MatPool.get(iT);
    				if (home_Item!=null && home_Item.getAmount()>0) {
    					// Bingo
    					outText.addOutLine("MHM-MH: " + iT.getName() + " beim HOME Depot vorhanden: " + home_Item.getAmount() + " (" + iT.getWeight() + " GE/Stück)");
    					int Liefermenge_1 = Math.min(anzahl, home_Item.getAmount());
    					outText.addOutLine("MHM-MH: Liefermenge 1 (ohne Berücksichtigung Kapazität MT): " + Liefermenge_1);
    					int maxOnMT = Integer.MAX_VALUE;
    					if (iT.getWeight()>0) {
    						maxOnMT = MT.getAvailable_capacity() / (int) iT.getWeight();
    					}
    					outText.addOutLine("MHM-MH: max on MT (Berücksichtigung Kapazität MT): " + maxOnMT);
    					int Liefermenge_Final = Math.min(Liefermenge_1, maxOnMT);
    					outText.addOutLine("MHM-MH: Liefermenge Final (Berücksichtigung Kapazität MT): " + Liefermenge_Final);
    					
    					if (Liefermenge_Final>0) {
	    					// Umsetzen
	    					Liefermenge_gesamt += Liefermenge_Final;
	    					Liefermenge_MT += Liefermenge_Final;
	    					anzahl -= Liefermenge_Final;
	    					
	    					home_Item.setAmount(home_Item.getAmount() - Liefermenge_Final);
	    					HOME_MatPool.put(iT, home_Item);
	    					
	    					MT.reduceAvailable_capacity((int) (Liefermenge_Final * iT.getWeight()));
	    					outText.addOutLine("MHM-MH-neu Liefermenge Gesamt: " + Liefermenge_gesamt + ", MT-Kapa: " + MT.getAvailable_capacity() + " GE, noch im HOME verfügbar: " + home_Item.getAmount() + " Stück, verbliebender Bedarf des MH: " + anzahl + " " + gegenstand);
	    					if (anzahl<=0) {
	    						outText.addOutLine("MHM-MH - Nachfrage vollständig erfüllt, breche weitere suche ab.");
	    						break;
	    					}
    					} else {
    						outText.addOutLine("MHM-MH - Keine Umsetzung (Liefermenge=0).");
    					}
    				} else {
    					outText.addOutLine("MHM-MH: prüfe " + iT.getName() + " beim HOME Depot nicht vorhanden");
    				}
    			}
    			
    			if (Liefermenge_gesamt>0) {
    				// Umsetzen der Requests
    				MH.addFromHOME(gegenstand, Liefermenge_gesamt);
    				
    				// Ergänzen des MatPoolRequests
    				// MatPoolRequest(int _ID, Script _script,int anzahl,String gegenstand,int _prio,String _kommentar,int _kapaPolicy,int _userKapa)
    				MatPoolRequest mpr = new MatPoolRequest(-1, MT, Liefermenge_gesamt, gegenstand, this.MT_Request_Prio, "MaterialHubTransporter<-doNotRemove", MatPoolRequest.KAPA_weight,0);
    				MT.addMatPoolRequest(mpr);
    				// Eintrag in den Script befehlen
    				String gegenstand_save = gegenstand;
    				gegenstand_save = gegenstand_save.replace(" ", "_");
    				String orderText = "// script request " + Liefermenge_gesamt + " " + gegenstand_save + " " + this.MT_Request_Prio + " info=MaterialHubTransporter<-doNotRemove";
    				MT.addOrder(orderText, false);
    				outText.addOutLine("MHM-MH: setze MT order " + orderText);
    				
    				// wenn der MT noch keine Order hat, dann jetzt setzen
    				if (MT.targetRegionCoord==null) {
    					MT.addComment("MHM MT: setze Kurs zur Zielregion " + MH.scriptUnit.getUnit().getRegion().toString());
    					outText.addOutLine("MHM MT: setze Kurs zur Zielregion " + MH.scriptUnit.getUnit().getRegion().toString());
    					// MT.scriptUnit.findScriptClass("Sailto", MH.scriptUnit.getUnit().getRegion().getCoordinate().toString(","));
    					List<Region> homePath = Regions.planShipRoute(MT.scriptUnit.getUnit().getModifiedShip(), MT.gd_Script, MH.scriptUnit.getUnit().getRegion().getCoordinate());
    					
    					if (homePath==null || homePath.size()==0) {
    						outText.addOutLine("!!!! MHM-MH: Pfad nicht gefunden !!!");
    						MT.doNotConfirmOrders("!!! MHM Pfad nach " + MH.scriptUnit.getUnit().getRegion().toString() + " nicht gefunden!!!");
    					} else {
    						MT.addOrder("// NACH " + Regions.getDirections(homePath) + " ; vom MHM zugewiesen", true);
    						
    						MT.addComment("Bearbeite SailTo-script manuell");
    						Sailto ST = new Sailto();
    						
    						ArrayList<String> order = new ArrayList<String>();
    						order.add(MH.scriptUnit.getUnit().getRegion().getCoordinate().toString(","));
    						ST.setArguments(order);
    						ST.setScriptUnit(MT.scriptUnit);
    						ST.setGameData(MT.scriptUnit.getScriptMain().gd_ScriptMain);
    						if (MT.scriptUnit.getScriptMain().client!=null){
    							ST.setClient(MT.scriptUnit.getScriptMain().client);
    						}
    						ST.run_Sailto_intern();
    						MT.scriptUnit.addAScript(ST);
    						
    						
    						FFToolsRegions.addMapLine(MT.scriptUnit.getUnit().getRegion(), MH.scriptUnit.getUnit().getRegion().getCoordinate(), MaterialHubManager_MHM.MAPLINE_RGB_R, MaterialHubManager_MHM.MAPLINE_RGB_G, MaterialHubManager_MHM.MAPLINE_RGB_B, 5, MaterialHubManager_MHM.MAPLINE_TAG);
    					}
    					
    					MT.targetRegionCoord = MH.scriptUnit.getUnit().getRegion().getCoordinate();
    					
    					// Settings-String setzen
    					Direction d = Direction.INVALID;
    					if (MT.scriptUnit.getUnit().getModifiedShip()!=null){
    						d = Regions.getMapMetric( MT.gd_Script).toDirection(MT.scriptUnit.getUnit().getModifiedShip().getShoreId());
    					} 
    					int speed = MT.gd_Script.getGameSpecificStuff().getGameSpecificRules().getShipRange(MT.scriptUnit.getUnit().getModifiedShip());
    					int Reisewochen = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(MT.actRegionCoord,d, MH.scriptUnit.getUnit().getRegion().getCoordinate(), MT.gd_Script, speed, null);
    					// MaterialHubTransporterSettings Target=X,Y StartRunde=xxxx	ETA=XXXX info=doNotEdit
    					orderText = "// MaterialHubTransporterSettings Target=" + MH.scriptUnit.getUnit().getRegion().getCoordinate().toString(",") + " StartRunde=" + MT.gd_Script.getDate().getDate() + " ETA=" + (MT.gd_Script.getDate().getDate() + Reisewochen) + " info=doNotEdit";
    					MT.addOrder(orderText, false);
    					// Anmeldung des MT beim MH
    					if (MH.getTalkLevel()>=1) {
    						MH.addComment("MT startet diese Runde aus der HOME-Region: " + MT.scriptUnit.toString() + ", ETA " + Reisewochen + " Runden");
    					}
    				}
    			}
    		}
		}
		return Liefermenge_MT;
	}
	
	
	/**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
    
    public void addMH(Materialhub mh) {
    	if (!this.MHs.contains(mh)) {
    		this.MHs.add(mh);
    	}
    }
    
    // MTs in HOME-Region....
    public void addMT(Materialtransport mt) {
    	if (!this.MTs.contains(mt)) {
    		this.MTs.add(mt);
    	}
    }
    

    /*
     * liefert das registrierte MH zu einer RegionID
     */
    public Materialhub getMH(CoordinateID cID) {
    	if (this.MHs==null) {
    		return null;
    	}
    	for (Materialhub _mh:this.MHs) {
    		if (_mh.getUnit().getRegion().getID().equals(cID)) {
    			// bingo
    			return _mh;
    		}
    	}
    	return null;
    }
    
    /*
     * befüllt unser eigenes kleines MatPool-Lager mit dem Bestand der Depots der HOME-Regionen 
     */
    private void init_Home_MatpoolItems() {
    	for (CoordinateID rID:this.HOME_Regions) {
    		Region r = this.overLord.getScriptMain().gd_ScriptMain.getRegion(rID);
    		if (r!=null) {
    			outText.addOutLine("init_Home_MatpoolItems for Region " + r.toString());
    			ScriptUnit _depot = this.overLord.getScriptMain().getRegionDepot(r);
    			if (_depot!=null) {
    				outText.addOutLine("init_Home_MatpoolItems from Depot " + _depot.toString() + " in " + r.toString());
    				HashMap<ItemType, Item> duplicateMP2 = new HashMap<ItemType, Item>(_depot.getModifiedItemsMatPool2());
    				this.HOME_MatPoolItems.put(rID, duplicateMP2);
    			} else {
    				outText.addOutLine("!!!! init_Home_MatpoolItems: no Depot-ScriptUnit in " + r.toString());
    			}
    		} else {
    			outText.addOutLine("!!!!  init_Home_MatpoolItems for Region " + rID.toString() + " Region not found!");
    		}
    	}
    }
    
    private void init_Materialscores() {
    	
    	/** Tränke **/
    	add_MaterialScore("Wundsalbe", 0.1,true);
    	add_MaterialScore("Berserkerblut", 0.1,true);
    	add_MaterialScore("Siebenmeilentee", 0.1,true);
    	add_MaterialScore("Trank der Wahrheit", 0.1,true);
    	add_MaterialScore("Goliathwasser", 0.1,true);
    	add_MaterialScore("Wasser des Lebens", 0.1,true);
    	add_MaterialScore("Bauernblut", 0.1,true);
    	add_MaterialScore("Schaffenstrunk", 2.0,true);
    	add_MaterialScore("Dumpfbackenbrot", 0.01,true);
    	add_MaterialScore("Gehirnschmalz", 5.0,true);
    	add_MaterialScore("Pferdeglück", 0.05,true);
    	add_MaterialScore("Nestwärme", 1.0,true);
    	add_MaterialScore("Bauernlieb", 0.01,true);
    	add_MaterialScore("Elixier der Macht", 10.0,true);
    	add_MaterialScore("Heiltrank", 0.5,true);
    	add_MaterialScore("Tränke", 0.1,true);
    	add_MaterialScore("Trank", 0.1,true);
    	
    	/** Items **/
    	add_MaterialScore("Amulett des wahren Sehens", 5.0,true);
    	add_MaterialScore("Ring der Unsichtbarkeit", 0.6,true);
    	add_MaterialScore("Gürtel der Trollstärke", 0.6,true);
    	add_MaterialScore("Sphäre der Unsichtbarkeit", 0.8,true);
    	add_MaterialScore("Ring der Macht", 20.1,true);
    }
    
    
    
    public double getMaterialScore(String s) {
    	double res = 1;
    	Double v = this.MaterialScores.get(s);
    	if (v != null) {
    		return v.doubleValue();
    	}
    	return res;
    }
    
    /**
     * Fügt die Werte zur MaterialScore Map hinzu
     * Liefert
     * 1 -  einfach ergänzt
     * 2 -  vorhandenen Wert ersetzt
     * -1 - gescheitert, weil nicht überschrieben werden darf
     * @param Gegenstand
     * @param faktor
     * @return
     */
    public int add_MaterialScore(String Gegenstand, double faktor, boolean noOverWrite) {
    	int returnvalue=1;
    	
    	if (this.MaterialScores.containsKey(Gegenstand)) {
    		returnvalue=2;
    		if (noOverWrite) {
    			return -1;
    		}
    	}
    	this.MaterialScores.put(Gegenstand, faktor);
    	return returnvalue;
    }
    
    
    
    
    
}
