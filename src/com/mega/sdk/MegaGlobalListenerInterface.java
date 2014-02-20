package com.mega.sdk;

public interface MegaGlobalListenerInterface 
{
	public void onUsersUpdate(MegaApiAndroid api);
	public void onNodesUpdate(MegaApiAndroid api);
	public void onReloadNeeded(MegaApiAndroid api);
}
