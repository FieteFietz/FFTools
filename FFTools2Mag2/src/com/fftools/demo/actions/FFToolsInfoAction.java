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

import com.fftools.VersionInfo;
import com.fftools.utils.MsgBox;

/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version $Revision : $
 */
public class FFToolsInfoAction extends MenuAction {
	
	public static final long serialVersionUID = 1L;
	
	private Client c=null;
	
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public FFToolsInfoAction(Client client) {
        super(client);
        super.setName("Info");
        this.c = client;
        VersionInfo.setFFTools2Path(client.getProperties());
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		// Version Anzeigen
		String info = "";
		info = "FFTools2. Version: " + VersionInfo.getVersionInfo();
		new MsgBox(c,info,"About FFTools2",false);
	}

	
}
