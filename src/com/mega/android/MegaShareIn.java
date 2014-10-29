package com.mega.android;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;


public class MegaShareIn {
	
	MegaUser user;
	MegaNode node;	
	
	public MegaShareIn(MegaUser _user, MegaNode _node) {
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
