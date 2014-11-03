package com.mega.sdk;

class DelegateMegaListener extends MegaListener
{
	MegaApiJava megaApi;
	MegaListenerInterface listener;
	
	DelegateMegaListener(MegaApiJava megaApi, MegaListenerInterface listener)
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
			final MegaRequest megaRequest = request.copy();
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
			final MegaRequest megaRequest = request.copy();;
			final MegaError megaError = e.copy();;
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onRequestFinish(megaApi, megaRequest, megaError);
			    }
			});
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApi api, MegaRequest request, MegaError e) 
	{
		if(listener != null)
		{
			final MegaRequest megaRequest = request.copy();
			final MegaError megaError = e.copy();
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
			    	listener.onRequestTemporaryError(megaApi, megaRequest, megaError);
			    }
			});
		}
	}

	@Override
	public void onTransferStart(MegaApi api, MegaTransfer transfer)
	{
		if(listener != null)
		{
			final MegaTransfer megaTransfer = transfer.copy();
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
			final MegaTransfer megaTransfer = transfer.copy();
			final MegaError megaError = e.copy();
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferFinish(megaApi, megaTransfer, megaError);
			    }
			});
		}
	}
	
	@Override
	public void onTransferUpdate(MegaApi api, MegaTransfer transfer)
	{
		if(listener != null)
		{
			final MegaTransfer megaTransfer = transfer.copy();
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
			final MegaTransfer megaTransfer = transfer.copy();
			final MegaError megaError = e.copy();
			megaApi.runCallback(new Runnable()
			{
			    public void run() 
			    {
					listener.onTransferTemporaryError(megaApi, megaTransfer, megaError);
			    }
			});
		}
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
