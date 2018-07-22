package com.fftools.overlord;

/**
 * alles, was der Overlord von scripten und Managern wissen muss
 * @author Fiete
 *
 */


public interface OverlordInfo {
	
	/**
	 * liefert array of int, in welchem durchlauf die manager aufgerufen werden sollen
	 * @return
	 */
	public int[] runAt();
}
