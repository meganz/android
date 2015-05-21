package nz.mega.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.widget.RemoteViews;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;


public class DropboxImportService extends Service implements MegaRequestListenerInterface, MegaTransferListenerInterface, MegaGlobalListenerInterface{

	public static String DROPBOX_IMPORT = "Dropbox Import";
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
	
	CreateFolderTreeTask createFolderTreeTask;
	
	static public boolean running = false;
	private boolean canceled;
	
	private Handler handler;
	
//	private CameraObserver cameraObserver;
		
	private int totalImported;
	private long totalSizeToImport;
	private int totalToImport;
	private long totalSizeImported;
	private Queue<Entry> filesToUpload = new LinkedList<Entry>();
	private Queue<Entry> filesToDownload = new LinkedList<Entry>();
	private Queue<Entry> filesUploaded = new LinkedList<Entry>();
	
	Object transferFinished = new Object();
	
	private boolean isForeground;
	
	private int notificationId = 10;
	private int notificationIdFinal = 20;
	
	private static long lastRun = 0;
	
	String localPath = "";
	long dropboxHandle = -1;
	
	Intent intentCreate = null;
	
	MegaPreferences prefs;
	
//	boolean newFileList = false;
	boolean stopped = false;
	
	Thread task;	
	Thread importTask;

	static DropboxImportService dropboxImportService;
	
    ProgressListener mProgressListener = new ProgressListener() {

    	 @Override
         public void onProgress(long downloaded, long total) {
             // TODO Auto-generated method stub
    		 log("long downloaded: "+downloaded+" long arg1: "+total);
    		 
    		 if(downloaded==total){
    			 log("FINISH!!");
    		 }
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
		
		MegaNode parentDropbox = megaApi.getNodeByPath("/"+DROPBOX_IMPORT);
		if(parentDropbox==null){
			megaApi.createFolder(DROPBOX_IMPORT, megaApi.getRootNode(), this);
		}
		else{
			dropboxHandle=parentDropbox.getHandle();
		}
				
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
						finish();
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
						finish();
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
						finish();
						return START_NOT_STICKY;
					}
					else{
						finish();
						return START_NOT_STICKY;
					}
				}
			}
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
	
	List<Entry> foldersCreation = Collections.synchronizedList(new ArrayList<Entry>());
	
	private class CreateFolderTreeTask extends Thread implements MegaRequestListenerInterface{
		DropboxImportService dImpServ;
		
		CreateFolderTreeTask(DropboxImportService dImpServ){
			this.dImpServ = dImpServ;			
		}
		
		@Override
		public void run()
		{
			synchronized(foldersCreation)
			{
				createNextFolder();
			}
		}
		
		private void createNextFolder()
		{
			log("createNextFolder");
			
			if (foldersCreation.isEmpty())
			{
				synchronized(this){
		            notifyAll();
		        }
				return;
			}
			
			Entry ent = foldersCreation.get(0);
			
			String pathParent = "/"+DROPBOX_IMPORT+ent.parentPath();
			log("pathNoName: "+pathParent);
			
			MegaNode parentNode = megaApi.getNodeByPath(pathParent);
			if(parentNode==null){
				//Create the folder				
				log("No parent");
			}
			else{
				log("Create the folder: "+ent.fileName()+"in the node: "+pathParent);
				megaApi.createFolder(ent.fileName(), parentNode, this);
			}
		}
		
		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart: " + request.getRequestString());
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) 
		{
			if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER)
			{
				if (e.getErrorCode() == MegaError.API_OK)
				{	
					Entry ent = foldersCreation.get(0);
					log("Removed: "+ent.path);
					foldersCreation.remove(0);					
					createNextFolder();				
				}
			}
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub			
		}
		
		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}
	}
		
	void initSync(){
		log("initSync");
		
		if(!wl.isHeld()){ 
			wl.acquire();
		}
		if(!lock.isHeld()){
			lock.acquire();
		}
		
		String destination = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.dropboxDIR;		
		File file = new File(destination);
		if(!file.exists()){
			file.mkdirs();
		}
		
		totalToImport = filesToDownload.size();
		totalImported=0;
		totalSizeImported=0;		
		totalSizeToImport = 0;

        Entry dirent;
		try {
			if(mDBApi!=null){
				dirent = mDBApi.metadata("/", 1000, null, true, null);
				ArrayList<Entry> files = new ArrayList<Entry>();
	            for (Entry ent: dirent.contents) 
	            {
	                files.add(ent);// Add it to the list of thumbs we can choose from                       
	                //dir = new ArrayList<String>();
	                if(ent.isDir){
	                	listDirectory(ent);
	                }
	                else{
	                	filesToDownload.add(ent);
	                	totalSizeToImport = totalSizeToImport + ent.bytes;
//	                	log("Added: "+ent.path+"size: "+ent.size+"totalSize: "+totalSizeToImport+"bytes: "+ent.bytes);
	                }
	            }
     
	            log("   "+filesToDownload.size());
	            
			}
			else{
				log("MDBApi NUUUUUULLLLLL");
			}
			
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			finish();
		}
		
		updateProgressNotification(totalSizeImported);
		
//		Log.i("DbExampleLog", "The file's rev is: " + info.getMetadata().rev);
		
		totalToImport = filesToDownload.size();
		
		//Start the thread to create the folder tree
		log("Start CreateFolderTreeTask");
		createFolderTreeTask = new CreateFolderTreeTask(this);
		createFolderTreeTask.start();
		
		synchronized(createFolderTreeTask){
            try{
                System.out.println("Waiting for b to complete...");
                createFolderTreeTask.wait();
            }catch(InterruptedException e){
                e.printStackTrace();
            }            
        }
		
		log("Start ImportTask");
		
		importTask = new Thread()
		{
			@Override
			public void run()
			{
				log("Run: initSyn");
				importFile();
			}
		};
		importTask.start();
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
	
	private void importFile(){		
		log("importFile");
		
		int result = shouldRun();
		if (result != 0){
			retryLater();
			return;
		}		
		
		if (filesToDownload.isEmpty())
		{
			onQueueComplete(true, totalImported);			
		}
		else{
			//Get the first element of the queue
			Entry entry = filesToDownload.element();
			log("Time to import: "+entry.fileName());
			
			String destination2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.dropboxDIR + "/" + entry.fileName();		
			File file2 = new File(destination2);
			
			log("destination: "+destination2);
			
			FileOutputStream outputStream;
			try {
				outputStream = new FileOutputStream(file2);

				DropboxFileInfo info = mDBApi.getFile(entry.path, null, outputStream, mProgressListener);				
				
				//Remove the filename of the path
				String parentPath = entry.path;
				parentPath = parentPath.replace(entry.fileName(),"");
				
				parentPath = "/"+DROPBOX_IMPORT+parentPath;
				log("parentPath: "+parentPath);
				
				MegaNode parent = megaApi.getNodeByPath(parentPath);
				
				//Upload in the corresponding folder
				log("Upload the file: "+entry.fileName()+" in the folder: "+entry.path);
				
				if(parent!=null){
					megaApi.startUpload(destination2, parent, entry.fileName(), this);
				}				
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
		
		int result = shouldRun();
		if (result != 0){
			retryLater();
			return;
		}
		
		foldersCreation.add(entry);
		
		Entry dirent;
		String path=entry.path;
		
		try {
			dirent = mDBApi.metadata(path, 1000, null, true, null);
			ArrayList<Entry> files = new ArrayList<Entry>();
	        ArrayList<String> dir=new ArrayList<String>();
	        for (Entry ent: dirent.contents) 
	        {
	            files.add(ent);
	            if(ent.isDir){
	            	listDirectory(ent);
	            }
	            else{
	            	filesToDownload.add(ent);
	            	totalSizeToImport = totalSizeToImport + ent.bytes;
	            	log("Added: "+ent.path);
	            }
	        }
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	/*

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
	}*/
	
	@SuppressLint("DefaultLocale")
	private void showCompleteSuccessNotification() {
		log("showCompleteSuccessNotification");

		String message = String.format(
				"%d %s %s (%s)",
				totalImported,
				getResources().getQuantityString(R.plurals.general_num_files,
						totalImported), getString(R.string.upload_uploaded),
				Formatter.formatFileSize(DropboxImportService.this, totalSizeImported));
		String title = getString(R.string.dropbox_import_notif_complete);
		Intent intent = new Intent(DropboxImportService.this, ManagerActivity.class);
		intent.putExtra(ManagerActivity.EXTRA_OPEN_FOLDER, dropboxHandle);

		mBuilderCompat
			.setSmallIcon(R.drawable.ic_stat_camera_sync)
			.setContentIntent(PendingIntent.getActivity(DropboxImportService.this, 0, intent, 0))
			.setAutoCancel(true).setTicker(title).setContentTitle(title)
			.setContentText(message)
			.setOngoing(false);
	
		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}

	private void finish(){
		log("finish DropboxImportService");
		
		if(running){
			handler.removeCallbacksAndMessages(null);
			running = false;
		}
		cancel();
	}
	
	private void reset() {
		log("---====  RESET  ====---");
		totalImported = -1;
		totalSizeToImport = 0;
		totalToImport = 0;
		totalSizeImported = 0;
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
					.setContentTitle(getString(R.string.dropbox_import_notif_error))
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
		
		 if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){		
				if (e.getErrorCode() == MegaError.API_OK){
					log("Folder created: "+request.getName());				
					//Add in the database the new folder
					String name = request.getName();
					if(name.equals(DROPBOX_IMPORT)){
						log("Dropbox folder created");						
					}

//					retryLaterShortTime();
				}
			}
		
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

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferStart: " + transfer.getFileName());
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		log("onTransferFinish: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
		log("transfer.getPath:" + transfer.getPath());
		log("transfer.getNodeHandle:" + transfer.getNodeHandle());
		if (canceled) {
			log("Cancelled: " + transfer.getFileName());
			if((lock != null) && (lock.isHeld()))
				try{ lock.release(); } catch(Exception ex) {}
			if((wl != null) && (wl.isHeld()))
				try{ wl.release(); } catch(Exception ex) {}
			DropboxImportService.this.cancel();
		}
		else{
			if (e.getErrorCode() == MegaError.API_OK) {
				totalSizeImported += transfer.getTransferredBytes();
				totalImported = totalImported + 1;
				log("Transfer finish OK: " + transfer.getPath());
				
				File previewDir = PreviewUtils.getPreviewFolder(this);
				File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
				File thumbDir = ThumbnailUtils.getThumbFolder(this);
				File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
				megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
				megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());
				
				//Delete the file in the queue
				Entry entry = filesToDownload.remove();
				log("Deleted: "+entry.path);
				
				//Delete the local file
				String pathFile = transfer.getPath();						
				File f = new File(pathFile);
				f.delete();	
				log("Deleted from the device: "+f.getAbsolutePath());
				
				//Import next file
				importTask = new Thread()
				{
					@Override
					public void run()
					{
						log("Run: initSyn");
						importFile();
					}
				};
				importTask.start();
			}
			else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
				log("OVERQUOTA ERROR: "+e.getErrorCode());
				
				Intent intent = new Intent(this, ManagerActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setAction(ManagerActivity.ACTION_OVERQUOTA_ALERT);
				startActivity(intent);
	
			}
			else{
				log("TransferDropbox FAIL: " + transfer.getFileName() + "___" + e.getErrorString());
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
		int progressPercent = (int) Math.round((double) progress / totalSizeToImport* 100);
		log("updateProgressNotification: " +progressPercent + " " + progress + " " + totalSizeToImport);
		int left = totalToImport - totalImported;
		int current = totalToImport - left;
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		
		String message = current + " ";
		if(progress>1)
		{
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
		}
		else{
			message = getString(R.string.download_preparing_files);
		}
			

		Intent intent = new Intent(DropboxImportService.this, ManagerActivity.class);
		intent.setAction(ManagerActivity.ACTION_CANCEL_DROPBOX_IMPORT);	
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
				.setContentText(getString(R.string.dropbox_import_notif_title))
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
	
	private void onQueueComplete(boolean success, int totalImported) {
		log("onQueueComplete");
		log("Stopping foreground service!");
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
		
		if (totalImported == 0) {
			log("totalImported == 0");
		} else {
			log("stopping service!");
			if (success){
				if (totalSizeImported != 0){
					showCompleteSuccessNotification();
				}
			}
		}
		
		//Delete the folder Dropbox in device
		//Delete the local file
		String pathFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.dropboxDIR; 						
		File f = new File(pathFile);
		try {
			Util.deleteFolderAndSubfolders(this,f);
			log("Deleted from the device: "+f.getAbsolutePath());	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log("Not deleted ERROR!");	
		}
		log("Deleted from the device: "+f.getAbsolutePath());		
				
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
