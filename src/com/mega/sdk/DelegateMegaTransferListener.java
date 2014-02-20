package com.mega.sdk;

import android.os.Handler;

public class DelegateMegaTransferListener extends MegaTransferListener
{
	MegaApiAndroid megaApi;
	MegaTransferListenerInterface listener;
	static Handler handler = new Handler();
	
	DelegateMegaTransferListener(MegaApiAndroid megaApi, MegaTransferListenerInterface listener)
	{
		this.megaApi = megaApi;
		this.listener = listener;
	}

	MegaTransferListenerInterface getUserListener()
	{
		return listener;
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
		megaApi.privateFreeTransferListener(this);
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
}
