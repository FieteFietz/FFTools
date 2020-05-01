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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;

import magellan.client.event.OrderConfirmEvent;
import magellan.client.event.UnitOrdersEvent;
import magellan.library.CoordinateID;
import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaConstants;
import magellan.library.rules.RegionType;
import magellan.library.utils.Islands;
import magellan.library.utils.Regions;
import magellan.library.utils.Resources;


/**
 * MenuAction zum Rekrutieren einer bestimmten Anzahl von Personen
 * @author Fiete
 * @version
 */
public class RideAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	private Region targetRegion = null;
	private String command = "";
	
	private GameData data=null;
	
	public Region getTargetRegion() {
		return targetRegion;
	}

	public void setTargetRegion(Region targetRegion) {
		this.targetRegion = targetRegion;
		command = "// script Goto " + targetRegion.getCoordX()+","+targetRegion.getCoordY()+" Pferde=ja ;" + targetRegion.getName(); 
		setName(command);
		this.data = this.selectionObserver.getClient().getData();
	}

	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public RideAction(SelectionObserver selectionObserver) {
        super(selectionObserver.getClient());
        setName("// script Ride");
        this.selectionObserver = selectionObserver; 
        this.data = this.selectionObserver.getClient().getData();
        
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		ArrayList<Object> units = this.selectionObserver.getObjectsOfClass(Unit.class);
		if (units!=null && units.size()>0){
			for (Iterator<Object> iter = units.iterator();iter.hasNext();){
				Unit actUnit = (Unit)iter.next();
				this.Goto(actUnit);
			}
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Kein Ride möglich.","Fehler",true);
		}
	}

	/**
	 * führt den set Goto für diese Unit durch
	 * @param u
	 */
	private void Goto(Unit u){
		if (this.targetRegion==null){
			new MsgBox(this.selectionObserver.getClient(),"Kein Ride möglich. (keine Region)","Fehler",true);
			return;
		}
		
		Region actRegion = null;
	   String path = "";
	   List<Region>regionList=null;
	   boolean PathNotFound = false;
	   Map<ID,RegionType> excludeMap = Regions.getOceanRegionTypes(data.getRules());
	   RegionType Feuerwand = Regions.getFeuerwandRegionType(data);
	   excludeMap.put(Feuerwand.getID(), Feuerwand);
	   String order = "nix";
	   if (onSameIsland(u.getRegion(), this.targetRegion)){
  	     if (actRegion==null || !u.getRegion().equals(actRegion)){
  	       // String path = Regions.getDirections(u.getScriptMain().gd_ScriptMain.regions(), act, dest, excludeMap);
  	       actRegion = u.getRegion();
  	       regionList = Regions.getLandPath(data, actRegion.getCoordinate(), this.targetRegion.getCoordinate(), excludeMap,1,1);
  	       path=Regions.getDirections(regionList);
  	     }
  	   
  	     if (path!=null && path.length()>0){
  	       // Pfad gefunden
  	    	order = Resources.getOrderTranslation(EresseaConstants.O_MOVE) + " " + path;
  	     } else {
  	    	 PathNotFound=true;
  	     }
	   } else {
		   order = "; !! not on same island!";
		   PathNotFound=true;
	   }
	   
	    // 20200211: alte // script GoTo Oders entfernen
 	    List<Order> orders =  u.getOrders2();
 	    List<Order> newOrders = new LinkedList<Order>();
 	    if (orders!=null && orders.size() >0) {
	  	    for (Order o:orders) {
	  	    	boolean isOK=true;
	  	    	if (o.getText().toUpperCase().startsWith("// SCRIPT GOTO")) {
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
		if (!PathNotFound){
			u.setOrdersConfirmed(true);
		}
		List<Unit> units = new LinkedList<Unit>();
		units.add(u);
		this.selectionObserver.getClient().getDispatcher().fire(new OrderConfirmEvent(this, units));
		this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this,u));
	}
	
	private boolean onSameIsland(Region r1,Region r2){
		  Collection<Region> island = new LinkedList<Region>();
	    Map<CoordinateID,Region> m = Islands.getIsland(r1);
	    if(m != null) {
	      island.addAll(m.values());
	      island.remove(r1);
	      if (island.contains(r2)){
	        return true;
	      }
	    }
	    return false;
		}
}
