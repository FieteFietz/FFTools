package com.fftools.transport;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;

import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.gamebinding.MovementEvaluator;
import magellan.library.rules.ItemType;

import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.TransportScript;
import com.fftools.scripts.WithGotoInfo;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.GotoInfo;
import com.fftools.utils.RegionPathElement;

/**
 * alles,was einen TRansporter so ausmacht
 * @author Fiete
 *
 */
public class Transporter {
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	public static int transporterMode_manuell = 1;
	public static int transporterMode_fullautomatic = 2;
	
	private static int transporterLernPrio = Integer.MAX_VALUE-1;
	
	/**
	 * zugrundeliegende scriptunit
	 * gleichzeitig aktuelle region
	 */
	private ScriptUnit scriptUnit = null;
	/**
	 * Zielregion bereits festgelegt
	 */
	private Region destRegion = null;
	/**
	 * soll Transporter immer maximal Pferde requesten?
	 */
	private boolean getMaxPferde = true;
	/**
	 * mit welcher Prio Pferde anfordern?
	 */
	private int pferdRequestPrio = 500;
	/**
	 * soll Transporter immer maximal Pferde requesten?
	 */
	private boolean getMaxWagen = true;
	/**
	 * mit welcher Prio Pferde anfordern?
	 */
	private int wagenRequestPrio = 500;
	/**
	 * Kapazität des Transporters
	 * in erster Version immer reitend...
	 */
	private int kapa = 0;
	
	/**
	 * was ist davon noch frei?
	 */
	private int kapa_frei = 0;
	
	/**
	 * Kapa und freie Kapa falls TMo auf dem weg zur Offer
	 */
	private int kapa_planung = 0;
	private int kapa_frei_planung = 0;
	private boolean geplanteLeerfahrt = false;
	private Region von_region_planung = null;
	private Region nach_region_planung = null;
	
	/**
	 * Spezialisierungen (nur Steine etc..)
	 */
	private ArrayList<String> specs = null;
	
	/**
	 * mit Route=fest kann der benutzer den weg des Transporters 
	 * bestimmen (mit scripten, oder mit @nach oder mit nach ... ;do_not_touch
	 */
	private boolean useUserRoute = false;
	
	/**
	 * Default zum Pferde und Wagen organisieren ist reiten..kann aber auch
	 * anders sein
	 */
	private boolean isRiding = true;
	
	/**
	 * infos vom und mit dem Goto
	 */
	private GotoInfo gotoInfo = null;
	
	/**
	 * modus des Transport...default später Auto..jetzt zum testen manuell
	 */
	private int mode = transporterMode_manuell;
	
	/**
	 * Liste der requests...
	 */
	private ArrayList<TransporterRequest> transporterRequests = null;
	
	/**
	 * Liste der geplanten requests...
	 */
	private ArrayList<TransporterRequest> transporterRequestsPlanung = null;
	
	/**
	 * wird vor dem sortieren von transportern mit 
	 * entfernung zu einem referenzpunkt gesetzt
	 */
	private int actDist = 0;
	
	/**
	 * Ab welcher Prio nimmt der Transporter auch Aufträge an, auch wenn er
	 * nicht die transporterErstPferdeMinAusstattung% Pferde bekommen hat
	 */
	private int transporterErstPferdePrio=500;
	
	/**
	 * Der Transporter sieht sich als ausgestattet an, wenn er mindestens
	 * transporterErstPferdeMinAusstattung% der benötigten Pferde bekommen hat.
	 */
	private int transporterErstPferdeMinAusstattung=50;
	
	/**
	 * Soll der Transporter nur bei transporterErstPferdeMinAusstattung an Pferden
	 * transportieren und sich ansonsten erstmal Pferde suchen bzw Lernen?
	 */
	private boolean transporterErstPferde=false;
	
	/**
	 * enthält bei recalcKapa berechnete Anzahl benötigter Pferde 
	 * bis zur vollAusstattung
	 */
	private int transporterErstPferdeForderung=0;
	
	/**
	 * wenn wahr, wird bei unzureichender PferdeAusstattung gelernt statt als Transporter zu arbeiten
	 */
	private boolean transporterErstPferdeLernen = false;
	
	/**
	 * unter diesem Reittalent lernt der Transporter reiten
	 * macht nix anderes
	 */
	private int minReitTalent = 0 ;
	
	/**
	 * ist wahr, wenn der Transporter gar nicht als Transporter fungiert
	 * sondern lernt
	 */
	private boolean isLearning = false;
	
	/**
	 * zum einfacheren Checken, wieviele Pferde erhalten worden sind
	 */
	private ArrayList<MatPoolRequest> pferdeMPRs;
	
	
	/**
	 * zum einfacheren Checken, wieviele Pferde erhalten worden sind
	 */
	private ArrayList<MatPoolRequest> wagenMPRs;
	
	/**
	 * initiiert den Transporter
	 * @param u
	 */
	public Transporter(ScriptUnit u){
		this.scriptUnit = u;
		
		// Kapazität erstmal gleich magellan kapa...Reitend
		this.kapa = (int)(this.scriptUnit.getPayloadOnHorse()/100);
		this.kapa_frei = this.kapa;
		
		// parst reportsettings und setzt, falls vorhanden
		// ToDo..falls benötigt
		
		// parst die orders und setzt attribute, falls vorhanden
		this.parseOrders();
	}
	
	
	public void checkFesteRoute() {
		if (this.useUserRoute){
			// in diesem Fall müsste bereits jetzt ein script eine GoTo info erzeugt haben
			Object o = this.getScriptUnit().getScript(WithGotoInfo.class);
			if (o!=null){
				WithGotoInfo otherScript = (WithGotoInfo) o;
				GotoInfo otherGotoInfo = otherScript.getGotoInfo();
				if (otherGotoInfo!=null){
					this.destRegion = otherGotoInfo.getDestRegion();
					this.scriptUnit.addComment("TM_T, Route=fest: Ziel übernommen: " + this.destRegion.toString());
					this.gotoInfo = otherGotoInfo;
				} else {
     				// GotoInfo = null
					this.scriptUnit.addComment("!!! Route=fest aber keine Route festgelegt durch Script: " + o.getClass().getName());
					this.useUserRoute=false;
				}
			} else {
				// kein Script mit GotoInfo...
				this.scriptUnit.addComment("!!! Route=fest aber kein Script, welches ein Goto produziert!");
				this.useUserRoute=false;
			}
		}
	}
	
	
	private void parseOrders(){
		if (this.scriptUnit==null){return;}
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Transport");
		
		if (OP.getOptionString("Route").equalsIgnoreCase("fest")){
			this.useUserRoute = true;
		}
		
		if (!OP.getOptionBoolean("maxWagen",true)){
			this.getMaxWagen=false;
		}
		
		if (!OP.getOptionBoolean("maxPferd",true)){
			this.getMaxPferde=false;
		}
		if (OP.isOptionString("kapa", "gehen")){
			this.isRiding=false;
			// this.kapa = (int)(this.scriptUnit.getUnit().getPayloadOnFoot()/100);
			MovementEvaluator ME = this.scriptUnit.getScriptMain().gd_ScriptMain.getGameSpecificStuff().getMovementEvaluator();
			this.kapa = (int)(ME.getPayloadOnFoot(this.scriptUnit.getUnit())/100);
			this.kapa_frei = this.kapa;
		}
		if (OP.getOptionInt("maxWagenPrio", -1)>=0){
			this.wagenRequestPrio = OP.getOptionInt("maxWagenPrio", -1);
		}
		if (OP.getOptionInt("maxPferdPrio", -1)>=0){
			this.pferdRequestPrio = OP.getOptionInt("maxPferdPrio", -1);
		}
		
		// Anpassung der Prio
		this.pferdRequestPrio+=this.scriptUnit.getSkillLevel("Reiten");
		this.pferdRequestPrio-=this.scriptUnit.getUnit().getModifiedPersons();
		
		
		if (OP.isOptionString("mode", "auto")){
			this.mode = transporterMode_fullautomatic;
		}
		
		// Specs
		String specsString = OP.getOptionString("Spec");
		if (specsString.length()>2){
			// splitten, falls mehrere da
			String[] specsArray = specsString.split(",");
			this.specs = new ArrayList<String>();
			for (int i=0;i<specsArray.length;i++){
				String s2 = specsArray[i];
				this.specs.add(s2);
			}
		}
		
		// tarnsporterErstPferde
		this.transporterErstPferde = reportSettings.getOptionBoolean("transporterErstPferde");
		if (OP.getOptionString("transporterErstPferde").length()>1) {
			this.transporterErstPferde=OP.getOptionBoolean("transporterErstPferde",false);
		}
		// transporterErstPferdeMinAusstattung
		int minA = reportSettings.getOptionInt("transporterErstPferdeMinAusstattung", this.getActRegion());
		if (minA>0){
			this.transporterErstPferdeMinAusstattung = minA;
		}
		minA=OP.getOptionInt("transporterErstPferdeMinAusstattung", -1);
		if (minA>0){
			this.transporterErstPferdeMinAusstattung = minA;
		}
		// transporterErstPferdePrio
		minA = reportSettings.getOptionInt("transporterErstPferdePrio", this.getActRegion());
		if (minA>0){
			this.transporterErstPferdePrio = minA;
		}
		minA=OP.getOptionInt("transporterErstPferdePrio", -1);
		if (minA>0){
			this.transporterErstPferdePrio = minA;
		}
		
        // tarnsporterErstPferdeLernen
		this.transporterErstPferdeLernen = reportSettings.getOptionBoolean("transporterErstPferdeLernen",this.getActRegion());
		if (OP.getOptionString("transporterErstPferdeLernen").length()>1) {
			this.transporterErstPferdeLernen=OP.getOptionBoolean("transporterErstPferdeLernen",false);
		}
		
		// minReitTalent
		int reportMinReitTalent = reportSettings.getOptionInt("transporterMinReitTalent",this.getActRegion());
		if (reportMinReitTalent!=ReportSettings.KEY_NOT_FOUND){
			this.minReitTalent = reportMinReitTalent;
		}
		
		if (OP.getOptionInt("minReitTalent", -1)>-1	){
			this.minReitTalent = OP.getOptionInt("minReitTalent", -1);
			this.scriptUnit.addComment("parse Orders: minReitTalent gesetzt auf " + this.minReitTalent);
		}
		if (OP.getOptionInt("minTalent", -1)>-1	){
			this.minReitTalent = OP.getOptionInt("minTalent", -1);
			this.scriptUnit.addComment("parse Orders: minReitTalent gesetzt auf " + this.minReitTalent);
		}
		
	}

	/**
	 * @return the destRegion
	 */
	public Region getDestRegion() {
		return destRegion;
	}

	/**
	 * @param destRegion the destRegion to set
	 */
	public void setDestRegion(Region destRegion) {
		this.destRegion = destRegion;
		this.gotoInfo = new GotoInfo();
		this.gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.getActRegion().getCoordinate(), destRegion.getCoordinate(),false,"Transporter - SetDestRegion");
	}
	
	public void processDestRegion(){
		if (this.destRegion!=null){
			this.gotoInfo = new GotoInfo();
			this.gotoInfo = FFToolsRegions.makeOrderNACH(this.scriptUnit, this.getActRegion().getCoordinate(), destRegion.getCoordinate(),true,"Transporter-  process Dest Region");
			
			// test more info
			GotoInfo test = FFToolsRegions.getPathDistLandGotoInfo(this.scriptUnit.getScriptMain().gd_ScriptMain, this.getActRegion().getCoordinate(), destRegion.getCoordinate(), this.isRiding, this.scriptUnit.isInsekt());
			if (test!=null){
				if (test.getAnzRunden()>0){
					if (test.getAnzRunden()>1){
						for (int i=0; i<test.getAnzRunden();i++){
							RegionPathElement pE = test.getPathElement(i);
							if (pE!=null){
								this.scriptUnit.addComment("Plan Runde " + i + ": von " + pE.getVon_Region().toString() + " nach " + pE.getNach_Region().toString());
							} else {
								this.scriptUnit.addComment("extended GoToInfo Runde " + i + ": keine Info.");
							}
						}
					}
				} else {
					this.scriptUnit.addComment("extended GoToInfo: " + test.getAnzRunden() + " Runden (?)");
				}
			} else {
				this.scriptUnit.addComment("no extended GoToInfo available - can not reach target region.");
			}
			
			
		}
	}
	
	/**
	 * @return the kapa
	 */
	public int getKapa() {
		return kapa;
	}

	/**
	 * @param kapa the kapa to set
	 */
	public void setKapa(int kapa) {
		this.kapa = kapa;
	}

	/**
	 * @return the kapa_frei
	 */
	public int getKapa_frei() {
		return kapa_frei;
	}

	/**
	 * @param kapa_frei the kapa_frei to set
	 */
	public void setKapa_frei(int kapa_frei) {
		this.kapa_frei = kapa_frei;
	}

	/**
	 * @return the pferdRequestPrio
	 */
	public int getPferdRequestPrio() {
		return pferdRequestPrio;
	}

	

	/**
	 * @return the wagenRequestPrio
	 */
	public int getWagenRequestPrio() {
		return wagenRequestPrio;
	}

	

	/**
	 * @return the getMaxPferde
	 */
	public boolean isGetMaxPferde() {
		return getMaxPferde;
	}

	/**
	 * @return the getMaxWagen
	 */
	public boolean isGetMaxWagen() {
		return getMaxWagen;
	}

	/**
	 * @return the scriptUnit
	 */
	public ScriptUnit getScriptUnit() {
		return scriptUnit;
	}

	/**
	 * @return the specs
	 */
	public ArrayList<String> getSpecs() {
		return specs;
	}

	/**
	 * @return the useUserRoute
	 */
	public boolean isUseUserRoute() {
		return useUserRoute;
	}

	/**
	 * @return the isRiding
	 */
	public boolean isRiding() {
		return isRiding;
	}
	
	/**
	 * liefert die Region, in welcher der Transport sich aktuell aufhält
	 * @return
	 */
	public Region getActRegion(){
		return this.scriptUnit.getUnit().getRegion();
	}
	
	/**
	 * fügt einen Request dazu. Transport in der Region der Offer...
	 * setzt auch die Nachbefehle, falls noch nicht geschehen
	 * @param request
	 * @param offer
	 * @param Anzahl
	 */
	public void processRequest(TransportRequest request, TransportOffer offer, int Anzahl, String intro){
		// wenn noch nicht gesetzt, Marschebfehl setzen
		if (this.destRegion==null){
			this.setDestRegion(request.getRegion());
		}
		// requests
		this.scriptUnit.findScriptClass("Request", Anzahl + " " + offer.getItemName() + " 2 region=ja");
		// bearbeiten der relations
		request.incBearbeitet(Anzahl);
		offer.incBearbeitet(Anzahl);
		// Kapa des Transports reduzieren
		if (offer.getItem().getItemType().getWeight()>0){
			int verbrauch = (int)Math.ceil((offer.getItem().getItemType().getWeight()*Anzahl));
			this.kapa_frei -= verbrauch;
		}
		
		// tarnsporterRequests füllen
		if (this.transporterRequests==null){
			this.transporterRequests = new ArrayList<TransporterRequest>(1);
		}
		TransporterRequest TR = new TransporterRequest(Anzahl,offer,request,this, intro);
		this.transporterRequests.add(TR);
		// this.informUnits(Anzahl, offer, request);
	}
	
	/**
	private void informUnits(int Anzahl,TransportOffer offer, TransportRequest request){
		
		// ETA berechnung
		int ETAAnzahlRunden = this.getETA(offer, request);
		offer.getScriptUnit().addComment("TM(-):" + Anzahl + " " + offer.getItemName() + " (P:" + request.getPrio()  + ",ETA:" + ETAAnzahlRunden + ") nach " + request.getRegion().toString() + " mit " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
		request.getScriptUnit().addComment("TM(+):" + Anzahl + " " + offer.getItemName() + " (P:" + request.getPrio() + ",ETA:" + ETAAnzahlRunden +  ") von " + offer.getRegion().toString() + ") mit " + this.scriptUnit.getUnit().toString(true) + " in " + this.scriptUnit.getUnit().getRegion().toString());
	}
	**/
	
	/**
	 * informiert die requester und offerer über die zugeteilten transporter
	 * und trägt entsprechend auch Infos beim transporter ein
	 */
	public void informUnits(){
		this.processDestRegion();
		if (this.transporterRequestsPlanung!=null && this.transporterRequestsPlanung.size()>0){
			this.informUnits(this.transporterRequestsPlanung);
		}
		if (this.transporterRequests!=null && this.transporterRequests.size()>0){
			this.informUnits(this.transporterRequests);
		}
	}
	
	/**
	 * ruft informUnits für die request // offer paare auf
	 * @param transporterRequests ArrayList of TransporterRequest
	 */
	private void informUnits(ArrayList<TransporterRequest> transporterRequests){
		for (TransporterRequest r : transporterRequests){
			r.informUnits();
		}
	}
	
	
	
	
	/**
	 * fügt einen Request zur Planung der Leerfahrt
	 * 
	 * @param request
	 * @param offer
	 * @param Anzahl
	 */
	public void processLeerfahrtRequest(TransportRequest request, TransportOffer offer, int Anzahl, String intro){
		// bearbeiten der relations
		request.incBearbeitet(Anzahl);
		offer.incBearbeitet(Anzahl);
		// Kapa des Transports reduzieren
		if (offer.getItem().getItemType().getWeight()>0){
			int verbrauch = (int)Math.ceil((offer.getItem().getItemType().getWeight()*Anzahl));
			this.kapa_frei_planung -= verbrauch;
		}
        //	tarnsporterRequestsPlanung füllen
		if (this.transporterRequestsPlanung==null){
			this.transporterRequestsPlanung = new ArrayList<TransporterRequest>(1);
		}
		TransporterRequest TR = new TransporterRequest(Anzahl,offer,request,this,intro);
		this.transporterRequestsPlanung.add(TR);
		// this.informUnits(Anzahl, offer, request);
	}

	/**
	 * @return the mode
	 */
	public int getMode() {
		return mode;
	}


	/**
	 * @return the transporterRequests
	 */
	public ArrayList<TransporterRequest> getTransporterRequests() {
		return transporterRequests;
	}
	
	/**
	 * fügt die TransporterRequests dem script hinzu
	 * @param s
	 */
	public void generateTransporterRequests(TransportScript s){
		if (this.transporterRequests==null || this.transporterRequests.isEmpty()){
			return;
		}
		for (Iterator<TransporterRequest> iter = this.transporterRequests.iterator();iter.hasNext();){
			TransporterRequest tR = (TransporterRequest)iter.next();
			MatPoolRequest mpr = tR.getMatPoolRequest(s);
			s.addMatPoolRequest(mpr);
		}
	}
	
	/**
	 * gibt Kurzinfo, was der Transport eigentlich machen soll...
	 *
	 
	public void informUser(int Anzahl,TransportRequest request,TransportOffer offer,String intro){
		// ETA berechnung
		int ETAAnzahlRunden = this.getETA(offer, request);
		// this.scriptUnit.addComment(intro + ":" + Anzahl + " " + offer.getItemName() + " von " + offer.getRegion().toString() + " nach " + request.getRegion().toString() + " Prio:" + request.getPrio());
		this.scriptUnit.addComment(intro + ":" + Anzahl + " " + offer.getItemName() + "(P:" + request.getPrio() + ",ETA:" + ETAAnzahlRunden + ") von " + offer.getRegion().toString() + " nach " + request.getRegion().toString());
	}
    */

	/**
	 * @return the gotoInfo
	 */
	public GotoInfo getGotoInfo() {
		return gotoInfo;
	}
	
	/**
	 * bestimmt die kapa das Trans neu, nachdem beim 1. MatPoolRun
	 * Pferde und Wagen zugerodnet wurden und die Ladung abgegeben
	 * wurde...
	 *
	 */
	public void recalcKapa(){
		// sollte doch 0 sein..naja, vielleicht haben wir ja externe übergaben
		// also drinne lassen
		int modLoad = this.scriptUnit.getModifiedLoad();
		int maxOnFoot = this.scriptUnit.getPayloadOnFoot();

		if(maxOnFoot < 0) {
			// kann sich gar nicht bewegen
			this.kapa = 0;
			this.kapa_frei = 0;
			this.isRiding = false;
		} else {
			this.kapa = (int)Math.floor(maxOnFoot/100);
			this.kapa_frei = (int)Math.floor((maxOnFoot - modLoad)/100);
			this.isRiding = false;
		}
		
		// kann er vielleicht doch reiten?
		int maxOnHorse  = this.scriptUnit.getPayloadOnHorse();
		if(!(maxOnHorse < 0)) {
			// sie kann reiten
			if (modLoad<=maxOnHorse){
				// OK, wir können reiten
				this.kapa = (int)Math.floor(maxOnHorse/100);
				this.kapa_frei = (int)Math.floor((maxOnHorse - modLoad)/100);
				this.isRiding = true;
			}
		}
		
		// Falls jetzt bereits Ziel gesetzt (Route=fest)
		// umsetzen und GotoInfo neu machen
		if (this.destRegion!=null){
			this.processDestRegion();
		}
		
		
		NumberFormat NF = NumberFormat.getInstance();
		NF.setMaximumFractionDigits(1);
		NF.setMinimumFractionDigits(0);
		NF.setMinimumIntegerDigits(1);
		NF.setMaximumIntegerDigits(3);
		
		// Überprüfen der Ausstattung mit Pferden
		// Anpassen der Prio und das ganze nur mit MP2 und wenn Pferde gefordert wurden
		if (!this.transporterErstPferde || this.pferdeMPRs==null){
			// Kein Limit setzen
			this.transporterErstPferdePrio=0;
		} else {
			// Erst Pferde muss untersucht werden
			// mal kucken wieviele wir bekommen haben und wie viele wir gewollt haben
			int erhalten = 0;
			Item pferdItem = this.scriptUnit.getModifiedItem(this.getScriptUnit().getScriptMain().gd_ScriptMain.getRules().getItemType("Pferd"));
			if (pferdItem!=null){
				erhalten = pferdItem.getAmount();
			}
			// gefordert doch neu ausrechnen
			int gefordert=0;
			// Reittalent
			Skill reitSkill= this.scriptUnit.getUnit().getModifiedSkill(this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getSkillType("Reiten", false));
			if (reitSkill!=null){
				gefordert = this.scriptUnit.getUnit().getModifiedPersons() * 2 * reitSkill.getLevel();
			}
			
			this.transporterErstPferdeForderung = gefordert - erhalten;
			
			if (gefordert<=0){
				this.transporterErstPferdePrio=0;
				this.scriptUnit.addComment("Keine Pferde angefordert (?)");
			} else {
				double auslastung = ((double)erhalten / (double)gefordert)*100;
				if (auslastung>=this.transporterErstPferdeMinAusstattung){
					this.transporterErstPferdePrio=0;	
				} else {
					this.scriptUnit.addComment("Ich brauche Pferde!");
				}
				this.scriptUnit.addComment("Pferdeausstattung: " + NF.format(auslastung) + "% (min:" + this.transporterErstPferdeMinAusstattung + "%) (gefordert: " + gefordert + " Pferde)");
			}
		}
		
	}


	/**
	 * @return the actDist
	 */
	public int getActDist() {
		return actDist;
	}


	/**
	 * @param actDist the actDist to set
	 */
	public void setActDist(int actDist) {
		this.actDist = actDist;
	}
	
	/**
	 * kurze info über auslastung und status des transporters
	 * @return
	 */
	public String informLogTransporterStatus(NumberFormat NF){
		String erg = "";
		
		if (isLearning){
			return "Transporter lernt.";
		}
		
		// Auslastung
		double auslastung = 0;
		if (this.kapa!=0){
			// in Prozent
			auslastung = (1 - ((double)this.kapa_frei/(double)this.kapa))*100;
		}
		erg += NF.format(auslastung) + "% ";
		erg += "Kapa " + this.kapa + " ";
		erg += this.getScriptUnit().unitDesc();
		if (getDestRegion()!=null){
			erg += " nach " + this.getDestRegion().toString();
		}
		return erg;
	}


	/**
	 * @return the kapa_frei_planung
	 */
	public int getKapa_frei_planung() {
		return kapa_frei_planung;
	}


	/**
	 * @param kapa_frei_planung the kapa_frei_planung to set
	 */
	public void setKapa_frei_planung(int kapa_frei_planung) {
		this.kapa_frei_planung = kapa_frei_planung;
	}


	/**
	 * @return the kapa_planung
	 */
	public int getKapa_planung() {
		return kapa_planung;
	}


	/**
	 * @param kapa_planung the kapa_planung to set
	 */
	public void setKapa_planung(int kapa_planung) {
		this.kapa_planung = kapa_planung;
	}


	/**
	 * @return the geplanteLeerfahrt
	 */
	public boolean isGeplanteLeerfahrt() {
		return geplanteLeerfahrt;
	}


	/**
	 * @param geplanteLeerfahrt the geplanteLeerfahrt to set
	 */
	public void setGeplanteLeerfahrt(boolean geplanteLeerfahrt) {
		this.geplanteLeerfahrt = geplanteLeerfahrt;
	}
	
	/**
	 * wird von TM gesetzt
	 * Transport ist auf dem Weg zur Offer
	 *
	 */
	public void setToLeerFahrt(TransportOffer offer,TransportRequest request,int Anzahl,String intro){
		this.von_region_planung = offer.getRegion();
		this.nach_region_planung = request.getRegion();
		this.geplanteLeerfahrt = true;
		this.kapa_planung = this.getKapa();
		this.kapa_frei_planung = this.getKapa_frei();
		
		this.setDestRegion(offer.getRegion());
		
		// offers und requests bearbeiten
		request.incBearbeitet(Anzahl);
		offer.incBearbeitet(Anzahl);
		
		// geplante kapa reduzieren
		if (offer.getItem().getItemType().getWeight()>0){
			int verbrauch = (int)Math.ceil((offer.getItem().getItemType().getWeight()*Anzahl));
			this.kapa_frei_planung -= verbrauch;
		}
        // tarnsporterRequestsPlanung füllen
		if (this.transporterRequestsPlanung==null){
			this.transporterRequestsPlanung = new ArrayList<TransporterRequest>(1);
		}
		TransporterRequest TR = new TransporterRequest(Anzahl,offer,request,this,intro);
		this.transporterRequestsPlanung.add(TR);
		// this.informUnits(Anzahl, offer, request);
	}
	
	/**
	 * liefert wahr, wenn transport geplant von offer nach request
	 * und noch kapa verfügbar
	 * @param offer
	 * @param request
	 * @return
	 */
	public boolean samePlanungOK(TransportOffer offer, TransportRequest request){
		boolean erg = true;
		
		if (this.kapa_frei_planung<=0){
			return false;
		}
		if (!this.geplanteLeerfahrt){
			return false;
		}
		if (this.mode!=Transporter.transporterMode_fullautomatic){
			return false;
		}
		if (!this.von_region_planung.equals(offer.getRegion())){
			return false;
		}
		if (!this.nach_region_planung.equals(request.getRegion())){
			return false;
		}
		
		return erg;
	}
	
	/**
	 * Liefert wahr, wenn  actSpec durch die Specs des Transporters
	 * als zugehörig identifiziert wird. Möglich sind beiderseitig Strings
	 * und Namen von Itemgroups
	 * 
	 * @param actSpec
	 * @return
	 */
	public boolean isInSpec(String actSpec){
		boolean erg = false;
		if (this.specs==null || this.specs.size()==0){
			return false;
		}
		for (Iterator<String> iter = this.specs.iterator();iter.hasNext();){
			String s = (String)iter.next();
			// check 1: direkte Übereinstimmung
			if (actSpec.equalsIgnoreCase(s)){
				return true;
			}
			
			// check 2: actSpec ist eine Itemgroup und s ein ItemType
			if (reportSettings.isInCategories(actSpec)){
				// ja...ist s in ihr enthalten?
				// Dazu ItemType feststellen
				ItemType actItemType = this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getItemType(s);
				if (actItemType!=null){
					// s ist ItemType
					if (reportSettings.getItemTypes(actSpec).contains(actItemType)){
						return true;
					}
				}
			}
			
			// check 3: s ist eine Itemgroup und actSpec ein ItemType
			if (reportSettings.isInCategories(s)){
				// ja...ist actSpec in ihr enthalten?
				// Dazu ItemType feststellen
				ItemType actItemType = this.scriptUnit.getScriptMain().gd_ScriptMain.getRules().getItemType(actSpec);
				if (actItemType!=null){
					// actSpec ist ItemType
					if (reportSettings.getItemTypes(s).contains(actItemType)){
						return true;
					}
				}
			}
			
			// check 4: beides ItemGroups, übereinstimmungen
			if (reportSettings.isInCategories(s) && reportSettings.isInCategories(actSpec)){
				for (Iterator<ItemType> iter2 = reportSettings.getItemTypes(s).iterator();iter2.hasNext();){
					ItemType actItemType = (ItemType)iter2.next();
					if (reportSettings.getItemTypes(actSpec).contains(actItemType)){
						return true;
					}
				}
			}
		}
		return erg;
	}
	
	/**
	 * Liefert wahr, wenn mindestens ein Spec aus der Arraylist
	 * durch die Specs dieses Transports als zugehörig identifiziert 
	 * wird
	 * @param actSpecs
	 * @return
	 */

	public boolean isInSpec(ArrayList<String> actSpecs){
		boolean erg = false;
		for (Iterator<String> iter = actSpecs.iterator();iter.hasNext();){
			String actSpec = (String)iter.next();
			if (this.isInSpec(actSpec)){
				return true;
			}
		}
		return erg;
	}
	
	/**
	 * liefert wahr, wenn dieser Transporter zur Bedienung des
	 * angegeben TransportRequests nach dem Kriterium der Specs
	 * herangezogen werden kann.
	 * @param TR TransportRequest
	 * @return
	 */
	public boolean isOK4TransportRequest(TransportRequest TR){
		
		
		if (isLearning){
			return false;
		}
		
		// Einschub der Prio...
		if (this.transporterErstPferdePrio>0 && TR.getPrio()<this.transporterErstPferdePrio){
			// sorry, aber TransportR liegt leider unter der Marke
			// wenn kein besserer TR kommt, wird dieser Transport wohl auf Pferdesuche gehen
			// jetzt! Pferde organisieren...falls nicht erfolgreich, je nach 
			// optionen, lernen oder weitermachen
			this.getScriptUnit().getOverlord().getTransportManager().erstPferde(this);
			if (this.getGotoInfo()!=null){
				// Der Transport hat nen Ziel
				// für neue Transporte verfügbar
				// zukünftig nicht mehr prüfen
				this.transporterErstPferdePrio = 0;
			} else {
				// noch keine Pferde GOTO gesetzt
				// soll er lernen oder transporter sein?
				// default: Lernen=aus -> Transporter
				if (this.transporterErstPferdeLernen){
					// nicht bewegen, nur Lernen
					this.getScriptUnit().addOrder("LERNEN Reiten", true);
					this.getScriptUnit().addComment("TM (ohne Pferde): keine Aufträge, freie Kapa:" + this.getKapa_frei());
					this.transporterErstPferdePrio = Transporter.transporterLernPrio;
					return false;
				} else {
					// kann weiter machen
					this.getScriptUnit().addComment("Keine Pferde auffindbar, weiter als Transporter im Einsatz", true);
					this.transporterErstPferdePrio = 0;
				}
			}
		} else {
			if (this.transporterErstPferdePrio>0){
				this.getScriptUnit().addComment("TM (zu wenig Pferde): der zugeteilte Auftrag ist wichtiger.");
			}
		}
		
		
		if (TR.getTransporterSpecs()==null || TR.getTransporterSpecs().size()==0){
			// keine Specs..dann jeder Transport geeignet
			return true;
		}
		
		return this.isInSpec(TR.getTransporterSpecs());
	}
	
	/**
	 * lädt aktuell geplante Ladung auf den anderen Transporter um und setzt
	 * den aktuellen wieder "frei"
	 * betrifft aktuelle und geplante requests
	 * quasi eine vollständige Übernahme
	 * @param transporter
	 */
	public void unload2Transport(Transporter transporter){
		if (this.transporterRequests!=null && this.transporterRequests.size()>0){
			for (TransporterRequest r:this.transporterRequests){
				transporter.addTransporterRequest(r);
			}
			this.transporterRequests.clear();
		}
		if (this.transporterRequestsPlanung!=null && this.transporterRequestsPlanung.size()>0){
			for (TransporterRequest r:this.transporterRequestsPlanung){
				transporter.addTransporterRequestPlanung(r);
			}
			this.transporterRequestsPlanung.clear();
		}
		
		// wenn dieser Transporter verplant war, ist es auch der andere nun
		if (this.von_region_planung!=null){
			transporter.von_region_planung = this.von_region_planung;
			transporter.nach_region_planung = this.nach_region_planung;
		}
		
		this.kapa_frei=this.kapa;
		this.kapa_frei_planung = this.kapa_planung;
		this.destRegion=null;
		this.gotoInfo=null;
		this.geplanteLeerfahrt=false;
		this.von_region_planung=null;
		this.nach_region_planung=null;
		// this.getScriptUnit().addComment("Transport was unloaded to " + transporter.getScriptUnit().unitDesc());
	}
	
	public void addTransporterRequest(TransporterRequest transporterRequest){
		if (this.transporterRequests==null){
			this.transporterRequests = new ArrayList<TransporterRequest>();
		}
		if (!this.transporterRequests.contains(transporterRequest)){
			this.transporterRequests.add(transporterRequest);
			this.kapa_frei-=transporterRequest.getWeight();
		}
	}
	
	public void addTransporterRequestPlanung(TransporterRequest transporterRequest){
		if (this.transporterRequestsPlanung==null){
			this.transporterRequestsPlanung = new ArrayList<TransporterRequest>();
		}
		if (!this.transporterRequestsPlanung.contains(transporterRequest)){
			this.transporterRequestsPlanung.add(transporterRequest);
			this.kapa_frei_planung-=transporterRequest.getWeight();
		}
	}


	/**
	 * @return the von_region_planung
	 */
	public Region getVon_region_planung() {
		return von_region_planung;
	}


	/**
	 * @return the nach_region_planung
	 */
	public Region getNach_region_planung() {
		return nach_region_planung;
	}

	/**
	 * @param pferdeMPR the pferdeMPR to set
	 */
	public void addPferdeMPR(MatPoolRequest pferdeMPR) {
		// this.pferdeMPR = pferdeMPR;
		if (this.pferdeMPRs==null){
			this.pferdeMPRs = new ArrayList<MatPoolRequest>();
		}
		this.pferdeMPRs.add(pferdeMPR);
	}

	/**
	 * @param wagenMPR the wagenMPR to set
	 */
	public void addWagenMPR(MatPoolRequest wagenMPR) {
		// this.pferdeMPR = pferdeMPR;
		if (this.wagenMPRs==null){
			this.wagenMPRs = new ArrayList<MatPoolRequest>();
		}
		this.wagenMPRs.add(wagenMPR);
	}

	/**
	 * @return the transporterErstPferdePrio
	 */
	public int getTransporterErstPferdePrio() {
		return transporterErstPferdePrio;
	}


	/**
	 * @return the transporterErstPferdeMinAusstattung
	 */
	public int getTransporterErstPferdeMinAusstattung() {
		return transporterErstPferdeMinAusstattung;
	}


	/**
	 * @return the transporterErstPferde
	 */
	public boolean isTransporterErstPferde() {
		return transporterErstPferde;
	}


	/**
	 * @return the transporterErstPferdeForderung
	 */
	public int getTransporterErstPferdeForderung() {
		return transporterErstPferdeForderung;
	}


	/**
	 * @return the minReitTalent
	 */
	public int getMinReitTalent() {
		return minReitTalent;
	}


	/**
	 * @param minReitTalent the minReitTalent to set
	 */
	public void setMinReitTalent(int minReitTalent) {
		this.minReitTalent = minReitTalent;
	}


	/**
	 * @return the isLearning
	 */
	public boolean isLearning() {
		return isLearning;
	}


	/**
	 * @param isLearning the isLearning to set
	 */
	public void setLearning(boolean isLearning) {
		this.isLearning = isLearning;
	}
	
}
