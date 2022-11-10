package com.fftools.trade;

import java.util.ArrayList;
import java.util.Iterator;

import com.fftools.OutTextClass;
import com.fftools.ReportSettings;
import com.fftools.ScriptUnit;
import com.fftools.scripts.Handeln;
import com.fftools.scripts.Lernfix;
import com.fftools.scripts.MatPoolScript;
import com.fftools.scripts.Script;
import com.fftools.transport.TransportRequest;
import com.fftools.utils.FFToolsOptionParser;
import com.fftools.utils.FFToolsRegions;

import magellan.library.Building;
import magellan.library.Item;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.rules.ItemType;
import magellan.library.rules.Race;
import magellan.library.rules.SkillType;

/**
 * Enthält alles Notwendige zum Handeln...
 * @author Fiete
 *
 */
public class Trader {
	private static final OutTextClass outText = OutTextClass.getInstance();
	public static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private ScriptUnit scriptUnit = null;
	private boolean isAreaOriginSetter = false;
	private String setAreaName = null;
	
	private int DEFAULT_VerkaufsVorratsRunden = 1;
	private int DEFAULT_PrognoseRunden = 2;
	
	private final int DEFAULT_REQUEST_PRIO = 700;
	private final int DEFAULT_REQUEST_SILBER_PRIO = 700;
	
	
	
	private int verkaufsvorratsrunden = -1;
	private int prognoseRunden = -1;

	
	
	public int getPrognoseRunden() {
		return prognoseRunden;
	}


	private int buyPolicy = 0;
	private Item buy = null;
	
	private boolean userWishIslandInfo = false;
	
	public static final int trader_buy_Max = 1;
	public static final int trader_buy_setUser = 2;
	public static final int trader_buy_setManager = 3;
	
	private boolean verkaufen = true;
	private boolean kaufen = true;
	private boolean lernen = false;
	
	private int warteAufBurgBis=0;
	private String LernfixOrder = "";
	
	private int sellItemRequestPrio = DEFAULT_REQUEST_PRIO;
	private int silverRequestPrio = DEFAULT_REQUEST_SILBER_PRIO;
	
	private int default_runden_silbervorrat = 3;
	private int anzahl_silber_runden = default_runden_silbervorrat;
	
	private MatPoolScript handeln = null; 
	
	private Double Profit=0.0;
	
	
	public Trader(ScriptUnit u){
		this.scriptUnit = u;
		
		this.init();
	}
	
	public void setScript(MatPoolScript _handeln){
		this.handeln = _handeln;
	}
	
	public int getRequestedSilver() {
		int erg=0;
		
		if (this.handeln!=null) {
			Handeln H = (Handeln)this.handeln;
			erg = H.getRequested_silver();
		}
		
		return erg;
	}
	
	
	public void init(){
		this.verkaufsvorratsrunden = DEFAULT_VerkaufsVorratsRunden;
		this.prognoseRunden = DEFAULT_PrognoseRunden;
		
		
		// haben wir nen reportweites setting ?
		int externVorrat = reportSettings.getOptionInt("vorrat",this.scriptUnit.getUnit().getRegion());
		if (externVorrat>0){
			this.verkaufsvorratsrunden = externVorrat;
		}
		int externPrognose = reportSettings.getOptionInt("prognose",this.scriptUnit.getUnit().getRegion());
		if (externPrognose>0){
			this.prognoseRunden = externPrognose;
		}
		
		
		
		this.buyPolicy = trader_buy_Max;
		

		// wieviel kaufen? default auf max möglich in region
		Region r = this.getScriptUnit().getUnit().getRegion();
		TradeRegion tR = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeRegion(r);
		tR.addTrader(this);
		ItemType buyItemType = tR.getBuyItemType();
		if (r.getPrices()!=null && buyItemType!=null){
			this.buy = new Item(buyItemType,r.maxLuxuries());
		}
		this.scriptUnit.addComment("TraderInit: starting OrderParser");
		this.parseOrders();
		this.scriptUnit.addComment("TraderInit: finished OrderParser");
		
	}
	
	/**
	 * @return the isAreaOriginSetter
	 */
	public boolean isAreaOriginSetter() {
		return isAreaOriginSetter;
	}
	/**
	 * @param isAreaOriginSetter the isAreaOriginSetter to set
	 */
	public void setAreaOriginSetter(boolean isAreaOriginSetter) {
		this.isAreaOriginSetter = isAreaOriginSetter;
		// es könnte der 2. Eintrag eines Händlers in der Region sein
		// der erste könnte bereits eine TR angelegt haben
		// diesen Fall herausfinden....
		
		TradeRegion tR = this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeRegion(this.scriptUnit.getUnit().getRegion());
		if (!tR.isSetAsTradeAreaOrigin()){
			// da haben wir unseren fall...
			tR.setTradeAreaName(this.getSetAreaName());
			this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().recalcTradeAreas();
		}
	}
	
	
	public void parseOrders(){
		/**
		for(Iterator iter = this.scriptUnit.getUnit().getOrders().iterator(); iter.hasNext();) {
			String s = (String) iter.next();
			this.parseOrder(s);
		}
		*/
		// umstellung auf OptionParser
		if (this.scriptUnit==null){
			outText.addOutLine("!!!!!!TraderParseOrders ScriptUnit=null!!!",true);
			return;
		}
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Handeln");
		if (OP.getOptionString("TradeArea").length()>0){
			this.setAreaName = OP.getOptionString("TradeArea");
			this.setAreaOriginSetter(true);
		}
		if (OP.isOptionString("menge", "auto")){
			this.buyPolicy = trader_buy_setManager;
		} else {
			int amount = OP.getOptionInt("menge", -1);
			if (amount>0 && amount < 2000){
				this.buyPolicy = trader_buy_setUser;
				if (this.buy!=null) {
					this.buy.setAmount(amount);
				}
			} else {
				if (amount!=-1){
					// durgefallen..nix machen..nur meldung
					outText.addOutLine("*Handeln: fehlerhafte Menge: " + this.scriptUnit.getUnit().toString(true));
				}
			}
		}
		
		if (OP.getOptionInt("vorrat", -1)>0){
			int amount = OP.getOptionInt("vorrat", -1);
			// kleiner Sicherheitscheck
			if (amount>0 && amount < 30){
				this.verkaufsvorratsrunden = amount;
			} else {
				// durgefallen..nix machen..nur meldung
				outText.addOutLine("*Handeln: fehlerhafte Vorratsangabe: " + this.scriptUnit.getUnit().toString(true));
			}
		}
		
		if (OP.getOptionInt("prognose", -1)>0){
			int amount = OP.getOptionInt("prognose", -1);
			// kleiner Sicherheitscheck
			if (amount>0 && amount < 50){
				this.prognoseRunden = amount;
			} else {
				// durgefallen..nix machen..nur meldung
				outText.addOutLine("*Handeln: fehlerhafte Angabe der Prognoserunden: " + this.scriptUnit.getUnit().toString(true));
			}
		}
		this.scriptUnit.addComment("Debug: Verbrauchsprognose über " + this.prognoseRunden + " Runden.");
		
		if (OP.getOptionBoolean("inselinfo", false)){
			this.setUserWishIslandInfo(true);
		}
		
		this.sellItemRequestPrio = OP.getOptionInt("prio", DEFAULT_REQUEST_PRIO);
		
		this.silverRequestPrio = OP.getOptionInt("silberprio", DEFAULT_REQUEST_SILBER_PRIO);
		
		this.kaufen = OP.getOptionBoolean("kaufen", true);
		this.verkaufen = OP.getOptionBoolean("verkaufen", true);
		
		// Silbervorrat settings
		
		this.anzahl_silber_runden = this.default_runden_silbervorrat;
		// reportweite settings
		int reportRunden = reportSettings.getOptionInt("DepotSilberRunden", this.scriptUnit.getUnit().getRegion());
		if (reportRunden>0){
			this.anzahl_silber_runden = reportRunden;
			this.scriptUnit.addComment("Reportsettings: DepotSilberRunden=" + reportRunden);
		}
		// aus den Optionen
		int optionRunden = OP.getOptionInt("DepotSilberRunden", -1);
		if (optionRunden>0){
			this.anzahl_silber_runden = optionRunden;
			this.scriptUnit.addComment("Optionen: DepotSilberRunden=" + optionRunden);
		}
		this.scriptUnit.addComment("parsing OK, DepotSilberRunden=" + this.anzahl_silber_runden);
		
		if (OP.getOptionInt("warteAufBurgBis", 0)>0) {
			this.warteAufBurgBis = OP.getOptionInt("warteAufBurgBis", 0);
		}
		if (OP.getOptionInt("waitForTowerUntil", 0)>0) {
			this.warteAufBurgBis = OP.getOptionInt("waitForTowerUntil", 0);
		}
		
		if (OP.getOptionString("Lernplan")!="") {
			this.LernfixOrder="Lernplan=" + OP.getOptionString("Lernplan");
		}
		
		if (OP.getOptionString("Talent")!="") {
			this.LernfixOrder="Talent=" + OP.getOptionString("Talent");
		}
		
		if (OP.getOptionInt("Ziel",0)>0) {
			this.LernfixOrder+=" Ziel=" + OP.getOptionInt("Ziel",0);
		}
		
		if (this.LernfixOrder==""){
			this.LernfixOrder="Talent=Handeln";
		}
		
		// Profit
		if (OP.getOptionDbl("Profit", 0)>0) {
			this.Profit = OP.getOptionDbl("Profit", 0);
			this.scriptUnit.addComment("Umsatzfaktor (war Profit) im TA soll gesetzt werden auf: " + this.Profit);
		}
		
		if (OP.getOptionDbl("Umsatzfaktor", 0)>0) {
			this.Profit = OP.getOptionDbl("Umsatzfaktor", 0);
			this.scriptUnit.addComment("Umsatzfaktor im TA soll gesetzt werden auf: " + this.Profit);
		}
		
		
		// Talentcheck
		int minTalent = OP.getOptionInt("minTalent", 0);
		if (minTalent>0){
			SkillType handelsSkillType =  this.getScriptUnit().getScriptMain().gd_ScriptMain.getRules().getSkillType("Handeln");
			if (handelsSkillType!=null){
				int actTalent = 0;
				Skill handelsSkill = this.scriptUnit.getUnit().getModifiedSkill(handelsSkillType);
				if (handelsSkill!=null){
					actTalent = handelsSkill.getLevel();
				}
				if (actTalent<minTalent){
					// soll lernen
					this.scriptUnit.addComment("mindestTalentwert von " + minTalent + " nicht erreicht. Es wird gelernt.");
					this.lernen = true;
					this.verkaufen = false;
					this.kaufen = false;
					// erledigt ToDo: Lernfix mit Zielangabe
					// this.scriptUnit.addOrder("Lernen Handeln", true);
					this.Lerne();
				} else  {
					this.scriptUnit.addComment("(mindestTalent ist erfüllt)");
				}
			} else {
				outText.addOutLine("!!! Handelstalent nicht erkannt!!!", true);
			}
		} else {
			this.scriptUnit.addComment("(kein mindestTalent angegeben)");
		}
		
		if (!this.lernen) {
			// herausfinden, ob in der Region Burg existiert, oder Insekt handelt (in wüsten und sumpf)
			boolean RegionHatBurg = false;
			Region r = this.scriptUnit.getUnit().getRegion();
			Building b = FFToolsRegions.getBiggestCastle(r);
			if (b!=null) {
				if (b.getSize()>1) {
					RegionHatBurg = true;
				}
			}
			if (RegionHatBurg) {
				this.scriptUnit.addComment("Trader: Burg in der Region vorhanden.");
			} else {
				this.scriptUnit.addComment("Trader: keine Burg in der Region vorhanden.");
				// Auf Rasse der Scriptunit prüfen
				// Insekten können in Wüsten und Sümpfen auch ohne Burgen handeln.
				Race race = this.scriptUnit.getUnit().getRace();
				if (race.getName().equalsIgnoreCase("Insekten")) {
					this.scriptUnit.addComment("Trader: Insekten erkannt, prüfe Regionstyp.");
					boolean RegionIstInsektenfreundlich = false;
					if (r.getRegionType().getName().equalsIgnoreCase("Wüste")) {
						RegionIstInsektenfreundlich = true;
					}
					if (r.getRegionType().getName().equalsIgnoreCase("Sumpf")) {
						RegionIstInsektenfreundlich = true;
					}
					if (RegionIstInsektenfreundlich) {
						this.scriptUnit.addComment("Trader: in Region können Insekten handeln");
						RegionHatBurg=true;
					} else {
						this.scriptUnit.addComment("Trader: in Region können KEINE Insekten handeln");
					}
				}
			}
			if (!RegionHatBurg) {
				this.verkaufen = false;
				this.kaufen = false;
				// In der Region kann derzeit nicht gehandelt werden: keine Burg und keine Insekten in Sumpf/Wüste
				// Verhalten normal: Abbruch des Traders - der soll nicht bestätigt werden
				// Ausnahme: warteAufBurgBis ist gesetzt, dann Lernen, sogar mit Lernpplan bzw Talent m it Ziel
				if (this.warteAufBurgBis>this.getScriptUnit().getScriptMain().gd_ScriptMain.getDate().getDate()) {
					// OK, weiter warten und Lernen
					this.scriptUnit.addComment("Trader: warten erkannt bis Runde " + this.warteAufBurgBis + ", werde Lernen");
					this.Lerne();
					this.lernen=true;
				} else {
					// nicht OK...Abbrechen
					this.scriptUnit.doNotConfirmOrders("!!!Trader: kein Handel möglich, kein Warten konfiguriert: unbestätigt!!!");
				}
			}
		}
	}

	
	private void Lerne() {
		this.scriptUnit.addComment("Lernfix wird initialisiert mit dem Parameter: " + this.LernfixOrder);
		Script L = new Lernfix();
		ArrayList<String> order = new ArrayList<String>();
		order.add(this.LernfixOrder);
		L.setArguments(order);
		L.setScriptUnit(this.scriptUnit);
		L.setGameData(this.scriptUnit.getScriptMain().gd_ScriptMain);
		if (this.scriptUnit.getScriptMain().client!=null){
			L.setClient(this.scriptUnit.getScriptMain().client);
		}
		this.scriptUnit.addAScript(L);
	}
	

	/**
	 * @return the setAreaName
	 */
	public String getSetAreaName() {
		return setAreaName;
	}


	/**
	 * @return the scriptUnit
	 */
	public ScriptUnit getScriptUnit() {
		return scriptUnit;
	}


	/**
	 * @return the verkaufsvorratsrunden
	 */
	public int getVerkaufsvorratsrunden() {
		return verkaufsvorratsrunden;
	}


	/**
	 * @param verkaufsvorratsrunden the verkaufsvorratsrunden to set
	 */
	public void setVerkaufsvorratsrunden(int verkaufsvorratsrunden) {
		this.verkaufsvorratsrunden = verkaufsvorratsrunden;
	}


	/**
	 * @return the buy
	 */
	public Item getBuy() {
		return buy;
	}


	/**
	 * @param buy the buy to set
	 */
	public void setBuy(Item buy) {
		this.buy = buy;
	}
	
	public void setBuyAmount(int menge){
		if (this.buy!=null){
			this.buy.setAmount(menge);
		}
	}


	/**
	 * @return the userWishIslandInfo
	 */
	public boolean isUserWishIslandInfo() {
		return userWishIslandInfo;
	}


	/**
	 * @param userWishIslandInfo the userWishIslandInfo to set
	 */
	public void setUserWishIslandInfo(boolean userWishIslandInfo) {
		this.userWishIslandInfo = userWishIslandInfo;
	}


	/**
	 * @return the buyPolicy
	 */
	public int getBuyPolicy() {
		return buyPolicy;
	}


	/**
	 * @param buyPolicy the buyPolicy to set
	 */
	public void setBuyPolicy(int buyPolicy) {
		this.buyPolicy = buyPolicy;
	}


	/**
	 * @return the kaufen
	 */
	public boolean isKaufen() {
		return kaufen;
	}


	/**
	 * @return the verkaufen
	 */
	public boolean isVerkaufen() {
		return verkaufen;
	}
	
	/**
	 * liefert die transportrequests dieses Traders
	 * abhängig auch von den aktuellen modified items....
	 * @return
	 */
	public ArrayList<TransportRequest> getTraderTransportRequests(){
		// nur was anfordern, wenn der auch verkaufen soll...
		if (!this.verkaufen){return null;}
		if (this.buy==null){return null;}
		ArrayList<TransportRequest> erg = null;
		for (Iterator<ItemType> iter = TradeUtils.handelItemTypes().iterator();iter.hasNext();){
			ItemType actItemType = (ItemType)iter.next();
			// nur requesten, was hier auch VERKAUFT werden kann...
			if (!actItemType.equals(this.buy.getItemType())){
				ArrayList<TransportRequest> newList = this.getTradeTransportRequests(actItemType);
				if (newList!=null){
					if (erg==null){
						erg = new ArrayList<TransportRequest>();
					}
					erg.addAll(newList);
				}
			}
		}
		return erg;
	}
	
	/**
	 * liefert die transportrequests dieses Traders für dieses ItemType
	 * @param itemType
	 * @return
	 */
	private ArrayList<TransportRequest> getTradeTransportRequests(ItemType itemType){
		
		if (this.prognoseRunden<=0){
			return null;
		}
		
		if (this.handeln==null){
			return null;
		}
		
		if (this.buy==null){return null;}
		
		// Die derzeitige erhaltene Menge ist gegenzurechnen
		// die in dieser Runde zu verkaufende Menge ist vorher abzuziehen
		// jetzige Menge herausfinden
		Item actItem = this.scriptUnit.getModifiedItem(itemType);
		int mengeVorhanden = 0;
		if (actItem!=null){
			mengeVorhanden = actItem.getAmount();
		}
		
		// wieviel ist eigentlich pro Woche drinne...
		int mengeProRunde = this.scriptUnit.getUnit().getRegion().maxLuxuries();

		// diese Runde wird von modified ja schon verkauft..also abziehen
		mengeVorhanden = Math.max(0, mengeVorhanden-mengeProRunde);
		
		// die rein rechnerische VerkaufsPRIO dieses Items hier
		int actPrio = mengeProRunde * getTradeRegion().getSellPrice(itemType); 
		
		// prios
		this.handeln.setPrioParameter(actPrio,-0.5 , 0, 0);
		
		// Das TradeArea normalisiert zum schluss alle Prios auf den 
		// vorgegebenen Bereich, siehe FFToolsArrayList
		
		// Ergebnisliste
		ArrayList<TransportRequest> erg = new ArrayList<TransportRequest>();
		for (int i = 1;i<=this.prognoseRunden;i++){
			// prio sinkt mit jeder runde in die zukunft
			// double d = this.prioAbsenkungProzent;
			// wegen prozent / 100
			// d = d/100;
			// erste runde nicht i=1 -> i-1=0
			// d = d * (i-1);
			// int actRundenPrio = (int)(actPrio - (int)(d*actPrio));
			// neu:
			int actRundenPrio = this.handeln.getPrio(i-1);
			// Request generieren
			TransportRequest TR = new TransportRequest(this.scriptUnit,mengeProRunde,itemType.getName(), actRundenPrio ,"Prognose in " + i + " Runden");
			TR.addSpec(itemType.getName());
			erg.add(TR);
		}
		return erg;
	}
	
	
	private TradeRegion getTradeRegion(){
		return this.scriptUnit.getScriptMain().getOverlord().getTradeAreaHandler().getTradeRegion(this.getRegion());
	}
	
	private Region getRegion(){
		return this.scriptUnit.getUnit().getRegion();
	}


	/**
	 * @return the sellItemRequestPrio
	 */
	public int getSellItemRequestPrio() {
		return sellItemRequestPrio;
	}


	/**
	 * @return the silverRequestPrio
	 */
	public int getSilverRequestPrio() {
		return silverRequestPrio;
	}


	/**
	 * @return the anzahl_silber_runden
	 */
	public int getAnzahl_silber_runden() {
		return anzahl_silber_runden;
	}


	/**
	 * @param anzahl_silber_runden the anzahl_silber_runden to set
	 */
	public void setAnzahl_silber_runden(int anzahl_silber_runden) {
		this.anzahl_silber_runden = anzahl_silber_runden;
	}

	/**
	 * @return the lernen
	 */
	public boolean isLernen() {
		return lernen;
	}

	public Double getProfit() {
		return Profit;
	}

	public void setProfit(Double profit) {
		Profit = profit;
	}
	
	
	
	
}
