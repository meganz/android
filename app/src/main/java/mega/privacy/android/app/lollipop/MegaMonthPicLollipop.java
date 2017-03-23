package mega.privacy.android.app.lollipop;

import java.util.ArrayList;


public class MegaMonthPicLollipop {

	public String monthYearString;
	public ArrayList<Long> nodeHandles;
	int numRows;

	MegaMonthPicLollipop(String monthYearString, ArrayList<Long> nodeHandles){
		this.monthYearString = monthYearString;
		this.nodeHandles = nodeHandles;
	}

	public MegaMonthPicLollipop() {
		this.nodeHandles = new ArrayList<Long>();
	}
	
}
