package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaTransferListenerInterface;
import com.mega.sdk.NodeList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.widget.RemoteViews;

public class CameraSyncService extends Service implements MegaRequestListenerInterface, MegaTransferListenerInterface{

	public static String PHOTO_SYNC = "PhotoSync";
	public static String ACTION_CANCEL = "CANCEL_SYNC";
	public static String ACTION_STOP = "STOP_SYNC";
	public static String ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER ="PHOTOS_VIDEOS_NEW_FOLDER";
	public final static int SYNC_OK = 0;
	public final static int CREATE_PHOTO_SYNC_FOLDER = 1;
	
	public final static String EXTRA_NEW_FILE_PATH = "newFilePath";
	
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
	
//	private CameraObserver cameraObserver;
	
	Thread task;
	
	private int totalUploaded;
	private long totalSizeToUpload;
	private int totalToUpload;
	private long totalSizeUploaded;
	private int successCount;
	private ConcurrentLinkedQueue<File> filesToUpload = new ConcurrentLinkedQueue<File>();
	private ConcurrentLinkedQueue<File> filesUploaded = new ConcurrentLinkedQueue<File>();
	
	Object transferFinished = new Object();
	
	private MegaTransferListenerInterface listener;
	
	private boolean isForeground;
	
	private int notificationId = 3;
	private int notificationIdFinal = 6;
	
	private static long lastRun = 0;
	
	String localPath = "";
	
	Intent intentCreate = null;
	
	private int lastError = 0;
	
	Preferences prefs;
	
	boolean newFileList = false;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate CamSync");
		reset();
		
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
		newFileList = false;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");
		
		try{
			app = (MegaApplication) getApplication();
		}
		catch(Exception ex){
			finish();
			return START_NOT_STICKY;
		}
		megaApi = app.getMegaApi();
		
		if (intent != null){
			if (intent.getAction() != null){
				if (intent.getAction().equals(ACTION_CANCEL)){
					log("Cancel intent");
					if (megaApi != null){
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
						dbH.setCamSyncEnabled(false);
						return START_NOT_STICKY;
					}
					else{
						cancel();
						return START_NOT_STICKY;
					}
				}
				else if (intent.getAction().equals(ACTION_STOP)){
					if (megaApi != null){
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
						dbH.setCamSyncEnabled(false);
						return START_NOT_STICKY;
					}
					else{
						cancel();
						return START_NOT_STICKY;
					}
				}
				else if (intent.getAction().equals(ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER)){
					log("List photos and videos");
					if (megaApi != null){
						newFileList = true;
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
						return START_NOT_STICKY;
					}
					else{
						cancel();
						return START_NOT_STICKY;
					}
				}
			}
		}
		
		if (newFileList){
			return START_NOT_STICKY;
		}
		
		if (megaApi == null){
			finish();
			return START_NOT_STICKY;
		}

		credentials = dbH.getCredentials();
		if (credentials == null){
			log("There are not user credentials");
			finish();
			return START_NOT_STICKY;
		}
		
		String lastEmail = credentials.getEmail();
		String gSession = credentials.getSession();
		
		if (megaApi.getRootNode() == null){
			log("RootNode = null");
			megaApi.fastLogin(gSession, this);
//			cancel();
//			retryLater();
			return START_NOT_STICKY;
		}
		
		prefs = dbH.getPreferences();
		if (prefs == null){
			dbH.setFirstTime(false);
			dbH.setStorageAskAlways(true);
			dbH.setCamSyncEnabled(false);
			dbH.setPinLockEnabled(false);
			dbH.setPinLockCode("");
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
					log("Not enabled");
					finish();
					return START_NOT_STICKY;
				}
				else{
					localPath = prefs.getCamSyncLocalPath();
					if ((localPath == null) || ("".compareTo(localPath) == 0) ){
						log("Not defined, so not enabled");
						finish();
						return START_NOT_STICKY;
					}
					
					boolean isWifi = Util.isOnWifi(this);
					if (prefs.getCamSyncWifi() == null || Boolean.parseBoolean(prefs.getCamSyncWifi())){
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
				log("must create the folder");
				megaApi.createFolder(PHOTO_SYNC, megaApi.getRootNode(), this);
				cancel();
				retryLater();
				return START_NOT_STICKY;
			}
		}
		
		log("TODO OK");
		log ("photosynchandle = " + photosyncHandle);
		
//		if (cameraObserver == null){
//			cameraObserver = new CameraObserver();
//			cameraObserver.startWatching();
//			log("observer started");
//		}
		
		onHandleIntent(intent);
		
				
		
//		log("onStartCommand");
//		try { 
//			app = (MegaApplication)getApplication(); 
//		}
//		catch(Exception ex) {
//			finish();
//			return START_NOT_STICKY;
//		}
//		megaApi = app.getMegaApi();
//		
//		int result = shouldRun();
//		switch(result){
//			case SYNC_OK:{
//				folderExists(intent);
//				break;
//			}
//			case CREATE_PHOTO_SYNC_FOLDER:{
//				intentCreate = intent;
//				return START_REDELIVER_INTENT;
//			}
//			default:{
//				return result;
//			}
//		}
		
		return START_REDELIVER_INTENT;		
	}
	
	public void onHandleIntent(Intent intent){
		
//		if (filesToUpload.size() == 0){
//			reset();
//		}
		
		String newFilePath = null;
		if (intent != null){
			newFilePath = intent.getStringExtra(EXTRA_NEW_FILE_PATH);
		}
		
		if (newFilePath != null){
			log("newFilePath != null");
			File file = new File(newFilePath);
			MegaNode nodeExists = megaApi.getNodeByPath("/" + PHOTO_SYNC + "/" + file.getName());
			if (nodeExists == null){
				
				Iterator<File> it = filesToUpload.iterator();
				boolean fileAlreadyToUpload = false;
				while (it.hasNext()){
					File f = it.next();
					if (f.getAbsolutePath().compareTo(file.getAbsolutePath()) == 0){
						fileAlreadyToUpload = true;
						break;
					}
				}
				
				//If the file is ready to upload already
				if (!fileAlreadyToUpload){
					totalToUpload++;
					totalSizeToUpload += file.length();
					filesToUpload.add(file);	
				}
			}
		}
		else{
			log("newFilePath == null");
			List<String> cameraFiles = new ArrayList<String>();
			if (prefs.getCamSyncFileUpload() == null){
				dbH.setCamSyncFileUpload(Preferences.ONLY_PHOTOS);
				cameraFiles.addAll(getCameraPhotosAll());
			}
			else{
				switch(Integer.parseInt(prefs.getCamSyncFileUpload())){
					case Preferences.ONLY_PHOTOS:{
						cameraFiles.addAll(getCameraPhotosAll());
						break;
					}
					case Preferences.ONLY_VIDEOS:{
						cameraFiles.addAll(getCameraVideosAll());
						break;
					}
					case Preferences.PHOTOS_AND_VIDEOS:{
						cameraFiles.addAll(getCameraPhotosAll());
						cameraFiles.addAll(getCameraVideosAll());
						break;
					}
				}
			}
			
			MegaNode targetNode = megaApi.getNodeByHandle(photosyncHandle);
			if(targetNode == null){
				showSyncError(R.string.settings_camera_notif_error_no_folder);
				finish();
				return;
			}
			
			for (String filePath : cameraFiles) {
				File file = new File(filePath);
				if(!file.exists()){
					continue;
				}
			
				
				MegaNode nodeExists = megaApi.getNodeByPath("/" + PHOTO_SYNC + "/" + file.getName());
				if (nodeExists == null){
					
					Iterator<File> it = filesToUpload.iterator();
					boolean fileAlreadyToUpload = false;
					while (it.hasNext()){
						File f = it.next();
						if (f.getAbsolutePath().compareTo(file.getAbsolutePath()) == 0){
							fileAlreadyToUpload = true;
							break;
						}
					}
					
					it = filesUploaded.iterator();
					boolean fileAlreadyUploaded = false;
					while (it.hasNext()){
						File f = it.next();
						if (f.getAbsolutePath().compareTo(file.getAbsolutePath()) == 0){
							fileAlreadyUploaded = true;
							break;
						}
					}
					
					//If the file is ready to upload already
					if (!fileAlreadyToUpload && !fileAlreadyUploaded){
						filesToUpload.add(file);	
						
					}
				}
			}
		}
		
		log("TOTALSIZETOUPLOAD = " + totalSizeToUpload + "___" + totalToUpload);
		
		
		while (filesToUpload.size() > 0){
			File f = filesToUpload.poll();
			filesUploaded.add(f);
			log("Ficheros subidos: " + f.getAbsolutePath());
			if(!wl.isHeld()){ 
				wl.acquire();
			}
			if(!lock.isHeld()){
				lock.acquire();
			}
			totalToUpload++;
			totalSizeToUpload += f.length();
			log("startUpload called");
			megaApi.startUpload(f.getAbsolutePath(), megaApi.getNodeByHandle(photosyncHandle), this);
		}
		
		log ("filestoupload.size() = " + filesToUpload.size());

	}
	
	private void reset() {
		log("---====  RESET  ====---");
		totalUploaded = 0;
		totalSizeToUpload = 0;
		totalToUpload = 0;
		totalSizeUploaded = 0;
		successCount = 0;
		filesToUpload.clear();
		filesUploaded.clear();
	}
	
	private void cancel() {
		canceled = true;
		isForeground = false;
		stopForeground(true);
		stopSelf();
	}
	
	private void retryLater(){
		log("retryLater");
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				onStartCommand(null, 0, 0);
			}
		}, 15 * 1000);
	}
	
	@SuppressLint("DefaultLocale")
	private void showCompleteSuccessNotification() {
		log("showCompleteSuccessNotification");

		String message = String.format(
				"%d %s %s (%s)",
				totalUploaded,
				getResources().getQuantityString(R.plurals.general_num_files,
						totalUploaded), getString(R.string.upload_uploaded),
				Formatter.formatFileSize(CameraSyncService.this, totalSizeUploaded));
		String title = getString(R.string.settings_camera_notif_complete);
		Intent intent = new Intent(CameraSyncService.this, ManagerActivity.class);
		intent.putExtra(ManagerActivity.EXTRA_OPEN_FOLDER, photosyncHandle);

		mBuilderCompat
			.setSmallIcon(R.drawable.ic_stat_camera_sync)
			.setContentIntent(PendingIntent.getActivity(CameraSyncService.this, 0, intent, 0))
			.setAutoCancel(true).setTicker(title).setContentTitle(title)
			.setContentText(message)
			.setOngoing(false);
	
		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}
	
	private List<String> getCameraPhotosAll() {
		List<String> photos = new ArrayList<String>();
		for (File folder : getCameraFolders()) {
			photos.addAll(getCameraPhotos(folder));
		}
		return photos;
	}

	private List<String> getCameraVideosAll() {
		List<String> photos = new ArrayList<String>();
		for (File folder : getCameraFolders()) {
			photos.addAll(getCameraVideos(folder));
		}
		return photos;
	}
	
	private List<File> getCameraFolders() {
		File dcimFolder = new File(localPath);
//		File dcimFolder = new File(CameraSettings.getSource(this));
		try {dcimFolder = dcimFolder.getCanonicalFile();} catch (IOException e) {}
		List<File> folders = new ArrayList<File>();
		if (dcimFolder.isDirectory() && dcimFolder.canRead()) {
			folders.add(dcimFolder);
			File[] subfolders = dcimFolder.listFiles();
			if (subfolders != null){
				for (File folder : dcimFolder.listFiles()) {
					try {folder = folder.getCanonicalFile();} catch (IOException e) {}
					if (folder.isDirectory() && !folder.getName().startsWith(".")) {
						folders.add(folder);
					}
				}
			}
		}
		return folders;
	}
	
	private List<String> getCameraPhotos(File folder) {
		ArrayList<String> result = new ArrayList<String>();
		try {folder = folder.getCanonicalFile();} catch (IOException e) {}
		File[] files = folder.listFiles();
		if(files==null) return result;
		for(int i=0; i<files.length; i++)
		{
			File file = files[i];
			if(MimeType.typeForName(file.getName()).isImage())
			{
				result.add(file.getAbsolutePath());
			}
		}
		return result;
	}
	
	private List<String> getCameraVideos(File folder) {
		ArrayList<String> result = new ArrayList<String>();
		try {folder = folder.getCanonicalFile();} catch (IOException e) {}
		File[] files = folder.listFiles();
		if(files==null) return result;
		for(int i=0; i<files.length; i++)
		{
			File file = files[i];
			if(MimeType.typeForName(file.getName()).isVideo())
			{
				result.add(file.getAbsolutePath());
			}
		}
		return result;
	}
	
	
	private void finish(){
		log("finish CameraSyncService");
		
		if(running){
			cancel();
		}
		
//		if (cameraObserver != null) {
//			cameraObserver.stopWatching();
//			log("observer stopped");
//		}
//		cameraObserver = null;
//		log("observer deleted");
		
		handler.removeCallbacksAndMessages(null);
		running = false;
		stopSelf();
	}
	
	public void onDestroy(){
		log("onDestroy");
		running = false;
		retryLater();
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
				Intent intent = new Intent(CameraSyncService.this, ManagerActivity.class);
						
				mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setContentIntent(PendingIntent.getActivity(CameraSyncService.this, 0, intent, 0))
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
		
		if (request.getType() == MegaRequest.TYPE_FAST_LOGIN){
			if (e.getErrorCode() == MegaError.API_OK){
				log("Fast login OK");
				
				DatabaseHandler dbH = new DatabaseHandler(getApplicationContext()); 
				dbH.clearCredentials();
				dbH.saveCredentials(credentials);

				log("Calling fetchNodes from CameraSyncService");
				megaApi.fetchNodes(this);
			}
			else{
				retryLater();
				cancel();				
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			if (e.getErrorCode() == MegaError.API_OK){
				retryLater();
				finish();				
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MKDIR){		
			if (e.getErrorCode() == MegaError.API_OK){
				log("Folder created");
				retryLater();
				finish();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			log("cancel_transfers received");
			if (e.getErrorCode() == MegaError.API_OK){
				megaApi.pauseTransfers(false, this);
				megaApi.resetTotalUploads();
				reset();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("pause_transfer false received");
			if (e.getErrorCode() == MegaError.API_OK){
				if (newFileList){
					newFileList = false;
					log("pauseNewFileList");
//					retryLater();
					mNotificationManager.cancel(notificationId);
					mNotificationManager.cancel(notificationIdFinal);
					isForeground = false;
					stopForeground(true);
					stopSelf();
				}
				else{
					cancel();
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}
	
//	private class CameraObserver {
//
//		private ArrayList<FileObserver> observers = new ArrayList<FileObserver>();
//		
//		class CameraLauncher implements Runnable {
//			String path;
//			public void setNewFile(String path)
//			{
//				this.path = path;
//			}
//			
//			@Override
//			public void run() {
//				log("observer run");
//				Intent intent = new Intent(CameraSyncService.this,
//						CameraSyncService.class);
//				intent.putExtra(EXTRA_NEW_FILE_PATH, path);
//				startService(intent);
//			}
//		};
//
//		CameraLauncher launcher = new CameraLauncher();
//		class CameraFileObserver extends FileObserver
//		{
//			String folder;
//			CameraObserver observer;
//			public CameraFileObserver(String path, int mask, CameraObserver observer) {
//				super(path, mask);
//				folder = path;
//				this.observer = observer;
//			}
//			@Override
//			public void onEvent(int event, String path) {
//				observer.onEvent(event, new File(folder, path).getAbsolutePath());				
//			}
//		}
//		
//		
//		public CameraObserver() {
//			String path = localPath;
//			File folder = new File(path);
//			String[] files = folder.list();
//			if(folder.isDirectory())
//			{
//				observers.add(new FileObserver(folder.getAbsolutePath(),
//								FileObserver.CREATE | FileObserver.MOVED_TO)
//				{
//					@Override
//					public void onEvent(int event, String path) 
//					{
//						CameraObserver.this.onEvent(event, path);
//					}
//				});
//			}
//			
//			
//			if(files != null)
//			{
//				for(int i=0; i<files.length; i++)
//				{
//					File file = new File(files[i]);
//					File correctFile = new File(folder, file.getName());
////					log("observer file: " + correctFile.getAbsolutePath());
//					if(correctFile.isDirectory())
//					{
//						log("observer directory: " + correctFile.getAbsolutePath());
//						observers.add(new CameraFileObserver(correctFile.getAbsolutePath(),
//										FileObserver.CREATE | FileObserver.MOVED_TO, this));
//					}
//				}
//			}
//		}
//
//		public void startWatching() {
//			for(int i=0; i<observers.size(); i++)
//				observers.get(i).startWatching();			
//		}
//
//		public void stopWatching() {
//			log("stopWatching");
//			for(int i=0; i<observers.size(); i++)
//				observers.get(i).stopWatching();
//		}
//
//		public void onEvent(int event, String path) {
//			log("observer onEvent path: " + path);
//			if (path == null || path.endsWith(".tmp")) {
//				return;
//			}
//			
//			launcher.setNewFile(path);
//			handler.removeCallbacks(launcher);
//			handler.postDelayed(launcher, 2 * 1000);
//			log("runnable posted");
//		}
//	}
	
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
								CameraSyncService.class));
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
		if (canceled) {
			log("Image sync cancelled: " + transfer.getFileName());
			if((lock != null) && (lock.isHeld()))
				try{ lock.release(); } catch(Exception ex) {}
			if((wl != null) && (wl.isHeld()))
				try{ wl.release(); } catch(Exception ex) {}
			CameraSyncService.this.cancel();
		}
		else{
			if (e.getErrorCode() == MegaError.API_OK) {
				log("Image Sync OK: " + transfer.getFileName());
				totalSizeUploaded += transfer.getTransferredBytes();
				log("IMAGESYNCFILE: " + transfer.getPath());
				onUploadComplete(true);
			}
			else{
				log("Image Sync OK: " + transfer.getFileName());
				lastError = e.getErrorCode();
				onUploadComplete(false);
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
			CameraSyncService.this.cancel();
			return;
		}
		
		final long bytes = transfer.getTransferredBytes();
		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
		updateProgressNotification(totalSizeUploaded + bytes);
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log("onTransferTemporaryError: " + transfer.getFileName());
	}
	
	@SuppressLint("NewApi")
	private void updateProgressNotification(final long progress) {
		log("updateProgressNotification");
		int progressPercent = (int) Math.round((double) progress / totalSizeToUpload
				* 100);
		log(progressPercent + " " + progress + " " + totalSizeToUpload);
		int left = totalToUpload - totalUploaded;
		int current = totalToUpload - left + 1;
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		
		String message = current + " ";
		if (totalToUpload == 1) {
			message += getResources().getQuantityString(
					R.plurals.general_num_files, 1);
		} else {
			message += getString(R.string.general_x_of_x)
					+ " "
					+ totalToUpload;
			
			if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB)
			{
				message += " "
					+ getResources().getQuantityString(
							R.plurals.general_num_files, totalToUpload);
			}
		}

		Intent intent = new Intent(CameraSyncService.this, ManagerActivity.class);
		intent.setAction(ManagerActivity.ACTION_CANCEL_CAM_SYNC);	
		String info = Util.getProgressSize(CameraSyncService.this, progress, totalSizeToUpload);

		PendingIntent pendingIntent = PendingIntent.getActivity(CameraSyncService.this, 0, intent, 0);
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
	
	private void onUploadComplete(boolean success) {
		log("onCamSyncComplete");
		
		if (success){
			successCount++;
		}
		totalUploaded++;
		
		if (totalUploaded == totalToUpload){
			onQueueComplete();
		}
	}
	
	private void onQueueComplete() {
		log("onQueueComplete");
		log("Stopping foreground!");
		log("stopping service! success: " + successCount + " total: " + totalToUpload);
		megaApi.resetTotalUploads();
		
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}
		
		//Sleep so the SDK keeps alive
		//TODO: Must create a method to know if the SDK is waiting for any operation
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (successCount == 0) {
			log("stopping service!2");
		} else {
			log("stopping service!");
			showCompleteSuccessNotification();
		}
				
		log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
		isForeground = false;
		stopForeground(true);
		stopSelf();
				
	}
	
	@Override
	public void onTaskRemoved (Intent rootIntent){
		log("onTaskRemoved -> retryLater");
		retryLater();
	}
}
