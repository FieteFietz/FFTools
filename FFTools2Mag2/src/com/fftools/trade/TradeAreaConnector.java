package com.fftools.trade;

import java.util.ArrayList;
import java.util.StringTokenizer;

import magellan.library.CoordinateID;
import magellan.library.Region;
import magellan.library.rules.ItemType;
import magellan.library.utils.Direction;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.scripts.Ontradeareaconnection;
import com.fftools.scripts.Vorrat;
import com.fftools.utils.FFToolsRegions;

public class TradeAreaConnector {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private ScriptUnit SU1 = null;
	private ScriptUnit SU2 = null;
	
	private TradeArea TA1 = null;
	private TradeArea TA2 = null;
	private String Name = "not named";
	
	
	

	private TradeAreaHandler TAH=null;
	
	private boolean isValid = false;
	

	private ArrayList<TAC_Transfer> transfersTo1 = new ArrayList<TAC_Transfer>();
	private ArrayList<TAC_Transfer> transfersTo2 = new ArrayList<TAC_Transfer>();
	
	private ArrayList<TAC_usage> usagesIn1 = new ArrayList<TradeAreaConnector.TAC_usage>();
	private ArrayList<TAC_usage> usagesIn2 = new ArrayList<TradeAreaConnector.TAC_usage>();
	
	private int dist = -1;
	private int speed = 6;
	private int TAC_vorratsfaktor = 2;
	private int prio=50;
	private int prioTM=99;
	
	private int maxNumberOfMoversIfIdle=1;
	
	
	private ArrayList<Ontradeareaconnection> movers = new ArrayList<Ontradeareaconnection>();
	private String moverInfo = "not built (no mover info)";
	private String weightInfo = "not built (no weight info)";
	
	public String getMoverInfo() {
		return moverInfo;
	}
	
	public TradeArea getTA1() {
		return TA1;
	}

	public TradeArea getTA2() {
		return TA2;
	}

	
	public boolean isValid() {
		return isValid;
	}
	
	public String getName() {
		return Name;
	}
	
	public TradeAreaConnector(ScriptUnit u1,ScriptUnit u2,String _Name,TradeAreaHandler _TAH){
		this.TAH = _TAH;
		this.SU1 = u1;
		this.SU2 = u2;
		
		this.TA1 = TAH.getTAinRange(this.SU1.getUnit().getRegion());
		this.TA2 = TAH.getTAinRange(this.SU2.getUnit().getRegion());
		
		this.Name = _Name;
		
		if (this.TA1!=null && this.TA2!= null && !this.TA1.equals(this.TA2)){
			isValid = true;
		}
		
		if (isValid && this.Name.length()<2){
			isValid = false;
		}
		if (isValid && this.Name.equalsIgnoreCase("not named")){
			isValid = false;
		}
		
		if (this.getDist()<=0){
			isValid = false;
		}
		
	}

	public ScriptUnit getSU1() {
		return SU1;
	}

	public ScriptUnit getSU2() {
		return SU2;
	}
	
	/**
	 * kleine private Klasse zum Erfassen von Transfers zwischen TAs
	 * @author Fiete
	 *
	 */
	private class TAC_Transfer{
		public ItemType itemType;
		// Menge nach relativer Distru
		public int amount_1 = 0;
		// Menge nach finaler Distru
		public int amount_2 = 0;
		
		public TAC_Transfer(ItemType _itemType,int _amount_1,int _amount_2){
			this.itemType = _itemType;
			this.amount_1 = _amount_1;
			this.amount_2 = _amount_2;
		}	
	}
	
	/**
	 * Kleine private Klasse zum Verwalten von Zusätzlichen Requests der Mover
	 * in den Startregionen
	 * @author Fiete
	 *
	 */
	private class TAC_usage{
		private String Name = "";
		private int Menge = 0;
		private int prio = 0;
		private int prioTM = 0;
		
		public TAC_usage(String _Name,int _Menge,int _Prio, int _PrioTM){
			this.Name = _Name;
			this.Menge = _Menge;
			this.prio = _Prio;
			this.prioTM = _PrioTM;
			
		}

		public String getName() {
			return Name;
		}

		public int getMenge() {
			return Menge;
		}

		public int getPrio() {
			return prio;
		}
		
		public int getPrioTM() {
			return prioTM;
		}
		
		public void MengeRequested(int _abgang) {
			this.Menge -= _abgang;
			if (this.Menge<0) {
				this.Menge=0;
			}
		}
		
	}
	
	/**
	 * ergänzt einen Transfer
	 * @param targetTA
	 * @param itemType
	 * @param amount
	 */
	public void addTransfer(TradeArea targetTA,ItemType itemType, int amount){
		TAC_Transfer actT = new TAC_Transfer(itemType,amount,0);
		if (targetTA.equals(this.getTA1())){
			this.transfersTo1.add(actT);
		}
		if (targetTA.equals(this.getTA2())){
			this.transfersTo2.add(actT);
		}
	}
	
	public void addUsage(TradeArea sourceTA, String Ware, int Menge, int Prio, int PrioTM){
		TAC_usage actU = new TAC_usage(Ware, Menge, Prio, PrioTM);
		if (sourceTA.equals(this.getTA1())){
			this.usagesIn1.add(actU);
		}
		if (sourceTA.equals(this.getTA2())){
			this.usagesIn2.add(actU);
		}
	}
	
	
	/**
	 * liefert den entsprechenden Betrag des Transfers in diesem TAC zum TA!
	 * @param targetTA
	 * @param itemType
	 * @return
	 */
	public int getTransferAmount(TradeArea targetTA, ItemType itemType,int whichAmount){
		ArrayList<TAC_Transfer> myList = null;
		if (targetTA.equals(this.getTA1())){
			myList = this.transfersTo1;
		}
		if (targetTA.equals(this.getTA2())){
			myList = this.transfersTo2;
		}
		if (myList==null || myList.size()==0){
			return 0;
		}
		for (TAC_Transfer actT:myList){
			if (actT.itemType.equals(itemType)){
				if (whichAmount==1){
					return actT.amount_1;
				}
				if (whichAmount==2){
					return actT.amount_2;
				}
				return 0;
			}
		}
		return 0;
	}
	
	/**
	 * erstellt die neuen Vorratsanfragen für alle Transfers
	 */
	public void process_Transfers(){
		// outText.addOutLine("!!!debug: doing transfers for TAC " + this.Name, true);
		if (this.transfersTo1.size()>0){
			process_Transfers_Dir(this.transfersTo1,this.getSU2());
		}
		if (this.transfersTo2.size()>0){
			process_Transfers_Dir(this.transfersTo2,this.getSU1());
		}
	}
	
	/**
	 * Erstellt die neuen Vorratsanfragen für eine Richtung des TAC
	 * @param transfers
	 * @param sourceSCU
	 */
	private void process_Transfers_Dir(ArrayList<TAC_Transfer> transfers, ScriptUnit sourceSCU){
		for (TAC_Transfer actTF : transfers){
			ArrayList<String> order = new ArrayList<String>(); 
			order.add("source=TAC");
			order.add("Ware=" + actTF.itemType.getName());
			int summe = (actTF.amount_1 * this.getDist()) * 2 * TAC_vorratsfaktor;
			order.add("Summe=" + summe);
			order.add("proRunde=" + actTF.amount_1);
			order.add("prio=" + prio);
			order.add("prioTM=" + prioTM);
			
			
			Vorrat vorrat = new Vorrat();
			
			this.TAH.scriptMain.getOverlord().addOverlordInfo(vorrat);
			vorrat.setScriptUnit(sourceSCU);
			if (this.TAH.scriptMain.client!=null){
				vorrat.setClient(this.TAH.scriptMain.client);
			}
			vorrat.setGameData(this.TAH.getData());
			vorrat.setArguments(order);
			sourceSCU.addAScriptNow(vorrat);
			
			// wird bei 190 im script vorrat aufgerufen
			// vorrat.vorMatpool();
			
			sourceSCU.addComment("TAC: Vorratsscript hinzugefügt: " + order.toString(),false);
		}
	}
	
	
	private int getDist(){
		if (this.dist==-1){
			// this.dist = FFToolsRegions.getShipPathSizeTurns_Virtuell(this.getSU1().getUnit().getRegion().getCoordinate(), this.getSU2().getUnit().getRegion().getCoordinate(), this.TAH.getData(), speed);
		}   this.dist = FFToolsRegions.getShipPathSizeTurns_Virtuell_Ports(this.getSU1().getUnit().getRegion().getCoordinate(), Direction.INVALID, this.getSU2().getUnit().getRegion().getCoordinate(), this.TAH.getData(), speed,null);
		return this.dist;
	}
	
	/**
	 * fügt einen Mover dieser TAC hinzu
	 * @param onTAC
	 */
	public void addMover(Ontradeareaconnection onTAC){
		if (this.movers.contains(onTAC)){
			onTAC.doNotConfirmOrders("!!!TAC:addMovver: bereits eingetragen!->unbestätigt");
			return;
		}
		this.movers.add(onTAC);
	}
	
	/**
	 * liefert summe aller registrierten Mover-kapas
	 * @return
	 */
	public int getOverallMoverKapa(){
		int erg = 0;
		if (this.movers.size()==0){
			this.moverInfo = "keine Mover registriert";
			return 0;
		}
		this.moverInfo = "";
		for (Ontradeareaconnection onTAC:this.movers){
			erg +=onTAC.getKapa();
			if (moverInfo.length()>0){
				moverInfo +=",";
			}
			moverInfo+=onTAC.unitDesc() + "(" + onTAC.getKapa() + ")";
		}
		moverInfo = "Summe Transporter " + erg + "GE: " + moverInfo;
		return erg;
	}
	
	/**
	 * Setzt die Requests um, damit der Mover nach toDir aufbrechen kann
	 * @param toDir
	 * @param onTAC
	 */
	public void processMoverRequests(int toDir,Ontradeareaconnection onTAC){
		ArrayList<TAC_Transfer> transfers = null;
		String target = "";
		if (toDir==1){
			transfers = this.transfersTo1;
			target = this.getTA1().getName();
		}
		if (toDir==2){
			transfers = this.transfersTo2;
			target = this.getTA2().getName();
		}
		
		if (transfers==null){
			return;
		}
		
		int transportFaktor = this.dist * 2;
		
		for (TAC_Transfer actTransfer : transfers){
			int actSumme = actTransfer.amount_1 * transportFaktor;
			// anteil des Movers
			actSumme = (int)Math.floor((double)actSumme * onTAC.getAnteil());
			// Prio
			int actPrio = this.prio + 1;
			// Kommentar
			String comment = "TAC nach " + target;
			// Request basteln
			MatPoolRequest MPR = new MatPoolRequest(onTAC, actSumme, actTransfer.itemType.getName(), actPrio, comment);
			onTAC.addMatPoolRequest(MPR);
			onTAC.addComment("TAC: " + actSumme + " " + actTransfer.itemType.getName() + " nach " + target + " mit Prio " + actPrio + " angefordert");
		}
		
	}
	
	/**
	 * Setzt die Requests um, die der Mover in inDir mitnehmen kann
	 * @param inDir
	 * @param onTAC
	 */
	public void processMoverUsages(int inDir,Ontradeareaconnection onTAC){
		ArrayList<TAC_usage> transfers = null;
		String target = "";
		if (inDir==1){
			transfers = this.usagesIn1;
			target = this.getTA2().getName();
		}
		if (inDir==2){
			transfers = this.usagesIn2;
			target = this.getTA1().getName();
		}
		
		if (transfers==null){
			return;
		}
		
		
		
		for (TAC_usage actTransfer : transfers){
			// Kommentar
			if (actTransfer.getMenge()>0) {
				String comment = "TAC nach " + target + " auf " + onTAC.getMyTAC().getName();
				// Request basteln
				MatPoolRequest MPR = new MatPoolRequest(onTAC, actTransfer.getMenge(), actTransfer.getName(), actTransfer.getPrio(), comment);
				MPR.setPrioChange(false);
				MPR.setPrioTM(actTransfer.getPrioTM());
				onTAC.addMatPoolRequest(MPR);
				onTAC.addComment("TAC-Usage: " + actTransfer.getMenge() + " " + actTransfer.getName() + " nach " + target + " mit Prio " + actTransfer.getPrio() + " angefordert (PrioTM=" + actTransfer.getPrioTM() +  ")");
				// actTransfer.MengeRequested(actTransfer.getMenge());
			} else {
				onTAC.addComment("TAC-Usage: bereits maximal angefordert " + actTransfer.getName() + " nach " + target + " mit Prio " + actTransfer.getPrio() + " angefordert (PrioTM=" + actTransfer.getPrioTM() +  ")");
			}
		}
		
	}
	
	
	public String toString(){
		return this.Name + " (" + this.getSU1().unitDesc() + "->" + this.getSU2().unitDesc()+", speed: " + this.speed + ", dist: " + this.dist + ")";
	}
	
	/**
	 * berechnet benötigte Menge an Transportkapa
	 * setzt WeightInfo
	 * @return
	 */
	public int getNeededGE(){
		// maximales Richtungsgewicht pro Runde
		// *2 
		// * dist
		// Richtung 1
		int w1 = 0;
		int w2 = 0;
		if (this.transfersTo1!=null && this.transfersTo1.size()>0){
			for (TAC_Transfer actT:this.transfersTo1){
				w1+=actT.amount_1 * (int)actT.itemType.getWeight();
			}
		}
		if (this.transfersTo2!=null && this.transfersTo2.size()>0){
			for (TAC_Transfer actT:this.transfersTo2){
				w2+=actT.amount_1 * (int)actT.itemType.getWeight();
			}
		}
		this.weightInfo = "No cargo";
		if ((w1==0) && (w2 == 0)){
			return 0;
		}
		
		this.weightInfo = w1 + "GE -> " + this.getTA1().getName() + "," + w2 + "GE -> " + this.getTA2().getName();
		w1 = Math.max(w1, w2);
		this.weightInfo += "; Dist: " + this.dist + "; Speed: " + this.speed;
		w1 = w1 * 2 * this.dist;
		return w1;
	}

	public String getWeightInfo() {
		return weightInfo;
	}
	
	/**
	 * eine Zeile...info
	 */
	public void informUsShort(){
		outText.addOutChars(this.Name, 30);
		outText.addOutChars(": Mover:");
		// Anzahl movers
		outText.addOutChars(this.movers.size()+"", 2);
		outText.addOutChars(" kapa ist:");
		// Anzahl Kapa on it
		int kapa_ist = this.getOverallMoverKapa();
		outText.addOutChars(kapa_ist+"", 10);
		outText.addOutChars(" kapa soll:");
		int kapa_soll = this.getNeededGE();
		outText.addOutChars(kapa_soll+"", 10);
		if (kapa_soll>0){
			int proz = (int)Math.floor(((double)kapa_ist / (double)kapa_soll) * 100);
			outText.addOutChars(proz + "%", 8);
		}
		outText.addNewLine();
		
		if (kapa_soll>kapa_ist){
			this.SU1.doNotConfirmOrders("!!! TAC " + this.Name + " fehlt Kapa: nur " + kapa_ist + " von " + kapa_soll + " verfügbar!");
		}
		
		if (kapa_soll==0 && this.movers.size()>this.maxNumberOfMoversIfIdle){
			this.SU1.doNotConfirmOrders("!!! TAC " + this.Name + " nutzt " + this.movers.size() + " Schiffe, obwohl derzeit kein Bedarf existiert! (max " + this.maxNumberOfMoversIfIdle + ")");
		}
		
		
	}
	
	
	public void setMaxNumberOfMoversIfIdle(int number){
		if (number>=1){
			this.maxNumberOfMoversIfIdle=number;
		} 
	}
	
	/*
	 * 
	 */
	public void clear_TAC_MapLines(){
		if (this.SU1==null || this.SU2==null){
			return;
		}
		Region r = this.SU1.getUnit().getRegion();
		FFToolsRegions.remove_MapLines_From_Region(r, TradeAreaHandler.MAPLINE_TAG_ID);
		
	}
	
	/*
	 * bereitet eine MapLine von SU1 nach SU2 vor
	 */
	public void create_TAC_MapLine(){
		if (this.SU1==null || this.SU2==null){
			return;
		}
		
		
		FFToolsRegions.addMapLine(this.SU1.getUnit().getRegion(), this.SU2.getUnit().getRegion().getID(), 153, 255, 204, 5, TradeAreaHandler.MAPLINE_TAG_ID);
		
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	
}
