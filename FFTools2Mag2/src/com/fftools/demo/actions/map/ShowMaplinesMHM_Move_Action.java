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

package com.fftools.demo.actions.map;

import java.awt.event.ActionEvent;

import com.fftools.demo.actions.MenuAction;
import com.fftools.pools.matpool.MaterialHubManager_MHM;
import com.fftools.utils.FFToolsRegions;

import magellan.client.Client;
import magellan.library.utils.logging.Logger;


/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version
 */
public class ShowMaplinesMHM_Move_Action extends MenuAction {

	static  final long serialVersionUID = 0;
	private static Logger log = null;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public ShowMaplinesMHM_Move_Action(Client client) {
        super(client);
        setName("Show MHM MapLines");
        log = Logger.getInstance(ShowMaplinesMHM_Move_Action.class);
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		/*
		 * Alle Regionen durchgehen, aus den prepared maplines diejenigen mit der richtigen TAG_ID in die maplines einfügen
		 */
		log.info("Show MHM MapLines started...");
		FFToolsRegions.activateMapLine(super.client.getData(), MaterialHubManager_MHM.MAPLINE_TAG);
		log.info("Show MHM MapLines finished...");
	}
}
