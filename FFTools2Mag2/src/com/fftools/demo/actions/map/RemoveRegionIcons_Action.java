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
import java.util.Collection;
import java.util.StringTokenizer;

import com.fftools.demo.actions.MenuAction;

import magellan.client.Client;
import magellan.client.swing.map.MarkingsImageCellRenderer;
import magellan.library.GameData;
import magellan.library.Order;
import magellan.library.Orders;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.utils.logging.Logger;


/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version
 */
public class RemoveRegionIcons_Action extends MenuAction {

	static  final long serialVersionUID = 0;
	private static Logger log = null;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public RemoveRegionIcons_Action(Client client) {
        super(client);
        setName("Remove All RegionIcons");
        log = Logger.getInstance(RemoveRegionIcons_Action.class);
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		/*
		 * Alle Regionen durchgehen, aus den maplines diejenigen mit der richtigen TAG_ID herausnehmen
		 */
		log.info("Remove All RegionIcons started...");
		GameData gd = super.client.getData();
		for (Region r:gd.getRegions()) {
			removeRegionIcon(r);
		}
		
		// refreshen
		
		log.info("Remove All RegionIcons finished...");
	}
	
	private void removeRegionIcon(Region r){
		if(r.containsTag(MarkingsImageCellRenderer.ICON_TAG)) {
			r.removeTag(MarkingsImageCellRenderer.ICON_TAG);
		}
	}
	
}
