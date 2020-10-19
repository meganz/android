package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import mega.privacy.android.app.MegaApplication;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CACHE_SIZE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.CACHE_SIZE;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.LogUtil.*;

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
        logDebug("doInBackground-Async Task ClearCacheTask");

        clearCache(context);
        String size = getCacheSize(context);
        return size;
    }

    @Override
    protected void onPostExecute(String size) {
        logDebug("ClearCacheTask::onPostExecute");
        Intent intent = new Intent(ACTION_UPDATE_CACHE_SIZE_SETTING);
        intent.putExtra(CACHE_SIZE, size);
        MegaApplication.getInstance().sendBroadcast(intent);
    }
}
