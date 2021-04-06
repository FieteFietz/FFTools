package com.fftools.pools.pferde;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import magellan.library.Region;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.overlord.Overlord;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Pferde;
import com.fftools.trade.TradeArea;
import com.fftools.trade.TradeAreaHandler;
import com.fftools.trade.TradeRegion;
import com.fftools.utils.FFToolsRegions;
import com.fftools.utils.FFToolsUnits;
import com.fftools.utils.GotoInfo;

/**
 * Verwaltet Pferde"macher"
 * erweiterbar auf Pferdezüchter (?)
 * 
 * @author Fiete
 *
 */
public class PferdeManager implements OverlordRun,OverlordInfo {
	
	private static final OutTextClass outText = OutTextClass.getInstance();
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private Overlord overLord = null;
	
	/*
	 * Ausgabe in Pferde_Name.txt ?
	 */
	private boolean reportOFF = false;
	
	/**
	 * Wann soll er laufen
	 * VOR Lernfix NACH Pferde
	 */
	private static final int Durchlauf = 55;
	
	// Rückgabe als Array
	private int[] runners = {Durchlauf};
	
	// Liste aller bekannten (bekanntgemachten) Pferdemacher
	private ArrayList<Pferde> pferdeMacher = null;
	
	// Liste aller betroffenen Regionen
	private ArrayList<Region> regions = null;
	
	// Liste aller betroffnen TradeAreas
	private ArrayList<TradeArea> tradeAreas = null;
	
	// Liste aller Pferdemacher mit mode=auto und keinem mache Pferd befehl
	private ArrayList<Pferde> movingPferdeMacher = null;
	
	
	public PferdeManager(Overlord overlord){
		this.overLord = overlord;
		this.reportOFF = reportSettings.getOptionBoolean("disable_report_Pferde");
	}
	
	
	/**
	 * ergänzt einen Pferdemacher
	 * @param actPferdeMacher
	 */
	public void addPferdeMacher(Pferde actPferdeMacher ){
		if (actPferdeMacher==null){
			return;
		}
		if (this.pferdeMacher==null){
			this.pferdeMacher = new ArrayList<Pferde>();
		}
		this.pferdeMacher.add(actPferdeMacher);
	}
	
	
	
	
	/**
	 * startet den Pferdemanager
	 */
	public void run(int durchlauf){
		// Regionenliste bauen, baut dabei auch tradeAreaList
		this.buildRegionList();
		
		// Regionen durchlaufen und bearbeiten
		for (Iterator<Region> iter = this.regions.iterator();iter.hasNext();){
			Region actRegion = (Region)iter.next();
			this.processRegion(actRegion);
		}
		
		// automovers..
		if (this.movingPferdeMacher!=null && this.movingPferdeMacher.size()>0){
			// sortieren
			Collections.sort(this.movingPferdeMacher, new PferdeComparator());
			this.checkTradeAreas();
		}
		
		// wenn noch welche da...-Alternativorder setzen
		this.lastMoversOnDefault();
		
	}
	
	/**
	 * bearbeitet eine Region
	 * @param r die zu bearbeitenden Region
	 */
	private void processRegion(Region r){
		ArrayList<Pferde> regionPferdeMacher = new ArrayList<Pferde>();
		// die PferdeMacher dieser Region ergänzen
		// gleich Bestandsschutz ermitteln
		// der grösste mindeste zählt
		int maxRegionPferde = 0;
		for (Iterator<Pferde> iter=this.pferdeMacher.iterator();iter.hasNext();) {
			Pferde actPferd = (Pferde)iter.next();
			Region actRegion = actPferd.region();
			
			if (actRegion.equals(r)){
				regionPferdeMacher.add(actPferd);
				if (actPferd.getMinPferdRegion()>maxRegionPferde){
					maxRegionPferde = actPferd.getMinPferdRegion();
				}
			}
		}
		
		// alle zusammen...
		// Wieviel Pferde jibbet in der Region ?
		int pferdeInRegion = r.getHorses();
		
		// müssen wir überhaupt etwas tun?
		int zuFangen = pferdeInRegion - maxRegionPferde;
		
		if (zuFangen>0){
			// yep, wir müssen was tun
			this.processRegion(regionPferdeMacher, zuFangen);
		} else {
			// nein, wir müssen nix tun
			// alle macher Lernen lassen
			for (Iterator<Pferde> iter=regionPferdeMacher .iterator();iter.hasNext();) {
				Pferde actPferd = (Pferde)iter.next();
				actPferd.addComment("Diese Runde in dieser Region keine Pferde zu fangen.");
				// actPferd.alternativOrder();
				this.putOnDefaultORMoving(actPferd);
			}
		}
	}
	
	/**
	 * Verteilt die zu fangen Pferde auf die registrierten Pferdemacher
	 * @param r die Region
	 * @param regionPferdeMacher  ArrayList der Pferdemacher
	 * @param zuFangen  wieviel ist denn zu fangen
	 */
	private void processRegion(ArrayList<Pferde> regionPferdeMacher,int zuFangen){
		ArrayList<Pferde> infoList = new ArrayList<Pferde>();
		infoList.addAll(regionPferdeMacher);
		int nochZuFangen = zuFangen;
		int rundenRegionFang = 0;
		PferdeComparator compi = new PferdeComparator();
		while (regionPferdeMacher.size()>0){
			// sortieren der pferdeMacher nach maximaler Auslastung
			// dazu Comparator setzen
			compi.setZuFangen(nochZuFangen);
			Collections.sort(regionPferdeMacher,compi);
			// den an nummer eins stehenden checken
			Pferde actPferdMacher = regionPferdeMacher.get(0);
			if (actPferdMacher.isInMinAuslastung(nochZuFangen)){
				// OK...do it
				int actZuFangen = Math.min(actPferdMacher.maxMachenPferde(), nochZuFangen);
				// Fangbefehl setzen
				actPferdMacher.addOrder("MACHEN " + actZuFangen + " Pferd", true);
				actPferdMacher.addComment("Insgesamt zu fangen (Soll): " + zuFangen + " Pferde");
				// Reduzieren
				nochZuFangen -= actZuFangen;
				// Erhöhen
				rundenRegionFang += actZuFangen;
				FFToolsUnits.leaveAcademy(actPferdMacher.scriptUnit, " Pferdemacher arbeitet und verlässt Aka");
			} else {
				// er kann nix tun..also Lernen
				actPferdMacher.addComment("Einheit fängt hier nicht. Zu fangen: " + zuFangen);
				// actPferdMacher.alternativOrder();
				this.putOnDefaultORMoving(actPferdMacher);
			}
			// den bearbeiteten aus der Liste entfernen
			regionPferdeMacher.remove(actPferdMacher);
		}
		// Information aller über Gesamtumfang
		if (rundenRegionFang>0){
			for (Iterator<Pferde> iter=infoList.iterator();iter.hasNext();){
				Pferde actPferd = (Pferde)iter.next();
				actPferd.addComment("Gesamt-Pferd-Produktion (Ist): " + rundenRegionFang);
			}
		}
		
	}
	
	
	/**
	 * baut eine Liste aller Regionen mit 
	 * registrierten Pferdefängern
	 *
	 */
	private void buildRegionList(){
		if (this.regions==null){
			this.regions = new ArrayList<Region>();
		}
		this.regions.clear();
		if (this.pferdeMacher!=null && this.pferdeMacher.size()>0){
			for (Iterator<Pferde> iter=this.pferdeMacher.iterator();iter.hasNext();) {
				Pferde actPferd = (Pferde)iter.next();
				Region actRegion = actPferd.region();
				if (!this.regions.contains(actRegion)){
					this.regions.add(actRegion);
				}
			}
		}
		if (this.tradeAreas==null){
			this.tradeAreas = new ArrayList<TradeArea>();
		} else {
			this.tradeAreas.clear();
		}
		if (this.regions.size()>0){
			// TradeAreaList bauen
			TradeAreaHandler tradeAreaHandler = this.overLord.getTradeAreaHandler();
			for (Region r:this.regions){
				TradeArea tradeArea = tradeAreaHandler.getTAinRange(r);
				if (tradeArea!=null){
					if (!this.tradeAreas.contains(tradeArea)){
						this.tradeAreas.add(tradeArea);
					}
				}
			}
		}
		
	}
	
	/**
	 * überprüft, ob der PferdeMacher auf "Auto" gesetzt ist
	 * wenn ja kommt er auf die Liste der Units, die reisen dürfen
	 * wenn nein, bekommt er seinen Default-Befehl
	 */
	private void putOnDefaultORMoving(Pferde actPferdemacher){
		if (actPferdemacher.isAutomode()){
			if (this.movingPferdeMacher==null){
				this.movingPferdeMacher = new ArrayList<Pferde>();
			}
			if (!this.movingPferdeMacher.contains(actPferdemacher)){
				this.movingPferdeMacher.add(actPferdemacher);
			}
		} else {
			actPferdemacher.alternativOrder();
		}
	}
	
	
	/**
	 * setzt alle verbliebenen AutoMover auf Alternativorder
	 *
	 */
	private void lastMoversOnDefault(){
		if (this.movingPferdeMacher!=null && this.movingPferdeMacher.size()>0){
			for (Pferde actP : this.movingPferdeMacher){
				actP.alternativOrder();
			}
		}
	}
	
	/**
	 * geht die TAs durch, in denen Pferdefänger registriert worden sind
	 * falls movingPferdeMacher > 0 wird jedes TA genauer untersucht 
	 *
	 */
	private void checkTradeAreas(){
		if (this.tradeAreas!=null && this.tradeAreas.size()>0 && this.movingPferdeMacher!=null && this.movingPferdeMacher.size()>0){
			for (TradeArea TA:this.tradeAreas){
				processTA(TA);
			}
		}
	}
	
	/**
	 * checked das TA nach Regionen mit Pferden ohne Pferdemacher drinne
	 * schickt - wenn passend - einen autoMover da hin
	 * @param actTA
	 */
	private void processTA(TradeArea actTA){
		boolean setOuttextFile = false;
		boolean oldScreenOut = outText.isScreenOut();
		ArrayList<Region> actRegions = new ArrayList<Region>();
		for (Iterator<TradeRegion> iter = actTA.getRegionIterator();iter.hasNext();){
			TradeRegion actTR = (TradeRegion)iter.next();
			Region actR = actTR.getRegion();
			actRegions.add(actR);
		}
		Collections.sort(actRegions, new PferdeRegionComparator());
		for (Iterator<Region> iter = actRegions.iterator();iter.hasNext();){
			Region actR = (Region)iter.next();
			if (actR.getHorses()>0 && !this.regions.contains(actR)){
				if (!setOuttextFile && !this.reportOFF){
					outText.setFile("Pferde_" + actTA.getName());
					outText.setScreenOut(false);
					setOuttextFile=true;
					outText.addOutLine("Bearbeite TA " + actTA.getName());
				}
				if (!this.reportOFF) {
					outText.addOutLine("Bearbeite Region " + actR.toString() + " (Pferde:" + actR.getHorses() + ")");
				}
				processTA_Region(actR, actTA);
			}
		}
		
		if (setOuttextFile && !this.reportOFF){
			outText.setFileStandard();
			outText.setScreenOut(oldScreenOut);
		}
	}
	
	/**
	 * versucht, im TA einen automover zu finden und in der Region zu schicken
	 * ...definitiv gibt es hier noch keinen PferdeMacher
	 * @param actR
	 * @param actTA
	 */
	private void processTA_Region(Region actR,TradeArea actTA){
		for (Pferde actMover:this.movingPferdeMacher){
			Boolean istGeeignet = true;
			String grund = "";
			// im gleichen TA?
			if (!actTA.contains(actMover.region())){
				istGeeignet=false;
				grund = "anderes TA";
			}
			// mindestauslastung erfüllt
			if (istGeeignet && !actMover.isInMinAuslastung(actR.getHorses())){
				istGeeignet=false;
				grund = "MindestAuslastung";
			}
			
			if (istGeeignet && actMover.scriptUnit.isInsekt() && actR.getRegionType().getName().equalsIgnoreCase("Gletscher")) {
				istGeeignet=false;
				grund = "Insekt kann nicht in Gletscher";
			}
			
			
			if (istGeeignet){
				// ist geeignet!
				// Kommen wir hin?
				GotoInfo gotoInfo = FFToolsRegions.getPathDistLandGotoInfo(overLord.getScriptMain().gd_ScriptMain, actMover.region().getCoordinate(), actR.getCoordinate(), false, actMover.scriptUnit.isInsekt());
				if (gotoInfo!=null && gotoInfo.getAnzRunden()>0){
					// wir kommen hin
					gotoInfo = FFToolsRegions.makeOrderNACH(actMover.scriptUnit, actMover.region().getCoordinate(),actR.getCoordinate(), true,"Pferdemanager");
					// aus der Liste der Mover entfernen
					this.movingPferdeMacher.remove(actMover);
					if (!this.reportOFF) {
						outText.addOutLine("dieser Region als Pferdemacher zugeordnet: " + actMover.unitDesc());
					}
					actMover.setGotoInfo(gotoInfo);
					// noch nen Pferde MPR hinzufügen
					if (actMover.scriptUnit.getSkillLevel("Reiten")>0) {
					  MatPoolRequest MPR = new MatPoolRequest(actMover,actMover.scriptUnit.getUnit().getModifiedPersons(), "Pferd", 20, "Pferdefänger unterwegs" );
					  actMover.addMatPoolRequest(MPR);
					}
					
					
					break;
				} else {
					if (!this.reportOFF) {
						outText.addOutLine("geeignet, aber keinen Weg gefunden?: " + actMover.unitDesc());
					}
					
				}
			} else {
				if (!this.reportOFF) {
					outText.addOutLine("als ungeeignet erkannt: " + actMover.unitDesc() + " (" + grund + ")");
				}
			}
		}
		if (!this.reportOFF) {
			outText.addOutLine("Ende bei Region " + actR.toString());
		}
	}
	
	
	/**
     * Meldung an den Overlord
     * @return
     */
    public int[] runAt(){
    	return runners;
    }
}
