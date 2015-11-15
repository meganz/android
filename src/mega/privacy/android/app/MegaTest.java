package mega.privacy.android.app;

import java.util.ArrayList;

import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import android.util.Log;


public class MegaTest extends Thread implements MegaRequestListenerInterface
{
	final String TAG = "MegaTest";
	MegaApiAndroid megaApi;
	
	MegaTest(MegaApiAndroid api)
	{
		megaApi = api;
	}
	
	public void run()
	{
		megaApi.login("testaccount@yopmail.com", "testaccount", 
			new MegaRequestListenerInterface()
		{
			final String TAG = "MegaTestInternalListener";

			@Override
			public void onRequestStart(MegaApiJava api, MegaRequest request)
			{
				Log.d(TAG, "onRequestStart");
			}

			@Override
			public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e)
			{
				Log.d(TAG, "onRequestFinish " + request.getRequestString() + " Result: " + e.getErrorCode());
				if(e.getErrorCode() != MegaError.API_OK) 
					return;
				
				megaApi.fetchNodes(MegaTest.this);
			}

			@Override
			public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e)
			{
				Log.d(TAG, "onRequestTemporaryError");
			}

			@Override
			public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request)
	{
		Log.d(TAG, "onRequestStart");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e)
	{
		Log.d(TAG, "onRequestFinish " + request.getRequestString() + " Result: " + e.getErrorCode());
		if(e.getErrorCode() != MegaError.API_OK) 
			return;
		
		ArrayList<MegaNode> children = megaApi.getChildren(megaApi.getRootNode());
		for(int i=0; i<children.size(); i++)
		{
			MegaNode node = children.get(i);
			Log.d(TAG, "Node: " + node.getName() + 
					(node.isFolder() ? " (folder)" : (" " + node.getSize() + " bytes")));
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e)
	{
		Log.d(TAG, "onRequestTemporaryError");
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
}
