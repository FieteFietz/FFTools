package com.fftools.scripts;

import java.util.Iterator;

import magellan.library.Skill;
import magellan.library.rules.SkillType;

import com.fftools.ReportSettings;
import com.fftools.pools.matpool.relations.MatPoolRequest;
import com.fftools.utils.FFToolsOptionParser;

/**
 * Requestet für Militäreinheiten benötigte Ausrüstung
 * @author Fiete
 *
 */
public class Material extends MatPoolScript {
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private static final int Durchlauf = 30;
	
	private final String[] talentNamen = {"Hiebwaffen","Stangenwaffen","Bogenschießen","Armbrustschießen","Katapultbedienung"};
	
	private final int default_basisPrio=700;
	private final int offsetRüstung = 0;
	private final int offsetSchilde = 20;
	private final int offsetWaffen = 40;
	private final int offsetHelden = 100;
	
	private final double hintenFaktor = 0.8;
	private final double relevantLevelFaktor = 3;
	private final double taktikerFaktor = 5;
	private final double spotterFaktor = 4;
	private final double stealthFaktor = 4;
	private final double reitenFaktor = 3;
	private final int magierPrio = 1000;
	private boolean isMage = false;
	private boolean isTactics = false;
	private boolean isSpotter = false;
	private boolean isStealth = false;
	
	private boolean MaterialWundsalbe = true;
	
	private int basisPrio = default_basisPrio;
	
	private final String materialString = "Material: ";
	
	/**
	 * MatRequests nur Innerhalb der Region?
	 */
	private boolean inRegion=false;
	
	public Material(){
		super.setRunAt(Durchlauf);
	}
	
	public void runScript(int scriptDurchlauf){
		switch (scriptDurchlauf){

		case Durchlauf:this.run1();break; 
		}
	}
	
	private void run1(){
		
		super.addVersionInfo();

		// check personenanzahl
		int persons = this.scriptUnit.getUnit().getModifiedPersons();
		if (persons<=0){
			return;
		}
		
		FFToolsOptionParser OP = new FFToolsOptionParser(this.scriptUnit,"Material");
		OP.addOptionList(this.getArguments());
		
		// als relevante Waffenlevel gibts entweder Hiebwaffen oder Stangenwaffen
		// oder Bogenschießen oder Armbrustschießen
		Skill relevantSkill = null;
		for (int i = 0;i<this.talentNamen.length;i++){
			String actName = this.talentNamen[i];
			SkillType actSkillType = this.gd_Script.getRules().getSkillType(actName);
			if (actSkillType!=null){
				Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
				if (actSkill!=null){
					if (relevantSkill==null || relevantSkill.getLevel()<actSkill.getLevel()){
						relevantSkill = actSkill;
					}
				}
			}
		}
		
		int prio = this.basisPrio;
		
		// bestSkillType feststellen
		SkillType bestSkillType = null;
		int bestSkillLevel = 0;
		
		for (Iterator<Skill> iter = this.scriptUnit.getUnit().getModifiedSkills().iterator();iter.hasNext();){
			Skill actSkill = (Skill)iter.next();
			if (actSkill.getLevel()>=bestSkillLevel){
				bestSkillType = actSkill.getSkillType();
				bestSkillLevel = actSkill.getLevel();
			}
		}
		
		
		// Besonderheiten
		// Taktiker
		int actTaktikLevel = 0;
		SkillType actSkillType = this.gd_Script.rules.getSkillType("Taktik");
		if (actSkillType.equals(bestSkillType)){
			    actTaktikLevel = this.scriptUnit.getUnit().getModifiedSkill(bestSkillType).getLevel();
				this.isTactics = true;
		}
		
		// Magier
		int magieLevel=0;
		actSkillType = this.gd_Script.rules.getSkillType("Magie");
		if (actSkillType!=null){
			Skill actSkill = this.scriptUnit.getUnit().getModifiedSkill(actSkillType);
			if (actSkill!=null){
				int actLevel = actSkill.getLevel();
				if (actLevel>0){
					// prio = this.magierPrio;
					this.isMage=true;
					magieLevel=actLevel;
				}
			}
		}
		// Wahrnehmer
		int actSpotterLevel =0;
		actSkillType = this.gd_Script.rules.getSkillType("Wahrnehmung");
		if (actSkillType!=null){
			if (actSkillType.equals(bestSkillType)){
				actSpotterLevel = this.scriptUnit.getUnit().getModifiedSkill(bestSkillType).getLevel();
				this.isSpotter=true;
			}
		}
		// Tarner
		int actStealthLevel = 0;
		actSkillType = this.gd_Script.rules.getSkillType("Tarnung");
		if (actSkillType!=null){
			if (actSkillType.equals(bestSkillType)){
				actStealthLevel = this.scriptUnit.getUnit().getModifiedSkill(bestSkillType).getLevel();
				this.isStealth=true;
			}
		}

		boolean specialAgent = false;
		String ItemGroupPräfix = "";
		if (this.isTactics){
			specialAgent=true;
			ItemGroupPräfix = "Taktiker";
		}
		if (this.isMage){
			specialAgent=true;
			ItemGroupPräfix = "Magier";
		}
		if (this.isSpotter){
			specialAgent=true;
			ItemGroupPräfix = "Wahrnehmer";
		}
		if (this.isStealth){
			specialAgent=true;
			ItemGroupPräfix = "Tarner";
		}
		
		
		if (relevantSkill==null && !specialAgent){
			this.addComment("Material: kein Talent erkannt");
			return;
		}
		
		if (relevantSkill==null && specialAgent){
			this.addComment("Material: SpecialAgent erkannt:" + bestSkillType.getName());
		}
		
		// rausfinden, ob es ne Itemgroup gibt
		// wenn nein, standardrequest der gängigsten waffe
		String materialName = "nix";
		
		if (relevantSkill!=null){
			materialName=relevantSkill.getName();
			String originalMaterialName = materialName;
			
			// Test auf SezielItemgroup
			if (specialAgent && reportSettings.isInCategories("Spezialist" + originalMaterialName)){
				materialName = "Spezialist" + originalMaterialName;
			}
			
			// Test auf SuperSezielItemgroup
			if (specialAgent && reportSettings.isInCategories(ItemGroupPräfix + originalMaterialName)){
				materialName = ItemGroupPräfix + originalMaterialName;
			}
			
			
			if (!reportSettings.isInCategories(materialName)){
				// nicht drinne, notlösung
				String matNameNeu = "nix";
				if (materialName.equalsIgnoreCase("Hiebwaffen")) {
					matNameNeu = "Schwert";
				} else if(materialName.equalsIgnoreCase("Stangenwaffen")){
					matNameNeu = "Speer";
				} else if(materialName.equalsIgnoreCase("Bogenschießen")){
					matNameNeu = "Bogen";
				} else if(materialName.equalsIgnoreCase("Armbrustschießen")){
					matNameNeu = "Armbrust";
				} else if (materialName.equalsIgnoreCase("Katapultbedienung")){
					matNameNeu="Katapult";
				} 
				
				if (matNameNeu.equalsIgnoreCase("nix") && !specialAgent){
					this.addComment("Material: unbekanntes Talent (keine Itemgroup)");
					return;
				}
				
				this.addComment("Material: keine Itemgroup für " + materialName + " bekannt. Benutze: " + matNameNeu);
				
				materialName = matNameNeu;
			} 
			
			if (specialAgent){
				this.addComment("Material: benutzte Waffen-ItemGroup: " + materialName );
			}
		}
		// prio
		
		int user_prio = OP.getOptionInt("prio",-1);
		if (user_prio>0){
			this.basisPrio = user_prio;
		}
		
		
		int user_prioAdd = OP.getOptionInt("prioAdd",0);
		if (user_prioAdd!=0){
			this.basisPrio += user_prioAdd;
		}
		
		
		int relevantSkillLevel=0;
		if (relevantSkill!=null){
			relevantSkillLevel = relevantSkill.getLevel();
		}
		
		prio = this.basisPrio + (int)((double)relevantSkillLevel * this.relevantLevelFaktor);
		
		if (this.isTactics){
			prio += (int)((double)actTaktikLevel * this.taktikerFaktor);
		}
		
		if (this.isSpotter){
			prio += (int)((double)actSpotterLevel * this.spotterFaktor);
		}
		
		if (this.isStealth){
			prio += (int)((double)actStealthLevel * this.stealthFaktor);
		}
		
		if (this.isMage){
			prio = this.magierPrio  + magieLevel + user_prioAdd;
		}
		
		
		
		// allgemein: + AusdauerLevel
		SkillType ausdauerType = this.gd_Script.rules.getSkillType("Ausdauer");
		Skill ausdauerSkill = this.scriptUnit.getUnit().getModifiedSkill(ausdauerType);
		if (ausdauerSkill!=null && ausdauerSkill.getLevel()>0){
			prio+=ausdauerSkill.getLevel();
		}
		
		// Zusatz: auch nur Reitlevel dazu
		SkillType reitType = this.gd_Script.rules.getSkillType("Reiten");
		Skill reitSkill = this.scriptUnit.getUnit().getModifiedSkill(reitType);
		if (reitSkill!=null && reitSkill.getLevel()>0){
			prio+=(int)(reitSkill.getLevel() * (int)reitenFaktor);
		}
		
		
		// Heldenberücksichtigung
		if (this.scriptUnit.getUnit().isHero()){
			prio+=offsetHelden;
		}
		
		// Kommentar
		String comment = this.materialString + this.scriptUnit.getUnit().toString(true);
		
		// Kapa-policy
		int kapa_policy = MatPoolRequest.KAPA_unbenutzt;
		int kapa_benutzer = 0;
		
		if (OP.getOptionString("kapa").equalsIgnoreCase("reiten")){
			kapa_policy = MatPoolRequest.KAPA_max_zuPferd;
		}
		if (OP.getOptionString("kapa").equalsIgnoreCase("gehen")){
			kapa_policy = MatPoolRequest.KAPA_max_zuFuss;
		}
		if (OP.getOptionInt("kapa",-1)>0){
			kapa_policy = MatPoolRequest.KAPA_benutzer;
			kapa_benutzer = OP.getOptionInt("kapa", 0);
		}
		
		this.inRegion = OP.getOptionBoolean("region",false);
		
		
		MatPoolRequest mpr = null;
		
		// request für die Waffen
		if (!materialName.equalsIgnoreCase("nix")){
			mpr = new MatPoolRequest(this,persons,materialName,prio + offsetWaffen,comment,kapa_policy,kapa_benutzer);
			if (this.inRegion){
				mpr.setOnlyRegion(true);
			}
			this.addMatPoolRequest(mpr);
			
			// waffentalent bekannt ?helden?
			if (this.scriptUnit.getUnit().isHero()){
				this.gd_Script.rules.getItemType("Gürtel der Trollstärke",true);
				mpr = new MatPoolRequest(this,persons,"Gürtel der Trollstärke",prio + offsetWaffen,comment,kapa_policy,kapa_benutzer);
				if (this.inRegion){
					mpr.setOnlyRegion(true);
				}
				this.addMatPoolRequest(mpr);
			}
			
		}
		
		// Besonderheit: Munition für Katapulte... 5 Schuss (?)
		if (materialName.equalsIgnoreCase("Katapult")){
			mpr = new MatPoolRequest(this,persons * 5,"Katapultmunition",prio + offsetWaffen,comment,kapa_policy,kapa_benutzer);
			if (this.inRegion){
				mpr.setOnlyRegion(true);
			}
			this.addMatPoolRequest(mpr);
		}
		
		// request für Schilde
		int schildPrio = prio;
		if (OP.getOptionBoolean("hinten", false)){
			schildPrio = (int)Math.floor((double)prio * this.hintenFaktor);
		}
		materialName = "Schilde";
		
		String originalMaterialName = materialName;
		
		// Test auf SezielItemgroup
		if (specialAgent && reportSettings.isInCategories("Spezialist" + originalMaterialName)){
			materialName = "Spezialist" + originalMaterialName;
		}
		
		// Test auf SuperSezielItemgroup
		if (specialAgent && reportSettings.isInCategories(ItemGroupPräfix + originalMaterialName)){
			materialName = ItemGroupPräfix + originalMaterialName;
		}
		
		if (!reportSettings.isInCategories(materialName)){
			this.addComment("Material: keine Itemgroup für " + materialName + " bekannt. Benutze: Schild");
			materialName = "Schild";
		}
		
		if (specialAgent){
			this.addComment("Material: benutzte Schild-ItemGroup: " + materialName);
		}
		
		mpr = new MatPoolRequest(this,persons,materialName,schildPrio + offsetSchilde,comment,kapa_policy,kapa_benutzer);
		if (this.inRegion){
			mpr.setOnlyRegion(true);
		}
		this.addMatPoolRequest(mpr);
		
		// wir brauchen info, ob Schwimmer gewünscht
		boolean isSchwimmer = OP.getOptionBoolean("Schwimmen", false);
			
		
		
		// request für die Ruestung
		int rüstPrio = prio;
		if (OP.getOptionBoolean("hinten", false)){
			rüstPrio = (int)Math.floor((double)prio * this.hintenFaktor);
		}
		materialName = "Rüstung";
		String defaultRüstung = "Plattenpanzer";
		
		originalMaterialName = materialName;
		
		// test auf hinten
		if (OP.getOptionBoolean("hinten", false) && reportSettings.isInCategories("RüstungHinten")){
			materialName = "RüstungHinten";
		}
		
		// Test auf SezielItemgroup
		if (specialAgent && reportSettings.isInCategories("Spezialist" + originalMaterialName)){
			materialName = "Spezialist" + originalMaterialName;
		}
		
		// Test auf SuperSezielItemgroup
		if (specialAgent && reportSettings.isInCategories(ItemGroupPräfix + originalMaterialName)){
			materialName = ItemGroupPräfix + originalMaterialName;
		}

		if (isSchwimmer){
			materialName = "SchwimmRüstung";
			defaultRüstung = "Kettenhemd";
		}
		if (!reportSettings.isInCategories(materialName)){
			this.addComment("Material: keine Itemgroup für " + materialName + " bekannt. Benutze: " + defaultRüstung);
			materialName = defaultRüstung;
		}
		
		if (specialAgent){
			this.addComment("Material: benutzte Rüstungs-ItemGroup: " + materialName);
		}
		
		mpr = new MatPoolRequest(this,persons,materialName,rüstPrio + offsetRüstung,comment,kapa_policy,kapa_benutzer);
		if (this.inRegion){
			mpr.setOnlyRegion(true);
		}
		this.addMatPoolRequest(mpr);
		
		// Pferde?!
		if ((OP.getOptionBoolean("Pferd", false) || OP.getOptionBoolean("Pferde", false))
				 && !isSchwimmer){
			// Pferde organisieren
			
			// Gewicht gesetzt?
			int user_pers_gewicht = OP.getOptionInt("pers_gewicht", -1);
			int anz_pferde = persons;
			
			if (user_pers_gewicht>0){
				anz_pferde = (int)Math.ceil(((double)persons * (double)user_pers_gewicht)/20);
			}
			
			if (reitSkill!=null && reitSkill.getLevel()>0){
				mpr = new MatPoolRequest(this,anz_pferde,"Pferd",prio,comment);
				this.addMatPoolRequest(mpr);
				this.addComment("Material: Pferde angefordert.");
			} else {
				this.addComment("Material: nicht genügend Reittalent für Pferde");
			}
		}
		
		
		
		
		if (reportSettings.getOptionString("MaterialWundsalbe", this.region())!=null) {
			this.MaterialWundsalbe = reportSettings.getOptionBoolean("MaterialWundsalbe", this.region());
		}
		
		this.MaterialWundsalbe = OP.getOptionBoolean("MaterialWundsalbe", this.MaterialWundsalbe);
		
		if (this.MaterialWundsalbe) {
			// Wundsalbe
			// um wieviele Wundsalben soll es denn gehen...pro 100 eine als ansatz
			// das vielleicht mal als scripteroption
			// 1 Wundsalbe = 400 Trefferpunkte
			// 1 Halbling 18 TP, ein Mensch 20 TP...Zwerg 24 TP, Troll 30 TP
			// 20 MM -> 400 TP, 50% wundsalbendurchsatz-> 50 Mann 1 Wundsalbe
			// hier das entscheidende Setting
			int personenProWundsalbe = 10;
			// wieviel Wundsalbe sollte diese Einheit also haben?
			int wishWS = persons / personenProWundsalbe ;
			if (wishWS>0){
				super.setPrioParameter(prio,-0.5,0,2);
				for (int i = 1;i<=wishWS;i++){
					int actPrio = super.getPrio(i-1);
					mpr = new MatPoolRequest(this,1,"Wundsalbe",actPrio,comment + "(WS:" + i + ")",kapa_policy,kapa_benutzer);
					if (this.inRegion){
						mpr.setOnlyRegion(true);
					}
					this.addMatPoolRequest(mpr);
				}
			}
		} else {
			this.addComment("Material: keine Anforderung von Wundsalbe, weil nicht gewünscht.");
		}
		
		
		// Heiltrank
		if (specialAgent || isMage){
			mpr = new MatPoolRequest(this,1,"Heiltrank",prio,comment,kapa_policy,kapa_benutzer);
			if (this.inRegion){
				mpr.setOnlyRegion(true);
			}
			this.addMatPoolRequest(mpr);
		}

		// Tarntalent ?
		boolean needRdU = false;
		int prioTarn = prio;
		/*
		if (this.isStealth){
			needRdU=true;
		}
		*/
		needRdU = OP.getOptionBoolean("RdU", false);
		if (isMage){
			// prioTarn = this.magierPrio;
			needRdU=true;
		}

		if (needRdU){
			this.gd_Script.rules.getItemType("Ring der Unsichtbarkeit",true);
			mpr = new MatPoolRequest(this,persons,"Ring der Unsichtbarkeit",prioTarn,comment,kapa_policy,kapa_benutzer);
			if (this.inRegion){
				mpr.setOnlyRegion(true);
			}
			this.addMatPoolRequest(mpr);
		}
		
		//	Wahrnehmung ?
		boolean needAdwS = false;
		prioTarn=prio;
		if (this.isSpotter){
			needAdwS=true;
		}
		
		if (needAdwS){
			this.gd_Script.rules.getItemType("Amulett des wahren Sehens",true);
			mpr = new MatPoolRequest(this,persons,"Amulett des wahren Sehens",prioTarn,comment,kapa_policy,kapa_benutzer);
			if (this.inRegion){
				mpr.setOnlyRegion(true);
			}
			this.addMatPoolRequest(mpr);
		}
		
		// RdM
		if (isMage){
			this.gd_Script.rules.getItemType("Ring der Macht",true);
			mpr = new MatPoolRequest(this,persons,"Ring der Macht",prioTarn,comment,kapa_policy,kapa_benutzer);
			if (this.inRegion){
				mpr.setOnlyRegion(true);
			}
			this.addMatPoolRequest(mpr);
		}
		
		// GdTS
		if (isMage || OP.getOptionBoolean("GdTS", false)){
			this.gd_Script.rules.getItemType("Gürtel der Trollstärke",true);
			mpr = new MatPoolRequest(this,persons,"Gürtel der Trollstärke",prioTarn,comment,kapa_policy,kapa_benutzer);
			if (this.inRegion){
				mpr.setOnlyRegion(true);
			}
			this.addMatPoolRequest(mpr);
		}
		
		// standardmässig requestinfo
		this.scriptUnit.findScriptClass("Requestinfo");
		
	}
	
	
	/**
	 * sollte falsch liefern, wenn nur jeweils einmal pro scriptunit
	 * dieserart script registriert werden soll
	 * wird überschrieben mit return true z.B. in ifregion, ifunit und request...
	 */
	public boolean allowMultipleScripts(){
		return false;
	}
	
}
