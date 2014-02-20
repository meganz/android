package com.mega.sdk;

public interface MegaRequestListenerInterface 
{
	public void onRequestStart(MegaApiAndroid api, MegaRequest request);
	public void onRequestFinish(MegaApiAndroid api, MegaRequest request, MegaError e);
	public void onRequestTemporaryError(MegaApiAndroid api, MegaRequest request, MegaError e);
}
		  