package mega.privacy.android.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.text.format.Formatter;
import android.util.SparseArray;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;


/*
 * Background service to download files
 */
public class DownloadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface{

	// Action to stop download
	public static String ACTION_CANCEL = "CANCEL_DOWNLOAD";
	public static String EXTRA_SIZE = "DOCUMENT_SIZE";
	public static String EXTRA_HASH = "DOCUMENT_HASH";
	public static String EXTRA_URL = "DOCUMENT_URL";
	public static String EXTRA_PATH = "SAVE_PATH";
	public static String EXTRA_FOLDER_LINK = "FOLDER_LINK";
	public static String EXTRA_OFFLINE = "IS_OFFLINE";
	public static String EXTRA_CONTACT_ACTIVITY = "CONTACT_ACTIVITY";
	public static String EXTRA_ZIP_FILE_TO_OPEN = "FILE_TO_OPEN";
	public static String EXTRA_OPEN_FILE = "OPEN_FILE";
	public static String EXTRA_CONTENT_URI = "CONTENT_URI";

	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";

	private int errorCount = 0;

	private boolean isForeground = false;
	private boolean canceled;

	private boolean isOffline = false;
	private boolean isFolderLink = false;
	private boolean fromContactFile = false;
	private String pathFileToOpen;
	private Uri contentUri;

	private boolean openFile = true;

	MegaApplication app;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;

	WifiLock lock;
	WakeLock wl;

	File currentFile;
	File currentDir;
	MegaNode currentDocument;

	DatabaseHandler dbH = null;

	int transfersCount = 0;

	HashMap<Long, Uri> storeToAdvacedDevices;

	private int notificationId = Constants.NOTIFICATION_DOWNLOAD;
	private int notificationIdFinal = Constants.NOTIFICATION_DOWNLOAD_FINAL;
	private NotificationCompat.Builder mBuilderCompat;
	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;

	MegaNode offlineNode;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate");

		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		megaApiFolder = app.getMegaApiFolder();

		isForeground = false;
		canceled = false;

		storeToAdvacedDevices = new HashMap<Long, Uri>();

		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
	public int onStartCommand(Intent intent, int flags, int startId){
		log("onStartCommand");

		if(intent == null){
			log("intent==null");
			return START_NOT_STICKY;
		}

		if (intent.getAction() != null){
			if (intent.getAction().equals(ACTION_CANCEL)){
				log("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD, this);
				megaApiFolder.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD, this);
				return START_NOT_STICKY;
			}
		}

		onHandleIntent(intent);
		return START_NOT_STICKY;
	}

    protected void onHandleIntent(final Intent intent) {
        log("onHandleIntent");

        long hash = intent.getLongExtra(EXTRA_HASH, -1);
        String url = intent.getStringExtra(EXTRA_URL);
        isOffline = intent.getBooleanExtra(EXTRA_OFFLINE, false);
        isFolderLink = intent.getBooleanExtra(EXTRA_FOLDER_LINK, false);
        fromContactFile = intent.getBooleanExtra(EXTRA_CONTACT_ACTIVITY, false);
        openFile = intent.getBooleanExtra(EXTRA_OPEN_FILE, true);
        if(intent.getStringExtra(EXTRA_CONTENT_URI)!=null){
            contentUri = Uri.parse(intent.getStringExtra(EXTRA_CONTENT_URI));
        }

        if(intent.getStringExtra(EXTRA_ZIP_FILE_TO_OPEN)!=null){
            pathFileToOpen = intent.getStringExtra(EXTRA_ZIP_FILE_TO_OPEN);
        }
        else{
            pathFileToOpen=null;
        }

        megaApi = app.getMegaApi();

        if (isFolderLink){
            currentDocument = megaApiFolder.getNodeByHandle(hash);
        }
        else{
            currentDocument = megaApi.getNodeByHandle(hash);
        }

        if((currentDocument == null) && (url == null)){
            log("Node not found");
            return;
        }

        if(url != null){
            log("Public node");
            currentDir = new File(intent.getStringExtra(EXTRA_PATH));
            if (currentDir != null){
                currentDir.mkdirs();
            }
            megaApi.getPublicNode(url, this);
            return;
        }

        currentDir = getDir(currentDocument, intent);
        currentDir.mkdirs();
        if (currentDir.isDirectory()){
            log("currentDir is Directory");
            currentFile = new File(currentDir, megaApi.escapeFsIncompatible(currentDocument.getName()));
        }
        else{
            log("currentDir is File");
            currentFile = currentDir;
        }
        log("dir: " + currentDir.getAbsolutePath() + " file: " + currentDocument.getName() + "  Size: " + currentDocument.getSize());
        if(!checkCurrentFile(currentDocument)){
            log("checkCurrentFile == false");

            if (megaApi.getNumPendingDownloads() == 0){
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

        if(contentUri!=null){
            //To download to Advanced Devices
            log("Download to advanced devices checked");
            currentDir = new File(intent.getStringExtra(EXTRA_PATH));
            currentDir.mkdirs();

            if (currentDir.isDirectory()){
                log("To download(dir): " + currentDir.getAbsolutePath() + "/");
            }
            else{
                log("currentDir is not a directory");
            }
            storeToAdvacedDevices.put(currentDocument.getHandle(), contentUri);

            if (isFolderLink){
                megaApiFolder.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
            }
            else{
                megaApi.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
            }
        }
        else{
            if (currentDir.isDirectory()){
                log("To download(dir): " + currentDir.getAbsolutePath() + "/");

                if(currentFile.exists()){
                    log("The file already exists!");
                    //Check the fingerprint
                    String localFingerprint = megaApi.getFingerprint(currentFile.getAbsolutePath());
                    String megaFingerprint = megaApi.getFingerprint(currentDocument);

                    if((localFingerprint!=null) && (!localFingerprint.isEmpty()) && (megaFingerprint!=null) && (!megaFingerprint.isEmpty()))
                    {
                        if(localFingerprint.compareTo(megaFingerprint)!=0)
                        {
                            log("Delete the old version");
                            currentFile.delete();
                        }
                    }
                }

                if (currentDocument.isFolder()){
                    log("IS FOLDER_:_");
                }
                else{
                    log("IS FILE_:_");
                }

                if (isFolderLink){
                    if (megaApi.getRootNode() == null){
                        megaApiFolder.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
                        log("Root node is null");
                        return;
                    }

                    log("Root node is not null");
                    currentDocument = megaApiFolder.authorizeNode(currentDocument);
                    if (currentDocument == null){
                        log("CurrentDocument is null");
                        megaApiFolder.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
                        return;
                    }
                }

                log("CurrentDocument is not null");
                megaApi.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);

            }
            else{
                log("currentDir is not a directory");
            }
        }
    }

	private void onQueueComplete() {
		log("onQueueComplete");

		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

        showCompleteNotification();

		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();

		int total = megaApi.getNumPendingUploads() + megaApi.getNumPendingDownloads();
		log("onQueueComplete: total of files before reset " + total);
		if(total <= 0){
			log("onQueueComplete: reset total uploads/downloads");
			megaApi.resetTotalUploads();
			megaApi.resetTotalDownloads();
			errorCount = 0;
		}
	}


	private File getDir(MegaNode document, Intent intent) {
		log("getDir");
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
		log("checkCurrentFile");
		if(currentFile.exists() && (document.getSize() == currentFile.length())){

			currentFile.setReadable(true, false);
//			Toast.makeText(getApplicationContext(), document.getName() + " " +  getString(R.string.general_already_downloaded), Toast.LENGTH_SHORT).show();

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
	 * Show download success notification
	 */
	private void showCompleteNotification() {
		log("showCompleteNotification");
		String notificationTitle, size;

        int totalDownloads = megaApi.getTotalDownloads();
        notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification, totalDownloads, totalDownloads);

        if (errorCount > 0){
            size = getResources().getQuantityString(R.plurals.download_service_failed, errorCount, errorCount);
        }
        else{
            String totalBytes = Formatter.formatFileSize(DownloadService.this, megaApi.getTotalDownloadedBytes());
            size = getString(R.string.general_total_size, totalBytes);
        }

		Intent intent = null;
		if(totalDownloads != 1)
		{
			intent = new Intent(getApplicationContext(), ManagerActivityLollipop.class);

			log("Show notification");
			mBuilderCompat
			.setSmallIcon(R.drawable.ic_stat_notify_download)
			.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
			.setAutoCancel(true).setTicker(notificationTitle)
			.setContentTitle(notificationTitle).setContentText(size)
			.setOngoing(false);

			mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
		}
		else
		{
			if (openFile){
				log("openFile true");
				if (MimeTypeList.typeForName(currentFile.getName()).isZip()){
					log("Download success of zip file!");

					if(pathFileToOpen!=null){
						Intent intentZip;
						intentZip = new Intent(this, ZipBrowserActivityLollipop.class);
						intentZip.setAction(ZipBrowserActivityLollipop.ACTION_OPEN_ZIP_FILE);
						intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_ZIP_FILE_TO_OPEN, pathFileToOpen);
						intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, currentFile.getAbsolutePath());
						intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, currentDocument.getHandle());


						if(intentZip!=null){
							intentZip.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intentZip);
						}
					}
					else{
						Intent intentZip = null;

						intentZip = new Intent(this, ManagerActivityLollipop.class);
						intentZip.setAction(Constants.ACTION_EXPLORE_ZIP);
						intentZip.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intentZip.putExtra(Constants.EXTRA_PATH_ZIP, currentFile.getAbsolutePath());

						startActivity(intentZip);
					}

					log("Lanzo intent al manager.....");
				}
				else if (MimeTypeList.typeForName(currentFile.getName()).isDocument()){
					log("Download is document");

					Intent viewIntent = new Intent(Intent.ACTION_VIEW);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					}
					else{
						viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					}
					viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					if (MegaApiUtils.isIntentAvailable(this, viewIntent))
						startActivity(viewIntent);
					else{
						Intent intentShare = new Intent(Intent.ACTION_SEND);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						else{
							intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(intentShare);
					}
				}
				else if (MimeTypeList.typeForName(currentFile.getName()).isImage()){
					log("Download is IMAGE");

					Intent viewIntent = new Intent(Intent.ACTION_VIEW);
//					viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					}
					else{
						viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					}
					viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					if (MegaApiUtils.isIntentAvailable(this, viewIntent))
						startActivity(viewIntent);
					else{
						Intent intentShare = new Intent(Intent.ACTION_SEND);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						else{
							intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(intentShare);
					}
				}
				else{

					log("Download is OTHER FILE");
					intent = new Intent(Intent.ACTION_VIEW);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						intent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					}
					else{
						intent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
					}
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					if (!MegaApiUtils.isIntentAvailable(DownloadService.this, intent)){
						intent.setAction(Intent.ACTION_SEND);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							intent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						else{
							intent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}

					log("Show notification");
					mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify_download)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(notificationTitle)
					.setContentTitle(notificationTitle).setContentText(size)
					.setOngoing(false);

					mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
				}
			}
			else{
				openFile=true; //Set the openFile to the default

				intent = new Intent(getApplicationContext(), ManagerActivityLollipop.class);

				log("Show notification");
				mBuilderCompat
				.setSmallIcon(R.drawable.ic_stat_notify_download)
				.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
				.setAutoCancel(true).setTicker(notificationTitle)
				.setContentTitle(notificationTitle).setContentText(size)
				.setOngoing(false);

				mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
			}
		}
	}

	/*
	 * Update notification download progress
	 */
	@SuppressLint("NewApi")
	private void updateProgressNotification() {

		int pendingTransfers = megaApi.getNumPendingDownloads();
        int totalTransfers = megaApi.getTotalDownloads();

        long totalSizePendingTransfer = megaApi.getTotalDownloadBytes();
        long totalSizeTransferred = megaApi.getTotalDownloadedBytes();

        int progressPercent = (int) Math.round((double) totalSizeTransferred / totalSizePendingTransfer * 100);
        log("updateProgressNotification: "+progressPercent);

        String message = "";
        if (totalTransfers == 0){
            message = getString(R.string.download_preparing_files);
        }
        else{
            int inProgress = totalTransfers - pendingTransfers + 1;
            message = getResources().getQuantityString(R.plurals.download_service_notification, totalTransfers, inProgress, totalTransfers);
		}

		Intent intent;
		intent = new Intent(DownloadService.this, ManagerActivityLollipop.class);
		intent.setAction(Constants.ACTION_SHOW_TRANSFERS);

		String info = Util.getProgressSize(DownloadService.this, totalSizeTransferred, totalSizePendingTransfer);

		PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, 0);
		Notification notification = null;

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_notify_download)
					.setProgress(100, progressPercent, false)
					.setContentIntent(pendingIntent)
					.setOngoing(true).setContentTitle(message).setSubText(info)
					.setContentText(getString(R.string.download_touch_to_show))
					.setOnlyAlertOnce(true);
			notification = mBuilder.build();
		}
		else if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
				mBuilder
				.setSmallIcon(R.drawable.ic_stat_notify_download)
				.setProgress(100, progressPercent, false)
				.setContentIntent(pendingIntent)
				.setOngoing(true).setContentTitle(message).setContentInfo(info)
				.setContentText(getString(R.string.download_touch_to_show))
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

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Download start: " + transfer.getFileName() + "_" + megaApi.getTotalDownloads());

		transfersCount++;

        updateProgressNotification();
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError error) {
		log("onTransferFinish: " + transfer.getFileName());

		transfersCount--;

		if(!transfer.isFolderTransfer()){
			if(transfer.getState()==MegaTransfer.STATE_COMPLETED){
				String size = Util.getSizeString(transfer.getTotalBytes());
				AndroidCompletedTransfer completedTransfer = new AndroidCompletedTransfer(transfer.getFileName(), transfer.getType(), transfer.getState(), size, transfer.getNodeHandle()+"");
				dbH.setCompletedTransfer(completedTransfer);
			}

			updateProgressNotification();
		}

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

                //To update thumbnails for videos
                if(Util.isVideoFile(transfer.getPath())){
                    log("Is video!!!");
                    MegaNode videoNode = megaApi.getNodeByHandle(transfer.getNodeHandle());
                    if (videoNode != null){
                        if(!videoNode.hasThumbnail()){
                            log("The video has not thumb");
                            ThumbnailUtilsLollipop.createThumbnailVideo(this, transfer.getPath(), megaApi, transfer.getNodeHandle());
                        }
                        if(videoNode.getDuration()<1){
                            log("The video has not duration!!!");
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(transfer.getPath());
                            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            if(time!=null){
                                double seconds = Double.parseDouble(time)/1000;
                                log("The original duration is: "+seconds);
                                int secondsAprox = (int) Math.round(seconds);
                                log("The duration aprox is: "+secondsAprox);

                                megaApi.setNodeDuration(videoNode, secondsAprox, null);
                            }
                        }
                    }
                    else{
                        log("videoNode is NULL");
                    }
                }
                else{
                    log("NOT video!");
                }

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
				Uri contentUri;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					contentUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", f);
				}
				else{
					contentUri = Uri.fromFile(f);
				}
                mediaScanIntent.setData(contentUri);
                mediaScanIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                this.sendBroadcast(mediaScanIntent);

                if(storeToAdvacedDevices.containsKey(transfer.getNodeHandle())){
                    log("Now copy the file to the SD Card");
                    openFile=false;
                    Uri tranfersUri = storeToAdvacedDevices.get(transfer.getNodeHandle());
                    MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
                    alterDocument(tranfersUri, node.getName());
                }

                if(isOffline){
                    dbH = DatabaseHandler.getDbHandler(getApplicationContext());
                    offlineNode = megaApi.getNodeByHandle(transfer.getNodeHandle());

                    if(offlineNode!=null){
                        saveOffline(offlineNode, transfer.getPath());
                    }
                }
            }
            else
            {
                log("Download Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());

				if(!transfer.isFolderTransfer()){
					errorCount++;
				}

                if(error.getErrorCode() == MegaError.API_EINCOMPLETE){
                    File file = new File(transfer.getPath());
                    file.delete();
                }
                else{
                    File file = new File(transfer.getPath());
                    file.delete();
                }
            }
        }

        if (megaApi.getNumPendingDownloads() == 0 && transfersCount==0){
            onQueueComplete();
        }
	}

	private void alterDocument(Uri uri, String fileName) {
		log("alterUri");
	    try {

	    	String sourceLocation = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.advancesDevicesDIR + "/"+fileName;

	    	log("Gonna copy: "+sourceLocation);

	        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
	        FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

	    	InputStream in = new FileInputStream(sourceLocation);
//
//	        OutputStream out = new FileOutputStream(targetLocation);
//
	        // Copy the bits from instream to outstream
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	        	fileOutputStream.write(buf, 0, len);
	        }
	        in.close();
//	        out.close();


//	        fileOutputStream.write(("Overwritten by MyCloud at " + System.currentTimeMillis() + "\n").getBytes());
	        // Let the document provider know you're done by closing the stream.
	        fileOutputStream.close();
	        pfd.close();

	        File deleteTemp = new File(sourceLocation);
	        deleteTemp.delete();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	public void saveOffline (MegaNode node, String path){
		log("saveOffline");

		File destination = null;
		if (Environment.getExternalStorageDirectory() != null){
			destination = new File(path);
		}
		else{
			destination = getFilesDir();
		}

		destination.mkdirs();

		log("saveOffline: "+ destination.getAbsolutePath());

		Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
		if (node.getType() == MegaNode.TYPE_FOLDER) {
			log("saveOffline:isFolder");
			getDlList(dlFiles, node, new File(destination, new String(node.getName())));
		} else {
			log("saveOffline:isFile");
			dlFiles.put(node, destination.getAbsolutePath());
		}

		ArrayList<MegaNode> nodesToDB = new ArrayList<MegaNode>();

		for (MegaNode document : dlFiles.keySet()) {
			nodesToDB.add(document);
		}
		insertDB(nodesToDB);
	}

	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		log("getDlList");

		if (megaApi.getRootNode() == null)
			return;

		folder.mkdir();
		ArrayList<MegaNode> nodeList = megaApi.getChildren(parent);
		for(int i=0; i<nodeList.size(); i++){
			MegaNode document = nodeList.get(i);
			if (document.getType() == MegaNode.TYPE_FOLDER) {
				File subfolder = new File(folder, new String(document.getName()));
				getDlList(dlFiles, document, subfolder);
			}
			else {
				dlFiles.put(document, folder.getAbsolutePath());
			}
		}
	}

	private void insertDB (ArrayList<MegaNode> nodesToDB){
		log("insertDB");

		MegaNode parentNode = null;
		MegaNode nodeToInsert = null;

		String path = "/";
		MegaOffline mOffParent=null;
		MegaOffline mOffNode = null;

		parentNode = megaApi.getParentNode(nodeToInsert);

		for(int i=nodesToDB.size()-1; i>=0; i--){

			nodeToInsert = nodesToDB.get(i);
			log("Node to insert: "+nodeToInsert.getName());

			//If I am the owner
			if (megaApi.checkAccess(nodeToInsert, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){

				if(megaApi.getParentNode(nodeToInsert).getType() != MegaNode.TYPE_ROOT){

					parentNode = megaApi.getParentNode(nodeToInsert);
					log("ParentNode: "+parentNode.getName());
					log("PARENT NODE nooot ROOT");

					path = MegaApiUtils.createStringTree(nodeToInsert, this);
					if(path==null){
						path="/";
					}
					else{
						path="/"+path;
					}
					log("PAth node to insert: --- "+path);
					//Get the node parent
					mOffParent = dbH.findByHandle(parentNode.getHandle());
					//If the parent is not in the DB
					//Insert the parent in the DB
					if(mOffParent==null){
						if(parentNode!=null){
							insertParentDB(parentNode);
						}
					}

					mOffNode = dbH.findByHandle(nodeToInsert.getHandle());
					mOffParent = dbH.findByHandle(parentNode.getHandle());
					if(mOffNode == null){

						if(mOffParent!=null){
							if(nodeToInsert.isFile()){
								MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FILE,false, "-1");
								long checkInsert=dbH.setOfflineFile(mOffInsert);
								log("Test insert A: "+checkInsert);
							}
							else{
								MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FOLDER, false, "-1");
								long checkInsert=dbH.setOfflineFile(mOffInsert);
								log("Test insert B: "+checkInsert);
							}
						}
					}

				}
				else{
					path="/";

					if(nodeToInsert.isFile()){
						MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(),-1, DB_FILE, false, "-1");
						long checkInsert=dbH.setOfflineFile(mOffInsert);
						log("Test insert C: "+checkInsert);
					}
					else{
						MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FOLDER, false, "-1");
						long checkInsert=dbH.setOfflineFile(mOffInsert);
						log("Test insert D: "+checkInsert);
					}

				}

			}
			else{
				//If I am not the owner

				log("Im not the owner: "+megaApi.getParentNode(nodeToInsert));

//				if(megaApi.getParentNode(nodeToInsert).getType() != MegaNode.TYPE_ROOT){

				parentNode = megaApi.getParentNode(nodeToInsert);
				log("ParentNode: "+parentNode.getName());

				path = MegaApiUtils.createStringTree(nodeToInsert, this);
				if(path==null){
					path="/";
				}
				else{
					path="/"+path;
				}

				log("PAth node to insert: --- "+path);
				//Get the node parent
				mOffParent = dbH.findByHandle(parentNode.getHandle());
				//If the parent is not in the DB
				//Insert the parent in the DB
				if(mOffParent==null){
					if(parentNode!=null){
						insertIncomingParentDB(parentNode);
					}
				}

				mOffNode = dbH.findByHandle(nodeToInsert.getHandle());
				mOffParent = dbH.findByHandle(parentNode.getHandle());

				String handleIncoming = "";
				if(parentNode!=null){
					MegaNode ownerNode = megaApi.getParentNode(parentNode);
					if(ownerNode!=null){
						MegaNode nodeWhile = ownerNode;
						while (nodeWhile!=null){
							ownerNode=nodeWhile;
							nodeWhile = megaApi.getParentNode(nodeWhile);
						}

						handleIncoming=Long.toString(ownerNode.getHandle());
					}
					else{
						handleIncoming=Long.toString(parentNode.getHandle());
					}

				}

				if(mOffNode == null){
					log("Inserto el propio nodo: "+ nodeToInsert.getName() + "handleIncoming: "+handleIncoming);

					if(mOffParent!=null){
						if(nodeToInsert.isFile()){
							MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FILE,true, handleIncoming);
							long checkInsert=dbH.setOfflineFile(mOffInsert);
							log("Test insert A: "+checkInsert);
						}
						else{
							MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FOLDER, true, handleIncoming);
							long checkInsert=dbH.setOfflineFile(mOffInsert);
							log("Test insert B: "+checkInsert);
						}
					}
				}
//				}
//				else{
//					path="/";
//
//					if(nodeToInsert.isFile()){
//						MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(),-1, DB_FILE, true);
//						long checkInsert=dbH.setOfflineFile(mOffInsert);
//						log("Test insert C: "+checkInsert);
//					}
//					else{
//						MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FOLDER, true);
//						long checkInsert=dbH.setOfflineFile(mOffInsert);
//						log("Test insert D: "+checkInsert);
//					}
//				}
			}
		}
	}

	//Insert for incoming

	private void insertIncomingParentDB (MegaNode parentNode){
		log("insertIncomingParentDB: Check SaveOffline: "+parentNode.getName());

		MegaOffline mOffParentParent = null;
		String path=MegaApiUtils.createStringTree(parentNode, this);
		if(path==null){
			path="/";
		}
		else{
			path="/"+path;
		}

		log("PATH   IncomingParentDB: "+path);

		MegaNode parentparentNode = megaApi.getParentNode(parentNode);

		if(parentparentNode==null){

			if(parentNode.isFile()){
				MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, true, Long.toString(parentNode.getHandle()));
				long checkInsert=dbH.setOfflineFile(mOffInsert);
				log("Test insert C: "+checkInsert);
			}
			else{
				MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, true, Long.toString(parentNode.getHandle()));
				long checkInsert=dbH.setOfflineFile(mOffInsert);
				log("Test insert D: "+checkInsert);
			}
		}
		else{

			String handleIncoming = "";

			MegaNode ownerNode = megaApi.getParentNode(parentparentNode);
			if(ownerNode!=null){
				MegaNode nodeWhile = ownerNode;
				while (nodeWhile!=null){
					ownerNode=nodeWhile;
					nodeWhile = megaApi.getParentNode(nodeWhile);
				}

				handleIncoming=Long.toString(ownerNode.getHandle());
			}
			else{
				handleIncoming=Long.toString(parentparentNode.getHandle());
			}


			mOffParentParent = dbH.findByHandle(parentparentNode.getHandle());
			if(mOffParentParent==null){
				insertIncomingParentDB(megaApi.getParentNode(parentNode));
				//Insert the parent node
				mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
				if(mOffParentParent==null){
					insertIncomingParentDB(megaApi.getParentNode(parentNode));

				}
				else{

					if(parentNode.isFile()){
						MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, true, handleIncoming);
						long checkInsert=dbH.setOfflineFile(mOffInsert);
						log("Test insert E: "+checkInsert);
					}
					else{
						MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, true, handleIncoming);
						long checkInsert=dbH.setOfflineFile(mOffInsert);
						log("Test insert F: "+checkInsert);
					}
				}
			}
			else{

				if(parentNode.isFile()){
					MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, true, handleIncoming);
					long checkInsert=dbH.setOfflineFile(mOffInsert);
					log("Test insert G: "+checkInsert);
				}
				else{
					MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, true, handleIncoming);
					long checkInsert=dbH.setOfflineFile(mOffInsert);
					log("Test insert H: "+checkInsert);
				}
			}
		}
//		else{
//			log("---------------PARENT NODE ROOT------");
//			if(parentNode.isFile()){
//				MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, false);
//				long checkInsert=dbH.setOfflineFile(mOffInsert);
//				log("Test insert I: "+checkInsert);
//			}
//			else{
//				MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, false);
//				long checkInsert=dbH.setOfflineFile(mOffInsert);
//				log("Test insert J: "+checkInsert);
//			}
//		}
	}


	private void insertParentDB (MegaNode parentNode){
		log("insertParentDB: Check SaveOffline: "+parentNode.getName());

		MegaOffline mOffParentParent = null;
		String path=MegaApiUtils.createStringTree(parentNode, this);
		if(path==null){
			path="/";
		}
		else{
			path="/"+path;
		}

		MegaNode parentparentNode = megaApi.getParentNode(parentNode);
		if(parentparentNode==null){
			log("return insertParentDB");
			return;
		}

		if(parentparentNode.getType() != MegaNode.TYPE_ROOT){


			if(parentparentNode.getHandle()==megaApi.getInboxNode().getHandle()){
				log("En algun momento!!!");
				log("---------------PARENT NODE INBOX------");
				if(parentNode.isFile()){
					MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, false, "-1");
					long checkInsert=dbH.setOfflineFile(mOffInsert);
					log("Test insert M: "+checkInsert);
				}
				else{
					MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, false, "-1");
					long checkInsert=dbH.setOfflineFile(mOffInsert);
					log("Test insert N: "+checkInsert);
				}
				return;
			}

			mOffParentParent = dbH.findByHandle(parentparentNode.getHandle());
			if(mOffParentParent==null){
				log("mOffParentParent==null");
				insertParentDB(megaApi.getParentNode(parentNode));
				//Insert the parent node
				mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
				if(mOffParentParent==null){
					log("call again");
					insertParentDB(megaApi.getParentNode(parentNode));

				}
				else{
					log("second check NOOOTTT mOffParentParent==null");
					if(parentNode.isFile()){
						MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, false, "-1");
						long checkInsert=dbH.setOfflineFile(mOffInsert);
						log("Test insert I: "+checkInsert);
					}
					else{
						MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, false, "-1");
						long checkInsert=dbH.setOfflineFile(mOffInsert);
						log("Test insert J: "+checkInsert);
					}
				}
			}
			else{
				log("NOOOTTT mOffParentParent==null");
				if(parentNode.isFile()){
					MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, false, "-1");
					long checkInsert=dbH.setOfflineFile(mOffInsert);
					log("Test insert K: "+checkInsert);
				}
				else{
					MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, false, "-1");
					long checkInsert=dbH.setOfflineFile(mOffInsert);
					log("Test insert L: "+checkInsert);
				}
			}
		}
		else{
			log("---------------PARENT NODE ROOT------");
			if(parentNode.isFile()){
				MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, false, "-1");
				long checkInsert=dbH.setOfflineFile(mOffInsert);
				log("Test insert M: "+checkInsert);
			}
			else{
				MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, false, "-1");
				long checkInsert=dbH.setOfflineFile(mOffInsert);
				log("Test insert N: "+checkInsert);
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
		if(!transfer.isFolderTransfer()){
			updateProgressNotification();
		}
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log(transfer.getPath() + "\nDownload Temporary Error: " + e.getErrorString() + "__" + e.getErrorCode());

		if(e.getErrorCode() == MegaError.API_EOVERQUOTA) {
			log("API_EOVERQUOTA error!!");

			Intent intent = null;
			intent = new Intent(this, ManagerActivityLollipop.class);
			intent.setAction(Constants.ACTION_OVERQUOTA_TRANSFER);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");

		if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
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
				MegaNode node = request.getPublicMegaNode();

				if(node!=null){
					if (currentDir.isDirectory()){
						currentFile = new File(currentDir, megaApi.escapeFsIncompatible(node.getName()));
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
						megaApi.startDownload(node, currentDir.getAbsolutePath() + "/", this);
					}
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
