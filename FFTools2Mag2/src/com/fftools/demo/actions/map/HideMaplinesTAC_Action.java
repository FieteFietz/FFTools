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
import com.fftools.trade.TradeAreaHandler;


/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version
 */
public class HideMaplinesTAC_Action extends MenuAction {

	static  final long serialVersionUID = 0;
	private static Logger log = null;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public HideMaplinesTAC_Action(Client client) {
        super(client);
        setName("Hide TAC MapLines");
        log = Logger.getInstance(HideMaplinesTAC_Action.class);
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
		log.info("Hide TAC MapLines started...");
		GameData gd = super.client.getData();
		ArrayList<String> newTags = new ArrayList<String>(); 
		for (Region r : gd.getRegions()){
			if (r.containsTag("mapline")){
				newTags = new ArrayList<String>(); 
				StringTokenizer st = new StringTokenizer(r.getTag("mapline"), " ");
				// alle Elemente des Tags durchgehen
				while(st.hasMoreTokens()) {
					String token = st.nextToken();
					String[] ss = token.split(",");
					boolean mayStay=true;
					// wir erkennen "unsere" am 7. Parameter
					if (ss.length>6){
						if (ss[6].equalsIgnoreCase(TradeAreaHandler.MAPLINE_TAG_ID)){
							// Treffer
							mayStay=false;
						}
					}
					// den neunen Tag zusammenbasteln - alle, die keine Treffer sind
					if (mayStay){
						newTags.add(token);
					}
				}
				
				if (newTags.size()>0){
					String newnewTag="";
					for (String token : newTags){
						newnewTag = newnewTag.concat(token).concat(" ");
					}
					newnewTag = newnewTag.trim();
					if (!newnewTag.equalsIgnoreCase(r.getTag("mapline"))){
						r.putTag("mapline", newnewTag);
					}
				} else {
					// nix mehr da - remove des kompletten Tags
					r.removeTag("mapline");
				}
				
			}	
		}
		
		
		// refreshen
		
		log.info("Hide TAC MapLines finished...");
	}
}
