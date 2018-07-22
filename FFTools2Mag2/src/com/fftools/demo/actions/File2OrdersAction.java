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
import magellan.library.GameData;
import magellan.library.event.GameDataEvent;

import com.fftools.utils.FFToolsTags;
import com.fftools.utils.MsgBox;


/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version
 */
public class File2OrdersAction extends MenuAction {

	private static final long serialVersionUID = 0;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public File2OrdersAction(Client client) {
        super(client);
        setName("File->Orders");
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		// aus gefundenen Tags orders machen....
		// die ejcTaggable ein wenig besser lesbar machen
		// alle anderen aber unterstützen...
		
		GameData gd = super.client.getData();
		FFToolsTags.File2Orders(gd);

		// refreshen
		this.client.getMagellanContext().getEventDispatcher().fire(new GameDataEvent(this, this.client.getData()));
		
		new MsgBox(null ,"OK","OK",true);
		
	}
}
