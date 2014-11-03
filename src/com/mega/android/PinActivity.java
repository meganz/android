package com.mega.android;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;

public class PinActivity extends ActionBarActivity{
	
	private MegaApiAndroid megaApi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
	}

	@Override
	protected void onPause() {
		log("onPause");
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		PinUtil.pause(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		log("onResume");
		super.onResume();
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
		log("retryPendingConnections()");
		megaApi.retryPendingConnections();
		
		PinUtil.resume(this);
	}
	
	public static void log(String message) {
		Util.log("PinActivity", message);
	}
}
