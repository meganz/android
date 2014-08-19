package com.mega.android;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

public class MegaApplication extends Application
{
	final String TAG = "MegaApplication";
	static final String APP_KEY = "U5NE3TxD";
	static final String USER_AGENT = "MEGA Android/2.0 BETA";
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	
	@Override
	public void onCreate() {
		super.onCreate();
//		new MegaTest(getMegaApi()).start();
	}
	
	public MegaApiAndroid getMegaApiFolder(){
		if (megaApiFolder == null){
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
			megaApiFolder = new MegaApiAndroid(MegaApplication.APP_KEY, 
					MegaApplication.USER_AGENT, path);
		}
		
		return megaApiFolder;
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
			megaApi = new MegaApiAndroid(MegaApplication.APP_KEY, 
					MegaApplication.USER_AGENT, path);
		}
		
		return megaApi;
	}
	
	public static void log(String message) {
		Util.log("MegaApplication", message);
	}
}
