package com.fftools.scripts;

import java.util.Collection;
import java.util.Iterator;

import magellan.library.Item;

import com.fftools.pools.matpool.relations.MatPoolRequest;

public class Lohn extends MatPoolScript{
	
	int Prioritaet_Lohn = 1000;
	int Min_Prioritaet_Lohn = 900;
	
	int Durchlauf_Anforderung = 34;
	int Durchlauf_afterMatPoolCheck = 420;
	// ueberpruefung mittels matPoolRunFinished
	// int Durchlauf_ueberpruefung = 7;
	
	private int[] runners = {Durchlauf_Anforderung,Durchlauf_afterMatPoolCheck};
	
	String kommentar = "Wochenlohn";
	
	private int silber_verlangt = 0;
	private int AnzRunden = 3;
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Lohn() {
		super.setRunAt(this.runners);
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
		
		if (scriptDurchlauf==this.Durchlauf_Anforderung){
			this.anforderung();
		}
		
		if (scriptDurchlauf==this.Durchlauf_afterMatPoolCheck){
			this.check();
		}
		
	}
	
	
	
	
	private void anforderung(){
		//		 hier code fuer Lohn anforderung
		// addOutLine("....scriptstart Lohn mit " + super.getArgCount() + " Argumenten");
		
		// eventuell Anzahl der Runden bestimmen
		// script aufruf dazu // script Lohn [Wochenanzahl]
		if (super.getArgCount()>0){
			try {
				this.AnzRunden = Integer.valueOf(super.getArgAt(0));
			} catch (NumberFormatException e) {
				this.doNotConfirmOrders();
				this.addComment("Lohn!!! Fehler beim Erkennen des Argumentes!!!");
				this.AnzRunden=1;
			}
			// Notloesung...lieber 2 runden statt 0 silber...hin und zurueck halt...
			if (this.AnzRunden==0){this.AnzRunden=2;}
		} else {
			// kein weiteres Argument
			int settingsRunden = reportSettings.getOptionInt("LohnRunden", this.region());
			if (settingsRunden>0){
				this.AnzRunden = settingsRunden;
			}
		}
		
		// anzahl der in Frage kommenden Personen bestimmen
		int personenAnzahl = super.scriptUnit.getUnit().getModifiedPersons();
		// wurde was dazurekrutiert?
		personenAnzahl += super.scriptUnit.getRecruitedPersons();
		
		// Menge benoetigten Silbers...Trivial
		// int silber = personenAnzahl * 10 * this.AnzRunden;
		// Und dazu...
		// this.addMatPoolRequest(new MatPoolRequest(this,silber,"Silber",Prioritaet_Lohn,kommentar));
		
		// falls mehrere Runden, Prio entsprechend anpassen
		this.setPrioParameter(this.Prioritaet_Lohn-this.Min_Prioritaet_Lohn, -0.5, 0, this.Min_Prioritaet_Lohn);
		int silber = personenAnzahl * 10;
		this.silber_verlangt = silber;
		for (int i = 1;i <= this.AnzRunden;i++){
			this.addMatPoolRequest(new MatPoolRequest(this,silber,"Silber",this.getPrio(i-1),kommentar));
		}
		
		
		
		// Marc will keine solchen Kommentare..tsss
		// super.addComment(silber + " Silber Wochenlohn mit Prio " + this.Prioritaet_Lohn + " angefordert", true);
		
	}
	
	
	private void check(){
		Collection<MatPoolRequest> myRequests = this.getRequests();
		if (myRequests==null){
			//be fail safe..something's gone wrong
			outText.addOutLine("!!! Lohn does not find it´s own request!: " + this.unitDesc());
			return;
		}
		// durch alle Requests dieser Unit durchgehen
		for (Iterator<MatPoolRequest> iter = myRequests.iterator();iter.hasNext();){
			// wir erhalten prinzipiell vom iterator ein object
			MatPoolRequest o = (MatPoolRequest) iter.next();
			if (o.getScript().equals(this)) {
				// und wie früher checken
				check_mpr(o);
			}
		}
	}
	
	private void check_mpr(MatPoolRequest _mpr){
		// die MatPoolRelation wurde von diesem script erstellt und ist nu abgeschlossen
		// Durchlauf nur, falls es mehrere gibt...
		
		// gehen wir mal nur von einem aus....dann egal
		
		// wenn unsere Lohnanforderung erfuellt worden ist, alles fein, sonst keine Bestaetigung
		if (_mpr.getOriginalGefordert()==_mpr.getBearbeitet()) {
			// alles fein, wie bestellt, so bekommen
			// super.addComment(_mpr.anzahl_bearbeitet + " Silber Wochenlohn mit Prio " + this.Prioritaet_Lohn + " von MatPool bestaetigt.", true);
			super.addComment(_mpr.getBearbeitet() + " Lohn ok (with prio " + _mpr.getPrio() +")", true);
		} else {
			// gar nix fein 
			// sonstiges GIBs am MatPool vorbei (noch) nicht beruecksichtigt
			// super.addComment("Nur " + _mpr.anzahl_bearbeitet + "/" + _mpr.item.getAmount() + " Silber Wochenlohn (Prio " + this.Prioritaet_Lohn + ") von MatPool erhalten!", true);
			// super.addComment(_mpr.anzahl_bearbeitet + "/" + _mpr.item.getAmount() + " Lohn. (Prio " + this.Prioritaet_Lohn + ")", true);
			
			// jetzt checken, ob modified Silber trotzdem ausreicht
			Item it = super.scriptUnit.getModifiedItem(reportSettings.getRules().getItemType("Silber"));
			int modified_Silber = 0;
			if (it!=null) {
				modified_Silber = it.getAmount();
			}
			if (modified_Silber>=this.silber_verlangt) {
				// Silber reicjt aber aus, welches die Einheit gerade bei sich hat...
				// super.addComment("Offenbar kein Problem, weil geplant genug Silber(" + modified_Silber + ") vorhanden.", true);
				super.addComment(_mpr.getBearbeitet() + "/" + _mpr.getOriginalGefordert() + " Lohn. (Prio " + _mpr.getPrio() + ")", true);
			} else {
				// super.addComment("Einheit durch Lohn NICHT bestaetigt", true);
				super.addComment("kein Lohn (Prio " + _mpr.getPrio() + ")", true);
				super.scriptUnit.doNotConfirmOrders();
			}
		}
	}
	
	/**
	 * @see ScriptInterface
	 */
	public boolean errorMsgIfNotAllowedAddedScript(){
		return false;
	}
	
}
