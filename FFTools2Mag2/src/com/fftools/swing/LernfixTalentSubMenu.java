package com.fftools.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import magellan.library.GameData;
import magellan.library.rules.SkillType;

import com.fftools.demo.actions.MenuAction;
import com.fftools.demo.actions.lernfix.LernfixTalentAction;
/**
 * SubMenu zur Auswahl, wieviele Personen eine/mehrere Einheiten
 * rekrutieren soll
 * nicht schön, funzt aber
 * @author Fiete
 *
 */
public class LernfixTalentSubMenu extends JMenu{
	private static final long serialVersionUID = 1L;
	
	private SelectionObserver selectionObserver = null;
	
	public LernfixTalentSubMenu(SelectionObserver selectionObserver) {
		super("Lernfix Talente");
		this.setMnemonic("L".charAt(0));
		setEnabled(true);
		this.selectionObserver = selectionObserver;
		
		
	}
	
	/**
	 * wird von aussen durch anderen GameDataListener getriggert
	 * (in LernfixLernplanSubMenu)
	 */
	public void init(){
		// System.out.println("TalentSubMenu init");
		this.removeAll();
		// Ergänzen der bekannten Talente?
		ArrayList<SkillType> skillTypes = new ArrayList<SkillType>();
		GameData data = this.selectionObserver.getClient().getData();
		for (Iterator<SkillType> iter = data.rules.getSkillTypeIterator();iter.hasNext();){
			skillTypes.add(iter.next());
		}
		
		Collections.sort(skillTypes,new SkillTypeComparator(data));
		for (SkillType skillType:skillTypes){
			addMenuItem(this,new LernfixTalentAction(selectionObserver,data.getTranslation(skillType.getName())));
		}
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
	
    /**
     * 
     * a small comparator to compare translated skillNames
     *
     * @author ...
     * @version 1.0, 20.11.2007
     */  
    private class SkillTypeComparator implements Comparator<SkillType> {
      
      // Reference to Translations
      private GameData data=null;
      
      /**
       * constructs new Comparator
       * @param _data
       */
      public SkillTypeComparator(GameData _data){
        this.data = _data;
      }
      
      public int compare(SkillType o1,SkillType o2){
        String s1 = data.getTranslation(o1.getName());
        String s2 = data.getTranslation(o2.getName());
        return s1.compareToIgnoreCase(s2);
      }
    }
    
    
    
    
}
