package com.fftools.magellan2;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import magellan.client.Client;
import magellan.client.event.EventDispatcher;
import magellan.client.extern.MagellanPlugIn;
import magellan.client.extern.MainMenuProvider;
import magellan.client.swing.context.MapContextMenuProvider;
import magellan.client.swing.context.UnitContextMenuProvider;
import magellan.client.swing.preferences.PreferencesFactory;
import magellan.library.CoordinateID;
import magellan.library.GameData;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.library.utils.logging.Logger;

import com.fftools.swing.FFToolsMenu;
import com.fftools.swing.MapContextMenu;
import com.fftools.swing.UnitSubmenu;

public class ConnectorPlugin implements MagellanPlugIn, MainMenuProvider, UnitContextMenuProvider, MapContextMenuProvider, ActionListener{
	

	private static final Logger log = Logger.getInstance(ConnectorPlugin.class);
	  
	  
	
	  /**
	   * our Client
	   */
	  private Client client=null;
	  
	  private UnitSubmenu unitContextMenu = null;
	  
	  private MapContextMenu mapContextMenu = null;
	  
	  
	  /**
	   * Returns the Name of the PlugIn. This name will
	   * be presented to the user in the options panel.
	   */
	  public String getName(){
		  return "FFTools2-Mag2";
	  }
	  
	  /**
	   * This method is called during client start up
	   * procedure. You can use this method to initialize
	   * your PlugIn (load preferences and so on...)
	   * 
	   * @param client     the main application
	   * @param properties the already loaded configuration
	   */
	  public void init(Client client, Properties properties){
		log.info("pluginInit (client):" + getName());
		this.client = client;
		unitContextMenu = new UnitSubmenu(this.client);
		unitContextMenu.setNamePrefix("FFTools ");
		
		mapContextMenu = new MapContextMenu(this.client);
		
	  }
	  
	  /**
	   * This method is called everytime the user has load a
	   * file into Magellan (open or add). You should use
	   * this method to load report specific informations.
	   * 
	   * @param data the loaded and merged gamedata
	   */
	  public void init(GameData data){
		  log.info("pluginInit (GameData):" + getName());   
	  }
	  
	  /**
	   * Returns the menu items that should be added to the
	   * Magellan PlugIn menu. You can return multiple menu
	   * items for every kind of action that is available
	   * in your PlugIn.
	   */
	  public List<JMenuItem> getMenuItems(){
		  return null;
	  }
	  
	  
	  public JMenu getJMenu(){
		  return new FFToolsMenu(client);
	  }
	  
	  /**
	   * This method is called whenever the application
	   * stops.
	   */
	  public void quit(boolean storeSettings){
		  log.info("pluginQuit (client):" + getName());   
	  }

	  /**
	   * handels the event that one of our Items was selected
	   * @param e the event
	   */
	  public void actionPerformed(ActionEvent e) {
	    
	  }

	/**
	 * 
	 */
	public PreferencesFactory getPreferencesProvider() {
		return null;
	}
	
  /**
   * @see magellan.client.extern.MagellanPlugIn#getDocks()
   */
  public Map<String, Component> getDocks() {
    return null;
  }
  
    
  
    /* (non-Javadoc)
	 * @see magellan.client.swing.context.UnitContextMenuProvider#createContextMenu(magellan.client.event.EventDispatcher, magellan.library.GameData, magellan.library.Unit, java.util.Collection)
	 */
	@SuppressWarnings("rawtypes")
	public JMenuItem createContextMenu(EventDispatcher dispatcher,GameData data, Unit unit, Collection selectedObjects) {
		if (unitContextMenu==null){
			unitContextMenu = new UnitSubmenu(this.client);
			unitContextMenu.setNamePrefix("FFTools ");
		}
		
		return unitContextMenu;
	}
	
	
	/**
	 * Map Context Menu bei RechtsClick auf Karte
	 */
	public JMenuItem createMapContextMenu(EventDispatcher dispatcher,
			GameData data) {
		if (mapContextMenu==null){
			mapContextMenu = new MapContextMenu(this.client);
		}
		return mapContextMenu;
	}

	
	/**
	 * Rechtsclick auf Map...
	 */
	public void update(Region r) {
		if (mapContextMenu==null){
			mapContextMenu = new MapContextMenu(this.client);
		}
		mapContextMenu.setRegion(r);
	}

	public void updateUnknownRegion(CoordinateID c) {
		// TODO Auto-generated method stub
		
	}

}
