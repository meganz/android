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
        Intent intent = new Intent(ACTION_UPDATE_CACHE_SIZE_SETTING);
        intent.putExtra(CACHE_SIZE, size);
        MegaApplication.getInstance().sendBroadcast(intent);
    }
}
