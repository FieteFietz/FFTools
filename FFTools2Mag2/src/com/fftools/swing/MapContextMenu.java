package com.fftools.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.fftools.demo.actions.MenuAction;
import com.fftools.demo.actions.map.GoToAction;
import com.fftools.demo.actions.map.RideAction;
import com.fftools.demo.actions.map.SailToAction;
import com.fftools.demo.actions.map.SailToAndBackAction;
import com.fftools.utils.FFToolsArrayList;

import magellan.client.Client;
import magellan.client.event.SelectionEvent;
import magellan.client.event.SelectionListener;
import magellan.library.Region;
import magellan.library.Unit;

public class MapContextMenu extends JMenu implements SelectionListener {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver = null;
	
	
	
	private GoToAction goToAction = null;
	private SailToAction sailToAction = null;
	private RideAction rideAction = null;
	private SailToAndBackAction sailToAndBackAction = null;

	public void setRegion(Region region) {
		
		
		if (goToAction!=null){
			goToAction.setTargetRegion(region);
		}
		if (sailToAction!=null){
			sailToAction.setTargetRegion(region);
		}
		if (rideAction!=null) {
			rideAction.setTargetRegion(region);
		}
		if (sailToAndBackAction!=null){
			sailToAndBackAction.setTargetRegion(region);
		}
		
		
	}





	public MapContextMenu(Client client) {
		super("FFTools");
		this.setMnemonic("F".charAt(0));
		setEnabled(true);
		
		if (this.selectionObserver==null){
			this.selectionObserver = new SelectionObserver(client);
		}

		
		// addMenuItem(this,new KlonenAction(this.selectionObserver));
		goToAction = new GoToAction(this.selectionObserver);
		rideAction = new RideAction(this.selectionObserver);
		sailToAction = new SailToAction(this.selectionObserver);
		sailToAndBackAction = new SailToAndBackAction(this.selectionObserver);
		
		
		
		addMenuItem(this,goToAction);
		addMenuItem(this,rideAction);
		addMenuItem(this,sailToAction);
		addMenuItem(this,sailToAndBackAction);

		client.getDispatcher().addSelectionListener(this);
		
	}
	
	
	
	
	
	public void selectionChanged(SelectionEvent se) {
		// System.out.println("Unitsubmenu selection changed");
		if (se.getSelectedObjects()!=null && 
			FFToolsArrayList.containsClass(se.getSelectedObjects(), Unit.class)){
			setEnabled(true);			

		} else {
			setEnabled(false);
		}
		
	}
	
	/**
     * Following 2 structures are copied from Client.java and made local
     * 
     * 
     * Adds a new menu item to the specifie menu associating it with the
     * specified action, setting its mnemonic and registers its accelerator if
     * it has one.
     * 
     * @param parentMenu
     *            TODO: DOCUMENT ME!
     * @param action
     *            TODO: DOCUMENT ME!
     * 
     * @return the menu item created.
     */
    private JMenuItem addMenuItem(JMenu parentMenu, MenuAction action) {
        JMenuItem item = parentMenu.add(action);
        new MenuActionObserver(item, action);

        return item;
    }
    
//  /////////////////
    // INNER Classes //
    // /////////////////
    private class MenuActionObserver implements PropertyChangeListener {
        protected JMenuItem item;

        /**
         * Creates a new MenuActionObserver object.
         * 
         * @param item
         *            TODO: DOCUMENT ME!
         * @param action
         *            TODO: DOCUMENT ME!
         */
        public MenuActionObserver(JMenuItem item, Action action) {
            this.item = item;
            action.addPropertyChangeListener(this);
        }

        /**
         * TODO: DOCUMENT ME!
         * 
         * @param e
         *            TODO: DOCUMENT ME!
         */
        public void propertyChange(PropertyChangeEvent e) {
            if ((e.getPropertyName() != null)
                    && e.getPropertyName().equals("accelerator")) {
                item.setAccelerator((KeyStroke) e.getNewValue());
            }
        }
    }
	
}
