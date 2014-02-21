package com.mega.sdk;

public class DelegateMegaGlobalListener extends MegaGlobalListener
{
	MegaApiAndroid megaApi;
	MegaGlobalListenerInterface listener;
	
	DelegateMegaGlobalListener(MegaApiAndroid megaApi, MegaGlobalListenerInterface listener)
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
			MegaApiAndroid.handler.post(new Runnable()
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
			MegaApiAndroid.handler.post(new Runnable()
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
			MegaApiAndroid.handler.post(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onReloadNeeded(megaApi);
			    }
			});
		}
	}
}
