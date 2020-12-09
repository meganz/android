package mega.privacy.android.app.lollipop.tasks;

import android.content.Intent;
import android.os.AsyncTask;
import mega.privacy.android.app.MegaApplication;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.LogUtil.logDebug;

/**
 * Background task to calculate the size of cache folder or to clear cache.
 */
public class ManageCacheTask extends AsyncTask<String, Void, String> {
    private final boolean isClearOption;

    public ManageCacheTask(boolean isClearOption) {
        this.isClearOption = isClearOption;
    }

    @Override
    protected String doInBackground(String... params) {
        logDebug("doInBackground-Async Task ManageCacheTask");

        if (isClearOption) {
            clearCache(MegaApplication.getInstance());
        }

        return getCacheSize(MegaApplication.getInstance());
    }

    @Override
    protected void onPostExecute(String size) {
        logDebug("ManageCacheTask::onPostExecute");
        Intent intent = new Intent(ACTION_UPDATE_CACHE_SIZE_SETTING);
        intent.putExtra(CACHE_SIZE, size);
        MegaApplication.getInstance().sendBroadcast(intent);
    }
}
