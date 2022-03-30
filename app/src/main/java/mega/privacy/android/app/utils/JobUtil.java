package mega.privacy.android.app.utils;

import static mega.privacy.android.app.jobservices.CameraUploadsService.EXTRA_IGNORE_ATTR_CHECK;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
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

    // when app is inactive, send heartbeat every 30 minutes
    private static final long INACTIVE_HEARTBEAT_INTERVAL = 30;

    private static final long HEARTBEAT_FLEX_INTERVAL = 20; // 20 minutes

    private static final long CU_SCHEDULER_INTERVAL = 1; // 1 hour

    private static final long SCHEDULER_FLEX_INTERVAL = 50; // 50 minutes

    private static final int CU_RESCHEDULE_INTERVAL = 5000; // 5000 milliseconds

    private static final int START_JOB_SUCCEED = 0;

    private static final int START_JOB_FAILED_NOT_ENABLED = -2;

    public static volatile boolean hasStartedCU;

    public static final String CAMERA_UPLOAD_TAG = "MEGA_CAMERA_UPLOAD_TAG";
    public static final String SINGLE_CAMERA_UPLOAD_TAG = "MEGA_SINGLE_CAMERA_UPLOAD_TAG";
    public static final String HEART_BEAT_TAG = "MEGA_HEART_BEAT_TAG";

    /**
     * Schedule job of camera upload
     *
     * @param context From which the action is done.
     * @return The result of schedule job
     */
    public static synchronized int scheduleCameraUploadJob(Context context) {
        if (!isCameraUploadEnabled(context)) {
            logDebug("Schedule failed as CU not enabled");
            return START_JOB_FAILED_NOT_ENABLED;
        }

        scheduleCameraUploadSyncActiveHeartbeat(context);

        logDebug("JobUtil: scheduleCameraUploadJob()");
        // periodic work that runs during the last 10 minutes of every one hour period
        PeriodicWorkRequest cameraUploadWorkRequest =
                new PeriodicWorkRequest.Builder(CameraUploadWork.class, CU_SCHEDULER_INTERVAL, TimeUnit.HOURS, SCHEDULER_FLEX_INTERVAL, TimeUnit.MINUTES)
                        .addTag(CAMERA_UPLOAD_TAG)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(CAMERA_UPLOAD_TAG, ExistingPeriodicWorkPolicy.KEEP, cameraUploadWorkRequest);
        logDebug("CameraUpload Work Status: " + WorkManager.getInstance(context).getWorkInfosByTag(CAMERA_UPLOAD_TAG));
        return START_JOB_SUCCEED;
    }

    /**
     * Schedule job of camera upload active heartbeat
     *
     * @param context From which the action is done.
     */
    private static void scheduleCameraUploadSyncActiveHeartbeat(Context context) {
        logDebug("JobUtil: scheduleCameraUploadSyncActiveHeartbeat()");
        // periodic work that runs during the last 10 minutes of every half an hour period
        PeriodicWorkRequest cuSyncActiveHeartbeatWorkRequest =
                new PeriodicWorkRequest.Builder(SendRegularCuSyncHeartbeatWork.class, INACTIVE_HEARTBEAT_INTERVAL, TimeUnit.MINUTES, HEARTBEAT_FLEX_INTERVAL, TimeUnit.MINUTES)
                        .addTag(HEART_BEAT_TAG)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(HEART_BEAT_TAG, ExistingPeriodicWorkPolicy.KEEP, cuSyncActiveHeartbeatWorkRequest);
        logDebug("CameraUpload Sync Heartbeat Work Status: " + WorkManager.getInstance(context).getWorkInfosByTag(HEART_BEAT_TAG));
    }

    /**
     * Fire a one time work request of camera upload to upload immediately;
     * It will also schedule the camera upload job inside of {@link CameraUploadsService}
     *
     * @param context From which the action is done.
     * @return The result of the job
     */
    public static synchronized int fireCameraUploadJob(Context context) {
        if (!isCameraUploadEnabled(context)) {
            logDebug("Schedule failed as CU not enabled");
            return START_JOB_FAILED_NOT_ENABLED;
        }

        logDebug("JobUtil: singleCameraUploadJob()");
        OneTimeWorkRequest cameraUploadWorkRequest =
                new OneTimeWorkRequest.Builder(CameraUploadWork.class)
                        .addTag(SINGLE_CAMERA_UPLOAD_TAG)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(SINGLE_CAMERA_UPLOAD_TAG, ExistingWorkPolicy.KEEP, cameraUploadWorkRequest);
        logDebug("Single CameraUpload Work Status: " + WorkManager.getInstance(context).getWorkInfosByTag(SINGLE_CAMERA_UPLOAD_TAG));
        return START_JOB_SUCCEED;
    }

    /**
     * This should be never called outside of {@link CameraUploadWork}, otherwise the WorkManager will not know about this job.
     *
     * @param context from which the action is started
     */
    public static synchronized void startCameraUploadService(Context context) {
        logDebug("JobUtil: startCameraUploadService()");
        start(context, false);
    }

    // TODO refactor like above with additional data param into CameraUploadWork
    public static synchronized void startCameraUploadServiceIgnoreAttr(final Context context) {
        new Handler().postDelayed((() -> {
            logDebug("JobUtil: startCameraUploadServiceIgnoreAttr()");
            start(context, true);
        }), CU_RESCHEDULE_INTERVAL);
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
            logDebug("JobUtil: rescheduleCameraUpload()");
            scheduleCameraUploadJob(context);
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
        logDebug("JobUtil: stopCameraUploadWork()");
        cancelWorkByTag(context, CAMERA_UPLOAD_TAG);
        cancelWorkByTag(context, SINGLE_CAMERA_UPLOAD_TAG);
    }

    /**
     * Stop regular camera upload sync heartbeat work by Tag
     *
     * @param context From which the action is done.
     */
    public static void stopRegularCuSyncHeartbeatWork(Context context) {
        logDebug("JobUtil: stopRegularCuSyncHeartbeatWork()");
        cancelWorkByTag(context, HEART_BEAT_TAG);
    }

    /**
     * Cancels all unfinished work with the given tag.
     *
     * @param context From which the action is done.
     * @param tag     The tag used to identify the work
     */
    private static void cancelWorkByTag(Context context, String tag) {
        if (context != null && tag != null) {
            logDebug("JobUtil: cancelWorkByTag(): " + tag);
            WorkManager.getInstance(context).cancelAllWorkByTag(tag);
        }
    }
}
