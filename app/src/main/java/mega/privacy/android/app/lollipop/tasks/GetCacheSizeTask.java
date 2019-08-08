package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.os.AsyncTask;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.CacheFolderManager.*;

/*
	 * Background task to calculate the size of cache folder
	 */
public class GetCacheSizeTask extends AsyncTask<String, Void, String> {
    Context context;

    public GetCacheSizeTask(Context context){
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        log("doInBackground-Async Task GetCacheSizeTask");

        String size = getCacheSize(context);
        return size;
    }

    @Override
    protected void onPostExecute(String size) {
        log("GetCacheSizeTask::onPostExecute");
        SettingsFragmentLollipop sttFLol = ((ManagerActivityLollipop)context).getSettingsFragment();
        if(sttFLol!=null){
            sttFLol.setCacheSize(size);
        }
    }

    public static void log(String message) {
        Util.log("GetCacheSizeTask", message);
    }
}
