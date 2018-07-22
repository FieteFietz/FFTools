package com.fftools.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Hilfstools zum Umgang mit ArrayLists
 * @author Fiete
 *
 */
public class FFToolsArrayList {
	
	/**
	 * Normalisiert eine ArrayList von PriorityUsers
	 * (bringt alle Prios auf einen Bereich von MinValue bis MAxValue
	 * 
	 * @param list list of PriorityUsers
	 * @param finalMaxValue
	 * @param finalMinValue
	 */
	public static void normalizeArrayList(ArrayList<PriorityUser> list,int finalMaxValue,int finalMinValue){
		
		// checks
		if (list == null || list.isEmpty()){
			return;
		}
		
		if (finalMinValue>=finalMaxValue){
			return;
		}
		
		//		 max und min rausfinden
		int maxPrio = Integer.MIN_VALUE;
		int minPrio = Integer.MAX_VALUE;
		for (Iterator<PriorityUser> iter = list.iterator();iter.hasNext();){
			PriorityUser entry = (PriorityUser)iter.next();
			maxPrio = Math.max(maxPrio, entry.getPrio());
			minPrio = Math.min(minPrio, entry.getPrio());
		}
		
		int istPrioSpannweite = maxPrio - minPrio;
		int sollPrioSpannweite = finalMaxValue - finalMinValue;
		double spannweitenFaktor = (double)istPrioSpannweite / (double)sollPrioSpannweite;
		for (Iterator<PriorityUser> iter = list.iterator();iter.hasNext();){
			PriorityUser entry = (PriorityUser)iter.next();
			int actPrio = entry.getPrio();
			// auf abstand von null bringen
			actPrio -= minPrio;
			// factor ansetzen
			actPrio = (int)((double)actPrio/(double)spannweitenFaktor);
			// auf soll abstand von null bringen
			actPrio += finalMinValue;
			entry.setPrio(actPrio);
		}
		
	}
	
	
	
	/**
	 * Überprüft, ob eine Collection eine Instanz einer 
	 * bestimmten Classe enthält.
	 * @param c
	 * @param t
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean containsClass(Collection c, Class<?> t){
		if (c==null || c.isEmpty()){
			return false;
		}
		
		for (Iterator<Object> iter = c.iterator();iter.hasNext();){
			Object o = iter.next();
			if (t.isInstance(o)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Zählt, wie oft eine Instanz einer bestimmten Klasse in einer 
	 * Collection vorkommt
	 * @param c
	 * @param t
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int countClass(Collection c, Class<?> t){
		if (c==null || c.isEmpty()){
			return 0;
		}
		int erg=0;
		for (Iterator<Object> iter = c.iterator();iter.hasNext();){
			Object o = iter.next();
			if (t.isInstance(o)){
				erg++;;
			}
		}
		return erg;
	}
	
	
}
