package com.fftools.demo.actions.lernfix;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import magellan.client.event.OrderConfirmEvent;
import magellan.client.event.UnitOrdersEvent;
import magellan.library.Unit;

import com.fftools.demo.actions.MenuAction;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;

/**
 * Setzt das Rekrutieren um
 * @author Fiete
 *
 */
public class LernfixTalentAction extends MenuAction{
	private static final long serialVersionUID = 1L;
	private String talentName = null;
	private SelectionObserver selectionObserver = null;

	
	public LernfixTalentAction(SelectionObserver selectionObserver, String talentName) {
		super(selectionObserver.getClient());
		setName("Lernfix " + talentName);
		this.selectionObserver = selectionObserver;
		this.talentName = talentName;
	}
	

	private void makeIt(Unit u){
		// Update/Add Orders
		u.addOrder("// Tipp: ziel=[maxLevel]");
		u.addOrder("// script Lernfix Talent=" + this.talentName);
		u.setOrdersConfirmed(true);
		List<Unit> units = new LinkedList<Unit>();
		units.add(u);
		this.selectionObserver.getClient().getDispatcher().fire(new OrderConfirmEvent(this, units));
		// client refresh
		this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this, u));
	}
	
	public void setCaption(String text){
		setName(text);
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
				this.makeIt(actUnit);
			}
		} else {
			new MsgBox(this.selectionObserver.getClient(),"Kein Lernfix möglich.","Fehler",true);
		}
	}


	/**
	 * @return the talentName
	 */
	public String getTalentName() {
		return talentName;
	}


	/**
	 * @param talentName the talentName to set
	 */
	public void setTalentName(String talentName) {
		this.talentName = talentName;
	}
	
}
