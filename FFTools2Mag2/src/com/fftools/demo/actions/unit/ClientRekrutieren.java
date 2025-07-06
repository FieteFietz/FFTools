package com.fftools.demo.actions.unit;

import java.util.ArrayList;
import java.util.Iterator;

import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;

import magellan.client.Client;
import magellan.library.Unit;
import magellan.library.event.GameDataEvent;
import magellan.library.rules.Race;

/**
 * Setzt das Rekrutieren um
 * @author Fiete
 *
 */
public class ClientRekrutieren implements Runnable{

	private int Anzahl = 0;
	private SelectionObserver selectionObserver = null;
	
	
	public ClientRekrutieren(SelectionObserver selectionObserver, int Anzahl) {
		super();
		this.selectionObserver = selectionObserver;
		this.Anzahl = Anzahl;
	}
	
	/**
	 * startet das rekrutieren als thread
	 *
	 */
	public void run(){
		/**
		System.out.print("Running Rekrutieren " + this.Anzahl + "\n");
		System.out.flush();
		*/
		if (this.Anzahl==0 || this.selectionObserver.getClient()== null || this.selectionObserver.getSelectedObjects()==null){
			System.out.print("Rekrutieren nicht m�glich (1).\n");
			System.out.flush();
			new MsgBox(this.selectionObserver.getClient(),"Rekrutieren nicht m�glich","Fehler",false);
			return;
		}
		
		ArrayList<Object> list = this.selectionObserver.getObjectsOfClass(Unit.class);
		if (list==null || list.size()==0){
			System.out.print("Rekrutieren nicht m�glich (2).\n");
			System.out.flush();
			new MsgBox(this.selectionObserver.getClient(),"Rekrutieren nicht m�glich","Fehler",false);
			return;
		}
		
		// OK..wir haben units.
		for (Iterator<Object> iter = list.iterator();iter.hasNext();){
			Object o = iter.next();
			if (o instanceof Unit) {
				Unit u = (Unit) o;
				
				// check verf�gbare Rekruten
				if (u.getRegion().modifiedRecruit()< this.Anzahl){
					// nicht mehr gen�gend frei!

					String s = "F�r " + u.getName() + " sind nicht mehr genug Rekruten verf�gbar. Abbruch.";
					new MsgBox(this.selectionObserver.getClient(),s,"Fehler",false);
					return;
				}
				this.processRekrutieren(u, this.Anzahl);
			} else {
				System.out.print("unerwartete Klasse in " + this.getClass().getName() + ": " + o.getClass().getName());
				System.out.flush();
			}
		}
		
		// Client refreshen
		Client client = this.selectionObserver.getClient();
		client.getMagellanContext().getEventDispatcher().fire(new GameDataEvent(this, client.getData()));
		
		
		/**
		System.out.print("Finished Rekrutieren " + this.Anzahl + "\n");
		System.out.flush();
		*/

	}
	
	/**
	 * setzt das rekrutieren tats�chlich um
	 * @param u die unit, die rekrutieren soll
	 * @param Anzahl die anzahl personenm die rekrutiert werden k�nnen
	 */
	private void processRekrutieren(Unit u, int Anzahl){
		// in der region sind gen�gend verf�gbare Rekruten ... bereits gecheckt
		// Vorgehen: 
		// Silber�bergaben an das Depot suchen und umbiegen
		// Silberbestand (unmodified) des Depots nutzen)
		// Rekrutierbefehl setzen und f�r nur diese Runde validieren
		
		// Silberbedarf checken
		Race race = u.getRace();
		if (race==null){
			return;
		}
		int silber = race.getRecruitmentCosts();
		if (silber==0){
			String s = "F�r die Rasse von " + u.toString(true) + " (" + race.getName() + ") sind keine Rekrutierungskosten bekannt!";
			new MsgBox(this.selectionObserver.getClient(),s,"Fehler",false);
			return;
		}

		// bei empf�ngereinheit request und rekrutieren setzen setzen
		int actRunde = this.selectionObserver.getClient().getData().getDate().getDate();
		u.addOrder("REKRUTIEREN " + Anzahl + " ; [manuell " + actRunde + "]");
		u.addOrder("// script RUNDE " + actRunde + " script REKRUTIEREN " + Anzahl);
		u.reparseOrders();
	}

}
