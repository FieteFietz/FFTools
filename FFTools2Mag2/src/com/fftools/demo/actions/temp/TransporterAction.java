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

package com.fftools.demo.actions.temp;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import magellan.client.event.TempUnitEvent;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.UnitID;

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;


/**
 * MenuAction zum Rekrutieren einer bestimmten Anzahl von Personen
 *
 * @author Fiete
 * @version
 */
public class TransporterAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public TransporterAction(SelectionObserver selectionObserver) {
        super(selectionObserver.getClient());
        setName("Temp: Transporter");
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
				this.makeIt(actUnit);
			}
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Keine Aktion möglich:" + this.getClass().getName(),"Fehler",true);
		}
	}

	/**
	 * führt den Klonvorgang für diese Unit durch
	 * @param u
	 */
	private void makeIt(Unit u){
		Unit parentUnit = u;
		if(u instanceof TempUnit) {
			parentUnit = ((TempUnit) u).getParent();
		}
		// neue Unit ID
		UnitID id = UnitID.createTempID(this.selectionObserver.getClient().getData(), this.selectionObserver.getClient().getProperties(), parentUnit);
		// Die tempUnit anlegen
		TempUnit tempUnit = parentUnit.createTemp(this.selectionObserver.getClient().getData(),id);
		
		// name setzen
		tempUnit.addOrder("BENENNEN EINHEIT Transport ;dnt");
		// script Setzen
		tempUnit.addOrder("// script Transport minReitTalent=2 mode=auto");
		
		// rekrutieren
		tempUnit.addOrder("// script Runde " + this.selectionObserver.getClient().getData().getDate().getDate() + " script Rekrutieren 3");

		this.selectionObserver.getClient().getDispatcher().fire(new TempUnitEvent(this, tempUnit, TempUnitEvent.CREATED));
	}
	
	
}
