package com.fftools;

import javax.swing.JTextArea;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Die Klasse wird statisch in alle Classen eingebunden, die Ausgaben
 * in Richtung Datei (FFTools_log.txt) oder in Richtung
 * JFrame via JTextArea machen wollen
 * Alle Ausgaben laufen defaultmässig in die TextDatei
 * Die Anzeige im JTextArea kann unterbunden werden mittels
 * setScreenOut
 * 
 * @author Fiete
 *
 */

public class OutTextClass {
	private JTextArea txtOutput = null;

	private boolean screenOut = true;

	private boolean showedVersionsTXTOutput = false;
	
	private String defaultFileName = "FFTools_log";
	private String actFileName = null;
	
	private long lastPointSetAt = 0;
	private long setPointEveryMilliseks = 300;
	
	private int cntPoints = 0;
	private int maxRowPoints = 200;
	
	private HashMap<String,FileWriter> fileWriters = null; 
	
	private static OutTextClass DEFAULT = new OutTextClass();
	
	/**
	 * setzt den JTextArea, sollte recht früh, beim allerersten
	 * Aufruf gesetzt werden, in der classe, die als erstes
	 * OutTextClass statisch referenziert
	 */
	public void setTxtOut(JTextArea _txtOutput){
		this.txtOutput = _txtOutput;
	}
	
	public JTextArea getTxtOut(){
		if (this.txtOutput==null) {
			return null;
		} else {
			return this.txtOutput;
		}
	}
	
	
	public void addPoint(){
		Date date = new Date();
		
		long actMillis = date.getTime();
		if (actMillis-lastPointSetAt>setPointEveryMilliseks){
			lastPointSetAt = actMillis;
			if (txtOutput!=null){
				txtOutput.append(".");
				txtOutput.setCaretPosition((txtOutput.getText().length()));
			} else {
				System.out.print(".");
				System.out.flush();
			}
			cntPoints++;
			checkMaxRowPoints();
		}
	}
	
	
	private void checkMaxRowPoints(){
		if (cntPoints>maxRowPoints){
			cntPoints=0;
			if (txtOutput!=null){
				txtOutput.append("*\r\n");
				txtOutput.setCaretPosition((txtOutput.getText().length()));
			} else {
				System.out.print("*\r\n");
				System.out.flush();
			}
			
		}
	}
	
	/**
	 * Ausgabe des Strings s, Zeilenendzeichen wird (davor) mitgesetzt
	 * @param s
	 */
	public void addOutLine(String s) {
		String sS = "\r\n" + getLogDateS() + s;
		this.addOutChars(sS);
		this.cntPoints = sS.length();
		// checkMaxRowPoints();
	}
	
	public void addNewLine(){
		this.addOutChars("\r\n" + getLogDateS());
		cntPoints=0;
	}
	
	
	public void addOutChars(String s){
		String sS = s;
		String originalString = sS;
		if (screenOut) {
			if (txtOutput!=null){
				if (!showedVersionsTXTOutput){
					sS = getVersionsString() + sS;
					showedVersionsTXTOutput = true;
				}
				txtOutput.append(sS);
				txtOutput.setCaretPosition((txtOutput.getText().length()));
			} else {
				System.out.print(sS);
				System.out.flush();
			}
			cntPoints+=sS.length();
			checkMaxRowPoints();
		}
		this.writeToLog(originalString);
	}
	
	
	public void addOutChars(String s,int charAmount){
		String sS = formatString(s, charAmount, " ");
		String originalString = sS;
		if (screenOut) {
			if (txtOutput!=null){
				if (!showedVersionsTXTOutput){
					sS = getVersionsString() + sS;
					showedVersionsTXTOutput = true;
				}
				txtOutput.append(sS);
				txtOutput.setCaretPosition((txtOutput.getText().length()));
			} else {
				System.out.print(sS);
				System.out.flush();
			}
			cntPoints+=sS.length();
			checkMaxRowPoints();
		}
		this.writeToLog(originalString);
	}
	
	
	private String formatString(String s, int countChars, String aChar){
		String erg=null;
		
		if (s==null){
			s="";
		}
		
		if (s.length()>countChars){
			return s.substring(0,countChars-1);
		}
		
		if (aChar==null || aChar.length()<1){
			return "ERR";
		}
		
		erg = s;
		while (erg.length()<countChars){
			erg = aChar + erg;
		}

		return erg;
	}
	
	
	public void addOutLine(String s,boolean explizitScreenout){
		boolean change_value = false;
		
		if (explizitScreenout!=this.screenOut){
			change_value=true;
			this.screenOut = explizitScreenout;
		}
		
		this.addOutLine(s);
		
		if (change_value){
			this.screenOut = !this.screenOut;
		}
	}
	
	
	private String getVersionsString(){
		String versionS = "\r\nVersions:\r\n";
		versionS+="FFTools2: " + VersionInfo.getVersionInfo() + "\r\n";
		versionS+="Magellan: " + magellan.library.utils.VersionInfo.getVersion(null) + "\r\n";
		return versionS;
	}
	
	/**
	 * 
	 */
	public void closeOut() {
		String s = getDateS() + " closing log.\r\n";
		this.addOutLine(s);
	}
	
	/**
	 * Ausgabe aller Strings in die TextDatei
	 */
	
	public void writeToLog(String s){
		if (s==null){
			s = "null - line";
		}
		if (actFileName==null){
			actFileName=defaultFileName;
		}
		FileWriter fileWriter = null;
		if (fileWriters!=null){
			fileWriter = fileWriters.get(actFileName);
		}
		if (fileWriter==null){
			try {
				actFileName = actFileName.replace("?", "_");
				fileWriter = new FileWriter(actFileName + ".txt",true);
				
				if (fileWriters==null){
					fileWriters=new HashMap<String, FileWriter>();
				}
				fileWriters.put(actFileName, fileWriter);
				
				fileWriter.write("\r\n\r\n\r\n");
				fileWriter.write("***** " + getLogDateS() + " new logentries follow....******\r\n");
				fileWriter.write(getVersionsString());
				fileWriter.write("\r\n");
			} catch (IOException e) {
				// pech
				System.out.print("Fehler beim anlegen der Datei: " + actFileName);
				System.out.print(e.toString());
			}
		}
		try {
			fileWriter.write(s);
			fileWriter.flush();
		} catch (IOException e) {
			// dumm
			System.out.print("Fehler beim Schreiben in die Datei: " + actFileName);
			System.out.print(e.toString());
		}
	}
	
	/**
	 * bestimmt, ob addOutLine auch versucht, auf JTextArea zu schreiben
	 * oder ob lediglich in textFile geschrieben wird
	 * @param _screenOut
	 */
	
	public void setScreenOut(boolean _screenOut){
		// this.addOutLine("new setting for screenOut: " + this.screenOut);
		this.screenOut = _screenOut;
	}
	
	
	
	
	
	/**
	 * liefert für alle Aufrufe die beim Initialiserung erzeugte
	 * Default-OutTextClass
	 * @return
	 */
	
	public static OutTextClass getInstance() {
		return DEFAULT;
	}
	
	public static String getDateS() {
		SimpleDateFormat fr = new SimpleDateFormat("_yyyyMMdd_HHmmss");
  	  	String dateS =  fr.format(new Date());
  	  	return dateS;
	}
	
	public static String getLogDateS() {
		SimpleDateFormat fr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  	  	String dateS =  fr.format(new Date()) + ":";
  	  	return dateS;
	}
	
	
	public void setFile(String s){
		if (s!=null && s.length()>3){
			actFileName = s;
		}
	}
	
	public String getActFileName(){
		return actFileName;
	}
	
	
	public void setFileStandard(){
		actFileName = defaultFileName;
	}

	/**
	 * @return the screenOut
	 */
	public boolean isScreenOut() {
		return screenOut;
	}
	
}
