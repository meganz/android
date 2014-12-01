package com.mega.android;

import java.util.ArrayList;

import com.mega.sdk.MegaNode;

public class MegaMonthPic {
	
	String monthYearString;
	ArrayList<Long> nodeHandles;
	int numRows;

	MegaMonthPic(String monthYearString, ArrayList<Long> nodeHandles){
		this.monthYearString = monthYearString;
		this.nodeHandles = nodeHandles;
	}

	public MegaMonthPic() {
		this.nodeHandles = new ArrayList<Long>();
	}
	
}
