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
import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.swing.SelectionObserver;
import com.fftools.utils.MsgBox;

/**
 * Setzt das Rekrutieren um
 * @author Fiete
 *
 */
public class LernfixLernplanAction extends MenuAction{
	private static final long serialVersionUID = 1L;
	private Lernplan lernPlan = null;
	private SelectionObserver selectionObserver = null;

	
	public LernfixLernplanAction(SelectionObserver selectionObserver, Lernplan lernPlan) {
		super(selectionObserver.getClient());
		setName("Lernfix " + lernPlan.getName());
		this.selectionObserver = selectionObserver;
		this.lernPlan = lernPlan;
	}
	

	private void makeIt(Unit u){
		// Update/Add Orders
		u.addOrder("// Tipp: ziel=[maxLevel]");
		u.addOrder("// script Lernfix Lernplan=" + this.lernPlan.getName());
		u.setOrdersConfirmed(true);
		List<Unit> units = new LinkedList<Unit>();
		units.add(u);
		this.selectionObserver.getClient().getDispatcher().fire(new OrderConfirmEvent(this, units));
		// client refresh
		this.selectionObserver.getClient().getDispatcher().fire(new UnitOrdersEvent(this, u));
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
	
}
