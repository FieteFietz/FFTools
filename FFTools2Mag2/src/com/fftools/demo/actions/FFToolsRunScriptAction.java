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

import java.awt.event.ActionEvent;

import magellan.client.Client;

import com.fftools.swing.FFToolsRunScript;

/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version
 */
public class FFToolsRunScriptAction extends MenuAction {
	
	static final long serialVersionUID =0;
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public FFToolsRunScriptAction(Client client) {
        super(client);
        super.setName("FFTools2 Run!");
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {

		
		FFToolsRunScript f = new FFToolsRunScript(client,false,client.getData());
		f.setVisible(true);
		Thread t = new Thread(f);
		t.start();
				
	}

}
