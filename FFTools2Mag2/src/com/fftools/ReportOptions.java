package com.fftools;

import java.util.Hashtable;
import java.util.Iterator;

import magellan.library.Region;

/**
 * enthält einstellungen, die per setscripteroption gemacht werden
 * können für region, insel oder gesamten report dienen
 *  
 * @author Fiete
 *
 */

public class ReportOptions {
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private Hashtable<String,String> reportOptions = null;
	
	private Region region = null;

	
	public ReportOptions (Region r){
		this.region = r;
	}
	
	/**
	 * returns an option-value-string for the given String key
	 * @param key
	 * @return
	 */
	public String getOptionString(String key){
		if (this.reportOptions==null){return null;}
		return this.reportOptions.get(key.toLowerCase());
	}
	
	/**
	 * @return the reportOptions
	 */
	public Hashtable<String, String> getReportOptions() {
		return reportOptions;
	}
	
	
	/**
	 * fuegt neues options-pärchen hinzu
	 * @param key
	 * @param value
	 */
	public void addReportOption(String key,String value,boolean doNotOverride){
		if (this.reportOptions==null){
			this.reportOptions = new Hashtable<String,String>();
		}
		if (doNotOverride){
			String v = (String)this.reportOptions.get(key);
			if (v!=null){
				// kein überschreiben
				return;
			}
		}
		
		this.reportOptions.put(key, value);
	}
	
	public void addReportOption(String key,String value){
		// default ist überschreiben
		addReportOption(key, value,false);
	}
	
	
	/**
	 * für die statusausgaben
	 *
	 */
	public void informUs(){
		if (this.region!=null){
			outText.addOutLine("***ScripterOptionen für Region: " + this.region.toString() + " ***");
		}
		if (this.reportOptions==null){
			outText.addOutLine("keine regionsspezifischen optionen gefunden");
		} else {
			for (Iterator<String> iter = this.reportOptions.keySet().iterator();iter.hasNext();){
				String key = (String)iter.next();
				String value = this.reportOptions.get(key);
				outText.addOutLine(key + "=>" + value);
			}	
		}
	}

	/**
	 * @return the region
	 */
	public Region getRegion() {
		return region;
	}
	
}
