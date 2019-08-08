package mega.privacy.android.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.Formatter;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.*;

/*
 * Service to Upload files
 */
public class UploadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface {

	public static String ACTION_CANCEL = "CANCEL_UPLOAD";
	public static String EXTRA_FILEPATH = "MEGA_FILE_PATH";
	public static String EXTRA_FOLDERPATH = "MEGA_FOLDER_PATH";
	public static String EXTRA_NAME = "MEGA_FILE_NAME";
	public static String EXTRA_LAST_MODIFIED = "MEGA_FILE_LAST_MODIFIED";
	public static String EXTRA_NAME_EDITED = "MEGA_FILE_NAME_EDITED";
	public static String EXTRA_SIZE = "MEGA_SIZE";
	public static String EXTRA_PARENT_HASH = "MEGA_PARENT_HASH";
	public static String EXTRA_UPLOAD_COUNT = "EXTRA_UPLOAD_COUNT";

	public static final int CHECK_FILE_TO_UPLOAD_UPLOAD = 1000;
	public static final int CHECK_FILE_TO_UPLOAD_COPY = 1001;
	public static final int CHECK_FILE_TO_UPLOAD_OVERWRITE = 1002;
	public static final int CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER = 1003;

	private int errorCount = 0;
	private int copiedCount = 0;
	private int childUploadSucceeded = 0;
	private int childUploadFailed = 0;

	private boolean isForeground = false;
	private boolean canceled;

    private MegaApplication app;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;

    private WifiLock lock;
    private WakeLock wl;
    private DatabaseHandler dbH = null;

	private int transfersCount = 0;

	private Notification.Builder mBuilder;
	private NotificationCompat.Builder mBuilderCompat;
	private NotificationManager mNotificationManager;

	private int notificationIdForFileUpload = Constants.NOTIFICATION_UPLOAD;
	private int notificationIdFinalForFileUpload = Constants.NOTIFICATION_UPLOAD_FINAL;
	private String notificationChannelIdForFileUpload = Constants.NOTIFICATION_CHANNEL_UPLOAD_ID;
	private String notificationChannelNameForFileUpload = Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME;

    private int notificationIdForFolderUpload = Constants.NOTIFICATION_UPLOAD_FOLDER;
    private int notificationIdFinalForFolderUpload = Constants.NOTIFICATION_UPLOAD_FINAL_FOLDER;
    private String notificationChannelIdForFolderUpload = Constants.NOTIFICATION_CHANNEL_UPLOAD_ID_FOLDER;
    private String notificationChannelNameForFolderUpload = Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME_FOLDER;

    private ExecutorService threadPool = Executors.newCachedThreadPool();

	private HashMap<String, String> transfersCopy;
    private HashMap<Integer, MegaTransfer> mapProgressFileTransfers;
    private HashMap<Integer, MegaTransfer> mapProgressFolderTransfers;
    private int totalFileUploadsCompleted = 0;
    private int totalFileUploadsCompletedSuccessfully = 0;
    private int totalFileUploads = 0;
    private int totalFolderUploadsCompleted = 0;
    private int totalFolderUploads = 0;
    private int totalFolderUploadsCompletedSuccessfully = 0;

	int totalUploads = 0;
    int uploadCount;
    int currentUpload;

	//0 - not overquota, not pre-overquota
	//1 - overquota
	//2 - pre-overquota
    private int isOverquota = 0;

    private int uploadedFileCount;

    @SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		log("onCreate");

		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();
		transfersCopy = new HashMap<String, String>();
		mapProgressFileTransfers = new HashMap();
        mapProgressFolderTransfers = new HashMap<>();
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		isForeground = false;
		canceled = false;
		isOverquota = 0;

		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaUploadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaUploadServicePowerLock:");

        mBuilder = new Notification.Builder(UploadService.this);
		mBuilderCompat = new NotificationCompat.Builder(UploadService.this, null);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onDestroy(){
		log("onDestroy");
        releaseLocks();

		if(megaApi != null) {
			megaApi.removeRequestListener(this);
            megaApi.removeTransferListener(this);
		}

        if (megaChatApi != null){
            megaChatApi.saveCurrentState();
        }

        resetUploadNumbers();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");
		canceled = false;

		if (intent == null) {
			return START_NOT_STICKY;
		}

		uploadCount = intent.getIntExtra(EXTRA_UPLOAD_COUNT, 0);

		if ((intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_CANCEL)) {
				log("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
				return START_NOT_STICKY;
			}
		}

		onHandleIntent(intent);
        log(currentUpload +" / " + uploadCount);
        if(currentUpload == uploadCount && uploadedFileCount != 0) {
            log("send message");
            Intent i = new Intent(this, ManagerActivityLollipop.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(Constants.SHOW_REPEATED_UPLOAD);
            String file = getResources().getQuantityString(R.plurals.new_general_num_files,uploadedFileCount,uploadedFileCount);
            String sShow = file + " " + getString(R.string.general_already_uploaded);
            i.putExtra("MESSAGE", sShow);
            startActivity(i);
            //reset
            currentUpload = 0;
            uploadedFileCount = 0;
        }
		return START_NOT_STICKY;
	}

	private synchronized void onHandleIntent(final Intent intent) {
		log("onHandleIntent");

		String action = intent.getAction();
		log("action is " + action);
		if(action != null){
            if(Constants.ACTION_CHILD_UPLOADED_OK.equals(action)){
                childUploadSucceeded++;
                return;
            }else if(Constants.ACTION_CHILD_UPLOADED_FAILED.equals(action)){
                childUploadFailed++;
                return;
            }
            if (Constants.ACTION_OVERQUOTA_STORAGE.equals(action)) {
                isOverquota = 1;
            }else if(Constants.ACTION_STORAGE_STATE_CHANGED.equals(action)){
                isOverquota = 0;
            }
            if (totalFileUploads > 0) {
                updateProgressNotification(false);
            }
            if (totalFolderUploads > 0) {
                updateProgressNotification(true);
            }
            return;
        }else {
            isOverquota = 0;
        }

        currentUpload ++;
		final File file = new File(intent.getStringExtra(EXTRA_FILEPATH));

		if(file!=null){
			log("File to manage: "+file.getAbsolutePath());
		}

		long parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, 0);
		String nameInMEGA = intent.getStringExtra(EXTRA_NAME);
		String nameInMEGAEdited = intent.getStringExtra(EXTRA_NAME_EDITED);
		long lastModified = intent.getLongExtra(EXTRA_LAST_MODIFIED, 0);
		if(lastModified <= 0){
		    lastModified = file.lastModified();
        }
		if (file.isDirectory()) {
            acquireLock();
            totalFolderUploads++;
			if (nameInMEGA != null){
                megaApi.startUpload(file.getAbsolutePath(),megaApi.getNodeByHandle(parentHandle),nameInMEGA,this);
			}
			else{
				megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), this);
			}
		}
		else {
			if (nameInMEGAEdited != null){
				switch (checkFileToUploadRenamed(file, parentHandle, nameInMEGAEdited)) {
					case CHECK_FILE_TO_UPLOAD_UPLOAD: {
						log("CHECK_FILE_TO_UPLOAD_UPLOAD");
                        acquireLock();
						totalFileUploads++;
                        totalUploads++;
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGAEdited, this);
						break;
					}
					case CHECK_FILE_TO_UPLOAD_COPY: {
						log("CHECK_FILE_TO_UPLOAD_COPY");
						break;
					}
					case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER: {
						log("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
						log("Return - file already uploaded");
						return;

					}
				}
			}
			else {
				switch (checkFileToUpload(file, parentHandle)) {
					case CHECK_FILE_TO_UPLOAD_UPLOAD: {
						log("CHECK_FILE_TO_UPLOAD_UPLOAD");

                        acquireLock();
						totalFileUploads++;

                        if (lastModified == 0) {
                            if (nameInMEGA != null) {
                                megaApi.startUpload(file.getAbsolutePath(),megaApi.getNodeByHandle(parentHandle),nameInMEGA,this);
                            } else {
                                megaApi.startUpload(file.getAbsolutePath(),megaApi.getNodeByHandle(parentHandle),this);
                            }
                        } else {
                            if (nameInMEGA != null) {
                                megaApi.startUpload(file.getAbsolutePath(),megaApi.getNodeByHandle(parentHandle),nameInMEGA,lastModified / 1000,this);
                            } else {
                                megaApi.startUpload(file.getAbsolutePath(),megaApi.getNodeByHandle(parentHandle),lastModified / 1000,this);
                            }
                        }

						break;
					}
					case CHECK_FILE_TO_UPLOAD_COPY: {
						log("CHECK_FILE_TO_UPLOAD_COPY");
						break;
					}
					case CHECK_FILE_TO_UPLOAD_OVERWRITE: {
						log("CHECK_FILE_TO_UPLOAD_OVERWRITE");
						MegaNode nodeExistsInFolder = megaApi.getNodeByPath(file.getName(), megaApi.getNodeByHandle(parentHandle));
						megaApi.remove(nodeExistsInFolder);

                        acquireLock();
						totalFileUploads++;

						if (nameInMEGA != null) {
							megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA, this);
						} else {
							megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), this);
						}
						break;
					}
					case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER: {
						log("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
                        uploadedFileCount++;
						log("Return - file already uploaded");
						return;
					}
				}
			}
		}
	}

	int checkFileToUploadRenamed (File file, long parentHandle, String nameInMEGAEdited) {
		MegaNode nodeEditedExistsInFolder = megaApi.getNodeByPath(nameInMEGAEdited, megaApi.getNodeByHandle(parentHandle));
		if (nodeEditedExistsInFolder == null){
			String localFingerPrint = megaApi.getFingerprint(file.getAbsolutePath());
			MegaNode nodeExists = megaApi.getNodeByFingerprint(localFingerPrint);
			if (nodeExists == null){
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
			}
			else if (nodeExists.getName().equals(nameInMEGAEdited)){
				transfersCopy.put(localFingerPrint, nameInMEGAEdited);
				megaApi.copyNode(nodeExists, megaApi.getNodeByHandle(parentHandle), this);
				return CHECK_FILE_TO_UPLOAD_COPY;
			}
			else {
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
			}
		}
		else{
			if (file.length() == nodeEditedExistsInFolder.getSize()){
				return CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER;
			}
			else{
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
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
				megaApi.copyNode(nodeExists, megaApi.getNodeByHandle(parentHandle), this);
				return CHECK_FILE_TO_UPLOAD_COPY;
			}
		}
		else{
			if (file.length() == nodeExistsInFolder.getSize()){
				return CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER;
			}
			else{
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
				//return CHECK_FILE_TO_UPLOAD_OVERWRITE;
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
		mNotificationManager.cancel(notificationIdForFileUpload);
		mNotificationManager.cancel(notificationIdForFolderUpload);
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
        releaseLocks();
        if (isOverquota != 0) {
            if (totalFileUploads > 0) {
                showStorageOverQuotaNotification(false);
            }
            if (totalFolderUploads > 0) {
                showStorageOverQuotaNotification(true);
            }
        } else {
            if (totalFileUploads > 0) {
                showFileUploadCompleteNotification();
            }
            if (totalFolderUploads > 0) {
                showFolderUploadCompleteNotification();
            }
        }

        if (megaApi.getNumPendingUploads() <= 0) {
            log("onQueueComplete: reset total uploads");
            megaApi.resetTotalUploads();
        }

        errorCount = 0;
        copiedCount = 0;

        resetUploadNumbers();

        log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
        isForeground = false;
        stopForeground(true);
        mNotificationManager.cancel(notificationIdForFileUpload);
        mNotificationManager.cancel(notificationIdForFolderUpload);
        stopSelf();
        log("after stopSelf");

        if (Util.isPermissionGranted(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			deleteCacheFolderIfEmpty(getApplicationContext(), TEMPORAL_FOLDER);
        }

	}

    private void notifyNotification(String notificationTitle,String size,int notificationId,String channelId,String channelName) {
        Intent intent = new Intent(UploadService.this,ManagerActivityLollipop.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,channelName,NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null,null);
            mNotificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(),channelId);

            mBuilderCompatO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(),0,intent,0))
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false);

            mNotificationManager.notify(notificationId,mBuilderCompatO.build());
        } else {
            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(),0,intent,0))
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false);

            mNotificationManager.notify(notificationId,mBuilderCompat.build());
        }
    }

    private long getTransferredByte(HashMap<Integer, MegaTransfer> map) {
        Collection<MegaTransfer> transfers = map.values();
        long transferredBytes = 0;
        for (Iterator iterator = transfers.iterator();iterator.hasNext();) {
            MegaTransfer currentTransfer = (MegaTransfer)iterator.next();
            transferredBytes = transferredBytes + currentTransfer.getTransferredBytes();
        }
        return transferredBytes;
    }

    /*
     * Show complete success notification
     */
    private void showFileUploadCompleteNotification() {
        log("showFileUploadCompleteNotification");
        if (isOverquota == 0) {
            String notificationTitle, size;
            int quantity = totalFileUploadsCompletedSuccessfully == 0 ? 1 : totalFileUploadsCompletedSuccessfully;
            notificationTitle = getResources().getQuantityString(R.plurals.upload_service_final_notification,quantity,totalFileUploadsCompletedSuccessfully);

            if (copiedCount > 0 && errorCount > 0) {
                String copiedString = getResources().getQuantityString(R.plurals.copied_service_upload,copiedCount,copiedCount);
                String errorString = getResources().getQuantityString(R.plurals.upload_service_failed,errorCount,errorCount);
                size = copiedString + ", " + errorString;
            } else if (copiedCount > 0) {
                size = getResources().getQuantityString(R.plurals.copied_service_upload,copiedCount,copiedCount);
            } else if (errorCount > 0) {
                size = getResources().getQuantityString(R.plurals.upload_service_failed,errorCount,errorCount);
            } else {
                long transferredBytes = getTransferredByte(mapProgressFileTransfers);
                String totalBytes = Formatter.formatFileSize(UploadService.this,transferredBytes);
                size = getString(R.string.general_total_size,totalBytes);
            }

            notifyNotification(notificationTitle,size,notificationIdFinalForFileUpload,notificationChannelIdForFileUpload,notificationChannelNameForFileUpload);
        }
    }

    private void showFolderUploadCompleteNotification() {
        log("showFolderUploadCompleteNotification");
        if (isOverquota == 0) {
            String notificationTitle = getResources().getQuantityString(R.plurals.folder_upload_service_final_notification,totalFolderUploadsCompletedSuccessfully,totalFolderUploadsCompletedSuccessfully);
            String notificationSubTitle;

            if (childUploadSucceeded > 0 && childUploadFailed > 0) {
                String uploadedString = getResources().getQuantityString(R.plurals.upload_service_final_notification,childUploadSucceeded,childUploadSucceeded);
                String errorString = getResources().getQuantityString(R.plurals.upload_service_failed,childUploadFailed,childUploadFailed);
                notificationSubTitle = uploadedString + ", " + errorString;
            } else if (childUploadSucceeded > 0) {
                notificationSubTitle = getResources().getQuantityString(R.plurals.upload_service_final_notification,childUploadSucceeded,childUploadSucceeded);
            } else if(childUploadFailed > 0){
                notificationSubTitle = getResources().getQuantityString(R.plurals.upload_service_failed,childUploadFailed,childUploadFailed);
            }else{
                long transferredBytes = getTransferredByte(mapProgressFolderTransfers);
                String totalBytes = Formatter.formatFileSize(UploadService.this,transferredBytes);
                notificationSubTitle = getString(R.string.general_total_size,totalBytes);
            }

            notifyNotification(notificationTitle,notificationSubTitle,notificationIdFinalForFolderUpload,notificationChannelIdForFolderUpload,notificationChannelNameForFolderUpload);
        }
    }

    private void notifyProgressNotification(int progressPercent,String message,String info,String actionString,int notificationId,String notificationChannelId,String notificationChannelName){
        Intent intent = new Intent(UploadService.this, ManagerActivityLollipop.class);
        switch (isOverquota) {
            case 0:
            default:
                intent.setAction(Constants.ACTION_SHOW_TRANSFERS);
                break;
            case 1:
                intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
                break;
            case 2:
                intent.setAction(Constants.ACTION_PRE_OVERQUOTA_STORAGE);
                break;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(UploadService.this, 0, intent, 0);
        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
            NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);
            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.mega))
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message).setSubText(info)
                    .setContentText(actionString)
                    .setOnlyAlertOnce(true);

            notification = mBuilderCompat.build();
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.mega))
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message).setSubText(info)
                    .setContentText(actionString)
                    .setOnlyAlertOnce(true);
            notification = mBuilder.build();
        }
        else {
            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message).setContentInfo(info)
                    .setContentText(actionString)
                    .setOnlyAlertOnce(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                mBuilder.setColor(ContextCompat.getColor(this,R.color.mega));
            }
            notification = mBuilder.build();
        }

        if (!isForeground) {
            log("starting foreground");
            try {
                startForeground(notificationId, notification);
                isForeground = true;
            }
            catch (Exception e){
                log("start foreground exception: " + e.getMessage());
                isForeground = false;
            }
        } else {
            mNotificationManager.notify(notificationId, notification);
        }
    }

    private void updateProgressNotification(boolean isFolderTransfer) {
        log("updateProgressNotification");
        Collection<MegaTransfer> transfers;
        if(isFolderTransfer){
            transfers = mapProgressFolderTransfers.values();
        }else{
            transfers = mapProgressFileTransfers.values();
        }

        UploadProgress up = getInProgressNotification(transfers);
        long total = up.total;
        long inProgress = up.inProgress;
        int progressPercent = 0;
        long inProgressTemp;
        if (total > 0) {
            inProgressTemp = inProgress * 100;
            progressPercent = (int)(inProgressTemp / total);
        }

        String message = getMessageForProgressNotification(inProgress,isFolderTransfer);
        String logMessage = isFolderTransfer ? "updateProgressNotificationForFolderUpload: " : "updateProgressNotificationForFileUpload: ";
        log(logMessage + progressPercent + " " + message);
        String actionString = isOverquota == 0 ? getString(R.string.download_touch_to_show) : getString(R.string.general_show_info);
        String info = getProgressSize(UploadService.this,inProgress,total);

        if(isFolderTransfer){
            notifyProgressNotification(progressPercent,message,info,actionString,notificationIdForFolderUpload,notificationChannelIdForFolderUpload,notificationChannelNameForFolderUpload);
        }else{
            notifyProgressNotification(progressPercent,message,info,actionString,notificationIdForFileUpload,notificationChannelIdForFileUpload,notificationChannelNameForFileUpload);
        }
    }

    private String getMessageForProgressNotification(long inProgress,boolean isFolderUpload) {
        log("getMessageForProgressNotification");
        String message;
        if (isOverquota != 0) {
            message = getString(R.string.overquota_alert_title);
        } else if (inProgress == 0) {
            message = getString(R.string.download_preparing_files);
        } else {
            int filesProgress;
            if(isFolderUpload){
                filesProgress = totalFolderUploadsCompleted + 1 > totalFolderUploads ? totalFolderUploads : totalFolderUploadsCompleted + 1;
                message = getResources().getQuantityString(R.plurals.folder_upload_service_notification,totalFolderUploads,filesProgress,totalFolderUploads);
            }else{
                filesProgress = totalFileUploadsCompleted + 1 > totalFileUploads ? totalFileUploads : totalFileUploadsCompleted + 1;
                message = getResources().getQuantityString(R.plurals.upload_service_notification,totalFileUploads,filesProgress,totalFileUploads);
            }
        }

        return message;
    }

    private UploadProgress getInProgressNotification(Collection<MegaTransfer> transfers){
        log("getInProgressNotification");
        UploadProgress progress = new UploadProgress();
        long total = 0;
        long inProgress = 0;

        for (Iterator iterator = transfers.iterator(); iterator.hasNext();) {
            MegaTransfer currentTransfer = (MegaTransfer) iterator.next();
            if(currentTransfer.getState()==MegaTransfer.STATE_COMPLETED){
                total = total + currentTransfer.getTotalBytes();
                inProgress = inProgress + currentTransfer.getTotalBytes();
            }
            else{
                total = total + currentTransfer.getTotalBytes();
                inProgress = inProgress + currentTransfer.getTransferredBytes();
            }
        }

        progress.setTotal(total);
        progress.setInProgress(inProgress);

        return progress;
    }

	public static void log(String log) {
		Util.log("UploadService", log);
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

		log("Upload start: " + transfer.getFileName());
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			String appData = transfer.getAppData();

			if(appData!=null){
				return;
			}

			if(isTransferBelongsToFolderTransfer(transfer)){
			    return;
            }

            transfersCount++;
            if(transfer.isFolderTransfer()){
                mapProgressFolderTransfers.put(transfer.getTag(), transfer);
                updateProgressNotification(true);
            }else{
                mapProgressFileTransfers.put(transfer.getTag(), transfer);
                updateProgressNotification(false);
            }
		}
	}

	@Override
	public void onTransferFinish(final MegaApiJava api, final MegaTransfer transfer, MegaError error) {
		log("onTransferFinish: path " + transfer.getPath() + " size " + transfer.getTransferredBytes());
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {

            if(isTransferBelongsToFolderTransfer(transfer)){
                return;
            }

            String appData = transfer.getAppData();
            if(appData!=null){
                return;
            }

            transfersCount--;
            if(transfer.isFolderTransfer()){
                totalFolderUploadsCompleted++;
                mapProgressFolderTransfers.put(transfer.getTag(), transfer);
            }else{
                totalFileUploadsCompleted++;
                mapProgressFileTransfers.put(transfer.getTag(), transfer);
                if (transfer.getState() == MegaTransfer.STATE_COMPLETED) {
                    String size = getSizeString(transfer.getTotalBytes());
                    AndroidCompletedTransfer completedTransfer = new AndroidCompletedTransfer(transfer.getFileName(), transfer.getType(), transfer.getState(), size, transfer.getNodeHandle() + "");
                    dbH.setCompletedTransfer(completedTransfer);
                }
            }

			if (canceled) {
				log("Upload canceled: " + transfer.getFileName());

                releaseLocks();
				UploadService.this.cancel();
				log("after cancel");
				deleteCacheFolderIfEmpty(getApplicationContext(), TEMPORAL_FOLDER);

			} else {
				if (error.getErrorCode() == MegaError.API_OK) {
					log("Upload OK: " + transfer.getFileName());
					if(transfer.isFolderTransfer()){
                        totalFolderUploadsCompletedSuccessfully++;
                    }else{
                        totalFileUploadsCompletedSuccessfully++;
                    }
					if (isVideoFile(transfer.getPath())) {
						log("Is video!!!");

						File previewDir = PreviewUtils.getPreviewFolder(this);
						File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						File thumbDir = ThumbnailUtils.getThumbFolder(this);
						File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
						megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

						MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());

						if (node != null) {
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
								catch (Exception e){
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
									catch (Exception e){
										log("Exception again, no chance to set coordinates of video");
									}
								}
							}
							else{
								log("No location info");
							}
						}
					} else if (MimeTypeList.typeForName(transfer.getPath()).isImage()) {
						log("Is image!!!");

						File previewDir = PreviewUtils.getPreviewFolder(this);
						File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						File thumbDir = ThumbnailUtils.getThumbFolder(this);
						File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
						megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

						MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
						if (node != null) {
							try {
								final ExifInterface exifInterface = new ExifInterface(transfer.getPath());
								float[] latLong = new float[2];
								if (exifInterface.getLatLong(latLong)) {
									log("Latitude: " + latLong[0] + " Longitude: " + latLong[1]);
									megaApi.setNodeCoordinates(node, latLong[0], latLong[1], null);
								}

							} catch (Exception e) {
								log("Couldn't read exif info: " + transfer.getPath());
							}
						}
					} else if (MimeTypeList.typeForName(transfer.getPath()).isPdf()) {
						log("Is pdf!!!");

						try {
							ThumbnailUtilsLollipop.createThumbnailPdf(this, transfer.getPath(), megaApi, transfer.getNodeHandle());
						} catch(Exception e) {
							log("Pdf thumbnail could not be created");
						}

						int pageNumber = 0;
						FileOutputStream out = null;

						try {
						PdfiumCore pdfiumCore = new PdfiumCore(this);
						MegaNode pdfNode = megaApi.getNodeByHandle(transfer.getNodeHandle());

						if (pdfNode == null){
							log("pdf is NULL");
							return;
						}

						File previewDir = PreviewUtils.getPreviewFolder(this);
						File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						File file = new File(transfer.getPath());

							PdfDocument pdfDocument = pdfiumCore.newDocument(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
							pdfiumCore.openPage(pdfDocument, pageNumber);
							int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
							int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
							Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
							pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
							Bitmap resizedBitmap = PreviewUtils.resizeBitmapUpload(bmp, width, height);
							out = new FileOutputStream(preview);
							boolean result = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
							if(result){
								log("Compress OK!");
								megaApi.setPreview(pdfNode, preview.getAbsolutePath());
							}
							else{
								log("Not Compress");
							}
							pdfiumCore.closeDocument(pdfDocument);
						} catch(Exception e) {
							log("Pdf preview could not be created");
						} finally {
							try {
								if (out != null)
									out.close();
							} catch (Exception e) {

							}
						}

					} else {
						log("NOT video, image or pdf!");
					}
				} else {
					log("Upload Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());

					if (error.getErrorCode() == MegaError.API_EOVERQUOTA) {
						isOverquota = 1;
					}
					else if (error.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
						isOverquota = 2;
					}

					if (!transfer.isFolderTransfer()) {
						errorCount++;
					}
				}

				String qrFileName = megaApi.getMyEmail() + "QRcode.jpg";

				File localFile = buildQrFile(getApplicationContext(),transfer.getFileName());
                if (isFileAvailable(localFile) && !localFile.getName().equals(qrFileName)) {
                    log("Delete file!: " + localFile.getAbsolutePath());
                    localFile.delete();
                }

                File tempPic = getCacheFolder(getApplicationContext(), TEMPORAL_FOLDER);
				log("IN Finish: " + transfer.getFileName() + "path? " + transfer.getPath());
				if (isFileAvailable(tempPic) && transfer.getPath() != null) {
					if (transfer.getPath().startsWith(tempPic.getAbsolutePath())) {
						File f = new File(transfer.getPath());
						f.delete();
					}
				} else {
					log("transfer.getPath() is NULL or temporal folder unavailable");
				}

				if (transfersCopy.isEmpty() && totalFileUploadsCompleted==totalFileUploads && transfersCount == 0) {
					onQueueComplete();
				} else{
                    updateProgressNotification(transfer.isFolderTransfer());
				}
			}
		}
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferUpdate");
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD){

            if(isTransferBelongsToFolderTransfer(transfer)){
                return;
            }

			String appData = transfer.getAppData();

			if(appData!=null){
				return;
			}

            if (canceled) {
                log("Transfer cancel: " + transfer.getFileName());
                releaseLocks();
                megaApi.cancelTransfer(transfer);
                UploadService.this.cancel();
                log("after cancel");
                return;
            }

            if(transfer.isFolderTransfer()){
                mapProgressFolderTransfers.put(transfer.getTag(), transfer);
                updateProgressNotification(true);
            }else{
                mapProgressFileTransfers.put(transfer.getTag(), transfer);
                updateProgressNotification(false);
            }
        }
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log("onTransferTemporaryError: " + e.getErrorString() + "__" + e.getErrorCode());

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
            if(isTransferBelongsToFolderTransfer(transfer)){
                return;
            }
			switch (e.getErrorCode())
			{
				case MegaError.API_EOVERQUOTA:
				case MegaError.API_EGOINGOVERQUOTA:
					if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
						isOverquota = 1;
					}
					else if (e.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
						isOverquota = 2;
					}

					if (e.getValue() != 0) {
						log("TRANSFER OVER QUOTA ERROR: " + e.getErrorCode());
					}
					else {
						log("STORAGE OVER QUOTA ERROR: " + e.getErrorCode());
                        if (totalFileUploads > 0) {
                            updateProgressNotification(false);
                        }
                        if (totalFolderUploads > 0) {
                            updateProgressNotification(true);
                        }
					}
					break;
			}
		}
	}

	private void showStorageOverQuotaNotification(boolean isFolderTransfer){
		log("showStorageOverQuotaNotification");
		String notificationChannelId,notificationChannelName;
		if(isFolderTransfer){
            notificationChannelId = notificationChannelIdForFolderUpload;
            notificationChannelName = notificationChannelNameForFolderUpload;
        }else{
            notificationChannelId = notificationChannelIdForFileUpload;
            notificationChannelName = notificationChannelNameForFileUpload;
        }

		String contentText = getString(R.string.download_show_info);
		String message = getString(R.string.overquota_alert_title);

		Intent intent = new Intent(this, ManagerActivityLollipop.class);
		if(isOverquota==1){
			intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
		}
		else{
			intent.setAction(Constants.ACTION_PRE_OVERQUOTA_STORAGE);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setShowBadge(true);
			channel.setSound(null, null);
			mNotificationManager.createNotificationChannel(channel);

			NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

			mBuilderCompatO
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompatO.build());
		}
		else {
			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompat.build());
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
		if (request.getType() == MegaRequest.TYPE_COPY){
            updateProgressNotification(false);
		}
	}

    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
        log("UPLOAD: onRequestFinish " + request.getRequestString());
        if (request.getType() == MegaRequest.TYPE_COPY) {
            log("TYPE_COPY finished");
            if (e.getErrorCode() == MegaError.API_OK) {
                copiedCount++;
                MegaNode n = megaApi.getNodeByHandle(request.getNodeHandle());
                if (n != null) {
                    String currentNodeName = n.getName();
                    String megaFingerPrint = megaApi.getFingerprint(n);
                    log("copy node");
                    String nameInMega = transfersCopy.get(megaFingerPrint);
                    if (nameInMega != null) {
                        if (nameInMega.compareTo(currentNodeName) != 0) {
                            megaApi.renameNode(n,nameInMega);
                        }
                    }
                    transfersCopy.remove(megaFingerPrint);

                    if (transfersCopy.isEmpty()) {
                        if (totalFileUploads == totalFileUploadsCompleted && transfersCount == 0) {
                            onQueueComplete();
                        }
                    }
                } else {
                    log("ERROR - node is NULL");
                }
            } else if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
                log("OVER QUOTA ERROR: " + e.getErrorCode());
                isOverquota = 1;
                onQueueComplete();
            } else if (e.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
                log("OVER QUOTA ERROR: " + e.getErrorCode());
                isOverquota = 2;
                onQueueComplete();
            } else {
                log("ERROR: " + e.getErrorCode());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
        log("onRequestTemporaryError: " + request.getName());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
        log("onRequestUpdate: " + request.getName());
    }

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}

	private void acquireLock(){
        log("acquireLock");
        if (!wl.isHeld()) {
            wl.acquire();
        }
        if (!lock.isHeld()) {
            lock.acquire();
        }
    }

	private void releaseLocks(){
        log("releaseLocks");
        if ((lock != null) && (lock.isHeld())) {
            try {
                lock.release();
            } catch (Exception ex) {
                log(ex.toString());
            }
        }
        if ((wl != null) && (wl.isHeld())) {
            try {
                wl.release();
            } catch (Exception ex) {
                log(ex.toString());
            }
        }
    }

    private void resetUploadNumbers(){
        log("resetUploadNumbers");
        totalFileUploads = 0;
        totalFileUploadsCompleted = 0;
        totalFileUploadsCompletedSuccessfully = 0;
        totalFolderUploadsCompleted = 0;
        totalFolderUploads = 0;
        totalFolderUploadsCompletedSuccessfully = 0;
        childUploadFailed = 0;
        childUploadSucceeded = 0;
    }

    class UploadProgress{
        private long total;
        private long inProgress;

        public void setTotal(long total) {
            this.total = total;
        }

        public void setInProgress(long inProgress) {
            this.inProgress = inProgress;
        }

        public long getTotal(){
            return this.total;
        }

        public long getInProgress() {
            return inProgress;
        }
    }

    private boolean isTransferBelongsToFolderTransfer(MegaTransfer transfer){
        return transfer.getFolderTransferTag() > 0;
    }
}
