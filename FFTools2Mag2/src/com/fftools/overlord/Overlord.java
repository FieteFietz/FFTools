package com.fftools.overlord;


import java.util.ArrayList;
import java.util.Iterator;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.pools.akademie.AkademieManager;
import com.fftools.pools.alchemist.AlchemistManager;
import com.fftools.pools.ausbildung.AusbildungsManager;
import com.fftools.pools.ausbildung.LernplanHandler;
import com.fftools.pools.bau.BauManager;
import com.fftools.pools.bau.WerftManager;
import com.fftools.pools.circus.CircusPoolManager;
import com.fftools.pools.heldenregionen.HeldenRegionsManager;
import com.fftools.pools.matpool.MatPoolManager;
import com.fftools.pools.pferde.PferdeManager;
import com.fftools.pools.seeschlangen.SeeschlangenJagdManager_SJM;
import com.fftools.pools.treiber.TreiberPoolManager;
import com.fftools.trade.TradeAreaHandler;
import com.fftools.transport.TransportManager;

import magellan.library.io.cr.CRParser;


/**
 * 
 * Klasse handelt Manager und Scripts
 * Für Scripts zu INfozwecken
 * 
 * @author Fiete
 *
 */
public class Overlord {
	private static final OutTextClass outText = OutTextClass.getInstance();
	
	private ArrayList<OverlordInfo> infoObjects = null;
	private ArrayList<OverlordRun> runnings = null;
	
	private int maxDurchLauf = -1;
	
	private ScriptMain scriptMain = null;
	
	/**
	 * Debug Zeitsummen
	 */
	public long Zeitsumme1 = 0;
	public long Zeitsumme2 = 0;
	public long Zeitsumme3 = 0;
	public long Zeitsumme4 = 0;
	public long Zeitsumme5 = 0;
	public long Zeitsumme6 = 0;
	public long Zeitsumme7 = 0;
	public long Zeitsumme8 = 0;
	public long Zeitsumme9 = 0;
	public long Zeitsumme10 = 0;
	
	
	
	/**
	 * die manager
	 */
	private MatPoolManager matPoolManager = null;
	private CircusPoolManager circusPoolManager = null;
	private TreiberPoolManager treiberPoolManager = null;
	private TransportManager transportManager = null;
	private AusbildungsManager ausbildungsManager=null; 
	private AlchemistManager alchemistManager = null;
	private AkademieManager akademieManager  = null;
	private PferdeManager pferdeManager = null;
	private HeldenRegionsManager heldenRegionsManager = null;
	private BauManager bauManager = null;
	private WerftManager werftManager = null;
	private SeeschlangenJagdManager_SJM SJM = null;
	
	
	/**
	 * die handler
	 */
	private TradeAreaHandler tradeAreaHandler = null;
	private LernplanHandler lernplanHandler = null;
	
	/**
	 * Hilfslisten
	 */
	private ArrayList<ScriptUnit> deletedUnits = null;
	
	/**
	 * Der aktuelle Zähler
	 */
	private int mainDurchlauf=-1;
	
	/**
	 * Konstruktor
	 * @param scM
	 */
	public Overlord(ScriptMain scM){
		this.scriptMain = scM;
	}
	
	/**
	 * start des durchlaufes
	 * pro durchlauf: erst scripte, dann manager
	 *
	 */
	public void run(){
		boolean sayTime = false;
		if (maxDurchLauf<0){
			outText.addOutLine("Overlord: run nicht möglich: kein Max Durchlauf");
			return;
		}
		if (this.scriptMain.getScriptUnits()==null){
			outText.addOutLine("Overlord: run nicht möglich: keine Scriptunits");
			return;
		}
		
		// Scriptgesteuerte Tags 3, 4 und 5 zurücksetzen, weil diese bei jedem scriptlauf neu vergeben werden.
		for (Iterator<ScriptUnit> iter = this.scriptMain.getScriptUnits().values().iterator();iter.hasNext();){
			ScriptUnit scrU = (ScriptUnit)iter.next();
			if (!isDeleted(scrU)){ 
				 // Tag3 löschen, wenn er vergeben ist
				 if (scrU.getUnit().containsTag(CRParser.TAGGABLE_STRING3)){
					 scrU.getUnit().removeTag(CRParser.TAGGABLE_STRING3);			 
				 }
				 
				// Tag4 löschen, wenn er vergeben ist
				 if (scrU.getUnit().containsTag(CRParser.TAGGABLE_STRING4)){
					 scrU.getUnit().removeTag(CRParser.TAGGABLE_STRING4);			 
				 }
				 
				// Tag5 löschen, wenn er vergeben ist
				 if (scrU.getUnit().containsTag(CRParser.TAGGABLE_STRING5)){
					 scrU.getUnit().removeTag(CRParser.TAGGABLE_STRING5);			 
				 }
			}
		}
		
		long time1 =0;
		long timeX = 0;
		long lastProzentAngabe = 0;
		// long tDiffX = 0;
		for (mainDurchlauf = 0;mainDurchlauf<Integer.MAX_VALUE;mainDurchlauf++){
			sayTime=false;
			time1 = System.currentTimeMillis();
			timeX = 0;
			// tDiffX = 0;
			// outText.addNewLine();
			// outText.addOutChars("*** start: " + mainDurchlauf + " *** ");
			// Info doch vorab
			String scriptNames = getScriptsForRunNumber(mainDurchlauf);
			
			timeX=System.currentTimeMillis();
			// tDiffX = timeX - time1;
			// outText.addNewLine();
			// outText.addOutChars(tDiffX + ": after getScriptsForRunNumber ");
			
			if (scriptNames.length()>0){
				// outText.addNewLine();
				// outText.addOutChars(mainDurchlauf + "-s>" + scriptNames + ":");
				sayTime=true;
			} else {
				// outText.addOutChars("," + mainDurchlauf);
			}
			
			
			this.Zeitsumme1=0;
			this.Zeitsumme2=0;
			this.Zeitsumme3=0;
			this.Zeitsumme4=0;
			this.Zeitsumme5=0;
			this.Zeitsumme6=0;
			this.Zeitsumme7=0;
			this.Zeitsumme8=0;
			this.Zeitsumme9=0;
			this.Zeitsumme10=0;
			
			long scriptU_Counter=0;
			// scriptunits anstossen
			for (Iterator<ScriptUnit> iter = this.scriptMain.getScriptUnits().values().iterator();iter.hasNext();){
				ScriptUnit scrU = (ScriptUnit)iter.next();
				if (!isDeleted(scrU)){
					scrU.runScripts(mainDurchlauf);
					outText.addPoint();
				}
				scriptU_Counter++;
				if ((System.currentTimeMillis() - lastProzentAngabe)>3000) {
					double actProz = (double)((double)scriptU_Counter/(double)this.scriptMain.getScriptUnits().values().size());
					long actProzL = Math.round(actProz * 100);
					outText.addOutChars("|" + actProzL + "%");
					lastProzentAngabe = System.currentTimeMillis();
				}
			}
			/*
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 1: " + this.Zeitsumme1);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 2: " + this.Zeitsumme2);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 3: " + this.Zeitsumme3);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 4: " + this.Zeitsumme4);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 5: " + this.Zeitsumme5);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 6: " + this.Zeitsumme6);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 7: " + this.Zeitsumme7);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 8: " + this.Zeitsumme8);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 9: " + this.Zeitsumme9);
			outText.addNewLine();
			outText.addOutChars("Zeitsumme 10: " + this.Zeitsumme10);
			*/
			
			timeX=System.currentTimeMillis();
			// tDiffX = timeX - time1;
			// outText.addNewLine();
			// outText.addOutChars(tDiffX + ": after scrU.runScripts ");
			
			
			// manager laufen lassen ?!
			if (this.runnings!=null){
				for (Iterator<OverlordRun> iter = this.runnings.iterator();iter.hasNext();){
					Object o = iter.next();
					OverlordInfo oI = (OverlordInfo)o;
					if (isInRun(oI, mainDurchlauf)){
						// outText.addNewLine();
						// outText.addOutChars(mainDurchlauf + "-m>" + this.getSimpleClassName(oI.getClass()) + ":");
						OverlordRun oR = (OverlordRun)o;
						oR.run(mainDurchlauf);
						outText.addOutChars(" | done with " + this.getSimpleClassName(oI.getClass()));
						outText.addNewLine();
						sayTime=true;
					}
				}
			}
			
			timeX=System.currentTimeMillis();
			// tDiffX = timeX - time1;
			// outText.addNewLine();
			// outText.addOutChars(tDiffX + ": after run Overlords-Runs ");
			
			
			if (sayTime){
				long time2 = System.currentTimeMillis();
				long tDiff = time2 - time1;
				outText.addOutChars("[" + mainDurchlauf + ":" + tDiff + "ms]");
				outText.addNewLine();
			}
			
			
			// maximalen Durchlauf erreicht?
			if (mainDurchlauf>maxDurchLauf){
				// Abbruch
				break;
			}
			
		}
		
		
		
	}
	
	
	/**
	 * fügt ein script hinzu, wenn es nicht bereits da ist
	 * @param s
	 */
	public void addOverlordInfo(OverlordInfo s){
		if (this.infoObjects==null){
			this.infoObjects = new ArrayList<OverlordInfo>(2);
		}
		// jedes script nur einmal...am namen erkennen
		boolean schonda = false;
		for (Iterator<OverlordInfo> iter = this.infoObjects.iterator();iter.hasNext();){
			OverlordInfo myS = (OverlordInfo)iter.next();
			if (myS.getClass().getName().equals(s.getClass().getName())){
				schonda = true;
				break;
			}
		}
		if (!schonda){
			this.infoObjects.add(s);
			this.checkMaxDurchlauf(s);
		}
	}
	
	/**
	 * überprüft und setzt gegebenenfalls die max Anzahl Durchläufe
	 * @param s
	 */
	private void checkMaxDurchlauf(OverlordInfo s){
		if (s.runAt()==null){return;}
		for (int i = 0;i<=s.runAt().length-1;i++){
			int actX = (int)s.runAt()[i];
			if (actX>this.maxDurchLauf){
				this.maxDurchLauf = actX;
			}
		}
	}
	
	
	private String getScriptsForRunNumber(int runN){
		String erg  = "";
		for (OverlordInfo oI:this.infoObjects){
			if (oI.runAt()!=null){
				for (int i = 0;i<=oI.runAt().length-1;i++){
					int actX = (int)oI.runAt()[i];
					if (actX==runN){
						String className = oI.getClass().getSimpleName();
						if (erg.length()>0){
							erg = erg + ",";
						}
						erg = erg + className;
					}
				}
			}
		}
		return erg;
	}
	
	
	/**
	 * Ist eine OverlordInfo im aktuellen Durchlauf enthalten?
	 * @param s
	 * @param check
	 * @return true or false
	 */
	private boolean isInRun(OverlordInfo s,int check){
		if (s.runAt()==null){return false;}
		for (int i = 0;i<s.runAt().length;i++){
			int actX = (int)s.runAt()[i];
			if (actX==check){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Kurze Info, was wann lief
	 *
	 */
	public void informUs(){
		if (this.infoObjects==null){
			outText.addOutLine("Scriptinfo: keine scripte bekannt");
			return;
		}
		outText.addOutLine("Overlord Skriptinfos");
		String names = "";
		for (int d = 0;d<=this.maxDurchLauf;d++){
			// scripts
			names = "";
			for (Iterator<OverlordInfo> iter = this.infoObjects.iterator();iter.hasNext();){
				OverlordInfo myS = (OverlordInfo)iter.next();
				if (this.isInRun(myS, d)){
					// treffer
					if (names.length()>1){
						names+=",";
					}
					names+=this.getSimpleClassName(myS.getClass());
				}
			}
			if (names.length()>1){
				outText.addOutLine(d + ":" + names);
			}
			// Managers
			names = "";
			if (this.runnings!=null){
				for (Iterator<OverlordRun> iter = this.runnings.iterator();iter.hasNext();){
					OverlordInfo myS = (OverlordInfo)iter.next();
					if (this.isInRun(myS, d)){
						// treffer
						if (names.length()>1){
							names+=",";
						}
						names+=this.getSimpleClassName(myS.getClass());
					}
				}
			}
			if (names.length()>1){
				outText.addOutLine(d + "->Manager:" + names);
			}
			
		}
		names = "";
		for (Iterator<OverlordInfo> iter = this.infoObjects.iterator();iter.hasNext();){
			OverlordInfo myS = (OverlordInfo)iter.next();
			if (myS.runAt()==null){
				// treffer
				if (names.length()>1){
					names+=",";
				}
				names+=this.getSimpleClassName(myS.getClass());
			}
		}
		if (names.length()>1){
			outText.addOutLine("no info:" + names);
		}
	}
	
	
	private String getSimpleClassName(Class<?> s){
		String work = s.getName();
		int i = work.lastIndexOf(".");
		if (i>0){
			work = work.substring(i+1);
		}
		return work;
	}

	/**
	 * fügt einen auszuführenden (Manager) hinzu
	 * @param o der Manager
	 */
	private void addRunner(OverlordRun o){
		if (this.runnings==null){
			this.runnings = new ArrayList<OverlordRun>(1);
		}
		if (!this.runnings.contains(o)){
			this.runnings.add(o);
			
			/*
			outText.addNewLine();
			outText.addOutChars("Debug: added to Overord-runnings: " + this.getSimpleClassName(o.getClass()) + "");
			*/
			// wenn es Instance vonm OverlordInfo ist, dann maxRun anpassen
			// outText.addOutChars("debug: added a OverlordRun " + o.toString());
			if (o instanceof OverlordInfo) {
				OverlordInfo oI = (OverlordInfo)o;
				this.checkMaxDurchlauf(oI);
			}
		}
	}
	
	/**
	 * @return the circusPoolManager
	 */
	public CircusPoolManager getCircusPoolManager() {
		if (circusPoolManager==null){
			  circusPoolManager = new CircusPoolManager(this.scriptMain);
			  this.addRunner(circusPoolManager);
		}
		return circusPoolManager;
	}

	/**
	 * @return the circusPoolManager
	 */
	public TreiberPoolManager getTreiberPoolManager() {
		if (treiberPoolManager==null){
			  treiberPoolManager = new TreiberPoolManager(this.scriptMain);
			  this.addRunner(treiberPoolManager);
		}
		return treiberPoolManager;
	}
	

	/**
	 * @return the matPoolManager
	 */
	public MatPoolManager getMatPoolManager() {
		if (this.matPoolManager==null){
			this.matPoolManager = new MatPoolManager(this.scriptMain);
			this.addRunner(matPoolManager);
		}
		return matPoolManager;
	}
	
	/**
	 * 
	 * @return the SJM
	 */
	public SeeschlangenJagdManager_SJM getSJM() {
		if (this.SJM==null) {
			this.SJM = new SeeschlangenJagdManager_SJM(this);
			this.addRunner(SJM);
			this.addOverlordInfo(SJM);
		}
		return this.SJM;
	}


	/**
	 * @return the scriptMain
	 */
	public ScriptMain getScriptMain() {
		return scriptMain;
	}


	/**
	 * @return the tradeAreaHandler
	 */
	public TradeAreaHandler getTradeAreaHandler() {
		if (tradeAreaHandler==null){
			tradeAreaHandler = new TradeAreaHandler(scriptMain);
			// diesen nur in die scripts mit aufnehmen
			// this.addOverlordInfo(tradeAreaHandler);
			this.addRunner(tradeAreaHandler);
		}
		return tradeAreaHandler;
	}

	/**
	 * @return the lernplanHandler
	 */
	public LernplanHandler getLernplanHandler() {
		if (lernplanHandler==null){
			lernplanHandler = new LernplanHandler();
			// diesen nur in die scripts mit aufnehmen
			this.addOverlordInfo(lernplanHandler);
		}
		return lernplanHandler;
	}
	
	
	/**
	 * @return the transportManager
	 */
	public TransportManager getTransportManager() {
		if (this.transportManager==null){
			this.transportManager = new TransportManager(this.scriptMain);
			this.addRunner(this.transportManager);
		}
		return transportManager;
	}
	
	/**
	 * @return the ausbildungsManager
	 */
	public AusbildungsManager getAusbildungsManager() {
		if (this.ausbildungsManager==null){
			this.ausbildungsManager = new AusbildungsManager(this.scriptMain);
			this.addRunner(this.ausbildungsManager);
		}
		return ausbildungsManager;
	}
	
		
	/**
	 * 
	 * @return the alchemistManager
	 */
	public AlchemistManager getAlchemistManager(){
		if (this.alchemistManager==null){
			this.alchemistManager = new AlchemistManager(this);
			this.addRunner(this.alchemistManager);
		}
		return this.alchemistManager;
	}
	
	
	/**
	 * 
	 * @return the akademieManager
	 */
	public AkademieManager getAkademieManager(){
		if (this.akademieManager==null){
			this.akademieManager = new AkademieManager(this.scriptMain);
			this.addRunner(this.akademieManager);
		}
		return this.akademieManager;
	}
	
	/**
	 * 
	 * @return the PferdeManager
	 */
	public PferdeManager getPferdeManager(){
		if (this.pferdeManager==null){
			this.pferdeManager = new PferdeManager(this);
			this.addRunner(this.pferdeManager);
		}
		return this.pferdeManager;
	}

	/**
	 * @return the heldenRegionsManager
	 */
	public HeldenRegionsManager getHeldenRegionsManager() {
		if (this.heldenRegionsManager==null){
			this.heldenRegionsManager = new HeldenRegionsManager(this.scriptMain);
			this.addRunner(this.heldenRegionsManager);
		}
		return heldenRegionsManager;
	}

	/**
	 * @return the bauManager
	 */
	public BauManager getBauManager() {
		if (this.bauManager==null){
			this.bauManager = new BauManager(this.scriptMain);
			this.addRunner(this.bauManager);
		}
		return bauManager;
	}

	
	public WerftManager getWerftManager(){
		if (this.werftManager==null){
			this.werftManager = new WerftManager(this.scriptMain);
			this.addRunner(this.werftManager);
		}
		return this.werftManager;
	}
	
	
	/**
	 * setzt die ScriptUnit auf die Liste gelöschter Units
	 * sollte ab jetzt ignoriert werden vom gesammten Script
	 * @param u
	 */
	public void deleteScriptUnit(ScriptUnit u){
		if (this.deletedUnits==null){
				this.deletedUnits = new ArrayList<ScriptUnit>();
		}
		if (!this.deletedUnits.contains(u)){
			this.deletedUnits.add(u);
		}
		
	}
	
	public boolean isDeleted(ScriptUnit u){
		if (this.deletedUnits==null || this.deletedUnits.size()==0){
			return false;
		}
		if (this.deletedUnits.contains(u)){
			return true;
		}
		return false;
	}

	public int getMainDurchlauf() {
		return mainDurchlauf;
	}
	
	
	
	
}
