package com.fftools.transport;


/**
 * Wer hat Wo Was Wieviel...und was ist noch zu haben...
 * 
 * @author Fiete
 *
 */
public class TransportStatistikEintrag implements Comparable<TransportStatistikEintrag> {
	/**
	 * Original gefordert
	 */
	private String gegenstand = "";
	
	/**
	 * Summe der offenen Forderungen
	 */
	private int anzahl_offen = 0;
	
	public String getGegenstand() {
		return gegenstand;
	}

	public int getAnzahl_offen() {
		return anzahl_offen;
	}

	public int getAnzahl_bearbeitet() {
		return anzahl_bearbeitet;
	}


	/**
	 * wieviel davon bereits bearbeitet....
	 */
	private int anzahl_bearbeitet = 0;
	
	
	
	public TransportStatistikEintrag(String _name){
		this.gegenstand = _name;
		this.anzahl_bearbeitet=0;
		this.anzahl_offen=0;
	}
	
	public void add(int Anzahl_bearbeitet, int Anzahl_offen){
		this.anzahl_bearbeitet+=Anzahl_bearbeitet;
		this.anzahl_offen += Anzahl_offen;
	}

	public int compareTo(TransportStatistikEintrag o){
		return o.anzahl_offen - this.anzahl_offen;
	}

	/**
	 * @return the scriptUnit
	 */
	
	
}
