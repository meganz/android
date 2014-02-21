package com.mega.sdk;

class DelegateMegaRequestListener extends MegaRequestListener {

	MegaApiAndroid megaApi;
	MegaRequestListenerInterface listener;
	
	DelegateMegaRequestListener(MegaApiAndroid megaApi, MegaRequestListenerInterface listener)
	{
		this.megaApi = megaApi;
		this.listener = listener;
	}
	
	MegaRequestListenerInterface getUserListener()
	{
		return listener;
	}
	
	@Override
	public void onRequestStart(MegaApi api, MegaRequest request) 
	{
		if(listener != null)
		{
			final MegaRequest requestCopy = request.copy();
			MegaApiAndroid.handler.post(new Runnable()
			{
			    public void run() 
			    {
					listener.onRequestStart(megaApi, requestCopy);
			    }
			});
		}
	}

	@Override
	public void onRequestFinish(MegaApi api, MegaRequest request, MegaError e) 
	{
		if(listener != null)
		{
			final MegaRequest requestCopy = request.copy();
			final MegaError errorCopy = e.copy();
			MegaApiAndroid.handler.post(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onRequestFinish(megaApi, requestCopy, errorCopy);
			    }
			});
		}
		megaApi.privateFreeRequestListener(this);
	}

	@Override
	public void onRequestTemporaryError(MegaApi api, MegaRequest request, MegaError e) 
	{
		if(listener != null)
		{
			final MegaRequest requestCopy = request.copy();
			final MegaError errorCopy = e.copy();
			MegaApiAndroid.handler.post(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onRequestTemporaryError(megaApi, requestCopy, errorCopy);
			    }
			});
		}
	}
}
