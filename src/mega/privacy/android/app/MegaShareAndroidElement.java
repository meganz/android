package mega.privacy.android.app;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;


public class MegaShareAndroidElement {
	
	MegaUser user;
	MegaNode node;
	boolean repeat=false;
	
	public boolean isRepeat() {
		return repeat;
	}

	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	public MegaShareAndroidElement(MegaUser _user, MegaNode _node) {
		super();
		this.user = _user;
		this.node = _node;
	}	
	
	public MegaUser getUser() {
		return user;
	}
	public void setUser(MegaUser _user) {
		this.user = _user;
	}
	public MegaNode getNode() {
		return node;
	}
	public void setNL(MegaNode _node) {
		this.node = _node;
	}
	
	

}
