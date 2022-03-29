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
import androidx.lifecycle.Observer;

import com.jeremyliao.liveeventbus.LiveEventBus;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import mega.privacy.android.app.globalmanagement.TransfersManagement;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.service.iar.RatingHandlerImpl;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaCancelToken;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferData;
import nz.mega.sdk.MegaTransferListenerInterface;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_SERVICE_IF_NO_TRANSFERS;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.WAIT_TIME_BEFORE_UPDATE;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.addCompletedTransfer;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.createInitialServiceNotification;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.launchTransferUpdateIntent;
import static mega.privacy.android.app.main.ManagerActivity.*;
import static mega.privacy.android.app.main.qrcode.MyCodeFragment.QR_IMAGE_FILE_NAME;
import static mega.privacy.android.app.textEditor.TextEditorUtil.getCreationOrEditorText;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.permission.PermissionUtils.*;
import static mega.privacy.android.app.utils.TextUtil.addStringSeparator;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import javax.inject.Inject;

/*
 * Service to Upload files
 */
@AndroidEntryPoint
public class UploadService extends Service implements MegaTransferListenerInterface {

	public static String ACTION_CANCEL = "CANCEL_UPLOAD";
	public static String EXTRA_FILE_PATH = "MEGA_FILE_PATH";
	public static String EXTRA_NAME = "MEGA_FILE_NAME";
	public static String EXTRA_LAST_MODIFIED = "MEGA_FILE_LAST_MODIFIED";
	public static String EXTRA_PARENT_HASH = "MEGA_PARENT_HASH";
    public static String EXTRA_UPLOAD_TXT = "EXTRA_UPLOAD_TXT";

    @Inject
    TransfersManagement transfersManagement;

	private boolean isForeground = false;
	private boolean canceled;

    private MegaApplication app;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;

    private WifiLock lock;
    private WakeLock wl;
    private DatabaseHandler dbH = null;

	private Notification.Builder mBuilder;
	private NotificationCompat.Builder mBuilderCompat;
	private NotificationManager mNotificationManager;

    private HashMap<Integer, MegaTransfer> mapProgressFileTransfers;
    private int pendingToAddInQueue = 0;
    private int completed = 0;
    private int completedSuccessfully = 0;
    private int alreadyUploaded = 0;
    private int uploadCount = 0;

	//NOT_OVERQUOTA_STATE           = 0 - not overquota, not pre-overquota
	//OVERQUOTA_STORAGE_STATE       = 1 - overquota
	//PRE_OVERQUOTA_STORAGE_STATE   = 2 - pre-overquota
    private int isOverquota = NOT_OVERQUOTA_STATE;

    /** the receiver and manager for the broadcast to listen to the pause event */
    private BroadcastReceiver pauseBroadcastReceiver;

    private final CompositeDisposable rxSubscriptions = new CompositeDisposable();

    // the flag to determine the rating dialog is showed for this upload action
    private boolean isRatingShowed;

    private final Observer<Boolean> stopServiceObserver = finish -> {
        if (finish && megaApi.getNumPendingUploads() == 0) {
            stopForeground();
        }
    };

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
		mBuilderCompat = new NotificationCompat.Builder(UploadService.this, NOTIFICATION_CHANNEL_UPLOAD_ID);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		startForeground();

        // delay 1 second to refresh the pause notification to prevent update is missed
        pauseBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new Handler().postDelayed(() -> {
                    updateProgressNotification();
                }, WAIT_TIME_BEFORE_UPDATE);
            }
        };

        registerReceiver(pauseBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));

        LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean.class)
                .observeForever(stopServiceObserver);
	}

    private void startForeground() {
        try {
            startForeground(NOTIFICATION_UPLOAD,
                    createInitialServiceNotification(NOTIFICATION_CHANNEL_UPLOAD_ID,
                            NOTIFICATION_CHANNEL_UPLOAD_NAME,
                            mNotificationManager,
                            new NotificationCompat.Builder(UploadService.this, NOTIFICATION_CHANNEL_UPLOAD_ID),
                            mBuilder));

            isForeground = true;
        } catch (Exception e) {
            logWarning("Error starting foreground.", e);
            isForeground = false;
        }
    }

    private void stopForeground() {
        isForeground = false;
        stopForeground(true);
        mNotificationManager.cancel(NOTIFICATION_UPLOAD);
        mNotificationManager.cancel(NOTIFICATION_UPLOAD_FOLDER);
        stopSelf();
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

        LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean.class)
                .removeObserver(stopServiceObserver);

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logDebug("onStartCommand");
		canceled = false;

		if (intent == null) {
		    canceled = true;
		    stopForeground();
			return START_NOT_STICKY;
		}

		if ((intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_CANCEL)) {
				logDebug("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD);
				stopForeground();
				return START_NOT_STICKY;
			}
		}

		onHandleIntent(intent);
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
                        stopForeground();
                        return;
                    }

                    int uploadsInProgress = transferData.getNumUploads();

                    for (int i = 0; i < uploadsInProgress; i++) {
                        MegaTransfer transfer = megaApi.getTransferByTag(transferData.getUploadTag(i));
                        if (transfer == null || isCUOrChatTransfer(transfer)) {
                            continue;
                        }

                        transfersManagement.checkIfTransferIsPaused(transfer);

                        if (!transfer.isFolderTransfer() && transfer.getAppData() == null){
                            mapProgressFileTransfers.put(transfer.getTag(), transfer);
                        }
                    }

                    uploadCount = mapProgressFileTransfers.size();

                    if (uploadCount > 0) {
                        isForeground = false;
                        stopForeground(true);
                        mNotificationManager.cancel(NOTIFICATION_UPLOAD);
                    }

                    launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);
                    break;
            }

            if (uploadCount == 0) {
                stopForeground();
            } else {
                updateProgressNotification();
            }

            return;
        } else {
            isOverquota = NOT_OVERQUOTA_STATE;
        }

        String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
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

        String textFileMode = intent.getStringExtra(EXTRA_UPLOAD_TXT);
        long parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, INVALID_HANDLE);
        String fileName = intent.getStringExtra(EXTRA_NAME);
        long lastModified = intent.getLongExtra(EXTRA_LAST_MODIFIED, 0);
        if (lastModified <= 0) {
            lastModified = file.lastModified() / 1000;
        }

        MegaNode parentNode = parentHandle == INVALID_HANDLE
                ? megaApi.getRootNode()
                : megaApi.getNodeByHandle(parentHandle);

        long mTime = lastModified == 0 ? INVALID_VALUE : lastModified;

        pendingToAddInQueue++;

        if (!isTextEmpty(textFileMode)) {
            String appData = APP_DATA_TXT_FILE + APP_DATA_INDICATOR + textFileMode
                    + APP_DATA_INDICATOR + intent.getBooleanExtra(FROM_HOME_PAGE, false);

            megaApi.startUpload(file.getAbsolutePath(), parentNode, fileName, mTime, appData,
                    true, true, null);
        } else {
            MegaCancelToken cancelToken = transfersManagement
                    .addScanningTransfer(MegaTransfer.TYPE_UPLOAD, file.getAbsolutePath(), parentNode, file.isDirectory());

            if (cancelToken != null) {
                megaApi.startUpload(file.getAbsolutePath(), parentNode, fileName, mTime, null,
                        false, false, cancelToken);
            }
        }
    }

	/*
	 * Stop uploading service
	 */
	private void cancel() {
		logDebug("cancel");
		canceled = true;
		stopForeground();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * No more intents in the queue, reset and finish service.
     *
     * @param showSnackbar True if should show finish snackbar, false otherwise.
	 */
	private void onQueueComplete(boolean showSnackbar) {
		logDebug("onQueueComplete");
        releaseLocks();
        if (isOverquota != NOT_OVERQUOTA_STATE) {
            showStorageOverQuotaNotification();
        } else {
            showUploadCompleteNotification();

            if (showSnackbar) {
                sendUploadFinishBroadcast();
            }
        }

        if (megaApi.getNumPendingUploads() <= 0) {
			logDebug("Reset total uploads");
            megaApi.resetTotalUploads();
        }

        resetUploadNumbers();

		logDebug("Stopping service!");
        stopForeground();
		logDebug("After stopSelf");

        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			deleteCacheFolderIfEmpty(getApplicationContext(), TEMPORAL_FOLDER);
        }

	}

    private void notifyNotification(String notificationTitle,String size,int notificationId,String channelId,String channelName) {
        Intent intent = new Intent(UploadService.this, ManagerActivity.class);
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
                    .setColor(ContextCompat.getColor(MegaApplication.getInstance(), R.color.red_600_red_300))
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false);

            mNotificationManager.notify(notificationId,mBuilderCompatO.build());
        } else {
            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(MegaApplication.getInstance(), R.color.red_600_red_300))
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
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
                .putExtra(NUMBER_FILES, uploadCount));
    }

    /**
     * Show complete success notification.
     */
    private void showUploadCompleteNotification() {
        logDebug("showUploadCompleteNotification");

        if (isOverquota == NOT_OVERQUOTA_STATE) {
            String notificationTitle = "";
            int errorCount = completed - completedSuccessfully - alreadyUploaded;

            if (completedSuccessfully > 0) {
                notificationTitle = StringResourcesUtils.getQuantityString(R.plurals.upload_service_final_notification,
                        completedSuccessfully, completedSuccessfully);
            }

            if (alreadyUploaded > 0) {
                notificationTitle = addStringSeparator(notificationTitle);
                notificationTitle += StringResourcesUtils.getQuantityString(R.plurals.upload_service_notification_already_uploaded,
                        alreadyUploaded, alreadyUploaded);
            }

            if (errorCount > 0) {
                notificationTitle = addStringSeparator(notificationTitle);
                notificationTitle += StringResourcesUtils.getQuantityString(R.plurals.upload_service_failed,
                        errorCount, errorCount);
            }

            long transferredBytes = getTransferredByte(mapProgressFileTransfers);
            String totalBytes = getSizeString(transferredBytes);
            String size = StringResourcesUtils.getString(R.string.general_total_size, totalBytes);

            notifyNotification(notificationTitle, size, NOTIFICATION_UPLOAD_FINAL, NOTIFICATION_CHANNEL_UPLOAD_ID, NOTIFICATION_CHANNEL_UPLOAD_NAME);
        }
    }

    private void notifyProgressNotification(int progressPercent, String message, String info, String actionString){
        Intent intent = new Intent(UploadService.this, ManagerActivity.class);
        switch (isOverquota) {
            case OVERQUOTA_STORAGE_STATE:
                intent.setAction(ACTION_OVERQUOTA_STORAGE);
                break;

            case PRE_OVERQUOTA_STORAGE_STATE:
                intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
                break;

            case NOT_OVERQUOTA_STATE:
            default:
                intent.setAction(ACTION_SHOW_TRANSFERS);
                intent.putExtra(TRANSFERS_TAB, PENDING_TAB);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(UploadService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_UPLOAD_ID, NOTIFICATION_CHANNEL_UPLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
            NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_UPLOAD_ID);
            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message).setSubText(info)
                    .setContentText(actionString)
                    .setOnlyAlertOnce(true);

            notification = mBuilderCompat.build();
        }
        else {
            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this,R.color.red_600_red_300))
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message).setContentInfo(info)
                    .setContentText(actionString)
                    .setOnlyAlertOnce(true);

            notification = mBuilder.build();
        }

        if (!isForeground) {
			logDebug("Starting foreground");
            try {
                startForeground(NOTIFICATION_UPLOAD, notification);
                isForeground = true;
            }
            catch (Exception e){
				logError("Start foreground exception", e);
                isForeground = false;
            }
        } else {
            mNotificationManager.notify(NOTIFICATION_UPLOAD, notification);
        }
    }

    private void updateProgressNotification() {
        Collection<MegaTransfer> transfers = mapProgressFileTransfers.values();

        UploadProgress up = getInProgressNotification(transfers);
        long total = up.total;
        long inProgress = up.inProgress;
        int progressPercent = 0;
        long inProgressTemp;
        if (total > 0) {
            inProgressTemp = inProgress * 100;
            progressPercent = (int)(inProgressTemp / total);

            showRating(total, megaApi.getCurrentUploadSpeed());
        }

        String message = getMessageForProgressNotification(inProgress);
        logDebug("updateProgressNotification" + progressPercent + " " + message);
        String actionString = isOverquota == NOT_OVERQUOTA_STATE ? getString(R.string.download_touch_to_show) : getString(R.string.general_show_info);
        String info = getProgressSize(UploadService.this,inProgress,total);

        notifyProgressNotification(progressPercent, message, info, actionString);
    }

    /**
     * Determine if should show the rating page to users
     *
     * @param total the total size of uploading file
     * @param currentUploadSpeed current uploading speed
     */
    private void showRating(long total, int currentUploadSpeed) {
        if (!isRatingShowed) {
            new RatingHandlerImpl(this)
                    .showRatingBaseOnSpeedAndSize(total, currentUploadSpeed, () -> isRatingShowed = true);
        }
    }

    private String getMessageForProgressNotification(long inProgress) {
        logDebug("inProgress: " + inProgress);
        String message;
        if (isOverquota != NOT_OVERQUOTA_STATE) {
            message = getString(R.string.overquota_alert_title);
        } else if (inProgress == 0) {
            message = getString(R.string.download_preparing_files);
        } else {
            message = StringResourcesUtils.getString(megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)
                    ? R.string.upload_service_notification_paused
                    : R.string.upload_service_notification, completed + 1, uploadCount);
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
		    if (isCUOrChatTransfer(transfer)) return;

		    launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);
			String appData = transfer.getAppData();

			if(appData!=null){
				return;
			}

            pendingToAddInQueue--;
            transfersManagement.checkScanningTransferOnStart(transfer);

            if (!transfer.isFolderTransfer()) {
                uploadCount++;
                mapProgressFileTransfers.put(transfer.getTag(), transfer);
                updateProgressNotification();
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
        if (isCUOrChatTransfer(transfer)) return;

		launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);

		if (error.getErrorCode() == MegaError.API_EBUSINESSPASTDUE) {
			sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
		}

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
		    if (!transfer.isFolderTransfer()) {
		        AndroidCompletedTransfer completedTransfer = new AndroidCompletedTransfer(transfer, error);
                addCompletedTransfer(completedTransfer, dbH);

                String appData = transfer.getAppData();

                if (!isTextEmpty(appData) && appData.contains(APP_DATA_TXT_FILE)) {
                    sendBroadcast(new Intent(BROADCAST_ACTION_SHOW_SNACKBAR)
                            .putExtra(SNACKBAR_TEXT, getCreationOrEditorText(transfer, error)));
                }

                if (transfer.getState() == MegaTransfer.STATE_FAILED) {
                    transfersManagement.setAreFailedTransfers(true);
                }
            }

            if (transfer.getAppData() != null) {
                if (megaApi.getNumPendingUploads() == 0) {
                    onQueueComplete(false);
                }

                return;
            }

            if (!transfer.isFolderTransfer()) {
                completed++;
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
                    if (!transfer.isFolderTransfer()) {
                        if (transfer.getTransferredBytes() == 0) {
                            alreadyUploaded++;
                        } else {
                            completedSuccessfully++;
                        }
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

					if (error.getErrorCode() == MegaError.API_EOVERQUOTA && !transfer.isForeignOverquota()) {
						isOverquota = OVERQUOTA_STORAGE_STATE;
					}
					else if (error.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
						isOverquota = PRE_OVERQUOTA_STORAGE_STATE;
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

                if (completed == uploadCount && pendingToAddInQueue == 0) {
                    onQueueComplete(true);
                } else {
                    updateProgressNotification();
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
            if (isCUOrChatTransfer(transfer)) return;

		    launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);

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

            transfersManagement.checkScanningTransferOnUpdate(transfer);

            if (!transfer.isFolderTransfer()) {
                mapProgressFileTransfers.put(transfer.getTag(), transfer);
                updateProgressNotification();
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
            if (isCUOrChatTransfer(transfer)) return;

			switch (e.getErrorCode())
			{
				case MegaError.API_EOVERQUOTA:
				case MegaError.API_EGOINGOVERQUOTA:
				    if (transfer.isForeignOverquota()) {
				        break;
                    }

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
                        updateProgressNotification();
					}
					break;
			}
		}
	}

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return true;
    }

    private void showStorageOverQuotaNotification(){
		logDebug("showStorageOverQuotaNotification");
        String contentText = getString(R.string.download_show_info);
		String message = getString(R.string.overquota_alert_title);

		Intent intent = new Intent(this, ManagerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isOverquota == OVERQUOTA_STORAGE_STATE) {
            intent.setAction(ACTION_OVERQUOTA_STORAGE);
        } else {
            intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
        }

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_UPLOAD_ID, NOTIFICATION_CHANNEL_UPLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setShowBadge(true);
			channel.setSound(null, null);
			mNotificationManager.createNotificationChannel(channel);

			NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_UPLOAD_ID);

			mBuilderCompatO
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompatO.build());
		}
		else {
			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
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
		pendingToAddInQueue = 0;
        completed = 0;
        completedSuccessfully = 0;
        alreadyUploaded = 0;
        uploadCount = 0;
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

    /**
     * Checks if a transfer is a CU or Chat transfer.
     *
     * @param transfer MegaTransfer to check
     * @return True if the transfer is a CU or Chat transfer, false otherwise.
     */
    private boolean isCUOrChatTransfer(MegaTransfer transfer) {
        String appData = transfer.getAppData();
        return !isTextEmpty(appData)
                && (appData.contains(APP_DATA_CU)
                || appData.contains(APP_DATA_CHAT));
    }
}
