package com.fftools;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import magellan.client.utils.MagellanFinder;

import com.fftools.utils.FileCopy;

/**
 * 
 * @author Fiete
 * @version $Revision: 1.2 $
 * 
 */
public class VersionInfo {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	
	private static final String versionInfo = "0.985 RC";

	private static String toAdd = "";
	
	private static String FFTools2Path = "";
	
	public static String compileTime = "";
	
	/**
	 * @return the versionInfo
	 */
	public static String getVersionInfo() {
		testJar();
		return versionInfo + ", " + toAdd;
		
	}
	
	private static void testJar(){
		JarFile jarFile = null;
		String verz = "FFTools2.jar";
		if (FFTools2Path.length()>2){
			verz=FFTools2Path;
		}
		try {
			jarFile = new JarFile(verz);
		} catch (Exception e){
			toAdd="jar Not OK";
		}
		if (jarFile!=null){
			// toAdd="jar OK";
			Enumeration<JarEntry> e = jarFile.entries();
			JarEntry jarEntry = e.nextElement();
			
			if (jarEntry!=null){
				Date d = new Date(jarEntry.getTime());
				toAdd ="compiled: " + FileCopy.getDateS(d);
				compileTime = FileCopy.getDateS(d);
			} else {
				toAdd ="jarEntry not found";
			}
			
		} else {
			toAdd ="jar not read: " + verz;
		}
	}

	/**
	 * @param workingDir the workingDir to set
	 */
	public static void setFFTools2Path(String workingDir) {
		outText.addOutLine("setting path to FFTools2 to: " + workingDir);
		VersionInfo.FFTools2Path = workingDir;
	}
	
	
	public static void setFFTools2Path(Properties settings){

		String magellanDirString = settings.getProperty("plugin.helper.magellandir");
		String test = magellanDirString + File.separator + "plugins" + File.separator + "FFTools2.jar";
	    File FFTools2InPlugins = new File(test);
		if (FFTools2InPlugins.exists()){
			VersionInfo.setFFTools2Path(test);
		} else {
			outText.addOutLine("FFToolsPath problem: no Dir: " + test);
			File test2 = MagellanFinder.findMagellanDirectory();
			if (test2.exists()){
				String test3=test2.getAbsolutePath() + File.separator + "plugins" + File.separator + "FFTools2.jar";
				FFTools2InPlugins = new File(test3);
				if (FFTools2InPlugins.exists()){
					VersionInfo.setFFTools2Path(test3);
				} else {
					outText.addOutLine("FFToolsPath problem: no Dir: " + test3);
				}
			}
		}
	}
}
