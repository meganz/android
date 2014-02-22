package com.mega.sdk;

public class DelegateMegaTransferListener extends MegaTransferListener
{
	MegaApiJava megaApi;
	MegaTransferListenerInterface listener;
	
	DelegateMegaTransferListener(MegaApiJava megaApi, MegaTransferListenerInterface listener)
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
			final MegaTransfer megaTransfer;
			if(megaApi.isRunCallbackThreaded())
				megaTransfer = transfer.copy();
			else
				megaTransfer = transfer;
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferStart(megaApi, megaTransfer);
			    }
			});
		}
	}
	
	@Override
	public void onTransferFinish(MegaApi api, MegaTransfer transfer, MegaError e)
	{
		if(listener != null)
		{
			final MegaTransfer megaTransfer;
			final MegaError megaError;
			if(megaApi.isRunCallbackThreaded())
			{
				megaTransfer = transfer.copy();
				megaError = e.copy();
			}
			else
			{
				megaTransfer = transfer;
				megaError = e;
			}
			
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferFinish(megaApi, megaTransfer, megaError);
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
			final MegaTransfer megaTransfer;
			if(megaApi.isRunCallbackThreaded())
				megaTransfer = transfer.copy();
			else
				megaTransfer = transfer;
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferUpdate(megaApi, megaTransfer);
			    }
			});
		}
	}
	
	@Override
	public void onTransferTemporaryError(MegaApi api, MegaTransfer transfer, MegaError e)
	{
		if(listener != null)
		{
			final MegaTransfer megaTransfer;
			final MegaError megaError;
			if(megaApi.isRunCallbackThreaded())
			{
				megaTransfer = transfer.copy();
				megaError = e.copy();
			}
			else
			{
				megaTransfer = transfer;
				megaError = e;
			}
			
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferTemporaryError(megaApi, megaTransfer, megaError);
			    }
			});
		}
	}
}
