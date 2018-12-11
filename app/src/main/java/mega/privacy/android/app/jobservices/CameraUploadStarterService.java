package mega.privacy.android.app.jobservices;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraUploadStarterService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            log("Starter start service here");
            PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CameraUploadStarterServiceWakeLock:");
            if (!wl.isHeld()) {
                wl.acquire();
            }
            
            if(!CameraUploadsService.isServiceRunning){
                Intent intent = new Intent(getApplicationContext(),CameraUploadsService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    log("starter starting on Oreo or above: ");
                    startForegroundService(intent);
                } else {
                    log("starter starting on below Oreo: ");
                    startService(intent);
                }
            }else{
                log("camera upload service has been started");
            }
            
            if(wl != null){
                wl.release();
            }
        } catch (Exception e) {
            log("starter Exception: " + e.getMessage() + "_" + e.getStackTrace());
        }
        return false;
    }
    
    private void log(String s) {
        CameraUploadsService.log(s);
    }
    
    @Override
    public boolean onStopJob(JobParameters params) {
        log("starter on stop job");
        return false;
    }
}
