package com.mega.android;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.mega.sdk.MegaApiAndroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class OpenLinkActivity extends PinActivity {

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
			
			Intent openIntent = new Intent(this, ManagerActivity.class);
			openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openIntent.setAction(ManagerActivity.ACTION_OPEN_MEGA_LINK);
			openIntent.setData(Uri.parse(url));
			startActivity(openIntent);
			finish();
//			
//			Intent errorIntent = new Intent(this, ManagerActivity.class);
//			errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(errorIntent);
//			finish();			
//			Toast toast = Toast.makeText(getApplicationContext(), "File link (not implemented yet)", Toast.LENGTH_LONG);
//			toast.show();
			return;
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
			return;
		}
		
		// Folder Download link
		if (url != null && url.matches("^https://mega.co.nz/#F!.+$")) {
			log("folder link url");
			Intent errorIntent = new Intent(this, ManagerActivity.class);
			errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(errorIntent);
			finish();			
			Toast toast = Toast.makeText(getApplicationContext(), "Folder link (not implemented yet)", Toast.LENGTH_LONG);
			toast.show();
			return;
		}
		
		log("wrong url");
		Intent errorIntent = new Intent(this, ManagerActivity.class);
		errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(errorIntent);
		finish();
	}
	
	public static void log(String message) {
		Util.log("OpenLinkActivity", message);
	}
}
