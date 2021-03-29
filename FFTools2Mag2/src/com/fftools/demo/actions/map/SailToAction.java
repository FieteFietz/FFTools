/*
 *  Copyright (C) 2000-2004 Roger Butenuth, Andreas Gampe,
 *                          Stefan Goetz, Sebastian Pappert,
 *                          Klaas Prause, Enno Rehling,
 *                          Sebastian Tusk, Ulrich Kuester,
 *                          Ilja Pavkovic
 *
 * This file is part of the Eressea Java Code Base, see the
 * file LICENSING for the licensing information applying to
 * this file.
 *
 */

package com.fftools.demo.actions.map;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import magellan.client.event.OrderConfirmEvent;
import magellan.client.event.UnitOrdersEvent;
import magellan.library.GameData;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.Unit;
import magellan.library.UnitContainer;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.rules.ShipType;
import magellan.library.utils.Direction;
import magellan.library.utils.Regions;
import magellan.library.utils.Resources;
import magellan.library.utils.logging.Logger;

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.MsgBox;


/**
 * MenuAction zum Erzeugen eines SailTo 
 *
 * @author Fiete
 * @version
 */
public class SailToAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	private static Logger log = null;
	
	private Region targetRegion = null;
	private String command = "";
	
	private GameData data=null;
	
	private boolean isShipableRegion=false;
	
	public Region getTargetRegion() {
		return targetRegion;
	}

	public void setTargetRegion(Region targetRegion) {
		this.targetRegion = targetRegion;
		command = "// script SailTo " + targetRegion.getCoordX()+","+targetRegion.getCoordY()+" ;" + targetRegion.getName(); 
		setName(command);
		this.data = this.selectionObserver.getClient().getData();
		
		this.isShipableRegion=false;
        if (targetRegion.getRegionType().isOcean()){
           this.isShipableRegion=true;  
        } else {
          // run through the neighbors
          for (Region r:targetRegion.getNeighbors().values()) {
            if (r.getRegionType().isOcean()) {
              this.isShipableRegion=true;
              break;
            }
          }
        }
	}

	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public SailToAction(SelectionObserver selectionObserver) {
        super(selectionObserver.getClient());
        setName("// script SailTo (and back)");
        this.selectionObserver = selectionObserver; 
        this.data = this.selectionObserver.getClient().getData();
        log = Logger.getInstance(SailToAction.class);
        
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		ArrayList<Object> units = this.selectionObserver.getObjectsOfClass(Unit.class);
		if (units!=null && units.size()>0){
			List<Unit> units2 = new LinkedList<Unit>();
			for (Iterator<Object> iter = units.iterator();iter.hasNext();){
				Unit actUnit = (Unit)iter.next();
				this.Goto(actUnit);
				units2.add(actUnit);
			}
			this.selectionObserver.getClient().getDispatcher().fire(new OrderConfirmEvent(this, units2));
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Kein SailTo möglich.","Fehler",true);
		}
	}

	/**
	 * führt den set SailTo für diese Unit durch
	 * @param u
	 */
	private void Goto(Unit u){
		if (this.targetRegion==null){
			new MsgBox(this.selectionObserver.getClient(),"Kein SailTo möglich. (keine Region)","Fehler",true);
			return;
		}
		
		String path = "";
	   List<Region>regionList=null;
	   
	   boolean PathNotFound = false;
	   String order = "nix";
	   
  	     
    	if (isSeaConnPossible(u)){
  	       regionList = Regions.planShipRoute(u.getModifiedShip(),data, this.targetRegion.getCoordinate());
           path=Regions.getDirections(regionList);
       } else {
    	   order = "; !!! nicht möglich (Kein Kapitän, ungünstige Zielregion)";
    	   PathNotFound=true;
       }
  	    
  	   
  	     if (path!=null && path.length()>0){
  	       // Pfad gefunden
  	    	order = Resources.getOrderTranslation(EresseaConstants.O_MOVE) + " " + path;
  	     } else {
  	    	 order = "; !!! Kein Weg gefunden!";
  	    	 PathNotFound=true;
  	     }
	    
  	    // 20200211: alte // script SailTo Oders entfernen
  	    List<Order> orders =  u.getOrders2();
  	    List<Order> newOrders = new LinkedList<Order>();
  	    if (orders!=null && orders.size() >0) {
	  	    for (Order o:orders) {
	  	    	boolean isOK=true;
	  	    	if (o.getText().toUpperCase().startsWith("// SCRIPT SAILTO")) {
	  	    		isOK=false;
	  	    	}
	  	    	if (o.getText().toUpperCase().startsWith("// SCRIPT ROUTE")) {
	  	    		isOK=false;
	  	    	}
	  	    	if (o.getText().toUpperCase().startsWith("NACH")) {
	  	    		isOK=false;
	  	    	}
	  	    	if (isOK) {
	  	    		newOrders.add(o);
	  	    	}
	  	    }
	  	    u.setOrders2(newOrders);
  	    }
  	     
  	     
	    u.addOrder(order);   
		u.addOrder(command);
		int speed=6;
		Ship s = u.getModifiedShip();
		Direction d = Direction.INVALID;
		if (s!=null){
			speed = data.getGameSpecificStuff().getGameSpecificRules().getShipRange(s);
			d = Regions.getMapMetric(data).toDirection(s.getShoreId());
		}
		log.info("SailToAction: starting with speed " + speed);
		int runden = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(u.getRegion().getCoordinate(),d, this.targetRegion.getCoordinate(), data, speed, log);
		u.addOrder("; Anzahl Runden: " + runden);
		
		
		if (!PathNotFound){
			u.setOrdersConfirmed(true);
		}
		this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this,u));
	}
	
	
	/**
	   * Prüfen ob für diese Unit eine Seeverbindung zur destRegion
	   * prinzipiell möglich ist
	   */
	  private boolean isSeaConnPossible(Unit u){
	    // oder benötigt...bei gleicher Region->false!
	    // Region extrahieren
	    Region originRegion = u.getRegion();
	    // nicht in gleicher Region
	    if (originRegion.equals(this.targetRegion)){
	      return false;
	    }
	    // Unit muss Kapitän sein
	    boolean capt=false;
	    UnitContainer uc = u.getModifiedUnitContainer();
	    if (uc!=null){
	      if (uc instanceof Ship){
	        Ship s = (Ship)uc;
	        if (s.getOwner()!=null && s.getOwner().equals(u)){
	          capt = true;
	        }
	      }
	    }
	    if (!capt){
	      // Kapitän
	      return false;
	    }
	    
	    // Zielregion muss am Meer liegen oder Ozean sein
	    if (!this.isShipableRegion){
	      return false;
	    }
	    
	    // Alle Fehler (prinzipiell) ausgeschlossen (?)
	    
	    return true;
	  }
	
}
