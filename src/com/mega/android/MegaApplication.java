package com.mega.android;

import com.mega.sdk.MegaApiAndroid;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class MegaApplication extends Application
{
	final String TAG = "MegaApplication";
	MegaApiAndroid megaApi;
	
	@Override
	public void onCreate() {
		super.onCreate();
		//new MegaTest(getMegaApi()).start();
	}
	
	public MegaApiAndroid getMegaApi()
	{
		if(megaApi == null)
		{
			PackageManager m = getPackageManager();
			String s = getPackageName();
			PackageInfo p;
			String path = null;
			try
			{
				p = m.getPackageInfo(s, 0);
				path = p.applicationInfo.dataDir + "/";
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			
			Log.d(TAG, "Database path: " + path);
			megaApi = new MegaApiAndroid(path);
		}
		
		return megaApi;
	}
}
