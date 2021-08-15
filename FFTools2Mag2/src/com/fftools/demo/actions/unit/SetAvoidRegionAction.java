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

package com.fftools.demo.actions.unit;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import magellan.client.event.OrderConfirmEvent;
import magellan.client.event.UnitOrdersEvent;
import magellan.library.Unit;

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;


/**
 * MenuAction zum setzen von setOK Runde + confirm
 *
 * @author Fiete
 * @version
 */
public class SetAvoidRegionAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public SetAvoidRegionAction(SelectionObserver selectionObserver) {
        super(selectionObserver.getClient());
        setName("AvoidRegion (auto)");
        this.selectionObserver = selectionObserver; 
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
				this.doAction(actUnit);
				units2.add(actUnit);
				this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this,actUnit));
			}
			
			this.selectionObserver.getClient().getDispatcher().fire(new OrderConfirmEvent(this, units2));
			
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Nicht möglich:" + this.getClass().getName(),"Fehler",true);
		}
	}

	/**
	 * führt die Aktion für diese Unit durch
	 * @param u
	 */
	private void doAction(Unit u){
		String order = "// script AvoidRegion mode=auto";
		u.addOrder( order);
		// u.setOrdersConfirmed(true);
	}
}
