package nz.mega.android.lollipop;

import java.util.ArrayList;

import nz.mega.sdk.MegaNode;


public class MegaMonthPicLollipop {
	
	String monthYearString;
	ArrayList<Long> nodeHandles;
	int numRows;

	MegaMonthPicLollipop(String monthYearString, ArrayList<Long> nodeHandles){
		this.monthYearString = monthYearString;
		this.nodeHandles = nodeHandles;
	}

	public MegaMonthPicLollipop() {
		this.nodeHandles = new ArrayList<Long>();
	}
	
}
