package nz.mega.sdk;

import java.util.ArrayList;

class DelegateMegaGlobalListener extends MegaGlobalListener
{
	MegaApiJava megaApi;
	MegaGlobalListenerInterface listener;
	
	DelegateMegaGlobalListener(MegaApiJava megaApi, MegaGlobalListenerInterface listener)
	{
		this.megaApi = megaApi;
		this.listener = listener;
	}
	
	MegaGlobalListenerInterface getUserListener()
	{
		return listener;
	}
	
	@Override
	public void onUsersUpdate(MegaApi api, MegaUserList userList) 
	{
		if(listener != null)
		{
			final ArrayList<MegaUser> users = MegaApiJava.userListToArray(userList);
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onUsersUpdate(megaApi, users);
			    }
			});
		}
	}

	@Override
	public void onNodesUpdate(MegaApi api, MegaNodeList nodeList) 
	{
		if(listener != null)
		{
			final ArrayList<MegaNode> nodes = MegaApiJava.nodeListToArray(nodeList);
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onNodesUpdate(megaApi, nodes);
			    }
			});
		}
	}

	@Override
	public void onReloadNeeded(MegaApi api)
	{
		if(listener != null)
		{
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onReloadNeeded(megaApi);
			    }
			});
		}
	}
	
	@Override
	public void onAccountUpdate(MegaApi api) 
	{
		if(listener != null)
		{
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onAccountUpdate(megaApi);
			    }
			});
		}
	}
	
	@Override
	public void onContactRequestsUpdate(MegaApi api, MegaContactRequestList contactRequestList) 
	{
		if(listener != null)
		{
			final ArrayList<MegaContactRequest> requests = MegaApiJava.contactRequestListToArray(contactRequestList);
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onContactRequestsUpdate(megaApi, requests);
			    }
			});
		}
	}
}
