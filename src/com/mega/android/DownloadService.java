package com.mega.android;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.widget.RemoteViews;
import android.widget.Toast;

/*
 * Background service to download files
 */
public class DownloadService extends Service implements MegaTransferListenerInterface{

	// Action to stop download
	public static String ACTION_CANCEL = "CANCEL_DOWNLOAD";
	public static String EXTRA_SIZE = "DOCUMENT_SIZE";
	public static String EXTRA_HASH = "DOCUMENT_HASH";
	public static String EXTRA_URL = "DOCUMENT_URL";
	public static String EXTRA_PATH = "SAVE_PATH";
	
	private LinkedList<Intent> intentQueue;
	private int totalCount = 0;
	private int successCount = 0;
	private long totalSize = 0;
	private long downloadedSize = 0;
	
	private int lastError = 0;
	
	private boolean isForeground = false;
	static public boolean isProcessingIntent = false;
	private boolean canceled;

	MegaApplication app;
	MegaApiAndroid megaApi;
	Handler guiHandler;
	
	WifiLock lock;
	WakeLock wl;
	
	Intent currentIntent;
	int currentTryCount;
	File currentFile;
	File currentDir;
	
	private int notificationId = 2;
	private NotificationCompat.Builder mBuilderCompat;
	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate");
		
		intentQueue = new LinkedList<Intent>();
		totalCount = 0;
		successCount = 0;
		totalSize = 0;
		downloadedSize = 0;
		lastError = 0;
		
		isProcessingIntent = false;
		isForeground = false;
		canceled = false;

		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		guiHandler = new Handler();
		
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
		mBuilderCompat = new NotificationCompat.Builder(DownloadService.this);
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	public void onDestroy(){				
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		log("onStartCommand");
		
		if(intent == null){
			return START_NOT_STICKY;
		}
		
		if (intent.getAction() != null && intent.getAction().equals(ACTION_CANCEL)){
			log("Cancel intent");
			cancel();
			return START_NOT_STICKY;
		}
		
		intentQueue.add(intent);
		totalCount++;
		totalSize += intent.getLongExtra(EXTRA_SIZE, 0);
		
		if (isProcessingIntent) {
			return START_NOT_STICKY;
		}
		isProcessingIntent = true;
		
		processQueue();
		updateProgressNotification(downloadedSize);
		return START_NOT_STICKY;
	}
	
	/*
	 * Start next item downloading if necessary
	 */
	private void processQueue() {
		guiHandler.post(new Runnable()
		{
			public void run()
			{
				log("processQueue");
				Intent intent = intentQueue.pollFirst();
				if (intent == null) {
					onQueueComplete();
				} else {
					onHandleIntent(intent, 0);
				}
			}
		});
	}
	
	private void onQueueComplete() {
		log("onQueueComplete");
		stopForeground(true);
		isForeground = false;
		log("Stopping foreground!");
		log("stopping service! success: " + successCount + " total: " + totalCount);
		if (successCount == 0) {
			log("stopping service!2");
			showCompleteFailNotification();
		} else {
			log("stopping service!");
			showCompleteSuccessNotification();
		}
		log("stopping service!");
		isProcessingIntent = false;
		stopSelf();
	}
	
	protected void onHandleIntent(final Intent intent, final int tryCount) {
		log("onHandleIntent");
		updateProgressNotification(downloadedSize);
		currentIntent = intent;
		currentTryCount = tryCount;
		
		long hash = currentIntent.getLongExtra(EXTRA_HASH, 0);
		String url = currentIntent.getStringExtra(EXTRA_URL);
		MegaNode document = megaApi.getNodeByHandle(hash);
		
		if((document == null) && (url == null)){
			log("Node not found");
			processQueue();
			return;
		}
		
//		if(url != null){
//			log("Public node");
//			megaApi.getPublicNode(url, publicNodeListener);
//			return;
//		}
		
		currentDir = getDir(document, currentIntent);
		currentFile = new File(currentDir, document.getName());
		log("dir: " + currentDir.getAbsolutePath() + " file: " + document.getName() + "  Size: " + document.getSize());
		if(!checkCurrentFile(document)){
			return;
		}
		
		if(!wl.isHeld()){ 
			wl.acquire();
		}
		if(!lock.isHeld()){
			lock.acquire();
		}
		
		megaApi.startDownload(document, currentDir.getAbsolutePath() + "/", this);
//		megaApi.startDownload(document, Environment.getExternalStorageDirectory() + "/", this);
	}
	
	/*
	 * Create new file to download to
	 */
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
//		file = new File(destDir, new String(document.getName()));
//		for (int i = 0;; i++) {
//			String name = i == 0 ? new String(document.getName()) : (i + 1) + "_"
//					+ new String(document.getName());
//			file = new File(destDir, name);
//			if (file.exists() && (file.length() != document.getSize())) {
//				continue;
//			}
//			break;
//		}
//		
//		log("save to " + file.getAbsolutePath());
//		return file;
	}
	
	boolean checkCurrentFile(MegaNode document)	{
		if(currentFile.exists() && (document.getSize() == currentFile.length()))
		{
			currentFile.setReadable(true, false);
			downloadedSize += document.getSize();
			log("size: " + downloadedSize);
			updateProgressNotification(downloadedSize);
			onDownloadComplete();
			return false;
		}
		
		if(document.getSize() > ((long)1024*1024*1024*4))
		{
			log("show size alert: " + document.getSize());
			guiHandler.post(new Runnable() {
			    public void run() {
			    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb), 
			    			Toast.LENGTH_LONG).show();
			    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb), 
			    			Toast.LENGTH_LONG).show();
			    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb), 
			    			Toast.LENGTH_LONG).show();
			    }
			});
		}
		return true;
	}
	
	/*
	 * Handle download file complete
	 */
	private void onDownloadComplete() {
		log("onDownloadComplete");
		successCount++;
		processQueue();
	}
	
	/*
	 * Handle download error. Try to retry if possible.
	 */
	private boolean onFailure(Intent intent, MegaError error, int tryCount) {
		log("onFailure " + tryCount);
		if (tryCount > 1) {
			return false;
		} else {
			lastError = error.getErrorCode();
			handleIntentDelayed(intent, tryCount + 1);
			return true;
		}
	}
	
	protected void handleIntentDelayed(final Intent intent, final int tryCount) {
		guiHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				onHandleIntent(intent, tryCount);
			}
		}, 1000);
	}
	
	/*
	 * Show download fail notification
	 */
	private void showCompleteFailNotification() {
		guiHandler.post(new Runnable()
		{
			public void run()
			{			
				log("showCompleteFailNotification");
				String title = getString(R.string.download_failed);
				String message = getString(R.string.error_server_connection_problem);
				if(lastError != 0) message = MegaError.getErrorString(lastError);
		
				Intent intent = new Intent(DownloadService.this, ManagerActivity.class);
				
				
				mBuilderCompat
						.setSmallIcon(R.drawable.ic_stat_notify_download)
						.setContentIntent(PendingIntent.getActivity(DownloadService.this, 0, intent, 0))
						.setAutoCancel(true).setContentTitle(title)
						.setContentText(message)
						.setOngoing(false);
		
				mNotificationManager.notify(notificationId, mBuilderCompat.build());
			}
		});
	}
	
	/*
	 * Show download success notification
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
								successCount) + " "
						+ getString(R.string.download_downloaded);
				size = getString(R.string.general_total_size) + " "
						+ Formatter.formatFileSize(DownloadService.this, totalSize);
		
				Intent intent = null;
				if(successCount != 1)
				{
					intent = new Intent(DownloadService.this, ManagerActivity.class);
				}
				else
				{
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
						.setContentIntent(PendingIntent.getActivity(DownloadService.this, 0, intent, 0))
						.setAutoCancel(true).setTicker(notificationTitle)
						.setContentTitle(notificationTitle).setContentText(size)
						.setOngoing(false);
		
				mNotificationManager.notify(notificationId, mBuilderCompat.build());
			}
		});
	}
	
	/*
	 * Update notification download progress
	 */
	@SuppressLint("NewApi")
	private void updateProgressNotification(final long progress) {
		guiHandler.post(new Runnable()
		{
			public void run()
			{
//				log("updateProgressNotification");
				int progressPercent = (int) Math.round((double) progress / totalSize
						* 100);
//				log(progressPercent + " " + progress + " " + totalSize);
				int left = intentQueue.size() + 1;
				int current = totalCount - left + 1;
				int currentapiVersion = android.os.Build.VERSION.SDK_INT;

				
				String message = getString(R.string.download_downloading) + " "
						+ current + " ";
				if (totalCount == 1) {
					message += getResources().getQuantityString(
							R.plurals.general_num_files, 1);
				} else {
					message += getString(R.string.general_x_of_x)
							+ " "
							+ totalCount;
					
					if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB)
					{
						message += " "
							+ getResources().getQuantityString(
									R.plurals.general_num_files, totalCount);
					}
				}

				Intent intent = new Intent(DownloadService.this, ManagerActivity.class);
				intent.setAction(ManagerActivity.ACTION_CANCEL_DOWNLOAD);	
				String info = Util.getProgressSize(DownloadService.this, progress, totalSize);

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
//					notification = mBuilder.build();
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
		});
	}
	
	/*
	 * Cancel download
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
	
	/*
	 * If there is an application that can manage the Intent, returns true. Otherwise, false.
	 */
	public boolean isIntentAvailable(Context ctx, Intent intent) {

		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	public static void log(String log){
		Util.log("DownloadService", log);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Download start");
		
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError error) {
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}
		
		log("Download finished: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
		log("transfer.getPath:" + transfer.getPath());
		if (canceled) {
			log("Download cancelled: " + transfer.getFileName());
			File file = new File(transfer.getPath());
			file.delete();
			guiHandler.removeCallbacksAndMessages(null);
			DownloadService.this.cancel();
		}		
		else{
			if (error.getErrorCode() == MegaError.API_OK) {
				log("Download OK: " + transfer.getFileName());
				downloadedSize += transfer.getTransferredBytes();
				log("DOWNLOADFILE: " + transfer.getPath());
				
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
				
				onDownloadComplete();
			}
			else 
			{
				log("Download Error: " + transfer.getFileName() + "_" + error.getErrorCode());
				lastError = error.getErrorCode();
				File file = new File(transfer.getPath());
				file.delete();
				if (onFailure(currentIntent, error, currentTryCount+1)) return;
				processQueue();
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
			guiHandler.removeCallbacksAndMessages(null);
			DownloadService.this.cancel();
			return;
		}
		
		final long bytes = transfer.getTransferredBytes();
//		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
    	updateProgressNotification(downloadedSize + bytes);
		
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log("Download Temporary Error");
		
	}
}
