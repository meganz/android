package mega.privacy.android.app;

import java.io.File;
import java.util.HashMap;


import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;

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
import android.os.Environment;
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
	DatabaseHandler dbH = null;
	
	private Notification.Builder mBuilder;
	private NotificationCompat.Builder mBuilderCompat;
	private NotificationManager mNotificationManager;
	
	Object syncObject = new Object();
	
	MegaRequestListenerInterface megaRequestListener;
	MegaTransferListenerInterface megaTransferListener;
	
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
		
		megaApi.addTransferListener(this);
		
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
		log("onDestroy");
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}
		
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
			megaApi.removeTransferListener(this);
		}
		
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

		final File file = new File(intent.getStringExtra(EXTRA_FILEPATH));
		
		long parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, 0);
		
		if (file.isDirectory()) {
			String nameInMEGA = intent.getStringExtra(EXTRA_NAME);
			if (nameInMEGA != null){
				megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA);
			}
			else{
				megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle));
			}
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
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA);
					}
					else{
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle));
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
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA);
					}
					else{
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle));
					}
					break;
				}
				case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER:{
					log("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
					String sShow=file.getName() + " " + getString(R.string.general_already_uploaded);					
					Toast.makeText(getApplicationContext(), sShow,Toast.LENGTH_SHORT).show();
					
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
		log("cancel");
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
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
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
		log("after stopSelf");
		String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR;						
		File f = new File(pathSelfie);
		//Delete recursively all files and folder
		if (f.exists()) {
			if (f.isDirectory()) {
			    for (File c : f.listFiles())
			      c.delete();
			}
			f.delete();
		}
	}
	
	/*
	 * Show complete error notification
	 */
	private void showCompleteFailNotification() {
		log("showCompleteFailNotification");
		String title = getString(R.string.upload_failed);
		String message = getString(R.string.error_server_connection_problem);
//		if(lastError != 0) message = MegaError.getErrorString(lastError);

		Intent intent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {	
			intent = new Intent(UploadService.this, ManagerActivityLollipop.class);
		}
		else{
			intent = new Intent(UploadService.this, ManagerActivity.class);
		}
		
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

		Intent intent = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {	
			intent = new Intent(UploadService.this, ManagerActivityLollipop.class);
		}
		else{
			intent = new Intent(UploadService.this, ManagerActivity.class);
		}
		
		mBuilderCompat
		.setSmallIcon(R.drawable.ic_stat_notify_upload)
		.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
		.setAutoCancel(true).setTicker(notificationTitle)
		.setContentTitle(notificationTitle).setContentText(size)
		.setOngoing(false);

		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
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
		
		Intent intent;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			log("intent from Lollipop");
			intent = new Intent(UploadService.this, ManagerActivityLollipop.class);
			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(getApplicationContext());	
			}
			if (dbH.getCredentials() == null){
				intent.setAction(ManagerActivityLollipop.ACTION_CANCEL_UPLOAD);
			}
			else{
				intent.setAction(ManagerActivityLollipop.ACTION_SHOW_TRANSFERS);
			}
		}
		else{
			log("intent NOOT Lollipop");
			intent = new Intent(UploadService.this, ManagerActivity.class);
			intent.setAction(ManagerActivity.ACTION_CANCEL_UPLOAD);
		}
		
		String info = Util.getProgressSize(UploadService.this, progress, totalSizeToUpload);

		PendingIntent pendingIntent = PendingIntent.getActivity(UploadService.this, 0, intent, 0);
		Notification notification = null;
		
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)	{
			
			if (currentapiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP){
				mBuilder
				.setSmallIcon(R.drawable.ic_stat_notify_upload)
				.setProgress(100, progressPercent, false)
				.setContentIntent(pendingIntent)
				.setOngoing(true).setContentTitle(message).setContentInfo(info)
				.setContentText(getString(R.string.download_touch_to_show))
				.setOnlyAlertOnce(true);
			notification = mBuilder.getNotification();
			}
			else{
				mBuilder
				.setSmallIcon(R.drawable.ic_stat_notify_upload)
				.setProgress(100, progressPercent, false)
				.setContentIntent(pendingIntent)
				.setOngoing(true).setContentTitle(message).setContentInfo(info)
				.setContentText(getString(R.string.upload_touch_to_cancel))
				.setOnlyAlertOnce(true);
			notification = mBuilder.getNotification();
			}			
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
		
		if (!transfer.isFolderTransfer()){
			updateProgressNotification(totalSizeUploaded);
			
			currentTransfers.put(transfer.getTag(), transfer);
			totalToUpload++;
			totalSizeToUpload += transfer.getTotalBytes();
			
			log("CURRENTTRANSFERS.SIZE = " + currentTransfers.size() + "___" + "TOTALTOUPLOAD: " + totalToUpload + "___" + "TOTALSIZETOUPLOAD: " + totalSizeToUpload + "____" + "TRANSFER.TAG: " + transfer.getTag());
		}
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,MegaError error) {
		log("Upload finished: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
		log("transfer.getPath:" + transfer.getPath());
		if (!transfer.isFolderTransfer()){
			if (canceled) {
				log("Upload cancelled: " + transfer.getFileName());
				
				if((lock != null) && (lock.isHeld()))
					try{ lock.release(); } catch(Exception ex) {}
				if((wl != null) && (wl.isHeld()))
					try{ wl.release(); } catch(Exception ex) {}
					
				UploadService.this.cancel();
				log("after cancel");
				String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR;						
				File f = new File(pathSelfie);
				//Delete recursively all files and folder
				if (f.isDirectory()) {
				    for (File c : f.listFiles())
				      c.delete();
				}
				f.delete();					
				
			}		
			else{
				if (error.getErrorCode() == MegaError.API_OK) {
					log("Upload OK: " + transfer.getFileName());
					
					totalUploaded++;
					currentTransfers.remove(transfer.getTag());
					transfersOK.put(transfer.getTag(), transfer);
					long currentSizeUploaded = 0;
					if (transfersUploadedSize.get(transfer.getTag()) != null){
						currentSizeUploaded = transfersUploadedSize.get(transfer.getTag());
					}
					totalSizeUploaded += (transfer.getTotalBytes()-currentSizeUploaded);
					transfersUploadedSize.put(transfer.getTag(), transfer.getTotalBytes());
					
					File previewDir = PreviewUtils.getPreviewFolder(this);
					File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
					File thumbDir = ThumbnailUtils.getThumbFolder(this);
					File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
					megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
					megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());				
					
					if(Util.isVideoFile(transfer.getPath())){
						log("Is video!!!");					
						ThumbnailUtilsLollipop.createThumbnailVideo(this, transfer.getPath(), megaApi, transfer.getNodeHandle());			
					}
					else{
						log("NOT video!");
					}				
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
					else if(error.getErrorCode()==MegaError.API_EOVERQUOTA){
						log("OVERQUOTA ERROR: "+error.getErrorCode());
						Intent intent;
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							intent = new Intent(this, ManagerActivityLollipop.class);
							intent.setAction(ManagerActivityLollipop.ACTION_OVERQUOTA_ALERT);
						}
						else{
							intent = new Intent(this, ManagerActivity.class);
							intent.setAction(ManagerActivity.ACTION_OVERQUOTA_ALERT);					}					
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						
						startActivity(intent);
						
						Intent tempIntent = null;
						tempIntent = new Intent(this, UploadService.class);
						tempIntent.setAction(UploadService.ACTION_CANCEL);
						startService(tempIntent);	
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
						log("Delete file!: "+localFile.getAbsolutePath());
						localFile.delete();
					}
				}
				else{
					File localFile = new File (getApplicationContext().getCacheDir(), transfer.getFileName());
					if (localFile.exists()){
						log("Delete file!: "+localFile.getAbsolutePath());
						localFile.delete();
					}
				}
				
				log("CURRENTTRANSFERS: " + currentTransfers.size() + "___ OK: " + transfersOK.size() + "___ ERROR: " + transfersError.size());
				if ((currentTransfers.size() == 0) && (transfersCopy.size() == 0)){
					successCount = transfersOK.size();
					errorCount = transfersError.size();
					onQueueComplete();
				}	
				
				log("En finish: "+transfer.getFileName()+"path? "+transfer.getPath());
				String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR;
				
				if(transfer.getPath().startsWith(pathSelfie)){
					File f = new File(transfer.getPath());
					f.delete();
				}
	
			}
		}
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		if (!transfer.isFolderTransfer()){
			if (canceled) {
				log("Transfer cancel: " + transfer.getFileName());
	
				if((lock != null) && (lock.isHeld()))
					try{ lock.release(); } catch(Exception ex) {}
				if((wl != null) && (wl.isHeld()))
					try{ wl.release(); } catch(Exception ex) {}
				
				megaApi.cancelTransfer(transfer);
				UploadService.this.cancel();
				log("after cancel");
				return;
			}
			
			if (transfer.getPath() != null){
				File f = new File(transfer.getPath());
				if (f.isDirectory()){
					transfer.getTotalBytes();				
				}
			}
	
					
			
			long currentSizeUploaded = 0;
			if (transfersUploadedSize.get(transfer.getTag()) != null){
				currentSizeUploaded = transfersUploadedSize.get(transfer.getTag());
			}
			totalSizeUploaded += (transfer.getTransferredBytes()-currentSizeUploaded);
			transfersUploadedSize.put(transfer.getTag(), transfer.getTransferredBytes());
			
			log("UPDATESIZES: " + currentSizeUploaded + "___" + transfer.getTransferredBytes() + "____" + totalSizeUploaded + "___" + transfer.getTotalBytes());
			
			final long bytes = transfer.getTransferredBytes();
			log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
			updateProgressNotification(totalSizeUploaded);
		}
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
			if (e.getErrorCode() == MegaError.API_OK){
				MegaNode n = megaApi.getNodeByHandle(request.getNodeHandle());
				if (n != null){
					String currentNodeName = n.getName();
					String megaFingerPrint = megaApi.getFingerprint(n);
					log("copy node");
					String nameInMega = transfersCopy.get(megaFingerPrint);
					if (nameInMega != null){
						if (nameInMega.compareTo(currentNodeName) != 0){
							megaApi.renameNode(n, nameInMega);
						}
					}
					totalSizeUploaded += n.getSize();
					totalUploaded++;
					transfersCopy.remove(megaFingerPrint);
					
					if ((currentTransfers.size() == 0) && (transfersCopy.size() == 0)){
						successCount = transfersOK.size();
						errorCount = transfersError.size();
						onQueueComplete();
					}	
				}
				else{
					Intent tempIntent = null;
					tempIntent = new Intent(this, UploadService.class);
					tempIntent.setAction(UploadService.ACTION_CANCEL);
					startService(tempIntent);
				}
			}
			else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
				log("OVERQUOTA ERROR: "+e.getErrorCode());
				
				Intent intent;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(ManagerActivityLollipop.ACTION_OVERQUOTA_ALERT);
				}
				else{
					intent = new Intent(this, ManagerActivity.class);
					intent.setAction(ManagerActivity.ACTION_OVERQUOTA_ALERT);
				}					

				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				
				Intent tempIntent = null;
				tempIntent = new Intent(this, UploadService.class);
				tempIntent.setAction(UploadService.ACTION_CANCEL);
				startService(tempIntent);	
			}
			else{
				Intent tempIntent = null;
				tempIntent = new Intent(this, UploadService.class);
				tempIntent.setAction(UploadService.ACTION_CANCEL);
				startService(tempIntent);
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
