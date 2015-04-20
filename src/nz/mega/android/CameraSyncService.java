package nz.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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


public class CameraSyncService extends Service implements MegaRequestListenerInterface, MegaTransferListenerInterface, MegaGlobalListenerInterface{

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
	
	static public boolean running = false;
	private boolean canceled;
	
	private Handler handler;
	
//	private CameraObserver cameraObserver;
		
	private int totalUploaded;
	private long totalSizeToUpload;
	private int totalToUpload;
	private long totalSizeUploaded;
	private int successCount;
	private ConcurrentLinkedQueue<Media> filesToUpload = new ConcurrentLinkedQueue<Media>();
	private ConcurrentLinkedQueue<Media> filesUploaded = new ConcurrentLinkedQueue<Media>();
	
	Object transferFinished = new Object();
	
	private boolean isForeground;
	
	private int notificationId = 3;
	private int notificationIdFinal = 6;
	
	private static long lastRun = 0;
	
	Queue<Media> cameraFiles = new LinkedList<Media>();
	String localPath = "";
	MegaNode cameraUploadNode = null;
	long cameraUploadHandle = -1;
	
	Intent intentCreate = null;
	
	MegaPreferences prefs;
	
	boolean newFileList = false;
	boolean stopped = false;
	
	MediaObserver mediaObserver;
	
	private class Media {
		public String filePath;
		public long timestamp;
	}
	
	Thread task;	
	
	//Secondary Variables	
	Queue<Media> mediaFilesSecondary = new LinkedList<Media>();	
	boolean secondaryEnabled= false;
	String localPathSecondary = "";
	long secondaryUploadHandle = -1;
	MegaNode secondaryUploadNode = null;
	
	long currentTimeStamp = 0;
	
//	boolean waitForOnNodesUpdate = false;
	
	static CameraSyncService cameraSyncService;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate CamSync");
		reset();
		cameraSyncService = this;
		
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
					log("Not enabled");
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
//					//TODO: now assuming that ischarging is needed
//					if (!isCharging){
//						log("no charging...");
//						finish();
//						return START_REDELIVER_INTENT;
//					}
					
					//The "Camera Upload" folder exists?
//					if (prefs.getCamSyncHandle() == null){
//						cameraUploadHandle = -1;
//					}
//					else{
//						cameraUploadHandle = Long.parseLong(prefs.getCamSyncHandle());
//						if (megaApi.getNodeByHandle(cameraUploadHandle) == null){
//							cameraUploadHandle = -1;
//						}
//						else{
//							if (megaApi.getParentNode(megaApi.getNodeByHandle(cameraUploadHandle)).getHandle() != megaApi.getRootNode().getHandle()){
//								cameraUploadHandle = -1;
//							}
//						}
//					}
					
					//Get the MEGA folder to sync the camera
					
					if (prefs.getCamSyncHandle() == null){
						cameraUploadHandle = -1;
					}
					else{
						cameraUploadHandle = Long.parseLong(prefs.getCamSyncHandle());
					}
					
					//Check the secondary folder
					if (prefs.getSecondaryMediaFolderEnabled() == null){
						dbH.setSecondaryUploadEnabled(false);
						log("Not defined, so not enabled");
						secondaryEnabled=false;
					}
					else{					
						if (!Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled())){
							log("Not enabled");
							secondaryEnabled=false;
						}					
						else{
							//Check the corresponding folders
							localPathSecondary = prefs.getLocalPathSecondaryFolder();
							if (localPathSecondary == null){
								dbH.setSecondaryUploadEnabled(false);
								log("Not defined secondary LOCAL, so not enabled");
								secondaryEnabled=false;
							}
							else{
								if ("".compareTo(localPathSecondary) == 0){
									log("Empty secondary LOCAL, so not enabled");
									dbH.setSecondaryUploadEnabled(false);
									dbH.setSecondaryFolderPath("-1");
									secondaryEnabled=false;				
								}
								else 
								{
									if ("-1".compareTo(localPathSecondary) == 0){
										log("-1 secondary LOCAL, so not enabled");
										dbH.setSecondaryUploadEnabled(false);
										secondaryEnabled=false;		
									}
									else{
										secondaryEnabled=true;		
										//Get the MEGA folder to sync the secondary media folder										
										if (prefs.getMegaHandleSecondaryFolder() == null){
											secondaryUploadHandle = -1;
										}
										else{
											secondaryUploadHandle = Long.parseLong(prefs.getMegaHandleSecondaryFolder());
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		//
		
		if (cameraUploadHandle == -1){
			//Find the "Camera Uploads" folder of the old "PhotoSync"
			ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
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
			if(n==null){
				//If ERROR with the handler: Create the folder Camera Uploads
				if (PHOTO_SYNC.compareTo(n.getName()) == 0){
					megaApi.renameNode(n, CAMERA_UPLOADS, this);	
				}
				else if (CAMERA_UPLOADS.compareTo(n.getName()) != 0){
//					dbH.setCamSyncHandle(-1);
					if (!running){
						running = true;
						megaApi.createFolder(CAMERA_UPLOADS, megaApi.getRootNode(), this);
					}
					return START_NOT_STICKY;
				}
			}
			else{
				//esta en la papelera? Si está, es como si fuera null. Si no, todo OK.
				log("Sync Folder " + cameraUploadHandle + " Node: "+n.getName());
			}			
		}
		
		if(secondaryEnabled){
			log("Secondary Enabled TRUE: "+secondaryUploadHandle);
			MegaNode secondaryN = null;
			if(secondaryUploadHandle!=-1){
				secondaryN = megaApi.getNodeByHandle(secondaryUploadHandle);
			}			
			if (secondaryN==null){
				//Create a secondary folder to sync
				log("secondaryN==null");
				//Find the "Secondary Uploads" 
				secondaryUploadHandle=-1;
				
				ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
				for (int i=0;i<nl.size();i++){
					if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
						secondaryUploadHandle = nl.get(i).getHandle();
						dbH.setSecondaryFolderHandle(secondaryUploadHandle);
						log("Found SECONDARY!!: "+nl.get(i).getName());
					}
				}
				
				if (secondaryUploadHandle == -1){
					log("must create the folder SECONDARY_UPLOADS");
					if (!running){
						running = true;
						log("Creating folder....");
						megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
					}
					else{
						log("retryLaterShortTime....");
						retryLaterShortTime();
					}
					return START_NOT_STICKY;
				}
			
//				return START_NOT_STICKY;
			}
			else{
				log("SECONDARY Sync Folder " + secondaryUploadHandle + " Node: "+secondaryN.getName());
			}
		}
		else{
			log("Secondary NOT Enabled");
		}
		
		log("shouldRun: TODO OK");
		
		return 0;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");
		
		if (megaApi == null){
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
		
		registerObservers();
		
		String projection[] = {	MediaColumns.DATA, 
				//MediaColumns.MIME_TYPE, 
				//MediaColumns.DATE_MODIFIED,
				MediaColumns.DATE_MODIFIED};
		
		String selectionCamera = null;
		String selectionSecondary = null;
		String[] selectionArgs = null;
		
		prefs = dbH.getPreferences();
		
		if (prefs != null){
			if (prefs.getCamSyncTimeStamp() != null){				
				long camSyncTimeStamp = Long.parseLong(prefs.getCamSyncTimeStamp());
				selectionCamera = "(" + MediaColumns.DATE_MODIFIED + "*1000) > " + camSyncTimeStamp;
				log("SELECTION: " + selectionCamera);
			}
			if(secondaryEnabled){
				if (prefs.getSecSyncTimeStamp() != null){
					long secondaryTimeStamp = Long.parseLong(prefs.getSecSyncTimeStamp());
					selectionSecondary = "(" + MediaColumns.DATE_MODIFIED + "*1000) > " + secondaryTimeStamp;
					log("SELECTION SECONDARY: " + selectionSecondary);
				}	
			}
		}
		
		String order = MediaColumns.DATE_MODIFIED + " ASC";
		
		ArrayList<Uri> uris = new ArrayList<Uri>();
		if (prefs.getCamSyncFileUpload() == null){
			dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
			uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		}
		else{
			switch(Integer.parseInt(prefs.getCamSyncFileUpload())){
				case MegaPreferences.ONLY_PHOTOS:{
					uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
					break;
				}
				case MegaPreferences.ONLY_VIDEOS:{
					uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
					uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
					break;
				}
				case MegaPreferences.PHOTOS_AND_VIDEOS:{
					uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
					uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
					uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
					break;
				}
			}
		}		
		
		for(int i=0; i<uris.size(); i++){
			
			Cursor cursorCamera = app.getContentResolver().query(uris.get(i), projection, selectionCamera, selectionArgs, order);
			if (cursorCamera != null){
				int dataColumn = cursorCamera.getColumnIndexOrThrow(MediaColumns.DATA);
		        int timestampColumn = cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED);
		        while(cursorCamera.moveToNext()){
		        	Media media = new Media();
			        media.filePath = cursorCamera.getString(dataColumn);
			        media.timestamp = cursorCamera.getLong(timestampColumn) * 1000;
			        
			        //Check files of the Camera Uploads
			        if (checkFile(media,localPath)){
			        	 cameraFiles.add(media);
					     log("Camera Files added: "+media.filePath);
			        }				        
		        }	
			}
			
			//Secondary Media Folder
			if(secondaryEnabled){
				Cursor cursorSecondary = app.getContentResolver().query(uris.get(i), projection, selectionSecondary, selectionArgs, order);
				if (cursorSecondary != null){
					int dataColumn = cursorSecondary.getColumnIndexOrThrow(MediaColumns.DATA);
			        int timestampColumn = cursorSecondary.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED);
			        while(cursorSecondary.moveToNext()){
			        	Media media = new Media();
				        media.filePath = cursorSecondary.getString(dataColumn);
				        media.timestamp = cursorSecondary.getLong(timestampColumn) * 1000;				        
				        
					    //Check files of Secondary Media Folder
				        if (checkFile(media,localPathSecondary)){
				        	 mediaFilesSecondary.add(media);
						     log("-----SECONDARY MEDIA Files added: "+media.filePath);
				        }
			        }
				}	
			}
			
		}
		
		cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
		if(cameraUploadNode == null){
			showSyncError(R.string.settings_camera_notif_error_no_folder);
			finish();
			return;
		}
		
		if(secondaryEnabled){
			if(secondaryUploadHandle==-1){
				ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
				boolean found = false;
				for (int i=0;i<nl.size();i++){
					if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
						secondaryUploadHandle = nl.get(i).getHandle();
						dbH.setSecondaryFolderHandle(secondaryUploadHandle);
						found = true;
						break;
					}
				}
				if(!found){
					secondaryEnabled = false;
					dbH.setSecondaryUploadEnabled(false);
				}
			}
			else{
				secondaryUploadNode = megaApi.getNodeByHandle(secondaryUploadHandle);
			}
		}
		
		totalToUpload = cameraFiles.size() + mediaFilesSecondary.size();
		totalUploaded=0;
		totalSizeUploaded=0;
		
		totalSizeToUpload = 0;
		Iterator<Media> itCF = cameraFiles.iterator();
		while (itCF.hasNext()){
			Media m = itCF.next();
			File f = new File(m.filePath);
			totalSizeToUpload = totalSizeToUpload + f.length();
		}	
		Iterator<Media> itmFS = mediaFilesSecondary.iterator();
		while (itmFS.hasNext()){
			Media m = itmFS.next();
			File f = new File(m.filePath);
			totalSizeToUpload = totalSizeToUpload + f.length();
		}		
				
//		uploadNextImage();
//		log("Time for secondary media folder!");
//		uploadNextMediaFile();
		uploadNext();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void uploadNext(){
		log("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&   UPLOAD NEXT");
		if (cameraFiles.size() > 0){
			uploadNextImage();
		}
		else if(mediaFilesSecondary.size() > 0){
			uploadNextMediaFile();
		}
		else
		{
			onQueueComplete(true, totalUploaded);
			finish();
		}
	}
	
	void uploadNextMediaFile(){
		
		totalUploaded++;
		log("------------------------------------------------uploadNextMediaFile: "+totalUploaded +" of "+totalToUpload);
		
		int result = shouldRun();
		if (result != 0){
			retryLater();
			return;
		}
		
		if (mediaFilesSecondary.size() > 0){
			final Media mediaSecondary = mediaFilesSecondary.poll();
			
			File file = new File(mediaSecondary.filePath);
			if(!file.exists()){
				uploadNext();
			}
			
			String localFingerPrint = megaApi.getFingerprint(mediaSecondary.filePath);
			
			MegaNode nodeExists = null;
			//Source file
			File sourceFile = new File(mediaSecondary.filePath);
	
			nodeExists = megaApi.getNodeByFingerprint(localFingerPrint, secondaryUploadNode);
			if(nodeExists == null)
			{			
				//Check if the file is already uploaded in the correct folder but without a fingerprint
				int photoIndex = 0;
				MegaNode possibleNode = null;
				String photoFinalName;
				do {
					//Iterate between all files with the correct target name
					//TODO Esto influye? Que es exactamente?
					photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
					possibleNode = megaApi.getChildNode(secondaryUploadNode, photoFinalName);
					
					// If the file matches name, mtime and size, and doesn't have a fingerprint, 
					// => we consider that it's the correct one
					if(possibleNode != null &&
						sourceFile.length() == possibleNode.getSize() &&
						megaApi.getFingerprint(possibleNode) == null)
					{
						nodeExists = possibleNode;
						break;
					}
					
					//Continue iterating
					photoIndex++;
				} while(possibleNode != null);
				
				if(nodeExists == null)
				{
					// If the file wasn't found by fingerprint nor in the destination folder,
					// take a look in the folder from v1
					SharedPreferences prefs = this.getSharedPreferences("prefs_main.xml", 0);
					if(prefs != null)
					{ 
						String handle = prefs.getString("camera_sync_folder_hash", null);
						if(handle != null)
						{
							MegaNode prevFolder = megaApi.getNodeByHandle(MegaApiAndroid.base64ToHandle(handle));
							if(prevFolder != null)
							{
								// If we reach this code, the app is an updated v1 and the previously selected
								// folder still exists
	
								// If the file matches name, mtime and size, and doesn't have a fingerprint, 
								// => we consider that it's the correct one
								possibleNode = megaApi.getChildNode(prevFolder, sourceFile.getName());
								if(possibleNode != null &&
										sourceFile.length() == possibleNode.getSize() &&
										megaApi.getFingerprint(possibleNode) == null)
								{
									nodeExists = possibleNode;
								}
							}
						}
					}
				}
			}
			
			if (nodeExists == null){
				log("SECONDARY MEDIA: SUBIR EL FICHERO: " + mediaSecondary.filePath);
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(mediaSecondary.timestamp);
				log("YYYY-MM-DD HH.MM.SS -- " + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR_OF_DAY) + "." + cal.get(Calendar.MINUTE) + "." + cal.get(Calendar.SECOND));
				boolean photoAlreadyExists = false;
				ArrayList<MegaNode> nL = megaApi.getChildren(secondaryUploadNode, MegaApiJava.ORDER_ALPHABETICAL_ASC);
				for (int i=0;i<nL.size();i++){
					if ((nL.get(i).getName().compareTo(Util.getPhotoSyncName(mediaSecondary.timestamp, mediaSecondary.filePath)) == 0) && (nL.get(i).getSize() == file.length())){
						photoAlreadyExists = true;
					}
				}
				
				if (!photoAlreadyExists){					
					
					if(Boolean.parseBoolean(prefs.getKeepFileNames())){
						//Keep the file names as device
						megaApi.startUpload(file.getAbsolutePath(), secondaryUploadNode, file.getName(), this);
						log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
					}
					else{
						int photoIndex = 0;
						String photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
						for (int i=0;i<nL.size();i++){
							photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);						
							if (nL.get(i).getName().compareTo(photoFinalName) == 0){
								photoIndex++;
							}
						}
						photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);					
						currentTimeStamp = mediaSecondary.timestamp;
						megaApi.startUpload(file.getAbsolutePath(), secondaryUploadNode, photoFinalName, this);
						log("CHANGED!!!! MediaFinalName: " + photoFinalName + "______" + photoIndex);
					}					
				}
				else{
					currentTimeStamp = mediaSecondary.timestamp;
					dbH.setSecSyncTimeStamp(currentTimeStamp);
					File f = new File(mediaSecondary.filePath);
					totalSizeUploaded += f.length();
					uploadNext();	
				}
			}
			else{
				log("NODO SECONDARY: " + megaApi.getParentNode(nodeExists).getName() + "___" + nodeExists.getName());				
				if (megaApi.getParentNode(nodeExists).getHandle() != secondaryUploadHandle){
					
					if(Boolean.parseBoolean(prefs.getKeepFileNames())){
						//Keep the file names as device
						megaApi.copyNode(nodeExists, secondaryUploadNode, file.getName(), this);
						log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
					}
					else{
						int photoIndex = 0;
						String photoFinalName = null;
						do {
							photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
							photoIndex++;
						}while(megaApi.getChildNode(secondaryUploadNode, photoFinalName) != null);
		
						currentTimeStamp = mediaSecondary.timestamp;
						megaApi.copyNode(nodeExists, secondaryUploadNode, photoFinalName, this);
						log("CHANGED!!!! SecondaryFinalName: " + photoFinalName + "______" + photoIndex);
					}	
						
//				if (megaApi.getNodeByPath("/" + CAMERA_UPLOADS + "/" + nodeExists.getName()) == null){
					
//					boolean photoAlreadyExists = false;
//					ArrayList<MegaNode> nL = megaApi.getChildren(cameraUploadNode);
//					for (int i=0;i<nL.size();i++){
//						if (nL.get(i).getName().compareTo(Util.getPhotoSyncName(media.timestamp, media.filePath)) == 0){
//							photoAlreadyExists = true;
//						}
//					}
//					
//					if (!photoAlreadyExists){
//						currentTimeStamp = media.timestamp;
//						megaApi.copyNode(nodeExists, cameraUploadNode, this);
//					}
//					else{
//						currentTimeStamp = media.timestamp;
//						dbH.setCamSyncTimeStamp(currentTimeStamp);
//						File f = new File(media.filePath);
//						totalSizeUploaded += f.length();
//						uploadNextImage();	
//					}
				}
				else{
//					if (nodeExists.getName().compareTo(Util.getPhotoSyncName(media.timestamp, media.filePath)) == 0){
//						currentTimeStamp = media.timestamp;
//						dbH.setCamSyncTimeStamp(currentTimeStamp);
//						File f = new File(media.filePath);
//						totalSizeUploaded += f.length();
//						uploadNextImage();	
//					}
//					else{
//						log("The file is in PhotoSync but with a different name");
//						megaApi.renameNode(nodeExists, Util.getPhotoSyncName(media.timestamp, media.filePath), this);
//					}
					
					final MegaNode existingNode = nodeExists;
					handler.post(new Runnable() {
						@Override
						public void run() {
							new LookForRenameTask(mediaSecondary, secondaryUploadNode).rename(existingNode); 
						}
					});
				}
			}
		}
		else{
			uploadNext();
//			if (cameraFiles.size() <= 0){
//				log("Finishing service: no more elements queuing");
//				onQueueComplete(true, totalUploaded);
//				finish();
//			}			
		}
	}
	
	void uploadNextImage(){
		totalUploaded++;
		log("============================================================================uploadNextImage: "+totalUploaded +" of "+totalToUpload);
		
		int result = shouldRun();
		if (result != 0){
			retryLater();
			return;
		}
		
		if (cameraFiles.size() > 0){
			final Media media = cameraFiles.poll();
			
			File file = new File(media.filePath);
			if(!file.exists()){
				uploadNext();
			}
			
			String localFingerPrint = megaApi.getFingerprint(media.filePath);
			
			MegaNode nodeExists = null;
			//Source file
			File sourceFile = new File(media.filePath);
	
			nodeExists = megaApi.getNodeByFingerprint(localFingerPrint, cameraUploadNode);
			if(nodeExists == null)
			{			
				//Check if the file is already uploaded in the correct folder but without a fingerprint
				int photoIndex = 0;
				MegaNode possibleNode = null;
				String photoFinalName;
				do {
					//Iterate between all files with the correct target name
					//TODO cambio?
					photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
					possibleNode = megaApi.getChildNode(cameraUploadNode, photoFinalName);
					
					// If the file matches name, mtime and size, and doesn't have a fingerprint, 
					// => we consider that it's the correct one
					if(possibleNode != null &&
						sourceFile.length() == possibleNode.getSize() &&
						megaApi.getFingerprint(possibleNode) == null)
					{
						nodeExists = possibleNode;
						break;
					}
					
					//Continue iterating
					photoIndex++;
				} while(possibleNode != null);
				
				if(nodeExists == null)
				{
					// If the file wasn't found by fingerprint nor in the destination folder,
					// take a look in the folder from v1
					SharedPreferences prefs = this.getSharedPreferences("prefs_main.xml", 0);
					if(prefs != null)
					{ 
						String handle = prefs.getString("camera_sync_folder_hash", null);
						if(handle != null)
						{
							MegaNode prevFolder = megaApi.getNodeByHandle(MegaApiAndroid.base64ToHandle(handle));
							if(prevFolder != null)
							{
								// If we reach this code, the app is an updated v1 and the previously selected
								// folder still exists
	
								// If the file matches name, mtime and size, and doesn't have a fingerprint, 
								// => we consider that it's the correct one
								possibleNode = megaApi.getChildNode(prevFolder, sourceFile.getName());
								if(possibleNode != null &&
										sourceFile.length() == possibleNode.getSize() &&
										megaApi.getFingerprint(possibleNode) == null)
								{
									nodeExists = possibleNode;
								}
							}
						}
					}
				}
			}
			
			if (nodeExists == null){
				log("SUBIR EL FICHERO: " + media.filePath);
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(media.timestamp);
				log("YYYY-MM-DD HH.MM.SS -- " + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR_OF_DAY) + "." + cal.get(Calendar.MINUTE) + "." + cal.get(Calendar.SECOND));
				boolean photoAlreadyExists = false;
				ArrayList<MegaNode> nL = megaApi.getChildren(cameraUploadNode, MegaApiJava.ORDER_ALPHABETICAL_ASC);
				for (int i=0;i<nL.size();i++){
					if ((nL.get(i).getName().compareTo(Util.getPhotoSyncName(media.timestamp, media.filePath)) == 0) && (nL.get(i).getSize() == file.length())){
						photoAlreadyExists = true;
					}
				}
				
				if (!photoAlreadyExists){
					
					if(Boolean.parseBoolean(prefs.getKeepFileNames())){
						//Keep the file names as device
						megaApi.startUpload(file.getAbsolutePath(), secondaryUploadNode, file.getName(), this);
						log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
					}
					else{
						int photoIndex = 0;
						String photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
						for (int i=0;i<nL.size();i++){
							photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
							log(photoFinalName + "_S_S_S_S_S_S____" + nL.get(i).getName());
							if (nL.get(i).getName().compareTo(photoFinalName) == 0){
								photoIndex++;
							}
						}
						photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
						log("photoFinalName: " + photoFinalName + "______" + photoIndex);
						currentTimeStamp = media.timestamp;
						
						megaApi.startUpload(file.getAbsolutePath(), cameraUploadNode, photoFinalName, this);
					}		
				}
				else{
					currentTimeStamp = media.timestamp;
					dbH.setCamSyncTimeStamp(currentTimeStamp);
					File f = new File(media.filePath);
					totalSizeUploaded += f.length();
					uploadNext();
				}
			}
			else{
				log("NODO: " + megaApi.getParentNode(nodeExists).getName() + "___" + nodeExists.getName());				
				if (megaApi.getParentNode(nodeExists).getHandle() != cameraUploadHandle){
					
					if(Boolean.parseBoolean(prefs.getKeepFileNames())){
						//Keep the file names as device
						megaApi.copyNode(nodeExists, cameraUploadNode, file.getName(), this);
						log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
					}
					else{
						int photoIndex = 0;
						String photoFinalName = null;
						do {
							photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
							photoIndex++;
						}while(megaApi.getChildNode(cameraUploadNode, photoFinalName) != null);
						
						log("photoFinalName: " + photoFinalName + "______" + photoIndex);
						currentTimeStamp = media.timestamp;
						megaApi.copyNode(nodeExists, cameraUploadNode, photoFinalName, this);
					}		
					
//				if (megaApi.getNodeByPath("/" + CAMERA_UPLOADS + "/" + nodeExists.getName()) == null){
					
//					boolean photoAlreadyExists = false;
//					ArrayList<MegaNode> nL = megaApi.getChildren(cameraUploadNode);
//					for (int i=0;i<nL.size();i++){
//						if (nL.get(i).getName().compareTo(Util.getPhotoSyncName(media.timestamp, media.filePath)) == 0){
//							photoAlreadyExists = true;
//						}
//					}
//					
//					if (!photoAlreadyExists){
//						currentTimeStamp = media.timestamp;
//						megaApi.copyNode(nodeExists, cameraUploadNode, this);
//					}
//					else{
//						currentTimeStamp = media.timestamp;
//						dbH.setCamSyncTimeStamp(currentTimeStamp);
//						File f = new File(media.filePath);
//						totalSizeUploaded += f.length();
//						uploadNextImage();	
//					}
				}
				else{
//					if (nodeExists.getName().compareTo(Util.getPhotoSyncName(media.timestamp, media.filePath)) == 0){
//						currentTimeStamp = media.timestamp;
//						dbH.setCamSyncTimeStamp(currentTimeStamp);
//						File f = new File(media.filePath);
//						totalSizeUploaded += f.length();
//						uploadNextImage();	
//					}
//					else{
//						log("The file is in PhotoSync but with a different name");
//						megaApi.renameNode(nodeExists, Util.getPhotoSyncName(media.timestamp, media.filePath), this);
//					}
					
					final MegaNode existingNode = nodeExists;
					handler.post(new Runnable() {
						@Override
						public void run() {
							new LookForRenameTask(media,cameraUploadNode).rename(existingNode); 
						}
					});
				}
			}
		}
		else{
//			if (mediaFilesSecondary.size() <= 0){
//				log("Finishing service: no more elements queuing");
//				onQueueComplete(true, totalUploaded);
//				finish();
//			}		
			uploadNext();
		}
	}
	
	private class LookForRenameTask{

		Media media;
		String photoFinalName;
		MegaNode uploadNode;
		
		public LookForRenameTask(Media media, MegaNode uploadNode) {
			this.media = media;
			this.uploadNode = uploadNode;
		}
		
		protected Boolean rename(MegaNode nodeExists) {

			File file = new File(media.filePath);
			log("RENOMBRAR EL FICHERO: " + media.filePath);
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(media.timestamp);
			log("YYYY-MM-DD HH.MM.SS -- " + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR_OF_DAY) + "." + cal.get(Calendar.MINUTE) + "." + cal.get(Calendar.SECOND));
			boolean photoAlreadyExists = false;
			ArrayList<MegaNode> nL = megaApi.getChildren(uploadNode, MegaApiJava.ORDER_ALPHABETICAL_ASC);
			for (int i=0;i<nL.size();i++){
				if ((nL.get(i).getName().compareTo(Util.getPhotoSyncName(media.timestamp, media.filePath)) == 0) && (nL.get(i).getSize() == file.length())){
					photoAlreadyExists = true;
				}
			}
			
			if (!photoAlreadyExists){
				int photoIndex = 0;
				this.photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
				for (int i=0;i<nL.size();i++){
					this.photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
					log(photoFinalName + "_S_S_S_S_S_S____" + nL.get(i).getName());
					if ((nL.get(i).getName().compareTo(photoFinalName) == 0) && (photoFinalName.compareTo(nodeExists.getName()) != 0)){
						photoIndex++;
					}
				}
				photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
				log("photoFinalName: " + photoFinalName + "______" + photoIndex);
				currentTimeStamp = media.timestamp;
				
				megaApi.renameNode(nodeExists, photoFinalName, cameraSyncService);
				
				return true;
			}
			else{
				currentTimeStamp = media.timestamp;
//				long parentHandle = megaApi.getParentNode(uploadNode).getHandle();
				if(uploadNode.getHandle() == secondaryUploadHandle){
					log("renameTask: Update SECONDARY Sync TimeStamp, parentHandle= "+uploadNode.getHandle()+" secondaryHandle: "+secondaryUploadHandle);					
					dbH.setSecSyncTimeStamp(currentTimeStamp);
				}
				else{
					log("renameTask: Update Camera Sync TimeStamp, parentHandle= "+uploadNode.getHandle()+" cameraHandle: "+cameraUploadHandle);				
					dbH.setCamSyncTimeStamp(currentTimeStamp);
				}
				
				log("Upoad NODE: "+uploadNode.getName());

				File f = new File(media.filePath);
				totalSizeUploaded += f.length();
				
				uploadNext();
				return false;	
			}
		}
	}
	
	void registerObservers(){
		log("registerObservers");
 		mediaObserver = new MediaObserver(handler, this);
 		try {
	 		ContentResolver cr = getContentResolver();
			cr.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false,  mediaObserver);		
			cr.registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, false,  mediaObserver);
			cr.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false,  mediaObserver);
			cr.registerContentObserver(MediaStore.Video.Media.INTERNAL_CONTENT_URI, false,  mediaObserver);
			cr.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false,  mediaObserver);
			cr.registerContentObserver(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, false,  mediaObserver);
 		}
 		catch (Exception e){}
 	}
	
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
		
		return false;
	}
	
	private void reset() {
		log("---====  RESET  ====---");
		totalUploaded = -1;
		totalSizeToUpload = 0;
		totalToUpload = 0;
		totalSizeUploaded = 0;
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
		}, 10 * 1000);
		
		//cAMBIAR 5 * 60 * 1000
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
				totalUploaded,
				getResources().getQuantityString(R.plurals.general_num_files,
						totalUploaded), getString(R.string.upload_uploaded),
				Formatter.formatFileSize(CameraSyncService.this, totalSizeUploaded));
		String title = getString(R.string.settings_camera_notif_complete);
		Intent intent = new Intent(CameraSyncService.this, ManagerActivity.class);
		intent.putExtra(ManagerActivity.EXTRA_OPEN_FOLDER, cameraUploadHandle);

		mBuilderCompat
			.setSmallIcon(R.drawable.ic_stat_camera_sync)
			.setContentIntent(PendingIntent.getActivity(CameraSyncService.this, 0, intent, 0))
			.setAutoCancel(true).setTicker(title).setContentTitle(title)
			.setContentText(message)
			.setOngoing(false);
	
		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}

	private void finish(){
		log("finish CameraSyncService");
		
		if(running){
			handler.removeCallbacksAndMessages(null);
			running = false;
		}
		cancel();
	}
	
	public void onDestroy(){
		log("onDestroy");
		running = false;
		
		if (!stopped){
			retryLater();
		}
		
		if(mediaObserver != null){
 	 		ContentResolver cr = getContentResolver();
 			cr.unregisterContentObserver(mediaObserver);
 			mediaObserver = null;
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
					
					totalSizeUploaded += megaApi.getNodeByHandle(request.getNodeHandle()).getSize();
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
		}
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
				
				String tempPath = transfer.getPath();
				if(tempPath.startsWith(localPath)){
					log("onTransferFinish: Update Camera Sync TimeStamp");
					dbH.setCamSyncTimeStamp(currentTimeStamp);
				}
				else{
					log("onTransferFinish: Update SECONDARY Sync TimeStamp");
					dbH.setSecSyncTimeStamp(currentTimeStamp);
				}
				
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
				log("Image Sync FAIL: " + transfer.getFileName());
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
			CameraSyncService.this.cancel();
			return;
		}
		
		final long bytes = transfer.getTransferredBytes();
//		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
		updateProgressNotification(totalSizeUploaded + bytes);
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
		int progressPercent = (int) Math.round((double) progress / totalSizeToUpload
				* 100);
		log(progressPercent + " " + progress + " " + totalSizeToUpload);
		int left = totalToUpload - totalUploaded;
		int current = totalToUpload - left;
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
	
	private void onQueueComplete(boolean success, int totalUploaded) {
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
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
		if (totalUploaded == 0) {
			log("TotalUploaded == 0");
		} else {
			log("stopping service!");
			if (success){
				if (totalSizeUploaded != 0){
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
