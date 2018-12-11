package mega.privacy.android.app.utils;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateUtils;
import java.util.List;
import mega.privacy.android.app.jobservices.CameraUploadStarterService;
import mega.privacy.android.app.jobservices.CameraUploadsService;

@TargetApi(21)
public class JobUtil {
    //todo short time for testing purpose
    public static final long SCHEDULER_INTERVAL = 15 * DateUtils.MINUTE_IN_MILLIS;
    
    public static final long SCHEDULER_INTERVAL_ANDROID_5_6 = 5 * DateUtils.MINUTE_IN_MILLIS;

    public static final int START_JOB_FAILED = -1;

    public static final int BOOT_JOB_ID = Constants.BOOT_JOB_ID;

    public static final int PHOTOS_UPLOAD_JOB_ID = Constants.PHOTOS_UPLOAD_JOB_ID;

    public static synchronized boolean isJobScheduled(Context context,int id) {
        JobScheduler js = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js != null) {
            List<JobInfo> jobs = js.getAllPendingJobs();
            for (JobInfo info : jobs) {
                if (info.getId() == id) {
                    log("Job already scheduled");
                    return true;
                }
            }
        }
        
        log("no scheduled job found");
        return false;
    }

    public static int restart(Context context) {
        cancelAllJobs(context);
        return startJob(context);
    }

    public static synchronized int startJob(Context context) {
        log("startJob");
        if (isJobScheduled(context,PHOTOS_UPLOAD_JOB_ID)) {
            return START_JOB_FAILED;
        }
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(PHOTOS_UPLOAD_JOB_ID,new ComponentName(context,CameraUploadStarterService.class));
            
            //todo testing purpose need to be removed
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
                jobInfoBuilder.setPeriodic(SCHEDULER_INTERVAL);
            }else{
                jobInfoBuilder.setPeriodic(SCHEDULER_INTERVAL_ANDROID_5_6);
            }
            jobInfoBuilder.setPersisted(true);

            int result = jobScheduler.schedule(jobInfoBuilder.build());
            log("job scheduled successfully");
            return result;
        }
        log("schedule failed");
        return START_JOB_FAILED;
    }

    public static synchronized void cancelAllJobs(Context context) {
        log("cancel all jobs");
        JobScheduler js = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js != null) {
            //stop service
            Intent stopIntent = new Intent(context, CameraUploadsService.class);
            stopIntent.setAction(CameraUploadsService.ACTION_STOP);
            context.startService(stopIntent);
            log("all job cancelled");
            
            //cancel scheduled job
            js.cancelAll();
        }
    }
    
    private static void log(String message){
        //Util.log("JobUtil", message);
        CameraUploadsService.log(message);
    }
}
