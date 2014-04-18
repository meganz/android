package com.mega.android;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.mega.sdk.MegaApiAndroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class OpenLinkActivity extends Activity {

	MegaApplication app;
	MegaApiAndroid megaApi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		app = (MegaApplication) getApplication();
		megaApi = app.getMegaApi();
		
		Intent intent = getIntent();
		String url = intent.getDataString();
		try {
			url = URLDecoder.decode(url, "UTF-8");
		} 
		catch (UnsupportedEncodingException e) {}
		url.replace(' ', '+');
		if(url.startsWith("mega://")){
			url = url.replace("mega://", "https://mega.co.nz/");
		}
		
		log("url " + url);
		
		// Download link
		if (url != null && url.matches("^https://mega.co.nz/#!.*!.*$")) {
			log("open link url");
		}
		
		// Confirmation link
		if (url != null && url.matches("^https://mega.co.nz/#confirm.+$")) {
			log("confirmation url");
			ManagerActivity.logout(this, app, megaApi);
			Intent confirmIntent = new Intent(this, LoginActivity.class);
			confirmIntent.putExtra(LoginActivity.EXTRA_CONFIRMATION, url);
			confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			confirmIntent.setAction(LoginActivity.ACTION_CONFIRM);
			startActivity(confirmIntent);
			finish();
		}
		
		// Folder Download link
		if (url != null && url.matches("^https://mega.co.nz/#F!.+$")) {
			log("folder link url");
		}
		
		
	}
	
	private void log(String message) {
		Util.log("OpenLinkActivity", message);
	}
}
