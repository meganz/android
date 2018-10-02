package mega.privacy.android.app.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.text.format.DateUtils;

import java.util.List;

import mega.privacy.android.app.jobservices.BootJobService;

import static mega.privacy.android.app.utils.Util.logJobState;

public class JobUtil {

    public static final long SCHEDULER_INTERVAL = 60 * DateUtils.MINUTE_IN_MILLIS;

    public static final int START_JOB_FAILED = -1;

    public static final int BOOT_JOB_ID = Constants.BOOT_JOB_ID;

    public static boolean isJobScheduled(Context context,int id) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js != null) {
            List<JobInfo> jobs = js.getAllPendingJobs();
            for (JobInfo info : jobs) {
                if (info.getId() == id) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int startJob(Context context) {
        if(isJobScheduled(context, BOOT_JOB_ID)) {
            return START_JOB_FAILED;
        }
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        if (jobScheduler != null) {
            JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(BOOT_JOB_ID,new ComponentName(context,BootJobService.class));
            jobInfoBuilder.setPeriodic(SCHEDULER_INTERVAL);
            jobInfoBuilder.setPersisted(true);

            int result = jobScheduler.schedule(jobInfoBuilder.build());
            logJobState(result,BootJobService.class.getName());
            return result;
        }
        return START_JOB_FAILED;
    }

    public static void cancelAllJobs(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js != null) {
            js.cancelAll();
        }
    }

    public static void cancelScheduledJob(Context context,int id) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js != null) {
            js.cancel(id);
        }
    }
}
