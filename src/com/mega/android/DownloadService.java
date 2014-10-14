package com.mega.android;

import java.io.File;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaTransferListenerInterface;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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
 * Background service to download files
 */
public class DownloadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface{
	
	// Action to stop download
	public static String ACTION_CANCEL = "CANCEL_DOWNLOAD";
	public static String ACTION_CANCEL_ONE_DOWNLOAD = "CANCEL_ONE_DOWNLOAD";
	public static String EXTRA_SIZE = "DOCUMENT_SIZE";
	public static String EXTRA_HASH = "DOCUMENT_HASH";
	public static String EXTRA_URL = "DOCUMENT_URL";
	public static String EXTRA_PATH = "SAVE_PATH";
	public static String EXTRA_FOLDER_LINK = "FOLDER_LINK";
	public static String EXTRA_OFFLINE = "IS_OFFLINE";
	public static String ACTION_OPEN_PDF = "OPEN_PDF";
	public static String EXTRA_PATH_PDF = "PATH_PDF";
	public static String ACTION_EXPLORE_ZIP = "EXPLORE_ZIP";
	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	public static String EXTRA_CONTACT_ACTIVITY = "CONTACT_ACTIVITY";
	
	
	private int successCount = 0;
	private int errorCount = 0;
		
	private boolean isForeground = false;
	private boolean canceled;
	
	private boolean isOffline = false;
	private boolean isFolderLink = false;
	private boolean fromContactFile = false;

	MegaApplication app;
	MegaApiAndroid megaApi;
	
	WifiLock lock;
	WakeLock wl;
	
	File currentFile;
	File currentDir;
	
	private int notificationId = 2;
	private int notificationIdFinal = 4;
	private NotificationCompat.Builder mBuilderCompat;
	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;
	
	private SparseArray<MegaTransfer> currentTransfers;
	private SparseArray<MegaTransfer> transfersOK;
	private SparseArray<MegaTransfer> transfersError;
	private SparseArray<Long> transfersDownloadedSize;
	
	int lastTag = -1;
	int totalToDownload;
	int totalDownloaded;
	int totalDownloadedError;
	long totalSizeToDownload;
	long totalSizeDownloaded;
	long totalSizeDownloadedError;	
	
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate");
		
		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		successCount = 0;
		
		totalToDownload = 0;
		totalDownloaded = 0;
		totalDownloadedError = 0;
		totalSizeToDownload = 0;
		totalSizeDownloaded = 0;
		totalSizeDownloadedError = 0;
		
		isForeground = false;
		canceled = false;
		
		currentTransfers = new SparseArray<MegaTransfer>();
		transfersOK = new SparseArray<MegaTransfer>();
		transfersError = new SparseArray<MegaTransfer>();
		transfersDownloadedSize = new SparseArray<Long>();
		
		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }
        
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			mBuilder = new Notification.Builder(DownloadService.this);	
		}
		mBuilderCompat = new NotificationCompat.Builder(getApplicationContext());
		
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
	public int onStartCommand(Intent intent, int flags, int startId){
		log("onStartCommand");
		
		if(intent == null){
			return START_NOT_STICKY;
		}
		
		if (intent.getAction() != null){
			if (intent.getAction().equals(ACTION_CANCEL)){
				log("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD, this);
				return START_NOT_STICKY;
			}
			else if (intent.getAction().equals(ACTION_CANCEL_ONE_DOWNLOAD)){
				log("Cancel one download intent");
								
				return START_NOT_STICKY;
			}
		}
				
		onHandleIntent(intent);
		return START_NOT_STICKY;
	}
	
	private void onQueueComplete() {
		log("onQueueComplete");
		log("Stopping foreground!");
		log("stopping service! success: " + successCount + " total: " + totalToDownload);
		megaApi.resetTotalDownloads();
		
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}
		
		if ((successCount + errorCount) > 0){
			if (!isOffline){
				if (successCount == 0) {
					log("stopping service!2");
					showCompleteFailNotification();
				} else {
					log("stopping service!");
					showCompleteSuccessNotification();
				}
			}
		}
		
		long totalFromSparse = 0;
		for (int i=0; i<transfersDownloadedSize.size(); i++){
			totalFromSparse += transfersDownloadedSize.valueAt(i);
		}
		
		log("totalSizeDownloaded: " + totalSizeDownloaded + "______ TOTALFROMSPARSE: " + totalFromSparse);
		
		log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
	}
	
	protected void onHandleIntent(final Intent intent) {
		log("onHandleIntent");
		
		updateProgressNotification(totalSizeDownloaded);
		
		long hash = intent.getLongExtra(EXTRA_HASH, -1);
		String url = intent.getStringExtra(EXTRA_URL);
		isOffline = intent.getBooleanExtra(EXTRA_OFFLINE, false);
		isFolderLink = intent.getBooleanExtra(EXTRA_FOLDER_LINK, false);
		fromContactFile = intent.getBooleanExtra(EXTRA_CONTACT_ACTIVITY, false);
		if (isFolderLink){
			megaApi = app.getMegaApiFolder();
		}
		else{
			megaApi = app.getMegaApi();
		}
		
		MegaNode document = megaApi.getNodeByHandle(hash);
	
		if((document == null) && (url == null)){
			log("Node not found");
			return;
		}
		
		if(url != null){
			log("Public node");
			currentDir = new File(intent.getStringExtra(EXTRA_PATH));
			megaApi.getPublicNode(url, this);
			return;
		}
		
		currentDir = getDir(document, intent);
		if (currentDir.isDirectory()){
			currentFile = new File(currentDir, document.getName());
		}
		else{
			currentFile = currentDir;
		}
		log("dir: " + currentDir.getAbsolutePath() + " file: " + document.getName() + "  Size: " + document.getSize());
		if(!checkCurrentFile(document)){
			log("checkCurrentFile == false");
			
			if (currentTransfers.size() == 0){
				successCount = transfersOK.size();
				errorCount = transfersError.size();
				onQueueComplete();
			}

			return;
		}
		
		if(!wl.isHeld()){ 
			wl.acquire();
		}
		if(!lock.isHeld()){
			lock.acquire();
		}
		
		if (currentDir.isDirectory()){
			log("To download(dir): " + currentDir.getAbsolutePath() + "/");
			megaApi.startDownload(document, currentDir.getAbsolutePath() + "/", this);
		}
		else{
			log("currentDir is not a directory");
		}
//		else{
//			log("To download(file): " + currentDir.getAbsolutePath());
//			megaApi.startDownload(document, currentDir.getAbsolutePath(), this);
//		}
	}
	
	private File getDir(MegaNode document, Intent intent) {
		boolean toDownloads = (intent.hasExtra(EXTRA_PATH) == false);
		File destDir;
		if (toDownloads) {
			destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		} else {
			destDir = new File(intent.getStringExtra(EXTRA_PATH));
		}
		log("save to: " + destDir.getAbsolutePath());
		return destDir;
	}
	
	boolean checkCurrentFile(MegaNode document)	{
		if(currentFile.exists() && (document.getSize() == currentFile.length())){
			
			currentFile.setReadable(true, false);
			Toast.makeText(getApplicationContext(), document.getName() + " already downloaded", Toast.LENGTH_SHORT).show();
			
			return false;
		}
		
		if(document.getSize() > ((long)1024*1024*1024*4))
		{
			log("show size alert: " + document.getSize());
	    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb), 
	    			Toast.LENGTH_LONG).show();
	    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb), 
	    			Toast.LENGTH_LONG).show();
	    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb), 
	    			Toast.LENGTH_LONG).show();
		}
		return true;
	}
	
	/*
	 * Show download fail notification
	 */
	private void showCompleteFailNotification() {		
		log("showCompleteFailNotification");
		String title = getString(R.string.download_failed);
		String message = getString(R.string.error_server_connection_problem);
//		if(lastError != 0) message = MegaError.getErrorString(lastError);

		Intent intent = new Intent(DownloadService.this, ManagerActivity.class);
		
		
		mBuilderCompat
				.setSmallIcon(R.drawable.ic_stat_notify_download)
				.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
				.setAutoCancel(true).setContentTitle(title)
				.setContentText(message)
				.setOngoing(false);

		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}
	
	/*
	 * Show download success notification
	 */
	private void showCompleteSuccessNotification() {
		log("showCompleteSuccessNotification");
		String notificationTitle, size;

		notificationTitle = successCount
				+ " "
				+ getResources().getQuantityString(R.plurals.general_num_files,
						successCount) + " "
				+ getString(R.string.download_downloaded);
		size = getString(R.string.general_total_size) + " "
				+ Formatter.formatFileSize(DownloadService.this, totalSizeToDownload);

		Intent intent = null;
		if(successCount != 1)
		{		
			intent = new Intent(getApplicationContext(), ManagerActivity.class);
		}
		else
		{
			if (MimeType.typeForName(currentFile.getName()).isPdf()){
				
				if (fromContactFile){
					Intent intentPdf = new Intent(this, ContactFileListActivity.class);
					intentPdf.setAction(ManagerActivity.ACTION_OPEN_PDF);
					intentPdf.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intentPdf.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					intentPdf.putExtra(ManagerActivity.EXTRA_PATH_PDF, currentFile.getAbsolutePath());			    
				    startActivity(intentPdf);
				}
				else{
					Intent intentPdf = new Intent(this, ManagerActivity.class);
					intentPdf.setAction(ManagerActivity.ACTION_OPEN_PDF);
					intentPdf.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intentPdf.putExtra(ManagerActivity.EXTRA_PATH_PDF, currentFile.getAbsolutePath());			    
				    startActivity(intentPdf);				
				}
			}
			else if (MimeType.typeForName(currentFile.getName()).isZip()){
				log("Download success of zip file!");
				
				Intent intentZip = new Intent(this, ManagerActivity.class);
				intentZip.setAction(ManagerActivity.ACTION_EXPLORE_ZIP);				
				intentZip.putExtra(ManagerActivity.EXTRA_PATH_ZIP, currentFile.getAbsolutePath());			    
			    startActivity(intentZip);
				log("Lanzo intent al manager.....");
			}
			else if (MimeType.typeForName(currentFile.getName()).isDocument()){
				Intent viewIntent = new Intent(Intent.ACTION_VIEW);
				viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
				viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				if (isIntentAvailable(this, viewIntent))
					startActivity(viewIntent);
				else{
					Intent intentShare = new Intent(Intent.ACTION_SEND);
					intentShare.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
					intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intentShare);
				}
			}
			log("Current File: " + currentFile.getAbsolutePath());
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName())
					.getType());
			
			if (!isIntentAvailable(DownloadService.this, intent)){
				intent.setAction(Intent.ACTION_SEND);
				intent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName())
						.getType());
			}
				
		}
		
		mBuilderCompat
		.setSmallIcon(R.drawable.ic_stat_notify_download)
		.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
		.setAutoCancel(true).setTicker(notificationTitle)
		.setContentTitle(notificationTitle).setContentText(size)
		.setOngoing(false);

		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}
	
	/*
	 * Update notification download progress
	 */
	@SuppressLint("NewApi")
	private void updateProgressNotification(final long progress) {
		log("updateProgressNotification");
		int progressPercent = (int) Math.round((double) progress / totalSizeToDownload
				* 100);
		log(progressPercent + " " + progress + " " + totalSizeToDownload);
		int left = totalToDownload - (totalDownloaded + totalDownloadedError);
		int current = totalToDownload - left + 1;
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		
		String message = "";
		if (totalToDownload == 0){
			message = getString(R.string.download_preparing_files);
		}
		else{
			message = getString(R.string.download_downloading) + " "
					+ current + " ";
			if (totalToDownload == 1) {
				message += getResources().getQuantityString(
						R.plurals.general_num_files, 1);
			} 
			else {
				message += getString(R.string.general_x_of_x)
						+ " "
						+ totalToDownload;
				
				if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB)
				{
					message += " "
						+ getResources().getQuantityString(
								R.plurals.general_num_files, totalToDownload);
				}
			}
		}

		Intent intent = new Intent(DownloadService.this, ManagerActivity.class);
		intent.setAction(ManagerActivity.ACTION_CANCEL_DOWNLOAD);	
		String info = Util.getProgressSize(DownloadService.this, progress, totalSizeToDownload);

		PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, 0);
		Notification notification = null;
		
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			mBuilder
				.setSmallIcon(R.drawable.ic_stat_notify_download)
				.setProgress(100, progressPercent, false)
				.setContentIntent(pendingIntent)
				.setOngoing(true).setContentTitle(message).setContentInfo(info)
				.setContentText(getString(R.string.download_touch_to_cancel))
				.setOnlyAlertOnce(true);
			notification = mBuilder.getNotification();
		}
		else
		{
			notification = new Notification(R.drawable.ic_stat_notify_download, null, 1);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
			notification.contentIntent = pendingIntent;
			notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_notify_download);
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
	
	/*
	 * Cancel download
	 */
	private void cancel() {
		canceled = true;
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
	}
	
	/*
	 * If there is an application that can manage the Intent, returns true. Otherwise, false.
	 */
	public boolean isIntentAvailable(Context ctx, Intent intent) {

		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Download start: " + transfer.getFileName() + "_" + megaApi.getTotalDownloads());
		
		currentTransfers.put(transfer.getTag(), transfer);
		totalToDownload++;
		totalSizeToDownload += transfer.getTotalBytes();
		
		log("CURRENTTRANSFERS.SIZE = " + currentTransfers.size() + "___" + "TOTALTODOWNLOAD: " + totalToDownload + "___" + "TOTALSIZETODOWNLOAD: " + totalSizeToDownload + "____" + "TRANSFER.TAG: " + transfer.getTag());
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError error) {
		log("Download finished: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
		log("transfer.getPath:" + transfer.getPath());
		if (canceled) {
			if((lock != null) && (lock.isHeld()))
				try{ lock.release(); } catch(Exception ex) {}
			if((wl != null) && (wl.isHeld()))
				try{ wl.release(); } catch(Exception ex) {}
			
			log("Download cancelled: " + transfer.getFileName());
			File file = new File(transfer.getPath());
			file.delete();
			DownloadService.this.cancel();
		}		
		else{
			if (error.getErrorCode() == MegaError.API_OK) {
				log("Download OK: " + transfer.getFileName());
				log("DOWNLOADFILE: " + transfer.getPath());
				
				totalDownloaded++;
				currentTransfers.remove(transfer.getTag());
				transfersOK.put(transfer.getTag(), transfer);
				long currentSizeDownloaded = transfersDownloadedSize.get(transfer.getTag());
				totalSizeDownloaded += (transfer.getTotalBytes()-currentSizeDownloaded);
				transfersDownloadedSize.put(transfer.getTag(), transfer.getTotalBytes());
				
				File resultFile = new File(transfer.getPath());
				File treeParent = resultFile.getParentFile();
				while(treeParent != null)
				{
					treeParent.setReadable(true, false);
					treeParent.setExecutable(true, false);
					treeParent = treeParent.getParentFile();
				}
				resultFile.setReadable(true, false);
				resultFile.setExecutable(true, false);
				
				String filePath = transfer.getPath();

				Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				File f = new File(filePath);
			    Uri contentUri = Uri.fromFile(f);
			    mediaScanIntent.setData(contentUri);
			    this.sendBroadcast(mediaScanIntent);
			}
			else 
			{
				log("Download Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());

				if(error.getErrorCode() == MegaError.API_EINCOMPLETE){
					totalToDownload--;
					totalSizeToDownload -= transfer.getTotalBytes();
					Long currentSizeDownloaded = transfersDownloadedSize.get(transfer.getTag());
					if (currentSizeDownloaded != null){
						totalSizeDownloaded -= currentSizeDownloaded;
					}
					currentTransfers.remove(transfer.getTag());
					File file = new File(transfer.getPath());
					file.delete();
				}
				else{
					totalDownloadedError++;
					totalSizeDownloadedError += transfer.getTotalBytes();
					currentTransfers.remove(transfer.getTag());
					transfersError.put(transfer.getTag(), transfer);
					File file = new File(transfer.getPath());
					file.delete();
				}
			}
			log("CURRENTTRANSFERS: " + currentTransfers.size() + "___ OK: " + transfersOK.size() + "___ ERROR: " + transfersError.size());
			if (currentTransfers.size() == 0){
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
			DownloadService.this.cancel();
			return;
		}
		
		long currentSizeDownloaded = 0;
		if (transfersDownloadedSize.get(transfer.getTag()) != null){
			currentSizeDownloaded = transfersDownloadedSize.get(transfer.getTag());
		}
		totalSizeDownloaded += (transfer.getTransferredBytes()-currentSizeDownloaded);
		transfersDownloadedSize.put(transfer.getTag(), transfer.getTransferredBytes());
		
		final long bytes = transfer.getTransferredBytes();
		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
    	updateProgressNotification(totalSizeDownloaded);
		
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log(transfer.getPath() + "\nDownload Temporary Error: " + e.getErrorString() + "__" + e.getErrorCode());
		
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		
		if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			log("cancel_transfers received");
			if (e.getErrorCode() == MegaError.API_OK){
				megaApi.pauseTransfers(false, this);
				megaApi.resetTotalDownloads();
				totalSizeToDownload = 0;
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("pause_transfer false received");
			if (e.getErrorCode() == MegaError.API_OK){
				cancel();
			}
		}
		else{
			log("Public node received");
			if (e.getErrorCode() != MegaError.API_OK) {
				log("Public node error");
				return;
			}
			else {
				MegaNode node = request.getPublicNode().copy();
				
				if (currentDir.isDirectory()){
					currentFile = new File(currentDir, node.getName());
					log("node.getName(): " + node.getName());
					
				}
				else{
					currentFile = currentDir;
					log("CURREN");
				}

				log("Public node download launched");
				if(!wl.isHeld()) wl.acquire();
				if(!lock.isHeld()) lock.acquire();
				if (currentDir.isDirectory()){
					log("To downloadPublic(dir): " + currentDir.getAbsolutePath() + "/");
					megaApi.startPublicDownload(node, currentDir.getAbsolutePath() + "/", this);
				}
			}
		}
	}
	

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}
	
	public static void log(String log){
		Util.log("DownloadService", log);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate");
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}
}
