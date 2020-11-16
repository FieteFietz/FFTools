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
import java.util.ArrayList;
import java.util.StringTokenizer;

import magellan.client.Client;
import magellan.library.GameData;
import magellan.library.Region;
import magellan.library.event.GameDataEvent;
import magellan.library.utils.logging.Logger;

import com.fftools.ScriptMain;
import com.fftools.demo.actions.MenuAction;
import com.fftools.pools.seeschlangen.SeeschlangenJagdManager_SJM;
import com.fftools.trade.TradeAreaHandler;
import com.fftools.utils.FFToolsRegions;


/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version
 */
public class ShowMaplinesSSJM_Move_Action extends MenuAction {

	static  final long serialVersionUID = 0;
	private static Logger log = null;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public ShowMaplinesSSJM_Move_Action(Client client) {
        super(client);
        setName("Show SSJM MapLines");
        log = Logger.getInstance(ShowMaplinesSSJM_Move_Action.class);
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
		log.info("Show SSJM MapLines started...");
		FFToolsRegions.activateMapLine(super.client.getData(), SeeschlangenJagdManager_SJM.MAPLINE_TAG);
		log.info("Show SSJM MapLines finished...");
	}
}
