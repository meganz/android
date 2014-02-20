package com.mega.sdk;

public interface MegaTransferListenerInterface 
{
	public void onTransferStart(MegaApiAndroid api, MegaTransfer transfer);
	public void onTransferFinish(MegaApiAndroid api, MegaTransfer transfer, MegaError e);
	public void onTransferUpdate(MegaApiAndroid api, MegaTransfer transfer);
	public void onTransferTemporaryError(MegaApiAndroid api, MegaTransfer transfer, MegaError e);
}
