package com.mega.sdk;

class DelegateMegaRequestListener extends MegaRequestListener {

	MegaApiJava megaApi;
	MegaRequestListenerInterface listener;
	
	DelegateMegaRequestListener(MegaApiJava megaApi, MegaRequestListenerInterface listener)
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
			final MegaRequest megaRequest;
			if(megaApi.isRunCallbackThreaded())
				megaRequest = request.copy();
			else
				megaRequest = request;
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
					listener.onRequestStart(megaApi, megaRequest);
			    }
			});
		}
	}

	@Override
	public void onRequestFinish(MegaApi api, MegaRequest request, MegaError e) 
	{
		if(listener != null)
		{
			final MegaRequest megaRequest;
			final MegaError megaError;
			if(megaApi.isRunCallbackThreaded())
			{
				megaRequest = request.copy();
				megaError = e.copy();
			}
			else
			{
				megaRequest = request;
				megaError = e;
			}
			
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onRequestFinish(megaApi, megaRequest, megaError);
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
			final MegaRequest megaRequest;
			final MegaError megaError;
			if(megaApi.isRunCallbackThreaded())
			{
				megaRequest = request.copy();
				megaError = e.copy();
			}
			else
			{
				megaRequest = request;
				megaError = e;
			}
			
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onRequestTemporaryError(megaApi, megaRequest, megaError);
			    }
			});
		}
	}
}
