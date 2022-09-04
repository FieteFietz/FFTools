package com.fftools.pools.bau;

import java.util.ArrayList;
import java.util.Collections;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.scripts.Lernfix;
import com.fftools.scripts.Werft;
import com.fftools.scripts.Werftsegler;
import com.fftools.utils.FFToolsUnits;

import magellan.library.Region;
import magellan.library.Ship;
import magellan.library.Unit;

public class WerftPool{ 
	
private static final OutTextClass outText = OutTextClass.getInstance();
public static final ReportSettings reportSettings = ReportSettings.getInstance();

private Region region=null;
private WerftManager myWerftManager = null;

private ArrayList<Werft> WerftListe= new ArrayList<Werft>();
private ArrayList<Ship> SchiffListe = new ArrayList<Ship>();


private ArrayList<Werftsegler> Flottenkapitäne = new ArrayList<Werftsegler>(); // sammeln schiffe
private ArrayList<Werftsegler> Hafenkapitäne = new ArrayList<Werftsegler>(); // besetzen neue Schiffe, übergeben an Flottenkapitäne
private ArrayList<Ship> ListeFreiWerdendeSchiffe = new ArrayList<Ship>();

private boolean allShips = false;

private int Durchlauf=0;



/**
	 * Konstruktor 
	 *
	 */
	
	public WerftPool(WerftManager _wm, Region _region){
		myWerftManager=_wm;
		region =_region;	   
        //  MatPool holen...
  		// this.matPool = this.myWerftManager.scriptMain.getOverlord().getMatPoolManager().getRegionsMatPool(this.region);
   	    
    }
    
	

    /**
     * Hier rennt der Pool. 
     * und zwar zunächst der anforderungslauf, dann der nachlauf
     */
	
	public void runPool(int Durchlauf){
		this.Durchlauf = Durchlauf;
		switch (Durchlauf){
			case WerftManager.Durchlauf0:
				this.runPoolLauf1();
				break;
			case WerftManager.Durchlauf1:
				this.runPoolLauf2();
				break;
			case WerftManager.Durchlauf0_2:
				this.GibFertigeSchiffeAnFlottenkapitän();
				this.betreteFreiWerdendeSchiffe();
				this.DoNotConfirmFullFleets();
				break;
			case WerftManager.Durchlauf2:
				this.setGIBOrders();
				break;
		}
	}
	
	
	/**
	 * 
	 */
	
	
	private void runPoolLauf1(){
		
		// das ist vor dem MP, was sollen wir hier machen?
		// Schiffsliste bauen...
		this.builtSchiffListe();
		if (this.SchiffListe.size()==0){
			this.informScriptsNoShips();
			return;
		}
	   
	}
	

	
	/**
	 * 
	 */
	
	private void runPoolLauf2(){
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
			Collections.sort(this.SchiffListe, new WerftSchiffComparator());
		}
		
		// Werft über Reihenfolge informieren
		this.informScriptsSchiffListe();
		
		WerftScriptComparator werftScriptComp = new WerftScriptComparator(0);
		
		
		// Werften sortieren
		Collections.sort(this.WerftListe, werftScriptComp);
		
		// Werften informieren
		this.informScriptsScriptListe();
		
		// die Werften entfernen, die nicht bauen können
		ArrayList<Werft> ActiveWerftList = new ArrayList<Werft>(0);
		for (Werft w:this.WerftListe){
			if (w.getBauPunkteMitHolz()>0){
				ActiveWerftList.add(w);
			}
		}
		

		// Schiffe der Reihe nach durchgehen und abarbeiten
		// dazu worklIst = Liste der schon fertigen Werften
		ArrayList<Werft> workList = new ArrayList<Werft>(0);
		if (this.SchiffListe.size()>0){
			for (Ship s:this.SchiffListe){
				
				int RepairPunkte = neededBaupunkte(s);

				// outText.addOutLine("Werft: bearbeite Schiff " + s.toString() + ", RP: " + RepairPunkte, true);
				Unit u = s.getOwnerUnit();
				if (u!=null){
					// scriptMain.getScriptUnits().containsKey(u)
					
					ScriptUnit su;
					if (this.myWerftManager.scriptMain.getScriptUnits().containsKey(u)){
						su = this.myWerftManager.scriptMain.getScriptUnit(u);
					} else {
						su = null;
					}
					int iterCounter=0;
					int oldRepairPunkte = 0;
					while (RepairPunkte>0 && ActiveWerftList.size()>0 && iterCounter<10000){
						werftScriptComp.setBauPunkte(RepairPunkte);
						Collections.sort(this.WerftListe,werftScriptComp);
						iterCounter=iterCounter+1;
						for (Werft w:ActiveWerftList){
							// outText.addOutLine("Werft: bearbeite Werft " + w.scriptUnit.toString(), true);
							if (!workList.contains(w) && RepairPunkte>0 && w.getBauPunkteMitHolz(s.getShipType())>0){
								// outText.addOutLine("Werft: beauftrage Werft " + w.scriptUnit.toString(), true);
								// ok..bauen lassen
								oldRepairPunkte = RepairPunkte;
								RepairPunkte = RepairPunkte - w.getBauPunkteMitHolz(s.getShipType());
								w.addOrder("machen schiff " + s.getID() + "; Werft-Script: verbaue " + w.getBauPunkteMitHolz(s.getShipType()) + ", verbleibend " + RepairPunkte,true);
								FFToolsUnits.leaveAcademy(w.scriptUnit, " Werft arbeitet und verlässt Aka");
								workList.add(w);
								if (su!=null){
									su.addComment("An Schiff wird diese Runde gebaut: " + w.getBauPunkteMitHolz(s.getShipType()) + " von " + w.scriptUnit.toString() + "[" + oldRepairPunkte + "->" + RepairPunkte + "]");
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
					}
					if (iterCounter>=10000 && su!=null){
						su.doNotConfirmOrders("!!! Werftmanager versagt! Erreichte 10000 Iterationen!!!");
					}
				} else {
					outText.addOutLine("!!! Schiff in Werftregion unbemannt!!!: " + s.toString() + " in " + s.getRegion().toString(), true);
				}
			}
		}
		// nicht mit aufträgen versorgte Werften lernen lassen
		for (Werft w:this.WerftListe){
			if (!workList.contains(w)){
				if (w.getBauPunkteMitHolz()==0){
					w.addComment("Ich konnte diese Runde nicht bauen!!!");
					if (w.hasLernfixOrder){
						w.addComment("Werft: Lernauftrag bereits mit Lernfix erteilt.");
					} else {
						w.addOrder("Lerne " + w.LernTalent + " ;Werft ", true);
					}
				} else {
					if (w.hasNeubauOrder()){
						w.addComment("Keine Reparatur- oder Weiterbauaufträge, Neubau wird begonnen.");
						w.addOrder("mache " + w.shipType.getName() + " ; Werft-Neubau",false);
						FFToolsUnits.leaveAcademy(w.scriptUnit, " Werft arbeitet und verlässt Aka");
					} else {
						w.addComment("Leider keine Arbeit für mich diese Runde...(Werft)");
						if (w.hasLernfixOrder){
							w.addComment("Werft: Lernauftrag bereits mit Lernfix erteilt.");
						} else {
							w.addOrder("Lerne " + w.LernTalent + " ;Werft ", true);
						}
					}
				}
				
			} else {
				w.addComment("Werft: kein Neubaucheck, da als Reparateur eingestuft.");
			}
		}
    }

   
	public void addWerft(Werft _w){
		if (!this.WerftListe.contains(_w)){
			this.WerftListe.add(_w);
		}
	}
	
	
	public void addWerftseglerAsFlottenkapitän(Werftsegler _w){
		if (!this.Flottenkapitäne.contains(_w)){
			this.Flottenkapitäne.add(_w);
		}
	}
	
	public void addWerftseglerAsHafenkapitän(Werftsegler _w){
		if (!this.Hafenkapitäne.contains(_w)){
			this.Hafenkapitäne.add(_w);
		}
	}
	
	
	public void addFreiWerdendesSchiff(Ship _w){
		if (!this.ListeFreiWerdendeSchiffe.contains(_w)){
			this.ListeFreiWerdendeSchiffe.add(_w);
		}
	}
	
	
	
	public void builtSchiffListe(){
		this.SchiffListe.clear();
		for (Ship s:this.region.ships()){
			boolean needRepair = true;
			// ist es beschädigt ?
			if (s.getDamageRatio()==0){
				needRepair = false;
			}
			
			// ist es unter dem Kommando einer scriptunit
			if (needRepair){
				Unit u = s.getOwnerUnit();
				if (u!=null){
					if (!this.myWerftManager.scriptMain.getScriptUnits().containsKey(u) && !this.allShips){
						// keine scriptunit als Kapitän
						needRepair=false;
					}
				} else {
					// unbesetztes Schiff ?!  ToDo: Warnung an die Werft ausgeben?? 
					needRepair=false;
				}
			}
			
			boolean isWeiterbau = false;
			
			if (!needRepair){
				isWeiterbau=true;
				if (s.getSize()==(s.getShipType().getMaxSize() * s.getAmount())){
					isWeiterbau=false;
				}
			}
			
			if (needRepair || isWeiterbau){
				this.SchiffListe.add(s);
			}
		}
	}
	
	
	private void informScriptsNoShips(){
		for (Werft w:this.WerftListe){
			if (w.hasNeubauOrder()){
				w.addComment("Werft: keine Teilnahme am Lernpool, obwohl keine Schiffe in Region, da Neubauauftrag vorhanden.");
			} else {
				w.addComment("keine Schiffe in der Region zu reparieren..Lerne " + w.LernTalent);
				// w.scriptUnit.findScriptClass("Lernfix", "Talent=" + w.LernTalent);
				Lernfix LF = new Lernfix();
				LF.setScriptUnit(w.scriptUnit);
				if (w.c!=null){
					LF.setClient(w.c);
				}
				LF.setGameData(w.gd_Script);
				ArrayList<String> ll = new ArrayList<String>();
				ll.add("Talent=Schiffbau");
				LF.setArguments(ll);
				LF.scriptStart();
				w.scriptUnit.addAScript(LF);
				w.hasLernfixOrder=true;
			}
		}
	}
	
	private void informScriptsSchiffListe(){
		for (Werft w:this.WerftListe){
			if (w.showInfos){
				int counter=0;
				w.addComment("Abarbeitungsliste der " + this.SchiffListe.size() + " Schiffe (Reparatur):");
				for (Ship s:this.SchiffListe){
					counter = counter+ 1;
					w.addComment(counter + ": " + s.toString(true) + "[" + neededBaupunkte(s) + "]" );
				}
			}
		}
	}
	
	private void informScriptsScriptListe(){
		for (Werft w:this.WerftListe){
			if (w.showInfos){
				int counter=0;
				w.addComment("Abarbeitungsliste der Werfteinheiten:");
				for (Werft w2:this.WerftListe){
					counter = counter+ 1;
					w.addComment(counter + ": " + w2.scriptUnit.toString()  + " [" + w2.getBauPunkteMitHolz() + "]");
				}
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



	public boolean isAllShips() {
		return allShips;
	}



	public void setAllShips(boolean allShips) {
		this.allShips = allShips;
	}
	
	
	
	/*** Bereich Werftsegler ****/
	
	
	private void GibFertigeSchiffeAnFlottenkapitän() {
		if (this.Flottenkapitäne.isEmpty()) {
			// Info an alle Übergabewilligen
			for (Werftsegler w: this.Hafenkapitäne) {
				if (w.getUnit().getShip()!=null && w.getMaxSchiffe()==0) {
					// Bingo
					w.doNotConfirmOrders("!!! Keine Flottensammelstelle (Werftsegler Schiffe=X) in der Region !!!");
				}
			}
			
			
			return;
		}
		
		// die am "fertigsten" zuerst
		this.Flottenkapitäne.sort(new WerftseglerComparator());
		for (Werftsegler werftsegler : Flottenkapitäne) {
			for (Werftsegler HafenK : this.Hafenkapitäne) {
				// werftsegler.addComment("Debug: prüfe " + HafenK.scriptUnit.toString(),false);
				if (HafenK.getGIB_AN_Werftsegler()==null && HafenK.getUnit().getShip()!=null && HafenK.getUnit().getShip().getAmount()==1) {
					// werftsegler.addComment("Debug: check(1)",false);
					if (werftsegler.getActSchiffe_nach_GIB()<werftsegler.getMaxSchiffe() && werftsegler.getUnit().getShip().getShipType().equals(HafenK.getUnit().getShip().getShipType())) {
						if (werftsegler.getUnit().getShip().getShoreId() == HafenK.getUnit().getShip().getShoreId()) {
						// Passt
						// werftsegler.addComment("Debug: check(2)",false);
						HafenK.setGIB_AN_Werftsegler(werftsegler);
						werftsegler.addNewShip(HafenK, HafenK.getUnit().getShip());
						// werftsegler.addComment("Debug: OK von " + HafenK.scriptUnit.toString(), false);
						} else {
							// falsche Küste
						}
					} else {
						// werftsegler.addComment("Debug: nicht geeignet (2)",false);
					}
				} else {
					// werftsegler.addComment("Debug: nicht geeignet (1)", false);
				}
			}
		}
		
		// wenn jetzt noch welche übergeben wollen, aber nicht können, diese unbestätigt lassen
		for (Werftsegler HafenK : this.Hafenkapitäne) {
			if (HafenK.getGIB_AN_Werftsegler()==null && HafenK.getUnit().getShip()!=null && HafenK.getUnit().getShip().getAmount()==1) {
				HafenK.doNotConfirmOrders("Werftsegler findet keine verfügbare Flottensammelstelle.");
			}
		}
		
	}
	
	
	
	private void betreteFreiWerdendeSchiffe() {
		if (this.ListeFreiWerdendeSchiffe.isEmpty()) {
			for (Werftsegler werftsegler : this.Hafenkapitäne) {
				if (werftsegler.getUnit().getShip()==null) {
					werftsegler.addComment("Keine Frei werdenden Schiffe in der Region verfügbar.");
				}
			}
			return;
		}
		for (Ship s : this.ListeFreiWerdendeSchiffe) {
			for (Werftsegler w: this.Hafenkapitäne) {
				if (!w.hasBetreteOrder && w.getUnit().getShip()==null) {
					// Bingo
					w.addOrder("BETRETE SCHIFF " + s.getID() + " ; Werftsegler, Schiff wird von Werft verlassen (" + this.Durchlauf + ")", true);
					w.hasBetreteOrder=true;
					break;
				}
				
				if (!w.hasBetreteOrder && w.getUnit().getShip()!=null && w.getUnit().getShip().getAmount()>1) {
					// Bingo
					w.addOrder("BETRETE SCHIFF " + s.getID() + " ; Werftsegler, Schiff wird von Werft verlassen, ich verlasse damit die Flotte. (" + this.Durchlauf + ")", true);
					w.hasBetreteOrder=true;
					break;
				}
			}
		}
		
		for (Werftsegler w: this.Hafenkapitäne) {
			if (!w.hasBetreteOrder && w.getUnit().getShip()==null) {
				// Bingo
				w.addComment("Werftsegler: keine weiteren Schiffe verfügbar, " + this.ListeFreiWerdendeSchiffe.size() + " Schiffe wurden anderweitig berücksichtigt.");
			}
			if (!w.hasBetreteOrder && w.getUnit().getShip()!=null && w.getUnit().getShip().getAmount()>1) {
				// bin auf einer Flotte, betrete kein neues Schiff, möchte hier runter...
				w.addOrder("VERLASSE ; Werftsegler - Schiff wurde übergeben, ich warte weiter an Land", false);
			}
		}
		
	}
	
	private void setGIBOrders() {
		for (Werftsegler w: this.Hafenkapitäne) {
			if (w.getUnit().getShip()!=null && w.getGIB_AN_Werftsegler()!=null) {
				// Bingo
				w.addOrder("GIB " + w.getGIB_AN_Werftsegler().getUnit().getID() + " 1 Schiff ; Werftsegler", true);
			}
		}
	}
	
	private void DoNotConfirmFullFleets() {
		for (Werftsegler w: this.Flottenkapitäne) {
			if (w.getActSchiffe_nach_GIB()==w.getMaxSchiffe()) {
				// Bingo
				w.doNotConfirmOrders("Flotte ist vollständig gefüllt (" + w.getMaxSchiffe() + " Schiffe)");
			}
		}
	}
	
	
	
   
}// ende class
