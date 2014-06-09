package com.mega.android;

import com.mega.sdk.MegaApiAndroid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity {

	private MegaApiAndroid megaApi;
	static PreferencesActivity preferencesActivity;
	
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
		
	}
	
}
