/*
 *  Copyright (C) 2000-2004 Roger Butenuth, Andreas Gampe,
 *                          Stefan Goetz, Sebastian Pappert,
 *                          Klaas Prause, Enno Rehling,
 *                          Sebastian Tusk, Ulrich Kuester,
 *                          Ilja Pavkovic
 *
 * This file is part of the Eressea Java Code Base, see the
 * file LICENSING for the licensing information applying to
 * this file.
 *
 */

package com.fftools.demo.actions.unit;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import magellan.client.event.UnitOrdersEvent;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.rules.SkillType;

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;


/**
 * MenuAction zum Rekrutieren einer bestimmten Anzahl von Personen
 *
 * @author Fiete
 * @version
 */
public class MaterialAction extends MenuAction {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver;
	
	private boolean pferde=false;
	
	
	/**
	 * Creates a new FFToolsOptionsAction object.
	 *
	 * @param parent TODO: DOCUMENT ME!
	 */
	public MaterialAction(SelectionObserver selectionObserver) {
        super(selectionObserver.getClient());
        setName("Material");
        this.selectionObserver = selectionObserver; 
	}
	
	public MaterialAction(SelectionObserver selectionObserver, boolean Pferde) {
		super(selectionObserver.getClient());
		setName("Material Pferde");
        this.selectionObserver = selectionObserver;
        this.pferde = Pferde;
	}
	

	/**
	 * TODO: DOCUMENT ME!
	 *
	 * @param e TODO: DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		ArrayList<Object> units = this.selectionObserver.getObjectsOfClass(Unit.class);
		if (units!=null && units.size()>0){
			for (Iterator<Object> iter = units.iterator();iter.hasNext();){
				Unit actUnit = (Unit)iter.next();
				this.setMaterial(actUnit);
			}
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Kein Material möglich.","Fehler",true);
		}
	}

	/**
	 * führt den setMaterial für diese Unit durch
	 * @param u
	 */
	private void setMaterial(Unit u){
		boolean hinten=true;
		// bestSkillType feststellen
		SkillType bestSkillType = null;
		int bestSkillLevel = 0;
		
		for (Iterator<Skill> iter = u.getModifiedSkills().iterator();iter.hasNext();){
			Skill actSkill = (Skill)iter.next();
			if (actSkill.getLevel()>=bestSkillLevel){
				bestSkillType = actSkill.getSkillType();
				bestSkillLevel = actSkill.getLevel();
			}
		}
		String bestSkillTypeName = bestSkillType.getName();
		if (bestSkillTypeName.equalsIgnoreCase("Hiebwaffen") ||
				bestSkillTypeName.equalsIgnoreCase("Stangenwaffen") ){
			hinten=false;
		}
		
		String order = "// script Material";
		if (hinten){
			order+=" hinten=an";
		}
		
		if (this.pferde){
			order+=" pferde=an";
		}
		
		u.addOrder( order);
		this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this,u));
	}
}
