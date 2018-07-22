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


/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version
 */
public class Orders2TagsAction extends MenuAction {

	static  final long serialVersionUID = 0;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public Orders2TagsAction(Client client) {
        super(client);
        setName("Orders->Tags");
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		// alle units durchlaufen und schauen
		// ob in den orders <// setTag> komments stehen
		// format: // setTag TagName TagValue
		// wenn dem so ist, Tag setzen
		GameData gd = super.client.getData();
		
		FFToolsTags.AllOrders2Tags(gd);
		
		// refreshen
		this.client.getMagellanContext().getEventDispatcher().fire(new GameDataEvent(this, this.client.getData()));	
	}
}
