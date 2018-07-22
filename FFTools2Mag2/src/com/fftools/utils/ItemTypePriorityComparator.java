package com.fftools.utils;

import java.util.Comparator;

import magellan.library.rules.ItemType;

import com.fftools.ReportSettings;

public class ItemTypePriorityComparator implements Comparator<ItemType>{
	private static final ReportSettings reportSettings = ReportSettings.getInstance();

	private String catName = "";
	
	public ItemTypePriorityComparator(String _catName){
		if (reportSettings.isInCategories(_catName)){
			this.catName = _catName;
		}
	}
	
	public int compare(ItemType o1,ItemType o2){
		if (catName.length()==0){
			// Kategorie nicht gefunden..
			return 0;
		}
		int prio1 = reportSettings.getReportSettingPrio(this.catName,o1);
		int prio2 = reportSettings.getReportSettingPrio(this.catName,o2);
		return prio2-prio1;
	}
	
}
