package mega.privacy.android.app.jobservices;

import android.app.job.JobParameters;
import android.app.job.JobService;

import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.JobUtil.startCameraUploadService;

public class CameraUploadStarterService extends JobService {

    /**
     * @see  JobService#onStartJob
     */
    @Override
    public boolean onStartJob(JobParameters params) {

        try {
            log("Starter start service here");
            startCameraUploadService(this);
        } catch (Exception e) {
            log("starter Exception: " + e.getMessage() + "_" + e.getStackTrace());
        }
        //there's no more work to be done for this job.
        return false;
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
