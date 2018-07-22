package com.fftools;

import java.io.File;
import java.util.Locale;
import java.util.Properties;

import magellan.client.Client;
import magellan.library.GameData;
import magellan.library.MissingData;
import magellan.library.io.GameDataReader;
import magellan.library.io.cr.CRWriter;
import magellan.library.io.file.FileType;
import magellan.library.io.file.FileTypeFactory;
import magellan.library.utils.Encoding;
import magellan.client.utils.MagellanFinder;
import magellan.library.utils.PropertiesHelper;
import magellan.library.utils.Resources;
import magellan.library.utils.SelfCleaningProperties;

public class RunConsole {
	private static String reportName=null; 
	private static File resourceDir = null;
	private static File settingsDir = null;
	private static final OutTextClass outText = OutTextClass.getInstance();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			outText.addOutLine("This is FFTools2 Version: " + VersionInfo.getVersionInfo() + "\n");
			
			
			/* process command line parameters */
			if (args.length<=1){
				outText.addOutLine("error: no parameter (MagellanDir report)found");
				return;
			}
			if (args.length>2){
				outText.addOutLine("error: wrong parameter count (MagellanDir report) found");
				return;
			}
			
			/*
	        int i = 0;
	        while (i < args.length) {
	        			if (i==0) {resourceDir = new File(args[i]);}
	                    if (i==1) {reportName = args[i];}
	            i++;
	        }
	        */
			
	        resourceDir = new File(args[0]);
	        reportName = args[1];
	        
	        if (!resourceDir.isDirectory()) {
	            System.out.println("<magellan_dir> must be a directory.");
	            System.exit(1);
	        }
			
	        // set the stderr to stdout while there is no log attached */
	        System.setErr(System.out);
	        
	        settingsDir = MagellanFinder.findSettingsDirectory(resourceDir, null);
	        outText.addOutLine("findSettingsDirectory returned: " + settingsDir.toString());
	        outText.addOutLine("took as reportname-parameter: " + reportName);
	       
	        
	        // bin abfang (?)
	        if (settingsDir.toString().endsWith("bin")){
	        	String newDirS = settingsDir.toString().substring(0,settingsDir.toString().length()-4 );
	        	outText.addOutLine("Reducing found Dir to: " + newDirS);
	        	settingsDir = new File(newDirS);
	        }
	        
	        // tell the user where we expect ini files and errors.txt
	        outText.addOutLine("RunConsole main: directory used for ini files: "
	                + settingsDir.toString());
	        
	        
	        PropertiesHelper.setSettingsDirectory(settingsDir);
	        Resources.getInstance().initialize(resourceDir, "");
	        
	       
	        String ff2path = resourceDir.toString()+ File.separator + "FFTools2.jar";
	        VersionInfo.setFFTools2Path(ff2path);
	        
	        outText.addOutLine("Again: This is FFTools2 Version: " + VersionInfo.getVersionInfo() + "\n");
	        outText.addOutLine("FFtools2 expecting here: " + ff2path);
	        
	        // can't call loadRules from here, so we initially work with an
	        // empty ruleset.
	        // This is not very nice, though...
	        GameData data = new MissingData();
	
	        outText.addOutLine("FFTools_Scripter Console Start");

	        // laden
			outText.addOutLine("trying to load: " + reportName);
	        File crFile = new File(reportName);
	        
	        FileType filetype = FileTypeFactory.singleton().createFileType(crFile, true);
	        try {
	            data = new GameDataReader(null).readGameData(filetype);
	        } catch (FileTypeFactory.NoValidEntryException e) {
	        	outText.addOutLine("Fehler beim Laden des Reports: " + e.toString());
	            System.exit(1);
	            return;
	        } catch (Exception exc) {
	            // here we also catch RuntimeExceptions on purpose!
	            // } catch (IOException exc) {
	        	outText.addOutLine("Schwerer Fehler beim Laden des Reports: " + exc.toString());
	            System.exit(1);
	            return;
	        }
	        // in data tatsächlich der geladenen Report?
	        outText.addOutLine(reportName + " loaded with " + data.getRegions().size() + " regions and " + data.getUnits().size() + " units.");
	        data.setLocale(new Locale("de"));
	        magellan.library.utils.Locales.setGUILocale(new Locale("de"));
	        // na denn los...
	        // scripter füllen nach MainScript verlegt
	        ScriptMain sm = new ScriptMain(data);
	        
	        
	        // versuch, ini zu finden?!
	        Properties settings = Client.loadSettings(resourceDir, "magellan.ini");
	        if (settings == null) {
	          outText.addOutLine("Client.loadSettings: settings file " + "magellan.ini" + " does not exist, using default values.");
	          settings = new SelfCleaningProperties();
	        } 
	        
	        sm.setSettings(settings);
	        
	        sm.refreshScripterRegions();
	        
	        sm.runScripts();
	        outText.addOutLine("going to save...");
    		// File tempFile = new File("temp.cr");
    		// Filetype organisieren..brauchen wir zum init des CRWriters
    		filetype = FileTypeFactory.singleton().createFileType(crFile, false);
    		filetype.setCreateBackup(false);
    		CRWriter crw = new CRWriter(sm.gd_ScriptMain,null,filetype,Encoding.ISO.toString());
    		// alle anderen Values des CRw auf default
    		crw.setServerConformance(false);
    		// temp.cr schreiben
    		// sm.gd_ScriptMain.encoding = FileType.ISO_8859_1;
    		sm.gd_ScriptMain.setEncoding(Encoding.ISO.toString());
    		crw.writeSynchronously();
    		
    		
    		crw.close();
    		outText.addOutLine("wrote " + crFile.getName());
    		outText.closeOut();
    		System.exit(0);
		} catch (Throwable exc) { // any fatal error
			outText.setScreenOut(true);
			outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt                       
            System.exit(1);
        }
	}

}
