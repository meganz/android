package com.mega.sdk;

import java.io.IOException;
import java.io.OutputStream;

public class DelegateOutputMegaTransferListener extends DelegateMegaTransferListener
{
	OutputStream outputStream;
	public DelegateOutputMegaTransferListener(MegaApiJava megaApi, OutputStream outputStream, MegaTransferListenerInterface listener,
			boolean singleListener)
	{
		super(megaApi, listener, singleListener);
		this.outputStream = outputStream;
	}

	public boolean onTransferData(MegaApi api, MegaTransfer transfer, byte[] buffer)
	{
		if(outputStream != null)
		{
			try
			{
				outputStream.write(buffer);
				return true;
			}
			catch (IOException e)
			{ }
		}	  
		return false;
	}
}
