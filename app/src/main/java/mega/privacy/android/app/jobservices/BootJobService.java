package mega.privacy.android.app.jobservices;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.List;

import static android.app.job.JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS;
import static mega.privacy.android.app.utils.Constants.PHOTOS_UPLOAD_JOB_ID;
import static mega.privacy.android.app.utils.Util.logJobState;

@TargetApi(Build.VERSION_CODES.N) //api 24
public class BootJobService extends JobService {
    
    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d("Yuan", "Boot job on start");
        final JobScheduler mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        
        //reschedule to apply latest user setting
        if(isScheduled(PHOTOS_UPLOAD_JOB_ID)){
            mJobScheduler.cancel(PHOTOS_UPLOAD_JOB_ID);
        }
        
        
        //todo check all user setting here?
        
        ComponentName name = new ComponentName(getApplicationContext().getPackageName(), CameraUploadsService.class.getName());
        
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(PHOTOS_UPLOAD_JOB_ID, name);
//        jobInfoBuilder.setPeriodic(60 * DateUtils.MINUTE_IN_MILLIS);
//
//        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
//         final Uri MEDIA_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/");
//        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(MEDIA_URI,0));
        
        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, FLAG_NOTIFY_FOR_DESCENDANTS));
        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Images.Media.INTERNAL_CONTENT_URI, FLAG_NOTIFY_FOR_DESCENDANTS));
        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, FLAG_NOTIFY_FOR_DESCENDANTS));
        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Video.Media.INTERNAL_CONTENT_URI, FLAG_NOTIFY_FOR_DESCENDANTS));
        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, FLAG_NOTIFY_FOR_DESCENDANTS));
        jobInfoBuilder.addTriggerContentUri(new JobInfo.TriggerContentUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, FLAG_NOTIFY_FOR_DESCENDANTS));
        
        int result = mJobScheduler.schedule(jobInfoBuilder.build());
        logJobState(result, CameraUploadsService.class.getName());
        
        return true;
    }
    
    // Check whether this job is currently scheduled.
    private boolean isScheduled(int id) {
        JobScheduler js = getSystemService(JobScheduler.class);
        List<JobInfo> jobs = js.getAllPendingJobs();
        if (jobs == null) {
            return false;
        }
        for (int i = 0;i < jobs.size();i++) {
            if (jobs.get(i).getId() == id) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
