package mega.privacy.android.app.lollipop.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import mega.privacy.android.app.MegaApplication;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_OFFLINE_SIZE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.OFFLINE_SIZE;
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
        Intent intent = new Intent(ACTION_UPDATE_OFFLINE_SIZE_SETTING);
        intent.putExtra(OFFLINE_SIZE, size);
        MegaApplication.getInstance().sendBroadcast(intent);
    }
}
