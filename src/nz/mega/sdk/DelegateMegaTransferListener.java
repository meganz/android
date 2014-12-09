package nz.mega.sdk;

class DelegateMegaTransferListener extends MegaTransferListener
{
	MegaApiJava megaApi;
	MegaTransferListenerInterface listener;
	boolean singleListener;
	
	DelegateMegaTransferListener(MegaApiJava megaApi, MegaTransferListenerInterface listener, boolean singleListener)
	{
		this.megaApi = megaApi;
		this.listener = listener;
		this.singleListener = singleListener;
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
		if (singleListener){
			megaApi.privateFreeTransferListener(this);
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
	
	public boolean onTransferData(MegaApi api, MegaTransfer transfer, byte[] buffer)
	{
		if(listener != null)
		{
			final MegaTransfer megaTransfer = transfer.copy();
			return listener.onTransferData(megaApi, megaTransfer, buffer);
		}	  
		return false;
	}
}
