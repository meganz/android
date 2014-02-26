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

public class MegaApplication extends Application implements MegaRequestListenerInterface
{
	final String TAG = "MegaApplication";
	MegaApiAndroid megaApi;
	
	Semaphore fileSystemLock;
	ArrayList<MegaRequest> pendingRequests = new ArrayList<MegaRequest>();
	
	@Override
	public void onCreate() {
		fileSystemLock = new Semaphore(1);
		super.onCreate();
//		new MegaTest(getMegaApi()).start();
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
	
	public void acquireFilesystemLock()	{
		log("Acquiring lock");
		fileSystemLock.acquireUninterruptibly();
		log("Lock acquired: "  + fileSystemLock.availablePermits());
	}
	
	public void releaseFilesystemLock(){
		log("Releasing lock");
		fileSystemLock.release();
		log("Lock released");
	}
	
	public void logout()
	{
		acquireFilesystemLock();
		megaApi.logout();
	}
	
	public static void log(String message) {
		Util.log("MegaApplication", message);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		pendingRequests.add(request);
		log("Request added. Pending requests: " + pendingRequests.size());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
		
		if (pendingRequests.size() != 0){
			pendingRequests.remove(pendingRequests.size() -1);
		}
		
		log("Request finished. Pending requests: " + pendingRequests.size());
		
		if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout finished");
			releaseFilesystemLock();
		}
		
		log("Request removed. Pending requests: " + pendingRequests.size());

	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,MegaError e) {
	
	}
}
