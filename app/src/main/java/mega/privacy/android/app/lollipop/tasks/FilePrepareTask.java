package mega.privacy.android.app.lollipop.tasks;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.util.List;

import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.lollipop.ContactPropertiesActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;

/*
	 * Background task to process files for uploading
	 */
public class FilePrepareTask extends AsyncTask<Intent, Void, List<ShareInfo>> {
    Context context;

    public FilePrepareTask(Context context){
        log("FilePrepareTask::FilePrepareTask");
        this.context = context;
    }

    @Override
    protected List<ShareInfo> doInBackground(Intent... params) {
        log("FilePrepareTask::doInBackGround");
        return ShareInfo.processIntent(params[0], context);
    }

    @Override
    protected void onPostExecute(List<ShareInfo> info) {
        log("FilePrepareTask::onPostExecute");
//        filePreparedInfos = info;
        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop)context).onIntentProcessed(info);
        }
        else if(context instanceof ContactPropertiesActivityLollipop){
            ((ContactPropertiesActivityLollipop)context).onIntentProcessed(info);
        }
    }

    public static void log(String message) {
        Util.log("FilePrepareTask", message);
    }
}
