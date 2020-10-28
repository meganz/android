package mega.privacy.android.app.utils;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.jobservices.CameraUploadStarterService;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.jobservices.CuSyncInactiveHeartbeatService;
import mega.privacy.android.app.sync.cusync.CuSyncManager;
import nz.mega.sdk.MegaApiJava;

import static mega.privacy.android.app.jobservices.CameraUploadsService.EXTRA_IGNORE_ATTR_CHECK;
import static mega.privacy.android.app.utils.Constants.CU_SYNC_INACTIVE_HEARTBEAT_JOB_ID;
import static mega.privacy.android.app.utils.Constants.PHOTOS_UPLOAD_JOB_ID;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.PermissionUtils.hasPermissions;

public class JobUtil {

    private static final long SCHEDULER_INTERVAL = 60 * DateUtils.MINUTE_IN_MILLIS;

    private static final int CU_RESCHEDULE_INTERVAL = 5000;   //milliseconds

    private static final int START_JOB_FAILED = -1;
    private static final int START_JOB_FAILED_NOT_ENABLED = -2;

    public static volatile boolean hasStartedCU;

    private static synchronized boolean isJobScheduled(Context context, int id) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js != null) {
            List<JobInfo> jobs = js.getAllPendingJobs();
            for (JobInfo info : jobs) {
                if (info.getId() == id) {
                    logDebug("Job already scheduled");
                    return true;
                }
            }
        }
        logDebug("No scheduled job found");
        return false;
    }

    public static synchronized int scheduleCameraUploadJob(Context context) {
        if (!isCameraUploadEnabled(context)) {
            logDebug("Schedule failed as CU not enabled");
            return START_JOB_FAILED_NOT_ENABLED;
        }

        scheduleCuSyncInactiveHeartbeat(context);

        if (isJobScheduled(context, PHOTOS_UPLOAD_JOB_ID)) {
            logDebug("Schedule failed as already scheduled");
            return START_JOB_FAILED;
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(PHOTOS_UPLOAD_JOB_ID,
                    new ComponentName(context, CameraUploadStarterService.class));
            jobInfoBuilder.setPeriodic(SCHEDULER_INTERVAL);
            jobInfoBuilder.setPersisted(true);

            int result = jobScheduler.schedule(jobInfoBuilder.build());
            logDebug("Job scheduled successfully");
            return result;
        }
        logError("Schedule job failed");
        return START_JOB_FAILED;
    }

    private static void scheduleCuSyncInactiveHeartbeat(Context context) {
        if (isJobScheduled(context, CU_SYNC_INACTIVE_HEARTBEAT_JOB_ID)) {
            return;
        }

        JobScheduler scheduler
                = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.schedule(
                    new JobInfo.Builder(CU_SYNC_INACTIVE_HEARTBEAT_JOB_ID,
                            new ComponentName(context, CuSyncInactiveHeartbeatService.class))
                            .setPeriodic(TimeUnit.SECONDS.toMillis(
                                    CuSyncManager.INACTIVE_HEARTBEAT_INTERVAL_SECONDS))
                            .setPersisted(true)
                            .build()
            );
        }
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
                ", isRunning:" + CameraUploadsService.isServiceRunning);
        if (!CameraUploadsService.isServiceRunning && !isOverQuota && hasReadPermission && isEnabled && !hasStartedCU) {
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
}
