package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.os.AsyncTask;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.OfflineUtils.*;

/*
	 * Background task to clear offline files
	 */
public class ClearOfflineTask extends AsyncTask<String, Void, String> {
    Context context;
    DatabaseHandler dbH;

    public ClearOfflineTask(Context context){

        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
    }

    @Override
    protected String doInBackground(String... params) {
        LogUtil.logDebug("doInBackground-Async Task ClearOfflineTask");

        clearOffline(context);
        dbH.clearOffline();
        String size = getOfflineSize(context);
        return size;
    }

    @Override
    protected void onPostExecute(String size) {
        LogUtil.logDebug("ClearOfflineTask::onPostExecute");
        SettingsFragmentLollipop sttFLol = ((ManagerActivityLollipop)context).getSettingsFragment();
        if(sttFLol!=null){
            sttFLol.setOfflineSize(size);
        }
    }
}
