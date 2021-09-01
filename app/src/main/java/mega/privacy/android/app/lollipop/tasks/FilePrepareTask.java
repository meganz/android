package mega.privacy.android.app.lollipop.tasks;

import android.content.Intent;
import android.os.AsyncTask;

import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.ShareInfo;

import static mega.privacy.android.app.utils.LogUtil.*;

/**
 * Background task to process files for uploading
 */
public class FilePrepareTask extends AsyncTask<Intent, Void, List<ShareInfo>> {
    private final ProcessedFilesCallback callback;

    public FilePrepareTask(ProcessedFilesCallback callback) {
        logDebug("FilePrepareTask::FilePrepareTask");
        this.callback = callback;
    }

    @Override
    protected List<ShareInfo> doInBackground(Intent... params) {
        logDebug("FilePrepareTask::doInBackGround");
        return ShareInfo.processIntent(params[0], MegaApplication.getInstance());
    }

    @Override
    protected void onPostExecute(List<ShareInfo> info) {
        logDebug("FilePrepareTask::onPostExecute");
        callback.onIntentProcessed(info);
    }

    public interface ProcessedFilesCallback {
        void onIntentProcessed(List<ShareInfo> info);
    }
}
