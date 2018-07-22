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

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;


/**
 * MenuAction zum Rekrutieren einer bestimmten Anzahl von Personen
 *
 * @author Fiete
 * @version
 */
public class RekrutierenAnzahlAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	private int Anzahl = 1;
	
	/**
	 * Creates a new Rekrutieren Menu.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public RekrutierenAnzahlAction(SelectionObserver selectionObserver, int Anzahl) {
        super(selectionObserver.getClient());
        this.setName("Rekrutieren " + Anzahl);
        this.selectionObserver = selectionObserver;
        this.Anzahl = Anzahl;
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		ClientRekrutieren clientRekrutieren = new ClientRekrutieren(selectionObserver,Anzahl); 
		Thread t = new Thread(clientRekrutieren);
		t.start();
	}
}
