package com.fftools.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import magellan.client.Client;
import magellan.client.swing.InternationalizedDialog;
import magellan.library.GameData;
import magellan.library.Region;
import magellan.library.utils.logging.Logger;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.VersionInfo;


/**
 * A First Test for FFTools
 * By Fiete
 * 
 */
public class FFToolsRunScript extends InternationalizedDialog implements Runnable{
	private static final Logger log = Logger.getInstance(Client.class);
	
	static final long serialVersionUID = 0;
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	private JTextArea txtOutput = null;
	private GameData gd = null;
    private Client c = null;
    private JScrollPane sp = null;
    private Collection<Region> regions = null;
    
    
    /**
	 * Create a new FFToolsRunTest Object
	 * 
	 */
    public FFToolsRunScript(Frame owner, boolean modal, GameData _data){
    	super(owner, modal);
		
		gd = _data;
		c = (Client)owner;
		init();
		this.regions = _data.getRegions();
		// runTest();
    }
    
	

	private void init() {
		// FFTools2 path to jar
		VersionInfo.setFFTools2Path(c.getProperties());
		setContentPane(getMainPanel());
		setTitle("FFTools2 Scriptlauf");
		setSize(500,400);
		outText.addOutLine("init done");
	}

	private Container getMainPanel() {
		JPanel mainPanel = new JPanel(new BorderLayout());
		txtOutput = new JTextArea("start");
		outText.setTxtOut(txtOutput);
		sp = new JScrollPane(txtOutput,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setPreferredSize(new Dimension(500,400));
		txtOutput.setEditable(false);
		mainPanel.add(sp,BorderLayout.CENTER);
		return mainPanel;
	}

	/**
	 * threadstart
	 *
	 */
	public void run(){
		runTest();
	}
	
	/**
	 * startet das eigentliche geschehen
	 *
	 */
	private void runTest() {
		log.info("FFTools lauf gestartet");
		outText.addOutLine("start runTest");
		int numberOfRegions = regions.size();
		int numberOfUnits = gd.getUnits().size();
		outText.addOutLine("found " + numberOfRegions + " Regions and " + numberOfUnits + " Units.");
		
		// boolean RegionHasScripter = false;
		// neuen Scripter basteln
//		 dem fuer die Ausgaben die JTextPane mitgeben und den client
		ScriptMain scriptMain = new ScriptMain(c,txtOutput);
		
		// durch die Regionen wandern..
		/**
		for (Iterator<Region> i=regions.values().iterator(); i.hasNext(); ){
			Region r = (Region) i.next();
			for (Iterator<Unit> i2=r.units().iterator();i2.hasNext();){
				Unit u = (Unit) i2.next();
				if (ScriptUnit.isScriptUnit(u)){
					scriptMain.addUnit(u);
					scriptMain.setFactionTrustlevel(u.getFaction());
				} 
			}
		}
		*/
		outText.addOutLine("Extracting script units");
		scriptMain.extractScriptUnits();
		
		
		// nu mal starten
		outText.addOutLine("Scripter enthaelt " + scriptMain.getNumberOfScriptUnits() + " units...starte scripter");
		outText.addOutLine("(BigCount: " + scriptMain.getNumberOfScriptPersons() + " persons)");
		scriptMain.runScripts();
		
		
		// test, der Versuch nach unten zu scrollen
		txtOutput.setCaretPosition((txtOutput.getText().length()));
		outText.addOutLine("\ntest finished");
		log.info("FFTools lauf beendet");
	}

	
	protected void quit() {
			dispose();
	}
	
	
}
