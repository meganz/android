package com.mega.android;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.NodeList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;

public class CameraSyncService extends Service implements MegaRequestListenerInterface{

	public static String PHOTO_SYNC = "PhotoSync";
	
	public final static int SYNC_OK = 0;
	public final static int CREATE_PHOTO_SYNC_FOLDER = 1;
	
	WifiLock lock;
	WakeLock wl;
	
	private NotificationCompat.Builder mBuilderCompat;
	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;
	
	MegaApiAndroid megaApi;
	MegaApplication app;
	DatabaseHandler dbH;
	
	UserCredentials credentials;
	
	long photosyncHandle = -1;
	
	static public boolean running = false;
	private boolean canceled;
	
	private Handler handler;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate CamSync");
		
		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }
        
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			mBuilder = new Notification.Builder(CameraSyncService.this);	
		}
		mBuilderCompat = new NotificationCompat.Builder(getApplicationContext());
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		dbH = new DatabaseHandler(getApplicationContext());
		
		handler = new Handler();
		canceled = false;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		try { app = (MegaApplication)getApplication(); }
		catch(Exception ex) 
		{ 
			finish();
			return START_NOT_STICKY;
		}
		megaApi = app.getMegaApi();
		
		int result = shouldRun();
		switch(result){
			case SYNC_OK:{
				folderExists();
				break;
			}
			case CREATE_PHOTO_SYNC_FOLDER:{
				return START_NOT_STICKY;
			}
			default:{
				return result;
			}
		}

		return START_NOT_STICKY;		
	}
	
	private int shouldRun(){
		
		if (megaApi.getRootNode() == null){
			cancel();
			retryLater();
			return START_REDELIVER_INTENT;
		}
		
		credentials = dbH.getCredentials();
		if (credentials == null){
			log("There are not user credentials");
			finish();
			return START_NOT_STICKY;
		}
		
		//Aqui deberian ir accesos a la base de datos para saber:
		// - la carpeta local a sincronizar
		Preferences prefs = dbH.getPreferences();
		if (prefs == null){
			log("Not defined, so not enabled");
			finish();
			return START_NOT_STICKY;
		}
		else{
			if (prefs.getCamSyncEnabled() == null){
				log("Not defined, so not enabled");
				finish();
				return START_NOT_STICKY;
			}
			else{
				if (!Boolean.parseBoolean(prefs.getCamSyncEnabled())){
					log("Not enabled");
					finish();
					return START_NOT_STICKY;
				}
				else{
					//On Wifi or wifi and data plan?
					boolean isWifi = Util.isOnWifi(this);
					if (prefs.getWifi() == null || Boolean.parseBoolean(prefs.getWifi())){
						if (!isWifi){
							log("no wifi...");
							cancel();
							retryLater();
							return START_REDELIVER_INTENT;
						}
					}
					
					//The "PhotoSync" folder exists?
					if (prefs.getCamSyncHandle() == null){
						photosyncHandle = -1;
					}
					else{
						photosyncHandle = Long.parseLong(prefs.getCamSyncHandle());
						if (megaApi.getNodeByHandle(photosyncHandle) == null){
							photosyncHandle = -1;
						}
						else{
							if (megaApi.getParentNode(megaApi.getNodeByHandle(photosyncHandle)).getHandle() != megaApi.getRootNode().getHandle()){
								photosyncHandle = -1;
							}
						}
					}
				}
			}
		}
				
		if (photosyncHandle == -1){
			NodeList nl = megaApi.getChildren(megaApi.getRootNode());
			for (int i=0;i<nl.size();i++){
				if ((PHOTO_SYNC.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
					photosyncHandle = nl.get(i).getHandle();
					dbH.setCamSyncHandle(photosyncHandle);
				}
			}
			
			
			if (photosyncHandle == -1){
				megaApi.createFolder(PHOTO_SYNC, megaApi.getRootNode(), this);
				return CREATE_PHOTO_SYNC_FOLDER;
			}
		}		
		
		log ("photosynchandle = " + photosyncHandle);
		
		return SYNC_OK;
		
	}
	
	private void cancel() {
		if(running){
			canceled = true;
		}
	}
	
	private void retryLater()
	{
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				onStartCommand(null, 0, 0);
			}
		}, 30 * 60 * 1000);
	}
	
	private void folderExists(){
		
	}
	
	private void finish(){
		log("finish CameraSyncService");
		
		if(running){
			cancel();
		}
		
		running = false;
		
		stopSelf();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public static void log(String log){
		Util.log("CameraSyncService", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		
		if (e.getErrorCode() == MegaError.API_OK){
			log("Folder created");
			folderExists();
		}
		else{
			log("Folder not created so stop service");
			finish();
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}

}
