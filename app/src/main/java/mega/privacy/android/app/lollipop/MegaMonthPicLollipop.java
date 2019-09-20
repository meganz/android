package mega.privacy.android.app.lollipop;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.LogUtil.*;


public class MegaMonthPicLollipop {

	public String monthYearString;
	public ArrayList<Long> nodeHandles;
	private Map<Long, Long> nodesSite = new TreeMap<>();
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
		logDebug("Node is " + node);
		logDebug("getPostion return null " + (nodesSite.get(new Long(node)) == null));
		return nodesSite.get(new Long(node));
	}

	public long getPosition(Long node){
		logDebug("Node is " + node);
		logDebug("getPostion return null " + (nodesSite.get(node) == null));
		return nodesSite.get(node);
	}

	public ArrayList<Long> getNodeHandles(){
		return nodeHandles;
	}
}
