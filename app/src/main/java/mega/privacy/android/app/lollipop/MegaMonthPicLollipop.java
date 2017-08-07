package mega.privacy.android.app.lollipop;

import java.util.ArrayList;
import java.util.HashMap;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaNode;


public class MegaMonthPicLollipop {

	public String monthYearString;
	public ArrayList<Long> nodeHandles;
	private HashMap<Long, Long> nodesSite = new HashMap<>();
	int numRows;

	MegaMonthPicLollipop(String monthYearString, ArrayList<Long> nodeHandles){
		this.monthYearString = monthYearString;
		this.nodeHandles = nodeHandles;
	}

	public MegaMonthPicLollipop() {
		this.nodeHandles = new ArrayList<Long>();
	}

	public void setPosition(MegaNode node, long position){
		if(!nodesSite.containsKey(new Long(node.getHandle()))){
			nodesSite.put(new Long(node.getHandle()), new Long(position));
		}
	}

	public long getPosition(MegaNode node){
		return nodesSite.get(new Long(node.getHandle()));
	}

	public long getPosition(long node){
		log("node is "+node);
		log("getPostion return null "+(nodesSite.get(new Long(node)) == null));
		return nodesSite.get(new Long(node));
	}

	private static void log(String txt){
		Util.log("MegaMonthPicLollipop", txt);
	}
	
}
