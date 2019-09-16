package mega.privacy.android.app.lollipop;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaNode;


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
		LogUtil.logDebug("Node is " + node);
		LogUtil.logDebug("getPostion return null " + (nodesSite.get(new Long(node)) == null));
		return nodesSite.get(new Long(node));
	}

	public long getPosition(Long node){
		LogUtil.logDebug("Node is " + node);
		LogUtil.logDebug("getPostion return null " + (nodesSite.get(node) == null));
		return nodesSite.get(node);
	}

	public ArrayList<Long> getNodeHandles(){
		return nodeHandles;
	}
}
