package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.os.AsyncTask;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.CacheFolderManager.*;

/*
 * Background task to clear cache
 */
public class ClearCacheTask extends AsyncTask<String, Void, String> {
    Context context;

    public ClearCacheTask(Context context){
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        log("doInBackground-Async Task ClearCacheTask");

        clearCache(context);
        String size = getCacheSize(context);
        return size;
    }

    @Override
    protected void onPostExecute(String size) {
        log("ClearCacheTask::onPostExecute");
        SettingsFragmentLollipop sttFLol = ((ManagerActivityLollipop)context).getSettingsFragment();
        if(sttFLol!=null){
            sttFLol.setCacheSize(size);
        }
    }

    public static void log(String message) {
        Util.log("ClearCacheTask", message);
    }
}
