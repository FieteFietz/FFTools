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


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

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
		
		String serverInfo = "http://fftools2.fietefietz.de/getVersion.php";
		String info2 = "";
		String info3 = "";
		 String sx1 = "";
         String sx2 = "";
		
		String thisCompileTime = VersionInfo.compileTime;
		
		BufferedReader br = null;
        InputStreamReader isr = null;
        URL url = null;
        try {
            url = new URL(serverInfo);
        } catch (MalformedURLException e2) {
            info2 = e2.getLocalizedMessage();
        }
        if (url != null) {
        	try {
                isr = new InputStreamReader(url.openStream());
                br = new BufferedReader(isr);
                info2 = br.readLine().trim();
                
                if (info2.compareTo(thisCompileTime)==0) {
                	info3 = info2 + ", Version ist aktuell";
                }
                if (info2.compareTo(thisCompileTime)>0) {
                	info3 = info2 + ", neue Version auf Server!";
                }
                if (info2.compareTo(thisCompileTime)<0) {
                	info3 = info2 + ", Upload auf Server ausstehend";
                }
            } catch (IOException e3) {
            	info2 = e3.getLocalizedMessage();
            }
        }
		new MsgBox(c,info,"About FFTools2",false,"ServerVersion: " + info3);
	}

	
}
