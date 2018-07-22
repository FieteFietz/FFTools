package com.fftools.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import magellan.client.Client;

import com.fftools.demo.actions.FFToolsInfoAction;
import com.fftools.demo.actions.FFToolsOptionsAction;
import com.fftools.demo.actions.FFToolsRunScriptAction;
import com.fftools.demo.actions.MenuAction;

public class FFToolsMenu extends JMenu {
	private static final long serialVersionUID = 1L;

	public FFToolsMenu(Client client) {
		super("FFTools");
		this.setMnemonic("F".charAt(0));
		FFToolsOptionsAction optionsAction = new FFToolsOptionsAction(client);
		optionsAction.setEnabled(false);
	    addMenuItem(this,optionsAction);
	    addMenuItem(this,new FFToolsRunScriptAction(client));
	    //addMenuItem(this,new FFToolsThreadTestAction(client));
	    this.add(new TagSubMenu(client));
	    this.add(new MaplinesSubMenu(client));
	    this.add(new RegionIconsSubMenu(client));
	    this.addSeparator();
	    this.add(new UnitSubmenu(client));
	    this.addSeparator();
	    addMenuItem(this,new FFToolsInfoAction(client));
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
