package mega.privacy.android.app.utils;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.jobservices.CancelUploadsWorker;
import mega.privacy.android.app.jobservices.StartCameraUploadWorker;
import mega.privacy.android.app.jobservices.StopCameraUploadWorker;
import mega.privacy.android.app.jobservices.SyncHeartbeatWorker;
import nz.mega.sdk.MegaApiJava;
import timber.log.Timber;

public class JobUtil {

    // when app is inactive, send heartbeat every 30 minutes
    private static final long INACTIVE_HEARTBEAT_INTERVAL = 30;

    private static final long HEARTBEAT_FLEX_INTERVAL = 20; // 20 minutes

    private static final long CU_SCHEDULER_INTERVAL = 1; // 1 hour

    private static final long SCHEDULER_FLEX_INTERVAL = 50; // 50 minutes

    private static final int CU_RESCHEDULE_INTERVAL = 5000; // 5000 milliseconds

    private static final int START_JOB_SUCCEED = 0;

    private static final int START_JOB_FAILED_NOT_ENABLED = -2;

    public static final String SHOULD_IGNORE_ATTRIBUTES = "SHOULD_IGNORE_ATTRIBUTES";

    /**
     * Worker tags
     */
    private static final String CAMERA_UPLOAD_TAG = "MEGA_CAMERA_UPLOAD_TAG";

    private static final String SINGLE_CAMERA_UPLOAD_TAG = "MEGA_SINGLE_CAMERA_UPLOAD_TAG";

    private static final String HEART_BEAT_TAG = "MEGA_HEART_BEAT_TAG";

    private static final String SINGLE_HEART_BEAT_TAG = "MEGA_SINGLE_HEART_BEAT_TAG";

    private static final String STOP_CAMERA_UPLOAD_TAG = "MEGA_STOP_CAMERA_UPLOAD_TAG";

    private static final String CANCEL_UPLOADS_TAG = "MEGA_CANCEL_UPLOADS_TAG";

    /**
     * Schedule job of camera upload
     *
     * @param context From which the action is done.
     * @return The result of schedule job
     */
    public static synchronized int scheduleCameraUploadJob(Context context) {
        if (isCameraUploadDisabled(context)) {
            Timber.d("Scheduling CameraUpload failed as CameraUpload not enabled");
            return START_JOB_FAILED_NOT_ENABLED;
        }

        scheduleCameraUploadSyncActiveHeartbeat(context);

        Timber.d("JobUtil: scheduleCameraUploadJob()");
        // periodic work that runs during the last 10 minutes of every one hour period
        PeriodicWorkRequest cameraUploadWorkRequest =
                new PeriodicWorkRequest.Builder(StartCameraUploadWorker.class, CU_SCHEDULER_INTERVAL, TimeUnit.HOURS, SCHEDULER_FLEX_INTERVAL, TimeUnit.MINUTES)
                        .addTag(CAMERA_UPLOAD_TAG)
                        .setInputData(new Data.Builder().putBoolean(SHOULD_IGNORE_ATTRIBUTES, false).build())
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(CAMERA_UPLOAD_TAG, ExistingPeriodicWorkPolicy.KEEP, cameraUploadWorkRequest);
        Timber.d("CameraUpload Work Status: %s", WorkManager.getInstance(context).getWorkInfosByTag(CAMERA_UPLOAD_TAG));
        return START_JOB_SUCCEED;
    }

    /**
     * Schedule job of camera upload active heartbeat
     *
     * @param context From which the action is done.
     */
    private static void scheduleCameraUploadSyncActiveHeartbeat(Context context) {
        Timber.d("JobUtil: scheduleCameraUploadSyncActiveHeartbeat()");
        // periodic work that runs during the last 10 minutes of every half an hour period
        PeriodicWorkRequest cuSyncActiveHeartbeatWorkRequest =
                new PeriodicWorkRequest.Builder(SyncHeartbeatWorker.class, INACTIVE_HEARTBEAT_INTERVAL, TimeUnit.MINUTES, HEARTBEAT_FLEX_INTERVAL, TimeUnit.MINUTES)
                        .addTag(HEART_BEAT_TAG)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(HEART_BEAT_TAG, ExistingPeriodicWorkPolicy.KEEP, cuSyncActiveHeartbeatWorkRequest);
        Timber.d("CameraUpload Sync Heartbeat Work Status: %s", WorkManager.getInstance(context).getWorkInfosByTag(HEART_BEAT_TAG));
    }

    /**
     * Fire a single camera upload heartbeat
     *
     * @param context From which the action is done.
     */
    public static synchronized void fireSingleHeartbeat(Context context) {
        Timber.d("JobUtil: sendSingleHeartbeat()");
        OneTimeWorkRequest heartbeatWorkRequest =
                new OneTimeWorkRequest.Builder(SyncHeartbeatWorker.class).addTag(SINGLE_HEART_BEAT_TAG).build();

        WorkManager.getInstance(context).
                enqueueUniqueWork(SINGLE_HEART_BEAT_TAG, ExistingWorkPolicy.KEEP, heartbeatWorkRequest);
        Timber.d("Single Sync Heartbeat Work Status: %s", WorkManager.getInstance(context).getWorkInfosByTag(SINGLE_HEART_BEAT_TAG));
    }

    /**
     * Fire a one time work request of camera upload to upload immediately;
     * It will also schedule the camera upload job inside of {@link CameraUploadsService}
     *
     * @param context From which the action is done.
     * @return The result of the job
     */
    public static synchronized int fireCameraUploadJob(Context context, boolean shouldIgnoreAttributes) {
        if (isCameraUploadDisabled(context)) {
            Timber.d("Firing CameraUpload request failed as CameraUpload not enabled");
            return START_JOB_FAILED_NOT_ENABLED;
        }

        Timber.d("JobUtil: fireCameraUploadJob()");
        OneTimeWorkRequest cameraUploadWorkRequest =
                new OneTimeWorkRequest.Builder(StartCameraUploadWorker.class)
                        .addTag(SINGLE_CAMERA_UPLOAD_TAG)
                        .setInputData(new Data.Builder().putBoolean(SHOULD_IGNORE_ATTRIBUTES, shouldIgnoreAttributes).build())
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(SINGLE_CAMERA_UPLOAD_TAG, ExistingWorkPolicy.KEEP, cameraUploadWorkRequest);
        Timber.d("Single CameraUpload Work Status: %s", WorkManager.getInstance(context).getWorkInfosByTag(SINGLE_CAMERA_UPLOAD_TAG));
        return START_JOB_SUCCEED;
    }

    /**
     * Fire a request to stop camera upload service.
     *
     * @param context From which the action is done.
     */
    public static synchronized void fireStopCameraUploadJob(Context context) {
        if (isCameraUploadDisabled(context)) {
            Timber.d("Firing StopCameraUpload request failed as CameraUpload not enabled");
            return;
        }

        Timber.d("JobUtil: fireStopCameraUploadJob()");
        OneTimeWorkRequest stopUploadWorkRequest =
                new OneTimeWorkRequest.Builder(StopCameraUploadWorker.class)
                        .addTag(STOP_CAMERA_UPLOAD_TAG)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(STOP_CAMERA_UPLOAD_TAG, ExistingWorkPolicy.KEEP, stopUploadWorkRequest);
        Timber.d("Stop CameraUpload Work Status: %s", WorkManager.getInstance(context).getWorkInfosByTag(STOP_CAMERA_UPLOAD_TAG));
    }

    public static synchronized void fireCancelUploadsJob(Context context) {
        Timber.d("JobUtil: fireCancelUploadsJob()");
        OneTimeWorkRequest cancelWorkRequest =
                new OneTimeWorkRequest.Builder(CancelUploadsWorker.class).addTag(CANCEL_UPLOADS_TAG).build();

        WorkManager.getInstance(context).
                enqueueUniqueWork(CANCEL_UPLOADS_TAG, ExistingWorkPolicy.KEEP, cancelWorkRequest);
        Timber.d("Cancel Uploads Work Status: %s", WorkManager.getInstance(context).getWorkInfosByTag(CANCEL_UPLOADS_TAG));
    }

    public static void rescheduleCameraUpload(final Context context) {
        fireStopCameraUploadJob(context);
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Timber.d("JobUtil: rescheduleCameraUpload()");
            scheduleCameraUploadJob(context);
        }, CU_RESCHEDULE_INTERVAL);
    }

    /**
     * Stop the camera upload work by tag.
     * Stop regular camera upload sync heartbeat work by tag.
     *
     * @param context From which the action is done.
     */
    public static void stopCameraUploadSyncHeartbeatWorkers(Context context) {
        if (context != null) {
            Timber.d("JobUtil: stopCameraUploadSyncHeartbeatWorkers()");
            WorkManager manager = WorkManager.getInstance(context);
            for (String tag : Arrays.asList(CAMERA_UPLOAD_TAG, SINGLE_CAMERA_UPLOAD_TAG, HEART_BEAT_TAG, SINGLE_HEART_BEAT_TAG)) {
                manager.cancelAllWorkByTag(tag);
            }
        }
    }

    private static boolean isCameraUploadDisabled(Context context) {
        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        MegaPreferences prefs = dbH.getPreferences();
        if (prefs == null) {
            Timber.d("MegaPreferences not defined, so not enabled");
            return true;
        }
        String cameraUploadEnabled = prefs.getCamSyncEnabled();
        if (TextUtils.isEmpty(cameraUploadEnabled)) {
            Timber.d("CameraUpload not enabled");
            return true;
        }
        return !Boolean.parseBoolean(cameraUploadEnabled);
    }

    public static boolean isOverQuota(Context context) {
        return ((MegaApplication) context.getApplicationContext()).getStorageState() == MegaApiJava.STORAGE_STATE_RED;
    }
}
