package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.widget.RemoteViews;

/*
 * Service to Upload files
 */
public class UploadService extends Service {

	public static String ACTION_CANCEL = "CANCEL_UPLOAD";
	
	public static String EXTRA_FILEPATH = "MEGA_FILE_PATH";
	public static String EXTRA_FOLDERPATH = "MEGA_FOLDER_PATH";
	public static String EXTRA_NAME = "MEGA_FILE_NAME";
	public static String EXTRA_SIZE = "MEGA_SIZE";
	public static String EXTRA_PARENT_HASH = "MEGA_PARENT_HASH";
	
	
	private LinkedList<Intent> intentQueue;
	static public boolean isProcessingIntent = false;
	private int totalCount = 0;
	private int successCount = 0;
	private long totalSize = 0;
	private long uploadedSize = 0;
	private int lastError = 0;
	private boolean isForeground = false;
	private boolean canceled;
	
	private HashMap<String, Long> createdFolders;
	MegaApplication app;
	MegaApiAndroid megaApi;
	
	Handler guiHandler;
	
	WifiLock lock;
	WakeLock wl;
	
	private Notification.Builder mBuilder;
	private NotificationCompat.Builder mBuilderCompat;
	private NotificationManager mNotificationManager;
	
	Object syncObject = new Object();
	
	MegaRequestListenerInterface megaRequestListener;
	MegaTransferListenerInterface megaTransferListener;
	
	UploadFolderTask uploadFolderTask;
	UploadFileTask uploadFileTask;
	
	private int notificationId = 1;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		log("onCreate");
		intentQueue = new LinkedList<Intent>();
		isProcessingIntent = false;
		totalCount = 0;
		successCount = 0;
		totalSize = 0;
		uploadedSize = 0;
		lastError = 0;
		isForeground = false;
		canceled = false;

		createdFolders = new HashMap<String, Long>();
		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		guiHandler = new Handler();
		
		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }
        
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaUploadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaUploadServicePowerLock");
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) 
			mBuilder = new Notification.Builder(UploadService.this);
		mBuilderCompat = new NotificationCompat.Builder(UploadService.this);
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	public void onDestroy(){				
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");
		
		if(intent == null){
			return START_NOT_STICKY;
		}
		
		if ((intent.getAction() != null) && intent.getAction().equals(ACTION_CANCEL)) {
			cancel();
			return START_NOT_STICKY;
		}
		
		
		//This thread reads the content of the intent (in case it is a directory, reads every file a
		new Thread() {
			Intent intent;
			Thread setIntent(Intent intent)
			{
				this.intent = intent;
				return this;
			}
			
			public void run(){
				File file = new File(intent.getStringExtra(EXTRA_FILEPATH));
				try {
					file = file.getCanonicalFile();
				} 
				catch (Exception e) {}
				
				synchronized(syncObject){
					if (file.isDirectory()) {
						addFolder(intent.getLongExtra(EXTRA_PARENT_HASH, 0), intent.getStringExtra(EXTRA_FOLDERPATH), file);
					} else {
						intentQueue.add(intent);
						totalCount++;
						totalSize += intent.getLongExtra(EXTRA_SIZE, 0);
					}
				}
				
				guiHandler.post(new Runnable() {
					@Override
					public void run() {
						if (isProcessingIntent) {
							return;
						}
						isProcessingIntent = true;
						processQueue();
					}
				});
				updateProgressNotification(uploadedSize);
			}
		}.setIntent(intent).start();
		
		return START_REDELIVER_INTENT;
	}
	
	/*
	 * Start next item uploading if necessary
	 */
	private void processQueue() {
		guiHandler.post(new Runnable() {
			@Override
			public void run() {
				log("processQueue");
				Intent intent = null;
				try{
					intent = intentQueue.pollFirst();
				}
				catch (Exception e){
				}
				
				if (intent == null) {
					onQueueComplete();
				} else {
					onHandleIntent(intent, 0);
				}
			}
		});
	}
	
	protected void onHandleIntent(final Intent intent, final int tryCount) {
		log("onHandleIntent");

		updateProgressNotification(uploadedSize);

		final File file = new File(intent.getStringExtra(EXTRA_FILEPATH));
		File parent;
		if (intent.hasExtra(EXTRA_FOLDERPATH)) {
			parent = new File(intent.getStringExtra(EXTRA_FOLDERPATH));
		} 
		else {
			parent = file.getParentFile();
		}
		
		if (file.isDirectory()) {
			uploadFolderTask = new UploadFolderTask(intent, file, parent, tryCount);
			uploadFolderTask.start();
		} else {
			uploadFileTask = new UploadFileTask(intent, file, parent, tryCount);
			uploadFileTask.start();
		}
	}
	
	private void addFolder(long parentHandle, String basePath, File folder) {
		try {
			folder = folder.getCanonicalFile();
		} 
		catch (Exception e) {}
		
		if (!folder.canRead()) {
			log("folder cant read!");
			return;
		}
		ArrayList<File> folders = new ArrayList<File>();
		
		addIntent(parentHandle, basePath, folder);

		File[] folderFiles = folder.listFiles();
		if(folderFiles == null){
			return;
		}
		for (File file : folderFiles) {
			if (!file.canRead()) {
				continue;
			}
			if (file.isDirectory()) {
				folders.add(file);
			} else {
				addIntent(parentHandle, basePath, file);
			}
		}
		folderFiles = null;
		for(int i=0; i<folders.size(); i++){
			addFolder(parentHandle, basePath, folders.get(i));
		}
	}
	
	private void addIntent(long parentHandle, String basePath, File file){
		try {
			file = file.getCanonicalFile();
		} 
		catch (IOException e) {}
		
		log("AddIntent: " + basePath + "   File: " + file.getAbsolutePath());

		Intent serviceIntent = new Intent(this, UploadService.class);
		if (file.isDirectory()) {
			serviceIntent.putExtra(UploadService.EXTRA_FILEPATH,file.getAbsolutePath());
			serviceIntent.putExtra(UploadService.EXTRA_NAME,file.getName());
		} else {
			ShareInfo info = ShareInfo.infoFromFile(file);
			if (info == null) {
				log("NULL INFO");
				return;
			}
			serviceIntent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
			serviceIntent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
			serviceIntent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
		}
		
		serviceIntent.putExtra(UploadService.EXTRA_FOLDERPATH, basePath);
		serviceIntent.putExtra(UploadService.EXTRA_PARENT_HASH, parentHandle);
		
		intentQueue.add(serviceIntent);
		totalCount++;
		totalSize += serviceIntent.getLongExtra(EXTRA_SIZE, 0);
	}
	
	/*
	 * Stop uploading service
	 */
	private void cancel() {
		canceled = true;
		guiHandler.removeCallbacksAndMessages(null);
		guiHandler.post(new Runnable()
		{
			public void run()
			{	
				log("cancel!");
				intentQueue = new LinkedList<Intent>();
				stopForeground(true);
				isForeground = false;
				isProcessingIntent = false;
				stopSelf();
			}
		});
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/*
	 * No more intents in the queue
	 */
	private void onQueueComplete() {
		log("onQueueComplete");
		stopForeground(true);
		isForeground = false;
		log("Stopping foreground!");
		log("stopping service!1");
		if (successCount == 0) {
			log("stopping service!2");
			showCompleteFailNotification();
		} else {
			log("stopping service!3");
			showCompleteSuccessNotification();
		}
		log("stopping service!");
		isProcessingIntent = false;
		stopSelf();
	}
	
	/*
	 * Show complete error notification
	 */
	private void showCompleteFailNotification() {
		guiHandler.post(new Runnable()
		{
			public void run()
			{	
				log("showCompleteFailNotification");
				String title = getString(R.string.upload_failed);
				String message = getString(R.string.error_server_connection_problem);
				if(lastError != 0) message = MegaError.getErrorString(lastError);
				
				Intent intent = new Intent(UploadService.this, ManagerActivity.class);
				
				mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify_upload)
					.setContentIntent(PendingIntent.getActivity(UploadService.this, 0, intent, 0))
					.setAutoCancel(true).setContentTitle(title)
					.setContentText(message)
					.setOngoing(false);

				mNotificationManager.notify(notificationId, mBuilderCompat.build());
			}
		});
	}
	
	/*
	 * Show complete success notification
	 */
	private void showCompleteSuccessNotification() {
		guiHandler.post(new Runnable()
		{
			public void run()
			{	
				log("showCompleteSuccessNotification");
				String notificationTitle, size;
		
				notificationTitle = successCount
						+ " "
						+ getResources().getQuantityString(R.plurals.general_num_files,
								successCount) + " " + getString(R.string.upload_uploaded);
				size = getString(R.string.general_total_size) + " "
						+ Formatter.formatFileSize(UploadService.this, totalSize);
		
				Intent intent = new Intent(UploadService.this, ManagerActivity.class);
				
				mBuilderCompat
						.setSmallIcon(R.drawable.ic_stat_notify_upload)
						.setContentIntent(PendingIntent.getActivity(UploadService.this, 0, intent, 0))
						.setAutoCancel(true).setTicker(notificationTitle)
						.setContentTitle(notificationTitle).setContentText(size)
						.setOngoing(false);
		
				mNotificationManager.notify(notificationId, mBuilderCompat.build());
			}
		});
	}
	
	/*
	 * Upload folder
	 */
	private class UploadFolderTask extends Thread
	{
		Intent intent;
		File folder;
		File parent;
		int tryCount;
		
		UploadFolderTask(Intent intent, File folder, File parent, int tryCount)	{
			this.intent = intent;
			this.folder = folder;
			this.parent = parent;
			this.tryCount = tryCount;
		}
		
		@Override
		public void run() {
			final long parentHash = intent.getLongExtra(EXTRA_PARENT_HASH, 0);
			final long uploadParentHash = getUploadParentHash(parentHash, folder, parent);
			final File relParent = getRelativeParent(parent, folder);
			if (uploadParentHash == 0) {
				processQueue();
				return;
			}

			log(folder.getAbsolutePath());

			MegaNode document = megaApi.getNodeByHandle(uploadParentHash);
			if(document != null){
				NodeList nodeList = megaApi.getChildren(document);
				for(int i=0; i<nodeList.size(); i++) {
					MegaNode node = nodeList.get(i);
					if((folder.getName().equals(node.getName())) && (node.getType() == MegaNode.TYPE_FOLDER)){
						log("putting hash " + getFolderKey(parentHash, new File(relParent, folder.getName())));
						createdFolders.put(getFolderKey(parentHash, new File(relParent, folder.getName())), node.getHandle());
						successCount++;						
						processQueue();
						return;
					}
				}
			}
			
			if(document == null) {
				lastError = MegaError.API_EACCESS;
				processQueue();
				return;
			}
			
			megaRequestListener = new MegaRequestListenerInterface() {
				
				@Override
				public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
						MegaError e) {			
				}
				
				@Override
				public void onRequestStart(MegaApiJava api, MegaRequest request) {			
				}
				
				@Override
				public void onRequestFinish(MegaApiJava api, MegaRequest request,
						MegaError e) {
					if (canceled) {
						guiHandler.removeCallbacksAndMessages(null);
						guiHandler.post(new Runnable() {
							public void run() {
								UploadService.this.cancel();
							}});
						return;
					}
					
					if (e.getErrorCode() != MegaError.API_OK) {
						log("onFailure " + tryCount);
						if (tryCount > 2) {
							processQueue();
						} else {
							lastError = e.getErrorCode();
							handleIntentDelayed(intent, tryCount + 1);
						}
					} else {
						log("putting hash " + getFolderKey(parentHash, new File(relParent, folder.getName())));
						createdFolders.put(getFolderKey(parentHash, new File(relParent, folder.getName())), 
								request.getNodeHandle());
						successCount++;
						processQueue();
					}
					
				}
			};
			
			log("NEW FOLDER: " + folder.getName() + "  Parent: " + document.getName());
			
			megaApi.createFolder(folder.getName(), document, megaRequestListener);
			
			return;
		}
	}
	
	/*
	 * Process upload intent from the queue
	 */
	protected void handleIntentDelayed(final Intent intent, final int tryCount) {
		if (canceled) {
			return;
		}
		guiHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (canceled) {
					return;
				}
				onHandleIntent(intent, tryCount);
			}
		}, tryCount * 3 * 1000);
	}
	
	/*
	 * Upload file
	 */
	class UploadFileTask extends Thread
	{
		Intent intent;
		File file;
		File parent;
		int tryCount;
		
		UploadFileTask(final Intent intent, final File file, final File parent, final int tryCount){
			this.intent = intent;
			this.file = file;
			this.parent = parent;
			this.tryCount = tryCount;
		}

		@Override
		public void run(){
			final ShareInfo info = new ShareInfo();
			info.size = intent.getLongExtra(EXTRA_SIZE, 0);
			info.title = intent.getStringExtra(EXTRA_NAME);

			final long parentHash = intent.getLongExtra(EXTRA_PARENT_HASH, 0);
			long uploadParentHash = getUploadParentHash(parentHash, file, parent);
			if (uploadParentHash == 0) {
				Util.deleteIfLocal(UploadService.this, file);
				processQueue();
				log(parentHash + " " + file.getAbsolutePath() + " "
						+ parent.getAbsolutePath());
				log("file upload prent hash = null");
				return;
			}
			
			MegaNode document = megaApi.getNodeByHandle(uploadParentHash);
			if(document != null) {
				document = document.copy();
				NodeList nodeList = megaApi.getChildren(document);
				for(int i=0; i<nodeList.size(); i++) {
					MegaNode node = nodeList.get(i);
					if(info.getTitle() == null){
						info.title = file.getName();
					}
					if(node == null){
						continue;
					}
					
					if((info.getTitle().equals(node.getName())) && (node.getType() == MegaNode.TYPE_FILE) && (node.getSize() == file.length())) {
						successCount++;
						uploadedSize += info.size;
						processQueue();
						return;
					}
				}
			}
			if(document == null) {
				lastError = MegaError.API_EACCESS;
				processQueue();
				return;
			}			
			
			megaTransferListener = new MegaTransferListenerInterface() {
				
				@Override
				public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
					if (canceled) {
						if((lock != null) && (lock.isHeld()))
							try{ lock.release(); } catch(Exception ex) {}
						if((wl != null) && (wl.isHeld()))
							try{ wl.release(); } catch(Exception ex) {}
						
						megaApi.cancelTransfer(transfer);
						guiHandler.removeCallbacksAndMessages(null);
						guiHandler.post(new Runnable() {
							public void run() {
								UploadService.this.cancel();
							}});
						Util.deleteIfLocal(UploadService.this, file);
						return;
					}
					
					long bytes = transfer.getTransferredBytes();
					updateProgressNotification(uploadedSize + bytes);					
				}
				
				@Override
				public void onTransferTemporaryError(MegaApiJava api,
						MegaTransfer transfer, MegaError e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
						MegaError e) {
					if((lock != null) && (lock.isHeld()))
						try{ lock.release(); } catch(Exception ex) {}
					if((wl != null) && (wl.isHeld()))
						try{ wl.release(); } catch(Exception ex) {}
					
					if (canceled) {
						guiHandler.removeCallbacksAndMessages(null);
						UploadService.this.cancel();
						
						if (e.getErrorCode() == MegaError.API_OK){
							successCount++;
							uploadedSize += info.size;
							MegaNode n =  megaApi.getNodeByHandle(transfer.getNodeHandle());
							if (n != null){
								return;
							}
						}
						Util.deleteIfLocal(UploadService.this, file);
						return;
					}

					if (e.getErrorCode() == MegaError.API_OK){
						successCount++;
						uploadedSize += info.size;
					} 
					else {
						log("Transfer failed: " + e.getErrorString() + " (" +  e.getErrorCode() + ")");
						if (onFileUploadFailure(intent, e, tryCount)) {
							return;
						}
					}
					processQueue();
					
				}
			};

			if(!wl.isHeld()) wl.acquire();
			if(!lock.isHeld()) lock.acquire();
			
			log("UPLOAD FILE: " + file.getAbsolutePath() + " PARENT: " + document.getName() + 
					"  NAME: " + info.getTitle());
			megaApi.startUpload(file.getAbsolutePath(), document, info.getTitle(), megaTransferListener);
			return;
		}	
	};
	
	private boolean onFileUploadFailure(Intent intent, MegaError error,
			int tryCount) {
		log("onFailure " + tryCount);
		if (tryCount > 2) {
			return false;
		} else {
			lastError = error.getErrorCode();
			handleIntentDelayed(intent, tryCount + 1);
			return true;
		}
	}
	
	/*
	 * Get key for folder
	 */
	private String getFolderKey(Long parentHash, File relativeParent) {
		return "" + parentHash + relativeParent.getAbsolutePath();
	}
	
	/*
	 * Get relative path from parent to file
	 */
	private File getRelativeParent(File parent, File file) {
		return new File(file.getAbsolutePath().replaceFirst(
				Pattern.quote(parent.getAbsolutePath()), "")).getParentFile();
	}
	
	private long getUploadParentHash(Long parentHash, File file, File parent) {
		File relParent = getRelativeParent(parent, file);
		if ((relParent == null) || (relParent.getParentFile() == null)) {
			return parentHash;
		}
		String folderKey = getFolderKey(parentHash, relParent);
		log("folder key is " + folderKey);
		if (createdFolders.containsKey(folderKey)) {
			return createdFolders.get(folderKey);
		}
		log("folder key not found");
		return 0;
	}
	
	@SuppressLint("NewApi")
	private void updateProgressNotification(final long progress) {
		guiHandler.post(new Runnable(){
			public void run(){	
				log("updateProgressNotification");
				int progressPercent = (int) Math.round((double) progress / totalSize
						* 100);
				log(progressPercent + " " + progress + " " + totalSize);
				int left = intentQueue.size() + 1;
				int current = totalCount - left + 1;
				int currentapiVersion = android.os.Build.VERSION.SDK_INT;

				String message = getString(R.string.upload_uploading) + " " + current + " ";
				if (totalCount == 1) {
					message += getResources().getQuantityString(R.plurals.general_num_files, 1);
				} else {
					message += getString(R.string.general_x_of_x) + " " + totalCount;
							
					if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
						message += " " + getResources().getQuantityString(R.plurals.general_num_files, totalCount);
					}
				}
				
				Intent intent = new Intent(UploadService.this, ManagerActivity.class);
				intent.setAction(ManagerActivity.ACTION_CANCEL_UPLOAD);
				String info = Util.getProgressSize(UploadService.this, progress, totalSize);

				PendingIntent pendingIntent = PendingIntent.getActivity(UploadService.this, 0, intent, 0);
				Notification notification = null;
				
				if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)	{
					mBuilder
						.setSmallIcon(R.drawable.ic_stat_notify_upload)
						.setProgress(100, progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setContentInfo(info)
						.setContentText(getString(R.string.upload_touch_to_cancel))
						.setOnlyAlertOnce(true);
					notification = mBuilder.getNotification();
//					notification = mBuilder.build();
				}
				else
				{
					notification = new Notification(R.drawable.ic_stat_notify_upload, null, 1);
					notification.flags |= Notification.FLAG_ONGOING_EVENT;
					notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
					notification.contentIntent = pendingIntent;
					notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_notify_upload);
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
		});
	}
	
	public static void log(String log) {
		Util.log("UploadService", log);
	}

}
