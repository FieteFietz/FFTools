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
public class ShowMaplinesTAC_Action extends MenuAction {

	static  final long serialVersionUID = 0;
	private static Logger log = null;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public ShowMaplinesTAC_Action(Client client) {
        super(client);
        setName("Show TAC MapLines");
        log = Logger.getInstance(ShowMaplinesTAC_Action.class);
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
		log.info("Show TAC MapLines started...");
		GameData gd = super.client.getData();
		ArrayList<String> newTags = new ArrayList<String>(); 
		for (Region r : gd.getRegions()){
			if (r.containsTag(ScriptMain.MAPLINE_TAG)){
				newTags = new ArrayList<String>();
				StringTokenizer st = new StringTokenizer(r.getTag(ScriptMain.MAPLINE_TAG), " ");
				
				// alle Elemente des Tags durchgehen
				while(st.hasMoreTokens()) {
					String token = st.nextToken();
					String[] ss = token.split(",");
					boolean isNew=false;
					// wir erkennen "unsere" am 7. Parameter
					if (ss.length>6){
						if (ss[6].equalsIgnoreCase(TradeAreaHandler.MAPLINE_TAG_ID)){
							// Treffer
							isNew=true;
						}
					}
					// den neunen Tag zusammenbasteln - alle, die keine Treffer sind
					if (isNew){
						newTags.add(token);
					}
				}
				
				
				// jetzt checken, ob wir einen mapline-Tag haben
				if (r.containsTag("mapline")){
					// wir wollen keinen doppelt drin haben
					if (newTags.size()>0){
						// ergänzen, wenn noch nicht da...
						String newnewTag = "";
						ArrayList<String> toAdd = new ArrayList<String>();  // enthält zu ergänzende Tags
						for (String token : newTags){
							StringTokenizer st2 = new StringTokenizer(r.getTag("mapline"), " ");
							// alle Elemente des Tags durchgehen
							boolean already_included=false;
							while(st2.hasMoreTokens()) {
								String actToken = st2.nextToken();
								if (actToken.equalsIgnoreCase(token)){
									already_included=true;
								}
							}
							if (!already_included){
								toAdd.add(token);
							}
						}
						
						if (toAdd.size()>0){
							newnewTag = r.getTag("mapline");
							for (String token : toAdd){
								newnewTag = newnewTag.concat(" ").concat(token);
							}
							r.putTag("mapline", newnewTag);
						}
						
					}
				} else {
					// es gibt noch keinen, ggf einen anlegen
					if (newTags.size()>0){
						// ergänzen
						String newnewTag = "";
						for (String token : newTags){
							newnewTag = newnewTag.concat(token).concat(" ");
						}
						newnewTag = newnewTag.trim();
						r.putTag("mapline", newnewTag);
					}
				}
			}	
		}

		log.info("Show TAC MapLines finished...");
	}
}
