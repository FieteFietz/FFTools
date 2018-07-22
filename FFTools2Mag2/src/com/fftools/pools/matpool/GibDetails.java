package com.fftools.pools.matpool;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import magellan.library.Item;
import magellan.library.Order;
import magellan.library.StringID;
import magellan.library.Unit;
import magellan.library.rules.ItemType;

import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;

/**
 * kapselt die inhalte einer GIB order
 * 
 * 
 * @author Fiete
 *
 */
public class GibDetails {

	/**
	 * die unit, die die orders enthält
	 */
	private ScriptUnit u = null;
	
	/**
	 * nummer der zielunit
	 */
	private String targetUnitID = null;
	
	/**
	 * wenn möglich, Zielscriptunit
	 */
	private ScriptUnit targetScriptUnit = null;
	
	/**
	 * betrag der GIB Order 
	 */
	private int amount = -1;
	
	/**
	 * original name des gegenstandes
	 */
	private String originalItemTypeName = null;
	
	/**
	 * String zur Anzahl: Betrag oder ALLES
	 */
	private String originalAmountString = null;
	
	/**
	 * wenn <code>originalItemTypeName</code> gültig, der Item
	 * Type der Order
	 */
	private ItemType itemType = null;
	
	/**
	 * der kommentar  (;DEPOT markiert eine automatisch generierte DEPOT-Oredr)
	 */
	private String kommentar = null;
	
	/**
	 * just to make it easy
	 */
	private boolean isDepotGib = false;
	
	private String originalOrder = null;
	
	/**
	 * Konstruktor
	 * @param _u  Scriptunit, die die GIB Befehle hat
	 * @param order eine Orderzeile
	 */
	public GibDetails(ScriptUnit _u, String order){
		this.u = _u;
		this.originalOrder = order;
		if (order!=null && order.length()>4 && order.substring(0, 3).equalsIgnoreCase("GIB")){
			// OK..eine GIB order
			Pattern p = Pattern.compile("(?:\\s+|^)(?:([\"'])(.*?)\\1|(\\S+))");
		    Matcher m = p.matcher(order);
		    int i = 0;
		    while (m.find()){
		    	String s = m.group().trim();
		    	i++;
		    	int posI = s.indexOf("\"");
		    	if (posI>-1){
		    		s = s.replace("\"","");
		    	}
		    	switch (i) {
		    	case 1:
		    		// GIB
		    		break;
		    	case 2:
		    		// zielunit
		    		if (s.equalsIgnoreCase("temp")){
		    			m.find();
		    			s = m.group().trim();
		    			s = "TEMP " + s;
		    		}
		    		this.targetUnitID = s;
		    		break;
		    	case 3:
		    		// Anzahl oder alles
		    		this.originalAmountString = s;
		    		break;
		    	case 4:
		    		// Item
		    		this.originalItemTypeName = s;
		    		break;
		    	case 5:
		    		// Kommentar
		    		this.kommentar = s;
		    		break;
		    	}    		
		    	
		    }
		    
		    // alles was gelesen werden konnte, gesetzt
		    if (this.targetUnitID!=null){
		    	this.findTargetScriptUnit();
		    }
		    
		    // ItemType feststellen
		    if (this.originalItemTypeName!=null){
		    	StringID stringID = StringID.create(this.originalItemTypeName);
		    	this.itemType = this.u.getScriptMain().gd_ScriptMain.rules.getItemType(stringID);
		    }
		    
		    if (this.originalAmountString!=null && this.originalAmountString.equalsIgnoreCase("alles") && this.itemType!=null){
		    	// ne alles order
		    	// amount setzen aus unmodified
		    	Item item = this.u.getUnit().getItem(this.itemType);
		    	if (item!=null){
		    		this.amount = item.getAmount();
		    	}
		    	
		    }
		    if ((this.originalAmountString!=null && !this.originalAmountString.equalsIgnoreCase("alles"))){
		    	// versuch der umwandlung
		    	Integer myInt = null;
		    	try {
		    		myInt = Integer.parseInt(this.originalAmountString);
		    	} catch (NumberFormatException e){
		    		// oops
		    	}
		    	if (myInt!=null){
		    		this.amount = myInt.intValue();
		    	}
		    }
		    
		    if (this.kommentar!=null && this.kommentar.startsWith(MatPool.depotIdComment)){
		    	this.isDepotGib = true;
		    }
		    
		}
	}
	
	/**
	 * versucht die Zieleinheit zu finden
	 *
	 */
	private void findTargetScriptUnit(){
		ScriptMain sm = this.u.getScriptMain();
		this.targetScriptUnit = sm.getScriptUnit(this.targetUnitID);
	}

	/**
	 * @return the isDepotGib
	 */
	public boolean isDepotGib() {
		return isDepotGib;
	}

	/**
	 * @param isDepotGib the isDepotGib to set
	 */
	public void setDepotGib(boolean isDepotGib) {
		this.isDepotGib = isDepotGib;
	}

	/**
	 * @return the amount
	 */
	public int getAmount() {
		return amount;
	}

	/**
	 * @return the itemType
	 */
	public ItemType getItemType() {
		return itemType;
	}

	/**
	 * @return the kommentar
	 */
	public String getKommentar() {
		return kommentar;
	}

	/**
	 * @return the targetScriptUnit
	 */
	public ScriptUnit getTargetScriptUnit() {
		return targetScriptUnit;
	}

	/**
	 * @return the targetUnitID
	 */
	public String getTargetUnitID() {
		return targetUnitID;
	}
	
	/**
	 * splittet diese Order auf...zweigt transferChange ab und GIBt es newReceiver
	 * @param newReceiver
	 * @param transferChange
	 */
	
	public int SplitOrder(ScriptUnit newReceiver, int transferChange, String newComment){
		return this.SplitOrder(newReceiver.getUnit(), transferChange, newComment);
	}
	
	/**
	 * splittet diese Order auf...zweigt transferChange ab und GIBt es newReceiver
	 * @param newReceiver
	 * @param transferChange
	 */
	
	public int SplitOrder(Unit newReceiver, int transferChange, String newComment){
		/**
		 * =0 alles schön
		 * <0 zu viel Transfer
		 * =1 keine vollständige GIB order zum splitten
		 * =2 keine order gesplittet
		 */
		int error_code = 0;
		if (this.amount<0 || this.itemType==null || this.targetScriptUnit==null){
			return 1;
		}
		
		if (transferChange>this.amount){
			return (this.amount - transferChange);
		}
		
		
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		boolean changed = false;
		for (Order o : this.u.getUnit().getOrders2()){
			String s = o.getText();
			if (s.equalsIgnoreCase(this.originalOrder)){
				changed=true;
				// original order
				String newOrder = "GIB " + this.targetUnitID + " " + (this.amount-transferChange) + " ";
				String itemName = this.itemType.getName();
				if (itemName.indexOf(" ")>-1){
					itemName = "\"" + itemName + "\"";
				}
				newOrder = newOrder + itemName + " ";
				// Kommentar aus orgi rekonstruieren
				String oldKomment = this.originalOrder.substring(this.originalOrder.indexOf(";"));
				newOrder = newOrder + oldKomment;
				if (this.amount-transferChange>0){
					newOrders.add(this.u.getUnit().createOrder(newOrder));
				}
				
				// neue Order
				newOrder = "GIB " + newReceiver.toString(false) + " " + transferChange + " ";
				newOrder += itemName + " ;";
				newOrder += newComment;
				
				if (!newReceiver.equals(this.u.getUnit())){
					newOrders.add(this.u.getUnit().createOrder(newOrder));
				}				
			} else {
				newOrders.add(this.u.getUnit().createOrder(s));
			}
		}
		if (changed){
			this.u.getUnit().setOrders2(newOrders);
		} else {
			return 2;
		}
		return error_code;
	}
	
}
