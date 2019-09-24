package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.os.AsyncTask;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.LogUtil.*;

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
        logDebug("doInBackground-Async Task GetCacheSizeTask");

        String size = getCacheSize(context);
        return size;
    }

    @Override
    protected void onPostExecute(String size) {
        logDebug("GetCacheSizeTask::onPostExecute");
        SettingsFragmentLollipop sttFLol = ((ManagerActivityLollipop)context).getSettingsFragment();
        if(sttFLol!=null){
            sttFLol.setCacheSize(size);
        }
    }
}
