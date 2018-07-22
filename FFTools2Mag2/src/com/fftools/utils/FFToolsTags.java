package com.fftools.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import magellan.library.GameData;
import magellan.library.Order;
import magellan.library.TempUnit;
import magellan.library.Unit;
import magellan.library.UnitID;
import magellan.library.io.cr.CRParser;

import com.fftools.OutTextClass;



/**
 * Tag-Handling
 * @author Fiete
 *
 */

public class FFToolsTags {

	private static final OutTextClass outText = OutTextClass.getInstance();
	
	public static void AllOrders2Tags(GameData gd){
		// aus gefundenen Tags orders machen....
		// die ejcTaggable ein wenig besser lesbar machen
		// alle anderen aber unterstützen...
		
		// normale units durchlaufen
		for (Unit u:gd.getUnits()) {
			FFToolsTags.Orders2TagsUnit(u);
		}
		
		// TempUnits durchlaufen
		for (@SuppressWarnings("deprecation")
		Iterator<TempUnit> iter = gd.tempUnits().values().iterator();iter.hasNext();) {
			Unit u = (Unit)iter.next();
			FFToolsTags.Orders2TagsUnit(u);
		}
		
	}
	
	public static void AllTags2Orders(GameData gd){
		// aus gefundenen Tags orders machen....
		// die ejcTaggable ein wenig besser lesbar machen
		// alle anderen aber unterstützen...
		
		// normale units durchlaufen
		for (Unit u:gd.getUnits()) {
			FFToolsTags.Tags2OrdersUnit(u);
		}
		
		// TempUnits durchlaufen
		for (@SuppressWarnings("deprecation")
		Iterator<TempUnit> iter = gd.tempUnits().values().iterator();iter.hasNext();) {
			// Unit u = (Unit)iter.next();
			FFToolsTags.Tags2OrdersUnit(iter.next());
		}
		
	}
	
	
	public static void Tags2OrdersUnit(Unit u){
		Map<String,String> z = u.getTagMap();
		if (z.size()>0) {
			for (Iterator<String> iter2 = z.keySet().iterator();iter2.hasNext();){
				// key
				String k = (String) iter2.next();
				// value
				String v = (String)z.get(k);
				// bei dem bekannten strings ne ersetzung vornhemen
				if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING)){
					k = "eTag1";
				} else if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING2)){
					k = "eTag2";
				} else if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING3)){
					k = "eTag3";
				} else if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING4)){
					k = "eTag4";
				} else if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING5)){
					k = "eTag5";
				}
				
				// order generieren
				// jetz gesetzt
				// setTag <key> <value>
				// kurzerhand bedingung: keine Leerzeichen...
			    // Marc: Tag 3,4 und 5 werden nicht gerettet, da sie bei jeden Lauf neu erzeugt werden
				
				if (!k.equalsIgnoreCase("eTag3")&&!k.equalsIgnoreCase("eTag4")&&!k.equalsIgnoreCase("eTag5")){
				   FFToolsTags.addOrder(u,"// setTag " + k + " " + v, true);
			    }
		   }
		}
	}
	
	public static void Tags2FileUnit(Unit u, FileWriter fileWriter){
		Map<String,String> z = u.getTagMap();
		if (z.size()>0) {
			for (Iterator<String> iter2 = z.keySet().iterator();iter2.hasNext();){
				// key
				String k = (String) iter2.next();
				// value
				String v = (String)z.get(k);
				// bei dem bekannten strings ne ersetzung vornhemen
				if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING)){
					k = "eTag1";
				} else if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING2)){
					k = "eTag2";
				} else if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING3)){
					k = "eTag3";
				} else if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING4)){
					k = "eTag4";
				} else if (k.equalsIgnoreCase(CRParser.TAGGABLE_STRING5)){
					k = "eTag5";
				}
				
				// order generieren
				// jetz gesetzt
				// setTag <key> <value>
				// kurzerhand bedingung: keine Leerzeichen...
			    // Marc: Tag 3,4 und 5 werden nicht gerettet, da sie bei jeden Lauf neu erzeugt werden
				
				if (!k.equalsIgnoreCase("eTag3")&&!k.equalsIgnoreCase("eTag4")&&!k.equalsIgnoreCase("eTag5")){
				   // FFToolsTags.addOrder(u,"// setTag " + k + " " + v, true);
				   try {
					   fileWriter.write(u.toString(false) + ";" + k + ";" + v +"\n");
				   } catch (IOException e) {
					   // pech
					   new MsgBox(null,"Fehler:" + e.toString(),"Fehler",true);
				   }
			    }
		   }
		}
	}
	
	private static void addOrder(Unit u,String s,boolean no_doubles) {
		boolean add_ok = true;
		
		if (no_doubles){
			for (Order o:u.getOrders2()){
				String s_old = o.getText();
				if (s_old.equalsIgnoreCase(s)) {add_ok = false;break;}
			}
		}
		if (add_ok){
			u.addOrder(s,false, 1);
			// u.setOrdersChanged(true);
			// u.ordersHaveChanged();
			u.reparseOrders();
		}
	}
	
	public static void Orders2TagsUnit(Unit u){
		Collection<Order> c = u.getOrders2();
		if (c.size()>0) {
			for (Order o:c){
				String s = o.getText();
				// starten wir mit dem richtigen Anfang?
				if (s.length()>8 && s.substring(0,9).equalsIgnoreCase("// setTag")) {
					// yep...geht ohne tokenizer...
					s = s.substring(10);
					int i = s.indexOf(" ");
					if (i>0) {
						// key
						String k = s.substring(0, i);
						// value
						String v = s.substring(i+1);
						// bei dem bekannten strings ne ersetzung vornhemen
						if (k.equalsIgnoreCase("eTag1")){
							k = CRParser.TAGGABLE_STRING;
						} else if (k.equalsIgnoreCase("eTag2")){
							k = CRParser.TAGGABLE_STRING2;
						} else if (k.equalsIgnoreCase("eTag3")){
							k = CRParser.TAGGABLE_STRING3;
						} else if (k.equalsIgnoreCase("eTag4")){
							k = CRParser.TAGGABLE_STRING4;
						} else if (k.equalsIgnoreCase("eTag5")){
							k = CRParser.TAGGABLE_STRING5;
						}
						
						// order generieren
						// jetz gesetzt
						// setTag <key> <value>
						// kurzerhand bedingung: keine Leerzeichen...
						FFToolsTags.addTag(u,k, v, true);
										}
				}
			}
		  // Nun die lästigen // setTag löschen.
		  ArrayList<Order> neueOrders = new ArrayList<Order>();
		  for (Order o:c){
			  String zeile = o.getText();
			  // Alle Zeilen die nicht "setTag" enthalten wandern in neue Orders
			 
			  // sehr kurze Zeilen gehen immer....
			  if (zeile.length()<9){
				  neueOrders.add(o); 
			  }
			  else {
				  // längere Zeilen muß man prüfen
				  if (!zeile.substring(0,9).equalsIgnoreCase("// setTag")){
					  neueOrders.add(o);
				  }
			  }
			  
		  }
		  u.setOrders2(neueOrders);
		 
		}
	}
	
	private static void addTag(Unit u,String k,String v, boolean no_doubles) {
		boolean add_ok = true;
		
		if (no_doubles){
			String uv = u.getTag(k);
			if (uv != null && uv.equalsIgnoreCase(v)){
				add_ok = false;
			}
		}
		if (add_ok){
			u.putTag(k,v);
		}
	}
	
	public static void AllTags2File(GameData gd){
		// aus gefundenen Tags orders machen....
		// die ejcTaggable ein wenig besser lesbar machen
		// alle anderen aber unterstützen...
		try {
			FileWriter fileWriter = new FileWriter("tags.tags",false);
			
			// normale units durchlaufen
			for (Unit u:gd.getUnits()) {
				FFToolsTags.Tags2FileUnit(u,fileWriter);
			}
			
			fileWriter.flush();
			fileWriter.close();
			new MsgBox(null ,"OK","OK",true);
		} catch (IOException e) {
			// pech
			new MsgBox(null,"Fehler:" + e.toString(),"Fehler",true);
		}
	}
	
	/*
	 * liest aus tags.tags die tags und setzt als orders
	 */
	public static void File2Orders(GameData gd){
		try {
			BufferedReader in = new BufferedReader(new FileReader("tags.tags"));
			String zeile = null;
			
			outText.setFile("file2orders_log");
			
			while ((zeile = in.readLine()) != null) {
				// System.out.println("Gelesene Zeile: " + zeile);
				// Aufsplitten
				outText.writeToLog("Lese: " + zeile);
				String [] vx = null;
				vx = zeile.split(";");
				Unit u = gd.getUnit(UnitID.createUnitID(vx[0],gd.base,gd.base));
				if (u!=null){
					// u.addOrder("// setTag " + vx[1] + " " + vx[2], false, 2);
					FFToolsTags.addOrder(u,"// setTag " + vx[1] + " " + vx[2], true);
					outText.writeToLog("order ergänzt.");
				} else {
					outText.writeToLog("order nicht ergänzt: (nicht gefunden) : " + vx[0]);
				}
			}
			in.close();
			outText.closeOut();
			outText.setFileStandard();
			
			
		} catch (IOException e) {
			new MsgBox(null,"Fehler:" + e.toString(),"Fehler",true);
			e.printStackTrace();
		}

	}
	
	
}
