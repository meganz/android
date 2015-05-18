package nz.mega.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import nz.mega.android.utils.PreviewUtils;
import nz.mega.android.utils.ThumbnailUtils;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.text.format.Time;
import android.widget.RemoteViews;
import android.widget.Toast;


public class DropboxImportService extends Service implements MegaRequestListenerInterface, MegaTransferListenerInterface, MegaGlobalListenerInterface{

	public static String PHOTO_SYNC = "PhotoSync";
	public static String CAMERA_UPLOADS = "Camera Uploads";
	public static String SECONDARY_UPLOADS = "Media Uploads";
	public static String ACTION_CANCEL = "CANCEL_SYNC";
	public static String ACTION_STOP = "STOP_SYNC";
	public static String ACTION_LOGOUT = "LOGOUT_SYNC";
	public static String ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER ="PHOTOS_VIDEOS_NEW_FOLDER";
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
	
	DropboxAPI<AndroidAuthSession> mDBApi;
	
	static public boolean running = false;
	private boolean canceled;
	
	private Handler handler;
	
//	private CameraObserver cameraObserver;
		
	private int totalImmported;
	private long totalSizeToImport;
	private int totalToImport;
	private long totalSizeImported;
	private int successCount;
	private ConcurrentLinkedQueue<Entry> filesToUpload = new ConcurrentLinkedQueue<Entry>();
	private ConcurrentLinkedQueue<Entry> filesToDownload = new ConcurrentLinkedQueue<Entry>();
	private ConcurrentLinkedQueue<Entry> filesUploaded = new ConcurrentLinkedQueue<Entry>();
	
	Object transferFinished = new Object();
	
	private boolean isForeground;
	
	private int notificationId = 3;
	private int notificationIdFinal = 6;
	
	private static long lastRun = 0;
	
	Queue<Entry> cameraFiles = new LinkedList<Entry>();
	String localPath = "";
	MegaNode cameraUploadNode = null;
	long cameraUploadHandle = -1;
	
	Intent intentCreate = null;
	
	MegaPreferences prefs;
	
	boolean newFileList = false;
	boolean stopped = false;
	
	Thread task;		
	
	long currentTimeStamp = 0;
	
//	boolean waitForOnNodesUpdate = false;
	
	static DropboxImportService dropboxImportService;
	
    ProgressListener mProgressListener = new ProgressListener() {

//        @Override
//        public void onProgress(long arg0, long arg1) {
//            // TODO Auto-generated method stub
//            tmpFile = new File(cachePath);
//            OnDownloadProgressDropboxChecked(FileHandler.PROGRESS_STATUS_ONPROGRESS, (int) (arg0 * 100 / arg1));
//            Log.d("Dolphin got interval", String.valueOf(tmpFile.length() + " - " + arg0 + " - " + arg1));
//        }
//
//        @Override
//        public long progressInterval() {
//            return 100;
    	 @Override
         public void onProgress(long arg0, long arg1) {
             // TODO Auto-generated method stub
    		 log("long arg0: "+arg0+" long arg1: "+arg1);
         }

         @Override
         public long progressInterval() {
             return 100;
         }
    };
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate DropboxService");
		reset();
		dropboxImportService = this;
		
		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }
        
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			mBuilder = new Notification.Builder(DropboxImportService.this);	
		}
		mBuilderCompat = new NotificationCompat.Builder(getApplicationContext());
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
//		dbH = new DatabaseHandler(getApplicationContext());
		
		handler = new Handler();
		canceled = false;
		newFileList = false;
//		waitForOnNodesUpdate = false;
		
		try{
			app = (MegaApplication) getApplication();
		}
		catch(Exception ex){
			finish();
		}
		
		megaApi = app.getMegaApi();
		
		mDBApi = app.getDropboxApi();
		
		megaApi.addGlobalListener(this);
	}
	
	private int shouldRun(){
		log("shouldRun");
		
		if (!Util.isOnline(this)){
			log("Not online");
			finish();
			return START_NOT_STICKY;
		}
		
		credentials = dbH.getCredentials();
		
		if (credentials == null){
			log("There are not user credentials");
			finish();
			return START_NOT_STICKY;
		}
		
		String gSession = credentials.getSession();
		
		if (megaApi.getRootNode() == null){
			log("RootNode = null");
			running = true;
			megaApi.fastLogin(gSession, this);
			return START_NOT_STICKY;
		}
		
		prefs = dbH.getPreferences();
		if (prefs == null){
			log("Not defined, so not enabled");
			finish();
			return START_NOT_STICKY;
		}
		else{
			if (prefs.getCamSyncEnabled() == null){
				dbH.setCamSyncEnabled(false);
				log("Not defined, so not enabled");
				finish();
				return START_NOT_STICKY;
			}
			else{
				if (!Boolean.parseBoolean(prefs.getCamSyncEnabled())){
					log("Camera Sync Not enabled");
					finish();
					return START_NOT_STICKY;
				}
				else{
					localPath = prefs.getCamSyncLocalPath();
					if (localPath == null){
						log("Not defined, so not enabled");
						finish();
						return START_NOT_STICKY;
					}
					else{
						if ("".compareTo(localPath) == 0){
							log("Not defined, so not enabled");
							finish();
							return START_NOT_STICKY;					
							
						}
					}
					
					boolean isWifi = Util.isOnWifi(this);
					if (prefs.getCamSyncWifi() == null){
						if (!isWifi){
							log("no wifi...");
							finish();
							return START_REDELIVER_INTENT;
						}
					}
					else{
						if (Boolean.parseBoolean(prefs.getCamSyncWifi())){
							if (!isWifi){
								log("no wifi...");
								finish();
								return START_REDELIVER_INTENT;
							}
						}
					}
					
					boolean isCharging = Util.isCharging(this);
					if (prefs.getCamSyncCharging() == null){
						if (!isCharging){
							log("not charging...");
							finish();
							return START_REDELIVER_INTENT;
						}
					}
					else{
						if (Boolean.parseBoolean(prefs.getCamSyncCharging())){
							if (!isCharging){
								log("not charging...");
								finish();
								return START_REDELIVER_INTENT;
							}
						}
					}
				}
			}
		}
//					
	/*
		if (cameraUploadHandle == -1){
			//Find the "Camera Uploads" folder of the old "PhotoSync"			
			for (int i=0;i<nl.size();i++){
				if ((CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
					cameraUploadHandle = nl.get(i).getHandle();
					dbH.setCamSyncHandle(cameraUploadHandle);
				}
				else if((PHOTO_SYNC.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
					cameraUploadHandle = nl.get(i).getHandle();
					dbH.setCamSyncHandle(cameraUploadHandle);
					megaApi.renameNode(nl.get(i), CAMERA_UPLOADS, this);					
				}
			}
			
			//If not "Camera Uploads" or "Photosync"
			if (cameraUploadHandle == -1){
				log("must create the folder");
				if (!running){
					running = true;
					megaApi.createFolder(CAMERA_UPLOADS, megaApi.getRootNode(), this);
				}
				else{
					if (megaApi != null){
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
						return START_NOT_STICKY;
					}
				}
				return START_NOT_STICKY;
			}
		}
		else{
			MegaNode n = megaApi.getNodeByHandle(cameraUploadHandle);
			//If ERROR with the handler (the node may not longer exist): Create the folder Camera Uploads
			if(n==null){				
				//Find the "Camera Uploads" folder of the old "PhotoSync"
				for (int i=0;i<nl.size();i++){
					if ((CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
						cameraUploadHandle = nl.get(i).getHandle();
						dbH.setCamSyncHandle(cameraUploadHandle);
					}
					else if((PHOTO_SYNC.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
						cameraUploadHandle = nl.get(i).getHandle();
						dbH.setCamSyncHandle(cameraUploadHandle);
						megaApi.renameNode(nl.get(i), CAMERA_UPLOADS, this);					
					}
				}
				
				//If not "Camera Uploads" or "Photosync"
				if (cameraUploadHandle == -1){
					log("must create the folder");
					if (!running){
						running = true;
						megaApi.createFolder(CAMERA_UPLOADS, megaApi.getRootNode(), this);
					}
					else{
						if (megaApi != null){
							megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
							return START_NOT_STICKY;
						}
					}
					return START_NOT_STICKY;
				}				
			}
			else{
				log("Sync Folder " + cameraUploadHandle + " Node: "+n.getName());
			}			
		}
	
		log("shouldRun: TODO OK");*/
		
		return 0;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");
		
		if (megaApi == null){
			finish();
			return START_NOT_STICKY;
		}
		
		if (mDBApi == null){
			finish();
			return START_NOT_STICKY;
		}
		
		String previousIP = app.getLocalIpAddress();
		String currentIP = Util.getLocalIpAddress();
		if (previousIP == null 
				|| (previousIP.length() == 0) 
				|| (previousIP.compareTo("127.0.0.1") == 0))
		{
			app.setLocalIpAddress(currentIP);
		}
		else if ((currentIP != null) 
				&& (currentIP.length() != 0) 
				&& (currentIP.compareTo("127.0.0.1") != 0)
				&& (currentIP.compareTo(previousIP) != 0))
		{
			app.setLocalIpAddress(currentIP);
			log("reconnect");
			megaApi.reconnect();
		}
		
		int result = shouldRun();
		if (result != 0){
			return result;
		}		
		
		if (intent != null){
			if (intent.getAction() != null){
				if (intent.getAction().equals(ACTION_CANCEL)){
					log("Cancel intent");
					intent.setAction(null);
					if (megaApi != null){
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
						dbH.setCamSyncEnabled(false);
						return START_NOT_STICKY;
					}
					else{
						finish();
						return START_NOT_STICKY;
					}
				}
				else if (intent.getAction().equals(ACTION_STOP)){
					log("Stop intent");
					intent.setAction(null);
					stopped = true;
					if (megaApi != null){
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
						dbH.setCamSyncEnabled(false);
						return START_NOT_STICKY;
					}
					else{
						finish();
						return START_NOT_STICKY;
					}
				}
				else if (intent.getAction().equals(ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER)){
					log("List photos and videos intent");
					intent.setAction(null);
					if (megaApi != null){
						newFileList = true;
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
						return START_NOT_STICKY;
					}
					else{
						finish();
						return START_NOT_STICKY;
					}
				}
				else if (intent.getAction().equals(ACTION_LOGOUT)){
					log("Logout intent");
					stopped = true;
					if (megaApi != null){
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
						return START_NOT_STICKY;
					}
					else{
						finish();
						return START_NOT_STICKY;
					}
				}
			}
		}
		
		if (newFileList){
			return START_NOT_STICKY;
		}
		
		if (!running){
			running = true;
			canceled = false;
			task = new Thread()
			{
				@Override
				public void run()
				{
					log("Run: initSyn");
					initSync();
				}
			};
			task.start();
			log("ESTOY CORRIENDO");
			
		}
		else{
			log("ESTABA CORRIENDO YA ASI QUE RETRYLATER");
			retryLater();
		}
		
		return START_NOT_STICKY;		
	}
	
	
	
	void initSync(){
		log("initSync");
		
		if(!wl.isHeld()){ 
			wl.acquire();
		}
		if(!lock.isHeld()){
			lock.acquire();
		}
		
		int i=0;
        String[] fnames = null;
        Entry dirent;
		try {
			if(mDBApi!=null){
				dirent = mDBApi.metadata("/", 1000, null, true, null);
				ArrayList<Entry> files = new ArrayList<Entry>();
	            ArrayList<String> dir=new ArrayList<String>();
	            for (Entry ent: dirent.contents) 
	            {
	                files.add(ent);// Add it to the list of thumbs we can choose from                       
	                //dir = new ArrayList<String>();
	                if(ent.isDir){
	                	listDirectory(ent);
	                }
	                else{
	                	filesToDownload.add(ent);
//	            		startDownload(ent);
	                	log("Added: "+ent.path);
	                }
//	                dir.add(new String(files.get(i++).path));
	            }
//	            i=0;
//	            fnames=dir.toArray(new String[dir.size()]);
//	            for(int j=0;j<fnames.length;j++)
//	            {
//	            	log("Lista: "+fnames[j]);
//	            }     
	            log("Acabo la recursividad---------------");
	            log("   "+filesToDownload.size());
	            
			}
			else{
				log("MDBApi NUUUUUULLLLLL");
			}
			
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			finish();
		}

		String destination = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.dropboxDIR;		
		File file = new File(destination);
		if(!file.exists()){
			file.mkdirs();
		}
		
//		Log.i("DbExampleLog", "The file's rev is: " + info.getMetadata().rev);
		
		totalToImport = filesToDownload.size();
		totalImmported=0;
		totalSizeImported=0;//		
		totalSizeToImport = 0;
		
		Iterator<Entry> itCF = filesToDownload.iterator();
		while (itCF.hasNext()){
			Entry m = itCF.next();
			File f = new File(m.size);
			totalSizeToImport = totalSizeToImport + f.length();
		}	
	
		startImport();
//		uploadNextImage();
//		log("Time for secondary media folder!");
//		uploadNextMediaFile();
		
//		uploadNext();
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	public void startDownload(Entry ent){
		log("startDownload Entry: "+ent.fileName());		
				
		String destination2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.dropboxDIR + "/" + ent.fileName();		
		File file2 = new File(destination2);
		
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file2);

			DropboxFileInfo info = mDBApi.getFile(ent.path, null, outputStream, mProgressListener);
			
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (DropboxException e) {
			e.printStackTrace();
		}
	}
	
	public void startImport(){
		
		Iterator<Entry> itCF = filesToDownload.iterator();
		while (itCF.hasNext()){
			Entry entry = itCF.next();
			
			String destination2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.dropboxDIR + "/" + entry.fileName();		
			File file2 = new File(destination2);
			
			FileOutputStream outputStream;
			try {
				outputStream = new FileOutputStream(file2);

				DropboxFileInfo info = mDBApi.getFile(entry.path, null, outputStream, mProgressListener);
				
			} 
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (DropboxException e) {
				e.printStackTrace();
			}
		}	
		
	}
	
	public void listDirectory(Entry entry){
		log("listDirectory: "+entry.path);
		
		Entry dirent;
		String path=entry.path;
		
		try {
			dirent = mDBApi.metadata(path, 1000, null, true, null);
			ArrayList<Entry> files = new ArrayList<Entry>();
	        ArrayList<String> dir=new ArrayList<String>();
	        for (Entry ent: dirent.contents) 
	        {
	            files.add(ent);// Add it to the list of thumbs we can choose from                       
	            //dir = new ArrayList<String>();
	            if(ent.isDir){
	            	listDirectory(ent);
	            }
	            else{
	            	filesToDownload.add(ent);
	            	log("Added: "+ent.path);
	            }
	        }
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void uploadNext(){
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		log("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&   UPLOAD NEXT");
//		if (cameraFiles.size() > 0){
//			uploadNextImage();
//		}
//		else if(mediaFilesSecondary.size() > 0){
//			uploadNextMediaFile();
//		}
//		else
//		{
//			onQueueComplete(true, totalImmported);
//			finish();
//		}
	}
	
	void uploadNextMediaFile(){
		
		totalImmported++;
		log("------------------------------------------------uploadNextMediaFile: "+totalImmported +" of "+totalToImport);
		
		int result = shouldRun();
		if (result != 0){
			retryLater();
			return;
		}
		

	}
	
	/*
	private boolean checkFile(Media media){
		
		if (media.filePath != null){
			if (media.filePath.startsWith(localPath)){
				Time t = new Time(Time.getCurrentTimezone());
				t.setToNow();
				long timeSpent = t.toMillis(true) - media.timestamp;
				if (timeSpent > ((5 * 60 * 1000)-1)){
					return true;
				}
				return true;
			}
		}
		
		return false;
	}
	
	private boolean checkFile(Media media, String path){
		
		if (media.filePath != null){
			if (path != null){
				if (path.compareTo("") != 0){
					if (media.filePath.startsWith(path)){
						Time t = new Time(Time.getCurrentTimezone());
						t.setToNow();
						long timeSpent = t.toMillis(true) - media.timestamp;
						if (timeSpent > ((5 * 60 * 1000)-1)){
							return true;
						}
						return true;
					}
				}
			}
		}
		
		return false;
	}	
	
	public void retryLaterShortTime(){
		log("retryLaterShortTime");
		stopped = true;
		running = false;
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				onStartCommand(null, 0, 0);
			}
		}, 10 * 1000);
	}
	
	@SuppressLint("DefaultLocale")
	private void showCompleteSuccessNotification() {
		log("showCompleteSuccessNotification");

		String message = String.format(
				"%d %s %s (%s)",
				totalImmported,
				getResources().getQuantityString(R.plurals.general_num_files,
						totalImmported), getString(R.string.upload_uploaded),
				Formatter.formatFileSize(DropboxImportService.this, totalSizeImported));
		String title = getString(R.string.settings_camera_notif_complete);
		Intent intent = new Intent(DropboxImportService.this, ManagerActivity.class);
		intent.putExtra(ManagerActivity.EXTRA_OPEN_FOLDER, cameraUploadHandle);

		mBuilderCompat
			.setSmallIcon(R.drawable.ic_stat_camera_sync)
			.setContentIntent(PendingIntent.getActivity(DropboxImportService.this, 0, intent, 0))
			.setAutoCancel(true).setTicker(title).setContentTitle(title)
			.setContentText(message)
			.setOngoing(false);
	
		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}*/

	private void finish(){
		log("finish CameraSyncService");
		
		if(running){
			handler.removeCallbacksAndMessages(null);
			running = false;
		}
		cancel();
	}
	
	private void reset() {
		log("---====  RESET  ====---");
		totalImmported = -1;
		totalSizeToImport = 0;
		totalToImport = 0;
		totalSizeImported = 0;
		successCount = 0;
		filesToUpload.clear();
		filesUploaded.clear();
	}
	
	private void cancel() {
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}
		
		canceled = true;
		isForeground = false;
		running = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
	}
	
	public void retryLater(){
		log("retryLater");
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				onStartCommand(null, 0, 0);
			}
		}, 5 * 60 * 1000);
	}
	
	public void onDestroy(){
		log("onDestroy");
		running = false;
		
		if (!stopped){
			retryLater();
		}		

		if(megaApi != null)
		{
			megaApi.removeGlobalListener(this);
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private void showSyncError(final int errResId) {
		handler.post(new Runnable() 
		{
			public void run()
			{
				log("show sync error");
				Intent intent = new Intent(DropboxImportService.this, ManagerActivity.class);
						
				mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setContentIntent(PendingIntent.getActivity(DropboxImportService.this, 0, intent, 0))
					.setContentTitle(getString(R.string.settings_camera_notif_error))
					.setContentText(getString(errResId))
					.setOngoing(false);
		
				if (!isForeground) {
					log("starting foreground!");
					startForeground(notificationId, mBuilderCompat.build());
					isForeground = true;
				} else {
					mNotificationManager.notify(notificationId, mBuilderCompat.build());
				}
			}}
		);
	}
	
	public static void log(String log){
		Util.log("DropboxImportService", log);
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
		/*
		if (request.getType() == MegaRequest.TYPE_LOGIN){
			if (e.getErrorCode() == MegaError.API_OK){
				log("Fast login OK");
				
				dbH.clearCredentials();
				dbH.saveCredentials(credentials);

				log("Calling fetchNodes from CameraSyncService");
				megaApi.fetchNodes(this);
			}
			else{
				retryLaterShortTime();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			if (e.getErrorCode() == MegaError.API_OK){
				retryLaterShortTime();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){		
			if (e.getErrorCode() == MegaError.API_OK){
				log("Folder created: "+request.getName());				
				//Add in the database the new folder
				String name = request.getName();
				if(name.contains(CAMERA_UPLOADS)){
					//Update in database
					log("CamSync Folder UPDATED DB");
					dbH.setCamSyncHandle(request.getNodeHandle());
				}
				else{
					//Update in database
					log("Secondary Folder UPDATED DB");
					dbH.setSecondaryFolderHandle(request.getNodeHandle());
				}				

				retryLaterShortTime();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME || request.getType() == MegaRequest.TYPE_COPY){
			if (e.getErrorCode() == MegaError.API_OK){
				
				if (megaApi.getNodeByHandle(request.getNodeHandle()).getName().compareTo(CAMERA_UPLOADS) == 0){
					log("Folder renamed to CAMERA_UPLOADS");
					retryLaterShortTime();
					finish();
				}
				else{
					MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
					long parentHandle = megaApi.getParentNode(node).getHandle();
					if(parentHandle == secondaryUploadHandle){
						log("Update SECONDARY Sync TimeStamp");
						dbH.setSecSyncTimeStamp(currentTimeStamp);
					}
					else{
						log("Update Camera Sync TimeStamp");
						dbH.setCamSyncTimeStamp(currentTimeStamp);
					}
					
					totalSizeImported += megaApi.getNodeByHandle(request.getNodeHandle()).getSize();
					uploadNext();					
				}
			}
			else{
				log("Error renaming");
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			log("cancel_transfers received");
			if (e.getErrorCode() == MegaError.API_OK){
				megaApi.pauseTransfers(false, this);
				megaApi.resetTotalUploads();
				reset();
			}
			else{
				retryLater();
				finish();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("pause_transfer false received");
			if (e.getErrorCode() == MegaError.API_OK){
				if (newFileList){
					newFileList = false;
					log("pauseNewFileList");
					retryLater();
					mNotificationManager.cancel(notificationId);
					mNotificationManager.cancel(notificationIdFinal);
					isForeground = false;
					finish();
					stopForeground(true);
					stopSelf();
				}
				else{
					retryLater();
					finish();
				}
			}
		}*/
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}
	
	public static void runIfNecessary(final Context context) {
		long time = System.currentTimeMillis();
		if (time - lastRun > 5  * 1000) {
			log("should start");
			lastRun = time;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (context != null) {
						context.startService(new Intent(context,
								DropboxImportService.class));
					}
				}
			}, 2000);

		} else {
			log("no need");
		}
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferStart: " + transfer.getFileName());
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		log("Image sync finished: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
		log("transfer.getPath:" + transfer.getPath());
		log("transfer.getNodeHandle:" + transfer.getNodeHandle());
		if (canceled) {
			log("Image sync cancelled: " + transfer.getFileName());
			if((lock != null) && (lock.isHeld()))
				try{ lock.release(); } catch(Exception ex) {}
			if((wl != null) && (wl.isHeld()))
				try{ wl.release(); } catch(Exception ex) {}
			DropboxImportService.this.cancel();
		}
		else{
			if (e.getErrorCode() == MegaError.API_OK) {
				log("Image Sync OK: " + transfer.getFileName());
				totalSizeImported += transfer.getTransferredBytes();
				log("IMAGESYNCFILE: " + transfer.getPath());
				
				String tempPath = transfer.getPath();
				if(tempPath.startsWith(localPath)){
					log("onTransferFinish: Update Camera Sync TimeStamp");
					dbH.setCamSyncTimeStamp(currentTimeStamp);
				}
				else{
					log("onTransferFinish: Update SECONDARY Sync TimeStamp");
					dbH.setSecSyncTimeStamp(currentTimeStamp);
				}
				
				File previewDir = PreviewUtils.getPreviewFolder(this);
				File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
				File thumbDir = ThumbnailUtils.getThumbFolder(this);
				File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
				megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
				megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());
				
//				dbH.setCamSyncTimeStamp(currentTimeStamp);
				
//				ArrayList<MegaNode> nLAfter = megaApi.getChildren(megaApi.getNodeByHandle(cameraUploadHandle), MegaApiJava.ORDER_ALPHABETICAL_ASC);
//				log("SIZEEEEEE: " + nLAfter.size());
				
				uploadNext();
			}
			else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
				log("OVERQUOTA ERROR: "+e.getErrorCode());
				
				Intent intent = new Intent(this, ManagerActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setAction(ManagerActivity.ACTION_OVERQUOTA_ALERT);
				startActivity(intent);
	
			}
			else{
				log("Image Sync FAIL: " + transfer.getFileName() + "___" + e.getErrorString());
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
			}
		}
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		if (canceled) {
			log("Transfer cancel: " + transfer.getFileName());

			if((lock != null) && (lock.isHeld()))
				try{ lock.release(); } catch(Exception ex) {}
			if((wl != null) && (wl.isHeld()))
				try{ wl.release(); } catch(Exception ex) {}
			
			megaApi.cancelTransfer(transfer);
			DropboxImportService.this.cancel();
			return;
		}
		
		final long bytes = transfer.getTransferredBytes();
//		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
		updateProgressNotification(totalSizeImported + bytes);
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log("onTransferTemporaryError: " + transfer.getFileName());
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void updateProgressNotification(final long progress) {
//		log("updateProgressNotification");
		int progressPercent = (int) Math.round((double) progress / totalSizeToImport
				* 100);
		log(progressPercent + " " + progress + " " + totalSizeToImport);
		int left = totalToImport - totalImmported;
		int current = totalToImport - left;
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		
		String message = current + " ";
		if (totalToImport == 1) {
			message += getResources().getQuantityString(
					R.plurals.general_num_files, 1);
		} else {
			message += getString(R.string.general_x_of_x)
					+ " "
					+ totalToImport;
			
			if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB)
			{
				message += " "
					+ getResources().getQuantityString(
							R.plurals.general_num_files, totalToImport);
			}
		}

		Intent intent = new Intent(DropboxImportService.this, ManagerActivity.class);
		intent.setAction(ManagerActivity.ACTION_CANCEL_CAM_SYNC);	
		String info = Util.getProgressSize(DropboxImportService.this, progress, totalSizeToImport);

		PendingIntent pendingIntent = PendingIntent.getActivity(DropboxImportService.this, 0, intent, 0);
		Notification notification = null;
		
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			mBuilder
				.setSmallIcon(R.drawable.ic_stat_camera_sync)
				.setProgress(100, progressPercent, false)
				.setContentIntent(pendingIntent)
				.setOngoing(true)
				.setContentTitle(message)
				.setContentInfo(info)
				.setContentText(getString(R.string.settings_camera_notif_title))
				.setOnlyAlertOnce(true);
			notification = mBuilder.getNotification();
//					notification = mBuilder.build();
		}
		else
		{
			notification = new Notification(R.drawable.ic_stat_camera_sync, null, 1);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
			notification.contentIntent = pendingIntent;
			notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_camera_sync);
			notification.contentView.setTextViewText(R.id.status_text, message);
			notification.contentView.setTextViewText(R.id.progress_text, info);
			notification.contentView.setProgressBar(R.id.status_progress, 100, progressPercent, false);
		}
		
		if (!isForeground) {
			log("starting foreground!");
			startForeground(notificationId, notification);
			isForeground = true;
		} else {
			mNotificationManager.notify(notificationId, notification);
		}
	}
	
	private void onQueueComplete(boolean success, int totalImmported) {
		log("onQueueComplete");
		log("Stopping foreground!");
		log("stopping service! success: " + successCount + " total: " + totalToImport);
		megaApi.resetTotalUploads();
		
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}
		
		//Sleep so the SDK keeps alive
		//TODO: Must create a method to know if the SDK is waiting for any operation
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
		if (totalImmported == 0) {
			log("totalImmported == 0");
		} else {
			log("stopping service!");
			if (success){
				if (totalSizeImported != 0){
//					showCompleteSuccessNotification();
				}
			}
		}
				
		log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
		isForeground = false;
		finish();
		stopForeground(true);
		stopSelf();				
	}
	
	@Override
	public void onTaskRemoved (Intent rootIntent){
		log("onTaskRemoved -> retryLater");
		retryLater();
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		log("onNodesUpdate");

	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}
}
