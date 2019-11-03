package com.fftools.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;

import magellan.library.Border;
import magellan.library.Building;
import magellan.library.CoordinateID;
import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Rules;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.gamebinding.EresseaMapMetric;
import magellan.library.rules.CastleType;
import magellan.library.rules.ItemType;
import magellan.library.rules.RegionType;
import magellan.library.rules.SkillType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;
import magellan.library.utils.Umlaut;
import magellan.library.utils.logging.Logger;

/**
 * Regionenhandling analog zu com.eressea.utils.Regions
 * @author Fiete
 *
 */
public class FFToolsRegions {
	public static final OutTextClass outText = OutTextClass.getInstance();
	
	private static long cntCacheHits = 0;
	private static long cntCacheRequests = 0;
	
	private static boolean notUseRegionNames = false;
	
	/**
	 * a cache for the calls to getPathDistLand with no(!) nextstepInfo!
	 */
	private static HashMap<PathDistLandInfo,Integer> pathDistCache = null;
	
	/**
	 * TH: A translation map of region names to region coordinates
	 */

	private static HashMap<String,CoordinateID> regionMap = null;
	
	public static Region nextShipStop = null;
	
	
	/**
	 * Gibt es eine Koordinate in den übergebenen Regionen?
	 * @param regions die übliche magellan map <ID,Region>
	 * @param c zu prüfende Region Coord (zu gebrauchen wenn bspw. geparst
	 * @return
	 */
	public static boolean isInRegions(Collection<Region> regions,CoordinateID c){
		if (regions==null || c==null) {return false;}
		for (Region r:regions){
			if (r.getCoordinate().equals(c)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * liefert den Abstand zweier Regionen in Anzahl Runden
	 * berücksichtigt dabei Strassen
	 * @param regions Zur Verfügung stehende Regionen...
	 * @param von Startregion
	 * @param nach Zielregion
	 * @param reitend  reitet die Einheit oder zu Fuss?
	 * @param nextHold wenn Übergeben, wird darin FFToolsCoordinateID des nächsten Halts der Unit abgelegt
	 * @return Anzahl benötigter Runden
	 */
	public static int getPathDistLand(GameData data, CoordinateID von, CoordinateID nach,boolean reitend,FFToolsCoordinateID nextHold){
		if (!data.getRegions().contains(data.getRegion(von))){
			return -1;
		}
		if (!data.getRegions().contains(data.getRegion(nach))){
			return -1;
		}
		if (von.equals(nach)){
			return 0;
		}
		
		// Path organisieren
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(data.getRules());
		RegionType Feuerwand = Regions.getFeuerwandRegionType(data);
		excludeMap.put(Feuerwand.getID(), Feuerwand);
		// String path = Regions.getDirections(data, von, nach, excludeMap,1);
		String path =  Regions.getDirections(Regions.getLandPath(data, von, nach, excludeMap,2, 3));
		if (path==null || path.length()==0) {
			return -1;
		}
		
		int bewegungsPunkte = 4;
		if (reitend){
			bewegungsPunkte = 6;
		}
		
		int anzRunden = 1;
		int restBewegunspunkte = bewegungsPunkte;
		String[] dirs = path.split(" ");
		int step = 0;
		CoordinateID lastCoord = null;
		CoordinateID actCoord = null;
		lastCoord = (CoordinateID)von.clone();
		actCoord = (CoordinateID)von.clone();
		boolean reached = false;
		boolean nextHoldSet = false;
		while (!reached){
			String actDir = dirs[step];
			Direction actDirInt = FFToolsRegions.getDirectionFromString(actDir);
			CoordinateID moveCoord = CoordinateID.create(actDirInt.toCoordinate());
			actCoord = actCoord.translate(moveCoord);
			
			int notwBewPunkte = 3;
			Region r1 = (Region)data.getRegion(lastCoord);
			Region r2 = (Region)data.getRegion(actCoord);
			
			if(r1==null || r2==null){
				return -1;
			}
			if (Regions.isCompleteRoadConnection(r1, r2)){
				notwBewPunkte = 2;
			}
			restBewegunspunkte-=notwBewPunkte;
			if (restBewegunspunkte<0) {
				anzRunden++;
				restBewegunspunkte = bewegungsPunkte-notwBewPunkte;
				nextHoldSet = true;
			} else {
				if (!nextHoldSet && nextHold!=null){
					nextHold.setToCoordinateID(actCoord);
				}
			}
			
			if (actCoord.equals(nach)){
				reached = true;
			} else {
				// schieben
				lastCoord = CoordinateID.create(actCoord);
				step++;
			}
		}
		
		return anzRunden;
	}
	
	/**
	 * ersetzt alte Funktion in magellan.Direction
	 * @param s
	 * @return
	 */
	public static Direction getDirectionFromString(String s){
		Direction erg = Direction.INVALID;
		
		if (s.equalsIgnoreCase("NW")){
			erg = EresseaMapMetric.NW;
		}
		if (s.equalsIgnoreCase("NO") || s.equalsIgnoreCase("NE")){
			erg = EresseaMapMetric.NE;
		}
		if (s.equalsIgnoreCase("O") || s.equalsIgnoreCase("E")){
			erg = EresseaMapMetric.E;
		}
		if (s.equalsIgnoreCase("SO") || s.equalsIgnoreCase("SE")){
			erg = EresseaMapMetric.SE;
		}
		if (s.equalsIgnoreCase("SW")){
			erg = EresseaMapMetric.SW;
		}
		if (s.equalsIgnoreCase("W")){
			erg = EresseaMapMetric.W;
		}
		return erg;
	}
	
	
	/**
	 * liefert komplette GotoInfo
	 * berücksichtigt dabei Strassen
	 * @param regions Zur Verfügung stehende Regionen...
	 * @param von Startregion
	 * @param nach Zielregion
	 * @param reitend  reitet die Einheit oder zu Fuss?
	 * @return GotoInfo
	 */
	public static GotoInfo getPathDistLandGotoInfo(GameData data, CoordinateID von, CoordinateID nach,boolean reitend){
		if (!data.getRegions().contains(data.getRegion(von))){
			return null;
		}
		if (!data.getRegions().contains(data.getRegion(nach))){
			return null;
		}
		GotoInfo erg = new GotoInfo();
		erg.setDestRegion(data.getRegion(nach));
		if (von.equals(nach)){
			erg.setAnzRunden(0);
			return erg;
		}
		
		// Path organisieren
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(data.getRules());
		RegionType Feuerwand = Regions.getFeuerwandRegionType(data);
		excludeMap.put(Feuerwand.getID(), Feuerwand);
		// String path = Regions.getDirections(data, von, nach, excludeMap,1);
		String path =  Regions.getDirections(Regions.getLandPath(data, von, nach, excludeMap,2, 3));
		if (path==null || path.length()==0) {
			return null;
		}
		erg.setPath(path);
		int bewegungsPunkte = 4;
		if (reitend){
			bewegungsPunkte = 6;
		}
		
		int anzRunden = 1;
		int restBewegunspunkte = bewegungsPunkte;
		String[] dirs = path.split(" ");
		int step = 0;
		CoordinateID lastCoord = null;
		CoordinateID actCoord = null;
		lastCoord = (CoordinateID)von.clone();
		actCoord = (CoordinateID)von.clone();
		CoordinateID nextHold = CoordinateID.create(0, 0);
		Region lastHoldRegion = data.getRegion(von);
		
		boolean reached = false;
		boolean nextHoldSet = false;
		while (!reached){
			String actDir = dirs[step];
			Direction actDirInt = FFToolsRegions.getDirectionFromString(actDir);
			CoordinateID moveCoord = CoordinateID.create(actDirInt.toCoordinate());
			actCoord = actCoord.translate(moveCoord);

			int notwBewPunkte = 3;
			Region r1 = (Region)data.getRegion((lastCoord));
			Region r2 = (Region)data.getRegion((actCoord));
			if(r1==null || r2==null){
				return null;
			}
			if (Regions.isCompleteRoadConnection(r1, r2)){
				notwBewPunkte = 2;
			}
			restBewegunspunkte-=notwBewPunkte;
			if (restBewegunspunkte<0) {
				erg.setPathElement(anzRunden-1, lastHoldRegion, data.getRegion(lastCoord));
				lastHoldRegion = data.getRegion(lastCoord);
				anzRunden++;
				restBewegunspunkte = bewegungsPunkte-notwBewPunkte;
				nextHoldSet = true;
				erg.setNextHold(data.getRegion(nextHold));
			} else {
				if (!nextHoldSet && nextHold!=null){
					nextHold = CoordinateID.create(actCoord);
				}
			}
			
			if (actCoord.equals(nach)){
				reached = true;
				erg.setPathElement(anzRunden-1, lastHoldRegion, data.getRegion(actCoord));
			} else {
				// schieben
				lastCoord = CoordinateID.create(actCoord);
				step++;
			}
		}
		erg.setAnzRunden(anzRunden);
		return erg;
	}
	
	
	/**
	 * Aufruf ohne CoordinateID für ersten Aufenthalt
	 * @param data
	 * @param von
	 * @param nach
	 * @param reitend
	 * @return
	 */
	public static int getPathDistLand(GameData data, CoordinateID von, CoordinateID nach,boolean reitend){
		// cache check
		cntCacheRequests++;
		if (pathDistCache==null){
			pathDistCache = new HashMap<PathDistLandInfo, Integer>();
		}
		// schon vorhanden ?
		for (Iterator<PathDistLandInfo> iter = pathDistCache.keySet().iterator();iter.hasNext();){
			PathDistLandInfo info = (PathDistLandInfo)iter.next();
			if (info.is(data, von, nach) || info.is(data, nach, von)){
				// Treffer
				Integer actI = pathDistCache.get(info);
				cntCacheHits++;
				return actI.intValue();
			}
		}
		
		int retVal =  getPathDistLand(data, von, nach, reitend,null);
		// in den Cache
		PathDistLandInfo neueInfo = new PathDistLandInfo(data,von,nach);
		Integer cacheValue = Integer.valueOf(retVal);
		pathDistCache.put(neueInfo,cacheValue);
		return retVal;
	}
	
	
	public static GotoInfo makeOrderNACH(ScriptUnit u,CoordinateID act,CoordinateID dest,boolean writeOrders,String originInfo){
		
		//	FF 20070103: eingebauter check, ob es actDest auch gibt?!
		if (!isInRegions(u.getScriptMain().gd_ScriptMain.getRegions(), dest)){
			// Problem  actDest nicht im CR -> abbruch
			u.doNotConfirmOrders("Goto Ziel nicht im CR");
			outText.addOutLine("!!! Goto Ziel nicht im CR: " + u.unitDesc());
			return null;
		} 
		
		GotoInfo erg = new GotoInfo();
		
		erg.setDestRegion(u.getScriptMain().gd_ScriptMain.getRegion(dest));
		
		Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(u.getScriptMain().gd_ScriptMain.getRules());
		RegionType Feuerwand = Regions.getFeuerwandRegionType(u.getScriptMain().gd_ScriptMain);
		excludeMap.put(Feuerwand.getID(), Feuerwand);
		String path =  Regions.getDirections(Regions.getLandPath(u.getScriptMain().gd_ScriptMain, act, dest, excludeMap,2, 3));
		if (path!=null && path.length()>0) {
			// path gefunden
			if (writeOrders){
				u.addOrder("NACH " + path, true);
				String info = "Einheit durch GOTO bestätigt.";
				if ((originInfo.length())>1) {
					info = info + "(" + originInfo + ")";
				}
				
				u.addComment(info ,true);
			}
			erg.setPath(path);
			
			// Testing anzRunden
			boolean reitend = false;
			if (u.getPayloadOnHorse()>=0){
				reitend = true;
			}
			FFToolsCoordinateID nextHold = FFToolsCoordinateID.create(0,0,0);
			int _anzRunden = FFToolsRegions.getPathDistLand(u.getScriptMain().gd_ScriptMain, act, dest, reitend,nextHold);
			if (_anzRunden>0){
				CoordinateID _nextRegionCoord = CoordinateID.create(nextHold.getX(),nextHold.getY(),nextHold.getZ());
				Region _nextHoldRegion = u.getScriptMain().gd_ScriptMain.getRegion(_nextRegionCoord);
				if (_nextHoldRegion!=null){
					if (writeOrders){
					  u.addComment("Nächster Halt in " + _nextHoldRegion.toString(),true);
					}
					erg.setNextHold(_nextHoldRegion);
				}
				if (writeOrders){
					u.addComment("Erwartete Ankunft am EndZiel in " + _anzRunden + " Runden",true);
				}
				erg.setAnzRunden(_anzRunden);
			} else {
				if (writeOrders){
					u.addComment("Anzahl Runden: " + _anzRunden + " (?)",true);
				}
			}
		} else {
			// path nicht gefunden
			u.doNotConfirmOrders("Es konnte kein Weg gefunden werden. (GOTO)");
			outText.addOutLine("X....kein Weg gefunden für " + u.unitDesc());
		}
		return erg;
	}
	
	public static void informUs(){
		String erg = "PathDistCache:";
		
		if (pathDistCache==null || pathDistCache.size()==0){
			erg += " unbenutzt";
		} else {
			erg += " " + pathDistCache.size() + " Datensätze";
			erg += ", Hits:" + cntCacheHits + "/" + cntCacheRequests;
		}
		outText.addOutLine(erg);
	}
	
	/**
	 * Liefert die Anzahl von ScriptPersonen in einer Region
	 * @param scriptUnits
	 * @param r
	 * @return
	 */
	public static int countScriptPersons(Hashtable<Unit,ScriptUnit> scriptUnits,Region r){
		int erg = 0;
		if (scriptUnits==null || scriptUnits.size()==0 || r==null){
			return 0;
		}
		
		for (Iterator<Unit> iter = scriptUnits.keySet().iterator();iter.hasNext();){
			Unit u = (Unit)iter.next();
			if (u.getRegion().equals(r)){
				erg+=u.getModifiedPersons();
			}
		}
		
		return erg;
	}
	
	/**
	 * liefert die fehlende Steinanzahl für angegebene Richtung
	 * in angegebener Region
	 * @param r Region
	 * @param direction int 
	 * @param data GameData
	 * @return
	 */
	public static int getMissingStreetStones(Region r, int direction,GameData data){
		
		// Anzahl Sollsteine ermitteln
		
		int anzahlSollSteine = ((RegionType)r.getType()).getRoadStones();
		
		if (anzahlSollSteine<=0){
			// kein Strassenbau möglich ?!
			return anzahlSollSteine;
		}
		
		// Grenze finden
		Border b = null;
		boolean borderfind = false;
		for(Iterator<Border> iter = r.borders().iterator(); iter.hasNext();) {
			b = (Border) iter.next();
			if(Umlaut.normalize(b.getType()).equals("STRASSE") &&
					   (b.getDirection() == direction)) {
				borderfind = true;
				break;
			}
		}
		
		if (!borderfind){
			// keine Grenze vorhanden
			return anzahlSollSteine;
		}
		// Gefundene Grenze in b
		int fertigProzent = b.getBuildRatio();
		if (fertigProzent==-1){
			// fehler im CR ?!
			return -2;
		}
		
		if (fertigProzent==100){
			// abkürzen
			return 0;
		}
		
		// berechnen
		double verbauteSteineDBL = ((double)fertigProzent/(double)100)* (double)anzahlSollSteine;
		// abrunden
		int verbauteSteine = (int)Math.floor(verbauteSteineDBL);
		return anzahlSollSteine - verbauteSteine;
	}
	
	
	/**
	 * liefert aus einer Region das passende Buildungm wenn vorhanden
	 * @param r
	 * @param number
	 * @return
	 */
	public static Building getBuilding(Region r,String number){
		Building b=null;
		if (r.buildings()!=null && r.buildings().size()>0){
			for (Iterator<Building> iter = r.buildings().iterator();iter.hasNext();){
				Building actB = (Building)iter.next();
				if (actB.getID().toString().equalsIgnoreCase(number)){
					return actB;
				}
			}
		}
		
		return b;
	}
	
	/**
	 * liefert Größte Burg der Region oder null, wenn es gar keine gibt.
	 * @param r
	 * @return
	 */
	public static Building getBiggestCastle(Region r){
		int maxFoundSize=0;
		Building actB=null;
		for (Iterator<Building> iter = r.buildings().iterator();iter.hasNext();){
			Building b = (Building)iter.next();
			if (b.getBuildingType() instanceof CastleType) {
				// Das oder ein zielobject
				if (b.getSize()>maxFoundSize){
					maxFoundSize = b.getSize();
					actB=b;
				}
			}
		}
		return actB;
	}
	
	
	/**
	 * Returns a map of all RegionTypes that are flagged as <tt>ocean</tt>.
	 *
	 * @param rules Rules of the game
	 *
	 * @return map of all ocean RegionTypes
	 */
	public static Map<ID,RegionType> getNonOceanRegionTypes(Rules rules) {
		Map<ID,RegionType> ret = new Hashtable<ID, RegionType>();

		for(Iterator<RegionType> iter = rules.getRegionTypeIterator(); iter.hasNext();) {
			RegionType rt = (RegionType) iter.next();

			if(!rt.isOcean()) {
				ret.put(rt.getID(), rt);
			}
		}

		return ret;
	}
	
	
	/**
	 * liefert nächsthöchste Ausbaustufe 
	 * @param actSize
	 * @return
	 */
	public static int getNextCastleSize(int actSize){
		int nextS = 6250;
		if (actSize<10){
			nextS = 10;
		} else if (actSize<50){
			nextS = 50;
		} else if (actSize<250){
			nextS = 250;
		} else if (actSize<1250){
			nextS = 1250;
		} else if (actSize<6250){
			nextS = 6250;
		}
		
		return nextS;
	}
	
	/**
	 * liefert nächste Ausbaustufe der Region
	 * @param r
	 * @return
	 */
	public static int getNextCastleSize(Region r){
		int erg=0;
		Building b = FFToolsRegions.getBiggestCastle(r);
		if (b!=null){
			// Es gibt eines....Steine bis zum nächsten Level?
			int actSize = b.getSize();
			erg = getNextCastleSize(actSize);
		} else {
			// Neubau -> bis 10
			erg = 10;
		}
		return erg;
	}
	
	
	/**
	 * liefert Wert eines zusätzlich verbauten Steines in die Burg der Region bis zur 
	 * Erreichung des nächsten Levels in Silberstücken
	 * @param r
	 * @return
	 */
	public static double getValueOfBuiltStones(Region r){
		
		if (r.getModifiedPeasants()<=0){
			return 0;
		}
		int stones = 0;
		Building b = FFToolsRegions.getBiggestCastle(r);
		if (b!=null){
			// Es gibt eines....Steine bis zum nächsten Level?
			int actSize = b.getSize();
			stones = getNextCastleSize(actSize)-actSize;
			if (stones<=0){
				return 0;
			}
		} else {
			// Neubau -> bis 10
			// Anzahl Bauern durch 10 Steine
			stones = 10;
		}
		return ((double)r.getModifiedPeasants()/stones);
	}
	
	/**
	 * Anzahl von ItemType bei allen script(!)units der Region
	 * @param r
	 * @param type
	 * @return
	 */
	public static int getNumberOfItemsInRegion(Region r, ItemType type, ScriptMain scriptMain){
		if (r.units()==null){
			return 0;
		}
		int erg = 0;
		for (Unit u:r.units()){
			ScriptUnit su = scriptMain.getScriptUnit(u);
			if (su!=null){
				Item item=u.getModifiedItem(type);
				if (item!=null){
					erg+=item.getAmount();
				}
			}
		}
		return erg;
	}
	
	/**
	 * Anzahl von SkillType bei allen script(!)units der Region
	 * @param r
	 * @param type
	 * @return
	 */
	public static int getNumberOfTalentInRegion(Region r, SkillType type, ScriptMain scriptMain){
		if (r.units()==null){
			return 0;
		}
		int erg = 0;
		for (Unit u:r.units()){
			ScriptUnit su = scriptMain.getScriptUnit(u);
			if (su!=null){
				Skill  skill=u.getModifiedSkill(type);
				if (skill!=null){
					erg+=(skill.getLevel() * u.getModifiedPersons());
				}
			}
		}
		return erg;
	}

	/**
	 * TODO Inserted by TH
	 * Returns coordinates of the region whose name was passed as argument, or NULL if not found
	 * @param data
	 * @param regionName
	 * @return
	 */
	public static CoordinateID getRegionCoordFromName (GameData data, String regionName) {
		String currentName = null;
		CoordinateID currentCoord = null;
		
		// usage of RegionNames as switch via reportSettiungs
		ReportSettings reportSettings = ReportSettings.getInstance();
		if (reportSettings.getOptionBoolean("notUseRegionNames")){
			notUseRegionNames=true;
		} else {
			notUseRegionNames=false;
		}
		
		
		if (notUseRegionNames){
			return null;
		}
		
		// be aware of spaces in the name
		// FFTools wide consept: use '_' for spaces (Ring_der_Unsichtbarkeit)
		// we have the correct names in the map - we need to replace '_' with ' ' 
		// in  the search string
		regionName = regionName.replace('_',' ');
		
		
		// If the translation map has not yet been initialized, do it:
		if (regionMap==null) {
			outText.addOutLine("Erzeuge neue Map RegionNames");
			System.out.println("Erzeuge neue Map RegionNames\n");
			regionMap = new HashMap<String, CoordinateID>();
			for (Region r:data.getRegions()){
				currentName = r.getName();
				currentCoord = r.getCoordinate();
				regionMap.put(currentName, currentCoord);
			}
			outText.addOutLine("Map RegionNames erzeugt");
			System.out.println("Map RegionNames erzeugt\n");
		}
		// Translation map has been initialized, now return the coordinate for the name
		// CAREFUL WITH THE RESULT: May be NULL if region name cannot be found!
		return regionMap.get(regionName.replace("_", " "));
	}

	public static boolean isNotUseRegionNames() {
		return notUseRegionNames;
	}

	public static void setNotUseRegionNames(boolean useRegionNames) {
		FFToolsRegions.notUseRegionNames = useRegionNames;
	}
	
	/**
	 * simuliert eine Schiffsbewegung, liefert die Entfernung
	 * @param from
	 * @param to
	 * @return
	 */
	public static int getShipPathSize_Virtuell(CoordinateID from, CoordinateID to,GameData data, int speed){
		
		Region fromR = data.getRegion(from);
		if (fromR==null){return -1;}
		
		List<Region> pathL = Regions.planShipRoute(data,from,Direction.INVALID,to,speed);
		if (pathL==null || pathL.size()<=0){
			return -1;
		}
		int dist = pathL.size()-1;
		
		return dist;
	}
	
	/**
	 * simuliert eine Schiffsbewegung, liefert die Rundenanzahl
	 * @param from
	 * @param to
	 * @return
	 */
	public static int getShipPathSizeTurns_Virtuell(CoordinateID from, CoordinateID to,GameData data, int speed){
		int turns = getShipPathSize_Virtuell(from,to,data,speed);
		if (turns<=0){
			return -1;
		}
		turns = (int)Math.ceil(((double)turns)/speed); 
		return turns;
	}
	
	/**
	 * simuliert eine Schiffsbewegung, liefert die Rundenanzahl, berücksichtigt Häfen (Ende der Bewegung für diese Runde)
	 * @param from
	 * @param to
	 * @param data
	 * @param speed
	 * @return
	 */
	public static int getShipPathSizeTurns_Virtuell_Ports(CoordinateID from, Direction returnDirection, CoordinateID to,GameData data, int speed, Logger log){
		
		FFToolsRegions.nextShipStop=null;
		
		Region fromR = data.getRegion(from);
		if (fromR==null){return -1;}
		
		List<Region> pathL = Regions.planShipRoute(data,from,returnDirection,to,speed);
		if (pathL==null || pathL.size()<=0){
			return -1;
		}
		
		int runden=0;
		int actDist=0;
		// nun mal doch druchgehen und mitzählen, bei Landkontakt endet die Runde
		Region lastRegion = null;
		for (Region r:pathL){
			if (r.getCoordinate().equals(from)){
				// Startpunkt: nix machen
				if (log!=null){
					log.info("getShipPathSize: start region " + r.toString());
				}
				continue;
			}
			actDist+=1;
			if (runden==0){
				runden=1;
			}
			if (log!=null){
				log.info("getShipPathSize: step next region, actMovement " + actDist + ", "+ r.toString() + ", Runden " + runden);
			}
			
			if (actDist>speed){
				actDist=1;
				runden++;
				if (lastRegion!=null && runden==2) {
					FFToolsRegions.nextShipStop=lastRegion;
				}
				if (log!=null){
					log.info("getShipPathSize: reached speed, actDist to 1, Runden: " + runden);
				}
			}
			// wenn an land und nicht am ziel und nicht sowieso am Bewegungsende, dann Bewegung hier enden lassen
			if (!r.getCoordinate().equals(to)){
				RegionType RT = r.getRegionType();
				if (RT!=null && !RT.isOcean()){
					// wir kommen an land...und sind nicht am Ziel -> Hafen (?)
					actDist=0;
					runden++;
					if (lastRegion!=null && runden==2) {
						FFToolsRegions.nextShipStop=r;
					}
					if (log!=null){
						log.info("getShipPathSize: reached LAND, actDist to 0, Runden: " + runden);
					}
				}
			}
			lastRegion = r;
		}
		if (log!=null){
			log.info("getShipPathSize: finished. Runden: " + runden);
		}
		if (runden==1) {
			FFToolsRegions.nextShipStop=lastRegion;
		}
		return runden;
	}
	
	/**
	 * Fügt die Region r zur Liste der ausgeschlossenen Regionen hinzu
	 * Diese Region wird nicht mehr für das PathFinding zur Verfügung stehen
	 * @param r
	 */
	public static void excludeRegion(Region r){
		if (Regions.excludedRegions==null){
			Regions.excludedRegions = new ArrayList<Region>();
		}
		if (!Regions.excludedRegions.contains(r)){
			Regions.excludedRegions.add(r);
		}
	}
	
	/**
	 * Bewertet die Einheiten in der Region, ob feindlich gesinnte dabei sind
	 * freindlich sind
	 * 	- Monster, ausser eigene (aus dem Report bekannt oder zu einer trusted faction gehörend)
	 *  - Spione
	 *  - alle anderen Einheiten, die nicht zu den trusted factions gehören
	 * @param r
	 * @return the number of the enemy-unit or "" of no enemy present
	 */
	public static String isEnemyInRegion(Region r, ScriptUnit su, boolean guardingOnly) {
		String erg = "";
		
		String[] trustedFactions = null;
		ReportSettings reportSettings = ReportSettings.getInstance();
		String tF = reportSettings.getOptionString("TrustedFactions", r);
		if (tF==null) tF = "";
		if (su!=null) su.addComment("isEnemyInRegion: TrustedFactions=" + tF);
		trustedFactions = tF.split(",");
		List<String> trustedFactionList = Arrays.asList(trustedFactions);
		
		
		for (Unit u : r.units()) {
			if (su!=null) su.addComment("isEnemyInRegion: checking " + u.toString(),false);
			// wir kennen den Kampfstatus - kann nicht feindlich sein
			if (u.getCombatStatus() != -1) {
				if (su!=null) su.addComment("isEnemyInRegion: own unit ("  + u.getCombatStatus() + ")",false);
				continue;
			}
			
			// wenn nicht bewacht, aber nur bewachende Zählen sollen....
			if (guardingOnly && !(u.getGuard()>0)) {
				continue;
			}
			
			// Monster
			if (u.getFaction()!=null && u.getFaction().getID().toString().equals("ii")) {
				erg=u.toString(true);
				if (su!=null) su.addComment("isEnemyInRegion: Monster!",false);
				break;
			}
			// Spion
			if (u.isSpy() && u.getCombatStatus() == EresseaConstants.CS_INIT) {
				if (su!=null) su.addComment("isEnemyInRegion: Spy!",false);
				erg=u.toString(true);
				break;
			}
			
			String actF = u.getFaction().getID().toString().toLowerCase();
			// Do we trust this faction?
			if (!trustedFactionList.contains(actF)) {
				erg=u.toString(true);
				if (su!=null) su.addComment("isEnemyInRegion: not trusted faction: " + actF,false);
				break;
			} else {
				if (su!=null) su.addComment("isEnemyInRegion: a trusted faction: " + actF,false);
			}
		}
		if (su!=null) su.addComment("isEnemyInRegion: returning=" + erg,false);
		return erg;
	}
	
}
