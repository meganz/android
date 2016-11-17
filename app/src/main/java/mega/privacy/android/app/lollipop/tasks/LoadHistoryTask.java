package mega.privacy.android.app.lollipop.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.SettingsFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Util;

/*
 * Background task to load history until the last message seen
 */
public class LoadHistoryTask extends AsyncTask<String, Void, String> {
    Context context;

    WeakReference<Activity> mWeakActivity;

    public LoadHistoryTask(WeakReference<Activity> activity){

        this.mWeakActivity = activity;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected String doInBackground(String... params) {
        log("doInBackground-Async LoadHistoryTask");

        Util.clearCache(context);
        String size = Util.getCacheSize(context);
        return size;
    }

    @Override
    protected void onPostExecute(String size) {
        log("LoadHistoryTask::onPostExecute");

        Activity activity = mWeakActivity.get();
        ((ChatActivityLollipop)activity).createInfoToShow();
    }

    public static void log(String message) {
        Util.log("LoadHistoryTask", message);
    }
}
