package com.fftools.scripts;

import com.fftools.transport.TransportManager;


/**
 * allesm was mit Transportern zu tun hat...
 * @author Fiete
 *
 */
public class TransportScript extends TradeAreaScript {
	
	private TransportManager transportManager = null;
	
	
	public TransportScript(){
		
	}
	
	public TransportManager getTransportManager(){
		if (this.transportManager==null){
			this.transportManager = this.scriptUnit.getScriptMain().getOverlord().getTransportManager();
		}
		return this.transportManager;
	}
	
	
}
