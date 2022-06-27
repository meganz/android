package mega.privacy.android.app;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CLEAR_OFFLINE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TRANSFER_OVER_QUOTA;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_TRANSFER_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.DOWNLOAD_TRANSFER;
import static mega.privacy.android.app.constants.BroadcastConstants.DOWNLOAD_TRANSFER_OPEN;
import static mega.privacy.android.app.constants.BroadcastConstants.NODE_HANDLE;
import static mega.privacy.android.app.constants.BroadcastConstants.NODE_LOCAL_PATH;
import static mega.privacy.android.app.constants.BroadcastConstants.NODE_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.NUMBER_FILES;
import static mega.privacy.android.app.constants.BroadcastConstants.OFFLINE_AVAILABLE;
import static mega.privacy.android.app.constants.BroadcastConstants.TRANSFER_TYPE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_SERVICE_IF_NO_TRANSFERS;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.WAIT_TIME_BEFORE_UPDATE;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.addCompletedTransfer;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.createInitialServiceNotification;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.launchTransferUpdateIntent;
import static mega.privacy.android.app.main.ManagerActivity.COMPLETED_TAB;
import static mega.privacy.android.app.main.ManagerActivity.PENDING_TAB;
import static mega.privacy.android.app.main.ManagerActivity.TRANSFERS_TAB;
import static mega.privacy.android.app.utils.Constants.ACTION_CANCEL_DOWNLOAD;
import static mega.privacy.android.app.utils.Constants.ACTION_RESTART_SERVICE;
import static mega.privacy.android.app.utils.Constants.ACTION_SHOW_TRANSFERS;
import static mega.privacy.android.app.utils.Constants.APP_DATA_BACKGROUND_TRANSFER;
import static mega.privacy.android.app.utils.Constants.APP_DATA_INDICATOR;
import static mega.privacy.android.app.utils.Constants.APP_DATA_SD_CARD;
import static mega.privacy.android.app.utils.Constants.APP_DATA_VOICE_CLIP;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SETTINGS_UPDATED;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED;
import static mega.privacy.android.app.utils.Constants.ERROR_VOICE_CLIP_TRANSFER;
import static mega.privacy.android.app.utils.Constants.EXTRA_NODE_HANDLE;
import static mega.privacy.android.app.utils.Constants.EXTRA_RESULT_TRANSFER;
import static mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING;
import static mega.privacy.android.app.utils.Constants.EXTRA_TRANSFER_TYPE;
import static mega.privacy.android.app.utils.Constants.HIGH_PRIORITY_TRANSFER;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_DOWNLOAD;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_DOWNLOAD_FINAL;
import static mega.privacy.android.app.utils.Constants.SEPARATOR;
import static mega.privacy.android.app.utils.Constants.SUCCESSFUL_VOICE_CLIP_TRANSFER;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.FileUtil.isFileDownloadedLatest;
import static mega.privacy.android.app.utils.FileUtil.isVideoFile;
import static mega.privacy.android.app.utils.FileUtil.purgeDirectory;
import static mega.privacy.android.app.utils.FileUtil.sendBroadcastToUpdateGallery;
import static mega.privacy.android.app.utils.MegaTransferUtils.getNumPendingDownloadsNonBackground;
import static mega.privacy.android.app.utils.MegaTransferUtils.isBackgroundTransfer;
import static mega.privacy.android.app.utils.MegaTransferUtils.isVoiceClipType;
import static mega.privacy.android.app.utils.OfflineUtils.OFFLINE_DIR;
import static mega.privacy.android.app.utils.OfflineUtils.saveOffline;
import static mega.privacy.android.app.utils.OfflineUtils.saveOfflineChatFile;
import static mega.privacy.android.app.utils.SDCardUtils.getSDCardTargetPath;
import static mega.privacy.android.app.utils.SDCardUtils.getSDCardTargetUri;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.ONTRANSFERUPDATE_REFRESH_MILLIS;
import static mega.privacy.android.app.utils.Util.getProgressSize;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD;

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
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.jeremyliao.liveeventbus.LiveEventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.components.saver.AutoPlayInfo;
import mega.privacy.android.app.fragments.offline.OfflineFragment;
import mega.privacy.android.app.globalmanagement.TransfersManagement;
import mega.privacy.android.app.main.LoginActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.notifications.TransferOverQuotaNotification;
import mega.privacy.android.app.objects.SDTransfer;
import mega.privacy.android.app.service.iar.RatingHandlerImpl;
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase;
import mega.privacy.android.app.utils.CacheFolderManager;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaCancelToken;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferData;
import timber.log.Timber;

/**
 * Background service to download files
 */
@AndroidEntryPoint
public class DownloadService extends Service implements MegaRequestListenerInterface {

    // Action to stop download
    public static final String ACTION_CANCEL = "CANCEL_DOWNLOAD";
    public static final String EXTRA_SIZE = "DOCUMENT_SIZE";
    public static final String EXTRA_HASH = "DOCUMENT_HASH";
    public static final String EXTRA_URL = "DOCUMENT_URL";
    public static final String EXTRA_DOWNLOAD_TO_SDCARD = "download_to_sdcard";
    public static final String EXTRA_TARGET_PATH = "target_path";
    public static final String EXTRA_TARGET_URI = "target_uri";
    public static final String EXTRA_PATH = "SAVE_PATH";
    public static final String EXTRA_FOLDER_LINK = "FOLDER_LINK";
    public static final String EXTRA_FROM_MV = "fromMV";
    public static final String EXTRA_CONTACT_ACTIVITY = "CONTACT_ACTIVITY";
    public static final String EXTRA_OPEN_FILE = "OPEN_FILE";
    public static final String EXTRA_CONTENT_URI = "CONTENT_URI";
    public static final String EXTRA_DOWNLOAD_BY_TAP = "EXTRA_DOWNLOAD_BY_TAP";
    public static final String EXTRA_DOWNLOAD_FOR_OFFLINE = "EXTRA_DOWNLOAD_FOR_OFFLINE";

    @Inject
    GetGlobalTransferUseCase getGlobalTransferUseCase;
    @Inject
    TransfersManagement transfersManagement;

    private static int errorEBloqued = 0;
    private int errorCount = 0;
    private int alreadyDownloaded = 0;

    private boolean isForeground = false;
    private boolean canceled;

    private boolean openFile = true;
    private boolean downloadByTap;
    private String type = "";
    private boolean isOverquota = false;
    private long downloadedBytesToOverquota = 0;
    private MegaNode rootNode;

    MegaApplication app;
    MegaApiAndroid megaApi;
    MegaApiAndroid megaApiFolder;
    MegaChatApiAndroid megaChatApi;

    ArrayList<Intent> pendingIntents = new ArrayList<Intent>();

    WifiLock lock;
    WakeLock wl;

    File currentFile;
    File currentDir;
    MegaNode currentDocument;

    DatabaseHandler dbH = null;

    int transfersCount = 0;
    Set<Integer> backgroundTransfers = new HashSet<>();

    HashMap<Long, Uri> storeToAdvacedDevices;
    HashMap<Long, Boolean> fromMediaViewers;

    private NotificationCompat.Builder mBuilderCompat;
    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    MegaNode offlineNode;

    boolean isLoggingIn = false;
    private long lastUpdated;

    private Intent intent;

    /**
     * the receiver and manager for the broadcast to listen to the pause event
     */
    private BroadcastReceiver pauseBroadcastReceiver;

    private final CompositeDisposable rxSubscriptions = new CompositeDisposable();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // the flag to determine the rating dialog is showed for this download action
    private boolean isRatingShowed;

    private boolean isDownloadForOffline;

    /**
     * Contains the info of a node that to be opened in-app.
     */
    private AutoPlayInfo autoPlayInfo;

    private final Observer<Boolean> stopServiceObserver = finish -> {
        if (finish && megaApi.getNumPendingDownloads() == 0) {
            stopForeground();
        }
    };

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");

        app = MegaApplication.getInstance();
        megaApi = app.getMegaApi();
        megaApi.addRequestListener(this);
        megaApiFolder = app.getMegaApiFolder();
        megaChatApi = app.getMegaChatApi();

        isForeground = false;
        canceled = false;

        storeToAdvacedDevices = new HashMap<Long, Uri>();
        fromMediaViewers = new HashMap<>();

        int wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

        WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        lock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock");
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock");
        mBuilder = new Notification.Builder(DownloadService.this);
        mBuilderCompat = new NotificationCompat.Builder(getApplicationContext());
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        startForeground();

        rootNode = megaApi.getRootNode();

        // delay 1 second to refresh the pause notification to prevent update is missed
        pauseBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new Handler().postDelayed(() -> updateProgressNotification(), WAIT_TIME_BEFORE_UPDATE);
            }
        };

        registerReceiver(pauseBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));

        LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean.class)
                .observeForever(stopServiceObserver);

        Disposable subscription = getGlobalTransferUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((event) -> {
                    if (event instanceof GetGlobalTransferUseCase.Result.OnTransferStart) {
                        MegaTransfer transfer = ((GetGlobalTransferUseCase.Result.OnTransferStart) event).getTransfer();
                        doOnTransferStart(transfer)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(() -> {
                                }, Timber::e);
                        ;
                    } else if (event instanceof GetGlobalTransferUseCase.Result.OnTransferUpdate) {
                        MegaTransfer transfer = ((GetGlobalTransferUseCase.Result.OnTransferUpdate) event).getTransfer();
                        doOnTransferUpdate(transfer)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(() -> {
                                }, Timber::e);
                    } else if (event instanceof GetGlobalTransferUseCase.Result.OnTransferFinish) {
                        MegaTransfer transfer = ((GetGlobalTransferUseCase.Result.OnTransferFinish) event).getTransfer();
                        MegaError error = ((GetGlobalTransferUseCase.Result.OnTransferFinish) event).getError();
                        doOnTransferFinish(transfer, error)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(() -> {
                                }, Timber::e);
                    } else if (event instanceof GetGlobalTransferUseCase.Result.OnTransferTemporaryError) {
                        MegaTransfer transfer = ((GetGlobalTransferUseCase.Result.OnTransferTemporaryError) event).getTransfer();
                        MegaError error = ((GetGlobalTransferUseCase.Result.OnTransferTemporaryError) event).getError();
                        doOnTransferTemporaryError(transfer, error)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(() -> {
                                }, Timber::e);
                    }
                }, Timber::e);
        rxSubscriptions.add(subscription);
    }

    private void startForeground() {
        if (getNumPendingDownloadsNonBackground(megaApi) <= 0) {
            return;
        }

        try {
            startForeground(NOTIFICATION_DOWNLOAD, createInitialServiceNotification(NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                    NOTIFICATION_CHANNEL_DOWNLOAD_NAME, mNotificationManager,
                    new NotificationCompat.Builder(DownloadService.this, NOTIFICATION_CHANNEL_DOWNLOAD_ID),
                    mBuilder));
            isForeground = true;
        } catch (Exception e) {
            Timber.w(e);
            isForeground = false;
        }
    }

    private void stopForeground() {
        isForeground = false;
        stopForeground(true);
        mNotificationManager.cancel(NOTIFICATION_DOWNLOAD);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy");
        if ((lock != null) && (lock.isHeld()))
            try {
                lock.release();
            } catch (Exception ex) {
            }
        if ((wl != null) && (wl.isHeld()))
            try {
                wl.release();
            } catch (Exception ex) {
            }

        if (megaApi != null) {
            megaApi.removeRequestListener(this);
        }

        if (megaChatApi != null) {
            megaChatApi.saveCurrentState();
        }

        rootNode = null;
        // remove all the generated folders in cache folder on SD card.
        File[] fs = getExternalCacheDirs();
        if (fs.length > 1 && fs[1] != null) {
            purgeDirectory(fs[1]);
        }

        unregisterReceiver(pauseBroadcastReceiver);
        rxSubscriptions.clear();
        stopForeground();

        LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean.class)
                .removeObserver(stopServiceObserver);

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        canceled = false;

        if (intent == null) {
            Timber.w("intent==null");
            return START_NOT_STICKY;
        }

        if (intent.getAction() != null && intent.getAction().equals(ACTION_CANCEL)) {
            Timber.d("Cancel intent");
            canceled = true;
            megaApi.cancelTransfers(TYPE_DOWNLOAD);
            return START_NOT_STICKY;
        }

        rxSubscriptions.add(Single.just(intent)
                .observeOn(Schedulers.single())
                .subscribe(this::onHandleIntent, Timber::e));
        return START_NOT_STICKY;
    }

    protected void onHandleIntent(final Intent intent) {
        Timber.d("onHandleIntent");
        this.intent = intent;

        if (intent.getAction() != null && intent.getAction().equals(ACTION_RESTART_SERVICE)) {
            MegaTransferData transferData = megaApi.getTransferData(null);
            if (transferData == null) {
                stopForeground();
                return;
            }

            int uploadsInProgress = transferData.getNumDownloads();

            for (int i = 0; i < uploadsInProgress; i++) {
                MegaTransfer transfer = megaApi.getTransferByTag(transferData.getDownloadTag(i));
                if (transfer == null) {
                    continue;
                }

                if (!isVoiceClipType(transfer) && !isBackgroundTransfer(transfer)) {
                    transfersManagement.checkIfTransferIsPaused(transfer);
                    transfersCount++;
                }
            }

            if (transfersCount > 0) {
                updateProgressNotification();
            } else {
                stopForeground();
            }

            launchTransferUpdateIntent(TYPE_DOWNLOAD);
            return;
        }

        long hash = intent.getLongExtra(EXTRA_HASH, -1);
        String url = intent.getStringExtra(EXTRA_URL);
        isDownloadForOffline = intent.getBooleanExtra(EXTRA_DOWNLOAD_FOR_OFFLINE, false);
        boolean isFolderLink = intent.getBooleanExtra(EXTRA_FOLDER_LINK, false);
        openFile = intent.getBooleanExtra(EXTRA_OPEN_FILE, true);
        downloadByTap = intent.getBooleanExtra(EXTRA_DOWNLOAD_BY_TAP, false);
        type = intent.getStringExtra(EXTRA_TRANSFER_TYPE);

        Uri contentUri = null;
        if (intent.getStringExtra(EXTRA_CONTENT_URI) != null) {
            contentUri = Uri.parse(intent.getStringExtra(EXTRA_CONTENT_URI));
        }

        boolean highPriority = intent.getBooleanExtra(HIGH_PRIORITY_TRANSFER, false);
        boolean fromMV = intent.getBooleanExtra(EXTRA_FROM_MV, false);
        Timber.d("fromMV: %s", fromMV);

        megaApi = app.getMegaApi();

        UserCredentials credentials = dbH.getCredentials();

        if (credentials != null) {

            String gSession = credentials.getSession();
            if (rootNode == null) {
                rootNode = megaApi.getRootNode();
                isLoggingIn = MegaApplication.isLoggingIn();
                if (!isLoggingIn) {
                    isLoggingIn = true;
                    MegaApplication.setLoggingIn(isLoggingIn);

                    ChatUtil.initMegaChatApi(gSession);

                    pendingIntents.add(intent);
                    if (type == null || (!type.contains(APP_DATA_VOICE_CLIP) && !type.contains(APP_DATA_BACKGROUND_TRANSFER))) {
                        updateProgressNotification();
                    }

                    megaApi.fastLogin(gSession);
                    return;
                } else {
                    Timber.w("Another login is processing");
                }
                pendingIntents.add(intent);
                return;
            }
        }

        String serialize = intent.getStringExtra(EXTRA_SERIALIZE_STRING);

        if (serialize != null) {
            Timber.d("serializeString: %s", serialize);
            currentDocument = MegaNode.unserialize(serialize);
            if (currentDocument != null) {
                hash = currentDocument.getHandle();
                Timber.d("hash after unserialize: %s", hash);
            } else {
                Timber.w("Node is NULL after unserialize");
            }
        } else if (isFolderLink) {
            currentDocument = megaApiFolder.getNodeByHandle(hash);
        } else {
            currentDocument = megaApi.getNodeByHandle(hash);
        }

        if (url != null) {
            Timber.d("Public node");
            currentDir = new File(intent.getStringExtra(EXTRA_PATH));
            if (currentDir != null) {
                currentDir.mkdirs();
            }
            megaApi.getPublicNode(url);
            return;
        }

        if ((currentDocument == null) && (url == null)) {
            Timber.w("Node not found");
            return;
        }

        fromMediaViewers.put(currentDocument.getHandle(), fromMV);

        currentDir = getDir(currentDocument, intent);
        currentDir.mkdirs();
        if (currentDir.isDirectory()) {
            currentFile = new File(currentDir, megaApi.escapeFsIncompatible(currentDocument.getName(), currentDir.getAbsolutePath() + SEPARATOR));
        } else {
            currentFile = currentDir;
        }

        String appData = getSDCardAppData(intent);

        if (!checkCurrentFile(currentDocument)) {
            Timber.d("checkCurrentFile == false");

            alreadyDownloaded++;
            if (getNumPendingDownloadsNonBackground(megaApi) == 0) {
                onQueueComplete(currentDocument.getHandle());
            }

            return;
        }

        if (!wl.isHeld()) {
            wl.acquire();
        }
        if (!lock.isHeld()) {
            lock.acquire();
        }

        if (contentUri != null || currentDir.isDirectory()) {
            if (contentUri != null) {
                //To download to Advanced Devices
                currentDir = new File(intent.getStringExtra(EXTRA_PATH));
                currentDir.mkdirs();

                if (!currentDir.isDirectory()) {
                    Timber.w("currentDir is not a directory");
                }

                storeToAdvacedDevices.put(currentDocument.getHandle(), contentUri);
            } else if (currentFile.exists()) {
                //Check the fingerprint
                String localFingerprint = megaApi.getFingerprint(currentFile.getAbsolutePath());
                String megaFingerprint = currentDocument.getFingerprint();

                if (!isTextEmpty(localFingerprint)
                        && !isTextEmpty(megaFingerprint)
                        && localFingerprint.equals(megaFingerprint)) {
                    Timber.d("Delete the old version");
                    currentFile.delete();
                }
            }

            if (currentDir.getAbsolutePath().contains(OFFLINE_DIR)) {
//			Save for offline: do not open when finishes
                openFile = false;
            }

            if (isFolderLink) {
                currentDocument = megaApiFolder.authorizeNode(currentDocument);
            }

            if (transfersManagement.isOnTransferOverQuota()) {
                checkTransferOverQuota(false);
            }

            Timber.d("CurrentDocument is not null");

            if (isTextEmpty(appData)) {
                appData = type != null && type.contains(APP_DATA_VOICE_CLIP) ? APP_DATA_VOICE_CLIP : "";
            }

            String localPath = currentDir.getAbsolutePath() + "/";
            MegaCancelToken token = transfersManagement.addScanningTransfer(TYPE_DOWNLOAD,
                    localPath, currentDocument, currentDocument.isFolder());

            if (token != null) {
                megaApi.startDownload(currentDocument, localPath, appData, null, highPriority, token);
            }
        } else {
            Timber.w("currentDir is not a directory");
        }
    }

    /**
     * Checks if the download of the current Intent corresponds to a SD card download.
     * If so, stores the SD card paths on an app data String.
     * If not, do nothing.
     *
     * @param intent Current Intent.
     * @return The app data String.
     */
    private String getSDCardAppData(Intent intent) {
        if (intent == null
                || !intent.getBooleanExtra(EXTRA_DOWNLOAD_TO_SDCARD, false)) {
            return null;
        }

        String sDCardAppData = APP_DATA_SD_CARD;

        String targetPath = intent.getStringExtra(EXTRA_TARGET_PATH);
        if (!isTextEmpty(targetPath)) {
            sDCardAppData += APP_DATA_INDICATOR + targetPath;
        }

        String targetUri = intent.getStringExtra(EXTRA_TARGET_URI);
        if (!isTextEmpty(targetUri)) {
            sDCardAppData += APP_DATA_INDICATOR + targetUri;
        }

        return sDCardAppData;
    }

    private void onQueueComplete(long handle) {
        Timber.d("onQueueComplete");

        if ((lock != null) && (lock.isHeld()))
            try {
                lock.release();
            } catch (Exception ex) {
            }
        if ((wl != null) && (wl.isHeld()))
            try {
                wl.release();
            } catch (Exception ex) {
            }

        showCompleteNotification(handle);
        stopForeground();
        rootNode = null;
        int pendingDownloads = getNumPendingDownloadsNonBackground(megaApi);
        Timber.d("onQueueComplete: total of files before reset %s", pendingDownloads);
        if (pendingDownloads <= 0) {
            Timber.d("onQueueComplete: reset total downloads");
            // When download a single file by tapping it, and auto play is enabled.
            int totalDownloads = megaApi.getTotalDownloads() - backgroundTransfers.size();
            if (totalDownloads == 1 && Boolean.parseBoolean(dbH.getAutoPlayEnabled()) && autoPlayInfo != null && downloadByTap) {
                sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
                        .putExtra(TRANSFER_TYPE, DOWNLOAD_TRANSFER_OPEN)
                        .putExtra(NODE_NAME, autoPlayInfo.getNodeName())
                        .putExtra(NODE_HANDLE, autoPlayInfo.getNodeHandle())
                        .putExtra(NUMBER_FILES, 1)
                        .putExtra(NODE_LOCAL_PATH, autoPlayInfo.getLocalPath()));
            } else if (totalDownloads > 0) {
                Intent intent = new Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
                        .putExtra(TRANSFER_TYPE, DOWNLOAD_TRANSFER)
                        .putExtra(NUMBER_FILES, totalDownloads);
                if (isDownloadForOffline) {
                    intent.putExtra(OFFLINE_AVAILABLE, true);
                }
                sendBroadcast(intent);
            }
            sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE));

            megaApi.resetTotalDownloads();
            backgroundTransfers.clear();
            errorEBloqued = 0;
            errorCount = 0;
            alreadyDownloaded = 0;
        }
    }

    private void sendTakenDownAlert() {
        if (errorEBloqued <= 0) return;

        Intent intent = new Intent(BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES);
        intent.putExtra(NUMBER_FILES, errorEBloqued);
        sendBroadcast(intent);
    }

    private File getDir(MegaNode document, Intent intent) {
        boolean toDownloads = (intent.hasExtra(EXTRA_PATH) == false);
        File destDir;
        if (toDownloads) {
            destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            destDir = new File(intent.getStringExtra(EXTRA_PATH));
        }
        return destDir;
    }

    boolean checkCurrentFile(MegaNode document) {
        Timber.d("checkCurrentFile");
        if (currentFile.exists()
                && document.getSize() == currentFile.length()
                && isFileDownloadedLatest(currentFile, document)) {

            currentFile.setReadable(true, false);

            return false;
        }

        if (document.getSize() > ((long) 1024 * 1024 * 1024 * 4)) {
            Timber.d("Show size alert: %s", document.getSize());
            uiHandler.post(() -> Toast.makeText(getApplicationContext(),
                    getString(R.string.error_file_size_greater_than_4gb),
                    Toast.LENGTH_LONG).show());
        }

        return true;
    }

    /*
     * Show download success notification
     */
    private void showCompleteNotification(long handle) {
        Timber.d("showCompleteNotification");
        String notificationTitle, size;

        int totalDownloads = megaApi.getTotalDownloads() - backgroundTransfers.size();

        if (alreadyDownloaded > 0 && errorCount > 0) {
            int totalNumber = totalDownloads + errorCount + alreadyDownloaded;
            notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification_with_details, totalNumber, totalDownloads, totalNumber);

            String copiedString = getResources().getQuantityString(R.plurals.already_downloaded_service, alreadyDownloaded, alreadyDownloaded);
            ;
            String errorString = getResources().getQuantityString(R.plurals.upload_service_failed, errorCount, errorCount);
            size = copiedString + ", " + errorString;
        } else if (alreadyDownloaded > 0) {
            int totalNumber = totalDownloads + alreadyDownloaded;
            notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification_with_details, totalNumber, totalDownloads, totalNumber);

            size = getResources().getQuantityString(R.plurals.already_downloaded_service, alreadyDownloaded, alreadyDownloaded);
        } else if (errorCount > 0) {
            sendTakenDownAlert();
            int totalNumber = totalDownloads + errorCount;
            notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification_with_details, totalNumber, totalDownloads, totalNumber);

            size = getResources().getQuantityString(R.plurals.download_service_failed, errorCount, errorCount);
        } else {
            notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification, totalDownloads, totalDownloads);
            String totalBytes = getSizeString(megaApi.getTotalDownloadedBytes());
            size = getString(R.string.general_total_size, totalBytes);
        }

        Intent intent = new Intent(getApplicationContext(), ManagerActivity.class);
        intent.setAction(ACTION_SHOW_TRANSFERS);
        intent.putExtra(TRANSFERS_TAB, COMPLETED_TAB);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (totalDownloads != 1) {
            Timber.d("Show notification");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setShowBadge(true);
                channel.setSound(null, null);
                mNotificationManager.createNotificationChannel(channel);

                NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

                mBuilderCompatO
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true).setTicker(notificationTitle)
                        .setContentTitle(notificationTitle).setContentText(size)
                        .setOngoing(false);

                mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
            } else {
                mBuilderCompat
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true).setTicker(notificationTitle)
                        .setContentTitle(notificationTitle).setContentText(size)
                        .setOngoing(false);

                mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
            }
        } else {
            try {
                boolean autoPlayEnabled = Boolean.parseBoolean(dbH.getAutoPlayEnabled());
                if (openFile && autoPlayEnabled) {
                    String fileLocalPath;
                    String path = getLocalFile(megaApi.getNodeByHandle(handle));
                    if (path != null) {
                        fileLocalPath = path;
                    } else {
                        fileLocalPath = currentFile.getAbsolutePath();
                    }

                    autoPlayInfo = new AutoPlayInfo(currentDocument.getName(), currentDocument.getHandle(), fileLocalPath, true);

                    Timber.d("Both openFile and autoPlayEnabled are true");
                    boolean fromMV = false;
                    if (fromMediaViewers.containsKey(handle)) {
                        Boolean result = fromMediaViewers.get(handle);
                        fromMV = result != null && result;
                    }

                    if (MimeTypeList.typeForName(currentFile.getName()).isPdf()) {
                        Timber.d("Pdf file");

                        if (fromMV) {
                            Timber.d("Show notification");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                                channel.setShowBadge(true);
                                channel.setSound(null, null);
                                mNotificationManager.createNotificationChannel(channel);

                                NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

                                mBuilderCompatO
                                        .setSmallIcon(R.drawable.ic_stat_notify)
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true).setTicker(notificationTitle)
                                        .setContentTitle(notificationTitle).setContentText(size)
                                        .setOngoing(false);

                                mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
                            } else {
                                mBuilderCompat
                                        .setSmallIcon(R.drawable.ic_stat_notify)
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true).setTicker(notificationTitle)
                                        .setContentTitle(notificationTitle).setContentText(size)
                                        .setOngoing(false);

                                mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
                            }
                        }
                    } else if (MimeTypeList.typeForName(currentFile.getName()).isVideoReproducible() || MimeTypeList.typeForName(currentFile.getName()).isAudio()) {
                        Timber.d("Video/Audio file");
                        if (fromMV) {
                            Timber.d("Show notification");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                                channel.setShowBadge(true);
                                channel.setSound(null, null);
                                mNotificationManager.createNotificationChannel(channel);

                                NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

                                mBuilderCompatO
                                        .setSmallIcon(R.drawable.ic_stat_notify)
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true).setTicker(notificationTitle)
                                        .setContentTitle(notificationTitle).setContentText(size)
                                        .setOngoing(false);

                                mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
                            } else {
                                mBuilderCompat
                                        .setSmallIcon(R.drawable.ic_stat_notify)
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true).setTicker(notificationTitle)
                                        .setContentTitle(notificationTitle).setContentText(size)
                                        .setOngoing(false);

                                mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
                            }
                        }
                    } else if (MimeTypeList.typeForName(currentFile.getName()).isImage()) {
                        Timber.d("Download is IMAGE");
                        if (fromMV) {
                            Timber.d("Show notification");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                                channel.setShowBadge(true);
                                channel.setSound(null, null);
                                mNotificationManager.createNotificationChannel(channel);

                                NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

                                mBuilderCompatO
                                        .setSmallIcon(R.drawable.ic_stat_notify)
                                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true).setTicker(notificationTitle)
                                        .setContentTitle(notificationTitle).setContentText(size)
                                        .setOngoing(false);

                                mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
                            } else {
                                mBuilderCompat
                                        .setSmallIcon(R.drawable.ic_stat_notify)
                                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true).setTicker(notificationTitle)
                                        .setContentTitle(notificationTitle).setContentText(size)
                                        .setOngoing(false);

                                mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
                            }
                        }

                    } else {
                        Timber.d("Show notification");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                            channel.setShowBadge(true);
                            channel.setSound(null, null);
                            mNotificationManager.createNotificationChannel(channel);

                            NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

                            mBuilderCompatO
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false);

                            mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
                        } else {
                            mBuilderCompat
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false);

                            mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
                        }
                    }
                } else {
                    openFile = true; //Set the openFile to the default

                    Timber.d("Show notification");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                        channel.setShowBadge(true);
                        channel.setSound(null, null);
                        mNotificationManager.createNotificationChannel(channel);

                        NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

                        mBuilderCompatO
                                .setSmallIcon(R.drawable.ic_stat_notify)
                                .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true).setTicker(notificationTitle)
                                .setContentTitle(notificationTitle).setContentText(size)
                                .setOngoing(false);

                        mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
                    } else {
                        mBuilderCompat
                                .setSmallIcon(R.drawable.ic_stat_notify)
                                .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true).setTicker(notificationTitle)
                                .setContentTitle(notificationTitle).setContentText(size)
                                .setOngoing(false);

                        mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
                    }
                }
            } catch (Exception e) {
                openFile = true; //Set the openFile to the default
                Timber.e(e);

                Timber.d("Show notification");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setShowBadge(true);
                    channel.setSound(null, null);
                    mNotificationManager.createNotificationChannel(channel);

                    NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

                    mBuilderCompatO
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true).setTicker(notificationTitle)
                            .setContentTitle(notificationTitle).setContentText(size)
                            .setOngoing(false);

                    mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
                } else {
                    mBuilderCompat
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true).setTicker(notificationTitle)
                            .setContentTitle(notificationTitle).setContentText(size)
                            .setOngoing(false);

                    mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
                }
            }
        }
    }


    /*
     * Update notification download progress
     */
    @SuppressLint("NewApi")
    private void updateProgressNotification() {
        int pendingTransfers = getNumPendingDownloadsNonBackground(megaApi);
        int totalTransfers = megaApi.getTotalDownloads() - backgroundTransfers.size();

        long totalSizePendingTransfer = megaApi.getTotalDownloadBytes();
        long totalSizeTransferred = megaApi.getTotalDownloadedBytes();

        boolean update;

        if (isOverquota) {
            Timber.d("Overquota flag! is TRUE");
            if (downloadedBytesToOverquota <= totalSizeTransferred) {
                update = false;
            } else {
                update = true;
                Timber.d("Change overquota flag");
                isOverquota = false;
            }
        } else {
            Timber.d("NOT overquota flag");
            update = true;
        }

        if (update) {
            /* refresh UI every 1 seconds to avoid too much workload on main thread
             * while in paused status, the update should not be avoided*/
            if (!isOverquota) {
                long now = System.currentTimeMillis();
                if (now - lastUpdated > ONTRANSFERUPDATE_REFRESH_MILLIS || megaApi.areTransfersPaused(TYPE_DOWNLOAD)) {
                    lastUpdated = now;
                } else {
                    return;
                }
            }
            int progressPercent = (int) Math.round((double) totalSizeTransferred / totalSizePendingTransfer * 100);
            Timber.d("Progress: %d%%", progressPercent);

            showRating(totalSizePendingTransfer, megaApi.getCurrentDownloadSpeed());

            String message = "";
            if (totalTransfers == 0) {
                message = getString(R.string.download_preparing_files);
            } else {
                int inProgress = pendingTransfers == 0 ? totalTransfers
                        : totalTransfers - pendingTransfers + 1;

                if (megaApi.areTransfersPaused(TYPE_DOWNLOAD)) {
                    message = StringResourcesUtils.getString(R.string.download_service_notification_paused, inProgress, totalTransfers);
                } else {
                    message = StringResourcesUtils.getString(R.string.download_service_notification, inProgress, totalTransfers);
                }
            }

            Intent intent;
            PendingIntent pendingIntent;

            String info = getProgressSize(DownloadService.this, totalSizeTransferred, totalSizePendingTransfer);

            Notification notification = null;

            String contentText = "";

            if (dbH.getCredentials() == null) {
                contentText = getString(R.string.download_touch_to_cancel);
                intent = new Intent(DownloadService.this, LoginActivity.class);
                intent.setAction(ACTION_CANCEL_DOWNLOAD);
                pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                contentText = getString(R.string.download_touch_to_show);
                intent = new Intent(DownloadService.this, ManagerActivity.class);
                intent.setAction(ACTION_SHOW_TRANSFERS);
                intent.putExtra(TRANSFERS_TAB, PENDING_TAB);
                pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setShowBadge(true);
                channel.setSound(null, null);
                mNotificationManager.createNotificationChannel(channel);

                NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

                mBuilderCompat
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                        .setProgress(100, progressPercent, false)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true).setContentTitle(message).setSubText(info)
                        .setContentText(contentText)
                        .setOnlyAlertOnce(true);

                notification = mBuilderCompat.build();
            } else {
                mBuilder
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                        .setProgress(100, progressPercent, false)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true).setContentTitle(message).setContentInfo(info)
                        .setContentText(contentText)
                        .setOnlyAlertOnce(true);

                notification = mBuilder.build();
            }

            if (!isForeground) {
                Timber.d("Starting foreground!");
                try {
                    startForeground(NOTIFICATION_DOWNLOAD, notification);
                    isForeground = true;
                } catch (Exception e) {
                    isForeground = false;
                }
            } else {
                mNotificationManager.notify(NOTIFICATION_DOWNLOAD, notification);
            }
        }
    }

    /**
     * Determine if should show the rating page to users
     *
     * @param total                the total size of uploading file
     * @param currentDownloadSpeed current downloading speed
     */
    private void showRating(long total, int currentDownloadSpeed) {
        if (!isRatingShowed) {
            new RatingHandlerImpl(this)
                    .showRatingBaseOnSpeedAndSize(total, currentDownloadSpeed, () -> isRatingShowed = true);
        }
    }

    private void cancel() {
        Timber.d("cancel");
        canceled = true;
        stopForeground();
        rootNode = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Completable doOnTransferStart(@Nullable MegaTransfer transfer) {
        return Completable.fromCallable(() -> {
            Timber.d("Download start: %d, totalDownloads: %d", transfer.getNodeHandle(), megaApi.getTotalDownloads());

            if (transfer.isStreamingTransfer() || isVoiceClipType(transfer)) return null;
            if (isBackgroundTransfer(transfer)) {
                backgroundTransfers.add(transfer.getTag());
                return null;
            }

            if (transfer.getType() == TYPE_DOWNLOAD) {
                String appData = transfer.getAppData();

                if (!isTextEmpty(appData) && appData.contains(APP_DATA_SD_CARD)) {
                    dbH.addSDTransfer(new SDTransfer(
                            transfer.getTag(),
                            transfer.getFileName(),
                            getSizeString(transfer.getTotalBytes()),
                            Long.toString(transfer.getNodeHandle()),
                            transfer.getPath(),
                            appData));
                }

                transfersManagement.checkScanningTransferOnStart(transfer);
                launchTransferUpdateIntent(TYPE_DOWNLOAD);
                transfersCount++;
                updateProgressNotification();
            }
            return null;
        });
    }

    private Completable doOnTransferFinish(@Nullable MegaTransfer transfer, MegaError error) {
        return Completable.fromCallable(() -> {
            Timber.d("Node handle: %d, Type = %d", transfer.getNodeHandle(), transfer.getType());

            if (transfer.isStreamingTransfer()) {
                return null;
            }

            if (error.getErrorCode() == MegaError.API_EBUSINESSPASTDUE) {
                sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
            }

            if (transfer.getType() == TYPE_DOWNLOAD) {
                boolean isVoiceClip = isVoiceClipType(transfer);
                boolean isBackgroundTransfer = isBackgroundTransfer(transfer);

                if (!isVoiceClip && !isBackgroundTransfer) transfersCount--;

                String path = transfer.getPath();
                String targetPath = getSDCardTargetPath(transfer.getAppData());

                if (!transfer.isFolderTransfer()) {
                    if (!isVoiceClip && !isBackgroundTransfer) {
                        AndroidCompletedTransfer completedTransfer = new AndroidCompletedTransfer(transfer, error);
                        if (!isTextEmpty(targetPath)) {
                            completedTransfer.setPath(targetPath);
                        }

                        addCompletedTransfer(completedTransfer, dbH);
                    }

                    launchTransferUpdateIntent(TYPE_DOWNLOAD);
                    if (transfer.getState() == MegaTransfer.STATE_FAILED) {
                        transfersManagement.setAreFailedTransfers(true);
                    }

                    if (!isVoiceClip && !isBackgroundTransfer) {
                        updateProgressNotification();
                    }
                }

                if (canceled) {
                    if ((lock != null) && (lock.isHeld()))
                        try {
                            lock.release();
                        } catch (Exception ex) {
                        }
                    if ((wl != null) && (wl.isHeld()))
                        try {
                            wl.release();
                        } catch (Exception ex) {
                        }

                    Timber.d("Download canceled: %s", transfer.getNodeHandle());

                    if (isVoiceClip) {
                        resultTransfersVoiceClip(transfer.getNodeHandle(), ERROR_VOICE_CLIP_TRANSFER);
                        File localFile = CacheFolderManager.buildVoiceClipFile(this, transfer.getFileName());
                        if (isFileAvailable(localFile)) {
                            Timber.d("Delete own voiceclip : exists");
                            localFile.delete();
                        }
                    } else {
                        File file = new File(transfer.getPath());
                        file.delete();
                    }
                    DownloadService.this.cancel();

                } else {
                    if (error.getErrorCode() == MegaError.API_OK) {
                        Timber.d("Download OK - Node handle: %s", transfer.getNodeHandle());

                        if (isVoiceClip) {
                            resultTransfersVoiceClip(transfer.getNodeHandle(), SUCCESSFUL_VOICE_CLIP_TRANSFER);
                        }

                        //need to move downloaded file to a location on sd card.
                        if (targetPath != null) {
                            File source = new File(path);

                            try {
                                SDCardOperator sdCardOperator = new SDCardOperator(this);
                                sdCardOperator.moveDownloadedFileToDestinationPath(source, targetPath,
                                        getSDCardTargetUri(transfer.getAppData()), transfer.getTag());
                            } catch (Exception e) {
                                Timber.e(e, "Error moving file to the sd card path.");
                            }
                        }
                        //To update thumbnails for videos
                        if (isVideoFile(transfer.getPath())) {
                            Timber.d("Is video!!!");
                            MegaNode videoNode = megaApi.getNodeByHandle(transfer.getNodeHandle());
                            if (videoNode != null) {
                                if (!videoNode.hasThumbnail()) {
                                    Timber.d("The video has not thumb");
                                    ThumbnailUtils.createThumbnailVideo(this, path, megaApi, transfer.getNodeHandle());
                                }
                            } else {
                                Timber.w("videoNode is NULL");
                            }
                        } else {
                            Timber.d("NOT video!");
                        }

                        if (!isTextEmpty(path)) {
                            sendBroadcastToUpdateGallery(this, new File(path));
                        }

                        if (storeToAdvacedDevices.containsKey(transfer.getNodeHandle())) {
                            Timber.d("Now copy the file to the SD Card");
                            openFile = false;
                            Uri tranfersUri = storeToAdvacedDevices.get(transfer.getNodeHandle());
                            MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
                            alterDocument(tranfersUri, node.getName());
                        }

                        if (!isTextEmpty(path) && path.contains(OFFLINE_DIR)) {
                            Timber.d("It is Offline file");
                            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
                            offlineNode = megaApi.getNodeByHandle(transfer.getNodeHandle());

                            if (offlineNode != null) {
                                saveOffline(this, megaApi, dbH, offlineNode, transfer.getPath());
                            } else {
                                saveOfflineChatFile(dbH, transfer);
                            }

                            refreshOfflineFragment();
                            refreshSettingsFragment();
                        }
                    } else {
                        Timber.e("Download ERROR: %s", transfer.getNodeHandle());
                        if (isVoiceClip) {
                            resultTransfersVoiceClip(transfer.getNodeHandle(), ERROR_VOICE_CLIP_TRANSFER);
                            File localFile = CacheFolderManager.buildVoiceClipFile(this, transfer.getFileName());
                            if (isFileAvailable(localFile)) {
                                Timber.d("Delete own voice clip : exists");
                                localFile.delete();
                            }
                        } else {
                            if (error.getErrorCode() == MegaError.API_EBLOCKED) {
                                errorEBloqued++;
                            }

                            if (!transfer.isFolderTransfer()) {
                                errorCount++;
                            }

                            if (!isTextEmpty(transfer.getPath())) {
                                File file = new File(transfer.getPath());
                                file.delete();
                            }
                        }
                    }
                }

                if (isVoiceClip || isBackgroundTransfer) return null;

                if (getNumPendingDownloadsNonBackground(megaApi) == 0 && transfersCount == 0) {
                    onQueueComplete(transfer.getNodeHandle());
                }
            }
            return null;
        });
    }

    private void resultTransfersVoiceClip(long nodeHandle, int result) {
        Timber.d("nodeHandle =  %d, the result is %d", nodeHandle, result);
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED);
        intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
        intent.putExtra(EXTRA_RESULT_TRANSFER, result);
        sendBroadcast(intent);
    }

    private void alterDocument(Uri uri, String fileName) {
        Timber.d("alterUri");
        try {

            File tempFolder = CacheFolderManager.getCacheFolder(getApplicationContext(), CacheFolderManager.TEMPORARY_FOLDER);
            if (!isFileAvailable(tempFolder)) return;

            String sourceLocation = tempFolder.getAbsolutePath() + File.separator + fileName;

            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

            InputStream in = new FileInputStream(sourceLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                fileOutputStream.write(buf, 0, len);
            }
            in.close();

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

    private Completable doOnTransferUpdate(@Nullable MegaTransfer transfer) {
        return Completable.fromCallable(() -> {
            if (transfer.getType() == TYPE_DOWNLOAD) {
                launchTransferUpdateIntent(TYPE_DOWNLOAD);
                if (canceled) {
                    Timber.d("Transfer cancel: %s", transfer.getNodeHandle());

                    if ((lock != null) && (lock.isHeld()))
                        try {
                            lock.release();
                        } catch (Exception ex) {
                        }
                    if ((wl != null) && (wl.isHeld()))
                        try {
                            wl.release();
                        } catch (Exception ex) {
                        }

                    megaApi.cancelTransfer(transfer);
                    DownloadService.this.cancel();
                    return null;
                }

                if (transfer.isStreamingTransfer() || isVoiceClipType(transfer)) return null;

                if (isBackgroundTransfer(transfer)) {
                    backgroundTransfers.add(transfer.getTag());
                    return null;
                }

                transfersManagement.checkScanningTransferOnUpdate(transfer);

                if (!transfer.isFolderTransfer()) {
                    sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE));
                    updateProgressNotification();
                }

                if (!transfersManagement.isOnTransferOverQuota() && transfersManagement.hasNotToBeShowDueToTransferOverQuota()) {
                    transfersManagement.setHasNotToBeShowDueToTransferOverQuota(false);
                }
            }
            return null;
        });
    }

    private Completable doOnTransferTemporaryError(@Nullable MegaTransfer transfer, MegaError e) {
        return Completable.fromCallable(() -> {
            Timber.w("Download Temporary Error - Node Handle: %d\nError: %d %s", transfer.getNodeHandle(), e.getErrorCode(), e.getErrorString());

            if (transfer.isStreamingTransfer() || isBackgroundTransfer(transfer)) {
                return null;
            }

            if (transfer.getType() == TYPE_DOWNLOAD) {
                if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
                    if (e.getValue() != 0) {
                        Timber.w("TRANSFER OVERQUOTA ERROR: %s", e.getErrorCode());
                        checkTransferOverQuota(true);

                        downloadedBytesToOverquota = megaApi.getTotalDownloadedBytes();
                        isOverquota = true;
                    }
                }
            }
            return null;
        });
    }

    /**
     * Checks if should show transfer over quota warning.
     * If so, sends a broadcast to show it in the current view.
     *
     * @param isCurrentOverQuota true if the overquota is currently received, false otherwise
     */
    private void checkTransferOverQuota(boolean isCurrentOverQuota) {
        if (app.isActivityVisible()) {
            if (transfersManagement.shouldShowTransferOverQuotaWarning()) {
                transfersManagement.setCurrentTransferOverQuota(isCurrentOverQuota);
                transfersManagement.setTransferOverQuotaTimestamp();
                sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE).setAction(ACTION_TRANSFER_OVER_QUOTA));
            }
        } else if (!transfersManagement.isTransferOverQuotaNotificationShown()) {
            transfersManagement.setTransferOverQuotaNotificationShown(true);
            isForeground = false;
            stopForeground(true);
            mNotificationManager.cancel(NOTIFICATION_DOWNLOAD);
            new TransferOverQuotaNotification(transfersManagement).show();
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        Timber.d("onRequestStart: %s", request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("onRequestFinish");

        if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS) {
            Timber.d("TYPE_CANCEL_TRANSFERS finished");
            if (e.getErrorCode() == MegaError.API_OK) {
                cancel();
            }
        } else if (request.getType() == MegaRequest.TYPE_LOGIN) {
            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("Logged in. Setting account auth token for folder links.");
                megaApiFolder.setAccountAuth(megaApi.getAccountAuth());
                Timber.d("Fast login OK, Calling fetchNodes from CameraSyncService");
                megaApi.fetchNodes();

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies();
            } else {
                Timber.e("ERROR: %s", e.getErrorString());
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);
            }
        } else if (request.getType() == MegaRequest.TYPE_FETCH_NODES) {
            if (e.getErrorCode() == MegaError.API_OK) {
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);

                for (int i = 0; i < pendingIntents.size(); i++) {
                    onHandleIntent(pendingIntents.get(i));
                }
                pendingIntents.clear();
            } else {
                Timber.e("ERROR: %s", e.getErrorString());
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);
            }
        } else {
            Timber.d("Public node received");

            if (e.getErrorCode() != MegaError.API_OK) {
                Timber.e("Public node error");
                return;
            }

            MegaNode node = request.getPublicMegaNode();
            if (node == null) {
                Timber.e("Public node is null");
                return;
            }

            if (currentDir == null) {
                Timber.e("currentDir is null");
                return;
            }

            if (currentDir.isDirectory()) {
                currentFile = new File(currentDir, megaApi.escapeFsIncompatible(node.getName(), currentDir.getAbsolutePath() + SEPARATOR));
            } else {
                currentFile = currentDir;
            }

            String appData = getSDCardAppData(intent);

            Timber.d("Public node download launched");
            if (!wl.isHeld()) wl.acquire();
            if (!lock.isHeld()) lock.acquire();
            if (currentDir.isDirectory()) {
                Timber.d("To downloadPublic(dir)");
                String localPath = currentDir.getAbsolutePath() + "/";
                MegaCancelToken token = transfersManagement.addScanningTransfer(TYPE_DOWNLOAD,
                        localPath, currentDocument, currentDocument.isFolder());

                if (token != null) {
                    megaApi.startDownload(currentDocument, localPath, appData, null, false, token);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.w("Node handle: %s", request.getNodeHandle());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        Timber.d("onRequestUpdate");
    }

    private void refreshOfflineFragment() {
        sendBroadcast(new Intent(OfflineFragment.REFRESH_OFFLINE_FILE_LIST));
    }

    private void refreshSettingsFragment() {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_SETTINGS_UPDATED);
        intent.setAction(ACTION_REFRESH_CLEAR_OFFLINE_SETTING);
        sendBroadcast(intent);
    }
}
