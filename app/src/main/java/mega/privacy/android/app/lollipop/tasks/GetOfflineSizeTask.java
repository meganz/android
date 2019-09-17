package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.os.AsyncTask;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;

/*
 * Background task to calculate the size of offline folder
 */
public class GetOfflineSizeTask extends AsyncTask<String, Void, String> {
    Context context;

    public GetOfflineSizeTask(Context context){
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        logDebug("doInBackground-Async Task GetOfflineSizeTask");

        String size = getOfflineSize(context);
        return size;
    }

    @Override
    protected void onPostExecute(String size) {
        logDebug("GetOfflineSizeTask::onPostExecute");
        SettingsFragmentLollipop sttFLol = ((ManagerActivityLollipop)context).getSettingsFragment();
        if(sttFLol!=null){
            sttFLol.setOfflineSize(size);
        }
    }
}
