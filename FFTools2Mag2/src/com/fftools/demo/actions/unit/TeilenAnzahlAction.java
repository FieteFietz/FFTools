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

import magellan.client.event.TempUnitEvent;
import magellan.client.event.UnitOrdersEvent;
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
public class TeilenAnzahlAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	private int Anzahl = 1;
	
	/**
	 * Creates a new Rekrutieren Menu.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public TeilenAnzahlAction(SelectionObserver selectionObserver, int Anzahl) {
        super(selectionObserver.getClient());
        this.setName("Teilen in " + Anzahl);
        this.selectionObserver = selectionObserver;
        this.Anzahl = Anzahl;
	}

	
	/**
	 * führt die Aktion für diese Unit durch
	 * @param u
	 */
	private void doAction(Unit u){
		
		int allPersons = u.getModifiedPersons();
		int personsPerTemp = (int)Math.round((double)allPersons/(double)this.Anzahl);
		// int personsPerTemp = (int)Math.floor((double)allPersons/(double)this.Anzahl);
		int remainingPersons=allPersons;
		
		ArrayList<TempUnit> tempUnits = new ArrayList<TempUnit>();
		
		for (int i = 1;i<=this.Anzahl;i++){
			if (remainingPersons>0){
				TempUnit newUnit = createTemp(u);
				// Übergabe setzen
				int transfer = Math.min(personsPerTemp,remainingPersons);
				remainingPersons-=transfer;
				if (remainingPersons<personsPerTemp){
					transfer+=remainingPersons;
					remainingPersons=0;
				}
				// Order setzen
				u.addOrder( "GIB " + newUnit.toString(false) + " " + transfer + " Personen ;dnt");
				tempUnits.add(newUnit);
			}
		}

		this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this,u));
		for (Unit t:tempUnits){
			this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this,t));
		}		
	}
	
	
	private TempUnit createTemp(Unit u){
		Unit parentUnit = u;
		if(u instanceof TempUnit) {
			parentUnit = ((TempUnit) u).getParent();
		}
		// neue Unit ID
		UnitID id = UnitID.createTempID(this.selectionObserver.getClient().getData(), this.selectionObserver.getClient().getProperties(), parentUnit);
		// Die tempUnit anlegen
		TempUnit tempUnit = parentUnit.createTemp(this.selectionObserver.getClient().getData(),id);
		
		// name setzen
		tempUnit.addOrder( "BENENNEN EINHEIT \"" + u.getModifiedName() + "\" ;dnt");
		

		this.selectionObserver.getClient().getDispatcher().fire(new TempUnitEvent(this, tempUnit, TempUnitEvent.CREATED));
		
		
		return tempUnit;
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
			new MsgBox(this.selectionObserver.getClient(),"Nicht möglich:" + this.getClass().getName(),"Fehler",true);
		}
	}
}
