package com.fftools.swing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import magellan.client.Client;
import magellan.client.event.SelectionEvent;
import magellan.client.event.SelectionListener;


/**
 * damit nicht jede classe auf events warten muss..machjts nur diese classe
 * @author Fiete
 *
 */
public class SelectionObserver implements SelectionListener{

	@SuppressWarnings("rawtypes")
	private Collection selectedObjects = null;
	private Client client = null;
	
	
	
	public SelectionObserver(Client client) {
		this.client = client;
		client.getDispatcher().addSelectionListener(this);
	}

	/**
	 * Bei Änderungen in der selection lediglich unsere
	 * Liste aktualisieren
	 */
	public void selectionChanged(SelectionEvent se) {
		this.selectedObjects = se.getSelectedObjects();
	}

	/**
	 * @return the client
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * @return the selectedObjects
	 */
	@SuppressWarnings("unchecked")
	public Collection<Object> getSelectedObjects() {
		return selectedObjects;
	}
	
	/**
	 * liefert eine ArrayList<object> mit Objecten der Klasse c
	 * @param c "Filter"-Klasse
	 * @return ArrayList oder null
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Object> getObjectsOfClass(Class<?> c){
		ArrayList<Object> erg = null;
		for (Iterator<Object> iter = this.selectedObjects.iterator();iter.hasNext();){
			Object o = iter.next();
			if (c.isInstance(o)){
				if (erg==null){
					erg = new ArrayList<Object>();
				}
				erg.add(o);
			}
		}
		return erg;
	}
	
}
