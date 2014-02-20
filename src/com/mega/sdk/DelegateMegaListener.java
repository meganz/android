package com.mega.sdk;

import android.os.Handler;

public class DelegateMegaListener extends MegaListener
{
	MegaApiAndroid megaApi;
	MegaListenerInterface listener;
	static Handler handler = new Handler();
	
	DelegateMegaListener(MegaApiAndroid megaApi, MegaListenerInterface listener)
	{
		this.megaApi = megaApi;
		this.listener = listener;
	}
	
	MegaListenerInterface getUserListener()
	{
		return listener;
	}
	
	@Override
	public void onRequestStart(MegaApi api, MegaRequest request) 
	{
		if(listener != null)
		{
			final MegaRequest requestCopy = request.copy();
			handler.post(new Runnable()
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
			handler.post(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onRequestFinish(megaApi, requestCopy, errorCopy);
			    }
			});
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApi api, MegaRequest request, MegaError e) 
	{
		if(listener != null)
		{
			final MegaRequest requestCopy = request.copy();
			final MegaError errorCopy = e.copy();
			handler.post(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onRequestTemporaryError(megaApi, requestCopy, errorCopy);
			    }
			});
		}
	}

	@Override
	public void onTransferStart(MegaApi api, MegaTransfer transfer)
	{
		if(listener != null)
		{
			final MegaTransfer transferCopy = transfer.copy();
			handler.post(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferStart(megaApi, transferCopy);
			    }
			});
		}
	}
	
	@Override
	public void onTransferFinish(MegaApi api, MegaTransfer transfer, MegaError e)
	{
		if(listener != null)
		{
			final MegaTransfer transferCopy = transfer.copy();
			final MegaError errorCopy = e.copy();
			handler.post(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferFinish(megaApi, transferCopy, errorCopy);
			    }
			});
		}
	}
	
	@Override
	public void onTransferUpdate(MegaApi api, MegaTransfer transfer)
	{
		if(listener != null)
		{
			final MegaTransfer transferCopy = transfer.copy();
			handler.post(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferUpdate(megaApi, transferCopy);
			    }
			});
		}
	}
	
	@Override
	public void onTransferTemporaryError(MegaApi api, MegaTransfer transfer, MegaError e)
	{
		if(listener != null)
		{
			final MegaTransfer transferCopy = transfer.copy();
			final MegaError errorCopy = e.copy();
			handler.post(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferTemporaryError(megaApi, transferCopy, errorCopy);
			    }
			});
		}
	}

	@Override
	public void onUsersUpdate(MegaApi api) 
	{
		if(listener != null)
		{
			handler.post(new Runnable()
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
			handler.post(new Runnable()
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
			handler.post(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onReloadNeeded(megaApi);
			    }
			});
		}
	}
}
