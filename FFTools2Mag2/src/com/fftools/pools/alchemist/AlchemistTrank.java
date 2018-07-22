package com.fftools.pools.alchemist;

import java.util.ArrayList;

import magellan.library.Potion;
import magellan.library.Region;
import magellan.library.rules.ItemType;

import com.fftools.OutTextClass;
import com.fftools.ScriptUnit;
import com.fftools.utils.FFToolsGameData;
import com.fftools.utils.FFToolsOptionParser;

/**
 * 
 * repräsentiert einen Trank in einer Trankorder
 * @author Fiete
 *
 */
public class AlchemistTrank {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	public static final int RANG_UNDEF = -1;
	
	/**
	 * der eigentliche Trank
	 */
	private Potion potion = null;
	
	/**
	 * Rangreihenfolge, abarbeitung aufsteigend (1 zuerst)
	 */
	private int rang = RANG_UNDEF;
	
	/**
	 * Kräutervorbehalt: wenn ja, werden für diesen Trank benötigte
	 * Gegenstände und Kräuter nicht für andere Tränke verwendet, wenn 
	 * der eigentliche Trank gerade nicht herstellbar ist
	 */
	private boolean vorbehalt = false;
	
	/**
	 * falls vorbehalt = ja: vorbehalt wird ignoriert, wenn mehr als vorbehaltMax
	 * Kräuter verfügbar sind
	 */
	private int vorbehaltMax = Integer.MAX_VALUE;
	
	
	/**
	 * Region, für die dieser trank definiert wurde oder null
	 * wenn reportweit
	 */
	private Region region = null;
	
	/**
	 * Referenz auf die ScriptUnit...für eventuelle Fehlerausgaben
	 */
	private ScriptUnit defScriptUnit = null;
	
	/**
	 * "Sicherheitskopie" der Original-Argumente
	 * (aus der setTrankOrder - Zeile)
	 */
	// private ArrayList<String> originalArguments=null;
	
	/**
	 * "Sicherheitskopie" des TrankNamens
	 */
	private String originalName = null;
	
	/**
	 * Maximaler Vorrat beim Depot der Region
	 * wenn genügend da, wird nicht mehr gebraut
	 */
	private int vorratMax = 0;
	
	/**
	 * Der ItemType des Tranks!
	 */
	private ItemType trankItemType = null;
	
	/**
	 * Maximale Produktion
	 * Sinnvoll bei Bauernblut
	 */
	private int absoluteMaxProduction  = 0;
	
	
	/**
	 * 
	 * @param u
	 * @param args
	 */
	public AlchemistTrank(ScriptUnit u, ArrayList<String> args){
		this.defScriptUnit = u;
		// this.originalArguments = args;
		
		// gleich parsen
		FFToolsOptionParser OP = new FFToolsOptionParser(u);
		OP.addOptionList(args);
		
		// Name
		this.originalName = OP.getOptionString("Name");
		
		// Check
		if (this.originalName.length()<2){
			outText.addOutLine("!!!AlchemistTrank nicht erkannt: " + u.unitDesc(),true);
			u.addComment("AlchemistTrank nicht erkannt");
			u.doNotConfirmOrders();
			return;
		}
		// Ersetzungen vornehmen
		this.originalName = this.originalName.replace("_"," ");
		// Trank organisieren
		this.potion = FFToolsGameData.getPotion(u.getScriptMain().gd_ScriptMain,this.originalName);
		if (this.potion==null){
			outText.addOutLine("!!!AlchemistTrank nicht erkannt: " + this.originalName + " bei " + u.unitDesc(),true);
			u.addComment("AlchemistTrank nicht erkannt: " + this.originalName);
			u.doNotConfirmOrders();
			return;
		}
		
		// ItemType
		this.trankItemType = u.getScriptMain().gd_ScriptMain.rules.getItemType(potion.getName(), false);
		if (this.trankItemType==null){
			outText.addOutLine("!!!AlchemistTrank nicht erkannt (ItemType): " + this.originalName + " bei " + u.unitDesc(),true);
			u.addComment("AlchemistTrank nicht erkannt (ItemType): " + this.originalName);
			u.doNotConfirmOrders();
			return;
		}
		
		
		// Rang
		this.rang = OP.getOptionInt("Rang", RANG_UNDEF);
		
		if (this.rang==RANG_UNDEF){
			outText.addOutLine("!!!AlchemistTrank Rang nicht erkannt: " + this.originalName + " bei " + u.unitDesc(),true);
			u.addComment("AlchemistTrank Rang nicht erkannt: " + this.originalName);
			u.doNotConfirmOrders();
			return;
		}
		
		// Region
		boolean onlyRegion = OP.getOptionBoolean("region", false);
		if (onlyRegion) {
			this.region = this.defScriptUnit.getUnit().getRegion();
		}
		
		// Vorbehalt
		this.vorbehalt = OP.getOptionBoolean("vorbehalt",false);
		
		// VorbehaltMax
		this.vorbehaltMax = OP.getOptionInt("vorbehaltMax",Integer.MAX_VALUE);
		
		// Vorrat Max
		this.vorratMax = OP.getOptionInt("vorratMax", Integer.MAX_VALUE);
		
		// Abolute Max Production
		this.absoluteMaxProduction = OP.getOptionInt("maxProd", 0);
		
	}


	/**
	 * @return the potion
	 */
	public Potion getPotion() {
		return potion;
	}


	/**
	 * @return the rang
	 */
	public int getRang() {
		return rang;
	}


	/**
	 * @return the region
	 */
	public Region getRegion() {
		return region;
	}


	/**
	 * @return the vorbehalt
	 */
	public boolean isVorbehalt() {
		return vorbehalt;
	}


	/**
	 * @return the vorbehaltMax
	 */
	public int getVorbehaltMax() {
		return vorbehaltMax;
	}


	/**
	 * @return the vorratMax
	 */
	public int getVorratMax() {
		return vorratMax;
	}


	/**
	 * @return the trankItemType
	 */
	public ItemType getTrankItemType() {
		return trankItemType;
	}


	public int getAbsoluteMaxProduction() {
		return absoluteMaxProduction;
	}

}
