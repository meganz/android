package mega.privacy.android.app.jobservices;

import android.app.job.JobParameters;
import android.app.job.JobService;


import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class CameraUploadStarterService extends JobService {

    /**
     * @see  JobService#onStartJob
     */
    @Override
    public boolean onStartJob(JobParameters params) {

        try {
            logDebug("Start service here");
            startCameraUploadService(this);
        } catch (Exception e) {
            logError("Exception", e);
        }
        //there's no more work to be done for this job.
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        logDebug("onStopJob");
        return false;
    }
}
