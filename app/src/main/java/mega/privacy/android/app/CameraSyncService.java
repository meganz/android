package mega.privacy.android.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.provider.DocumentFile;
import android.text.format.Formatter;
import android.text.format.Time;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;


public class CameraSyncService extends Service implements MegaRequestListenerInterface, MegaTransferListenerInterface, MegaGlobalListenerInterface, MegaChatRequestListenerInterface {

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
	MegaChatApiAndroid megaChatApi;
	MegaApplication app;
	DatabaseHandler dbH;
	ChatSettings chatSettings;

	static public boolean running = false;
	private boolean canceled;

	private boolean isOverquota = false;

	private Handler handler;

//	private CameraObserver cameraObserver;

	private int totalUploaded;
	private long totalSizeToUpload;
	private int totalToUpload;
	private long totalSizeUploaded;
	private int successCount;

	Object transferFinished = new Object();

	private boolean isForeground;

	private int notificationId = Constants.NOTIFICATION_CAMERA_UPLOADS;
	private int notificationIdFinal = Constants.NOTIFICATION_CAMERA_UPLOADS_FINAL;
	private String notificationChannelId = Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID;
	private String notificationChannelName = Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME;

	private static long lastRun = 0;

	Queue<Media> cameraFiles = new LinkedList<Media>();
	ArrayList<DocumentFile> cameraFilesExternalSDCardList = new ArrayList<DocumentFile>();
	Queue<DocumentFile> cameraFilesExternalSDCardQueue;
	String localPath = "";
	MegaNode cameraUploadNode = null;
	long cameraUploadHandle = -1;
	boolean isExternalSDCard = false;

	Intent intentCreate = null;

	MegaPreferences prefs;

	boolean newFileList = false;
	boolean stopped = false;

	MediaObserver mediaObserver;

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		if (request.getType() == MegaChatRequest.TYPE_CONNECT){

			isLoggingIn = false;
			MegaApplication.setLoggingIn(isLoggingIn);

			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Connected to chat!");
			}
			else{
				log("EEEERRRRROR WHEN CONNECTING " + e.getErrorString());
//				showSnackbar(getString(R.string.chat_connection_error));
				retryLaterShortTime();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

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

	boolean isLoggingIn = false;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("public void onCreate()");
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
		isOverquota = false;
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
		log("private int shouldRun()");

		if (!Util.isOnline(this)){
			log("Not online");
			finish();
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
						else{
							log("Localpath: " + localPath);
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

					if (prefs.getCameraFolderExternalSDCard() != null){
						isExternalSDCard = Boolean.parseBoolean(prefs.getCameraFolderExternalSDCard());
					}

					UserCredentials credentials = dbH.getCredentials();

					if (credentials == null){
						log("There are not user credentials");
						finish();
						return START_NOT_STICKY;
					}

					String gSession = credentials.getSession();

					if (megaApi.getRootNode() == null){
						log("RootNode = null");
						running = true;

						isLoggingIn = MegaApplication.isLoggingIn();
						if (!isLoggingIn){

							isLoggingIn  = true;
							MegaApplication.setLoggingIn(isLoggingIn);

							if(Util.isChatEnabled()){
								log("shouldRun: Chat is ENABLED");
								if (megaChatApi == null){
									megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
								}

								int ret = megaChatApi.getInitState();

								if(ret==MegaChatApi.INIT_NOT_DONE||ret==MegaChatApi.INIT_ERROR){
									ret = megaChatApi.init(gSession);
									log("shouldRun: result of init ---> "+ret);
									chatSettings = dbH.getChatSettings();
									if (ret == MegaChatApi.INIT_NO_CACHE)
									{
										log("shouldRun: condition ret == MegaChatApi.INIT_NO_CACHE");
										megaChatApi.enableGroupChatCalls(true);

									}
									else if (ret == MegaChatApi.INIT_ERROR)
									{
										log("shouldRun: condition ret == MegaChatApi.INIT_ERROR");
										if(chatSettings==null) {
											log("1 - shouldRun: ERROR----> Switch OFF chat");
											chatSettings = new ChatSettings();
											chatSettings.setEnabled(false+"");
											dbH.setChatSettings(chatSettings);
										}
										else{
											log("2 - shouldRun: ERROR----> Switch OFF chat");
											dbH.setEnabledChat(false + "");
										}
										megaChatApi.logout(this);
									}
									else{
										log("shouldRun: Chat correctly initialized");
										megaChatApi.enableGroupChatCalls(true);
									}
								}
							}

							megaApi.fastLogin(gSession, this);
						}
						else{
							log("Another login is processing");
						}
						return START_NOT_STICKY;
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
						log("if (prefs.getCamSyncHandle() == null)");
						cameraUploadHandle = -1;
					}
					else{
						log("if (prefs.getCamSyncHandle() != null)");
						cameraUploadHandle = Long.parseLong(prefs.getCamSyncHandle());
					}

					//Check the secondary folder
					if (prefs.getSecondaryMediaFolderEnabled() == null){
						log("if (prefs.getSecondaryMediaFolderEnabled() == null)");
						dbH.setSecondaryUploadEnabled(false);
						log("Not defined, so not enabled");
						secondaryEnabled=false;
					}
					else{
						log("if (prefs.getSecondaryMediaFolderEnabled() != null)");
						if (!Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled())){
							log("Not enabled Secondary");
							secondaryEnabled=false;
						}
						else{
							secondaryEnabled=true;
							localPathSecondary = prefs.getLocalPathSecondaryFolder();

							/*


							//Check the corresponding folders

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
//									secondaryEnabled=false;
								}
								else
								{
									if ("-1".compareTo(localPathSecondary) == 0){
										log("-1 secondary LOCAL, so not enabled");
										dbH.setSecondaryUploadEnabled(false);
//										secondaryEnabled=false;
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
							}*/
						}
					}
				}
			}
		}

		ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
		if(secondaryEnabled){
			log("the secondary uploads are enabled");
			String temp = prefs.getMegaHandleSecondaryFolder();
			if (temp != null){
				if (temp.compareTo("") != 0){
					secondaryUploadHandle= Long.parseLong(prefs.getMegaHandleSecondaryFolder());
					if (secondaryUploadHandle == -1){
						for (int i=0;i<nl.size();i++){
							if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
								secondaryUploadHandle = nl.get(i).getHandle();
								dbH.setSecondaryFolderHandle(secondaryUploadHandle);
							}
						}

						//If not "Media Uploads"
						if (secondaryUploadHandle == -1){
							log("must create the folder");
							if (!running){
								running = true;
								megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
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
						log("SecondaryUploadHandle: "+secondaryUploadHandle);
						MegaNode n = megaApi.getNodeByHandle(secondaryUploadHandle);
						//If ERROR with the handler (the node may not longer exist): Create the folder MEdia Uploads
						if(n==null){
							secondaryUploadHandle=-1;
							log("The secondary media folder may not longer exists!!!");
							for (int i=0;i<nl.size();i++){
								if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
									secondaryUploadHandle = nl.get(i).getHandle();
									dbH.setSecondaryFolderHandle(secondaryUploadHandle);
								}
							}

							//If not "Media Uploads"
							if (secondaryUploadHandle == -1){
								log("must create the folder");
								if (!running){
									running = true;
									megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
								}
								else{
									log("cancel transfers");
									if (megaApi != null){
										megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
										return START_NOT_STICKY;
									}
								}
								return START_NOT_STICKY;
							}
						}
						else{
							log("Secondary Folder " + secondaryUploadHandle + " Node: "+n.getName());
							secondaryUploadNode=megaApi.getNodeByHandle(secondaryUploadHandle);
						}
					}
				}
				else{
					//If empty string as SecondaryHandle
					secondaryUploadHandle=-1;
					for (int i=0;i<nl.size();i++){
						if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
							secondaryUploadHandle = nl.get(i).getHandle();
							dbH.setSecondaryFolderHandle(secondaryUploadHandle);
						}
					}

					//If not "Media Uploads"
					if (secondaryUploadHandle == -1){
						log("must create the folder");
						if (!running){
							running = true;
							megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
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
			}
			else{
				for (int i=0;i<nl.size();i++){
					if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
						secondaryUploadHandle = nl.get(i).getHandle();
						dbH.setSecondaryFolderHandle(secondaryUploadHandle);
					}
				}

				//If not "Media Uploads"
				if (secondaryUploadHandle == -1){
					log("must create the folder");
					if (!running){
						running = true;
						megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
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
		}
		else{
			log("Secondary NOT Enabled");
		}

		if (cameraUploadHandle == -1){
			log("Find the Camera Uploads folder of the old PhotoSync");
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

			log("If not Camera Uploads nor Photosync");
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
				log("Node with cameraUploadHandle is not NULL");
				cameraUploadHandle = -1;
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
					log("If not Camera Uploads nor Photosync--- must create the folder");
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

		log("shouldRun: OK");

		return 0;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("public int onStartCommand(Intent intent, int flags, int startId)");

		if (megaApi == null){
			finish();
			return START_NOT_STICKY;
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
						dbH.setSecondaryUploadEnabled(false);
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
						dbH.setSecondaryUploadEnabled(false);
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
					try{
						initSync();
					}
					catch (SecurityException ex){
						retryLater();
						finish();
					}
				}
			};
			task.start();
			log("STARTS NOW");

		}
		else{
			log("RUNNING ALREADY SO RETRY LATER");
			retryLater();
		}

		return START_NOT_STICKY;
	}

	void initSync() throws SecurityException{
		log("initSync");

		if(!wl.isHeld()){
			wl.acquire();
		}
		if(!lock.isHeld()){
			lock.acquire();
		}

		registerObservers();

		if (!isExternalSDCard){
			log("if (!isExternalSDCard)");
			String projection[] = {	MediaColumns.DATA,
					//MediaColumns.MIME_TYPE,
					MediaColumns.DATE_ADDED,
					MediaColumns.DATE_MODIFIED};

			String selectionCamera = null;
			String selectionSecondary = null;
			String[] selectionArgs = null;

			prefs = dbH.getPreferences();

			if (prefs != null){
				log("if (prefs != null)");
				if (prefs.getCamSyncTimeStamp() != null){
					log("if (prefs.getCamSyncTimeStamp() != null)");
					long camSyncTimeStamp = Long.parseLong(prefs.getCamSyncTimeStamp());
					selectionCamera = "(" + MediaColumns.DATE_MODIFIED + "*1000) > " + camSyncTimeStamp + " OR " + "(" + MediaColumns.DATE_ADDED + "*1000) > " + camSyncTimeStamp;
					log("SELECTION: " + selectionCamera);
				}
				if(secondaryEnabled){
					log("if(secondaryEnabled)");
					if (prefs.getSecSyncTimeStamp() != null){
						log("if (prefs.getSecSyncTimeStamp() != null)");
						long secondaryTimeStamp = Long.parseLong(prefs.getSecSyncTimeStamp());
						selectionSecondary = "(" + MediaColumns.DATE_MODIFIED + "*1000) > " + secondaryTimeStamp + " OR " + "(" + MediaColumns.DATE_ADDED + "*1000) > " + secondaryTimeStamp;
						log("SELECTION SECONDARY: " + selectionSecondary);
					}
				}
			}

			String order = MediaColumns.DATE_MODIFIED + " ASC";

			ArrayList<Uri> uris = new ArrayList<Uri>();
			if (prefs.getCamSyncFileUpload() == null){
				log("if (prefs.getCamSyncFileUpload() == null)");
				dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
				uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			}
			else{
				log("if (prefs.getCamSyncFileUpload() != null)");
				switch(Integer.parseInt(prefs.getCamSyncFileUpload())){
					case MegaPreferences.ONLY_PHOTOS:{
						log("case MegaPreferences.ONLY_PHOTOS:");
						uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
						break;
					}
					case MegaPreferences.ONLY_VIDEOS:{
						log("case MegaPreferences.ONLY_VIDEOS:");
						uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
						uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
						break;
					}
					case MegaPreferences.PHOTOS_AND_VIDEOS:{
						log("case MegaPreferences.PHOTOS_AND_VIDEOS:");
						uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
						uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
						uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
						break;
					}
				}
			}

			for(int i=0; i<uris.size(); i++){
				log("for(int i=0; i<uris.size(); i++)");
				Cursor cursorCamera = app.getContentResolver().query(uris.get(i), projection, selectionCamera, selectionArgs, order);
				if (cursorCamera != null){
					log("if (cursorCamera != null)");
					int dataColumn = cursorCamera.getColumnIndexOrThrow(MediaColumns.DATA);
					int timestampColumn = 0;
					if(cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)==0){
						log("if(cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED) == 0)");
						timestampColumn = cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED);
					}
					else
					{
						log("if(cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED) != 0)");
						timestampColumn = cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_ADDED);
					}

					while(cursorCamera.moveToNext()){

						Media media = new Media();
						media.filePath = cursorCamera.getString(dataColumn);
						//			        log("Tipo de fichero:--------------------------: "+media.filePath);
						media.timestamp = cursorCamera.getLong(timestampColumn) * 1000;

						log("while(cursorCamera.moveToNext()) - media.filePath: " + media.filePath + "_localPath: " + localPath);

						//Check files of the Camera Uploads
						if (checkFile(media,localPath)){
							log("if (checkFile(media," + localPath + "))");
							cameraFiles.add(media);
							log("Camera Files added: "+media.filePath);
						}
					}
				}

				//Secondary Media Folder
				if(secondaryEnabled){
					log("if(secondaryEnabled)");
					Cursor cursorSecondary = app.getContentResolver().query(uris.get(i), projection, selectionSecondary, selectionArgs, order);
					if (cursorSecondary != null){
						try {
							log("SecondaryEnabled en initsync COUNT: "+cursorSecondary.getCount());
							int dataColumn = cursorSecondary.getColumnIndexOrThrow(MediaColumns.DATA);
							int timestampColumn = 0;
							if(cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)==0){
								timestampColumn = cursorSecondary.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED);
							}
							else
							{
								timestampColumn = cursorSecondary.getColumnIndexOrThrow(MediaColumns.DATE_ADDED);
							}
							while(cursorSecondary.moveToNext()){

								Media media = new Media();
								media.filePath = cursorSecondary.getString(dataColumn);
								media.timestamp = cursorSecondary.getLong(timestampColumn) * 1000;
								log("Check: " + media.filePath + " in localPath: " + localPathSecondary);
								//Check files of Secondary Media Folder
								if (checkFile(media, localPathSecondary)) {
									mediaFilesSecondary.add(media);
									log("-----SECONDARY MEDIA Files added: " + media.filePath + " in localPath: " + localPathSecondary);
								}
							}
						}
						catch (Exception e){
							log("Exception cursorSecondary:" + e.getMessage() + "____" + e.getStackTrace());
						}
					}
				}
			}

			cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
			if(cameraUploadNode == null){
				log("ERROR: cameraUploadNode == null");
				//			showSyncError(R.string.settings_camera_notif_error_no_folder);
				retryLater();
				finish();
				return;
			}

			//		if(secondaryEnabled){
			//			if(secondaryUploadHandle==-1){
			//				ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
			//				boolean found = false;
			//				for (int i=0;i<nl.size();i++){
			//					if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
			//						secondaryUploadHandle = nl.get(i).getHandle();
			//						dbH.setSecondaryFolderHandle(secondaryUploadHandle);
			//						found = true;
			//						break;
			//					}
			//				}
			//				if(!found){
			//					//Create folder
			//					secondaryEnabled = false;
			//					dbH.setSecondaryUploadEnabled(false);
			//				}
			//			}
			//			else{
			//				secondaryUploadNode = megaApi.getNodeByHandle(secondaryUploadHandle);
			//			}
			//		}



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
		else{
			log("isExternalSDCard");
			DocumentFile pickedDir = null;
			if (prefs != null){
				if (prefs.getUriExternalSDCard() != null){
					String uriString = prefs.getUriExternalSDCard();
					Uri uri = Uri.parse(uriString);
					pickedDir = DocumentFile.fromTreeUri(getApplicationContext(), uri);
					log("PICKEDDIR: " + pickedDir.getName());
					DocumentFile[] files = pickedDir.listFiles();
					if(files!=null){
						log("The number of files is: "+files.length);
					}
					else{
						log("files is NULL!");
					}
					ArrayList<DocumentFile> auxCameraFilesExternalSDCard = new ArrayList<DocumentFile>();
					for (int i=0;i<files.length;i++){
						log("Name to check: "+ files[i].getName());
						switch(Integer.parseInt(prefs.getCamSyncFileUpload())){
							case MegaPreferences.ONLY_PHOTOS:{
								String fileType = files[i].getType();
								if (fileType != null){
									if (fileType.startsWith("image/")){
										auxCameraFilesExternalSDCard.add(files[i]);
									}
									else{
										log("No image");
									}
								}
								else{
									log("File is null");
								}
								break;
							}
							case MegaPreferences.ONLY_VIDEOS:{
								String fileType = files[i].getType();
								String fileName = files[i].getName();
								if (fileType != null){
									if (fileName != null){
										if (fileType.startsWith("video/") || (fileName.endsWith(".mkv"))) {
											auxCameraFilesExternalSDCard.add(files[i]);
										}
									}
								}
								break;
							}
							case MegaPreferences.PHOTOS_AND_VIDEOS:{
								String fileType = files[i].getType();
								String fileName = files[i].getName();
								if (fileType != null){
									if (fileName != null) {
										if (fileType.startsWith("image/") || fileType.startsWith("video/") || (fileName.endsWith(".mkv"))) {
											auxCameraFilesExternalSDCard.add(files[i]);
										}
									}
								}
								break;
							}
						}
					}

					log("auxCameraFilesExternalSDCard.size() = " + auxCameraFilesExternalSDCard.size());
					int j=0;
					for (int i=0;i<auxCameraFilesExternalSDCard.size();i++){
						if (cameraUploadNode == null){
							log("Camera Upload Node null");
							cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
						}
						if (cameraUploadNode != null){
							log("Camera Upload Node not null");
							if (megaApi.getChildNode(cameraUploadNode, auxCameraFilesExternalSDCard.get(i).getName()) == null){
								cameraFilesExternalSDCardList.add(j, auxCameraFilesExternalSDCard.get(i));
								log("FILE ADDED: " + auxCameraFilesExternalSDCard.get(i).getName());
								j++;
							}
						}
						else{
							log("Camera Upload null");
						}
					}


//					Collections.sort(auxCameraFilesExternalSDCard, new MediaComparator());

//					for (int i=0;i<auxCameraFilesExternalSDCard.size();i++){
//						long camSyncTimeStamp = Long.parseLong(prefs.getCamSyncTimeStamp());
//						log("CAMSYNCTIMESTAMP: " + camSyncTimeStamp + "___" + auxCameraFilesExternalSDCard.get(i).lastModified());
//						if (auxCameraFilesExternalSDCard.get(i).lastModified() > camSyncTimeStamp){
//							int j = 0;
//							for ( ; j<cameraFilesExternalSDCardList.size(); j++){
//								if (auxCameraFilesExternalSDCard.get(i).lastModified() < cameraFilesExternalSDCardList.get(j).lastModified()){
//									break;
//								}
//							}
//							cameraFilesExternalSDCardList.add(j, auxCameraFilesExternalSDCard.get(i));
//						}
////						if (cameraFilesExternalSDCardList.size() == 25){
////							break;
////						}
//						log("NAME: " + auxCameraFilesExternalSDCard.get(i).getName() + "_LAST_ " + auxCameraFilesExternalSDCard.get(i).lastModified());
//					}

					for (int i=0;i<cameraFilesExternalSDCardList.size();i++){
						log("ORD_NAME: " + cameraFilesExternalSDCardList.get(i).getName() + "____" + cameraFilesExternalSDCardList.get(i).lastModified());
					}

					cameraFilesExternalSDCardQueue = new LinkedList<DocumentFile>(cameraFilesExternalSDCardList);

					totalToUpload = cameraFilesExternalSDCardQueue.size();
					totalUploaded=0;
					totalSizeUploaded=0;

					totalSizeToUpload = 0;
					Iterator<DocumentFile> itCF = cameraFilesExternalSDCardQueue.iterator();
					while (itCF.hasNext()){
						DocumentFile dF = itCF.next();
						totalSizeToUpload = totalSizeToUpload + dF.length();
					}
					uploadNextSDCard();
				}
				else{
					finish();
				}
			}
			else{
				finish();
			}
			//TODO: The secondary media folder has to be here implemented also (or separate in two pieces - isExternal !isExternal)
		}
	}

	private class MediaComparator implements Comparator<DocumentFile>{

		@Override
		public int compare(DocumentFile d1, DocumentFile d2) {
			if (d1.lastModified() > d2.lastModified()){
				return 1;
			}
			else{
				return -1;
			}
		}

	}

	@SuppressLint("NewApi")
	public void uploadNextSDCard(){

		totalUploaded++;

		isExternalSDCard = true;
		int result = shouldRun();
		if (result != 0){
			retryLater();
			return;
		}

		if (cameraUploadNode == null){
			cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
		}

		if (cameraFilesExternalSDCardQueue == null){
			retryLater();
			return;
		}
		if (cameraFilesExternalSDCardQueue.size() > 0){
			DocumentFile dF = cameraFilesExternalSDCardQueue.poll();

			File[] fs = getExternalFilesDirs(null);
			if (fs.length > 1){
				if (fs[1] == null){
					finish();
				}
				else{
					fs[1].mkdirs();
					if (copyFileSDCard(dF, fs[1])){
						File file = new File(fs[1], dF.getName());
						if(!file.exists()){
							uploadNextSDCard();
						}

						//					String localFingerPrint = megaApi.getFingerprint(file.getAbsolutePath());
						//
						//					MegaNode nodeExists = null;
						//					nodeExists = megaApi.getNodeByFingerprint(localFingerPrint, cameraUploadNode);

						final Media media = new Media();
						media.timestamp = dF.lastModified();
						media.filePath = file.getAbsolutePath();

						MegaNode possibleNode = megaApi.getChildNode(cameraUploadNode, dF.getName());
						if (possibleNode != null){
							dbH.setCamSyncTimeStamp(media.timestamp);
							file.delete();
							uploadNextSDCard();
						}
						else{
							currentTimeStamp = media.timestamp;
							megaApi.startUpload(file.getAbsolutePath(), cameraUploadNode, media.timestamp/1000, this);
						}
					}
				}
			}
			else{
				finish();
			}
		}
		else{
			finish();
		}

	}

	public void uploadNext(){
		log("public void uploadNext()");
		if (cameraUploadNode == null){
			log("if (cameraUploadNode == null)");
			cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
		}

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&   UPLOAD NEXT");
		if (cameraFiles.size() > 0){
			log("if (cameraFiles.size() > 0)");
			uploadNextImage();
		}
		else if(mediaFilesSecondary.size() > 0){
			log("else if(mediaFilesSecondary.size() > 0)");
			uploadNextMediaFile();
		}
		else
		{
			log("else");
			onQueueComplete(true, totalUploaded);
			finish();
		}
	}

	void uploadNextMediaFile(){

		//Check problem with secondary

		totalUploaded++;
		log("------------------------------------------------uploadNextMediaFile: "+totalUploaded +" of "+totalToUpload);

		int result = shouldRun();
		if (result != 0){
			retryLater();
			return;
		}

		if (cameraUploadNode == null){
			cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
		}

		if (mediaFilesSecondary.size() > 0){
			final Media mediaSecondary = mediaFilesSecondary.poll();

			File file = new File(mediaSecondary.filePath);
			if(!file.exists()){
				uploadNext();
			}

			log("mediaSecondary.filePath: "+mediaSecondary.filePath);
			String localFingerPrint = megaApi.getFingerprint(mediaSecondary.filePath);

			MegaNode nodeExists = null;
			//Source file
			File sourceFile = new File(mediaSecondary.filePath);

			nodeExists = megaApi.getNodeByFingerprint(localFingerPrint, secondaryUploadNode);
//			if(nodeExists == null){
//				log("Soy null");
//			}
//			else{
//				log("No soy null: "+nodeExists.getName());
//				if(nodeExists.getHandle()!=-1){
//					log("Y el handle es: "+nodeExists.getHandle());
//					MegaNode prueba = megaApi.getNodeByHandle(nodeExists.getHandle());
//					if(prueba!=null){
//						log("Lo encuentro por handle: "+prueba.getName());
//					}
//					else{
//						log("Por handle no lo encuentro");
//					}
//				}
//			}


			if(nodeExists == null)
			{
				log("nodeExists1==null");
				//Check if the file is already uploaded in the correct folder but without a fingerprint
				int photoIndex = 0;
				MegaNode possibleNode = null;
				String photoFinalName;
				do {
					//Create the final name taking into account the
					if(Boolean.parseBoolean(prefs.getKeepFileNames())){
						//Keep the file names as device

						photoFinalName = mediaSecondary.filePath;
						log("Keep the secondary name: "+photoFinalName);
					}
					else{
						photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
						log("CHANGE the secondary name: "+photoFinalName);
					}

					//Iterate between all files with the correct target name

					possibleNode = megaApi.getChildNode(secondaryUploadNode, photoFinalName);
					// If the file matches name, mtime and size, and doesn't have a fingerprint,
					// => we consider that it's the correct one
					if(possibleNode != null &&
							sourceFile.length() == possibleNode.getSize() &&
							megaApi.getFingerprint(possibleNode) == null)
					{
						log("nodeExists = possibleNode: "+possibleNode);
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
						currentTimeStamp = mediaSecondary.timestamp;
						megaApi.startUpload(file.getAbsolutePath(), secondaryUploadNode, file.getName(), this);
						log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
					}
					else{
						int photoIndex = 0;
						String photoFinalName = null;
						do {
							photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
							photoIndex++;
						}while(megaApi.getChildNode(secondaryUploadNode, photoFinalName) != null);

						log("photoFinalName: " + photoFinalName + "______" + photoIndex);
						currentTimeStamp = mediaSecondary.timestamp;

						megaApi.startUpload(file.getAbsolutePath(), secondaryUploadNode, photoFinalName, this);
						log("CHANGED!!!! MediaFinalName: " + photoFinalName + "______" + photoIndex);

//						int photoIndex = 0;
//						String photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
//						for (int i=0;i<nL.size();i++){
//							photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
//							if (nL.get(i).getName().compareTo(photoFinalName) == 0){
//								photoIndex++;
//							}
//						}
//						photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
//						currentTimeStamp = mediaSecondary.timestamp;
////						log("Voy a subir: "+file.getAbsolutePath());
////						log(" secondaryNode: "+secondaryUploadNode.getName());
////						log(" photoName: "+photoFinalName);
//
//						megaApi.startUpload(file.getAbsolutePath(), secondaryUploadNode, photoFinalName, this);
//						log("CHANGED!!!! MediaFinalName: " + photoFinalName + "______" + photoIndex);
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
				log("nodeExists=!null");

//				if(megaApi.getParentNode(nodeExists)!=null){
//					log("NODO SECONDARY: getParent!=null");
//
//					if(megaApi.getParentNode(nodeExists).getName()!=null){
//						log("NODO SECONDARY: " + megaApi.getParentNode(nodeExists).getName());
//
//						if(nodeExists.getName()!=null){
//							log("NODO SECONDARY: " + megaApi.getParentNode(nodeExists).getName() + "___" + nodeExists.getName());
//						}
//
//					}
//					else{
//						log("NULLL");
//					}
//
//				}
//				else{
//					log("NODO SECONDARY: getParent==null");
//					log("nodeExists: "+nodeExists.getName());
//					String path = megaApi.getNodePath(nodeExists);
//					if(path!=null){
//						log("el path: "+path);
//					}
//					else{
//						log("Path NULL");
//					}
//
//				}

				if(megaApi.getParentNode(nodeExists)!=null){
					if (megaApi.getParentNode(nodeExists).getHandle() != secondaryUploadHandle){

						if(Boolean.parseBoolean(prefs.getKeepFileNames())){
							//Keep the file names as device
							currentTimeStamp = mediaSecondary.timestamp;
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

						if(!(Boolean.parseBoolean(prefs.getKeepFileNames()))){
							//Change the file names as device
							log("Call Look for Rename Task");
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
					//What if the parent node is null
					log("This is an error!!!");
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

		if (cameraUploadNode == null){
			log("if (cameraUploadNode == null)");
			cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
		}

		if (cameraFiles.size() > 0){
			log("if (cameraFiles.size() > 0)");
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
				log("if(nodeExists == null)");
				//Check if the file is already uploaded in the correct folder but without a fingerprint
				int photoIndex = 0;
				MegaNode possibleNode = null;
				String photoFinalName;
				do {
					//Iterate between all files with the correct target name

					//Create the final name taking into account the
					if(Boolean.parseBoolean(prefs.getKeepFileNames())){
						//Keep the file names as device

						photoFinalName = media.filePath;
						log("Keep the camera file name: "+photoFinalName);
					}
					else{
						photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
						log("CHANGE the camera file name: "+photoFinalName);
					}

					possibleNode = megaApi.getChildNode(cameraUploadNode, photoFinalName);

					// If the file matches name, mtime and size, and doesn't have a fingerprint,
					// => we consider that it's the correct one
					if(possibleNode != null &&
							sourceFile.length() == possibleNode.getSize() &&
							megaApi.getFingerprint(possibleNode) == null)
					{
						nodeExists = possibleNode;
						log("nodeExists = possibleNode;");
						break;
					}

					//Continue iterating
					photoIndex++;
				} while(possibleNode != null);

				if(nodeExists == null)
				{
					log("if(nodeExists == null)");
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
				log("UPLOAD THE FILE: " + media.filePath);
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
					log("if (!photoAlreadyExists)");

					if(Boolean.parseBoolean(prefs.getKeepFileNames())){
						//Keep the file names as device
						currentTimeStamp = media.timestamp;
						megaApi.startUpload(file.getAbsolutePath(), cameraUploadNode, file.getName(), this);
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

						megaApi.startUpload(file.getAbsolutePath(), cameraUploadNode, photoFinalName, this);
						log("CHANGEEEEEEE: filePath: "+file.getAbsolutePath()+" Change finalName: "+photoFinalName);

//						int photoIndex = 0;
//						String photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
//						for (int i=0;i<nL.size();i++){
//							photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
//							log(photoFinalName + "_S_S_S_S_S_S____" + nL.get(i).getName());
//							if (nL.get(i).getName().compareTo(photoFinalName) == 0){
//								photoIndex++;
//							}
//						}
//						photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
//						log("photoFinalName: " + photoFinalName + "______" + photoIndex);
//						currentTimeStamp = media.timestamp;
//
//						megaApi.startUpload(file.getAbsolutePath(), cameraUploadNode, photoFinalName, this);
//						log("CHANGEEEEEEE: filePath: "+file.getAbsolutePath()+" Change finalName: "+photoFinalName);
					}
				}
				else{
					log("if (photoAlreadyExists)");
					currentTimeStamp = media.timestamp;
					dbH.setCamSyncTimeStamp(currentTimeStamp);
					File f = new File(media.filePath);
					totalSizeUploaded += f.length();
					uploadNext();
				}
			}
			else{
				log("NODE EXISTS: " + megaApi.getParentNode(nodeExists).getName() + "___" + nodeExists.getName());
				if (megaApi.getParentNode(nodeExists).getHandle() != cameraUploadHandle){

					if(Boolean.parseBoolean(prefs.getKeepFileNames())){
						//Keep the file names as device
						currentTimeStamp = media.timestamp;
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
						log("CHANGED!!!! MediaFinalName: " + file.getName());
					}
				}
				else{
					if(!(Boolean.parseBoolean(prefs.getKeepFileNames()))){
						//Change the file names as device
						log("Call Look for Rename Task");
						final MegaNode existingNode = nodeExists;
						handler.post(new Runnable() {
							@Override
							public void run() {
								new LookForRenameTask(media,cameraUploadNode).rename(existingNode);
							}
						});
					}
					else{
						currentTimeStamp = media.timestamp;
						dbH.setCamSyncTimeStamp(currentTimeStamp);
						uploadNext();
					}

				}
			}
		}
		else{
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
				this.photoFinalName = null;
				do {
					photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
					photoIndex++;
				}while(megaApi.getChildNode(uploadNode, photoFinalName) != null);

				log("photoFinalName: " + photoFinalName + "______" + photoIndex);
				currentTimeStamp = media.timestamp;

				megaApi.renameNode(nodeExists, photoFinalName, cameraSyncService);
				log("RENAMED!!!! MediaFinalName: " + photoFinalName + "______" + photoIndex);

//				int photoIndex = 0;
//				this.photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
//				for (int i=0;i<nL.size();i++){
//					this.photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
//					log(photoFinalName + "_S_S_S_S_S_S____" + nL.get(i).getName());
//					if ((nL.get(i).getName().compareTo(photoFinalName) == 0) && (photoFinalName.compareTo(nodeExists.getName()) != 0)){
//						photoIndex++;
//					}
//				}
//				photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
//				log("photoFinalName: " + photoFinalName + "______" + photoIndex);
//				currentTimeStamp = media.timestamp;
//
//				megaApi.renameNode(nodeExists, photoFinalName, cameraSyncService);

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
		log("void registerObservers()");
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

	private void reset() {
		log("---====  RESET  ====---");
		totalUploaded = -1;
		totalSizeToUpload = 0;
		totalToUpload = 0;
		totalSizeUploaded = 0;
		successCount = 0;
	}

	private void cancel() {
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

		if(isOverquota){
			showStorageOverquotaNotification();
		}

		canceled = true;
		isForeground = false;
		running = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
	}

	private void showStorageOverquotaNotification(){
		log("showStorageOverquotaNotification");

		String contentText = getString(R.string.download_show_info);
		String message = getString(R.string.overquota_alert_title);

		Intent intent = new Intent(this, ManagerActivityLollipop.class);
		intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setShowBadge(true);
			channel.setSound(null, null);
			mNotificationManager.createNotificationChannel(channel);

			NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

			mBuilderCompatO
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompatO.build());
		}
		else {
			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompat.build());
		}
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
		Intent intent = null;
		intent = new Intent(CameraSyncService.this, ManagerActivityLollipop.class);
		intent.putExtra(Constants.EXTRA_OPEN_FOLDER, cameraUploadHandle);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setShowBadge(true);
			channel.setSound(null, null);
			mNotificationManager.createNotificationChannel(channel);

			NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

			mBuilderCompatO
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setContentIntent(PendingIntent.getActivity(CameraSyncService.this, 0, intent, 0))
					.setAutoCancel(true).setTicker(title).setContentTitle(title)
					.setContentText(message)
					.setOngoing(false);

			mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
		}
		else {
			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setContentIntent(PendingIntent.getActivity(CameraSyncService.this, 0, intent, 0))
					.setAutoCancel(true).setTicker(title).setContentTitle(title)
					.setContentText(message)
					.setOngoing(false);

			mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
		}
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

        if (megaChatApi != null){
            megaChatApi.saveCurrentState();
        }

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

//	private void showSyncError(final int errResId) {
//		handler.post(new Runnable() 
//		{
//			public void run()
//			{
//				log("show sync error");
//				Intent intent = null;
//				
//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {	
//					intent = new Intent(CameraSyncService.this, ManagerActivityLollipop.class);
//				}
//				else{
//					intent = new Intent(CameraSyncService.this, ManagerActivity.class);
//				}
//						
//				mBuilderCompat
//					.setSmallIcon(R.drawable.ic_stat_camera_sync)
//					.setContentIntent(PendingIntent.getActivity(CameraSyncService.this, 0, intent, 0))
//					.setContentTitle(getString(R.string.settings_camera_notif_error))
//					.setContentText(getString(errResId))
//					.setOngoing(false);
//		
////				if (!isForeground) {
////					log("starting foreground!");
////					startForeground(notificationId, mBuilderCompat.build());
////					isForeground = true;
////				} else {
////					mNotificationManager.notify(notificationId, mBuilderCompat.build());
////				}
//			}}
//		);
//	}

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
				log("Calling fetchNodes from CameraSyncService");
				megaApi.fetchNodes(this);
			}
			else{
				log("ERROR: " + e.getErrorString());
				isLoggingIn = false;
				MegaApplication.setLoggingIn(isLoggingIn);
				finish();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			if (e.getErrorCode() == MegaError.API_OK){
				chatSettings = dbH.getChatSettings();
				if(chatSettings!=null) {
					boolean chatEnabled = Boolean.parseBoolean(chatSettings.getEnabled());
					if(chatEnabled){
						log("Chat enabled-->connect");
						megaChatApi.connectInBackground(this);
						isLoggingIn = false;
						MegaApplication.setLoggingIn(isLoggingIn);
						retryLaterShortTime();
					}
					else{
						log("Chat NOT enabled - readyToManager");
						isLoggingIn = false;
						MegaApplication.setLoggingIn(isLoggingIn);
						retryLaterShortTime();
					}
				}
				else{
					log("chatSettings NULL - readyToManager");
					isLoggingIn = false;
					MegaApplication.setLoggingIn(isLoggingIn);
					retryLaterShortTime();
				}
			}
			else{
				log("ERROR: " + e.getErrorString());
				isLoggingIn = false;
				MegaApplication.setLoggingIn(isLoggingIn);
				finish();
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
					if(megaApi.getParentNode(node)!=null){
						long parentHandle = megaApi.getParentNode(node).getHandle();
						if(parentHandle == secondaryUploadHandle){
							log("Update SECONDARY Sync TimeStamp");
							dbH.setSecSyncTimeStamp(currentTimeStamp);
						}
						else{
							log("Update Camera Sync TimeStamp");
							dbH.setCamSyncTimeStamp(currentTimeStamp);
						}
					}
					else
					{
						retryLaterShortTime();
					}

					totalSizeUploaded += megaApi.getNodeByHandle(request.getNodeHandle()).getSize();
					if (cameraFilesExternalSDCardQueue != null){
						if (cameraFilesExternalSDCardQueue.size() > 0){
							uploadNextSDCard();
						}
						else{
							uploadNext();
						}
					}
					else{
						uploadNext();
					}
				}
			}
			else{
				log("Error ("+e.getErrorCode()+"): "+request.getType()+" : "+request.getRequestString());
				if(request.getNodeHandle()!=-1){
					MegaNode nodeError = megaApi.getNodeByHandle(request.getNodeHandle());
					if(nodeError!=null){
						log("Node: "+nodeError.getName());
					}
				}

				if (e.getErrorCode() == MegaError.API_EOVERQUOTA)
					isOverquota = true;

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
		log("transfer.getNodeHandle:" + transfer.getNodeHandle());

		if (canceled) {
			log("Image sync cancelled: " + transfer.getFileName());
			if((lock != null) && (lock.isHeld()))
				try{ lock.release(); } catch(Exception ex) {}
			if((wl != null) && (wl.isHeld()))
				try{ wl.release(); } catch(Exception ex) {}

			if (isExternalSDCard){
				File fileToDelete = new File(transfer.getPath());
				if (fileToDelete != null){
					if (fileToDelete.exists()){
						fileToDelete.delete();
					}
				}
			}

			CameraSyncService.this.cancel();
		}
		else{
			if (e.getErrorCode() == MegaError.API_OK) {

				if(isOverquota){
					log("After overquota error");
					isOverquota = false;
				}

				log("Image Sync OK: " + transfer.getFileName());
				totalSizeUploaded += transfer.getTransferredBytes();
				log("IMAGESYNCFILE: " + transfer.getPath());

				String tempPath = transfer.getPath();
				if (!isExternalSDCard){
					if(tempPath.startsWith(localPath)){
						log("onTransferFinish: Update Camera Sync TimeStamp");
						dbH.setCamSyncTimeStamp(currentTimeStamp);
					}
					else{
						log("onTransferFinish: Update SECONDARY Sync TimeStamp");
						dbH.setSecSyncTimeStamp(currentTimeStamp);
					}
				}

				if (isExternalSDCard){
					dbH.setCamSyncTimeStamp(currentTimeStamp);
				}

				if(Util.isVideoFile(transfer.getPath())){
					log("Is video!!!");
					File previewDir = PreviewUtils.getPreviewFolder(this);
					File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
					File thumbDir = ThumbnailUtils.getThumbFolder(this);
					File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
					megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
					megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

					MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
					if(node!=null){
						MediaMetadataRetriever retriever = new MediaMetadataRetriever();
						retriever.setDataSource(transfer.getPath());

						String location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
						if(location!=null){
							log("Location: "+location);

							boolean secondTry = false;
							try{
								final int mid = location.length() / 2; //get the middle of the String
								String[] parts = {location.substring(0, mid),location.substring(mid)};

								Double lat = Double.parseDouble(parts[0]);
								Double lon = Double.parseDouble(parts[1]);
								log("Lat: "+lat); //first part
								log("Long: "+lon); //second part

								megaApi.setNodeCoordinates(node, lat, lon, null);
							}
							catch (Exception exc){
								secondTry = true;
								log("Exception, second try to set GPS coordinates");
							}

							if(secondTry){
								try{
									String latString = location.substring(0,7);
									String lonString = location.substring(8,17);

									Double lat = Double.parseDouble(latString);
									Double lon = Double.parseDouble(lonString);
									log("Lat2: "+lat); //first part
									log("Long2: "+lon); //second part

									megaApi.setNodeCoordinates(node, lat, lon, null);
								}
								catch (Exception ex){
									log("Exception again, no chance to set coordinates of video");
								}
							}
						}
						else{
							log("No location info");
						}
					}
				}
				else if (MimeTypeList.typeForName(transfer.getPath()).isImage()){
					log("Is image!!!");

					File previewDir = PreviewUtils.getPreviewFolder(this);
					File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
					File thumbDir = ThumbnailUtils.getThumbFolder(this);
					File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
					megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
					megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

					MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
					if(node!=null){
						try {
							final ExifInterface exifInterface = new ExifInterface(transfer.getPath());
							float[] latLong = new float[2];
							if (exifInterface.getLatLong(latLong)) {
								log("Latitude: "+latLong[0]+" Longitude: " +latLong[1]);
								megaApi.setNodeCoordinates(node, latLong[0], latLong[1], null);
							}

						} catch (Exception exception) {
							log("Couldn't read exif info: " + transfer.getPath());
						}
					}
				}
				else{
					log("NOT video or image!");
				}

				if (isExternalSDCard){
					File fileToDelete = new File(transfer.getPath());
					if (fileToDelete != null){
						if (fileToDelete.exists()){
							fileToDelete.delete();
						}
					}
				}

//				dbH.setCamSyncTimeStamp(currentTimeStamp);

//				ArrayList<MegaNode> nLAfter = megaApi.getChildren(megaApi.getNodeByHandle(cameraUploadHandle), MegaApiJava.ORDER_ALPHABETICAL_ASC);
//				log("SIZEEEEEE: " + nLAfter.size());

				if (cameraFilesExternalSDCardQueue != null){
					if (cameraFilesExternalSDCardQueue.size() > 0){
						uploadNextSDCard();
					}
					else{
						uploadNext();
					}
				}
				else{
					uploadNext();
				}
			}
			else{
				log("Image Sync FAIL: " + transfer.getFileName() + "___" + e.getErrorString());

				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					log("OVERQUOTA ERROR: "+e.getErrorCode());
					isOverquota = true;
				}

				CameraSyncService.this.cancel();
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

		if(isOverquota){
			log("After overquota error");
			isOverquota = false;
		}

		final long bytes = transfer.getTransferredBytes();
//		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
		updateProgressNotification(totalSizeUploaded + bytes);
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log("onTransferTemporaryError: " + transfer.getFileName());

		if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
			if (e.getValue() != 0)
				log("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
			else
				log("STORAGE OVERQUOTA ERROR: " + e.getErrorCode());

			isOverquota = true;

			updateProgressNotification(totalSizeToUpload);
		}
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

		String status = isOverquota ? getString(R.string.overquota_alert_title) :
				getString(R.string.settings_camera_notif_title);

		Intent intent = null;

		intent = new Intent(CameraSyncService.this, ManagerActivityLollipop.class);
		intent.setAction(isOverquota ? Constants.ACTION_OVERQUOTA_STORAGE :
				Constants.ACTION_CANCEL_CAM_SYNC);

		String info = Util.getProgressSize(CameraSyncService.this, progress, totalSizeToUpload);

		PendingIntent pendingIntent = PendingIntent.getActivity(CameraSyncService.this, 0, intent, 0);
		Notification notification = null;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setShowBadge(true);
			channel.setSound(null, null);
			mNotificationManager.createNotificationChannel(channel);

			NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setProgress(100, progressPercent, false)
					.setContentIntent(pendingIntent)
					.setOngoing(true)
					.setContentTitle(message)
					.setSubText(info)
					.setContentText(status)
					.setOnlyAlertOnce(true);

			notification = mBuilderCompat.build();
		}
		else if (currentapiVersion >= android.os.Build.VERSION_CODES.N) {
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setProgress(100, progressPercent, false)
					.setContentIntent(pendingIntent)
					.setOngoing(true)
					.setContentTitle(message)
					.setSubText(info)
					.setContentText(status)
					.setOnlyAlertOnce(true);
			notification = mBuilder.getNotification();
		}
		else if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_camera_sync)
					.setProgress(100, progressPercent, false)
					.setContentIntent(pendingIntent)
					.setOngoing(true)
					.setContentTitle(message)
					.setContentInfo(info)
					.setContentText(status)
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
			log("starting foreground");
			try {
				startForeground(notificationId, notification);
			}
			catch(Exception e){
				log("startforeground exception: " + e.getMessage());
				retryLaterShortTime();
				return;
			}
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
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		log("onUserAlertsUpdate");
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		log("onNodesUpdate");

	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api,
										ArrayList<MegaContactRequest> requests) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	public boolean copyFileSDCard(final DocumentFile source, final File dir) {

		InputStream input;
		try {
			input = getContentResolver().openInputStream(source.getUri());

			File target = new File(dir, source.getName());
			OutputStream outStream = new FileOutputStream(target);
			byte [] buffer = new byte[4096];
			int bytesRead = 0;
			while((bytesRead = input.read(buffer)) != -1) {
				outStream.write(buffer, 0, bytesRead);
			}
			input.close();
			outStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}
}
