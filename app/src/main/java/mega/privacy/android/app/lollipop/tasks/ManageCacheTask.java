package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import mega.privacy.android.app.MegaApplication;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CACHE_SIZE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.CACHE_SIZE;
import static mega.privacy.android.app.utils.CacheFolderManager.clearCache;
import static mega.privacy.android.app.utils.CacheFolderManager.getCacheSize;
import static mega.privacy.android.app.utils.LogUtil.logDebug;

/**
 * Background task to calculate the size of cache folder or to clear cache.
 */
public class ManageCacheTask extends AsyncTask<String, Void, String> {
    private final Context context;
    private final boolean isClearOption;

    public ManageCacheTask(Context context, boolean isClearOption) {
        this.context = context;
        this.isClearOption = isClearOption;
    }

    @Override
    protected String doInBackground(String... params) {
        logDebug("doInBackground-Async Task ManageCacheTask");

        if (isClearOption) {
            clearCache(context);
        }

        return getCacheSize(context);
    }

    @Override
    protected void onPostExecute(String size) {
        logDebug("ManageCacheTask::onPostExecute");
        Intent intent = new Intent(ACTION_UPDATE_CACHE_SIZE_SETTING);
        intent.putExtra(CACHE_SIZE, size);
        MegaApplication.getInstance().sendBroadcast(intent);
    }
}
