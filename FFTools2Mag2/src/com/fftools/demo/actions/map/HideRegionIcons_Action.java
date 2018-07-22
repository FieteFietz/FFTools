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
public class HideRegionIcons_Action extends MenuAction {

	static  final long serialVersionUID = 0;
	private static Logger log = null;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public HideRegionIcons_Action(Client client) {
        super(client);
        setName("Hide RegionIcons");
        log = Logger.getInstance(HideRegionIcons_Action.class);
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
		log.info("Hide RegionIcons started...");
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
								log.info("Removing FFTools region icon " + iconName + " in " + u.getRegion().toString() + " def with unit " + u.toString());
								removeRegionIcon(iconName, u.getRegion());
								counter++;
							}
						}
					}
				}
			}
			if (counter>0) {
				log.info("removed " + counter + " region icons.");
			}
		} else {
			log.info("GameDate contains no units?!...");
		}
		
		
		// refreshen
		
		log.info("Hide RegionIcons finished...");
	}
	
	
	private void removeRegionIcon(String iconname,Region r){
		if(r.containsTag(MarkingsImageCellRenderer.ICON_TAG)) {
			StringBuilder newTag = new StringBuilder();
			StringTokenizer st = new StringTokenizer(r.getTag(MarkingsImageCellRenderer.ICON_TAG), " ");
			while(st.hasMoreTokens()) {
                String token = st.nextToken();
                if (!(token.equals(iconname))){
                	if (newTag.length()>0){
                		newTag.append(" ");
                	}
                	newTag.append(token);
                }
			}
			if (newTag.length()>0){
				r.putTag(MarkingsImageCellRenderer.ICON_TAG, newTag.toString());
			} else {
				r.removeTag(MarkingsImageCellRenderer.ICON_TAG);
			}
		}
	}
	
}
