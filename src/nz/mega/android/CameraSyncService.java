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


public class CameraSyncService extends Service implements MegaRequestListenerInterface, MegaTransferListenerInterface, MegaGlobalListenerInterface{

	public static String PHOTO_SYNC = "PhotoSync";
	public static String CAMERA_UPLOADS = "Camera Uploads";
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
	
	long cameraUploadHandle = -1;
	
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
	
	String localPath = "";
	
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
	
	MegaNode cameraUploadNode = null;
	
	Queue<Media> cameraFiles = new LinkedList<Media>();
	
	long currentTimeStamp = 0;
	
	boolean waitForOnNodesUpdate = false;
	
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
		waitForOnNodesUpdate = false;
		
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
					if (prefs.getCamSyncWifi() == null || Boolean.parseBoolean(prefs.getCamSyncWifi())){
						if (!isWifi){
							log("no wifi...");
							finish();
							return START_REDELIVER_INTENT;
						}
					}
					
					//The "Camera Upload" folder exists?
					if (prefs.getCamSyncHandle() == null){
						cameraUploadHandle = -1;
					}
					else{
						cameraUploadHandle = Long.parseLong(prefs.getCamSyncHandle());
						if (megaApi.getNodeByHandle(cameraUploadHandle) == null){
							cameraUploadHandle = -1;
						}
						else{
							if (megaApi.getParentNode(megaApi.getNodeByHandle(cameraUploadHandle)).getHandle() != megaApi.getRootNode().getHandle()){
								cameraUploadHandle = -1;
							}
						}
					}
				}
			}
		}
		
		if (cameraUploadHandle == -1){
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
			if (PHOTO_SYNC.compareTo(n.getName()) == 0){
				megaApi.renameNode(n, CAMERA_UPLOADS, this);	
			}
			else if (CAMERA_UPLOADS.compareTo(n.getName()) != 0){
				dbH.setCamSyncHandle(-1);
				if (!running){
					running = true;
					megaApi.createFolder(CAMERA_UPLOADS, megaApi.getRootNode(), this);
				}
				return START_NOT_STICKY;
			}
		}
		
		log("TODO OK");
		log ("photosynchandle = " + cameraUploadHandle);
		
		return 0;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");
		
		if (megaApi == null){
			finish();
			return START_NOT_STICKY;
		}
		
		if (app.getLocalIpAddress() == null){
			app.setLocalIpAddress(Util.getLocalIpAddress());
		}
		else{
			if (app.getLocalIpAddress().compareTo("") == 0){
				app.setLocalIpAddress(Util.getLocalIpAddress());
			}
			else{
				if (app.getLocalIpAddress().compareTo(Util.getLocalIpAddress()) != 0){
					app.setLocalIpAddress(Util.getLocalIpAddress());
					
					if (megaApi.getRootNode() != null){
						log("reconnect");
						megaApi.reconnect();
					}
				}
			}
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
				MediaColumns.DATE_ADDED};
		
		String selection = null;
		String[] selectionArgs = null;
		
		if (prefs != null){
			if (prefs.getCamSyncTimeStamp() != null){
				long camSyncTimeStamp = Long.parseLong(prefs.getCamSyncTimeStamp());
				selection = "(" + MediaColumns.DATE_ADDED + "*1000) > " + camSyncTimeStamp;
				log("SELECTION: " + selection);
			}
		}
		
		String order = MediaColumns.DATE_ADDED + " ASC";
		
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
			
			Cursor cursor = app.getContentResolver().query(uris.get(i), projection, selection, selectionArgs, order);
			if (cursor != null){
				int dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
		        int timestampColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED);
		        while(cursor.moveToNext()){
		        	Media media = new Media();
			        media.filePath = cursor.getString(dataColumn);
			        media.timestamp = cursor.getLong(timestampColumn) * 1000;
			        
			        if (!checkFile(media)){
			        	continue;	
			        }
			        
			        cameraFiles.add(media);
		        }	
			}
		}
		
		cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
		if(cameraUploadNode == null){
			showSyncError(R.string.settings_camera_notif_error_no_folder);
			finish();
			return;
		}
		
		totalToUpload = cameraFiles.size();
		
		totalSizeToUpload = 0;
		Iterator<Media> itCF = cameraFiles.iterator();
		while (itCF.hasNext()){
			Media m = itCF.next();
			File f = new File(m.filePath);
			totalSizeToUpload = totalSizeToUpload + f.length();
		}		
				
		uploadNextImage();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void uploadNextImage(){
		log("uploadNextImage");
		totalUploaded++;
		
		int result = shouldRun();
		if (result != 0){
			retryLater();
			return;
		}
		
		if (cameraFiles.size() > 0){
			Media media = cameraFiles.poll();
			
			File file = new File(media.filePath);
			if(!file.exists()){
				uploadNextImage();
			}
			
			String localFingerPrint = megaApi.getFingerprint(media.filePath);
			
			MegaNode nodeExists = megaApi.getNodeByFingerprint(localFingerPrint);
			
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
				else{
					currentTimeStamp = media.timestamp;
					dbH.setCamSyncTimeStamp(currentTimeStamp);
					File f = new File(media.filePath);
					totalSizeUploaded += f.length();
					uploadNextImage();	
				}
			}
			else{
				log("NODO: " + megaApi.getParentNode(nodeExists).getName() + "___" + nodeExists.getName());				
				if (megaApi.getParentNode(nodeExists).getHandle() != cameraUploadHandle){
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
					else{
						currentTimeStamp = media.timestamp;
						dbH.setCamSyncTimeStamp(currentTimeStamp);
						File f = new File(media.filePath);
						totalSizeUploaded += f.length();
						uploadNextImage();	
					}
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
					
					new LookForRenameTask(media).execute(nodeExists); 
				}
			}
		}
		else{
			onQueueComplete(true, totalUploaded);
			finish();
		}
	}
	
	private class LookForRenameTask extends AsyncTask<MegaNode, Void, Boolean> {

		Media media;
		MegaNode nodeExists;
		String photoFinalName;
		
		public LookForRenameTask(Media media) {
			this.media = media;
		}
		
		@Override
		protected Boolean doInBackground(MegaNode... args) {
			this.nodeExists = args[0];
			File file = new File(media.filePath);
			log("RENOMBRAR EL FICHERO: " + media.filePath);
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
				
				return true;
			}
			else{
				return false;	
			}
		}
		
		@Override
		protected void onPostExecute(Boolean shouldRename) {
			if(shouldRename){
				megaApi.renameNode(nodeExists, photoFinalName, cameraSyncService);				
			}
			else{
				currentTimeStamp = media.timestamp;
				dbH.setCamSyncTimeStamp(currentTimeStamp);
				File f = new File(media.filePath);
				totalSizeUploaded += f.length();
				uploadNextImage();
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
		}, 5 * 60 * 1000);
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
		
		megaApi.removeGlobalListener(this);
		
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
				log("Folder created");
				retryLaterShortTime();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			if (e.getErrorCode() == MegaError.API_OK){
				log("Node copied: " + megaApi.getNodePath(megaApi.getNodeByHandle(request.getNodeHandle())));
				megaApi.renameNode(megaApi.getNodeByHandle(request.getNodeHandle()), Util.getPhotoSyncName(currentTimeStamp, megaApi.getNodeByHandle(request.getNodeHandle()).getName()), this);
			}
			else{
				log("Error copying");
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			if (e.getErrorCode() == MegaError.API_OK){
				
				if (megaApi.getNodeByHandle(request.getNodeHandle()).getName().compareTo(CAMERA_UPLOADS) == 0){
					log("Folder renamed to CAMERA_UPLOADS");
					retryLaterShortTime();
					finish();
				}
				else{
					dbH.setCamSyncTimeStamp(currentTimeStamp);
					totalSizeUploaded += megaApi.getNodeByHandle(request.getNodeHandle()).getSize();
					uploadNextImage();
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
				dbH.setCamSyncTimeStamp(currentTimeStamp);
				ArrayList<MegaNode> nLAfter = megaApi.getChildren(megaApi.getNodeByHandle(cameraUploadHandle), MegaApiJava.ORDER_ALPHABETICAL_ASC);
				log("SIZEEEEEE: " + nLAfter.size());
				waitForOnNodesUpdate = true;
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
		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
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
			log("stopping service!2");
		} else {
			log("stopping service!");
			if (success){
				if (totalSizeUploaded != 0){
					showCompleteSuccessNotification();
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
		if (waitForOnNodesUpdate){
			waitForOnNodesUpdate = false;
			uploadNextImage();
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}
}
