package mega.privacy.android.app.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.jobservices.CameraUploadStarterService;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import nz.mega.sdk.MegaApiJava;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PermissionUtils.*;

@TargetApi(21)
public class JobUtil {

    private static final long SCHEDULER_INTERVAL = 60 * DateUtils.MINUTE_IN_MILLIS;

    private static final int CU_RESCHEDULE_INTERVAL = 5000;   //milliseconds

    private static final int START_JOB_FAILED = -1;
    private static final int START_JOB_FAILED_NOT_ENABLED = -2;

    private static final int PHOTOS_UPLOAD_JOB_ID = Constants.PHOTOS_UPLOAD_JOB_ID;

    private static synchronized boolean isJobScheduled(Context context,int id) {
        JobScheduler js = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
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
        if(!isCameraUploadEnabled(context)){
            logDebug("Schedule failed as CU not enabled");
            return START_JOB_FAILED_NOT_ENABLED;
        }
        if (isJobScheduled(context,PHOTOS_UPLOAD_JOB_ID)) {
            logDebug("Schedule failed as already scheduled");
            return START_JOB_FAILED;
        }
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(PHOTOS_UPLOAD_JOB_ID,new ComponentName(context,CameraUploadStarterService.class));
            jobInfoBuilder.setPeriodic(SCHEDULER_INTERVAL);
            jobInfoBuilder.setPersisted(true);

            int result = jobScheduler.schedule(jobInfoBuilder.build());
            logDebug("Job scheduled successfully");
            return result;
        }
        logError("Schedule job failed");
        return START_JOB_FAILED;
    }

    public static synchronized void startCameraUploadService(Context context) {
        boolean isOverQuota = isOverquota(context);
        boolean hasReadPermission = hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean isEnabled = isCameraUploadEnabled(context);
        logDebug("isOverQuota:" + isOverQuota +
                ", hasStoragePermission:" + hasReadPermission +
                ", isCameraUploadEnabled:" + isEnabled +
                ", isRunning:" + CameraUploadsService.isServiceRunning);
        if (!CameraUploadsService.isServiceRunning && !isOverQuota && hasReadPermission && isEnabled) {
            Intent newIntent = new Intent(context,CameraUploadsService.class);
            postIntent(context, newIntent);
        }
    }

    private static boolean isOverquota(Context context) {
        MegaApplication app = (MegaApplication)context.getApplicationContext();
        return app.getStorageState() == MegaApiJava.STORAGE_STATE_RED;
    }

    public static synchronized void stopRunningCameraUploadService(Context context) {
        logDebug("stopRunningCameraUploadService");
        Intent stopIntent = new Intent(context,CameraUploadsService.class);
        stopIntent.setAction(CameraUploadsService.ACTION_STOP);
        postIntent(context, stopIntent);
    }

    public static synchronized void cancelAllUploads(Context context) {
        logDebug("stopRunningCameraUploadService");
        Intent stopIntent = new Intent(context,CameraUploadsService.class);
        stopIntent.setAction(CameraUploadsService.ACTION_CANCEL_ALL);
        postIntent(context, stopIntent);
    }

    private static void postIntent(Context context, Intent intent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            logDebug("Starting on Oreo or above");
            context.startForegroundService(intent);
        } else {
            logDebug("Starting below Oreo");
            context.startService(intent);
        }
    }
    
    public static void rescheduleCameraUpload(final Context context) {
        stopRunningCameraUploadService(context);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            
            @Override
            public void run() {
                logDebug("Rescheduling CU");
                scheduleCameraUploadJob(context);
            }
        },CU_RESCHEDULE_INTERVAL);
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
