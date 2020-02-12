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
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;

import static mega.privacy.android.app.lollipop.qrcode.MyCodeFragment.QR_IMAGE_FILE_NAME;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.*;

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

	private int notificationIdForFileUpload = NOTIFICATION_UPLOAD;
	private int notificationIdFinalForFileUpload = NOTIFICATION_UPLOAD_FINAL;
	private String notificationChannelIdForFileUpload = NOTIFICATION_CHANNEL_UPLOAD_ID;
	private String notificationChannelNameForFileUpload = NOTIFICATION_CHANNEL_UPLOAD_NAME;

    private int notificationIdForFolderUpload = NOTIFICATION_UPLOAD_FOLDER;
    private int notificationIdFinalForFolderUpload = NOTIFICATION_UPLOAD_FINAL_FOLDER;
    private String notificationChannelIdForFolderUpload = NOTIFICATION_CHANNEL_UPLOAD_ID_FOLDER;
    private String notificationChannelNameForFolderUpload = NOTIFICATION_CHANNEL_UPLOAD_NAME_FOLDER;

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
		logDebug("onCreate");

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
		logDebug("onDestroy");
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
		logDebug("onStartCommand");
		canceled = false;

		if (intent == null) {
			return START_NOT_STICKY;
		}

		uploadCount = intent.getIntExtra(EXTRA_UPLOAD_COUNT, 0);

		if ((intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_CANCEL)) {
				logDebug("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
				return START_NOT_STICKY;
			}
		}

		onHandleIntent(intent);
		logDebug(currentUpload +" / " + uploadCount);
        if(currentUpload == uploadCount && uploadedFileCount != 0) {
			logDebug("Send message");
            Intent i = new Intent(this, ManagerActivityLollipop.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(SHOW_REPEATED_UPLOAD);
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
		logDebug("onHandleIntent");

		String action = intent.getAction();
		logDebug("Action is " + action);
		if(action != null){
            if(ACTION_CHILD_UPLOADED_OK.equals(action)){
                childUploadSucceeded++;
                return;
            }else if(ACTION_CHILD_UPLOADED_FAILED.equals(action)){
                childUploadFailed++;
                return;
            }
            if (ACTION_OVERQUOTA_STORAGE.equals(action)) {
                isOverquota = 1;
            }else if(ACTION_STORAGE_STATE_CHANGED.equals(action)){
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
			logDebug("File to manage: " + file.getAbsolutePath());
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
						logDebug("CHECK_FILE_TO_UPLOAD_UPLOAD");
                        acquireLock();
						totalFileUploads++;
                        totalUploads++;
						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGAEdited, this);
						break;
					}
					case CHECK_FILE_TO_UPLOAD_COPY: {
						logDebug("CHECK_FILE_TO_UPLOAD_COPY");
						break;
					}
					case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER: {
						logDebug("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
						logDebug("Return - file already uploaded");
						return;

					}
				}
			}
			else {
				switch (checkFileToUpload(file, parentHandle)) {
					case CHECK_FILE_TO_UPLOAD_UPLOAD: {
						logDebug("CHECK_FILE_TO_UPLOAD_UPLOAD");

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
						logDebug("CHECK_FILE_TO_UPLOAD_COPY");
						break;
					}
					case CHECK_FILE_TO_UPLOAD_OVERWRITE: {
						logDebug("CHECK_FILE_TO_UPLOAD_OVERWRITE");
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
						logDebug("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
                        uploadedFileCount++;
						logDebug("Return - file already uploaded");
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
		logDebug("cancel");
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
		logDebug("onQueueComplete");
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
			logDebug("Reset total uploads");
            megaApi.resetTotalUploads();
        }

        errorCount = 0;
        copiedCount = 0;

        resetUploadNumbers();

		logDebug("Stopping service!");
        isForeground = false;
        stopForeground(true);
        mNotificationManager.cancel(notificationIdForFileUpload);
        mNotificationManager.cancel(notificationIdForFolderUpload);
        stopSelf();
		logDebug("After stopSelf");

        if (isPermissionGranted(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
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
		logDebug("showFileUploadCompleteNotification");
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
                String totalBytes = getSizeString(transferredBytes);
                size = getString(R.string.general_total_size,totalBytes);
            }

            notifyNotification(notificationTitle,size,notificationIdFinalForFileUpload,notificationChannelIdForFileUpload,notificationChannelNameForFileUpload);
        }
    }

    private void showFolderUploadCompleteNotification() {
		logDebug("showFolderUploadCompleteNotification");
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
                String totalBytes = getSizeString(transferredBytes);
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
                intent.setAction(ACTION_SHOW_TRANSFERS);
                break;
            case 1:
                intent.setAction(ACTION_OVERQUOTA_STORAGE);
                break;
            case 2:
                intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
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
			logDebug("Starting foreground");
            try {
                startForeground(notificationId, notification);
                isForeground = true;
            }
            catch (Exception e){
				logError("Start foreground exception", e);
                isForeground = false;
            }
        } else {
            mNotificationManager.notify(notificationId, notification);
        }
    }

    private void updateProgressNotification(boolean isFolderTransfer) {
		logDebug("isFolderTransfer: " + isFolderTransfer);
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
		logDebug(logMessage + progressPercent + " " + message);
        String actionString = isOverquota == 0 ? getString(R.string.download_touch_to_show) : getString(R.string.general_show_info);
        String info = getProgressSize(UploadService.this,inProgress,total);

        if(isFolderTransfer){
            notifyProgressNotification(progressPercent,message,info,actionString,notificationIdForFolderUpload,notificationChannelIdForFolderUpload,notificationChannelNameForFolderUpload);
        }else{
            notifyProgressNotification(progressPercent,message,info,actionString,notificationIdForFileUpload,notificationChannelIdForFileUpload,notificationChannelNameForFileUpload);
        }
    }

    private String getMessageForProgressNotification(long inProgress,boolean isFolderUpload) {
        logDebug("inProgress: " + inProgress + ", isFolderUpload:" + isFolderUpload);
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
		logDebug("getInProgressNotification");
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

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

		logDebug("Upload start: " + transfer.getFileName());
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
		logDebug("Path: " + transfer.getPath() + ", Size: " + transfer.getTransferredBytes());
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
				logDebug("Upload canceled: " + transfer.getFileName());

                releaseLocks();
				UploadService.this.cancel();
				logDebug("After cancel");
				deleteCacheFolderIfEmpty(getApplicationContext(), TEMPORAL_FOLDER);

			} else {
				if (error.getErrorCode() == MegaError.API_OK) {
					logDebug("Upload OK: " + transfer.getFileName());
					if(transfer.isFolderTransfer()){
                        totalFolderUploadsCompletedSuccessfully++;
                    }else{
                        totalFileUploadsCompletedSuccessfully++;
                    }
					if (isVideoFile(transfer.getPath())) {
						logDebug("Is video!!!");

						File previewDir = getPreviewFolder(this);
						File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						File thumbDir = ThumbnailUtils.getThumbFolder(this);
						File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
						megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

						MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());

						if (node != null) {
							MediaMetadataRetriever retriever = new MediaMetadataRetriever();
							String location = null;
							try {
								retriever.setDataSource(transfer.getPath());
								location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
							} catch (Exception ex) {
								logError("Exception is thrown", ex);
							}

							if(location!=null){
								logDebug("Location: " + location);

								boolean secondTry = false;
								try{
									final int mid = location.length() / 2; //get the middle of the String
									String[] parts = {location.substring(0, mid),location.substring(mid)};

									Double lat = Double.parseDouble(parts[0]);
									Double lon = Double.parseDouble(parts[1]);
									logDebug("Lat: " + lat); //first part
									logDebug("Long: " + lon); //second part

									megaApi.setNodeCoordinates(node, lat, lon, null);
								}
								catch (Exception e){
									secondTry = true;
									logError("Exception, second try to set GPS coordinates", e);
								}

								if(secondTry){
									try{
										String latString = location.substring(0,7);
										String lonString = location.substring(8,17);

										Double lat = Double.parseDouble(latString);
										Double lon = Double.parseDouble(lonString);
										logDebug("Lat: " + lat); //first part
										logDebug("Long: " + lon); //second part

										megaApi.setNodeCoordinates(node, lat, lon, null);
									}
									catch (Exception e){
										logError("Exception again, no chance to set coordinates of video", e);
									}
								}
							}
							else{
								logDebug("No location info");
							}
						}
					} else if (MimeTypeList.typeForName(transfer.getPath()).isImage()) {
						logDebug("Is image!!!");

						File previewDir = getPreviewFolder(this);
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
									logDebug("Latitude: " + latLong[0] + " Longitude: " + latLong[1]);
									megaApi.setNodeCoordinates(node, latLong[0], latLong[1], null);
								}

							} catch (Exception e) {
								logWarning("Couldn't read exif info: " + transfer.getPath(), e);
							}
						}
					} else if (MimeTypeList.typeForName(transfer.getPath()).isPdf()) {
						logDebug("Is pdf!!!");

						try {
							createThumbnailPdf(this, transfer.getPath(), megaApi, transfer.getNodeHandle());
						} catch(Exception e) {
							logWarning("Pdf thumbnail could not be created", e);
						}

						int pageNumber = 0;
						FileOutputStream out = null;

						try {
						PdfiumCore pdfiumCore = new PdfiumCore(this);
						MegaNode pdfNode = megaApi.getNodeByHandle(transfer.getNodeHandle());

						if (pdfNode == null){
							logError("pdf is NULL");
							return;
						}

						File previewDir = getPreviewFolder(this);
						File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						File file = new File(transfer.getPath());

							PdfDocument pdfDocument = pdfiumCore.newDocument(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
							pdfiumCore.openPage(pdfDocument, pageNumber);
							int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
							int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
							Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
							pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
							Bitmap resizedBitmap = resizeBitmapUpload(bmp, width, height);
							out = new FileOutputStream(preview);
							boolean result = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
							if(result){
								logDebug("Compress OK!");
								megaApi.setPreview(pdfNode, preview.getAbsolutePath());
							}
							else{
								logWarning("Not Compress");
							}
							pdfiumCore.closeDocument(pdfDocument);
						} catch(Exception e) {
							logWarning("Pdf preview could not be created", e);
						} finally {
							try {
								if (out != null)
									out.close();
							} catch (Exception e) {

							}
						}

					} else {
						logDebug("NOT video, image or pdf!");
					}
				} else {
					logError("Upload Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());

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

				String qrFileName = megaApi.getMyEmail() + QR_IMAGE_FILE_NAME;

				File localFile = buildQrFile(getApplicationContext(),transfer.getFileName());
                if (isFileAvailable(localFile) && !localFile.getName().equals(qrFileName)) {
					logDebug("Delete file!: " + localFile.getAbsolutePath());
                    localFile.delete();
                }

                File tempPic = getCacheFolder(getApplicationContext(), TEMPORAL_FOLDER);
				logDebug("IN Finish: " + transfer.getFileName() + "path? " + transfer.getPath());
				if (isFileAvailable(tempPic) && transfer.getPath() != null) {
					if (transfer.getPath().startsWith(tempPic.getAbsolutePath())) {
						File f = new File(transfer.getPath());
						f.delete();
					}
				} else {
					logError("transfer.getPath() is NULL or temporal folder unavailable");
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
		logDebug("onTransferUpdate");
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD){

            if(isTransferBelongsToFolderTransfer(transfer)){
                return;
            }

			String appData = transfer.getAppData();

			if(appData!=null){
				return;
			}

            if (canceled) {
				logDebug("Transfer cancel: " + transfer.getFileName());
                releaseLocks();
                megaApi.cancelTransfer(transfer);
                UploadService.this.cancel();
				logDebug("After cancel");
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
		logWarning("onTransferTemporaryError: " + e.getErrorString() + "__" + e.getErrorCode());

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
						logWarning("TRANSFER OVER QUOTA ERROR: " + e.getErrorCode());
					}
					else {
						logWarning("STORAGE OVER QUOTA ERROR: " + e.getErrorCode());
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
		logDebug("showStorageOverQuotaNotification");
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
			intent.setAction(ACTION_OVERQUOTA_STORAGE);
		}
		else{
			intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
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

			mNotificationManager.notify(NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompatO.build());
		}
		else {
			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompat.build());
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getName());
		if (request.getType() == MegaRequest.TYPE_COPY){
            updateProgressNotification(false);
		}
	}

    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
		logDebug("UPLOAD: onRequestFinish " + request.getRequestString());
        if (request.getType() == MegaRequest.TYPE_COPY) {
			logDebug("TYPE_COPY finished");
            if (e.getErrorCode() == MegaError.API_OK) {
                copiedCount++;
                MegaNode n = megaApi.getNodeByHandle(request.getNodeHandle());
                if (n != null) {
                    String currentNodeName = n.getName();
                    String megaFingerPrint = megaApi.getFingerprint(n);
					logDebug("Copy node");
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
					logError("ERROR - node is NULL");
                }
            } else if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
				logWarning("OVER QUOTA ERROR: " + e.getErrorCode());
                isOverquota = 1;
                onQueueComplete();
            } else if (e.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
				logWarning("OVER QUOTA ERROR: " + e.getErrorCode());
                isOverquota = 2;
                onQueueComplete();
            } else {
				logError("ERROR: " + e.getErrorCode());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
		logDebug("onRequestTemporaryError: " + request.getName());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
		logDebug("onRequestUpdate: " + request.getName());
    }

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}

	private void acquireLock(){
		logDebug("acquireLock");
        if (!wl.isHeld()) {
            wl.acquire();
        }
        if (!lock.isHeld()) {
            lock.acquire();
        }
    }

	private void releaseLocks(){
		logDebug("releaseLocks");
        if ((lock != null) && (lock.isHeld())) {
            try {
                lock.release();
            } catch (Exception ex) {
				logError("EXCEPTION", ex);
            }
        }
        if ((wl != null) && (wl.isHeld())) {
            try {
                wl.release();
            } catch (Exception ex) {
				logError("EXCEPTION", ex);
            }
        }
    }

    private void resetUploadNumbers(){
		logDebug("resetUploadNumbers");
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
