package com.mega.sdk;

public interface MegaGlobalListenerInterface 
{
	public void onUsersUpdate(MegaApiJava api);
	public void onNodesUpdate(MegaApiJava api);
	public void onReloadNeeded(MegaApiJava api);
}
