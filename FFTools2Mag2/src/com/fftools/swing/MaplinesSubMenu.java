package com.fftools.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import magellan.client.Client;

import com.fftools.demo.actions.MenuAction;
import com.fftools.demo.actions.map.HideMaplinesTAC_Action;
import com.fftools.demo.actions.map.ShowMaplinesTAC_Action;

public class MaplinesSubMenu extends JMenu {
	
	private static final long serialVersionUID = 0;
	
	
	public MaplinesSubMenu(Client client) {
		super("MapLines");
		this.setMnemonic("M".charAt(0));
	    addMenuItem(this,new ShowMaplinesTAC_Action(client));
	    addMenuItem(this,new HideMaplinesTAC_Action(client));
	  
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
