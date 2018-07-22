package com.fftools.utils;

/**
 * abstract class for all Objects in ArrayLists
 * which should be normalized once using
 * FFToolsArrayLists
 * @author Fiete
 *
 */
public class PriorityUser {
	
	private int prio = 0;
	
	/**
	 * returning the Priority og a thing
	 * @return
	 */
	public int getPrio(){
		return prio;
	}
	
	/**
	 * sets the new prio
	 * @param newPrio
	 */
	public void setPrio(int newPrio){
		this.prio = newPrio;
	}
	
}
