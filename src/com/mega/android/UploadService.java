package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import com.mega.sdk.TransferList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
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
public class UploadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface {

	public static String ACTION_CANCEL = "CANCEL_UPLOAD";
	public static String ACTION_CANCEL_ONE_UPLOAD = "CANCEL_ONE_UPLOAD";
	public static String EXTRA_FILEPATH = "MEGA_FILE_PATH";
	public static String EXTRA_FOLDERPATH = "MEGA_FOLDER_PATH";
	public static String EXTRA_NAME = "MEGA_FILE_NAME";
	public static String EXTRA_SIZE = "MEGA_SIZE";
	public static String EXTRA_PARENT_HASH = "MEGA_PARENT_HASH";
	
	
	private int totalCount = 0;
	private int successCount = 0;
	private int doneCount = 0;
	
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
	
	private int notificationId = 1;
	private int notificationIdFinal = 5;
	
	private class IntentData{
		long parentHandle;
		File file;
		
		IntentData (File file, long parentHandle){
			this.file = file;
			this.parentHandle = parentHandle;
		}
		
		public void setParentHandle(long parentHandle){
			this.parentHandle = parentHandle;
		}
		
		public long getParentHandle(){
			return parentHandle;
		}
		
		public void setFile (File file){
			this.file = file;
		}
		
		public File getFile(){
			return file;
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		log("onCreate");
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
		
		if ((intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_CANCEL)) {
				log("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
				return START_NOT_STICKY;
			}
			else if (intent.getAction().equals(ACTION_CANCEL_ONE_UPLOAD)){
				log("Cancel one upload intent");
				totalCount--;
				if (totalCount == 0){
					megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
				}
				totalSize = uploadedSize;
				TransferList tL = megaApi.getTransfers();
				for (int i=0;i<tL.size();i++){
					totalSize += tL.get(i).getTotalBytes();
				}
				updateProgressNotification(uploadedSize);
				return START_NOT_STICKY;
			}
		}
	
		onHandleIntent(intent);
			
		return START_REDELIVER_INTENT;
	}
	
	protected void onHandleIntent(final Intent intent) {
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
		
		long parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, 0);
		
		if (file.isDirectory()) {
			uploadFolderTask = new UploadFolderTask(file, parentHandle, this);
			uploadFolderTask.start();
		} 
		else {
			if(!wl.isHeld()){ 
				wl.acquire();
			}
			if(!lock.isHeld()){
				lock.acquire();
			}
			megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), this);
		}
	}
	
	
	/*
	 * Handle download file complete
	 */
	private void onUploadComplete(boolean success) {
		log("onDownloadComplete");
		
		if (success){
			successCount++;
		}
		doneCount++;
		
		if (doneCount == totalCount){
			onQueueComplete();
		}
	}
	
	/*
	 * Stop uploading service
	 */
	private void cancel() {
		canceled = true;
		isForeground = false;
		stopForeground(true);
		stopSelf();
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
		log("Stopping foreground!");
		log("stopping service! success: " + successCount + " total: " + totalCount);
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
			showCompleteFailNotification();
		} else {
			log("stopping service!");
			showCompleteSuccessNotification();
		}
				
		log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
		isForeground = false;
		stopForeground(true);
		stopSelf();
	}
	
	/*
	 * Show complete error notification
	 */
	private void showCompleteFailNotification() {
		
		
		log("showCompleteFailNotification");
		String title = getString(R.string.upload_failed);
		String message = getString(R.string.error_server_connection_problem);
		if(lastError != 0) message = MegaError.getErrorString(lastError);

		Intent intent = new Intent(UploadService.this, ManagerActivity.class);
		
		mBuilderCompat
				.setSmallIcon(R.drawable.ic_stat_notify_download)
				.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
				.setAutoCancel(true).setContentTitle(title)
				.setContentText(message)
				.setOngoing(false);

		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}
	
	/*
	 * Show complete success notification
	 */
	private void showCompleteSuccessNotification() {
		
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
		.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
		.setAutoCancel(true).setTicker(notificationTitle)
		.setContentTitle(notificationTitle).setContentText(size)
		.setOngoing(false);

		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}
	
	/*
	 * Upload folder
	 */
	private class UploadFolderTask extends Thread implements MegaRequestListenerInterface{
		
		File folder;
		long parentHandle;
		ArrayList<IntentData> intents;
		ArrayList<String> foldersPath = new ArrayList<String>();
		boolean firstFolder = false;
		long firstFolderHandle = -1;
		UploadService uploadService;
		
		UploadFolderTask(File folder, long parentHandle, UploadService uploadService){
			this.folder = folder;
			this.parentHandle = parentHandle;
			this.intents = new ArrayList<UploadService.IntentData>();
			this.uploadService = uploadService;
		}

		@Override
		public void run(){
			foldersPath.add(folder.getAbsolutePath());
			createFoldersPathArray(folder);
			createFolder(foldersPath.get(0));
		}
		
		private void createFoldersPathArray(File currentFolder){
			if (currentFolder.isDirectory()){
				File[] files = currentFolder.listFiles();
				for (int i=0;i<files.length;i++){
					File f = files[i];
					if (f.isDirectory()){
						foldersPath.add(f.getAbsolutePath());
						createFoldersPathArray(f);
					}					
				}
			}	
		}
		
		private void createFolder(String path){
			if (foldersPath.size() > 0){
				if (path.compareTo(folder.getAbsolutePath()) == 0){
//					NodeList nL = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
//					boolean folderExists = false;
//					for (int i=0;i<nL.size();i++){
//						if ((folder.getName().compareTo(nL.get(i).getName()) == 0) && (nL.get(i).isFolder())){
//							folderExists = true;
//						}
//					}
//					if (!folderExists){
						megaApi.createFolder(new File(path).getName(), megaApi.getNodeByHandle(parentHandle), this);
//					}
//					else{
//						foldersPath.remove(0);
//						if (!foldersPath.isEmpty()){
//							createFolder(foldersPath.get(0));
//						}
//					}
				}
				else{
					long uploadParentHandle = getUploadParentHandle(path);
					megaApi.createFolder(new File(path).getName(), megaApi.getNodeByHandle(uploadParentHandle), this);
				}
			}
//			else{
//				
//			}
		}
		
		private long getUploadParentHandle(String path){
			String relativePath = path.replaceFirst(folder.getAbsolutePath(), "");
			String [] parts = relativePath.split("/");
			log("RELATIVE PATH: " + relativePath);			
			long tempHandle = firstFolderHandle;
			if (parts.length > 1){
				for (int i=1;i<parts.length;i++){
					NodeList nL = megaApi.getChildren(megaApi.getNodeByHandle(tempHandle));
					boolean folderExists = false;
					int j = 0;
					while (!folderExists && (j < nL.size()) ){
						MegaNode n = nL.get(j).copy();
						if ( (n.getName().compareTo(parts[i]) == 0) && (n.isFolder()) ){
							tempHandle = n.getHandle();
							folderExists = true;
						}
						j++;
					}
				}
				return tempHandle;
			}
			return tempHandle;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart: " + request.getType());
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			if (request.getType() == MegaRequest.TYPE_MKDIR){
				log("onRequestFinish: " + request.getType() + "_" + foldersPath.get(0));
				if (e.getErrorCode() == MegaError.API_OK){
					MegaNode n = megaApi.getNodeByHandle(request.getNodeHandle()).copy();
					if (!firstFolder){
						firstFolder = true;				
						firstFolderHandle = n.getHandle();
					}
						
					File currentFolder = new File(foldersPath.get(0));
					File[] files = currentFolder.listFiles();
					for (int i=0;i<files.length;i++){
						File f = files[i];
						if (f.isFile()){
							megaApi.startUpload(f.getAbsolutePath(), n, uploadService);
						}
					}
					
					foldersPath.remove(0);
					if (!foldersPath.isEmpty()){
						createFolder(foldersPath.get(0));
					}
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			log("onRequestTemporaryError: " + request.getType());
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
	}
	
	@SuppressLint("NewApi")
	private void updateProgressNotification(final long progress) {
		guiHandler.post(new Runnable(){
			public void run(){	
				log("updateProgressNotification");
				int progressPercent = (int) Math.round((double) progress / totalSize
						* 100);
				log(progressPercent + " " + progress + " " + totalSize);
				int left = totalCount - doneCount;
				int current = totalCount - left + 1;
				int currentapiVersion = android.os.Build.VERSION.SDK_INT;
				
				String message = "";
				if (totalCount == 0){
					message = getString(R.string.download_preparing_files);
				}
				else{

					message = getString(R.string.upload_uploading) + " " + current + " ";
					if (totalCount == 1) {
						message += getResources().getQuantityString(R.plurals.general_num_files, 1);
					} else {
						message += getString(R.string.general_x_of_x) + " " + totalCount;
								
						if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
							message += " " + getResources().getQuantityString(R.plurals.general_num_files, totalCount);
						}
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

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Upload start: " + transfer.getFileName() + "_" + megaApi.getTotalUploads());
		totalCount++;
		totalSize += transfer.getTotalBytes();
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		log("Upload finished: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
		log("transfer.getPath:" + transfer.getPath());
		if (canceled) {
			log("Upload cancelled: " + transfer.getFileName());
			
			if((lock != null) && (lock.isHeld()))
				try{ lock.release(); } catch(Exception ex) {}
			if((wl != null) && (wl.isHeld()))
				try{ wl.release(); } catch(Exception ex) {}
				
			UploadService.this.cancel();
		}		
		else{
			if (e.getErrorCode() == MegaError.API_OK) {
				log("Upload OK: " + transfer.getFileName());
				uploadedSize += transfer.getTransferredBytes();
				log("UPLOADEDFILE: " + transfer.getPath());
				onUploadComplete(true);
			}
			else{
				log("Upload Error: " + transfer.getFileName() + "_" + e.getErrorCode() + "___" + e.getErrorString());
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
			UploadService.this.cancel();
			return;
		}
				
		final long bytes = transfer.getTransferredBytes();
		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
    	updateProgressNotification(uploadedSize + bytes);
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log(transfer.getPath() + "\nDownload Temporary Error: " + e.getErrorString() + "__" + e.getErrorCode());
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			log("cancel_transfers received");
			if (e.getErrorCode() == MegaError.API_OK){
				megaApi.pauseTransfers(false, this);
				megaApi.resetTotalUploads();
				totalCount = 0;
				totalSize = 0;
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("pause_transfer false received");
			if (e.getErrorCode() == MegaError.API_OK){
				cancel();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}
}
