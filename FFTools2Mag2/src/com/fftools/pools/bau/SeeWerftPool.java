package com.fftools.pools.bau;

import java.util.ArrayList;
import java.util.Collections;

import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.scripts.Seewerft;

import magellan.library.Ship;
import magellan.library.Unit;

public class SeeWerftPool{ 
	
public static final ReportSettings reportSettings = ReportSettings.getInstance();

private SeeWerftManager_SWM myWerftManager = null;

private ArrayList<Seewerft> WerftListe= new ArrayList<Seewerft>();
private ArrayList<Ship2Repair> SchiffListe = new ArrayList<Ship2Repair>();


/**
	 * Konstruktor 
	 *
	 */
	
	public SeeWerftPool(SeeWerftManager_SWM _wm){
		myWerftManager=_wm;	   
    }
    

	
	/**
	 * 
	 */
	
	public void runPool(){
		// nach dem MP...jetzt wissen die WerftScripte, wieviel Holz sie bekommen
		
		// 
		/*  auch bei keinen Schiffen auf Neubau prüfen
		if (this.SchiffListe.size()==0){
			return;
		}
		*/
		if (this.WerftListe.size()==0){
			return;
		}
		
		
		// Schiffe Sortieren
		if (this.SchiffListe.size()>0){
			Collections.sort(this.SchiffListe);
		}
		
		// Werft über Reihenfolge informieren
		this.informScriptsSchiffListe();
				
		SeeWerftScriptComparator werftScriptComp = new SeeWerftScriptComparator(0);
		
		// Werften sortieren
		Collections.sort(this.WerftListe, werftScriptComp);
		
		// Werften informieren
		this.informScriptsScriptListe();

		// die Werften entfernen, die nicht bauen können
		ArrayList<Seewerft> ActiveWerftList = new ArrayList<Seewerft>(0);
		for (Seewerft w:this.WerftListe){
			if (w.getBauPunkteMitHolz()>0){
				ActiveWerftList.add(w);
			}
		}
		

		// Schiffe der Reihe nach durchgehen und abarbeiten
		// dazu worklIst = Liste der schon fertigen Werften
		ArrayList<Seewerft> workList = new ArrayList<Seewerft>(0);
		if (this.SchiffListe.size()>0){
			for (Ship2Repair s:this.SchiffListe){
				int RepairPunkte = neededBaupunkte(s.s);
				ScriptUnit su = s.captn;
				int iterCounter=0;
				int oldRepairPunkte = 0;
				while (RepairPunkte>0 && ActiveWerftList.size()>0 && iterCounter<10000){
					werftScriptComp.setBauPunkte(RepairPunkte);
					Collections.sort(this.WerftListe,werftScriptComp);
					iterCounter=iterCounter+1;
					for (Seewerft w:ActiveWerftList){
						// outText.addOutLine("Werft: bearbeite Werft " + w.scriptUnit.toString(), true);
						if (!workList.contains(w) && RepairPunkte>0 && w.getBauPunkteMitHolz(s.s.getShipType())>0){
							// outText.addOutLine("Werft: beauftrage Werft " + w.scriptUnit.toString(), true);
							// ok..bauen lassen
							oldRepairPunkte = RepairPunkte;
							RepairPunkte = RepairPunkte - w.getBauPunkteMitHolz(s.s.getShipType());
							w.addOrder("machen schiff " + s.s.getID() + "; Werft-Script: verbaue " + w.getBauPunkteMitHolz(s.s.getShipType()) + ", verbleibend " + RepairPunkte,true);
							workList.add(w);
							if (su!=null){
								su.addComment("An Schiff wird diese Runde gebaut: " + w.getBauPunkteMitHolz(s.s.getShipType()) + " von " + w.scriptUnit.toString() + "[" + oldRepairPunkte + "->" + RepairPunkte + "]");
							}
							ActiveWerftList.remove(w);
							break;
						}
					}
				}
				if (RepairPunkte<0){
					RepairPunkte = 0;
				}
				if (su!=null){
					su.addComment("Werftinfo: verbleibend " + RepairPunkte + " Schaden.");
					s.repairsNeeded = RepairPunkte;
				}
				if (iterCounter>=10000 && su!=null){
					su.doNotConfirmOrders("!!! Werftmanager versagt! Erreichte 10000 Iterationen!!!");
				}
				
			}
		}
		// nicht mit aufträgen versorgte Werften lernen lassen
		for (Seewerft w:this.WerftListe){
			if (!workList.contains(w)){
				w.orderLernen();
			} 
		}
    }

   
	public void addWerft(Seewerft _w){
		if (!this.WerftListe.contains(_w)){
			this.WerftListe.add(_w);
		}
	}



	private void informScriptsSchiffListe(){
		for (Seewerft w:this.WerftListe){
			int counter=0;
			w.addComment("Abarbeitungsliste der " + this.SchiffListe.size() + " Schiffe (Reparatur):");
			for (Ship2Repair s:this.SchiffListe){
				counter = counter+ 1;
				w.addComment(counter + ": " + s.s.toString(true) + "[" + neededBaupunkte(s.s) + "]" );
			}
			
		}
	}
	
	
	private void informScriptsScriptListe(){
		for (Seewerft w:this.WerftListe){
			int counter=0;
			w.addComment("Abarbeitungsliste der Werfteinheiten:");
			for (Seewerft w2:this.WerftListe){
				counter = counter+ 1;
				w.addComment(counter + ": " + w2.scriptUnit.toString()  + " [" + w2.getBauPunkteMitHolz() + "]");
			}
		}
	}
	
	private int neededBaupunkte(Ship s){
		int erg=0;
		
		if (s.getDamageRatio()>0){
			double actDamage = s.getDamageRatio();
			double normalSize = (s.getShipType().getMaxSize() * s.getAmount());
			erg = (int) (Math.ceil(normalSize * (actDamage/100)));
		}
		
		if (erg==0){
			erg = (s.getShipType().getMaxSize() * s.getAmount()) - s.getSize();
		}
		
		
		return erg;
	}
	
	/*
	 * direkt ein Schiff hinzufügen, vorher testen, ob nicht schon vorhanden
	 * wird nur hinzugefügt, wenn beschädigt, und kapitän ist scriptunit
	 */
	public void addShip (Ship s) {
		for (Ship2Repair sh:this.SchiffListe) {
			if (sh.s.equals(s)) {
				return;
			}
		}
		// noch nicht vorhanden
		int neededBP = neededBaupunkte(s);
		if (neededBP>0) {
			Unit u = s.getModifiedOwnerUnit();
			if (u!=null) {
				ScriptUnit su = this.myWerftManager.getOverlord().getScriptMain().getScriptUnit(u);
				if (su!=null) {
					Ship2Repair pS = new Ship2Repair();
					pS.captn = su;
					pS.nextShipStop = null;
					pS.s = s;
					pS.repairsNeeded = neededBP;
					this.SchiffListe.add(pS);
					su.addComment("Schiff als zu reparieren dem SeeWeftPool der Region hinzugefügt");
				}
			}
		}
	}
	
	
	public void addShip2Repair(Ship2Repair pS) {
		for (Ship2Repair sh:this.SchiffListe) {
			if (sh.equals(pS)) {
				return;
			}
		}
		this.SchiffListe.add(pS);
		pS.captn.addComment("SWP: Schiff dem SeewerftPool der Region hinzugefügt");
	}
}// ende class
