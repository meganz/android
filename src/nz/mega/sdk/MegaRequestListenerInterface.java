package nz.mega.sdk;

public interface MegaRequestListenerInterface 
{
	public void onRequestStart(MegaApiJava api, MegaRequest request);
	public void onRequestUpdate(MegaApiJava api, MegaRequest request);
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e);
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e);
}
