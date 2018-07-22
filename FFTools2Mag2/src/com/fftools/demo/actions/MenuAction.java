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

package com.fftools.demo.actions;

import javax.swing.AbstractAction;
import javax.swing.Action;

import magellan.client.Client;

/**
 * A common super class for all menu actions. It offers all necessary information to build a menu
 * with it.
 */
public abstract class MenuAction extends AbstractAction {
	// private static final Logger log = Logger.getInstance(MenuAction.class);
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Client client;

	/**
	 * Creates a new MenuAction object reading its name, mnemonic and accelerator from the
	 * dictionary.
	 * 
	 * @param client The client for this MenuAction object.
	 */
	public MenuAction(Client client) {
        this.client = client;
		
	}

	

	/**
	 * This method is called whenever this action is invoked.
	 */
	public abstract void actionPerformed(java.awt.event.ActionEvent e);

	/**
	 * Sets the name of this menu action.
	 *
	 * @param name TODO: DOCUMENT ME!
	 */
	protected void setName(String name) {
		this.putValue(Action.NAME, name);
	}

	/**
	 * Returns the name of this menu action.
	 *
	 * @return TODO: DOCUMENT ME!
	 */
	protected String getName() {
		return (String) this.getValue(Action.NAME);
	}


	/**
	 * Returns a String representation of this MenuAction object.
	 *
	 * @return TODO: DOCUMENT ME!
	 */
	public String toString() {
		return this.getName();
	}

	
}
