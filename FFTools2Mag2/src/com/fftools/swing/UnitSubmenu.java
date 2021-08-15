package com.fftools.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import magellan.client.Client;
import magellan.client.event.SelectionEvent;
import magellan.client.event.SelectionListener;
import magellan.library.Unit;

import com.fftools.demo.actions.MenuAction;
import com.fftools.demo.actions.lernfix.LernfixTalentAction;
import com.fftools.demo.actions.unit.BauarbeiterAction;
import com.fftools.demo.actions.unit.ClearOrdersAction;
import com.fftools.demo.actions.unit.DepotAction;
import com.fftools.demo.actions.unit.DepotWahrnehmerAction;
import com.fftools.demo.actions.unit.HandelnAction;
import com.fftools.demo.actions.unit.KlonenAction;
import com.fftools.demo.actions.unit.KräuterAction;
import com.fftools.demo.actions.unit.MaterialAction;
import com.fftools.demo.actions.unit.PferdeAction;
import com.fftools.demo.actions.unit.RohstoffAction;
import com.fftools.demo.actions.unit.SetAvoidRegionAction;
import com.fftools.demo.actions.unit.SetOKAction;
import com.fftools.demo.actions.unit.TransporterAction;
import com.fftools.demo.actions.unit.UnterhaltenAction;
import com.fftools.demo.actions.unit.WahrnehmerAction;
import com.fftools.demo.actions.unit.ZupferAction;
import com.fftools.utils.FFToolsArrayList;
import com.fftools.utils.FFToolsUnits;

public class UnitSubmenu extends JMenu implements SelectionListener {
	private static final long serialVersionUID = 1L;
	private SelectionObserver selectionObserver = null;
	
	private String namePrefix = "";
	
	private LernfixTalentAction iTalent = null;
	
	public UnitSubmenu(Client client) {
		super("Einheit");
		this.setMnemonic("E".charAt(0));
		setEnabled(false);
		
		if (this.selectionObserver==null){
			this.selectionObserver = new SelectionObserver(client);
		}

		this.add(new RekrutierenSubMenu(this.selectionObserver));
		this.add(new TeilenSubMenu(this.selectionObserver));
		this.add(new TempSubMenu(this.selectionObserver));
		this.add(new LernfixLernplanSubMenu(this.selectionObserver));
		this.iTalent = new LernfixTalentAction(this.selectionObserver,"idle");
		this.add(this.iTalent);
		update_iTalent();
		addSeparator();
		addMenuItem(this,new KlonenAction(this.selectionObserver));
		addMenuItem(this,new MaterialAction(this.selectionObserver));
		addMenuItem(this,new MaterialAction(this.selectionObserver,true));
		addMenuItem(this,new DepotAction(this.selectionObserver));
		addMenuItem(this,new DepotWahrnehmerAction(this.selectionObserver));
		addMenuItem(this,new HandelnAction(this.selectionObserver));
		addMenuItem(this,new TransporterAction(this.selectionObserver));
		addMenuItem(this,new UnterhaltenAction(this.selectionObserver));
		addMenuItem(this,new WahrnehmerAction(this.selectionObserver));
		addMenuItem(this,new KräuterAction(this.selectionObserver));
		addMenuItem(this,new ZupferAction(this.selectionObserver));
		addMenuItem(this,new PferdeAction(this.selectionObserver));
		addMenuItem(this,new RohstoffAction(this.selectionObserver));
		addMenuItem(this,new BauarbeiterAction(this.selectionObserver));
		addSeparator();
		addMenuItem(this,new SetAvoidRegionAction(this.selectionObserver));
		addMenuItem(this,new ClearOrdersAction(this.selectionObserver));
		addMenuItem(this,new SetOKAction(this.selectionObserver));
		client.getDispatcher().addSelectionListener(this);
		
	}
	
	
	private void update_iTalent(){
		if (this.iTalent==null){
			return;
		}
		
		// System.out.println("updating iTalent");
		
		this.iTalent.setCaption("iTalent");
		this.iTalent.setEnabled(false);
		if (this.selectionObserver.getSelectedObjects()!=null && this.selectionObserver.getSelectedObjects().size()==1){
			// System.out.println("updating iTalent 1");
			Object[] objects = this.selectionObserver.getSelectedObjects().toArray();
			Object o = objects[0];
			if (o instanceof Unit){
				// System.out.println("updating iTalent 2");
				String name = FFToolsUnits.getBestSkillTypeName((Unit)o);
				if (name!=null && name.length()>0){
					// System.out.println("updating iTalent 3");
					this.iTalent.setTalentName(name);
					this.iTalent.setCaption("iTalent->" + name);
					this.iTalent.setEnabled(true);
				}
			}
		}
	}
	
	
	public void selectionChanged(SelectionEvent se) {
		// System.out.println("Unitsubmenu selection changed");
		if (se.getSelectedObjects()!=null && 
			FFToolsArrayList.containsClass(se.getSelectedObjects(), Unit.class)){
			setEnabled(true);			
			this.update_iTalent();
			if (FFToolsArrayList.countClass(se.getSelectedObjects(), Unit.class)>1){
				setText(this.namePrefix + "Einheiten");
				
			} else {
				setText(this.namePrefix + "Einheit");
			}
		} else {
			setText(this.namePrefix + "Einheit");
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
    
    /**
	 * @return the namePrefix
	 */
	public String getNamePrefix() {
		return namePrefix;
	}


	/**
	 * @param namePrefix the namePrefix to set
	 */
	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}
	
	
}
