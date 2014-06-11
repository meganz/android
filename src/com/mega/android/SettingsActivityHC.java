package com.mega.android;


import java.util.List;

import com.mega.sdk.MegaApiAndroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

@SuppressLint("NewApi")
public class SettingsActivityHC extends Activity {
	
	private MegaApiAndroid megaApi;
	static SettingsActivityHC preferencesActivity;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		preferencesActivity = this;
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext()); 
		if (dbH.getCredentials() == null){
			ManagerActivity.logout(this, app, megaApi);
			return;
		}	
		
		Preferences prefs = dbH.getPreferences();
		
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}
}