package com.mega.sdk;

public class DelegateMegaGlobalListener extends MegaGlobalListener
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
	public void onUsersUpdate(MegaApi api) 
	{
		if(listener != null)
		{
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onUsersUpdate(megaApi);
			    }
			});
		}
	}

	@Override
	public void onNodesUpdate(MegaApi api) 
	{
		if(listener != null)
		{
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onNodesUpdate(megaApi);
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
}
