package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CACHE_SIZE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_OFFLINE_SIZE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.CACHE_SIZE;
import static mega.privacy.android.app.constants.BroadcastConstants.OFFLINE_SIZE;
import static mega.privacy.android.app.utils.LogUtil.*;
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
        logDebug("doInBackground-Async Task ClearOfflineTask");

        clearOffline(context);
        dbH.clearOffline();
        String size = getOfflineSize(context);
        return size;
    }

    @Override
    protected void onPostExecute(String size) {
        logDebug("ClearOfflineTask::onPostExecute");
        Intent intent = new Intent(ACTION_UPDATE_OFFLINE_SIZE_SETTING);
        intent.putExtra(OFFLINE_SIZE, size);
        MegaApplication.getInstance().sendBroadcast(intent);
    }
}
