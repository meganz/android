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
import android.text.format.DateUtils;

import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.jobservices.CameraUploadStarterService;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;

@TargetApi(21)
public class JobUtil {

    public static final long SCHEDULER_INTERVAL = 60 * DateUtils.MINUTE_IN_MILLIS;
    
    public static final int CU_RESCHEDULE_INTERVAL = 5000;   //milliseconds

    public static final int START_JOB_FAILED = -1;

    public static final int PHOTOS_UPLOAD_JOB_ID = Constants.PHOTOS_UPLOAD_JOB_ID;

    public static synchronized boolean isJobScheduled(Context context,int id) {
        JobScheduler js = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js != null) {
            List<JobInfo> jobs = js.getAllPendingJobs();
            for (JobInfo info : jobs) {
                if (info.getId() == id) {
                    LogUtil.logDebug("Job already scheduled");
                    return true;
                }
            }
        }
        LogUtil.logDebug("No scheduled job found");
        return false;
    }

    public static int restart(Context context) {
        stopRunningCameraUploadService(context);
        return scheduleCameraUploadJob(context);
    }

    public static synchronized int scheduleCameraUploadJob(Context context) {
        LogUtil.logDebug("scheduleCameraUploadJob");
        if (isJobScheduled(context,PHOTOS_UPLOAD_JOB_ID)) {
            return START_JOB_FAILED;
        }
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(PHOTOS_UPLOAD_JOB_ID,new ComponentName(context,CameraUploadStarterService.class));
            jobInfoBuilder.setPeriodic(SCHEDULER_INTERVAL);
            jobInfoBuilder.setPersisted(true);

            int result = jobScheduler.schedule(jobInfoBuilder.build());
            LogUtil.logDebug("Job scheduled successfully");
            return result;
        }
        LogUtil.logError("Schedule job failed");
        return START_JOB_FAILED;
    }

    public static synchronized void startCameraUploadService(Context context) {
        boolean isOverQuota = isOverquota(context);
        boolean hasReadPermission = Util.hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        LogUtil.logDebug("isOverQuota:" + isOverQuota + ", hasStoragePermission:" + hasReadPermission);
        if (!CameraUploadsService.isServiceRunning && !isOverQuota && hasReadPermission) {
            Intent newIntent = new Intent(context,CameraUploadsService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LogUtil.logDebug("Starting on Oreo or above");
                context.startForegroundService(newIntent);
            } else {
                LogUtil.logDebug("Starting below Oreo");
                context.startService(newIntent);
            }
        } else {
            LogUtil.logDebug("Service not started because service is running");
        }
    }

    private static boolean isOverquota(Context context) {
        MegaApplication app = (MegaApplication)context.getApplicationContext();
        return app.getStorageState() == MegaApiJava.STORAGE_STATE_RED;
    }

    public static synchronized void stopRunningCameraUploadService(Context context) {
        LogUtil.logDebug("stopRunningCameraUploadService");
        Intent stopIntent = new Intent(context,CameraUploadsService.class);
        stopIntent.setAction(CameraUploadsService.ACTION_STOP);
        context.startService(stopIntent);
    }

    public static synchronized void cancelAllUploads(Context context) {
        LogUtil.logDebug("stopRunningCameraUploadService");
        Intent stopIntent = new Intent(context,CameraUploadsService.class);
        stopIntent.setAction(CameraUploadsService.ACTION_CANCEL_ALL);
        context.startService(stopIntent);
    }
    
    public static void rescheduleCameraUpload(final Context context) {
        stopRunningCameraUploadService(context);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            
            @Override
            public void run() {
                LogUtil.logDebug("Rescheduling CU");
                scheduleCameraUploadJob(context);
            }
        },CU_RESCHEDULE_INTERVAL);
    }
}
