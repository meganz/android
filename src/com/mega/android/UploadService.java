package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

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
import android.util.SparseArray;
import android.widget.RemoteViews;
import android.widget.Toast;

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
	
	
	public static final int CHECK_FILE_TO_UPLOAD_UPLOAD = 1000;
	public static final int CHECK_FILE_TO_UPLOAD_COPY = 1001;
	public static final int CHECK_FILE_TO_UPLOAD_OVERWRITE = 1002;
	public static final int CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER = 1003;
	
	
	private int successCount = 0;
	private int errorCount = 0;
	
	private boolean isForeground = false;
	private boolean canceled;
	
	MegaApplication app;
	MegaApiAndroid megaApi;
		
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
	
	private SparseArray<MegaTransfer> currentTransfers;
	private SparseArray<MegaTransfer> transfersOK;
	private SparseArray<MegaTransfer> transfersError;
	private SparseArray<Long> transfersUploadedSize;
	
	private HashMap<String, String> transfersCopy;
	
	int lastTag = -1;
	int totalToUpload;
	int totalUploaded;
	int totalUploadedError;
	long totalSizeToUpload;
	long totalSizeUploaded;
	long totalSizeUploadedError;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		log("onCreate");
		
		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		successCount = 0;
		
		totalToUpload = 0;
		totalUploaded = 0;
		totalUploadedError = 0;
		totalSizeToUpload = 0;
		totalSizeUploaded = 0;
		totalSizeUploadedError = 0;
		
		currentTransfers = new SparseArray<MegaTransfer>();
		transfersOK = new SparseArray<MegaTransfer>();
		transfersError = new SparseArray<MegaTransfer>();
		transfersUploadedSize = new SparseArray<Long>();
		transfersCopy = new HashMap<String, String>();
		
		isForeground = false;
		canceled = false;

		
		
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
				
				return START_NOT_STICKY;
			}
		}
	
		onHandleIntent(intent);
			
		return START_NOT_STICKY;
	}
	
	protected void onHandleIntent(final Intent intent) {
		log("onHandleIntent");

//		updateProgressNotification(totalSizeUploaded);

		final File file = new File(intent.getStringExtra(EXTRA_FILEPATH));
		
		long parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, 0);
		
		if (file.isDirectory()) {
			uploadFolderTask = new UploadFolderTask(file, parentHandle, this);
			uploadFolderTask.start();
		} 
		else {
			switch(checkFileToUpload(file, parentHandle)){
				case CHECK_FILE_TO_UPLOAD_UPLOAD:{
					log("CHECK_FILE_TO_UPLOAD_UPLOAD");
					
					if(!wl.isHeld()){ 
						wl.acquire();
					}
					if(!lock.isHeld()){
						lock.acquire();
					}
					
					String nameInMEGA = intent.getStringExtra(EXTRA_NAME);
					if (nameInMEGA != null){
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA, this);
					}
					else{
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), this);
					}
					break;
				}
				case CHECK_FILE_TO_UPLOAD_COPY:{
					log("CHECK_FILE_TO_UPLOAD_COPY");
					break;
				}
				case CHECK_FILE_TO_UPLOAD_OVERWRITE:{
					log("CHECK_FILE_TO_UPLOAD_OVERWRITE");
					MegaNode nodeExistsInFolder = megaApi.getNodeByPath(file.getName(), megaApi.getNodeByHandle(parentHandle));
					megaApi.remove(nodeExistsInFolder);
					
					if(!wl.isHeld()){ 
						wl.acquire();
					}
					if(!lock.isHeld()){
						lock.acquire();
					}
					
					String nameInMEGA = intent.getStringExtra(EXTRA_NAME);
					if (nameInMEGA != null){
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA, this);
					}
					else{
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), this);
					}
					break;
				}
				case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER:{
					log("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
					Toast.makeText(getApplicationContext(), file.getName() + " already uploaded in folder " + megaApi.getNodeByHandle(parentHandle).getName(), Toast.LENGTH_SHORT).show();
					
					if ((currentTransfers.size() == 0) && (transfersCopy.size() == 0)){
						successCount = transfersOK.size();
						errorCount = transfersError.size();
						onQueueComplete();
					}

					return;					
				}
			}
		}
	}
	
	int checkFileToUpload(File file, long parentHandle){
		
		MegaNode nodeExistsInFolder = megaApi.getNodeByPath(file.getName(), megaApi.getNodeByHandle(parentHandle));
		if (nodeExistsInFolder == null){
			String localFingerPrint = megaApi.getFingerprint(file.getAbsolutePath());
			MegaNode nodeExists = megaApi.getNodeByFingerprint(localFingerPrint);
			if (nodeExists == null){
				return CHECK_FILE_TO_UPLOAD_UPLOAD;				
			}
			else{				
				transfersCopy.put(localFingerPrint, file.getName());
				totalToUpload++;
				totalSizeToUpload += file.length();
				megaApi.copyNode(nodeExists, megaApi.getNodeByHandle(parentHandle), this);
				return CHECK_FILE_TO_UPLOAD_COPY;				
			}	
		}
		else{
			if (file.length() == nodeExistsInFolder.getSize()){
				return CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER;
			}
			else{
				return CHECK_FILE_TO_UPLOAD_OVERWRITE;	
			}
						
		}
	}
	
	/*
	 * Stop uploading service
	 */
	private void cancel() {
		canceled = true;
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
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
		log("stopping service! success: " + successCount + " total: " + totalToUpload);
		megaApi.resetTotalDownloads();
		
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
		
		if ((successCount + errorCount) > 0){
			if (successCount == 0) {
				log("stopping service!2");
				showCompleteFailNotification();
			} else {
				log("stopping service!");
				showCompleteSuccessNotification();
			}
		}
		
		long totalFromSparse = 0;
		for (int i=0; i<transfersUploadedSize.size(); i++){
			totalFromSparse += transfersUploadedSize.valueAt(i);
		}
		
		log("totalSizeUploaded: " + totalSizeUploaded + "______ TOTALFROMSPARSE: " + totalFromSparse);
				
		
		log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
	}
	
	/*
	 * Show complete error notification
	 */
	private void showCompleteFailNotification() {
		log("showCompleteFailNotification");
		String title = getString(R.string.upload_failed);
		String message = getString(R.string.error_server_connection_problem);
//		if(lastError != 0) message = MegaError.getErrorString(lastError);

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
				+ Formatter.formatFileSize(UploadService.this, totalSizeToUpload);

		Intent intent = new Intent(UploadService.this, ManagerActivity.class);
		
		mBuilderCompat
		.setSmallIcon(R.drawable.ic_stat_notify_upload)
		.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
		.setAutoCancel(true).setTicker(notificationTitle)
		.setContentTitle(notificationTitle).setContentText(size)
		.setOngoing(false);

		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}
	
	private class FolderCreation{
		public String localPath;
		public String folderName;
		public String parentFolderPath;
		public long parentFolderHandle;
		
		FolderCreation(String localPath, String folderName, String parentFolderPath){
			this.localPath = localPath;
			this.folderName = folderName;
			this.parentFolderPath = parentFolderPath;
			this.parentFolderHandle = -1;
		}
		
		FolderCreation(String localPath, String folderName, String parentFolderPath, long parentFolderHandle){
			this.localPath = localPath;
			this.folderName = folderName;
			this.parentFolderPath = parentFolderPath;
			this.parentFolderHandle = parentFolderHandle;
		}
	}
	
	ArrayList<FolderCreation> foldersCreation = new ArrayList<FolderCreation>();
	
	
	
	/*
	 * Upload folder
	 */
	private class UploadFolderTask extends Thread implements MegaRequestListenerInterface{
		
		File folder;
		long parentHandle;
		ArrayList<String> foldersPath = new ArrayList<String>();
		boolean firstFolder = false;
		long firstFolderHandle = -1;
		UploadService uploadService;
		
		UploadFolderTask(File folder, long parentHandle, UploadService uploadService){
			this.folder = folder;
			this.parentHandle = parentHandle;
			this.uploadService = uploadService;
		}

		@Override
		public void run(){
			FolderCreation fC = new FolderCreation(folder.getAbsolutePath(), folder.getName(), megaApi.getNodePath(megaApi.getNodeByHandle(parentHandle)), parentHandle);
			foldersCreation.add(fC);
			log("FC: " + fC.parentFolderPath + "/" + fC.folderName);
			createFoldersCreationArray(folder, fC.parentFolderPath + "/" + fC.folderName);
			for (int i=0;i<foldersCreation.size();i++){
				log("[" + i + "] PARENTPATH: " + foldersCreation.get(i).parentFolderPath +  "___ FOLDER NAME: " + foldersCreation.get(i).folderName + "____ PARENTFOLDERHANDLE: " + foldersCreation.get(i).parentFolderHandle);
			}
			createFolderCreation(foldersCreation.get(0));
		}
		
		private void createFolderCreation(FolderCreation fC){
			if (foldersCreation.size() > 0){
				String nodePath = fC.parentFolderPath + "/" + fC.folderName;
				MegaNode parentNode = megaApi.getNodeByHandle(fC.parentFolderHandle);
				log("CURRENT NODEPATH:" + nodePath);
				if (parentNode != null){
					NodeList nL = megaApi.search (parentNode, fC.folderName, false);
					if (nL.size() == 0){
						megaApi.createFolder(fC.folderName, parentNode, this);
					}
					else{
						MegaNode currentNode = nL.get(0).copy();
						long currentNodeHandle = currentNode.getHandle();
						File localFolder = new File(fC.localPath);
						if (localFolder.isDirectory()){
							File[] files = localFolder.listFiles();
							for (int i=0;i<files.length;i++){
								File f = files[i];
								if (f.isFile()){
									switch(checkFileToUpload(f, currentNodeHandle)){
										case CHECK_FILE_TO_UPLOAD_UPLOAD:{
											log(f.getName() + "__" + currentNode.getName() + "___CHECK_FILE_TO_UPLOAD_UPLOAD");
											
											if(!wl.isHeld()){ 
												wl.acquire();
											}
											if(!lock.isHeld()){
												lock.acquire();
											}
											
											megaApi.startUpload(f.getAbsolutePath(), currentNode, uploadService);
											break;
										}
										case CHECK_FILE_TO_UPLOAD_COPY:{
											log(f.getName() + "__" + currentNode.getName() + "___CHECK_FILE_TO_UPLOAD_COPY");
											break;
										}
										case CHECK_FILE_TO_UPLOAD_OVERWRITE:{
											log(f.getName() + "__" + currentNode.getName() + "___CHECK_FILE_TO_UPLOAD_OVERWRITE");
											MegaNode nodeExistsInFolder = megaApi.getNodeByPath(f.getName(), currentNode);
											megaApi.remove(nodeExistsInFolder);
											
											if(!wl.isHeld()){ 
												wl.acquire();
											}
											if(!lock.isHeld()){
												lock.acquire();
											}
											
											megaApi.startUpload(f.getAbsolutePath(), currentNode, uploadService);

											break;
										}
										case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER:{
											log(f.getName() + "__" + currentNode.getName() + "___CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
//											Toast.makeText(getApplicationContext(), f.getName() + " already uploaded in folder " + currentNode.getName(), Toast.LENGTH_SHORT).show();
											
											if ((currentTransfers.size() == 0) && (transfersCopy.size() == 0) && (foldersCreation.size() == 0)){
												successCount = transfersOK.size();
												errorCount = transfersError.size();
												
												onQueueComplete();												
											}
	
											break;					
										}
									}
								}
							}
						}
						
						foldersCreation.remove(0);
						if (!foldersCreation.isEmpty()){
							for (int i=0; i<foldersCreation.size(); i++){
								if (nodePath.compareTo(foldersCreation.get(i).parentFolderPath) == 0){
									foldersCreation.get(i).parentFolderHandle = currentNodeHandle;
									log("FOLDER EXISTS: NAME" + foldersCreation.get(i).folderName + "____ PARENTPATH: " + foldersCreation.get(i).parentFolderPath + "____ HANDLE: " + foldersCreation.get(i).parentFolderHandle);
								}
							}
							
							createFolderCreation(foldersCreation.get(0));
						}
					}
				}
			}
		}
		
		private void createFoldersCreationArray(File currentFolder, String parentFolderPath){
			if (currentFolder.isDirectory()){
				File[] files = currentFolder.listFiles();
				for (int i=0; i<files.length; i++){
					File f = files[i];
					if (f.isDirectory()){
						FolderCreation fC = new FolderCreation(f.getAbsolutePath(), f.getName(), parentFolderPath);
						foldersCreation.add(fC);
						createFoldersCreationArray(f, parentFolderPath + "/" + f.getName());
					}
				}
			}
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
					NodeList nL = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
					MegaNode nFolder = null;
					boolean folderExists = false;
					for (int i=0;i<nL.size();i++){
						if ((folder.getName().compareTo(nL.get(i).getName()) == 0) && (nL.get(i).isFolder())){
							folderExists = true;
							nFolder = nL.get(i);
							break;
						}
					}
					if (!folderExists){
						megaApi.createFolder(new File(path).getName(), megaApi.getNodeByHandle(parentHandle), this);
					}
					else{
						if (!firstFolder){
							firstFolder = true;				
							firstFolderHandle = nFolder.getHandle();
						}
							
						File currentFolder = new File(foldersPath.get(0));
						File[] files = currentFolder.listFiles();
						for (int i=0;i<files.length;i++){
							File f = files[i];
							if (f.isFile()){
								switch(checkFileToUpload(f, nFolder.getHandle())){
									case CHECK_FILE_TO_UPLOAD_UPLOAD:{
										log("CHECK_FILE_TO_UPLOAD_UPLOAD");
										
										if(!wl.isHeld()){ 
											wl.acquire();
										}
										if(!lock.isHeld()){
											lock.acquire();
										}
										
										megaApi.startUpload(f.getAbsolutePath(), nFolder, uploadService);
										
										break;
									}
									case CHECK_FILE_TO_UPLOAD_COPY:{
										log("CHECK_FILE_TO_UPLOAD_COPY");
										break;
									}
									case CHECK_FILE_TO_UPLOAD_OVERWRITE:{
										log("CHECK_FILE_TO_UPLOAD_OVERWRITE");
										MegaNode nodeExistsInFolder = megaApi.getNodeByPath(f.getName(), nFolder);
										megaApi.remove(nodeExistsInFolder);
										
										if(!wl.isHeld()){ 
											wl.acquire();
										}
										if(!lock.isHeld()){
											lock.acquire();
										}
										
										megaApi.startUpload(f.getAbsolutePath(), nFolder, uploadService);
										
										break;
									}
									case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER:{
										log("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
	
										break;					
									}
								}
							}
						}
						
						foldersPath.remove(0);
						if (!foldersPath.isEmpty()){
							createFolder(foldersPath.get(0));
						}
						
						if ((currentTransfers.size() == 0) && (transfersCopy.size() == 0) && (foldersPath.isEmpty())){
							successCount = transfersOK.size();
							errorCount = transfersError.size();
							onQueueComplete();
						}
					}
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
			log("onRequestStart: " + request.getRequestString());
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			if (request.getType() == MegaRequest.TYPE_MKDIR){
				log("onRequestFinish: " + request.getRequestString() + "_" + foldersCreation.get(0).folderName);
				if (e.getErrorCode() == MegaError.API_OK){					
					long currentNodeHandle = request.getNodeHandle();
					MegaNode currentNode = megaApi.getNodeByHandle(currentNodeHandle);
					
					FolderCreation fC = foldersCreation.get(0);
					
					File localFolder = new File(fC.localPath);
					if (localFolder.isDirectory()){
						File[] files = localFolder.listFiles();
						for (int i=0;i<files.length;i++){
							File f = files[i];
							if (f.isFile()){
								megaApi.startUpload(f.getAbsolutePath(), currentNode, uploadService);
							}
						}
					}
					
					foldersCreation.remove(0);
					if (!foldersCreation.isEmpty()){
						String newFolderPath = megaApi.getNodePath(currentNode);
						
						for (int i=0; i<foldersCreation.size(); i++){
							if (newFolderPath.compareTo(foldersCreation.get(i).parentFolderPath) == 0){
								foldersCreation.get(i).parentFolderHandle = currentNodeHandle;
								log("NEW FOLDERSCREATION: NAME___" + foldersCreation.get(i).folderName + "____ PARENTPATH: " + foldersCreation.get(i).parentFolderPath + "____ HANDLE: " + foldersCreation.get(i).parentFolderHandle);
							}
						}
						
						createFolderCreation(foldersCreation.get(0));
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
			log("onRequestUpdate:" + request.getName());
		}
	}
	
	@SuppressLint("NewApi")
	private void updateProgressNotification(final long progress) {
		log("updateProgressNotification");
		int progressPercent = (int) Math.round((double) progress / totalSizeToUpload
				* 100);
		log(progressPercent + " " + progress + " " + totalSizeToUpload);
		int left = totalToUpload - (totalUploaded + totalUploadedError);
		int current = totalToUpload - left + 1;
		
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		
		String message = "";
		if (totalToUpload == 0){
			message = getString(R.string.download_preparing_files);
		}
		else{
			message = getString(R.string.upload_uploading) + " " + current + " ";
			if (totalToUpload == 1) {
				message += getResources().getQuantityString(R.plurals.general_num_files, 1);
			} else {
				message += getString(R.string.general_x_of_x) + " " + totalToUpload;
						
				if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
					message += " " + getResources().getQuantityString(R.plurals.general_num_files, totalToUpload);
				}
			}
		}
		
		Intent intent = new Intent(UploadService.this, ManagerActivity.class);
		intent.setAction(ManagerActivity.ACTION_CANCEL_UPLOAD);
		String info = Util.getProgressSize(UploadService.this, progress, totalSizeToUpload);

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
	
	public static void log(String log) {
		Util.log("UploadService", log);
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Upload start: " + transfer.getFileName() + "_" + megaApi.getTotalUploads());
		
		updateProgressNotification(totalSizeUploaded);
		
		currentTransfers.put(transfer.getTag(), transfer);
		totalToUpload++;
		totalSizeToUpload += transfer.getTotalBytes();
		
		log("CURRENTTRANSFERS.SIZE = " + currentTransfers.size() + "___" + "TOTALTOUPLOAD: " + totalToUpload + "___" + "TOTALSIZETOUPLOAD: " + totalSizeToUpload + "____" + "TRANSFER.TAG: " + transfer.getTag());
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError error) {
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
			if (error.getErrorCode() == MegaError.API_OK) {
				log("Upload OK: " + transfer.getFileName());
				
				totalUploaded++;
				currentTransfers.remove(transfer.getTag());
				transfersOK.put(transfer.getTag(), transfer);
				long currentSizeUploaded = transfersUploadedSize.get(transfer.getTag());
				totalSizeUploaded += (transfer.getTotalBytes()-currentSizeUploaded);
				transfersUploadedSize.put(transfer.getTag(), transfer.getTotalBytes());
			}
			else{
				log("Upload Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());
				if(error.getErrorCode() == MegaError.API_EINCOMPLETE){
					totalToUpload--;
					totalSizeToUpload -= transfer.getTotalBytes();
					Long currentSizeUploaded = transfersUploadedSize.get(transfer.getTag());
					if (currentSizeUploaded != null){
						totalSizeUploaded -= currentSizeUploaded;
					}
					currentTransfers.remove(transfer.getTag());
				}
				else{
					totalUploadedError++;
					totalSizeUploadedError += transfer.getTotalBytes();
					currentTransfers.remove(transfer.getTag());
					transfersError.put(transfer.getTag(), transfer);
				}
			}
			
			if (getApplicationContext().getExternalCacheDir() != null){
				File localFile = new File (getApplicationContext().getExternalCacheDir(), transfer.getFileName());
				if (localFile.exists()){
					localFile.delete();
				}
			}
			else{
				File localFile = new File (getApplicationContext().getCacheDir(), transfer.getFileName());
				if (localFile.exists()){
					localFile.delete();
				}
			}
			
			log("CURRENTTRANSFERS: " + currentTransfers.size() + "___ OK: " + transfersOK.size() + "___ ERROR: " + transfersError.size());
			if ((currentTransfers.size() == 0) && (transfersCopy.size() == 0)){
				successCount = transfersOK.size();
				errorCount = transfersError.size();
				onQueueComplete();
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
				
		
		long currentSizeUploaded = 0;
		if (transfersUploadedSize.get(transfer.getTag()) != null){
			currentSizeUploaded = transfersUploadedSize.get(transfer.getTag());
		}
		totalSizeUploaded += (transfer.getTransferredBytes()-currentSizeUploaded);
		transfersUploadedSize.put(transfer.getTag(), transfer.getTransferredBytes());
		
		final long bytes = transfer.getTransferredBytes();
		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
		updateProgressNotification(totalSizeUploaded);
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log(transfer.getPath() + "\nDownload Temporary Error: " + e.getErrorString() + "__" + e.getErrorCode());
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
		if (request.getType() == MegaRequest.TYPE_COPY){
			updateProgressNotification(totalSizeUploaded);
		}
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			log("cancel_transfers received");
			if (e.getErrorCode() == MegaError.API_OK){
				megaApi.pauseTransfers(false, this);
				megaApi.resetTotalUploads();
				totalSizeToUpload = 0;
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("pause_transfer false received");
			if (e.getErrorCode() == MegaError.API_OK){
				cancel();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			MegaNode n = megaApi.getNodeByHandle(request.getNodeHandle());
			String currentNodeName = n.getName();
			String megaFingerPrint = megaApi.getFingerprint(n);
			log("copy node");
			if (e.getErrorCode() == MegaError.API_OK){
				String nameInMega = transfersCopy.get(megaFingerPrint);
				if (nameInMega != null){
					if (nameInMega.compareTo(currentNodeName) != 0){
						megaApi.renameNode(n, nameInMega);
					}
				}
			}
			totalSizeUploaded += n.getSize();
			totalUploaded++;
			transfersCopy.remove(megaFingerPrint);
			
			if ((currentTransfers.size() == 0) && (transfersCopy.size() == 0) && (foldersCreation.size() == 0)){
				successCount = transfersOK.size();
				errorCount = transfersError.size();
				onQueueComplete();
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
		log("onRequestUpdate: " + request.getName());
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}
}
