package com.fftools.demo.actions.unit;

import java.util.ArrayList;
import java.util.Iterator;

import magellan.client.Client;
import magellan.library.Item;
import magellan.library.Order;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.event.GameDataEvent;
import magellan.library.rules.ItemType;
import magellan.library.rules.Race;

import com.fftools.ScriptUnit;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;

/**
 * Setzt das Rekrutieren um
 * @author Fiete
 *
 */
public class ClientRekrutieren implements Runnable{

	private int Anzahl = 0;
	private SelectionObserver selectionObserver = null;
	
	private Unit depotUnit = null;
	
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
			System.out.print("Rekrutieren nicht möglich (1).\n");
			System.out.flush();
			new MsgBox(this.selectionObserver.getClient(),"Rekrutieren nicht möglich","Fehler",false);
			return;
		}
		
		ArrayList<Object> list = this.selectionObserver.getObjectsOfClass(Unit.class);
		if (list==null || list.size()==0){
			System.out.print("Rekrutieren nicht möglich (2).\n");
			System.out.flush();
			new MsgBox(this.selectionObserver.getClient(),"Rekrutieren nicht möglich","Fehler",false);
			return;
		}
		
		// OK..wir haben units.
		for (Iterator<Object> iter = list.iterator();iter.hasNext();){
			Object o = iter.next();
			if (o instanceof Unit) {
				Unit u = (Unit) o;
				
				// check verfügbare Rekruten
				if (u.getRegion().modifiedRecruit()< this.Anzahl){
					// nicht mehr genügend frei!

					String s = "Für " + u.getName() + " sind nicht mehr genug Rekruten verfügbar. Abbruch.";
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
	 * setzt das rekrutieren tatsächlich um
	 * @param u die unit, die rekrutieren soll
	 * @param Anzahl die anzahl personenm die rekrutiert werden können
	 */
	private void processRekrutieren(Unit u, int Anzahl){
		// in der region sind genügend verfügbare Rekruten ... bereits gecheckt
		// Vorgehen: 
		// Silberübergaben an das Depot suchen und umbiegen
		// Silberbestand (unmodified) des Depots nutzen)
		// Rekrutierbefehl setzen und für nur diese Runde validieren
		
		// Silberbedarf checken
		Race race = u.getRace();
		if (race==null){
			return;
		}
		int silber = race.getRecruitmentCosts();
		if (silber==0){
			String s = "Für die Rasse von " + u.toString(true) + " (" + race.getName() + ") sind keine Rekrutierungskosten bekannt!";
			new MsgBox(this.selectionObserver.getClient(),s,"Fehler",false);
			return;
		}
		// Anzahl der Leute....
		silber = silber * Anzahl;
		// int neededSilber = silber;
		
		// depot finden....es sollte nur eines geben...
		if (this.depotUnit==null || !this.depotUnit.getRegion().equals(u.getRegion())){
			this.depotUnit = this.getDepot(u.getRegion());
		}
		if (this.depotUnit==null){
			String s = "Kein Depot gefunden in " + u.getRegion().toString() + ".\n Derzeit werden nur Regionen unterstützt, in denen Depots existieren.";
			new MsgBox(this.selectionObserver.getClient(),s,"Fehler",false);
			return;
		}
		
		// wir haben ein Depot
		// units durchgehen und Silberübergaben an das Depot finden
		
		for (Iterator<Unit> iter = u.getRegion().units().iterator();iter.hasNext();){
			Unit checkUnit = (Unit)iter.next();
			silber -= this.processUnit(checkUnit, u, silber);
			if (silber<=0){
				break;
			}
		}
		
		// beim depot selbst angreifen
		if (silber>0 && this.depotUnit!=null){
			silber -= this.processDepot(u,silber);
		}
		
		if (silber>0){
			// nicht alle Kosten auftreibbar
			String s = "Für " + u.toString(true) + " in " + u.getRegion().toString() + " fehlen " + silber + " Silber!";
			new MsgBox(this.selectionObserver.getClient(),s,"Problem",false);
		}
		
		// bei empfängereinheit request und rekrutieren setzen setzen
		int actRunde = this.selectionObserver.getClient().getData().getDate().getDate();
		u.addOrder("REKRUTIEREN " + Anzahl + " ; [manuell " + actRunde + "]");
		u.addOrder("// script RUNDE " + actRunde + " script REKRUTIEREN " + Anzahl);
		u.reparseOrders();
	}
	
	/**
	 * versucht, vom Depot noch <b>silber</b> abzuzweigen
	 * @param silber
	 * @return
	 */
	private int processDepot(Unit u,int silber){
		int erg = 0;
		ItemType silverType = this.selectionObserver.getClient().getData().rules.getItemType("Silber");
		Item silberItem = this.depotUnit.getItem(silverType);
		int vorhanden = 0;
		if (silberItem!=null){
			vorhanden = silberItem.getAmount();
		}
		if (vorhanden<=0 || silber <=0){
			return 0;
		}
		int betrag = Math.min(vorhanden,silber);
		// beim Depot die Order generieren, wenn nicht gleich u
		if (!this.depotUnit.equals(u)){
			this.depotUnit.addOrder("GIB " + u.toString(false) + " " + betrag + " Silber ; Rekrutierunskosten [manuell]");
		}
		erg += betrag;
		return erg;
	}
	
	
	/**
	 * untersucht geberunit auf silberübergaben an das depot und biegt die auf den nehmer um
	 * @param geber
	 * @param nehmer
	 * @param silber
	 */
	private int processUnit(Unit geber,Unit nehmer,int silber ){
		String searchString = "GIB " + this.depotUnit.toString(false);
		searchString = searchString.toLowerCase();
		ArrayList<Order> newOrders = new ArrayList<Order>(1);
		int erg = 0;
		if (silber<=0){
			return 0;
		}
		
		boolean needNewOrders = false;
		for (Iterator<Order> iter = geber.getOrders2().iterator();iter.hasNext();){
			Order o = (Order)iter.next();
			String order = o.getText();
			String returnOrder = order;
			order = order.toLowerCase();
			if (order.startsWith(searchString) && order.indexOf("silber")>0
					&& !(order.indexOf("do_not_confirm")>0)
						&& !(order.indexOf(";dnt")>0)
							&& silber>0){
				// hier weiter...wir haben eine silberübergabe gefunden..
				needNewOrders = true;
				
				// Anzahl Silber herausfinden..wir ignorieren ALLES (vorerst)
				String[] token = order.split(" ");
				// gib nummer x silber oder gib temp nummer x silber
				// immer direkt vor Silber....finden wir also Silber
				int anzahlPos =0;
				for (int i = 0;i<token.length;i++){
					if (token[i].equalsIgnoreCase("Silber")){
						anzahlPos = i-1;
						break;
					}
				}
				if (anzahlPos<=0){
					String s = "Fehler beim Parsen der Silberübergabe bei " + geber.toString(true);
					new MsgBox(this.selectionObserver.getClient(),s,"Fehler",false);
				}
				int Anzahl = Integer.parseInt(token[anzahlPos]);
				if (Anzahl<=0){
					String s = "Fehler beim Parsen der Silberübergabe bei " + geber.toString(true);
					new MsgBox(this.selectionObserver.getClient(),s,"Fehler",false);
				}
				// tatsächliche umzubiegende Menge feststellen
				// 2 Orders: die alte, geändert falls noch mit inhalt
				// die neue
				int betrag = Math.min(Anzahl, silber);
			
				
				// die neue order  nur wenn empfänger <> geber
				String newS = "";
				if (!geber.equals(nehmer)){
					newS = "GIB " + nehmer.toString(false) + " " + betrag + " SILBER ;Rekrutieren [manuell]";
					newOrders.add(geber.createOrder(newS));
				} else {
					// die gleiche unit...nix machen
				}
				// die alte Order...
				Anzahl -= betrag;
				if (Anzahl>0){
					// alte Order verändert einfügen
					newS = "";
					for (int i = 0;i<token.length;i++){
						if (i!=anzahlPos){
							newS = newS.concat(token[i] + " ");
						} else {
							newS = newS.concat(Anzahl + " ");
						}
					}
					newOrders.add(geber.createOrder(newS));
				}
				
				// silber reduzieren
				silber -= betrag;
				erg+=betrag;
				
			} else {
				newOrders.add(geber.createOrder(returnOrder));
			}
		}
		
		if (needNewOrders){
			geber.setOrders2(newOrders);
			geber.reparseOrders();
		}
		return erg;
	}
	
	/** 
	 * liefert das (erste) Depot der Region...
	 * @param r
	 * @return
	 */
	private Unit getDepot(Region r){
		for (Iterator<Unit> iter = r.units().iterator();iter.hasNext();){
			Unit u = (Unit)iter.next();
			if (ScriptUnit.isDepot(u)){
				return u;
			}
		}
		return null;
	}
	
}
