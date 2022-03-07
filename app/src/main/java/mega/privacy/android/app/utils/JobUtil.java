package mega.privacy.android.app.utils;

import static mega.privacy.android.app.jobservices.CameraUploadsService.EXTRA_IGNORE_ATTR_CHECK;
import static mega.privacy.android.app.sync.cusync.CuSyncManager.INACTIVE_HEARTBEAT_INTERVAL_MINUTES;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.jobservices.CameraUploadWork;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.jobservices.SendRegularCuSyncHeartbeatWork;
import nz.mega.sdk.MegaApiJava;

public class JobUtil {

    private static final long CU_SCHEDULER_INTERVAL = 60; // TimeUnit.MINUTES

    private static final int CU_RESCHEDULE_INTERVAL = 5000;   //milliseconds

    private static final int START_JOB_SUCCEED = 0;
    private static final int START_JOB_FAILED = -1;
    private static final int START_JOB_FAILED_NOT_ENABLED = -2;

    public static volatile boolean hasStartedCU;

    public static final String CAMERA_UPLOAD_TAG = "CAMERA_UPLOAD_TAG";
    public static final String HEART_BEAT_TAG = "HEART_BEAT_TAG";

    /**
     * Schedule job of camera upload
     * @param context From which the action is done.
     * @param bKeep true - keep existing work and ignore the new work
     *              false - replace existing work with the new work. This option cancels the existing work
     * @return The result of schedule job
     */
    public static synchronized int scheduleCameraUploadJob(Context context, Boolean bKeep) {
        if (!isCameraUploadEnabled(context)) {
            logDebug("Schedule failed as CU not enabled");
            return START_JOB_FAILED_NOT_ENABLED;
        }

        scheduleCuSyncActiveHeartbeat(context);

        logDebug("Schedule camera upload");
        PeriodicWorkRequest cameraUploadWorkRequest =
                new PeriodicWorkRequest.Builder(CameraUploadWork.class, CU_SCHEDULER_INTERVAL, TimeUnit.MINUTES)
                        // Additional configuration
                        .addTag(CAMERA_UPLOAD_TAG)
                        .build();
        Operation operation = WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(CAMERA_UPLOAD_TAG, bKeep ? ExistingPeriodicWorkPolicy.KEEP : ExistingPeriodicWorkPolicy.REPLACE, cameraUploadWorkRequest);
        logDebug("Schedule camera upload with result: " + operation.getState().getValue());
        return START_JOB_SUCCEED;
    }

    /**
     * Schedule job of camera upload active heartbeat
     * @param context From which the action is done.
     */
    private static void scheduleCuSyncActiveHeartbeat(Context context) {
        logDebug("Schedule Cu Sync Active Heartbeat");
        PeriodicWorkRequest cuSyncActiveHeartbeatWorkRequest =
                new PeriodicWorkRequest.Builder(SendRegularCuSyncHeartbeatWork.class, INACTIVE_HEARTBEAT_INTERVAL_MINUTES, TimeUnit.MINUTES)
                        // Additional configuration
                        .addTag(HEART_BEAT_TAG)
                        .build();
        Operation operation = WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(HEART_BEAT_TAG, ExistingPeriodicWorkPolicy.KEEP,cuSyncActiveHeartbeatWorkRequest);
        logDebug("Schedule Cu Sync Active Heartbeat with result: "+ operation.getState().getValue());
    }

    public static synchronized void startCameraUploadService(Context context) {
        start(context, false);
    }

    public static synchronized void startCameraUploadServiceIgnoreAttr(final Context context) {
        new Handler().postDelayed(() -> start(context, true), CU_RESCHEDULE_INTERVAL);
    }

    private static void start(Context context, boolean shouldIgnoreAttr) {
        boolean isOverQuota = isOverquota(context);
        boolean hasReadPermission = hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean isEnabled = isCameraUploadEnabled(context);
        logDebug("isOverQuota:" + isOverQuota +
                ", hasStoragePermission:" + hasReadPermission +
                ", isCameraUploadEnabled:" + isEnabled +
                ", isRunning:" + CameraUploadsService.isServiceRunning +
                ", hasStartedCU:" + hasStartedCU +
                ", should ignore attr: " + shouldIgnoreAttr);
        if (!CameraUploadsService.isServiceRunning
                && !isOverQuota && hasReadPermission
                && isEnabled && !hasStartedCU) {
            hasStartedCU = true;
            Intent newIntent = new Intent(context, CameraUploadsService.class);
            newIntent.putExtra(EXTRA_IGNORE_ATTR_CHECK, shouldIgnoreAttr);
            ContextCompat.startForegroundService(context, newIntent);
        }
    }

    private static boolean isOverquota(Context context) {
        MegaApplication app = (MegaApplication) context.getApplicationContext();
        return app.getStorageState() == MegaApiJava.STORAGE_STATE_RED;
    }

    public static synchronized void stopRunningCameraUploadService(Context context) {
        if (!isCameraUploadEnabled(context) && !CameraUploadsService.isServiceRunning) return;
        logDebug("Stop CU.");
        Intent stopIntent = new Intent(context, CameraUploadsService.class);
        stopIntent.setAction(CameraUploadsService.ACTION_STOP);
        ContextCompat.startForegroundService(context, stopIntent);
    }

    public static synchronized void cancelAllUploads(Context context) {
        logDebug("stopRunningCameraUploadService");
        Intent stopIntent = new Intent(context, CameraUploadsService.class);
        stopIntent.setAction(CameraUploadsService.ACTION_CANCEL_ALL);
        ContextCompat.startForegroundService(context, stopIntent);
    }

    public static void rescheduleCameraUpload(final Context context) {
        stopRunningCameraUploadService(context);
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            logDebug("Rescheduling CU");
            scheduleCameraUploadJob(context, true);
        }, CU_RESCHEDULE_INTERVAL);
    }

    private static boolean isCameraUploadEnabled(Context context) {
        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        MegaPreferences prefs = dbH.getPreferences();
        if (prefs == null) {
            logDebug("MegaPreferences not defined, so not enabled");
            return false;
        }

        String cuEnabled = prefs.getCamSyncEnabled();
        if (TextUtils.isEmpty(cuEnabled)) {
            logDebug("CU not enabled");
            return false;
        }

        return Boolean.parseBoolean(cuEnabled);
    }

    /**
     * Stop the camera upload work by Tag
     *
     * @param context From which the action is done.
     */
    public static void stopCameraUploadWork(Context context) {
        logDebug("Stop camera upload work");
        cancelWorkByTag(context, CAMERA_UPLOAD_TAG);
    }

    /**
     * Stop regular camera upload sync heartbeat work by Tag
     *
     * @param context From which the action is done.
     */
    public static void stopRegularCuSyncHeartbeatWork(Context context) {
        logDebug("Stop regular cu sync heartbeat work");
        cancelWorkByTag(context, HEART_BEAT_TAG);
    }

    /**
     * Cancels all unfinished work with the given tag.
     *
     * @param context From which the action is done.
     * @param tag The tag used to identify the work
     */
    private static void cancelWorkByTag(Context context, String tag) {
        if (context != null && tag != null) {
            logDebug("Stop work by tag: " + tag);
            WorkManager.getInstance(context).cancelAllWorkByTag(tag);
        }
    }

}
