package mega.privacy.android.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferData;
import nz.mega.sdk.MegaTransferListenerInterface;

import static mega.privacy.android.app.components.transferWidget.TransfersManagement.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.*;
import static mega.privacy.android.app.lollipop.qrcode.MyCodeFragment.QR_IMAGE_FILE_NAME;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.PermissionUtils.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.*;

/*
 * Service to Upload files
 */
public class UploadService extends Service implements MegaTransferListenerInterface {

	public static String ACTION_CANCEL = "CANCEL_UPLOAD";
	public static String EXTRA_FILEPATH = "MEGA_FILE_PATH";
	public static String EXTRA_FOLDERPATH = "MEGA_FOLDER_PATH";
	public static String EXTRA_NAME = "MEGA_FILE_NAME";
	public static String EXTRA_LAST_MODIFIED = "MEGA_FILE_LAST_MODIFIED";
	public static String EXTRA_NAME_EDITED = "MEGA_FILE_NAME_EDITED";
	public static String EXTRA_SIZE = "MEGA_SIZE";
	public static String EXTRA_PARENT_HASH = "MEGA_PARENT_HASH";
	public static String EXTRA_UPLOAD_COUNT = "EXTRA_UPLOAD_COUNT";

    private static final int NOT_OVERQUOTA_STATE = 0;
    private static final int OVERQUOTA_STORAGE_STATE = 1;
    private static final int PRE_OVERQUOTA_STORAGE_STATE = 2;

	private int errorCount = 0;
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

    private HashMap<Integer, MegaTransfer> mapProgressFileTransfers;
    private HashMap<Integer, MegaTransfer> mapProgressFolderTransfers;
    private static int totalFileUploadsCompleted = 0;
    private static int totalFileUploadsCompletedSuccessfully = 0;
    private static int totalFileUploads = 0;
    private static int totalFolderUploadsCompleted = 0;
    private static int totalFolderUploads = 0;
    private static int totalFolderUploadsCompletedSuccessfully = 0;

    private static int uploadCount = 0;
    private static int currentUpload = 0;

	//NOT_OVERQUOTA_STATE           = 0 - not overquota, not pre-overquota
	//OVERQUOTA_STORAGE_STATE       = 1 - overquota
	//PRE_OVERQUOTA_STORAGE_STATE   = 2 - pre-overquota
    private int isOverquota = NOT_OVERQUOTA_STATE;

    /** the receiver and manager for the broadcast to listen to the pause event */
    private BroadcastReceiver pauseBroadcastReceiver;

    private CompositeDisposable rxSubscriptions = new CompositeDisposable();

    @SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		logDebug("onCreate");

		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		megaApi.addTransferListener(this);
		megaChatApi = app.getMegaChatApi();
		mapProgressFileTransfers = new HashMap<>();
        mapProgressFolderTransfers = new HashMap<>();
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		isForeground = false;
		canceled = false;
		isOverquota = NOT_OVERQUOTA_STATE;

        int wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;

        WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null) {
            lock = wifiManager.createWifiLock(wifiLockMode, "MegaUploadServiceWifiLock");
        }

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaUploadServicePowerLock:");
        }

        mBuilder = new Notification.Builder(UploadService.this);
		mBuilderCompat = new NotificationCompat.Builder(UploadService.this, null);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // delay 1 second to refresh the pause notification to prevent update is missed
        pauseBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new Handler().postDelayed(() -> {
                    if (totalFileUploads > 0) {
                        updateProgressNotification(false);
                    }
                    if (totalFolderUploads > 0) {
                        updateProgressNotification(true);
                    }
                }, 1000);
            }
        };
        registerReceiver(pauseBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));
	}

	@Override
	public void onDestroy(){
		logDebug("onDestroy");
        releaseLocks();

		if(megaApi != null) {
            megaApi.removeTransferListener(this);
		}

        if (megaChatApi != null){
            megaChatApi.saveCurrentState();
        }


        unregisterReceiver(pauseBroadcastReceiver);
        rxSubscriptions.clear();

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
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD);
				return START_NOT_STICKY;
			}
		}

		onHandleIntent(intent);
		logDebug(currentUpload +" / " + uploadCount);
		return START_NOT_STICKY;
	}

	private synchronized void onHandleIntent(final Intent intent) {
		logDebug("onHandleIntent");

        String action = intent.getAction();
        logDebug("Action is " + action);
        if (action != null) {
            switch (action) {
                case ACTION_OVERQUOTA_STORAGE:
                    isOverquota = OVERQUOTA_STORAGE_STATE;
                    break;

                case ACTION_STORAGE_STATE_CHANGED:
                    isOverquota = NOT_OVERQUOTA_STATE;
                    break;

                case ACTION_RESTART_SERVICE:
                    MegaTransferData transferData = megaApi.getTransferData(null);
                    if (transferData == null) {
                        return;
                    }

                    int uploadsInProgress = transferData.getNumUploads();

                    for (int i = 0; i < uploadsInProgress; i++) {
                        MegaTransfer transfer = megaApi.getTransferByTag(transferData.getUploadTag(i));
                        if (transfer == null) {
                            continue;
                        }

                        String data = transfer.getAppData();
                        if (!isTextEmpty(data) && (data.contains(UPLOAD_APP_DATA_CHAT) || data.contains(CU_UPLOAD))) {
                            continue;
                        }

                        if (transfer.isFolderTransfer()) {
                            mapProgressFolderTransfers.put(transfer.getTag(), transfer);
                        } else {
                            mapProgressFileTransfers.put(transfer.getTag(), transfer);
                        }
                    }

                    totalFolderUploads = mapProgressFolderTransfers.size();
                    totalFileUploads = mapProgressFileTransfers.size();
                    uploadCount = currentUpload = transfersCount = totalFileUploads + totalFolderUploads;
                    break;
            }

            if (totalFileUploads > 0) {
                updateProgressNotification(false);
            }

            if (totalFolderUploads > 0) {
                updateProgressNotification(true);
            }

            return;
        } else {
            isOverquota = NOT_OVERQUOTA_STATE;
        }

        currentUpload ++;

        String filePath = intent.getStringExtra(EXTRA_FILEPATH);
        if (isTextEmpty(filePath)) {
            logWarning("Error: File path is NULL or EMPTY");
            return;
        }

        acquireLock();

        rxSubscriptions.add(Single.just(true)
            .observeOn(Schedulers.single())
            .subscribe(ignored -> doHandleIntent(intent, filePath),
                throwable -> logError("doHandleIntent onError", throwable)));
    }

    private void doHandleIntent(Intent intent, String filePath) {
        final File file = new File(filePath);
        logDebug("File to manage: " + file.getAbsolutePath());

        long parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, 0);
        String nameInMEGA = intent.getStringExtra(EXTRA_NAME);
        String nameInMEGAEdited = intent.getStringExtra(EXTRA_NAME_EDITED);
        long lastModified = intent.getLongExtra(EXTRA_LAST_MODIFIED, 0);
        if (lastModified <= 0) {
            lastModified = file.lastModified();
        }

        MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

        if (file.isDirectory()) {
            // Folder upload
            totalFolderUploads++;
            if (nameInMEGA != null) {
                megaApi.startUpload(file.getAbsolutePath(), parentNode, nameInMEGA);
            } else {
                megaApi.startUpload(file.getAbsolutePath(), parentNode);
            }
        } else {
            totalFileUploads++;

            if (nameInMEGAEdited != null) {
                // File upload with edited name
                megaApi.startUpload(file.getAbsolutePath(), parentNode, nameInMEGAEdited);
            } else if (lastModified == 0) {
                if (nameInMEGA != null) {
                    megaApi.startUpload(file.getAbsolutePath(), parentNode, nameInMEGA);
                } else {
                    megaApi.startUpload(file.getAbsolutePath(), parentNode);
                }
            } else {
                if (nameInMEGA != null) {
                    megaApi.startUpload(file.getAbsolutePath(), parentNode, nameInMEGA, lastModified / 1000);
                } else {
                    megaApi.startUpload(file.getAbsolutePath(), parentNode, lastModified / 1000);
                }
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
        if (isOverquota != NOT_OVERQUOTA_STATE) {
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

            sendUploadFinishBroadcast();
        }

        if (megaApi.getNumPendingUploads() <= 0) {
			logDebug("Reset total uploads");
            megaApi.resetTotalUploads();
        }

        errorCount = 0;

        resetUploadNumbers();

		logDebug("Stopping service!");
        isForeground = false;
        stopForeground(true);
        mNotificationManager.cancel(notificationIdForFileUpload);
        mNotificationManager.cancel(notificationIdForFolderUpload);
        stopSelf();
		logDebug("After stopSelf");

        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			deleteCacheFolderIfEmpty(getApplicationContext(), TEMPORAL_FOLDER);
        }

	}

    private void notifyNotification(String notificationTitle,String size,int notificationId,String channelId,String channelName) {
        Intent intent = new Intent(UploadService.this, ManagerActivityLollipop.class);
        intent.setAction(ACTION_SHOW_TRANSFERS);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TRANSFERS_TAB, COMPLETED_TAB);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,channelName,NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null,null);
            mNotificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(),channelId);

            mBuilderCompatO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT))
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false);

            mNotificationManager.notify(notificationId,mBuilderCompatO.build());
        } else {
            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT))
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

    private void sendUploadFinishBroadcast() {
        sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
                .putExtra(TRANSFER_TYPE, UPLOAD_TRANSFER)
                .putExtra(NUMBER_FILES, totalFileUploads + totalFolderUploads));
    }

    /*
     * Show complete success notification
     */
    private void showFileUploadCompleteNotification() {
		logDebug("showFileUploadCompleteNotification");
        if (isOverquota == NOT_OVERQUOTA_STATE) {
            String notificationTitle, size;
            int quantity = totalFileUploadsCompletedSuccessfully == 0 ? 1 : totalFileUploadsCompletedSuccessfully;
            notificationTitle = getResources().getQuantityString(R.plurals.upload_service_final_notification,quantity,totalFileUploadsCompletedSuccessfully);

            if (errorCount > 0) {
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
        if (isOverquota == NOT_OVERQUOTA_STATE) {
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
            case NOT_OVERQUOTA_STATE:
            default:
                intent.setAction(ACTION_SHOW_TRANSFERS);
                intent.putExtra(TRANSFERS_TAB, PENDING_TAB);
                break;

            case OVERQUOTA_STORAGE_STATE:
                intent.setAction(ACTION_OVERQUOTA_STORAGE);
                break;

            case PRE_OVERQUOTA_STORAGE_STATE:
                intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(UploadService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
      rxSubscriptions.add(Single.just(isFolderTransfer)
          .observeOn(Schedulers.single())
          .subscribe(this::doUpdateProgressNotification,
              throwable -> logError("doUpdateProgressNotification onError", throwable)));
    }

    private void doUpdateProgressNotification(boolean isFolderTransfer) {
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
        String actionString = isOverquota == NOT_OVERQUOTA_STATE ? getString(R.string.download_touch_to_show) : getString(R.string.general_show_info);
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
        if (isOverquota != NOT_OVERQUOTA_STATE) {
            message = getString(R.string.overquota_alert_title);
        } else if (inProgress == 0) {
            message = getString(R.string.download_preparing_files);
        } else {
            int filesProgress;
            if (isFolderUpload) {
                filesProgress = totalFolderUploadsCompleted + 1 > totalFolderUploads ? totalFolderUploads : totalFolderUploadsCompleted + 1;
                if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                    message = getResources().getQuantityString(R.plurals.folder_upload_service_paused_notification, totalFolderUploads, filesProgress, totalFolderUploads);
                } else {
                    message = getResources().getQuantityString(R.plurals.folder_upload_service_notification, totalFolderUploads, filesProgress, totalFolderUploads);
                }
            } else {
                filesProgress = totalFileUploadsCompleted + 1 > totalFileUploads ? totalFileUploads : totalFileUploadsCompleted + 1;
                if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                    message = getResources().getQuantityString(R.plurals.upload_service_paused_notification, totalFileUploads, filesProgress, totalFileUploads);
                } else {
                    message = getResources().getQuantityString(R.plurals.upload_service_notification, totalFileUploads, filesProgress, totalFileUploads);
                }
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
      rxSubscriptions.add(Single.just(transfer)
          .observeOn(Schedulers.single())
          .subscribe(this::doOnTransferStart,
              throwable -> logError("doOnTransferStart onError", throwable)));
    }

    private void doOnTransferStart(MegaTransfer transfer) {
		logDebug("Upload start: " + transfer.getFileName());
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
		    if (isCUTransfer(transfer)) return;

		    launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);
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
      rxSubscriptions.add(Single.just(true)
          .observeOn(Schedulers.single())
          .subscribe(ignored -> doOnTransferFinish(transfer, error),
              throwable -> logError("doOnTransferFinish onError", throwable)));
    }

    private void doOnTransferFinish(MegaTransfer transfer, MegaError error) {
		logDebug("Path: " + transfer.getPath() + ", Size: " + transfer.getTransferredBytes());
        if (isCUTransfer(transfer)) return;

		launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);

		if (error.getErrorCode() == MegaError.API_EBUSINESSPASTDUE) {
			sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
		}

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
		    if (!transfer.isFolderTransfer()) {
                addCompletedTransfer(new AndroidCompletedTransfer(transfer, error));

                if (transfer.getState() == MegaTransfer.STATE_FAILED) {
                    MegaApplication.getTransfersManagement().setFailedTransfers(true);
                }
            }

            if (isTransferBelongsToFolderTransfer(transfer)) {
                if (!transfer.isFolderTransfer()) {
                    if (error.getErrorCode() == MegaError.API_OK) {
                        childUploadSucceeded++;
                    } else {
                        childUploadFailed++;
                    }
                }
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
								double[] latLong = exifInterface.getLatLong();
								megaApi.setNodeCoordinates(node, latLong[0], latLong[1], null);
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
						isOverquota = OVERQUOTA_STORAGE_STATE;
					}
					else if (error.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
						isOverquota = PRE_OVERQUOTA_STORAGE_STATE;
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

                if (totalFileUploadsCompleted == totalFileUploads
                        && totalFolderUploadsCompleted == totalFolderUploads
                        && transfersCount == 0
                        && (totalFileUploadsCompleted + totalFolderUploadsCompleted) == currentUpload
                        && (totalFileUploadsCompleted + totalFolderUploadsCompleted) >= uploadCount
                ) {
                    onQueueComplete();
                } else {
                    updateProgressNotification(transfer.isFolderTransfer());
                }
			}
		}
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
      rxSubscriptions.add(Single.just(transfer)
          .observeOn(Schedulers.single())
          .subscribe(this::doOnTransferUpdate,
              throwable -> logError("doOnTransferUpdate onError", throwable)));
    }

    private void doOnTransferUpdate(MegaTransfer transfer) {
		logDebug("onTransferUpdate");
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD){
            if (isCUTransfer(transfer)) return;

		    launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);

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
      rxSubscriptions.add(Single.just(true)
          .observeOn(Schedulers.single())
          .subscribe(ignored -> doOnTransferTemporaryError(transfer, e),
              throwable -> logError("doOnTransferTemporaryError onError", throwable)));
    }

    private void doOnTransferTemporaryError(MegaTransfer transfer, MegaError e) {
		logWarning("onTransferTemporaryError: " + e.getErrorString() + "__" + e.getErrorCode());

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
            if (isCUTransfer(transfer)) return;

            if(isTransferBelongsToFolderTransfer(transfer)){
                return;
            }
			switch (e.getErrorCode())
			{
				case MegaError.API_EOVERQUOTA:
				case MegaError.API_EGOINGOVERQUOTA:
					if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
						isOverquota = OVERQUOTA_STORAGE_STATE;
					}
					else if (e.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
						isOverquota = PRE_OVERQUOTA_STORAGE_STATE;
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

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return true;
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
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isOverquota == OVERQUOTA_STORAGE_STATE) {
            intent.setAction(ACTION_OVERQUOTA_STORAGE);
        } else {
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
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompatO.build());
		}
		else {
			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompat.build());
		}
	}

	private void acquireLock(){
		logDebug("acquireLock");
        if (wl != null && !wl.isHeld()) {
            wl.acquire();
        }
        if (lock != null && !lock.isHeld()) {
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
        uploadCount = 0;
        currentUpload = 0;
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

    /**
     * Checks if a transfer is a CU transfer.
     *
     * @param transfer  MegaTransfer to check
     * @return True if the transfer is a CU transfer, false otherwise.
     */
    private boolean isCUTransfer(MegaTransfer transfer) {
        String appData = transfer.getAppData();
        return !isTextEmpty(appData) && appData.contains(CU_UPLOAD);
    }
}
