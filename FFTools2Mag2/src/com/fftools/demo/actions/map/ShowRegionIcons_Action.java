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
public class ShowRegionIcons_Action extends MenuAction {

	static  final long serialVersionUID = 0;
	private static Logger log = null;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public ShowRegionIcons_Action(Client client) {
        super(client);
        setName("Show RegionIcons");
        log = Logger.getInstance(ShowRegionIcons_Action.class);
	}

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		/*
		 * Alle Regionen durchgehen, aus Befehlen die RegionIcons einfügen, wenn noch nicht vorhanden
		 */
		log.info("Show regionIcons started...");
		GameData gd = super.client.getData();
		Collection<Unit> units = gd.getUnits();
		String FForderString="// script setregionicon";
		int counter=0;
		if (units!=null && units.size()>0) {
			for (Unit u:units) {
				// "kennen" wir die unit ?
				if (u.getCombatStatus()>=0) {
					// die Orders durchgehen
					Orders o = u.getOrders2();
					if (o.size()>0) {
						for (Order s:o) {
							String ss = s.getText();
							if (ss.toLowerCase().startsWith(FForderString)) {
								// bingo - treffer
								String iconName = ss.substring(FForderString.length()+1);
								log.info("Showing FFTools region icon " + iconName + " in " + u.getRegion().toString() + " def with unit " + u.toString());
								setRegionIcon(iconName, u.getRegion());
								counter++;
							}
						}
					}
				}
			}
			if (counter>0) {
				log.info("added " + counter + " region icons.");
			}
		} else {
			log.info("GameDate contains no units?!...");
		}

		log.info("Show regionIcons finished...");
	}
	
	
	/**
	 * Setzt den tag bei der Region, achtet auf Double-Tags
	 * @param iconname
	 * @param r
	 */
	private void setRegionIcon(String iconname, Region r){
		String finalIconName = iconname;
		if (r==null){
			log.error(getName() + ": error: region is null, iconname: " + iconname);
			return;
		}
		if(r.containsTag(MarkingsImageCellRenderer.ICON_TAG)) {
			StringTokenizer st = new StringTokenizer(r.getTag(MarkingsImageCellRenderer.ICON_TAG), " ");
			while(st.hasMoreTokens()) {
	            String token = st.nextToken();
	            if (token.equalsIgnoreCase(finalIconName)){
	            	// bereits vorhanden
	            	return;
	            }
			}
		}
		// nicht bereits vorhanden->ergänzen
		String newTag = "";
		if(r.containsTag(MarkingsImageCellRenderer.ICON_TAG)) {
			newTag = r.getTag(MarkingsImageCellRenderer.ICON_TAG).concat(" ");
		} 
		newTag = newTag.concat(finalIconName);
		r.putTag(MarkingsImageCellRenderer.ICON_TAG, newTag);
		// log.info(getName() +  ": put to " + r.getName() + " " + r.getCoordinate().toString() + " tagvalue " + newTag);
	}
	
	
}


