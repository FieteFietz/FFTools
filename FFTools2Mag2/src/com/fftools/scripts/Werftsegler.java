package com.fftools.scripts;

import com.fftools.utils.FFToolsOptionParser;

import magellan.library.Ship;
import magellan.library.rules.ShipType;

public class Werftsegler extends Script{
	
	
	private static final int Durchlauf = 212;  // nach Werft (210) ... | vor Werftmanager
	
	public ShipType shipType = null;
	
	private int maxSchiffe = 0;
	private int actSchiffe_nach_GIB = 0;
	private int actSchiffe_bevor_GIB = 0;
	
	private Werftsegler GIB_AN_Werftsegler=null;
	public boolean hasBetreteOrder = false;
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Werftsegler() {
		super.setRunAt(Durchlauf);
	}
	
	
	/**
	 * Eigentliche Prozedur
	 * runScript von Script.java MUSS/SOLLTE ueberschrieben werden
	 * Durchlauf kommt von ScriptMain
	 * 
	 * in Script steckt die ScriptUnit
	 * in der ScriptUnit steckt die Unit
	 * mit addOutLine jederzeit Textausgabe ans Fenster
	 * mit addComment Kommentare...siehe Script.java
	 */
	
	public void runScript(int scriptDurchlauf){
		
		if (scriptDurchlauf==Durchlauf){
			this.scriptStart();
		}
	}
	
	private void scriptStart(){
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit);
		OP.addOptionList(this.getArguments());
		int setMaxSchiffe = OP.getOptionInt("Schiffe", 0);
		if (setMaxSchiffe>1) {
			this.maxSchiffe = setMaxSchiffe;
		}
		if (setMaxSchiffe==1) {
			this.addComment("!!! Schiffe=1 ist kein sinnvoller Befehl !!");
		}
		
		if (this.getUnit().getShip()!=null) {
			// wir sind gerade auf einem Schiff
			Ship actShip = this.getUnit().getShip();
			this.actSchiffe_bevor_GIB = actShip.getAmount();
			this.actSchiffe_nach_GIB = actShip.getAmount();
			if (actShip.getAmount()==1 && this.maxSchiffe==0 & actShip.getOwner().equals(this.getUnit())) {
				ShipType ST = actShip.getShipType();
				if (actShip.getSize()==ST.getMaxSize()) {
					// wir sind auf einem (fertigen) Schiff Anzahl 1 und wir sind der Kapitän, wir sind kein Flottenkapitän
					this.addComment("Werftsegler: ich  bin Kapitän auf einem neuen Schiff, suche Flottensammelstelle");
					// beim Werftpoolmanager ergänzen
					this.getOverlord().getWerftManager().addWerftseglerUnit(this, false);
				} else {
					// Schiff noch nicht fertig
					this.addComment("Werftsegler: ich  bin Kapitän auf einem neuen Schiff, doch das Schiff ist noch nicht fertig, ich bleibe");
				}
			}
			
			if (actShip.getAmount()>=1 && this.maxSchiffe>0 & actShip.getOwner().equals(this.getUnit())) {
				// wir sind auf einem Schiff Größe 1 und wir sind der Kapitän, und wir sind Flottenkapitän, wir sammeln schiffe
				this.addComment("Werftsegler: ich  bin (Flotten)Kapitän auf einem neuen Schiff, ich bin eine Sammelstelle für max " + this.maxSchiffe + " Schiffe");
				// beim Werftpoolmanager ergänzen
				this.getOverlord().getWerftManager().addWerftseglerUnit(this, true);
			}
			
			if (actShip.getAmount()>=1 && this.maxSchiffe==0 & !actShip.getOwner().equals(this.getUnit())) {
				// wir sind auf einer Flotte und wir sind nict der Kapitän, und wir sind nicht Flottenkapitän, wir betreten, oder zumindest Verlassen
				this.addComment("Werftsegler: ich  bin auf einer Flotte, bereit, ein fertiges Schiff zu betreten, zumindest aber dieses zu verlassen.");
				// beim Werftpoolmanager ergänzen
				this.getOverlord().getWerftManager().addWerftseglerUnit(this, false);
			}
			
		} else {
			// wir sind gerade nicht auf einem Schiff, ich könnte mir eines Suchen, welches frei wird
			this.addComment("Werftsegler: ich bin bereit, Schiffe zu betreten, die von der Werft verlassen werden");
			// beim Werftpoolmanager ergänzen
			this.getOverlord().getWerftManager().addWerftseglerUnit(this, false);
		}
	}
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}


	public int getMaxSchiffe() {
		return maxSchiffe;
	}


	public int getActSchiffe_bevor_GIB() {
		return actSchiffe_bevor_GIB;
	}


	public Werftsegler getGIB_AN_Werftsegler() {
		return GIB_AN_Werftsegler;
	}


	public void setGIB_AN_Werftsegler(Werftsegler gIB_AN_Werftsegler) {
		GIB_AN_Werftsegler = gIB_AN_Werftsegler;
		this.addComment("Werftsegler: übergebe mein Schiff an " + gIB_AN_Werftsegler.scriptUnit.toString());
	}


	public int getActSchiffe_nach_GIB() {
		return actSchiffe_nach_GIB;
	}
	
	public void addNewShip(Werftsegler w, Ship s) {
		this.actSchiffe_nach_GIB++;
		this.addComment("Werftsegler: erhalte " + s.toString() + " von " + w.scriptUnit.toString() + ": somit " + this.actSchiffe_nach_GIB + " / " + this.maxSchiffe);
		
	}


	


	

	
	
}
