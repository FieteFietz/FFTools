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
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import com.fftools.utils.FFToolsTags;
import com.fftools.utils.FileCopy;
import com.fftools.utils.MsgBox;

import magellan.client.Client;
import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.Unit;
import magellan.library.gamebinding.EresseaSpecificStuff;
import magellan.library.io.file.FileType;
import magellan.library.io.file.FileTypeFactory;
import magellan.library.utils.Encoding;
import magellan.library.utils.OrderWriter;


/**
 * DOCUMENT ME!
 *
 * @author Fiete
 * @version
 */
public class CreateOrderTextFiles extends MenuAction {

	private static final long serialVersionUID = 0;
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public CreateOrderTextFiles(Client client) {
        super(client);
        setName("Write Orders");
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
		FFToolsTags.AllTags2Orders(gd);
		int counter=0;
		for (Iterator<Faction> iter = gd.getFactions().iterator();iter.hasNext();){
    		Faction F = (Faction)iter.next();
    		if (F.getPassword()!=null) {
    			
    			// gibt es was zu schreiben?
    			boolean somethingToWrite = false;
    			for (Iterator<Unit> F_iter = F.units().iterator();F_iter.hasNext();) {
    				Unit actU = (Unit)F_iter.next();
    				if (actU.isOrdersConfirmed()) {
    					somethingToWrite = true;
    					break;
    				}
    			}
    			if (somethingToWrite) {
    				counter++;
    				String myFileName = "bef_" + F.getID().toString() +"_" + gd.getDate().getDate() + "_"  + FileCopy.getDateS() + ".txt";
    				try {
    					// FileType fT = new FileType(new File(myFileName),false);
    					FileType fT = FileTypeFactory.singleton().createFileType(new File(myFileName), false);
    					// FileWriter FW = new FileWriter(myFileName,true);
    					// BufferedWriter bFW = new BufferedWriter(FW);
    					fT.setCreateBackup(false);
    					// Default leads since UTF-8 to errors from server
    					// Writer wR = fT.createWriter(FileType.DEFAULT_ENCODING);
    					Writer wR = fT.createWriter(Encoding.ISO.toString());
    					// Writer wR = fT.createWriter(FileType.UTF_8);
    					if (write(wR, false,true, F,gd)) {
    						// new MsgBox(null ,"wrote " + myFileName,"OK " + F.getID().toString(),true);
    					} else {
    						new MsgBox(null ,"Probelm writing orders","Problem " + F.getID().toString(),true);
    					}
    					
    					wR.flush();
    					wR.close();
    				
    				} catch(IOException ioe) {
    					
    					new MsgBox(null ,ioe.toString(),"Problem " + F.getID().toString(),true);
    				}
    			}
    		}
    	}	
		new MsgBox(null ,"Fertig: " + counter,"OK",true);
		
	}
	
	
	private boolean write(Writer stream, boolean forceUnixLineBreaks, boolean closeStream, Faction faction, GameData gd) {

		// Faction faction = (Faction) cmbFaction.getSelectedItem();
		
		try {
			// OrderWriter cw = new OrderWriter(data, faction);
			OrderWriter cw;
			cw = (OrderWriter) new EresseaSpecificStuff().getOrderWriter();
			cw.setFaction(faction);
			cw.setGameData(gd);
			cw.setAddECheckComments(true);
			cw.setRemoveComments(true, false);
			cw.setConfirmedOnly(true);
			cw.setWriteUnitTagsAsVorlageComment(false);

			cw.setForceUnixLineBreaks(forceUnixLineBreaks);
			cw.setWriteTimeStamp(false);

			int writtenUnits = cw.write(stream);
			if (writtenUnits<=0) {
				return false;
			}
		} catch(IOException ioe) {
			return false;
		}

		return true;
	}
	
}
