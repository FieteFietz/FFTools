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
import com.fftools.utils.FFToolsUnits;
import com.fftools.utils.MsgBox;


/**
 * MenuAction zum Rekrutieren einer bestimmten Anzahl von Personen
 *
 * @author Fiete
 * @version
 */
public class RohstoffAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public RohstoffAction(SelectionObserver selectionObserver) {
        super(selectionObserver.getClient());
        setName("Rohstoffe");
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
			for (Iterator<Object> iter = units.iterator();iter.hasNext();){
				Unit actUnit = (Unit)iter.next();
				this.doAction(actUnit);
			}
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Nicht m�glich:" + this.getClass().getName(),"Fehler",true);
		}
	}

	/**
	 * f�hrt die Aktion f�r diese Unit durch
	 * @param u
	 */
	private void doAction(Unit u){
		String name = FFToolsUnits.getBestSkillTypeName(u);
		String order = null;
		if (name==null || name.length()==0){
			return;
		}
		
		int laufzeit=5;
		
		int zielrunde = (this.selectionObserver.getClient().getData().getDate().getDate() + laufzeit);
		
		if (name.equalsIgnoreCase("Steinbau")){
			order = "// script ACK Laufzeit=" + zielrunde + " machen stein";
		}
		if (name.equalsIgnoreCase("bergbau")){
			order = "// script ACK Laufzeit=" + zielrunde + " machen eisen";
		}
		if (name.equalsIgnoreCase("Holzf�llen")){
			if (u.getRegion().isMallorn()){
				order = "// script ACK Laufzeit=" + zielrunde + " machen mallorn";
			} else {
				order = "// script ACK Laufzeit=" + zielrunde + " machen holz";	
			}
		}
		
		u.addOrder( order);
		u.setOrdersConfirmed(true);
		List<Unit> units = new LinkedList<Unit>();
		units.add(u);
		this.selectionObserver.getClient().getDispatcher().fire(new OrderConfirmEvent(this, units));
		this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this,u));
	}
}
