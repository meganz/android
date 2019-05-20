package mega.privacy.android.app.jobservices;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;

import mega.privacy.android.app.utils.JobUtil;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.JobUtil.startCameraUploadService;

public class CameraUploadStarterService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            log("Starter start service here");
            int result = JobUtil.startDaemon(this);
            log("start daemon result: " + result + ", success=" + (result != JobUtil.START_JOB_FAILED));
            startCameraUploadService(this);
        } catch (Exception e) {
            log("starter Exception: " + e.getMessage() + "_" + e.getStackTrace());
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return false;
        } else {
            return true;
        }
    }

    private void log(String message) {
        Util.log("CameraUploadStarterService", message);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        log("starter on stop job");
        return false;
    }
}
