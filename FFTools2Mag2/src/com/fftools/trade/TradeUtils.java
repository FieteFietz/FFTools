package com.fftools.trade;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import magellan.library.Building;
import magellan.library.CoordinateID;
import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.StringID;
import magellan.library.Unit;
import magellan.library.rules.CastleType;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;
import magellan.library.utils.Islands;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptMain;


/**
 * class offering common functiosn for trade
 * @author Fiete
 * function should be used in a static way
 */
public class TradeUtils {
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	public static final String[] HANDELSWAREN={"Balsam","Gewürz","Juwel","Myrrhe","Öl","Seide","Weihrauch"};
	
	/**
	 * 
	 * @param amount Wieviel soll gekauft werden
	 * @param basisPrice Wie hoch ist der Basispreis
	 * @param volumen Bauern/100...für wieviel Güter gilt der Basispreis
	 * @return insgesamt zu zahlender Preis
	 * 
	 * entstanden unter sehr aktiver Mithilfe von TheWhiteWolf, thx!
	 * 
	 */
	public static int getPrice(int amount,int basisPrice,int volumen){
		if (volumen == 0 || amount ==0){
			return 0;
		}
		int n = (int)Math.floor(amount/volumen);
		int restmenge=amount-n*volumen;
		int preis = basisPrice*(volumen*n*(n+1)/2 + restmenge*(n+1));
		return preis;
	}
	
	/**
	 * umgekehrter werg, sehr umständlich
	 * wieviel kann ich für eine bestimmte menge silber kaufen?
	 * @param silber Was steht zur verfügung
	 * @param basisPrice Basispreis des Gutes
	 * @param volumen Max Handelsvolumen in der Region
	 * @return maximale Menge des Gutes, welches mit <silber> zu erwerben ist
	 */
	public static int getMenge(int silber,int basisPrice,int volumen){
		if (silber <= 0){return 0;}
		int erg = 0;
		
		for (erg=0;erg<Integer.MAX_VALUE;erg++){
			int preis = TradeUtils.getPrice(erg, basisPrice, volumen);
			if (preis>silber){
				break;
			}
		}
		if (erg>0){erg-=1;}
		return erg;
	}
	
	/**
	 * liefert einen Überblick über Handelssituation auf einer Insel
	 * @param r
	 * @param data
	 * @return
	 */
	
	public static LinkedList<String> getIslandInfo(Region r,ScriptMain scriptMain){
		LinkedList<String> erg = new LinkedList<String>();
		GameData data = scriptMain.gd_ScriptMain;
		Map<CoordinateID,Region> regions = Islands.getIsland(r);
		if (regions==null){
			erg.add("Island Info not available");
			return erg;
		}
		
		
		// insel checken wg burgen usw...
		// irgendjemand mit passwort muss handeln können
		// muss ne burg mind 2 haben
		// Map newRegions = CollectionFactory.createHashtable();
		Hashtable<ID, Region> newRegions = new Hashtable<ID, Region>(0);
		
		
		for (Iterator<Region> iter = regions.values().iterator();iter.hasNext();){
			Region actR = (Region)iter.next();
			if (isCommonTradeRegion(actR, data)){
				newRegions.put(actR.getID(),actR);
			}
		}
		
		
		// neuen TradeAreaHandler machen
		TradeArea myTA = null;
		TradeAreaHandler TAH = new TradeAreaHandler(scriptMain);
		for (Iterator<Region> iter = newRegions.values().iterator();iter.hasNext();){
			Region actR = (Region)iter.next();
			TradeRegion actTR = TAH.getTradeRegion(actR);
			TradeArea actTA = TAH.getTradeArea(actTR, true);
			if (myTA==null){myTA = actTA;}
			if (!actTA.equals(myTA)){
				// nanu? neues Tradearea?
				erg.add("Island Info error: More than 1 TradeAreas");
				return erg;
			}
		}
		if (myTA==null){
			erg.add("Island Info error: no TradeArea");
			return erg;
		}
		
		// Die gewünschte Auskunftsregion als TradeRegion
		TradeRegion myTR = TAH.getTradeRegion(r);
		
		erg =  myTA.getTradeAreaUnitInfo(myTR);
		erg.addLast("***IslandInfo****");
		return erg;
	}
	
	/**
	 * Überürft, ob eine Region als handelbare Region angesehen werden kann
	 * es wird geprüft auf richtige burggrösse und eine priviligierte Einheit,
	 * die mind Talentlevel 1 in Handeln hat.
	 * @param r
	 * @param data
	 * @return
	 */
	public static boolean isCommonTradeRegion(Region r,GameData data){
		boolean erg = false;
		if (r.buildings()==null) {return false;}
		
		for (Iterator<Building> iter = r.buildings().iterator();iter.hasNext();){
			Building b = (Building)iter.next();
			if(b.getType() instanceof CastleType) {
				if (b.getSize()>1){
					erg = true;
					break;
				}
			}
		}
		
		// wenn !erg keine Burg gross genug
		if (!erg){return false;}
		erg = false;
		
		SkillType handelSkillType = data.rules.getSkillType("Handeln");
		// jemand, der Handeln kann vor Ort?
		for (Iterator<Unit> iter = r.units().iterator();iter.hasNext();){
			Unit u = (Unit)iter.next();
			Skill handelsSkill = u.getModifiedSkill(handelSkillType);
			if (handelsSkill!=null){
				if (handelsSkill.getLevel()>0){
					erg = true;
					break;
				}
			}
		}
		return erg;
	}
	
	/**
	 * liefert true, wenn sich r1 und r2 auf der gleichen Insel befinden
	 * sonst false
	 * 
	 * @param r1 Erste Region
	 * @param r2 Zweite Region
	 * @param data GamData object
	 * @return
	 */
	public static boolean onSameIsland(Region r1,Region r2,GameData data){
		Map<CoordinateID,Region> regions = Islands.getIsland(r1);
		if (regions==null){
			OutTextClass.getInstance().addOutLine("!!!Islands->regions liefert NULL!!!");
			return false;
		}
		if (regions.containsKey(r2.getID())){
			return true;
		}
		return false;
	}
	
	
	/**
	 * liefert ein Array mit den ItemTypes der Handelswaren
	 * 
	 * @return
	 */
	public static ArrayList<ItemType> handelItemTypes(){
		ArrayList<ItemType> erg = new ArrayList<ItemType>();
		for (int i = 0;i < HANDELSWAREN.length;i++){
			String s = HANDELSWAREN[i];
			ItemType actItemType = reportSettings.getRules().getItemType(StringID.create(s));
			if (actItemType!=null){
				erg.add(actItemType);
			}
		}
		return erg;
	}
	
	/**
	 * liefert true, wenn <code>itemType</code> ein Handelsgut ist
	 * @param itemType
	 * @return
	 */
	public static boolean isTradeItemType(ItemType itemType){
		if (itemType==null){
			return false;
		}
		if (handelItemTypes().contains(itemType)){
			return true;
		}
		return false;
	}
	
}
