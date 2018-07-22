package com.fftools;

import java.io.File;
import java.util.Locale;
import java.util.Properties;

import com.fftools.utils.IslandInfo;

import magellan.client.Client;
import magellan.library.GameData;
import magellan.library.MissingData;
import magellan.library.io.GameDataReader;
import magellan.library.io.file.FileType;
import magellan.library.io.file.FileTypeFactory;
import magellan.client.utils.MagellanFinder;
import magellan.library.utils.PropertiesHelper;
import magellan.library.utils.Resources;
import magellan.library.utils.SelfCleaningProperties;

public class RunIslandInfo {
	private static String reportName=null; 
	private static final OutTextClass outText = OutTextClass.getInstance();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			outText.addOutLine("This is FFTools2 Version: " + VersionInfo.getVersionInfo() + "\n");
			
			
			/* process command line parameters */
			if (args.length==0){
				outText.addOutLine("error: no parameter (report)found");
				return;
			}
			
	        int i = 0;
	        while (i < args.length) {
	                    if (i==0) {reportName = args[i];}
	            i++;
	        }
			
	        File fileDir = null; // the directory to store ini files and
	        // stuff in
	        // set the stderr to stdout while there is no log attached */
	        System.setErr(System.out);
	
	        /* determine default value for files directory */
	        fileDir = MagellanFinder.findMagellanDirectory();
	        
	        if (fileDir==null){
	        	outText.addOutLine("MagellanFinder returned null!");
	        	fileDir = new File(".");
	        	outText.addOutLine("Now using: " + fileDir.getAbsolutePath() + "\n");
	        }
	        
	        File settFileDir = null;
	        settFileDir = MagellanFinder.findSettingsDirectory(fileDir,
	                settFileDir);
	        
	        // bin abfang (?)
	        if (settFileDir.toString().endsWith("bin")){
	        	String newDirS = settFileDir.toString().substring(0,settFileDir.toString().length()-4 );
	        	outText.addOutLine("Reducing found Dir to: " + newDirS);
	        	settFileDir = new File(newDirS);
	        }
	        
	        // tell the user where we expect ini files and errors.txt
	        outText.addOutLine("Client.main(): directory used for ini files: "
	                + settFileDir.toString());
	        
	        
	        Resources.getInstance().initialize(settFileDir, "");
	        String ff2path = settFileDir.toString()+ File.separator + "FFTools2.jar";
	        VersionInfo.setFFTools2Path(ff2path);
	        
	        outText.addOutLine("Again: This is FFTools2 Version: " + VersionInfo.getVersionInfo() + "\n");
	        
	        // can't call loadRules from here, so we initially work with an
	        // empty ruleset.
	        // This is not very nice, though...
	        GameData data = new MissingData();
	
	        outText.addOutLine("FFTools_Scripter Console Start");
	        // Properties settings =  new SelfCleaningProperties();
	        // init the translations with the loaded settings
			// Translations.setClassLoader(new ResourcePathClassLoader(settings));
	        
	        // tell the user where we expect ini files and errors.txt
	        PropertiesHelper.setSettingsDirectory(settFileDir);

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
	        Properties settings = Client.loadSettings(settFileDir, "magellan.ini");
	        if (settings == null) {
	          outText.addOutLine("Client.loadSettings: settings file " + "magellan.ini" + " does not exist, using default values.");
	          settings = new SelfCleaningProperties();
	          settings.setProperty(PropertiesHelper.CLIENT_LOOK_AND_FEEL,"Windows");
	          settings.setProperty(PropertiesHelper.ADVANCEDSHAPERENDERER_SETS,",Einkaufsgut");
	          settings.setProperty(PropertiesHelper.ADVANCEDSHAPERENDERER_CURRENT_SET,"Einkaufsgut");
	          settings.setProperty(PropertiesHelper.ADVANCEDSHAPERENDERER+"Einkaufsgut"+PropertiesHelper.ADVANCEDSHAPERENDERER_CURRENT,"\u00A7if\u00A7<\u00A7price\u00A7\u00D6l\u00A7-1\u00A71\u00A7else\u00A7if\u00A7<\u00A7price\u00A7Weihrauch\u00A7-1\u00A72\u00A7else\u00A7if\u00A7<\u00A7price\u00A7Seide\u00A7-1\u00A73\u00A7else\u00A7if\u00A7<\u00A7price\u00A7Myrrhe\u00A7-1\u00A74\u00A7else\u00A7if\u00A7<\u00A7price\u00A7Juwel\u00A7-1\u00A75\u00A7else\u00A7if\u00A7<\u00A7price\u00A7Gew\u00FCrz\u00A7-1\u00A76\u00A7else\u00A7if\u00A7<\u00A7price\u00A7Balsam\u00A7-1\u00A77\u00A7end\u00A7end\u00A7end\u00A7end\u00A7end\u00A7end\u00A7");
	          settings.setProperty(PropertiesHelper.ADVANCEDSHAPERENDERER+"Einkaufsgut"+PropertiesHelper.ADVANCEDSHAPERENDERER_MAXIMUM,"10");
	          settings.setProperty(PropertiesHelper.ADVANCEDSHAPERENDERER+"Einkaufsgut"+PropertiesHelper.ADVANCEDSHAPERENDERER_COLORS,"0.0;223,131,39;0.12162162;220,142,24;0.14864865;153,153,153;0.23648648;153,153,153;0.26013514;204,255,255;0.3445946;204,255,255;0.3716216;0,204,0;0.42905405;0,204,0;0.46283785;255,51,0;0.5371622;255,51,0;0.5608108;255,255,0;0.6317568;255,255,0;0.6621622;51,51,255;1.0;0,51,255");
	          settings.setProperty(PropertiesHelper.ADVANCEDSHAPERENDERER+"Einkaufsgut"+PropertiesHelper.ADVANCEDSHAPERENDERER_VALUES,"0.0;0.0;1.0;1.0");
	          settings.setProperty(PropertiesHelper.ADVANCEDSHAPERENDERER+"Einkaufsgut"+PropertiesHelper.ADVANCEDSHAPERENDERER_MINIMUM,"0");
	          // Message Panel Default colors.
	          settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_EVENTS_COLOR,"#009999"); // Format: #RRGGBB
	          settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_MOVEMENTS_COLOR,"#000000");// Format: #RRGGBB
	          settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_ECONOMY_COLOR,"#000066");// Format: #RRGGBB
	          settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_MAGIC_COLOR,"#666600");// Format: #RRGGBB
	          settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_STUDY_COLOR,"#006666");// Format: #RRGGBB
	          settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_PRODUCTION_COLOR,"#009900");// Format: #RRGGBB
	          settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_ERRORS_COLOR,"#990000");// Format: #RRGGBB
	          settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_BATTLE_COLOR,"#999900");// Format: #RRGGBB
	          
	          // try to set path to ECheck
	         
	        } else {
	          
	          
	          // backward compatibility for white message tags (it's now the text color)
	          if (settings.getProperty(PropertiesHelper.MESSAGETYPE_SECTION_EVENTS_COLOR,"-").equals("#FFFFFF")) {
	            settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_EVENTS_COLOR,"#009999"); // Format: #RRGGBB
	          }
	          if (settings.getProperty(PropertiesHelper.MESSAGETYPE_SECTION_MOVEMENTS_COLOR,"-").equals("#FFFFFF")) {
	            settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_MOVEMENTS_COLOR,"#000000");// Format: #RRGGBB
	          }
	          if (settings.getProperty(PropertiesHelper.MESSAGETYPE_SECTION_ECONOMY_COLOR,"-").equals("#FFFFFF")) {
	            settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_ECONOMY_COLOR,"#000066");// Format: #RRGGBB
	          }
	          if (settings.getProperty(PropertiesHelper.MESSAGETYPE_SECTION_MAGIC_COLOR,"-").equals("#FFFFFF")) {
	            settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_MAGIC_COLOR,"#666600");// Format: #RRGGBB
	          }
	          if (settings.getProperty(PropertiesHelper.MESSAGETYPE_SECTION_STUDY_COLOR,"-").equals("#FFFFFF")) {
	            settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_STUDY_COLOR,"#006666");// Format: #RRGGBB
	          }
	          if (settings.getProperty(PropertiesHelper.MESSAGETYPE_SECTION_PRODUCTION_COLOR,"-").equals("#FFFFFF")) {
	            settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_PRODUCTION_COLOR,"#009900");// Format: #RRGGBB
	          }
	          if (settings.getProperty(PropertiesHelper.MESSAGETYPE_SECTION_ERRORS_COLOR,"-").equals("#FFFFFF")) {
	            settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_ERRORS_COLOR,"#990000");// Format: #RRGGBB
	          }
	          if (settings.getProperty(PropertiesHelper.MESSAGETYPE_SECTION_BATTLE_COLOR,"-").equals("#FFFFFF")) {
	            settings.setProperty(PropertiesHelper.MESSAGETYPE_SECTION_BATTLE_COLOR,"#999900");// Format: #RRGGBB
	          }
	        }
	        
	        sm.setSettings(settings);
	        outText.addOutLine("starting IslandInfo");
	        IslandInfo.writeInfos(data);
	        outText.addOutLine("finished IslandInfo");
    		outText.closeOut();
		} catch (Throwable exc) { // any fatal error
			outText.setScreenOut(true);
			outText.addOutLine(exc.toString()); // print it so it can be written to errors.txt                       
            System.exit(1);
        }
	}

}
