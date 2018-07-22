package com.fftools.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import magellan.library.event.GameDataEvent;
import magellan.library.event.GameDataListener;

import com.fftools.ReportSettings;
import com.fftools.demo.actions.MenuAction;
import com.fftools.demo.actions.lernfix.LernFixReadCRAction;
import com.fftools.demo.actions.lernfix.LernfixLernplanAction;
import com.fftools.pools.ausbildung.Lernplan;
import com.fftools.pools.ausbildung.LernplanHandler;
/**
 * SubMenu zur Auswahl, wieviele Personen eine/mehrere Einheiten
 * rekrutieren soll
 * nicht schön, funzt aber
 * @author Fiete
 *
 */
public class LernfixLernplanSubMenu extends JMenu implements GameDataListener{
	private static final long serialVersionUID = 1L;
	private static final ReportSettings reportSettings = ReportSettings.getInstance();
	
	private SelectionObserver selectionObserver = null;
	
	private LernfixTalentSubMenu talentSubMenu = null;
	
	public LernfixLernplanSubMenu(SelectionObserver selectionObserver) {
		super("Lernfix");
		this.setMnemonic("L".charAt(0));
		setEnabled(true);
		this.selectionObserver = selectionObserver;
		talentSubMenu = new LernfixTalentSubMenu(this.selectionObserver);
		init_initial();
		selectionObserver.getClient().getDispatcher().addGameDataListener(this);
		
	}
	
	private void init_initial(){
		addMenuItem(this, new LernFixReadCRAction(this.selectionObserver,this));
		this.addSeparator();
		this.add(talentSubMenu);
	}
	
	/**
	 * Übernimmt aus den Units die Lernpläne
	 * wird NUR vom LernFixReadCRAction nach Einlesen aufgerufen
	 */
	public void init(){
		// Ergänzen der bekannten Lehrpläne
		// haben wir welche im LPH ?
		this.removeAll();
		LernplanHandler LPH = reportSettings.getScriptMain().getOverlord().getLernplanHandler();
		ArrayList<Lernplan> lernPläne = LPH.getSortedLernPläne();
		if (lernPläne!=null && lernPläne.size()>0){
			// System.out.println("ergänze Lernpläne");
			for (Lernplan lernPlan:lernPläne){
				if (!(lernPlan.getName().indexOf("%")>0)){
				   addMenuItem(this,new LernfixLernplanAction(selectionObserver,lernPlan));
				   // System.out.println("Add: " + lernPlan.getName());
				}
			}
		} else {
			// System.out.println("keine Lernpläne");
		}
		this.addSeparator();
		init_initial();
		validate();
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
	
    
    public void gameDataChanged(GameDataEvent e){
    	// Lernpläne validieren -> löschen
    	removeAll();
    	init_initial();
    	talentSubMenu.init();
    	validate();
    }
    
    
}
