package com.fftools.utils;

//FileCopy.java
//Das Beispiel ist dem Buch "Java Examples in a Nutshell" von
//David Flanagan (O'Reilly) entnommen. Ist ein sehr schönes
//und praktisches Buch!

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileCopy {

	// copy ist static, da hier keine Operationen auf einzelnen Objekten vom Typ FileCopy
	// durchgefuehrt werden. Stattdessen hat copy() eine Funktionalitaet, die die Klasse
	// _an_sich_ haben soll!
	public static boolean copy(String from_name, String to_name) throws IOException {
		File from_file = new File(from_name);
		File to_file = new File(to_name);
		
		// Hier müßten eigentlich jede Menge Fehlerabfragen hin!
		// deshalb werden auch erst zwei File-Objekte instanziiert. Die Klasse File
		// haelt naemlich einiges an Methoden bereit, um den Status von Files zu 
		// ueberpruefen.
		
		FileInputStream from = null;
		FileOutputStream to = null;
		
		try {
			from = new FileInputStream(from_file);
			to = new FileOutputStream(to_file);
			byte[] buffer = new byte[4096];
			int bytes_read;
			// folgende Zeile hat es in sich!
			// zunaechst wird der Ausdruck "bytes_read = from.read(buffer)"
			// ausgewertet. Dabei wird buffer[] automatisch gefüllt.
			// read(), gibt ausserdem einen Wert zurueck, der gleich der
			// Anzahl der gelesenen Zeichen ist. Falls keine Zeichen gelesen
			// wurden, wird -1 zurueckgegeben.
			// Ein komplexer Ausdruck wie "bytes_read = from.read(buffer)"
			// hat immer den Wert, der links vom Gleichheitszeichen steht,
			// also hier den Wert von bytes_read. Wurden Zeichen gelesen,
			// ist die while-Bedingung also wahr, und der Schleifenkoerper
			// wird ausgefuehrt.
			// Wurden keine Zeichen gelesen, erhaelt bytes_read den Wert -1.
			// (-1 != -1) ist nicht wahr, also wird die Schleife abgebrochen.
			while ( (bytes_read = from.read(buffer)) != -1)
				to.write(buffer, 0, bytes_read);
		}
		finally {
			if(from != null) 
				try { 
				    from.close();       // alle FileStreams schoen schliessen... 
				} 
				catch  (IOException e) { 
				    System.out.println(e);
				    return false;
				}
			if(to != null) 
				try { 
				    to.close(); 
				} 
				catch  (IOException e) { 
				    System.out.println(e);
				    return false;
				}
		}
		
		return true;
	}
	
	public static String getDateS() {
		SimpleDateFormat fr = new SimpleDateFormat("_yyyyMMdd_HHmmss");
  	  	String dateS =  fr.format(new Date());
  	  	return dateS;
	}
	
	public static String getDateSDay() {
		SimpleDateFormat fr = new SimpleDateFormat("yyyyMMdd");
  	  	String dateS =  fr.format(new Date());
  	  	return dateS;
	}
	
	public static String getDateS(Date d) {
		SimpleDateFormat fr = new SimpleDateFormat("yyyyMMdd_HHmmss");
  	  	String dateS =  fr.format(d);
  	  	return dateS;
	}
	
	
}

