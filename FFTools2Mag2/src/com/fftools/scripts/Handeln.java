package com.fftools.scripts;


import java.util.Iterator;
import java.util.LinkedList;

import magellan.library.Item;
import magellan.library.LuxuryPrice;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.SkillType;

import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.trade.TradeUtils;
import com.fftools.trade.Trader;
import com.fftools.utils.FFToolsUnits;



public class Handeln extends TradeAreaScript{
	
	private Region r = null;
	
	public static final int Durchlauf_VorMatpool = 32;
	public static final int Durchlauf_NachMatpool = 360;
	public static final int Durchlauf_NachMatpool2 = 810;
	
	
	private int[] runsAt = {Durchlauf_VorMatpool,Durchlauf_NachMatpool,Durchlauf_NachMatpool2};
	
	// private final int DEFAULT_REQUEST_PRIO = 10;  moved to trader
	private String kommentar = "Handel";
	private String kommentar_silberRequest = "Handelseinkauf";
	
	// ein ganz simpler merker
	private int requested_silver = -1;
	
	public int getRequested_silver() {
		return this.requested_silver;
	}
	
	private LuxuryPrice buyingPrice = null;
	private int adjustedBuyAmount = -1;
	// private int prio = DEFAULT_REQUEST_PRIO;   moved to trader
	
	private int overallSellAmount = 0;
	

	private String default_silbervorrat_kommentar = "Händler - SilberDepot";

	private LinkedList<MatPoolRequest> silberDepotMPRs = null;
	
	
	
	private boolean doSomething = false; 
	
	
	
	
	/**
	 * Parameterloser Constructor
	 * Drinne Lassen fuer die Instanzierung des Objectes
	 */
	
	public Handeln() {
		super.setRunAt(this.runsAt);
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
		
		
		
		switch (scriptDurchlauf){
		
		case Durchlauf_VorMatpool:this.vorMatpool();break;

		case Durchlauf_NachMatpool:this.nachMatPool();break;
		
		case Durchlauf_NachMatpool2:this.nachMatPool2();break;
		
		}
	}
	
	/**
	 * requested was man verkaufen kann
	 * (in den nächsten X runden...)
	 * 
	 *
	 */
	private void vorMatpool(){
		
		super.addVersionInfo();
		
		// falls Trader noch unbekannt (sollte nicht sein)
		// Trader sollten vorher beim Durchlaufen der scriptunits
		// in scriptmain bereits gesetzt sein, inkl TradeAreas usw
		// jetzt einen setzen
		
		this.getTrader().setScript(this);
		this.getTrader().init();
		
		if (this.getTrader().isLernen()){
			return;
		}
		
		if (this.getTrader().getBuy()==null){
			// kann nix kaufen und verkaufen
			if (this.getTrader().isKaufen()||this.getTrader().isVerkaufen()){
				this.addComment("!!! kein Handel möglich - kein Handelsgut.");
				outText.addOutLine("!!!Händler in Region ohne Handelsgut:" + this.unitDesc(), true);
			}
			return;
		}
		
		
		
		// Region...
		r = this.scriptUnit.getUnit().getRegion();
		if(r.getPrices() != null) {
			String infoString = "";
			for(Iterator<LuxuryPrice> iter = r.getPrices().values().iterator(); iter.hasNext();) {
				LuxuryPrice p = (LuxuryPrice) iter.next();

				if(p.getPrice() > 0) {
					// hier was zum verkaufen...
					// FF 20070109 eventuell soll er gar nicht verkaufen...
					if (this.getTrader().isVerkaufen()){
						ItemType lux = p.getItemType();
						int amount = r.maxLuxuries() * this.getTrader().getVerkaufsvorratsrunden();
						if (infoString.length()>1){
							infoString += ",";
						}
						infoString += amount + " " + lux.getName();
						MatPoolRequest HR = new MatPoolRequest(this,amount,lux.getName(),this.getTrader().getSellItemRequestPrio(),kommentar);
						HR.setOnlyRegion(true);
						this.addMatPoolRequest(HR);
					}
				} else {
					// neu: kein Silber anfordern
					this.buyingPrice = p;
				}
			}
	        // super.addComment("Zum Verkauf benötigt: " + infoString, true);
		} else {
			// keine Region prices
			// Meldung wenn er verkaufen oder kaufen sollte..sonst nicht
			if (this.getTrader().isKaufen() || this.getTrader().isVerkaufen()){
				outText.addOutLine("!!!Handeln: keine Preisinfos verfügbar: " + this.unitDesc());
				super.scriptUnit.doNotConfirmOrders("keine Preisinfos verfügbar");
			}
		}
	}
	
	private void nachMatPool(){
		// erstmal verkaufen, was wir so haben
		// mitzählen, ob Talente reichen....
		// feststellen, ob er überhaupt etwas machen kann...
		if (this.getTradeArea()!=null && this.getTrader()!=null ){
			if (this.getTrader().getProfit()>0) {
				this.getTradeArea().setProfit(this.getTrader().getProfit());
			}
		} else {
			this.addComment("Profit konnte nicht gesetzt werden - TA nicht gefunden");
		}
		
		// user info hier?
		this.showTradeAreaInfo();
		
		if (this.getTrader().isLernen()){
			return;
		}
		
		if (this.getTrader().getBuy()==null){
			return;
		}
		
		
		
		int neededTalent = 0;
		// Verkaufen..
		if(r.getPrices() != null && this.getTrader().isVerkaufen()) {
			for(Iterator<LuxuryPrice> iter = r.getPrices().values().iterator(); iter.hasNext();) {
				LuxuryPrice p = (LuxuryPrice) iter.next();
				if(p.getPrice() > 0) {
					Item item = this.scriptUnit.getModifiedItem(p.getItemType());
					int amount = 0;
					if (item!=null){
						amount = item.getAmount();
					}
					if (amount>0){
						// OK...hier können wir was verkaufen
						super.addOrder("VERKAUFEN ALLES " + p.getItemType().getName(),true);
						doSomething = true;
						// Talente mitzählen...
						if (amount>r.maxLuxuries()){amount=r.maxLuxuries();}
						overallSellAmount+=amount;
						// neededTalent += (int)Math.ceil((double)amount/10);
						FFToolsUnits.leaveAcademy(this.scriptUnit, " Händler verkauft und verlässt Aka");
					}
				}
			}
		}
		
		this.addComment("Händler: erwartete Gesamtverkaufsmenge: " + overallSellAmount);
		// so....kaufen...
		// wat haben wir denn so an Geld -> später, was wir so kriegen können
		// jetzt fragen wir uns, ob er noch genügend Talente hat
		if (this.getTrader().isKaufen()){
			// Talentpunkte der unit ermitteln...
			SkillType handelsSkillType =  super.gd_Script.getRules().getSkillType("Handeln");
			Skill handelsSkill =  this.scriptUnit.getUnit().getModifiedSkill(handelsSkillType);
			int persons = this.scriptUnit.getUnit().getModifiedPersons();
			int availableTalentMenge = 0;
			if (handelsSkill!=null){
				availableTalentMenge = handelsSkill.getLevel() * persons * 10;
			}
			
			// wieviel soll gekauft werden?
			int kaufMenge = this.getTrader().getBuy().getAmount();
			if (this.getTrader().getBuyPolicy() == Trader.trader_buy_setManager){
				// soll suggested übernehmen, egal wieviel
				int vorMenge = getTradeArea().suggestedBuyAmount(getTradeRegion(),true,null);
				if (vorMenge>0){
					kaufMenge = vorMenge;
				} else if (vorMenge==-1){
					addComment("!! Es gibt keinen Grund, hier etwas einzukaufen!!");
					kaufMenge=0;
				} else {
					this.scriptUnit.doNotConfirmOrders("!!unerwartete vorgeschlagene Einkaufsmenge!!");
				}
			}
			if (this.getTrader().getBuyPolicy() == Trader.trader_buy_Max){
				// soll suggested übernehmen, mindestens aber r.max
				int vorMenge = getTradeArea().suggestedBuyAmount(getTradeRegion(),true,null);
				if (vorMenge>0){
					// nur wenn überkauf
					// unterkauf nicht
					if (vorMenge>kaufMenge){
						kaufMenge = vorMenge;
					}
				} else if (vorMenge==-1){
					addComment("!! Es gibt keinen Grund, hier etwas einzukaufen!!");
					kaufMenge=0;
				} else {
					this.scriptUnit.doNotConfirmOrders("!!unerwartete vorgeschlagene Einkaufsmenge!!");
				}
			}
			
			this.getTrader().getBuy().setAmount(kaufMenge);
			
			// hier kleiner Schnitt.....nicht schön.
			
			int sollKauf = this.getTrader().getBuy().getAmount();
			// wieviel Talent dafür benötigt?

			// durch Verkäufe verbrauchtes Talent abziehen vom avail
			availableTalentMenge -= overallSellAmount;
			
			if (sollKauf>availableTalentMenge){
				// wenigstens ne Info schreiben...
				int fehlendeTalente = (int)Math.ceil((double)(sollKauf- availableTalentMenge)/10);
				this.addComment("zu wenig Talentpunkte zum Kauf: " + availableTalentMenge + "/" + sollKauf + " (Es fehlen " + fehlendeTalente + " Level)");
				this.addOutLine("!Handeln: zu wenig Talentpunkte zum Kauf: (" + availableTalentMenge + "/" + sollKauf + ", Es fehlen " + fehlendeTalente + " Level): " + this.unitDesc());
				if (availableTalentMenge>0){
					// ok, wir können noch ein wenig einkaufen
					// wieviel ist damit möglich?
					int availKauf =availableTalentMenge;
					// adjusted entsprechend setzen
					this.adjustedBuyAmount = Math.min(sollKauf, availKauf);
					this.addComment("neue Einkaufsmenge wegen Talent:" + this.adjustedBuyAmount);
				} else {
					// nix zu machen, wir können nicht kaufen
					this.adjustedBuyAmount = 0;
				}
				kaufMenge = this.adjustedBuyAmount;
				
			} else {
				// alles schön, genug vorhanden
				this.adjustedBuyAmount = sollKauf;
			}
			
			// Kauf setzen
			this.getTrader().getBuy().setAmount(this.adjustedBuyAmount);
			// adjusted silber anfordern
			int amount = TradeUtils.getPrice(kaufMenge, Math.abs(this.buyingPrice.getPrice()), r.maxLuxuries());
			this.requested_silver = amount;
			MatPoolRequest HR = new MatPoolRequest(this,amount,"Silber",this.getTrader().getSilverRequestPrio(),kommentar_silberRequest);
			super.addComment("Einkaufssilber: " + amount + " Silber (" + kaufMenge + " " + this.buyingPrice.getItemType().getName() + ")",true);
			this.addMatPoolRequest(HR);
			
			// Silvervorrat
			
			
			if (this.getTrader().getAnzahl_silber_runden()>0 && reportSettings.getOptionBoolean("DepotSilber", this.region())){
				super.setPrioParameter(this.getTrader().getSilverRequestPrio()-2,-0.5,0,2);
				
				int kostenProRunde = this.requested_silver;
			
				this.addComment("SilberVorrat(Depot): fordere " + this.getTrader().getAnzahl_silber_runden() + " Runden a " + kostenProRunde + " Silber an.");
				
				
				this.silberDepotMPRs = new LinkedList<MatPoolRequest>();
				// los gehts
				for (int i = 1;i<=this.getTrader().getAnzahl_silber_runden();i++){
					int actPrio = super.getPrio(i-1);
					MatPoolRequest actMPR = new MatPoolRequest(this,kostenProRunde,"Silber",actPrio,this.default_silbervorrat_kommentar);
					this.addMatPoolRequest(actMPR);
					this.silberDepotMPRs.add(actMPR);
				}
			} else {
				super.addComment("Debug: anzahlSilberRunden: " + this.getTrader().getAnzahl_silber_runden());
				if (reportSettings.getOptionBoolean("DepotSilber", this.region())){
					super.addComment("Debug: SilberDepot aktiv");
				} else {
					super.addComment("Debug: SilberDepot inaktiv");
				}
				super.addComment("Keine SilberDepotFunktion aktiviert");
			}
			
		}	
	}
	
	
	private void privat_return(){
		if (!doSomething){
			super.addComment("Händler hat keinen Auftrag und lernt.");
			// super.lerneTalent("Handeln", true);
			this.addOrder("Lernen Handeln", true);
		}
	}
	
	/**
	 * Bekommen wir genügend Silber?
	 * Sonst warnen
	 */
	private void nachMatPool2(){
		if (!this.getTrader().isKaufen()){
			privat_return();
			return;
		}
		if (this.getTrader().isLernen()){
			privat_return();
			return;
		}
		if (this.getTrader().getBuy()==null){
			privat_return();
			return;
		}
		
		if (this.getTrader().getBuy().getAmount()<=0){
			super.addComment("Keinen Kaufbefehl gesetzt. Kein Bedarf", true);
			privat_return();
			return;
		}
			
		ItemType silver =  super.gd_Script.getRules().getItemType("Silber");
		Item silverItem = this.scriptUnit.getModifiedItem(silver);
		int silverAmount = 0;
		if (silverItem != null){
			silverAmount = silverItem.getAmount();
		}
		if (this.requested_silver>-1 && silverAmount<this.requested_silver){
			// nicht genügend Silber erhalten...
			// wieviel kann man damit kaufen?
			int changedAmount = TradeUtils.getMenge(silverAmount, Math.abs(this.buyingPrice.getPrice()), r.maxLuxuries());
			if (changedAmount>0){
				// kann nur vermindert kaufen
				super.addComment("Silberknappheit. Sollkauf: " + this.getTrader().getBuy().getAmount() + ", wegen Silberknappheit nur: " + changedAmount, true);
				outText.addOutLine("!!!*Handeln: Silberknappheit->" + changedAmount + "/" + this.getTrader().getBuy().getAmount() + ": " + this.unitDesc());
				super.addOrder("KAUFEN " + changedAmount + " " + this.getTrader().getBuy().getItemType().getName(),true);
				doSomething=true;
			} else {
				// kann gar nix kaufen
				outText.addOutLine("!!!** Handeln: Kein Kaufsilber: " + this.unitDesc());
				super.scriptUnit.doNotConfirmOrders("Kein Silber. Sollkauf: " + this.getTrader().getBuy().getAmount());
			}
		} else {
			// gehe davon aus, genug Silber zu haben
			if (this.getTrader().getBuy().getAmount()>0){
				super.addOrder("KAUFEN " + this.getTrader().getBuy().getAmount() + " " + this.getTrader().getBuy().getItemType().getName(),true);
				doSomething=true;
				FFToolsUnits.leaveAcademy(this.scriptUnit, " Händler kauft und verlässt Aka");
			}
		}
		
		
		// Silbervorrsat info
		this.SiberVorratInfo();
		
		// überhaupt was getan?
		// dann bisher kein langer Befehl erfolgt..schlagen wir lernen vor
		
		privat_return();
	}
	
	/**
	 * rein informativ für die unit
	 * @param scriptDurchlauf
	 */
	public void SiberVorratInfo(){
		if (this.silberDepotMPRs==null || this.silberDepotMPRs.size()==0){
			return;
		}
		for (Iterator<MatPoolRequest> iter = this.silberDepotMPRs.iterator();iter.hasNext();){
			MatPoolRequest MPR = (MatPoolRequest)iter.next();
			String erg = "DepotSilber gefordert:";
			erg += MPR.getOriginalGefordert() + "(Prio " + MPR.getPrio() + ")";
			erg += ",bearbeitet:" + MPR.getBearbeitet();
			this.addComment(erg);
			
			// Problem
			if (MPR.getPrio()==this.getTrader().getSilverRequestPrio() && MPR.getBearbeitet()<MPR.getOriginalGefordert()){
				// max prio und nicht erfüllt ?!
				// TM Info
				String s2Info = "";
				if (MPR.getTransportRequest()!=null){
					s2Info = " (TM: " + MPR.getTransportRequest().getBearbeitet() + " geladen) ";
				}
				
				this.scriptUnit.doNotConfirmOrders("!!! Händler: DepotSilberWarnung !!!" + s2Info);
				outText.addOutLine("!!! Händler: DepotSilberWarnung: " + s2Info + this.unitDesc(),true);
			}
		}
	}
	
	
	private void showTradeAreaInfo(){
		if (this.getTrader().isUserWishIslandInfo()){
			LinkedList<String> islandInfo = TradeUtils.getIslandInfo(this.scriptUnit.getUnit().getRegion(), this.scriptUnit.getScriptMain());
			if (islandInfo==null){
				addComment("keine Inselinfo verfügbar");
				return;
			}
			if (islandInfo.size()==0){
				addComment("keine Inselinfo verfügbar");
				return;
			}
			for (Iterator<String> iter = islandInfo.iterator();iter.hasNext();){
				String s = (String)iter.next();
				this.addComment(s,false);
			}
		}
		

		LinkedList<String> info = this.getTradeArea().getTradeAreaUnitInfo(this.getTradeRegion());
		if (info==null){return;}
		if (info.size()==0){return;}
		for (Iterator<String> iter = info.iterator();iter.hasNext();){
			String s = (String)iter.next();
			this.addComment(s,false);
		}
	}
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return true;
	}


}
