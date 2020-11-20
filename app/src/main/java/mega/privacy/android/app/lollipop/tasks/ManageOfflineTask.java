package mega.privacy.android.app.lollipop.tasks;

import android.content.Intent;
import android.os.AsyncTask;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.OfflineUtils.*;

/**
 * Background task to calculate the size of offline folder or to clear offline files.
 */
public class ManageOfflineTask extends AsyncTask<String, Void, String> {

    private boolean isClearOption;

    public ManageOfflineTask(boolean isClearOption) {
        this.isClearOption = isClearOption;
    }

    @Override
    protected String doInBackground(String... params) {
        logDebug("doInBackground-Async Task ManageOfflineTask");

        if (isClearOption) {
            clearOffline(MegaApplication.getInstance());
            MegaApplication.getInstance().getDbH().clearOffline();
        }

        return getOfflineSize(MegaApplication.getInstance());
    }

    @Override
    protected void onPostExecute(String size) {
        logDebug("ManageOfflineTask::onPostExecute");
        Intent intent = new Intent(ACTION_UPDATE_OFFLINE_SIZE_SETTING);
        intent.putExtra(OFFLINE_SIZE, size);
        MegaApplication.getInstance().sendBroadcast(intent);
    }
}