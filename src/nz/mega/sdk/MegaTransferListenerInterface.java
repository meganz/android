package nz.mega.sdk;

public interface MegaTransferListenerInterface 
{
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer);
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e);
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer);
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e);
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer);
}
