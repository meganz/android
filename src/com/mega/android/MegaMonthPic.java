package com.mega.android;

import java.util.ArrayList;

import com.mega.sdk.MegaNode;

public class MegaMonthPic {
	
	String monthYearString;
	ArrayList<Long> nodeHandles;

	MegaMonthPic(String monthYearString, ArrayList<Long> nodes){
		this.monthYearString = monthYearString;
		this.nodeHandles = nodeHandles;
	}

	public MegaMonthPic() {
		this.nodeHandles = new ArrayList<Long>();
	}
	
}
