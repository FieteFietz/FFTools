package com.fftools.utils;

import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.Building;
import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Item;
import magellan.library.Potion;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.StringID;
import magellan.library.Unit;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;

/**
 * Nützliches um GameData herum
 * @author Fiete
 *
 */
public class FFToolsGameData {
	public static final OutTextClass outText = OutTextClass.getInstance();
	
	private static ArrayList<String>longEresseaOrders = null; 
	
	/**
	 * liefert die Potion zu dem Namen aus data
	 * @param data
	 * @param Name
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Potion getPotion(GameData data,String Name){
		
		if (data.potions()==null || data.potions().size()==0){
			// keine Tränke bekannt
			return null;
		}
		for (Iterator<Potion> iter = data.potions().values().iterator();iter.hasNext();){
			Potion actPotion = (Potion)iter.next();
			if (actPotion.getName().equalsIgnoreCase(Name)){
				return actPotion;
			}
		}
		// nix gefunden
		return null;
	}
	
	public static boolean hasRdfFEffekt(ScriptUnit scriptUnit){
		
		// RdF ?
		ItemType itemType = scriptUnit.getScriptMain().gd_ScriptMain.rules.getItemType(StringID.create("Ring der flinken Finger"),false);
		
		if (itemType!=null){
			Item item = scriptUnit.getModifiedItem(itemType);
			if (item!=null && item.getAmount()>=scriptUnit.getUnit().getModifiedPersons()){
				return true;
			}
		}
		return false;
	}
	
	
	public static boolean hasSchaffenstrunkEffekt(ScriptUnit scriptUnit,boolean allowUse){
		Unit u = scriptUnit.getUnit();
		int actEffekte = 0;
		if (u.getEffects()!=null && u.getEffects().size()>0){
			for (Iterator<String> iter = u.getEffects().iterator();iter.hasNext();){
				String effect = (String)iter.next();
				String[] pairs = effect.split(" ");
				if (pairs.length>0 &&  pairs[1].equalsIgnoreCase("Schaffenstrunk")){
					// Untersuchen, ob anzahl der effekte>=Personenanzahl
					Integer I = Integer.parseInt(pairs[0]);
					if (I!=null){
						actEffekte=I.intValue();
					}
					if (I!=null && actEffekte>=u.getModifiedPersons()){
						return true;
					} else {
						if (I==null){
							scriptUnit.addComment("!!! Fehler beim Parsen der Effektanzahl!");
						} else {
							scriptUnit.addComment("Anzahl der Effekte bei Schaffenstrunk reicht nicht aus.");
						}
					}
					
				}
			}
		}
		// ok, nix active oder zu wenig
		if (allowUse){
			// checken, ob die unit was am Mann hat...
			ItemType itemType = scriptUnit.getScriptMain().gd_ScriptMain.rules.getItemType(StringID.create("Schaffenstrunk"));
			int anzahlSchaffenstrunk = 0;
			Item item = u.getItem(itemType);
			if (item!=null){
				anzahlSchaffenstrunk=item.getAmount();
			}
			if (((anzahlSchaffenstrunk * 10)+ actEffekte) >= u.getModifiedPersons()){
				// yep..wir haben Schaffenstrunk am Mann!
				if (scriptUnit.hasOrder("BENUTZEN SCHAFFENSTRUNK")){
					scriptUnit.addComment("Order zum Benutzen von Schaffenstrunk bereits gegeben");
				} else {
					scriptUnit.addOrder("BENUTZEN SCHAFFENSTRUNK", true);
				}
				return true;
			} else {
				scriptUnit.addComment("Schaffenstrunk: nicht genügend Tränke vorhanden.");
			}
		}
		return false;
	}
	
	/**
	 * liefert eine bestimmte Unit(!) nach nummer bzw namen
	 * @param r Region
	 * @param unitDesc  Nummer oder Name
	 * @return
	 */
	public static Unit getUnitInRegion(Region r,String unitDesc){
		//		 units durchlaufen
		for (Iterator<Unit> iter = r.units().iterator();iter.hasNext();){
			Unit actU = (Unit)iter.next();
			// und prüfen
			String test = actU.getName();
			if (test==null){
				test = actU.getModifiedName();
			}
			ID test2 = actU.getID();
			if (test != null && test2 != null){
				if (test.equalsIgnoreCase(unitDesc) || actU.toString(false).equalsIgnoreCase(unitDesc)){
					return actU;
				} 
			} 
		}
		return null;
	}
	
	public static boolean isUnitInRegion(Region r, String unitDesc){
		return getUnitInRegion(r, unitDesc)==null ? false : true ;
	}
	
	/**
	 * zu faul, es aus CastleTypes rauszulesen...
	 * @param size
	 * @return
	 */
	public static int getCastleSizeBuildSkillLevel(int size){
		int erg = 1;
		if (size>=10){erg=2;}
		if (size>=50){erg=3;}
		if (size>=250){erg=4;}
		if (size>=1250){erg=5;}
		if (size>=6250){erg=6;}
		return erg;
	}
	/**
	 * Liefert true, wenn der übergebene String mit einem ausgeschriebene 
	 * langen Eressea Befehl beginnt
	 * @param s
	 * @return
	 */
	public static boolean isLongEresseaOrder(String s){
		//		 mir fällz nix besseres ein...Manuell liste bauen
		if (longEresseaOrders==null) {
			longEresseaOrders = new ArrayList<String>();
			longEresseaOrders.add("ARBEITE");
			longEresseaOrders.add("ATTACKIERE");
			longEresseaOrders.add("BEKLAUE");
			longEresseaOrders.add("BELAGERE");
			longEresseaOrders.add("FAHRE");
			longEresseaOrders.add("FOLGE SCHIFF");
			longEresseaOrders.add("FOLGEN SCHIFF");
			longEresseaOrders.add("FORSCHE");
			longEresseaOrders.add("KAUFE");
			longEresseaOrders.add("LEHRE");
			longEresseaOrders.add("LERNE");
			longEresseaOrders.add("MACHE");
			longEresseaOrders.add("NACH");
			longEresseaOrders.add("PFLANZE");
			longEresseaOrders.add("PIRATERIE");
			longEresseaOrders.add("ROUTE");
			longEresseaOrders.add("SABOTIERE");
			longEresseaOrders.add("SPIONIERE");
			longEresseaOrders.add("TREIBE");
			longEresseaOrders.add("UNTERHALTE");
			longEresseaOrders.add("VERKAUFE");
			longEresseaOrders.add("ZAUBERE");
			longEresseaOrders.add("ZÜCHTE");
		}
		boolean erg = false;
		// s -> eventuell mit @ ? , dann entfernen
		if (s.startsWith("@")){
			s = s.substring(1);
		}
		// nun checken, ob unsere order damit anfängt
		for (Iterator<String> iter = longEresseaOrders.iterator();iter.hasNext();){
			String toCheck = (String)iter.next();
			if (s.toUpperCase().startsWith(toCheck)){
				// tuut sie..fein.
				erg = true;
				break;
			}
		}
		return erg;
	}
	
	/**
	 * Überprüft, ob eine Unit in einem Bestimmten Talent 
	 * einen bestimmten Wert unterschreitet
	 * @param u
	 * @param gd
	 * @param TalentName
	 * @param Level
	 * @return
	 */
	public static boolean talentUnderLevel(Unit u,GameData gd,String TalentName,int Level){
		boolean erg = false;
		SkillType skillType = gd.rules.getSkillType(TalentName, false);
		if (skillType!=null){
			Skill skill = u.getModifiedSkill(skillType);
			if (skill!=null){
				if (skill.getLevel()<Level){
					erg = true;
				}
			} else {
				// kennt das Talent noch nicht
				if (Level>0){
					erg=true;
				}
			}
		}
		return erg;
	}
	
	/**
	 * Befindet sich die Unit (modified) im gebäude eines Types
	 * @param u - die scriptunit
	 * @param typName Name des Gebäudetypes ("Akademie"
	 * @return  true or false
	 */
	public static boolean isInGebäude(ScriptUnit u,String typName){
		boolean erg=false;
		
		Building gebaeude = u.getUnit().getModifiedBuilding();
    	if (gebaeude != null){
    		if ((u.getScriptMain().gd_ScriptMain.rules.getBuildingType(typName).equals(gebaeude.getBuildingType()))){
    			erg=true;
    		}
      	}
		
		
		return erg;
	}
	
	/**
	 * Nimmt standardmässige Ersetzungen vor...für request, vorrat etc
	 * @param s
	 * @return
	 */
	public static String translateItemShortform(String itemName){
		
		// replacen
		if (itemName!=null && itemName.length()>0){
			itemName = itemName.replace("_"," ");
		}
		
		// Abkürzungen
		if (itemName.equalsIgnoreCase("Adws")){
			itemName = "Amulett des wahren Sehens";
		}
		if (itemName.equalsIgnoreCase("RdU")){
			itemName = "Ring der Unsichtbarkeit";
		}
		if (itemName.equalsIgnoreCase("GdtS")){
			itemName = "Gürtel der Trollstärke";
		}
		if (itemName.equalsIgnoreCase("RdfF")){
			itemName = "Ring der flinken Finger";
		}
		if (itemName.equalsIgnoreCase("SdU")){
			itemName = "Sphäre der Unsichtbarkeit";
		}
		
		if (itemName.equalsIgnoreCase("WdL")){
			itemName = "Wasser des Lebens";
		}
		
		if (itemName.equalsIgnoreCase("RdM")){
			itemName = "Ring der Macht";
		}
		
		return itemName;
	}
	
}
