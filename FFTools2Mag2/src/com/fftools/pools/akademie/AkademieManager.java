package com.fftools.pools.akademie;

	import java.util.ArrayList;
import java.util.HashMap;

import com.fftools.OutTextClass;
import com.fftools.ScriptMain;
import com.fftools.ScriptUnit;
import com.fftools.overlord.OverlordInfo;
import com.fftools.overlord.OverlordRun;
import com.fftools.pools.ausbildung.AusbildungsManager;
import com.fftools.pools.ausbildung.AusbildungsPool;
import com.fftools.pools.ausbildung.relations.AusbildungsRelation;
import com.fftools.scripts.Akademie;
import com.fftools.scripts.Lernfix;

import magellan.library.Building;
import magellan.library.Region;
import magellan.library.Rules;

	public class AkademieManager implements OverlordRun,OverlordInfo {
		
		private static final OutTextClass outText = OutTextClass.getInstance();
		
		private static final int Durchlauf1 = 664;  // eigentliches Poolen, Akademie laufen 662
		private static final int Durchlauf2 = 666;  // aka-warning checks?
		
		private int[] runners = {Durchlauf1, Durchlauf2};
		
		//  Verbindung zu FFtools halten über Scriptmain
		public ScriptMain scriptMain;
		
		// Da stecken die Pools!
		private ArrayList<AkademiePool> AkademiePools = null;
		
		
		// pro Region eine Liste mit relevantenAusbildungsRelations
		private HashMap<Region,ArrayList<AusbildungsRelation>> relevantAR = null;
		
		private ArrayList<AkademieTalent> defaultTalentList = null;
		
		
		/**
		 * Konstruktor noch gaanz einfach.
		 */
		public AkademieManager(ScriptMain _scriptmain){
		     scriptMain = _scriptmain;	
		}
		
	


	/**
	 * 
	 * Hier stoesst man die AkademiePools an.
	 *
	 */	
		
	    public void run(int Durchlauf){
	    	outText.addOutLine("AkademieManager",true);
	    	long start = System.currentTimeMillis();
	    	if (AkademiePools != null){
	    		if (Durchlauf == Durchlauf1){
		    		// das Poolen
			        for (AkademiePool AP : AkademiePools){
				        AP.runPool();
				        outText.addPoint();
			        }
			        // Leermachen der Akademien
			        for (AkademiePool AP : AkademiePools){
				        AP.leaveAkademie();
				        outText.addPoint();
			        }
			        // Betrete setzen
			        // durch alle Regionen laufen
			        for (Region r:this.relevantAR.keySet()){
			        	ArrayList<AusbildungsRelation> actARs = this.relevantAR.get(r);
			        	for (AusbildungsRelation AR:actARs){
			        		if (AR.getAkademieFromAM()!=null){
			        			// OK, die soll irgendwo rein
			        			// frage, ob sie da schon drinne ist?
			        			boolean needEnterOrder = false;
			        			Building neueAka = AR.getScriptUnit().getUnit().getModifiedBuilding();
			        			if (neueAka==null){
			        				needEnterOrder=true;
			        			} else {
			        				if (!neueAka.equals(AR.getAkademieFromAM())){
			        					needEnterOrder=true;
			        				}
			        			}
			        			if (needEnterOrder){
			        				AR.getScriptUnit().addOrder("BETRETEN BURG " + AR.getAkademieFromAM().getID() + " ; AkaPool-> " + AR.getAkademieFromAM().toString(), true);
			        				AR.setOrderedNewAka(true);
			        			}
			        		}
			        	}
			        	outText.addPoint();
			        }
		    	 }
	    		if (Durchlauf == Durchlauf2){
	    			// Akawarnungscheck
	    			this.scriptMain.getOverlord().getAusbildungsManager().runAkaWarnungen();
	    		}
	    	}
	    	long ende = System.currentTimeMillis();
	    	outText.addOutLine("AkademieManager benötigte:" + (ende-start) + "ms",true);
	    }
	    /**
	     * Meldung an den Overlord
	     * @return
	     */
	    public int[] runAt(){
	    	return runners;
	    }
	    
	    /**
	     * 
	     * Ergänzt die Akademie b zu den Pools
	     * (sofern noch nicht vorhanden)
	     * @param b MUSS eine Aka sein
	     * @return der Pool oder null, als schon da
	     */
	    public AkademiePool addAkademie(Building b, Akademie AT){
	    	// gibts schon Pool-List?
	    	if (this.AkademiePools==null){
	    		this.AkademiePools = new ArrayList<AkademiePool>();
	    	}
	    	// gibts schon das Gebäude in dem Pool?
	    	for (AkademiePool AP:this.AkademiePools){
	    		if (AP.getAkademieBuilding().equals(b)){
	    			// Fehler hier
	    			return null;
	    		}
	    	}
	    	// alles fein, anlegen und ergänzen
	    	AkademiePool AP = new AkademiePool(b,AT);
	    	this.AkademiePools.add(AP);
	    	return AP;
	    }
	    
	    /**
	     * liefert für eine Region die Liste relevanter ARs
	     * falls noch keine vorhanden ist, wird sie generiert
	     * @param r Die betreffende Region
	     * @return
	     */
	    public ArrayList<AusbildungsRelation> getRelevantAR(Region r){
	    	if (this.relevantAR==null){
	    		this.relevantAR = new HashMap<Region, ArrayList<AusbildungsRelation>>();
	    	}
	    	ArrayList<AusbildungsRelation> erg = this.relevantAR.get(r);
	    	if (erg==null){
	    		erg = this.createRelevantAR(r);
	    		this.relevantAR.put(r,erg);
	    	}
	    	return erg;
	    }
	    
	    /**
	     * generiert die Liste relevanter ARs für diese Region
	     * @param r
	     * @return
	     */
	    private ArrayList<AusbildungsRelation> createRelevantAR(Region r){
	    	ArrayList<AusbildungsRelation> erg = new ArrayList<AusbildungsRelation>();
	    	// wir brauchen den AM
	    	AusbildungsManager AM = scriptMain.getOverlord().getAusbildungsManager();
	    	AusbildungsPool AP = AM.getAusbildungsPool(r);
	    	if (AP==null){
	    		// nix zu tun, kein Pool vorhanden
	    		return erg;
	    	}
	    	ArrayList<AusbildungsRelation> regionList = AP.getRelationList();
	    	if (regionList==null || regionList.isEmpty()){
	    		// nix zu tun, Pool-List ist leer
	    		return erg;
	    	}
	    	
	    	// Es ist was zu tun!
	    	// Vorhandene Filtern...
	    	for (AusbildungsRelation AR:regionList){
	    		boolean isInFilter=true;
	    		ScriptUnit sU = AR.getScriptUnit();

	    		// Schiffsesatzungen raus
	    		if (sU.getUnit().getModifiedShip()!=null){
	    			isInFilter=false;
	    			sU.addComment("Akadademiemanager: Einheit auf Schiff, daher unberücksichtigt");
	    		}
	    		
	    		// wenn er partou nicht will (aka=false)
	    		Object o = sU.getScript(Lernfix.class);
	    		if (o != null) {
	    			Lernfix L = (Lernfix)o;
	    			if (L.isAvoidAka()) {
	    				AR.setAvoidAka(true);
		    			sU.addComment("Akadademiemanager: Einheit widersetzt sich meinen Planungen, daher unberücksichtigt");
	    			} else {
	    				sU.addComment("Akadademiemanager: Einheit wird verplant");
	    			}
	    		} else {
	    			sU.addComment("Akadademiemanager: kein Lernfix script gefunden");
	    		}
	    		
	    		
	    		
	    		// Akademiebesitzer immer drinne ?! (Nur wenn <=25 Pers!
	    		Building b = AR.getScriptUnit().getUnit().getModifiedBuilding();
	    		if (b!=null && b.getOwner()!=null && b.getOwner().equals(sU.getUnit()) && sU.getUnit().getModifiedPersons()<=25){
	    			isInFilter=true;
	    		}
	    		if (isInFilter){
	    			AR.setAkademieFromAM(null);
	    			erg.add(AR);
	    		}
	    	}
	    	return erg;
	    }
	    
	    
	    public ArrayList<AkademieTalent> getDefaultTalentList(){
	    	if (this.defaultTalentList==null){
	    		this.defaultTalentList = this.defaultTalente();
	    	}
	    	return this.defaultTalentList;
	    }
	    
	    
	    private ArrayList<AkademieTalent> defaultTalente(){
			ArrayList<AkademieTalent> erg = new ArrayList<AkademieTalent>();
			Rules R = scriptMain.gd_ScriptMain.getRules();
			erg.add(new AkademieTalent(R.getSkillType("draig",true), 25));
			erg.add(new AkademieTalent(R.getSkillType("illaun",true), 25));
			erg.add(new AkademieTalent(R.getSkillType("tybied",true), 25));
			erg.add(new AkademieTalent(R.getSkillType("gwyrrd",true), 25));
			erg.add(new AkademieTalent(R.getSkillType("cerddor",true), 25));
			erg.add(new AkademieTalent(R.getSkillType("Taktik"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Wahrnehmung"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Tarnung"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Spionage"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Hiebwaffen"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Bogenschießen"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Armbrustschießen"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Stangenwaffen"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Ausdauer"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Reiten"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Alchemie"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Kräuterkunde"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Bergbau"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Steinbau"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Holzfällen"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Pferdedressur"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Waffenbau"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Rüstungsbau"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Schiffbau"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Segeln"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Burgenbau"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Straßenbau"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Unterhaltung"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Steuereintreiben"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Handeln"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Wagenbau"), 25));
			erg.add(new AkademieTalent(R.getSkillType("Katapultbedienung"), 25));
			return erg;
		}
	    
	  	
	}

