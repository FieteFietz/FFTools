package com.fftools.transport;

import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.Region;
import magellan.library.rules.ItemType;

import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.PriorityUser;

/**
 * 
 * wer braucht wo was wieviel mit welcher Prio
 * 
 * @author Fiete
 *
 */
public class TransportRequest extends PriorityUser implements Comparable<TransportRequest> {
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	/**
	 * Region, wo was hinsoll
	 */
	private Region region = null;
	
	/**
	 * die Items, die hier gefordert werdeb
	 */
	private ArrayList<ItemType> itemTypes = null;
	
	/**
	 * zum Nachverfolgen
	 */
	private String originalGegenstand = null;
	
	/**
	 * die Menge, die angefordert wird
	 */
	private int gefordert = 0;
	
	
	/**
	 * wieviel ist von diesem request bereits abgearbeitet?
	 * wenn fertig, dann ist anzahl_bearbeitet == item.amount
	 */
	private int bearbeitet = 0;
	/**
	 * just for info zur Ausgabe in Infolisten und eventuell beim Transporter oder so 
	 */
	private String kommentar = null;
	
	/**
	 * die requestende unit (wer soll es im besten fall erhalten...)
	 */
	private ScriptUnit scriptUnit = null;
	
	/**
	 * schränkt die zu nutzenden Transporter ein
	 * Strings können beliebige Strinhs, ItemTypes oder ItemGroups sein.
	 */
	private ArrayList<String> transporterSpecs = null;
	
	/**
	 * Bestimmt im seltenen Fall den SortierModus des TransportManagers
	 */
	private int TM_sortMode = MatPoolRequest.TM_sortMode_dist;
	
	
	/**
	 * vollständiger Konstruktor
	 * @param _region
	 * @param _item
	 * @param _prio
	 * @param _kommentar
	 */
	public TransportRequest(ScriptUnit scriptUnit,int menge,String _gegenstand,int _prio,String _kommentar){
		region=scriptUnit.getUnit().getRegion();
		this.scriptUnit = scriptUnit;
		originalGegenstand = _gegenstand;
		super.setPrio(_prio);
		kommentar = _kommentar;
		gefordert = menge;
		
		//	gegenstand ist entweder der Name eines ItemTypes oder der einer (script)Kategorie
		// ist es ein ItemType
		ItemType itemType = reportSettings.getRules().getItemType(_gegenstand); 
		if (itemType!=null){
			// es ist ein ItemType
			this.itemTypes = new ArrayList<ItemType>();
			this.itemTypes.add(itemType);
		} else {
			// ist es eine Kategorie?
			ArrayList<ItemType> _itemTypes = reportSettings.getItemTypes(_gegenstand);
			if (_itemTypes!=null){
				// reportSettings liefert bereits sortierte Liste
				this.itemTypes = new ArrayList<ItemType>();
				this.itemTypes.addAll(_itemTypes);
			}
		}
	}
	
	/**
	 * Konstruktor ohne Konmmentar -> null
	 * @param _region
	 * @param _item
	 * @param _prio
	 */
	public TransportRequest(ScriptUnit scriptUnit,int menge, String _gegenstand,int _prio){
		this(scriptUnit,menge,_gegenstand,_prio,null);
	}

	/**
	 * @return the anzahl_bearbeitet
	 */
	public int getBearbeitet() {
		return bearbeitet;
	}


	/**
	 * @return the kommentar
	 */
	public String getKommentar() {
		return kommentar;
	}

	/**
	 * @param kommentar the kommentar to set
	 */
	public void setKommentar(String kommentar) {
		this.kommentar = kommentar;
	}


	

	/**
	 * @return the r
	 */
	public Region getRegion() {
		return region;
	}
	
	/**
	 * liefert wahr, wenn das itemType Teil dieses Requests ist
	 * @param itemType
	 */
	public boolean containsItemType(ItemType itemType){
		if (this.itemTypes==null){return false;}
		return this.itemTypes.contains(itemType);
	}
	
	/**
	 * was steht derzeit noch aus?
	 */
	public int getForderung(){
		return Math.max(0,this.gefordert-this.bearbeitet);
	}
	
	/**
	 * erhöht die anzahl bearbeiteter gegenstände um betrag
	 * @param betrag
	 */
	public void incBearbeitet(int betrag){
		this.bearbeitet+=betrag;
	}
	
	public int compareTo(TransportRequest o){
		int i =0;
		// i = Math.max(0,(o.getPrio() - this.getPrio()));
		// THAT WAS BUGGY!!!!  FF 2011-11-11 ...very late to find out..
		i = o.getPrio() - this.getPrio();
		return i;
	}

	/**
	 * @return the gefordert
	 */
	public int getOriginalGefordert() {
		return gefordert;
	}

	/**
	 * @return the originalGegenstand
	 */
	public String getOriginalGegenstand() {
		return originalGegenstand;
	}

	/**
	 * @return the itemTypes
	 */
	public ArrayList<ItemType> getItemTypes() {
		return itemTypes;
	}

	/**
	 * @return the scriptUnit
	 */
	public ScriptUnit getScriptUnit() {
		return scriptUnit;
	}

	
	/**
	 * fügt eine Spezialisierungsrestriktion zu dem TransportRequest hinzu.
	 * @param spec
	 */
	public void addSpec(String spec){
		if (spec==null || spec.length()==0){
			return;
		}
		if (this.transporterSpecs==null){
			this.transporterSpecs = new ArrayList<String>();
		}
		this.transporterSpecs.add(spec);
	}

	/**
	 * fügt eine Liste von Spezialisierungsrestriktionen zu dem TransportRequest hinzu.
	 * @param spec
	 */
	public void addSpec(ArrayList<String> spec){
		if (spec==null || spec.size()==0){
			return;
		}
		for (Iterator<String> iter = spec.iterator();iter.hasNext();){
			String s = (String)iter.next();
			this.addSpec(s);
		}
	}
	
	/**
	 * @return the transporterSpecs
	 */
	public ArrayList<String> getTransporterSpecs() {
		return transporterSpecs;
	}

	/**
	 * @return the tM_sortMode
	 */
	public int getTM_sortMode() {
		return TM_sortMode;
	}

	/**
	 * @param mode the tM_sortMode to set
	 */
	public void setTM_sortMode(int mode) {
		TM_sortMode = mode;
	}
	
	public String toString(){
		String info = "";
		info = this.gefordert + " " + this.originalGegenstand + " Prio " + this.getPrio() + " nach " + this.region.toString() +  " (" + this.getKommentar() + ")";
		return info;
	}
	
}
