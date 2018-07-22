package com.fftools.utils;

import magellan.library.Battle;
import magellan.library.CoordinateID;
import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Island;
import magellan.library.Message;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.impl.MagellanMessageImpl;

import com.fftools.OutTextClass;

/**
 * Soll wichtige Informationen zu einer Insel zusammensuchen und 
 * darstellen 
 * @author Fiete
 *
 */
public class IslandInfo {
	public static final OutTextClass outText = OutTextClass.getInstance();
	
	private static  StringBuilder feinde = new StringBuilder();
	private static  StringBuilder neueUntote = new StringBuilder();
	private static  StringBuilder hunger = new StringBuilder();
	private static  StringBuilder kampf = new StringBuilder();
	
	public static StringBuilder makeIslandInfo(GameData data, ID islandID){
		StringBuilder sb = new StringBuilder();
		Island island = data.getIsland(islandID);
		if (island==null){
			sb.append("Insel nicht gefunden (ID:" + islandID.toString() + ")\r\n");
		} else {
			sb.append("Inselinfo zur Insel: " + island.getName()+"\r\n");
			feinde = new StringBuilder();
			hunger = new StringBuilder();
			kampf = new StringBuilder();
			neueUntote = new StringBuilder();
			for (Region r : island.regions()){
				examineRegion(r);
				searchBattleInRegion(data,r);
				searchHungerInRegion(data,r);
				searchFeindeInRegion(data,r);
			}
		}
		
		
		// null setzen, wenn nix zu melden
		int counter = 0;
		counter += feinde.length();
		counter += hunger.length();
		counter += kampf.length();
		counter += neueUntote.length();
		
		if (counter==0){
			sb = new StringBuilder();
			return sb;
		}
		
		
		sb.append("\r\n");
		
		if (neueUntote.length()>0){
			sb.append("Neue Untote:\r\n" + neueUntote.toString()+"\r\n");
		} else {
			sb.append("Keine neuen Untoten\r\n");
		}
		
		sb.append("************************\r\n");
		
		if (feinde.length()>0){
			sb.append("Monster/Parteigetarnte/Spione:\r\n" + feinde.toString()+"\r\n");
		} else {
			sb.append("Keine Monster/Parteigetarnte/Spione\r\n");
		}
		
		sb.append("************************\r\n");
		
		if (kampf.length()>0){
			sb.append("Kaempfe:\r\n" + kampf.toString()+"\r\n");
		} else {
			sb.append("Keine Kämpfe\r\n");
		}
		
		sb.append("************************\r\n");
		
		if (hunger.length()>0){
			sb.append("Hunger:\r\n" + hunger.toString()+"\r\n");
		} else {
			sb.append("Kein Hunger\r\n");
		}
		
		return sb;
	}
	
	/**
	 * Untersucht die Region und befüllt die StringBuilder
	 * @param r
	 */
	private static void examineRegion(Region r){
		if (r.getMessages()==null){
			return;
		}
		for (Message m : r.getMessages()){
			MagellanMessageImpl magM = (MagellanMessageImpl)m;
			if (!(magM.getID().intValue()<=0)){
				examineMessage(r,magM);
			}
		}
		
	}
	
	private static void examineMessage(Region r, MagellanMessageImpl m){
		// Untote erheben sich:
		if (m.getID().intValue()==798169913){
			neueUntote.append(m.getText() + "\r\n");
		}
		
		// Unterernährung 1158830147
		if (m.getID().intValue()==1158830147){
			hunger.append(m.getText() + "\r\n");
		}
		
		// Schwächung 829394366
		if (m.getID().intValue()==829394366){
			hunger.append(m.getText() + "\r\n");
		}
		
		
		// Verhungerte Bauern
		if (m.getID().intValue()==1195097758){
			hunger.append(r.getName()+": " + m.getText() + "\r\n");
		}
	}
	
	/**
	 * Kämpfe werden bei den Fractionen geführt...
	 * 
	 * @param data
	 * @param r
	 */
	private static void searchBattleInRegion(GameData data,Region r){
		for (Faction f : data.getFactions()){
			if (f.getBattles()!=null && f.getBattles().size()>0){
				for (Battle b : f.getBattles()){
					CoordinateID c = b.getID();
				    Region r2 = data.getRegion(c);
				    if (r2.equals(r)){
				    	// Bingo
				    	kampf.append("Kampf in " + r.getName() + "\r\n");
				    	return;
				    }
				}
			}
		}
	}
	/**
	 * Hungermeldg werden bei den Fractionen geführt...
	 * 
	 * @param data
	 * @param r
	 */
	private static void searchHungerInRegion(GameData data,Region r){
		for (Faction f : data.getFactions()){
			if (f.getMessages()!=null && f.getMessages().size()>0){
				for (Message m : f.getMessages()){
					MagellanMessageImpl msg = (MagellanMessageImpl)m;
					if (msg.getAttributes() != null) {
			            String regionCoordinate = msg.getAttributes().get("region");

			            if (regionCoordinate != null) {
			              ID coordinate = CoordinateID.parse(regionCoordinate, ",");

			              if (coordinate == null) {
			                coordinate = CoordinateID.parse(regionCoordinate, " ");
			              }
			              
			              if (coordinate.equals(r.getCoordinate())){
			            	  // Message für unsere Region
			            	  
			            	// Unterernährung 1158830147
			          		if (msg.getMessageType().getID().intValue()==1158830147){
			          			hunger.append(msg.getText() + "\r\n");
			          		}
			          		
			          		// Schwächung 829394366
			          		if (msg.getMessageType().getID().intValue()==829394366){
			          			hunger.append(msg.getText() + "\r\n");
			          		} 
			              }
			              
			           }
					}
				}
			}
			
			
		}
	}
	
	
	/**
	 * Durchsucht die Region nach Monster und Parteigetarnten
	 * @param data
	 * @param r
	 */
	private static void searchFeindeInRegion(GameData data, Region r){
		int countMonster=0; 
		int countParteigetarnt=0;
		int countUnbekannt = 0;
		int countSpy = 0;
		if (r.units()==null){
			return;
		}
		for (Unit u : r.units()){
			if (u.isHideFaction()){
				countParteigetarnt += 1;
			} else {
				if (u.isSpy()){
					countSpy+=1;
				} else {
					Faction f2 = u.getFaction();
					if (f2==null){
						countUnbekannt+=1;
					} else {
						// Monster?
						if (f2.getID().toString().equals("ii")){
							countMonster+=1;
						}
					}
				}
			}
		}
		
		if ((countMonster + countParteigetarnt + countSpy + countUnbekannt) > 0){
			feinde.append("In " + r.getName() + ": " + countMonster + " Monster");
			feinde.append(", " + countParteigetarnt + " Parteigetarnte");
			feinde.append(", " + countSpy + " Spione");
			feinde.append(", " + countUnbekannt + " Unbekannte(?)");
			feinde.append("\r\n");
		}
	}
	
	/**
	 * erzeugt für jede Insel ne File - test
	 * @param path
	 */
	public static void writeInfos(GameData data){
		Integer i ;
		String s;
		for (Island island : data.getIslands()){
			try {
				i = Integer.valueOf(island.getName());
			} catch (NumberFormatException e){
				i = 0;
			}
			if (i>0){
				outText.addOutLine("skipping " + island.getName());
			} else {
				s = makeIslandInfo(data, island.getID()).toString();
				if (s.length()>0){
					outText.setFile("IslandInfo_" + island.getName());
					outText.addOutLine(s);
					outText.setFileStandard();
				} else {
					outText.addOutLine("no news " + island.getName());
				}
			}
		}
	}
	
}
