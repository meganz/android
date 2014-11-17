package com.mega.sdk;

import java.util.ArrayList;

public interface MegaGlobalListenerInterface 
{
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users);
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes);
	public void onReloadNeeded(MegaApiJava api);
}
