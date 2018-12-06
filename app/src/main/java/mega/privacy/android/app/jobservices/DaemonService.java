package mega.privacy.android.app.jobservices;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;

import mega.privacy.android.app.utils.JobUtil;

@TargetApi(21)
public class DaemonService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        log("DaemonService onStartJob, start camera upload service.");
        int result = JobUtil.startJob(this);
        log("result: " + result + ", success=" + (result != JobUtil.START_JOB_FAILED));
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        log("DaemonService onStopJob");
        return true;
    }

    public static void log(String message) {
//        Util.log("DaemonService",message);
        CameraUploadsService.log("DaemonService",message);
    }
}
