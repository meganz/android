package mega.privacy.android.app.jobservices;

import static android.content.ContentResolver.QUERY_ARG_OFFSET;
import static android.content.ContentResolver.QUERY_ARG_SQL_LIMIT;
import static android.content.ContentResolver.QUERY_ARG_SQL_SELECTION;
import static android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CU;
import static mega.privacy.android.app.constants.BroadcastConstants.PENDING_TRANSFERS;
import static mega.privacy.android.app.constants.BroadcastConstants.PROGRESS;
import static mega.privacy.android.app.constants.SettingsConstants.INVALID_PATH;
import static mega.privacy.android.app.constants.SettingsConstants.VIDEO_QUALITY_ORIGINAL;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.addCompletedTransfer;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.launchTransferUpdateIntent;
import static mega.privacy.android.app.listeners.CreateFolderListener.ExtraAction.INIT_CAMERA_UPLOAD;
import static mega.privacy.android.app.main.ManagerActivity.PENDING_TAB;
import static mega.privacy.android.app.main.ManagerActivity.TRANSFERS_TAB;
import static mega.privacy.android.app.receivers.NetworkTypeChangeReceiver.MOBILE;
import static mega.privacy.android.app.utils.CameraUploadUtil.disableCameraUploadSettingProcess;
import static mega.privacy.android.app.utils.CameraUploadUtil.disableMediaUploadProcess;
import static mega.privacy.android.app.utils.CameraUploadUtil.findDefaultFolder;
import static mega.privacy.android.app.utils.CameraUploadUtil.getPrimaryFolderHandle;
import static mega.privacy.android.app.utils.CameraUploadUtil.getSecondaryFolderHandle;
import static mega.privacy.android.app.utils.Constants.ACTION_CANCEL_CAM_SYNC;
import static mega.privacy.android.app.utils.Constants.ACTION_OVERQUOTA_STORAGE;
import static mega.privacy.android.app.utils.Constants.ACTION_SHOW_SETTINGS;
import static mega.privacy.android.app.utils.Constants.APP_DATA_CU;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION;
import static mega.privacy.android.app.utils.Constants.INVALID_NON_NULL_VALUE;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CAMERA_UPLOADS;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_STORAGE_OVERQUOTA;
import static mega.privacy.android.app.utils.Constants.SEPARATOR;
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.FileUtil.copyFile;
import static mega.privacy.android.app.utils.FileUtil.getFullPathFromTreeUri;
import static mega.privacy.android.app.utils.FileUtil.isVideoFile;
import static mega.privacy.android.app.utils.FileUtil.purgeDirectory;
import static mega.privacy.android.app.utils.ImageProcessor.createImagePreview;
import static mega.privacy.android.app.utils.ImageProcessor.createThumbnail;
import static mega.privacy.android.app.utils.ImageProcessor.createVideoPreview;
import static mega.privacy.android.app.utils.JobUtil.fireSingleHeartbeat;
import static mega.privacy.android.app.utils.JobUtil.fireStopCameraUploadJob;
import static mega.privacy.android.app.utils.JobUtil.scheduleCameraUploadJob;
import static mega.privacy.android.app.utils.MegaNodeUtil.isNodeInRubbishOrDeleted;
import static mega.privacy.android.app.utils.PreviewUtils.getPreviewFolder;
import static mega.privacy.android.app.utils.SDCardUtils.isLocalFolderOnSDCard;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbFolder;
import static mega.privacy.android.app.utils.Util.ONTRANSFERUPDATE_REFRESH_MILLIS;
import static mega.privacy.android.app.utils.Util.getLocalIpAddress;
import static mega.privacy.android.app.utils.Util.getPhotoSyncNameWithIndex;
import static mega.privacy.android.app.utils.Util.getProgressSize;
import static mega.privacy.android.app.utils.Util.isCharging;
import static mega.privacy.android.app.utils.Util.isOnWifi;
import static mega.privacy.android.app.utils.Util.isOnline;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.MediaStore;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.VideoCompressor;
import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository;
import mega.privacy.android.app.domain.entity.SyncRecord;
import mega.privacy.android.app.domain.entity.SyncRecordType;
import mega.privacy.android.app.domain.entity.SyncStatus;
import mega.privacy.android.app.domain.repository.CameraUploadRepository;
import mega.privacy.android.app.listeners.CreateFolderListener;
import mega.privacy.android.app.listeners.GetCameraUploadAttributeListener;
import mega.privacy.android.app.listeners.SetAttrUserListener;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.receivers.NetworkTypeChangeReceiver;
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import timber.log.Timber;

@AndroidEntryPoint
public class CameraUploadsService extends Service implements NetworkTypeChangeReceiver.OnNetworkTypeChangeCallback, MegaRequestListenerInterface, MegaTransferListenerInterface, VideoCompressionCallback {

    private static final int LOCAL_FOLDER_REMINDER_PRIMARY = 1908;
    private static final int LOCAL_FOLDER_REMINDER_SECONDARY = 1909;
    private static final String OVER_QUOTA_NOTIFICATION_CHANNEL_ID = "overquotanotification";
    private static final String ERROR_NOT_ENOUGH_SPACE = "ERROR_NOT_ENOUGH_SPACE";
    private static final String ERROR_CREATE_FILE_IO_ERROR = "ERROR_CREATE_FILE_IO_ERROR";
    private static final String ERROR_SOURCE_FILE_NOT_EXIST = "SOURCE_FILE_NOT_EXIST";
    private static final int BATTERY_STATE_LOW = 20;
    private static final int LOW_BATTERY_LEVEL = 20;
    public static final String CAMERA_UPLOADS_ENGLISH = "Camera Uploads";
    public static final String SECONDARY_UPLOADS_ENGLISH = "Media Uploads";
    public static final String ACTION_CANCEL = "CANCEL_SYNC";
    public static final String ACTION_STOP = "STOP_SYNC";
    public static final String ACTION_CANCEL_ALL = "CANCEL_ALL";
    public static final String ACTION_LOGOUT = "LOGOUT_SYNC";
    public static final String ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER = "PHOTOS_VIDEOS_NEW_FOLDER";
    public static final String EXTRA_IGNORE_ATTR_CHECK = "EXTRA_IGNORE_ATTR_CHECK";
    public static final String CU_CACHE_FOLDER = "cu";
    public static int PAGE_SIZE = 200;
    public static int PAGE_SIZE_VIDEO = 10;
    private static volatile boolean isServiceRunning = false;
    public static boolean uploadingInProgress;
    public static boolean isCreatingPrimary;
    public static boolean isCreatingSecondary;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;

    private final int notificationId = NOTIFICATION_CAMERA_UPLOADS;
    private final String notificationChannelId = NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID;
    private final String notificationChannelName = NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME;

    public static boolean running, ignoreAttr;
    private Handler handler;

    private WifiManager.WifiLock lock;
    private PowerManager.WakeLock wl;

    private boolean isOverQuota;
    private boolean canceled;
    private boolean stopByNetworkStateChange;

    @Inject
    CameraUploadRepository cameraUploadRepository;

    @Inject
    ThreadPoolExecutor megaThreadPoolExecutor;

    private MegaPreferences prefs;

    private MegaApiAndroid megaApi;
    private MegaApiAndroid megaApiFolder;
    private MegaApplication app;

    private String localPath = INVALID_NON_NULL_VALUE;
    private boolean removeGPS = true;
    private long cameraUploadHandle = INVALID_HANDLE;
    private boolean secondaryEnabled;
    private String localPathSecondary = INVALID_NON_NULL_VALUE;
    private long secondaryUploadHandle = INVALID_HANDLE;
    private MegaNode secondaryUploadNode;

    private boolean isLoggingIn;

    private static final int LOGIN_IN = 12;
    private static final int SETTING_USER_ATTRIBUTE = 7;
    private static final int TARGET_FOLDER_NOT_EXIST = 8;
    private static final int CHECKING_USER_ATTRIBUTE = 9;
    private static final int SHOULD_RUN_STATE_FAILED = -1;

    private long lastUpdated = 0;
    private boolean isPrimaryHandleSynced;
    private boolean stopped;
    private NetworkTypeChangeReceiver receiver;

    public static class Media {
        public String filePath;
        public long timestamp;
    }

    private final Queue<Media> cameraFiles = new LinkedList<>();
    private final Queue<Media> primaryVideos = new LinkedList<>();
    private final Queue<Media> secondaryVideos = new LinkedList<>();
    private final Queue<Media> mediaFilesSecondary = new LinkedList<>();
    private MegaNode cameraUploadNode = null;
    private int totalUploaded;
    private int totalToUpload;
    private final List<MegaTransfer> cuTransfers = new ArrayList<>();

    private long currentTimeStamp = 0;
    private long secondaryTimeStamp = 0;
    private long currentVideoTimeStamp = 0;
    private long secondaryVideoTimeStamp = 0;
    private Notification mNotification;
    private Intent mIntent, batteryIntent;
    private PendingIntent mPendingIntent;
    private String tempRoot;
    private VideoCompressor mVideoCompressor;

    private BroadcastReceiver pauseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            new Handler().postDelayed(() -> updateProgressNotification(), 1000);
        }
    };

    private final BroadcastReceiver chargingStopReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mVideoCompressor != null && isChargingRequired(mVideoCompressor.getTotalInputSize() / (1024 * 1024))) {
                Timber.d("Detected device stops charging.");
                mVideoCompressor.stop();
            }
        }
    };

    private final BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            batteryIntent = intent;
            if (isDeviceLowOnBattery(batteryIntent)) {
                Timber.d("Device is on low battery.");
                stopped = true;
                if (megaApi != null) {
                    for (MegaTransfer transfer : cuTransfers) {
                        megaApi.cancelTransfer(transfer);
                    }
                }
                finish();
            }
        }
    };

    private GetCameraUploadAttributeListener getAttrUserListener;
    private SetAttrUserListener setAttrUserListener;
    private CreateFolderListener createFolderListener;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        registerReceiver(chargingStopReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(pauseReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));
        getAttrUserListener = new GetCameraUploadAttributeListener(this);
        setAttrUserListener = new SetAttrUserListener(this);
        createFolderListener = new CreateFolderListener(this, INIT_CAMERA_UPLOAD);
    }

    @Override
    public void onDestroy() {
        Timber.d("Service destroys.");
        super.onDestroy();
        isServiceRunning = false;
        uploadingInProgress = false;
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        unregisterReceiver(chargingStopReceiver);
        unregisterReceiver(batteryInfoReceiver);
        unregisterReceiver(pauseReceiver);
        getAttrUserListener = null;
        setAttrUserListener = null;
        createFolderListener = null;

        // CU process is running, but interrupted.
        if (CameraUploadSyncManager.INSTANCE.isActive()) {
            //Update backups' state.
            CameraUploadSyncManager.INSTANCE.updatePrimaryBackupState(CameraUploadSyncManager.State.CU_SYNC_STATE_TEMPORARY_DISABLED);
            CameraUploadSyncManager.INSTANCE.updateSecondaryBackupState(CameraUploadSyncManager.State.CU_SYNC_STATE_TEMPORARY_DISABLED);

            // Send failed heartbeat.
            CameraUploadSyncManager.INSTANCE.reportUploadInterrupted();
        }
        CameraUploadSyncManager.INSTANCE.stopActiveHeartbeat();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onTypeChanges(int type) {
        Timber.d("Network type change to: %s", type);
        megaThreadPoolExecutor.execute(() -> {
            stopByNetworkStateChange = type == MOBILE && cameraUploadRepository.isSyncByWifi();
            if (stopByNetworkStateChange) {
                for (MegaTransfer transfer : cuTransfers) {
                    megaApi.cancelTransfer(transfer, this);
                }
                stopped = true;
                finish();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("Starting CameraUpload service (flags: " + flags + ", startId: " + startId + ")");
        isServiceRunning = true;
        startForegroundNotification();
        initService();

        if (megaApi == null) {
            Timber.d("MegaApi is null, return.");
            finish();
            return START_NOT_STICKY;
        }

        if (intent != null && intent.getAction() != null) {
            Timber.d("onStartCommand intent action is %s", intent.getAction());
            if (intent.getAction().equals(ACTION_CANCEL) ||
                    intent.getAction().equals(ACTION_STOP) ||
                    intent.getAction().equals(ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER)) {
                Timber.d("Cancel all CameraUpload transfers.");
                for (MegaTransfer transfer : cuTransfers) {
                    megaApi.cancelTransfer(transfer, this);
                }
            } else if (ACTION_CANCEL_ALL.equals(intent.getAction()) || intent.getAction().equals(ACTION_LOGOUT)) {
                Timber.d("Cancel all transfers.");
                megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
            }
            stopped = true;
            finish();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            ignoreAttr = intent.getBooleanExtra(EXTRA_IGNORE_ATTR_CHECK, false);
        }

        Timber.d("Start service here, creating new working thread.");
        startWorkerThread();
        return START_NOT_STICKY;
    }

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

    /**
     * Show a foreground notification.
     * It's a requirement of Android system for foreground service.
     * Should call this both when "onCreate" and "onStartCommand".
     */
    private void startForegroundNotification() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        Notification notification = createNotification(getString(R.string.section_photo_sync), getString(R.string.settings_camera_notif_initializing_title), null, false);
        startForeground(notificationId, notification);
    }

    private void registerNetworkTypeChangeReceiver() {
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        receiver = new NetworkTypeChangeReceiver();
        receiver.setCallback(this);
        registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    private void startWorkerThread() {
        try {
            Thread task = createWorkerThread();
            task.start();
        } catch (Exception ex) {
            Timber.e(ex);
            finish();
        }
    }

    private Thread createWorkerThread() {
        return new Thread() {

            @Override
            public void run() {
                try {
                    int result = shouldRun();
                    Timber.d("Should run result: %s", result);
                    switch (result) {
                        case 0:
                            startCameraUploads();
                            break;
                        case LOGIN_IN:
                        case CHECKING_USER_ATTRIBUTE:
                        case TARGET_FOLDER_NOT_EXIST:
                        case SETTING_USER_ATTRIBUTE:
                            Timber.d("Wait for login or check user attribute.");
                            break;
                        default:
                            finish();
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handleException(e);
                }
            }
        };
    }

    private void startCameraUploads() {
        showNotification(getString(R.string.section_photo_sync), getString(R.string.settings_camera_notif_checking_title), mPendingIntent, false);
        // Start the real uploading process, before is checking settings.
        uploadingInProgress = true;
        getFilesFromMediaStore();
    }

    private boolean shouldCompressVideo() {
        String qualitySetting = prefs.getUploadVideoQuality();
        return qualitySetting != null && Integer.parseInt(qualitySetting) != VIDEO_QUALITY_ORIGINAL;
    }

    private void extractMedia(Cursor cursor, boolean isSecondary, boolean isVideo) {
        try {
            Timber.d("Extract " + cursor.getCount() + " media from cursor, is video: " + isVideo + ", is secondary: " + isSecondary);
            String parentPath = isSecondary ? localPathSecondary : localPath;

            int dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int modifiedColumn = 0, addedColumn = 0;
            if (cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED) != -1) {
                modifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);
            }
            if (cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED) != -1) {
                addedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);
            }

            while (cursor.moveToNext()) {

                Media media = new Media();
                media.filePath = cursor.getString(dataColumn);
                long addedTime = cursor.getLong(addedColumn) * 1000;
                long modifiedTime = cursor.getLong(modifiedColumn) * 1000;
                media.timestamp = Math.max(addedTime, modifiedTime);
                Timber.d("Extract from cursor, add time: " + addedTime + ", modify time: " + modifiedTime + ", chosen time: " + media.timestamp);

                //Check files of the Camera Uploads
                if (checkFile(media, parentPath)) {
                    if (isSecondary) {
                        if (isVideo) {
                            secondaryVideos.add(media);
                        } else {
                            mediaFilesSecondary.add(media);
                        }
                    } else {
                        if (isVideo) {
                            primaryVideos.add(media);
                        } else {
                            cameraFiles.add(media);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void getFilesFromMediaStore() {
        Timber.d("Get pending files from media store database.");
        cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
        if (cameraUploadNode == null) {
            Timber.d("ERROR: primary parent folder is null.");
            finish();
            return;
        }

        String[] projection = {
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED
        };

        String selectionCamera;
        String selectionCameraVideo;
        String selectionSecondary = null;
        String selectionSecondaryVideo = null;

        currentTimeStamp = cameraUploadRepository.getSyncTimeStamp();
        Timber.d("Primary photo timestamp is: %s", currentTimeStamp);

        selectionCamera = "((" + MediaStore.MediaColumns.DATE_MODIFIED + "*1000) > " + currentTimeStamp + " OR " + "(" + MediaStore.MediaColumns.DATE_ADDED + "*1000) > " + currentTimeStamp + ") AND " + MediaStore.MediaColumns.DATA + " LIKE '" + localPath + "%'";

        currentVideoTimeStamp = cameraUploadRepository.getVideoSyncTimeStamp();
        Timber.d("Primary video timestamp is: %s", currentVideoTimeStamp);

        selectionCameraVideo = "((" + MediaStore.MediaColumns.DATE_MODIFIED + "*1000) > " + currentVideoTimeStamp + " OR " + "(" + MediaStore.MediaColumns.DATE_ADDED + "*1000) > " + currentVideoTimeStamp + ") AND " + MediaStore.MediaColumns.DATA + " LIKE '" + localPath + "%'";

        if (secondaryEnabled) {
            Timber.d("Secondary upload is enabled.");
            secondaryUploadNode = megaApi.getNodeByHandle(secondaryUploadHandle);

            String secondaryTime = cameraUploadRepository.getSecondarySyncTimeStamp();
            if (secondaryTime != null) {
                secondaryTimeStamp = Long.parseLong(secondaryTime);
                Timber.d("Secondary photo timestamp is: %s", secondaryTimeStamp);
                selectionSecondary = "((" + MediaStore.MediaColumns.DATE_MODIFIED + "*1000) > " + secondaryTimeStamp + " OR " + "(" + MediaStore.MediaColumns.DATE_ADDED + "*1000) > " + secondaryTimeStamp + ") AND " + MediaStore.MediaColumns.DATA + " LIKE '" + localPathSecondary + "%'";
            }

            String secondaryVideoTime = cameraUploadRepository.getSecondaryVideoSyncTimeStamp();
            if (secondaryVideoTime != null) {
                secondaryVideoTimeStamp = Long.parseLong(secondaryVideoTime);
                Timber.d("Secondary video timestamp is: %s", secondaryVideoTimeStamp);
                selectionSecondaryVideo = "((" + MediaStore.MediaColumns.DATE_MODIFIED + "*1000) > " + secondaryVideoTimeStamp + " OR " + "(" + MediaStore.MediaColumns.DATE_ADDED + "*1000) > " + secondaryVideoTimeStamp + ") AND " + MediaStore.MediaColumns.DATA + " LIKE '" + localPathSecondary + "%'";
            }
        }

        ArrayList<Uri> uris = new ArrayList<>();
        cameraUploadRepository.manageSyncFileUpload(preference -> {
            switch (preference) {
                case MegaPreferences.ONLY_PHOTOS: {
                    Timber.d("Only upload photo.");
                    uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    break;
                }
                case MegaPreferences.ONLY_VIDEOS: {
                    Timber.d("Only upload video.");
                    uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
                    break;
                }
                case MegaPreferences.PHOTOS_AND_VIDEOS: {
                    Timber.d("Upload photo and video.");
                    uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
                    break;
                }
            }
            return Unit.INSTANCE;
        }, () -> {
            Timber.d("What to upload setting is NULL, only upload photo.");
            uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            return Unit.INSTANCE;
        });

        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);
            boolean isVideo = uri.equals(MediaStore.Video.Media.EXTERNAL_CONTENT_URI) || uri.equals(MediaStore.Video.Media.INTERNAL_CONTENT_URI);

            //Primary Media Folder
            Cursor cursorPrimary;
            String orderVideo = MediaStore.MediaColumns.DATE_MODIFIED + " ASC ";
            String orderImage = MediaStore.MediaColumns.DATE_MODIFIED + " ASC ";

            // Only paging for files in internal storage, because files on SD card usually have same timestamp(the time when the SD is loaded).
            boolean shouldPagingPrimary = !isLocalFolderOnSDCard(this, localPath);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && shouldPagingPrimary) {
                Bundle args = new Bundle();
                args.putString(QUERY_ARG_SQL_SORT_ORDER, orderVideo);
                args.putString(QUERY_ARG_OFFSET, "0");
                if (isVideo) {
                    args.putString(QUERY_ARG_SQL_SELECTION, selectionCameraVideo);
                    args.putString(QUERY_ARG_SQL_LIMIT, Integer.toString(PAGE_SIZE_VIDEO));
                } else {
                    args.putString(QUERY_ARG_SQL_SELECTION, selectionCamera);
                    args.putString(QUERY_ARG_SQL_LIMIT, Integer.toString(PAGE_SIZE));
                }
                cursorPrimary = app.getContentResolver().query(uri, projection, args, null);
            } else {
                if (shouldPagingPrimary) {
                    orderVideo += " LIMIT 0," + PAGE_SIZE_VIDEO;
                    orderImage += " LIMIT 0," + PAGE_SIZE;
                }

                if (isVideo) {
                    cursorPrimary = app.getContentResolver().query(uri, projection, selectionCameraVideo, null, orderVideo);
                } else {
                    cursorPrimary = app.getContentResolver().query(uri, projection, selectionCamera, null, orderImage);
                }
            }

            if (cursorPrimary != null) {
                extractMedia(cursorPrimary, false, isVideo);
            }

            //Secondary Media Folder
            if (secondaryEnabled) {
                Timber.d("Secondary is enabled.");
                Cursor cursorSecondary;
                String orderVideoSecondary = MediaStore.MediaColumns.DATE_MODIFIED + " ASC ";
                String orderImageSecondary = MediaStore.MediaColumns.DATE_MODIFIED + " ASC ";

                boolean shouldPagingSecondary = !isLocalFolderOnSDCard(this, localPathSecondary);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && shouldPagingSecondary) {
                    Bundle args = new Bundle();
                    args.putString(QUERY_ARG_SQL_SORT_ORDER, orderVideo);
                    args.putString(QUERY_ARG_OFFSET, "0");

                    if (isVideo) {
                        args.putString(QUERY_ARG_SQL_SELECTION, selectionSecondaryVideo);
                        args.putString(QUERY_ARG_SQL_LIMIT, Integer.toString(PAGE_SIZE_VIDEO));
                    } else {
                        args.putString(QUERY_ARG_SQL_SELECTION, selectionSecondary);
                        args.putString(QUERY_ARG_SQL_LIMIT, Integer.toString(PAGE_SIZE));
                    }
                    cursorSecondary = app.getContentResolver().query(uri, projection, args, null);
                } else {
                    if (shouldPagingSecondary) {
                        orderVideoSecondary += " LIMIT 0," + PAGE_SIZE_VIDEO;
                        orderImageSecondary += " LIMIT 0," + PAGE_SIZE;
                    }

                    if (isVideo) {
                        cursorSecondary = app.getContentResolver().query(uri, projection, selectionSecondaryVideo, null, orderVideoSecondary);
                    } else {
                        cursorSecondary = app.getContentResolver().query(uri, projection, selectionSecondary, null, orderImageSecondary);
                    }
                }

                if (cursorSecondary != null) {
                    extractMedia(cursorSecondary, true, isVideo);
                }
            }
        }
        totalUploaded = 0;
        prepareUpload(cameraFiles, mediaFilesSecondary, primaryVideos, secondaryVideos);
    }

    private void prepareUpload(Queue<Media> primaryList, Queue<Media> secondaryList, Queue<Media> primaryVideoList, Queue<Media> secondaryVideoList) {
        Timber.d("\nPrimary photo count from media store database: " + primaryList.size() + "\n"
                + "Secondary photo count from media store database: " + secondaryList.size() + "\n"
                + "Primary video count from media store database: " + primaryVideoList.size() + "\n"
                + "Secondary video count from media store database: " + secondaryVideoList.size());

        List<SyncRecord> pendingUploadsList = getPendingList(primaryList, false, false);
        Timber.d("Primary photo pending list size: %s", pendingUploadsList.size());
        saveDataToDB(pendingUploadsList);

        List<SyncRecord> pendingVideoUploadsList = getPendingList(primaryVideoList, false, true);
        Timber.d("Primary video pending list size: %s", pendingVideoUploadsList.size());
        saveDataToDB(pendingVideoUploadsList);

        //secondary list
        if (secondaryEnabled) {
            List<SyncRecord> pendingUploadsListSecondary = getPendingList(secondaryList, true, false);
            Timber.d("Secdonary photo pending list size: %s", pendingUploadsListSecondary.size());
            saveDataToDB(pendingUploadsListSecondary);

            List<SyncRecord> pendingVideoUploadsListSecondary = getPendingList(secondaryVideoList, true, true);
            Timber.d("Secdonary video pending list size: %s", pendingVideoUploadsListSecondary.size());
            saveDataToDB(pendingVideoUploadsListSecondary);
        }

        if (stopped) return;

        // Need to maintain timestamp for better performance
        updateTimeStamp();

        List<SyncRecord> finalList = cameraUploadRepository.getPendingSyncRecords();

        // Reset backup state as active.
        CameraUploadSyncManager.INSTANCE.updatePrimaryBackupState(CameraUploadSyncManager.State.CU_SYNC_STATE_ACTIVE);
        CameraUploadSyncManager.INSTANCE.updateSecondaryBackupState(CameraUploadSyncManager.State.CU_SYNC_STATE_ACTIVE);

        if (finalList.size() == 0) {
            if (isCompressedVideoPending()) {
                Timber.d("Pending upload list is empty, now check view compression status.");
                startVideoCompression();
            } else {
                Timber.d("Nothing to upload.");

                // Make sure to send inactive heartbeat.
                fireSingleHeartbeat(this);

                // Make sure to re schedule the job
                scheduleCameraUploadJob(this);

                finish();
                purgeDirectory(new File(tempRoot));
            }
        } else {
            Timber.d("Start to upload " + finalList.size() + " files.");
            startParallelUpload(finalList, false);
        }
    }

    private void startParallelUpload(List<SyncRecord> finalList, boolean isCompressedVideo) {
        CameraUploadSyncManager.INSTANCE.startActiveHeartbeat(finalList);

        for (SyncRecord file : finalList) {
            if (!running) break;

            boolean isSec = file.isSecondary();
            MegaNode parent = isSec ? secondaryUploadNode : cameraUploadNode;

            if (parent == null) continue;


            if (file.getType() == SyncRecordType.TYPE_PHOTO.getValue() && !file.isCopyOnly()) {
                if (removeGPS) {
                    String newPath = createTempFile(file);
                    //IOException occurs.
                    if (ERROR_CREATE_FILE_IO_ERROR.equals(newPath)) continue;

                    // Only retry for 60 seconds
                    int counter = 60;
                    while (ERROR_NOT_ENOUGH_SPACE.equals(newPath) && running && counter != 0) {
                        counter--;
                        try {
                            Timber.d("Waiting for disk space to process");
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //show no space notification
                        if (megaApi.getNumPendingUploads() == 0) {
                            Timber.w("Stop service due to out of space issue");
                            finish();
                            String title = getString(R.string.title_out_of_space);
                            String message = getString(R.string.error_not_enough_free_space);
                            Intent intent = new Intent(this, ManagerActivity.class);
                            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                            showNotification(title, message, pendingIntent, true);
                            return;
                        }
                        newPath = createTempFile(file);
                    }
                    if (!newPath.equals(file.getNewPath())) {
                        file.setNewPath(newPath);
                    }
                } else {
                    // Set as don't remove GPS
                    file.setNewPath(file.getLocalPath());
                }
            }

            String path;
            if (isCompressedVideo || file.getType() == SyncRecordType.TYPE_PHOTO.getValue() || (file.getType() == SyncRecordType.TYPE_VIDEO.getValue() && shouldCompressVideo())) {
                path = file.getNewPath();
                File temp = new File(path);
                if (!temp.exists()) {
                    path = file.getLocalPath();
                }
            } else {
                path = file.getLocalPath();
            }

            if (file.isCopyOnly()) {
                Timber.d("Copy from node, file timestamp is: %s", file.getTimestamp());
                totalToUpload++;
                megaApi.copyNode(megaApi.getNodeByHandle(file.getNodeHandle()), parent, file.getFileName(), this);
            } else {
                File toUpload = new File(path);
                if (toUpload.exists()) {
                    //compare size
                    MegaNode node = checkExsitBySize(parent, toUpload.length());
                    if (node != null && node.getOriginalFingerprint() == null) {
                        Timber.d("Node with handle: " + node.getHandle() + " already exists, delete record from database.");
                        cameraUploadRepository.deleteSyncRecord(path, isSec);
                    } else {
                        totalToUpload++;
                        long lastModified = getLastModifiedTime(file);
                        megaApi.startUpload(path, parent, file.getFileName(), lastModified / 1000,
                                APP_DATA_CU, false, false, null, this);
                    }
                } else {
                    Timber.d("Local file is unavailable, delete record from database.");
                    cameraUploadRepository.deleteSyncRecord(path, isSec);
                }
            }
        }
        if (totalToUpload == totalUploaded) {
            if (isCompressedVideoPending() && !canceled && isCompressorAvailable()) {
                Timber.d("Got pending videos, will start compress.");
                startVideoCompression();
            } else {
                Timber.d("No pending videos, finish.");
                onQueueComplete();
            }
        }
    }

    private MegaNode checkExsitBySize(MegaNode parent, long size) {
        ArrayList<MegaNode> nL = megaApi.getChildren(parent, MegaApiJava.ORDER_ALPHABETICAL_ASC);
        for (MegaNode node : nL) {
            if (node.getSize() == size) {
                return node;
            }
        }
        return null;
    }

    private long getLastModifiedTime(SyncRecord file) {
        File source = new File(file.getLocalPath());
        return source.lastModified();
    }

    private void saveDataToDB(List<SyncRecord> list) {
        for (SyncRecord file : list) {
            Timber.d("Handle with local file which timestamp is: %s", file.getTimestamp());
            if (stopped) return;

            SyncRecord exist = cameraUploadRepository.getSyncRecordOrNull(file.getOriginFingerprint(), file.isSecondary(), file.isCopyOnly());
            if (exist != null) {
                if (exist.getTimestamp() < file.getTimestamp()) {
                    Timber.d("Got newer time stamp.");
                    cameraUploadRepository.deleteSyncRecordLocalPath(exist.getLocalPath(), exist.isSecondary());
                } else {
                    Timber.w("Duplicate sync records.");
                    continue;
                }
            }

            boolean isSec = file.isSecondary();
            MegaNode parent = isSec ? secondaryUploadNode : cameraUploadNode;

            if (!file.isCopyOnly()) {
                File f = new File(file.getLocalPath());
                if (!f.exists()) {
                    Timber.w("File does not exist, remove from database.");
                    cameraUploadRepository.deleteSyncRecordLocalPath(file.getLocalPath(), isSec);
                    continue;
                }
            }

            String fileName;
            boolean inCloud;
            boolean inDatabase;
            int photoIndex = 0;

            if (Boolean.parseBoolean(prefs.getKeepFileNames())) {
                //Keep the file names as device but need to handle same file name in different location
                String tempFileName = file.getFileName();

                do {
                    if (stopped) return;

                    fileName = getNoneDuplicatedDeviceFileName(tempFileName, photoIndex);
                    Timber.d("Keep file name as in device, name index is: %s", photoIndex);
                    photoIndex++;

                    inCloud = megaApi.getChildNode(parent, fileName) != null;
                    inDatabase = cameraUploadRepository.doesFileNameExist(fileName, isSec, SyncRecordType.TYPE_ANY.getValue());
                } while ((inCloud || inDatabase));
            } else {
                do {
                    if (stopped) return;

                    fileName = getPhotoSyncNameWithIndex(getLastModifiedTime(file), file.getLocalPath(), photoIndex);
                    Timber.d("Use MEGA name, name index is: %s", photoIndex);
                    photoIndex++;

                    inCloud = megaApi.getChildNode(parent, fileName) != null;
                    inDatabase = cameraUploadRepository.doesFileNameExist(fileName, isSec, SyncRecordType.TYPE_ANY.getValue());
                } while ((inCloud || inDatabase));
            }

            String extension = "";
            String[] s = fileName.split("\\.");
            if (s.length > 0) {
                extension = s[s.length - 1];
            }

            file.setFileName(fileName);
            String newPath = tempRoot + System.nanoTime() + "." + extension;
            file.setNewPath(newPath);
            Timber.d("Save file to database, new path is: %s", newPath);
            cameraUploadRepository.saveSyncRecord(file);
        }
    }

    private void onQueueComplete() {
        Timber.d("Stopping foreground!");

        if (megaApi.getNumPendingUploads() <= 0) {
            megaApi.resetTotalUploads();
        }
        totalUploaded = 0;
        totalToUpload = 0;

        CameraUploadSyncManager.INSTANCE.reportUploadFinish();
        CameraUploadSyncManager.INSTANCE.stopActiveHeartbeat();
        finish();
    }

    private List<SyncRecord> getPendingList(Queue<Media> mediaList, boolean isSecondary, boolean isVideo) {
        Timber.d("Get pending list, is secondary upload: " + isSecondary + ", is video: " + isVideo);
        ArrayList<SyncRecord> pendingList = new ArrayList<>();

        long parentNodeHandle = (isSecondary) ? secondaryUploadHandle : cameraUploadHandle;
        MegaNode parentNode = megaApi.getNodeByHandle(parentNodeHandle);

        Timber.d("Upload to parent node which handle is: %s", parentNodeHandle);
        int type = isVideo ? SyncRecordType.TYPE_VIDEO.getValue() : SyncRecordType.TYPE_PHOTO.getValue();

        while (mediaList.size() > 0) {
            if (stopped) break;

            Media media = mediaList.poll();
            if (media == null) continue;

            if (cameraUploadRepository.doesLocalPathExist(media.filePath, isSecondary, SyncRecordType.TYPE_ANY.getValue())) {
                Timber.d("Skip media with timestamp: %s", media.timestamp);
                continue;
            }

            //Source file
            File sourceFile = new File(media.filePath);
            String localFingerPrint = megaApi.getFingerprint(media.filePath);
            MegaNode nodeExists = null;

            try {
                nodeExists = getPossibleNodeFromCloud(localFingerPrint, parentNode);
            } catch (Exception e) {
                Timber.e(e);
            }

            if (nodeExists == null) {
                Timber.d("Possible node with same fingerprint is null.");
                float[] gpsData = getGPSCoordinates(sourceFile.getAbsolutePath(), isVideo);
                SyncRecord record = new SyncRecord(0,
                        sourceFile.getAbsolutePath(),
                        null,
                        localFingerPrint,
                        null,
                        media.timestamp,
                        sourceFile.getName(),
                        gpsData[1],
                        gpsData[0],
                        shouldCompressVideo() && type == SyncRecordType.TYPE_VIDEO.getValue() ? SyncStatus.STATUS_TO_COMPRESS.getValue() : SyncStatus.STATUS_PENDING.getValue(),
                        type,
                        null,
                        false,
                        isSecondary);
                Timber.d("Add local file with timestamp: " + record.getTimestamp() + " to pending list, for upload.");
                pendingList.add(record);
            } else {
                Timber.d("Possible node with same fingerprint which handle is: %s", nodeExists.getHandle());
                if (megaApi.getParentNode(nodeExists).getHandle() != parentNodeHandle) {
                    SyncRecord record = new SyncRecord(0,
                            media.filePath,
                            null,
                            nodeExists.getOriginalFingerprint(),
                            nodeExists.getFingerprint(),
                            media.timestamp,
                            sourceFile.getName(),
                            (float) nodeExists.getLongitude(),
                            (float) nodeExists.getLatitude(),
                            SyncStatus.STATUS_PENDING.getValue(),
                            type,
                            nodeExists.getHandle(),
                            true,
                            isSecondary);
                    Timber.d("Add local file with handle: " + record.getNodeHandle() + " to pending list, for copy.");
                    pendingList.add(record);
                } else {
                    if (!isSecondary) {
                        if (isVideo) {
                            if (media.timestamp > currentVideoTimeStamp) {
                                currentVideoTimeStamp = media.timestamp;
                                cameraUploadRepository.setSyncTimeStamp(media.timestamp, DefaultCameraUploadRepository.SyncTimeStamp.PRIMARY_VIDEO);
                            }
                        } else {
                            if (media.timestamp > currentTimeStamp) {
                                currentTimeStamp = media.timestamp;
                                cameraUploadRepository.setSyncTimeStamp(media.timestamp, DefaultCameraUploadRepository.SyncTimeStamp.PRIMARY);
                            }
                        }
                    } else {
                        if (isVideo) {
                            if (media.timestamp > secondaryVideoTimeStamp) {
                                secondaryVideoTimeStamp = media.timestamp;
                                cameraUploadRepository.setSyncTimeStamp(media.timestamp, DefaultCameraUploadRepository.SyncTimeStamp.SECONDARY_VIDEO);
                            }
                        } else {
                            if (media.timestamp > secondaryTimeStamp) {
                                secondaryTimeStamp = media.timestamp;
                                cameraUploadRepository.setSyncTimeStamp(media.timestamp, DefaultCameraUploadRepository.SyncTimeStamp.SECONDARY);
                            }
                        }
                    }
                }
            }
        }
        return pendingList;
    }

    private boolean checkFile(Media media, String path) {
        return media.filePath != null && !isTextEmpty(path) && media.filePath.startsWith(path);
    }

    private int shouldRun() {

        if (!isOnline(this)) {
            Timber.w("Not online");
            return SHOULD_RUN_STATE_FAILED;
        }

        UserCredentials credentials = dbH.getCredentials();
        if (credentials == null) {
            Timber.w("There are not user credentials");
            return SHOULD_RUN_STATE_FAILED;
        }

        if (isDeviceLowOnBattery(batteryIntent)) {
            return BATTERY_STATE_LOW;
        }

        prefs = dbH.getPreferences();
        if (prefs == null) {
            Timber.w("Not defined, so not enabled");
            return SHOULD_RUN_STATE_FAILED;
        }

        if (prefs.getCamSyncEnabled() == null) {
            Timber.w("Not defined, so not enabled");
            return SHOULD_RUN_STATE_FAILED;
        }

        if (!Boolean.parseBoolean(prefs.getCamSyncEnabled())) {
            Timber.w("Camera Sync Not enabled");
            return SHOULD_RUN_STATE_FAILED;
        }

        if (Boolean.parseBoolean(prefs.getCameraFolderExternalSDCard())) {
            Uri uri = Uri.parse(prefs.getUriExternalSDCard());
            localPath = getFullPathFromTreeUri(uri, this);
        } else {
            localPath = prefs.getCamSyncLocalPath();
        }

        if (isTextEmpty(localPath)) {
            Timber.w("localPath is not defined, so not enabled");
            finish();
            return SHOULD_RUN_STATE_FAILED;
        }

        if (!checkPrimaryLocalFolder()) {
            localFolderUnavailableNotification(R.string.camera_notif_primary_local_unavailable, LOCAL_FOLDER_REMINDER_PRIMARY);
            disableCameraUploadSettingProcess();
            dbH.setCamSyncLocalPath(INVALID_NON_NULL_VALUE);
            dbH.setSecondaryFolderPath(INVALID_NON_NULL_VALUE);
            //refresh settings fragment UI
            sendBroadcast(new Intent(ACTION_REFRESH_CAMERA_UPLOADS_SETTING));
            return SHOULD_RUN_STATE_FAILED;
        } else {
            mNotificationManager.cancel(LOCAL_FOLDER_REMINDER_PRIMARY);
        }

        if (!checkSecondaryLocalFolder()) {
            localFolderUnavailableNotification(R.string.camera_notif_secondary_local_unavailable, LOCAL_FOLDER_REMINDER_SECONDARY);
            // disable media upload only
            disableMediaUploadProcess();
            dbH.setSecondaryFolderPath(INVALID_PATH);
            sendBroadcast(new Intent(ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING));
            return SHOULD_RUN_STATE_FAILED;
        } else {
            mNotificationManager.cancel(LOCAL_FOLDER_REMINDER_SECONDARY);
        }

        if (!localPath.endsWith(SEPARATOR)) {
            localPath += SEPARATOR;
        }

        if (prefs.getRemoveGPS() != null) {
            removeGPS = Boolean.parseBoolean(prefs.getRemoveGPS());
        }

        if (prefs.getCamSyncWifi() == null || Boolean.parseBoolean(prefs.getCamSyncWifi())) {
            if (!isOnWifi(this)) {
                Timber.w("Not start, require WiFi.");
                return SHOULD_RUN_STATE_FAILED;
            }
        }

        isLoggingIn = MegaApplication.isLoggingIn();
        if (megaApi.getRootNode() == null && !isLoggingIn) {
            Timber.w("RootNode = null");
            running = true;
            setLoginState(true);
            megaApi.fastLogin(credentials.getSession(), this);

            ChatUtil.initMegaChatApi(credentials.getSession());

            return LOGIN_IN;
        }

        cameraUploadHandle = getPrimaryFolderHandle();
        secondaryUploadHandle = getSecondaryFolderHandle();

        //Prevent checking while app alive because it has been handled by global event
        Timber.d("ignoreAttr: %s", ignoreAttr);
        if (!ignoreAttr && !isPrimaryHandleSynced) {
            Timber.d("Try to get Camera Uploads primary target folder from CU attribute.");
            megaApi.getUserAttribute(USER_ATTR_CAMERA_UPLOADS_FOLDER, getAttrUserListener);
            return CHECKING_USER_ATTRIBUTE;
        }
        return checkTargetFolders();
    }

    /**
     * When local folder is unavailable, CU cannot launch, need to show a notification to let the user know.
     *
     * @param resId  The content text of the notification. Here is the string's res id.
     * @param notiId Notification id, can cancel the notification by the same id when need.
     */
    private void localFolderUnavailableNotification(int resId, int notiId) {
        boolean isShowing = false;
        for (StatusBarNotification notification : mNotificationManager.getActiveNotifications()) {
            if (notification.getId() == notiId) {
                isShowing = true;
            }
        }
        if (!isShowing) {
            mNotification = createNotification(getString(R.string.section_photo_sync), getString(resId), null, false);
            mNotificationManager.notify(notiId, mNotification);
        }
    }

    /**
     * Check the availability of primary local folder.
     * If it's a path in internal storage, just check its existence.
     * If it's a path in SD card, check the corresponding DocumentFile's existence.
     *
     * @return true, if primary local folder is available. false， when it's unavailable.
     */
    private boolean checkPrimaryLocalFolder() {
        // check primary local folder
        if (Boolean.parseBoolean(prefs.getCameraFolderExternalSDCard())) {
            Uri uri = Uri.parse(prefs.getUriExternalSDCard());
            DocumentFile file = DocumentFile.fromTreeUri(this, uri);
            if (file == null) {
                Timber.d("Local folder on sd card is unavailable.");
                return false;
            }

            return file.exists();
        } else {
            return new File(localPath).exists();
        }
    }

    /**
     * Check the availability of secondary local folder.
     * If it's a path in internal storage, just check its existence.
     * If it's a path in SD card, check the corresponding DocumentFile's existence.
     *
     * @return true, if secondary local folder is available. false， when it's unavailable.
     */
    private boolean checkSecondaryLocalFolder() {
        // check secondary local folder if media upload is enabled
        secondaryEnabled = Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled());
        if (secondaryEnabled) {
            if (dbH.getMediaFolderExternalSdCard()) {
                Uri uri = Uri.parse(dbH.getUriMediaExternalSdCard());
                localPathSecondary = getFullPathFromTreeUri(uri, this);
                if (localPathSecondary != null && !localPathSecondary.endsWith(SEPARATOR)) {
                    localPathSecondary += SEPARATOR;
                }

                DocumentFile file = DocumentFile.fromTreeUri(this, uri);
                if (file == null) {
                    Timber.d("Local media folder on sd card is unavailable.");
                    return false;
                }

                return file.exists();
            } else {
                localPathSecondary = prefs.getLocalPathSecondaryFolder();

                if (localPathSecondary == null) return false;

                if (!localPathSecondary.endsWith(SEPARATOR)) {
                    localPathSecondary += SEPARATOR;
                }

                return new File(localPathSecondary).exists();
            }
        } else {
            Timber.d("Not enabled Secondary");
            dbH.setSecondaryUploadEnabled(false);
            // if not enable secondary
            return true;
        }
    }

    /**
     * Before CU process launches, check CU and MU folder.
     *
     * @return 0, if both folders are alright, CU will start normally.
     * TARGET_FOLDER_NOT_EXIST, CU or MU folder is deleted, will create new folder. CU process will launch after the creation completes.
     * SETTING_USER_ATTRIBUTE, set CU attributes with valid hanle. CU process will launch after the setting completes.
     */
    private int checkTargetFolders() {
        long primaryToSet = INVALID_HANDLE;
        // If CU folder in local setting is deleted, then need to reset.
        boolean needToSetPrimary = isNodeInRubbishOrDeleted(cameraUploadHandle);

        if (needToSetPrimary) {
            // Try to find a folder which name is "Camera Uploads" from root.
            cameraUploadHandle = findDefaultFolder(getString(R.string.section_photo_sync));
            // Cannot find a folder with the name, create one.
            if (cameraUploadHandle == INVALID_HANDLE) {
                // Flag, prevent to create duplicate folder.
                if (!isCreatingPrimary) {
                    Timber.d("Must create CU folder.");
                    isCreatingPrimary = true;
                    // Create a folder with name "Camera Uploads" at root.
                    megaApi.createFolder(getString(R.string.section_photo_sync), megaApi.getRootNode(), createFolderListener);
                }
                if (!secondaryEnabled) {
                    return TARGET_FOLDER_NOT_EXIST;
                }
            } else {
                // Found, prepare to set the folder as CU folder.
                primaryToSet = cameraUploadHandle;
            }
        }

        long secondaryToSet = INVALID_HANDLE;
        boolean needToSetSecondary = false;
        // Only check MU folder when secondary upload is enabled.
        if (secondaryEnabled) {
            Timber.d("Secondary uploads are enabled.");
            // If MU folder in local setting is deleted, then need to reset.
            needToSetSecondary = isNodeInRubbishOrDeleted(secondaryUploadHandle);
            if (needToSetSecondary) {
                // Try to find a folder which name is "Media Uploads" from root.
                secondaryUploadHandle = findDefaultFolder(getString(R.string.section_secondary_media_uploads));
                // Cannot find a folder with the name, create one.
                if (secondaryUploadHandle == INVALID_HANDLE) {
                    // Flag, prevent to create duplicate folder.
                    if (!isCreatingSecondary) {
                        Timber.d("Must create MU folder.");
                        isCreatingSecondary = true;
                        // Create a folder with name "Media Uploads" at root.
                        megaApi.createFolder(getString(R.string.section_secondary_media_uploads), megaApi.getRootNode(), createFolderListener);
                    }
                    return TARGET_FOLDER_NOT_EXIST;
                } else {
                    // Found, prepare to set the folder as MU folder.
                    secondaryToSet = secondaryUploadHandle;
                }
            }
        } else {
            Timber.d("Secondary NOT Enabled");
        }

        if (needToSetPrimary || needToSetSecondary) {
            Timber.d("Set CU attribute: " + primaryToSet + " " + secondaryToSet);
            megaApi.setCameraUploadsFolders(primaryToSet, secondaryToSet, setAttrUserListener);
            return SETTING_USER_ATTRIBUTE;
        }
        return 0;
    }

    private void initService() {
        registerNetworkTypeChangeReceiver();
        try {
            app = (MegaApplication) getApplication();
        } catch (Exception ex) {
            finish();
        }

        int wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        WifiManager wifiManager = (WifiManager) (getApplicationContext().getSystemService(Context.WIFI_SERVICE));
        lock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock");
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock:");

        if (!wl.isHeld()) {
            wl.acquire();
        }
        if (!lock.isHeld()) {
            lock.acquire();
        }

        stopByNetworkStateChange = false;
        lastUpdated = 0;
        totalUploaded = 0;
        totalToUpload = 0;
        canceled = false;
        isOverQuota = false;
        running = true;
        handler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PAGE_SIZE = 1000;
            PAGE_SIZE_VIDEO = 50;
        } else {
            PAGE_SIZE = 400;
            PAGE_SIZE_VIDEO = 10;
        }

        megaApi = app.getMegaApi();
        megaApiFolder = app.getMegaApiFolder();

        if (megaApi == null) {
            finish();
            return;
        }

        String previousIP = app.getLocalIpAddress();
        // the new logic implemented in NetworkStateReceiver
        String currentIP = getLocalIpAddress(getApplicationContext());
        app.setLocalIpAddress(currentIP);
        if ((currentIP != null) && (currentIP.length() != 0) && (currentIP.compareTo("127.0.0.1") != 0)) {
            if ((previousIP == null) || (currentIP.compareTo(previousIP) != 0)) {
                Timber.d("Reconnecting...");
                megaApi.reconnect();
            } else {
                Timber.d("Retrying pending connections...");
                megaApi.retryPendingConnections();
            }
        }
        // end new logic
        mIntent = new Intent(this, ManagerActivity.class);
        mIntent.setAction(ACTION_CANCEL_CAM_SYNC);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.putExtra(TRANSFERS_TAB, PENDING_TAB);

        mPendingIntent = PendingIntent.getActivity(this, 0, mIntent, PendingIntent.FLAG_IMMUTABLE);

        megaThreadPoolExecutor.execute(() -> {
            tempRoot = new File(getCacheDir(), CU_CACHE_FOLDER).getAbsolutePath() + File.separator;
            File root = new File(tempRoot);
            if (!root.exists()) {
                root.mkdirs();
            }

            if (dbH.shouldClearCamsyncRecords()) {
                dbH.deleteAllSyncRecords(SyncRecordType.TYPE_ANY.getValue());
                dbH.saveShouldClearCamsyncRecords(false);
            }
        });
    }

    private void handleException(Exception e) {
        Timber.e(e);

        if (running) {
            handler.removeCallbacksAndMessages(null);
            running = false;
        }
        releaseLocks();

        if (isOverQuota) {
            showStorageOverQuotaNotification();
        }

        canceled = true;
        running = false;
        stopForeground(true);
        cancelNotification();
    }

    private void finish() {
        Timber.d("Finish CU process.");

        if (running) {
            handler.removeCallbacksAndMessages(null);
            running = false;
        }
        cancel();
    }

    private void cancel() {
        releaseLocks();

        if (isOverQuota) {
            showStorageOverQuotaNotification();
            fireStopCameraUploadJob(this);
        }

        if (mVideoCompressor != null) {
            mVideoCompressor.stop();
        }
        cuTransfers.clear();
        canceled = true;
        running = false;
        stopForeground(true);
        cancelNotification();
        stopSelf();
    }

    private void cancelNotification() {
        if (mNotificationManager != null) {
            Timber.d("Cancelling notification ID is %s", notificationId);
            mNotificationManager.cancel(notificationId);
        } else {
            Timber.w("No notification to cancel");
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("onRequestFinish: %s", request.getRequestString());

        try {
            requestFinished(request, e);
        } catch (Throwable th) {
            Timber.e(th);
            th.printStackTrace();
        }
    }

    private synchronized void requestFinished(MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_LOGIN) {
            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("Logged in. Setting account auth token for folder links.");
                megaApiFolder.setAccountAuth(megaApi.getAccountAuth());
                Timber.d("Fast login OK, Calling fetchNodes from CameraSyncService");
                megaApi.fetchNodes(this);

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies();
            } else {
                Timber.d("ERROR: %s", e.getErrorString());
                setLoginState(false);
                finish();
            }
        } else if (request.getType() == MegaRequest.TYPE_FETCH_NODES) {
            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("fetch nodes ok");
                setLoginState(false);
                Timber.d("Start service here MegaRequest.TYPE_FETCH_NODES");
                startWorkerThread();
            } else {
                Timber.d("ERROR: %s", e.getErrorString());
                setLoginState(false);
                finish();
            }
        } else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER) {
            Timber.d("Cancel transfer received");
            if (e.getErrorCode() == MegaError.API_OK) {
                new Handler().postDelayed(() -> {
                    if (megaApi.getNumPendingUploads() <= 0) {
                        megaApi.resetTotalUploads();
                    }
                }, 200);
            } else {
                finish();
            }
        } else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS) {
            if (e.getErrorCode() == MegaError.API_OK && megaApi.getNumPendingUploads() <= 0) {
                megaApi.resetTotalUploads();
            }
        } else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS) {
            Timber.d("Pausetransfer false received");
            if (e.getErrorCode() == MegaError.API_OK) {
                finish();
            }
        } else if (request.getType() == MegaRequest.TYPE_COPY) {
            if (e.getErrorCode() == MegaError.API_OK) {
                MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
                String fingerPrint = node.getFingerprint();
                boolean isSecondary = node.getParentHandle() == secondaryUploadHandle;
                dbH.deleteSyncRecordByFingerprint(fingerPrint, fingerPrint, isSecondary);
                CameraUploadSyncManager.INSTANCE.onUploadSuccess(node, isSecondary);
            }
            updateUpload();
        }
    }

    /**
     * Callback when getting CU folder handle from CU attributes completes.
     *
     * @param handle      CU folder hanlde stored in CU atrributes.
     * @param errorCode   Used to get error code to see if the request is successful.
     * @param shouldStart If should start CU process.
     */
    public void onGetPrimaryFolderAttribute(long handle, int errorCode, boolean shouldStart) {
        if (errorCode == MegaError.API_OK || errorCode == MegaError.API_ENOENT) {
            isPrimaryHandleSynced = true;
            if (cameraUploadHandle != handle) cameraUploadHandle = handle;
            if (shouldStart) {
                Timber.d("On get primary, start work thread.");
                startWorkerThread();
            }
        } else {
            Timber.w("Get primary handle faild, finish process.");
            finish();
        }
    }

    /**
     * Callback when getting MU folder handle from CU attributes completes.
     *
     * @param handle    MU folder hanlde stored in CU atrributes.
     * @param errorCode Used to get error code to see if the request is successful.
     */
    public void onGetSecondaryFolderAttribute(long handle, int errorCode) {
        if (errorCode == MegaError.API_OK || errorCode == MegaError.API_ENOENT) {
            if (handle != secondaryUploadHandle) secondaryUploadHandle = handle;
            // Start to upload. Unlike onGetPrimaryFolderAttribute needs to wait for getting MU folder handle completes.
            Timber.d("On get secondary, start work thread.");
            startWorkerThread();
        } else {
            Timber.w("Get secondary handle faild, finish process.");
            finish();
        }
    }

    public void onSetFolderAttribute() {
        Timber.d("On set CU folder, start work thread.");
        startWorkerThread();
    }

    public void onCreateFolder(boolean isSuccessful) {
        if (!isSuccessful) {
            finish();
        }
    }

    private void setLoginState(boolean b) {
        isLoggingIn = b;
        MegaApplication.setLoggingIn(b);
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.w("onRequestTemporaryError: %s", request.getRequestString());
    }

    @Override
    public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
        cuTransfers.add(transfer);
        launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);
    }

    @Override
    public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
        transferUpdated(transfer);
    }

    private synchronized void transferUpdated(MegaTransfer transfer) {
        if (canceled) {
            Timber.d("Transfer cancel: %s", transfer.getNodeHandle());
            megaApi.cancelTransfer(transfer);
            cancel();
            return;
        }

        if (isOverQuota) {
            return;
        }

        updateProgressNotification();
    }

    @Override
    public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
        Timber.w("onTransferTemporaryError: %s", transfer.getNodeHandle());
        if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
            if (e.getValue() != 0)
                Timber.w("TRANSFER OVERQUOTA ERROR: %s", e.getErrorCode());
            else
                Timber.w("STORAGE OVERQUOTA ERROR: %s", e.getErrorCode());

            isOverQuota = true;
            cancel();
        }
    }

    @Override
    public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {
        Timber.d("Image sync finished, error code: " + e.getErrorCode() + ", handle: " + transfer.getNodeHandle() + ", size: " + transfer.getTransferredBytes());

        try {
            launchTransferUpdateIntent(MegaTransfer.TYPE_UPLOAD);
            transferFinished(transfer, e);
        } catch (Throwable th) {
            Timber.e(th);
            th.printStackTrace();
        }
    }

    private synchronized void transferFinished(MegaTransfer transfer, MegaError e) {
        String path = transfer.getPath();
        if (isOverQuota) {
            return;
        }

        if (transfer.getState() == MegaTransfer.STATE_COMPLETED) {
            addCompletedTransfer(new AndroidCompletedTransfer(transfer, e), dbH);
        }

        if (e.getErrorCode() == MegaError.API_OK) {
            Timber.d("Image Sync API_OK");
            MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
            boolean isSecondary = (node.getParentHandle() == secondaryUploadHandle);
            SyncRecord record = dbH.findSyncRecordByNewPath(path);
            if (record == null) {
                record = dbH.findSyncRecordByLocalPath(path, isSecondary);
            }
            if (record != null) {
                CameraUploadSyncManager.INSTANCE.onUploadSuccess(node, record.isSecondary());

                String originalFingerprint = record.getOriginFingerprint();
                megaApi.setOriginalFingerprint(node, originalFingerprint, this);
                megaApi.setNodeCoordinates(node, record.getLatitude(), record.getLongitude(), null);

                File src = new File(record.getLocalPath());
                if (src.exists()) {
                    Timber.d("Creating preview");
                    File previewDir = getPreviewFolder(this);
                    final File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + JPG_EXTENSION);
                    File thumbDir = getThumbFolder(this);
                    final File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + JPG_EXTENSION);
                    final SyncRecord finalRecord = record;
                    if (isVideoFile(transfer.getPath())) {
                        megaThreadPoolExecutor.execute(() -> {
                            File img = new File(finalRecord.getLocalPath());
                            if (!preview.exists()) {
                                createVideoPreview(CameraUploadsService.this, img, preview);
                            }
                            createThumbnail(img, thumb);
                        });
                    } else if (MimeTypeList.typeForName(transfer.getPath()).isImage()) {
                        megaThreadPoolExecutor.execute(() -> {
                            if (!preview.exists()) {
                                createImagePreview(src, preview);
                            }
                            createThumbnail(src, thumb);
                        });
                    }
                }
                //delete database record
                dbH.deleteSyncRecordByPath(path, isSecondary);
                //delete temp files
                if (path.startsWith(tempRoot)) {
                    File temp = new File(path);
                    if (temp.exists()) {
                        temp.delete();
                    }
                }
            }
        } else if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
            Timber.w("Over quota error: %s", e.getErrorCode());
            isOverQuota = true;
            cancel();
        } else {
            Timber.w("Image Sync FAIL: " + transfer.getNodeHandle() + "___" + e.getErrorString());
        }
        if (canceled) {
            Timber.w("Image sync cancelled: %s", transfer.getNodeHandle());
            cancel();
        }
        updateUpload();
    }

    private void updateUpload() {
        if (!canceled) {
            updateProgressNotification();
        }

        totalUploaded++;
        Timber.d("Total to upload is " + totalToUpload + " totalUploaded " + totalUploaded + " pendings are " + megaApi.getNumPendingUploads());
        if (totalToUpload == totalUploaded) {
            Timber.d("Photo upload finished, now checking videos");
            if (isCompressedVideoPending() && !canceled && isCompressorAvailable()) {
                Timber.d("Got pending videos, will start compress");
                startVideoCompression();
            } else {
                Timber.d("No pending videos, finish");
                onQueueComplete();
                sendBroadcast(new Intent(ACTION_UPDATE_CU)
                        .putExtra(PROGRESS, 100)
                        .putExtra(PENDING_TRANSFERS, 0));
            }
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return true;
    }

    private void updateTimeStamp() {
        //primary
        Long timeStampPrimary = dbH.findMaxTimestamp(false, SyncRecordType.TYPE_PHOTO.getValue());
        if (timeStampPrimary == null) {
            timeStampPrimary = 0L;
        }
        if (timeStampPrimary > currentTimeStamp) {
            Timber.d("Update primary photo timestamp with: %s", timeStampPrimary);
            updateCurrentTimeStamp(timeStampPrimary);
        } else {
            Timber.d("Primary photo timestamp is: %s", currentTimeStamp);
        }

        Long timeStampPrimaryVideo = dbH.findMaxTimestamp(false, SyncRecordType.TYPE_VIDEO.getValue());
        if (timeStampPrimaryVideo == null) {
            timeStampPrimaryVideo = 0L;
        }
        if (timeStampPrimaryVideo > currentVideoTimeStamp) {
            Timber.d("Update primary video timestamp with: %s", timeStampPrimaryVideo);
            updateCurrentVideoTimeStamp(timeStampPrimaryVideo);
        } else {
            Timber.d("Primary video timestamp is: %s", currentVideoTimeStamp);
        }

        //secondary
        if (secondaryEnabled) {
            Long timeStampSecondary = dbH.findMaxTimestamp(true, SyncRecordType.TYPE_PHOTO.getValue());
            if (timeStampSecondary == null) {
                timeStampSecondary = 0L;
            }
            if (timeStampSecondary > secondaryTimeStamp) {
                Timber.d("Update secondary photo timestamp with: %s", timeStampSecondary);
                updateSecondaryTimeStamp(timeStampSecondary);
            } else {
                Timber.d("Secondary photo timestamp is: %s", secondaryTimeStamp);
            }

            Long timeStampSecondaryVideo = dbH.findMaxTimestamp(true, SyncRecordType.TYPE_VIDEO.getValue());
            if (timeStampSecondaryVideo == null) {
                timeStampSecondaryVideo = 0L;
            }
            if (timeStampSecondaryVideo > secondaryVideoTimeStamp) {
                Timber.d("Update secondary video timestamp with: %s", timeStampSecondaryVideo);
                updateSecondaryVideoTimeStamp(timeStampSecondaryVideo);
            } else {
                Timber.d("Secondary video timestamp is: %s", secondaryVideoTimeStamp);
            }
        }
    }

    private void updateCurrentTimeStamp(long timeStamp) {
        currentTimeStamp = timeStamp;
        cameraUploadRepository.setSyncTimeStamp(currentTimeStamp, DefaultCameraUploadRepository.SyncTimeStamp.PRIMARY);
    }

    private void updateCurrentVideoTimeStamp(long timeStamp) {
        currentVideoTimeStamp = timeStamp;
        cameraUploadRepository.setSyncTimeStamp(currentVideoTimeStamp, DefaultCameraUploadRepository.SyncTimeStamp.PRIMARY_VIDEO);
    }

    private void updateSecondaryTimeStamp(long timeStamp) {
        secondaryTimeStamp = timeStamp;
        cameraUploadRepository.setSyncTimeStamp(secondaryTimeStamp, DefaultCameraUploadRepository.SyncTimeStamp.SECONDARY);
    }

    private void updateSecondaryVideoTimeStamp(long timeStamp) {
        secondaryVideoTimeStamp = timeStamp;
        cameraUploadRepository.setSyncTimeStamp(secondaryVideoTimeStamp, DefaultCameraUploadRepository.SyncTimeStamp.SECONDARY_VIDEO);
    }

    private boolean isCompressedVideoPending() {
        return dbH.findVideoSyncRecordsByState(SyncStatus.STATUS_TO_COMPRESS.getValue()).size() > 0 && !String.valueOf(VIDEO_QUALITY_ORIGINAL).equals(prefs.getUploadVideoQuality());
    }

    private boolean isCompressorAvailable() {
        if (mVideoCompressor == null) {
            return true;
        } else {
            return !mVideoCompressor.isRunning();
        }
    }

    private void startVideoCompression() {
        List<SyncRecord> fullList = dbH.findVideoSyncRecordsByState(SyncStatus.STATUS_TO_COMPRESS.getValue());
        if (megaApi.getNumPendingUploads() <= 0) {
            megaApi.resetTotalUploads();
        }
        totalUploaded = 0;
        totalToUpload = 0;

        mVideoCompressor = new VideoCompressor(this, this, Integer.parseInt(prefs.getUploadVideoQuality()));
        mVideoCompressor.setPendingList(fullList);
        mVideoCompressor.setOutputRoot(tempRoot);
        long totalPendingSizeInMB = mVideoCompressor.getTotalInputSize() / (1024 * 1024);
        Timber.d("Total videos count are " + fullList.size() + ", " + totalPendingSizeInMB + " mb to Conversion");

        if (shouldStartVideoCompression(totalPendingSizeInMB)) {
            Thread t = new Thread(() -> {
                Timber.d("Starting compressor");
                mVideoCompressor.start();
            });
            t.start();
        } else {
            Timber.d("Compression queue bigger than setting, show notification to user.");
            finish();
            Intent intent = new Intent(this, ManagerActivity.class);
            intent.setAction(ACTION_SHOW_SETTINGS);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            String title = getString(R.string.title_compression_size_over_limit);
            String size = prefs.getChargingOnSize();
            String message = getString(R.string.message_compression_size_over_limit,
                    getString(R.string.label_file_size_mega_byte, String.valueOf(size)));
            showNotification(title, message, pendingIntent, true);
        }
    }

    private boolean shouldStartVideoCompression(long queueSize) {
        if (isChargingRequired(queueSize) && !isCharging(this)) {
            Timber.d("Should not start video compression.");
            return false;
        }
        return true;
    }

    @Override
    public void onInsufficientSpace() {
        Timber.w("Insufficient space for video compression.");
        finish();
        Intent intent = new Intent(this, ManagerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        String title = getResources().getString(R.string.title_out_of_space);
        String message = getResources().getString(R.string.message_out_of_space);
        showNotification(title, message, pendingIntent, true);
    }

    public synchronized void onCompressUpdateProgress(int progress) {
        if (!canceled) {
            String message = getString(R.string.message_compress_video, progress + "%");
            String subText = getString(R.string.title_compress_video, mVideoCompressor.getCurrentFileIndex(), mVideoCompressor.getTotalCount());
            showProgressNotification(progress, mPendingIntent, message, subText, "");
        }
    }

    public synchronized void onCompressSuccessful(SyncRecord record) {
        Timber.d("Compression successfully for file with timestamp: %s", record.getTimestamp());
        dbH.updateSyncRecordStatusByLocalPath(SyncStatus.STATUS_PENDING.getValue(), record.getLocalPath(), record.isSecondary());
    }

    public synchronized void onCompressNotSupported(SyncRecord record) {
        Timber.d("Compression failed, not support for file with timestampe: %s", record.getTimestamp());
    }

    public synchronized void onCompressFailed(SyncRecord record) {
        String localPath = record.getLocalPath();
        boolean isSecondary = record.isSecondary();
        Timber.w("Compression failed for file with timestampe:  %s", record.getTimestamp());

        File srcFile = new File(localPath);
        if (srcFile.exists()) {
            try {
                StatFs stat = new StatFs(tempRoot);
                double availableFreeSpace = stat.getAvailableBytes();
                if (availableFreeSpace > srcFile.length()) {
                    Timber.d("Can not compress but got enough disk space, so should be un-supported format issue");
                    String newPath = record.getNewPath();
                    File temp = new File(newPath);
                    dbH.updateSyncRecordStatusByLocalPath(SyncStatus.STATUS_PENDING.getValue(), localPath, isSecondary);
                    if (newPath.startsWith(tempRoot) && temp.exists()) {
                        temp.delete();
                    }
                } else {
                    //record will remain in DB and will be re-compressed next launch
                }
            } catch (Exception ex) {
                Timber.e(ex);
            }
        } else {
            Timber.w("Compressed video not exists, remove from DB");
            dbH.deleteSyncRecordByLocalPath(localPath, isSecondary);
        }
    }

    public void onCompressFinished(String currentIndexString) {
        if (!canceled) {
            Timber.d("Preparing to upload compressed video.");
            ArrayList<SyncRecord> compressedList = new ArrayList<>(dbH.findVideoSyncRecordsByState(SyncStatus.STATUS_PENDING.getValue()));
            if (compressedList.size() > 0) {
                Timber.d("Start to upload " + compressedList.size() + " compressed videos.");
                startParallelUpload(compressedList, true);
            } else {
                onQueueComplete();
            }
        } else {
            Timber.d("Compress finished, but process is canceled.");
        }
    }

    private synchronized void updateProgressNotification() {
        //refresh UI every 1 seconds to avoid too much workload on main thread
        long now = System.currentTimeMillis();
        if (now - lastUpdated > ONTRANSFERUPDATE_REFRESH_MILLIS) {
            lastUpdated = now;
        } else {
            return;
        }

        int pendingTransfers = megaApi.getNumPendingUploads();
        int totalTransfers = megaApi.getTotalUploads();
        long totalSizePendingTransfer = megaApi.getTotalUploadBytes();
        long totalSizeTransferred = megaApi.getTotalUploadedBytes();

        int progressPercent = (int) Math.round((double) totalSizeTransferred / totalSizePendingTransfer * 100);

        String message;
        if (totalTransfers == 0) {
            message = getString(R.string.download_preparing_files);
        } else {
            int inProgress;
            if (pendingTransfers == 0) {
                inProgress = totalTransfers - pendingTransfers;
            } else {
                inProgress = totalTransfers - pendingTransfers + 1;
            }

            sendBroadcast(new Intent(ACTION_UPDATE_CU)
                    .putExtra(PROGRESS, progressPercent)
                    .putExtra(PENDING_TRANSFERS, pendingTransfers));

            if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                message = StringResourcesUtils.getString(R.string.upload_service_notification_paused, inProgress, totalTransfers);
            } else {
                message = StringResourcesUtils.getString(R.string.upload_service_notification, inProgress, totalTransfers);
            }
        }

        String info = getProgressSize(this, totalSizeTransferred, totalSizePendingTransfer);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mIntent, PendingIntent.FLAG_IMMUTABLE);
        showProgressNotification(progressPercent, pendingIntent, message, info, getString(R.string.settings_camera_notif_title));
    }

    private Notification createNotification(String title, String content, PendingIntent intent, boolean isAutoCancel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
        }

        mBuilder = new NotificationCompat.Builder(this, notificationChannelId);
        mBuilder.setSmallIcon(R.drawable.ic_stat_camera_sync)
                .setOngoing(false)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentText(content)
                .setOnlyAlertOnce(true)
                .setAutoCancel(isAutoCancel);

        if (intent != null) {
            mBuilder.setContentIntent(intent);
        }
        return mBuilder.build();
    }

    private void showNotification(String title, String content, PendingIntent intent, boolean isAutoCancel) {
        mNotification = createNotification(title, content, intent, isAutoCancel);
        mNotificationManager.notify(notificationId, mNotification);
    }

    private void showProgressNotification(int progressPercent, PendingIntent pendingIntent, String message, String subText, String contentText) {
        mNotification = null;
        mBuilder = new NotificationCompat.Builder(this, notificationChannelId);
        mBuilder.setSmallIcon(R.drawable.ic_stat_camera_sync)
                .setProgress(100, progressPercent, false)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(subText))
                .setContentTitle(message)
                .setContentText(contentText)
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setSubText(subText);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mBuilder.setSubText(subText);
        } else {
            mBuilder.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setContentInfo(subText);
        }
        mNotification = mBuilder.build();
        mNotificationManager.notify(notificationId, mNotification);
    }

    private void showStorageOverQuotaNotification() {
        Timber.d("Show storage over quota notification.");

        String contentText = getString(R.string.download_show_info);
        String message = getString(R.string.overquota_alert_title);

        Intent intent = new Intent(this, ManagerActivity.class);
        intent.setAction(ACTION_OVERQUOTA_STORAGE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, OVER_QUOTA_NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_stat_camera_sync)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true)
                .setTicker(contentText)
                .setContentTitle(message)
                .setOngoing(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(OVER_QUOTA_NOTIFICATION_CHANNEL_ID, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
            builder.setContentText(contentText);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setContentText(contentText);
        } else {
            builder.setContentInfo(contentText)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300));

        }
        mNotificationManager.notify(NOTIFICATION_STORAGE_OVERQUOTA, builder.build());
    }

    private void removeGPSCoordinates(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "0/1,0/1,0/1000");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "0");
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "0/1,0/1,0/1000");
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "0");
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "0/1,0/1,0/1000");
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0");
            exif.saveAttributes();
        } catch (IOException e) {
            Timber.e(e);
            e.printStackTrace();
        }
    }

    private String createTempFile(SyncRecord file) {
        File srcFile = new File(file.getLocalPath());
        if (!srcFile.exists()) {
            Timber.d(ERROR_SOURCE_FILE_NOT_EXIST);
            return ERROR_SOURCE_FILE_NOT_EXIST;
        }

        try {
            StatFs stat = new StatFs(tempRoot);
            double availableFreeSpace = stat.getAvailableBytes();
            if (availableFreeSpace <= srcFile.length()) {
                Timber.d(ERROR_NOT_ENOUGH_SPACE);
                return ERROR_NOT_ENOUGH_SPACE;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Timber.e(ex);
        }

        String destPath = file.getNewPath();
        File destFile = new File(destPath);
        try {
            copyFile(srcFile, destFile);
            removeGPSCoordinates(destPath);
        } catch (IOException e) {
            e.printStackTrace();
            Timber.e(e);
            return ERROR_CREATE_FILE_IO_ERROR;
        }
        return destPath;
    }

    private String getNoneDuplicatedDeviceFileName(String fileName, int index) {
        if (index == 0) {
            return fileName;
        }

        String name = "", extension = "";
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            name = fileName.substring(0, pos);
            extension = fileName.substring(pos);
        }

        fileName = name + "_" + index + extension;
        return fileName;
    }

    private float[] getGPSCoordinates(String filePath, boolean isVideo) {
        float[] output = new float[2];
        try {
            if (isVideo) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(filePath);

                String location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
                if (location != null) {
                    boolean secondTry = false;
                    try {
                        final int mid = location.length() / 2; //get the middle of the String
                        String[] parts = {location.substring(0, mid), location.substring(mid)};

                        output[0] = Float.parseFloat(parts[0]);
                        output[1] = Float.parseFloat(parts[1]);

                    } catch (Exception exc) {
                        secondTry = true;
                        Timber.e(exc);
                    }

                    if (secondTry) {
                        try {
                            String latString = location.substring(0, 7);
                            String lonString = location.substring(8, 17);

                            output[0] = Float.parseFloat(latString);
                            output[1] = Float.parseFloat(lonString);

                        } catch (Exception ex) {
                            Timber.e(ex);
                        }
                    }
                } else {
                    Timber.w("No location info");
                }
                retriever.release();
            } else {
                ExifInterface exif = new ExifInterface(filePath);
                double[] latLong = exif.getLatLong();
                if (latLong != null) {
                    output[0] = (float) latLong[0];
                    output[1] = (float) latLong[1];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Timber.e(e);
        }
        return output;
    }

    /**
     * Check if there's a node with the same fingerprint in cloud drive. In order to avoid uploading duplicate file.
     * <p>
     * NOTE: only looking for the node by original fingerprint is not enough,
     * because some old nodes don't have the attribute[OriginalFingerprint].
     * In this case, should also looking for the node by attribute[Fingerprint].
     *
     * @param localFingerPrint Fingerprint of the local file.
     * @param parentNode       Prefered parent node, could be null for searching all the place in cloud drive.
     * @return A node with the same fingerprint, or null when cannot find.
     */
    private MegaNode getPossibleNodeFromCloud(String localFingerPrint, MegaNode parentNode) {
        MegaNode preferNode;

        // Try to find the node by original fingerprint from the selected parent folder.
        MegaNodeList possibleNodeListFPO = megaApi.getNodesByOriginalFingerprint(localFingerPrint, parentNode);
        preferNode = getFirstNodeFromList(possibleNodeListFPO);
        if (preferNode != null) {
            Timber.d("Found node by original fingerprint with the same local fingerprint in node with handle: " + parentNode.getHandle() + ", node handle: " + preferNode.getHandle());
            return preferNode;
        }

        // Try to find the node by fingerprint from the selected parent folder.
        preferNode = megaApi.getNodeByFingerprint(localFingerPrint, parentNode);
        if (preferNode != null) {
            Timber.d("Found node by fingerprint with the same local fingerprint in node with handle: " + parentNode.getHandle() + ", node handle: " + preferNode.getHandle());
            return preferNode;
        }

        // Try to find the node by original fingerprint in the account.
        possibleNodeListFPO = megaApi.getNodesByOriginalFingerprint(localFingerPrint, null);
        preferNode = getFirstNodeFromList(possibleNodeListFPO);
        if (preferNode != null) {
            Timber.d("Found node by original fingerprint with the same local fingerprint in the account, node handle: %s", preferNode.getHandle());
            return preferNode;
        }

        // Try to find the node by fingerprint in the account.
        preferNode = megaApi.getNodeByFingerprint(localFingerPrint);
        if (preferNode != null) {
            Timber.d("Found node by fingerprint with the same local fingerprint in the account, node handle: %s", preferNode.getHandle());
            return preferNode;
        }

        return null;
    }

    private MegaNode getFirstNodeFromList(MegaNodeList megaNodeList) {
        if (megaNodeList != null && megaNodeList.size() > 0) {
            return megaNodeList.get(0);
        }
        return null;
    }

    private void releaseLocks() {
        if ((lock != null) && (lock.isHeld())) {
            try {
                lock.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if ((wl != null) && (wl.isHeld())) {
            try {
                wl.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean isChargingRequired(long queueSize) {
        MegaPreferences preferences = dbH.getPreferences();
        if (preferences != null && preferences.getConversionOnCharging() != null) {
            if (Boolean.parseBoolean(preferences.getConversionOnCharging())) {
                int queueSizeLimit = Integer.parseInt(preferences.getChargingOnSize());
                if (queueSize > queueSizeLimit) {
                    Timber.d("isChargingRequired " + true + ", queue size is " + queueSize + ", limit size is " + queueSizeLimit);
                    return true;
                }
            }
        }
        Timber.d("isChargingRequired " + false);
        return false;
    }

    private boolean isDeviceLowOnBattery(Intent intent) {
        if (intent == null) {
            return false;
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        Timber.d("Device battery level is %s", level);
        return level <= LOW_BATTERY_LEVEL && !isCharging(CameraUploadsService.this);
    }
}
