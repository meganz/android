package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_OFFLINE_SIZE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.OFFLINE_SIZE;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.OfflineUtils.clearOffline;
import static mega.privacy.android.app.utils.OfflineUtils.getOfflineSize;

/**
 * Background task to calculate the size of offline folder or to clear offline files.
 */
public class ManageOfflineTask extends AsyncTask<String, Void, String> {

    private Context context;
    private DatabaseHandler dbH;
    private boolean isClearOption;

    public ManageOfflineTask(Context context, boolean isClearOption) {
        this.context = context;
        this.isClearOption = isClearOption;
        dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
    }

    @Override
    protected String doInBackground(String... params) {
        logDebug("doInBackground-Async Task ManageOfflineTask");

        if (isClearOption) {
            clearOffline(context);
            dbH.clearOffline();
        }

        return getOfflineSize(context);
    }

    @Override
    protected void onPostExecute(String size) {
        logDebug("ManageOfflineTask::onPostExecute");
        Intent intent = new Intent(ACTION_UPDATE_OFFLINE_SIZE_SETTING);
        intent.putExtra(OFFLINE_SIZE, size);
        MegaApplication.getInstance().sendBroadcast(intent);
    }
}